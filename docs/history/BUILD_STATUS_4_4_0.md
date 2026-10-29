# Falley build status

**Current: 4.4.0/code 17**, prepared 29 October 2026. Firebase Crashlytics and Analytics are implemented as separate optional choices, off by default in Settings → Diagnostics & analytics. Gameplay still works offline.

## Local deliverables

Portable directory example: `C:\local-delivery\2026-10-29\Falley-4.4`.

| Artifact | Bytes | SHA-256 |
| --- | ---: | --- |
| `Falley-4.4-debug.apk` | 7,112,517 | `c1bc2c6a330e51ee68d136a069e11d7802b12b7cbf44aebe8ff57c14901af7c6` |
| `release/Falley-4.4.0-beta.aab` | 3,214,112 | `2760faf1a668cddce4fd20d6e3c7a7a434547eeab9a20250b5ce7a38abd12f48` |
| `release/Falley-4.4.0-beta-local.apk` | 2,230,251 | `22b8932b7fb7efaaf0eec5c766cc9a84feb59410c7c7c333f5312fadd7782c5f` |

Debug package `com.perfgames.falley.motiontest`; release `com.perfgames.falley`. Signing identities are preserved (debug APK v2 signing verified). Release R8/resource shrinking, bundle/signature validation, exact embedded mapping/runtime-ID matching and native ELF/ZIP 16 KiB checks **PASS**. Exact release mapping upload to Firebase **succeeded**; local artifact hashes are unchanged. JSON, keys, APK/AAB/APKS, mappings and generated intermediates are excluded from Git.

## Implemented and tested

- Firebase initializes only after a saved opt-in. Analytics storage consent is separately controlled; ad consent, advertising identifiers and automatic screen reporting stay disabled. Crash changes fully apply next launch, and later opt-in can include cached reports; received server data is not deleted by opt-out.
- Closed-vocabulary run-start/end events and coarse crash context, labelled debug/release. No custom name/email/user ID, raw motion, touch history, full save or Daily seed/date upload.
- **36 portable groups**, reward/storage (704/32), media (56), policy (222), release translations (52,482), consent controller (1,403) and event-schema (17,475) checks pass.
- Accepted native checks on final binaries, font 1.3:

| Native fixture | Checks | Captures |
| --- | ---: | ---: |
| debug-API35-small | 407 | 4 |
| debug-API35-tall | 407 | 4 |
| debug-API25-firebase | 403 | 4 |
| debug-API25-privacy | 2,417 | 12 |
| debug-API35-small-ux | 12,428 | 92 |
| debug-API35-tall-ux | 12,513 | 92 |
| release-API36-small | 255 | 13 |
| release-API36-tall | 257 | 13 |
| release-API25-small | 234 | 13 |

The synthetic debug exception was captured and bound to the report queued by the SDK after reopening. This does **not** claim Firebase Console dashboard confirmation. Release smoke deliberately leaves production reporting off. Native preferences/display/font and the owned emulator's temporary network change are restored; harnesses are removed and owned emulators stopped after verification.

Release lint reports **0 errors/13 warnings**, with no unsupported Kotlin metadata diagnostics. Reviewed warnings concern legacy/launcher artwork, orientation/tooling/style and existing Gradle advice; no telemetry/security errors were suppressed. Firebase BoM 34.19.0 (Analytics 23.2.0/Crashlytics 20.1.1) matches existing AGP 8.10.1/R8's Kotlin 2.2 support. Newer SDK 24 metadata was rejected during validation rather than shipped with incomplete analysis.

Screens, accepted log summaries, provenance and limits: [4.4 evidence](../../screenshots/v4.4/README.md). Source-only CI needs no Firebase configuration or signing keys.

## Before beta distribution

Redeploy the [updated policy page](../../policy-site/README.md), review Play Data safety and selected Firebase/Analytics console settings, and confirm the debug test report/DebugView in the **Motion Test** app registration. Both app registrations share one Firebase project. No Play upload, Vercel deployment, monetization or production test crash was performed.

Test physical Redmi/tablet feel, audio, sustained performance, TalkBack and fluent Hindi/Indonesian copy. Emulator fixtures do not certify every Android version/device. [Development](../DEVELOPMENT.md) · [Firebase guide](../FIREBASE_4_4.md) · [Historical 4.3 status](../history/BUILD_STATUS_4_3.md)
