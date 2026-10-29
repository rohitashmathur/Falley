# Falley development and builds

Current version: **4.4.1/code 18**. This revision builds debug only; release commands below are documented for a later authorized release. Artifacts stay local. Exact completed build/native checks are in [Build Status](../BUILD_STATUS.md); Firebase behavior and console steps are in [Firebase 4.4](FIREBASE_4_4.md).

## Prerequisites and configuration

Requires **JDK 17**, Android SDK **platform 36**, **build-tools 35.0.0**, Internet access for the first Gradle dependency download, and Android **7.0/API 24** or newer to run. The wrapper pins **Gradle 8.11.1** and **AGP 8.10.1**. Set `JAVA_HOME`/`ANDROID_HOME` or pass explicit paths. Run from the repository root.

Install the separately downloaded Android configuration files before building:

```powershell
.\scripts\configure-firebase.ps1 -DebugConfig 'C:\private-config\google-services-debug.json' -ProductionConfig 'C:\private-config\google-services-prod.json'
```

The helper validates package/client identity without printing file contents, then places ignored local JSON under `app/src/debug/` and `app/src/release/`. Debug and production are different Android registrations in the same Firebase project; use app/stream filters and `build_channel` to separate measurements. No service-account credentials belong in this project. Portable source CI does not invoke Gradle or need either JSON.

## Local debug APK

```powershell
.\scripts\test-expansion.ps1 -JdkRoot 'C:\path\to\jdk-17'
.\scripts\build-android.ps1 -SdkRoot 'C:\path\to\android-sdk' -JdkRoot 'C:\path\to\jdk-17' -OutputApk 'C:\local-delivery\Falley-4.4.1-debug.apk'
adb install -r 'C:\local-delivery\Falley-4.4.1-debug.apk'
adb shell am start -n com.perfgames.falley.motiontest/com.perfgames.ropelab.MainActivity
```

The debug script now uses Gradle and defaults to **4.4.1-beta-test/code 18**, package `com.perfgames.falley.motiontest`, label **Falley Motion Test**, without R8. It includes Firebase's dependency resources and classes. The existing ignored `build/debug.keystore` is used explicitly; preserve it for compatible updates. Do not uninstall merely to bypass a signing mismatch if progress matters. `test-physics.ps1` remains an entry point to the portable suite.

## Signed Play bundle and local release test APK

```powershell
.\scripts\build-release.ps1 -SdkRoot 'C:\path\to\android-sdk' -JdkRoot 'C:\path\to\jdk-17' -BundletoolJar 'C:\path\to\bundletool-all-1.18.3.jar' -OutputDirectory 'C:\local-delivery\Falley-4.4.1\release'
```

Reuse the existing upload identity; see [release signing](RELEASE_SIGNING.md). Do not initialize a replacement key for an existing app. Release uses R8 optimization/obfuscation, resource shrinking and SDK/app consumer rules. The script runs lint and bundle generation, validates production identity/version/SDK/allowed permissions, verifies signatures against the preserved upload certificate, and derives a bundle-based local install APK. A Play-installed APK may instead carry Google's app-signing certificate.

Keep **mapping.txt**, R8 reports, the Crashlytics runtime mapping ID and `release-verification.json` beside the exact artifacts. The AAB's embedded mapping must match the local mapping. Automatic Crashlytics mapping upload is disabled during ordinary builds; use the separate [exact-build upload route](FIREBASE_4_4.md#release-mapping). Artifact creation does not imply a Play upload, rollout or Firebase Console delivery.

## Native checks

Use an emulator, the installed debug app and explicit artifact folder:

```powershell
.\scripts\test-ui.ps1 -FirebaseOnly -AppPackage com.perfgames.falley.motiontest -Device emulator-5580 -Viewport small,tall -FontScale 1.3 -ArtifactRoot 'C:\local-delivery\verification\firebase'
.\scripts\test-ui.ps1 -PrivacyOnly -AppPackage com.perfgames.falley.motiontest -Device emulator-5580 -Viewport small,tall -FontScale 1.3 -ArtifactRoot 'C:\local-delivery\verification\privacy'
```

`-FirebaseOnly` checks the actual SDK, consent choices, UI/localization and event routing without an intentional crash. A debug-only controlled crash and dashboard receipt are separate verification steps. The runner snapshots/restores preferences and display/font settings and removes instrumentation. Keep generated logs local unless reviewed/redacted for public evidence. The release smoke uses public UI without retaining private implementation names for reflection.

For gameplay/layout regression, `-V43Only` and `-UxOnly` retain their explicit scopes. Supported viewport fixtures include `small,tall,tablet,xlarge,landscape,native`; `native` preserves a cold-boot AVD's physical display. Use a dedicated Pixel Tablet AVD for tablet checks: hot-resizing a phone can leave taskbar geometry inconsistent with insets. The tablet fixture checks actual display rotation, Pause and same-run preservation. Phones retain portrait presentation; displays of at least 600dp permit user rotation. Physical tablet/multi-window behavior remains an owner check.

Portable tests cover gameplay, steering, progression, saves, daily isolation, rewards, translations, reporting consent and event filtering. Native SDK checks do not prove network delivery, physical motion comfort, sound audibility, performance or TalkBack. Have fluent speakers review Hindi/Indonesian. Historical [4.3 evidence](../screenshots/v4.3/README.md) and older release receipts do not validate a 4.4 binary.

## Privacy and publication

Gameplay works offline; optional Firebase diagnostics/analytics can use the network after player choice. Both switches start off under Settings → Diagnostics & analytics. No accounts, ads or billing are added. Current collection, cache/timing and deletion limits are described in [Firebase 4.4](FIREBASE_4_4.md) and the [policy website source](../policy-site/README.md).

Preferences, stars, avatar/rope choices, records and streaks are held in app-private storage; backup/device transfer remain disabled. Sensor history and the temporary country-language hint are not sent in custom events. Daily result images use private cache and explicit read-only share URIs; receiving apps control shared copies. Clearing storage removes local progress, not reporting data already received by Google.

Daily resets at **00:00 UTC** (05:30 in India), depends on the device clock and has no server verification/global leaderboard. Ads, revives, billing, donations and online game services remain deferred. Before beta distribution, redeploy matching policy HTML and update Play Data safety; Git publication does not deploy Vercel or submit store declarations. APK/AAB/APKS, signing files, JSON configuration, mappings and generated build files remain local.
