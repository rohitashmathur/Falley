package com.perfgames.ropelab;

/** Falley V2: long chapter descents, elastic ropes and a bouncy target floor. */
public final class Physics {
    public static final float W = 480, H = 820, DT = 1f / 240, R = 17;
    public static final float GRAVITY = 900, ROPE_SPACING = 170;
    public static final float GROUND_Y = 4200, TARGET_LEFT = 158, TARGET_RIGHT = 322;
    public final Rope[] ropes = new Rope[8];
    public StageCatalog.Stage stage = StageCatalog.get(0);
    public static final class FireHazard {
        public float x, y, warningTime, radius = 12;
        public boolean active;
        private float speed;
    }
    public final FireHazard[] fireHazards = new FireHazard[4];
    public float x, y, vx, vy, gx, cameraY, elapsed, flash, blast;
    public int score, brokenCount;
    public boolean lost, won;
    public float winTime, landingPulse, groundBounce;
    public String deathReason = "";
    private float jumpCooldown, nextRopeY, fireCountdown, touchVelocity, deepestY;
    private boolean touchControl;
    private java.util.Random random;

    public static final class Rope {
        public static final int N = 25;
        public final float[] x = new float[N], y = new float[N];
        public final float[] px = new float[N], py = new float[N];
        public final float[] oldX = new float[N], oldY = new float[N];
        public float base, strength, tension;
        // One removed constraint creates two visibly separated, independently simulated ends.
        public int cut = -1;
        private final int index;

        Rope(int index) { this.index = index; }
        void reset(float height, float left, float right) {
            base = height;
            strength = 1650 + (index % 3) * 80;
            cut = -1;
            tension = 0;
            for (int i = 0; i < N; i++) {
                x[i] = px[i] = oldX[i] = left + (right - left) * i / (N - 1f);
                y[i] = py[i] = oldY[i] = base + 18 * (float) Math.sin(Math.PI * i / (N - 1));
            }
        }
    }

    public Physics() {
        for (int i = 0; i < ropes.length; i++) ropes[i] = new Rope(i);
        for (int i = 0; i < fireHazards.length; i++) fireHazards[i] = new FireHazard();
        reset();
    }
    public void reset(int stageIndex) { stage = StageCatalog.get(stageIndex); reset(); }
    public void reset() {
        x = 240; y = 310; vx = vy = gx = cameraY = elapsed = flash = blast = 0;
        score = brokenCount = 0; lost = won = false; deathReason = ""; jumpCooldown = 0;
        winTime = landingPulse = groundBounce = 0;
        touchVelocity = 0;
        deepestY = y;
        nextRopeY = 450 + ropes.length * ROPE_SPACING;
        random = new java.util.Random(7283L + stage.index * 101L);
        fireCountdown = 1.5f;
        for (FireHazard fire : fireHazards) { fire.active = false; fire.warningTime = 0; }
        for (int i = 0; i < ropes.length; i++) ropes[i].reset(450 + i * ROPE_SPACING, leftWall(), rightWall());
    }
    public float groundY() { return stage.groundY; }
    public float targetLeft() { return stage.targetCenter - stage.targetWidth / 2; }
    public float targetRight() { return stage.targetCenter + stage.targetWidth / 2; }
    public float hazardInset() { return stage.hazard == StageCatalog.SPIKES ? 18 : stage.hazard == StageCatalog.FIRE ? 10 : 4; }
    public void touchControlled(boolean enabled) { touchControl = enabled; if (enabled) gx = 0; }
    public void steerTouch(float desiredVelocity) {
        touchVelocity = clamp(desiredVelocity, -340, 340);
        if (touchControl && !lost && !won) vx = touchVelocity;
    }
    public float mass() { return Math.min(6, 1 + score * .012f); }
    public float radius() { return R + (mass() - 1) * 1.1f; }
    public float leftWall() {
        float progress = clamp((Math.max(y, deepestY) - 310) / (groundY() - 310), 0, 1);
        return Math.min(214, 28 + progress * 92 + elapsed * .7f + score * .025f);
    }
    public float rightWall() { return W - leftWall(); }
    public float jumpRemaining() { return jumpCooldown; }
    public boolean jump() {
        if (lost || won || jumpCooldown > 0) return false;
        // Same impulse is less effective as the ball gets heavier, with a playability floor.
        vy = -Math.max(350, 650 / (float) Math.sqrt(mass()));
        jumpCooldown = .30f;
        flash = .65f;
        return true;
    }

    public void step() {
        if (lost) { blast = Math.max(0, blast - DT * .7f); return; }
        landingPulse = Math.max(0, landingPulse - DT);
        groundBounce = Math.max(0, groundBounce - DT * 2);
        if (won) { celebrateLanding(); return; }
        elapsed += DT;
        score = (int) (elapsed * 10) + brokenCount * 30;
        jumpCooldown = Math.max(0, jumpCooldown - DT);
        flash = Math.max(0, flash - DT * 2);
        float left = leftWall(), right = rightWall();
        for (Rope rope : ropes) {
            if (rope.base < cameraY - 240 && nextRopeY < groundY() - 320) {
                rope.reset(nextRopeY, left, right);
                nextRopeY += ROPE_SPACING;
            }
            simulateRope(rope, left, right);
        }
        vx = touchControl ? touchVelocity : clamp((vx + clamp(gx, -950, 950) * DT) * .9995f, -950, 950);
        vy = clamp(vy + GRAVITY * DT, -1100, 800);
        x += vx * DT;
        y += vy * DT;
        deepestY = Math.max(deepestY, y);
        // Walls are lethal on first overlap, including walls that move into a stationary ball.
        if (hitsSide()) { die(sideReason()); return; }
        if (y - cameraY < 160 + radius()) { y = cameraY + 160 + radius(); vy = Math.abs(vy) * .25f; }
        for (Rope rope : ropes) contact(rope);
        if (lost) return;
        contactGround();
        if (!won) simulateFire();
        // Follow downwards, stopping where the target floor remains in view.
        cameraY = Math.min(groundY() - 675, Math.max(cameraY, y - 430));
    }

    private void simulateRope(Rope r, float left, float right) {
        for (int i = 0; i < Rope.N; i++) {
            r.oldX[i] = r.x[i]; r.oldY[i] = r.y[i];
            if (i == 0 || i == Rope.N - 1) continue;
            float dx = clamp((r.x[i] - r.px[i]) * .998f, -4, 4);
            float dy = clamp((r.y[i] - r.py[i]) * .998f, -4, 4);
            r.px[i] = r.x[i]; r.py[i] = r.y[i];
            r.x[i] += dx; r.y[i] += dy + 380 * DT * DT;
        }
        float rest = (right - left) * 1.025f / (Rope.N - 1);
        for (int iteration = 0; iteration < 8; iteration++) {
            pin(r, left, right);
            for (int i = 0; i < Rope.N - 1; i++) {
                if (i == r.cut) continue;
                float dx = r.x[i + 1] - r.x[i], dy = r.y[i + 1] - r.y[i];
                float length = (float) Math.hypot(dx, dy);
                if (length < .001f) continue;
                float correction = (length - rest) / length * .42f;
                float wa = i == 0 ? 0 : (i + 1 == Rope.N - 1 ? 1 : .5f);
                float wb = i + 1 == Rope.N - 1 ? 0 : (i == 0 ? 1 : .5f);
                r.x[i] += dx * correction * wa; r.y[i] += dy * correction * wa;
                r.x[i + 1] -= dx * correction * wb; r.y[i + 1] -= dy * correction * wb;
            }
        }
        pin(r, left, right);
        float length = 0;
        for (int i = 0; i < Rope.N - 1; i++) if (i != r.cut) length += Math.hypot(r.x[i + 1] - r.x[i], r.y[i + 1] - r.y[i]);
        r.tension = clamp((length / (rest * (Rope.N - 1)) - 1) * 7, 0, 1);
    }
    private void pin(Rope r, float left, float right) {
        r.x[0] = left; r.y[0] = r.base;
        r.x[Rope.N - 1] = right; r.y[Rope.N - 1] = r.base;
    }
    private void contact(Rope r) {
        // A broken rope no longer catches the ball; its two ends keep animating.
        if (r.cut >= 0 || Math.abs(y - r.base) > 220) return;
        for (int i = 0; i < Rope.N - 1; i++) {
            float sx = r.x[i + 1] - r.x[i], sy = r.y[i + 1] - r.y[i];
            float length = sx * sx + sy * sy;
            if (length < .01f) continue;
            float t = clamp(((x - r.x[i]) * sx + (y - r.y[i]) * sy) / length, 0, 1);
            float nx = x - r.x[i] - sx * t, ny = y - r.y[i] - sy * t;
            float distance = (float) Math.hypot(nx, ny);
            if (distance >= radius() + 3) continue;
            if (distance < .001f) { nx = 0; ny = -1; distance = .001f; }
            else { nx /= distance; ny /= distance; }
            float ropeVx = ((r.x[i] - r.oldX[i]) * (1 - t) + (r.x[i + 1] - r.oldX[i + 1]) * t) / DT;
            float ropeVy = ((r.y[i] - r.oldY[i]) * (1 - t) + (r.y[i + 1] - r.oldY[i + 1]) * t) / DT;
            float normalVelocity = (vx - ropeVx) * nx + (vy - ropeVy) * ny;
            float load = mass() * (GRAVITY + Math.max(0, -normalVelocity) * .6f);
            if (ny < -.25f && vy >= 0 && load > r.strength) {
                r.cut = i;
                landingPulse = .18f;
                brokenCount++;
                score = (int) (elapsed * 10) + brokenCount * 30;
                flash = 1;
                // Rupture slows the fall slightly and kicks the exposed tips apart.
                vy *= .85f;
                if (i > 0) { r.px[i] += 2; r.py[i] -= 2; }
                if (i + 1 < Rope.N - 1) { r.px[i + 1] -= 2; r.py[i + 1] -= 2; }
                return;
            }
            float penetration = radius() + 3 - distance;
            if (!touchControl) x += nx * penetration;
            y += ny * penetration;
            if (normalVelocity < 0) {
                // Low restitution keeps gravity and deliberate touch jumps in control.
                float impulse = Math.min(1000, -normalVelocity * 1.16f);
                if (!touchControl) vx += nx * impulse;
                vy += ny * impulse;
                if (ny < -.45f && normalVelocity < -55) {
                    landingPulse = .18f;
                    // A landing naturally produces a small spring-assisted human jump.
                    vy = -Math.max(235, 325 / (float) Math.sqrt(mass()));
                    flash = Math.max(flash, .35f);
                }
                for (int k = i; k <= i + 1; k++) if (k > 0 && k < Rope.N - 1) {
                    float weight = k == i ? 1 - t : t;
                    r.px[k] += nx * impulse * DT * .18f * mass() * weight;
                    r.py[k] += ny * impulse * DT * .18f * mass() * weight;
                }
            }
        }
        if (hitsSide()) die(sideReason());
    }
    private void contactGround() {
        if (vy < 0 || y + radius() < groundY()) return;
        float impact = vy;
        y = groundY() - radius();
        vy = -Math.min(350, Math.max(90, impact * .32f));
        landingPulse = .18f;
        groundBounce = 1;
        if (x - radius() >= targetLeft() && x + radius() <= targetRight()) {
            won = true;
            winTime = 0;
            score += 100;
            y = groundY() - radius();
            gx = 0;
            vx = 0;
        }
    }
    private void celebrateLanding() {
        // Gameplay is terminal, but a short real bounce plays before the result panel.
        winTime += DT;
        gx = vx = 0;
        vy += GRAVITY * DT;
        y += vy * DT;
        if (y + radius() >= groundY() && vy > 0) {
            y = groundY() - radius();
            vy = vy < 55 ? 0 : -vy * .25f;
            groundBounce = Math.max(groundBounce, .3f);
        }
    }
    private boolean hitsSide() {
        return x - radius() <= leftWall() + hazardInset() || x + radius() >= rightWall() - hazardInset();
    }
    private String sideReason() {
        return stage.hazard == StageCatalog.LASER ? "LASER" : stage.hazard == StageCatalog.SPIKES ? "SPIKES" : "FIRE";
    }
    private void simulateFire() {
        if (!stage.fireRain) return;
        if (y > 1300 && y < groundY() - 550) {
            fireCountdown -= DT;
            if (fireCountdown <= 0) {
                for (FireHazard fire : fireHazards) if (!fire.active) {
                    float left = leftWall() + hazardInset() + 28, right = rightWall() - hazardInset() - 28;
                    fire.x = left + random.nextFloat() * (right - left);
                    fire.y = cameraY + 155;
                    fire.radius = 10 + random.nextFloat() * 4;
                    fire.warningTime = .9f;
                    fire.speed = 1100;
                    fire.active = true;
                    break;
                }
                fireCountdown = 2.4f + random.nextFloat() * .6f;
            }
        }
        for (FireHazard fire : fireHazards) if (fire.active) {
            if (fire.warningTime > 0) {
                fire.y = cameraY + 155;
                fire.warningTime = Math.max(0, fire.warningTime - DT);
                continue;
            }
            fire.speed = Math.min(1400, fire.speed + 100 * DT);
            fire.y += fire.speed * DT;
            if (Math.hypot(fire.x - x, fire.y - y) <= radius() + fire.radius) { die("FALLING FIRE"); return; }
            if (fire.y > cameraY + H + 140 || fire.y > groundY()) fire.active = false;
        }
    }
    private void die(String reason) { lost = true; deathReason = reason; blast = 1; gx = 0; }
    public static float clamp(float value, float low, float high) { return Math.max(low, Math.min(high, value)); }
}
