# Firebase in Falley 4.4

Version **4.4.0/code 17** adds Firebase Crashlytics and Google Analytics for Firebase. It does not change game physics, add accounts, advertising, purchases or a game backend. Build and test results are recorded in [Build Status](../BUILD_STATUS.md); this document describes the implementation and remaining console work.

## Player choice

Open **Settings → Diagnostics & analytics**. **Crash reports** and **Gameplay analytics** are independent switches, both off by default. No reporting prompt appears on first launch and neither option is needed to play.

Firebase auto-initialization providers are removed. If both saved choices are off at startup, Falley leaves Firebase uninitialized. Enabling either choice initializes the SDK with Analytics/ad-storage/ad-user-data/ad-personalization consent denied by default; only Analytics storage is granted when Gameplay analytics is on. Advertising ID, SSAID and automatic screen reporting are disabled. Advertising-ID and Ad Services permissions are removed from the merged app.

Analytics collection changes apply immediately; disabling it asks the SDK to reset local Analytics data. Crashlytics uses its documented collection controls: changes fully apply on the next app launch. Later crash opt-in can include reports stored while Firebase was previously active, and already queued/in-flight uploads may finish after opt-out. Disabling either option or clearing app storage does not delete information already received by Google.

Choices are stored in the existing private profile preferences. A failed preference save does not enable the service. Reporting failures do not block play or saving progress.

## Reporting schema

Custom events use a closed vocabulary rather than arbitrary text:

| Event | Parameters |
| --- | --- |
| `falley_run_start` | `mode` (campaign/practice/arcade/daily), campaign/practice `stage` (1–24), Arcade `difficulty`, selected `avatar`, `control` (touch/motion) |
| `falley_run_end` | Start fields plus `result` (win/loss/abandon), bounded `wave` and `jumps`, `time_bucket`, and a fixed `hazard` category |

`build_channel` distinguishes debug and release, including default Analytics event parameters and Crashlytics context. A run's end is sent only if its start was logged while Analytics was enabled; enabling Analytics during a run does not retrospectively log that run. Explicit abandonment closes the old run before the next reset. Crash context contains the same limited game-state vocabulary, with stale values reset at a new run.

The SDK can also collect standard app/session events, installation/source attribution, app/device/OS/language details, app-instance/installation identifiers and approximate geography. There is no custom name, email, account ID, raw sensor data, touch-coordinate history, full save upload or Daily date/seed in Falley's events. Standard SDK identifiers are still data collection and must be included in the privacy/Data safety review.

## Local configuration and variant separation

The supplied files represent **two Android app registrations in the same Firebase project**. They do not provide isolated projects. Debug uses `com.perfgames.falley.motiontest`; release uses `com.perfgames.falley`. Filter Analytics/Crashlytics by the correct app registration/stream and `build_channel`; confirm stream setup in the console rather than mixing test and production measurements.

```powershell
.\scripts\configure-firebase.ps1 -DebugConfig 'C:\private-config\google-services-debug.json' -ProductionConfig 'C:\private-config\google-services-prod.json'
```

The helper validates both Android clients before copying either file to ignored local paths:

- `app/src/debug/google-services.json`
- `app/src/release/google-services.json`

Missing or mismatched configuration fails the relevant build with contents suppressed. Do not substitute a service-account private key. Client configuration must be packaged into the app for Firebase to work; keeping JSON out of Git does not turn it into a secret inside an APK. Service-account credentials, signing material, JSON, generated resources, APK/AAB/APKS and mapping files remain outside Git.

The pinned integration uses Firebase BoM **34.19.0** (Analytics **23.2.0**, Crashlytics **20.1.1**), Google Services plugin **4.5.0** and Crashlytics plugin **3.0.8**, with existing AGP **8.10.1**, Gradle **8.11.1**, SDK **36** and JDK **17**. Debug now uses Gradle so dependency resources/classes are included, while preserving `build/debug.keystore`.

## Release mapping

Ordinary builds disable automatic Crashlytics mapping upload. The release route retains the mapping, nonblank runtime mapping ID, configuration digest and exact bundle receipts locally; the embedded bundle mapping must match. To upload that exact validated release mapping separately:

```powershell
.\scripts\upload-crashlytics-mapping.ps1 -ReleaseDirectory 'C:\local-delivery\Falley-4.4.0\release' -SdkRoot 'C:\path\to\android-sdk' -JdkRoot 'C:\path\to\jdk-17'
```

The helper verifies saved/current mapping, mapping ID, production configuration and bundle hashes, and the Gradle upload action guards them again immediately before sending. It must not regenerate the mapping ID or rebuild a different release. A successful upload-task receipt is distinct from seeing a symbolicated crash in Firebase Console. Never upload debug-key artifacts to Play or create a replacement production upload key.

## Verification and owner console work

`test-ui.ps1 -FirebaseOnly` checks the actual SDK initialization boundary, independent durable choices, localized settings, event routing and installed permissions. It does not deliberately crash the app. `-PrivacyOnly` checks policy copy/layout and contact handoff. Exact completed devices/results belong in [Build Status](../BUILD_STATUS.md). Portable CI tests the consent controller, event schema, privacy/translations, gameplay, storage and audio without loading Firebase SDKs or requiring configuration.

In the correct Firebase/Analytics app registration, confirm Analytics integration, inspect DebugView after explicit Analytics opt-in, and confirm a controlled debug test-crash report after crash opt-in/relaunch. The intentional crash hook is debug-only and is removed from optimized release code; no production crash button is added. Local SDK event submission or a successful build does not establish dashboard receipt.

Before beta distribution, redeploy the [4.4 policy page](../policy-site/README.md), update Play Data safety for optional diagnostics/interactions/identifiers/approximate geography as applicable, review the selected Analytics retention, project access and Firebase data-sharing settings, and keep advertising integrations off. The policy guide lists the normal networking/background/install-referrer permissions and provider disclosures. Play upload, website deployment and console changes are separate owner actions unless explicitly recorded as completed.

## Primary references

- [Firebase Android setup](https://firebase.google.com/docs/android/setup)
- [Crashlytics Android setup](https://firebase.google.com/docs/crashlytics/android/get-started)
- [Crashlytics collection controls](https://firebase.google.com/docs/crashlytics/android/customize-crash-reports)
- [Analytics collection controls](https://firebase.google.com/docs/analytics/android/configure-data-collection)
- [SDK data disclosures](https://firebase.google.com/docs/android/play-data-disclosure)
