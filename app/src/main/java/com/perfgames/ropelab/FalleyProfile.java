package com.perfgames.ropelab;

/** Small portable profile following Frontline's preferences/result-record pattern. */
public final class FalleyProfile {
    public boolean motion = true, sound = true, haptics = true;
    public int preset = MotionSteering.BALANCED, bestScore, wins;
    public float fastestWin;

    public void sanitize() {
        if (preset < MotionSteering.LOOSE || preset > MotionSteering.TIGHT) preset = MotionSteering.BALANCED;
        bestScore = Math.max(0, bestScore);
        wins = Math.max(0, wins);
        if (!Float.isFinite(fastestWin) || fastestWin <= 0f) fastestWin = 0f;
    }

    /** The UI calls once per finished run. Losses can improve score but never add wins. */
    public boolean recordResult(boolean won, int score, float elapsed) {
        boolean changed = false;
        if (Math.max(0, score) > bestScore) { bestScore = score; changed = true; }
        if (won) {
            wins++;
            changed = true;
            if (Float.isFinite(elapsed) && elapsed > 0f && (fastestWin == 0f || elapsed < fastestWin))
                fastestWin = elapsed;
        }
        return changed;
    }
}
