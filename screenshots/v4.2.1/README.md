# Falley 4.2.1 debug verification evidence

25 October 2026. **4.2.1-beta-test / code 15**, isolated package **com.perfgames.falley.motiontest**. These receipts describe the current debug APK. No new release APK/AAB was built, and no store submission or website deployment is implied. The previous 4.2 production receipts remain historical. [4.2.1 notes](../../docs/BETA_4_2_1.md) describe the implementation and phone checklist.

## Exact artifact and source

| Item | Value |
| --- | --- |
| Local filename | `Falley-4.2.1-debug.apk` |
| Bytes | 1,120,186 |
| SHA-256 | `487e5304c912ef4a061594b9357e2727c5e524a9d3af856c0b745009faef2f24` |
| Build/signature | Debug build PASS; APK v2/v3 verification PASS |
| Repository binary inclusion | None; APK, generated files and signing material remain local |

`SOURCE_HASHES.json` records the 59 frozen application/build input files and a deterministic combined SHA-256. `DEBUG_VERIFICATION.json` ties that receipt to the tested APK and native screenshots. Application/build source is recorded at `09108128247ec41bc9d0ef3cf8b09419778e0e59`. A class-header comment was corrected after compile; `COMMENT_PROOF.json` proves its regenerated class is byte-identical to the tested compilation stage. Subsequent documentation or harness-only changes do not change the APK. This manifest is an input identity receipt, not a claim that independent builds produce byte-identical APKs.

## Current Android 15 acceptance

API 35; font scale **1.3**; English, Hindi and Bahasa Indonesia.

| Layout | Display / density | Logged checks | Native captures |
| --- | --- | ---: | ---: |
| Small | 480×800 / 240 | 11,065 | 92 |
| Tall / supplied phone dimensions | 720×1600 / 320 | 11,086 | 92 |
| **Total** | | **22,151** | **184** |

These counts include artifact cleanup and harness assertions, rather than counting unique product behaviors. Both complete runs pass. The 720×1600 layout matches the supplied Redmi screenshot dimensions; an emulator does not establish physical Redmi performance or sensor feel. Android 7.1/API 25 compatibility is verified separately when its final receipt is available and is not claimed by this API 35 result.

The checks exercise first-run language and retained-language migration, permission-free local country fallback, root navigation spacing, the two-tab Collection and informational rope guide, difficulty/hazard cards, the last-hazard guard and direct Touch/Motion selection. Campaign fixtures cover independent star evidence, Continue/replay, quick Retry, actual Classic/Spring/Static rope support and newly recycled Arcade Wave 2 ropes. Daily records/sharing remain isolated; the share chooser is intercepted before a destination app opens, so no external message is sent. Music/effects lifecycle is checked through the native audio controller; listening quality is not assessed.

Avatar checks use a real native finale landing after explicit prerequisite-win fixtures: Result Continue opens the new reward, Use equips Demon, and the next Angel offer compares against that selected Demon in all three languages. Keep and system Back preserve current equipment, acknowledgment survives saved Activity recreation/relaunch, a language-first cold start retains its offer, replays stay quiet, and old earned profiles avoid a reward cascade. Portable storage tests separately simulate recreation from durable preference snapshots and save-failure rollback; the native suite does not kill its own instrumentation process.

Original preferences were restored exactly. The runner restored display/font settings and removed its instrumentation package; `debug-recovery.txt` passes. Final emulator shutdown, any additional platform result and publication status belong to the current [Build Status](../../BUILD_STATUS.md).

## Curated unedited captures

These **18 PNGs** are byte-identical copies of actual Android compositor screenshots from the final passing API 35 runs. The harness invalidates the native view, awaits a hardware frame commit where supported and lets the compositor settle. Screenshots were not cropped, recolored or redrawn. Each original relative source, size and SHA-256 is recorded in `DEBUG_VERIFICATION.json`.

| File | What it shows |
| --- | --- |
| `first-language-en-small.png` | Fresh English default and explicit confirmation |
| `first-language-hi-small.png` | Hindi language choice |
| `first-language-id-small.png` | Bahasa Indonesia language choice |
| `home-tall.png` | Campaign spotlight and reserved root navigation |
| `collection-avatars-tall.png` | Selected-avatar preview and earned chapter locks |
| `rope-guide-hi-small.png` | Hindi rope guide, without a global equip control |
| `briefing-tall.png` | Campaign briefing, star goals and control choices |
| `settings-id-small.png` | Indonesian settings with navigation spacing |
| `reward-result-tall-controlled.png` | Actual finale landing fixture and explicit reward Continue |
| `unlock-demon-small-controlled.png` | New Demon versus selected Human |
| `unlock-angel-en-tall-controlled.png` | New Angel versus currently selected Demon |
| `unlock-angel-hi-small-controlled.png` | Hindi version of the same current-equipment offer |
| `unlock-angel-id-tall-controlled.png` | Indonesian version of the same offer |
| `arcade-touch-small.png` | Difficulty/hazard cards with Touch controls |
| `arcade-motion-id-tall.png` | Indonesian Arcade with Motion selected |
| `spring-support-tall-controlled.png` | Actual individual Spring support and HUD feedback |
| `static-support-small-controlled.png` | Actual individual Static support and charged-jump HUD |
| `retry-tall-controlled.png` | Compact death result and direct Retry |

`controlled` captures contain explicit prerequisite-win, support, landing or death fixtures. Their progress, times and scores are not natural player results. The language screen's country hint comes from the disposable emulator; it does not identify a physical player's country. Owner-supplied screenshots and artwork references are not included in this evidence set.

## Logs and limits

- `debug-small.txt`, `debug-tall.txt`, `debug-recovery.txt`: exact successful instrumentation summaries.
- `debug-native-log.txt`, `debug-build-log.txt`: final successful API 35 runner and matching APK build/signature output.
- `portable-log.txt`, `reward-storage-log.txt`, `media-log.txt`: portable regression, 704 avatar-model plus 32 durable-storage assertions, and audio/controller proof.
- `SOURCE_HASHES.json`, `DEBUG_VERIFICATION.json`: application input identity, APK identity and per-image provenance.

Copied text logs replace personal absolute paths with portable placeholders; results and counts are unchanged. Raw logs, all 184 native screenshots and the debug APK remain local. These fixtures do not validate an optimized production artifact, every random run, arbitrary font sizes or full accessibility. Physical-phone sensor comfort, sound/music audibility and balance, haptics, sustained performance, difficulty, TalkBack and fluent-speaker wording remain owner acceptance checks.

## Older Android acceptance

The same final APK passes the complete focused UX run on **Android 7.1/API 25**, 480×800/density240/font 1.3: **11000 checks / 92 captures**. `api25-native-log.txt` and the per-run receipts preserve the final result. Three `api25-*` PNGs are unedited examples from that run. This covers real legacy Back and audio-focus/background/resume behavior. The old Android ActivityMonitor blocks an actual chooser attempt and independently checks the generated PNG provider; deep SEND payload/grant checks are provided by API 35, not invented on the old API. Preferences/display/font are restored, both test harnesses removed and both owned emulators stopped (`emulator-cleanup.json`). It remains an emulator check rather than a low-end physical-phone test.

Portable proof is in `portable-log.txt` (29 groups), `reward-storage-log.txt` (704 model/ 32 persistence checks), `media-log.txt` (56 checks), and `daily-compatibility-log.txt` (48,471,122 bit-identical float comparisons against published 4.2).
