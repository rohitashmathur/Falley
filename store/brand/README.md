# Falley Demon branding

Original code-native artwork for the 4.2.1 polish: the approved Demon avatar's winged jump pose and colors, plus a simple geometric FALLEY wordmark. No third-party image or bundled font is used for the logo. Promotional illustrations use the same rig and palette; they are not screenshots or representations of a particular stage.

`generate_brand.py` is the common geometry source for the Android legacy vector, adaptive foreground, monochrome layer, splash vector, SVG references, 512px Play icon and three promotional graphics. Pillow is required. From the repository root:

```powershell
python store/brand/generate_brand.py
python scripts/generate_store_assets.py --check
```

The preview is written to ignored `build/branding-4.2.1/brand-review.png`. It shows actual 48/64/96px icon sizes, adaptive circle/squircle/themed previews, the splash composition and the default promotional graphic. Launcher masks and Android's splash scaling must also be checked on the final APK. The 512px Play icon deliberately has no imposed rounded corners or shadow.

Android API 24–25 uses the legacy vector; API 26+ uses separate adaptive background/foreground layers; API 33+ also supplies the monochrome themed-icon layer. Important adaptive artwork fits the central 66px safe circle of the 108px foreground. The legacy/Play composition enlarges the same geometry slightly to fill a flat icon more effectively.

## Refreshing gameplay images

Brand-only regeneration leaves all six native phone screenshot sources, crops, pixels and source-version provenance unchanged:

```powershell
python scripts/generate_store_assets.py --brand-only
```

The current six images come from the final **4.2.1-beta-test / code 15** debug APK (`com.perfgames.falley.motiontest`) on Android 15, captured at 720×1600/density 320/font scale 1.3. Exact native copies and a provenance receipt are under `store/assets/source/v4.2.1/`; the portable configuration records the APK hash, actual roles, full app-content crop and controlled-fixture disclosures. They include one active Arcade frame and five native interface screens. Pictured survival time, prerequisite wins and unlocks are controlled fixtures, not natural playthrough evidence.

For a later refresh, capture the final debug or release-derived app, prepare a complete six-key source JSON using the structure in `store/assets/source-config.json`, measure the actual app-content crop for each image, and record the real package/version and whether each scene is controlled. Then run:

```powershell
python scripts/generate_store_assets.py --source-config build/store-captures/4.2.1/sources.json
python scripts/generate_store_assets.py --check
```

No game pixels are added, removed, composited or retouched by the screenshot pipeline. Android bars and unused letterboxing may be cropped; the app viewport is proportionally fitted with truthful captions. Review captions against the new captures before upload. The generated manifest records individual native source hashes and versions, so a brand refresh cannot silently relabel old gameplay as new.
