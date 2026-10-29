# Firebase in Falley 4.4

Version **4.4.1/code 18** adds Firebase Crashlytics and Google Analytics for Firebase. It does not change game physics, add accounts, advertising, purchases or a game backend. Build and test results are recorded in [Build Status](../BUILD_STATUS.md); this document describes the implementation and remaining console work.

## Player choice

Open **Settings → Diagnostics & analytics**. **Crash reports** and **Gameplay analytics** are independent switches, both on by default when no preference is saved, in debug and release. A saved explicit opt-out is preserved. First-launch language selection explains reporting; neither service is needed to play.

Firebase auto-initialization providers are removed. If both saved choices are off at startup, Falley leaves Firebase uninitialized. Enabling either choice initializes the SDK with Analytics/ad-storage/ad-user-data/ad-personalization consent denied by default; Analytics storage is granted when Gameplay analytics is on (including the initial default). Advertising ID, SSAID and automatic screen reporting are disabled. Advertising-ID and Ad Services permissions are removed from the merged app.

Analytics collection changes apply immediately; disabling it purges unsent in-memory batches, invalidates a worker batch already being drained, and asks the SDK to reset local Analytics data. Crashlytics uses its documented collection controls: changes fully apply on the next app launch. Later crash opt-in can include reports stored while Firebase was previously active, and already queued/in-flight uploads may finish after opt-out. Disabling either option or clearing app storage does not delete information already received by Google.

Choices are stored in the existing private profile preferences. A failed preference save does not enable the service. Reporting failures do not block play or saving progress.

## Reporting schema

Custom events use a closed vocabulary rather than arbitrary text:

| Event | Parameters |
| --- | --- |
| `falley_run_start` | `mode` (campaign/practice/arcade/daily), campaign/practice `stage` (1–24), Arcade `difficulty`, selected `avatar`, `control` (touch/motion) |
| `falley_run_end` | Start fields plus `result` (win/loss/abandon), bounded `wave` and `jumps`, `time_bucket`, and a fixed `hazard` category |

`build_channel` distinguishes debug and release, including default Analytics event parameters and Crashlytics context. A run's end is sent only if its start was logged while Analytics was enabled; enabling Analytics during a run does not retrospectively log that run. Explicit abandonment closes the old run before the next reset. Crash context contains the same limited game-state vocabulary, with stale values reset at a new run.

The SDK can also collect standard app/session events, installation/source attribution, app/device/OS/language details, app-instance/installation identifiers and approximate geography. There is no custom name, email, account ID, full save upload or Daily date/seed. Sampled raw sensors and touch coordinates ARE included while Gameplay analytics is on. Standard SDK identifiers are still data collection and must be included in the privacy/Data safety review.


## Detailed tracking in 4.4.1

All events use a fixed name and include `build_channel`. Run-related events include the initial run context and a random, per-run `run_id` (not a persistent user ID).

| Event | Trigger and extra fields |
| --- | --- |
| `falley_jump` | Every gameplay jump attempt; `accepted`, `jumps_used`, elapsed `time_ms`, avatar `x/y` in world units |
| `falley_swipe` | Completed steering gesture; logical arena `x0/y0/x1/y1`, `duration_ms`, direction, time |
| `falley_input_batch` | Up to 10 samples in `s0`…`s9`; `schema=1`, `stream`, `sample_count`, run context |
| `falley_navigation` | Screen changes only, not every frame; `from_screen`, `to_screen` |
| `falley_language_change` | Saved language change; `from_language`, `to_language` (`en`, `hi`, `id`) |
| `falley_avatar_unlock` | New earned avatar after a successful progress save; `unlocked_avatar` |
| `falley_rope` | Actual support bounce or cut; `rope_index` is zero-based course identity, `rope_type`, `encounter`, time |
| `falley_share` | Daily Share Result tap; does not prove sending or record image/recipient/destination |
| `falley_buffer_overflow` | Count of observations dropped because of queue/encoding limits |

The batch streams are `accel`, `gyro` and `touch`. Sensor rows are `milliseconds,x,y,z,rotation`: original unfiltered Android floats, natural device axes, acceleration m/s² including gravity, angular velocity rad/s, rotation enum 0/1/2/3. Each sensor is sampled at most 2 Hz during active motion gameplay. Touch rows are `milliseconds,phase,x_px,y_px,width_px,height_px`, relative to the app View; phases 0 down / 1 move / 2 up / 3 cancel. Primary-pointer moves are sampled at most 10 Hz, with press/release/cancel boundaries also included. No collection from other apps, keyboards or paused/background sensors. Run time uses monotonic elapsed time and includes pauses; run-end duration buckets retain the simulation's gameplay time.

A low-priority HandlerThread hands events to the Firebase SDK after about 15 seconds, on run end or on Activity pause. A bounded memory queue holds 192 events plus up to nine partial samples per stream. Overflow drops oldest queued events and emits a count. Process death can lose not-yet-handed-off data. This is best-effort diagnostic sampling, not a lossless replay. Firebase handles persistent offline batching, retries and real network timing; a 15-second handoff does not promise a 15-second upload. Opt-out cannot retract data already handed to Firebase or received by Google. When both services are enabled, Analytics breadcrumbs (including input event parameters) can appear in Crashlytics logs.

## Analysing the input samples

Use the **Motion Test** app stream and filter `build_channel=debug`. The SDK package suffix does not itself enable GA DebugView; temporarily enable Firebase debug logging for that package when inspecting live events and turn it off afterwards. That is separate from ordinary Analytics collection.

Register useful event-scoped dimensions such as `mode`, `stage`, `avatar`, `control`, `result`, `difficulty`, `rope_type`, `encounter` and `to_screen` for reports. Do not register packed samples or random run IDs as dashboard dimensions. For raw input analysis, link the property's BigQuery export, query `falley_input_batch` and export event rows as JSON/NDJSON. `scripts/decode-analytics-input.py` expands those samples to CSV, without exporting `user_pseudo_id`. Keep exports in ignored `analytics-data/` or outside the repo. BigQuery linking, retention and any billing configuration remain owner actions; the app does not create them. See [BigQuery export](https://support.google.com/analytics/answer/9358801) and [schema](https://support.google.com/analytics/answer/7029846).

```
python scripts/decode-analytics-input.py --input analytics-data/events.json --output analytics-data/inputs.csv
```

Only debug is built for 4.4.1. The default-on behavior is shared source for both variants; a new release artifact and its mapping upload have NOT been prepared. Prior 4.4.0 release validation is historical.

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

In the correct Firebase/Analytics app registration, confirm Analytics integration, inspect DebugView with Analytics enabled, and confirm a controlled debug test-crash report after crash opt-in/relaunch. The intentional crash hook is debug-only and is removed from optimized release code; no production crash button is added. Local SDK event submission or a successful build does not establish dashboard receipt.

Before beta distribution, redeploy the [4.4.1 policy page](../policy-site/README.md), update Play Data safety for optional diagnostics/interactions/identifiers/approximate geography as applicable, review the selected Analytics retention, project access and Firebase data-sharing settings, and keep advertising integrations off. The policy guide lists the normal networking/background/install-referrer permissions and provider disclosures. Play upload, website deployment and console changes are separate owner actions unless explicitly recorded as completed.

## Primary references

- [Firebase Android setup](https://firebase.google.com/docs/android/setup)
- [Crashlytics Android setup](https://firebase.google.com/docs/crashlytics/android/get-started)
- [Crashlytics collection controls](https://firebase.google.com/docs/crashlytics/android/customize-crash-reports)
- [Analytics collection controls](https://firebase.google.com/docs/analytics/android/configure-data-collection)
- [SDK data disclosures](https://firebase.google.com/docs/android/play-data-disclosure)
