"""Render Falley's Google Play artwork from its own icon and native captures.

Requires Pillow. Run from any directory:
  python scripts/generate_store_assets.py
  python scripts/generate_store_assets.py --source-config build/store-captures/sources.json
  python scripts/generate_store_assets.py --check

The JSON keeps legacy keys campaign, landing, lightning, saw, arcade,
settings; explicit role fields describe current gameplay/menu subjects. Without
--source-config the checked-in current source-config.json is replayed. Each value has path (relative to the repository or absolute), app_crop
([left, top, right, bottom], excluding Android system bars and unused outer
letterboxing), controlled
(bool), and description. No game pixels are added, removed, or retouched.
"""
from __future__ import annotations

import argparse
import importlib.util
from functools import lru_cache
import hashlib
import json
import math
from pathlib import Path
import shutil
import xml.etree.ElementTree as ET

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "store/assets"
LANG = OUT / "en-US"
NS = "{http://schemas.android.com/apk/res/android}"
MINT, WHITE, GOLD = "#5de0ba", "#f0f7f7", "#f6d477"
BG, MUTED = "#0c1b29", "#b3c9cf"
RESAMPLE = Image.Resampling.LANCZOS
FONT_PATHS = [Path("C:/Windows/Fonts/bahnschrift.ttf"), Path("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf")]
FONT_BOLD_PATHS = [Path("C:/Windows/Fonts/segoeuib.ttf"), Path("/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf")]

FALLBACK = {
    "campaign": {"path": "screenshots/v3/api36/small/arcade-survival-controlled.png", "description": "V3 Arcade survival fixture: moving avatar, ropes, narrowing spike walls and touch drift."},
    "landing": {"path": "screenshots/v3/api36/small/stage-6-target-bounce-controlled.png", "description": "V3 campaign stage 6 controlled successful target-bounce fixture."},
    "lightning": {"path": "screenshots/v3/api36/small/arcade-lightning-warning-controlled.png", "description": "V3 controlled diagonal lightning warning before it becomes lethal."},
    "saw": {"path": "screenshots/v3.1/api36/small/arcade-saw-active-controlled.png", "description": "V3.1 live moving buzzsaw fixture; avatar alive above rope."},
    "arcade": {"path": "screenshots/v3.1/api36/small/arcade-setup-difficulty-1.png", "description": "V3.1 five-hazard Arcade setup with Standard difficulty selected."},
    "settings": {"path": "screenshots/v3.1/api36/small/settings-sound-on.png", "description": "V3.1 settings with Tight motion feel and sound on; motion steering toggle off."},
}

SHOTS = [
    ("campaign", "01-motion-and-touch.png", "Read every rope", "Read the colour. Time your next jump.", "Actual Falley Arcade gameplay with the Shadow avatar, narrowing spike walls and mixed individual rope types."),
    ("landing", "02-ropes-and-target.png", "Learn the ropes", "Classic, Spring and Static. Know the difference.", "The actual Collection rope guide explains Classic, Spring and Static effects and where special ropes appear."),
    ("lightning", "03-dodge-the-hazards.png", "A new way to fall", "Unlock avatars. Choose your next look.", "The actual Demon unlock celebration offers Use Demon or Keep Human beside the animated winged avatar."),
    ("saw", "04-buzzsaw-survival.png", "Plan your drop", "Finish first. Chase the extra stars.", "The actual campaign briefing explains stage-one story, rope cue and three star goals before starting."),
    ("arcade", "05-choose-your-skirmish.png", "Build your survival", "Choose your difficulty and hazards.", "The actual Arcade setup offers Chill, Standard and Brutal cards, five hazard choices and touch or motion controls."),
    ("settings", "06-motion-your-way.png", "Make it your game", "Tilt or swipe. Tune the feel.", "Actual Falley Settings show motion feel, language, independent retro music and sound, vibration and blood controls."),
]


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def font(size, bold=False):
    paths = FONT_BOLD_PATHS if bold else FONT_PATHS
    for path in paths:
        if path.exists():
            return ImageFont.truetype(str(path), size)
    raise RuntimeError("Install a usable Segoe UI/Bahnschrift or DejaVu font; no font is bundled in the repository.")


def text_fit(draw, xy, text, size, fill=WHITE, width=None, bold=False):
    f = font(size, bold)
    if width:
        while draw.textbbox((0, 0), text, font=f)[2] > width:
            size -= 1
            f = font(size, bold)
    draw.text(xy, text, font=f, fill=fill, anchor="lt")


@lru_cache(maxsize=2)
def background_template(size):
    width, height = size
    image = Image.new("RGB", size)
    pix = image.load()
    # A restrained navy/teal gradient extends the game's own neon palette.
    for y in range(height):
        for x in range(width):
            glow = max(0, 1-math.hypot((x-width*.82)/width, (y-height*.35)/height))
            pix[x, y] = (int(9+7*glow), int(23+22*glow), int(36+26*glow))
    return image


def background(size):
    return background_template(size).copy()


def round_line(draw, points, fill, width):
    width = max(1, int(round(width)))
    draw.line(points, fill=fill, width=width, joint="curve")
    r = width/2
    for x, y in (points[0], points[-1]):
        draw.ellipse((x-r, y-r, x+r, y+r), fill=fill)


def bezier(a, b, c, d, steps=100):
    return [((1-t)**3*a[0]+3*(1-t)**2*t*b[0]+3*(1-t)*t*t*c[0]+t**3*d[0],
             (1-t)**3*a[1]+3*(1-t)**2*t*b[1]+3*(1-t)*t*t*c[1]+t**3*d[1])
            for t in (i/steps for i in range(steps+1))]


def brand_renderer():
    source = ROOT / "store/brand/generate_brand.py"
    spec = importlib.util.spec_from_file_location("falley_brand", source)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def render_icon():
    renderer = brand_renderer()
    source = renderer.write_android()
    renderer.render_store_icon()
    return source


def human(draw, x, y, r):
    """Original Falley Canvas avatar, using MainActivity.drawHuman coordinates."""
    skin, trousers, coral = "#ffd4ae", "#65b9f2", "#ff816c"
    def ln(a,b,w,color):
        round_line(draw,[(x+a[0]*r,y+a[1]*r),(x+b[0]*r,y+b[1]*r)],color,w*r)
    def circ(cx,cy,rad,color):
        draw.ellipse((x+(cx-rad)*r,y+(cy-rad)*r,x+(cx+rad)*r,y+(cy+rad)*r), fill=color)
    for a,b,w,c in [((-.17,.28),(-.34,.60),.22,trousers),((.34,.60),(.17,.28),.22,trousers),
                    ((-.34,.60),(-.31,1),.18,trousers),((.34,.60),(.31,1),.18,trousers),
                    ((-.40,1),(-.23,1),.15,WHITE),((.23,1),(.40,1),.15,WHITE),
                    ((-.32,-.44),(-.95,-.25),.16,skin),((.32,-.44),(.95,-.25),.16,skin)]:
        ln(a,b,w,c)
    circ(-.95,-.25,.11,skin); circ(.95,-.25,.11,skin)
    draw.rounded_rectangle((x-.34*r,y-.46*r,x+.34*r,y+.34*r), radius=.13*r, fill=coral)
    ln((-.13,-.32),(.13,-.32),.065,"#b9584b")
    circ(0,-.95,.34,skin); circ(0,-1.15,.26,"#353133")
    draw.rounded_rectangle((x-.30*r,y-1.12*r,x+.30*r,y-.87*r),radius=.1*r,fill=skin)
    circ(-.105,-.915,.036,"#17191b"); circ(.105,-.915,.036,"#17191b")
    ln((-.08,-.78),(.08,-.78),.035,"#9b6653")


def saw(draw,x,y,r):
    points=[]
    for i in range(48):
        a=i*math.tau/48
        rr=r*(1 if i%4<2 else .82)
        points.append((x+rr*math.cos(a),y+rr*math.sin(a)))
    draw.polygon(points,fill="#c6d0db")
    draw.ellipse((x-r*.66,y-r*.66,x+r*.66,y+r*.66),fill="#545f70")
    draw.ellipse((x-r*.14,y-r*.14,x+r*.14,y+r*.14),fill="#e3e8ef")
    for i in range(6):
        a=i*math.tau/6
        xx,yy=x+r*.43*math.cos(a),y+r*.43*math.sin(a)
        draw.ellipse((xx-r*.07,yy-r*.07,xx+r*.07,yy+r*.07),fill="#121a20")


def feature(kind="default"):
    return brand_renderer().render_feature(kind)


def read_sources(config):
    sources=json.loads(json.dumps(FALLBACK))
    for value in sources.values():
        value.update(app_crop=[0,32,480,728], controlled=True, source_version="3.1.0" if "/v3.1/" in value["path"] else "3.0.0")
    if config:
        data=json.loads(config.read_text(encoding="utf-8-sig"))
        assert set(data)==set(sources), "A source config must explicitly provide all six capture keys; no fallback captures are mixed into a configured pack."
        for key,value in data.items():
            assert all(k in value for k in ("path","app_crop","controlled","description")), key
            sources[key]=value
    return sources


def screenshot(key,name,title,subtitle,alt,spec):
    path=Path(spec["path"])
    if not path.is_absolute():
        path=ROOT/path
    source=Image.open(path).convert("RGB")
    rect=tuple(spec["app_crop"])
    assert len(rect)==4 and 0<=rect[0]<rect[2]<=source.width and 0<=rect[1]<rect[3]<=source.height, (key,rect,source.size)
    app=source.crop(rect)
    rawdest=OUT/"raw-source"/(key+".png")
    rawdest.parent.mkdir(parents=True,exist_ok=True)
    if path.resolve()!=rawdest.resolve():
        shutil.copyfile(path,rawdest)
    appdest=OUT/"app-crops"/(key+".png")
    appdest.parent.mkdir(parents=True,exist_ok=True)
    app.save(appdest,optimize=True)
    image=background((1080,1920))
    d=ImageDraw.Draw(image)
    text_fit(d,(72,62),"FALLEY",28,MINT,bold=True)
    text_fit(d,(68,126),title,72,width=950,bold=True)
    text_fit(d,(72,231),subtitle,37,MUTED,width=936)
    # Full app content, including header and touch control, is fitted without
    # stretching, masks, invented screen elements, or device imagery.
    factor=min(936/app.width,1462/app.height)
    fitted=app.resize((round(app.width*factor),round(app.height*factor)),RESAMPLE)
    x=(1080-fitted.width)//2
    y=340+(1462-fitted.height)//2
    d.rounded_rectangle((x-4,y-4,x+fitted.width+4,y+fitted.height+4),radius=6,fill="#416069")
    image.paste(fitted,(x,y))
    text_fit(d,(72,1854),"FALLEY  /  PHYSICS ARCADE",22,"#73939d")
    number=name[:2]
    text_fit(d,(958,1854),number,22,MINT)
    dest=LANG/"phone"/name
    dest.parent.mkdir(parents=True,exist_ok=True)
    image.save(dest,optimize=True)
    try:
        relative=path.relative_to(ROOT).as_posix()
    except ValueError:
        relative=path.name
    return {"path":dest.relative_to(OUT).as_posix(),"kind":"phone_screenshot","alt_text":alt,
            "source":relative,"source_sha256":sha(path),"native_size":list(source.size),
            "app_crop":list(rect),"controlled_fixture":spec["controlled"],
            "source_version":spec.get("source_version","release candidate"),
            "source_version_code":spec.get("source_version_code","unspecified"),
            "source_role":spec.get("role",key),
            "source_package":spec.get("source_package","unspecified"),
            "source_apk_sha256":spec.get("source_apk_sha256","unspecified"),
            "capture_api":spec.get("capture_api","unspecified"),
            "capture_viewport":spec.get("capture_viewport","unspecified"),
            "capture_origin":spec.get("capture_origin",relative),
            "crop_verification":spec.get("crop_verification","Measured native app viewport excluding system bars and unused outer letterboxing."),
            "provenance":spec["description"],"operations":"Android system bars and unused outer letterboxing cropped; the full rendered design viewport, including header, jump and settings controls, stays intact and is proportionally fitted within a navy card with truthful captions.",
            "source_copy":rawdest.relative_to(OUT).as_posix(),"app_crop_copy":appdest.relative_to(OUT).as_posix()}


def make_contact_sheet(shots):
    image=Image.new("RGB",(1080,1400),BG)
    d=ImageDraw.Draw(image)
    text_fit(d,(28,18),"Falley / Google Play phone assets",32,WHITE,bold=True)
    for i,(_,name,title,*_) in enumerate(shots):
        src=Image.open(LANG/"phone"/name)
        src.thumbnail((324,576),RESAMPLE)
        x=28+(i%3)*352
        y=80+(i//3)*650
        image.paste(src,(x,y))
        text_fit(d,(x,y+590),name[:2]+" / "+title,18,MUTED,width=330)
    image.save(OUT/"review-contact-sheet.png",optimize=True)


def validate(manifest):
    count=0
    for item in manifest["assets"]:
        path=OUT/item["path"]
        with Image.open(path) as image:
            assert list(image.size)==item["dimensions"], path
            assert image.mode==item["mode"], path
            if item["kind"]=="icon":
                assert image.size==(512,512) and image.mode=="RGBA" and path.stat().st_size<=1024*1024
            elif item["kind"]=="feature_graphic":
                assert image.size==(1024,500) and image.mode=="RGB"
            else:
                assert image.size==(1080,1920) and image.mode=="RGB"
                original=OUT/item["source_copy"]
                assert sha(original)==item["source_sha256"], original
                with Image.open(original) as raw, Image.open(OUT/item["app_crop_copy"]) as crop:
                    assert list(raw.size)==item["native_size"]
                    assert raw.convert("RGB").crop(tuple(item["app_crop"])).tobytes()==crop.convert("RGB").tobytes(), item["path"]
            assert path.stat().st_size<8*1024*1024
            assert item["sha256"]==sha(path), path
            assert len(item["alt_text"])<=140, item["alt_text"]
        count+=1
    assert sum(a["kind"]=="phone_screenshot" for a in manifest["assets"])==6
    print(f"PASS: {count} upload assets; exact dimensions, PNG modes, sizes, hashes and alt text verified.")


def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source-config",type=Path)
    parser.add_argument("--check",action="store_true")
    parser.add_argument("--brand-only",action="store_true",help="Refresh branding without changing native gameplay captures or their provenance.")
    args=parser.parse_args()
    if args.check:
        validate(json.loads((OUT/"manifest.json").read_text(encoding="utf-8")))
        return
    LANG.mkdir(parents=True,exist_ok=True)
    render_icon()
    for kind in ("default","motion","arcade"):
        feature(kind)
    renderer=brand_renderer()
    if args.brand_only:
        renderer.update_manifest()
        renderer.contact_sheet(ROOT/"build/branding-4.2.1/brand-review.png")
        validate(json.loads((OUT/"manifest.json").read_text(encoding="utf-8")))
        print("Branding refreshed; native screenshot source versions and pixels unchanged.")
        return
    assets=renderer.manifest_entries()
    source_config=args.source_config or ((OUT/"source-config.json") if (OUT/"source-config.json").exists() else None)
    sources=read_sources(source_config)
    for key,name,title,subtitle,alt in SHOTS:
        assets.append(screenshot(key,name,title,subtitle,alt,sources[key]))
    # Preserve a repository-local replay configuration, so a fresh clone can
    # regenerate the exact source pack without ignored build/ captures.
    replay={key:{**spec,"path":spec["path"] if str(spec["path"]).startswith("store/assets/source/") else f"store/assets/raw-source/{key}.png"} for key,spec in sources.items()}
    (OUT/"source-config.json").write_text(json.dumps(replay,indent=2)+"\n",encoding="utf-8")
    for item in assets:
        path=OUT/item["path"]
        with Image.open(path) as image:
            item.update(dimensions=list(image.size),mode=image.mode,bytes=path.stat().st_size,sha256=sha(path))
    make_contact_sheet(SHOTS)
    manifest={"product":"Falley","locale":"en-US","prepared_date":"2026-10-25","brand_version":renderer.VERSION,"specification_source":"https://support.google.com/googleplay/android-developer/answer/9866151?hl=en",
              "recommended_upload_order":[x[1] for x in SHOTS],"default_feature_graphic":"en-US/feature-graphic-1024x500.png",
              "notes":["Feature variants are optional alternatives; use one feature graphic per listing.","Original native captures and app-only crops are provenance/support files, not additional phone uploads.","Controlled fixtures are native app renders with deterministic position/hazard state; no altered scores, fabricated effects or stitched gameplay.","Assets avoid blood, corpses and death effects. Content-rating disclosures must still accurately describe the game's violence.","No ads, leaderboards, monetization, awards, pricing or store-badge claims are depicted.","Each phone screenshot retains its actual source_version and role. Current images include native menus and controlled gameplay/reward fixtures; displayed scores and unlocks are not natural playthrough claims."],"assets":assets}
    (OUT/"manifest.json").write_text(json.dumps(manifest,indent=2)+"\n",encoding="utf-8")
    # An early review-only source contact sheet is not a deliverable.
    old=OUT/"source-review.png"
    if old.exists():
        old.unlink()
    validate(manifest)
    print(f"Output: {OUT}")


if __name__=="__main__":
    main()
