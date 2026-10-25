# Local campaign, controls and languages test — 17 October 2026

This update is for owner phone testing. Source changes remain uncommitted and unpushed. No release bundle or Play submission was prepared. APK/AAB files and signing keys remain local and ignored by Git.

## Changes

- Two successful player jumps between supporting landings; cooldown taps do not spend a jump. A rope bounce or safe floor contact restores both. Cutting a rope does not restore jumps. Restart starts with two; changing controls preserves the remaining count.
- The arena ceiling kills on contact with the visible head. A fresh single jump fits below it; a second jump near the top can hit it.
- Campaign steering effects begin after **30% of all stage ropes** have been cut, including recycled mounts. Arcade effects begin during **Wave 2**. Both ease into a maximum 12% sensitivity reduction and a gentle alternating nudge, bounded to 12 world units/second. Neutral motion still brakes promptly toward the small current nudge; it has no constant left pull.
- Fresh profiles default to **Loose**. Valid saved presets are preserved, including Balanced or Tight.
- Twelve stages across six chapters. The original six stage identities, layouts and saved-record keys remain stable. Storm passage, Blade foundry and Last descent introduce focused threats before mixed attacks. Chapter pages make every stage selectable.
- System launch artwork and the in-app splash use the Falley name and human avatar. Android 13+ explicitly prefers the branded starting screen for default launch styles. The launcher icon remains the app icon.
- English, Hindi and Bahasa Indonesia selection in Settings, persisted locally; offline menu, help, gameplay, results, privacy and accessibility copy. Hindi uses whole-string font shaping. Branding and numerical values remain consistent across languages.

## Debug build

The test app uses `com.perfgames.falley.motiontest`, the same package and local debug identity as the earlier motion test, so it can update that app while preserving its choices and records. The normal Falley installation is separate. The source release metadata remains 3.2.0/code 7; the portable builder overrides metadata for this local build.

Built local artifact: `C:\local-delivery\2026-10-17\Falley-Campaign-Languages-Test\Falley-Campaign-debug.apk`, version **3.2.0-campaign-test / code 9**, **337,360 bytes**. SHA-256: `4C83BD6992A886C93E3DDC86F8AF2057D0223A0D48AD0495DA0F59142CAF8A7A`. APK v2/v3 signature checks pass. Manifest inspection confirms debuggable, API 24 minimum/API 36 target, optional motion sensors and no requested permissions. Source remains based on `2010f4e618424f31ae6b9e59b28ee0a3e09f0739` with local changes.

Run from the repository root with JDK 17, SDK platform 36 and build-tools 35.0.0:

```powershell
.\scripts\test-physics.ps1 -JdkRoot 'C:\path\to\jdk-17'
.\scripts\build-android.ps1 -SdkRoot 'C:\path\to\android-sdk' -JdkRoot 'C:\path\to\jdk-17' -ApplicationId com.perfgames.falley.motiontest -AppLabel 'Falley Motion Test' -VersionName '3.2.0-campaign-test' -VersionCode 9 -OutputApk 'C:\local-output\Falley-Campaign-debug.apk'
adb install -r C:\local-output\Falley-Campaign-debug.apk
adb shell am start -n com.perfgames.falley.motiontest/com.perfgames.ropelab.MainActivity
```

The ignored `build/debug.keystore` must be preserved for compatible local updates. Never publish that key or the APK. Avoid uninstalling the test app if its local records should be kept.

## Verification and next phone test

Portable acceptance: **all twelve test classes pass**, totaling **41,949,801 assertions** across an initial broad run and a corrected targeted remainder. Campaign and Arcade each include 100,000 seeded stability steps. All twelve campaign courses finish through real gravity, rope recycling/cuts and bounded touch input in 10.12–18.74 simulated seconds. Mixed Arcade survives 65 seconds through Wave 4 at all three difficulties with live attacks and warnings. Focused checks cover two-jump budgeting/refills/ceiling, sensor neutrality/reversal/braking, strict 30%/Wave 2 onset, bounded alternating nudge, local preference migration and all 183 translation keys with placeholder/fallback integrity.

Receipts: `build/expanded-campaign/physics-suite.log` and `followup.log`. The first log retains a test assertion failure that incorrectly compared a moving nudge's instantaneous target with a lagging servo. The test was corrected to compare paired opposite commands, preserving a strict signed-response check and separate neutral-braking check; the targeted remainder then passed. App code and APK were unchanged by that test correction. The two large stability runs were not repeated unnecessarily.

Native Android 15/API 35 feature acceptance on the final APK: **1,640 checks and 34 distinct PNGs per layout**, at 480×800 and 720×1600; **3,280 checks/68 PNGs total**. Actual touch, drawn text bounds and accessibility nodes verify all three languages, saved language/Tight preset, fresh Loose default, chapter paging/stage 12, double-jump button/cooldown/refills, lethal ceiling and HUD spacing. Hindi shaping, longer Indonesian labels, privacy wrapping and controlled results were visually reviewed. Proof: `build/device/campaign-languages/final-features/{small,tall}` and the matching `small-features.txt`/`tall-features.txt` receipts.

A separate final launcher-style splash check passes **23 checks/2 captures** under `build/device/campaign-languages/final-splash/`. The actual system frame now visibly shows FALLEY plus the avatar/rope; the custom timed frame shows the Home-style avatar, name and Perf Games credit. Initial checks exposed a background-only system frame; the final build adds the supported Android 13+ `icon_preferred` theme setting. Earlier blank-frame receipts and one internal startup-wait fixture failure remain separately under the pre-fix directories and are not accepted artwork proof. The complete final feature runs were repeated on the rebuilt APK.

The final retained motion regression passes **1,746 checks/2 captures** on the final APK at 480×800, covering actual app centering, neutral hold, both tilt directions, braking, restart/resume and touch-to-motion handoff. Proof: `build/device/campaign-languages/final-motion/`. All final focused native checks total **5,049 checks/72 distinct PNGs**. Preferences/display settings were restored, the test harness was removed and the owned emulator was shut down. The APK hash stayed unchanged during these final runs.

Portable full-course controllers demonstrate simulated reachability; they do not establish physical-phone motion comfort or human success rates. A separate read-only code review found no actionable defect in the new jump, ceiling, steering, migration, paging or localization changes. Historical broad release-suite modes were not rerun for this debug update; some older collision fixtures still require migration to the bounded motion model. Current debug acceptance uses the focused feature, splash and motion modes.

On a phone, try one and two jumps near the top, count refills after a light rope bounce versus a cut, and compare steering before/after 30% cuts and across Arcade Wave 2. Try Loose first and confirm that switching presets remains useful. Test all three languages, including Settings, chapter paging and result screens. Have Hindi and Indonesian speakers review wording. The six new stages need player feedback on timing, warning readability and difficulty; no claim is made that all motion-controlled player runs can avoid every hazard.

This remains offline and ad-free. Existing support-contact/privacy hosting and store-submission owner gates remain open. The previous store images and release bundle describe the earlier six-stage build and need revision before any future submission.
