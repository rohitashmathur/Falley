package com.perfgames.ropelab;

/** Portable local preferences plus independent campaign and arcade records. */
public final class FalleyProfile {
    public boolean motion = true, sound = true, haptics = true;
    public int preset = MotionSteering.BALANCED, bestScore, wins;
    public float fastestWin;
    public int selectedStage;
    public final int[] best = new int[StageCatalog.COUNT], stageWins = new int[StageCatalog.COUNT];
    public final float[] stageTimes = new float[StageCatalog.COUNT];
    public boolean blood = true;
    public int arcadeDifficulty = 1, arcadeHazards = ArcadeRules.ALL;
    public final int[] arcadeBest = new int[3], arcadeRuns = new int[3];
    public final float[] arcadeLongest = new float[3];

    public void sanitize() {
        if (preset < MotionSteering.LOOSE || preset > MotionSteering.TIGHT) preset = MotionSteering.BALANCED;
        bestScore = Math.max(0, bestScore);
        wins = Math.max(0, wins);
        if (!Float.isFinite(fastestWin) || fastestWin <= 0f) fastestWin = 0f;
        selectedStage = Math.max(0, Math.min(StageCatalog.COUNT-1, selectedStage));
        arcadeDifficulty = Math.max(0, Math.min(2, arcadeDifficulty));
        arcadeHazards = ArcadeRules.normalizeMask(arcadeHazards);
        for (int i = 0; i < arcadeBest.length; i++) {
            arcadeBest[i] = Math.max(0, arcadeBest[i]);
            arcadeRuns[i] = Math.max(0, arcadeRuns[i]);
            if (!Float.isFinite(arcadeLongest[i]) || arcadeLongest[i] <= 0f) arcadeLongest[i] = 0f;
        }
        for (int i = 0; i < StageCatalog.COUNT; i++) {
            best[i] = Math.max(0, best[i]);
            stageWins[i] = Math.max(0, stageWins[i]);
            if (!Float.isFinite(stageTimes[i]) || stageTimes[i] <= 0f) stageTimes[i] = 0f;
        }
    }

    /** The UI calls once per finished run. Losses can improve score but never add wins. */
    public boolean recordResult(boolean won, int score, float elapsed) {
        return recordResult(selectedStage, won, score, elapsed);
    }
    public boolean recordResult(int stageIndex, boolean won, int score, float elapsed) {
        int index = Math.max(0, Math.min(StageCatalog.COUNT-1, stageIndex));
        boolean changed = false;
        if (Math.max(0, score) > bestScore) { bestScore = score; changed = true; }
        if (Math.max(0, score) > best[index]) { best[index] = score; changed = true; }
        if (won) {
            wins++;
            stageWins[index]++;
            changed = true;
            if (Float.isFinite(elapsed) && elapsed > 0f && (fastestWin == 0f || elapsed < fastestWin))
                fastestWin = elapsed;
            if (Float.isFinite(elapsed) && elapsed > 0f && (stageTimes[index] == 0f || elapsed < stageTimes[index]))
                stageTimes[index] = elapsed;
        }
        return changed;
    }
    /** Called once for an arcade death; campaign wins/finish times remain independent. */
    public boolean recordArcadeResult(int difficulty, int score, float elapsed) {
        int index = Math.max(0, Math.min(2, difficulty));
        arcadeRuns[index]++;
        arcadeBest[index] = Math.max(arcadeBest[index], Math.max(0, score));
        if (Float.isFinite(elapsed) && elapsed > arcadeLongest[index]) arcadeLongest[index] = elapsed;
        return true;
    }
}
