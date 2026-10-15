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
        check(profile.blood && profile.arcadeDifficulty == 1 && profile.arcadeHazards == 15
                && profile.arcadeBest.length == 3 && profile.arcadeRuns.length == 3 && profile.arcadeLongest.length == 3,
                "new arcade preferences default to blood, Standard and all four hazards");
        profile.blood = false;
        profile.arcadeDifficulty = 2;
        profile.arcadeHazards = 6;
        profile.sanitize();
        check(!profile.blood && profile.arcadeDifficulty == 2 && profile.arcadeHazards == 6,
                "loaded blood/custom hazard/difficulty choices remain unchanged");
        profile.arcadeDifficulty = 99;
        profile.arcadeHazards = 16;
        profile.arcadeBest[0] = -20;
        profile.arcadeRuns[0] = -3;
        profile.arcadeLongest[0] = Float.NaN;
        profile.sanitize();
        check(profile.arcadeDifficulty == 2 && profile.arcadeHazards == 15 && profile.arcadeBest[0] == 0
                && profile.arcadeRuns[0] == 0 && profile.arcadeLongest[0] == 0,
                "invalid arcade preferences/records sanitize without leaving an empty hazard set");
        profile.arcadeDifficulty = -8;
        profile.arcadeHazards = 17;
        profile.sanitize();
        check(profile.arcadeDifficulty == 0 && profile.arcadeHazards == 1,
                "difficulty remains bounded and unknown mask bits are discarded while valid choices survive");
        int campaignBest = profile.bestScore, campaignWins = profile.wins;
        float campaignFastest = profile.fastestWin;
        int[] stageBest = profile.best.clone(), stageWins = profile.stageWins.clone();
        float[] stageTimes = profile.stageTimes.clone();
        check(profile.recordArcadeResult(0, 200, 10.5f) && profile.arcadeBest[0] == 200
                && profile.arcadeRuns[0] == 1 && profile.arcadeLongest[0] == 10.5f,
                "an arcade result records its own score, run and survival duration");
        profile.recordArcadeResult(0, 100, 15);
        check(profile.arcadeBest[0] == 200 && profile.arcadeRuns[0] == 2 && profile.arcadeLongest[0] == 15,
                "longest survival and best score improve independently");
        profile.recordArcadeResult(0, 300, Float.NaN);
        profile.recordArcadeResult(0, -9, Float.POSITIVE_INFINITY);
        check(profile.arcadeBest[0] == 300 && profile.arcadeRuns[0] == 4 && profile.arcadeLongest[0] == 15,
                "invalid scores/times cannot corrupt arcade records while completed runs still count");
        profile.recordArcadeResult(1, 50, 4);
        check(profile.arcadeBest[1] == 50 && profile.arcadeRuns[1] == 1 && profile.arcadeLongest[1] == 4
                && profile.arcadeBest[2] == 0 && profile.arcadeRuns[2] == 0 && profile.arcadeLongest[2] == 0,
                "each survival difficulty keeps separate records");
        check(profile.bestScore == campaignBest && profile.wins == campaignWins && profile.fastestWin == campaignFastest
                && java.util.Arrays.equals(profile.best, stageBest) && java.util.Arrays.equals(profile.stageWins, stageWins)
                && java.util.Arrays.equals(profile.stageTimes, stageTimes),
                "arcade survival never creates campaign wins or changes campaign records");
        check(!profile.blood && !profile.motion && !profile.sound && !profile.haptics,
                "arcade record updates preserve the user's feedback/control choices");
        System.out.println("PASS: local preferences/blood, campaign and per-stage records, Arcade mask/difficulty sanitization, independent survival score/runs/longest time and campaign isolation (" + checks + " checks)");
    }
}
