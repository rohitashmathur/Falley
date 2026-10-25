# Falley

An offline Android physics game by **Perf Games**. Fall through elastic ropes, steer with phone motion or swipes, dodge hazards and land on the target.

**Latest testing checkpoint: 4.2.1 / code 15 — debug only.** APKs, bundles, mappings and signing keys remain local. No new Play bundle or rollout is part of this checkpoint.

- 24 Campaign stages, six chapter finales and optional mastery stars.
- Daily Drop with local streaks and an explicitly shared result card.
- Arcade survival with selectable hazard cards and mixed ropes from Wave 2.
- Human starter and six earned avatars with distinct cosmetic jump styles; new rewards offer **Use new** or **Keep current**.
- Classic, Spring and Static ropes appear within Campaign/Arcade courses. Colour and symbols explain the surface; Daily stays Classic.
- Touch or motion steering, English/Hindi/Bahasa Indonesia and independent music/effects controls.

## Testing

Install the local **Falley Motion Test** debug APK to keep it separate from the production package. Preserve its signing key for compatible updates. Testers should focus on motion fairness, avatar choices, readable controls, rope effects and whether retries feel enjoyable.

Current results and limitations: [Build status](BUILD_STATUS.md). Reproduction: [Development](docs/DEVELOPMENT.md). Focused checklist: [4.2.1 beta notes](docs/BETA_4_2_1.md). Older build history: [archive](docs/history/BUILD_STATUS_THROUGH_4_2.md).

## Support and privacy

- Support: [mathurrohitash16@gmail.com](mailto:mathurrohitash16@gmail.com)
- Privacy: [falley-privacy-policy.vercel.app](https://falley-privacy-policy.vercel.app/)
- Production package: `com.perfgames.falley`
- Local test package: `com.perfgames.falley.motiontest`

No accounts, ads, purchases, analytics or Internet permission. Progress/preferences stay on the device; uninstalling or clearing storage removes them. The country hint is temporary and never chooses a language for the player. Daily resets at 00:00 UTC and has no server-verified leaderboard.

The updated [privacy website source](policy-site/) must be deployed to the public site before distributing the matching beta. Source publication does not deploy Vercel. Physical-phone feel/audio/performance, fluent-speaker review and TalkBack remain beta acceptance checks.
