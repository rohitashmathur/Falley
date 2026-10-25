package com.perfgames.ropelab;

/** Visible style differences and contact/finite invariants, independent of Android rendering. */
public final class AvatarPoseTest {
    private static int checks;
    private static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    private static float[] points(AvatarPose p){return new float[]{p.pelvis,p.shoulder,p.head,p.kneeX,p.kneeY,p.footX,p.footY,p.handX,p.handY,p.wingSpan,p.wingLift,p.torsoWidth,p.rise,p.fall,p.landing};}
    public static void main(String[]args){
        AvatarPose human=AvatarPose.sample(0,-350,0);
        for(int id=0;id<7;id++){
            AvatarPose idle=AvatarPose.sample(id,0,0),jump=AvatarPose.sample(id,-350,0),land=AvatarPose.sample(id,400,.18f);
            check(idle.footY==1&&land.footY==1,"all grounded and landing feet share contact line");
            check(land.rise==0&&land.fall==0&&land.landing==1,"landing compression overrides airborne style");
            check(!AvatarPose.styleName(id).isEmpty(),"each approved avatar has a named style");
            if(id>0){float difference=0;float[] a=points(jump),b=points(human);for(int k=0;k<a.length;k++)difference+=Math.abs(a[k]-b[k]);check(difference>.1,"each reward has a visible pose distinct from Human");}
            for(float velocity:new float[]{Float.NaN,Float.NEGATIVE_INFINITY,Float.POSITIVE_INFINITY,-10000,-350,-30,0,45,400,10000})
                for(float pulse:new float[]{Float.NaN,Float.POSITIVE_INFINITY,-1,0,.09f,.18f,2})
                    for(float coordinate:points(AvatarPose.sample(id,velocity,pulse)))check(Float.isFinite(coordinate)&&Math.abs(coordinate)<2,"pose is finite and bounded even for invalid inputs");
            for(int velocity=-500;velocity<500;velocity++){
                float[] a=points(AvatarPose.sample(id,velocity,0)),b=points(AvatarPose.sample(id,velocity+1,0));
                for(int k=0;k<a.length;k++)check(Math.abs(a[k]-b[k])<.025f,"pose changes continuously with airborne velocity");
            }
        }
        check(AvatarPose.sample(1,-350,0).wingSpan-AvatarPose.sample(1,0,0).wingSpan>.6,"Demon spreads wings during jump");
        check(AvatarPose.sample(2,-350,0).wingLift<-.95,"Angel gracefully lifts feather fan");
        check(AvatarPose.sample(3,-350,0).footY<.5,"Military tucks feet in air");
        check(AvatarPose.sample(4,-350,0).handX>.95,"Astronaut extends float pose");
        check(AvatarPose.sample(5,-350,0).kneeX>human.kneeX+.2,"Robot visibly folds mechanical spring");
        check(AvatarPose.sample(6,-350,0).torsoWidth<human.torsoWidth-.05,"Shadow narrows and stretches in rise");
        check(AvatarPose.sample(-1,0,0).handX==AvatarPose.sample(0,0,0).handX,"negative avatar clamps to Human");
        System.out.println("PASS: seven distinct avatar styles, safe landing contact and smooth bounded poses ("+checks+" checks)");
    }
}
