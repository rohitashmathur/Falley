# Falley — 4.2 beta

Falley is an offline Android physics game by **Perf Games**. Fall through elastic ropes, steer with phone motion or swipes, avoid hazards and aim for a bouncy target. The current source version is **4.2.0 / version code 14**; a signed R8 release bundle and local debug APK have been built and verified. See [build status](BUILD_STATUS.md) and [4.2 preparation and verification notes](docs/BETA_4_2.md) for the exact artifacts and completed checks; source metadata alone is not a Play submission or rollout.

- **Production package:** `com.perfgames.falley`
- **Local debug package:** `com.perfgames.falley.motiontest` — Falley Motion Test
- **Support:** [mathurrohitash16@gmail.com](mailto:mathurrohitash16@gmail.com)
- **Privacy policy:** [falley-privacy-policy.vercel.app](https://falley-privacy-policy.vercel.app/)
- **Source:** [github.com/rohitashmathur/Falley](https://github.com/rohitashmathur/Falley)

APKs, App Bundles, mapping files, signing material and generated build outputs stay local. This repository contains source, documentation and selected verification evidence.

## Play and progress

**Campaign** has 24 sequentially unlocked stages across six chapters. Each chapter introduces mechanics, tells a short story and ends in a telegraphed machine finale. Moving or shrinking targets, wind, bounce pads, breakable anchors and slow-time pickups change the descent. Later stages introduce low gravity, gusts, heavier rope loads and a one-jump burst.

Each stage offers three independent, cumulative stars: **Finish**, **Jump par**, and **Gold centre**. Finishing unlocks the next stage; the other stars are optional mastery goals. Replays combine earned stars across successful runs. The gold landing zone covers the central 35% of the target width. Normally two quick jumps are available per burst, recovering after descent or a supporting landing; finite campaign jump allowances grow from 8 to 18. Recovery does not refund that total allowance. A one-jump modifier changes the burst, rather than limiting an entire run to one jump.

**Daily Drop** is an offline UTC-date/version-seeded course with unlimited retries, separate records and completion streaks. Six bounded templates provide seeded variation. A player can explicitly share a generated result image through Android's share menu; nothing is posted automatically. **Arcade** is configurable survival against fireballs, top/side lasers, diagonal lightning and buzzsaws, with separate records and no overall jump cap.

Human is the starter avatar. Demon, Angel, Military, Astronaut, Robot and Shadow unlock through the six chapter finales. All avatars have identical physics and hitboxes. Collection focuses on avatars and three campaign rope types: Classic is the original behavior, Spring unlocks at 6 stars with a 1.5× supporting-rope rebound, and Static unlocks at 12 stars with a weaker next manual jump and brief steering damping after support. Daily Drop and Arcade always use Classic. Saved decorative palettes remain separate from these abilities; there are no paid items. Mirror Practice is optional and awards no campaign progress or stars. Existing twelve-stage campaign saves migrate once without inventing optional mastery evidence.

## Interface and controls

Home highlights one campaign Continue action, chapter artwork, progress and the next reward. Daily Drop and Arcade are secondary choices. The main navigation is **Play · Collection · Settings**. Continue finds the first unfinished stage, then missing mastery stars, while chapter selection still supports explicit replays.

Briefings explain the three star goals before the drop. Touch and Motion choices, optional practice controls, named star results and quick Retry keep the next action clear. Collection shows the selected avatar and earned or locked rewards. Settings groups motion feel, independent music/sound preferences, vibration, blood effects, Help and Privacy.

English, Hindi and Bahasa Indonesia are supported. New players choose their language before Home, with English selected initially. A permission-free country hint can help surface a relevant option; it does not replace the player's choice. Returning saved language choices remain selected. Touch steering works without sensors; Motion uses a comfortable hold as its center, with Loose, Balanced and Tight response settings.

## Build a local debug APK

Requires **JDK 17**, Android SDK **platform 36**, **build-tools 35.0.0** and Android **7.0 / API 24** or newer. Set `JAVA_HOME` and `ANDROID_HOME`, or pass explicit paths. Run from the repository root:

```powershell
.\scripts\test-expansion.ps1 -JdkRoot 'C:\path\to\jdk-17'
.\scripts\build-android.ps1 -SdkRoot 'C:\path\to\android-sdk' -JdkRoot 'C:\path\to\jdk-17' -ApplicationId com.perfgames.falley.motiontest -AppLabel 'Falley Motion Test' -OutputApk 'C:\local-delivery\Falley-4.2.0-debug.apk'
adb install -r 'C:\local-delivery\Falley-4.2.0-debug.apk'
adb shell am start -n com.perfgames.falley.motiontest/com.perfgames.ropelab.MainActivity
```

The debug script defaults to **4.2.0-beta-test / code 14** and does not shrink or obfuscate the app. Its ignored `build/debug.keystore` is preserved across builds. Keep that key for compatible debug updates. Do not uninstall an existing app merely to bypass a signing mismatch if its progress matters. The established `scripts/test-physics.ps1` entry point delegates to the full portable suite.

## Prepare a signed Play bundle

The Gradle wrapper pins **8.11.1** and Android Gradle Plugin **8.10.1**, with compile/target SDK 36. Release uses R8 code optimization/obfuscation, resource shrinking, Android's optimized default rules and narrowly scoped app rules. Audio resources are referenced directly, rather than looked up by names at runtime.

```powershell
.\scripts\build-release.ps1 -SdkRoot 'C:\path\to\android-sdk' -JdkRoot 'C:\path\to\jdk-17' -BundletoolJar 'C:\path\to\bundletool-all-1.18.3.jar' -OutputDirectory 'C:\local-delivery\Falley-4.2.0\release'
```

Reuse the existing upload signing identity. See [release signing](docs/RELEASE_SIGNING.md); do not initialize a replacement key for an existing app. The release script runs lint and bundle generation, validates package/version/SDK/permissions, checks bundle signatures against the preserved upload certificate, and derives a local install APK. These checks happen only when the script runs successfully.

The script retains **mapping.txt**, confirms that the AAB's `BUNDLE-METADATA/com.android.tools.build.obfuscation/proguard.map` has the same SHA-256, copies available R8 configuration/usage reports, and writes `release-verification.json` plus artifact hashes beside the local output. Keep that exact mapping with each version. The bundle-derived upload-signed APK is a local test artifact; a Play-installed APK may use Google's app-signing certificate. Play upload, beta enrollment, content declarations and rollout are separate owner actions.

## Verification and publishing

Current acceptance: all 26 portable test groups pass; the final debug build passes native small/tall EN/HI/ID checks. The exact optimized production APK passes reflection-free smoke tests on Android 15 and 16 at both display sizes. Release lint has 0 errors / 10 nonfatal warnings; signing, mapping equality, bundle validation and ZIP alignment pass. See the linked receipts for exact identities and limits.

Portable tests cover physics, stability, motion/touch response, progression, migration, daily isolation, stars, cosmetics, recommendation rules and three-language copy. Native debug fixtures inspect controls and measured rendering; a reflection-free release smoke checks the optimized production artifact without retaining private implementation names just for tests. [4.2 notes](docs/BETA_4_2.md) and [build status](BUILD_STATUS.md) distinguish completed results from pending checks. Historical evidence is version-specific: [4.1 UX](docs/UX_UPDATE_4_1.md), [4.0 expansion](docs/EXPANSION_DEBUG_4_0.md), and [3.3.1 privacy](docs/PRIVACY_UPDATE_3_3_1.md).

Physical-phone acceptance still matters for motion comfort, difficulty, sound/music balance, haptics, reachability, sustained performance and TalkBack. Have fluent speakers review Hindi and Indonesian in context. Custom canvas text-fit tests do not establish arbitrary-size text accessibility or full WCAG conformance.

## Privacy and limits

The app has no accounts, Internet access, advertising, analytics or backend, and requests no Android permissions. Sensor samples are temporary. Preferences, stars, cosmetics, records and streaks stay on the device; app backup/device transfer are excluded. Result images use private cache and a read-only temporary share URI. Clearing app data or uninstalling removes local progress; copies deliberately shared to another app are controlled by that app.

Daily Drop resets at **00:00 UTC** (05:30 in India), uses local clock safeguards and has no server verification or global leaderboard. Date changes or cleared storage can defeat offline streak guarantees. Ads, revives, paid avatars, billing, donations and online services remain deferred. The [policy-site source](policy-site/README.md) and store listing must match the final shipped behavior; building the app does not deploy the website.