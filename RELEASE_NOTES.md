# SolidCraft Mobile Release Notes

SolidCraft Mobile is an early Android development preview of the shared SolidCraft Rust CAD engine and egui interface. It is intended for hands-on testing and feedback, not as a feature-complete or production-signed CAD release.

## v0.1.0 Android Preview 10

- Published as [GitHub prerelease `v0.1.0-android-preview.10`](https://github.com/Exa201/SolidCraft-Mobile/releases/tag/v0.1.0-android-preview.10) from source commit `233af14f2ab8bd2954ac9c0d5724eab1a28dfa79`.
- Android APK workflow [41241432049](https://github.com/Exa201/SolidCraft-Mobile/actions/runs/41241432049) passed Kotlin/JVM tests, ARM64 and x86_64 Rust builds, APK ABI/package verification, signature verification, and artifact upload. Docker workflow [41241432023](https://github.com/Exa201/SolidCraft-Mobile/actions/runs/41241432023) and packaging lint passed. A forced local rerun passed 48 Android JVM tests (0 failures/errors/skips).
- Includes the SAF mapping durability fix: Open and Save As synchronously persist the provider URI-to-working-copy mapping before the native engine receives the staged path.
- Includes debug-signed APKs for arm64-v8a (92,998,719 bytes, SHA-256 `a78406138ce36fbdd06116126c180f2a6de8afafdde60e0ed6a9c27696ea7dbb`) and x86_64 (109,818,340 bytes, SHA-256 `7a54ac8eadcdb5abac5ce8f87337aea723d73dd9ffc79e54fbeb37df45f603e7`); see the release's `SHA256SUMS.txt`.
- Minimum Android API 28; compile/target SDK 37. A Preview 10 API-36 x86_64 smoke test opened and saved a cache-free box through SAF, force-stopped/relaunched and reopened it from Open Recent, and used Save As to create a valid 8,347-byte CAD file in Downloads. The body stayed visible through a File-menu redraw and no matching FBO/fatal logs appeared. On a subsequent restart Open Recent omitted the Save As destination; recent-file registration needs investigation. Provider write-back after restart was not tested. Prior x86_64 emulator runs did show intermittent GLES viewport/framebuffer problems, so this single clean run does not resolve that issue. No physical Xiaomi/ARM64 run was performed; behavior on the user's Xiaomi remains unverified.
- This remains a development preview, not full desktop feature parity or a stable release. If Android refuses installation due to a different debug signing key, uninstall the previous `.debug` app first; uninstalling deletes its local data.

## v0.1.0 Android Preview 9

- Published as [GitHub prerelease `v0.1.0-android-preview.9`](https://github.com/Exa201/SolidCraft-Mobile/releases/tag/v0.1.0-android-preview.9) from source commit `395fe89310355b99fb22831fd6627cf3c4259620`; the GitHub release assets were replaced with the exact APKs uploaded by Android workflow [41236181932](https://github.com/Exa201/SolidCraft-Mobile/actions/runs/41236181932), which completed successfully.
- Includes arm64-v8a and x86_64 debug APKs (compile/target SDK 37, minimum Android API 28). The release includes `SHA256SUMS.txt`; ARM64 SHA-256 is `2ac31af9e7bbaf5e90b212fbc8df150f8720edb67af62df66e5d01d3b34054a4`.
- Includes the Android native event-loop destruction fix and SAF document write-back using process context. The release notes record 43 JVM tests and a successful SAF Activity-destruction emulator instrumentation test for that build.
- Treat this as a device-testing preview, not a stable or feature-complete release. GLES rendering failures have been observed on x86_64 Android emulators; Xiaomi/ARM64 rendering is not established. See [TEST_REPORT.md](TEST_REPORT.md) for later device observations and exact limitations.
- APKs are debug-signed. If Android rejects an update because the signing key or package ID differs, uninstall the previous preview before installing this one; uninstalling removes that app's local data.

## v0.1.0 Android Preview 7

- Published as [GitHub prerelease `v0.1.0-android-preview.7`](https://github.com/Exa201/SolidCraft-Mobile/releases/tag/v0.1.0-android-preview.7) from source commit `ab7c1b03f3d11833e6a11917d785280301bef0d1`.
- GitHub Android workflow [41220939709](https://github.com/Exa201/SolidCraft-Mobile/actions/runs/41220939709) passed Kotlin/JVM tests, Android APK build, ARM64/x86_64 split and package verification, signature verification, and artifact upload. 43 JVM tests passed in the fresh local test run for this source.
- Attached debug-signed APKs for arm64-v8a (93,000,023 bytes, SHA-256 `1cc38db6adb621cabbbdbbee5110aa1ad6fa49f0caa7a946a613e8ce1e0cc41d`) and x86_64 (109,812,420 bytes, SHA-256 `deec1e37c2b456f6287a725d1da4b6398bb503525120c3dd7e57755b4104e026`); see the release's `SHA256SUMS.txt`.
- Installed the exact CI x86_64 APK on API-35 and API-36 emulators and confirmed app launch. Successful opens of cached and cache-free box files were followed by a black screen with GLES framebuffer errors on both API versions. On a clean API-36 install the body appeared once before tapping File triggered it. API 36 also reproduced it after Save As, which still wrote a valid file. Picker cancellation did not reproduce it; rotation did not recover the API-36 viewport. The cause and Xiaomi/ARM64 Vulkan behavior remain unknown.
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
