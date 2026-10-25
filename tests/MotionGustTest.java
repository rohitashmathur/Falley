package com.perfgames.ropelab;

/** Intentional gust acceptance: calm sensor startup, visible alternating challenge and no bias. */
public final class MotionGustTest {
    private static int checks;
    private static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    private static void near(float a,float b,float tolerance,String message){check(Math.abs(a-b)<=tolerance,message+": "+a+" / "+b);}
    private static void startupModesAndLanding(){
        for(int stage=0;stage<StageCatalog.COUNT;stage++){
            Physics p=new Physics();p.reset(stage);p.touchControlled(false);
            for(float time:new float[]{0,1,2,2.5f}){p.elapsed=time;near(p.motionGustVelocity(),0,.000001f,"startup grace leaves a comfortable neutral hold");}
            float prior=0;boolean left=false,right=false;
            for(int i=0;i<=240;i++){
                p.elapsed=2.5f+i*.05f;float gust=p.motionGustVelocity();
                check(Float.isFinite(gust)&&Math.abs(gust)<=10.00001f&&Math.abs(gust-prior)<.6f,"opening gust eases in, is finite and bounded");
                left|=gust<-1;right|=gust>1;prior=gust;
            }
            check(left&&right,"each course has both gust directions, never a fixed pull");
            p.elapsed=8;p.y=p.groundY()-350;near(p.motionGustVelocity(),0,.000001f,"last 350 pixels leave the final landing calm");
            p.brokenCount=p.totalRopes();near(p.nudgeVelocity(),0,.000001f,"the existing late motion nudge also fades at landing");
            p.y=p.groundY()-700;float middle=p.nudgeVelocity();p.y=p.groundY()-1000;check(Math.abs(middle)<=Math.abs(p.nudgeVelocity()),"landing fade is gradual");
            p.brokenCount=0;p.elapsed=8;p.touchControlled(true);near(p.motionGustVelocity(),0,.000001f,"touch input receives no extra motion gust");
            p.resetDaily(stage);p.touchControlled(false);p.elapsed=8;near(p.motionGustVelocity(),0,.000001f,"Daily keeps its published motion course family");
        }
        Physics p=new Physics();p.resetArcade(0,ArcadeRules.ALL);p.touchControlled(false);p.elapsed=8;p.y=900000;
        check(Math.abs(p.motionGustVelocity())>1,"endless Arcade never inherits a finite-floor fade");
        p.lost=true;near(p.motionGustVelocity(),0,0,"terminal runs do not show an active gust cue");
    }
    private static void cycleBiasAndCombinedCap(){
        double period=2*Math.PI/.9;
        for(int stage=0;stage<StageCatalog.COUNT;stage++)for(boolean late:new boolean[]{false,true}){
            Physics p=new Physics();p.reset(stage);p.touchControlled(false);p.y=1000;if(late)p.brokenCount=p.totalRopes();
            double sum=0,position=0,min=0,max=0;float peak=0;int count=72000;
            for(int i=0;i<count;i++){
                p.elapsed=(float)(10+i*(period*100/count));float velocity=p.nudgeVelocity()+p.motionGustVelocity();
                check(Math.abs(velocity)<=18.0001f,"opening gust gives way to late nudge rather than stacking beyond its cap");
                sum+=velocity;position+=velocity*(period*100/count);min=Math.min(min,position);max=Math.max(max,position);peak=Math.max(peak,Math.abs(velocity));
            }
            check(Math.abs(sum/count)<.001&&Math.abs(position)<.05,"100 complete gust cycles retain no directional bias");
            check(max-min<=41&&peak>9,"neutral environmental excursion stays bounded over a long run");
        }
    }
    private static void sensorResponseIndependent(){
        for(int direction:new int[]{-1,1}){
            Physics a=new Physics(),b=new Physics();a.resetDaily(0);b.resetDaily(0);a.touchControlled(false);b.touchControlled(false);
            // Directly compare the exact existing servo response in the unchanged Daily family.
            a.gx=direction*500;b.gx=0;
            for(int i=0;i<96;i++){
                for(Physics p:new Physics[]{a,b}){p.y=1800;p.vy=0;p.cameraY=1370;for(Physics.Rope r:p.ropes){r.base=100000;r.cut=12;}p.step();}
            }
            near(a.vx-b.vx,direction*500/950f*Physics.MAX_MOTION_SPEED*(1-(float)Math.exp(-10*96*Physics.DT)),.02f,"sensor command response remains unchanged");
        }
    }
    public static void main(String[]args){startupModesAndLanding();cycleBiasAndCombinedCap();sensorResponseIndependent();System.out.println("PASS: motion-only gust grace/ease/landing fade, combined cap, 100-cycle bias bounds and preserved sensor servo ("+checks+" checks)");}
}
