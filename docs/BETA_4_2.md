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

## R8, resources and mapping

Release enables `minifyEnabled true`, `shrinkResources true` and `proguard-android-optimize.txt`. The app rules keep only manifest entry-point names/constructors and the SDK-guarded API 33 Back and API 26 audio-focus helpers' optimization boundaries. Other application code can shrink, optimize and be renamed. No blanket app-wide keep rule is added for instrumentation.

The audio player uses direct `R.raw` references. The current production source has no resource-name lookups, class-name reflection or JNI requiring broad resource/member preservation. Resources referenced by the manifest, style hierarchy and direct audio IDs remain reachable through the Android build pipeline. A release test must still prove that the final package launches and that audio/settings navigation does not fail after shrinking.

The release script requires a nonempty `app/build/outputs/mapping/release/mapping.txt`. It copies that file beside the local artifacts, verifies the AAB contains `BUNDLE-METADATA/com.android.tools.build.obfuscation/proguard.map`, and requires an exact SHA-256 match. Available R8 `configuration.txt`, `usage.txt`, `seeds.txt`, `resources.txt` and `missing_rules.txt` reports are copied under `r8/`. `SHA256SUMS.txt` covers the AAB, local APK and mapping; `release-verification.json` records identities, mapping equality, report hashes and successful pipeline checks. These generated reports and mappings stay local.

Keep the exact mapping for every published version. Play can extract bundled deobfuscation data when using a supported Android Gradle Plugin; verify the uploaded version's assets in Console. Old version-code-11 warnings do not prove anything about a new code-14 artifact. This is Java code, so R8 mapping is relevant; native C/C++ symbol archives are not applicable to this source.

References: [Android app optimization](https://developer.android.com/topic/performance/app-optimization/enable-app-optimization), [narrow keep rules](https://developer.android.com/topic/performance/app-optimization/keep-rules-overview), [resource keep guidance](https://developer.android.com/topic/performance/app-optimization/customize-which-resources-to-keep), [Play deobfuscation files](https://support.google.com/googleplay/android-developer/answer/9848633).

## Verification strategy

Use the full portable suite for physics, stars, migration, Daily isolation, recommendation rules, localization and new preference behavior. Run the native debug fixtures against the exact final debug APK for controlled collision/result/layout evidence. Their private-member reflection is a debug test aid and must not force a broad production keep contract.

Use a separate **reflection-free release smoke** against the exact bundle-derived optimized production APK. It should verify the production package/version/non-debuggable flag and permission list, a fresh language choice, Home, Collection, Settings, independent music/sound choices, Hindi/Indonesian selection, policy visibility and real Campaign touch play/pause/restart/Back navigation through exposed accessibility nodes or real pointer events. The fixture backs up/restores preferences and display/font settings, only targets a disposable emulator, captures submitted compositor frames and removes its instrumentation app after verified recovery.

A successful automated smoke is distinct from physical-phone listening, sensor comfort, TalkBack navigation, performance and human language review. API 24-32 need particular attention to legacy Back behavior, the API 33 helper boundary and the API 24/25 legacy audio-focus path; a single API 35 emulator does not certify every supported Android version. A final Play-installed closed-test run should also cover app-signing updates and device-specific behavior.

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

1. Record final artifact identities and exact-build test results in Build Status and local receipts. Keep the AAB, mapping and private signing backup together in controlled local storage.
2. Review the final policy-site text and redeploy it if needed. Confirm the public policy URL and support email in Play Console.
3. Align screenshots, description, release notes, target audience, content rating and Data safety with the actual offline game, hazards and optional blood effects. No ad or purchase disclosure should imply an SDK that has not been added.
4. Run physical-phone acceptance and a Play-installed closed test. Collect reports on controls, first-run language choice, star comprehension, retries, audio balance and progression before widening the rollout.

Historical [4.1](UX_UPDATE_4_1.md), [4.0](EXPANSION_DEBUG_4_0.md) and [3.3.1](PRIVACY_UPDATE_3_3_1.md) receipts remain valid only for their named artifacts. They are not substituted for final 4.2 proof.