package com.perfgames.ropelab;

/** Bosses must require a dodge, advertise immutable lanes and retain a reachable refuge. */
public final class BossChallengeTest {
    private static int checks;
    private static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    private static void command(Physics p,boolean touch,float target){
        target=Physics.clamp(target,p.leftWall()+p.hazardInset()+p.radius()+14,p.rightWall()-p.hazardInset()-p.radius()-14);
        float scale=p.steeringScale()*p.ropeSteeringScale();
        if(touch)p.steerTouch(Physics.clamp(((target-p.x)*6-p.driftVelocity()-p.nudgeVelocity()-p.windVelocity())/scale,-180,180));
        else p.gx=Physics.clamp(((target-p.x)*6-p.vx*.3f-p.nudgeVelocity()-p.motionGustVelocity()-p.windVelocity())/scale*950/Physics.MAX_MOTION_SPEED,-700,700);
    }
    private static void centerCampingLoses(){
        for(int stage:new int[]{3,7})for(boolean touch:new boolean[]{true,false}){
            Physics p=new Physics();p.reset(stage);p.touchControlled(touch);boolean warning=false;float first=-1;
            for(int step=0;step<240*90&&!p.lost&&!p.won;step++){
                command(p,touch,240);p.step();
                if(p.bossWarning>0){warning=true;if(first<0)first=p.elapsed;check(!p.lost,"warning cannot kill the centered player before activation");}
            }
            check(p.lost&&!p.won&&warning&&p.elapsed-first>=1.54f,"a real natural centered descent must dodge the boss: stage="+stage+" touch="+touch+" cause="+p.deathReason);
            check(stage==7?"BUZZSAW".equals(p.deathReason):p.deathReason.contains("LASER"),"center camping dies to the advertised boss, not an unrelated wall/floor");
        }
    }
    private static void delayedVisibleDodges(){
        for(int stage:new int[]{3,7})for(int avatar:new int[]{0,3,4})for(boolean touch:new boolean[]{true,false}){
            Physics p=new Physics();p.configureAvatar(avatar);p.reset(stage);p.touchControlled(touch);
            int oldPhase=0,noticed=-1;float target=240,heldCommand=0;int live=0;boolean right=false,left=false;float path1=0,path2=0,width=0;
            for(int step=0;step<240*120&&!p.won&&!p.lost;step++){
                if(p.bossPhase!=oldPhase){
                    oldPhase=p.bossPhase;noticed=step;
                    if(stage==7){Physics.SawHazard saw=p.sawHazards[0];path1=saw.pathX1;path2=saw.pathX2;check(saw.bossRail&&saw.warningTime>1.5f,"Saw finale advertises a mounted full-height blade");}
                    else{Physics.BeamHazard beam=p.beamHazards[0];path1=beam.x1;path2=beam.x2;width=beam.width;check(beam.bossLane&&beam.warningTime>1.5f,"Laser finale advertises its occupied grid lane");}
                }
                // A visible cue, 300ms reaction and 8Hz input; no world/velocity tampering.
                if(step%30==0){
                    if(noticed<0||step-noticed>=72)target=p.bossActive&&p.bossPhase>0?p.bossSafeX:p.targetCenter();
                    command(p,touch,target);heldCommand=touch?p.steeringVelocity():p.gx;
                }
                if(touch)p.steerTouch(heldCommand);else p.gx=heldCommand;
                p.step();
                if(p.bossActive){right|=p.x>260;left|=p.x<220;}
                if(p.bossPhase==oldPhase&&noticed>=0){
                    if(stage==7){Physics.SawHazard saw=p.sawHazards[0];if(saw.active){check(saw.pathX1==path1&&saw.pathX2==path2,"saw path never tracks the player after warning");check(Math.abs(saw.railTop-p.cameraY-160)<4&&Math.abs(saw.railBottom-p.cameraY-730)<4,"drawn rail covers the same camera-relative lethal height");if(saw.warningTime<=0)live++;}}
                    else{Physics.BeamHazard beam=p.beamHazards[0];if(beam.active){check(beam.x1==path1&&beam.x2==path2&&beam.width==width,"laser lane never tracks the player after warning");if(beam.warningTime<=0)live++;}}
                }
            }
            check(p.won&&!p.lost&&p.bossPhase>=2&&live>0&&right&&left,"300ms-delayed bounded visible dodges clear alternating boss lanes: stage="+stage+" avatar="+avatar+" touch="+touch+" cause="+p.deathReason+" t="+p.elapsed+" x="+p.x+" y="+p.y+" target="+target+" safe="+p.bossSafeX+" phase="+p.bossPhase+" left="+p.leftWall());
        }
    }
    private static void sideEmitters(){
        for(int stage:new int[]{11,15,19,23}){
            Physics p=new Physics();p.reset(stage);p.touchControlled(true);boolean sawSource=false;
            for(int step=0;step<240*100&&!p.lost&&!p.won;step++){
                CampaignExpansionTest.steer(p);p.step();
                if(p.bossWarning>0){
                    sawSource=true;check(p.bossSourceSide==-1||p.bossSourceSide==1,"later bosses emit from a side");check(p.bossSourceKind!=ArcadeRules.TOP_LASERS,"later side casters do not spawn a disconnected overhead laser");
                    for(Physics.FireHazard f:p.fireHazards)if(f.active&&f.warningTime>0)check(f.x==p.bossSourceX&&Math.abs(f.y-p.bossSourceY)<.001f&&f.velocityX*p.bossSourceSide<0,"fireball starts at the visible side muzzle and travels inward");
                    for(Physics.BeamHazard b:p.beamHazards)if(b.active&&b.kind==ArcadeRules.SIDE_LASERS)check(b.x1==p.bossSourceX&&Math.abs(b.y1-p.bossSourceY)<.001f,"side laser starts at the side muzzle");
                    for(Physics.SawHazard s:p.sawHazards)if(s.active&&s.warningTime>0)check(s.x==p.bossSourceX&&Math.abs(s.y-p.bossSourceY)<.001f,"side blade starts at the side muzzle");
                }
            }
            check(p.won&&!p.lost&&sawSource,"later side-emitter boss still completes naturally at stage "+stage+" cause="+p.deathReason);
        }
    }
    public static void main(String[]args){centerCampingLoses();delayedVisibleDodges();sideEmitters();System.out.println("PASS: natural center-camping failures, real boss warnings, 300ms/8Hz bounded alternating dodges and matching side emission origins ("+checks+" checks)");}
}
