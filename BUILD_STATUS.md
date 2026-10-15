# Falley V3 build status

Verified portable source and native debug APK on 15 October 2026. Source revision: 3.0.0 / code 5, application ID com.perfgames.falley. Minimum API 24; compile/target API 36. APKs remain local.

- Campaign physics: PASS, 20940137 checks including all six real course wins, drift/countersteering/release, swept hazards, forgiving/strict floor behavior and 100000 seeded steps.
- Six campaign finishes: 10.12, 11.15, 12.20, 13.26, 14.29 and 15.35 seconds; bounded test controller times, not human playtest results.
- Arcade physics: PASS, 20800408 checks including configuration/mode reset, known swept/cap geometry, three beam death reasons/near misses, warning/refuge/direction-lock scenarios for all four families at all difficulties, no idle campaign clears and 100000 survival stability steps.
- Natural mixed Arcade attacks: test controller survives 25 seconds/wave 2 at Chill, Standard and Brutal, score 2920 each; no campaign floor/win.
- Touch policy: PASS, 16 cases. Motion filtering: PASS, 17 cases.
- Profile/preferences: PASS, 31 cases including blood, mask/difficulty sanitization, independent survival records and campaign isolation.
- Complete portable suite log: screenshots/v3/physics-suite.log (raw local copy: build/physics-v3.log).
- Final local APK: Falley-debug.apk (45600 bytes), version 3.0.0 / code 5; portable JDK 17/platform 36/build-tools 35.0.0 build and v2/v3 signature verification PASS.
- SHA-256: 581143CC411FA44128683A525CA20E3F89D66EF9F947FAF642F2C7EEF3539CD4
- Native V3 Android 15/API 35: PASS on 480x800 @ density 240 (650 checks, 51 captures) and 720x1600 @ density 320 (646 checks, 51 captures). Full-suite total: 1296 checks, 102 captures.
- Native V3 Android 16/API 36: PASS on 480x800 @ density 240 and 720x1600 @ density 320 (652 checks, 53 captures each). Full-suite total: 1304 checks, 106 captures.
- All four full native suites: PASS, 2600 logged checks and 208 captures, using the same APK checksum above. Coverage includes actual swipe/countersteering/release input, system Back and pause, settings/blood/preset persistence after recreation, campaign records and strict floor misses, Arcade setup/last-hazard guard/separate records, warning-safe/live hazards and grazing swept collisions.
- Additional focused API 35 effects suites: PASS, 173 checks and 15 captures per layout (346 checks, 30 captures total), covering partial side beams, diagonal lightning and late fire/lightning ash. Raw local evidence: build/device/v3-effects/api35.
- Full raw native evidence: build/device/v3/{api35,api36}/{small,tall}. Curated PNGs and four full-suite/two focused-suite logs are under screenshots/v3/{api35,api36}. Preferences and emulator display settings were restored; the test APK was removed.
- Controlled target/hazard states and the 40-second Arcade fixture are staged proof, not natural phone play sessions. Focused effects suites supplement the full suites and do not repeat unrelated flows.

Toolchain: JDK 17, SDK platform 36/build-tools 35.0.0 for app builds; platform 35 additionally supplies the native harness compilation jar. Ignored build/debug.keystore is preserved.

Physical-phone feel/fun, broader hardware coverage, Gradle verification, production release AAB and Google Play submission remain unverified.
