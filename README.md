# Falley — 3.2 offline beta

**Physics at play.** An offline Android physics game with elastic ropes, a falling human and two ways to play: a six-stage campaign and configurable Arcade survival.

Version **3.2.0 / code 7** prepares an **offline, ad-free Android phone beta**. APKs, AABs and keys stay local; source, screenshots, test evidence and store artwork may be shared. No Google Play upload or rollout has been performed.

## Play

**Campaign:** choose one of six stages across Laser, Spike and Fire chapters. Survive the narrowing sides, cut ropes as weight increases, and land with the whole contact area inside the stage's target. Later stages shift and narrow the target. Stages 1–3 allow an off-target bounce and recovery; **stages 4–6 make a missed floor target fatal**.

Stage 4, **Needle run**, introduces a guaranteed telegraphed buzzsaw before mixing saws with side lasers. Stage 6 mixes all five incoming attack families. Incoming attacks retire within the final 650 world units so they cannot conflict with the target approach; narrowing boundaries and floor rules remain active.

**Arcade:** configure **Chill / Standard / Brutal** and enable one or more incoming attacks: falling fire, top laser, side laser, diagonal lightning and **buzzsaws**. A saw warns along a locked lateral path, then sweeps across part of the corridor while leaving an escape side. These toggles select incoming attacks; the narrowing boundary hazards always remain lethal. Survive for score and time; there is no campaign finish or win. Warning cues precede active hazards. Arcade records are separate for each difficulty and never increase campaign wins.

Tap to jump and swipe horizontally to steer, or enable phone motion. In non-motion play, deliberate drift requires continued steering; leaving the figure idle is not an automatic route to a win. Pause, restart and Home navigation remain available. The human uses jumping, falling and crouched landing poses, plus hazard-specific death reactions.

## Settings and records

Motion uses gyro plus accelerometer when available and filtered accelerometer fallback otherwise. Loose / Balanced / Tight tune sensor response in Settings. The first valid sample automatically centers the current phone pose; no manual centering control is shown.

Motion feel, sound effects, vibration and **Blood effects** persist locally. Blood effects can be turned off. There is no music setting, downloaded audio, account, advertising or backend.

V3.1 bundles **11 original synthesized effects** for fire launch/burning, laser, lightning, saw motor/impact, UI, jump, rope, win and loss. Android SoundPool plays bounded one-shots with at most three voices. Sound off stops active effects; pause/background/restart clears playback and destruction releases the player. Samples still loading are dropped rather than queued, so an early cue may be silent and cannot arrive late after a pause. Real-device audibility and mix quality have not been verified.

The 3.2 beta adds a scrollable offline **Privacy Policy** in Settings and exposes menu controls as accessibility nodes. The screen stays awake during active play; static menus avoid continuous rendering. Native privacy/accessibility/wake checks pass; real-phone screen-reader acceptance is still needed.

Campaign best scores, wins and fastest finishes are stored globally and per stage. Arcade stores best score, run count and longest survival separately for Chill, Standard and Brutal. Selected campaign stage and Arcade difficulty/hazard choices persist. At least one Arcade hazard must remain enabled. Unfinished runs are not restored after process death.

Existing saved four-family selections (mask `15`) stay unchanged after updating. New profiles enable all five (mask `31`); an empty or invalid-only mask falls back to all five.

## Build and install locally

Requires JDK 17, Android SDK **platform 36**, **build-tools 35.0.0**, and Android 7.0/API 24 or newer. From this project root:

```powershell
.\scripts\build-android.ps1 -SdkRoot 'C:\path\to\android-sdk' -JdkRoot 'C:\path\to\jdk-17'
.\scripts\test-physics.ps1 -JdkRoot 'C:\path\to\jdk-17'
adb install -r .\Falley-debug.apk
adb shell am start -n com.perfgames.falley/com.perfgames.ropelab.MainActivity
```

Alternatively set `ANDROID_HOME` and `JAVA_HOME`. The builder emits `Falley-debug.apk` locally with fresh staging under `build/`. Preserve the ignored `build/debug.keystore` to install later updates; never publish the key or APK/AAB binaries. Application ID remains `com.perfgames.falley`.

The checked-in wrapper pins **Gradle 8.11.1** with a distribution checksum; the project uses **AGP 8.10.1**, JDK 17 and compile/target SDK 36. The portable builder remains available for debug iteration.

For the local upload-signed beta bundle, the tool setup helper downloads and checksum-verifies official **bundletool 1.18.3**. First-time setup initializes the private upload identity; omit `-InitializeSigning` on later builds:

```powershell
$bundletool = .\scripts\setup-release-tools.ps1
.\scripts\build-release.ps1 -SdkRoot 'C:\path\to\android-sdk' -JdkRoot 'C:\path\to\jdk-17' -BundletoolJar $bundletool -InitializeSigning
```

The script runs `lintRelease` and `bundleRelease`, validates the bundle, checks its signature and signed payload certificates, and generates/verifies a universal APK. Final completion is recorded in [build status](BUILD_STATUS.md). Local outputs are `releases/3.2.0/Falley-3.2.0-beta.aab`, `Falley-3.2.0-beta-local.apk`, its `.apks` archive, manifest and `SHA256SUMS.txt`. The AAB is for a later owner-authorized Play upload; the generated APK is for local install testing.

`scripts/release-signing.ps1` keeps the upload key under ignored `releases/private-signing/`, restricts its Windows directory access and protects its credential using the current Windows user's DPAPI. Preserve this identity and a secure recoverable backup; the encrypted credential is tied to that Windows user. See [release signing](docs/RELEASE_SIGNING.md). Never publish signing files or binaries. A release-signed install may require uninstalling an older debug-signed copy, which deletes local records. The locally derived APK uses the upload key; it does not replace testing a later Play-signed install/update.

Native regressions require a running disposable emulator, the installed APK and **SDK platform 35** for the reflection-only test harness, in addition to the app builder's platform 36:

```powershell
.\scripts\test-ui.ps1 -Device emulator-5584 -SdkRoot 'C:\path\to\android-sdk' -JdkRoot 'C:\path\to\jdk-17'
```

Controlled hazard/floor fixtures are test setups, not natural phone play sessions. Portable source tests live in `tests/`; native tests live in `android-tests/`.

The bundled WAVs are generated from oscillators and deterministic noise, without external recordings or samples. Python's standard library is sufficient to validate or regenerate them:

```powershell
python .\scripts\generate_audio.py --check
python .\scripts\generate_audio.py
```

## 3.2 beta status

**The frozen release build passes:** `lintRelease` and `bundleRelease`, bundletool validation, strict bundle signature checks, all 24 payload certificates, derived APK v2/v3 signatures and `zipalign -P 16`. Lint reports **0 errors and 9 warnings**. Jarsigner also reports ZIP-attribute/archive-order warnings; the strict verification and every-entry certificate checks pass, with unsigned/added-unsigned-entry negative controls rejected. This is not a zero-warning claim.

The local AAB is **301,784 bytes** and its derived APK **321,030 bytes**. [Build status](BUILD_STATUS.md) records exact SHA-256 values, upload certificate and app-source revision. Manifest checks confirm a non-debuggable API 24–36 app with optional sensors, no requested permissions and backup/cloud/device-transfer exclusions.

**Current native checks pass on the frozen, bundle-derived APK:** full Android 16/API 36 suites at 480x800 and 720x1600 each pass **1,019 checks/64 captures**; focused Android 15/API 35 suites each pass **387 checks/13 captures**. Four runs total **2,812 checks/154 captures**. Curated evidence under `screenshots/v3.2/` contains 42 PNGs, four run receipts/check logs and release-validation logs. These are controlled emulator checks, not human listening or TalkBack acceptance.

A separate store-capture run passes **125 checks** and produces six fresh **1080x1920** native captures from 3.2. All **10 prepared image assets**, including optional feature-graphic variants, pass dimensions, mode, size, checksum, alt-text and source/crop-integrity checks and visual review. The app-source revision remains frozen; the later documentation/artwork commit does not change the binary.

Campaign/Arcade physics and the bundled WAVs are unchanged from 3.1. Their deterministic course/survival and asset-validation proof below is carried forward, **not rerun or claimed as new 3.2 binary verification**. Current native checks cover the privacy, accessibility, rendering, packaging and lifecycle changes.

## Historical 3.1 verification

**Portable V3.1 tests: PASS.** All six courses complete through gravity/rope progression and bounded steering, including the actual saw introduction. Campaign and Arcade each pass **100,000 seeded stability steps**. Focused tests cover harmless saw warnings, fast swept hits and near misses, reachable narrow-corridor refuges, locked/exclusive attacks, saved masks, clear landing approaches and one-shot audio events. A bounded controller survives **25 seconds into wave 2 with all five hazards at every difficulty**. These simulation results are not human success rates or a guarantee that every possible situation is avoidable.

Campaign has 20,939,979 checks, Arcade 21,000,408, focused saw/cues 879, audio event consumption 20, gestures 16, motion 17 and preferences/records 33. Audio asset validation reports all 11 files valid, distinct, faded and within the generator's PCM peak/headroom bounds. Evidence belongs under `screenshots/v3.1/physics-suite.log` and `audio-assets.log`; raw simulation output is `build/physics-v3.1.log`.

**The V3.1/code 6 local APK build and v2/v3 signature checks pass.** The same APK passed full native Android 16/API 36 suites at 480x800 and 720x1600: **799 checks/62 captures each, 1,598 checks/124 captures total**. Focused Android 15/API 35 saw/audio suites passed **185 checks/11 captures each, 370 checks/22 captures total**. All four current runs total **1,968 checks/146 captures**, with 40 curated PNGs and run logs under `screenshots/v3.1/`.

All 11 SoundPool samples loaded; actual physics cues incremented native playback once, and mute, menu/background, recreation and release checks passed. Saw warning/active/impact frames were visually reviewed. This verifies software playback behavior, not audible output or mix quality. Native collision/hazard fixtures use controlled states rather than natural phone play sessions.

Historical V3.0/code 5 passed four API 35/36 suites with 2,600 checks/208 captures, plus 346 focused effects checks/30 captures. Its evidence remains under `screenshots/v3/`; it does not validate V3.1 audio or saws. [Build status](BUILD_STATUS.md) records the current artifact and verification state.

## Privacy and known gaps

Preferences, campaign/Arcade records and sensor processing stay on the device. The app has no Internet permission, analytics or backend and does not save sensor history. Clear app data or uninstall to remove local records; backup/data-extraction rules disable app-data backup and transfer. Settings includes an offline policy, but the owner still needs a real support contact and a publicly accessible policy URL before closed-beta submission.

Real-phone motion/swipe comfort, blood-effect readability, haptics/audio, battery use, screen-reader use and subjective fun still need testing. Balance and difficulty are provisional. Physics uses a compact contact proxy, simplified rope cuts and fixed substeps rather than a full articulated ragdoll or continuous-collision engine. Menus remain English-only; new accessibility nodes do not establish full real-device TalkBack acceptance.

This beta stays offline and ad-free. **AdMob and consent handling are not integrated.** [The beta submission guide](docs/BETA_SUBMISSION.md) orders the owner/Console steps; [store drafts](docs/store/README.md) contain listing, app-content, reviewer and tester material. [Store image instructions](docs/store-assets.md) describe the prepared icon, promotional graphic and six phone images. Artwork and draft text do not publish a listing or establish approval, an age rating or IP clearance.

The **support email and public privacy-policy URL are not ready**; no contact details have been invented. The real contact must also be added to the in-app policy, requiring a **new higher version code and rebuilt/retested bundle before closed beta**. The owner must confirm account/package access and review IARC, audience and Data safety answers. Start with a small Play internal-test cohort and real-phone audio/control/performance sessions, then complete the listing/policy gates before closed testing. Any account-specific tester-duration requirement still applies. Future advertising needs a separate SDK/consent implementation and fresh privacy declarations; production remains outside this beta task.
