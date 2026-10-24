# SolidCraft Mobile Release Notes

## Development preview status

SolidCraft Mobile currently starts the shared SolidCraft Rust engine and CAD interface on Android. This is an early development preview, not a feature-complete mobile CAD release.

### Present

- Rust geometry/feature engine and egui CAD interface are reused from SolidCraft.
- Android GameActivity host with arm64-v8a native library.
- Compile/target SDK 37, minimum API 28.
- Android Storage Access Framework bridge for open/create-document requests, private staging, and URI writeback is implemented. Downloads provider open, Save As, reopen after process restart, and asynchronous DXF export passed on an API 36 emulator.
- SAF writeback now fsyncs a local rollback copy and a hash marker before touching the provider; later saves can distinguish an already verified write from an interrupted one. JVM regression tests cover stale backup cleanup and unreadable-provider recovery. Provider-failure behavior has not yet been verified on device.
- Android stores UI preferences and the Recent list in app-private eframe storage. SAF working copies use durable URI-stable paths and can be reopened from Recent after a process restart.
- The shared unsaved-design recovery service now starts in an Android-private recovery directory using the saved autosave interval; forced-crash recovery on device still needs verification.
- The CAD viewport now hides Android system bars so the top toolbar remains touchable; temporary system bars can still be revealed with an edge swipe.
- On narrow portrait windows, the floating Browser starts as a compact header to preserve the viewport and expands by touch to the full model tree. The shared Browser test group passes 12/12; the behavior was also checked on an API 36 emulator.
- The latest debug APK installed and launched on both an API 36 emulator and a physical Android 16 device; these are launch/display smoke checks, not complete CAD workflow validation.

### Not yet release-ready

- Additional-provider, failure, cancellation, lifecycle recovery, and broader Android device verification.
- Mobile-first responsive treatment for the remaining panels and dialogs; only the command ribbon and floating Browser have narrow-screen adaptations so far.
- Android-device gesture verification, precise small-element selection, stylus and mouse workflows. Two-finger mapping and viewport ownership pass a host egui integration test only.
- Forced-process-death autosave and recovery prompt workflow on a physical Android device.
- Automated Android UI/integration coverage and broader device verification.
- Successful CI APK artifact for the current PR commit.
- Signed release APK with reproducible artifact checksum.

Do not treat the existing `v0.1.0-android-preview.1` debug prerelease as completion of this work. A new prerelease must wait for verified release criteria.
