package com.perfgames.ropelab;

/** Fully unlocked campaign: two focused descents in each of six chapters. */
public final class StageCatalog {
    public static final int LASER = 0, SPIKES = 1, FIRE = 2;
    public static final int COUNT = 12, CHAPTER_COUNT = 6;
    public static final class Stage {
        public final int index, chapterIndex, hazard, attackDifficulty, openingHazard;
        public final String title, chapterTitle;
        public final float groundY, targetCenter, targetWidth;
        public final boolean fireRain;
        public final int extraHazards;
        public final boolean fatalMiss;
        private Stage(int index, int chapterIndex, String title, String chapterTitle, int hazard, float groundY,
                      boolean rain, float targetCenter, float targetWidth, int extraHazards, int attackDifficulty,
                      int openingHazard, boolean fatalMiss) {
            this.index = index; this.title = title; this.chapterIndex = chapterIndex;
            this.chapterTitle = chapterTitle; this.hazard = hazard; this.groundY = groundY; this.fireRain = rain;
            this.targetCenter = targetCenter; this.targetWidth = targetWidth;
            this.extraHazards = extraHazards; this.attackDifficulty = attackDifficulty;
            this.openingHazard = openingHazard; this.fatalMiss = fatalMiss;
        }
    }
    private static final Stage[] STAGES = {
        // The original six indices, layouts and attack rules retain existing local records.
        new Stage(0, 0, "First light", "Laser descent", LASER, 4200, false, 240, 164, 0, 0, 0, false),
        new Stage(1, 0, "Tight beam", "Laser descent", LASER, 5000, false, 210, 90,
                ArcadeRules.TOP_LASERS, 0, 0, false),
        new Stage(2, 1, "Sharp turn", "Spike canyon", SPIKES, 5800, false, 270, 86, 0, 1, 0, false),
        new Stage(3, 1, "Needle run", "Spike canyon", SPIKES, 6600, false, 210, 82,
                ArcadeRules.SIDE_LASERS | ArcadeRules.BUZZSAWS, 1, ArcadeRules.BUZZSAWS, true),
        new Stage(4, 2, "Warm welcome", "Fire fall", FIRE, 7400, false, 270, 82,
                ArcadeRules.LIGHTNING, 2, 0, true),
        new Stage(5, 2, "Sky on fire", "Fire fall", FIRE, 8200, true, 240, 100,
                ArcadeRules.ALL, 2, 0, true),
        // Specialist introductions give a longer warning before their paired mixed stage.
        new Stage(6, 3, "Crosswind", "Storm passage", LASER, 8800, false, 220, 112,
                ArcadeRules.LIGHTNING, 1, 0, true),
        new Stage(7, 3, "Storm window", "Storm passage", LASER, 9400, false, 260, 104,
                ArcadeRules.LIGHTNING | ArcadeRules.TOP_LASERS, 2, 0, true),
        // A shorter foundry opening shifts the challenge to reading moving blades.
        new Stage(8, 4, "Slow blades", "Blade foundry", SPIKES, 9000, false, 240, 112,
                ArcadeRules.BUZZSAWS, 1, 0, true),
        new Stage(9, 4, "Factory line", "Blade foundry", SPIKES, 9800, false, 220, 104,
                ArcadeRules.BUZZSAWS | ArcadeRules.SIDE_LASERS, 2, 0, true),
        new Stage(10, 5, "Ember crossing", "Last descent", FIRE, 10400, true, 260, 100,
                ArcadeRules.FIREBALLS | ArcadeRules.LIGHTNING, 2, 0, true),
        new Stage(11, 5, "Final descent", "Last descent", FIRE, 10800, true, 240, 100,
                ArcadeRules.ALL, 2, 0, true)
    };
    public static Stage get(int index) { return STAGES[Math.max(0, Math.min(COUNT - 1, index))]; }
    public static int firstStageInChapter(int chapterIndex) {
        int chapter = Math.max(0, Math.min(CHAPTER_COUNT - 1, chapterIndex));
        for (Stage stage : STAGES) if (stage.chapterIndex == chapter) return stage.index;
        throw new IllegalStateException("Chapter has no stages: " + chapter);
    }
    public static int stageCountInChapter(int chapterIndex) {
        int chapter = Math.max(0, Math.min(CHAPTER_COUNT - 1, chapterIndex)), count = 0;
        for (Stage stage : STAGES) if (stage.chapterIndex == chapter) count++;
        return count;
    }
    private StageCatalog() { }
}
