# Falley 3.2 beta build status

Current source: **3.2.0 / code 7**, application ID `com.perfgames.falley`; minimum API 24, compile/target API 36. Preparation date: **16 October 2026**. Scope is an offline, ad-free Android phone beta. Binaries and signing material remain local; no Play upload/rollout has occurred.

- Release tooling prepared: checksum-pinned Gradle **8.11.1** wrapper, AGP **8.10.1**, JDK **17**, official bundletool **1.18.3**. `scripts/build-release.ps1` runs lint/bundle generation, strict signature and every-payload certificate checks, bundle validation and universal-APK verification.
- Signing prepared through `scripts/release-signing.ps1`: private upload identity under ignored `releases/private-signing/`, Windows current-user directory permissions and DPAPI-protected credentials. Preserve a recoverable private backup; no key, password or binary belongs in Git.
- Frozen release build: **PASS**, `lintRelease` and `bundleRelease`; lint reports **0 errors and 9 warnings**. Initial guarded-API errors were fixed before this successful final build. App-source revision embedded in the AAB: `21f2f87c3bc37aa9ab3fc34d3b0e62f3d2b4dd38`; later documentation/artwork commits do not identify a different app binary.
- Local AAB: `releases/3.2.0/Falley-3.2.0-beta.aab`, **301784 bytes**, SHA-256 `2d7c7638bfbcfae1161af690bc0b3cc47bb5e8198b9c223bd76725343175b75c`.
- Bundle-derived, upload-key-signed local APK: `releases/3.2.0/Falley-3.2.0-beta-local.apk`, **321030 bytes**, SHA-256 `2d62249186607d650a1719083e77c63a1b81f20b0a3c12c7225593f5cc508c39`. Universal `.apks`, inspected manifest and `SHA256SUMS.txt` accompany these local artifacts.
- Bundletool **1.18.3 validation PASS**; strict jarsigner verification against the expected key **PASS**; Java `JarFile` verification confirms **all 24 payload entries** match the preserved certificate. Negative controls reject an unsigned bundle and an added unsigned payload. Missing signing-environment guard also **PASS**.
- Public upload certificate SHA-256: `16cc163611d6a0b2f13474470ed8328e7e5093ba8c5af34ef0701ebf423f9caa`. The key and DPAPI credential remain private, excluded from Git. Derived APK v2/v3 signatures and `zipalign -P 16` **PASS**. Local upload-key testing is not proof of a later Play app-signing install/update.
- Artifact manifest: min API **24**, target API **36**, **non-debuggable**, no requested permissions, optional accelerometer/gyroscope and backup/cloud/device-transfer exclusions.
- Jarsigner emits POSIX ZIP-attribute and archive/JarInputStream-order warnings on the AGP-generated AAB. Strict verification and every-entry `JarFile` certificate checks pass; this report does not claim zero warnings. Raw build/controls/guard evidence: `build/release-v32.log`, `build/signature-controls.log`, `build/release-signing-guard.log`.
- Full native Android 16/API 36 release suites: **PASS** on 480x800 @ density 240 and 720x1600 @ density 320, **1019 checks/64 captures each; 2038 checks/128 captures total**, on the frozen bundle-derived APK.
- Focused Android 15/API 35 suites: **PASS**, **387 checks/13 captures per layout; 774 checks/26 captures total**, covering current privacy, menu-accessibility and screen-awake behavior alongside focused release regressions.
- Four current native runs: **PASS, 2812 logged checks/154 captures**. Curated proof contains **42 PNGs** (16 API 36, 26 API 35) and **14 receipts/logs** under `screenshots/v3.2/`. Original preferences/display settings were restored, the instrumentation APK was removed and both owned emulators were stopped; no failure captures were produced. Frozen AAB/APK hashes were confirmed unchanged after testing. Controlled emulator fixtures do not establish real-phone listening, human TalkBack navigation or natural player success rates.
- Store capture acceptance: **PASS, 125 checks**, separate from the four native-run totals; six fresh native **1080x1920** captures from 3.2.0. All **10 prepared upload assets** (including optional feature variants) pass dimension/mode/size/hash checks, alt text at most 140 characters, source/crop integrity and root/asset visual review. Store capture receipts are kept separately under `screenshots/v3.2/`; upload assets/provenance are under `store/assets/`.
- Source changes verified in current native checks: offline Settings privacy policy, menu accessibility nodes, active-play-only screen-awake behavior/static-menu rendering, guarded API annotation, static raw-resource audio lookup and explicit backup/data-extraction rules. App-source commit and artifact hashes above remain unchanged; the later documentation/artwork commit is separate.
- Campaign/Arcade physics and WAV payloads are unchanged from 3.1. The deterministic six-course/survival, 100,000 + 100,000 stability-step and audio-file proof below is **carried forward, not rerun as a 3.2 test claim**.

Owner gates remain open: a real monitored support email, publicly hosted privacy policy/contact mechanism, Console/account/package access, audience/content-rating/Data safety review and real-phone acceptance. The in-app policy has no fabricated contact. Adding the real contact requires a **new higher version code and rebuilt/retested bundle before closed beta**. AdMob and consent are absent; this is not an advertising release or an approved closed-beta submission.

Use [BETA_SUBMISSION.md](docs/BETA_SUBMISSION.md) for the internal-then-closed-test order, [release signing](docs/RELEASE_SIGNING.md) for private-key preservation, [store drafts](docs/store/README.md) for owner-reviewed content and [store-assets.md](docs/store-assets.md) for upload images. The local release artifact and technical/store-image checks pass. No Google approval, age rating, real-device acceptance or broad IP clearance is implied; owner submission gates remain open.

## Historical 3.1 proof

Current source: **3.1.0 / code 6**, application ID `com.perfgames.falley`; minimum API 24, compile/target API 36. APK/AAB files and signing material remain local. Verification date: 15 October 2026.

- Campaign physics: **PASS, 20,939,979 checks**, including six actual full-course wins, the guaranteed stage 4 saw introduction, clear target approaches and 100,000 seeded stability steps.
- Six bounded-controller finishes: **10.12, 11.15, 12.20, 12.61, 14.29 and 15.33 seconds**. These are simulation times, not human playtest results.
- Arcade physics: **PASS, 21,000,408 checks**, including existing beam/fire geometry and drift tests plus the expanded five-family selection and 100,000 stability steps.
- Natural mixed Arcade: all-five-family controller survives **25 seconds/wave 2** at Chill, Standard and Brutal; scores **3010 / 2920 / 2920**. No campaign floor or false win.
- Focused buzzsaw/cue tests: **PASS, 879 checks**. Harmless warnings, fast relative swept-circle collision, grazing hit/near miss, actual reachable refuge dodges at minimum Arcade width/all difficulties, locked paths, exclusive attacks, reset and landing retirement.
- Hazard audio event consumption: **PASS, 20 checks**, including duplicate/stale suppression, resume/restart and same-step motor/impact priority. Touch: **16**; motion: **17**; profile: **33**, all PASS. Existing mask 15 stays selected; fresh/empty defaults use 31.
- Audio generator `--check`: **PASS for 11 synthesized WAV assets**, with distinct files, fades, no clipped PCM and peak/headroom validation. This is file validation, not a listening test.
- Current proof: `screenshots/v3.1/physics-suite.log` and `screenshots/v3.1/audio-assets.log`; raw physics output: `build/physics-v3.1.log`.
- Final V3.1 local APK: **PASS**, `Falley-debug.apk`, **312635 bytes**, version **3.1.0 / code 6**; v2/v3 signatures verified. Manifest inspection confirms min API 24, target API 36, debug build, optional sensors and no requested Internet permission.
- SHA-256: `F50FFD0A8B228EB1869EBA24126962A12F15AB791FA1E79A25F46265AF492A6F`.
- Full native Android 16/API 36: **PASS**, 480x800 @ density 240 and 720x1600 @ density 320, **799 logged checks and 62 captures each; 1598 checks/124 captures total**. Frozen APK checksum unchanged. All 11 SoundPool samples load; actual physics cues increment native playback once. Mute, menu/background, recreation and release checks pass. Playback counters do not verify audible output or mix quality.
- Focused Android 15/API 35 saw/audio checks: **PASS**, **185 checks and 11 captures per layout; 370 checks/22 captures total**. Corrected warning track/blade frames visually verified on both sizes. These focused checks supplement the full API 36 suites; they do not repeat every unrelated full-suite flow.
- All four current native runs: **PASS, 1968 logged checks/146 captures** on the unchanged APK checksum above. Final curated evidence contains **40 PNGs** (18 API 36, 22 API 35), four run logs/check receipts and the physics/audio logs under `screenshots/v3.1/`. Original preferences/display settings were restored and test APKs removed. Controlled hazard/collision fixtures are not natural physical-phone sessions.
- API 36 visual review confirms the five-row setup and active/impact saw frames. Curated evidence: 18 PNGs under `screenshots/v3.1/api36/{small,tall}`. Two original saw-warning captures showed a stale prior frame despite passing numerical warning-safe assertions; those PNGs are excluded from curated evidence and raw full-suite receipts are retained. The focused API 35 harness now advances frames and asserts the warning phase before capture. This is a test-only timing correction; no app change or rebuild.

Independent source review found no actionable blocking code defect. The historical V3 results below remain separate from current proof; no automated result replaces real-phone listening, controls or performance acceptance.

Review fixed a landing fairness defect: an attack warned above the spawn cutoff could remain active across a strict target lane. Fire, beam and saw pools now retire inside the final 650 world units. Jumping back above that band can permit another warning, which retires on reentry. Boundary hazards and strict missed-floor deaths remain active.

SoundPool uses at most three one-shot voices. Sound off/background/restart stops playback and Activity destruction releases resources. Unloaded samples are dropped, not delayed; an early cue can be silent. Physical audio quality, volume/mix and interruption behavior still require phone testing.

**Readiness: local testing; not a production advertising release.** No AdMob or consent SDK is integrated. See [Play Store readiness](docs/PLAY_STORE_READINESS.md). Next work is real-phone audio/performance acceptance, a validated release-signed AAB and privacy/content-rating work; add advertising and consent as a separate verified release task.

## Historical V3.0 proof

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

At the historical V3.0 checkpoint, physical-phone feel/fun, broader hardware coverage, Gradle verification, production release AAB and Google Play submission remained unverified. See the current 3.2 status above for later tooling work.
