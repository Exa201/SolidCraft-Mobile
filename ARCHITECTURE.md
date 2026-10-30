# SolidCraft Mobile Architecture

## Current state

SolidCraft Mobile packages the existing SolidCraft Rust CAD engine and egui interface in a native Android application. Kotlin is used for Android platform APIs and lifecycle integration; Rust remains the owner of CAD documents, commands, geometry, undo/recovery, and rendering. The Android application targets API 37, supports API 28 and later, and packages separate `arm64-v8a` and `x86_64` APKs. The x86_64 Android build selects GLES to avoid the API 36 emulator's crashing gfxstream Vulkan path; a local API 36 x86_64 run rendered the UI and passed lifecycle instrumentation. Exact-commit CI and Xiaomi/physical ARM64 verification remain separate; see `TEST_REPORT.md`.

```text
Android Activity (Kotlin / GameActivity)
  ├─ Android lifecycle, immersive window, text input
  ├─ Storage Access Framework and ContentResolver
  ├─ FileProvider and Android app chooser for exported meshes
  └─ JNI callbacks for document picker, write-back and mesh sharing
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

Mesh exports stay in the app's private cache until the user selects an Android app. Rust writes STL or 3MF to a unique cache file; Kotlin checks that it is inside the dedicated share folder and returns a read-only `FileProvider` URI through `ACTION_SEND`. The chooser grants read access to the receiving app. Old exports are pruned after seven days. Desktop slicer detection and process launching remain separate and are not available through Android's app chooser.

The engine accepts filesystem paths, so SAF documents are copied into durable app-private storage. Open selections are staged before the Rust file command receives the path. Save selections retain a URI-to-staged-path mapping; Rust saves to the staged file and the Kotlin bridge writes the completed file back through `ContentResolver`. Persistable grants are retained when the provider supports them. No broad storage permission is requested. The staged workspace is not the user-visible source of truth: provider write failures are returned to the save workflow.

For glTF documents with external relative buffers, Kotlin parses the glTF JSON or GLB v2 JSON chunk, rejects unsafe paths, and asks the user to select the buffer folder. Only referenced files are copied into the staged document directory, with traversal and size limits. GLB integer fields are little-endian. File-picker results and cancellation are serialized in a small request registry; an active picker keeps its request-code association until Android returns, so a late result cannot be mistaken for a newer request.

External glTF buffers use the same fsync-and-atomic-replace staging path as opened CAD documents. A provider read failure or a buffer that exceeds the remaining 1 GiB import budget therefore leaves any previous staged buffer intact; no partially copied buffer is exposed to the Rust importer.

The Rust Android entry points eframe persistence and the shared recovery service at app-private paths. Preferences and recent documents are saved by eframe; the recovery service uses SolidCraft's existing autosave interval and document recovery logic. The Android host uses a cached JNI global class reference created on the UI thread because class lookup from a native worker thread failed in device testing.

## Input, viewport and rendering

Camera navigation lives in `crates/ui-egui/src/navigation.rs`. The shared event layer maps multi-touch gestures to pan, pinch zoom and orbit, tracks touch ownership until release, and suppresses emulated primary-button input during navigation. Viewport touch streams are distinguished from touches outside the viewport. Touch interaction expands screen-space hit slop for sketch points and curves, vertices, edges and axes; mouse selection keeps its precise thresholds. The viewport also supports long-press context actions.

Host integration tests exercise egui touch events through camera navigation and viewport selection. The complete egui crate suite has passed on the current working tree before the final full-workspace CI run. Small-target accuracy and gesture arbitration have not yet been verified on an Android device. Physical keyboard, mouse and stylus behavior also need device-level coverage.

The Rust UI is rendered through eframe/wgpu on GameActivity. Android startup passes eframe's wgpu render state to `SolveApp::set_wgpu` and installs the shared picture renderer; omitting this setup left the viewport blank even though CAD commands completed. x86_64 Android builds use GLES because the API 36 emulator's gfxstream Vulkan implementation reproducibly crashes in `vkQueueSubmit`; ARM64 retains wgpu's normal Vulkan-first selection. A local API 36 x86_64 APK displayed the model workspace and opened the Create Sketch dialog after touch input. GLES lacks the independent blending used by weighted order-independent transparency, so transparent surfaces use the existing sorted fallback. Android lifecycle callbacks and eframe persistence are wired, and the app has launched on an API 36 emulator and a physical Android 16 device. Pause/resume, surface recreation, GPU context loss, forced-crash recovery, and memory-pressure behavior are not yet fully tested on Android.

## Responsive presentation

The existing Rust UI switches the command ribbon to compact icon presentation below 600 egui points. The floating Browser starts as a compact 196-point header at phone widths and expands by touch without changing the desktop fold preference. Targeted host tests and API 36 portrait/landscape UI checks cover these changes. Other panels and dialogs still need systematic phone and tablet review; the shared desktop UI is not yet fully touch-optimized.

## Tests and evidence

- `FEATURE_PARITY.md` inventories 405 registered desktop commands plus one command marked planned by the original source. Shared Rust implementation is not treated as proof of Android device behavior.
- Android Kotlin unit tests cover CAD MIME mappings, SAF filter behavior, safe relative glTF paths, sidecar discovery, and GLB little-endian JSON extraction.
- The API 36 emulator has verified SAF Save As to Downloads, process restart, opening through DocumentsUI, Open Recent persistence, and overwriting the same SAF document with an added body before reopening; asynchronous DXF export through Downloads was also verified in an earlier emulator pass.
- Rust host tests cover navigation, touch ownership and expanded selection hit slop. They do not prove physical-device touch precision.
- The last full local Rust suite passed on the source state recorded in `TEST_REPORT.md`; the current mesh-sharing path has its own targeted Rust test. Android JVM tests pass, and a targeted API 36 instrumentation test verifies that FileProvider exposes the dedicated mesh cache and rejects other cache files. Opening the system chooser and receiving the export in a slicer remain unverified.
- Android APK workflow `40965988223` passed for commit `6daae56f`, including host tests, native APK build, split-ABI/signature verification and artifact upload. It predates the uncommitted Android mesh-sharing work. The Xiaomi preview remains a debug-signed test build; production signing and runtime verification of the current change remain outstanding.

## Android UI strategy and remaining work

Keep CAD behavior and shared command semantics in Rust, and use Kotlin for Android APIs, lifecycle, SAF, and native host callbacks. Continue adding responsive egui layouts instead of a second Compose implementation of the same command system. Compose overlays should be considered only if they can coexist with GameActivity rendering without creating duplicate CAD behavior.

The application remains a development preview. Full command-by-command Android usability, provider error and permission cases, multi-provider SAF tests, device CAD workflows, lifecycle recovery, small-screen dialogs, tablet adaptation, performance on complex models, production release signing, and exact-commit CI for the current mesh-sharing change remain outstanding. See `FEATURE_PARITY.md` and `TEST_REPORT.md` for current coverage and gaps.
