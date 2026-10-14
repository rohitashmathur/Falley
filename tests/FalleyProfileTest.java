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
        System.out.println("PASS: local profile defaults, valid settings preservation, corrupted record sanitization, loss/win accounting, best score, fastest time and settings independence (" + checks + " checks)");
    }
}
