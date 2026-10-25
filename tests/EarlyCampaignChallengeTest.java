package com.perfgames.ropelab;

/** The opening chapter asks for a readable dodge while preserving neutral steering and an escape. */
public final class EarlyCampaignChallengeTest {
    private static int checks;
    private static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    private static Physics course(int stage,boolean touch){Physics p=new Physics();p.reset(stage);p.touchControlled(touch);return p;}
    private static void idleCannotWin(){
        for(int stage=0;stage<3;stage++)for(boolean touch:new boolean[]{false,true}){
            Physics p=course(stage,touch);float seen=-1,warning=0,aim=0;
            for(int i=0;i<240*30&&!p.lost&&!p.won;i++){
                if(touch)p.steerTouch(0);p.step();
                for(Physics.BeamHazard beam:p.beamHazards)if(beam.active&&beam.kind==ArcadeRules.TOP_LASERS){
                    if(seen<0){seen=p.elapsed;warning=beam.warningTime;aim=beam.x1;}
                    if(p.elapsed<seen+1.5f)check(beam.x1==aim&&beam.x2==aim,"opening warning stays frozen rather than tracking the player");
                }
            }
            check(p.lost&&!p.won&&"TOP LASER".equals(p.deathReason),"hands-off early course cannot auto-win in touch or motion");
            check(seen>=1.99f&&warning>=1.64f&&p.elapsed>=3.64f,"first danger allows a startup grace period plus generous visible dodge window");
        }
    }
    private static float destination(Physics p){
        float low=p.leftWall()+p.hazardInset()+p.radius()+14,high=p.rightWall()-p.hazardInset()-p.radius()-14;
        float target=Physics.clamp(p.targetCenter(),low,high);
        for(Physics.BeamHazard beam:p.beamHazards)if(beam.active&&beam.kind==ArcadeRules.TOP_LASERS&&Math.abs(target-beam.x1)<p.radius()+25)
            target=Math.abs(low-beam.x1)>Math.abs(high-beam.x1)?low:high;
        return target;
    }
    private static void boundedHumanLikeController(){
        for(int stage=0;stage<3;stage++)for(boolean touch:new boolean[]{false,true}){
            Physics p=course(stage,touch);int steps=0,noticed=-1;float command=0,firstX=p.x;boolean sawWarning=false;float greatestCommand=0,maxTravel=0;
            while(!p.lost&&!p.won&&steps++<240*60){
                boolean danger=false;for(Physics.BeamHazard beam:p.beamHazards)if(beam.active)danger=true;
                if(danger&&noticed<0){noticed=steps;sawWarning=true;}if(!danger)noticed=-1;
                // Read controls eight times per second, with a 300ms response delay to a new cue.
                if(steps%30==0&&(!danger||steps-noticed>=72)){
                    float target=destination(p);float scale=p.steeringScale()*p.ropeSteeringScale();
                    if(touch)command=Physics.clamp(((target-p.x)*6-p.driftVelocity()-p.nudgeVelocity()-p.windVelocity())/scale,-180,180);
                    else command=Physics.clamp(((target-p.x)*5-p.vx*.3f-p.nudgeVelocity()-p.motionGustVelocity()-p.windVelocity())/scale*950/Physics.MAX_MOTION_SPEED,-700,700);
                    greatestCommand=Math.max(greatestCommand,Math.abs(command));
                }
                if(touch)p.steerTouch(command);else p.gx=command;p.step();maxTravel=Math.max(maxTravel,Math.abs(p.x-firstX));
                check(Float.isFinite(p.x)&&Float.isFinite(p.y),"finite guided opening course");
            }
            check(p.won&&!p.lost,"opening is escapable with delayed, bounded guidance: stage="+stage+" touch="+touch+" cause="+p.deathReason);
            check(sawWarning&&maxTravel>12&&greatestCommand>20,"win involved warning response and visible deliberate steering");
        }
    }
    public static void main(String[]args){idleCannotWin();boundedHumanLikeController();System.out.println("PASS: hands-off opening prevention, visible grace/frozen warnings and bounded touch/motion clears ("+checks+" checks)");}
}