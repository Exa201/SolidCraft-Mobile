# SolidCraft Mobile

SolidCraft Mobile packages the existing SolidCraft Rust engine and egui CAD interface as a native Android application. The Android-specific code is limited to a Kotlin `GameActivity` host and the Android build configuration; sketches, the feature timeline, geometry and rendering are shared with SolidCraft.

## Android target

- Compile and target SDK: Android API 37
- Minimum Android version: API 28
- Native ABI: `arm64-v8a`
- Rust toolchain: stable, with `aarch64-linux-android`
- Android host: GameActivity 4.4.0, which provides soft-keyboard input

## Build

Install Android SDK platform 37, Android NDK 30, JDK 17 or newer, Rust stable, the `aarch64-linux-android` Rust target and `cargo-ndk`. Then build from this repository root:

```powershell
.\gradlew.bat :app:assembleDebug
```

The Gradle build compiles `apps/solidcraft-android` for Android and packages a stripped native library into the APK. Install the debug APK with Android Studio or `adb install app/build/outputs/apk/debug/app-debug.apk`.

The Android host connects the shared CAD interface to Android's Storage Access Framework. Open and Save through Downloads have been exercised on a physical Android 16 device. Save As, reopen after process restart, and asynchronous DXF export have passed a Downloads-provider workflow on an API 36 emulator. Android Save now creates a streaming local backup before provider writes, verifies the saved bytes, and attempts rollback after write or verification errors; five JVM tests cover partial writes, rollback, retained-backup recovery and verification mismatch. The updated APK builds and installs on the physical Android 16 device, and its lifecycle instrumentation test passes there. A new post-test visual smoke check was inconclusive because the device returned to an unrelated app; provider failure/rollback behavior still needs testing against real document providers. Opening glTF files with relative sidecar buffers stages the selected buffers after the user selects their source folder; the provider-backed sidecar flow still needs an Android device test. The current viewport has touch-aware pan, pinch, and orbit handling plus a long-press context menu verified on both emulator and physical device. Touches outside the CAD viewport do not widen selection hit slop; viewport touch and release frames widen point, curve, edge, vertex, and axis targets while mouse frames keep the original precise thresholds. The boundary and selection behavior has targeted host integration coverage and the full egui UI suite passes (1,861 passed, 6 ignored); device-level selection accuracy remains unverified. The physical Android 16 device also verified background/foreground resume and system-bar restoration, but not rotation or GPU context recovery.

The application is still a development preview, not a feature-complete Android release. Most registered CAD commands share the Rust implementation but have not been individually verified on Android. Recent-file recovery, provider error/permission cases, rotation testing, GPU context recovery, and device-level selection accuracy remain open. A physical Android-16 instrumentation test now checks that system bars stay hidden after resume; there is not yet an automated CAD touch or SAF UI suite. See [FEATURE_PARITY.md](FEATURE_PARITY.md) and [TEST_REPORT.md](TEST_REPORT.md) for the verified state and gaps.
