# SolidCraft Mobile Release Notes

SolidCraft Mobile is an early Android development preview of the shared SolidCraft Rust CAD engine and egui interface. It is intended for hands-on testing and feedback, not as a feature-complete or production-signed CAD release.

## v0.1.0 Android Preview 7

- Published as [GitHub prerelease `v0.1.0-android-preview.7`](https://github.com/Exa201/SolidCraft-Mobile/releases/tag/v0.1.0-android-preview.7) from source commit `ab7c1b03f3d11833e6a11917d785280301bef0d1`.
- GitHub Android workflow [41220939709](https://github.com/Exa201/SolidCraft-Mobile/actions/runs/41220939709) passed Kotlin/JVM tests, Android APK build, ARM64/x86_64 split and package verification, signature verification, and artifact upload. 43 JVM tests passed in the fresh local test run for this source.
- Attached debug-signed APKs for arm64-v8a (93,000,023 bytes, SHA-256 `1cc38db6adb621cabbbdbbee5110aa1ad6fa49f0caa7a946a613e8ce1e0cc41d`) and x86_64 (109,812,420 bytes, SHA-256 `deec1e37c2b456f6287a725d1da4b6398bb503525120c3dd7e57755b4104e026`); see the release's `SHA256SUMS.txt`.
- Installed the exact CI x86_64 APK on API-35 and API-36 emulators and confirmed app launch. Successful SAF open of cached and cache-free box files reproduced a black screen with GLES framebuffer errors on both API versions; API 36 also reproduced it after Save As. Picker cancellation and creating a box directly in the UI did not. One earlier API-35 open succeeded but the result was not repeatable. The cause and Xiaomi/ARM64 behavior remain unknown.
- The APK remains a development preview without full desktop parity. On a later preview, Android may require uninstalling the debug package if its ephemeral signing key changes; this removes that installation's local data.

## v0.1.0 Android Preview 6

- Built by Android APK workflow run [41211769543](https://github.com/Exa201/SolidCraft-Mobile/actions/runs/41211769543) from source commit `96a223144f555d15625c633a2a9882889a3a3c5b`.
- Includes arm64-v8a and x86_64 CI APKs. Compile/target SDK 37; minimum Android API 28.
- CI completed `:app:testDebugUnitTest :app:assembleDebug`, built both Rust Android ABIs, and verified the split APK package IDs, ABI contents, and signatures.
- APKs are debug-signed. The package ID is `org.solidcraft.mobile.debug` so this preview can coexist with the older `org.solidcraft.mobile` previews. A later CI build can have a different debug signing key; if Android refuses an update, uninstall this `.debug` app before installing the next build (this removes its local data).
- ARM64 APK SHA-256: `780E301F1CC3354EEC5FC5450DB88785D07DCD039A74685A9BEBC703CDF14417`.
- The separate x86_64 emulator APK is also attached for testing.

### Included Android work

- SolidCraft's shared Rust geometry/feature engine and egui CAD interface hosted through Android GameActivity.
- Android Storage Access Framework integration for opening and saving documents, private staging, and provider write-back; recent files and recovery infrastructure.
- Touch-oriented CAD navigation work and Android lifecycle handling.

### Known limitations

- This is not full desktop feature parity. Some panels, dialogs, and workflows have not received mobile-specific interaction design, and device UI coverage is incomplete.
- On an Android 16 x86_64 emulator, loading nonempty sketch and solid projects has reproduced a black display with GLES framebuffer errors (`depth attachment not complete: 0x8d56`, GL error `0x506`). It reproduced with both the host NVIDIA renderer and SwiftShader. The cause and behavior on physical ARM64 devices are not established. Please save work frequently and report whether this occurs on the Xiaomi.
- No physical Xiaomi/ARM64 test was available during this build. The GitHub CI build and its host/unit tests do not substitute for that device test.
- This APK is debug-signed, not a production release. Install it only if you are comfortable sideloading a test build.

See [TEST_REPORT.md](TEST_REPORT.md) for the exact test scope and device observations.
