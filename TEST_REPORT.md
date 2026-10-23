# SolidCraft Mobile Test Report

## Environment and evidence

| Check | Result | Evidence / limitation |
|---|---|---|
| Rust host check for Android package | Passed previously | `cargo check -p solidcraft-android` on host; this does not cross-compile Android code. |
| Android arm64 native build | Passed locally | Gradle `:app:buildRustAndroid` rebuilt the Rust workspace after fixing root-relative task inputs. |
| Gradle debug APK build | Passed locally | Latest `:app:assembleDebug` includes durable URI-stable SAF working copies, eframe settings/recent-file persistence, phone-friendly recent names, Android recovery startup, and the compact narrow-screen Browser; 23 Oct 2026; 176,647,009 bytes; SHA-256 `A7A5CABCAF857A3F172D024E2241FE7B93B0352A0F50D808386CF4BDE4D554F6`. |
| Gradle optimized release APK build | Passed locally | Current `:app:assembleRelease`; 91,007,758 bytes (~86.8 MiB); SHA-256 `A376C4A53204044C17F065D1EEC36F4D99C6C1CA96CEAD2F1AA77A7F494E4170`. |
| Release APK metadata | Passed | `org.solidcraft.mobile`, version `0.1.0`, min API 28, target/compile API 37, arm64-v8a. |
| Release APK signature | Not release-ready | Current `app-release-unsigned.apk` fails `apksigner verify` because it is unsigned. No protected production signing key is configured in this checkout. |
| Physical Android 16 install and launch | Passed latest debug smoke test | Current rebuilt APK (including the sphere-tangency kernel changes) installed on HONOR PTP-N49 (API 36), launched; process 24904 remained active and sampled logcat had no fatal exception or Rust panic. No CAD/file workflow was run in this smoke test. |
| Targeted navigation and touch integration tests | Passed | `cargo test -p solidcraft-ui-egui --profile ci navigation::tests --lib`: 16 passed. Includes egui `Event::Touch` flow through the shared UI into camera movement, release reset, and rejection of gestures outside the viewport. This is a host integration test, not an Android device gesture test. |
| Touch long-press context menu | Passed | `viewport::tests::touch_long_press_opens_the_viewport_context_menu`: verifies the viewport opens its context menu after egui's touch hold threshold using the combined touch and primary-pointer event stream. Included in the complete UI suite. |
| File picker lifecycle tests after SAF integration | Passed | `cargo test -p solidcraft-ui-egui --profile ci file_picker::tests --lib`: 7 passed, 0 failed. These cover generic picker wait/cancel/save sequencing, not Kotlin `ContentResolver` or device provider behavior. |
| Phone-width ribbon test | Passed | `toolbar::responsive_ribbon_tests::command_labels_are_compact_on_phone_width_and_preserved_on_tablets`; phone width 432 points uses icons, 600 points and above respect the saved label preference. |
| Recent-document menu label regression | Passed | `toolbar::responsive_ribbon_tests::recent_file_menu_shows_document_name_instead_of_android_private_path` verifies Android paths display a useful filename, including the missing-file state. |
| Android preferences and recent documents persist | Passed | API-36 emulator: Eframe settings storage was found in `files/solidcraft-mobile.ron` after autosave; after force-stop/relaunch, File → Open Recent showed `solidcraft-saf-test.solidcraft`, and selecting it reopened the one-body design. |
| Shared CAD recovery logic | Passed (shared-code tests) | `cargo test -p solidcraft-ui-egui --profile ci --lib recovery_ui::tests`: 10 passed. The Android launch path starts this service in app-private storage; startup created the recovery directory on both the API-36 emulator and Android-16 HONOR device. A forced-crash dirty-document recovery has not yet been verified on Android. |
| Complete egui UI crate unit tests | Passed | Latest `cargo test -p solidcraft-ui-egui --profile ci --lib -- --test-threads=8`: 1,860 passed, 6 intentionally ignored, 0 failed (441.61 seconds). This run includes the narrow-screen Browser regression and all other egui tests. |
| UI crate Clippy | Passed | `cargo clippy -p solidcraft-ui-egui --profile ci --lib -- -D warnings`. |
| Rust formatting, workspace Clippy, and diff whitespace | Passed locally | After the latest kernel retry change: `cargo fmt --all -- --check`, `cargo clippy --workspace --profile ci -- -D warnings`, and `git diff --check` all passed. |
| Full repository CI suite | Passed locally | `cargo xtask ci` completed with exit code 0: all static gates passed and all 79 test binaries passed. Engine: 1,986 passed, 6 ignored; kernel: 871 passed, 28 ignored; egui: 1,860 passed, 6 ignored; action matrix: 22 passed, 5 ignored; atomic commands: 3 passed; model cache: 5 passed; icon Python tests: 17 passed. No tests were disabled. The optional offline web test was explicitly skipped because Chrome is unavailable (`web-offline-test: skipped (no Chrome; set CHROME to its executable)`). |
| Android emulator launch and immersive viewport | Passed | Rebuilt and reinstalled latest debug APK on API 36 emulator `emulator-5554`; `org.solidcraft.mobile/.MainActivity` was top-resumed, with no fatal exception or Rust panic in the recent log. Immersive mode removes status/navigation bars from the CAD canvas. |
| Narrow-screen Browser behavior | Passed | New Rust regression test verifies compact default, hidden rows, touch expansion, full-width tree, and separation from the desktop collapse preference. The complete Browser card test group passed (12/12). Latest APK screenshot on API-36 emulator showed the compact header on a 390 px portrait viewport; tapping it revealed the model tree. |
| Landscape and physical Android 16 smoke check | Passed (launch/display only) | Latest APK launched top-resumed on emulator in landscape with the full Browser tree and on physical HONOR PTP-N49 (API 36). Captured screens showed the viewport and controls; recent logs contained no fatal exception or Rust panic. Full CAD modeling gesture workflows were not run on the physical device. |
| Android emulator startup panic log | Clear after fix | Package-filtered recent log contained no Rust panic or fatal exception; graphics startup messages only. |
| GitHub packaging lint | Passed for prior PR branch HEAD | Check at run `39842659701`; this worktree contains unpushed changes. |
| GitHub Android APK workflow | Not verified for current worktree | Prior run `39842660249` was cancelled while the APK build step was still running; no artifact uploaded. |
| GitHub CI | Not verified | Current local changes are not pushed; the open PR has not run CI on this worktree state. |
| SAF file open on device | Passed | API-36 emulator: chose `solidcraft-saf-test.solidcraft` in Android Downloads. Rust loaded it from the staged private copy and displayed one body in the Browser. |
| SAF Save As and reopen | Passed | API-36 emulator: Save As wrote `saf-roundtrip.solidcraft` to Downloads (8,919 bytes); after force-stopping and restarting the app, the same provider file was selected and reopened with one body. |
| SAF asynchronous DXF export | Passed | API-36 emulator: exported the current view; provider file size 19,265 bytes and DXF header `AC1015` verified. The path is synchronized back only after the background export completes. |
| SAF recent-file restoration after process restart | Passed | Latest debug APK on API-36 emulator: opened the Downloads design, waited for autosave, force-stopped and relaunched the app, selected `solidcraft-saf-test.solidcraft` from File → Open Recent, and reopened the design with one body. The preferences file and durable URI-derived workspace file were verified in app-private storage. |
| Android touch long-press context menu | Passed | API-36 emulator `emulator-5554`: a 1.1-second one-finger hold in the viewport opened the menu with Pan, Zoom, Orbit, Fit, and Home View. The interaction was verified from the captured screen. |
| SAF bridge build | Passed | Kotlin activity and Rust JNI bridge compile into the arm64 debug APK. A global JNI class reference loaded on the UI thread is reused by native save workers. Open/create-document requests stage files in app cache and route writeback through a Kotlin host callback. |
| Android multitouch / rotation / pause-resume | Partially tested | Two-finger navigation is exercised with actual egui touch events in a Rust host integration test; no Android device gesture or lifecycle transitions were exercised. No automated Android UI test suite is configured. |
| Engine command parity on Android | Not tested end-to-end | Android shares Rust engine/UI but each desktop command has not been exercised on device. |

## Required follow-up verification

1. Add automated Android SAF host tests and exercise additional providers, overwrite, cancellation, permission loss, and write failures. The API-36 emulator already verified a Downloads open/save/reopen and DXF export round trip.
2. Add automated engine, Kotlin host, and UI/input tests; retain upstream engine unit coverage.
3. Run Android 16 physical-device tests for sketching, selection, transforms, pan/pinch/orbit, viewport lifecycle, rotation, pause/resume, and recovery.
4. Test a small-screen portrait device and a tablet/landscape form factor.
5. Obtain a successful GitHub CI and APK workflow result for the exact pushed commit.
6. Build and inspect a signed/release APK; compare its checksum with the uploaded release artifact.

The 3.11 runtime was installed under the user-local tooling directory only; no Python runtime or generated `.pyc` files are part of the repository. The full offline web test remains unverified because Chrome is unavailable in this environment.

A successful APK build alone is not a release-quality result.
