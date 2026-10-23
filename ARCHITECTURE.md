# SolidCraft Mobile Architecture

## Current state

The Android package is a Kotlin `GameActivity` host which loads `solidcraft_android`, a Rust `cdylib`. `apps/solidcraft-android/src/lib.rs` starts `eframe` with the shared `solidcraft_ui_egui::SolveApp` and `solidcraft_engine::Session`. Geometry, sketches, the parametric feature timeline, import/export code and most CAD UI are therefore shared with desktop. Android does not fork or reimplement the geometry engine.

## Layers

```text
Android Activity (Kotlin / GameActivity)
        ↓ GameActivity native loading
Rust Android entry (apps/solidcraft-android)
        ↓
eframe + egui UI (crates/ui-egui)
        ↓ command API
Engine / document / IO / sketch / kernel / geometry crates
        ↓
wgpu rendering (shared renderer)
```

The Android activity provides lifecycle, text input, native-library loading, immersive viewport handling, and a Storage Access Framework (SAF) bridge. `ACTION_OPEN_DOCUMENT` stages selected content in a durable app-private workspace using a stable URI-derived path; `ACTION_CREATE_DOCUMENT` stages output and retains a URI mapping so Rust file commands can write the result back through `ContentResolver`. Persistable URI grants are requested where the provider offers them. The durable path preserves SolidCraft's recent-file entries if Android clears its cache, and reopening the same provider URI refreshes the same working copy. An API-36 emulator test opened a SolidCraft design from Downloads, saved a new `.solidcraft` file, reopened it after force-stopping the app through the persisted Recent list, and exported a DXF through the asynchronous file worker. Eframe settings are stored in the app's internal data directory. The Rust recovery UI is also enabled against a private `recovery` directory and uses the saved autosave interval; its directory creation and shared recovery logic are covered, but forced-process-death recovery has not yet been exercised on device. The Android bridge uses a JNI global class reference created on the UI thread because class lookup from a native worker thread could not resolve app classes. No broad storage permission is requested. Android registers bundled font families and starts the shared system-font loader before constructing the app; without this registration the app panicked during startup on Android.

## Input and rendering

`crates/ui-egui/src/navigation.rs` owns shared camera navigation, including pinch and trackpad event handling. Two-finger input is mapped to pan, pinch zoom and twist orbit; three-finger drag maps to orbit. Multi-touch latches pointer ownership until every touch is released, suppressing the primary-button emulation during a navigation gesture. Pure mapping tests and a host integration test that feeds egui touch events through the viewport into camera movement pass. Android device gesture arbitration, small-target picking and stylus input still need device tests.

The Rust app runs through eframe/wgpu and GameActivity. Eframe persistence is pointed at the Android internal data directory; the app loads the shared `SolveApp` preferences and writes them at a two-second interval, including the recent-document list. The latest emulator run reopened a SAF document from that list after force-stopping and relaunching the process. The latest debug APK installed and remained top-resumed on both an API 36 emulator and the attached Android 16 HONOR device; sampled logs on both showed no fatal exception or Rust panic. Opening the system picker returned to the immersive CAD view on the emulator. Pause/resume, orientation changes, surface recreation and GPU context loss still need explicit lifecycle verification. The SAF round-trip remains emulator-only.

## File workflow target

The SAF bridge is present in `MainActivity.kt` and `apps/solidcraft-android/src/storage.rs`. It stages provider files because engine commands consume filesystem paths. Design save jobs and background DXF export write back only after the local file is complete; a failed provider write keeps the design dirty or reports a file-operation error. The Downloads provider has passed open, Save As, process restart/reopen, and asynchronous DXF export. Additional providers, overwrite/cancel/error cases, and lifecycle recovery still need automated or device tests. Cancellation currently resolves to no selection; provider failures are returned to the UI status/toast path.

## Android UI strategy

The existing project instruction keeps application behavior in Rust and makes the UI crate swappable. Preserve the Rust CAD UI and engine while adding an Android-specific responsive egui presentation and only use Kotlin for Android platform APIs, lifecycle, document-provider callbacks, and native host concerns. This avoids duplicating engine commands in a second UI stack. A Compose overlay may be evaluated only if it can coexist with the GameActivity render surface and does not create a second source of CAD command behavior.

The command ribbon switches to its icon-only presentation below 600 egui points and retains the saved label preference at tablet/desktop widths. The floating Browser also starts as a 196-point compact header below 600 points and expands by touch without changing the desktop fold preference. The Browser breakpoint has a unit test and phone-width emulator screenshot/tap verification. Other dialogs, panels and interaction targets still require mobile-specific work.

## Risks and decisions

- Existing desktop panels can consume too much of a phone viewport; a mobile layout mode is required.
- Direct imports/exports and auxiliary UI actions may depend on filesystem paths, native dialogs, external programs, keychains, or network services that are absent on Android.
- The current debug APK is large; use release stripping and ABI splits only after verifying the generated artifact and supported device targets.
- Full parity spans hundreds of registered commands. `FEATURE_PARITY.md` records each command and keeps shared-code status separate from Android verification.
