package com.perfgames.ropelab;

/** Mastery stars and explicit twelve-to-twenty-four stage progress migration. */
public final class ProgressionV4Test {
    private static int checks;
    private static void check(boolean value, String message) {
        checks++; if (!value) throw new AssertionError(message);
    }
    private static void mastery() {
        FalleyProfile p = new FalleyProfile();
        int par = StageCatalog.get(0).parJumps;
        check(par >= 0 && par < 8, "intro par is an optional goal below its hard allowance");
        p.recordResult(0, false, 99, 2, 0, 240, 240, 100);
        check(p.stars[0] == 0 && p.totalStars() == 0, "a loss awards no mastery or completion stars");
        p.recordResult(0, true, 100, 20, par + 1, 200, 240, 100);
        check(p.stars[0] == FalleyProfile.COMPLETE, "a finish above par and outside center earns completion");
        p.recordResult(0, true, 110, 21, par, 200, 240, 100);
        check(p.stars[0] == (FalleyProfile.COMPLETE | FalleyProfile.PAR), "par may be earned independently on replay");
        p.recordResult(0, true, 90, 22, par + 2, 240, 240, 100);
        check(p.stars[0] == FalleyProfile.ALL_STARS && p.totalStars() == 3,
                "center may be earned on a separate replay without replacing the par star");
        p.recordResult(0, false, 120, 1, 0, 240, 240, 100);
        check(p.stars[0] == FalleyProfile.ALL_STARS && p.stageWins[0] == 3,
                "a later death neither removes stars nor awards another win");
        float halfCenter = 100 * FalleyProfile.PRECISION_FRACTION / 2;
        p.recordResult(1, true, 1, 1, 0, 240 + halfCenter, 240, 100);
        check((p.stars[1] & FalleyProfile.PRECISION) != 0, "the marked center boundary is inclusive");
        p.recordResult(2, true, 1, 1, -1, Float.NaN, 240, 100);
        check(p.stars[2] == FalleyProfile.COMPLETE, "missing jump/landing evidence cannot invent mastery");
        p.recordResult(3, true, 1, 1, 99, 240, 240, Float.POSITIVE_INFINITY);
        check(p.stars[3] == FalleyProfile.COMPLETE, "invalid target dimensions cannot award precision");
        int[] stars = p.stars.clone();
        p.recordArcadeResult(0, 9999, 99);
        p.recordDaily(20_000, true, 9999, 99, 0, DailyDrop.MOTION);
        check(java.util.Arrays.equals(stars, p.stars), "Arcade and Daily never mint campaign stars");
    }
    private static void migration() {
        check(StageCatalog.COUNT == 24 && StageCatalog.CHAPTER_COUNT == 6, "expanded campaign has six four-stage chapters");
        for (int prefix = 0; prefix <= 12; prefix++) {
            int[] best = new int[12], wins = new int[12]; float[] times = new float[12];
            for (int i = 0; i < 12; i++) { best[i] = 100 + i; if (i < prefix) { wins[i] = 2 + i; times[i] = 40 - i; } }
            FalleyProfile p = new FalleyProfile(); p.wins = 80; p.bestScore = 5000;
            p.migrateLegacyCampaign(best, wins, times, Math.min(11, prefix));
            check(p.wins == 80 && p.bestScore == 5000, "migration leaves aggregate historical records intact");
            check(p.progressionVersion == FalleyProfile.PROGRESSION_VERSION, "migration marks the new storage format");
            for (int i = 0; i < 12; i++) {
                int mapped = FalleyProfile.legacyStageIndex(i);
                check(mapped == i / 2 * 4 + (i % 2 == 0 ? 0 : 3), "legacy layout map is explicit and stable");
                check(p.best[mapped] == best[i] && p.stageWins[mapped] == wins[i]
                        && p.stageTimes[mapped] == times[i], "each old score, win count and time survives on its mapped course");
            }
            int expectedFrontier = Math.min(23, (prefix / 2) * 4 + (prefix % 2));
            check(p.highestUnlockedStage() == expectedFrontier, "previously cleared chapters remain open without skipping new uncleared stages");
            for (int i = 0; i < 24; i++)
                check(p.stars[i] == (p.stageWins[i] > 0 ? FalleyProfile.COMPLETE : 0),
                        "legacy completions never fabricate optional par or precision achievements");
            for (int chapter = 0; chapter < 6; chapter++)
                check(p.isChapterComplete(chapter) == (chapter < prefix / 2), "chapter reward access is preserved for the cleared prefix");
        }
        FalleyProfile sparse = new FalleyProfile();
        int[] sparseWins = new int[12]; sparseWins[7] = 3;
        sparse.migrateLegacyCampaign(new int[12], sparseWins, new float[12], 7);
        check(sparse.stageWins[15] == 3 && sparse.highestUnlockedStage() == 0 && sparse.selectedStage == 0,
                "out-of-order historical records are retained but cannot bypass the sequential campaign");
    }
    private static void badStorage() {
        FalleyProfile p = new FalleyProfile(); p.stars[0] = 7; p.stars[1] = 99; p.stageWins[1] = 1;
        p.dailyDay = Long.MAX_VALUE; p.lastObservedDay = -2; p.lastDailyCompletedDay = -1;
        p.dailyAttempts = -1; p.dailyWins = 100; p.dailyBestTime = Float.NaN; p.dailyBestJumps = -9;
        p.dailyStreak = -8; p.dailyBestStreak = -2; p.dailyBestControlMode = 99; p.sanitize();
        check(p.stars[0] == 0 && p.stars[1] == 3, "invalid star bits are removed and wins retain completion");
        check(p.dailyDay == DailyDrop.NO_DAY && p.lastObservedDay == DailyDrop.NO_DAY
                && p.lastDailyCompletedDay == DailyDrop.NO_DAY, "corrupt dates cannot poison the clock watermark");
        check(p.dailyAttempts == 0 && p.dailyWins == 0 && p.dailyBestTime == 0 && p.dailyBestJumps == -1
                && p.dailyStreak == 0 && p.dailyBestStreak == 0 && p.dailyBestControlMode == DailyDrop.TOUCH,
                "invalid daily preferences restore safe independent defaults");
        p.wins = p.stageWins[0] = Integer.MAX_VALUE;
        p.recordResult(0, true, 0, 1);
        check(p.wins == Integer.MAX_VALUE && p.stageWins[0] == Integer.MAX_VALUE,
                "lifetime counters saturate rather than wrapping negative");
    }
    public static void main(String[] args) {
        mastery(); migration(); badStorage();
        System.out.println("PASS: independent cumulative mastery stars, explicit legacy migration, progress preservation and corrupt preference safety (" + checks + " checks)");
    }
}
