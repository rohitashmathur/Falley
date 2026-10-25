package com.perfgames.ropelab;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;

/** Original cosmetic variants share one rig and exactly the same contact height. */
public final class AvatarRenderer {
    private static final Paint P=new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Path PATH=new Path();
    public static void draw(Canvas c,float x,float y,float r,float vy,float landingPulse,int avatar,int palette){
        int id=Math.max(0,Math.min(AvatarCatalog.COUNT-1,avatar));
        AvatarPose pose=AvatarPose.sample(id,vy,landingPulse);
        float q=pose.landing,pelvis=pose.pelvis,shoulder=pose.shoulder,head=pose.head;
        float ky=pose.kneeY,kx=pose.kneeX,hy=pose.handY,hx=pose.handX;
        float fx=pose.footX,fy=pose.footY;
        int skin=id==1 ? 0xfff1948b:id==5 ? 0xff91bbc7:0xffffd4ae;
        int shirt=accent(id,palette), legs=id==3 ? 0xff759081:id==4 ? 0xffbbc4e3:id==6 ? 0xff65638c:0xff65b9f2;
        int arms=id>=3 ? shirt:skin;
        c.save();c.translate(x,y);c.scale(r,r);
        if(id==1 || id==2)for(int side=-1;side<=1;side+=2){
            float span=pose.wingSpan,tip=shoulder+pose.wingLift;
            if(id==1){
                // A bat-like fan opens with upward speed; the leading edge follows the pose.
                poly(c,0xff792d53,side*.26f,shoulder+.05f,side*span,tip,
                     side*(span*.86f),shoulder+.15f,side*(span*.64f),shoulder+.03f,
                     side*(span*.50f),shoulder+.40f,side*.35f,shoulder+.47f);
                line(c,side*.32f,shoulder+.13f,side*span,tip,0xffd46a83,.06f);
                line(c,side*.34f,shoulder+.17f,side*span*.64f,shoulder+.03f,0xffa54568,.04f);
            }else{
                // Three feather lobes give Angel a broad, soft, lifted silhouette.
                poly(c,0xffabc9de,side*.24f,shoulder+.03f,side*span,tip,
                     side*(span*.92f),tip+.28f,side*(span*.74f),tip+.18f,
                     side*(span*.77f),tip+.47f,side*(span*.57f),tip+.39f,
                     side*(span*.54f),shoulder+.42f,side*.31f,shoulder+.49f);
                line(c,side*.34f,shoulder+.16f,side*span*.91f,tip+.11f,0xffe1f5ff,.075f);
                line(c,side*.34f,shoulder+.21f,side*span*.70f,tip+.35f,0xffd1e8f7,.055f);
            }
        }
        if(id==4)rect(c,-.47f,shoulder+.09f,.47f,pelvis,.12f,0xff667596);
        line(c,-.17f,pelvis,-kx,ky,legs,.22f);line(c,kx,ky,.17f,pelvis,legs,.22f);
        line(c,-kx,ky,-fx,fy,legs,.18f);line(c,kx,ky,fx,fy,legs,.18f);
        line(c,-fx-.09f,fy,-fx+.08f,fy,0xffedf8f7,.15f);line(c,fx-.08f,fy,fx+.09f,fy,0xffedf8f7,.15f);
        if(id==5&&pose.rise>.05f){
            // A visible little piston folds between the unchanged hip and knee rig.
            for(int side=-1;side<=1;side+=2){
                float ax=side*.18f,ay=pelvis+.04f,bx=side*kx,by=ky;
                for(int i=0;i<4;i++){
                    float t=i/4f,tt=(i+1)/4f;
                    float xx=ax+(bx-ax)*t+(i%2==0?-.06f:.06f);
                    float xxx=ax+(bx-ax)*tt+(i%2==0?.06f:-.06f);
                    line(c,xx,ay+(by-ay)*t,xxx,ay+(by-ay)*tt,0xffc5e5eb,.045f);
                }
                circle(c,bx,by,.09f,0xff5de0ba);
            }
        }
        line(c,-.32f,shoulder,-hx,hy,arms,.16f);line(c,.32f,shoulder,hx,hy,arms,.16f);
        circle(c,-hx,hy,.11f,id==4 ? 0xffedf8f7:skin);circle(c,hx,hy,.11f,id==4 ? 0xffedf8f7:skin);
        rect(c,-pose.torsoWidth,shoulder-.02f,pose.torsoWidth,pelvis+.06f,.13f,shirt);
        line(c,-.13f,shoulder+.12f,.13f,shoulder+.12f,0xff263342,.065f);
        if(id==5){
            line(c,0,head-.33f,0,head-.50f,skin,.05f);circle(c,0,head-.52f,.07f,0xff5de0ba);
            rect(c,-.35f,head-.30f,.35f,head+.30f,.12f,skin);
            rect(c,-.26f,head-.11f,.26f,head+.13f,.07f,0xff263342);
            circle(c,-.115f,head,.052f,0xff5de0ba);circle(c,.115f,head,.052f,0xff5de0ba);
            line(c,-.1f,head+.22f,.1f,head+.22f,0xff263342,.035f);
            circle(c,0,shoulder+.37f,.075f,0xff5de0ba);
        }else if(id==4){
            circle(c,0,head,.40f,0xffedf8f7);rect(c,-.31f,head-.21f,.31f,head+.18f,.16f,0xff283e5b);
            line(c,-.18f,head-.12f,.08f,head-.12f,0xff8fceee,.055f);
            rect(c,-.20f,shoulder+.22f,.20f,shoulder+.46f,.04f,0xff4d5f80);
            circle(c,-.09f,shoulder+.33f,.035f,0xff5de0ba);circle(c,.08f,shoulder+.33f,.035f,0xffff816c);
        }else if(id==6){
            poly(c,0xff65638c,0,head-.48f,-.42f,head+.24f,.42f,head+.24f);
            rect(c,-.26f,head-.17f,.26f,head+.23f,.16f,0xff282938);
            line(c,-.16f,head+.02f,-.07f,head+.02f,0xffae99ee,.055f);
            line(c,.07f,head+.02f,.16f,head+.02f,0xffae99ee,.055f);
        }else{
            circle(c,0,head,.34f,skin);circle(c,0,head-.20f,.26f,id==2 ? 0xffb8a06c:0xff353133);
            rect(c,-.30f,head-.17f,.30f,head+.08f,.10f,skin);
            circle(c,-.105f,head+.035f,.036f,0xff171b20);circle(c,.105f,head+.035f,.036f,0xff171b20);
            line(c,-.08f,head+.17f,.08f,head+.17f,0xff9b6653,.035f);
            if(id==1){poly(c,0xfff6d477,-.27f,head-.15f,-.33f,head-.55f,-.08f,head-.30f);poly(c,0xfff6d477,.27f,head-.15f,.33f,head-.55f,.08f,head-.30f);}
            if(id==2){P.setColor(0xfff6d477);P.setStrokeWidth(.055f);P.setStyle(Paint.Style.STROKE);c.drawOval(-.29f,head-.64f,.29f,head-.48f,P);P.setStyle(Paint.Style.FILL);}
            if(id==3){rect(c,-.36f,head-.36f,.36f,head-.08f,.14f,0xff7f9b84);line(c,-.38f,head-.08f,.38f,head-.08f,0xff4f6a59,.075f);rect(c,-.27f,shoulder+.24f,-.04f,shoulder+.43f,.03f,0xff4f6a59);rect(c,.04f,shoulder+.24f,.27f,shoulder+.43f,.03f,0xff4f6a59);}
        }
        c.restore();
    }
    public static int accent(int id,int palette){
        if(palette==1)return 0xff5de0ba;if(palette==2)return 0xffae99ee;if(palette==3)return 0xfff6d477;
        int[] colors={0xffff816c,0xffd65f78,0xffd9e8f6,0xff7f9b84,0xffcbd3ee,0xff83afc1,0xff65638c};
        return colors[Math.max(0,Math.min(colors.length-1,id))];
    }
    private static void poly(Canvas c,int color,float...p){P.setStyle(Paint.Style.FILL);P.setColor(color);PATH.reset();PATH.moveTo(p[0],p[1]);for(int i=2;i<p.length;i+=2)PATH.lineTo(p[i],p[i+1]);PATH.close();c.drawPath(PATH,P);}
    private static void line(Canvas c,float x,float y,float xx,float yy,int color,float width){P.setStyle(Paint.Style.STROKE);P.setStrokeWidth(width);P.setStrokeCap(Paint.Cap.ROUND);P.setColor(color);c.drawLine(x,y,xx,yy,P);P.setStyle(Paint.Style.FILL);}
    private static void circle(Canvas c,float x,float y,float r,int color){P.setStyle(Paint.Style.FILL);P.setColor(color);c.drawCircle(x,y,r,P);}
    private static void rect(Canvas c,float l,float t,float r,float b,float radius,int color){P.setStyle(Paint.Style.FILL);P.setColor(color);c.drawRoundRect(l,t,r,b,radius,radius,P);}
    private AvatarRenderer(){}
}
