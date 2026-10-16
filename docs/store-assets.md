# Falley Google Play images

Prepared 16 October 2026. The upload set lives in `store/assets/en-US/`.
Use the six numbered images in `phone/` in order. The first four show actual
native gameplay; the last two explain Arcade configuration and motion feel.
No death frames, blood, bone fragments or corpses appear in the listing art.
The game's violence must still be disclosed accurately in the content rating.

## Upload files

| Play Console field | File | Dimensions / format |
| --- | --- | --- |
| App icon | `store/assets/en-US/icon-512.png` | 512 × 512, RGBA PNG |
| Feature graphic, recommended default | `store/assets/en-US/feature-graphic-1024x500.png` | 1024 × 500, RGB PNG |
| Phone screenshot 1 | `store/assets/en-US/phone/01-motion-and-touch.png` | 1080 × 1920, RGB PNG |
| Phone screenshot 2 | `store/assets/en-US/phone/02-ropes-and-target.png` | 1080 × 1920, RGB PNG |
| Phone screenshot 3 | `store/assets/en-US/phone/03-dodge-the-hazards.png` | 1080 × 1920, RGB PNG |
| Phone screenshot 4 | `store/assets/en-US/phone/04-buzzsaw-survival.png` | 1080 × 1920, RGB PNG |
| Phone screenshot 5 | `store/assets/en-US/phone/05-choose-your-skirmish.png` | 1080 × 1920, RGB PNG |
| Phone screenshot 6 | `store/assets/en-US/phone/06-motion-your-way.png` | 1080 × 1920, RGB PNG |

Optional feature graphic alternatives are
`store/assets/en-US/variants/feature-motion-1024x500.png` and
`store/assets/en-US/variants/feature-arcade-1024x500.png`. Use one graphic per
listing. They can also support a later custom store listing test; they do not
create or publish a listing automatically.

`store/assets/manifest.json` contains upload order, alt text (at most 140
characters), dimensions, PNG mode, byte counts, SHA-256 values, source capture
paths and deterministic-fixture provenance. Copy its alt text into Play
Console's image descriptions where available.

## Design and source integrity

The icon exports the existing Android vector icon's viewport coordinates and
colors at high resolution; no rounded mask, drop shadow or badge is imposed.
The feature graphics use original vector artwork that repeats Falley's own
Canvas avatar, ropes, target and hazard primitives. They are promotional art,
not purported gameplay screenshots. No external media, AI image generation,
third-party characters, sample imagery or store badges were used.

Each phone image contains a native app capture. Android status/navigation
bars and unused outer letterboxing are cropped. The full rendered design
viewport, including the header, jump button and settings controls, stays
intact and is fitted proportionally inside a simple navy card.
No app content is repainted, moved, stretched, merged with another gameplay
frame, or hidden by marketing text. Captions occupy under 20% of the image.
Any controlled captures are native app renders with deterministic avatar,
camera and hazard states, as described per image in the manifest. The target
landing capture is a controlled successful landing, not evidence of a human
playtest or claimed completion rate.

`store/assets/raw-source/` preserves each original native capture.
`store/assets/app-crops/` preserves app-only crops. These are provenance and
review files; do not upload them as additional phone screenshots. The contact
sheet at `store/assets/review-contact-sheet.png` is for review only.

The assets depict implemented controls, hazards and modes. They make no claim
about ads, rewards, online leaderboards, pricing, awards or user ratings.
No fonts are redistributed; the generator uses local system fonts to render
artwork.

## Rebuild and validate

The final six native originals are 1080 × 1920 captures from the 3.2.0
release-derived APK on Android 16. All six entries record version 3.2.0 in the
manifest. A controlled app fixture positions the avatar, camera or warning;
the game then renders real physics frames. Arcade and Settings show their
actual interactive UI, with motion enabled and Balanced selected in Settings.
System bars and unused outer letterboxing outside the scaled design viewport
were removed at the recorded crop `[33, 63, 1047, 1794]`; the full rendered
design viewport remains intact. No older V3/V3.1 source remains in the
final image set.

The renderer uses Python and Pillow. Rebuild the committed pack from its
preserved native originals:

```text
python scripts/generate_store_assets.py --source-config store/assets/source-config.json
python scripts/generate_store_assets.py --check
```

To replace every source with a fresh capture session:

```text
python scripts/generate_store_assets.py --source-config build/store-captures/sources.json
python scripts/generate_store_assets.py --check
```

The optional source JSON is keyed `campaign`, `landing`, `lightning`, `saw`,
`arcade` and `settings`. Each entry has `path`, `app_crop` as
`[left, top, right, bottom]`, `controlled` and `description`; `source_version`
is optional. Paths are repository-relative or absolute. A configured pack must
explicitly supply all six keys; the renderer will not mix in older fallback
captures. Example entry (the complete file must include the other five keys):

```json
{
  "saw": {
    "path": "build/store-captures/saw.png",
    "app_crop": [0, 72, 1080, 1776],
    "controlled": true,
    "source_version": "release candidate",
    "description": "Native app render with a live buzzsaw and living avatar."
  }
}
```

Example crop numbers above are illustrative; read the real system-bar/app
bounds for each capture. With no source JSON, the generator falls back to
documented V3/V3.1 captures. Prefer fresh high-resolution release-candidate
captures for submission. Inspect the manifest after regeneration to confirm
every source is the intended candidate.

`--check` validates all ten upload files: dimensions, image mode, file sizes,
hashes and alt-text lengths. It also verifies each preserved native source
hash and checks that its app-only crop exactly matches the recorded rectangle.
Visual review must additionally check readable
captions, full app visibility, correct controls, and the absence of death/gore.

## Specification checked

Google's [preview asset requirements](https://support.google.com/googleplay/android-developer/answer/9866151?hl=en),
checked 16 October 2026, specify a 512 × 512 32-bit PNG icon (maximum 1024 KB),
a 1024 × 500 feature graphic without alpha, and phone screenshots without
alpha. The six 9:16 images meet the 1080 × 1920 portrait recommendation for
games. Google requires at least two screenshots and recommends at least three
actual gameplay images for games; this set supplies four first. There are no
fake ratings, store badges, pricing claims, download calls to action or device
mockups. Promotional eligibility and publication remain subject to Google's
review.
