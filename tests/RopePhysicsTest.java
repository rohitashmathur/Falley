package com.perfgames.ropelab;

/** Individual supporting-rope abilities are real, bounded and mixed safely into complete courses. */
public final class RopePhysicsTest {
    private static int checks;
    private static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    private static void near(float actual,float expected,float tolerance,String message){check(Math.abs(actual-expected)<=tolerance,message+" actual="+actual+" expected="+expected);}
    private static Physics fixture(int type){
        Physics p=new Physics();p.reset(0);p.touchControlled(true);p.y=2400;p.cameraY=1970;
        try{java.lang.reflect.Field f=Physics.class.getDeclaredField("attackCountdown");f.setAccessible(true);f.setFloat(p,100000);}catch(ReflectiveOperationException e){throw new AssertionError(e);}
        for(Physics.Rope rope:p.ropes){rope.cut=12;rope.base=100000;}
        Physics.Rope rope=p.ropes[0];rope.reset(2400,p.leftWall(),p.rightWall());rope.type=type;p.x=240;p.y=rope.y[Physics.Rope.N/2]-p.radius()-5;p.vy=80;
        return p;
    }
    private static void neutral(Physics p){p.steerTouch((-p.driftVelocity()-p.nudgeVelocity()-p.windVelocity())/p.steeringScale()/p.ropeSteeringScale());p.step();}
    private static void support(Physics p){int n=0;while(p.landingPulse<=0&&!p.lost&&n++<120)neutral(p);check(p.landingPulse>0&&p.ropes[0].cut<0&&p.vy<0,"fixture catches intact supporting rope");}
    private static void catalog(){
        check(RopeCatalog.COUNT==3&&RopeCatalog.CLASSIC==0&&RopeCatalog.SPRING==1&&RopeCatalog.STATIC==2,"stable earned rope identities");
        for(int i=0;i<3;i++){check(!RopeCatalog.name(i).isEmpty()&&!RopeCatalog.description(i).isEmpty()&&(RopeCatalog.color(i)>>>24)==255,"named opaque rope preview");check(RopeCatalog.requiredStars(i)==i*6,"earned rope star thresholds");}
        check(RopeCatalog.normalize(-1)==0&&RopeCatalog.normalize(3)==0&&RopeCatalog.requiredStars(999)==0,"invalid rope safely normalizes to Classic");
    }
    private static void effects(){
        Physics classic=fixture(0),spring=fixture(1),statik=fixture(2);support(classic);support(spring);support(statik);
        near(-spring.vy/-classic.vy,1.5f,.001f,"Spring supporting rebound is one and a half times Classic");
        near(statik.vy,classic.vy,.001f,"Static support rebound stays normal");
        check(spring.ropeAbilityTime>0&&!spring.staticJumpReady&&statik.staticJumpReady&&statik.ropeAbilityTime>0,"support arms only the equipped ability");
        check(classic.jump()&&spring.jump()&&statik.jump(),"supported jumps remain available");
        near(spring.vy,classic.vy,.001f,"Spring never magnifies a manual jump");near(statik.vy/classic.vy,.8f,.001f,"Static reduces exactly the next manual jump to eighty percent");
        check(!statik.staticJumpReady,"Static next-jump charge consumed once");
        statik.y=2400;statik.cameraY=1970;statik.vy=0;for(Physics.Rope r:statik.ropes)r.cut=12;
        for(int i=0;i<80;i++){statik.y=2400;statik.cameraY=1970;statik.vy=0;neutral(statik);}
        check(statik.jump(),"second jump becomes available after cooldown");near(statik.vy,-Math.max(350,450/(float)Math.sqrt(statik.mass())),.001f,"subsequent manual jumps return to normal");
        Physics damping=fixture(2);support(damping);float environmental=damping.driftVelocity()+damping.nudgeVelocity()+damping.windVelocity();damping.steerTouch(120);
        near(damping.vx-environmental,120*damping.steeringScale()*.84f,.001f,"Static applies mild symmetric horizontal command damping");
        damping.steerTouch(-120);near(damping.vx-environmental,-120*damping.steeringScale()*.84f,.001f,"Static damping has no right or left bias");
        for(Physics.Rope r:damping.ropes)r.cut=12;
        for(int i=0;i<200;i++){damping.y=2400;damping.cameraY=1970;damping.vy=0;neutral(damping);}
        check(damping.ropeAbilityTime==0&&damping.ropeSteeringScale()==1&&damping.staticJumpReady,"brief damping expires while the unused next-jump charge remains");
        Physics cut=fixture(2);cut.score=300;cut.brokenCount=10;for(int i=0;i<120&&cut.ropes[0].cut<0;i++)neutral(cut);
        check(cut.ropes[0].cut>=0&&!cut.staticJumpReady&&cut.ropeAbilityTime==0,"cutting without support grants no ability");
    }
    private static void modesAndSafety(){
        Physics p=fixture(2);support(p);check(p.lastSupportRope==2&&p.staticJumpReady,"support feedback records the individual Static rope");
        p.reset();check(p.lastSupportRope==0&&!p.staticJumpReady&&p.ropeAbilityTime==0&&p.ropeSteeringScale()==1,"restart clears feedback, charge and damping");
        p.resetDaily(123);check(p.daily&&!p.arcade,"Daily remains independent");for(Physics.Rope rope:p.ropes)check(rope.type==0,"Daily initial ropes remain Classic");
        p.resetArcade(1,ArcadeRules.ALL);for(Physics.Rope rope:p.ropes)check(rope.type==0,"Arcade Wave 1 starts with readable Classic ropes");
        for(int type=0;type<3;type++){
            p=fixture(type);p.y=310;p.cameraY=0;for(Physics.Rope rope:p.ropes)rope.cut=12;
            check(p.jump(),"fresh manual jump available regardless of course rope type");float initial=p.vy;
            for(int i=0;i<150;i++)p.step();check(!p.lost,"fresh manual jump never gains an unsafe rope bonus");near(initial,-450,.001f,"all initial manual jumps remain identical");
        }
        p=fixture(1);support(p);for(Physics.Rope r:p.ropes)r.cut=12;float minimum=p.headTop();for(int i=0;i<240&&!p.lost;i++){neutral(p);minimum=Math.min(minimum,p.headTop());}
        check(!p.lost&&minimum>Physics.ARENA_TOP,"automatic Spring support bounce retains ceiling clearance");
        p=fixture(2);support(p);p.ropes[0].reset(2400,p.leftWall(),p.rightWall());p.ropes[0].type=0;
        p.y=p.ropes[0].y[Physics.Rope.N/2]-p.radius()-5;p.cameraY=1970;p.vy=80;p.landingPulse=0;support(p);
        check(p.lastSupportRope==0&&p.staticJumpReady&&p.ropeSteeringScale()==.84f,"a real later Classic support preserves the pending Static charge and brief current");
        p.jump();check(!p.staticJumpReady,"next-jump charge is consumed exactly once, independently of last support");
        near(p.vy,-Math.max(350,450/(float)Math.sqrt(p.mass()))*.8f,.001f,"Classic support does not discard the promised next smaller Static jump");
    }
    private static void mixedCourseClearance(){
        for(boolean touch:new boolean[]{true,false})for(int stage=0;stage<StageCatalog.COUNT;stage++){
            Physics p=new Physics();p.reset(stage);p.touchControlled(touch);int steps=0;boolean spring=false,statik=false;
            while(!p.lost&&!p.won&&steps++<240*120){
                for(Physics.Rope r:p.ropes){spring|=r.type==RopeCatalog.SPRING;statik|=r.type==RopeCatalog.STATIC;}
                if(touch)CampaignExpansionTest.steer(p);else CampaignExpansionTest.steerMotion(p);p.step();
                check(Float.isFinite(p.x)&&Float.isFinite(p.y)&&Float.isFinite(p.vx)&&Float.isFinite(p.vy),"finite real mixed-rope course");
            }
            check(p.won&&!p.lost,"mixed course clear: touch="+touch+" stage="+stage+" reason="+p.deathReason+" x="+p.x+" y="+p.y);
            check(spring&&(stage<2||statik),"whole actual course contains the introduced visible rope types");
            System.out.printf("Mixed %s stage %d PASS %.2fs%n",touch?"touch":"motion",stage+1,p.elapsed);
        }
    }
    public static void main(String[]args){catalog();effects();modesAndSafety();mixedCourseClearance();System.out.println("PASS: mixed rope physical effects, safety, reset/Daily isolation and all-course touch/motion clearance ("+checks+" checks)");}
}