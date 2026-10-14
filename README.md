# Falley — V2

**Physics at play.** An offline Android game about guiding a falling human through elastic ropes to a bouncy ground target. V2 expands the descent into **six stages across three chapters**: Laser, Spikes and Fire. The final Fire stage adds falling fireballs with warnings before they drop.

This repository shares source and test evidence. **APKs are built and kept locally; no public APK download is provided.** This is a debug testing build, not a Google Play production submission.

## Play

Choose a chapter and stage from Home, then descend to its marked target. Tap the arena or **Jump** for an upward impulse; intact-rope landings also produce a small natural jump. Points increase weight, heavy contacts snap ropes, and the side hazards gradually close in.

Tilt to steer when motion is enabled. With motion off, swipe left/right to move in that direction; release or cancel ends touch steering. Touch steering remains available without motion sensors. The taller play area keeps attention on the descent; motion feel belongs in Settings rather than the gameplay screen.

Land with the whole character contact area inside the ground target to win. Later stages shift and narrow the target, requiring an intentional finish rather than staying at the arena center. A miss bounces without a false win, allowing another attempt. A short physical finish bounce precedes the result. Pause, restart, replay and Home navigation are available. The human uses distinct falling, jumping and crouched landing poses rather than a ragdoll.

Home has an animated Falley title. Startup shows the Falley icon and **Falley by Perf Games**; returning from the background does not replay the intro.

## Settings and local records

Motion steering uses gyro plus accelerometer when available, with filtered accelerometer fallback. **Loose / Balanced / Tight** adjust sensor gain, dead zone and smoothing; they are motion feel choices, not difficulty presets. The first valid sample automatically centers the comfortable phone pose; resume starts from a fresh sensor reference. There is no manual centering control.

Motion, feel, short sound effects and vibration settings persist on the device. There is no music setting or downloaded audio. Best score, wins and fastest finish are kept globally and per stage; the selected stage persists too. An unfinished run is not saved across process death.

## Build and install locally

Requires JDK 17, Android SDK **platform 36**, **build-tools 35.0.0**, and an Android 7.0/API 24 or newer phone. Run from this project root:

```powershell
.\scripts\build-android.ps1 -SdkRoot 'C:\path\to\android-sdk' -JdkRoot 'C:\path\to\jdk-17'
.\scripts\test-physics.ps1 -JdkRoot 'C:\path\to\jdk-17'
adb install -r .\Falley-debug.apk
adb shell am start -n com.perfgames.falley/com.perfgames.ropelab.MainActivity
```

Alternatively set `ANDROID_HOME` and `JAVA_HOME`. The builder emits `Falley-debug.apk` locally and uses fresh staging under `build/`. Preserve the ignored `build/debug.keystore` to install future debug updates over this one; never publish that key or APK binaries. Application ID remains `com.perfgames.falley`; its legacy internal Java namespace is intentional.

Gradle configuration uses AGP 8.10.1, Gradle 8.11.1 compatibility, JDK 17 and compile/target SDK 36. A wrapper is not bundled; the alternate `gradle :app:assembleDebug` path requires the SDK and plugin-repository access. [Official AGP compatibility](https://developer.android.com/build/releases/agp-8-10-0-release-notes) supports this combination. The portable builder is the validation path used here.

The native regression runner needs an already running disposable emulator, an installed Falley APK and **SDK platform 35** in addition to the app builder's platform 36. Platform 35 supplies `android.jar` for compiling the reflection-only harness:

```powershell
.\scripts\test-ui.ps1 -Device emulator-5584 -SdkRoot 'C:\path\to\android-sdk' -JdkRoot 'C:\path\to\jdk-17'
```

Its controlled target/death fixtures are test setups, not evidence of natural phone play. Source lives in `android-tests/`; portable simulation/profile tests live in `tests/`.

## Rules and verification

`StageCatalog.java` describes the finite stages. `Physics.java` handles fixed 240 Hz gravity, elastic ropes, load/cuts, steering, closing side hazards, fireball warning/drop behavior, floor contact and win/loss state. A small rope pool recycles during longer descents and stops before the ground. Winning freezes gameplay input and score while the celebration bounce continues. `MotionSteering.java` filters sensors; `FalleyProfile.java` handles safe local result accounting.

**Portable tests: PASS.** All six complete courses were won using actual gravity/rope progression and bounded steering; final-stage fire warnings were encountered and avoided. Physics checks cover touch direction/release/no drift, rope recycling/cuts, chapter hazards, warning/fire collision, shifted target recovery, complete contact-area bounds, sides remaining closed through actual jumps, result/reset state and **100,000 seeded stability steps**. Touch gestures have 16 cases, motion filtering 17 and global/per-stage profiles 21. See `BUILD_STATUS.md` and `screenshots/v2/physics-suite.log` for the final local APK/build evidence.

**Native final APK checks: PASS.** Android 15/API 35 and Android 16/API 36, each at 480x800/240 dpi and 720x1600/320 dpi: four suites with 416 logged checks each (**1,664 total**). Actual swipe/tap/Jump, release/cancel/no drift, system Back, pause, settings/preset persistence, chapter/stage selection and replay were exercised. Controlled fixtures verify all six target finishes, Next Stage, per-stage records, three side-hazard reasons and final-stage fire warnings/drop/collision. Home, chapter selection, gameplay and results were visually reviewed without clipping or system-bar overlap.

The native runner captured 112 images locally; **48 curated images** plus suite/check logs are kept under `screenshots/v2/api35/` and `api36/`. Original emulator preferences/display settings were restored and test APKs removed. Controlled target/fire fixtures do not establish natural phone play success or subjective fun.

## Privacy and remaining gaps

Preferences, stage records and sensor processing stay on the device. This build has no Internet permission, accounts, analytics, ads or backend; it does not record sensor history. Clear app data or uninstall to remove local records. Android app backup is disabled.

Real-phone tilt comfort, swipe feel, haptics/audio, battery use and subjective fun still need testing. The six deterministic steering runs finished in approximately **10.1–15.4 seconds**; these are simulation test durations, not player completion times. Physics uses a compact circular proxy, simplified rope cuts and small fixed steps rather than full articulated/continuous collision. Canvas controls are English-only and do not yet offer complete independent TalkBack controls.

A production release-signed AAB, Play Console setup/disclosures and production rollout remain unbuilt/unsubmitted. Target SDK and a local debug APK do not establish store or worldwide IP approval. More modes and content beyond these V2 stages remain deferred until the controls and chapter pacing are tested on phones.
