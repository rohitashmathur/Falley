# Falley 4.2 debug verification evidence

25 October 2026. App source commit **`2c309f1cbd12bfaa4755b6b98ad4d69d277a412d`**; debug **4.2.0-beta-test / code 14**, isolated package **com.perfgames.falley.motiontest**. The APK, release artifacts, mappings and signing material stay local. [4.2 notes](../../docs/BETA_4_2.md) explain the implementation and separate release acceptance.

## Exact debug artifact

| Item | Value |
| --- | --- |
| Local filename | `Falley-4.2.0-debug.apk` |
| Bytes | 1,103,387 |
| SHA-256 | `dc4f949497b41433bee0cb52f11710a58fc3fad49021e447ad56cc4b158442ec` |
| Build/signature | Build PASS; APK v2/v3 verification PASS |
| Production identity | Separate `com.perfgames.falley`, 4.2.0/code 14 |

## Native debug acceptance

Android 15/API 35; font scale **1.3**; English, Hindi and Bahasa Indonesia.

| Layout | Display / density | Logged checks | Native captures |
| --- | --- | ---: | ---: |
| Small | 480×800 / 240 | 10,107 | 83 |
| Tall | 720×1600 / 320 | 10,107 | 83 |
| **Total** | | **20,214** | **166** |

Counts are the actual final instrumentation receipts, including **84 prior-artifact cleanup assertions per layout**. They are not a count of unique product behaviors. The passing run checks first-run language confirmation and permission-free country fallback, retained language migration, the two-tab Collection, earned rope unlocks and real support effects, Classic isolation in Daily/Arcade, independent music/effects settings and music lifecycle, Home/briefing/control/navigation flows, compact Retry, independent star evidence, cumulative mastery and local persistence. Daily result sharing is intercepted before any destination app opens; no external message or share is sent.

Original app preferences were restored exactly; the runner also restores display/font settings and removes its instrumentation app. The separate recovery receipt passes. The final release cleanup receipt below confirms both owned emulators were stopped after verified recovery.

## Curated unedited captures

These **18 PNGs** are copies of actual native Android compositor screenshots from the final passing debug run. Capture synchronization invalidates the view, awaits a hardware frame commit when available and lets the compositor settle. The copied bytes were verified with SHA-256. No screenshot was cropped, recolored or redrawn. `DEBUG_VERIFICATION.json` records each original relative path, image hash and size.

| File | What it shows |
| --- | --- |
| `first-language-en-small.png` | Fresh English default, three options, explicit Continue and emulator region hint |
| `first-language-hi-small.png` | Fresh Hindi choice and localized confirmation |
| `first-language-id-small.png` | Fresh Bahasa Indonesia choice |
| `home-tall.png` | Campaign spotlight, progress, reward distance and three root destinations |
| `collection-avatars-tall.png` | Selected avatar preview, chapter locks and focused tabs |
| `ropes-spring-six-stars-small-controlled.png` | Spring's six-star unlock in a controlled profile |
| `ropes-static-twelve-stars-tall-controlled.png` | Static's twelve-star unlock in a controlled profile |
| `briefing-en-tall.png` | Optional star goals, Touch/Motion and tucked-away practice options |
| `briefing-hi-small.png` | Hindi briefing layout on the smaller display |
| `settings-en-tall.png` | Independent music/effects preferences and discoverable Language/Help/Privacy |
| `settings-id-small.png` | Indonesian Settings on the smaller display |
| `result-finish-new-tall-controlled.png` | Singular new-star headline and named independent objectives |
| `result-gold-new-par-saved-small-controlled.png` | New gold-centre evidence with a previously saved jump-par star |
| `retry-tall-controlled.png` | Compact cause-specific death result and direct Retry |
| `touch-cue-small.png` | First-stage touch instruction |
| `motion-cue-tall.png` | First-stage motion instruction |
| `privacy-tall.png` | Updated offline policy including country/language disclosure |
| `arcade-small.png` | Configurable survival setup |

The `controlled` files use explicit test unlock, support, landing or death fixtures. Their scores, times and earned progress are not natural player results. Country hints shown in these images come from the disposable emulator and are not evidence of a physical player's country. The full 166-capture output remains in the local verification delivery.

## Receipts and limits

- `debug-small.txt`, `debug-tall.txt`, `debug-recovery.txt`: exact final instrumentation summaries.
- `debug-native-log.txt`: passing native runner log with personal paths replaced by portable placeholders.
- `debug-build-log.txt`: matching final debug build/signature output, similarly sanitized.
- `DEBUG_VERIFICATION.json`: source revision, APK identity, layout counts, selected-image provenance and hashes.

This debug proof does **not** validate the optimized production AAB/APK. Release R8/mapping/signature checks and reflection-free production smoke are separately recorded below. Consult the current [Build Status](../../BUILD_STATUS.md) for final release results. Human phone checks remain necessary for sensor comfort, sound/music audibility and balance, haptics, difficulty, sustained performance, TalkBack and native-speaker wording. No Play upload, rollout or policy website deployment is implied by these receipts.
## Optimized production evidence

`RELEASE_VERIFICATION.json` identifies the exact signed AAB/derived APK/mapping and app source revision. `release-build-log.txt`, `release-lint.txt`, `release-api35-log.txt`, `release-api36-log.txt` and each layout/recovery receipt are sanitized copies of the final successful run. Personal absolute paths are replaced; checks and counts are unchanged. Raw originals and exact binary/mapping/R8 reports stay local. `emulator-cleanup.json` records restored display/font, absent test harnesses and shutdown of both owned emulators.

The reflection-free optimized release smoke passes on Android 15 and Android 16, small/tall at font scale 1.3: **850 checks including two recovery checks / 48 captures**. Six `release-api*` PNGs are unedited compositor screenshots from those exact runs, with source paths and SHA-256 in the receipt. They show language choice, rope Collection, Settings, Home, touch gameplay and Hindi. Unlike debug unlock/result fixtures, these production captures use public view/accessibility and pointer paths. Neither evidence set proves physical-phone sensor comfort, listening quality, sustained performance or full accessibility.

`portable-log.txt` is the full 26-group portable run before the final singular-star wording correction. `release-strings-log.txt` is the final focused 8,397-check translation proof; `media-log.txt` records the exact PCM and 56 music/controller checks. The final debug and production APK checks include that corrected wording.
