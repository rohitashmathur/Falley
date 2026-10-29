package com.perfgames.ropelab;

/** Falley: elastic descents and an endless, configurable survival skirmish. */
public final class Physics {
    public static final float W = 480, H = 820, DT = 1f / 240, R = 17;
    public static final float GRAVITY = 900, ROPE_SPACING = 170;
    public static final float MAX_MOTION_SPEED = 240;
    public static final int MAX_JUMPS = 2;
    public static final float BURST_DROP = 45, BURST_RECOVERY_TIME = .90f;
    public static final float ARENA_TOP = 160;
    private static final float FIRST_ROPE_Y = 450, FLOOR_ROPE_CLEARANCE = 320;
    public static final float GROUND_Y = 4200, TARGET_LEFT = 158, TARGET_RIGHT = 322;
    public static final int SFX_FIRE_LAUNCH = 1, SFX_FIRE_BURN = 2, SFX_LASER = 3,
            SFX_LIGHTNING = 4, SFX_SAW_MOTOR = 5, SFX_SAW_IMPACT = 6;
    public long soundSequence;
    public interface RopeObserver { void encountered(int index,int type,boolean cut); }
    public RopeObserver ropeObserver;
    public int soundEvent;
    public final Rope[] ropes = new Rope[8];
    public StageCatalog.Stage stage = StageCatalog.get(0);
    public static final class FireHazard {
        public float x, y, warningTime, radius = 12, velocityX, screenY = 155;
        public boolean active;
        private float speed;
        public float verticalSpeed(){return speed;}
        private boolean activated;
    }
    public final FireHazard[] fireHazards = new FireHazard[4];
    public static final class BeamHazard {
        public float x1, y1, x2, y2, warningTime, liveTime = .4f, width = 10;
        public int kind;
        public boolean active, followCamera, bossLane;
        private float screenY1, screenY2;
        private boolean activated;
    }
    public final BeamHazard[] beamHazards = new BeamHazard[3];
    public static final class SawHazard {
        public float x, y, previousX, previousY, pathX1, pathX2, radius = 16,
                warningTime, liveTime, velocityX;
        public boolean active, followCamera, bossRail;
        public float railTop, railBottom;
        private float screenY;
        private boolean activated;
    }
    public final SawHazard[] sawHazards = new SawHazard[2];
    public boolean arcade, daily;
    /** Static world-space mechanisms, generated before input is read. */
    public static final class WindZone { public float top, bottom, strength; public boolean active; }
    public static final class BouncePad { public float x, y, width = 76, cooldown; public boolean active; }
    public static final class SlowPickup { public float x, y; public boolean active, collected; }
    public final WindZone[] windZones = {new WindZone(), new WindZone(), new WindZone()};
    public final BouncePad[] bouncePads = {new BouncePad(), new BouncePad(), new BouncePad()};
    public final SlowPickup[] slowPickups = {new SlowPickup(), new SlowPickup(), new SlowPickup()};
    public float slowTimeRemaining, landingX, landingTargetCenter, landingTargetWidth;
    /** Boss cues stay visible before any existing hazard becomes lethal. */
    public int bossPhase;
    public float bossWarning, bossLive, bossSafeX, bossY;
    public boolean bossActive;
    /** Mouth/nozzle world point. Rig follows the camera; its advertised X lane never homes. */
    public float bossSourceX,bossSourceY;
    public int bossSourceSide=-1,bossSourceKind;
    private float bossSourceScreenY=270;
    private int configuredAvatar=AvatarCatalog.HUMAN,activeAvatar=AvatarCatalog.HUMAN;
    public float immunityPulse;
    public int immunityKind;
    public void configureAvatar(int avatar){configuredAvatar=AvatarAbilities.normalize(avatar);}
    public int avatar(){return daily?AvatarCatalog.HUMAN:activeAvatar;}

    public int arcadeDifficulty = 1, arcadeHazards = ArcadeRules.ALL, wave = 1;
    public float x, y, vx, vy, gx, cameraY, elapsed, flash, blast;
    public int score, brokenCount;
    public boolean lost, won;
    public float winTime, landingPulse, groundBounce;
    public String deathReason = "";
    private float jumpCooldown, nextRopeY, attackCountdown, touchVelocity, deepestY;
    private float burstStartY, burstPeakY, lastJumpTime;
    private int attackCount, jumpsLeft, manualJumps;
    private boolean touchControl;
    private java.util.Random random;
    private long dailySeed;
    private float dt = DT, bossCountdown;
    /** Type of the most recent intact supporting rope, for feedback only. */
    public int lastSupportRope=RopeCatalog.CLASSIC;
    private float staticCurrentTime;
    /** Short cosmetic feedback/damping pulse. Static's next-jump charge lasts until consumed. */
    public float ropeAbilityTime;
    public boolean staticJumpReady;
    public float ropeSteeringScale(){return staticCurrentTime>0?.84f:1;}

    public static final class Rope {
        public static final int N = 25;
        public final float[] x = new float[N], y = new float[N];
        public final float[] px = new float[N], py = new float[N];
        public final float[] oldX = new float[N], oldY = new float[N];
        public float base, strength, tension, anchorStress;
        /** Stable world mount identity; type is assigned before its rope becomes visible. */
        public int courseIndex, type=RopeCatalog.CLASSIC;
        public boolean fragile;
        // One removed constraint creates two visibly separated, independently simulated ends.
        public int cut = -1;
        private final int index;

        Rope(int index) { this.index = index; }
        void reset(float height, float left, float right) {
            base = height;
            strength = 1650 + (index % 3) * 80;
            cut = -1;
            type=RopeCatalog.CLASSIC;
            tension = 0;
            anchorStress = 0; fragile = false;
            for (int i = 0; i < N; i++) {
                x[i] = px[i] = oldX[i] = left + (right - left) * i / (N - 1f);
                y[i] = py[i] = oldY[i] = base + 18 * (float) Math.sin(Math.PI * i / (N - 1));
            }
        }
    }

    public Physics() {
        for (int i = 0; i < ropes.length; i++) ropes[i] = new Rope(i);
        for (int i = 0; i < fireHazards.length; i++) fireHazards[i] = new FireHazard();
        for (int i = 0; i < beamHazards.length; i++) beamHazards[i] = new BeamHazard();
        for (int i = 0; i < sawHazards.length; i++) sawHazards[i] = new SawHazard();
        reset();
    }
    public void reset(int stageIndex) { arcade = daily = false; stage = StageCatalog.get(stageIndex); reset(); }
    public void resetDaily(long seed) {
        daily = true; arcade = false; dailySeed = seed; stage = StageCatalog.daily(seed); reset();
    }
    public void resetArcade(int difficulty, int mask) {
        arcade = true; daily = false; arcadeDifficulty = ArcadeRules.clampDifficulty(difficulty);
        arcadeHazards = ArcadeRules.normalizeMask(mask); stage = StageCatalog.get(arcadeDifficulty * 4);
        reset();
    }
    public void reset() {
        activeAvatar=daily?AvatarCatalog.HUMAN:configuredAvatar;
        float extraHeight=Math.max(0,AvatarAbilities.jumpHeight(activeAvatar)*450*450/(2*GRAVITY*gravityScale())-450*450/(2*GRAVITY));
        x = 240; y = 310+extraHeight; vx = vy = gx = cameraY = elapsed = flash = blast = 0;
        immunityPulse=0;immunityKind=0;
        score = brokenCount = 0; lost = won = false; deathReason = ""; jumpCooldown = 0;
        jumpsLeft = maxBurstJumps();
        manualJumps = 0; burstStartY = burstPeakY = y; lastJumpTime = -BURST_RECOVERY_TIME;
        winTime = landingPulse = groundBounce = 0;
        touchVelocity = 0;
        deepestY = y;
        nextRopeY = FIRST_ROPE_Y + ropes.length * ROPE_SPACING;
        random = new java.util.Random(daily ? dailySeed : 7283L + courseSalt() * 101L);
        dt = DT; slowTimeRemaining = 0; lastSupportRope=RopeCatalog.CLASSIC; ropeAbilityTime=staticCurrentTime=0; staticJumpReady=false;
        landingX = landingTargetCenter = landingTargetWidth = Float.NaN;
        bossPhase = 0; bossWarning = bossLive = 0; bossCountdown = .6f;
        bossActive = false; bossSafeX = 240; bossY = groundY() - 1550;
        bossSourceSide=-1;bossSourceKind=0;bossSourceScreenY=270;bossSourceX=leftWall()+hazardInset();bossSourceY=270;
        attackCountdown = 2.0f;
        attackCount = 0;
        wave = 1;
        soundSequence = 0; soundEvent = 0;
        for (FireHazard fire : fireHazards) { fire.active = fire.activated = false; fire.warningTime = 0; fire.velocityX=0;fire.screenY=155; }
        for (BeamHazard beam : beamHazards) {
            beam.active = beam.followCamera = beam.activated = beam.bossLane = false; beam.warningTime = 0; beam.liveTime = .4f;
        }
        for (SawHazard saw : sawHazards) {
            saw.active = saw.followCamera = saw.activated = saw.bossRail = false;
            saw.x = saw.y = saw.previousX = saw.previousY = saw.pathX1 = saw.pathX2 = 0;
            saw.warningTime = saw.liveTime = saw.velocityX = saw.railTop = saw.railBottom = 0; saw.radius = 16;
        }
        for (int i = 0; i < ropes.length; i++) resetRope(ropes[i], FIRST_ROPE_Y + i * ROPE_SPACING, leftWall(), rightWall());
        resetMechanisms();
    }
    public float groundY() { return arcade ? 1_000_000 : stage.groundY; }
    /** Full chapter rope schedule, including recycled ropes; Arcade is endless. */
    public int totalRopes() {
        if (arcade) return 0;
        int total = ropes.length;
        for (float height = FIRST_ROPE_Y + ropes.length * ROPE_SPACING;
                height < groundY()-FLOOR_ROPE_CLEARANCE; height += ROPE_SPACING) total++;
        return total;
    }
    /** Ease in after 30% of chapter cuts, or over Arcade Wave 2. */
    private float steeringChallenge() {
        float progress = arcade ? (elapsed-20f)/20f : (brokenCount/(float)totalRopes()-.30f)/.70f;
        progress = clamp(progress, 0, 1);
        return progress*progress*(3-2*progress);
    }
    public float steeringScale() { return 1-.12f*steeringChallenge(); }
    private int courseSalt() { return arcade ? arcadeDifficulty*2 : stage.index; }
    /** A gentle alternating deviation, shared by touch and tilt; never a fixed pull. */
    public float nudgeVelocity() {
        return (touchControl||daily?12:18)*steeringChallenge()*(float)Math.sin(elapsed*.9f + courseSalt()*.8f)
                *(!touchControl&&!daily?motionLandingFade():1);
    }
    /** Intentional environmental motion challenge, separate from calibrated sensor output.
     * Touch and Daily retain their existing rules. The opening eases in over 2.5s after
     * a 2.5s grace; it gives way to the existing later nudge rather than stacking it. */
    public float motionGustVelocity() {
        if(touchControl||daily||lost||won)return 0;
        float opening=smooth(clamp((elapsed-2.5f)/2.5f,0,1));
        return 10*opening*(1-steeringChallenge())*(float)Math.sin(elapsed*.9f+courseSalt()*.8f)*motionLandingFade();
    }
    private float motionLandingFade(){return arcade?1:smooth(clamp((groundY()-y-350)/650,0,1));}
    private static float smooth(float value){return value*value*(3-2*value);}
    public float targetWidth() {
        if (won) return landingTargetWidth;
        if (arcade || !stage.hasMechanic(StageCatalog.SHRINKING_TARGET)) return stage.targetWidth;
        return stage.targetWidth * (.82f + .18f * (float)Math.cos(elapsed * .65f));
    }
    public float targetCenter() {
        if (won) return landingTargetCenter;
        float center = stage.targetCenter;
        if (!arcade && stage.hasMechanic(StageCatalog.MOVING_TARGET))
            center += 23 * (float)Math.sin(elapsed * .55f);
        // Keep the target's centre reachable by a whole avatar even as the corridor narrows.
        // Using the full target width here would silently stop its motion on later stages.
        float margin = leftWall() + hazardInset() + radius() + 10;
        return clamp(center, Math.min(240, margin), Math.max(240, W-margin));
    }
    public float targetLeft() { return targetCenter() - targetWidth() / 2; }
    public float targetRight() { return targetCenter() + targetWidth() / 2; }
    public float gravityScale() { return !arcade && stage.hasModifier(StageCatalog.LOW_GRAVITY) ? .72f : 1; }
    public int maxBurstJumps() { return !arcade && stage.hasModifier(StageCatalog.SINGLE_JUMP) ? 1 : MAX_JUMPS; }
    public float worldTimeScale() { return slowTimeRemaining > 0 ? .58f : 1; }
    public float hazardInset() { return stage.hazard == StageCatalog.SPIKES ? 18 : stage.hazard == StageCatalog.FIRE ? 10 : 4; }
    public void touchControlled(boolean enabled) {
        // Releasing a swipe must not carry touch-only wind into motion play.
        if (touchControl && !enabled) { vx = 0; touchVelocity = 0; }
        touchControl = enabled;
        if (enabled) gx = 0;
    }
    public void steerTouch(float desiredVelocity) {
        touchVelocity = clamp(desiredVelocity, -340, 340);
        if (touchControl && !lost && !won) vx = touchVelocity * steeringScale() * ropeSteeringScale() + driftVelocity() + nudgeVelocity() + windVelocity();
    }
    public float steeringVelocity() { return touchVelocity; }
    /** A bounded, alternating air current makes idle touch-only runs require correction. */
    public float driftVelocity() {
        float phase = courseSalt() * 1.13f + (arcade ? .6f : 0);
        return .35f*(72 * (float)Math.sin(elapsed * .63f + phase) + 28 * (float)Math.sin(elapsed * 1.17f + .8f + phase));
    }
    public float baseMass() { return Math.min(6, (1 + score * .012f) * (!arcade && stage.hasModifier(StageCatalog.HEAVY_LOAD) ? 1.3f : 1)); }
    public float mass() { return baseMass()*AvatarAbilities.massScale(avatar()); }
    public float radius() { return R + (baseMass() - 1) * 1.1f; }
    public float leftWall() {
        if (arcade) return Math.min(145, 28 + Math.min(1, deepestY / 18000f) * 65 + elapsed * .35f + score * .008f);
        float progress = clamp((Math.max(y, deepestY) - 310) / (groundY() - 310), 0, 1);
        return Math.min(214, 28 + progress * 92 + elapsed * .7f + score * .025f);
    }
    public float rightWall() { return W - leftWall(); }
    public float jumpRemaining() { return jumpCooldown; }
    public int jumpsRemaining() { return jumpsLeft; }
    /** A generous total allowance grows with chapter difficulty; Arcade has no lifetime cap. */
    public int jumpLimit() { return arcade ? 0 : daily ? 14 : 8+2*stage.chapterIndex; }
    public int jumpsUsed() { return manualJumps; }
    public int jumpAllowanceRemaining() { return arcade ? -1 : Math.max(0,jumpLimit()-manualJumps); }
    public boolean canJump() {
        return !lost && !won && jumpCooldown <= 0 && jumpsLeft > 0 && (arcade || manualJumps < jumpLimit());
    }
    /** Recover while actually descending, rather than waiting forever for a surviving rope. */
    public float burstRecoveryProgress() {
        if (jumpsLeft == maxBurstJumps()) return 1;
        float fall = (y-burstPeakY)/Math.max(1,burstStartY+BURST_DROP-burstPeakY);
        return Math.min(clamp((elapsed-lastJumpTime)/BURST_RECOVERY_TIME,0,1),clamp(fall,0,1));
    }
    private void refillBurst() { jumpsLeft = maxBurstJumps(); burstStartY = burstPeakY = y; }
    /** Matches the highest visible hair point in the upright human figure. */
    public float headTop() { return y-cameraY-1.42f*radius(); }
    public boolean jump() {
        if (!canJump()) return false;
        if (jumpsLeft == maxBurstJumps()) burstStartY = burstPeakY = y;
        // One fresh jump fits beneath the ceiling; a second near it is a deliberate risk.
        float jumpScale=staticJumpReady?.8f:1;
        vy = -Math.max(350, 450 / (float) Math.sqrt(baseMass()))*(float)Math.sqrt(AvatarAbilities.jumpHeight(avatar()))*jumpScale;
        staticJumpReady=false;
        jumpsLeft--;
        manualJumps++;
        lastJumpTime = elapsed;
        jumpCooldown = .30f;
        flash = .65f;
        return true;
    }

    private void resetRope(Rope rope, float height, float left, float right) {
        rope.reset(height, left, right);
        int ordinal = Math.round((height-FIRST_ROPE_Y)/ROPE_SPACING);
        rope.courseIndex=ordinal;
        rope.type=scheduledRopeType(ordinal);
        rope.fragile = !arcade && stage.hasMechanic(StageCatalog.BREAKABLE_ANCHORS) && ordinal > 1 && ordinal % 3 == 2;
    }
    /** No hazard RNG is consumed: retries use the same readable world-space pattern.
     * Arcade only introduces specials on newly generated Wave 2+ mounts; existing
     * visible ropes never change type underneath the player. */
    private int scheduledRopeType(int ordinal){
        if(daily||ordinal<3)return RopeCatalog.CLASSIC;
        if(arcade){
            if(elapsed<20)return RopeCatalog.CLASSIC;
            int phase=Math.floorMod(ordinal,8);
            return phase==3?RopeCatalog.SPRING:phase==7?RopeCatalog.STATIC:RopeCatalog.CLASSIC;
        }
        if(ordinal>=totalRopes()-3)return RopeCatalog.CLASSIC;
        if(stage.index<2)return ordinal%8==(stage.index==0?4:3)?RopeCatalog.SPRING:RopeCatalog.CLASSIC;
        int phase=Math.floorMod(ordinal+stage.index%4,8);
        return phase==3?RopeCatalog.SPRING:phase==7?RopeCatalog.STATIC:RopeCatalog.CLASSIC;
    }
    private void resetMechanisms() {
        for (int i=0;i<3;i++) {
            WindZone wind = windZones[i];
            wind.active = !arcade && stage.hasMechanic(StageCatalog.WIND_ZONES);
            wind.top = 1100 + i*(groundY()-2400)/3;
            wind.bottom = wind.top+380;
            wind.strength = (i%2==0 ? 1 : -1)*(daily ? 24 : 36);
            BouncePad pad = bouncePads[i];
            pad.active = !arcade && stage.hasMechanic(StageCatalog.BOUNCE_PADS);
            pad.x = i%2==0 ? 210 : 270;
            pad.y = 1250+i*(groundY()-2550)/3;
            pad.cooldown = 0;
            SlowPickup pickup = slowPickups[i];
            pickup.active = !arcade && stage.hasMechanic(StageCatalog.SLOW_PICKUPS);
            pickup.x = i%2==0 ? 220 : 260;
            pickup.y = 1000+i*(groundY()-2200)/3;
            pickup.collected = false;
        }
    }
    /** Motion and touch experience the same bounded, visibly advertised air currents. */
    public float windVelocity() {
        if (arcade) return 0;
        float wind = 0;
        for (WindZone zone : windZones) if (zone.active && y>=zone.top && y<=zone.bottom) {
            float edge = Math.min((y-zone.top)/80, (zone.bottom-y)/80);
            wind += zone.strength*clamp(edge,0,1);
        }
        if (stage.hasModifier(StageCatalog.GUSTS)) wind *= .65f+.35f*(float)Math.sin(elapsed*1.6f);
        return wind;
    }
    private void simulateMechanisms(float oldX, float oldY) {
        for (BouncePad pad : bouncePads) if (pad.active) {
            pad.cooldown = Math.max(0,pad.cooldown-dt);
            if (pad.cooldown<=0 && vy>0 && oldY+radius()<=pad.y && y+radius()>=pad.y
                    && Math.abs(x-pad.x)<=pad.width/2+radius()*.4f) {
                y=pad.y-radius(); vy=-390; pad.cooldown=1.1f;
                landingPulse=.28f; flash=.7f; refillBurst();
            }
        }
        for (SlowPickup pickup : slowPickups) if (pickup.active && !pickup.collected) {
            float reach=radius()+15;
            if (pointSegmentDistanceSquared(pickup.x,pickup.y,oldX,oldY,x,y)<=reach*reach) {
                pickup.collected=true; slowTimeRemaining=3.5f; flash=.8f;
            }
        }
    }

    public void step() {
        if (lost) { blast = Math.max(0, blast - dt * .7f); return; }
        dt = DT * worldTimeScale();
        slowTimeRemaining = Math.max(0,slowTimeRemaining-DT);
        immunityPulse=Math.max(0,immunityPulse-dt);
        ropeAbilityTime=Math.max(0,ropeAbilityTime-dt);
        staticCurrentTime=Math.max(0,staticCurrentTime-dt);
        landingPulse = Math.max(0, landingPulse - dt);
        groundBounce = Math.max(0, groundBounce - dt * 2);
        if (won) { celebrateLanding(); return; }
        elapsed += dt;
        score = (int) (elapsed * 10) + brokenCount * 30;
        wave = 1 + (int)(elapsed / 20);
        jumpCooldown = Math.max(0, jumpCooldown - dt);
        flash = Math.max(0, flash - dt * 2);
        float left = leftWall(), right = rightWall();
        for (Rope rope : ropes) {
            if (rope.base < cameraY - 240 && (arcade || nextRopeY < groundY() - FLOOR_ROPE_CLEARANCE)) {
                resetRope(rope, nextRopeY, left, right);
                nextRopeY += ROPE_SPACING;
            }
            simulateRope(rope, left, right);
        }
        float previousX = x, previousY = y;
        if (touchControl) vx = touchVelocity * steeringScale() * ropeSteeringScale() + driftVelocity() + nudgeVelocity() + windVelocity();
        else {
            // Tilt requests a bounded sideways speed. Returning to neutral
            // brakes momentum and rope kicks instead of coasting for seconds.
            float command = clamp(gx, -950, 950) * (MAX_MOTION_SPEED/950) * steeringScale() * ropeSteeringScale();
            float target = clamp(command+nudgeVelocity()+motionGustVelocity()+windVelocity(), -MAX_MOTION_SPEED, MAX_MOTION_SPEED);
            float response = Math.abs(command) < 1 ? 14 : 10;
            vx += (target-vx) * (1f-(float)Math.exp(-response*dt));
            vx = clamp(vx, -MAX_MOTION_SPEED, MAX_MOTION_SPEED);
            if (target == 0 && Math.abs(vx) < .5f) vx = 0;
        }
        vy = clamp(vy + GRAVITY * gravityScale() * dt, -1100, 800);
        x += vx * dt;
        y += vy * dt;
        deepestY = Math.max(deepestY, y);
        // Walls are lethal on first overlap, including walls that move into a stationary ball.
        if (hitsSide()) { die(sideReason()); return; }
        if (hitsCeiling()) return;
        for (Rope rope : ropes) contact(rope);
        simulateMechanisms(previousX, previousY);
        if (!touchControl) vx = clamp(vx, -MAX_MOTION_SPEED, MAX_MOTION_SPEED);
        if (lost) return;
        if (hitsCeiling()) return;
        if (!arcade) contactGround();
        if (!won && !lost && jumpsLeft < maxBurstJumps()) {
            burstPeakY = Math.min(burstPeakY,y);
            if (elapsed-lastJumpTime >= BURST_RECOVERY_TIME && vy >= 80 && y >= burstStartY+BURST_DROP)
                refillBurst();
        }
        if (!won && !lost) simulateHazards(previousX, previousY);
        // Follow downwards, stopping where the target floor remains in view.
        cameraY = arcade ? Math.max(cameraY, y - 430) : Math.min(groundY() - 675, Math.max(cameraY, y - 430));
    }

    private void simulateRope(Rope r, float left, float right) {
        for (int i = 0; i < Rope.N; i++) {
            r.oldX[i] = r.x[i]; r.oldY[i] = r.y[i];
            if (i == 0 || i == Rope.N - 1) continue;
            float dx = clamp((r.x[i] - r.px[i]) * .998f, -4, 4);
            float dy = clamp((r.y[i] - r.py[i]) * .998f, -4, 4);
            r.px[i] = r.x[i]; r.py[i] = r.y[i];
            r.x[i] += dx; r.y[i] += dy + 380 * dt * dt;
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
            float ropeVx = ((r.x[i] - r.oldX[i]) * (1 - t) + (r.x[i + 1] - r.oldX[i + 1]) * t) / dt;
            float ropeVy = ((r.y[i] - r.oldY[i]) * (1 - t) + (r.y[i + 1] - r.oldY[i + 1]) * t) / dt;
            float normalVelocity = (vx - ropeVx) * nx + (vy - ropeVy) * ny;
            float load = mass() * (GRAVITY + Math.max(0, -normalVelocity) * .6f);
            if (r.fragile && ny < -.25f && vy>=0 && normalVelocity < -45)
                r.anchorStress = Math.min(1,r.anchorStress+.42f+Math.max(0,-normalVelocity)/1600);
            if (ny < -.25f && vy >= 0 && (load > r.strength || r.anchorStress >= 1)) {
                // Fragile anchors detach at their advertised end, while ordinary ropes cut underfoot.
                r.cut = r.anchorStress>=1 ? (r.index%2==0 ? 0 : Rope.N-2) : i;
                landingPulse = .18f;
                brokenCount++;
                if(ropeObserver!=null)ropeObserver.encountered(r.courseIndex,r.type,true);
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
                    if(ropeObserver!=null)ropeObserver.encountered(r.courseIndex,r.type,false);
                    refillBurst();
                    landingPulse = .18f;
                    // A landing naturally produces a small spring-assisted human jump.
                    float rebound=Math.max(235,325/(float)Math.sqrt(mass()));
                    lastSupportRope=RopeCatalog.normalize(r.type);
                    if(lastSupportRope==RopeCatalog.SPRING){
                        // The bonus belongs to rope support, never to manual jumps or cutting.
                        float ceilingRoom=Math.max(0,headTop()-ARENA_TOP-12);
                        rebound=Math.min(rebound*1.5f,(float)Math.sqrt(2*GRAVITY*gravityScale()*ceilingRoom)*.95f);
                        ropeAbilityTime=.65f;
                    }else if(lastSupportRope==RopeCatalog.STATIC){
                        staticJumpReady=true;ropeAbilityTime=staticCurrentTime=.75f;
                    }else ropeAbilityTime=0;
                    vy=-rebound;
                    flash = Math.max(flash, .35f);
                }
                for (int k = i; k <= i + 1; k++) if (k > 0 && k < Rope.N - 1) {
                    float weight = k == i ? 1 - t : t;
                    r.px[k] += nx * impulse * dt * .18f * mass() * weight;
                    r.py[k] += ny * impulse * dt * .18f * mass() * weight;
                }
            }
        }
        if (hitsSide()) die(sideReason());
    }
    private void contactGround() {
        if (vy < 0 || y + radius() < groundY()) return;
        boolean inside = x - radius() >= targetLeft() && x + radius() <= targetRight();
        if (!inside && stage.fatalMiss) {
            y = groundY() - radius(); vx = vy = 0; die("HARD LANDING"); return;
        }
        float impact = vy;
        y = groundY() - radius();
        vy = -Math.min(350, Math.max(90, impact * .32f));
        landingPulse = .18f;
        groundBounce = 1;
        refillBurst();
        if (inside) {
            landingX=x; landingTargetCenter=targetCenter(); landingTargetWidth=targetWidth();
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
        winTime += dt;
        gx = vx = 0;
        vy += GRAVITY * dt;
        y += vy * dt;
        if (y + radius() >= groundY() && vy > 0) {
            y = groundY() - radius();
            vy = vy < 55 ? 0 : -vy * .25f;
            groundBounce = Math.max(groundBounce, .3f);
        }
    }
    private boolean hitsSide() {
        return x - radius() <= leftWall() + hazardInset() || x + radius() >= rightWall() - hazardInset();
    }
    private boolean hitsCeiling() {
        if (headTop() > ARENA_TOP) return false;
        y = cameraY+ARENA_TOP+1.42f*radius();
        vx = vy = 0;
        die("CEILING");
        return true;
    }
    private String sideReason() {
        return stage.hazard == StageCatalog.LASER ? "LASER" : stage.hazard == StageCatalog.SPIKES ? "SPIKES" : "FIRE";
    }
    private void simulateHazards(float previousX, float previousY) {
        // A strict finish must not compete with an attack whose warning began farther up.
        if (!arcade && y >= groundY() - 650) {
            for (FireHazard fire : fireHazards) fire.active = false;
            for (BeamHazard beam : beamHazards) beam.active = false;
            for (SawHazard saw : sawHazards) saw.active = false;
            bossActive=false; bossWarning=bossLive=0;
            return;
        }
        int difficulty = arcade ? arcadeDifficulty : stage.attackDifficulty;
        int mask = arcade ? arcadeHazards : stage.extraHazards;
        boolean occupied = false;
        for (FireHazard fire : fireHazards) if (fire.active) occupied = true;
        for (BeamHazard beam : beamHazards) if (beam.active) occupied = true;
        for (SawHazard saw : sawHazards) if (saw.active) occupied = true;
        boolean inBoss = !arcade && stage.bossKind!=StageCatalog.BOSS_NONE
                && deepestY>=Math.max(1400,groundY()-4200);
        bossActive=inBoss;
        if(inBoss){
            bossSourceY=cameraY+bossSourceScreenY;
            // Narrowing walls can invalidate an old refuge marker. Move the marker
            // inward with the walls only; attack geometry remains frozen.
            bossSafeX=clamp(bossSafeX,leftWall()+hazardInset()+radius()+14,rightWall()-hazardInset()-radius()-14);
        }
        bossWarning=bossLive=0;
        if (inBoss) {
            for (FireHazard fire:fireHazards) if(fire.active) {
                bossWarning=Math.max(bossWarning,fire.warningTime);
                if(fire.warningTime<=0)bossLive=1;
            }
            for (BeamHazard beam:beamHazards) if(beam.active) {
                bossWarning=Math.max(bossWarning,beam.warningTime);
                if(beam.warningTime<=0)bossLive=Math.max(bossLive,beam.liveTime);
            }
            for (SawHazard saw:sawHazards) if(saw.active) {
                bossWarning=Math.max(bossWarning,saw.warningTime);
                if(saw.warningTime<=0)bossLive=Math.max(bossLive,saw.liveTime);
            }
            if (!occupied) {
                bossCountdown-=dt;
                if(bossCountdown<=0) {
                    spawnBossAttack(); bossPhase++; bossCountdown=.45f;
                    occupied=true;
                }
            }
        }
        // One attack at a time preserves a readable escape route, including in mixed skirmishes.
        if (!inBoss && !occupied && mask != 0 && (arcade || y < groundY() - 650)) {
            attackCountdown -= DT;
            if (attackCountdown <= 0) {
                int count = Integer.bitCount(mask), choice = random.nextInt(count), kind = 1;
                for (; kind <= ArcadeRules.BUZZSAWS; kind <<= 1) if ((mask & kind) != 0 && choice-- == 0) break;
                // Introduce the new blade predictably before mixing it with side lasers.
                if (!arcade && stage.openingHazard != 0 && attackCount == 0) kind = stage.openingHazard;
                spawnAttack(kind, difficulty);
                if(!arcade&&!daily&&stage.index<3&&attackCount==0){
                    // An aimed, frozen opening warning gives a generous, readable escape window.
                    float aim=clamp(x+(touchControl?driftVelocity()*.6f:0),leftWall()+hazardInset()+38,rightWall()-hazardInset()-38);
                    for(BeamHazard beam:beamHazards)if(beam.active&&beam.kind==ArcadeRules.TOP_LASERS){
                        beam.x1=beam.x2=aim;beam.warningTime=1.65f;beam.liveTime=.34f;
                    }
                }
                attackCount++;
                attackCountdown = ArcadeRules.attackGap(difficulty, wave) + random.nextFloat() * .3f;
            }
        }
        for (FireHazard fire : fireHazards) if (fire.active) {
            if (fire.warningTime > 0) {
                fire.y = cameraY + fire.screenY;
                fire.warningTime = Math.max(0, fire.warningTime - dt);
                continue;
            }
            if (!fire.activated) { fire.activated = true; emitSound(SFX_FIRE_LAUNCH); }
            float oldX = fire.x, oldY = fire.y;
            fire.speed = Math.min(1400, fire.speed + 100 * dt);
            fire.x += fire.velocityX * dt;
            fire.y += fire.speed * dt;
            // Relative swept motion covers the whole step, not only its end positions.
            float reach = radius() + fire.radius;
            if (pointSegmentDistanceSquared(0, 0, previousX - oldX, previousY - oldY, x - fire.x, y - fire.y) <= reach * reach) {
                if(AvatarAbilities.fireballImmune(avatar())){fire.active=false;absorb(ArcadeRules.FIREBALLS);continue;}
                die("FALLING FIRE"); return;
            }
            if (fire.y > cameraY + H + 140 || !arcade && fire.y > groundY()) fire.active = false;
        }
        for (BeamHazard beam : beamHazards) if (beam.active) {
            float oldY1 = beam.y1, oldY2 = beam.y2;
            if (beam.followCamera) { beam.y1 = cameraY + beam.screenY1; beam.y2 = cameraY + beam.screenY2; }
            if (beam.warningTime > 0) {
                beam.warningTime = Math.max(0, beam.warningTime - dt);
                continue;
            }
            if (!beam.activated) { beam.activated = true; emitSound(beam.kind == ArcadeRules.LIGHTNING ? SFX_LIGHTNING : SFX_LASER); }
            float shift = beam.y1 - oldY1;
            float reach = radius() + beam.width / 2;
            boolean contact=beam.bossLane
                    ? sweptCircleRect(previousX,previousY+shift,x,y,beam.x1-beam.width/2,Math.min(beam.y1,beam.y2),beam.x1+beam.width/2,Math.max(beam.y1,beam.y2),radius())
                    : segmentDistanceSquared(previousX,previousY+shift,x,y,beam.x1,beam.y1,beam.x2,beam.y2)<=reach*reach;
            if (contact) {
                if(AvatarAbilities.beamImmune(avatar(),beam.kind)){beam.active=false;absorb(beam.kind);continue;}
                die(beam.kind == ArcadeRules.LIGHTNING ? "LIGHTNING" : beam.kind == ArcadeRules.SIDE_LASERS ? "SIDE LASER" : "TOP LASER"); return;
            }
            beam.liveTime -= DT;
            if (beam.liveTime <= 0) beam.active = false;
        }
        for (SawHazard saw : sawHazards) if (saw.active) {
            saw.previousX = saw.x; saw.previousY = saw.y;
            if (saw.followCamera) saw.y = cameraY + saw.screenY;
            if(saw.bossRail){saw.railTop=cameraY+ARENA_TOP;saw.railBottom=cameraY+730;}
            if (saw.warningTime > 0) {
                saw.warningTime = Math.max(0, saw.warningTime - dt);
                continue;
            }
            if (!saw.activated) { saw.activated = true; emitSound(SFX_SAW_MOTOR); }
            float travel = saw.velocityX * dt;
            if (saw.followCamera) {
                // Stop at the advertised endpoint; the far side remains a usable refuge.
                saw.x = saw.velocityX > 0 ? Math.min(saw.pathX2, saw.x + travel) : Math.max(saw.pathX2, saw.x + travel);
            } else saw.x += travel;
            float reach = radius() + saw.radius;
            boolean contact=saw.bossRail
                    ? sweptCircleRect(previousX+saw.x-saw.previousX,previousY+saw.y-saw.previousY,x,y,saw.x-saw.radius,saw.railTop,saw.x+saw.radius,saw.railBottom,radius())
                    : pointSegmentDistanceSquared(0, 0, previousX - saw.previousX, previousY - saw.previousY,x - saw.x, y - saw.y)<=reach*reach;
            if (contact) {
                die("BUZZSAW"); return;
            }
            saw.liveTime -= DT;
            if (saw.liveTime <= 0 || saw.followCamera && Math.abs(saw.x - saw.pathX2) < .001f) saw.active = false;
        }
    }
    private void spawnBossAttack() {
        int kind;
        switch(stage.bossKind) {
            case StageCatalog.BOSS_LASER: kind=bossPhase%2==0?ArcadeRules.TOP_LASERS:ArcadeRules.SIDE_LASERS; break;
            case StageCatalog.BOSS_SAW: kind=ArcadeRules.BUZZSAWS; break;
            case StageCatalog.BOSS_FIRE: kind=ArcadeRules.FIREBALLS; break;
            case StageCatalog.BOSS_STORM: kind=ArcadeRules.LIGHTNING; break;
            case StageCatalog.BOSS_FORGE: kind=bossPhase%2==0?ArcadeRules.BUZZSAWS:ArcadeRules.SIDE_LASERS; break;
            default:
                int[] sequence={ArcadeRules.SIDE_LASERS,ArcadeRules.FIREBALLS,ArcadeRules.LIGHTNING,ArcadeRules.BUZZSAWS};
                kind=sequence[bossPhase%sequence.length];
        }
        spawnAttack(kind,0);
        float left=leftWall()+hazardInset(), right=rightWall()-hazardInset();
        boolean fromLeft=bossPhase%2==0;
        bossSafeX=clamp(240+(fromLeft?1:-1)*Math.min(58,(right-left)/4),left+radius()+14,right-radius()-14);
        bossSourceSide=fromLeft?-1:1;bossSourceKind=kind;bossSourceScreenY=270;
        bossSourceX=fromLeft?left:right;bossSourceY=cameraY+bossSourceScreenY;
        for(FireHazard fire:fireHazards)if(fire.active) {
            fire.warningTime=1.55f;fire.x=bossSourceX;fire.screenY=bossSourceScreenY;fire.y=cameraY+fire.screenY;
            fire.radius=14;fire.speed=1100;
            // Aim once at the advertised opposite/central lane. Never retarget after cue.
            float aim=240+(fromLeft?-1:1)*Math.min(24,(right-left)/8);
            fire.velocityX=(aim-fire.x)/.60f;
        }
        for(BeamHazard beam:beamHazards)if(beam.active) {
            beam.warningTime=1.55f; beam.liveTime=.34f;
            if(stage.bossKind==StageCatalog.BOSS_LASER){
                // An advertised half-width grid leaves one full-body refuge and
                // covers every jump height; a centered hands-off descent is unsafe.
                beam.bossLane=true;beam.width=(right-left)/2+4;
                beam.x1=beam.x2=fromLeft?left+beam.width/2:right-beam.width/2;
                beam.screenY1=ARENA_TOP;beam.screenY2=730;
                beam.y1=cameraY+beam.screenY1;beam.y2=cameraY+beam.screenY2;
            }else if(kind==ArcadeRules.TOP_LASERS)beam.x1=beam.x2=240+(fromLeft?-1:1)*Math.min(42,(right-left)/5);
            else if(kind==ArcadeRules.SIDE_LASERS) {
                // A 1.55s warning permits substantial downward wall closure. Reserve
                // more of the opposite side than ordinary short-warning attacks.
                float length=Math.max(20,(right-left)*.30f);
                beam.x1=fromLeft?left:right; beam.x2=fromLeft?left+length:right-length;
                bossSourceScreenY=beam.screenY1;bossSourceY=cameraY+bossSourceScreenY;
            } else {
                // A diagonal bolt occupies one half, leaving a full avatar-wide refuge.
                beam.x1=fromLeft?left:right; beam.x2=240;
                beam.screenY1=bossSourceScreenY; beam.screenY2=710;
                beam.y1=cameraY+beam.screenY1;beam.y2=cameraY+beam.screenY2;
            }
        }
        for(SawHazard saw:sawHazards)if(saw.active) {
            saw.warningTime=1.55f;
            saw.bossRail=stage.bossKind==StageCatalog.BOSS_SAW;
            saw.railTop=cameraY+ARENA_TOP;saw.railBottom=cameraY+730;
            saw.pathX1=fromLeft?left-saw.radius-8:right+saw.radius+8;
            saw.pathX2=fromLeft?240-12:240+12;
            saw.x=saw.previousX=saw.pathX1;bossSourceX=saw.pathX1;
            saw.velocityX=fromLeft?420:-420;
            saw.liveTime=Math.abs(saw.pathX2-saw.pathX1)/420+dt;
            if(saw.bossRail){saw.screenY=bossSourceScreenY;saw.y=saw.previousY=cameraY+saw.screenY;}
            else{bossSourceScreenY=saw.screenY;bossSourceY=cameraY+bossSourceScreenY;}
        }
        bossWarning=1.55f;
    }
    private void spawnAttack(int kind, int difficulty) {
        float left = leftWall() + hazardInset(), right = rightWall() - hazardInset();
        float warning = daily ? Math.max(1.45f,ArcadeRules.warning(0,1)) : ArcadeRules.warning(difficulty, wave);
        if (kind == ArcadeRules.FIREBALLS) {
            for (FireHazard fire : fireHazards) if (!fire.active) {
                fire.x = left + 26 + random.nextFloat() * Math.max(1, right - left - 52);
                fire.velocityX=0;fire.screenY=155;fire.y = cameraY + fire.screenY; fire.radius = 10 + random.nextFloat() * 4;
                fire.warningTime = warning; fire.speed = 1050 + difficulty * 75; fire.active = true; fire.activated = false;
                return;
            }
        } else if (kind == ArcadeRules.BUZZSAWS) {
            for (SawHazard saw : sawHazards) if (!saw.active) {
                saw.radius = 16;saw.bossRail=false;
                boolean fromLeft = random.nextBoolean();
                float length = Math.max(10, Math.min((right - left) * .52f,
                        right - left - (radius() * 2 + saw.radius * 2 + 28)));
                saw.pathX1 = fromLeft ? left - saw.radius - 8 : right + saw.radius + 8;
                saw.pathX2 = fromLeft ? left + length : right - length;
                saw.x = saw.previousX = saw.pathX1;
                saw.screenY = clamp(y - cameraY, 260, 600);
                saw.y = saw.previousY = cameraY + saw.screenY;
                saw.velocityX = (fromLeft ? 1 : -1) * (420 + difficulty * 100);
                saw.warningTime = warning;
                saw.liveTime = Math.abs(saw.pathX2 - saw.pathX1) / Math.abs(saw.velocityX) + dt;
                saw.active = saw.followCamera = true; saw.activated = false;
                return;
            }
        } else for (BeamHazard beam : beamHazards) if (!beam.active) {
            beam.bossLane=false;beam.kind = kind; beam.width = kind == ArcadeRules.LIGHTNING ? 14 : 10;
            beam.warningTime = warning; beam.liveTime = ArcadeRules.beamLife(difficulty);
            beam.followCamera = true; beam.active = true; beam.activated = false;
            if (kind == ArcadeRules.TOP_LASERS) {
                beam.x1 = beam.x2 = left + 25 + random.nextFloat() * Math.max(1, right - left - 50);
                beam.screenY1 = 160; beam.screenY2 = 730;
            } else if (kind == ArcadeRules.SIDE_LASERS) {
                // The untouched side fits the full avatar plus a generous beam/wall margin.
                float length = Math.min((right - left) * .56f, right - left - (radius() * 2 + beam.width + 24));
                length = Math.max(15, length);
                boolean fromLeft = random.nextBoolean();
                beam.x1 = fromLeft ? left : right; beam.x2 = fromLeft ? left + length : right - length;
                beam.screenY1 = beam.screenY2 = clamp(y - cameraY, 220, 640);
            } else {
                boolean fromLeft = random.nextBoolean();
                beam.x1 = fromLeft ? left + 8 : right - 8; beam.x2 = fromLeft ? right - 8 : left + 8;
                beam.screenY1 = 180; beam.screenY2 = 710;
            }
            beam.y1 = cameraY + beam.screenY1; beam.y2 = cameraY + beam.screenY2;
            return;
        }
    }
    /** Exact moving-circle contact with a drawn, axis-aligned rectangle. Edge segment
     * distances include circular avatar corners without inventing large beam endcaps. */
    public static boolean sweptCircleRect(float ax,float ay,float bx,float by,float left,float top,float right,float bottom,float radius){
        if(ax>=left&&ax<=right&&ay>=top&&ay<=bottom||bx>=left&&bx<=right&&by>=top&&by<=bottom)return true;
        float distance=Math.min(Math.min(segmentDistanceSquared(ax,ay,bx,by,left,top,right,top),segmentDistanceSquared(ax,ay,bx,by,right,top,right,bottom)),
                Math.min(segmentDistanceSquared(ax,ay,bx,by,right,bottom,left,bottom),segmentDistanceSquared(ax,ay,bx,by,left,bottom,left,top)));
        return distance<=radius*radius;
    }
    public static float pointSegmentDistanceSquared(float px, float py, float ax, float ay, float bx, float by) {
        float dx = bx - ax, dy = by - ay, denominator = dx * dx + dy * dy;
        float t = denominator < .000001f ? 0 : clamp(((px - ax) * dx + (py - ay) * dy) / denominator, 0, 1);
        float ex = px - ax - dx * t, ey = py - ay - dy * t;
        return ex * ex + ey * ey;
    }
    public static float segmentDistanceSquared(float ax, float ay, float bx, float by, float cx, float cy, float dx, float dy) {
        float abx = bx - ax, aby = by - ay, cdx = dx - cx, cdy = dy - cy;
        float cross = abx * cdy - aby * cdx;
        if (Math.abs(cross) > .000001f) {
            float acx = cx - ax, acy = cy - ay;
            float t = (acx * cdy - acy * cdx) / cross, u = (acx * aby - acy * abx) / cross;
            if (t >= 0 && t <= 1 && u >= 0 && u <= 1) return 0;
        }
        return Math.min(Math.min(pointSegmentDistanceSquared(ax, ay, cx, cy, dx, dy), pointSegmentDistanceSquared(bx, by, cx, cy, dx, dy)),
                Math.min(pointSegmentDistanceSquared(cx, cy, ax, ay, bx, by), pointSegmentDistanceSquared(dx, dy, ax, ay, bx, by)));
    }
    private void absorb(int kind){immunityPulse=.45f;immunityKind=kind;flash=Math.max(flash,.35f);}
    private void emitSound(int event) { soundEvent = event; soundSequence++; }
    private void die(String reason) {
        lost = true; deathReason = reason; blast = 1; gx = 0;
        if ("FALLING FIRE".equals(reason) || "FIRE".equals(reason)) emitSound(SFX_FIRE_BURN);
        else if ("BUZZSAW".equals(reason)) emitSound(SFX_SAW_IMPACT);
        else if ("LASER".equals(reason)) emitSound(SFX_LASER);
    }
    public static float clamp(float value, float low, float high) { return Math.max(low, Math.min(high, value)); }
}
