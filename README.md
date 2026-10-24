# Falley — 4.1 Chapter spotlight debug preview

An offline Android physics game about falling, bouncing on elastic ropes and reaching a target. Play with phone motion or touch steering. This version is a **debug preview** for owner testing. Source is maintained in the public [Falley repository](https://github.com/rohitashmathur/Falley); APKs and signing material stay local. No release APK or App Bundle was built for this update.

## Interface and navigation

- **Play / Collection / Settings** provide the three main destinations. Home spotlights the next Campaign stage with chapter artwork and one main Start/Continue action; Daily Drop and Arcade sit in smaller cards. Chapters live inside the Campaign card.
- Continue finds the first unfinished stage, then missing mastery stars after Campaign completion. Explicit stage replays do not displace that recommendation. The next chapter avatar is shown as a reward preview.
- All screens share the same dark cards, mint primary action, quieter secondary controls, spacing and icons. Briefings offer Touch/Motion controls; Settings clearly labels Language and includes Help and Privacy.
- Gameplay emphasizes the landing/survival objective and jump charges. First-stage control hints are contextual, results name each independent star objective, and Retry appears shortly after a loss.
- Six chapter illustrations and avatar previews are rendered from native vector code. Home and splash respect the system animation setting.

## What is playable

- **Campaign:** 24 sequentially unlocked stages across six chapters, four stages per chapter. Each chapter introduces a mechanic and ends with a telegraphed machine encounter. Short original chapter stories appear in briefings and finale results.
- **New mechanics:** moving and shrinking targets, wind zones, bounce pads, load-sensitive breakable anchors and slow-time pickups. Campaign twists include low gravity, gusts, heavy rope load and one jump per burst. Optional mirrored steering is clearly marked practice and does not award campaign progress or stars.
- **Replay mastery:** three independent, cumulative stars for finishing, finishing at or below the optional jump par, and finishing inside the central gold 35% of the target. A later run can earn a missing star without repeating every objective in one run. Stars never block normal stage progression.
- **Daily Drop:** one offline UTC-date/version-seeded course, unlimited attempts, separate daily records and completion streaks. It uses six bounded course templates with seeded variation. Results can be shared as a generated image through Android's share menu; nothing is posted automatically.
- **Arcade:** configurable Chill / Standard / Brutal survival with falling fire, top lasers, side lasers, diagonal lightning and buzzsaws. Records remain separate from Campaign and Daily Drop.
- **Earned wardrobe:** Human starter; Demon, Angel, Military, Astronaut, Robot and Shadow unlocked by completing the six chapters. This roster was approved by the owner. Avatar physics and hitboxes are identical. Stars unlock rope colors, trails and landing effects; there are no paid items.
- English, Hindi and Bahasa Indonesia, six illustrated How to Play pages with separate Touch/Motion guidance, persistent sound/haptics/blood preferences, and the existing motion centering and Loose default.

There are normally two quick jumps per burst, with recovery after actual descent or a supporting landing. The one-jump modifier limits that burst to one. Campaign total jump allowances still grow from 8 to 18 by chapter; the optional star par is lower. Daily Drop allows 14 total jumps, while Arcade has no lifetime cap. The ceiling and narrowing side hazards remain lethal. Chapter 1 allows off-target floor recovery; later chapters require landing on the target.

## Local delivery

Current test APK:

`C:\Users\mathu\Documents\Codex\2026-10-24\Falley-4.1.0-debug\Falley-4.1.0-debug.apk`

Version **4.1.0-ux-test / code 13**, package **com.perfgames.falley.motiontest**, label **Falley Motion Test**. It uses the preserved debug key for compatible updates to earlier local motion-test builds. Production package remains `com.perfgames.falley`; no new release APK/AAB was built. See [4.1 interface and verification notes](docs/UX_UPDATE_4_1.md), [the historical expansion checkpoint](docs/EXPANSION_DEBUG_4_0.md) and [build status](BUILD_STATUS.md).

Existing twelve-stage saves are migrated once into the expanded campaign. Previously completed contiguous chapters remain complete; their inserted stages get completion credit only. Old best scores/times are retained on mapped introduction/finale stages. No invented mastery stars are granted.

## Build, test and run

Requires JDK 17, Android SDK platform **36**, build-tools **35.0.0**, and Android 7.0/API 24 or newer. The native test harness additionally needs SDK platform 35 and a disposable emulator.

```powershell
.\scripts\test-expansion.ps1 -JdkRoot 'C:\path\to\jdk-17'
.\scripts\build-android.ps1 -SdkRoot 'C:\path\to\android-sdk' -JdkRoot 'C:\path\to\jdk-17' -ApplicationId com.perfgames.falley.motiontest -AppLabel 'Falley Motion Test' -VersionName '4.1.0-ux-test' -VersionCode 13 -OutputApk 'C:\local-delivery\Falley-4.1.0-debug.apk'
adb install -r 'C:\local-delivery\Falley-4.1.0-debug.apk'
adb shell am start -n com.perfgames.falley.motiontest/com.perfgames.ropelab.MainActivity
```

Alternatively set `ANDROID_HOME` and `JAVA_HOME`. The established `scripts/test-physics.ps1` command delegates to the full portable suite. The builder uses fresh staging and preserves ignored `build/debug.keystore`; preserve that key for future compatible debug updates. A different signing identity cannot update an installed copy. Do not uninstall an existing app merely to bypass a signing mismatch if its progress matters.

Current focused native interface acceptance:

```powershell
.\scripts\test-ui.ps1 -UxOnly -AppPackage com.perfgames.falley.motiontest -Device emulator-5580 -FontScale 1.3 -SdkRoot 'C:\path\to\android-sdk' -JdkRoot 'C:\path\to\jdk-17'
```

The native fixture only runs on an emulator, preserves preferences/display/font settings and removes its instrumentation app after recovery. Controlled native fixtures are distinct from the portable full-course simulations and from human phone playtesting.

The Gradle wrapper pins **8.11.1** and AGP **8.10.1**, with compile/target SDK 36. The existing release workflow is documented in [release signing](docs/RELEASE_SIGNING.md); it is not part of this debug task. Historical signed **3.3.1/code 11** evidence remains in [privacy update notes](docs/PRIVACY_UPDATE_3_3_1.md) and does not validate this expansion for store submission.

## Privacy and remaining work

The game requests no Android permissions and has no accounts, Internet access, advertising, analytics or backend. Sensor data is temporary. Preferences, stars, cosmetics, records and streaks stay on-device; backup/device transfer are excluded. Daily result images use private cache and a read-only, temporarily granted share URI. Clear app data or uninstall to remove local records; independently shared copies are controlled by their receiving apps.

The existing public [privacy policy](https://falley-privacy-policy.vercel.app/) and support email **mathurrohitash16@gmail.com** remain linked in Settings. The local [policy website source](policy-site/README.md) now describes optional Daily Drop sharing and new local records; redeploy it before publishing this expansion. This task does not deploy the website.

Current automated results and curated native screenshots are recorded in [4.1 evidence](screenshots/v4.1/README.md). Phone motion comfort, audio/haptics, frame pacing, subjective difficulty, real TalkBack navigation and native-speaker language review still need owner feedback. Daily Drop resets at 00:00 UTC (05:30 in India), is not server-verified, and cannot prevent clock manipulation after clearing app data. Backward dates are practice-only for records. There are no global leaderboards or notifications. Replay content and rewards provide goals, but retention needs actual playtest results. Ads, revives, donations, purchases and online services remain deferred.
