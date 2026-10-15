package com.perfgames.ropelab;

/** Shared controls and attack timing for the offline survival arena. */
public final class ArcadeRules {
    public static final int FIREBALLS = 1, TOP_LASERS = 2, SIDE_LASERS = 4, LIGHTNING = 8, BUZZSAWS = 16, ALL = 31;
    public static final String[] DIFFICULTIES = {"CHILL", "STANDARD", "BRUTAL"};
    public static int clampDifficulty(int difficulty) { return Math.max(0, Math.min(2, difficulty)); }
    public static int normalizeMask(int mask) { int valid = mask & ALL; return valid == 0 ? ALL : valid; }
    public static float warning(int difficulty, int wave) {
        return Math.max(.80f, 1.45f - clampDifficulty(difficulty) * .20f - Math.min(10, Math.max(0, wave - 1)) * .025f);
    }
    public static float attackGap(int difficulty, int wave) {
        return Math.max(.50f, 1.65f - clampDifficulty(difficulty) * .35f - Math.min(10, Math.max(0, wave - 1)) * .055f);
    }
    public static float beamLife(int difficulty) { return .28f + clampDifficulty(difficulty) * .06f; }
    private ArcadeRules() { }
}
