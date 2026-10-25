package com.perfgames.ropelab;

/** Acceptance for the small progression penalty without weakening neutral braking. */
public final class SteeringProgressionTest {
    private static int checks;
    private static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    private static void near(float a,float b,float tolerance,String message){check(Math.abs(a-b)<=tolerance,message+": "+a+" versus "+b);}
    private static Physics chapter(int index){Physics p=new Physics();p.reset(index);return p;}
    private static void totalsAndStrictThresholds() {
        int[] expected={21,23,25,29,29,31,34,36,35,37,39,43,41,43,45,49,47,49,51,55,52,55,58,63};
        for(int stage=0;stage<StageCatalog.COUNT;stage++) {
            Physics p=chapter(stage);
            check(p.totalRopes()==expected[stage],"total mounts match the finite chapter schedule, including the strict final floor clearance");
            int threshold=(int)Math.floor(p.totalRopes()*.3);
            for(int cuts=0;cuts<=threshold;cuts++) {
                p.brokenCount=cuts;
                near(p.steeringScale(),1,.000001f,"at or below30percent of all chapter ropes steering is untouched");
            }
            p.score=10000;p.elapsed=100;
            near(p.steeringScale(),1,.000001f,"chapter score, mass and elapsed time cannot replace the cut threshold");
            p.brokenCount=threshold+1;
            check(p.steeringScale()<1 && p.steeringScale()>.97f,"the first cut strictly beyond30percent starts a small gradual change");
            check(Math.abs(p.nudgeVelocity())<.2f,"the first threshold crossing cannot introduce a sudden sideways shove");
            float previous=1;
            for(int cuts=0;cuts<=p.totalRopes()+3;cuts++) {
                p.brokenCount=cuts;float scale=p.steeringScale();
                check(Float.isFinite(scale) && scale>=.84999f && scale<=1 && scale<=previous+.000001f,
                        "progression stays finite, monotonic and bounded to at most15percent reduction");
                check(previous-scale<=.030001f,"one additional rope cannot create a sudden large steering loss");
                previous=scale;
            }
            p.brokenCount=p.totalRopes();near(p.steeringScale(),.88f,.00001f,"a fully cut chapter reaches the modest12percent cap");
            p.brokenCount=p.totalRopes()+100;near(p.steeringScale(),.88f,.00001f,"even an oversized cut count cannot exceed the penalty cap");
        }
    }
    private static void boundedAlternatingNudge() {
        for(int stage=0;stage<StageCatalog.COUNT;stage++) {
            Physics p=chapter(stage);p.brokenCount=(int)Math.floor(p.totalRopes()*.3);p.elapsed=100;
            near(p.nudgeVelocity(),0,.000001f,"no nudge is introduced before the strict cut threshold");
            p.brokenCount=p.totalRopes();boolean positive=false,negative=false;
            for(int i=0;i<80;i++) {
                p.elapsed=i*.25f;float nudge=p.nudgeVelocity();
                check(Float.isFinite(nudge) && Math.abs(nudge)<=18.00001f,"progression nudge remains bounded to18pixels/sec");
                positive|=nudge>1;negative|=nudge<-1;
            }
            check(positive && negative,"late chapter nudge alternates direction instead of becoming a permanent left/right pull");
        }
    }
    private static void gentleAlternatingWind() {
        for(int stage=0;stage<StageCatalog.COUNT;stage++) {
            Physics p=chapter(stage);boolean positive=false,negative=false;float peak=0,sum=0;
            for(int i=0;i<1200;i++) {
                p.elapsed=i*.1f;float wind=p.driftVelocity();
                check(Float.isFinite(wind) && Math.abs(wind)<=35.0001f,"reduced wind stays finite and below35pixels/sec");
                positive|=wind>4;negative|=wind<-4;peak=Math.max(peak,Math.abs(wind));sum+=wind;
            }
            check(positive && negative && peak>20 && Math.abs(sum/1200)<2,"wind remains a gentle centered alternating influence rather than zero or a permanent pull");
        }
    }
    private static Physics horizontal(int stage,boolean late,boolean touch) {
        Physics p=chapter(stage);p.touchControlled(touch);if(late)p.brokenCount=p.totalRopes();
        return isolatedArena(p);
    }
    private static Physics isolatedArena(Physics p) {
        p.y=2000;p.cameraY=1570;
        // Isolate sideways control from level termination and recycling; real courses are tested below.
        for(Physics.Rope r:p.ropes){r.cut=12;r.base=1_000_000;for(int i=0;i<Physics.Rope.N;i++)r.y[i]=r.py[i]=r.oldY[i]=r.base;}
        return p;
    }
    private static void levelStep(Physics p){p.y=2000;p.vy=0;p.cameraY=1570;p.step();}
    private static void commandsAndNeutral() {
        for(boolean late:new boolean[]{false,true}) {
            float[] touch=new float[2],motion=new float[2],nudge=new float[2];int index=0;
            for(int direction:new int[]{-1,1}) {
                Physics p=horizontal(0,late,true);p.steerTouch(direction*100);
                touch[index]=p.vx-p.driftVelocity()-p.nudgeVelocity();
                near(touch[index],direction*100*p.steeringScale(),.0001f,"touch command is scaled without multiplying the environmental current");
                p.steerTouch(0);near(p.vx,p.driftVelocity()+p.nudgeVelocity(),.0001f,"release preserves exactly the intended touch-only wind");
                p=horizontal(0,late,false);p.gx=direction*500;
                for(int i=0;i<96;i++)levelStep(p);
                motion[index]=p.vx;nudge[index]=p.nudgeVelocity();
                check(!p.lost && p.vx*direction>40 && Math.abs(p.vx)<=Physics.MAX_MOTION_SPEED,
                        "late progression keeps both signed motion commands useful and bounded");
                float stopX=p.x;p.gx=0;for(int i=0;i<120;i++)levelStep(p);
                check(Math.abs(p.vx-p.nudgeVelocity()-p.motionGustVelocity())<1.5f && Math.abs(p.x-stopX)<15,"neutral braking stays prompt after both early and penalized turns");
                index++;
            }
            near(touch[0]+touch[1],0,.0001f,"progression preserves mirrored touch steering");
            float expectedCommand=500/950f*Physics.MAX_MOTION_SPEED*(late?.88f:1f)*(1-(float)Math.exp(-10*96*Physics.DT));
            near((motion[1]-motion[0])/2,expectedCommand,.02f,"paired opposite commands cancel the moving nudge and retain the intended signed servo response");
            near((motion[0]+motion[1])/2,(nudge[0]+nudge[1])/2,1.5f,"the symmetric response midpoint follows only the small nudge with normal servo lag");
        }
        Physics early=horizontal(0,true,false),late=horizontal(0,true,false);
        early.vx=0;late.vx=120;early.gx=late.gx=0;
        for(int i=0;i<120;i++){levelStep(early);levelStep(late);near(late.vx-early.vx,120*(float)Math.exp(-14*(i+1)*Physics.DT),.02f,"neutral braking removes old momentum at full strength while following the same small nudge");}
    }
    private static float beamClearance(float x,float y,Physics.BeamHazard b) {
        if(b.kind==ArcadeRules.SIDE_LASERS)return Math.abs(x-Physics.clamp(x,Math.min(b.x1,b.x2),Math.max(b.x1,b.x2)))-b.width/2;
        return (float)Math.sqrt(Physics.pointSegmentDistanceSquared(x,y,b.x1,b.y1,b.x2,b.y2))-b.width/2;
    }
    private static float clearance(Physics p,float x) {
        float clear=10000;
        for(Physics.FireHazard f:p.fireHazards)if(f.active && (f.warningTime>0 || f.y<p.y+90))clear=Math.min(clear,Math.abs(x-f.x)-f.radius);
        for(Physics.BeamHazard b:p.beamHazards)if(b.active)clear=Math.min(clear,beamClearance(x,p.y,b));
        for(Physics.SawHazard s:p.sawHazards)if(s.active)clear=Math.min(clear,
                Math.abs(x-Physics.clamp(x,Math.min(s.warningTime>0?s.pathX1:s.x,s.pathX2),Math.max(s.warningTime>0?s.pathX1:s.x,s.pathX2)))-s.radius);
        return clear;
    }
    private static void steerCourse(Physics p) {
        float low=p.leftWall()+p.hazardInset()+p.radius()+8,high=p.rightWall()-p.hazardInset()-p.radius()-8;
        if(p.y>p.groundY()-350){low=Math.max(low,p.targetLeft()+p.radius()+3);high=Math.min(high,p.targetRight()-p.radius()-3);}
        float center=Physics.clamp(p.bossActive?p.bossSafeX:(p.targetLeft()+p.targetRight())/2,low,high),best=center,bestClear=-1;
        for(float candidate:new float[]{low,center,high}) {
            float clear=clearance(p,candidate);
            if(clear>bestClear || clear==bestClear && candidate==center){bestClear=clear;best=candidate;}
        }
        if(clearance(p,center)>p.radius()+9)best=center;
        // A bounded player-style correction; no artificial velocity or position changes.
        p.steerTouch(Physics.clamp(((best-p.x)*10-p.driftVelocity()-p.nudgeVelocity()-p.windVelocity())/p.steeringScale(),-300,300));
    }
    private static void wholeCourses() {
        for(int index=0;index<StageCatalog.COUNT;index++) {
            Physics p=chapter(index);p.touchControlled(true);boolean reduced=false;
            for(int step=0;step<240*100 && !p.lost && !p.won;step++) {
                steerCourse(p);p.step();
                if(p.steeringScale()<.99999f)reduced=true;
            }
            check(p.won && !p.lost && reduced,"actual chapter "+(index+1)+" remains reachable after the late-cut steering change; reason="+p.deathReason);
            check(p.brokenCount==p.totalRopes(),"winning natural descent cuts every counted mount rather than counting the reusable pool");
            System.out.printf("Progression chapter %d PASS: %d/%d cuts, scale %.3f, %.2fs%n",index+1,p.brokenCount,p.totalRopes(),p.steeringScale(),p.elapsed);
        }
    }
    private static void arcadeTimeRamp() {
        Physics p=new Physics();p.resetArcade(1,ArcadeRules.ALL);
        check(p.totalRopes()==0,"endless Arcade must not expose a finite reusable-pool denominator");
        for(float time:new float[]{0,10,19.99f,20}) {
            p.elapsed=time;p.wave=1+(int)(time/20);
            near(p.steeringScale(),1,.000001f,"Arcade Wave1 and the exact Wave2 boundary remain unchanged");
        }
        p.elapsed=20.25f;p.wave=2;
        check(p.steeringScale()<1 && p.steeringScale()>.999f,"Wave2 starts with a gentle continuous reduction rather than a discrete penalty");
        p.elapsed=30;near(p.steeringScale(),.94f,.00001f,"halfway through Wave2 the penalty is only6percent");
        float previous=1;
        for(int i=0;i<=80;i++) {
            p.elapsed=20+i*.5f;p.wave=1+(int)(p.elapsed/20);float scale=p.steeringScale();
            check(scale>=.87999f && scale<=previous+.000001f && previous-scale<.005f,
                    "Arcade ramp is smooth, monotonic and capped without abrupt wave jumps");
            previous=scale;
        }
        p.elapsed=40;p.wave=3;near(p.steeringScale(),.88f,.00001f,"full gentle Arcade penalty is reached at Wave3");
        p.elapsed=200;p.wave=11;near(p.steeringScale(),.88f,.00001f,"late waves cannot keep weakening steering");
        p.elapsed=30;p.brokenCount=0;float timeScale=p.steeringScale();p.brokenCount=999;
        near(p.steeringScale(),timeScale,.000001f,"Arcade progression uses elapsed waves rather than the chapter cut count");
        p.reset();near(p.steeringScale(),1,.000001f,"Arcade restart restores the unpenalized opening");
    }
    private static void arcadeCommands() {
        for(float time:new float[]{10,30,40})for(int direction:new int[]{-1,1}) {
            Physics p=new Physics();p.resetArcade(0,ArcadeRules.FIREBALLS);p.elapsed=time;p.wave=1+(int)(time/20);
            p.touchControlled(true);isolatedArena(p);p.steerTouch(direction*150);
            near(p.vx-p.driftVelocity()-p.nudgeVelocity(),direction*150*p.steeringScale(),.0001f,
                    "Arcade touch input follows the opening/mid-ramp/capped scale without changing wind");
            levelStep(p);near(p.vx-p.driftVelocity()-p.nudgeVelocity(),direction*150*p.steeringScale(),.0001f,
                    "Arcade per-step touch response agrees with the immediate gesture response");
            p.steerTouch(0);near(p.vx,p.driftVelocity()+p.nudgeVelocity(),.0001f,"Arcade release leaves wind unchanged at every ramp phase");
            p=new Physics();p.resetArcade(0,ArcadeRules.FIREBALLS);p.elapsed=time;p.wave=1+(int)(time/20);
            p.touchControlled(false);isolatedArena(p);p.gx=direction*500;
            for(int i=0;i<96;i++)levelStep(p);
            near(p.vx,direction*500/950f*Physics.MAX_MOTION_SPEED*p.steeringScale()+p.nudgeVelocity()+p.motionGustVelocity(),4,
                    "Arcade signed motion response follows the same gentle time ramp");
            // Compare with the same live environmental forces from an already-neutral
            // body. Raw travel now intentionally includes the stronger 4.3 breeze;
            // only old steering momentum should disappear within half a second.
            Physics environment=new Physics();environment.resetArcade(0,ArcadeRules.FIREBALLS);
            environment.elapsed=p.elapsed;environment.wave=p.wave;environment.score=p.score;
            environment.brokenCount=p.brokenCount;environment.touchControlled(false);isolatedArena(environment);environment.x=p.x;
            float releaseSpeed=p.vx;p.gx=0;
            for(int i=0;i<120;i++){levelStep(p);levelStep(environment);}
            check(!p.lost && !environment.lost && Math.abs(p.vx-environment.vx)<1 && Math.abs(p.x-environment.x)<12,
                    "Arcade neutral braking removes old turn momentum within half a second while preserving the same intentional breeze");
            near(p.vx-environment.vx,releaseSpeed*(float)Math.exp(-14*120*Physics.DT),.002f,
                    "neutral servo retains its original braking response independently of stronger environmental drift");
            check(Math.abs(p.vx-p.nudgeVelocity()-p.motionGustVelocity())<1.5f,
                    "the neutral body follows the current breeze without persistent steering momentum");
        }
    }
    private static void resetAndModes() {
        Physics p=chapter(5);p.brokenCount=p.totalRopes();check(p.steeringScale()<1,"reset fixture starts with a real chapter penalty");
        p.reset();near(p.steeringScale(),1,.000001f,"chapter restart clears progression penalty");
        p.resetArcade(1,ArcadeRules.ALL);p.brokenCount=999;near(p.steeringScale(),1,.000001f,"old chapter cuts cannot penalize a new Arcade opening");
        p.reset(2);near(p.steeringScale(),1,.000001f,"switching back to a chapter clears Arcade progression");
    }
    private static void liveArcadeRamp() {
        StringBuilder failures=new StringBuilder();
        try {
            java.lang.reflect.Method avoid=ArcadePhysicsTest.class.getDeclaredMethod("avoid",Physics.class);
            avoid.setAccessible(true);
            for(int difficulty=0;difficulty<3;difficulty++) {
                Physics p=new Physics();p.resetArcade(difficulty,ArcadeRules.ALL);p.touchControlled(true);
                int steps=0,lateLive=0,lateWarnings=0;
                while(!p.lost && p.elapsed<65 && steps++<240*66) {
                    avoid.invoke(null,p);p.step();
                    if(p.elapsed>=20) {
                        for(Physics.FireHazard f:p.fireHazards)if(f.active){if(f.warningTime>0)lateWarnings++;else lateLive++;}
                        for(Physics.BeamHazard b:p.beamHazards)if(b.active){if(b.warningTime>0)lateWarnings++;else lateLive++;}
                        for(Physics.SawHazard s:p.sawHazards)if(s.active){if(s.warningTime>0)lateWarnings++;else lateLive++;}
                    }
                }
                System.out.printf("Live Arcade %s: %.2fs, wave%d, scale%.3f, late live/warning steps%d/%d, reason=%s%n",
                        ArcadeRules.DIFFICULTIES[difficulty],p.elapsed,p.wave,p.steeringScale(),lateLive,lateWarnings,p.deathReason);
                if(p.lost || p.elapsed<64.9f || p.wave<4 || p.steeringScale()>.88001f || lateLive==0 || lateWarnings==0)
                    failures.append(ArcadeRules.DIFFICULTIES[difficulty]).append(" t=").append(p.elapsed)
                            .append(" scale=").append(p.steeringScale()).append(" reason=").append(p.deathReason).append("; ");
            }
        } catch(ReflectiveOperationException e) {throw new AssertionError("shared bounded Arcade controller unavailable",e);}
        check(failures.length()==0,"live bounded control survives real hazards through Wave2 ramp and into capped Wave3: "+failures);
    }
    public static void main(String[] args) {
        totalsAndStrictThresholds();boundedAlternatingNudge();gentleAlternatingWind();arcadeTimeRamp();commandsAndNeutral();arcadeCommands();resetAndModes();wholeCourses();liveArcadeRamp();
        System.out.println("PASS: finite totals/strict30percent, gradual12percent cap and Wave2 ramp, mirrored touch/motion, unchanged wind/neutral braking, mode/reset isolation, twelve real late-cut chapter clears and live65second Arcade ("+checks+" checks)");
    }
}
