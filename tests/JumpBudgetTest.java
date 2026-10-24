package com.perfgames.ropelab;

/** Burst recovery requires real descent; campaign allowances cannot be refilled by landing. */
public final class JumpBudgetTest {
    private static int checks;
    private static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    private static Physics isolated(int stage,boolean arcade) {
        Physics p=new Physics();if(arcade){p.resetArcade(0,ArcadeRules.FIREBALLS);p.arcadeHazards=0;}else p.reset(stage);
        p.y=2000;p.cameraY=1570;p.touchControlled(true);
        // Isolate the jump budget from incoming attacks and supporting surfaces.
        try {
            java.lang.reflect.Field countdown=Physics.class.getDeclaredField("attackCountdown");
            countdown.setAccessible(true);countdown.setFloat(p,100000);
        } catch(ReflectiveOperationException e){throw new AssertionError("jump fixture cannot isolate incoming attacks",e);}
        for(Physics.Rope r:p.ropes){r.cut=12;r.base=100000;}
        for(Physics.WindZone z:p.windZones)z.active=false;
        for(Physics.BouncePad pad:p.bouncePads)pad.active=false;
        for(Physics.SlowPickup pickup:p.slowPickups)pickup.active=false;
        return p;
    }
    private static void step(Physics p){p.steerTouch((-p.driftVelocity()-p.nudgeVelocity())/p.steeringScale());p.step();}
    private static void steps(Physics p,int count){for(int i=0;i<count;i++)step(p);}
    private static void cooldownAndRecovery() {
        Physics p=isolated(0,false);
        check(p.jumpsRemaining()==2 && p.jumpsUsed()==0 && p.jumpLimit()==8 && p.jumpAllowanceRemaining()==8,"first chapter starts with two burst charges and a generous8-jump allowance");
        float anchor=p.y;check(p.jump() && p.jumpsRemaining()==1 && p.jumpsUsed()==1,"successful jump consumes one burst charge and one overall use");
        check(!p.jump() && p.jumpsUsed()==1,"cooldown rejection does not spend another jump");
        steps(p,73);check(p.jump() && p.jumpsRemaining()==0 && p.jumpsUsed()==2,"a second quick airborne jump is permitted");
        float elapsed=p.elapsed,progress=p.burstRecoveryProgress();
        for(int i=0;i<100;i++)check(!p.canJump() && p.elapsed==elapsed && p.burstRecoveryProgress()==progress,"paused/query-only time cannot recharge an exhausted burst");
        steps(p,73);check(!p.lost && !p.jump() && p.jumpsRemaining()==0,"cooldown expiry alone is not enough for a third jump");
        p.touchControlled(false);p.touchControlled(true);check(p.jumpsRemaining()==0 && p.jumpsUsed()==2,"control handoff cannot reset burst or overall usage");
        int count=0;while(!p.canJump() && !p.lost && count++<720){step(p);check(p.burstRecoveryProgress()>=0 && p.burstRecoveryProgress()<=1,"recovery progress stays bounded");}
        check(!p.lost && p.y>=anchor+45 && p.vy>=80 && p.jumpsRemaining()==2,"a naturally descending body recovers another burst without touching a rope");
        check(p.jump() && p.jumpsUsed()==3 && p.jumpsRemaining()==1,"more than two manual airborne jumps are available after real descent");
        p.reset();check(p.jumpsUsed()==0 && p.jumpsRemaining()==2 && p.jumpAllowanceRemaining()==8,"restart resets usage and burst for a new run");
    }
    private static void chapterAllowancesAndArcade() {
        for(int chapter=0;chapter<StageCatalog.CHAPTER_COUNT;chapter++) {
            Physics p=isolated(StageCatalog.firstStageInChapter(chapter),false);int cap=8+chapter*2;
            check(p.jumpLimit()==cap && p.jumpAllowanceRemaining()==cap,"deeper chapters receive increasingly generous overall jump allowances");
            float previousBurstStart=-100000;int used=0,steps=0;
            while(!p.lost && used<cap && steps++<240*60) {
                if(p.canJump()) {
                    if(p.jumpsRemaining()==2){check(p.y>=previousBurstStart+45,"successive airborne bursts must move below their previous start rather than stall at the ceiling");previousBurstStart=p.y;}
                    check(p.jump(),"an available jump succeeds");used++;
                    check(p.jumpsUsed()==used && p.jumpAllowanceRemaining()==cap-used,"campaign overall allowance tracks actual accepted uses");
                }
                step(p);
            }
            check(!p.lost && used==cap && !p.canJump() && p.jumpAllowanceRemaining()==0,"each chapter allowance can be used naturally but cannot be exceeded; chapter="+chapter+", accepted="+used+", stored="+p.jumpsUsed()+", y="+p.y+", reason="+p.deathReason+", t="+p.elapsed);
            steps(p,480);check(!p.jump() && p.jumpsUsed()==cap,"later falling or burst recovery cannot bypass the chapter-wide cap");
        }
        Physics p=isolated(0,true);int used=0,steps=0;
        while(!p.lost && used<24 && steps++<240*60){if(p.canJump()){check(p.jump(),"Arcade recovered burst permits another jump");used++;}step(p);}
        check(!p.lost && used==24 && p.jumpLimit()==0 && p.jumpAllowanceRemaining()==-1 && p.jumpsUsed()==24,"Arcade has no campaign total cap while keeping the same descent-based burst recovery");
        p.reset(8);check(!p.arcade && p.jumpsUsed()==0 && p.jumpLimit()==12,"starting a chapter after Arcade installs its own fresh finite allowance");
    }
    private static void landingsAndCeiling() {
        Physics p=isolated(0,false);p.jump();steps(p,73);p.jump();int used=p.jumpsUsed();
        Physics.Rope r=p.ropes[0];r.reset(2400,p.leftWall(),p.rightWall());p.x=240;p.y=r.y[Physics.Rope.N/2]-p.radius()-5;p.vy=80;
        for(int i=0;i<120 && p.landingPulse==0;i++)step(p);
        check(!p.lost && r.cut<0 && p.landingPulse>0 && p.jumpsRemaining()==2 && p.jumpsUsed()==used && p.jumpAllowanceRemaining()==p.jumpLimit()-used,"a genuine supporting rope refills only the burst, preserving spent campaign allowance");
        p=isolated(0,false);p.jump();steps(p,73);p.jump();p.brokenCount=10;p.score=300;r=p.ropes[0];r.reset(2400,p.leftWall(),p.rightWall());
        p.x=240;p.y=r.y[Physics.Rope.N/2]-p.radius()-5;p.vy=80;for(int i=0;i<120 && r.cut<0;i++)step(p);
        check(r.cut>=0 && p.jumpsRemaining()==0 && p.jumpsUsed()==2,"cutting a rope is not a supporting landing and does not recharge the burst");
        p=isolated(0,false);p.jump();steps(p,73);p.jump();p.x=153;p.y=p.groundY()-p.radius()-1;p.vy=600;step(p);
        check(!p.won && !p.lost && p.groundBounce>0 && p.jumpsRemaining()==2 && p.jumpsUsed()==2,"forgiving floor support refills burst without refunding run usage");
        for(boolean arcade:new boolean[]{false,true}) {
            p=new Physics();if(arcade)p.resetArcade(1,ArcadeRules.ALL);
            p.y=Physics.ARENA_TOP+1.42f*p.radius()+.05f;p.vy=-100;step(p);
            check(p.lost && "CEILING".equals(p.deathReason) && Math.abs(p.headTop()-Physics.ARENA_TOP)<.001f && !p.jump(),"the visible ceiling remains lethal in both modes");
        }
        p=new Physics();check(p.jump(),"a normal spawn jump is available");for(int i=0;i<150;i++){step(p);check(!p.lost,"a single fresh jump fits below the ceiling");}
        p=new Physics();p.jump();steps(p,73);check(p.jump(),"a risky second near-spawn jump is initially allowed");
        for(int i=0;i<240 && !p.lost;i++)step(p);
        check(p.lost && "CEILING".equals(p.deathReason),"two rapid upward jumps cannot perch against a harmless top clamp");
    }
    public static void main(String[] args) {
        cooldownAndRecovery();chapterAllowancesAndArcade();landingsAndCeiling();
        System.out.println("PASS: two-jump bursts, natural descent recovery/more airborne uses, pause/handoff isolation, growing campaign allowances, unlimited Arcade total, support versus cut behavior and lethal ceiling ("+checks+" checks)");
    }
}
