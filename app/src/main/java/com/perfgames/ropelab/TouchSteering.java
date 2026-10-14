package com.perfgames.ropelab;

/** Portable gesture policy: taps jump, deliberate swipes control velocity. */
public final class TouchSteering {
    public static final float TAP_SLOP = 12f, MAX_VELOCITY = 340f;
    private float startX, startY, velocity;
    private boolean active, dragging;

    public void begin(float x, float y) {
        if (!Float.isFinite(x) || !Float.isFinite(y)) { cancel(); return; }
        startX = x; startY = y; velocity = 0; active = true; dragging = false;
    }
    public void move(float x, float y) {
        if (!active || !Float.isFinite(x) || !Float.isFinite(y)) return;
        float dx = x-startX, dy = y-startY;
        if (dx*dx+dy*dy >= TAP_SLOP*TAP_SLOP) dragging = true;
        velocity = dragging ? Math.max(-MAX_VELOCITY,Math.min(MAX_VELOCITY,dx*5f)) : 0;
    }
    /** Returns true only for a tap; release always clears steering immediately. */
    public boolean release(float x, float y) {
        if (!active) return false;
        if (!Float.isFinite(x) || !Float.isFinite(y)) { cancel(); return false; }
        move(x,y);
        boolean tap = !dragging;
        cancel();
        return tap;
    }
    public void cancel() { active = dragging = false; velocity = 0; }
    public boolean active() { return active; }
    public boolean dragging() { return dragging; }
    public float velocity() { return velocity; }
}
