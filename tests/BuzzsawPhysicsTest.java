package com.perfgames.ropelab;

/** Focused saw acceptance cases; existing campaign/survival suites retain their stress bounds. */
public final class BuzzsawPhysicsTest {
    private static int checks;
    private static void check(boolean ok, String message) { checks++; if (!ok) throw new AssertionError(message); }
    private static Physics fixture() {
        Physics p = new Physics(); p.resetArcade(0, ArcadeRules.BUZZSAWS); p.touchControlled(true);
        p.y = 1000; p.cameraY = 570; p.vy = 0;
        for (Physics.Rope r : p.ropes) r.cut = 12;
        return p;
    }
    private static void stepCentered(Physics p) { p.steerTouch(-p.driftVelocity()); p.step(); }
    private static Physics.SawHazard saw(Physics p, float x, float y, float velocity, float warning) {
        Physics.SawHazard s = p.sawHazards[0];
        s.active = true; s.x = x; s.y = y; s.radius = 16; s.velocityX = velocity;
        s.warningTime = warning; s.liveTime = .5f; s.followCamera = false;
        return s;
    }
    private static void warningsSweepsAndNearMisses() {
        Physics p = fixture(); Physics.SawHazard s = saw(p, p.x, p.y, 0, .1f);
        for (int i = 0; i < 12; i++) stepCentered(p);
        check(!p.lost && s.warningTime > 0 && p.soundSequence == 0, "overlapping saw warning is harmless and silent before activation");
        s.warningTime = 0; stepCentered(p);
        check(p.lost && "BUZZSAW".equals(p.deathReason) && p.soundEvent == Physics.SFX_SAW_IMPACT,
                "a live blade produces its own death and impact cue");
        check(p.soundSequence == 2, "first live contact emits motor then impact exactly once");
        long terminalSequence = p.soundSequence; for (int i = 0; i < 30; i++) p.step();
        check(p.soundSequence == terminalSequence, "terminal updates cannot repeat a saw hit cue");

        p = fixture(); s = saw(p, 100, p.y, 240f / Physics.DT, 0); stepCentered(p);
        check(p.lost && s.previousX == 100 && s.x > 300,
                "relative circular sweep detects a fast blade that crosses the player between separated endpoints");
        p = fixture(); s = saw(p, 100, p.y - (p.radius() + 16 + 1), 240f / Physics.DT, 0); stepCentered(p);
        check(!p.lost, "a full-speed pass outside the summed circular radii remains a near miss");
        p = fixture(); s = saw(p, 100, p.y - (p.radius() + 16 - 1), 240f / Physics.DT, 0); stepCentered(p);
        check(p.lost, "the neighboring grazing sweep inside summed radii is lethal");
    }
    private static int activeCount(Physics p) {
        int count = 0;
        for (Physics.FireHazard f : p.fireHazards) if (f.active) count++;
        for (Physics.BeamHazard b : p.beamHazards) if (b.active) count++;
        for (Physics.SawHazard s : p.sawHazards) if (s.active) count++;
        return count;
    }
    private static void narrowRefugesAndLockedPaths() {
        for (int difficulty = 0; difficulty < 3; difficulty++) {
            Physics p = fixture(); p.resetArcade(difficulty, ArcadeRules.BUZZSAWS);
            p.elapsed = 400; p.score = 4000; p.y = 20000; p.cameraY = p.y - 430; p.vy = 800;
            Physics.SawHazard s = null;
            for (int i = 0; i < 720 && s == null; i++) {
                for (Physics.Rope r : p.ropes) r.cut = 12;
                stepCentered(p);
                for (Physics.SawHazard candidate : p.sawHazards) if (candidate.active) s = candidate;
            }
            check(s != null && !p.lost && activeCount(p) == 1 && s.warningTime >= .79f,
                    "saw-only late-wave setup gives one readable warning even at the narrowest corridor");
            float path1 = s.pathX1, path2 = s.pathX2, warning = s.warningTime;
            float refuge = s.velocityX > 0 ? p.rightWall() - p.hazardInset() - p.radius() - 6 :
                    p.leftWall() + p.hazardInset() + p.radius() + 6;
            float clearance = (float)Math.sqrt(Physics.pointSegmentDistanceSquared(refuge, s.y,
                    path1, s.y, path2, s.y)) - s.radius;
            check(clearance > p.radius() + 8, "entire advertised sweep leaves a full-body refuge with margin");
            check(Math.abs(refuge - p.x) < (340 - 100) * warning,
                    "refuge is reachable before activation with bounded touch speed against maximum drift");
            float expectedMotorSequence = p.soundSequence + 1;
            int steps = 0;
            while (s.active && !p.lost && steps++ < 600) {
                for (Physics.Rope r : p.ropes) r.cut = 12;
                p.steerTouch(Physics.clamp((refuge - p.x) * 12 - p.driftVelocity(), -340, 340)); p.step();
                check(activeCount(p) <= 1 && s.pathX1 == path1 && s.pathX2 == path2,
                        "saw path stays locked and cannot stack another incoming attack");
            }
            check(!p.lost && !s.active && Math.abs(s.x - path2) < .01f,
                    "a real bounded dodge survives the sweep until it expires at its advertised endpoint");
            check(p.soundSequence == expectedMotorSequence && p.soundEvent == Physics.SFX_SAW_MOTOR,
                    "one natural sweep emits one motor cue, not a cue per substep");
        }
    }
    private static void resetsCampaignAndCues() {
        Physics p = fixture(); saw(p, 100, p.y, 0, 0); stepCentered(p);
        check(p.soundSequence == 1 && p.soundEvent == Physics.SFX_SAW_MOTOR, "safe saw activation emits its motor cue");
        p.reset();
        check(p.arcade && p.arcadeHazards == ArcadeRules.BUZZSAWS && p.soundSequence == 0 && p.soundEvent == 0,
                "restart retains saw-only selection and clears pending sound events");
        for (Physics.SawHazard s : p.sawHazards) check(!s.active && !s.followCamera && s.warningTime == 0,
                "restart clears live and warning saws");
        check((StageCatalog.get(3).extraHazards & ArcadeRules.BUZZSAWS) != 0,
                "a later campaign stage introduces saws alongside its existing attack");
        p.reset(3); p.touchControlled(true); p.y = p.groundY() - 600; p.cameraY = p.y - 430;
        Physics.FireHazard approachFire=p.fireHazards[0]; approachFire.active=true; approachFire.x=p.x; approachFire.y=p.y;
        Physics.BeamHazard approachBeam=p.beamHazards[0]; approachBeam.active=true; approachBeam.kind=ArcadeRules.SIDE_LASERS;
        approachBeam.x1=100;approachBeam.x2=380;approachBeam.y1=approachBeam.y2=p.y;
        saw(p, p.x, p.y, 0, 0); stepCentered(p);
        check(!p.lost && activeCount(p)==0 && p.soundSequence == 0,
                "campaign fire, beams and saws all retire before target approach instead of creating an impossible strict landing");

        for (int kind : new int[]{ArcadeRules.TOP_LASERS, ArcadeRules.LIGHTNING}) {
            p = fixture(); Physics.BeamHazard b = p.beamHazards[0];
            b.active = true; b.kind = kind; b.x1 = b.x2 = 75; b.y1 = 850; b.y2 = 1150; b.liveTime = .3f;
            for (int i = 0; i < 12; i++) stepCentered(p);
            check(!p.lost && p.soundSequence == 1 && p.soundEvent ==
                    (kind == ArcadeRules.LIGHTNING ? Physics.SFX_LIGHTNING : Physics.SFX_LASER),
                    "safe beam activation emits one family-specific cue");
        }
        p = fixture(); Physics.FireHazard f = p.fireHazards[0];
        f.active = true; f.x = 75; f.y = p.y; stepCentered(p);
        check(!p.lost && p.soundSequence == 1 && p.soundEvent == Physics.SFX_FIRE_LAUNCH,
                "a launched fireball emits one launch cue");
        f.x = p.x; f.y = p.y; stepCentered(p);
        check(p.lost && p.soundSequence == 2 && p.soundEvent == Physics.SFX_FIRE_BURN,
                "subsequent fire contact emits the burn cue without repeating launch");
        p = fixture(); p.x = p.leftWall() + p.hazardInset() + p.radius(); stepCentered(p);
        check(p.lost && "LASER".equals(p.deathReason) && p.soundSequence == 1 && p.soundEvent == Physics.SFX_LASER,
                "laser boundary death also emits its typed cue without a beam activation");
    }
    public static void main(String[] args) {
        warningsSweepsAndNearMisses(); narrowRefugesAndLockedPaths(); resetsCampaignAndCues();
        System.out.println("PASS: buzzsaw warning/live sweep/near misses, narrow-corridor reachable refuges and real dodges, locked paths/exclusive attacks, reset/campaign approach and one-shot hazard cues (" + checks + " checks)");
    }
}
