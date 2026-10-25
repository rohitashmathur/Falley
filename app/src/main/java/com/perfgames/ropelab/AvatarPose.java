package com.perfgames.ropelab;

/** Cosmetic rig coordinates. Every avatar uses the same collision body in Physics. */
public final class AvatarPose {
    public final float pelvis, shoulder, head, kneeX, kneeY, footX, footY;
    public final float handX, handY, wingSpan, wingLift, torsoWidth, rise, fall, landing;
    private AvatarPose(float p,float s,float h,float kx,float ky,float fx,float fy,
                       float hx,float hy,float wings,float lift,float width,float up,float down,float q){
        pelvis=p;shoulder=s;head=h;kneeX=kx;kneeY=ky;footX=fx;footY=fy;
        handX=hx;handY=hy;wingSpan=wings;wingLift=lift;torsoWidth=width;rise=up;fall=down;landing=q;
    }
    public static AvatarPose sample(int avatar,float velocity,float landingPulse){
        int id=Math.max(0,Math.min(AvatarCatalog.COUNT-1,avatar));
        float v=Float.isFinite(velocity)?velocity:0;
        float q=Float.isFinite(landingPulse)?clamp(landingPulse/.18f):0;
        float up=smooth(clamp((-v-30)/190)),down=smooth(clamp((v-45)/275));
        // Contact compression wins over airborne gestures, retaining the same foot line at +1.
        up*=1-q;down*=1-q;
        float p=.28f+q*.22f,s=-.44f+q*.29f,h=-.95f+q*.30f;
        float kx=.34f+q*.23f,ky=.60f+q*.09f-.08f*up,fx=.31f,fy=1;
        float hx=.57f+.09f*up+.38f*down,hy=.28f-1.13f*up-.53f*down;
        float wings=.83f,lift=-.45f,width=.34f;
        if(id==AvatarCatalog.DEMON){
            wings+=.66f*up+.45f*down;lift-=.52f*up+.21f*down;
            hx+=.19f*up;hy+=.17f*up;kx+=.12f*up;
        }else if(id==AvatarCatalog.ANGEL){
            wings+=.76f*up+.54f*down;lift-=.81f*up+.43f*down;
            hx+=.35f*up+.09f*down;hy+=.37f*up+.10f*down;
            kx-=.12f*up;ky+=.12f*up;fx-=.09f*up;fy-=.12f*up;
        }else if(id==AvatarCatalog.MILITARY){
            kx+=.14f*up;ky-=.53f*up;fx+=.20f*up;fy-=.55f*up;
            hx-=.23f*up;hy+=.57f*up;
        }else if(id==AvatarCatalog.ASTRONAUT){
            float air=Math.max(up,down);kx+=.16f*air;ky-=.15f*air;fx+=.13f*air;fy-=.17f*air;
            hx+=.35f*up+.02f*down;hy+=.35f*up+.12f*down;
            h-=.035f*air;
        }else if(id==AvatarCatalog.ROBOT){
            kx+=.24f*up;ky-=.36f*up;fx+=.14f*up;fy-=.28f*up;
            hx-=.05f*up;hy+=.15f*up;p-=.10f*up;
        }else if(id==AvatarCatalog.SHADOW){
            p-=.06f*up;s-=.19f*up;h-=.19f*up;width-=.075f*up;
            kx-=.13f*up;ky-=.10f*up;fx-=.10f*up;fy-=.08f*up;
            hx-=.19f*up;hy+=.05f*up;
        }
        hx=mix(hx,.67f,q);hy=mix(hy,.37f,q);
        return new AvatarPose(p,s,h,kx,ky,fx,fy,hx,hy,wings,lift,width,up,down,q);
    }
    public static String styleName(int avatar){
        String[] names={"Natural jump","Wing spread","Graceful flare","Tucked knees","Float pose","Spring jump","Shadow stretch"};
        return names[Math.max(0,Math.min(names.length-1,avatar))];
    }
    private static float clamp(float x){return Math.max(0,Math.min(1,x));}
    private static float smooth(float x){return x*x*(3-2*x);}
    private static float mix(float a,float b,float t){return a+(b-a)*t;}
}
