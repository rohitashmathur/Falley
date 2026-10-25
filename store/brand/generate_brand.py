"""Original Falley Demon branding. Rebuild only branding; native gameplay captures stay unchanged.

python store/brand/generate_brand.py
Requires Pillow. The geometry below supplies Android vectors, SVG and raster art.
The wordmark consists of original simple geometric strokes, with no bundled font.
"""
from __future__ import annotations
import argparse, hashlib, html, json, math
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont

ROOT=Path(__file__).resolve().parents[2]
OUT=ROOT/'store/assets'
RES=ROOT/'app/src/main/res'
BG='#17191b'; MINT='#5de0ba'; WHITE='#f0f7f7'; MUTED='#b3c9cf'
SKIN='#f1948b'; SHIRT='#d65f78'; WING='#792d53'; EDGE='#d46a83'; GOLD='#f6d477'; BLUE='#65b9f2'
VERSION='4.2.1'
SAMPLE=Image.Resampling.LANCZOS


def num(x): return f'{x:.4f}'.rstrip('0').rstrip('.')
def poly(points, fill): return {'type':'poly','points':points,'fill':fill}
def line(points, color, width): return {'type':'line','points':points,'stroke':color,'width':width}
def circle(x,y,r,fill): return {'type':'circle','x':x,'y':y,'r':r,'fill':fill}
def rect(l,t,r,b,radius,fill): return {'type':'rect','box':(l,t,r,b),'radius':radius,'fill':fill}


def demon(x,y,r):
    """Same original Demon colors/wing fan/rig as AvatarRenderer at full jump rise."""
    items=[]
    def p(coords,fill): items.append(poly([(x+a*r,y+b*r) for a,b in coords],fill))
    def ln(coords,color,width): items.append(line([(x+a*r,y+b*r) for a,b in coords],color,width*r))
    def ci(a,b,rr,fill): items.append(circle(x+a*r,y+b*r,rr*r,fill))
    def rr(l,t,ri,b,rad,fill): items.append(rect(x+l*r,y+t*r,x+ri*r,y+b*r,rad*r,fill))
    shoulder=-.44; pelvis=.28; head=-.95; span=1.49
    for side in (-1,1):
        p([(side*.26,shoulder+.05),(side*span,shoulder-.97),(side*span*.86,shoulder+.15),(side*span*.64,shoulder+.03),(side*span*.50,shoulder+.40),(side*.35,shoulder+.47)],WING)
        ln([(side*.32,shoulder+.13),(side*span,shoulder-.97)],EDGE,.06)
        ln([(side*.34,shoulder+.17),(side*span*.64,shoulder+.03)],'#a54568',.04)
    for side in (-1,1):
        ln([(side*.17,pelvis),(side*.46,.52),(side*.31,1)],BLUE,.22)
        ln([(side*.31-.09,1),(side*.31+.08,1)],WHITE,.15)
        ln([(side*.32,shoulder),(side*.85,-.68)],SKIN,.16)
        ci(side*.85,-.68,.11,SKIN)
    rr(-.34,shoulder-.02,.34,pelvis+.06,.13,SHIRT)
    ln([(-.13,shoulder+.12),(.13,shoulder+.12)],'#263342',.065)
    ci(0,head,.34,SKIN);ci(0,head-.20,.26,'#353133')
    rr(-.30,head-.17,.30,head+.08,.10,SKIN)
    for side in (-1,1): ci(side*.105,head+.035,.036,'#171b20')
    ln([(-.08,head+.17),(.08,head+.17)],'#9b6653',.035)
    p([(-.27,head-.15),(-.33,head-.55),(-.08,head-.30)],GOLD)
    p([(.27,head-.15),(.33,head-.55),(.08,head-.30)],GOLD)
    return items


def wordmark(x,y,height=8,color=WHITE,width=48):
    # F A L L E Y. Fixed original strokes retain the same name across every export.
    glyphs=[ [[(0,8),(0,0),(5,0)],[(0,3.7),(4.1,3.7)]],
             [[(0,8),(2.5,0),(5,8)],[(1.05,5.1),(3.95,5.1)]],
             [[(0,0),(0,8),(5,8)]], [[(0,0),(0,8),(5,8)]],
             [[(5,0),(0,0),(0,8),(5,8)],[(0,3.7),(4.1,3.7)]],
             [[(0,0),(2.5,3.8),(5,0)],[(2.5,3.8),(2.5,8)]] ]
    sy=height/8; sx=width/48
    return [line([(x+(index*8.6+a)*sx,y+b*sy) for a,b in stroke],color,1.3*sy)
            for index,glyph in enumerate(glyphs) for stroke in glyph]


def branding(): return demon(54,47,13.3)+wordmark(30,67.5)


def validate_safe_circle():
    # Include stroke caps: essential foreground geometry stays inside the 66px safe circle.
    for item in branding():
        if 'points' in item:
            extent=item.get('width',0)/2
            assert all(math.hypot(x-54,y-54)+extent <= 33 for x,y in item['points']), item
        elif item['type']=='circle':
            assert math.hypot(item['x']-54,item['y']-54)+item['r'] <= 33, item
        else:
            l,t,r,b=item['box']
            assert all(math.hypot(x-54,y-54) <= 33 for x,y in ((l,t),(l,b),(r,t),(r,b))), item


def transform(items,scale=1,dx=0,dy=0):
    out=[]
    for original in items:
        a=original.copy()
        if 'points' in a:a['points']=[(x*scale+dx,y*scale+dy) for x,y in a['points']]
        if a['type']=='circle':a.update(x=a['x']*scale+dx,y=a['y']*scale+dy,r=a['r']*scale)
        if a['type']=='rect':
            l,t,r,b=a['box'];a.update(box=(l*scale+dx,t*scale+dy,r*scale+dx,b*scale+dy),radius=a['radius']*scale)
        if 'width' in a:a['width']*=scale
        out.append(a)
    return out


def pathdata(a):
    typ=a['type']
    if typ in ('poly','line'):
        return 'M'+'L'.join(f'{num(x)},{num(y)}' for x,y in a['points'])+('z' if typ=='poly' else '')
    if typ=='circle':
        x,y,r=a['x'],a['y'],a['r']
        return f'M{num(x-r)},{num(y)}a{num(r)},{num(r)} 0,1 0,{num(2*r)} 0a{num(r)},{num(r)} 0,1 0,{num(-2*r)} 0z'
    l,t,r,b=a['box'];q=a['radius']
    return f'M{num(l+q)},{num(t)}H{num(r-q)}Q{num(r)},{num(t)} {num(r)},{num(t+q)}V{num(b-q)}Q{num(r)},{num(b)} {num(r-q)},{num(b)}H{num(l+q)}Q{num(l)},{num(b)} {num(l)},{num(b-q)}V{num(t+q)}Q{num(l)},{num(t)} {num(l+q)},{num(t)}z'


def android(items,viewport=108,size=108,background=None,monochrome=False):
    paths=[]
    if background:paths.append(f'    <path android:fillColor="{background}" android:pathData="M0,0h{viewport}v{viewport}H0z"/>')
    for a in items:
        color='#ffffff' if monochrome else a.get('fill',a.get('stroke'))
        if a['type']=='line':style=f'android:fillColor="@android:color/transparent" android:strokeColor="{color}" android:strokeWidth="{num(a["width"])}" android:strokeLineCap="round" android:strokeLineJoin="round"'
        else:style=f'android:fillColor="{color}"'
        paths.append(f'    <path {style} android:pathData="{pathdata(a)}"/>')
    return f'<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="{size}dp" android:height="{size}dp" android:viewportWidth="{viewport}" android:viewportHeight="{viewport}">\n'+'\n'.join(paths)+'\n</vector>\n'


def svg(items,viewport=108,background=None):
    paths=[]
    if background:paths.append(f'<rect width="{viewport}" height="{viewport}" fill="{background}"/>')
    for a in items:
        style=f'fill="none" stroke="{a["stroke"]}" stroke-width="{num(a["width"])}" stroke-linecap="round" stroke-linejoin="round"' if a['type']=='line' else f'fill="{a["fill"]}"'
        paths.append(f'<path d="{pathdata(a)}" {style}/>')
    return f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {viewport} {viewport}">\n'+'\n'.join(paths)+'\n</svg>\n'


def render(items,size=(512,512),viewport=108,background=None,monochrome=False):
    k=size[0]/viewport*4
    im=Image.new('RGBA',(size[0]*4,size[1]*4),background or (0,0,0,0));d=ImageDraw.Draw(im)
    for a in items:
        fill='#ffffff' if monochrome else a.get('fill',a.get('stroke'))
        if a['type'] in ('poly','line'):
            pts=[(x*k,y*k) for x,y in a['points']]
            if a['type']=='poly':d.polygon(pts,fill=fill)
            else:
                w=max(1,round(a['width']*k));d.line(pts,fill=fill,width=w,joint='curve')
                for x,y in pts:d.ellipse((x-w/2,y-w/2,x+w/2,y+w/2),fill=fill)
        elif a['type']=='circle':
            x,y,r=a['x']*k,a['y']*k,a['r']*k;d.ellipse((x-r,y-r,x+r,y+r),fill=fill)
        else:d.rounded_rectangle(tuple(v*k for v in a['box']),radius=a['radius']*k,fill=fill)
    return im.resize(size,SAMPLE)


def write(path,text):
    path.parent.mkdir(parents=True,exist_ok=True);path.write_text(text,encoding='utf-8')


def write_android():
    validate_safe_circle()
    art=branding();legacy=transform(art,1.2,-10.8,-10.8)
    write(RES/'drawable/icon.xml',android(legacy,background=BG))
    write(RES/'mipmap-anydpi/ic_launcher.xml',android(legacy,background=BG))
    write(RES/'drawable/ic_falley_foreground.xml',android(art))
    write(RES/'drawable/ic_falley_monochrome.xml',android(art,monochrome=True))
    write(RES/'drawable/splash_icon.xml',android(transform(art,2,-8,-8),viewport=200,size=240))
    write(RES/'values/icon_colors.xml','<resources>\n    <color name="falley_icon_background">#17191B</color>\n</resources>\n')
    for version in (26,33):
        mono='\n    <monochrome android:drawable="@drawable/ic_falley_monochrome"/>' if version==33 else ''
        write(RES/f'mipmap-anydpi-v{version}/ic_launcher.xml',f'<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">\n    <background android:drawable="@color/falley_icon_background"/>\n    <foreground android:drawable="@drawable/ic_falley_foreground"/>{mono}\n</adaptive-icon>\n')
    src=ROOT/'store/brand'
    write(src/'falley-demon-icon.svg',svg(legacy,background=BG))
    write(src/'falley-demon-foreground.svg',svg(art))
    return RES/'drawable/icon.xml'


def render_store_icon():
    dest=OUT/'en-US/icon-512.png';dest.parent.mkdir(parents=True,exist_ok=True)
    render(transform(branding(),1.2,-10.8,-10.8),background=BG).save(dest,optimize=True)
    return dest


def font(size,bold=False):
    candidates=[Path('C:/Windows/Fonts/segoeuib.ttf' if bold else 'C:/Windows/Fonts/segoeui.ttf'),Path('/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf' if bold else '/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf')]
    for p in candidates:
        if p.exists():return ImageFont.truetype(str(p),size)
    return ImageFont.load_default(size=size)


def render_feature(kind='default'):
    art=[rect(0,0,1024,500,0,BG)]
    # An original illustrated poster, not an app screenshot or level mockup.
    for x in range(610,970,44):art.append(line([(x,66),(x,426)],'#233332',.65))
    for y in range(74,427,44):art.append(line([(597,y),(963,y)],'#233332',.65))
    art+=wordmark(76,164,70,width=420)+demon(790,250,82)
    rope=[(625+i*325/60,370+26*math.sin(math.pi*i/60)) for i in range(61)]
    art+=[line(rope,'#294c43',14),line(rope,MINT,4),circle(625,370,5,MINT),circle(950,370,5,MINT)]
    if kind=='motion':
        art+=[line([(620,244),(603,258),(620,272)],MINT,3),line([(953,244),(970,258),(953,272)],MINT,3)]
    elif kind=='arcade':
        art+=[line([(953,109),(930,149),(950,149),(927,194)],'#8fd7ff',4),circle(623,151,15,'#ff9c52')]
    im=render(art,(2048,1000),viewport=1024,background=BG).convert('RGB');d=ImageDraw.Draw(im)
    d.text((154,206),'PERF GAMES',font=font(26,True),fill=MINT)
    caption={'default':('Fall. Dodge. Land.','Physics in your hands.'),'motion':('Move with gravity.','Tilt or swipe. Find your landing.'),'arcade':('Survive the fall.','Choose your Arcade hazards.')}[kind]
    d.text((152,550),caption[0],font=font(56,True),fill=MINT)
    d.text((152,640),caption[1],font=font(36),fill=MUTED)
    dest=OUT/'en-US'/('feature-graphic-1024x500.png' if kind=='default' else f'variants/feature-{kind}-1024x500.png')
    dest.parent.mkdir(parents=True,exist_ok=True);im.resize((1024,500),SAMPLE).save(dest,optimize=True)
    return dest


def sha(path):return hashlib.sha256(path.read_bytes()).hexdigest()


def manifest_entries():
    icon=OUT/'en-US/icon-512.png';src=RES/'drawable/icon.xml'
    entries=[{'path':'en-US/icon-512.png','kind':'icon','alt_text':'Falley icon: the original winged Demon avatar above a crisp FALLEY wordmark.','source':src.relative_to(ROOT).as_posix(),'source_sha256':sha(src),'provenance':'Original code-native Demon rig and geometric FALLEY wordmark; supersampled from shared Android/vector geometry. No imposed corner mask or shadow.'}]
    for kind in ('default','motion','arcade'):
        dest=OUT/'en-US'/('feature-graphic-1024x500.png' if kind=='default' else f'variants/feature-{kind}-1024x500.png')
        entries.append({'path':dest.relative_to(OUT).as_posix(),'kind':'feature_graphic','alt_text':{'default':'FALLEY wordmark and winged Demon avatar above a mint rope.','motion':'FALLEY wordmark and winged Demon avatar with motion arrows above a mint rope.','arcade':'FALLEY wordmark and winged Demon avatar with illustrated fireball and lightning.'}[kind],'source':'store/brand/generate_brand.py','source_sha256':sha(Path(__file__)),'provenance':'Original promotional vector illustration matching the Demon avatar rig and game palette; not a gameplay screenshot.'})
    for entry in entries:
        p=OUT/entry['path']
        with Image.open(p) as im:entry.update(dimensions=list(im.size),mode=im.mode,bytes=p.stat().st_size,sha256=sha(p),brand_version=VERSION)
    return entries


def update_manifest():
    p=OUT/'manifest.json';data=json.loads(p.read_text(encoding='utf-8-sig'))
    replacements={x['path']:x for x in manifest_entries()}
    data['assets']=[replacements.get(x['path'],x) for x in data['assets']]
    data['brand_version']=VERSION;data['branding_updated_date']='2026-10-25'
    note='Branding refreshed for 4.2.1. Phone screenshot source versions remain explicit; do not describe historical native captures as current gameplay.'
    if note not in data['notes']:data['notes'].append(note)
    write(p,json.dumps(data,indent=2)+'\n')


def contact_sheet(path):
    im=Image.new('RGB',(1400,1210),'#101315');d=ImageDraw.Draw(im)
    d.text((36,25),'FALLEY / DEMON BRAND REVIEW',font=font(34,True),fill=WHITE)
    d.text((36,73),'Original avatar geometry. Android legacy, adaptive masks, themed icon and Play art.',font=font(20),fill=MUTED)
    icon=Image.open(OUT/'en-US/icon-512.png').convert('RGBA');im.paste(icon.resize((290,290),SAMPLE),(36,125))
    d.text((36,432),'512px Play / legacy composition',font=font(20),fill=MUTED)
    for i,size in enumerate((48,64,96)):
        x=385+i*170;im.paste(icon.resize((size,size),SAMPLE),(x,158),icon.resize((size,size),SAMPLE));d.text((x,278),f'{size}px actual',font=font(18),fill=MUTED)
    # Android masks the central 72px of a 108px adaptive foreground; preview that crop.
    art=render(branding(),(324,324),background=BG).crop((54,54,270,270))
    for i,shape in enumerate(('circle','squircle','square','themed')):
        x=385+i*235;y=350
        tile=art.copy()
        if shape=='themed':
            tile=Image.new('RGBA',(216,216),'#d9e8f6');mono=render(branding(),(324,324),monochrome=True).crop((54,54,270,270));tint=Image.new('RGBA',mono.size,'#263342');tint.putalpha(mono.getchannel('A'));tile.alpha_composite(tint)
        mask=Image.new('L',(216,216),0);md=ImageDraw.Draw(mask)
        if shape in ('circle','themed'):md.ellipse((0,0,215,215),fill=255)
        elif shape=='squircle':md.rounded_rectangle((0,0,215,215),radius=52,fill=255)
        else:md.rectangle((0,0,215,215),fill=255)
        im.paste(tile,(x,y),mask);d.text((x,y+230),shape,font=font(18),fill=MUTED)
    splash=render(transform(branding(),2,-8,-8),(240,240),viewport=200,background=BG)
    im.paste(splash,(36,492));d.text((36,745),'Android splash composition',font=font(18),fill=MUTED)
    feature=Image.open(OUT/'en-US/feature-graphic-1024x500.png')
    feature.thumbnail((1328,398),SAMPLE);im.paste(feature,((1400-feature.width)//2,795))
    path.parent.mkdir(parents=True,exist_ok=True);im.save(path,optimize=True)


def main():
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--preview',type=Path,default=ROOT/'build/branding-4.2.1/brand-review.png');a=parser.parse_args()
    write_android();render_store_icon()
    for kind in ('default','motion','arcade'):render_feature(kind)
    update_manifest();contact_sheet(a.preview)
    print('PASS: original vector resources, Play icon and three promotional illustrations generated. Native gameplay captures unchanged.')
    print(f'Preview: {a.preview}')

if __name__=='__main__':main()
