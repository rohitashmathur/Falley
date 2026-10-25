# Falley 4.3 debug evidence

All current frames are unedited native Android screenshots of **4.3.0-beta-test/code 16**, package `com.perfgames.falley.motiontest`. Font scale 1.3. The local APK SHA-256 is `a08391a972b64729b7ec474a19a11819c32646a49a3459eb1f8716f63d9db377`. APKs, mappings and keys stay local.

## Native verification

| Run | Checks | Captures |
| --- | ---: | ---: |
| API 35-small-v43 | 4,031 | 45 |
| API 35-tall-v43 | 4,031 | 45 |
| API 35-small-ux | 12,083 | 92 |
| API 35-tall-ux | 12,083 | 92 |
| API 25-small-v43 | 3,940 | 45 |
| API 36-tablet-v43 | 4,635 | 52 |

Phone fixtures use 480x800 and 720x1600. API 25 is Android 7.1; API 35 is Android 15. The dedicated API 36 Pixel Tablet starts at physical 2560x1600/density320, with a full 2560x1600 app window and 112 px taskbar inset. Real system rotation swaps the compositor and app to 1600x2560; the same Activity/Physics run pauses, retains state and resumes. An earlier forced-portrait letterbox experiment is excluded from this accepted evidence. Preferences, display/font and rotation settings are restored; instrumentation is removed.

These are **controlled fixtures**: progress/avatars may be granted, boss phases are positioned deliberately, and the avatar may be placed in a safe lane. They establish rendering and state routing; they do not establish natural phone difficulty, physical motion comfort, audio audibility, TalkBack or sustained performance.

## Selected screens

- Home spacing: [phone](phone-home.png), [tablet landscape](tablet-landscape-home.png), [tablet portrait](tablet-portrait-home.png).
- Combined Campaign: [phone](phone-campaign.png), [small phone](small-campaign.png), [Hindi tablet](tablet-landscape-campaign-hi.png), [portrait tablet](tablet-portrait-campaign.png).
- [Arcade cards](phone-arcade.png), [Robot traits](phone-robot.png), [Shadow traits](phone-shadow.png), [API 25 Home](legacy-home.png), [rotated gameplay](tablet-portrait-play.png).
- Boss warnings: [gear grid](boss-1-warning.png), [blade rig](boss-2-warning.png), [Furnace Demon](boss-3-warning.png), [Storm Robot](boss-4-warning.png), [Forge Robot](boss-5-warning.png), [Core Demon](boss-6-warning.png), [live Core attack](boss-6-live.png).

[Verification receipt](DEBUG_VERIFICATION.json) records per-file hashes, image dimensions and log provenance. [Application source hashes](SOURCE_HASHES.json) bind the final APK to its inputs. Portable, storage, music and Daily-compatibility logs are included here; full diagnostic captures remain in the local delivery folder. Existing store assets remain explicitly historical 4.2.1 material.
