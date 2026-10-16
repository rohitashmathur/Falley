# Falley 3.2 beta verification receipts

Verified 16 October 2026 against the **3.2.0 / code 7** non-debuggable, upload-key-signed universal APK generated from the release AAB. App source embedded in the bundle: `21f2f87c3bc37aa9ab3fc34d3b0e62f3d2b4dd38`. Later documentation, test and artwork changes do not change the app binary.

| Artifact | SHA-256 |
| --- | --- |
| `Falley-3.2.0-beta.aab` | `2d7c7638bfbcfae1161af690bc0b3cc47bb5e8198b9c223bd76725343175b75c` |
| `Falley-3.2.0-beta-local.apk` | `2d62249186607d650a1719083e77c63a1b81f20b0a3c12c7225593f5cc508c39` |

## Recorded checks

- Android 16/API 36 full native suite: **1,019 checks / 64 captures per layout**, 2,038 checks / 128 captures total. Viewports: 480x800 at density 240 and 720x1600 at density 320. Eight selected PNGs per layout are published here, along with all numerical checks and suite summaries.
- Android 15/API 35 focused release suite: **387 checks / 13 captures per layout**, 774 checks / 26 captures total. Same two viewports. All focused captures, checks and summaries are published here. This supplements the API 36 full suite; it does not repeat all campaign and touch flows.
- Four native runs total: **2,812 checks / 154 captures**, with **42 curated PNGs** here. Coverage includes the offline policy and real scrolling/return navigation, virtual menu accessibility actions, active-play screen awake behavior, settings and saved selections, actual hazard/audio event handling and SoundPool lifecycle. Preferences and display settings were restored; test APKs were removed.
- Separate store session: **125 checks / 6 native captures** at 1080x1920. `store-capture-suite.txt` and `store-capture-checks.txt` are the receipts. The originals, precise app crops and final upload artwork are in [`../../store/assets/`](../../store/assets/manifest.json).
- Release lint/bundle, bundletool validation, strict expected-key jarsigner verification, all **24 signed payload entries**, APK v2/v3 signing and 16 KB page alignment checks passed. Lint: **0 errors / 9 warnings**; the unsuppressed report is retained. Jarsigner reports POSIX ZIP-attribute and JarInputStream ordering caveats; the strict JarFile verification and every-entry certificate/digest verification pass. See `release-v32.log` and `lint-results-release.txt`.
- Negative controls: unsigned input and a valid bundle with an added unsigned payload were rejected. Missing upload-signing environment was rejected rather than falling back to an unsigned release. See `signature-controls.log` and `release-signing-guard.log`.

Captures with controlled positions, target collisions or hazards are deterministic native test fixtures. They are not natural phone play sessions or human completion rates. Menu accessibility nodes do not establish full TalkBack usability. SoundPool load/start counters do not establish audible quality. The unchanged 3.1 physics/audio-file proof is carried forward in [`../../BUILD_STATUS.md`](../../BUILD_STATUS.md), not rerun as a 3.2 result.

Release binaries and private signing material stay local. No Play upload, Google acceptance, assigned rating, physical-phone acceptance or advertising integration is implied. Follow [`../../docs/BETA_SUBMISSION.md`](../../docs/BETA_SUBMISSION.md) for the remaining owner and Console steps.
