# Falley 3.2.0: Google Play beta submission pack

**Publication update — 23 October 2026:** The owner has supplied **mathurrohitash16@gmail.com** and published the [privacy policy](https://falley-privacy-policy.vercel.app/), verified in a browser without a login. Those two missing-input gates below are resolved. The contact and public URL are also included in the new **3.3.1/code 11** local debug/release builds, with native checks passing; see [current privacy build notes](PRIVACY_UPDATE_3_3_1.md) and [publishing fields](store/privacy/PUBLISHING_NOTES.md). The remainder preserves the original 3.2 guide and older receipts; use the current local artifact rather than its code 7 bundle. No Play Console submission or approval is implied.

Prepared **16 October 2026**, version **3.2.0 / code 7**, application ID `com.perfgames.falley`. Scope: an Android phone beta that stays **offline and ad-free**. “CPP” is treated here as Google Play store/promotion preparation. No Console app, tester list, public policy page or store release has been created or uploaded by this work.

## What is ready locally, and what still needs the owner

The store copy, content-answer drafts, reviewer instructions, feedback/invitation templates and policy publication draft are under [`store/`](store/README.md). Prepared artwork is under [`../store/assets/`](../store/assets/manifest.json). **[`../BUILD_STATUS.md`](../BUILD_STATUS.md) is the source of truth for the final release artifact, checksum and verification.** Treat earlier 3.1.0 results as historical evidence rather than proof for the current 3.2.0 binary.

| Gate | Current status / action |
| --- | --- |
| Support email | **BLOCKED: owner has not set one up.** Supply a real monitored address for the app's Play contact details and privacy inquiries. |
| Public privacy-policy URL | **BLOCKED: no site/policy has been published.** Finish the local HTML draft and host it publicly. A general support website is optional; the policy still needs a public URL. |
| In-app privacy contact | **Owner input pending.** The offline Privacy Policy screen uses current facts without fake contact details. Once the email is real, add a real contact mechanism and align with the public policy. |
| Legal account and package ownership | Unknown: verify Console identity/device/package registration and account access. Confirm this is the intended permanent package before first upload. |
| Release-signed AAB / QA | Refer to current `BUILD_STATUS.md` and the local delivery receipt. Do not treat a debug APK as a Play submission. |
| IARC and audience | Draft inventory provided; actual questionnaire, calculated rating and intended audience remain owner/Console tasks. No rating certificate is implied. |
| Real-phone acceptance | Required before widening the cohort: motion, touch, sounds, interruptions, readability and mixed-hazard fairness. |
| Advertising | **Excluded from this beta.** No SDK, live ad IDs, consent flow or monetization claim is included. Treat ads as a later release with new privacy/declaration work. |

Support email is a required listing contact. The app-setup page also notes that free games may appear on Play Games on PC by default; check available platform/device settings and keep this beta's distribution aligned with tested Android phones. [Create/set up a Play app](https://support.google.com/googleplay/android-developer/answer/9859152?hl=en)

For closed beta, a no-data app still completes Data safety and supplies a privacy-policy link; an app exclusively on internal testing is exempt from that form. Use the prepared policy notes and content draft rather than assuming the in-app screen is sufficient. [Data safety requirements](https://support.google.com/googleplay/android-developer/answer/10787469?hl=en)

## Upload sequence once the blockers are resolved

1. **Check the account.** The owner signs in to Play Console, confirms identity/device verification and package-registration tasks, and establishes whether the account is personal or an organization. Do not create a fictitious company to bypass account requirements. No account status was inspected here.
2. **Create or select Falley.** Confirm default language `en-US`, type Game, intended price choice, branding and support email. Review the permanent package ID `com.perfgames.falley`; uploading the first bundle fixes its association with the app entry.
3. **Review the local release receipt.** Select the upload-signed **3.2.0/code 7 AAB** identified by the final build report. Confirm it is non-debuggable, target API 36, minimum API 24, has optional sensors and no Internet/ad identifier permission or ad/analytics SDK. Preserve the private upload key and local backup; never commit keys or binaries. Android 16/API 36 is the currently required target for new mobile submissions/updates. [Target API policy](https://support.google.com/googleplay/android-developer/answer/11926878?hl=en)
4. **Start with Internal testing.** In Test and release > Testing > Internal testing, create the release, configure Play App Signing, upload the AAB, add the release notes and review every reported error/warning. Add a small adult QA cohort and a real feedback channel, then deliberately roll out to that track. Install from Play and check clean install plus later updates. Internal testing supports up to 100 testers and can start before full setup; it is not production approval. [Testing-track setup](https://support.google.com/googleplay/android-developer/answer/9845334?hl=en)
5. **Complete the listing and app content.** Copy the title/descriptions, upload the primary icon/feature graphic and phone screenshots, enter alt text and real support/privacy details. Use `APP_CONTENT_DRAFT.md` and `REVIEWER_ACCESS.md`, then complete IARC and intended-audience choices in the actual forms. Confirm all policy/app-dashboard tasks; avoid speculative answers to new conditional questions. [App dashboard setup](https://support.google.com/googleplay/android-developer/answer/9859454?hl=en)
6. **Prepare a Closed testing track.** After internal install/privacy/input checks pass and contact/policy/audience tasks are complete, configure the intended country availability and invited tester eligibility. Add the same verified bundle from the artifact library or upload a new higher-code build if anything changed. Use the closed-beta release notes and owner-reviewed declarations. Inspect the release summary and submission status before rollout; leave Production untouched. [Prepare and roll out a release](https://support.google.com/googleplay/android-developer/answer/9859348?hl=en)
7. **Share the real opt-in link.** Fill the unsent invitation template. A tester must use an eligible Google account and opt in; do not assume receipt of an email installs the game. Internal participants moving to closed testing must leave internal testing first. Use one real feedback channel and gather specific device/control/audio reports. [Testing-track setup](https://support.google.com/googleplay/android-developer/answer/9845334?hl=en)
8. **Review results without daily noise.** Use the short test plan in `FEEDBACK_TEMPLATE.md`, review pre-launch crash/ANR/accessibility findings, retain actual feedback and retest fixes. Test on actual phones, offline, through interruptions, and in both control modes. Keep the beta ad-free while assessing fun and fairness.
9. **Close the readiness gate before production.** If this is a personal developer account created after 13 November 2023, at least **12 testers must stay opted in continuously for 14 days** before applying for production access. Actual participation/feedback and a readiness application are still required; count/time alone is not approval. Older personal/organization accounts may follow different gates. Check the owner's Console, not assumptions. [New personal-account testing rules](https://support.google.com/googleplay/android-developer/answer/14151465?hl=en)

The first Play-signed installation may not update a sideloaded app if its signing key differs. This applies to older debug copies and to the local upload-key-signed beta APK when Play uses a different app-signing key. If Android reports a signature mismatch, uninstall that local copy before installing from Play; this deletes its app-local scores/settings. Future Play updates should preserve those records. Do not change the final application ID just to work around a test-device signature mismatch.

## Release acceptance checklist

- [ ] Local receipt identifies the exact AAB and private upload certificate; AAB validation, generated APK installation, version/manifest inspection and relevant source/native checks pass.
- [ ] The Play-produced install opens, has the correct icon/version, and starts without network access or credentials.
- [ ] The Privacy Policy entry is reachable, readable, factually correct and includes the completed contact mechanism; the public policy URL works outside the owner's signed-in browser.
- [ ] Motion availability/fallback, Loose/Balanced/Tight settings, touch countersteering/jump and settings/records behave correctly on real phones.
- [ ] Fire, laser, lightning and saw sounds are distinct and comfortable; mute/background/restart behavior works on speakers/headphones. No claim of human listening acceptance is based only on playback counters.
- [ ] All stages and the five Arcade hazards are available; telegraphs, deaths and later target misses match the declarations. Screenshots show the actual app.
- [ ] The owner has reviewed IARC, audience, Data safety, no-ads and app-access answers and any country restrictions.
- [ ] Feedback/triage channel exists, testers understand the content and test scope, and the required cohort/opt-in period is tracked if applicable.
- [ ] No SDK, permission, network feature or advertisement has been added without re-auditing the offline policy/declarations.

Nothing in this pack guarantees Google approval or an age rating. Release binaries and signing material stay local; public GitHub source, documentation and store graphics may be shared under the repository rules.
