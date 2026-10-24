package com.perfgames.ropelab;

/** Earned appearance choices never change collision size, physics, controls or stage access. */
public final class Cosmetics {
    public static final int PALETTE_COUNT = 4, TRAIL_COUNT = 4, LANDING_COUNT = 4;
    private static final int[] PALETTE_STARS = {0, 6, 18, 36};
    private static final int[] TRAIL_STARS = {0, 12, 30, 48};
    private static final int[] LANDING_STARS = {0, 9, 24, 54};
    private static final String[] PALETTE_NAMES = {"Original", "Mint", "Violet", "Gold"};
    private static final String[] TRAIL_NAMES = {"None", "Spark", "Comet", "Prism"};
    private static final String[] LANDING_NAMES = {"Ripple", "Bloom", "Starburst", "Nova"};

    public static boolean isAvatarUnlocked(FalleyProfile profile, int avatar) {
        if (avatar < 0 || avatar >= AvatarCatalog.COUNT) return false;
        if (avatar == AvatarCatalog.HUMAN) return true;
        // Starter Human plus one appearance after each of the six chapter finales.
        int chapter = avatar - 1;
        return chapter < StageCatalog.CHAPTER_COUNT && profile.isChapterComplete(chapter);
    }
    public static boolean isPaletteUnlocked(FalleyProfile p, int id) {
        return unlocked(p, id, PALETTE_STARS);
    }
    public static boolean isTrailUnlocked(FalleyProfile p, int id) {
        return unlocked(p, id, TRAIL_STARS);
    }
    public static boolean isLandingUnlocked(FalleyProfile p, int id) {
        return unlocked(p, id, LANDING_STARS);
    }
    private static boolean unlocked(FalleyProfile p, int id, int[] requirements) {
        return id >= 0 && id < requirements.length && p.totalStars() >= requirements[id];
    }
    public static int paletteStars(int id) { return requirement(id, PALETTE_STARS); }
    public static int trailStars(int id) { return requirement(id, TRAIL_STARS); }
    public static int landingStars(int id) { return requirement(id, LANDING_STARS); }
    private static int requirement(int id, int[] values) {
        return values[Math.max(0, Math.min(values.length - 1, id))];
    }
    public static String paletteName(int id) { return name(id, PALETTE_NAMES); }
    public static String trailName(int id) { return name(id, TRAIL_NAMES); }
    public static String landingName(int id) { return name(id, LANDING_NAMES); }
    private static String name(int id, String[] names) { return names[Math.max(0, Math.min(names.length - 1, id))]; }
    private Cosmetics() { }
}
