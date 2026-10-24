package com.perfgames.ropelab;

/** Six chapters: introduce, practise, combine, then defeat a chapter machine. */
public final class StageCatalog {
    public static final int LASER = 0, SPIKES = 1, FIRE = 2;
    public static final int COUNT = 24, CHAPTER_COUNT = 6;
    public static final int MOVING_TARGET = 1, SHRINKING_TARGET = 2, WIND_ZONES = 4,
            BOUNCE_PADS = 8, BREAKABLE_ANCHORS = 16, SLOW_PICKUPS = 32;
    public static final int LOW_GRAVITY = 1, GUSTS = 2, HEAVY_LOAD = 4, SINGLE_JUMP = 8;
    public static final int BOSS_NONE = 0, BOSS_LASER = 1, BOSS_SAW = 2, BOSS_FIRE = 3,
            BOSS_STORM = 4, BOSS_FORGE = 5, BOSS_CORE = 6;
    public static final class Stage {
        public final int index, chapterIndex, hazard, attackDifficulty, openingHazard;
        public final String title, chapterTitle;
        public final float groundY, targetCenter, targetWidth;
        public final boolean fireRain;
        public final int extraHazards, mechanics, modifiers, bossKind, parJumps;
        public final boolean fatalMiss;
        private Stage(int index, int chapter, String title, int hazard, float groundY,
                      float center, float width, int attacks, int difficulty, int mechanics,
                      int modifiers, int boss, int parJumps) {
            this.index = index; this.title = title; chapterIndex = chapter;
            chapterTitle = CHAPTERS[chapter]; this.hazard = hazard; this.groundY = groundY;
            targetCenter = center; targetWidth = width; extraHazards = attacks;
            attackDifficulty = difficulty; this.mechanics = mechanics; this.modifiers = modifiers;
            bossKind = boss; this.parJumps = parJumps; fatalMiss = chapter > 0;
            fireRain = (attacks & ArcadeRules.FIREBALLS) != 0;
            openingHazard = (attacks & ArcadeRules.BUZZSAWS) != 0 ? ArcadeRules.BUZZSAWS : 0;
        }
        public boolean hasMechanic(int flag) { return (mechanics & flag) != 0; }
        public boolean hasModifier(int flag) { return (modifiers & flag) != 0; }
    }
    private static final String[] CHAPTERS = {
        "Laser descent", "Spike canyon", "Fire fall", "Storm passage", "Blade foundry", "Last descent"
    };
    private static Stage s(int index, String title, float ground, float center, float width,
                           int attacks, int mechanics, int modifiers, int boss, int par) {
        int chapter = index / 4;
        int side = chapter == 1 || chapter == 4 ? SPIKES : chapter == 2 || chapter == 5 ? FIRE : LASER;
        return new Stage(index, chapter, title, side, ground, center, width, attacks,
                Math.min(2, Math.max(0, chapter - 1)), mechanics, modifiers, boss, par);
    }
    private static final Stage[] STAGES = {
        s(0, "First light", 4200, 240, 164, 0, 0, 0, 0, 3),
        s(1, "Catch the light", 4600, 240, 144, 0, MOVING_TARGET, 0, 0, 3),
        s(2, "Moon drift", 5000, 240, 144, ArcadeRules.TOP_LASERS, MOVING_TARGET, LOW_GRAVITY, 0, 4),
        s(3, "Prism keeper", 5600, 240, 144, 0, MOVING_TARGET, 0, BOSS_LASER, 4),
        s(4, "Spring canyon", 5600, 240, 142, 0, BOUNCE_PADS, 0, 0, 4),
        s(5, "Heavy footsteps", 6000, 240, 140, 0, BOUNCE_PADS, HEAVY_LOAD, 0, 4),
        s(6, "Spring pursuit", 6400, 240, 138, ArcadeRules.BUZZSAWS, BOUNCE_PADS | MOVING_TARGET, 0, 0, 5),
        s(7, "Sawtooth sentinel", 6800, 240, 140, 0, BOUNCE_PADS | MOVING_TARGET, 0, BOSS_SAW, 5),
        s(8, "Fraying point", 6600, 240, 142, 0, BREAKABLE_ANCHORS, 0, 0, 4),
        s(9, "Ember anchors", 7000, 240, 140, ArcadeRules.FIREBALLS, BREAKABLE_ANCHORS, HEAVY_LOAD, 0, 5),
        s(10, "One spark", 7400, 240, 140, ArcadeRules.FIREBALLS, BREAKABLE_ANCHORS | BOUNCE_PADS, SINGLE_JUMP, 0, 5),
        s(11, "Furnace heart", 8000, 240, 140, 0, BREAKABLE_ANCHORS | MOVING_TARGET, 0, BOSS_FIRE, 6),
        s(12, "Read the wind", 7600, 240, 144, 0, WIND_ZONES, 0, 0, 5),
        s(13, "Gust windows", 8000, 240, 140, ArcadeRules.LIGHTNING, WIND_ZONES, GUSTS, 0, 6),
        s(14, "Storm springs", 8400, 240, 138, ArcadeRules.TOP_LASERS, WIND_ZONES | BOUNCE_PADS, LOW_GRAVITY, 0, 6),
        s(15, "Thunder engine", 9000, 240, 140, 0, WIND_ZONES | MOVING_TARGET, 0, BOSS_STORM, 6),
        s(16, "Borrowed seconds", 8600, 240, 142, ArcadeRules.BUZZSAWS, SLOW_PICKUPS, 0, 0, 6),
        s(17, "Soft metal", 9000, 240, 140, ArcadeRules.BUZZSAWS, SLOW_PICKUPS | BREAKABLE_ANCHORS, HEAVY_LOAD, 0, 6),
        s(18, "Clockwork bounce", 9400, 240, 138, ArcadeRules.SIDE_LASERS, SLOW_PICKUPS | BOUNCE_PADS | MOVING_TARGET, SINGLE_JUMP, 0, 7),
        s(19, "Foundry guardian", 10000, 240, 140, 0, SLOW_PICKUPS | BREAKABLE_ANCHORS, 0, BOSS_FORGE, 7),
        s(20, "Closing circle", 9600, 240, 150, ArcadeRules.FIREBALLS, SHRINKING_TARGET | SLOW_PICKUPS, 0, 0, 7),
        s(21, "Living current", 10000, 240, 150, ArcadeRules.LIGHTNING, SHRINKING_TARGET | MOVING_TARGET | WIND_ZONES, GUSTS, 0, 8),
        s(22, "Last rehearsal", 10600, 240, 150, ArcadeRules.BUZZSAWS, SHRINKING_TARGET | BOUNCE_PADS | BREAKABLE_ANCHORS | SLOW_PICKUPS, LOW_GRAVITY, 0, 8),
        s(23, "The falling core", 11400, 240, 154, 0, MOVING_TARGET | SHRINKING_TARGET | WIND_ZONES | BOUNCE_PADS | BREAKABLE_ANCHORS | SLOW_PICKUPS, 0, BOSS_CORE, 8)
    };
    /** Bounded daily templates omit bosses, hard modifiers and compound attacks. */
    public static Stage daily(long seed) {
        int template = (int)Math.floorMod(seed ^ (seed >>> 32), 6L);
        int[] attacks = {0, ArcadeRules.TOP_LASERS, ArcadeRules.FIREBALLS,
                ArcadeRules.LIGHTNING, ArcadeRules.BUZZSAWS, ArcadeRules.SIDE_LASERS};
        int[] mechanics = {MOVING_TARGET, BOUNCE_PADS, BREAKABLE_ANCHORS,
                WIND_ZONES, SLOW_PICKUPS, SHRINKING_TARGET};
        float offset = Math.floorMod(seed >>> 8, 21L) - 10;
        return new Stage(template * 4, template, "Daily Drop", template == 1 ? SPIKES : LASER,
                5400 + template * 170, 240 + offset, 154, attacks[template], 0,
                mechanics[template], 0, BOSS_NONE, 5);
    }
    public static Stage get(int index) { return STAGES[Math.max(0, Math.min(COUNT - 1, index))]; }
    public static int firstStageInChapter(int chapter) { return Math.max(0, Math.min(CHAPTER_COUNT - 1, chapter)) * 4; }
    public static int stageCountInChapter(int chapter) { return 4; }
    private StageCatalog() { }
}
