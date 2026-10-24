#![cfg(target_os = "android")]
#![deny(clippy::unwrap_used, clippy::expect_used, clippy::panic, clippy::unimplemented, clippy::todo, clippy::unreachable)]

use android_activity::AndroidApp;
use eframe::egui;
use solidcraft_engine::Session;
use solidcraft_ui_egui::SolveApp;

mod storage;

struct MobileApp {
    solve: SolveApp,
}

impl eframe::App for MobileApp {
    fn logic(&mut self, ctx: &egui::Context, _frame: &mut eframe::Frame) {
        self.solve.logic(ctx);
    }

    fn ui(&mut self, ui: &mut egui::Ui, _frame: &mut eframe::Frame) {
        self.solve.ui(ui);
    }

    fn save(&mut self, storage: &mut dyn eframe::Storage) {
        storage.set_string(solidcraft_ui_egui::prefs::STORAGE_KEY, self.solve.prefs());
    }

    fn auto_save_interval(&self) -> std::time::Duration {
        // Android may reclaim the activity without delivering a graceful process exit. Persist
        // recent documents and preferences frequently so a normal pause/restart loses little.
        std::time::Duration::from_secs(2)
    }

    fn on_exit(&mut self) {
        solidcraft_ui_egui::recovery_ui::close(&mut self.solve);
    }
}

// android-activity locates this Rust-ABI entry point by its exported symbol name.
#[allow(unsafe_code)]
#[unsafe(no_mangle)]
fn android_main(app: AndroidApp) {
    android_logger::init_once(android_logger::Config::default().with_max_level(log::LevelFilter::Info));

    let data_directory = app.internal_data_path();
    let persistence_path = data_directory.as_ref().map(|directory| directory.join("solidcraft-mobile.ron"));
    let recovery_directory = data_directory.map(|directory| directory.join("recovery"));
    let options = eframe::NativeOptions {
        android_app: Some(app),
        persistence_path,
        depth_buffer: 24,
        stencil_buffer: 8,
        ..Default::default()
    };
    let result = eframe::run_native(
        "SolidCraft",
        options,
        Box::new(|creation| {
            solidcraft_ui_egui::fonts::setup_bundled_fonts(&creation.egui_ctx);
            solidcraft_ui_egui::fonts::load_system_fonts_in_background(&creation.egui_ctx);
            let mut solve = SolveApp::new(Session::default(), storage::services());
            if let Some(render_state) = &creation.wgpu_render_state {
                solve.set_wgpu(render_state, 24, 8, render_state.msaa_samples);
                solidcraft_ui_egui::gpu::install_picture_renderer(render_state);
            } else {
                log::error!("SolidCraft Android started without a wgpu render state; the 3D viewport will not render");
            }
            if let Some(prefs) = creation.storage.and_then(|store| store.get_string(solidcraft_ui_egui::prefs::STORAGE_KEY)) {
                solve.load_prefs(&prefs);
            }
            if let Some(directory) = recovery_directory.as_deref() {
                let minutes = solve.autosave_minutes;
                let interval = if minutes > 0.0 {
                    std::time::Duration::from_secs_f64((minutes * 60.0).min(1e9))
                } else {
                    std::time::Duration::from_secs_f64(1e9)
                };
                solidcraft_ui_egui::recovery_ui::start(&mut solve, directory, interval);
            }
            Ok(Box::new(MobileApp { solve }))
        }),
    );
    if let Err(error) = result {
        log::error!("SolidCraft Android stopped: {error}");
    }
}
