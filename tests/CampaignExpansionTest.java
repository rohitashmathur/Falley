package com.perfgames.ropelab;

/** Real, complete courses using bounded steering, without teleporting or removing obstacles. */
public final class CampaignExpansionTest {
    private static int checks;
    private static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    private static float clearance(Physics p,float candidate) {
        float clear=10000;
        for(Physics.FireHazard f:p.fireHazards)if(f.active&&(f.warningTime>0||f.y<p.y+90))
            clear=Math.min(clear,Math.abs(candidate-f.x)-f.radius);
        for(Physics.BeamHazard b:p.beamHazards)if(b.active) {
            float distance=b.kind==ArcadeRules.SIDE_LASERS
                    ? Math.abs(candidate-Physics.clamp(candidate,Math.min(b.x1,b.x2),Math.max(b.x1,b.x2)))
                    : (float)Math.sqrt(Physics.pointSegmentDistanceSquared(candidate,p.y,b.x1,b.y1,b.x2,b.y2));
            clear=Math.min(clear,distance-b.width/2);
        }
        for(Physics.SawHazard s:p.sawHazards)if(s.active)
            clear=Math.min(clear,Math.abs(candidate-Physics.clamp(candidate,
                    Math.min(s.warningTime>0?s.pathX1:s.x,s.pathX2),Math.max(s.warningTime>0?s.pathX1:s.x,s.pathX2)))-s.radius);
        return clear;
    }
    private static float destination(Physics p) {
        float low=p.leftWall()+p.hazardInset()+p.radius()+8;
        float high=p.rightWall()-p.hazardInset()-p.radius()-8;
        if(p.y>p.groundY()-350){low=Math.max(low,p.targetLeft()+p.radius()+3);high=Math.min(high,p.targetRight()-p.radius()-3);}
        float center=Physics.clamp(p.targetCenter(),low,high),best=center,bestClear=-10000;
        for(float candidate:new float[]{low,center,high}) {
            float clear=clearance(p,candidate);
            if(clear>bestClear||clear==bestClear&&candidate==center){bestClear=clear;best=candidate;}
        }
        if(clearance(p,center)>p.radius()+9)best=center;
        return best;
    }
    static void steer(Physics p) {
        p.steerTouch(Physics.clamp(((destination(p)-p.x)*10-p.driftVelocity()-p.nudgeVelocity()-p.windVelocity())/p.steeringScale()/p.ropeSteeringScale(),-300,300));
    }
    static void steerMotion(Physics p) {
        p.gx=Physics.clamp(((destination(p)-p.x)*6-p.vx*.65f-p.nudgeVelocity()-p.motionGustVelocity()-p.windVelocity())/p.steeringScale()/p.ropeSteeringScale()*950/Physics.MAX_MOTION_SPEED,-950,950);
    }
    private static void catalog() {
        check(StageCatalog.COUNT==24&&StageCatalog.CHAPTER_COUNT==6,"24 courses in six chapters");
        int mechanics=0,modifiers=0;
        java.util.Set<String> names=new java.util.HashSet<>();
        for(int chapter=0;chapter<6;chapter++) {
            int first=StageCatalog.firstStageInChapter(chapter);
            check(first==chapter*4&&StageCatalog.stageCountInChapter(chapter)==4,"four contiguous courses per chapter");
            for(int offset=0;offset<4;offset++) {
                StageCatalog.Stage s=StageCatalog.get(first+offset);
                check(s.index==first+offset&&s.chapterIndex==chapter&&names.add(s.title),"unique stable stage identity");
                check((offset==3)==(s.bossKind!=0),"each chapter ends in exactly one real boss");
                check(s.parJumps>=0&&s.parJumps<8+2*chapter,"mastery par is lower than hard allowance");
                check(s.targetWidth>=138&&s.targetWidth<=164&&s.groundY>=4200&&s.groundY<=11400,"bounded target and drop geometry");
                mechanics|=s.mechanics;modifiers|=s.modifiers;
            }
        }
        check(mechanics==63&&modifiers==15,"all six mechanisms and four physics modifiers are present");
        check(StageCatalog.get(-1).index==0&&StageCatalog.get(99).index==23,"catalog bounds invalid selections");
    }
    private static void complete(Physics p,String label) { complete(p,label,true); }
    private static void complete(Physics p,String label,boolean touch) {
        p.touchControlled(touch);int steps=0,maxAttacks=0,warnings=0,lives=0;
        while(!p.lost&&!p.won&&steps++<240*100) {
            if(touch)steer(p);else steerMotion(p);p.step();
            check(Float.isFinite(p.x)&&Float.isFinite(p.y)&&Float.isFinite(p.vx)&&Float.isFinite(p.vy),"finite world in "+label);
            int attacks=0;
            for(Physics.FireHazard h:p.fireHazards)if(h.active){attacks++;if(h.warningTime>0)warnings++;else lives++;}
            for(Physics.BeamHazard h:p.beamHazards)if(h.active){attacks++;if(h.warningTime>0)warnings++;else lives++;}
            for(Physics.SawHazard h:p.sawHazards)if(h.active){attacks++;if(h.warningTime>0)warnings++;else lives++;}
            maxAttacks=Math.max(maxAttacks,attacks);
        }
        check(p.won&&!p.lost,label+" must finish naturally: "+p.deathReason+" x="+p.x+" y="+p.y+" t="+p.elapsed+" boss="+p.bossPhase);
        check(maxAttacks<=1,"no compound unavoidable attacks in "+label);
        check(p.brokenCount>10&&p.cameraY>0&&p.groundY()-p.cameraY<=Physics.H,"real ropes, recycling and visible floor in "+label);
        check(Float.isFinite(p.landingX)&&p.landingTargetWidth>0&&p.landingX>=p.targetLeft()+p.radius()&&p.landingX<=p.targetRight()-p.radius(),"raw valid landing recorded for stars");
        if(p.stage.bossKind!=0)check(p.bossPhase>=2&&warnings>0&&lives>0,"boss attacks have repeated visible warnings and lethal phases in "+label);
        System.out.printf("%s PASS %.2fs %dcuts boss%d attacks%d%n",label,p.elapsed,p.brokenCount,p.bossPhase,maxAttacks);
    }
    private static void dailyDeterminism() {
        for(long seed:new long[]{0,1,2,3,4,5,7283,20261024,-1,Long.MIN_VALUE,Long.MAX_VALUE}) {
            Physics a=new Physics(),b=new Physics();a.resetDaily(seed);b.resetDaily(seed);
            check(a.daily&&!a.arcade&&a.stage.bossKind==0&&a.stage.modifiers==0,"daily remains a bounded separate mode");
            a.touchControlled(true);b.touchControlled(true);
            for(int i=0;i<240*8&&!a.lost&&!a.won;i++) {
                steer(a);steer(b);a.step();b.step();
                check(Float.floatToIntBits(a.x)==Float.floatToIntBits(b.x)&&Float.floatToIntBits(a.y)==Float.floatToIntBits(b.y)&&a.score==b.score&&a.brokenCount==b.brokenCount,"same seed and inputs produce exact replay");
            }
            a.reset();check(a.daily&&a.stage.targetCenter==b.stage.targetCenter&&a.elapsed==0,"daily retry keeps seed and template");
        }
        for(int template=0;template<6;template++){Physics p=new Physics();p.resetDaily(template);complete(p,"Daily template "+template);}
    }
    public static void main(String[] args) {
        catalog();
        for(int stage=0;stage<StageCatalog.COUNT;stage++){Physics p=new Physics();p.reset(stage);complete(p,"Stage "+(stage+1));}
        for(int stage=0;stage<StageCatalog.COUNT;stage++){Physics p=new Physics();p.reset(stage);complete(p,"Motion stage "+(stage+1),false);}
        dailyDeterminism();
        System.out.println("PASS campaign/daily expansion ("+checks+" checks)");
    }
}
