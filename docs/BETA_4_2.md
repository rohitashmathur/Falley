# Falley 4.2 — beta preparation and release verification

Prepared 25 October 2026. Source version **4.2.0 / code 14**, production package **com.perfgames.falley**. This document describes the release configuration and acceptance scope. The exact artifact sizes, hashes, completed checks and remaining work belong in [build status](../BUILD_STATUS.md) and the local delivery receipts after the final build. Preparing source is not proof that a bundle was built, uploaded or accepted by Play.

## Player-facing scope

The preceding 24-stage campaign, six chapter finales, Daily Drop, Arcade, cumulative stars and approved avatar roster remain included. The 4.2 refinements make reward distance explicit on Home, explain optional star goals in the briefing, tuck practice controls behind a disclosure, shorten loss results and distinguish new stars from saved mastery on wins. Collection focuses on avatars and campaign rope abilities, with the equipped avatar and next locked reward clearly shown. Classic preserves the original rebound; Spring unlocks at 6 stars and multiplies a supporting-rope rebound by 1.5; Static unlocks at 12 stars and gives a weaker next manual jump plus brief steering damping after support. Daily Drop and Arcade always use Classic. Avatar animation differences remain cosmetic and all avatar hitboxes are identical.

A new player chooses English, Hindi or Bahasa Indonesia before Home, with English selected initially. Country hints use available network, SIM or device-region information without requesting a permission or silently overriding the chosen language. Existing explicit language preferences migrate as already confirmed. Music has its own saved setting, independent of one-shot sound effects, and pauses when the app loses the foreground. The final app and policy text should describe the shipped settings accurately.

Support is **mathurrohitash16@gmail.com**. The public policy is **https://falley-privacy-policy.vercel.app/**. The app remains offline, without ads, purchases, accounts, analytics, Internet permission or backend. Building a new bundle does not deploy the policy website or update Play listing fields.

## Artifact identities

| Purpose | Package | Version | Code | Signing |
| --- | --- | --- | ---: | --- |
| Owner debug APK | `com.perfgames.falley.motiontest` | `4.2.0-beta-test` | 14 | Preserved local debug key |
| Play beta AAB | `com.perfgames.falley` | `4.2.0` | 14 | Preserved upload identity |
| Bundle-derived local APK | `com.perfgames.falley` | `4.2.0` | 14 | Same upload identity |

Outputs stay outside Git, under an owner-chosen local delivery directory or ignored `releases/4.2.0/`. The AAB is the Play submission artifact. The derived APK is useful for local verification, but Play App Signing may use a different distribution certificate. Preserve existing signing keys; never generate a replacement merely to get past an update mismatch.

## Completed debug verification

The final local debug APK is built from app source commit **`2c309f1cbd12bfaa4755b6b98ad4d69d277a412d`**, version **4.2.0-beta-test / code 14**, package **com.perfgames.falley.motiontest**. It is **1,103,387 bytes**, SHA-256 **`dc4f949497b41433bee0cb52f11710a58fc3fad49021e447ad56cc4b158442ec`**. The matching builder log reports successful APK v2/v3 verification.

Native debug `-UxOnly` acceptance passes on Android 15/API 35 at **480×800/density 240** and **720×1600/density 320**, font scale **1.3**, across English, Hindi and Bahasa Indonesia. Final receipts record **10,107 checks / 83 captures per layout; 20,214 checks / 166 captures total**. Counts include 84 test-artifact cleanup assertions per layout. Preferences are restored exactly; display/font settings are recovered and instrumentation removed. [Curated 4.2 debug evidence](../screenshots/v4.2/README.md) includes 18 unedited screenshots, exact artifact identity, sanitized logs, per-layout receipts and image provenance.

Coverage includes fresh explicit language choice and returning-language migration, temporary country hints, two-tab earned avatar/rope selection, native rope support effects and Classic mode isolation, independent music/effects preferences and music lifecycle, Home/navigation/briefing flows, localized control cues, fast cause-specific Retry, named independent star evidence and cumulative progress. Controlled native fixtures do not establish human success rates, sensor comfort, audio audibility or full accessibility conformance.

## Completed production release verification

The upload-signed **4.2.0/code 14** AAB is **1,037,308 bytes**, SHA-256 `f5b93a995065da8af8f7fdb7c3cb63c85addd163911e93e93c021777dde0341c`. Its embedded VCS revision is `2c309f1cbd12bfaa4755b6b98ad4d69d277a412d`; later evidence/harness fixes leave app inputs unchanged. R8/resource shrinking, release lint (0 errors / 10 nonfatal warnings), bundletool validation, strict signature/all 26 payload certificates, APK v2/v3 and 16 KiB ZIP alignment pass. The preserved upload certificate is unchanged. The local mapping and embedded bundle mapping match exactly: SHA-256 `11d48e42b62050bb5e7d0b62c133690fc794739897915854145e06689a6e2cc8`.

The exact bundle-derived optimized production APK passes reflection-free smoke on **Android 15/API 35** (222 small + 220 tall + 1 recovery checks) and **Android 16/API 36** (201 small + 205 tall + 1 recovery checks), with **48 captures total** at font scale 1.3. Checks cover version/production/non-debuggable/no-permission identity, all 12 packaged WAV resources, first-launch language choice, public navigation/Collection/independent audio settings, Hindi/Indonesian, policy, real touch play and pause/restart/Back. Preferences/display/font are restored, both harnesses removed and both owned emulators stopped. The harness compiles without application classes or private reflection, so production optimization is not weakened to accommodate it.

All 26 portable suites pass, including bounded opening-stage clears, all 24 Classic touch/motion courses, all 24 earned-rope touch courses, six Daily templates and profile migration. Earned-rope motion clearance covers the opening three stages; the full earned-rope campaign still needs phone balance feedback. Final focused translations pass 8,397 checks and original PCM/controller tests pass 56 cases. [Build Status](../BUILD_STATUS.md) and the [release receipt](../screenshots/v4.2/RELEASE_VERIFICATION.json) identify all outputs. No Play upload or policy website deployment was performed.
## R8, resources and mapping

Release enables `minifyEnabled true`, `shrinkResources true` and `proguard-android-optimize.txt`. The app rules keep only manifest entry-point names/constructors and the SDK-guarded API 33 Back and API 26 audio-focus helpers' optimization boundaries. Other application code can shrink, optimize and be renamed. No blanket app-wide keep rule is added for instrumentation.

The audio player uses direct `R.raw` references. The current production source has no resource-name lookups, class-name reflection or JNI requiring broad resource/member preservation. Resources referenced by the manifest, style hierarchy and direct audio IDs remain reachable through the Android build pipeline. A release test must still prove that the final package launches and that audio/settings navigation does not fail after shrinking.

The release script requires a nonempty `app/build/outputs/mapping/release/mapping.txt`. It copies that file beside the local artifacts, verifies the AAB contains `BUNDLE-METADATA/com.android.tools.build.obfuscation/proguard.map`, and requires an exact SHA-256 match. Available R8 `configuration.txt`, `usage.txt`, `seeds.txt`, `resources.txt` and `missing_rules.txt` reports are copied under `r8/`. `SHA256SUMS.txt` covers the AAB, local APK and mapping; `release-verification.json` records identities, mapping equality, report hashes and successful pipeline checks. These generated reports and mappings stay local.

Keep the exact mapping for every published version. Play can extract bundled deobfuscation data when using a supported Android Gradle Plugin; verify the uploaded version's assets in Console. Old version-code-11 warnings do not prove anything about a new code-14 artifact. This is Java code, so R8 mapping is relevant; native C/C++ symbol archives are not applicable to this source.

References: [Android app optimization](https://developer.android.com/topic/performance/app-optimization/enable-app-optimization), [narrow keep rules](https://developer.android.com/topic/performance/app-optimization/keep-rules-overview), [resource keep guidance](https://developer.android.com/topic/performance/app-optimization/customize-which-resources-to-keep), [Play deobfuscation files](https://support.google.com/googleplay/android-developer/answer/9848633).

## Verification strategy

Use the full portable suite for physics, stars, migration, Daily isolation, recommendation rules, localization and new preference behavior. Run the native debug fixtures against the exact final debug APK for controlled collision/result/layout evidence. Their private-member reflection is a debug test aid and must not force a broad production keep contract.

Use a separate **reflection-free release smoke** against the exact bundle-derived optimized production APK. It should verify the production package/version/non-debuggable flag and permission list, a fresh language choice, Home, Collection, Settings, independent music/sound choices, Hindi/Indonesian selection, policy visibility and real Campaign touch play/pause/restart/Back navigation through exposed accessibility nodes or real pointer events. The fixture backs up/restores preferences and display/font settings, only targets a disposable emulator, captures submitted compositor frames and removes its instrumentation app after verified recovery.

A successful automated smoke is distinct from physical-phone listening, sensor comfort, TalkBack navigation, performance and human language review. API 24-32 need particular attention to legacy Back behavior, the API 33 helper boundary and the API 24/25 legacy audio-focus path; API 35/36 emulators do not certify every supported Android version. A final Play-installed closed-test run should also cover app-signing updates and device-specific behavior.

## Reproduce locally

```powershell
.\scripts\test-expansion.ps1 -JdkRoot 'C:\path\to\jdk-17'
.\scripts\build-android.ps1 -SdkRoot 'C:\path\to\android-sdk' -JdkRoot 'C:\path\to\jdk-17' -ApplicationId com.perfgames.falley.motiontest -AppLabel 'Falley Motion Test' -OutputApk 'C:\local-delivery\Falley-4.2.0-debug.apk'
.\scripts\build-release.ps1 -SdkRoot 'C:\path\to\android-sdk' -JdkRoot 'C:\path\to\jdk-17' -BundletoolJar 'C:\path\to\bundletool-all-1.18.3.jar' -OutputDirectory 'C:\local-delivery\Falley-4.2.0\release'
```

After separately installing the bundle-derived production APK on the disposable emulator, run:

```powershell
.\scripts\test-release-smoke.ps1 -Device emulator-5580 -SdkRoot 'C:\path\to\android-sdk' -JdkRoot 'C:\path\to\jdk-17' -FontScale 1.3 -ArtifactRoot 'C:\local-delivery\Falley-4.2.0\verification\release-smoke'
```

This signs only the temporary instrumentation harness with the existing upload identity; it does not build the app or upload anything. Install the exact production APK first. `-BuildOnly` prepares that harness without running a device; `-RestoreOnly` recovers a marked interrupted preference fixture. A smoke pass proves navigation and packaged audio readability, rather than listening quality or full R8 functional equivalence.

See [signing instructions](RELEASE_SIGNING.md) before the release command. Do not pass `-InitializeSigning` when the existing upload identity is already present. The release script checks lint/build success, bundle integrity, all payload certificates, expected manifest/SDK/backup/permission flags, derived APK signatures, 16 KiB ZIP alignment and mapping consistency. Any failure must be resolved before treating the bundle as submission-ready.

## Before an owner beta rollout

1. Keep the recorded AAB, exact mapping and private signing backup in controlled local storage. Artifact identities and exact-build results are recorded above and in Build Status.
2. Review the final policy-site text and redeploy it if needed. Confirm the public policy URL and support email in Play Console.
3. Align screenshots, description, release notes, target audience, content rating and Data safety with the actual offline game, hazards and optional blood effects. No ad or purchase disclosure should imply an SDK that has not been added.
4. Run physical-phone acceptance and a Play-installed closed test. Collect reports on controls, first-run language choice, star comprehension, retries, audio balance and progression before widening the rollout.

Historical [4.1](UX_UPDATE_4_1.md), [4.0](EXPANSION_DEBUG_4_0.md) and [3.3.1](PRIVACY_UPDATE_3_3_1.md) receipts remain valid only for their named artifacts. They are not substituted for final 4.2 proof.