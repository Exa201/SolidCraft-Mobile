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
    done: std::sync::mpsc::Receiver<(Session, Result<Value, String>)>,
}

/// Run `file.save` or `file.save_as` with `params`. On the desktop it runs on a worker and is
/// taken up by [`tick`] (true: it is under way); in the browser, or when no thread can start, it
/// runs here and is finished on return (false).
pub fn start(app: &mut SolveApp, command: &'static str, params: Value) -> bool {
    #[cfg(not(target_arch = "wasm32"))]
    if app.saving.is_none() && app.file_picker.is_none() && !app.file_load.busy() {
        let mut copy = app.session.scratch();
        // The copy has no file: the command saves to this session's (and names the design after it).
        copy.path.clone_from(&app.session.path);
        let (tx, rx) = std::sync::mpsc::channel();
        let given = params.clone();
        let persist_document_save = app.services.persist_document_save.clone();
        let job = std::thread::Builder::new().name("save".into()).spawn(move || {
            let r = copy.execute(command, &given).map_err(|e| e.to_string()).and_then(|value| {
                if let (Some(persist), Some(path)) = (&persist_document_save, copy.path.as_deref()) {
                    persist(path)?;
                }
                Ok(value)
            });
            let _ = tx.send((copy, r));
        });
        if job.is_ok() {
            app.saving = Some(Saving { command, done: rx });
            return true;
        }
        // No thread to write on: save here, as before.
    }
    let _ = app.run(command, params);
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
    let Some(Saving { command, .. }) = app.saving.take() else { return };
    let r = match outcome {
        Some((copy, Ok(v))) => {
            app.session.adopt_save(&copy);
            Ok(v)
        }
        Some((_, Err(e))) => Err(e),
        None => Err(format!("{command}: the save stopped")),
    };
    app.finish_command(command, &r);
    crate::documents::saved_for_close(app);
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
        assert!(super::start(&mut h.app, "file.save_as", json!({"path": path.to_string_lossy()})), "under way on a worker");
        assert!(h.app.file_picker_busy() && h.app.run("solid.sphere", json!({"diameter": 4})).is_err(), "the design waits for it");
        h.frame();
        h.settle();
        assert!(h.app.saving.is_none() && !h.app.session.is_dirty());
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
}
