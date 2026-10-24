# SolidCraft Mobile Architecture

## Current state

SolidCraft Mobile packages the existing SolidCraft Rust CAD engine and egui interface in a native Android application. Kotlin is used for Android platform APIs and lifecycle integration; Rust remains the owner of CAD documents, commands, geometry, undo/recovery, and rendering. The Android application targets API 37, supports API 28 and later, and currently packages the `arm64-v8a` ABI.

```text
Android Activity (Kotlin / GameActivity)
  ├─ Android lifecycle, immersive window, text input
  ├─ Storage Access Framework and ContentResolver
  └─ JNI callbacks for document picker and write-back
          ↓ GameActivity native loading / JNI
Rust Android entry (apps/solidcraft-android)
  ├─ eframe + egui
  └─ shared SolveApp and Session
          ↓
CAD UI / command API (crates/ui-egui)
          ↓
Engine, document, IO, sketch, kernel and geometry crates
          ↓
Shared wgpu rendering
```

## Platform boundary and data flow

`MainActivity` is a `GameActivity` host. It hides system bars while retaining transient gesture access, registers itself for JNI callbacks, and starts SAF intents on the Android UI thread. It does not implement CAD commands or duplicate the Rust interface in Compose.

The engine accepts filesystem paths, so SAF documents are copied into durable app-private storage. Open selections are staged before the Rust file command receives the path. Save selections retain a URI-to-staged-path mapping; Rust saves to the staged file and the Kotlin bridge writes the completed file back through `ContentResolver`. Persistable grants are retained when the provider supports them. No broad storage permission is requested. The staged workspace is not the user-visible source of truth: provider write failures are returned to the save workflow.

For glTF documents with external relative buffers, Kotlin parses the glTF JSON or GLB v2 JSON chunk, rejects unsafe paths, and asks the user to select the buffer folder. Only referenced files are copied into the staged document directory, with traversal and size limits. GLB integer fields are little-endian. File-picker cancellation resolves as no selection.

The Rust Android entry points eframe persistence and the shared recovery service at app-private paths. Preferences and recent documents are saved by eframe; the recovery service uses SolidCraft's existing autosave interval and document recovery logic. The Android host uses a cached JNI global class reference created on the UI thread because class lookup from a native worker thread failed in device testing.

## Input, viewport and rendering

Camera navigation lives in `crates/ui-egui/src/navigation.rs`. The shared event layer maps multi-touch gestures to pan, pinch zoom and orbit, tracks touch ownership until release, and suppresses emulated primary-button input during navigation. Viewport touch streams are distinguished from touches outside the viewport. Touch interaction expands screen-space hit slop for sketch points and curves, vertices, edges and axes; mouse selection keeps its precise thresholds. The viewport also supports long-press context actions.

Host integration tests exercise egui touch events through camera navigation and viewport selection. The complete egui crate suite has passed on the current working tree before the final full-workspace CI run. Small-target accuracy and gesture arbitration have not yet been verified on an Android device. Physical keyboard, mouse and stylus behavior also need device-level coverage.

The Rust UI is rendered through eframe/wgpu on GameActivity. Android lifecycle callbacks and eframe persistence are wired, and the app has launched on an API 36 emulator and a physical Android 16 device. Pause/resume, rotation, surface recreation, GPU context loss, forced-crash recovery, and memory-pressure behavior are not yet fully tested on Android.

## Responsive presentation

The existing Rust UI switches the command ribbon to compact icon presentation below 600 egui points. The floating Browser starts as a compact 196-point header at phone widths and expands by touch without changing the desktop fold preference. Targeted host tests and API 36 portrait/landscape UI checks cover these changes. Other panels and dialogs still need systematic phone and tablet review; the shared desktop UI is not yet fully touch-optimized.

## Tests and evidence

- `FEATURE_PARITY.md` inventories 405 registered desktop commands plus one command marked planned by the original source. Shared Rust implementation is not treated as proof of Android device behavior.
- Android Kotlin unit tests cover CAD MIME mappings, SAF filter behavior, safe relative glTF paths, sidecar discovery, and GLB little-endian JSON extraction.
- The API 36 emulator has verified SAF open, Save As, reopen after process restart, and asynchronous DXF export through Downloads.
- Rust host tests cover navigation, touch ownership and expanded selection hit slop. They do not prove physical-device touch precision.
- A full `cargo xtask ci` run for the exact working tree is in progress; `TEST_REPORT.md` records completed subtests and must be updated with its final result.
- The current debug APK has been built and inspected but the latest APK has not yet been installed and re-tested on a device. Production signing is not configured.

## Android UI strategy and remaining work

Keep CAD behavior and shared command semantics in Rust, and use Kotlin for Android APIs, lifecycle, SAF, and native host callbacks. Continue adding responsive egui layouts instead of a second Compose implementation of the same command system. Compose overlays should be considered only if they can coexist with GameActivity rendering without creating duplicate CAD behavior.

The application remains a development preview. Full command-by-command Android usability, provider error and permission cases, multi-provider SAF tests, device CAD workflows, lifecycle recovery, small-screen dialogs, tablet adaptation, performance on complex models, release signing, exact-commit GitHub CI, and a verified prerelease remain outstanding. See `FEATURE_PARITY.md` and `TEST_REPORT.md` for current coverage and gaps.
