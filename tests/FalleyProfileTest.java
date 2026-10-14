package com.perfgames.ropelab;

/** Local preference/result safety before Android SharedPreferences persistence. */
public final class FalleyProfileTest {
    private static int checks;
    private static void check(boolean ok, String message) {
        checks++;
        if (!ok) throw new AssertionError(message);
    }
    public static void main(String[] args) {
        FalleyProfile profile = new FalleyProfile();
        check(profile.motion && profile.sound && profile.haptics
                && profile.preset == MotionSteering.BALANCED,
                "a fresh profile enables feedback with Balanced motion feel");
        profile.motion = profile.sound = profile.haptics = false;
        profile.preset = MotionSteering.TIGHT;
        profile.bestScore = 250;
        profile.wins = 2;
        profile.fastestWin = 15.5f;
        profile.sanitize();
        check(!profile.motion && !profile.sound && !profile.haptics && profile.preset == MotionSteering.TIGHT
                && profile.bestScore == 250 && profile.wins == 2 && profile.fastestWin == 15.5f,
                "sanitizing loaded data preserves valid settings and records");
        profile.preset = 99;
        profile.bestScore = -5;
        profile.wins = -9;
        profile.fastestWin = Float.NaN;
        profile.sanitize();
        check(profile.preset == MotionSteering.BALANCED && profile.bestScore == 0
                && profile.wins == 0 && profile.fastestWin == 0,
                "corrupted local preference values restore safe defaults");
        for (float invalid : new float[] {Float.POSITIVE_INFINITY, -5, 0}) {
            profile.fastestWin = invalid;
            profile.sanitize();
            check(profile.fastestWin == 0, "invalid fastest times cannot enter local records");
        }
        check(profile.recordResult(false, 125, 8), "a new loss best updates the record");
        check(profile.bestScore == 125 && profile.wins == 0 && profile.fastestWin == 0,
                "losing can improve best score without inventing a win time");
        check(!profile.recordResult(false, 50, 6), "a lower loss result does not request an unnecessary save");
        check(profile.recordResult(true, 250, 18.5f), "a completed win updates records");
        check(profile.bestScore == 250 && profile.wins == 1 && profile.fastestWin == 18.5f,
                "the first win stores its count and finish time");
        profile.recordResult(true, 120, 22);
        check(profile.wins == 2 && profile.bestScore == 250 && profile.fastestWin == 18.5f,
                "a slower win increments wins without replacing better records");
        profile.recordResult(true, 50, 9);
        check(profile.wins == 3 && profile.bestScore == 250 && profile.fastestWin == 9,
                "a quicker win improves finish time independently of score");
        profile.recordResult(true, -1, Float.NaN);
        check(profile.wins == 4 && profile.bestScore == 250 && profile.fastestWin == 9,
                "an invalid result time cannot overwrite a valid fastest time");
        check(!profile.motion && !profile.sound && !profile.haptics,
                "recording gameplay never changes user control/feedback settings");
        check(profile.best.length == StageCatalog.COUNT && profile.stageWins.length == StageCatalog.COUNT
                && profile.stageTimes.length == StageCatalog.COUNT, "each chapter stage has its own local record");
        int firstStageBest = profile.best[0], firstStageWins = profile.stageWins[0];
        float firstStageTime = profile.stageTimes[0];
        profile.recordResult(1, true, 500, 8);
        check(profile.best[1] == 500 && profile.stageWins[1] == 1 && profile.stageTimes[1] == 8,
                "a new stage stores its own successful result");
        check(profile.best[0] == firstStageBest && profile.stageWins[0] == firstStageWins
                && profile.stageTimes[0] == firstStageTime, "another stage cannot overwrite previous stage records");
        profile.recordResult(2, false, 600, 4);
        check(profile.best[2] == 600 && profile.stageWins[2] == 0 && profile.stageTimes[2] == 0
                && profile.bestScore == 600 && profile.fastestWin == 8,
                "per-stage loss scores update independently without inventing successful finishes");
        profile.selectedStage = 99;
        profile.best[3] = -20;
        profile.stageWins[3] = -1;
        profile.stageTimes[3] = Float.POSITIVE_INFINITY;
        profile.sanitize();
        check(profile.selectedStage >= 0 && profile.selectedStage < StageCatalog.COUNT
                && profile.best[3] == 0 && profile.stageWins[3] == 0 && profile.stageTimes[3] == 0,
                "invalid saved stage selection and records sanitize into safe bounds");
        profile.selectedStage = 4;
        profile.recordResult(true, 10, 12);
        check(profile.stageWins[4] == 1 && profile.stageTimes[4] == 12 && profile.selectedStage == 4,
                "the compatibility result entry point records the persisted selected stage");
        System.out.println("PASS: local profile defaults/settings, sanitization, global and per-stage loss/win accounting, best score, fastest time, selected stage and independent records (" + checks + " checks)");
    }
}
