package com.perfgames.ropelab;

/** Stable individual rope identities and readable, deterministic mixed courses. */
public final class MixedRopeScheduleTest {
    private static int checks;
    private static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    private static void campaign(){
        for(int stage=0;stage<StageCatalog.COUNT;stage++){
            Physics a=new Physics(),b=new Physics();a.reset(stage);b.reset(stage);a.touchControlled(true);b.touchControlled(true);
            java.util.Map<Integer,Integer> seen=new java.util.TreeMap<>();
            for(int step=0;step<240*120&&!a.won&&!a.lost;step++){
                for(int i=0;i<a.ropes.length;i++){
                    Physics.Rope r=a.ropes[i],other=b.ropes[i];
                    check(r.courseIndex==other.courseIndex&&r.type==other.type,"same course and input yield stable individual rope identities");
                    Integer type=seen.put(r.courseIndex,r.type);check(type==null||type==r.type,"a visible mount never changes its ability in place");
                    if(r.courseIndex<3||r.courseIndex>=a.totalRopes()-3)check(r.type==0,"opening three and final three finite mounts remain Classic");
                    if(stage<2)check(r.type!=2,"Static is introduced after the first two courses");
                }
                CampaignExpansionTest.steer(a);CampaignExpansionTest.steer(b);a.step();b.step();
                check(Float.floatToIntBits(a.x)==Float.floatToIntBits(b.x)&&Float.floatToIntBits(a.y)==Float.floatToIntBits(b.y),"mixed schedule never consumes the hazard RNG differently on retry");
            }
            check(a.won&&!a.lost&&seen.size()==a.totalRopes(),"actual full campaign course exposes every scheduled mount");
            for(int i=1;i<a.totalRopes();i++)check(seen.get(i)==0||seen.get(i-1)==0,"Classic mount separates consecutive special ropes");
        }
    }
    private static void arcade(){
        for(int difficulty=0;difficulty<3;difficulty++)for(boolean touch:new boolean[]{true,false}){
            Physics a=new Physics(),b=new Physics();a.resetArcade(difficulty,ArcadeRules.ALL);b.resetArcade(difficulty,ArcadeRules.ALL);a.touchControlled(touch);b.touchControlled(touch);
            boolean spring=false,statik=false;float firstSpecial=Float.NaN;int warningSteps=0,liveSteps=0;
            java.util.Map<Integer,Integer> seen=new java.util.HashMap<>();
            for(int step=0;step<240*66&&!a.lost&&a.elapsed<65;step++){
                for(int i=0;i<a.ropes.length;i++){
                    Physics.Rope r=a.ropes[i],other=b.ropes[i];check(r.courseIndex==other.courseIndex&&r.type==other.type,"Arcade retry preserves mixed-rope generation");
                    Integer old=seen.put(r.courseIndex,r.type);check(old==null||old==r.type,"Arcade never converts an existing mount during a wave change");
                    if(a.elapsed<20)check(r.type==0,"all Wave 1 ropes remain Classic");
                    if(r.type!=0&&r.base-a.cameraY<Physics.H&&r.base>a.cameraY){
                        if(Float.isNaN(firstSpecial))firstSpecial=a.elapsed;spring|=r.type==1;statik|=r.type==2;
                    }
                }
                if(touch){ArcadePhysicsTest.avoid(a);ArcadePhysicsTest.avoid(b);}else{ArcadePhysicsTest.avoidMotion(a);ArcadePhysicsTest.avoidMotion(b);}
                a.step();b.step();check(Float.floatToIntBits(a.x)==Float.floatToIntBits(b.x)&&Float.floatToIntBits(a.y)==Float.floatToIntBits(b.y),"same Arcade inputs retain exact replay");
                for(Physics.BeamHazard h:a.beamHazards)if(h.active){if(h.warningTime>0)warningSteps++;else liveSteps++;}
            }
            check(!a.lost&&a.elapsed>=64.9f&&a.wave>=4,"mixed Arcade survives bounded control through four waves: difficulty="+difficulty+" touch="+touch+" t="+a.elapsed+" reason="+a.deathReason);
            check(spring&&statik&&firstSpecial>=20&&firstSpecial<25,"both special types enter the actual viewport soon after Wave 2");
            check(warningSteps>0&&liveSteps>0,"course exercise retains real hazard warnings and live attacks");
            System.out.printf("Mixed Arcade %d %s PASS first special %.2fs / finish %.2fs%n",difficulty,touch?"touch":"motion",firstSpecial,a.elapsed);
        }
    }
    private static void daily(){
        for(long seed:new long[]{0,1,2,3,4,5,7283,-1,Long.MAX_VALUE}){
            Physics p=new Physics();p.resetDaily(seed);p.touchControlled(true);
            for(int step=0;step<240*120&&!p.won&&!p.lost;step++){
                for(Physics.Rope r:p.ropes)check(r.type==0,"every initial and recycled Daily rope remains Classic");
                check(p.motionGustVelocity()==0,"Daily never receives the new gust");CampaignExpansionTest.steer(p);p.step();
            }
            check(p.won&&!p.lost,"the preserved Daily family remains completable");
        }
    }
    public static void main(String[]args){campaign();arcade();daily();System.out.println("PASS: mixed-rope stable generation, staged introduction, spacing/landing safety, exact retries, four-wave Arcade both controls and unchanged Daily ("+checks+" checks)");}
}
