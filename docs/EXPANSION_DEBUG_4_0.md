# Falley 4.0 expansion — local debug notes

**Historical local checkpoint, superseded by [4.1 UX notes](UX_UPDATE_4_1.md).** Its debug artifact and test receipts remain valid for that exact earlier build; the source is included in the subsequently authorized 4.1 source publication.

Prepared 24 October 2026. Scope: implement the approved retention/gameplay expansion and build a debug APK for owner testing. No release build, Git commit/push, website deployment or Play submission is authorized for this checkpoint.

## Delivered features

- 24 sequential campaign stages in six chapters, with one distinct themed machine finale per chapter and short original chapter intro/outro text.
- Moving/shrinking targets, wind zones, physical bounce pads, anchors that rupture under load and slow-time pickups.
- Low gravity, gusts, heavy rope load and a one-jump-per-burst campaign twist. Optional mirrored steering is practice-only and awards no campaign records/stars.
- Independent cumulative finish / jump-par / central-landing stars, up to 72 total. A star is earned only on a won run; losing does not grant stars or unlock stages.
- UTC-date/version-seeded Daily Drop with six bounded templates, retries, separate bests, completion streaks, rollback handling and an optional image share action.
- Approved Human / Demon / Angel / Military / Astronaut / Robot / Shadow roster, with one avatar per chapter reward after Human. Identical physics and hitboxes. Earned rope palettes, trails and landing effects.
- Expanded EN/HI/ID copy, six illustrated Help pages and stage briefings; existing touch/motion, sound and accessibility controls retained.
- One-time twelve-to-twenty-four-stage save migration. Completed contiguous old chapters remain completed without requiring replay of inserted stages; mapped old scores/times remain, new completion credit grants no invented par/precision stars.

## Debug identity and location

- APK: `C:\local-delivery\2026-10-24\Falley-4.0.0-debug\Falley-4.0.0-debug.apk`
- Version: **4.0.0-expansion-test**, code **12**.
- Package: **com.perfgames.falley.motiontest**; launcher label **Falley Motion Test**.
- Production package remains `com.perfgames.falley`; no production artifact was generated.
- APK size: **370,128 bytes**; SHA-256: `CCA0847706E938371CD82F06ADF1974E9FF11C4EF3B5AE5152865EF7305F8B5E`.
- Preserved local debug signing key. APK v2/v3 signature and 16 KiB ZIP alignment verification pass.
- Exact final SHA-256, input source hashes and native run receipts are recorded alongside the delivery in `verification/`.

Install with `adb install -r` to preserve an existing compatible motion-test installation. Do not clear data unless deliberately testing a clean profile. Unfinished active runs are not restored after process death.

## Rules to test on a phone

1. Try both Touch and Motion; neutral steering should stay centered while touch mode retains a small alternating drift.
2. Replay an early course for its three different stars. Par is optional and does not replace the larger stage jump allowance.
3. Try new chapter mechanics before their finales. The attacks warn first, leave an escape, and retire for the final approach; side hazards and strict target rules still apply.
4. Check your existing progress after updating. Complete a chapter and equip its avatar in Wardrobe; try star-unlocked rope/trail/landing effects.
5. Retry Daily Drop to compare the same course, finish for streak credit and inspect its share image. Daily/Arcade runs must not change Campaign completion.
6. Switch English/Hindi/Bahasa Indonesia and read Help and stage briefings. Report control comfort, unclear warnings, repeated deaths, performance or text issues.

## Verification

**Build PASS:** portable Android builder using JDK 17, platform 36/build-tools 35.0.0; debug v2/v3 signature verification passes. No release lint/AAB was run for this expansion.

**Portable suite PASS:** all **18** test classes, **42,297,497 checks**. Includes actual completion of all 24 courses in touch and motion simulations, all six Daily templates, all six boss warning/live sequences, observable new mechanism effects, both 100,000-step stability checks, 65-second mixed Arcade survival for all difficulties, burst/total jump rules, steering, migration/stars/cosmetics/daily isolation and EN/HI/ID format integrity. Final focused localization checks were rerun after review: **1,249 expansion + 2,324 existing catalog checks PASS**. These overlap the broad suite and are reported separately. These are bounded test controllers, not human success rates.

**Native Android 15/API 35 PASS:** 480x800 at density 240 and 720x1600 at density 320, font scale 1.3, English/Hindi/Bahasa Indonesia: **5,070 checks and 48 captures per layout; 10,140 checks / 96 captures total** on the final APK above. Coverage includes chapter locks, three independent stars earned across actual native landing fixtures, chapter avatar rewards and saved selections, Daily retry seed/result isolation/streaks/recreation, intercepted Android share intent and actual 1080x1350 PNG, provider path/write rejection, mirror practice isolation, six real machine warning phases, new mechanic rendering and twelve-to-twenty-four-stage save migration. No external share destination was opened or contacted.

Root visual review confirms current small/tall briefings, guides, wardrobe, target, wind, pads, anchors, slow pickup, boss warning and share card. Boss status was moved into the header after a marker covered its original label. Controlled fixture captures now advance real frames before capture; earlier stale compositor frames are excluded from delivered proof. An earlier native attempt failed because of a test-only UI-thread invocation; the corrected suite passes. Original emulator preferences/display/font were restored, instrumentation removed and the owned emulator stopped.

Final native receipts and PNGs are under delivery `verification/android/`; the full native runner log is `verification/native-proof.log`. Portable course completions, controlled native fixtures and human phone playtesting remain distinct. Existing 3.3.1 release proof is historical and is not reused as 4.0 acceptance.

Reproduce using `scripts/test-expansion.ps1`, `scripts/build-android.ps1` and `scripts/test-ui.ps1 -ExpansionOnly`; see the root README for full arguments.

## Known gaps and next iteration

- Balance, sensor feel, readable boss warnings, touch accessibility, performance, audio/haptics and fun need physical-phone testing. Simulation completion is not a guarantee that every possible hazard combination is escapable.
- Daily resets at **00:00 UTC**, not the local midnight. A run retains its starting course date. No server or global leaderboard verifies dates/results; rollback protection is local and can be bypassed by clearing storage or reinstalling. Missing a day resets the current completion streak, while its best remains saved.
- Daily uses six bounded templates with seeded variation; expanding the template pool should follow feedback, not uncontrolled generation. Bump `DailyDrop.GENERATOR_VERSION` whenever daily generation or its underlying course physics changes so shared results identify compatible rules.
- Modifiers are introduced through Campaign; mirrored steering is optional practice. A configurable standalone modifier playlist/weekly mode is not included.
- All cosmetics are earned locally. Ads, revives, donations, paid avatars, billing, accounts and backend remain deferred.
- The in-app offline policy and local `policy-site/public/index.html` disclose optional sharing. The public website has not been redeployed in this task. Redeploy before a future store build and refresh listing screenshots/copy for the expanded campaign.
- Existing store assets and signed release artifacts predate this expansion. After owner playtesting, fix balance issues, review translations on devices, then explicitly prepare/retest a higher-code signed store bundle.

## Source state

Working directory: `<repository-root>`.

Base Git HEAD: `5ef23fb21067ecdc0421e489876b79a8a985d99d`. At this checkpoint, expansion source remained local, uncommitted and unpushed. The later 4.1 update authorizes publication of these changes along with the UX work. Binaries, generated builds and signing material stay outside versioned source. The delivery input manifest identifies the working-tree files used for the final APK.
