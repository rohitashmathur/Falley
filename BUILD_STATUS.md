# Falley V1 build status

Verified 14 October 2026. Final debug testing build; no Google Play production release.

- Version: 1.0.0 / code 3; application ID: com.perfgames.falley
- Minimum API 24; compile/target API 36; installed on Android 15/API 35 and Android 16/API 36
- APK: Falley-debug.apk (33238 bytes)
- SHA-256: 221F88F2987F0666783C528338D6D20FD37DF4082B23A59447A6408CADAF22D9
- Portable SDK builder: JDK 17.0.20.1+1, platform 36/build-tools 35.0.0; PASS
- APK signature: v2/v3 verification PASS
- Physics: complete finite course, rope land/jump/cut, target/off-target ground, terminal win/celebration, reset and 100,000 seeded steps; PASS
- Motion: 17 scenarios; PASS
- Profile: 15 scenarios; PASS
- Native final APK: four suites, 198/184/184/184 checks (750 total); 48 screenshots; PASS
- Devices:API 35 and API 36 emulators, each 480x800@240 dpi and 720x1600@320 dpi
- Actual pointer navigation/system Back, menu pause, jump/drag/restart/replay, settings across recreation: PASS
- Controlled floor bounce/win, rail death and off-target recovery: PASS; test fixtures, not physical-phone playtesting
- Visual review: Home, Settings, gameplay/human, target bounce and Results; no clipping or system-bar overlap
- Test runner restores original preferences and display settings; test APKs removed
- Native evidence: screenshots/v1/api35 and screenshots/v1/api36, with readable suite logs

Physical-phone comfort, older Android devices, subjective fun, Gradle build, release AAB and Play production submission remain unverified.
Warnings: Java 8 bootstrap classpath and supported deprecated inset/display/legacy Back accessors.
