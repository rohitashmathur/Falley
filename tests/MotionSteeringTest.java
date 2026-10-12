package com.perfgames.ropelab;

/** Sensor playback without a phone: timing, calibration and response comparisons. */
public final class MotionSteeringTest {
    private static final long START = 1000000000L;
    private static int checks;
    private static void check(boolean ok, String message) {
        checks++;
        if (!ok) throw new AssertionError(message);
    }
    private static MotionSteering tilted(int preset, int rate, float x) {
        MotionSteering motion = new MotionSteering(false);
        motion.setPreset(preset);
        motion.acceptAccelerometer(START, 0, 0, 9.80665f);
        for (int i = 1; i <= rate * 2; i++)
            motion.acceptAccelerometer(START + Math.round(i * 1e9 / rate), x, 0, 9.3f);
        return motion;
    }
    private static void centeringAndFallback() {
        MotionSteering motion = new MotionSteering(false);
        check(!motion.hasSample() && motion.acceleration() == 0 && !motion.calibrate(),
                "no samples means no steering or false calibration success");
        motion.acceptGyroscope(START, 0, 1, 0);
        check(!motion.hasSample() && motion.acceleration() == 0, "gyro-only startup cannot invent a gravity reference");
        motion.acceptAccelerometer(START, -3, 0, 9.3f);
        check(motion.hasSample() && motion.acceleration() == 0, "the first phone pose automatically centers steering");

        MotionSteering right = tilted(MotionSteering.BALANCED, 60, -3);
        MotionSteering left = tilted(MotionSteering.BALANCED, 60, 3);
        check(right.acceleration() > 50 && left.acceleration() < -50,
                "accelerometer-only fallback steers in both directions");
        check(Math.abs(right.acceleration() + left.acceleration()) < 1,
                "matching opposite tilts produce symmetric steering");
        check(right.calibrate() && right.acceleration() == 0, "ZERO centers the current pose immediately");
        for (int i = 1; i <= 60; i++) right.acceptAccelerometer(START + 2000000000L + i * 16666667L, -3, 0, 9.3f);
        check(Math.abs(right.acceleration()) < 15, "holding the calibrated pose stays near neutral");
        right.resetSamples();
        check(!right.hasSample() && right.acceleration() == 0, "resume reset clears old sample and steering");
        right.acceptAccelerometer(START + 6000000000L, 4, 0, 9);
        check(right.hasSample() && right.acceleration() == 0, "the next sample after reset centers the new pose");
    }
    private static void responsePresetsAndTiming() {
        float loose = tilted(MotionSteering.LOOSE, 60, -3).acceleration();
        float balanced = tilted(MotionSteering.BALANCED, 60, -3).acceleration();
        float tight = tilted(MotionSteering.TIGHT, 60, -3).acceleration();
        check(loose > balanced * 1.15f && balanced > tight * 1.15f,
                "Loose gives larger steering than Balanced, and Tight gives smaller steering");
        for (int preset = MotionSteering.LOOSE; preset <= MotionSteering.TIGHT; preset++) {
            float slowRate = tilted(preset, 30, -3).acceleration();
            float fastRate = tilted(preset, 120, -3).acceleration();
            check(Math.abs(slowRate - fastRate) < 15,
                    "response depends on elapsed time rather than sample rate");
        }
        MotionSteering extreme = tilted(MotionSteering.LOOSE, 60, -9.8f);
        check(Float.isFinite(extreme.acceleration()) && Math.abs(extreme.acceleration()) <= 1400,
                "extreme tilt is capped and finite");
    }
    private static void gyroReactionAndInvalidSamples() {
        MotionSteering fused = new MotionSteering(true);
        fused.acceptAccelerometer(START, 0, 0, 9.80665f);
        fused.acceptGyroscope(START, 0, .8f, 0);
        for (int i = 1; i <= 20; i++)
            fused.acceptGyroscope(START + i * 10000000L, 0, .8f, 0);
        check(fused.acceleration() > 50, "gyroscope rotation contributes before another accelerometer sample");
        float before = fused.acceleration();
        fused.acceptAccelerometer(START - 1, 5, 0, 9);
        fused.acceptAccelerometer(START + 300000000L, Float.NaN, 0, 9);
        fused.acceptAccelerometer(START + 300000000L, 0, 0, 0);
        fused.acceptAccelerometer(START + 300000000L, 50, 0, 0);
        fused.acceptGyroscope(START + 190000000L, 0, -10, 0);
        fused.acceptGyroscope(START + 300000000L, Float.POSITIVE_INFINITY, 0, 0);
        check(fused.acceleration() == before, "old timestamps, invalid values, freefall and severe shakes are ignored");
        for (int i = 1; i <= 300; i++)
            fused.acceptAccelerometer(START + 300000000L + i * 16666667L, 0, 0, 9.80665f);
        check(Math.abs(fused.acceleration()) < 15, "accelerometer gravity correction removes gyro drift after rotation");
    }
    public static void main(String[] args) {
        centeringAndFallback();
        responsePresetsAndTiming();
        gyroReactionAndInvalidSamples();
        System.out.println("PASS: sensor startup/centering, accelerometer fallback, ZERO calibration, resume reset, Loose/Balanced/Tight response, sample-rate consistency, gyro reaction, invalid samples and drift correction (" + checks + " checks)");
    }
}
