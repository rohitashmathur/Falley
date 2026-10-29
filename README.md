# Falley

An Android physics game by **Perf Games**. Fall through elastic ropes, steer with phone motion or swipes, dodge attacks and land on the target. Gameplay works offline.

**Current: 4.4.1 / code 18 — expanded Firebase tracking; debug testing.** Only a debug APK is prepared for this revision. Release bundles, APKs, mappings and signing files stay local. Exact build/test status is in [Build Status](BUILD_STATUS.md).

- 24 Campaign stages across six chapters, mastery stars and earned avatar rewards.
- One Campaign action opens combined chapter/stage selection and briefing; later stages unlock by completing the previous stage.
- Working gear rigs in the first two finales and large side-mounted Demon/Robot attackers later.
- Seven avatars with different jump, weight and attack-resistance abilities in Campaign/Arcade. Daily uses standard physics for everyone.
- Mixed Classic, Spring and Static ropes; Arcade hazard cards; Daily Drop and explicit result-image sharing.
- Touch/motion controls, phone/tablet layouts, English/Hindi/Bahasa Indonesia, retro music and independent effects controls.
- **Crash reports** and **Gameplay analytics** are independent switches in Settings → Diagnostics & analytics. Both default on when no choice is saved; explicit opt-outs are preserved. First launch explains reporting.

## Build and test

[Build/run instructions](docs/DEVELOPMENT.md) explain local configuration and debug/release builds. [Firebase 4.4 notes](docs/FIREBASE_4_4.md) describe consent, events, configuration, mapping and console checks. Source-only CI runs portable gameplay, save, privacy and audio checks without Firebase JSON or signing keys.

Use **Falley Motion Test** for debug testing, separate from the production app. Update without uninstalling to retain progress and reuse the existing debug signing identity. [4.3 phone/tablet checks](docs/BETA_4_3.md) remain the gameplay acceptance checklist; [4.3 evidence](screenshots/v4.3/README.md) is historical and does not validate a 4.4 binary. Current results and evidence are linked from [Build Status](BUILD_STATUS.md).

## Support and privacy

- Support: [mathurrohitash16@gmail.com](mailto:mathurrohitash16@gmail.com)
- Privacy: [falley-privacy-policy.vercel.app](https://falley-privacy-policy.vercel.app/)
- Production package: `com.perfgames.falley`
- Debug package: `com.perfgames.falley.motiontest`

No accounts, ads or purchases. Progress/preferences stay on the device. Optional reporting can send limited diagnostics, gameplay events and SDK installation/device information to Google for Perf Games. Both switches off at startup means Firebase is not initialized. Analytics changes apply immediately; crash-report changes fully apply next launch and later opt-in may send cached reports. Turning reporting off does not delete information already received by Google.

Gameplay analytics now includes individual jump attempts, swipes, sampled raw motion/touch input, menu transitions, language changes, earned avatars, rope encounters and Share Result taps. Samples are bounded and handed to Firebase in background batches. The full save file, temporary country hint and Daily seed/date remain excluded. Analytics may separately infer approximate geography from network information. Daily resets at 00:00 UTC and has no server-verified leaderboard.

Redeploy the [updated policy website source](policy-site/) and review Play Data safety before distributing 4.4.1; a source push does not confirm a matching live policy or Play Console update. Physical-device motion, balance, audio, performance, fluent-speaker feedback and TalkBack remain owner acceptance checks.
