# SolidCraft Mobile

SolidCraft Mobile packages the existing SolidCraft Rust engine and egui CAD interface as a native Android application. The Android-specific code is limited to a Kotlin `GameActivity` host and the Android build configuration; sketches, the feature timeline, geometry and rendering are shared with SolidCraft.

## Android target

- Compile and target SDK: Android API 37
- Minimum Android version: API 28
- Native ABIs: `arm64-v8a` (physical ARM64 devices) and `x86_64` (Android emulators)
- Rust toolchain: stable, with `aarch64-linux-android` and `x86_64-linux-android`
- Android host: GameActivity 4.4.0, which provides soft-keyboard input

## Build

Install Android SDK platform 37, Android NDK 30, JDK 17 or newer, Rust stable, the `aarch64-linux-android` Rust target and `cargo-ndk`. Then build from this repository root:

```powershell
.\gradlew.bat :app:assembleDebug
```

The Gradle build compiles `apps/solidcraft-android` for both Android ABIs and packages separate, stripped APKs so devices do not download the other architecture's CAD engine. Install the ARM64 debug APK on a phone with `adb install app/build/outputs/apk/debug/app-arm64-v8a-debug.apk`; install `app-x86_64-debug.apk` on an x86_64 emulator.

The Android host connects the shared CAD interface to Android's Storage Access Framework. Open and Save through Downloads have been exercised on a physical Android 16 device. Save As, reopen after process restart, and asynchronous DXF export have passed a Downloads-provider workflow on an API 36 emulator. Android Save creates a streaming local backup before provider writes, verifies the saved bytes, and attempts rollback after write or verification errors; JVM tests cover partial writes, rollback, retained-backup recovery and verification mismatch. A physical Android-16 lifecycle instrumentation test passes. Provider failure/rollback behavior still needs testing against additional real document providers. Opening glTF files with relative sidecar buffers stages the selected buffers after the user selects their source folder; this workflow was exercised through DocumentsUI on an API-36 emulator by selecting a project subfolder and importing a body. The Downloads root itself is blocked by Android's folder picker. The viewport has touch-aware pan, pinch and orbit handling plus a long-press context menu verified on both emulator and physical device. Shared Rust navigation tests cover two-finger pan/pinch/twist and three-finger orbit, but a real Android multi-touch camera gesture remains unverified. Touches outside the CAD viewport do not widen selection hit slop; viewport touch and release frames widen point, curve, edge, vertex and axis targets while mouse frames keep the original precise thresholds. Device-level selection accuracy remains unverified. An API-36 emulator lifecycle test covers pause/resume and rotation; physical Android 16 rotation and GPU context recovery remain unverified.

The application is still a development preview, not a feature-complete Android release. Most registered CAD commands share the Rust implementation but have not been individually verified on Android. Provider error/permission cases, GPU context recovery and device-level selection accuracy remain open. Android lifecycle instrumentation covers system-bar restoration on a physical Android-16 device and pause/resume plus rotation on an API-36 emulator; there is not yet an automated Android CAD-touch or SAF UI suite. Both ABI variants cross-compile, but the local x86_64 Android 16 emulator still fails its runtime smoke test: `MainActivity` becomes unresponsive during native renderer startup, and a GLES-only diagnostic build did not recover it. No physical x86_64 device has been tested; do not treat x86_64 runtime support as verified. See [FEATURE_PARITY.md](FEATURE_PARITY.md) and [TEST_REPORT.md](TEST_REPORT.md) for the verified state and gaps.
