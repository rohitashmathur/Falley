package com.perfgames.ropelab;

/** Portable local preferences plus independent campaign and arcade records. */
public final class FalleyProfile {
    public static final int PROGRESSION_VERSION = 2;
    public static final int COMPLETE = 1, PAR = 2, PRECISION = 4, ALL_STARS = 7;
    public static final float PRECISION_FRACTION = .35f;
    public boolean motion = true, sound = true, haptics = true, music = true;
    public boolean languageConfirmed;
    public int preset = MotionSteering.LOOSE, bestScore, wins;
    public int language = FalleyStrings.ENGLISH;
    public float fastestWin;
    public int selectedStage;
    public final int[] best = new int[StageCatalog.COUNT], stageWins = new int[StageCatalog.COUNT];
    public final float[] stageTimes = new float[StageCatalog.COUNT];
    public final int[] stars = new int[StageCatalog.COUNT];
    public int progressionVersion = PROGRESSION_VERSION;
    public int selectedAvatar, selectedPalette, selectedTrail, selectedLanding;
    /** Legacy equipment preference retained for save compatibility; mixed ropes ignore it. */
    public int selectedRope;
    public int avatarRewardsVersion=AvatarRewards.VERSION;
    public int avatarRewardsSeenMask=AvatarRewards.STARTER_MASK, avatarRewardsPendingMask;
    /** In-memory retry marker; only a successful durable reward save clears it. */
    public boolean avatarRewardsNeedCommit;
    public long dailyDay = DailyDrop.NO_DAY, lastDailyCompletedDay = DailyDrop.NO_DAY,
            lastObservedDay = DailyDrop.NO_DAY;
    public int dailyBestScore, dailyAttempts, dailyWins, dailyBestJumps = -1,
            dailyBestControlMode = DailyDrop.TOUCH, dailyStreak, dailyBestStreak;
    public float dailyBestTime;
    public int dailyGeneratorVersion=DailyDrop.GENERATOR_VERSION;
    public boolean blood = true;
    public int arcadeDifficulty = 1, arcadeHazards = ArcadeRules.ALL;
    public final int[] arcadeBest = new int[3], arcadeRuns = new int[3];
    public final float[] arcadeLongest = new float[3];

    public void sanitize() {
        language = FalleyStrings.sanitize(language);
        if (preset < MotionSteering.LOOSE || preset > MotionSteering.TIGHT) preset = MotionSteering.LOOSE;
        bestScore = Math.max(0, bestScore);
        wins = Math.max(0, wins);
        if (!Float.isFinite(fastestWin) || fastestWin <= 0f) fastestWin = 0f;
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
            stars[i] = stageWins[i] > 0 ? ((Math.max(0, stars[i]) & ALL_STARS) | COMPLETE) : 0;
        }
        selectedStage = Math.max(0, Math.min(highestUnlockedStage(), selectedStage));
        if (!Cosmetics.isAvatarUnlocked(this, selectedAvatar)) selectedAvatar = AvatarCatalog.HUMAN;
        if (!Cosmetics.isPaletteUnlocked(this, selectedPalette)) selectedPalette = 0;
        selectedRope=RopeCatalog.normalize(selectedRope);

        if (!Cosmetics.isTrailUnlocked(this, selectedTrail)) selectedTrail = 0;
        if (!Cosmetics.isLandingUnlocked(this, selectedLanding)) selectedLanding = 0;
        if(dailyGeneratorVersion!=DailyDrop.GENERATOR_VERSION){
            dailyBestScore=dailyAttempts=dailyWins=0;dailyBestJumps=-1;dailyBestTime=0;dailyBestControlMode=DailyDrop.TOUCH;
            dailyGeneratorVersion=DailyDrop.GENERATOR_VERSION;
        }
        if (!DailyDrop.validDay(dailyDay)) dailyDay = DailyDrop.NO_DAY;
        if (!DailyDrop.validDay(lastDailyCompletedDay)) lastDailyCompletedDay = DailyDrop.NO_DAY;
        if (!DailyDrop.validDay(lastObservedDay)) lastObservedDay = DailyDrop.NO_DAY;
        if (dailyDay != DailyDrop.NO_DAY) lastObservedDay = Math.max(lastObservedDay, dailyDay);
        if (lastDailyCompletedDay != DailyDrop.NO_DAY)
            lastObservedDay = Math.max(lastObservedDay, lastDailyCompletedDay);
        dailyBestScore = Math.max(0, dailyBestScore);
        dailyAttempts = Math.max(0, dailyAttempts);
        dailyWins = Math.max(0, Math.min(dailyAttempts, dailyWins));
        dailyBestJumps = Math.max(-1, dailyBestJumps);
        if (!Float.isFinite(dailyBestTime) || dailyBestTime <= 0f) dailyBestTime = 0f;
        if (dailyWins == 0) { dailyBestTime = 0; dailyBestJumps = -1; }
        dailyBestControlMode = DailyDrop.controlMode(dailyBestControlMode);
        dailyStreak = Math.max(0, dailyStreak);
        dailyBestStreak = Math.max(dailyStreak, Math.max(0, dailyBestStreak));
        if (lastDailyCompletedDay == DailyDrop.NO_DAY) dailyStreak = 0;
        if (dailyDay == DailyDrop.NO_DAY) {
            dailyBestScore = dailyAttempts = dailyWins = 0;
            dailyBestJumps = -1; dailyBestTime = 0; dailyBestControlMode = DailyDrop.TOUCH;
        }
        progressionVersion = PROGRESSION_VERSION;
        AvatarRewards.sanitize(this);
    }

    /** Stage zero is open; each contiguous recorded win opens the following stage. */
    public int highestUnlockedStage() {
        int next = 0;
        while (next < StageCatalog.COUNT && stageWins[next] > 0) next++;
        return Math.min(StageCatalog.COUNT - 1, next);
    }
    public boolean isStageUnlocked(int stageIndex) {
        return stageIndex >= 0 && stageIndex < StageCatalog.COUNT && stageIndex <= highestUnlockedStage();
    }
    public boolean isChapterUnlocked(int chapterIndex) {
        return chapterIndex >= 0 && chapterIndex < StageCatalog.CHAPTER_COUNT
                && isStageUnlocked(StageCatalog.firstStageInChapter(chapterIndex));
    }
    public boolean isChapterComplete(int chapterIndex) {
        if (chapterIndex < 0 || chapterIndex >= StageCatalog.CHAPTER_COUNT) return false;
        int first = StageCatalog.firstStageInChapter(chapterIndex);
        for (int i = first; i < first + StageCatalog.stageCountInChapter(chapterIndex); i++)
            if (stageWins[i] <= 0) return false;
        return true;
    }
    public int totalStars() {
        int count = 0;
        for (int i = 0; i < stars.length; i++)
            if (stageWins[i] > 0) count += Integer.bitCount((Math.max(0, stars[i]) & ALL_STARS) | COMPLETE);
        return count;
    }

    /** Twelve-stage records map to each expanded chapter's introduction and finale. */
    public static int legacyStageIndex(int oldIndex) {
        int index = Math.max(0, Math.min(11, oldIndex));
        return index / 2 * 4 + (index % 2 == 0 ? 0 : 3);
    }
    public void migrateLegacyCampaign(int[] oldBest, int[] oldWins, float[] oldTimes, int oldSelected) {
        java.util.Arrays.fill(best, 0); java.util.Arrays.fill(stageWins, 0);
        java.util.Arrays.fill(stageTimes, 0); java.util.Arrays.fill(stars, 0);
        int prefix = 0;
        while (prefix < 12 && prefix < oldWins.length && oldWins[prefix] > 0) prefix++;
        for (int i = 0; i < 12; i++) {
            int mapped = legacyStageIndex(i);
            if (mapped >= StageCatalog.COUNT) continue;
            best[mapped] = i < oldBest.length ? Math.max(0, oldBest[i]) : 0;
            stageWins[mapped] = i < oldWins.length ? Math.max(0, oldWins[i]) : 0;
            float time = i < oldTimes.length ? oldTimes[i] : 0;
            stageTimes[mapped] = Float.isFinite(time) && time > 0 ? time : 0;
            if (stageWins[mapped] > 0) stars[mapped] = COMPLETE;
        }
        // Previously cleared chapters stay cleared. There is no invented jump or precision record.
        for (int chapter = 0; chapter < prefix / 2; chapter++) {
            for (int i = chapter * 4; i < chapter * 4 + 4 && i < StageCatalog.COUNT; i++) {
                stageWins[i] = Math.max(1, stageWins[i]); stars[i] = COMPLETE;
            }
        }
        selectedStage = legacyStageIndex(oldSelected);
        progressionVersion = PROGRESSION_VERSION;
        sanitize();
    }

    /** The UI calls once per finished run. Losses can improve score but never add wins. */
    public boolean recordResult(boolean won, int score, float elapsed) {
        return recordResult(selectedStage, won, score, elapsed);
    }
    public boolean recordResult(int stageIndex, boolean won, int score, float elapsed) {
        int index = Math.max(0, Math.min(StageCatalog.COUNT-1, stageIndex));
        if(won)AvatarRewards.sanitize(this);
        int avatarsBefore=won?AvatarRewards.unlockedMask(this):AvatarRewards.STARTER_MASK;
        boolean changed = false;
        if (Math.max(0, score) > bestScore) { bestScore = score; changed = true; }
        if (Math.max(0, score) > best[index]) { best[index] = score; changed = true; }
        if (won) {
            wins = increment(wins);
            stageWins[index] = increment(stageWins[index]);
            stars[index] |= COMPLETE;
            changed = true;
            if (Float.isFinite(elapsed) && elapsed > 0f && (fastestWin == 0f || elapsed < fastestWin))
                fastestWin = elapsed;
            if (Float.isFinite(elapsed) && elapsed > 0f && (stageTimes[index] == 0f || elapsed < stageTimes[index]))
                stageTimes[index] = elapsed;
            AvatarRewards.noteUnlocked(this,avatarsBefore);
        }
        return changed;
    }
    /** Independent mastery goals accumulate across successful runs, never across deaths. */
    public boolean recordResult(int stageIndex, boolean won, int score, float elapsed, int jumpsUsed,
                                float landingX, float landingCenter, float landingWidth) {
        boolean changed = recordResult(stageIndex, won, score, elapsed);
        if (!won) return changed;
        int index = Math.max(0, Math.min(StageCatalog.COUNT - 1, stageIndex));
        if (jumpsUsed >= 0 && jumpsUsed <= StageCatalog.get(index).parJumps) stars[index] |= PAR;
        if (Float.isFinite(landingX) && Float.isFinite(landingCenter) && Float.isFinite(landingWidth)
                && landingWidth > 0 && Math.abs(landingX - landingCenter) <= landingWidth * PRECISION_FRACTION / 2)
            stars[index] |= PRECISION;
        return true;
    }

    /** Opening today's course resets today's records only; attempts remain unlimited. */
    public boolean beginDaily(long day) {
        if (!DailyDrop.validDay(day) || day < lastObservedDay) return false;
        boolean changed = day != dailyDay || day != lastObservedDay;
        lastObservedDay = day;
        if (day != dailyDay) {
            dailyDay = day; dailyBestScore = dailyAttempts = dailyWins = 0;
            dailyBestJumps = -1; dailyBestTime = 0; dailyBestControlMode = DailyDrop.TOUCH;
        }
        if (lastDailyCompletedDay != DailyDrop.NO_DAY && day > lastDailyCompletedDay + 1 && dailyStreak != 0) {
            dailyStreak = 0; changed = true;
        }
        return changed;
    }

    /** Call once for a finished run. A clock rollback may be played but cannot award records/streaks. */
    public boolean recordDaily(long day, boolean won, int score, float elapsed, int jumpsUsed, int controlMode) {
        if (!DailyDrop.validDay(day) || day < lastObservedDay) return false;
        beginDaily(day);
        dailyAttempts = increment(dailyAttempts);
        dailyBestScore = Math.max(dailyBestScore, Math.max(0, score));
        if (won) {
            dailyWins = increment(dailyWins);
            if (jumpsUsed >= 0 && (dailyBestJumps < 0 || jumpsUsed < dailyBestJumps)) dailyBestJumps = jumpsUsed;
            if (Float.isFinite(elapsed) && elapsed > 0 && (dailyBestTime == 0 || elapsed < dailyBestTime)) {
                dailyBestTime = elapsed;
                dailyBestControlMode = DailyDrop.controlMode(controlMode);
            }
            if (day != lastDailyCompletedDay) {
                dailyStreak = lastDailyCompletedDay != DailyDrop.NO_DAY && day == lastDailyCompletedDay + 1
                        ? increment(dailyStreak) : 1;
                dailyBestStreak = Math.max(dailyBestStreak, dailyStreak);
                lastDailyCompletedDay = day;
            }
        }
        return true;
    }
    /** Called once for an arcade death; campaign wins/finish times remain independent. */
    public boolean recordArcadeResult(int difficulty, int score, float elapsed) {
        int index = Math.max(0, Math.min(2, difficulty));
        arcadeRuns[index] = increment(arcadeRuns[index]);
        arcadeBest[index] = Math.max(arcadeBest[index], Math.max(0, score));
        if (Float.isFinite(elapsed) && elapsed > arcadeLongest[index]) arcadeLongest[index] = elapsed;
        return true;
    }
    private static int increment(int value) { return value == Integer.MAX_VALUE ? value : Math.max(0, value) + 1; }
}
