# Falley build status

**Current: 4.2.1-beta-test / code 15 — local debug testing only**, prepared 25 October 2026. Package: `com.perfgames.falley.motiontest`. Source checkpoint: `09108128247ec41bc9d0ef3cf8b09419778e0e59`; subsequent harness/evidence updates do not change the APK. One corrected source-comment difference is explicitly proven bytecode-identical in the receipt.

## What changed

- Animated, once-per-new-avatar reward choice: Use the unlocked avatar or Keep whichever avatar is selected. Saved acknowledgement/equipment, restart recovery and old-save migration are covered.
- Mild alternating motion gusts with visible cues, centered calibration and a calmer landing approach.
- Shared bottom-tab spacing based on supplied Redmi 13C screenshots.
- Individually marked Classic/Spring/Static ropes in Campaign; new Arcade mounts mix from Wave 2. Collection explains the course mechanics. Daily physics stays unchanged.
- Card-based Arcade difficulty/hazard choices, Touch/Motion selection and at-least-one-hazard protection.
- Original Demon/FALLEY launcher/adaptive/themed/splash branding; current icon, promotional illustrations and six actual native store frames.
- Short README, archived history, current GitHub description and source-only CI.

## Local debug deliverable

`C:\local-delivery\2026-10-25\Falley-4.2.1\Falley-4.2.1-debug.apk` (portable example path), **1,120,186 bytes**. SHA-256: `487e5304c912ef4a061594b9357e2727c5e524a9d3af856c0b745009faef2f24`.

Debug compilation, package/version/min24/target36 inspection and APK v2/v3 signature checks **PASS** with the preserved debug identity. No APK/AAB, signing key, mapping or generated intermediate is tracked. **No new release APK/AAB, release lint/R8 bundle verification, Play upload or Vercel deployment was performed.** The older 4.2 signed files do not validate this checkpoint for submission.

## Completed checks

- **29 portable groups PASS**, including all 24 mixed-rope Campaign courses in touch/motion and 65-second Arcade runs at all 3 difficulties/both controls. Bounded gust/no-directional-bias and support/cut/reset safety pass.
- **11,489 translation checks**, **704 avatar-model / 32 durable-storage checks**, **56 music/controller checks** pass. Daily remains bit-identical to published 4.2 across 11 seeds/both controls/3 input patterns: **48,471,122 float checks**.
- Android 15/API 35: **22,151 checks / 184 captures**, 480×800 and 720×1600, font 1.3, EN/HI/ID. Native reward choices include new Angel vs selected Demon, both choices/Back/recreation, no replay/cascade; actual rope contact, Arcade cards and independent audio pass.
- Android 7.1/API 25: **11,000 checks / 92 captures**, small layout, same APK. Legacy Back/audio focus/background/resume and UX pass. Old monitor verifies blocked chooser/provider access; deep share-intent flags are API 35-only proof.
- Both runners restore preferences/display/font, remove instrumentation and stop the owned emulators. **10 store upload assets validate** against dimension/mode/hash/crop checks. Captions and controlled-fixture provenance match the final debug app.

Receipts and selected images: [4.2.1 evidence](screenshots/v4.2.1/README.md). Full raw logs/captures and the APK stay local. [Phone checklist](docs/BETA_4_2_1.md) covers sensor feel, performance, audio audibility, TalkBack and fluent-speaker feedback. These tests do not certify every Android version or physical Redmi behavior.

## Before beta publication

Test this debug on the owner's phone first. Redeploy the updated [policy-site source](policy-site/) to the existing Vercel URL; no Vercel authentication is configured here. The public site was confirmed to contain older 23 October wording and still needs the prepared country/Daily/music/records disclosures. Monetization remains deferred. CI is source-only; its hosted status is recorded separately after publication.

[Development](docs/DEVELOPMENT.md) · [Current beta notes](docs/BETA_4_2_1.md) · [Archived build history through 4.2](docs/history/BUILD_STATUS_THROUGH_4_2.md)
