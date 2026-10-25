# Falley 4.3 — avatar abilities and responsive Campaign

Version **4.3.0-beta-test/code 16**. Local package `com.perfgames.falley.motiontest`, using the existing debug signing identity. Source publication and a local debug APK are the deliverables. No release APK, Play bundle, upload or Firebase integration is part of 4.3.

## Play changes

- Motion's opening alternating breeze increases from 6 to 10 world units/second; its late-cut/Wave 2 challenge increases from 12 to 18. It remains separate from sensor calibration, starts after a grace period, changes direction smoothly and eases near the Campaign target. Touch's existing drift is unchanged.
- The first chapter finale warns before an alternating half-arena laser grid. The second has a mounted blade column with its full sweep advertised. Existing safe gaps, lethal collisions and final landing clearance matter; bouncing out of a thin hazard row no longer bypasses these two attacks.
- Later chapter finales replace decorative gears with large side-mounted Furnace/Core Demons and Storm/Forge Robots. Charging, muzzles and firing animation use actual hazard state; attacks are aimed once and do not home onto the player after the warning.
- Home keeps exactly six actions: Campaign, Daily, Arcade and the three existing bottom tabs. Campaign opens one combined chapter/stage selector and briefing. Four stage choices sit beside their instructions, goals and controls; one Start action launches the drop. Locked stages cannot start. Back returns Home without a separate Chapters detour. The result screen's Chapters action opens the combined selector directly; Home remains a separate destination.
- Home card icons and text have separate rows. Menus use available safe height, cap text-column width on tablets and anchor bottom navigation to the actual viewport. Gameplay keeps the same world scale and lethal geometry while showing additional lower arena where space permits. Modern system/caption/cutout/touch insets reserve the system UI area. Tablet rotation pauses and preserves the current run in place.
- Avatar Collection and unlock screens describe real traits alongside original stories. Unlock offers still compare the new avatar with whichever avatar is equipped. Saves, stars and existing records are retained.

## Avatar rules

The height column describes a manual tap jump relative to Human at the same score and gravity. Velocity uses the square root of that multiplier; it is not a 20% velocity boost. Weight changes rope loading independently of the shared score-based collision size.

| Avatar | Tap-jump height | Rope load | Resistance |
| --- | --- | --- | --- |
| Human | 1.00x | 1.00x | None |
| Demon | 1.20x | 1.00x | None |
| Angel | 1.20x | 1.00x | None |
| Military | 0.85x | 1.25x | None |
| Astronaut | 1.35x | 0.75x | None |
| Robot | 1.00x | 1.15x | Fireball projectiles |
| Shadow | 1.20x | 0.85x | Laser and lightning attacks; fireballs still kill |

**All arena boundaries, spikes, ceiling and fatal missed-floor rules still apply to every avatar.** Robot cannot enter fire walls; Shadow cannot enter laser walls. Resistant projectile contact gets a brief shield response. Two jumps per airborne burst and Campaign total budgets are unchanged. Higher-jumping avatars begin slightly lower so their first ordinary tap has ceiling clearance; jumping again near the ceiling remains dangerous.

Campaign and Arcade use the selected avatar's abilities. **Daily Drop uses Human physics and Classic ropes for everyone**, retains the chosen visual avatar, and never overwrites equipment. Its generator stays at version 2. Historical Arcade records are retained; different avatar abilities make old and new beta scores imperfect balance comparisons.

## Owner acceptance checklist

1. Update the debug APK without uninstalling. Confirm language, chapter unlocks, mastery and equipped avatar survive.
2. On Redmi 13C, inspect Home Daily/Arcade icon spacing and bottom tabs. Open Campaign, inspect chapter/stage choices, enter a locked chapter, return, start a replay and use Back; selection and locks should stay understandable.
3. Compare Loose/Balanced/Tight motion. A neutral phone should drift in alternating directions; small opposite tilt should remain effective, and landing approach should feel calmer. Physical sensor comfort cannot be established on an emulator.
4. Play every avatar in Campaign and Arcade. Compare higher/shorter jumps and rope loading. Try Robot against fireballs, Shadow against beams/lightning, and both against their remaining hazards. All walls and spikes remain dangerous.
5. Clear the first two finales by reading their full warning areas; check later side figures visibly charge and fire their attacks. Safe gaps must remain legible on narrow late arenas.
6. On a real tablet, inspect menus, briefing, collection, settings, Help and gameplay; resize if supported, check navigation bars/taskbar and reachability. Repeat with Hindi/Indonesian and larger font settings.
7. Recheck unlock Use/Keep, double-jump refill/budget, ceiling, restart/pause/background, audio/haptics, sustained frame rate and TalkBack. Have fluent speakers review new copy in context.

## Verification and limits

Completed checks, exact APK hash and current native captures are recorded in [Build Status](../BUILD_STATUS.md) and [4.3 evidence](../screenshots/v4.3/README.md). Controlled fixtures may grant progress or position the avatar to expose a visual/physics state; they are not natural phone-play evidence. Historical store frames retain their original 4.2.1 provenance.

No accounts, ads, billing, revives, Internet access or analytics were added. Firebase Crashlytics/Analytics are reserved for 4.4 after the owner prepares configuration; that future integration will require collection controls and privacy/Data safety changes. The existing prepared privacy website source still needs a matching public deployment before beta distribution.
