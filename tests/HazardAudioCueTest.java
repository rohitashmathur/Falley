package com.perfgames.ropelab;

/** Actual fixed-step cue consumption, including terminal and pause/resume boundaries. */
public final class HazardAudioCueTest {
    private static int checks;
    private static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    public static void main(String[] args) {
        HazardAudioCue cue = new HazardAudioCue(); cue.reset(0);
        check(cue.consume(0,Physics.SFX_FIRE_LAUNCH)==0,"initial render cannot invent a fire sound");
        for(int event=1;event<=6;event++) {
            check(cue.consume(event,event)==event,"each new hazard event is available once");
            check(cue.consume(event,event)==0,"a second poll cannot repeat the same sound");
        }
        check(cue.consume(5,Physics.SFX_LASER)==0,"a stale substep event cannot rewind the cue cursor");
        check(cue.consume(7,99)==0 && cue.consume(7,Physics.SFX_SAW_IMPACT)==0,
                "an unknown event is consumed safely rather than replayed under another type");
        cue.reset(7);
        check(cue.consume(7,Physics.SFX_SAW_MOTOR)==0 && cue.consume(8,Physics.SFX_SAW_MOTOR)==Physics.SFX_SAW_MOTOR,
                "resume suppresses old cues but accepts the next real activation");
        cue.reset(0);
        check(cue.consume(1,Physics.SFX_FIRE_LAUNCH)==Physics.SFX_FIRE_LAUNCH,
                "a restarted run with a reset sequence can emit its first sound again");
        cue.reset(-100);
        check(cue.consume(-1,Physics.SFX_LASER)==0 && cue.consume(1,Physics.SFX_LASER)==Physics.SFX_LASER,
                "invalid negative sequence reset cannot enable stale playback");

        Physics p = new Physics(); p.resetArcade(0,ArcadeRules.BUZZSAWS); p.touchControlled(true);
        p.y=1000;p.cameraY=570;for(Physics.Rope r:p.ropes)r.cut=12;
        Physics.SawHazard saw=p.sawHazards[0];saw.active=true;saw.x=p.x;saw.y=p.y;saw.liveTime=.4f;
        cue.reset(p.soundSequence);p.step();
        check(p.lost && p.soundSequence==2 && cue.consume(p.soundSequence,p.soundEvent)==Physics.SFX_SAW_IMPACT,
                "two events in one lethal fixed step deliver the final impact rather than an outdated motor cue");
        int repeats=0;for(int i=0;i<240;i++){p.step();if(cue.consume(p.soundSequence,p.soundEvent)!=0)repeats++;}
        check(repeats==0,"one second of terminal animation produces no repeated hazard sounds");
        System.out.println("PASS: hazard cue edges, duplicate/stale/invalid suppression, pause/resume/restart and real fixed-step motor/impact priority ("+checks+" checks)");
    }
}
