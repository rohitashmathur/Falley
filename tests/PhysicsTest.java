package com.perfgames.ropelab;

/** Deterministic play scenarios; runs on a desktop JDK without Android. */
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
        Physics.Rope r = p.ropes[0];
        p.x = r.x[Physics.Rope.N / 2];
        p.y = r.y[Physics.Rope.N / 2] - p.radius() - 5;
        p.vx = 0;
        p.vy = 80;
    }
    private static void scoringAndWeight() {
        Physics p = new Physics();
        float initialMass = p.mass();
        run(p, 240);
        check(!p.lost && p.score >= 9 && p.score <= 11, "surviving one second earns about 10 points");
        check(p.mass() > initialMass, "survival score increases ball mass");
        p.score = 100;
        float mediumMass = p.mass();
        p.score = 300;
        check(p.mass() > mediumMass, "larger score increases mass");
        p.score = 100000;
        check(p.mass() <= 6.01f && p.mass() >= 5.9f, "mass is bounded at six units");
        p.reset();
        check(p.mass() == initialMass && p.score == 0 && p.brokenCount == 0, "restart clears score and weight");
    }
    private static void lightCatchAndHeavyCut() {
        Physics light = new Physics();
        positionAboveFirstRope(light);
        float lightInitialY = light.y;
        run(light, 100);
        check(!light.lost && light.brokenCount == 0 && light.ropes[0].cut == -1,
                "a low-weight gentle landing keeps the rope intact");
        check(light.y < lightInitialY + 50, "the intact rope supports or bounces the light ball");

        Physics heavy = new Physics();
        heavy.elapsed = 30;
        heavy.score = 300;
        positionAboveFirstRope(heavy);
        float base = heavy.ropes[0].base;
        int scoreBefore = heavy.score;
        for (int i = 0; i < 120 && heavy.ropes[0].cut == -1; i++) heavy.step();
        Physics.Rope broken = heavy.ropes[0];
        check(broken.cut >= 0 && broken.cut < Physics.Rope.N - 1, "heavy load cuts a rope segment");
        check(heavy.brokenCount == 1 && heavy.score >= scoreBefore + 30, "one rope break awards 30 bonus points");
        int cut = broken.cut;
        int count = heavy.brokenCount;
        heavy.y = base - 150;
        heavy.vx = heavy.vy = 0;
        run(heavy, 30);
        check(broken.cut == cut && broken.base == base, "a cut remains broken while the rope is visible");
        check(heavy.brokenCount == count, "the same cut does not score again without another contact");
    }
    private static void narrowingAndSideDeath() {
        Physics p = new Physics();
        float initialGap = p.rightWall() - p.leftWall();
        float previous = initialGap;
        for (int second = 1; second <= 150; second++) {
            p.elapsed = second;
            p.score = second * 10;
            float gap = p.rightWall() - p.leftWall();
            check(gap <= previous + .01f, "side walls narrow monotonically");
            check(gap >= 37.99f, "side walls stop at the terminal safety bound");
            previous = gap;
        }
        check(previous < initialGap - 80, "difficulty visibly narrows the arena");
        p.elapsed = 100000;
        p.score = 1000000;
        float finalGap = p.rightWall() - p.leftWall();
        check(finalGap >= 37.99f && finalGap <= 40,
                "late-run terminal wall gap is smaller than a heavy ball's diameter");

        p.reset();
        p.x = p.leftWall() + p.radius() - .5f;
        p.step();
        check(p.lost && p.blast > 0, "left sideline contact kills and blasts the ball");
        check(p.deathReason != null && !p.deathReason.isEmpty(), "side death has an explanation");
        float deadX = p.x, deadY = p.y;
        check(!p.jump(), "jump input is ignored after death");
        run(p, 100);
        check(p.x == deadX && p.y == deadY, "dead gameplay state cannot resume moving");
        p.reset();
        p.x = p.rightWall() - p.radius() + .5f;
        p.step();
        check(p.lost && p.blast > 0, "right sideline contact kills rather than reflects the ball");
        p.reset();
        check(!p.lost && p.blast == 0 && p.cameraY == 0 && p.elapsed == 0,
                "restart clears death, blast, camera and elapsed difficulty");
        for (Physics.Rope rope : p.ropes) check(rope.cut == -1, "restart repairs all ropes");
    }
    private static void jumpAndSteering() {
        Physics p = new Physics();
        float startY = p.y;
        check(p.jump() && p.vy < 0, "tap jump creates a real upward impulse");
        float lightJump = -p.vy;
        check(!p.jump(), "immediate repeated jump is blocked by cooldown");
        run(p, 30);
        check(p.y < startY - 5, "the ball visibly rises following jump input");
        run(p, 45);
        check(p.jump(), "jump is available again after the short cooldown");

        Physics heavy = new Physics();
        heavy.score = 300;
        check(heavy.jump() && -heavy.vy < lightJump, "higher mass reduces the jump impulse");
        Physics right = new Physics();
        right.vx = 0;
        right.gx = 450;
        run(right, 30);
        check(right.x > Physics.W / 2 + 2 && right.vx > 0, "positive steering moves the ball right");
        Physics left = new Physics();
        left.vx = 0;
        left.gx = -450;
        run(left, 30);
        check(left.x < Physics.W / 2 - 2 && left.vx < 0, "negative steering moves the ball left");
    }
    private static void cameraAndRecycling() {
        Physics p = new Physics();
        float oldBase = p.ropes[0].base;
        p.ropes[0].cut = 12;
        p.x = Physics.W / 2;
        p.y = 5000;
        p.vx = 0;
        p.vy = 300;
        // The camera jumps immediately; allow several frames for sequential rope recycling.
        run(p, 30);
        check(!p.lost && p.cameraY > 3000, "camera follows deep descent without floor death");
        check(p.ropes[0].base > oldBase && p.ropes[0].cut == -1,
                "a rope that has left the screen recycles below and repairs its cut");
        int ropesAhead = 0;
        for (Physics.Rope rope : p.ropes) if (rope.base > p.cameraY) ropesAhead++;
        check(ropesAhead >= 4, "recycling supplies a continuing stack of ropes");
    }
    private static void eventualCrush() {
        for (int jumpEvery : new int[] {0, 72, 120, 240}) {
            Physics p = new Physics();
            for (int i = 0; i < 240 * 100 && !p.lost; i++) {
                if (jumpEvery > 0 && i % jumpEvery == 0) p.jump();
                p.step();
            }
            check(p.lost && p.blast > 0,
                    "closing walls eventually end a centered passive or repeated-jump run: cadence=" + jumpEvery);
        }
    }
    private static void stress() {
        Physics p = new Physics();
        java.util.Random random = new java.util.Random(7);
        int resets = 0;
        for (int i = 0; i < 100000; i++) {
            if (p.lost) { p.reset(); resets++; }
            if (i % 95 == 0) p.jump();
            if (i % 180 == 0) p.gx = (random.nextFloat() * 2 - 1) * 1300;
            if (i % 10000 == 9999) { p.score = 450; p.elapsed = 45; }
            p.step();
            check(Float.isFinite(p.x) && Float.isFinite(p.y) && Float.isFinite(p.vx)
                    && Float.isFinite(p.vy) && Float.isFinite(p.cameraY), "mixed-input stress ball stays finite");
            check(p.mass() >= 1 && p.mass() <= 6.01f && p.radius() > 0, "stress weight and radius remain bounded");
            for (Physics.Rope rope : p.ropes) {
                check(Float.isFinite(rope.tension) && Float.isFinite(rope.base), "stress rope properties stay finite");
                for (int k = 0; k < Physics.Rope.N; k++)
                    check(Float.isFinite(rope.x[k]) && Float.isFinite(rope.y[k]), "stress rope nodes stay finite");
            }
        }
        check(resets > 0, "stress scenarios exercise wall deaths and restarts");
    }
    public static void main(String[] args) {
        scoringAndWeight();
        lightCatchAndHeavyCut();
        narrowingAndSideDeath();
        jumpAndSteering();
        cameraAndRecycling();
        eventualCrush();
        stress();
        System.out.println("PASS: survival score/mass, light catch/heavy cut, persistent cuts, narrowing/lethal walls, jump/cooldown, steering, restart, camera/recycling, 100000 seeded stability steps (" + checks + " checks)");
    }
}
