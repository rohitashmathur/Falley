package com.perfgames.ropelab;

/** Offline date generation, repeatable physics, unlimited attempts and rollback-safe daily streaks. */
public final class DailyDropTest {
    private static int checks;
    private static void check(boolean value, String message) {
        checks++; if (!value) throw new AssertionError(message);
    }
    private static void dates() {
        check(DailyDrop.utcDay(0) == 0 && DailyDrop.utcDay(86_399_999) == 0
                && DailyDrop.utcDay(86_400_000) == 1 && DailyDrop.utcDay(-1) == -1,
                "daily resets occur at UTC midnight, including correct negative floor division");
        long day = 20_000;
        check(DailyDrop.dateLabel(day).equals("2024-10-04"), "the date label is UTC ISO order");
        java.util.TimeZone previous = java.util.TimeZone.getDefault();
        java.util.Locale locale = java.util.Locale.getDefault();
        try {
            for (String zone : new String[] {"Asia/Kolkata", "Pacific/Honolulu", "Pacific/Kiritimati", "UTC"}) {
                java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone(zone));
                java.util.Locale.setDefault(new java.util.Locale("hi", "IN"));
                check(DailyDrop.dateLabel(day).equals("2024-10-04") && DailyDrop.seed(day) == DailyDrop.seed(day),
                        "timezone and language do not alter the course identifier or date");
            }
        } finally { java.util.TimeZone.setDefault(previous); java.util.Locale.setDefault(locale); }
        check(DailyDrop.courseId(day).contains("v" + DailyDrop.GENERATOR_VERSION), "sharing includes the generator version");
        java.util.HashSet<Long> seeds = new java.util.HashSet<>();
        for (long d = 18_000; d < 28_000; d++) check(seeds.add(DailyDrop.seed(d)), "supported consecutive days produce distinct seeds");
        check(!DailyDrop.validDay(DailyDrop.NO_DAY) && !DailyDrop.validDay(Long.MAX_VALUE), "invalid dates are rejected");
        boolean rejected = false; try { DailyDrop.seed(DailyDrop.NO_DAY); } catch (IllegalArgumentException expected) { rejected = true; }
        check(rejected, "a missing date is never silently used to generate a real course");
    }
    private static void records() {
        long day = 20_000; FalleyProfile p = new FalleyProfile();
        check(p.beginDaily(day) && p.dailyDay == day && p.dailyAttempts == 0, "opening the course does not consume an attempt");
        check(!p.beginDaily(day), "redrawing today's menu does not reset today's records");
        for (int i = 0; i < 100; i++) p.recordDaily(day, false, i, 9, 2, DailyDrop.TOUCH);
        check(p.dailyAttempts == 100 && p.dailyWins == 0 && p.dailyBestScore == 99 && p.dailyBestTime == 0,
                "daily attempts are unlimited and deaths never create a completion time");
        p.recordDaily(day, true, 120, 30, 6, DailyDrop.MOTION);
        check(p.dailyAttempts == 101 && p.dailyWins == 1 && p.dailyStreak == 1 && p.dailyBestStreak == 1
                && p.dailyBestTime == 30 && p.dailyBestJumps == 6 && p.dailyBestControlMode == DailyDrop.MOTION,
                "the first completed run records its actual control mode and starts a streak");
        p.recordDaily(day, true, 110, 25, 7, DailyDrop.MIXED);
        check(p.dailyStreak == 1 && p.dailyBestTime == 25 && p.dailyBestJumps == 6
                && p.dailyBestScore == 120 && p.dailyBestControlMode == DailyDrop.MIXED,
                "same-day replays improve independent records without inflating the streak");
        check(p.wins == 0 && p.bestScore == 0 && p.totalStars() == 0 && p.highestUnlockedStage() == 0,
                "daily runs cannot unlock campaign courses or cosmetics");
        p.recordDaily(day + 1, true, 10, 50, 8, DailyDrop.TOUCH);
        check(p.dailyDay == day + 1 && p.dailyAttempts == 1 && p.dailyBestScore == 10
                && p.dailyStreak == 2 && p.dailyBestStreak == 2, "consecutive UTC completion days extend the streak and start new day records");
        p.beginDaily(day + 4);
        check(p.dailyStreak == 0 && p.dailyBestStreak == 2, "missing days clear the current streak while retaining the best");
        p.recordDaily(day + 4, true, 50, 15, 1, DailyDrop.TOUCH);
        check(p.dailyStreak == 1 && p.dailyBestStreak == 2, "returning after a miss starts a fresh streak without taking away earned history");
        int attempts = p.dailyAttempts, wins = p.dailyWins;
        check(!p.beginDaily(day + 2) && !p.recordDaily(day + 2, true, 9999, 1, 0, DailyDrop.MOTION),
                "rolling the clock backwards cannot award a historical daily completion");
        check(p.dailyDay == day + 4 && p.dailyAttempts == attempts && p.dailyWins == wins
                && p.dailyStreak == 1 && p.dailyBestScore == 50, "rollback attempts do not corrupt the current course records");
        check(!p.recordDaily(Long.MAX_VALUE, true, 100, 1, 0, 0), "an invalid future date is refused without poisoning the watermark");
        p.recordDaily(day + 5, true, 60, 20, 4, DailyDrop.MIXED); p.sanitize();
        check(p.dailyStreak == 2 && p.dailyBestControlMode == DailyDrop.MIXED, "normal consecutive play resumes and persists mixed control metadata");
    }
    private static void repeatablePhysics() {
        java.util.HashSet<Integer> templates = new java.util.HashSet<>();
        for (long day = 20_000; day < 20_060; day++) {
            Physics first = new Physics(), second = new Physics(); long seed = DailyDrop.seed(day);
            first.resetDaily(seed); second.resetDaily(seed);
            check(first.daily && !first.arcade && first.stage.index == second.stage.index, "both clients select the same finite daily template");
            templates.add(first.stage.index);
            check(first.groundY() >= 3_000 && first.groundY() <= 12_000
                    && first.targetCenter() >= 140 && first.targetCenter() <= 340 && first.targetWidth() >= 75,
                    "daily templates stay within tested course and target bounds");
            float initialTarget = first.targetCenter(), initialWidth = first.targetWidth();
            first.touchControlled(true); second.touchControlled(true);
            for (int step = 0; step < 180; step++) {
                if (step == 10 || step == 60) { first.jump(); second.jump(); }
                float steering = (float) Math.sin(step * .03) * 30;
                first.steerTouch(steering); second.steerTouch(steering);
                first.step(); second.step();
                check(Float.floatToIntBits(first.x) == Float.floatToIntBits(second.x)
                        && Float.floatToIntBits(first.y) == Float.floatToIntBits(second.y)
                        && first.score == second.score && first.brokenCount == second.brokenCount,
                        "identical dated course and inputs reproduce the same physics");
            }
            int stage = first.stage.index;
            first.reset();
            check(first.daily && first.stage.index == stage && first.targetCenter() == initialTarget && first.targetWidth() == initialWidth
                    && first.score == 0 && first.elapsed == 0, "retry retains the day's course without generating another variation");
        }
        check(templates.size() == 6, "the date sampler reaches all six bounded course templates");
    }
    public static void main(String[] args) {
        dates(); records(); repeatablePhysics();
        System.out.println("PASS: UTC/versioned daily seeds, deterministic bounded template retries, unlimited attempts, isolated records and rollback-safe streaks (" + checks + " checks)");
    }
}
