# Falley

An offline Android physics game by **Perf Games**. Fall through elastic ropes, steer with phone motion or swipes, dodge attacks and land on the target.

**Current debug checkpoint: 4.3.0 / code 16 — local debug testing.** Installable files, mappings and signing keys stay local; this checkpoint does not prepare a Play release. Firebase Crashlytics and Analytics are planned separately for **4.4** and are not included.

- 24 Campaign stages across six chapters, mastery stars and earned avatar rewards.
- One Campaign action on Home opens a combined chapter/stage picker and briefing, with one Start action. Later stages unlock by completing the previous stage.
- Readable boss attack cues: working gear rigs in the first two chapter finales; large side-mounted Demon/Robot attackers in later finales.
- Seven avatars with distinct jumping styles **and gameplay abilities** in Campaign/Arcade. New unlocks offer Use new or Keep current.
- Individually marked Classic, Spring and Static ropes. Arcade survival offers selectable hazard cards; Daily Drop uses fixed standard avatar stats and Classic ropes for everyone.
- Motion or touch steering, responsive phone/tablet layouts, English/Hindi/Bahasa Indonesia and independent retro music/effects controls.

## Test the debug build

Use the local **Falley Motion Test** APK to keep testing separate from the production app. Update it without uninstalling to retain progress, using the preserved debug signing identity.

Read [current build results](BUILD_STATUS.md), [4.3 phone/tablet checklist](docs/BETA_4_3.md) and [build/run instructions](docs/DEVELOPMENT.md). Current native screenshots and receipts belong under [4.3 evidence](screenshots/v4.3/README.md). Earlier results are explicitly historical: [4.2.1](docs/history/BUILD_STATUS_4_2_1.md), [through 4.2](docs/history/BUILD_STATUS_THROUGH_4_2.md).

## Support and privacy

- Support: [mathurrohitash16@gmail.com](mailto:mathurrohitash16@gmail.com)
- Privacy: [falley-privacy-policy.vercel.app](https://falley-privacy-policy.vercel.app/)
- Production package: `com.perfgames.falley`
- Local test package: `com.perfgames.falley.motiontest`

No accounts, ads, purchases, analytics or Internet permission. Progress/preferences stay on the device; uninstalling or clearing storage removes them. The temporary country hint never selects the player's language. Daily resets at 00:00 UTC and has no server-verified leaderboard.

The [privacy website source](policy-site/) must match the public policy before distributing a beta; pushing Git does not deploy Vercel. Physical-device motion comfort, balance, audio, performance, fluent-speaker feedback and TalkBack remain owner acceptance checks.
