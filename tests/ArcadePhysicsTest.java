package com.perfgames.ropelab;

/** Survival configuration, attack geometry, warnings and drift acceptance scenarios. */
public final class ArcadePhysicsTest {
    private static int checks;
    private static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    private static void near(float actual,float expected,String message){check(Math.abs(actual-expected)<.001f,message+": "+actual);}
    private static Physics arcade(int difficulty,int mask){Physics p=new Physics();p.resetArcade(difficulty,mask);p.touchControlled(true);return p;}
    private static void rulesAndModes(){
        check(ArcadeRules.normalizeMask(0)==31 && ArcadeRules.normalizeMask(32)==31 && ArcadeRules.normalizeMask(33)==1 && ArcadeRules.normalizeMask(15)==15 && ArcadeRules.normalizeMask(16)==16,"custom hazard masks preserve old choices and the new saw bit, discard unknown bits and cannot become empty");
        check(ArcadeRules.clampDifficulty(-1)==0 && ArcadeRules.clampDifficulty(99)==2,"difficulty is bounded");
        check(ArcadeRules.warning(0,1)>ArcadeRules.warning(1,1) && ArcadeRules.warning(1,1)>ArcadeRules.warning(2,1),"harder survival gives a shorter warning");
        check(ArcadeRules.attackGap(0,1)>ArcadeRules.attackGap(1,1) && ArcadeRules.attackGap(1,1)>ArcadeRules.attackGap(2,1),"harder survival leaves less time between attacks");
        for(int d=0;d<3;d++)check(ArcadeRules.warning(d,100)>=.799f && ArcadeRules.attackGap(d,100)>=.499f,"late waves retain bounded readable warnings and gaps");
        Physics p=arcade(99,0);check(p.arcade && p.arcadeDifficulty==2 && p.arcadeHazards==31,"survival startup normalizes setup");
        p.elapsed=40;p.score=900;p.lost=true;p.steerTouch(200);p.fireHazards[0].active=true;p.beamHazards[0].active=true;p.reset();
        check(p.arcade && p.arcadeDifficulty==2 && p.arcadeHazards==31 && !p.lost && !p.won && p.elapsed==0 && p.score==0 && p.wave==1 && p.steeringVelocity()==0,"restart retains survival setup and clears the whole run");
        for(Physics.FireHazard f:p.fireHazards)check(!f.active,"restart removes falling fire");
        for(Physics.BeamHazard b:p.beamHazards)check(!b.active && !b.followCamera,"restart removes live/warning beams");
        p.y=StageCatalog.get(StageCatalog.COUNT-1).groundY+1200;p.vy=800;p.step();
        check(!p.won && !p.lost && p.cameraY>StageCatalog.get(StageCatalog.COUNT-1).groundY,"survival continues below every campaign floor without a false win");
        p.reset(3);check(!p.arcade && p.stage.index==3 && p.groundY()==p.stage.groundY,"campaign selection exits survival");
        p.reset();check(!p.arcade && p.stage.index==3,"campaign restart retains its own stage/mode");
    }
    private static void geometry(){
        near(Physics.pointSegmentDistanceSquared(3,4,0,0,10,0),16,"perpendicular point-to-line distance");
        near(Physics.pointSegmentDistanceSquared(-3,4,0,0,10,0),25,"beam endpoint uses a rounded cap");
        near(Physics.pointSegmentDistanceSquared(3,4,0,0,0,0),25,"zero-length geometry remains well defined");
        near(Physics.segmentDistanceSquared(0,0,10,10,0,10,10,0),0,"crossing swept paths cannot tunnel through a diagonal");
        near(Physics.segmentDistanceSquared(0,0,10,0,0,3,10,3),9,"parallel separated paths preserve their distance");
        near(Physics.segmentDistanceSquared(0,0,10,0,5,0,15,0),0,"collinear overlap collides");
        near(Physics.segmentDistanceSquared(0,0,2,0,4,0,6,0),4,"collinear disjoint endpoints do not collide");
        near(Physics.segmentDistanceSquared(0,0,0,0,3,4,3,4),25,"degenerate swept segments are finite");
    }
    private static Physics fixture(int kind){
        Physics p=arcade(0,kind);p.y=1000;p.cameraY=570;p.vy=0;
        for(Physics.Rope r:p.ropes)r.cut=12;
        return p;
    }
    private static void beamWarningsAndDeaths(){
        int[] kinds={ArcadeRules.TOP_LASERS,ArcadeRules.SIDE_LASERS,ArcadeRules.LIGHTNING};
        String[] reasons={"TOP LASER","SIDE LASER","LIGHTNING"};
        for(int i=0;i<kinds.length;i++){
            Physics p=fixture(kinds[i]);Physics.BeamHazard b=p.beamHazards[0];
            b.active=true;b.kind=kinds[i];b.warningTime=.3f;b.liveTime=.4f;b.width=10;b.followCamera=false;
            if(i==0){b.x1=b.x2=240;b.y1=850;b.y2=1150;}
            else if(i==1){b.x1=100;b.x2=380;b.y1=b.y2=1000;}
            else {b.x1=100;b.x2=380;b.y1=850;b.y2=1150;}
            for(int k=0;k<24;k++){p.steerTouch(-p.driftVelocity());p.step();}
            check(!p.lost && b.warningTime>0,"a directly overlapping warning remains harmless: "+reasons[i]);
            b.warningTime=0;p.step();
            check(p.lost && !p.won && p.blast>0 && reasons[i].equals(p.deathReason),"live geometry produces its distinct fatal reason: "+reasons[i]);
        }
        for(float endpointOffset:new float[]{20,26}){
            Physics p=fixture(ArcadeRules.SIDE_LASERS);p.x=200+endpointOffset;
            Physics.BeamHazard b=p.beamHazards[0];b.active=true;b.kind=ArcadeRules.SIDE_LASERS;b.x1=100;b.x2=200;b.y1=b.y2=1000;b.width=10;b.liveTime=.4f;
            p.steerTouch(-p.driftVelocity());p.step();
            check(p.lost==(endpointOffset==20),"beam thickness/endpoint cap distinguishes contact from a nearby miss");
        }
        Physics p=fixture(ArcadeRules.LIGHTNING);Physics.BeamHazard b=p.beamHazards[0];
        b.active=true;b.kind=ArcadeRules.LIGHTNING;b.x1=70;b.x2=70;b.y1=850;b.y2=1150;b.width=14;b.liveTime=.01f;
        for(int k=0;k<10;k++){p.steerTouch(-p.driftVelocity());p.step();}
        check(!p.lost && !b.active,"a live beam expires safely when the player uses the refuge");
    }
    private static int activeCount(Physics p){int count=0;for(Physics.FireHazard f:p.fireHazards)if(f.active)count++;for(Physics.BeamHazard b:p.beamHazards)if(b.active)count++;for(Physics.SawHazard s:p.sawHazards)if(s.active)count++;return count;}
    private static void spawnedWarningsAndRefuges(){
        for(int d=0;d<3;d++)for(int kind:new int[]{1,2,4,8}){
            Physics p=arcade(d,kind);
            for(int k=0;k<720 && !p.lost && activeCount(p)==0;k++){p.steerTouch(-p.driftVelocity());p.step();}
            check(!p.lost && activeCount(p)==1,"selected custom hazard spawns one attack after preparation time");
            float warning=0,lockedX1=0,lockedX2=0;Physics.BeamHazard beam=null;Physics.FireHazard fire=null;
            for(Physics.FireHazard f:p.fireHazards)if(f.active){fire=f;warning=f.warningTime;lockedX1=f.x;check(kind==1,"custom mask cannot spawn an unselected falling fireball");}
            for(Physics.BeamHazard b:p.beamHazards)if(b.active){beam=b;warning=b.warningTime;lockedX1=b.x1;lockedX2=b.x2;check(kind==b.kind && b.followCamera,"custom mask spawns its selected beam family");}
            check(warning>.75f,"even Brutal warnings give reaction time before damage");
            float low=p.leftWall()+p.hazardInset()+p.radius()+5,high=p.rightWall()-p.hazardInset()-p.radius()-5;
            boolean reachableRefuge=false;
            for(int k=0;k<=60;k++){
                float candidate=low+(high-low)*k/60;
                float clearance=fire!=null?Math.abs(candidate-fire.x)-fire.radius:
                    (float)Math.sqrt(Physics.pointSegmentDistanceSquared(candidate,p.y,beam.x1,beam.y1,beam.x2,beam.y2))-beam.width/2;
                if(clearance>p.radius()+5 && Math.abs(candidate-p.x)<(340*p.steeringScale()-35-12)*warning)reachableRefuge=true;
            }
            check(reachableRefuge,"a spawned telegraph leaves a reachable full-body refuge within the warning budget");
            for(int k=0;k<24;k++){p.steerTouch(100-p.driftVelocity());p.step();check(activeCount(p)<=1,"warnings never stack incompatible simultaneous attacks");}
            check(!p.lost,"movement during a warning is not punished before activation");
            check(fire!=null?fire.x==lockedX1:beam.x1==lockedX1 && beam.x2==lockedX2,"telegraph direction is locked rather than chasing the player's new position");
        }
    }
    private static void idleRequiresCorrection(){
        int idleDeaths=0;
        for(int i=0;i<StageCatalog.COUNT;i++){
            Physics p=new Physics();p.reset(i);p.touchControlled(true);
            for(int k=0;k<240*60 && !p.lost && !p.won;k++)p.step();
            check(p.lost || p.won,"every course reaches a terminal outcome without an artificial fixed pull");
            if(p.lost)idleDeaths++;
        }
        check(idleDeaths>=StageCatalog.COUNT*2/3,"most courses require active correction while safe mechanism introductions may be forgiving");
        Physics p=arcade(0,ArcadeRules.FIREBALLS);float initial=p.x;
        for(int k=0;k<120;k++)p.step();
        check(Math.abs(p.x-initial)>4 && Math.abs(p.driftVelocity())<=35.01f,"survival begins with a bounded, intentional current");
        p.reset();
        for(int k=0;k<240;k++){p.steerTouch(-p.driftVelocity());p.step();}
        check(!p.lost && Math.abs(p.x-240)<1,"the intentional current is controllable with opposite steering");
    }
    private static float destination(Physics p){
        float low=p.leftWall()+p.hazardInset()+p.radius()+5,high=p.rightWall()-p.hazardInset()-p.radius()-5;
        float center=Physics.clamp(240,low,high),best=center,bestClearance=-10000,centerClearance=10000;
        for(int k=0;k<=40;k++){
            float candidate=k==20?center:low+(high-low)*k/40,clearance=10000;
            for(Physics.FireHazard f:p.fireHazards)if(f.active && (f.warningTime>0 || f.y<p.y+90))clearance=Math.min(clearance,Math.abs(candidate-f.x)-f.radius);
            for(Physics.BeamHazard b:p.beamHazards)if(b.active)clearance=Math.min(clearance,beamClearance(candidate,p.y,b));
            for(Physics.SawHazard s:p.sawHazards)if(s.active)clearance=Math.min(clearance,Math.abs(candidate-Physics.clamp(candidate,Math.min(s.warningTime>0?s.pathX1:s.x,s.pathX2),Math.max(s.warningTime>0?s.pathX1:s.x,s.pathX2)))-s.radius);
            if(k==20)centerClearance=clearance;
            if(clearance>bestClearance){bestClearance=clearance;best=candidate;}
        }
        if(centerClearance>p.radius()+9)best=center;
        return best;
    }
    static void avoid(Physics p){
        p.steerTouch(Physics.clamp(((destination(p)-p.x)*12-p.driftVelocity()-p.nudgeVelocity())/p.steeringScale()/p.ropeSteeringScale(),-340,340));
    }
    static void avoidMotion(Physics p){
        p.gx=Physics.clamp(((destination(p)-p.x)*8-p.vx*.65f-p.nudgeVelocity()-p.motionGustVelocity())/p.steeringScale()/p.ropeSteeringScale()*950/Physics.MAX_MOTION_SPEED,-950,950);
    }
    private static void naturalSurvivalAndStress(){
        for(int d=0;d<3;d++){
            Physics p=arcade(d,ArcadeRules.ALL);
            for(int k=0;k<240*25 && !p.lost;k++){avoid(p);p.step();}
            check(!p.lost && !p.won && p.elapsed>24 && p.wave>=2 && p.cameraY>8200,
                    "bounded steering survives mixed natural attacks into the next wave at difficulty "+d+"; reason="+p.deathReason+", t="+p.elapsed);
            System.out.printf("Arcade %s PASS: %.2fs, wave %d, score %d%n",ArcadeRules.DIFFICULTIES[d],p.elapsed,p.wave,p.score);
        }
        Physics p=arcade(0,ArcadeRules.ALL);int warnings=0;
        for(int i=0;i<100000;i++){
            if(p.lost || i%9000==0)p.resetArcade((i/9000)%3,1+(i/3000)%ArcadeRules.ALL);
            avoid(p);if(i%1000==0)p.jump();p.step();
            check(!p.won && Float.isFinite(p.x)&&Float.isFinite(p.y)&&Float.isFinite(p.cameraY)&&Float.isFinite(p.vx)&&Float.isFinite(p.vy),"endless survival state stays finite without a false campaign win");
            for(Physics.BeamHazard b:p.beamHazards){check(Float.isFinite(b.x1)&&Float.isFinite(b.y1)&&Float.isFinite(b.x2)&&Float.isFinite(b.y2)&&Float.isFinite(b.warningTime)&&Float.isFinite(b.liveTime),"survival beam pool stays finite");if(b.active&&b.warningTime>0)warnings++;}
            for(Physics.FireHazard f:p.fireHazards){check(Float.isFinite(f.x)&&Float.isFinite(f.y)&&Float.isFinite(f.warningTime),"survival fire pool stays finite");if(f.active&&f.warningTime>0)warnings++;}
            for(Physics.SawHazard s:p.sawHazards){check(Float.isFinite(s.x)&&Float.isFinite(s.y)&&Float.isFinite(s.warningTime)&&Float.isFinite(s.liveTime),"survival saw pool stays finite");if(s.active&&s.warningTime>0)warnings++;}
            for(Physics.Rope r:p.ropes)for(int k=0;k<Physics.Rope.N;k++)check(Float.isFinite(r.x[k])&&Float.isFinite(r.y[k]),"survival rope pool stays finite");
        }
        check(warnings>100,"long survival stress exercises actual scheduled telegraphs");
    }
    private static float beamClearance(float candidate,float y,Physics.BeamHazard b) {
        // Hold the horizontal refuge through a bounce, rather than recentering into a live lane.
        if(b.kind==ArcadeRules.SIDE_LASERS)
            return Math.abs(candidate-Physics.clamp(candidate,Math.min(b.x1,b.x2),Math.max(b.x1,b.x2)))-b.width/2;
        return (float)Math.sqrt(Physics.pointSegmentDistanceSquared(candidate,y,b.x1,b.y1,b.x2,b.y2))-b.width/2;
    }
    public static void main(String[] args){
        rulesAndModes();geometry();beamWarningsAndDeaths();spawnedWarningsAndRefuges();idleRequiresCorrection();naturalSurvivalAndStress();
        System.out.println("PASS: Arcade configuration/reset, swept geometry, three beam deaths/near misses, warning/refuge/lock fairness, gentle drift/later-stage challenge, three-difficulty natural survival and 100000 survival stability steps ("+checks+" checks)");
    }
}
