package com.perfgames.ropelab;

/** Deterministic Falley V1 play scenarios on a desktop JDK, without Android. */
public final class PhysicsTest {
    private static int checks;
    private static void check(boolean ok, String message) {
        checks++;
        if (!ok) throw new AssertionError(message);
    }
    private static void run(Physics p, int steps) {
        for (int i = 0; i < steps; i++) p.step();
    }
    private static void positionAboveFirstRope(Physics p) {
        Physics.Rope rope = p.ropes[0];
        p.x = rope.x[Physics.Rope.N / 2];
        p.y = rope.y[Physics.Rope.N / 2] - p.radius() - 5;
        p.vx = 0;
        p.vy = 80;
    }
    private static void scoringAndWeight() {
        Physics p = new Physics();
        float initialMass = p.mass();
        run(p, 240);
        check(!p.lost && !p.won && p.score >= 9 && p.score <= 11, "one second of play earns about 10 points");
        check(p.mass() > initialMass, "score increases physical weight");
        p.score = 100;
        float mediumMass = p.mass();
        p.score = 300;
        check(p.mass() > mediumMass, "larger score increases weight");
        p.score = 100000;
        check(p.mass() <= 6.01f && p.mass() >= 5.9f, "weight has a six-unit cap");
        p.reset();
        check(p.mass() == initialMass && p.score == 0 && p.brokenCount == 0, "restart clears score and weight");
    }
    private static void landingAnimationAndHeavyCut() {
        Physics light = new Physics();
        positionAboveFirstRope(light);
        for (int i = 0; i < 120 && light.landingPulse == 0; i++) light.step();
        check(!light.lost && light.ropes[0].cut == -1 && light.brokenCount == 0,
                "a gentle light landing leaves the rope intact");
        check(light.landingPulse > 0 && light.vy < -100,
                "an intact-rope landing emits a crouch pulse and a physical automatic jump");
        float landingY = light.y;
        run(light, 24);
        check(light.y < landingY - 5, "the automatic jump visibly raises the character");
        run(light, 30);
        check(light.landingPulse == 0, "landing animation pulse ends after its brief pose window");

        Physics heavy = new Physics();
        heavy.elapsed = 30;
        heavy.score = 300;
        positionAboveFirstRope(heavy);
        float base = heavy.ropes[0].base;
        int scoreBefore = heavy.score;
        for (int i = 0; i < 120 && heavy.ropes[0].cut == -1; i++) heavy.step();
        Physics.Rope broken = heavy.ropes[0];
        check(broken.cut >= 0 && broken.cut < Physics.Rope.N - 1, "heavy load cuts a rope segment");
        check(heavy.brokenCount == 1 && heavy.score >= scoreBefore + 30, "one cut earns a 30-point bonus");
        int cut = broken.cut;
        heavy.y = base - 150;
        heavy.vx = heavy.vy = 0;
        run(heavy, 30);
        check(broken.cut == cut && broken.base == base && heavy.brokenCount == 1,
                "a cut remains broken and does not repeatedly award points");
    }
    private static void wallsJumpAndReset() {
        Physics p = new Physics();
        float previousGap = p.rightWall() - p.leftWall();
        for (int second = 1; second <= 150; second++) {
            p.elapsed = second;
            p.score = second * 10;
            float gap = p.rightWall() - p.leftWall();
            check(gap <= previousGap + .01f && gap > 0, "side walls steadily narrow without crossing");
            previousGap = gap;
        }
        check(previousGap < Physics.R * 2 + 12, "stalling eventually leaves too little room for a heavy character");
        p.reset();
        float startY = p.y;
        check(p.jump() && p.vy < 0, "manual touch jump creates real upward motion");
        float lightImpulse = -p.vy;
        check(!p.jump(), "the short jump cooldown prevents immediate repeats");
        run(p, 30);
        check(p.y < startY - 5, "manual jump visibly moves upward");
        run(p, 45);
        check(p.jump(), "manual jump becomes available after its cooldown");
        p.reset();
        p.score = 300;
        check(p.jump() && -p.vy < lightImpulse, "weight makes a manual jump less effective");
        p.reset();
        p.gx = 450;
        run(p, 30);
        check(p.x > Physics.W / 2 + 2 && p.vx > 0, "positive steering moves right");
        p.reset();
        p.gx = -450;
        run(p, 30);
        check(p.x < Physics.W / 2 - 2 && p.vx < 0, "negative steering moves left");
        for (boolean right : new boolean[] {false, true}) {
            p.reset();
            p.x = right ? p.rightWall() - p.radius() + .5f : p.leftWall() + p.radius() - .5f;
            p.step();
            check(p.lost && !p.won && p.blast > 0 && !p.deathReason.isEmpty(), "each sideline kills with a blast");
            float deadX = p.x, deadY = p.y;
            check(!p.jump(), "dead gameplay cannot jump");
            run(p, 100);
            check(p.x == deadX && p.y == deadY, "dead gameplay does not restart moving");
        }
        p.reset();
        check(!p.lost && !p.won && p.blast == 0 && p.cameraY == 0 && p.elapsed == 0,
                "restart clears terminal state, camera and difficulty");
    }
    private static Physics landInTarget() {
        Physics p = new Physics();
        p.y = Physics.GROUND_Y - p.radius() - 1;
        p.vy = 500;
        p.step();
        check(p.won && !p.lost, "a descending central target landing wins");
        return p;
    }
    private static void finiteCourseWinAndPersistentLayout() {
        Physics p = new Physics();
        float[] bases = new float[p.ropes.length];
        for (int i = 0; i < bases.length; i++) bases[i] = p.ropes[i].base;
        int steps = 0;
        while (!p.lost && !p.won && steps++ < 240 * 60) {
            // Real, bounded steering input corrects recoil; no state teleport or cuts are injected.
            p.gx = Physics.clamp((Physics.W / 2 - p.x) * 18 - p.vx * 5, -450, 450);
            p.step();
        }
        check(p.won && !p.lost, "the complete finite course can be won through gravity, rope progression and steering");
        check(p.brokenCount > 0 && p.brokenCount <= p.ropes.length, "a complete course encounters and cuts a finite rope stack");
        check(p.score >= (int)(p.elapsed * 10) + p.brokenCount * 30 + 100, "target landing awards its 100-point bonus once");
        check(p.y + p.radius() <= Physics.GROUND_Y + .01f, "winning feet cannot overshoot the target floor");
        check(p.cameraY <= Physics.GROUND_Y - 565 + .01f && p.cameraY > 500,
                "the descent camera moves down and stops with the target floor visible");
        for (int i = 0; i < bases.length; i++)
            check(p.ropes[i].base == bases[i], "rope mounts remain fixed through the finite course");
        int cut = p.ropes[0].cut;
        check(cut >= 0, "the first rope remains cut after the character reaches the ground");
        run(p, 240);
        check(p.ropes[0].cut == cut, "a rope never recycles or repairs itself during win celebration");
    }
    private static void offTargetBounceAndRecovery() {
        Physics p = new Physics();
        p.x = 100;
        p.y = Physics.GROUND_Y - p.radius() - 1;
        p.vy = 600;
        p.step();
        check(!p.won && !p.lost && p.vy < 0 && p.groundBounce > 0,
                "landing outside the target bounces without a false win or floor death");
        check(p.y + p.radius() <= Physics.GROUND_Y + .01f, "off-target landing respects the floor surface");
        p.gx = 450;
        for (int i = 0; i < 480 && !p.lost && !p.won; i++) p.step();
        check(p.won && !p.lost, "steering after an off-target bounce can recover into the target");
        for (boolean right : new boolean[] {false, true}) {
            p.reset();
            p.x = right ? Physics.TARGET_RIGHT - p.radius() + 1 : Physics.TARGET_LEFT + p.radius() - 1;
            p.y = Physics.GROUND_Y - p.radius();
            p.vy = 100;
            p.step();
            check(!p.won && !p.lost, "a character partially outside either target edge cannot win");
        }
        p.reset();
        p.x = 100;
        p.y = Physics.GROUND_Y + 5000;
        p.vy = 10000;
        p.step();
        check(!p.lost && !p.won && p.y + p.radius() <= Physics.GROUND_Y + .01f,
                "an extreme descending overshoot is clamped back to the floor");
        check(p.cameraY <= Physics.GROUND_Y - 565 + .01f, "floor overshoot cannot push the camera beyond its cap");
    }
    private static void terminalWinAndCelebration() {
        Physics p = landInTarget();
        int wonScore = p.score;
        float wonElapsed = p.elapsed, wonX = p.x, floorY = p.y;
        check(!p.jump(), "manual jump input is ignored after winning");
        for (int i = 0; i < 60; i++) { p.gx = 10000; p.step(); }
        check(p.y < floorY - 1 && p.winTime > .2f && p.groundBounce >= 0,
                "a win continues a real damped celebration bounce");
        check(p.won && !p.lost && p.score == wonScore && p.elapsed == wonElapsed && p.x == wonX && p.gx == 0,
                "winning freezes scoring and gameplay steering while celebration runs");
        run(p, 1000);
        check(p.y + p.radius() <= Physics.GROUND_Y + .01f && Float.isFinite(p.vy), "celebration remains bounded at the floor");
        check(p.score == wonScore && p.elapsed == wonElapsed, "a long celebration cannot repeatedly award points");
        p.reset();
        check(!p.won && !p.lost && p.winTime == 0 && p.landingPulse == 0 && p.groundBounce == 0 && p.score == 0,
                "restart fully clears a finished win and its animations");
        for (Physics.Rope rope : p.ropes) check(rope.cut == -1, "restart repairs each fixed rope");
    }
    private static void stallingAndStress() {
        Physics stalling = new Physics();
        for (int i = 0; i < 240 * 100 && !stalling.lost && !stalling.won; i++) {
            if (i % 120 == 0) stalling.jump();
            stalling.step();
        }
        check(stalling.lost && !stalling.won, "repeated air jumps cannot stall forever above the finite goal");
        Physics p = new Physics();
        java.util.Random random = new java.util.Random(7);
        int deaths = 0, victories = 0;
        for (int i = 0; i < 100000; i++) {
            if (p.lost) { p.reset(); deaths++; }
            if (p.won && p.winTime > 1.2f) { p.reset(); victories++; }
            if (i % 6000 == 0) {
                p.reset(); p.y = Physics.GROUND_Y - p.radius() - 1; p.vy = 1450;
            } else {
                if (i % 95 == 0) p.jump();
                if (i % 180 == 0) p.gx = (random.nextFloat() * 2 - 1) * 1300;
            }
            p.step();
            check(Float.isFinite(p.x) && Float.isFinite(p.y) && Float.isFinite(p.vx) && Float.isFinite(p.vy)
                    && Float.isFinite(p.cameraY) && Float.isFinite(p.winTime)
                    && Float.isFinite(p.landingPulse) && Float.isFinite(p.groundBounce), "all run and animation state stays finite");
            check(!(p.won && p.lost) && p.cameraY >= 0 && p.cameraY <= Physics.GROUND_Y - 565 + .01f,
                    "stress run preserves exclusive results and a bounded camera");
            check(p.mass() >= 1 && p.mass() <= 6.01f && p.radius() > 0, "stress weight and radius stay bounded");
            for (Physics.Rope rope : p.ropes) {
                check(Float.isFinite(rope.tension) && Float.isFinite(rope.base), "stress rope properties stay finite");
                for (int k = 0; k < Physics.Rope.N; k++)
                    check(Float.isFinite(rope.x[k]) && Float.isFinite(rope.y[k]), "stress rope nodes stay finite");
            }
        }
        check(deaths > 0 && victories > 0, "stress exercises losses, wins, celebration and restarts");
    }
    public static void main(String[] args) {
        scoringAndWeight();
        landingAnimationAndHeavyCut();
        wallsJumpAndReset();
        finiteCourseWinAndPersistentLayout();
        offTargetBounceAndRecovery();
        terminalWinAndCelebration();
        stallingAndStress();
        System.out.println("PASS: complete finite course win, landing/automatic jump, weight/cuts, fixed ropes, walls, manual controls, off-target recovery, target bounds, win freeze/celebration, floor/camera caps, restart, 100000 seeded stability steps (" + checks + " checks)");
    }
}
