# Falley 3.3 verification evidence

Captured 17 October 2026 from the final code 10 binaries. Application source revision: `f4b3a5abc465fffd50f1e904e593914bf11efb7d`. Images here are emulator test evidence, not Play promotional assets or natural phone play sessions. The user's supplied phone screenshot is not republished.

- `help-touch.png`: new illustrated English Touch instructions, 480×800 / Android font scale 1.3.
- `help-burst-hi.png`: Hindi jump/recovery/total-budget page, 480×800 / font scale 1.3.
- `help-motion-id-tall.png`: Indonesian Motion guide, 720×1600 / font scale 1.3.
- `language-label-id.png`: visible localized Language caption in Settings.
- `campaign-locks.png`: fresh campaign with only stage 1 open and later stages visibly locked.
- `budget-exhausted-controlled.png`: staged total-budget exhaustion; burst recovery does not restore the total. The moved/cut ropes are controlled test state, not a captured natural run.

`debug-small.txt` / `debug-tall.txt` each record 5,170 checks and 26 PNGs. `release-small.txt` / `release-tall.txt` each record 4,365 checks and 14 PNGs against the upload-signed, non-debuggable production package. Total: **19,070 checks / 80 distinct PNGs**. Raw complete captures remain local under `build/device/revision-330/`.

`physics-suite.log` preserves the successful 100,000-step campaign run and its later obsolete intro-idle assertion; `followup.log` records the corrected remaining tests, including Arcade's 100,000 steps. In the deliberately gentler wind, the first two forgiving stages can finish idle; stages 3–12 still need steering. All 13 portable classes pass across these logs.

`release-validation.log` records the successful release build, bundle validation and signature checks. `lint-release.txt` reports 0 errors and 9 warnings. Archive/JarInputStream-order warnings are retained and not claimed absent; every payload certificate was verified. No private signing material, APK, AAB or user attachment is included.
