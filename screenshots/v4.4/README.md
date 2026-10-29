# Falley 4.4 verification

Version **4.4.0/code 17**. APK/AAB/APKS, Firebase JSON, signing material, mapping and raw SDK logs stay local. [Receipt](VERIFICATION.json) records artifact hashes and accepted evidence; [source provenance](SOURCE_HASHES.json) separates frozen runtime inputs from final build/upload workflow maintenance.

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

Debug fixtures use `com.perfgames.falley.motiontest`; signed optimized smoke uses `com.perfgames.falley`. Small/tall are 480x800 and 720x1600, font 1.3. API 25/35/36 correspond to Android 7.1/15/16. Fixtures restore preferences/display/font and remove test harnesses. They are controlled emulator checks, not natural phone play or physical sensor/audio/performance certification.

Actual SDK checks prove default-off cold initialization, independent durable choices, Analytics ID availability after consent and null after denial, and gated one-shot run events. An explicit synthetic debug fatal was captured and its session matched the SDK-queued report after reopening. The emulator initially had airplane mode enabled; its owned connectivity settings were restored after the online check. Dashboard ingestion remains owner confirmation. Production smoke keeps reporting off.

**36 portable groups**, 222 policy checks, 52,482 release translation checks, 1,403 consent checks, 17,475 event-schema checks, 704/32 reward/storage checks and 56 media lifecycle checks pass. Release lint: 0 errors/13 reviewed warnings; no unsupported Kotlin metadata diagnostics. R8 mapping, embedded bundle mapping and nonblank runtime ID agree; exact mapping upload succeeded without changing accepted artifacts. Native ELF and APK ZIP 16 KiB alignment checks pass.

Screens: [English](consent-en.png), [Hindi](consent-hi.png), [Indonesian](consent-id.png), [small phone](consent-small.png), [release Settings](release-settings.png), [release default-off controls](release-consent-off.png).

Full raw logs, synthetic crash logs and private configuration receipts remain in the local delivery. Current policy/console/Play tasks: [Firebase guide](../../docs/FIREBASE_4_4.md). Older SDK candidates and failed fixture experiments are excluded from accepted evidence. Audio-only fixtures explicitly hold simulation time while testing actual Android lifecycle and native audio; functional legacy startup has a bounded wait for a loaded emulator and is not a performance test.
