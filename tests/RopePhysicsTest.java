package com.perfgames.ropelab;

/** Supporting-rope abilities are real, bounded and isolated from comparable Daily/Arcade runs. */
public final class RopePhysicsTest {
    private static int checks;
    private static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    private static void near(float actual,float expected,float tolerance,String message){check(Math.abs(actual-expected)<=tolerance,message+" actual="+actual+" expected="+expected);}
    private static Physics fixture(int type){
        Physics p=new Physics();p.configureRope(type);p.reset(0);p.touchControlled(true);p.y=2400;p.cameraY=1970;
        try{java.lang.reflect.Field f=Physics.class.getDeclaredField("attackCountdown");f.setAccessible(true);f.setFloat(p,100000);}catch(ReflectiveOperationException e){throw new AssertionError(e);}
        for(Physics.Rope rope:p.ropes){rope.cut=12;rope.base=100000;}
        Physics.Rope rope=p.ropes[0];rope.reset(2400,p.leftWall(),p.rightWall());p.x=240;p.y=rope.y[Physics.Rope.N/2]-p.radius()-5;p.vy=80;
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
        Physics p=new Physics();p.configureRope(1);check(p.effectiveRope()==0,"equipment applies on next campaign start, not midrun");p.reset(0);check(p.effectiveRope()==1,"campaign installs configured rope");
        p.resetDaily(123);check(p.effectiveRope()==0&&!p.staticJumpReady&&p.ropeAbilityTime==0,"Daily forces Classic and clears ability state");p.reset();check(p.effectiveRope()==0,"Daily retry remains Classic");
        p.resetArcade(1,ArcadeRules.ALL);check(p.effectiveRope()==0,"Arcade forces Classic");p.reset(0);check(p.effectiveRope()==1,"temporary comparable-mode override preserves Campaign equipment");
        p.configureRope(2);p.reset(1);p.staticJumpReady=true;p.ropeAbilityTime=.5f;p.reset();check(p.effectiveRope()==2&&!p.staticJumpReady&&p.ropeAbilityTime==0,"restart resets ability state, retaining selected equipment");
        p.configureRope(55);p.reset(0);check(p.effectiveRope()==0,"invalid equipment cannot leak an unknown physical effect");
        for(int type=0;type<3;type++){
            p=new Physics();p.configureRope(type);p.reset(0);check(p.jump(),"fresh manual jump available for every rope");
            float initial=p.vy;for(int i=0;i<150;i++)p.step();check(!p.lost,"fresh manual jump never gains a rope-only bonus or unsafe ceiling death");near(initial,-450,.001f,"all equipped ropes use the same initial manual jump");
        }
        p=fixture(1);support(p);for(Physics.Rope r:p.ropes)r.cut=12;float minimum=p.headTop();for(int i=0;i<240&&!p.lost;i++){neutral(p);minimum=Math.min(minimum,p.headTop());}
        check(!p.lost&&minimum>Physics.ARENA_TOP,"automatic Spring support bounce retains ceiling clearance");
        for(int mode=0;mode<2;mode++){
            Physics a=new Physics(),b=new Physics();a.configureRope(1);b.configureRope(2);if(mode==0){a.resetDaily(123);b.resetDaily(123);}else{a.resetArcade(0,ArcadeRules.TOP_LASERS);b.resetArcade(0,ArcadeRules.TOP_LASERS);}
            a.touchControlled(true);b.touchControlled(true);for(int i=0;i<240*8&&!a.lost&&!a.won;i++){CampaignExpansionTest.steer(a);CampaignExpansionTest.steer(b);a.step();b.step();check(Float.floatToIntBits(a.x)==Float.floatToIntBits(b.x)&&Float.floatToIntBits(a.y)==Float.floatToIntBits(b.y)&&a.score==b.score,"equipment cannot alter comparable-mode trajectory or records");}
        }
    }
    private static void earnedCourseClearance(){
        for(int type=1;type<3;type++)for(int stage=0;stage<24;stage++){
            Physics p=new Physics();p.configureRope(type);p.reset(stage);p.touchControlled(true);int steps=0;
            while(!p.lost&&!p.won&&steps++<240*120){CampaignExpansionTest.steer(p);p.step();check(Float.isFinite(p.x)&&Float.isFinite(p.y)&&Float.isFinite(p.vx)&&Float.isFinite(p.vy),"finite earned-rope course");}
            check(p.won&&!p.lost,"equipped rope keeps complete course escapable: type="+type+" stage="+stage+" reason="+p.deathReason+" x="+p.x+" y="+p.y);
        }
    }
    public static void main(String[]args){catalog();effects();modesAndSafety();earnedCourseClearance();System.out.println("PASS: earned rope physical effects, safety, reset/mode isolation and all-course clearance ("+checks+" checks)");}
}