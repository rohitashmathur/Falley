# Falley build status

**4.3.0-beta-test/code 16 — local debug testing**, prepared 25 October 2026. Package: `com.perfgames.falley.motiontest`. The [application input hashes](screenshots/v4.3/SOURCE_HASHES.json) identify the built source; later test/documentation edits do not alter the APK.

## Delivered

- Stronger alternating motion breeze/nudge, with a clear HUD cue and calmer landing approach.
- Meaningful gear-grid/blade-rig attacks in the first two finales; side-mounted Demon/Robot casters in later finales, driven by actual warning/firing state.
- Seven real avatar ability profiles and original stories. Campaign/Arcade use selected traits; Daily retains standard Human physics for fairness. Every boundary, spike, ceiling and fatal floor remains lethal.
- One Home Campaign action opens combined chapter/stage selection and briefing. Locks, replay, recommended stage and Result navigation are covered; no extra Home buttons.
- Separate card icon/text rows, safe-height menus, width caps, bottom tabs and real tablet orientation/resize handling.

## Local build

Portable example: `C:\local-delivery\2026-10-25\Falley-4.3\Falley-4.3-debug.apk`, **1,132,474 bytes**. SHA-256: `a08391a972b64729b7ec474a19a11819c32646a49a3459eb1f8716f63d9db377`.

Compilation, min24/target36, package/version, zero requested permissions and preserved v2/v3 debug signing identity **PASS**. Source/tests/docs/evidence may be public; APK/AAB/APKS, keys, mappings and generated intermediates remain local. **No release APK/AAB, release R8/lint verification, Play upload, Firebase integration or Vercel deployment was performed.**

## Completed checks

- **33 portable groups PASS**: 336 avatar/Campaign/control cases, 14 avatar/Arcade cases, delayed boss dodges, exact boss collision boundaries, mixed ropes, motion bounds, menus/progression and 20,562 release-language checks.
- **704 reward-model/32 durable-storage checks**, **56 music lifecycle checks** and published 4.2 Daily compatibility (**48,471,122 bit-exact float checks**) PASS.
- Final-APK native results at font 1.3, EN/HI/ID:

| Run | Checks | Captures |
| --- | ---: | ---: |
| API 35-small-v43 | 4,031 | 45 |
| API 35-tall-v43 | 4,031 | 45 |
| API 35-small-ux | 12,083 | 92 |
| API 35-tall-ux | 12,083 | 92 |
| API 25-small-v43 | 3,940 | 45 |
| API 36-tablet-v43 | 4,635 | 52 |

The native API 36 evidence includes actual 2560x1600→1600x2560 tablet display rotation and same-run pause/state preservation. Native fixtures restore preferences/display/font/rotation and remove instrumentation. Owned emulators are stopped after verification. Screenshots, receipts and limits: [4.3 evidence](screenshots/v4.3/README.md).

## Remaining acceptance

Install over the existing Motion Test app without uninstalling. Test physical Redmi/tablet motion comfort, avatar/rope balance, audio audibility, reachability, performance, TalkBack and fluent Hindi/Indonesian copy. Automated fixtures do not certify every Android version or physical device. See the [4.3 checklist](docs/BETA_4_3.md).

Firebase Crashlytics/Analytics remain **4.4** work after owner configuration. Redeploy matching [privacy website source](policy-site/) before beta distribution; a Git push does not deploy Vercel. Existing store screenshots are historical 4.2.1 assets and are not new submission evidence.

[Development](docs/DEVELOPMENT.md) · [Previous 4.2.1 status](docs/history/BUILD_STATUS_4_2_1.md) · [Older history](docs/history/BUILD_STATUS_THROUGH_4_2.md)
