# Falley 3.3.1 — public privacy contact

Prepared **23 October 2026**. Source version **3.3.1 / code 11**. Source, policy website, documentation and selected verification evidence are included in the repository; Android binaries and signing files remain local. No Play upload or rollout has occurred. The game remains offline and ad-free, with no requested Android permissions.

## What changed

Settings > Privacy Policy now includes **mathurrohitash16@gmail.com** and **https://falley-privacy-policy.vercel.app/**, with the original 16 October effective date and a 23 October update date. The offline policy also explains voluntary Gmail support correspondence and separate Vercel website requests, matching the public page.

Two fixed footer buttons open the public policy in the user's browser or compose an email in the user's email app. No message or gameplay data is sent automatically. Devices without a suitable handler get a localized message. Long addresses wrap within the document even at Android font scale 1.6. English, Hindi and Bahasa Indonesia include the new copy, labels and accessibility text. Gameplay, records and controls are unchanged.

## Local delivery

Directory: `C:\Users\mathu\Documents\Codex\2026-10-23\Falley-3.3.1`.

| File | Identity | Bytes | SHA-256 |
| --- | --- | ---: | --- |
| `Falley-3.3.1-debug.apk` | `com.perfgames.falley.motiontest`, `3.3.1-privacy-test`, code 11 | 345552 | `66e2d4c1776124a1ace16bcfb5abc890498bf3d3d0eee7cca2da80fce97ee615` |
| `release/Falley-3.3.1-beta.aab` | `com.perfgames.falley`, `3.3.1`, code 11 | 322208 | `fe8128c3fd48252413081cb561556fdf956b3a350e4cc76febd713196ccaaf0f` |
| `release/Falley-3.3.1-beta-local.apk` | Bundle-derived upload-signed local install | 341510 | `013a809451a5046ea580198164cf0845affa5ad226dfb2b3782d7764b100440f` |

The debug app updates the existing Falley Motion Test package using its preserved debug key. The upload-signed release APK is separate; a later Play-signed app may use a different signing certificate. Release generation preserves the existing upload certificate SHA-256 `16cc163611d6a0b2f13474470ed8328e7e5093ba8c5af34ef0701ebf423f9caa`. APKs, AABs, APKS archives and signing files remain excluded from Git.

## Verification and reproduction

- Debug build and APK v2/v3 verification pass.
- Release `lintRelease` and `bundleRelease` pass: **0 errors, 9 warnings**.
- Bundletool validation, strict JAR verification, all **24 payload certificate checks**, derived APK v2/v3 signatures and 16 KiB ZIP alignment pass. Existing jarsigner ZIP/archive-order warnings remain; this is not a zero-warning claim.
- Portable language selection, fallback, formatting and whole-catalog checks pass: **2,276 assertions**.
- Read-only source review found no actionable defect in link handling, localization, wrapping, accessibility bounds or version metadata.

The dedicated native `-PrivacyOnly` suite checks real Settings navigation, all three languages, scrolling to the literal URL and contact, large-font bounds, accessibility text/buttons and both Back paths. Intent monitors intercept browser/email requests to verify destinations without opening external activities or sending email. It also inspects the installed app's requested permission list.

Native Android 15/API 35 acceptance **passes** for both exact APKs above, at **480x800 and 720x1600**, using Android font scale **1.6** and all three languages. Debug records **2,407 checks / 24 captures**; signed release records **2,400 checks / 24 captures**. The combined **4,807 checks / 48 captures** include seven debug fixture-cleanup checks. Root and QA screenshot review found no address overflow or overlap with the fixed footer. Preferences, display settings and font scale were restored, the instrumentation package was removed, and the owned emulator was stopped. No email was sent; real-browser/mail-app compatibility on physical phones remains a useful acceptance check.

Build and localization receipts, native evidence and source hashes are under the delivery directory's `verification/`. These binaries were built before the privacy source was committed, from a working tree based on Git HEAD `e343348e11e1d520fb6a886882cbf70da2f65cf8`; their embedded base revision alone does not identify the privacy changes. `verification/SOURCE-SHA256SUMS.txt` records the original application source/configuration bytes used by those builds. [Curated repository evidence](../screenshots/v3.3.1/README.md) preserves that manifest, the four passing native receipts, localization output and selected screenshots. Gameplay physics suites are carried forward from 3.3 rather than rerun for this privacy-only change.

```powershell
.\scripts\build-android.ps1 -SdkRoot 'C:\path\to\android-sdk' -JdkRoot 'C:\path\to\jdk-17' -ApplicationId com.perfgames.falley.motiontest -AppLabel 'Falley Motion Test' -VersionName '3.3.1-privacy-test' -VersionCode 11 -OutputApk 'C:\local-output\Falley-3.3.1-debug.apk'
.\scripts\build-release.ps1 -SdkRoot 'C:\path\to\android-sdk' -JdkRoot 'C:\path\to\jdk-17' -BundletoolJar 'C:\path\to\bundletool-all-1.18.3.jar' -OutputDirectory 'C:\local-output\release'
.\scripts\test-ui.ps1 -PrivacyOnly -AppPackage com.perfgames.falley.motiontest -FontScale 1.6
.\scripts\test-ui.ps1 -PrivacyOnly -ReleaseSigning -AppPackage com.perfgames.falley -FontScale 1.6
```

Install the corresponding APK on an emulator before running its UI check. Preserve the existing signing identity; do not initialize a replacement upload key.

## Publishing tasks still outside this update

The owner must enter the real policy URL and support email in Play Console, review audience/content rating and Data safety against the final artifact, update old store copy/artwork where needed, and complete physical-phone acceptance. No ads or consent SDK has been added. [Privacy publishing notes](store/privacy/PUBLISHING_NOTES.md) contain the confirmed fields; [3.3 controls notes](CONTROLS_UPDATE_3_3.md) retain prior gameplay evidence and limits.
