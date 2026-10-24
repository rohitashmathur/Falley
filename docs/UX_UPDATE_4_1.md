# Falley 4.1 — Chapter spotlight UX update

Prepared 24 October 2026. This is an owner-test debug checkpoint of the approved home redesign, applied across Falley's screens. Source publication is authorized, including the preceding campaign expansion. The installable APK stays local; no release APK, AAB, Play submission or website deployment is part of this update.

## What changed

Home now centres on the next campaign drop instead of presenting seven equally weighted buttons. A chapter spotlight shows the selected avatar in original chapter artwork, current stage, chapter progress and the next avatar reward. The main action starts a new campaign or continues the player's progress. Choose Stage stays inside that campaign area. Daily Drop and Arcade are smaller secondary cards with useful status. The persistent root navigation is **Play · Collection · Settings**.

Continue is based on the first unfinished sequential stage, independently of an earlier stage selected for replay. After all 24 stages are complete it offers the first stage with missing mastery stars. Once every star is earned, the selected stage remains available for replay. Completing a chapter removes its avatar from the next-reward preview; after the sixth chapter all avatars are shown as unlocked.

The visual scheme is shared throughout the app: charcoal backgrounds, quiet rounded panels, mint primary accents, gold mastery/reward accents, clear text hierarchy and consistent spacing. Shared vector illustrations depict laser descent, spike canyon, fire, storm, blade foundry and the final core. They use the approved avatar renderer and are cosmetic only. The Home wordmark, rope and avatar move gently; disabling Android's system animations freezes the decorative Home motion. Other chapter illustrations remain still to keep information easy to read.

Screen details:

- **Campaign selection:** illustrated chapter header, four stage cards per chapter, clear locked stages, earned stars, optional jump par, finale labels and the chapter avatar reward. The recommendation receives a mint marker.
- **Stage briefing:** chapter story, artwork, concise mechanic hints, the optional par goal, Touch/Motion choice and an explicit Mirror Practice toggle. Practice does not award campaign records or stars.
- **Daily Drop:** date and UTC reset information, course artwork, streak, attempts and best score, followed by one clear start action. Unlimited retries and clock-change practice status remain visible.
- **Arcade setup:** difficulty and five hazard switches are grouped above one start action. At least one hazard must remain enabled.
- **Collection:** earned avatars, ropes, trails and landing effects sit behind four tabs. Selection, locked state and unlock requirement remain visible; no shop or paid rewards are introduced.
- **Settings:** motion steering and feel are grouped, with a clearly labelled Language row, sound, vibration and blood-effects toggles. How to Play and the privacy policy live here. Entering Settings from pause returns to that paused run.
- **How to Play and Language:** retain the same palette, panels, headers and spacious actions. The six illustrated guide pages distinguish touch from motion and explain burst recovery, hazards, mechanics and mastery.
- **Gameplay:** the header keeps restart/pause apart from progress counters. The jump area distinguishes burst charges, total stage allowance and descent-recovery status. Brief contextual cues appear only during the first campaign stage before it has been cleared; they disappear after the opening seconds or meaningful control input.
- **Pause and results:** matching panels establish a clear primary action. Death results present a cause-specific recovery tip and direct Retry. Campaign wins show named Finish / Jump par / Gold centre goals, with honest New / Earned / Saved / Not yet statuses. Daily sharing remains an explicit player action through Android's share sheet.

## Preserved gameplay and save semantics

This UX update changes presentation and navigation, not physics, collision size, stage difficulty, unlock conditions or avatar advantages. The preceding 4.0 expansion remains included: 24 stages, six machine finales and stories, campaign mechanics/modifiers, Daily Drop, cumulative mastery, earned cosmetics and legacy save migration. See [the historical 4.0 checkpoint](EXPANSION_DEBUG_4_0.md) for that implementation and its earlier exact-build proof.

A won campaign run earns Finish, may earn Jump par with a non-negative manual jump count at or below that stage's par, and may earn Gold centre using the raw first winning landing within the central **35% of the target width**, inclusive of its boundary. These stars accumulate across successful runs. Results keep the current run's evidence separate from prior saved progress; a replay or loss cannot falsely claim an old star as newly earned. Daily, Arcade and Mirror Practice do not mint campaign stars.

Two jumps remain a **burst** limit, with a one-jump burst on the appropriate modifier stages. Descending below the burst's start after the brief recovery delay restores the burst; a supporting rope or pad can restore it sooner. Recovery never refunds the campaign's total stage allowance. Arcade has no overall jump allowance. The short recharge cue says to fall to refill in all three languages; rope contact is not required.

## Local debug build

| Item | Value |
| --- | --- |
| APK | `C:\Users\mathu\Documents\Codex\2026-10-24\Falley-4.1.0-debug\Falley-4.1.0-debug.apk` |
| Version | `4.1.0-ux-test` |
| Version code | `13` |
| Package | `com.perfgames.falley.motiontest` |
| Launcher label | `Falley Motion Test` |
| Size | `386,512 bytes` |
| SHA-256 | `d7b77c23afccbb768de75ad395216d890140092dc47b8853f0c6548b95d2a078` |
| Signature and alignment | APK v2/v3 verification PASS; 16 KiB ZIP alignment PASS |

The local debug signing identity is retained for a compatible update to the existing motion-test app. Install as an update to keep progress; clearing app storage or uninstalling deliberately removes local saves. The production package remains `com.perfgames.falley`. No production artifact was generated. Binaries, build intermediates and signing material are excluded from Git.

## Verification and evidence

All **20 portable test groups** passed. They cover the retained gameplay/progression expansion as well as the new UX rules and localization. Focused UX checks passed separately: **71,120 rule checks** and **1,067 string checks**. The rule checks compare all stage result masks against actual profile recording, including inclusive gold-zone boundaries, invalid/missing evidence, old/current/new stars, recommendations, mastery fallback, avatar rewards and Daily/Arcade isolation. The copy checks cover English, Hindi and Bahasa Indonesia, formatting signatures, delegated catalog entries and control/death/recharge cues.

Native acceptance uses Android 15/API 35 at **480 × 800 / density 240** and **720 × 1600 / density 320**, font scale **1.3**, across English, Hindi and Bahasa Indonesia. Final exact-build native counts, capture lists and any capture-timing corrections are recorded in [the 4.1 screenshot and evidence index](../screenshots/v4.1/README.md); use that receipt rather than provisional counts. Build receipts and full verification outputs are also delivered alongside the local APK. Controlled native fixtures, portable controllers and owner phone playtests are different kinds of evidence; automated completion does not establish human comfort or difficulty.

No app-source changes are intended after the APK identified above. If source changes are necessary after review, rebuild and refresh the artifact identity and relevant proof before handing it off. No release build is required for this checkpoint.

## Focused owner playtest

1. Open with existing progress. Check that Continue points to the first unfinished stage, while Choose Stage still allows an earlier unlocked replay. Return Home after a win and confirm the spotlight and next avatar update.
2. Find Daily Drop and Arcade without searching through a long menu. Open Collection and Settings from the bottom navigation, and find Language and How to Play in Settings. Repeat in Hindi and Bahasa Indonesia.
3. Play the first stage with Touch, then Motion. Check the opening cue, burst dots, total jump allowance, recharge feedback and header spacing on a real phone.
4. Die to two different hazards. Read the tip, retry immediately, and check whether the next attempt feels quick and understandable. Pause, change settings and return to the same paused run.
5. Win once for a new star, then replay without meeting that goal. Confirm the old star reads Saved rather than New; aim for the gold zone to earn another goal on a separate win.
6. Inspect a chapter finale reward, Daily completed state and share preview. Judge whether the home artwork is inviting while the actual danger warnings remain easy to read.

## Known gaps and next steps

- Motion comfort, touch feel, reachability, sustained performance, battery use and fun still need representative physical phones. Emulator sensors and automated play cannot settle those questions.
- Accessibility nodes and actions have automated coverage, but this is not a full accessibility audit. Do a real TalkBack listening/navigation session, check focus order and wording, and inspect very large text/display settings. The custom canvas interface fits some text to available space; the 1.3 font test does not establish arbitrary-size readability or WCAG conformance.
- Sound and vibration preferences are preserved, but listening quality, loudness, timing and haptics require device checks with speakers, headphones and quiet sound settings. No music or new audio assets were added for this UX task.
- Hindi and Indonesian copy has automated format/layout checks, not professional linguistic review. Have fluent players review terminology and comprehension in context.
- No measured onboarding funnel, retention study or playtest success-rate evidence exists yet. Prioritize whether new players find Continue, understand burst recovery and improve after a death before adding more menu content.
- Daily remains offline and resets at 00:00 UTC. It has six bounded templates and local clock/streak safeguards; it has no server validation or global leaderboard. Ads, revives, purchases, paid avatars, donations and backend remain deferred.
- Existing store assets and signed release proof predate this update. The local policy-site draft was not deployed to the public website. After owner feedback, align that site and store copy/screenshots, review policy requirements, then separately authorize and test a signed store bundle with an appropriate production version code.