# SolidCraft Mobile Release Notes

SolidCraft Mobile is an early Android development preview of the shared SolidCraft Rust CAD engine and egui interface. It is intended for hands-on testing and feedback, not as a feature-complete or production-signed CAD release.

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
