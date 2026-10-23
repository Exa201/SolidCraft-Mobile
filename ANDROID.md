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

The Android host connects the shared CAD interface to Android's Storage Access Framework. Open, Save As, reopen after process restart, and asynchronous DXF export have passed a Downloads-provider workflow on an API 36 emulator. The current viewport has touch-aware pan, pinch, and orbit handling plus a long-press context menu; these features have unit/host integration coverage and an initial emulator check.

The application is still a development preview, not a feature-complete Android release. Most registered CAD commands share the Rust implementation but have not been individually verified on Android. Recent-file recovery, provider error/permission cases, lifecycle and rotation testing, fine-grained touch selection, and automated Android UI tests remain open. See [FEATURE_PARITY.md](FEATURE_PARITY.md) and [TEST_REPORT.md](TEST_REPORT.md) for the verified state and gaps.
