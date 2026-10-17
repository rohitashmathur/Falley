# Falley 3.3.0 — controls and campaign correction

Prepared 17 October 2026. Android only, offline and ad-free. Source publication is authorized; APKs/AABs and signing material remain local. No Google Play upload is included in this task. Prior debug notes in `LOCAL_TEST_BUILD.md` describe the superseded code 9 jump rule and unlocked campaign, not this build.

## Player changes

- Up to two quick jumps in a burst. Another burst becomes available while falling, without requiring a rope landing. Recovery needs at least 0.90 simulation seconds since the last jump and descent 45 world units below the first jump's starting height. This forces downward progress instead of allowing hovering at the top. The existing 0.30-second tap cooldown and lethal ceiling remain.
- Each campaign chapter allows 8/10/12/14/16/18 total manual jumps per stage. The footer separately shows burst charges, total remaining and recovery status. Landings/refills never restore spent total allowance. Arcade has no lifetime jump cap; its burst/descent rule still applies. Automatic rope bounces do not consume manual jump allowance.
- Stage 1 starts open. Completing each stage opens the next; completing a chapter's two stages opens the next chapter. Locks apply to pointer, accessibility and direct activation entry points. Existing scores and sparse later wins are retained; filling earlier gaps makes those records relevant again. Unreachable saved selections clamp to the current reachable frontier.
- Touch air current is 35% of the previous strength, bounded to 35 world units/second, alternating and nonzero. The existing 30%-cut/Wave-2 mild nudge is unchanged. The first two forgiving introductions can complete idle; stages 3–12 still lose without input in the deterministic probe.
- Settings now labels the language globe. English, Hindi and Bahasa Indonesia remain offline and persistent.
- How to Play replaces the overlapping numbered rows with three illustrated pages: control gestures, jump/recovery/budget, and danger/landing rules. Touch/Motion tabs open on the actual control mode; browsing them does not change gameplay settings. Text wraps within separated content panels and accessibility reads only the current page.

## Build identities

Source release metadata is **3.3.0 / code 10**, package `com.perfgames.falley`, minimum API 24 and compile/target API 36.

The compatible debug test build is **3.3.0-controls-test / code 10**, package `com.perfgames.falley.motiontest`, label **Falley Motion Test**. It updates the earlier motion-test app and preserves its preferences/records. The production package is separate. Debug APK: `C:\Users\mathu\Documents\Codex\2026-10-17\Falley-3.3.0\Falley-3.3.0-debug.apk`, 341,456 bytes, SHA-256 `62AE017D69D6752E3CE8EC0F8D48F97311CCB7F01E475E6D1DE821B7F8DF6AE4`; v2/v3 signatures pass.

From the source repository with JDK 17, SDK platform 36/build-tools 35.0.0:

```powershell
.\scripts\test-physics.ps1 -JdkRoot 'C:\path\to\jdk-17'
.\scripts\build-android.ps1 -SdkRoot 'C:\path\to\android-sdk' -JdkRoot 'C:\path\to\jdk-17' -ApplicationId com.perfgames.falley.motiontest -AppLabel 'Falley Motion Test' -VersionName '3.3.0-controls-test' -VersionCode 10 -OutputApk 'C:\local-output\Falley-3.3.0-debug.apk'
.\scripts\build-release.ps1 -SdkRoot 'C:\path\to\android-sdk' -JdkRoot 'C:\path\to\jdk-17' -BundletoolJar 'C:\path\to\bundletool-all-1.18.3.jar'
```

Release generation reuses the existing private upload identity. Do not initialize a replacement key. Default outputs derive from the Gradle version under ignored `releases/3.3.0/`; `-OutputDirectory` can select another local directory. Older 3.2 artifacts remain untouched. Preserve the ignored debug and upload identities for compatible updates. A local upload-signed APK is different from a later Play-app-signing APK; prefer the compatible debug test build for current phone feedback.

## Verification

All 13 portable test classes pass, **41,965,180 assertions** across the broad campaign run and corrected remaining run. Campaign and Arcade each pass 100,000 seeded stability steps. All twelve natural courses complete through real gravity/ropes and bounded steering; mixed Arcade survives 65 seconds at all three difficulties. New cases prove more than two airborne uses without a rope, 24 accepted Arcade jumps, finite 8–18 campaign allowances, no refill by pause/control switches, real downward progression, prefix unlocks, migration and all 211 translation keys/format arguments.

Receipts: `build/control-correction/physics-suite.log`, `followup.log`, `targeted.log` and `idle-wind-probe.log`. The first run retains an obsolete assertion that every stage must fail idle; after reducing wind intentionally, the first two forgiving introductions can finish idle. The assertion was updated to require input in later stages, while retaining strict nonzero/alternating/max-35 drift checks. The large campaign stress run was not repeated after that test-only correction. Independent read-only source review found no actionable defect in burst recovery, total allowance, unlock migration or release output/version safeguards.

Native debug acceptance, release build/lint/signature receipts, source revision and final artifact hashes will be recorded after completion.

## Limits and next test

Phone motion comfort, haptic/audio quality, human success rates, native-speaker wording and real TalkBack navigation need player feedback. Chapter budgets and reduced drift are tuning choices; check that eight jumps feel generous in the first chapter and longer drops remain fair. Legacy broad native collision fixtures still need migration before claiming a complete broad release regression. Current acceptance uses the focused revision suite and release smoke.

The app still has no Internet permission, analytics, advertising SDK or backend. The public support email/privacy-policy hosting and Play Console audience/rating/Data safety owner gates from the prior release preparation remain open. Existing store artwork/listing drafts predate these mechanics and need updating before a future submission. Release preparation here creates local signed files; it does not publish to Play or establish store approval.
