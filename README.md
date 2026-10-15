# Falley — V3

**Physics at play.** An offline Android physics game with elastic ropes, a falling human and two ways to play: a six-stage campaign and configurable Arcade survival.

APKs are built and kept locally. Source, screenshots and test evidence may be shared; no public APK download or Google Play production release is provided.

## Play

**Campaign:** choose one of six stages across Laser, Spike and Fire chapters. Survive the narrowing sides, cut ropes as weight increases, and land with the whole contact area inside the stage's target. Later stages shift and narrow the target. Stages 1–3 allow an off-target bounce and recovery; **stages 4–6 make a missed floor target fatal**.

**Arcade:** configure **Chill / Standard / Brutal** and enable one or more incoming attacks: falling fire, top laser, side laser and diagonal lightning. These toggles select incoming attacks; the narrowing boundary hazards always remain lethal. Survive for score and time; there is no campaign finish or win. Warning cues precede active hazards. Arcade records are separate for each difficulty and never increase campaign wins.

Tap to jump and swipe horizontally to steer, or enable phone motion. In non-motion play, deliberate drift requires continued steering; leaving the figure idle is not an automatic route to a win. Pause, restart and Home navigation remain available. The human uses jumping, falling and crouched landing poses, plus hazard-specific death reactions.

## Settings and records

Motion uses gyro plus accelerometer when available and filtered accelerometer fallback otherwise. Loose / Balanced / Tight tune sensor response in Settings. The first valid sample automatically centers the current phone pose; no manual centering control is shown.

Motion feel, sound effects, vibration and **Blood effects** persist locally. Blood effects can be turned off. There is no music setting, downloaded audio, account, advertising or backend.

Campaign best scores, wins and fastest finishes are stored globally and per stage. Arcade stores best score, run count and longest survival separately for Chill, Standard and Brutal. Selected campaign stage and Arcade difficulty/hazard choices persist. At least one Arcade hazard must remain enabled. Unfinished runs are not restored after process death.

## Build and install locally

Requires JDK 17, Android SDK **platform 36**, **build-tools 35.0.0**, and Android 7.0/API 24 or newer. From this project root:

```powershell
.\scripts\build-android.ps1 -SdkRoot 'C:\path\to\android-sdk' -JdkRoot 'C:\path\to\jdk-17'
.\scripts\test-physics.ps1 -JdkRoot 'C:\path\to\jdk-17'
adb install -r .\Falley-debug.apk
adb shell am start -n com.perfgames.falley/com.perfgames.ropelab.MainActivity
```

Alternatively set `ANDROID_HOME` and `JAVA_HOME`. The builder emits `Falley-debug.apk` locally with fresh staging under `build/`. Preserve the ignored `build/debug.keystore` to install later updates; never publish the key or APK/AAB binaries. Application ID remains `com.perfgames.falley`.

Gradle configuration remains AGP 8.10.1, Gradle 8.11.1 compatibility, JDK 17 and compile/target SDK 36. A wrapper is not bundled; the alternate `gradle :app:assembleDebug` route requires the SDK and plugin-repository access. The portable builder is the validation path used here.

Native regressions require a running disposable emulator, the installed APK and **SDK platform 35** for the reflection-only test harness, in addition to the app builder's platform 36:

```powershell
.\scripts\test-ui.ps1 -Device emulator-5584 -SdkRoot 'C:\path\to\android-sdk' -JdkRoot 'C:\path\to\jdk-17'
```

Controlled hazard/floor fixtures are test setups, not natural phone play sessions. Portable source tests live in `tests/`; native tests live in `android-tests/`.

## V3 verification

**Portable V3 tests: PASS.** All six campaign courses were completed through real gravity/rope progression and bounded steering, including the new hazards and fatal later-stage floor misses. Idle touch-only runs lose; countersteering controls the intentional current. Campaign and Arcade each passed **100,000 seeded stability steps**.

Arcade tests cover selected hazard families at every difficulty, warning reaction time, reachable full-body refuges, locked telegraphs, rounded beam endpoints/near misses, swept diagonal/parallel geometry, distinct collision reasons and mode/reset isolation. A bounded test controller survived **25 seconds into wave 2 with all four hazards at each difficulty**. These are deterministic simulation results, not human success rates or a guarantee that every possible situation is avoidable.

Gesture policy has 16 cases, motion filtering 17 and local preferences/independent campaign-and-Arcade records 31. **The local 3.0.0/code 5 APK build and v2/v3 signatures pass.** The same APK passed four native suites on Android 15/API 35 and Android 16/API 36 at 480x800 and 720x1600: **2,600 checks and 208 captures**. Separate focused API 35 effects suites added **346 checks and 30 captures**, covering partial side beams, diagonal lightning and lingering fire/lightning ash.

Native checks exercised swipes, countersteering, release behavior, Back/pause navigation, settings/record persistence, campaign targets and strict floor misses, Arcade setup and distinct hazard deaths. Collision, target and 40-second Arcade fixtures use controlled states; they are not natural phone play sessions. Curated screenshots and suite logs are under `screenshots/v3/`. `BUILD_STATUS.md` records the checksum and evidence. Real-phone play remains untested.

## Privacy and known gaps

Preferences, campaign/Arcade records and sensor processing stay on the device. The app has no Internet permission, analytics or backend and does not save sensor history. Clear app data or uninstall to remove local records; Android app backup is disabled.

Real-phone motion/swipe comfort, blood-effect readability, haptics/audio, battery use and subjective fun still need testing. Balance and difficulty are provisional. Physics uses a compact contact proxy, simplified rope cuts and fixed substeps rather than a full articulated ragdoll or continuous-collision engine. Canvas controls remain English-only without complete independent TalkBack navigation.

Release-signed AAB generation, Play Console declarations/content rating and production rollout remain outside this debug prototype. No store submission or public binary publication is performed.
