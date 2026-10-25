# Falley 4.2.1 — debug polish checkpoint

Version **4.2.1-beta-test/code 15**, local package `com.perfgames.falley.motiontest`. This checkpoint produces a local debug APK and source publication only. Existing 4.2 signed artifacts remain historical; no new release AAB/APK or Play submission is requested.

## Implemented

- After a newly earned Campaign avatar, show its animated jump plus **Use <new avatar>** and **Keep <currently selected avatar>**. Result/reward/equipment acknowledgements are durable; process recreation recovers a pending offer; replay and Back cannot repeat or bypass it. Existing upgrades acknowledge previously earned avatars without showing a cascade.
- Motion has mild alternating environmental gusts after a grace period, easing into the established late-cut/Wave 2 challenge. Sensor calibration stays centered. A breeze arrow shows the direction and the final target approach becomes calmer.
- Home/Collection/Settings reserve a shared bottom navigation region with space above it, based on the supplied 720×1600 Redmi 13C screenshots. Insets and visual spacing are separate.
- Campaign contains individually marked Classic/Spring/Static ropes, introduced gradually. The first three and last three finite mounts are Classic. Spring appears from Stage 1; Static from Stage 3. Arcade introduces newly recycled special mounts from Wave 2. No global rope equip setting changes the course; old saved selection remains retained only for compatibility. Collection's Ropes tab is an ability guide.
- Spring boosts supporting-rope rebound 1.5× with ceiling headroom protection. Static charges one 0.8× manual jump and brief 0.84× steering damping. A later Classic support does not erase the current. Manual jump bursts/budgets, lethal ceiling and chapter-dependent missed-floor rules remain intact.
- Arcade uses selectable difficulty and hazard cards, real previews/check states, Touch/Motion controls and an explicit rope-mix explanation.
- Original Demon/FALLEY legacy, adaptive, themed and splash branding, Play icon and promotional illustrations. Gameplay listing captures must come from the actual current app, with provenance retained.
- Short current README, archived historical build status and source-only CI checks. No licensing permission is invented; the repository's licensing policy is unchanged.

## Verification scope

Portable tests cover actual mixed-course clears in both controls, Arcade through Wave 4 at all difficulties, deterministic schedule/reset behavior, bounded/no-bias gusts, one-use Static support, Daily equivalence, avatar reward migration and saved-choice failure/retry. Native fixtures cover visual layout, public actions, reward persistence, rope feedback and EN/HI/ID. Final debug receipts and checksums are recorded in [Build Status](../BUILD_STATUS.md). Native API 35 passes 22,151 checks/ 184 captures; API 25 passes 11,000 checks/ 92 captures. Both original preference/display/font states were restored and the owned emulators stopped. Physical-phone acceptance remains distinct.

Daily Drop is intentionally unchanged: generator family stays at version 2. A comparison against published 4.2 verifies identical float/event/cut traces for multiple seeds and both control paths. Campaign stars, local records and existing avatar selections are preserved. Existing Arcade personal bests are retained; rules have changed, so old and new beta runs are not a controlled balance comparison.

## Owner phone checklist

1. Update the debug app without uninstalling; verify the selected avatar and earned progress survive.
2. Complete a new chapter with Demon or another nonstarter selected. Try both Use and Keep on separate fresh unlocks; reopen during an offer; confirm boss replay does not repeat it.
3. Compare Loose/Balanced/Tight on Redmi. At rest the breeze should be slight and alternate directions; opposite tilt should overcome it easily.
4. Identify Spring/Static by colour and symbol. Check warnings and the final landing remain readable/fair.
5. Try Arcade hazard cards, disabled-all protection, Touch/Motion and Wave 2 rope changes.
6. Check Home/Collection/Settings in gesture and three-button navigation and all languages; background/resume/Back/music; sustained play, heat and TalkBack.

Publish only after deploying the updated policy and verifying the actual new debug on a phone. Additional modes, stages, ads, billing and new rope abilities remain deferred until feedback.
