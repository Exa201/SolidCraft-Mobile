#![cfg(target_os = "android")]
#![deny(clippy::unwrap_used, clippy::expect_used, clippy::panic, clippy::unimplemented, clippy::todo, clippy::unreachable)]

use android_activity::AndroidApp;
use eframe::egui;
use solidcraft_engine::Session;
use solidcraft_ui_egui::{Services, SolveApp};

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
}

// android-activity locates this Rust-ABI entry point by its exported symbol name.
#[allow(unsafe_code)]
#[unsafe(no_mangle)]
fn android_main(app: AndroidApp) {
    android_logger::init_once(android_logger::Config::default().with_max_level(log::LevelFilter::Info));

    let options = eframe::NativeOptions { android_app: Some(app), ..Default::default() };
    let result = eframe::run_native(
        "SolidCraft",
        options,
        Box::new(|_creation| Ok(Box::new(MobileApp { solve: SolveApp::new(Session::default(), Services::default()) }))),
    );
    if let Err(error) = result {
        log::error!("SolidCraft Android stopped: {error}");
    }
}
