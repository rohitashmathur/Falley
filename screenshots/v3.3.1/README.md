# Falley 3.3.1 privacy verification

Checked **23 October 2026**, version **3.3.1/code 11**. These are selected native Android screenshots and verification receipts, not store-upload artwork. APKs, AABs and signing files remain local.

The focused API 35 suite passed on both the compatible debug app and upload-signed release APK, at 480x800 and 720x1600 with Android font scale 1.6 and English, Hindi and Bahasa Indonesia. Debug: **2,407 checks / 24 captures**; release: **2,400 checks / 24 captures**. The combined **4,807 checks / 48 captures** include seven debug fixture-cleanup checks. Six selected images are published here; all 48 captures remain in the local delivery directory.

| Evidence | Purpose |
| --- | --- |
| `debug-small.txt`, `debug-tall.txt` | Passing debug native receipts |
| `release-small.txt`, `release-tall.txt` | Passing release native receipts |
| `localization.log` | 2,276 portable localization assertions |
| `SOURCE-SHA256SUMS.txt` | Original Windows build-input hashes, recorded before source publication |
| `debug-small-en-end.png` | English contact and separate footer actions |
| `debug-small-hi-end.png` | Hindi contact and long email wrapping |
| `debug-tall-id-url.png` | Indonesian public URL wrapping |
| `release-small-en-top.png` | Release policy identity and effective/update dates |
| `release-small-hi-end.png` | Release Hindi contact/footer layout |
| `release-tall-id-url.png` | Release Indonesian public URL/footer layout |

Checks cover real Settings navigation, scrolling until the email/URL are visible, glyph bounds, full accessible policy, both Back paths and no requested app permissions. Browser/email intents were intercepted to verify exact destinations without opening external apps or sending messages. Root and QA visually reviewed the layout. Original app preferences, display and font settings were restored; the harness was uninstalled and the owned emulator stopped.

These are controlled UI checks rather than physical-phone acceptance. Gameplay physics proof is carried forward from 3.3. Binary hashes, local delivery paths, signature validation and reproduction commands are in the [3.3.1 update notes](../../docs/PRIVACY_UPDATE_3_3_1.md). The app and website remain offline/ad-free and static respectively; no advertising integration or Play submission is implied.
