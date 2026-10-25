# Falley development and build instructions

The current debug checkpoint is **4.3.0/code 16**. Release instructions below are for a future separately requested signed build; no new release bundle is prepared in this checkpoint.

## Build a local debug APK

Requires **JDK 17**, Android SDK **platform 36**, **build-tools 35.0.0** and Android **7.0 / API 24** or newer. Set `JAVA_HOME` and `ANDROID_HOME`, or pass explicit paths. Run from the repository root:

```powershell
.\scripts\test-expansion.ps1 -JdkRoot 'C:\path\to\jdk-17'
.\scripts\build-android.ps1 -SdkRoot 'C:\path\to\android-sdk' -JdkRoot 'C:\path\to\jdk-17' -ApplicationId com.perfgames.falley.motiontest -AppLabel 'Falley Motion Test' -OutputApk 'C:\local-delivery\Falley-4.3.0-debug.apk'
adb install -r 'C:\local-delivery\Falley-4.3.0-debug.apk'
adb shell am start -n com.perfgames.falley.motiontest/com.perfgames.ropelab.MainActivity
```

The debug script defaults to **4.3.0-beta-test / code 16** and does not shrink or obfuscate the app. Its ignored `build/debug.keystore` is preserved across builds. Keep that key for compatible debug updates. Do not uninstall an existing app merely to bypass a signing mismatch if its progress matters. The established `scripts/test-physics.ps1` entry point delegates to the full portable suite.

## Prepare a signed Play bundle

The Gradle wrapper pins **8.11.1** and Android Gradle Plugin **8.10.1**, with compile/target SDK 36. Release uses R8 code optimization/obfuscation, resource shrinking, Android's optimized default rules and narrowly scoped app rules. Audio resources are referenced directly, rather than looked up by names at runtime.

```powershell
.\scripts\build-release.ps1 -SdkRoot 'C:\path\to\android-sdk' -JdkRoot 'C:\path\to\jdk-17' -BundletoolJar 'C:\path\to\bundletool-all-1.18.3.jar' -OutputDirectory 'C:\local-delivery\Falley-4.3.0\release'
```

Reuse the existing upload signing identity. See [release signing](RELEASE_SIGNING.md); do not initialize a replacement key for an existing app. The release script runs lint and bundle generation, validates package/version/SDK/permissions, checks bundle signatures against the preserved upload certificate, and derives a local install APK. These checks happen only when the script runs successfully.

The script retains **mapping.txt**, confirms that the AAB's `BUNDLE-METADATA/com.android.tools.build.obfuscation/proguard.map` has the same SHA-256, copies available R8 configuration/usage reports, and writes `release-verification.json` plus artifact hashes beside the local output. Keep that exact mapping with each version. The bundle-derived upload-signed APK is a local test artifact; a Play-installed APK may use Google's app-signing certificate. Play upload, beta enrollment, content declarations and rollout are separate owner actions.

## Verification and publishing

The 4.3 debug route retains the existing debug signing key and isolated package. Firebase/Analytics integration is deferred to 4.4; no Gradle dependency or permission changes are part of 4.3.

Current acceptance is recorded in [Build Status](../BUILD_STATUS.md) and [4.3.0 notes](BETA_4_3.md). Historical 4.2/4.2.1 release and debug receipts do not validate the current debug checkpoint.

Portable tests cover physics, stability, motion/touch response, progression, migration, daily isolation, stars, cosmetics, recommendation rules and three-language copy. Native debug fixtures inspect controls and measured rendering; a reflection-free release smoke checks the optimized production artifact without retaining private implementation names just for tests. [current debug notes](BETA_4_3.md) and [build status](../BUILD_STATUS.md) distinguish completed results from pending checks. Historical evidence is version-specific: [4.1 UX](UX_UPDATE_4_1.md), [4.0 expansion](EXPANSION_DEBUG_4_0.md), and [3.3.1 privacy](PRIVACY_UPDATE_3_3_1.md).

Physical-phone acceptance still matters for motion comfort, difficulty, sound/music balance, haptics, reachability, sustained performance and TalkBack. Have fluent speakers review Hindi and Indonesian in context. Custom canvas text-fit tests do not establish arbitrary-size text accessibility or full WCAG conformance.

## Phone and tablet rendering checks

The native UX runner accepts `-Viewport small,tall,tablet,xlarge,landscape,native` (480x800, 720x1600, 1200x1920, 1600x2560 and 1920x1200 fixtures; `native` preserves a cold-boot AVD's physical display). It runs only on emulators, snapshots/restores preferences and display/font settings, and records measured rendering plus screenshots. Use `-UxOnly -AppPackage com.perfgames.falley.motiontest -FontScale 1.3`; provide the actual emulator ID and artifact directory. For tablet acceptance, use a dedicated Pixel Tablet AVD and `-Viewport native`: hot-resizing a phone AVD can leave Launcher taskbar touch geometry inconsistent with reported insets. The current 4.3 fixture rotates the actual tablet window, checks Pause and same-instance run preservation, and captures both menu orientations. Nominal requested dimensions alone are not proof of a tablet window.

Phones retain portrait presentation. At 600dp and above the activity allows user rotation and handles size/orientation changes in place, pausing a live run and resetting steering samples. Current evidence records actual window dimensions and responsive layout. Physical tablets and multi-window behavior remain owner acceptance checks. Android 11+ reserves typed system/caption/cutout/touch insets; older supported Android versions retain the established system-window fallback.

## Privacy and limits

The app has no accounts, Internet access, advertising, analytics or backend, and requests no Android permissions. Sensor samples are temporary. Preferences, stars, cosmetics, records and streaks stay on the device; app backup/device transfer are excluded. Result images use private cache and a read-only temporary share URI. Clearing app data or uninstalling removes local progress; copies deliberately shared to another app are controlled by that app.

Daily Drop resets at **00:00 UTC** (05:30 in India), uses local clock safeguards and has no server verification or global leaderboard. Date changes or cleared storage can defeat offline streak guarantees. Ads, revives, paid avatars, billing, donations and online services remain deferred. The [policy-site source](../policy-site/README.md) and store listing must match the final shipped behavior; building the app does not deploy the website.