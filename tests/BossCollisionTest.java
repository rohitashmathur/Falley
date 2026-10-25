package com.perfgames.ropelab;

/** Collision follows the drawn grid/bar rectangle, including exact avatar corners and sweeps. */
public final class BossCollisionTest {
    private static int checks;
    private static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    private static void geometry(){
        check(Physics.sweptCircleRect(240,300,240,300,200,160,280,730,17),"avatar inside rectangle contacts");
        check(!Physics.sweptCircleRect(240,750,240,750,200,160,280,730,17),"no invisible wide beam endcap below rectangle");
        check(Physics.sweptCircleRect(240,747,240,747,200,160,280,730,17),"exact bottom tangent contacts by avatar radius only");
        check(!Physics.sweptCircleRect(294,744,294,744,200,160,280,730,17),"expanded rectangular corner is not a false contact outside the avatar circle");
        check(Physics.sweptCircleRect(290,740,290,740,200,160,280,730,17),"actual round avatar corner contact is detected");
        check(Physics.sweptCircleRect(160,400,320,400,200,160,280,730,17),"fast horizontal sweep cannot tunnel through a grid");
        check(Physics.sweptCircleRect(240,130,240,770,200,160,280,730,17),"fast vertical sweep cannot tunnel through a grid");
        check(!Physics.sweptCircleRect(160,760,320,760,200,160,280,730,17),"sweep below drawn rectangle remains safe");
        check(!Physics.sweptCircleRect(298,100,298,800,200,160,280,730,17),"sweep outside side plus avatar radius remains safe");
    }
    private static Physics fixture(float x,float y){
        Physics p=new Physics();p.touchControlled(true);p.x=x;p.y=y;p.vy=0;p.cameraY=0;
        for(Physics.Rope r:p.ropes){r.cut=12;r.base=100000;for(int i=0;i<Physics.Rope.N;i++)r.y[i]=r.py[i]=r.oldY[i]=100000;}
        return p;
    }
    private static void step(Physics p){p.steerTouch(-p.driftVelocity()-p.nudgeVelocity()-p.windVelocity());p.step();}
    private static void actualGrid(){
        for(float offset:new float[]{-1,2,20,60}){
            Physics p=fixture(240,730+Physics.R+offset);Physics.BeamHazard beam=p.beamHazards[0];beam.active=true;beam.bossLane=true;beam.kind=ArcadeRules.TOP_LASERS;beam.x1=beam.x2=240;beam.y1=160;beam.y2=730;beam.width=160;beam.liveTime=.4f;step(p);
            check(p.lost==(offset<0),"real boss grid has no wide capsule below its drawn bottom: offset="+offset);
        }
        Physics p=fixture(240,750);Physics.BeamHazard b=p.beamHazards[0];b.active=true;b.kind=ArcadeRules.TOP_LASERS;b.x1=b.x2=240;b.y1=160;b.y2=730;b.width=160;b.liveTime=.4f;step(p);
        check(p.lost,"ordinary nonboss beam retains its existing round endpoint contract");
    }
    private static void actualBar(){
        for(float offset:new float[]{-1,2,12,20}){
            Physics p=fixture(240,730+Physics.R+offset);Physics.SawHazard saw=p.sawHazards[0];saw.active=true;saw.bossRail=true;saw.x=240;saw.y=270;saw.railTop=160;saw.railBottom=730;saw.radius=16;saw.liveTime=.4f;step(p);
            check(p.lost==(offset<0),"real saw rail is bounded by its clipped drawing, without an extra blade-radius endcap: offset="+offset);
        }
    }
    public static void main(String[]args){geometry();actualGrid();actualBar();System.out.println("PASS: exact drawn boss rectangles, round avatar corners, swept crossing/near misses and real grid/bar end boundaries ("+checks+" checks)");}
}
