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

The first mobile milestone launches a new SolidCraft design in the shared CAD interface. Android document-provider integration, mobile-first panel layouts, and touch-specific CAD navigation are follow-on work; file open/save is not yet wired to Android's Storage Access Framework.
