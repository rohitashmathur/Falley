package com.perfgames.ropelab;

/** Deterministic chapter, touch, hazard and complete-course scenarios without Android. */
public final class PhysicsTest {
    private static int checks;
    private static void check(boolean ok, String message) {
        checks++;
        if (!ok) throw new AssertionError(message);
    }
    private static void run(Physics p, int steps) { for (int i=0;i<steps;i++) p.step(); }
    private static Physics stage(int index) {
        Physics p=new Physics(); p.reset(index); p.touchControlled(true); return p;
    }
    private static void aboveRope(Physics p) {
        Physics.Rope r=p.ropes[0]; p.x=r.x[Physics.Rope.N/2];
        p.y=r.y[Physics.Rope.N/2]-p.radius()-5; p.vx=0; p.vy=80;
    }
    private static void catalogAndReset() {
        check(StageCatalog.COUNT==12 && StageCatalog.CHAPTER_COUNT==6,"twelve stages in six chapters are available");
        float previous=0;
        for(int i=0;i<StageCatalog.COUNT;i++) {
            StageCatalog.Stage s=StageCatalog.get(i);
            check(s.index==i && s.chapterIndex==i/2 && !s.title.isEmpty() && !s.chapterTitle.isEmpty(),"stages form six named chapter pairs");
            check(s.groundY>=4200 && (i%2==0 || s.groundY>previous) && s.hazard>=StageCatalog.LASER && s.hazard<=StageCatalog.FIRE,"each chapter has a longer second descent and an explicit valid boundary hazard");
            check(s.fireRain==((s.extraHazards&ArcadeRules.FIREBALLS)!=0),"fire-rain capability matches each stage's explicit incoming attack mask"); previous=s.groundY;
            check(s.fatalMiss==(i>=3),"stages after the first three make a floor miss fatal");
            Physics p=stage(i); p.score=99;p.won=true;p.reset();
            check(p.stage.index==i && p.score==0 && !p.won && !p.lost && p.cameraY==0,"restart preserves the chosen stage and clears gameplay");
            for(Physics.FireHazard f:p.fireHazards) check(!f.active && f.warningTime==0,"restart clears the fire pool");
        }
        check(StageCatalog.get(-1).index==0 && StageCatalog.get(99).index==StageCatalog.COUNT-1,"invalid stage indices remain bounded");
    }
    private static void ropesMassAndJump() {
        Physics p=stage(0);float initial=p.mass();run(p,240);
        check(p.score>=9 && p.score<=11 && p.mass()>initial,"elapsed play earns points and weight");
        p.score=100000;check(p.mass()>=5.9f && p.mass()<=6.01f,"weight stays capped");p.reset();
        aboveRope(p);
        for(int i=0;i<120 && p.landingPulse==0;i++)p.step();
        check(p.ropes[0].cut==-1 && p.landingPulse>0 && p.vy<0,"a light landing catches, crouches and auto-jumps");
        float landedY=p.y;run(p,24);check(p.y<landedY-5,"the rope-assisted jump causes real ascent");
        p.reset();p.elapsed=30;p.score=300;aboveRope(p);
        float base=p.ropes[0].base;
        for(int i=0;i<120 && p.ropes[0].cut==-1;i++)p.step();
        int cut=p.ropes[0].cut;
        check(cut>=0 && p.brokenCount==1 && p.score>=320,"heavy weight cuts a segment and awards a bonus");
        p.y=base-150;p.vy=0;run(p,30);
        check(p.ropes[0].cut==cut && p.ropes[0].base==base && p.brokenCount==1,"a visible cut persists without repeated scoring");
        p.reset();float initialY=p.y;
        check(p.jump() && p.vy<0 && !p.jump(),"manual jump gives an upward impulse with cooldown");
        run(p,30);check(p.y<initialY-5,"manual jumping visibly raises the character");run(p,45);
        check(p.jump(),"jump becomes available after the cooldown");
    }
    private static void touchDirectionAndRelease() {
        for(int direction:new int[]{-1,1}) {
            Physics p=stage(0);aboveRope(p);p.gx=-direction*950;
            float start=p.x;p.steerTouch(direction*160);run(p,48);
            check((p.x-start)*direction>20 && p.vx*direction>0,"each swipe moves in its direction despite rope contact or stale sensor force");
            p.steerTouch(0);float released=p.x;run(p,200);
            check(p.steeringVelocity()==0 && Math.abs(p.vx-p.driftVelocity()-p.nudgeVelocity())<.1f,
                    "release/cancel removes player steering while the intentional air current/nudge remain");
            check(Math.abs(p.x-released)>1 && Math.abs(p.driftVelocity())<=35.01f,"the visible air current requires a correction after release");
        }
        Physics neutral=stage(0);aboveRope(neutral);float center=neutral.x;
        for(int i=0;i<1000 && !neutral.lost;i++){neutral.steerTouch((-neutral.driftVelocity()-neutral.nudgeVelocity())/neutral.steeringScale());neutral.step();}
        check(!neutral.lost && Math.abs(neutral.x-center)<1,"countersteering can hold position through air current and rope reactions");
        neutral.touchControlled(false);neutral.gx=450;run(neutral,30);
        check(neutral.vx>0 && neutral.x>center+2,"sensor/acceleration steering still works outside touch-only mode");
    }
    private static void sideHazardsAndNarrowing() {
        for(int i=0;i<StageCatalog.COUNT;i++) {
            Physics p=stage(i);float initial=p.rightWall()-p.leftWall();
            p.y=p.groundY()/2; p.elapsed=10;p.score=200;
            check(p.rightWall()-p.leftWall()<initial-30,"the sides narrow during a long descent");
            String reason=p.stage.hazard==StageCatalog.LASER?"LASER":p.stage.hazard==StageCatalog.SPIKES?"SPIKES":"FIRE";
            for(boolean right:new boolean[]{false,true}) {
                p.reset();p.x=right?p.rightWall()-p.hazardInset()-p.radius()+.5f:p.leftWall()+p.hazardInset()+p.radius()-.5f;
                p.step();check(p.lost && !p.won && p.blast>0 && reason.equals(p.deathReason),"both chapter sides kill with the correct hazard reason");
                check(!p.jump(),"a hazard death cannot jump again");
            }
        }
        Physics stalled=stage(0);
        for(int i=0;i<240*260 && !stalled.lost && !stalled.won;i++) {
            stalled.steerTouch(-stalled.driftVelocity());if(i%120==0)stalled.jump();stalled.step();
        }
        check(stalled.lost && !stalled.won,"repeated air jumps cannot stall the course indefinitely");
    }
    private static void jumpsNeverWidenHazards() {
        Physics p=stage(0);float left=p.leftWall(),right=p.rightWall(),initialGap=right-left;int jumps=0;
        for(int i=0;i<240*90 && !p.lost && !p.won;i++) {
            p.steerTouch(-p.driftVelocity());if(i%80==0 && p.jump())jumps++;
            p.step();
            check(p.leftWall()>=left-.0001f && p.rightWall()<=right+.0001f,
                    "actual manual/automatic jumps cannot reopen the narrowing sides");
            left=p.leftWall();right=p.rightWall();
        }
        check(jumps>0 && right-left<=initialGap && (!p.lost || p.deathReason.equals("CEILING") || p.deathReason.equals("LASER")),"budgeted jumps cannot widen the sides, including a terminal ceiling contact");
    }
    private static void safeSteer(Physics p) {
        float safeLeft=p.leftWall()+p.hazardInset()+p.radius()+8;
        float safeRight=p.rightWall()-p.hazardInset()-p.radius()-8;
        // Dodge telegraphed fire in the whole safe arena, then line up to finish.
        if(p.y>p.groundY()-350) {
            safeLeft=Math.max(safeLeft,p.targetLeft()+p.radius()+3);
            safeRight=Math.min(safeRight,p.targetRight()-p.radius()-3);
        }
        float center=Physics.clamp((p.targetLeft()+p.targetRight())/2,safeLeft,safeRight);
        float[] candidates={safeLeft,center,safeRight};float best=center,bestClearance=-1;
        for(float candidate:candidates) {
            float clearance=10000;
            for(Physics.FireHazard f:p.fireHazards)
                if(f.active && (f.warningTime>0 || f.y<p.y+90))clearance=Math.min(clearance,Math.abs(candidate-f.x));
            for(Physics.BeamHazard b:p.beamHazards)if(b.active) {
                if(b.warningTime>0 && b.warningTime>(p.groundY()-p.y)/Math.max(100,p.vy))continue;
                clearance=Math.min(clearance,beamClearance(candidate,p.y,b));
            }
            for(Physics.SawHazard s:p.sawHazards)if(s.active)
                clearance=Math.min(clearance,Math.abs(candidate-Physics.clamp(candidate,Math.min(s.warningTime>0?s.pathX1:s.x,s.pathX2),Math.max(s.warningTime>0?s.pathX1:s.x,s.pathX2)))-s.radius);
            if(clearance>bestClearance || clearance==bestClearance && candidate==center){bestClearance=clearance;best=candidate;}
        }
        // Prefer the finish lane whenever its clearance is sufficient; only dodge a real threat.
        float centerClearance=10000;
        for(Physics.FireHazard f:p.fireHazards)if(f.active && (f.warningTime>0 || f.y<p.y+90))
            centerClearance=Math.min(centerClearance,Math.abs(center-f.x)-f.radius);
        for(Physics.BeamHazard b:p.beamHazards)if(b.active) {
            if(b.warningTime>0 && b.warningTime>(p.groundY()-p.y)/Math.max(100,p.vy))continue;
            centerClearance=Math.min(centerClearance,beamClearance(center,p.y,b));
        }
        for(Physics.SawHazard s:p.sawHazards)if(s.active)
            centerClearance=Math.min(centerClearance,Math.abs(center-Physics.clamp(center,Math.min(s.warningTime>0?s.pathX1:s.x,s.pathX2),Math.max(s.warningTime>0?s.pathX1:s.x,s.pathX2)))-s.radius);
        if(centerClearance>p.radius()+9)best=center;
        p.steerTouch(Physics.clamp(((best-p.x)*10-p.driftVelocity()-p.nudgeVelocity())/p.steeringScale(),-300,300));
    }
    private static void variedTargetsRequireSteering() {
        for(int i=0;i<StageCatalog.COUNT;i++) {
            Physics p=stage(i);p.elapsed=30;p.score=300;
            check(p.targetLeft()>0 && p.targetRight()<Physics.W && p.targetRight()-p.targetLeft()>p.radius()*2+12,
                    "each stage has a bounded target corridor with room for the complete character proxy");
            float center=(p.targetLeft()+p.targetRight())/2;
            p.x=center;p.y=p.groundY()-p.radius()-1;p.vy=240;p.step();
            check(p.won && !p.lost,"the center of each stage target permits a complete descending landing");
            if(i>=1 && i<=4) {
                p.reset();p.elapsed=30;p.score=300;p.x=240;p.y=p.groundY()-p.radius()-1;p.vy=240;p.step();
                if(p.stage.fatalMiss) {
                    check(!p.won && p.lost && "HARD LANDING".equals(p.deathReason),"a late-stage target miss causes the required hard landing");
                } else {
                    check(!p.won && !p.lost && p.groundBounce>0,"an early target miss gets a forgiving bounce");
                    for(int k=0;k<240 && !p.won && !p.lost;k++){p.steerTouch((center<240?-140:140)-p.driftVelocity());p.step();}
                    check(p.won && !p.lost,"intentional steering recovers into an early shifted target");
                }
            }
        }
    }
    private static void allCoursesWinAndRecycle() {
        for(int i=0;i<StageCatalog.COUNT;i++) {
            Physics p=stage(i);float initialBase=p.ropes[0].base;
            int steps=0,warnings=0,sawWarnings=0;
            float previousLeft=p.leftWall(),previousRight=p.rightWall();
            while(!p.lost && !p.won && steps++<240*120) {
                safeSteer(p);p.step();
                check(p.leftWall()>=previousLeft-.0001f && p.rightWall()<=previousRight+.0001f,
                        "rope landings and fire dodges cannot widen the course walls");
                previousLeft=p.leftWall();previousRight=p.rightWall();
                for(Physics.FireHazard f:p.fireHazards)if(f.active && f.warningTime>0)warnings++;
                for(Physics.BeamHazard b:p.beamHazards)if(b.active && b.warningTime>0)warnings++;
                for(Physics.SawHazard s:p.sawHazards)if(s.active && s.warningTime>0){warnings++;sawWarnings++;}
            }
            check(p.won && !p.lost,"stage "+i+" can be won by actual gravity/rope progression and bounded steering; reason="+p.deathReason+", x="+p.x+", y="+p.y+", elapsed="+p.elapsed);
            check(p.elapsed>8 && p.brokenCount>8 && p.ropes[0].base>initialBase,"the longer course uses and recycles its rope pool");
            check(p.groundY()-p.cameraY<=Physics.H && p.cameraY>0 && p.y+p.radius()<=p.groundY()+.01f,"camera and target landing stay bounded");
            if(i==StageCatalog.COUNT-1)check(warnings>0,"the winning final-stage run encounters mixed hazard warnings");
            if(i==3)check(sawWarnings>0,"the actual winning Needle run introduces telegraphed saws before landing");
            for(Physics.Rope r:p.ropes)check(r.base<p.groundY()-300,"recycled mounts stop above the target floor");
            System.out.printf("Stage %d PASS: %.2fs, %d cuts, score %d%n",i+1,p.elapsed,p.brokenCount,p.score);
        }
    }
    private static void fireWarningAndContact() {
        Physics p=stage(5);p.resetArcade(0,ArcadeRules.FIREBALLS);p.y=1800;p.cameraY=1370;p.vy=0;
        for(Physics.Rope r:p.ropes)r.cut=12;
        Physics.FireHazard spawned=null;
        for(int i=0;i<720 && !p.lost && spawned==null;i++) {
            p.steerTouch(-p.driftVelocity());p.step();for(Physics.FireHazard f:p.fireHazards)if(f.active){spawned=f;break;}
        }
        check(spawned!=null && spawned.warningTime>.5f && !p.lost,"a fireball first appears as a substantial warning, not an instant hit");
        float warningX=spawned.x,warningRadius=spawned.radius;
        p.reset();p.y=1800;p.cameraY=1370;p.vy=0;for(Physics.Rope r:p.ropes)r.cut=12;
        Physics.FireHazard repeated=null;
        for(int i=0;i<720 && repeated==null;i++){p.steerTouch(-p.driftVelocity());p.step();for(Physics.FireHazard f:p.fireHazards)if(f.active){repeated=f;break;}}
        check(repeated!=null && repeated.x==warningX && repeated.radius==warningRadius,"restart replays the deterministic warning sequence for tests");
        p.reset();p.y=1800;p.cameraY=1370;p.vy=0;
        Physics.FireHazard f=p.fireHazards[0];f.active=true;f.warningTime=.5f;f.x=p.x;f.y=p.y;
        p.step();check(!p.lost && f.warningTime>0,"a warning cannot kill even when staged at the character");
        f.warningTime=0;f.x=p.x;f.y=p.y;p.step();
        check(p.lost && "FALLING FIRE".equals(p.deathReason),"a live falling fireball has its own collision death reason");
        p.reset(4);p.y=1800;p.vy=0;run(p,600);
        for(Physics.FireHazard fire:p.fireHazards)check(!fire.active,"earlier Fire stages do not unexpectedly rain fire");
    }
    private static void floorRecoveryAndWinFreeze() {
        Physics p=stage(0);p.x=153;p.y=p.groundY()-p.radius()-1;p.vy=600;p.step();
        check(!p.won && !p.lost && p.vy<0 && p.groundBounce>0,"an off-target floor landing bounces without a false win");
        for(int i=0;i<240 && !p.won && !p.lost;i++){p.steerTouch(150-p.driftVelocity());p.step();}
        check(p.won && !p.lost,"a swipe can recover from the bounce into the target");
        int score=p.score;float elapsed=p.elapsed,x=p.x;p.steerTouch(-340);
        check(!p.jump(),"winning locks manual jump input");run(p,60);
        check(p.won && !p.lost && p.score==score && p.elapsed==elapsed && p.x==x && p.winTime>.2f,"winning freezes score and touch input while its bounce continues");
        run(p,1000);check(p.y+p.radius()<=p.groundY()+.01f && Float.isFinite(p.vy),"celebration stays bounded at the floor");
        p.reset(3);p.x=(p.targetLeft()+p.targetRight())/2;p.y=p.groundY()+5000;p.vy=10000;p.step();
        check(p.won && p.y+p.radius()<=p.groundY()+.01f && p.cameraY<p.groundY(),"a descending floor overshoot is safely clamped");
        p.reset();check(p.stage.index==3 && !p.won && p.winTime==0 && p.score==0,"restart clears a win and retains the stage");
        for(boolean right:new boolean[]{false,true}) {
            p.reset();p.x=right?p.targetRight()-p.radius()+1:p.targetLeft()+p.radius()-1;p.y=p.groundY()-p.radius();p.vy=100;p.step();
            check(!p.won,"partial contact beyond either target edge cannot win");
        }
    }
    private static void stress() {
        Physics p=stage(0);java.util.Random random=new java.util.Random(7);int deaths=0,wins=0;
        for(int i=0;i<100000;i++) {
            if(p.lost){p.reset(random.nextInt(StageCatalog.COUNT));deaths++;}
            if(p.won && p.winTime>1.2f){p.reset(random.nextInt(StageCatalog.COUNT));wins++;}
            if(i%6000==0){p.reset(random.nextInt(StageCatalog.COUNT));p.y=p.groundY()-p.radius()-1;p.vy=800;}
            else {if(i%95==0)p.jump();if(i%180==0)p.steerTouch((random.nextFloat()*2-1)*500);}
            p.step();
            check(Float.isFinite(p.x)&&Float.isFinite(p.y)&&Float.isFinite(p.vx)&&Float.isFinite(p.vy)&&Float.isFinite(p.cameraY)
                    &&Float.isFinite(p.winTime)&&Float.isFinite(p.landingPulse)&&Float.isFinite(p.groundBounce),"all gameplay/animation state stays finite");
            check(!(p.won&&p.lost)&&p.cameraY>=0&&p.cameraY<p.groundY()&&p.mass()>=1&&p.mass()<=6.01f,"result, camera and weight bounds hold");
            for(Physics.Rope r:p.ropes)for(int k=0;k<Physics.Rope.N;k++)check(Float.isFinite(r.x[k])&&Float.isFinite(r.y[k]),"rope nodes stay finite");
            for(Physics.FireHazard f:p.fireHazards)check(Float.isFinite(f.x)&&Float.isFinite(f.y)&&Float.isFinite(f.warningTime)&&f.warningTime>=0,"the fixed fire pool stays finite");
            for(Physics.BeamHazard b:p.beamHazards)check(Float.isFinite(b.x1)&&Float.isFinite(b.y1)&&Float.isFinite(b.x2)&&Float.isFinite(b.y2)
                    &&Float.isFinite(b.warningTime)&&Float.isFinite(b.liveTime),"beam geometry/timers remain finite");
        }
        check(deaths>0&&wins>0,"stress exercises hazards, victories, celebrations and restarts");
    }
    private static float beamClearance(float candidate,float y,Physics.BeamHazard b) {
        // Hold the horizontal refuge through a bounce, rather than recentering into a live lane.
        if(b.kind==ArcadeRules.SIDE_LASERS)
            return Math.abs(candidate-Physics.clamp(candidate,Math.min(b.x1,b.x2),Math.max(b.x1,b.x2)))-b.width/2;
        return (float)Math.sqrt(Physics.pointSegmentDistanceSquared(candidate,y,b.x1,b.y1,b.x2,b.y2))-b.width/2;
    }
    public static void main(String[] args) {
        catalogAndReset();ropesMassAndJump();touchDirectionAndRelease();sideHazardsAndNarrowing();jumpsNeverWidenHazards();
        variedTargetsRequireSteering();allCoursesWinAndRecycle();fireWarningAndContact();floorRecoveryAndWinFreeze();stress();
        System.out.println("PASS: twelve campaign courses, intentional drift/nudge/countersteer, ropes/weight, narrowing/ceiling hazards, warning fire/contact, forgiving and fatal floors, terminal wins/reset and 100000 seeded stability steps ("+checks+" checks)");
    }
}
