package com.perfgames.ropelab;

/**
 * Android-independent complementary filter for portrait game steering.
 * Inputs use screen-aligned device axes: x right, y up, z out of the display.
 * Accelerometer units are m/s^2, gyroscope units rad/s and timestamps nanoseconds.
 * Integrating the gyro preserves quick response; gravity from the accelerometer
 * corrects long-term drift. All filters use elapsed time rather than sample count.
 */
public final class MotionSteering {
    public static final int LOOSE = 0, BALANCED = 1, TIGHT = 2;
    private static final float[] GAIN = {1850f, 1350f, 950f};
    private static final float[] DEAD_ZONE = {.03f, .045f, .065f};
    private static final float[] OUTPUT_TAU = {.045f, .065f, .09f};
    private static final float[] ACCEL_TAU = {.06f, .08f, .10f};
    private static final long SETTLE_NS = 250000000L, STALE_NS = 250000000L;
    private final boolean gyroAvailable;
    private int preset = BALANCED;
    private float gx, gy, gz = 1f, neutral, output;
    private long accelTime, gyroTime, outputTime, centerStart;
    private boolean sampled, centering;

    public MotionSteering(boolean gyroAvailable) { this.gyroAvailable = gyroAvailable; }
    public int preset() { return preset; }
    public void setPreset(int value) {
        preset = Math.max(LOOSE, Math.min(TIGHT, value));
        // Retain the pose and calibration while changing the response curve.
        output = clamp(output, -1400f, 1400f);
    }
    public boolean hasSample() { return sampled; }
    public boolean isCentering() { return !sampled || centering; }
    public float acceleration() { return sampled ? output : 0f; }
    /** nowNs must use Android's elapsedRealtimeNanos clock, like SensorEvent. */
    public float acceleration(long nowNs) {
        return !sampled || nowNs < accelTime || nowNs-accelTime > STALE_NS ? 0f : output;
    }

    /** Clears stale integration and automatically centers the next valid pose. */
    public void resetSamples() {
        gx = gy = neutral = output = 0f; gz = 1f;
        accelTime = gyroTime = outputTime = centerStart = 0L;
        sampled = false; centering = true;
    }
    public boolean calibrate() {
        if (!sampled) return false;
        neutral = -gx;
        output = 0f; centering = false;
        return true;
    }

    public void acceptAccelerometer(long timestampNs, float x, float y, float z) {
        if (!finite(x) || !finite(y) || !finite(z) || timestampNs <= 0L) return;
        float length = (float)Math.sqrt(x*x + y*y + z*z);
        // Very low/high norms represent free fall/shakes rather than useful gravity.
        if (!finite(length) || length < 3f || length > 18f) return;
        if (accelTime != 0L && timestampNs <= accelTime) return;
        x /= length; y /= length; z /= length;
        if (!sampled || timestampNs-accelTime > STALE_NS) {
            gx = x; gy = y; gz = z; sampled = true;
            neutral = -gx; output = 0f;
            accelTime = outputTime = centerStart = timestampNs;
            gyroTime = 0L; centering = true;
            return;
        }
        float dt = seconds(timestampNs - accelTime);
        accelTime = timestampNs;
        boolean gyroRecent = gyroAvailable && gyroTime != 0L
                && Math.abs(timestampNs - gyroTime) < 150000000L;
        // Settle to the latest natural hold, rather than anchoring a noisy
        // first sample taken while tapping Play or picking up the phone.
        float tau = centering ? .04f : gyroRecent ? .35f : ACCEL_TAU[preset];
        float blend = 1f - (float)Math.exp(-dt / tau);
        gx += (x - gx) * blend; gy += (y - gy) * blend; gz += (z - gz) * blend;
        normalize();
        if (centering) {
            neutral = -gx; output = 0f; outputTime = timestampNs;
            if (timestampNs-centerStart >= SETTLE_NS) centering = false;
            return;
        }
        updateOutput(timestampNs);
    }

    public void acceptGyroscope(long timestampNs, float x, float y, float z) {
        if (!gyroAvailable || !finite(x) || !finite(y) || !finite(z)
                || timestampNs <= 0L || (gyroTime != 0L && timestampNs <= gyroTime)) return;
        long previous = gyroTime;
        gyroTime = timestampNs;
        if (!sampled || previous == 0L) return;
        if (timestampNs-accelTime > STALE_NS) { output = 0f; return; }
        if (centering) return;
        float dt = seconds(timestampNs - previous);
        if (dt > .15f) return;
        // Gravity is fixed in world space, so in the device frame it rotates by
        // -omega. Rodrigues' formula avoids sample-rate-dependent Euler drift.
        float speed = (float)Math.sqrt(x*x + y*y + z*z);
        if (!finite(speed) || speed > 20f) return;
        if (speed > .0001f) {
            float ax = -x/speed, ay = -y/speed, az = -z/speed;
            float angle = speed*dt, co = (float)Math.cos(angle), si = (float)Math.sin(angle);
            float dot = ax*gx + ay*gy + az*gz;
            float nx = gx*co + (ay*gz-az*gy)*si + ax*dot*(1f-co);
            float ny = gy*co + (az*gx-ax*gz)*si + ay*dot*(1f-co);
            float nz = gz*co + (ax*gy-ay*gx)*si + az*dot*(1f-co);
            gx = nx; gy = ny; gz = nz;
            normalize();
        }
        updateOutput(timestampNs);
    }

    private void updateOutput(long timestampNs) {
        if (timestampNs <= outputTime) return;
        float dt = Math.min(.1f, seconds(timestampNs-outputTime));
        outputTime = timestampNs;
        float tilt = -gx - neutral;
        float magnitude = Math.max(0f, Math.abs(tilt)-DEAD_ZONE[preset]);
        float target = clamp(Math.copySign(magnitude*GAIN[preset], tilt), -1400f, 1400f);
        // A level hold means brake now, not a slowly decaying old turn.
        if (magnitude == 0f) output = 0f;
        else output += (target-output)*(1f-(float)Math.exp(-dt/OUTPUT_TAU[preset]));
    }
    private void normalize() {
        float length = (float)Math.sqrt(gx*gx+gy*gy+gz*gz);
        if (length > .0001f) { gx /= length; gy /= length; gz /= length; }
    }
    private static float seconds(long ns) { return ns*1e-9f; }
    private static boolean finite(float v) { return !Float.isNaN(v) && !Float.isInfinite(v); }
    private static float clamp(float v, float lo, float hi) { return Math.max(lo, Math.min(hi, v)); }
}
