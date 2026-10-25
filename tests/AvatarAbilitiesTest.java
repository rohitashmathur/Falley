package com.perfgames.ropelab;

/** Real avatar heights/loads, projectile-only shields and Daily fairness. */
public final class AvatarAbilitiesTest {
    private static int checks;
    private static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    private static void near(float a,float b,float tolerance,String message){check(Math.abs(a-b)<=tolerance,message+": "+a+" / "+b);}
    private static Physics actor(int avatar){Physics p=new Physics();p.configureAvatar(avatar);p.reset(0);return p;}
    private static void traitsAndFirstJump(){
        float[] heights={1,1.2f,1.2f,.85f,1.35f,1,1.2f},weights={1,1,1,1.25f,.75f,1.15f,.85f};
        for(int avatar=0;avatar<7;avatar++){
            Physics p=actor(avatar);near(AvatarAbilities.jumpHeight(avatar),heights[avatar],0,"approved tap-jump height");near(p.mass(),weights[avatar],.0001f,"approved initial load");
            check(p.avatar()==avatar&&p.canJump()&&p.jump(),"selected avatar starts with an available jump");float launch=p.vy;
            near(launch*launch/(450*450),heights[avatar],.0001f,"velocity uses square root, rather than incorrectly multiplying jump height twice");
            float top=p.headTop();for(int i=0;i<180&&!p.lost;i++){p.step();top=Math.min(top,p.headTop());}
            check(!p.lost&&top>Physics.ARENA_TOP,"fresh first jump leaves ceiling clearance for avatar "+avatar);
            for(int stage=0;stage<24;stage++)if(StageCatalog.get(stage).hasModifier(StageCatalog.LOW_GRAVITY)){
                p=actor(avatar);p.reset(stage);p.jump();float minimum=p.headTop();for(int i=0;i<220&&!p.lost;i++){p.step();minimum=Math.min(minimum,p.headTop());}
                check(!p.lost&&minimum>Physics.ARENA_TOP,"Low-gravity first tap also retains ceiling room: avatar="+avatar+" stage="+stage);
            }
            p=actor(avatar);p.y=240;p.vy=0;p.jump();for(int i=0;i<200&&!p.lost;i++)p.step();check(p.lost&&"CEILING".equals(p.deathReason),"higher-jump traits never disable the lethal ceiling");
            p=actor(avatar);p.score=420;Physics human=actor(0);human.score=420;near(p.radius(),human.radius(),0,"same collision size at matched score for every avatar");near(p.mass()/human.mass(),weights[avatar],.0001f,"load trait remains independent of collision size");
            p.jump();launch=p.vy;human.jump();near(launch*launch/(human.vy*human.vy),heights[avatar],.0001f,"late-score tap-jump height stays separate from mass");
        }
        check(AvatarAbilities.normalize(-1)==0&&AvatarAbilities.normalize(100)==0,"invalid selections use Human");
    }
    private static Physics contactFixture(int avatar){
        Physics p=actor(avatar);p.touchControlled(true);p.y=2000;p.cameraY=1570;
        for(Physics.Rope r:p.ropes){r.base=100000;r.cut=12;}
        return p;
    }
    private static void step(Physics p){p.steerTouch(-p.driftVelocity()-p.nudgeVelocity()-p.windVelocity());p.step();}
    private static void projectileShields(){
        for(int avatar=0;avatar<7;avatar++){
            Physics p=contactFixture(avatar);Physics.FireHazard fire=p.fireHazards[0];fire.active=true;fire.warningTime=0;fire.x=p.x;fire.y=p.y;step(p);
            check(p.lost!=(avatar==AvatarCatalog.ROBOT),"only Robot absorbs a live fireball");if(!p.lost)check(!fire.active&&p.immunityPulse>0&&p.immunityKind==ArcadeRules.FIREBALLS,"fireball shield consumes one projectile and exposes feedback");
            for(int kind:new int[]{ArcadeRules.TOP_LASERS,ArcadeRules.SIDE_LASERS,ArcadeRules.LIGHTNING}){
                p=contactFixture(avatar);Physics.BeamHazard beam=p.beamHazards[0];beam.active=true;beam.warningTime=0;beam.kind=kind;beam.x1=p.x-50;beam.x2=p.x+50;beam.y1=beam.y2=p.y;beam.width=10;beam.liveTime=.4f;step(p);
                check(p.lost!=(avatar==AvatarCatalog.SHADOW),"only Shadow absorbs active laser/lightning beams");if(!p.lost)check(!beam.active&&p.immunityPulse>0,"beam shield consumes an overlapping attack once");
            }
            p=contactFixture(avatar);p.x=p.leftWall()+p.hazardInset()+p.radius()-1;step(p);check(p.lost,"all avatars remain vulnerable to arena side hazards");
            p=actor(avatar);p.reset(4);p.x=p.leftWall()+p.hazardInset()+p.radius()-1;step(p);check(p.lost&&"SPIKES".equals(p.deathReason),"spikes stay lethal for every avatar");
            p=actor(avatar);p.reset(8);p.x=p.leftWall()+p.hazardInset()+p.radius()-1;step(p);check(p.lost&&"FIRE".equals(p.deathReason),"fire boundary is lethal even for Robot");
            p=actor(avatar);p.reset(4);p.y=p.groundY()-p.radius();p.cameraY=p.groundY()-675;p.x=p.leftWall()+p.hazardInset()+p.radius()+5;p.vy=200;step(p);check(p.lost&&"HARD LANDING".equals(p.deathReason),"missed fatal landing stays lethal");
        }
    }
    private static void resetsAndDaily(){
        for(int avatar=0;avatar<7;avatar++){
            Physics a=actor(avatar);a.configureAvatar((avatar+1)%7);check(a.avatar()==avatar,"new selection never changes an active run");a.reset();check(a.avatar()==(avatar+1)%7,"restart applies the configured next-run selection");
            a.configureAvatar(avatar);a.resetDaily(123);Physics b=actor(0);b.resetDaily(123);check(a.avatar()==0&&a.y==310,"Daily forces Human physics and original spawn for every selected appearance");
            for(int i=0;i<240*4&&!a.lost;i++){a.touchControlled(true);b.touchControlled(true);a.steerTouch(20);b.steerTouch(20);if(i==20){a.jump();b.jump();}a.step();b.step();check(Float.floatToIntBits(a.x)==Float.floatToIntBits(b.x)&&Float.floatToIntBits(a.y)==Float.floatToIntBits(b.y)&&a.mass()==b.mass(),"Daily trajectory is bit-identical across equipped avatars");}
            a.reset();check(a.daily&&a.avatar()==0,"Daily retry remains Human");a.resetArcade(0,ArcadeRules.ALL);check(a.avatar()==avatar,"leaving Daily restores configured Arcade ability");a.reset(0);check(a.avatar()==avatar&&!a.daily&&!a.arcade,"campaign keeps the selected ability after a Daily override");
        }
    }
    private static void completeCampaignAndArcade(){
        for(int avatar=0;avatar<7;avatar++)for(boolean touch:new boolean[]{true,false}){
            for(int stage=0;stage<24;stage++){
                Physics p=actor(avatar);p.reset(stage);p.touchControlled(touch);int steps=0;
                while(!p.lost&&!p.won&&steps++<240*120){if(touch)CampaignExpansionTest.steer(p);else CampaignExpansionTest.steerMotion(p);p.step();check(Float.isFinite(p.x)&&Float.isFinite(p.y)&&Float.isFinite(p.vx)&&Float.isFinite(p.vy),"finite natural avatar campaign");}
                check(p.won&&!p.lost,"natural campaign clear avatar="+avatar+" touch="+touch+" stage="+stage+" cause="+p.deathReason+" t="+p.elapsed);
            }
            Physics p=actor(avatar);p.resetArcade(1,ArcadeRules.ALL);p.touchControlled(touch);int steps=0;
            while(!p.lost&&p.elapsed<45&&steps++<240*46){if(touch)ArcadePhysicsTest.avoid(p);else ArcadePhysicsTest.avoidMotion(p);p.step();}
            check(!p.lost&&p.elapsed>=44.9f&&p.wave>=3,"real mixed Arcade reaches Wave3 for each avatar/control: avatar="+avatar+" touch="+touch+" cause="+p.deathReason);
            System.out.printf("Avatar %d %s PASS all24 campaign + %.2fs Arcade%n",avatar,touch?"touch":"motion",p.elapsed);
        }
    }
    public static void main(String[]args){traitsAndFirstJump();projectileShields();resetsAndDaily();completeCampaignAndArcade();System.out.println("PASS: seven real avatar abilities, first-jump safety/shared hitboxes, lethal boundaries/floors, projectile shields, Daily isolation and full mixed campaign/Arcade both controls ("+checks+" checks)");}
}
