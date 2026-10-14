# Falley V2 build status

Verified source/local build on 14 October 2026. Debug testing build; APK stays local and no Google Play production rollout is submitted.

- Version: 2.0.0 / code 4; application ID: com.perfgames.falley
- Minimum API 24; compile/target API 36
- Local APK: Falley-debug.apk (41504 bytes); final rebuild includes shifted targets and monotonic side pressure
- SHA-256: 26DA00D761940B6E99AC654B2D9759F3E3D26B60C14C636333A7E1225BB4DD8F
- Portable SDK builder: JDK 17 / platform 36 / build-tools 35.0.0; PASS
- APK signature: v2/v3 verification PASS
- Physics: all six complete chapter courses won with bounded steering; rope pool recycling, weight/cuts, sides never widening during actual jumps, fire warning/drop/collision, touch direction/release/no drift, shifted/full-area target recovery, terminal results/reset, 100000 seeded steps; PASS (20640127 checks)
- Complete simulation runs: 10.12, 11.15, 12.20, 13.25, 14.29 and 15.35 seconds; test controller durations, not human playtest results
- Touch gesture policy: 16 cases; PASS
- Motion filtering: 17 cases; PASS
- Global/per-stage profile accounting: 21 cases; PASS
- Portable log: screenshots/v2/physics-suite.log (raw build copy: build/physics-v2.log)
- Native final V2 APK: PASS on Android 15/API 35 and Android 16/API 36, each at 480x800/240 dpi and 720x1600/320 dpi
- Four native suites: 416 logged checks each, 1664 total; 112 local captures, with 48 curated images shared in source
- Actual bilateral swipes, no automatic swipe jump, tap/Jump, release/cancel/no lateral drift, rope-contact stability, Back/pause/settings/preset recreation, chapters/stage selection: PASS
- Controlled six-stage target wins/Next Stage/per-stage records, three side-hazard reasons, final-stage fire warning/drop/contact: PASS; staged fixtures, not physical-phone playtesting
- Visual review: Home, chapter selection, gameplay and results; no clipping or system-bar overlap
- Native evidence: screenshots/v2/api35/ and screenshots/v2/api36/; full local captures under ignored build/device/v2/
- Original emulator preferences/display overrides restored; test APKs removed

The local debug key is preserved under ignored build/. APK/AAB files are not committed or published. Physical-phone feel/fun, broader hardware coverage, Gradle build verification, release-signed AAB and Google Play submission remain unverified.
