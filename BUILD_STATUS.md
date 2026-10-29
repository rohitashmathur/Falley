# Falley build status

**4.4.1 / code 18**, prepared 29 October 2026. Default-on Firebase reporting and detailed gameplay input analysis. **Debug only** for owner testing; no new release APK/AAB or mapping upload.

## Local debug

`Falley-4.4.1-debug.apk` — `7,016,177` bytes, package `com.perfgames.falley.motiontest`, label Falley Motion Test. Existing debug signing identity preserved; APK v2 verification passes.

SHA256: `a001c515f49ea5b60c89ad8c59918a779b3dfdaf4ada9af9e747f5a870726c20`

## Implemented

- Crash reports and Gameplay analytics default ON in shared debug/release source when no choice is saved. Existing explicit opt-outs remain OFF. Independent Settings switches, translated first-launch notice and updated EN/HI/ID privacy policy.
- Individual jump attempts, completed swipes, menu transitions, saved language changes, earned avatar unlocks, actual rope support/cuts and Share Result taps.
- Unfiltered accelerometer/gyro samples at up to 2 Hz per sensor during motion gameplay; primary-touch move coordinates at up to 10 Hz plus boundaries. No paused/background/other-app input capture.
- Ten samples per input event, 192-event memory queue, low-priority worker handoff about every 15 seconds/on pause/end. Firebase handles offline persistence/retries after handoff. Opt-out purges pending batches; overflow/process death can lose samples.
- Updated HTML/Vercel policy project and a private-export JSON-to-CSV decoder. [Event schema and analysis instructions](docs/FIREBASE_4_4.md).

## Validation

- 37 portable Java groups pass, including 631 buffer checks, 1,404 settings checks and 17,475 event-schema checks; reference campaign simulations verify rope callbacks do not alter physics.
- Storage/reward checks 704/32 pass; 4 synthetic export-decoder tests pass; 48 HTML/app content/link checks pass.
- Android API35 native: Firebase small 779/tall 775 checks, 8 captures each; privacy small 2,752 checks, 12 captures. Tests cover actual SDK switches, worker handoff, gameplay/reward/share hooks, opt-out, long translated policy and accessible first-launch/settings layouts. Original emulator preferences/display/font restored.
- Debug lint: 0 errors/13 reviewed existing warnings. No new permissions or SDK versions added. [Evidence](screenshots/v4.4.1/README.md).

## Remaining owner steps

Install the local debug update without uninstalling. If you previously disabled reporting, enable it once in Settings for this test. Confirm events in the Motion Test stream; raw-input analysis benefits from BigQuery export and the supplied decoder. Console ingestion, BigQuery linking/retention/billing, physical device comfort/battery/data-use and fluent-language acceptance are not certified here.

Redeploy `policy-site/` and review Play Data safety and the legal basis for worldwide default-on input collection before public distribution. This change does not claim regulatory approval. No Vercel or Play publication was performed. [Historical 4.4.0 release checks](docs/history/BUILD_STATUS_4_4_0.md) are separate from this debug build.
