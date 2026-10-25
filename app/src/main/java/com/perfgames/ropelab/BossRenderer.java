package com.perfgames.ropelab;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;

/** Original boss actors and attack art. All positions and danger bounds come from Physics. */
public final class BossRenderer {
    private static final Paint P=new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Path PATH=new Path();
    private static final int DARK=0xff172029, STEEL=0xff8196a5, WHITE=0xfff0f7f7,
            MINT=0xff5de0ba, GOLD=0xfff6d477, PINK=0xffff5f9a, FIRE=0xffff9c52, BOLT=0xff8fd7ff;

    public static String name(int chapter){
        if(chapter<2)return "GEAR RIG";
        return chapter==2?"FURNACE DEMON":chapter==3?"STORM ROBOT":chapter==4?"FORGE ROBOT":"CORE DEMON";
    }

    /** Draw before the existing hazards; this method never changes collision or simulation state. */
    public static void draw(Canvas c,Physics physics,float time,float top,float bottom){
        if(!physics.bossActive||physics.arcade||physics.daily)return;
        if(!Float.isFinite(time))time=0;
        int chapter=physics.stage.chapterIndex;
        float sourceY=physics.bossSourceY-physics.cameraY;
        // No attack yet: waiting actors sit clear of the ceiling and HUD.
        boolean hasSource=physics.bossSourceKind!=0;
        if(!hasSource)sourceY=top+110;
        sourceY=clamp(sourceY,top+12,bottom-12);
        int color=attackColor(physics.bossSourceKind);
        float charge=physics.bossWarning>0?clamp(1-physics.bossWarning/1.55f,0,1):0;
        boolean warning=physics.bossWarning>0,live=physics.bossLive>0;
        c.save();c.clipRect(20,top,460,bottom);
        for(int side=-1;side<=1;side+=2){
            boolean active=hasSource&&side==physics.bossSourceSide;
            float x=side<0?physics.leftWall()+physics.hazardInset():physics.rightWall()-physics.hazardInset();
            if(active)x=physics.bossSourceX;
            float direction=side<0?1:-1;
            float available=side<0?x-20:460-x;
            float actorWidth=chapter<2?60:96;
            float actorScale=clamp((available-2)/actorWidth,.60f,1);
            c.save();c.translate(x,sourceY);c.scale(direction*actorScale,actorScale);
            if(chapter<2)gear(c,time,active?charge:0,active&&warning,active&&live,color);
            else if(chapter==2||chapter==5)demon(c,chapter==5,active?charge:0,active&&warning,active&&live,color);
            else robot(c,chapter==3,active?charge:0,active&&warning,active&&live,color);
            c.restore();
        }
        // Short power feed ends at the actual existing attack, never at an invented target.
        if(hasSource&&(warning||live)){
            float sx=physics.bossSourceX,sy=physics.bossSourceY-physics.cameraY;
            if(physics.bossSourceKind==ArcadeRules.TOP_LASERS||physics.bossSourceKind==ArcadeRules.SIDE_LASERS){
                for(Physics.BeamHazard b:physics.beamHazards)if(b.active&&b.bossLane){
                    float edge=b.x1+(sx<b.x1?-1:1)*b.width*.5f;
                    feed(c,sx,sy,edge,clamp(sy,top,bottom),color,warning);break;
                }
            }else if(physics.bossSourceKind==ArcadeRules.BUZZSAWS){
                for(Physics.SawHazard saw:physics.sawHazards)if(saw.active&&saw.bossRail){
                    feed(c,sx,sy,saw.x,sy,color,warning);break;
                }
            }
        }
        c.restore();
    }

    private static void gear(Canvas c,float time,float charge,boolean warning,boolean live,int color){
        rect(c,-58,-61,-15,63,8,0xff303e48);rect(c,-52,-55,-22,54,5,DARK);
        line(c,-37,-53,-37,52,0xff50626c,3);
        c.save();c.translate(-36,2);c.rotate(time*(live?210:warning?95:35));
        for(int i=0;i<12;i++){
            double a=i*Math.PI/6;
            line(c,(float)Math.cos(a)*18,(float)Math.sin(a)*18,(float)Math.cos(a)*26,(float)Math.sin(a)*26,warning?GOLD:STEEL,5);
        }
        circle(c,0,0,19,STEEL);circle(c,0,0,12,DARK);circle(c,0,0,6,warning||live?color:MINT);c.restore();
        // Cannon collar stays at the same point as the spawned blade/laser mouth.
        rect(c,-30,-12,0,12,4,0xff556b78);rect(c,-19,-8,2,8,3,0xff92a8b4);
        line(c,-48,37,-27,37,0xff647b87,4);circle(c,-37,-40,5,warning?GOLD:live?color:MINT);
        nozzle(c,charge,warning,live,color,false);
    }

    private static void robot(Canvas c,boolean storm,float charge,boolean warning,boolean live,int color){
        int armor=storm?0xff58758c:0xff77786e,trim=storm?BOLT:GOLD;
        // Large outboard silhouette: shoulders, piston legs, antenna and a recognizable visor.
        rect(c,-77,-24,-29,43,9,0xff303d48);rect(c,-72,-21,-34,36,6,armor);
        rect(c,-73,-60,-34,-25,8,0xffa5b8c3);rect(c,-70,-52,-37,-33,5,DARK);
        line(c,-65,-43,-43,-43,warning||live?color:trim,4);
        line(c,-54,-61,-54,-74,STEEL,3);circle(c,-54,-77,5,warning||live?color:trim);
        for(int side=-1;side<=1;side+=2){
            float leg=-53+side*13;
            line(c,leg,37,leg+side*4,63,0xff4d5b65,14);
            line(c,leg+side*4,61,leg+side*6,77,STEEL,9);
            rect(c,leg+side*6-10,74,leg+side*6+11,84,4,0xffb9c7ce);
        }
        rect(c,-83,-17,-69,17,5,0xff9bb0ba);line(c,-77,10,-82,39,armor,10);
        circle(c,-53,7,14,DARK);circle(c,-53,7,9,warning||live?color:trim);
        line(c,-57,3,-49,11,WHITE,2);line(c,-49,3,-57,11,WHITE,2);
        // The forearm terminates exactly at local(0,0), matching bossSourceX/Y.
        line(c,-34,-15,-23,0,armor,16);circle(c,-23,0,10,STEEL);
        rect(c,-24,-10,0,10,4,0xffb9cbd3);rect(c,-12,-14,0,14,4,0xff526d80);
        for(int i=0;i<3;i++)line(c,-17+i*5,-11,-17+i*5,11,storm?trim:0xffc4b889,2);
        nozzle(c,charge,warning,live,color,storm);
    }

    private static void demon(Canvas c,boolean core,float charge,boolean warning,boolean live,int color){
        int skin=core?0xffa9719b:0xffd57b63,wing=core?0xff4e315e:0xff76374a,trim=core?0xffc69fee:GOLD;
        poly(c,wing,-52,-37,-93,-65,-83,-9,-72,-19,-63,1,-51,-7);
        line(c,-54,-32,-91,-63,core?0xffa56bcb:0xffcb6577,3);
        poly(c,wing,-41,-36,-29,-66,-23,-21,-33,-26,-34,0,-45,-9);
        line(c,-41,-31,-29,-64,core?0xffa56bcb:0xffcb6577,2);
        rect(c,-69,-25,-32,35,9,core?0xff663f74:0xffab4d56);
        line(c,-61,34,-69,59,skin,13);line(c,-69,59,-74,76,0xff5b4053,11);
        line(c,-40,34,-34,60,skin,13);line(c,-34,60,-31,76,0xff5b4053,11);
        line(c,-81,78,-68,78,trim,5);line(c,-38,78,-25,78,trim,5);
        // Tail and far hand remain beyond the dangerous arena corridor.
        line(c,-63,28,-85,42,core?0xff8e649c:0xffac5660,5);poly(c,trim,-89,35,-93,48,-80,46);
        line(c,-66,-16,-78,5,skin,10);line(c,-78,5,-80,23,skin,8);
        circle(c,-51,-46,20,skin);
        poly(c,trim,-68,-54,-74,-83,-53,-64);poly(c,trim,-34,-54,-29,-83,-49,-64);
        poly(c,0xff3d2d3b,-68,-49,-59,-67,-43,-67,-34,-49,-43,-56,-59,-56);
        line(c,-62,-43,-54,-40,warning||live?WHITE:trim,3);line(c,-47,-40,-39,-43,warning||live?WHITE:trim,3);
        line(c,-55,-32,-45,-32,0xff3d2d3b,2);
        line(c,-35,-17,-20,-3,skin,11);line(c,-20,-3,-5,0,skin,8);
        circle(c,-4,0,8,skin);line(c,-5,-5,0,-10,skin,4);line(c,-4,4,1,9,skin,4);
        circle(c,-50,5,11,0xff322b3b);circle(c,-50,5,6,warning||live?color:trim);
        nozzle(c,charge,warning,live,color,false);
    }

    private static void nozzle(Canvas c,float charge,boolean warning,boolean live,int color,boolean coil){
        circle(c,0,0,9,DARK);
        if(warning){
            circle(c,0,0,14+charge*9,withAlpha(color,28+(int)(charge*34)));
            ring(c,0,0,11+charge*9,color,1.5f);circle(c,0,0,3+charge*5,color);
            for(int i=0;i<3;i++){double a=i*Math.PI*2/3;float r=17-charge*7;circle(c,(float)Math.cos(a)*r,(float)Math.sin(a)*r,2.2f,color);}
        }else if(live){
            circle(c,0,0,23,withAlpha(color,58));circle(c,0,0,12,color);circle(c,0,0,5,WHITE);
            if(coil)poly(c,WHITE,0,-14,8,-4,4,-4,13,9,4,3,8,3,0,14);
            else poly(c,withAlpha(color,170),0,-11,23,0,0,11,5,0);
        }else circle(c,0,0,4,0xff536571);
    }

    /** Full-width laser grid exactly matches the existing bossLane collision footprint. */
    public static void drawBeamLane(Canvas c,Physics physics,Physics.BeamHazard beam,float top,float bottom){
        float y1=Math.max(top,Math.min(beam.y1,beam.y2)-physics.cameraY);
        float y2=Math.min(bottom,Math.max(beam.y1,beam.y2)-physics.cameraY);
        if(y2<=y1)return;
        float left=beam.x1-beam.width*.5f,right=beam.x1+beam.width*.5f;
        boolean warning=beam.warningTime>0;int color=warning?GOLD:PINK;
        c.save();c.clipRect(left,y1,right,y2);
        rect(c,left,y1,right,y2,0,withAlpha(color,warning?27:70));
        for(float x=left-(y2-y1);x<right;x+=22)line(c,x,y1,x+(y2-y1),y2,withAlpha(color,warning?85:135),warning?1:2);
        if(!warning){for(float x=left+6;x<right;x+=15)line(c,x,y1,x,y2,withAlpha(WHITE,115),1.2f);}
        c.restore();
        if(warning){dashed(c,left,y1,left,y2,color,2);dashed(c,right,y1,right,y2,color,2);}
        else{line(c,left,y1,left,y2,PINK,3);line(c,right,y1,right,y2,PINK,3);}
        // Lens caps locate the field on the top/bottom rails instead of an unexplained flood of color.
        rect(c,left,y1,right,y1+7,2,0xff4e3648);line(c,left+3,y1+4,right-3,y1+4,color,2);
        rect(c,left,y2-7,right,y2,2,0xff4e3648);line(c,left+3,y2-4,right-3,y2-4,color,2);
    }

    /** The full-height moving blade bar and its complete swept warning use the real rail bounds. */
    public static void drawSawRail(Canvas c,Physics physics,Physics.SawHazard saw,float time,float top,float bottom){
        float y1=Math.max(top,Math.min(saw.railTop,saw.railBottom)-physics.cameraY);
        float y2=Math.min(bottom,Math.max(saw.railTop,saw.railBottom)-physics.cameraY);
        if(y2<=y1)return;
        if(!Float.isFinite(time))time=0;
        boolean warning=saw.warningTime>0;
        float from=Math.min(saw.pathX1,saw.pathX2),to=Math.max(saw.pathX1,saw.pathX2),r=saw.radius;
        // The rail is a finite rectangle. Teeth must not imply danger beyond its blunt ends.
        c.save();c.clipRect(20,y1,460,y2);
        // Two mechanical rails explain how the same existing saw sweep carries a whole blade column.
        line(c,from,y1+4,to,y1+4,0xff61707b,7);line(c,from,y2-4,to,y2-4,0xff61707b,7);
        if(warning){
            rect(c,from-r,y1,to+r,y2,0,0x18f6d477);
            dashed(c,from-r,y1,from-r,y2,GOLD,1.5f);dashed(c,to+r,y1,to+r,y2,GOLD,1.5f);
            for(float y=y1+24;y<y2;y+=34)dashed(c,from,y,to,y,0x77f6d477,1);
        }
        int bar=warning?0x667f8d9e:0xff8c9fae;
        // Solid spine fills the collision width even between the circular blade teeth.
        rect(c,saw.x-r,y1,saw.x+r,y2,0,bar);
        line(c,saw.x,y1,saw.x,y2,warning?0x66818e99:0xff384a58,6);
        for(float y=y1+r;y<y2;y+=r*2){
            c.save();c.translate(saw.x,y);c.rotate(time*440*(saw.velocityX<0?-1:1));
            PATH.reset();
            for(int i=0;i<32;i++){
                double a=i*Math.PI/16;float rr=r*(i%4<2?1:.76f);
                float x=(float)Math.cos(a)*rr,yy=(float)Math.sin(a)*rr;
                if(i==0)PATH.moveTo(x,yy);else PATH.lineTo(x,yy);
            }
            PATH.close();P.setStyle(Paint.Style.FILL);P.setColor(warning?0x8896a3ad:0xffd3dce1);c.drawPath(PATH,P);
            circle(c,0,0,r*.58f,warning?0x66546570:0xff566d7b);circle(c,0,0,r*.17f,warning?GOLD:0xffff816c);c.restore();
        }
        circle(c,saw.x,y1+5,6,warning?GOLD:WHITE);circle(c,saw.x,y2-5,6,warning?GOLD:WHITE);
        c.restore();
    }

    private static int attackColor(int kind){return kind==ArcadeRules.FIREBALLS?FIRE:kind==ArcadeRules.LIGHTNING?BOLT:kind==ArcadeRules.BUZZSAWS?GOLD:PINK;}
    private static int withAlpha(int color,int alpha){return (color&0xffffff)|(alpha<<24);}
    private static float clamp(float v,float lo,float hi){return Math.max(lo,Math.min(hi,v));}
    private static void feed(Canvas c,float x,float y,float xx,float yy,int color,boolean warning){if(warning)dashed(c,x,y,xx,yy,withAlpha(color,180),2);else line(c,x,y,xx,yy,withAlpha(color,170),3);}
    private static void dashed(Canvas c,float x,float y,float xx,float yy,int color,float width){
        float dx=xx-x,dy=yy-y,length=(float)Math.hypot(dx,dy);
        for(float d=0;d<length;d+=18){float a=d/Math.max(1,length),b=Math.min(length,d+9)/Math.max(1,length);line(c,x+dx*a,y+dy*a,x+dx*b,y+dy*b,color,width);}
    }
    private static void rect(Canvas c,float l,float t,float r,float b,float radius,int color){P.setStyle(Paint.Style.FILL);P.setColor(color);c.drawRoundRect(l,t,r,b,radius,radius,P);}
    private static void circle(Canvas c,float x,float y,float r,int color){P.setStyle(Paint.Style.FILL);P.setColor(color);c.drawCircle(x,y,r,P);}
    private static void ring(Canvas c,float x,float y,float r,int color,float width){P.setStyle(Paint.Style.STROKE);P.setStrokeWidth(width);P.setColor(color);c.drawCircle(x,y,r,P);P.setStyle(Paint.Style.FILL);}
    private static void line(Canvas c,float x,float y,float xx,float yy,int color,float width){P.setStyle(Paint.Style.STROKE);P.setStrokeCap(Paint.Cap.ROUND);P.setStrokeJoin(Paint.Join.ROUND);P.setStrokeWidth(width);P.setColor(color);c.drawLine(x,y,xx,yy,P);P.setStyle(Paint.Style.FILL);}
    private static void poly(Canvas c,int color,float...p){PATH.reset();PATH.moveTo(p[0],p[1]);for(int i=2;i<p.length;i+=2)PATH.lineTo(p[i],p[i+1]);PATH.close();P.setStyle(Paint.Style.FILL);P.setColor(color);c.drawPath(PATH,P);}
    private BossRenderer(){}
}
