//! Save and Save As off the window's thread (#3044): writing a large design (and the model a
//! save keeps with it) takes long enough that the window must keep drawing meanwhile. The
//! command runs on a copy of the session on a worker; the design waits for it like it waits for
//! a file picker, and the outcome is taken up as running the command here would have left it.

use serde_json::Value;
use solidcraft_engine::Session;

use crate::SolveApp;

/// A save being written on a worker thread.
pub struct Saving {
    command: &'static str,
    adopt_save: bool,
    on_success: Option<Box<dyn FnOnce(&mut SolveApp, &Value)>>,
    done: std::sync::mpsc::Receiver<(Session, Result<Value, String>)>,
}

/// Run `file.save` or `file.save_as` with `params`. On the desktop it runs on a worker and is
/// taken up by [`tick`] (true: it is under way); in the browser, or when no thread can start, it
/// runs here and is finished on return (false).
pub fn start(app: &mut SolveApp, command: &'static str, params: Value) -> bool {
    start_job(app, command, params, true, None)
}

/// Run a file export on the save worker without marking the design itself as saved. A provider
/// output is written back on the worker before `on_success` runs on the UI thread.
pub(crate) fn start_export(
    app: &mut SolveApp,
    command: &'static str,
    params: Value,
    on_success: Option<Box<dyn FnOnce(&mut SolveApp, &Value)>>,
) -> bool {
    let on_success = on_success.or_else(|| {
        let path = params.get("path").and_then(Value::as_str)?.to_owned();
        let language = app.preferences.language.clone();
        Some(Box::new(move |app: &mut SolveApp, _result: &Value| {
            let _language = crate::i18n::scope(&language);
            app.set_status(crate::i18n::format("ui.saved_to", &[("path", &path)]), false);
        }) as Box<dyn FnOnce(&mut SolveApp, &Value)>)
    });
    start_job(app, command, params, false, on_success)
}

fn start_job(
    app: &mut SolveApp,
    command: &'static str,
    params: Value,
    adopt_save: bool,
    on_success: Option<Box<dyn FnOnce(&mut SolveApp, &Value)>>,
) -> bool {
    #[cfg(not(target_arch = "wasm32"))]
    if app.saving.is_none() && app.file_picker.is_none() && !app.file_load.busy() {
        let mut copy = app.session.scratch();
        // The copy has no file: the command saves to this session's (and names the design after it).
        copy.path.clone_from(&app.session.path);
        let (tx, rx) = std::sync::mpsc::channel();
        let given = params.clone();
        let output_path = params.get("path").and_then(Value::as_str).map(str::to_owned);
        let persist_document_save = app.services.persist_document_save.clone();
        let job = std::thread::Builder::new().name("save".into()).spawn(move || {
            let r = copy.execute(command, &given).map_err(|e| e.to_string()).and_then(|value| {
                let path = output_path.as_deref().or(copy.path.as_deref());
                if let (Some(persist), Some(path)) = (&persist_document_save, path) {
                    persist(path)?;
                }
                Ok(value)
            });
            let _ = tx.send((copy, r));
        });
        if job.is_ok() {
            app.saving = Some(Saving { command, adopt_save, on_success, done: rx });
            return true;
        }
        // No thread to write on: save here, as before.
    }
    let result = app.run(command, params);
    if let (Some(on_success), Ok(value)) = (on_success, result.as_ref()) {
        on_success(app, value);
    }
    false
}

/// The command started by a save picker may use a worker. Save jobs persist their output on that
/// worker; direct exporters and small sidecar writers can flush the selected staged file here.
pub(crate) fn picker_save_completed(app: &mut SolveApp, path: &str) {
    if app.saving.is_some() || app.file_load.busy() {
        return;
    }
    let persist = app.services.persist_document_save.clone();
    if let Some(persist) = persist
        && let Err(error) = persist(path)
    {
        app.set_status(error, true);
    }
}

/// Take up a finished save (`wait`: wait for a running one).
pub fn tick(app: &mut SolveApp, wait: bool) {
    let Some(saving) = app.saving.as_ref() else { return };
    let outcome = if wait {
        saving.done.recv().ok()
    } else {
        match saving.done.try_recv() {
            Ok(outcome) => Some(outcome),
            Err(std::sync::mpsc::TryRecvError::Empty) => return,
            Err(std::sync::mpsc::TryRecvError::Disconnected) => None,
        }
    };
    let Some(Saving { command, adopt_save, on_success, .. }) = app.saving.take() else { return };
    let r = match outcome {
        Some((copy, Ok(v))) => {
            if adopt_save {
                app.session.adopt_save(&copy);
            }
            Ok(v)
        }
        Some((_, Err(e))) => Err(e),
        None => Err(format!("{command}: the save stopped")),
    };
    app.finish_command(command, &r);
    if let (Some(on_success), Ok(value)) = (on_success, r.as_ref()) {
        on_success(app, value);
    }
    if adopt_save {
        crate::documents::saved_for_close(app);
    }
}

#[cfg(test)]
mod tests {
    use serde_json::json;

    /// #3044: Save writes on a worker while the window keeps drawing; the design waits (commands
    /// are refused) until the save is taken up, which leaves it saved under its file, in the
    /// recent files, with the command echoed, as a save run here does.
    #[test]
    fn save_writes_on_a_worker_and_is_taken_up_like_a_save_here() {
        let mut h = crate::scenario::Harness::new();
        h.app.run("solid.box", json!({"length": 10, "width": 10, "height": 10})).unwrap();
        let path = h.dir.join("Plate.solidcraft");
        let caller_thread = std::thread::current().id();
        let (provider_started_tx, provider_started_rx) = std::sync::mpsc::channel();
        let (release_provider_tx, release_provider_rx) = std::sync::mpsc::channel();
        let provider_release = std::sync::Mutex::new(release_provider_rx);
        let persisted_on = std::sync::Arc::new(std::sync::Mutex::new(None));
        let persisted_on_worker = persisted_on.clone();
        h.app.services.persist_document_save = Some(std::sync::Arc::new(move |_| {
            provider_started_tx.send(()).map_err(|error| error.to_string())?;
            provider_release.lock().map_err(|error| error.to_string())?.recv().map_err(|error| error.to_string())?;
            *persisted_on_worker.lock().map_err(|error| error.to_string())? = Some(std::thread::current().id());
            Ok(())
        }));
        assert!(super::start(&mut h.app, "file.save_as", json!({"path": path.to_string_lossy()})), "under way on a worker");
        assert!(h.app.file_picker_busy() && h.app.run("solid.sphere", json!({"diameter": 4})).is_err(), "the design waits for it");
        provider_started_rx.recv_timeout(std::time::Duration::from_secs(2)).expect("provider persistence starts on the save worker");
        h.frame();
        assert!(h.app.saving.is_some(), "a slow Android document provider must not block the UI frame");
        release_provider_tx.send(()).expect("release the simulated provider");
        h.settle();
        assert!(h.app.saving.is_none() && !h.app.session.is_dirty());
        assert_ne!(persisted_on.lock().unwrap().as_ref(), Some(&caller_thread), "provider write-back must not run on the window thread");
        assert_eq!(h.app.session.path.as_deref(), path.to_str());
        assert_eq!(h.app.session.doc.name, "Plate");
        assert!(h.app.home.recent.iter().any(|r| r.as_str() == path.to_str().unwrap()), "{:?}", h.app.home.recent);
        assert!(h.app.session.log.iter().any(|l| l.contains("Save As")), "{:?}", h.app.session.log);
        // A failing save reports why, and leaves the changes unsaved.
        h.app.run("solid.sphere", json!({"diameter": 4})).unwrap();
        let bad = h.dir.join("missing").join("x.solidcraft");
        super::start(&mut h.app, "file.save_as", json!({"path": bad.to_string_lossy()}));
        super::tick(&mut h.app, true);
        assert!(h.app.session.is_dirty() && h.app.status.as_ref().is_some_and(|(_, _, error)| *error));
    }

    #[test]
    fn mesh_export_persists_on_worker_without_marking_the_design_saved() {
        let mut h = crate::scenario::Harness::new();
        h.app.run("solid.box", json!({"length": 10, "width": 10, "height": 10})).unwrap();
        assert!(h.app.session.is_dirty());
        let path = h.dir.join("Block.stl");
        let caller_thread = std::thread::current().id();
        let (provider_started_tx, provider_started_rx) = std::sync::mpsc::channel();
        let (release_provider_tx, release_provider_rx) = std::sync::mpsc::channel();
        let provider_release = std::sync::Mutex::new(release_provider_rx);
        let persisted_on = std::sync::Arc::new(std::sync::Mutex::new(None));
        let persisted_on_worker = persisted_on.clone();
        h.app.services.persist_document_save = Some(std::sync::Arc::new(move |_| {
            provider_started_tx.send(()).map_err(|error| error.to_string())?;
            provider_release.lock().map_err(|error| error.to_string())?.recv().map_err(|error| error.to_string())?;
            *persisted_on_worker.lock().map_err(|error| error.to_string())? = Some(std::thread::current().id());
            Ok(())
        }));

        assert!(super::start_export(&mut h.app, "file.save_mesh", json!({"path": path.to_string_lossy(), "format": "stl"}), None,));
        provider_started_rx.recv_timeout(std::time::Duration::from_secs(2)).expect("mesh provider write starts on the worker");
        h.frame();
        assert!(h.app.saving.is_some(), "a slow mesh provider must not block the UI frame");
        assert!(h.app.session.is_dirty(), "exporting a mesh must not mark its source design as saved");
        release_provider_tx.send(()).expect("release the simulated provider");
        h.settle();

        assert!(path.is_file());
        assert!(h.app.session.is_dirty(), "finishing a mesh export must leave the source design dirty");
        assert_ne!(persisted_on.lock().unwrap().as_ref(), Some(&caller_thread));
    }

    #[test]
    fn cad_export_persists_on_worker_without_marking_the_design_saved() {
        let mut h = crate::scenario::Harness::new();
        h.app.run("solid.box", json!({"length": 10, "width": 10, "height": 10})).unwrap();
        assert!(h.app.session.is_dirty());
        let path = h.dir.join("Block.step");
        let caller_thread = std::thread::current().id();
        let (provider_started_tx, provider_started_rx) = std::sync::mpsc::channel();
        let (release_provider_tx, release_provider_rx) = std::sync::mpsc::channel();
        let provider_release = std::sync::Mutex::new(release_provider_rx);
        let persisted_on = std::sync::Arc::new(std::sync::Mutex::new(None));
        let persisted_on_worker = persisted_on.clone();
        h.app.services.persist_document_save = Some(std::sync::Arc::new(move |_| {
            provider_started_tx.send(()).map_err(|error| error.to_string())?;
            provider_release.lock().map_err(|error| error.to_string())?.recv().map_err(|error| error.to_string())?;
            *persisted_on_worker.lock().map_err(|error| error.to_string())? = Some(std::thread::current().id());
            Ok(())
        }));

        assert!(super::start_export(&mut h.app, "file.export", json!({"path": path.to_string_lossy()}), None));
        provider_started_rx.recv_timeout(std::time::Duration::from_secs(2)).expect("CAD export provider write starts on the worker");
        h.frame();
        assert!(h.app.saving.is_some(), "a slow CAD export provider must not block the UI frame");
        assert!(h.app.session.is_dirty(), "exporting CAD data must not mark the design as saved");
        release_provider_tx.send(()).expect("release the simulated provider");
        h.settle();

        assert!(path.is_file() && std::fs::metadata(&path).unwrap().len() > 0);
        assert!(h.app.session.is_dirty(), "finishing a CAD export must leave the source design dirty");
        assert_ne!(persisted_on.lock().unwrap().as_ref(), Some(&caller_thread));
    }
}
