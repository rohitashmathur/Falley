# Falley 4.1 native UX evidence

24 October 2026; debug **4.1.0-ux-test / code 13**, isolated package `com.perfgames.falley.motiontest`. APKs and signing material remain local. See [implementation notes](../../docs/UX_UPDATE_4_1.md).

## Current acceptance

Android 15/API 35; `small`: 480×800/density 240; `tall`: 720×1600/density 320; system font scale **1.3**; English, Hindi and Bahasa Indonesia.

| Layout | Logged checks | Captures |
| --- | ---: | ---: |
| small | 10,155 | 76 |
| tall | 10,155 | 76 |
| **Total** | **20,310** | **152** |

Navigation owners/Back, sequential locks, six chapter previews, all Collection categories, labeled Language, Help/Privacy, Touch/Motion cues, quick Retry, independent new/earned/saved stars, Continue frontier/mastery recommendation, Collection persistence/recreation, mirror practice and Daily/Arcade isolation pass. Daily checks intercept the Android share intent and verify the real generated PNG/provider without contacting a destination app. Original preferences/display/font settings were restored and the instrumentation app removed; the owned emulator was stopped after verification.

This directory contains **37 curated actual Android screenshots**, final per-layout receipts, the full native log, portable/focused logs, build log, APK identity/hash and exact app-source input hashes. Full 152-capture output is kept in the local delivery's `verification/android/`. Files marked `controlled` use intentional landing/death/progress fixtures; their scores/times are not natural playtest results. Fresh Home/chapter images show real lock state. All images are unedited compositor screenshots except `daily-share-card.png`, the actual app-generated share card.

The first native attempt exposed a harness ambiguity between the Collection heading and its same-named bottom tab; association now uses position and preserves full control/text bounds checks. A provisional passing run had stale compositor frames on a few chapter screenshots. The final run explicitly invalidates, awaits hardware frame commit when available and settles before capture. The corrected chapter pages were reviewed visually. Earlier attempts are excluded from current acceptance totals and curated images. No app change or APK rebuild was needed for these test-fixture corrections.

All **20 portable test groups pass**, including physics, all 24 Campaign courses in touch/motion controllers, six Daily templates, mechanisms/finales, jump/steering rules, migration, stars, cosmetics and translations. Final focused presentation checks pass **71,120 rules + 1,067 string checks** after the last copy adjustment. These focused assertions overlap the broad suite and are not added to a misleading unique-test total. Bounded controllers and native fixtures do not measure human success, listening quality, sensor comfort or retention.

## Reproduce

Use `scripts/test-expansion.ps1`, build the isolated debug APK using the README command, then `scripts/test-ui.ps1 -UxOnly -AppPackage com.perfgames.falley.motiontest -Device emulator-5580 -FontScale 1.3` on a disposable emulator with SDK platform 35 available. The runner rejects physical devices.

Physical-phone controls, audio/haptics, performance, real TalkBack navigation and native-speaker review remain owner playtests. No release build, release lint, AAB, store screenshot refresh, website deployment or Play upload was performed.
