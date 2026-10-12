# Mode 1 build status — 0.2.0

Verified 12 October 2026.

- Package: com.perfgames.ropelab; version code 2; minimum API 24; target/compile API 35
- APK: RopeLab-debug.apk, 25046 bytes, locally debug-signed
- SHA-256: EEC012F886452F55ACA9FA602AE82498E3837849C21BB10E6B047808F385E09F
- Build: PASS with JDK 17.0.20.1+1, Android platform 35/build-tools 35.0.0
- APK signature: v2/v3 PASS
- Portable physics suite: PASS; 100,000 seeded steps and gameplay cases
- Portable motion suite: PASS; 17 scenario checks for gyro/accelerometer filtering, presets, calibration and lifecycle reset
- API 35 emulator install/launch: PASS, version 0.2.0 confirmed
- Visual inspection: PASS; 720x1280/320 dpi and 1080x2400/420 dpi, controls clear system bars
- Touch: jump ascent and motion-OFF drag steering checked; lethal wall blast observed
- Sensors: Loose/Balanced/Tight cycling, ZERO, motion ON/OFF, simulated accel+gyro steering checked; wall blast observed
- Gameplay: score/weight growth, 44 rope cuts and narrower walls observed in the emulator
- Lifecycle: Home/resume exercised; no AndroidRuntime crash output
- Physical phone and real sensor comfort: NOT TESTED
- Gradle build and release signing: NOT TESTED

Warnings: Java 8 bootstrap classpath and supported deprecated Android window-inset/display APIs.
This verifies prototype operation, not balanced gameplay, accurate rope materials or physical-phone performance.

