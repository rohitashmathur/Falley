package com.perfgames.ropelab;

/** Test observable physical effects and safety, rather than just catalog flags. */
public final class CampaignMechanicsTest {
    private static int checks;
    private static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    private static Physics isolated(int index) {
        Physics p=new Physics();p.reset(index);p.touchControlled(true);p.y=2400;p.cameraY=1970;
        for(Physics.Rope rope:p.ropes){rope.cut=12;rope.base=100000;}
        try {java.lang.reflect.Field f=Physics.class.getDeclaredField("attackCountdown");f.setAccessible(true);f.setFloat(p,100000);}
        catch(ReflectiveOperationException e){throw new AssertionError(e);}
        return p;
    }
    private static void neutral(Physics p){p.steerTouch((-p.driftVelocity()-p.nudgeVelocity())/p.steeringScale());p.step();}
    private static void targets() {
        Physics p=new Physics();p.reset(1);float first=p.targetCenter();p.elapsed=3;
        check(Math.abs(first-p.targetCenter())>10,"moving target actually moves in world space");
        p.reset(20);float full=p.targetWidth();p.elapsed=(float)(Math.PI/.65);
        check(p.targetWidth()<full*.7f&&p.targetWidth()>95,"shrinking target preserves an avatar-wide landing area");
        for(int i=0;i<StageCatalog.COUNT;i++) {
            p.reset(i);
            for(int t=0;t<50;t++){p.elapsed=t;p.y=p.groundY()-200;p.score=t*30;
                check(Float.isFinite(p.targetCenter())&&p.targetWidth()>=95&&p.targetLeft()<240&&p.targetRight()>240,"dynamic target remains bounded and reachable in every phase");}
        }
        p.reset(1);p.touchControlled(true);p.y=p.groundY()-p.radius()-1;p.cameraY=p.groundY()-675;
        p.x=p.targetCenter()+4;p.vy=500;float raw=p.x;neutral(p);
        check(p.won&&Math.abs(p.landingX-raw)<.1f&&Math.abs(p.landingTargetCenter-p.stage.targetCenter)<1,"raw first landing captured before terminal velocity reset");
        float center=p.targetCenter(),width=p.targetWidth();for(int i=0;i<300;i++)p.step();
        check(p.targetCenter()==center&&p.targetWidth()==width,"target stays still during win celebration");
    }
    private static void physicalModifiers() {
        Physics normal=isolated(0),moon=isolated(2);neutral(normal);neutral(moon);
        check(moon.vy<normal.vy*.8f&&moon.gravityScale()==.72f,"low gravity changes actual acceleration");
        Physics heavy=new Physics();heavy.reset(5);Physics standard=new Physics();standard.reset(4);
        check(heavy.mass()>standard.mass()*1.2f&&heavy.radius()>standard.radius(),"heavy load physically increases mass and rope load");
        Physics single=isolated(10);single.y=2800;single.cameraY=2370;
        check(single.maxBurstJumps()==1&&single.jumpsRemaining()==1&&single.jump(),"single-jump challenge starts with one charge per burst");
        for(int i=0;i<80;i++)neutral(single);
        check(!single.jump()&&single.jumpsRemaining()==0,"single-jump modifier rejects a second quick jump");
        int steps=0;while(!single.canJump()&&!single.lost&&steps++<720)neutral(single);
        check(single.canJump()&&single.jump()&&single.jumpsUsed()==2,"one-jump challenge recharges after actual descent rather than limiting the entire run to one");
        Physics wind=isolated(12);wind.y=wind.windZones[0].top+150;wind.cameraY=wind.y-430;neutral(wind);
        check(wind.windVelocity()>20&&wind.vx>20,"wind zone applies real sideways motion in touch play");
        wind.touchControlled(false);wind.gx=0;for(int i=0;i<100;i++)wind.step();
        check(wind.vx>5,"the same zone affects neutral motion controls");
        wind.y=wind.windZones[0].bottom+100;check(wind.windVelocity()==0,"wind stays within its visible zone");
    }
    private static void mechanisms() {
        Physics p=isolated(4);Physics.BouncePad pad=p.bouncePads[0];
        p.x=pad.x;p.y=pad.y-p.radius()-1;p.cameraY=p.y-430;p.vy=600;neutral(p);
        check(p.vy<0&&pad.cooldown>1&&p.landingPulse>0,"bounce pad catches downward swept motion and rebounds");
        p=isolated(16);Physics.SlowPickup pickup=p.slowPickups[0];
        p.x=pickup.x;p.y=pickup.y;p.cameraY=p.y-430;neutral(p);
        check(pickup.collected&&p.slowTimeRemaining>3&&p.worldTimeScale()<1,"collectible triggers actual slow motion");
        float before=p.elapsed;neutral(p);check(p.elapsed-before<Physics.DT*.6f,"world simulation runs slower during pickup effect");
        for(int i=0;i<240*4&&!p.lost;i++)neutral(p);
        check(p.slowTimeRemaining==0&&p.worldTimeScale()==1,"slow effect expires and cannot be collected twice");
        p=isolated(8);Physics.Rope rope=p.ropes[0];rope.reset(2400,p.leftWall(),p.rightWall());
        rope.fragile=true;rope.anchorStress=.85f;p.x=240;p.y=rope.y[Physics.Rope.N/2]-p.radius()-4;p.vy=150;
        for(int i=0;i<60&&rope.cut<0;i++)neutral(p);
        check(rope.anchorStress==1&&(rope.cut==0||rope.cut==Physics.Rope.N-2),"fragile anchor fails at its advertised endpoint under actual landing load");
        check(p.brokenCount==1,"anchor release counts as one rope cut");
    }
    private static void bossesAndArcade() {
        for(int chapter=0;chapter<6;chapter++) {
            Physics p=new Physics();p.reset(chapter*4+3);p.touchControlled(true);
            p.y=p.groundY()-2400;p.cameraY=p.y-430;
            for(Physics.Rope rope:p.ropes){rope.cut=12;rope.base=100000;}
            for(int i=0;i<170&&!p.lost;i++){CampaignExpansionTest.steer(p);p.step();}
            check(p.bossActive&&p.bossPhase>0&&p.bossWarning>0&&!p.lost,"each boss announces a real attack before becoming lethal");
            p.y=p.groundY()-640;p.cameraY=p.y-430;p.x=240;CampaignExpansionTest.steer(p);p.step();
            check(!p.bossActive&&p.bossWarning==0&&p.bossLive==0,"boss yields a clear final landing corridor");
        }
        for(int difficulty=0;difficulty<3;difficulty++) {
            Physics p=new Physics();p.resetArcade(difficulty,ArcadeRules.ALL);
            check(p.arcade&&!p.daily&&p.stage.hazard==difficulty&&p.gravityScale()==1&&p.maxBurstJumps()==2,"Arcade retains original wall, gravity and burst behavior");
            for(Physics.WindZone z:p.windZones)check(!z.active,"campaign wind absent from existing Arcade");
            for(Physics.BouncePad pad:p.bouncePads)check(!pad.active,"campaign pad absent from existing Arcade");
            for(Physics.SlowPickup pickup:p.slowPickups)check(!pickup.active,"campaign pickup absent from existing Arcade");
        }
    }
    public static void main(String[] args){targets();physicalModifiers();mechanisms();bossesAndArcade();System.out.println("PASS campaign mechanisms ("+checks+" checks)");}
}
