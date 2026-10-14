package com.perfgames.ropelab;

/** Small, fully unlocked prototype campaign. All chapters share the same controls. */
public final class StageCatalog {
    public static final int LASER = 0, SPIKES = 1, FIRE = 2, COUNT = 6;
    public static final class Stage {
        public final int index, chapterIndex, hazard;
        public final String title, chapterTitle;
        public final float groundY, targetCenter, targetWidth;
        public final boolean fireRain;
        private Stage(int index, String title, String chapterTitle, int hazard, float groundY, boolean rain,
                      float targetCenter, float targetWidth) {
            this.index = index; this.title = title; this.chapterIndex = index / 2;
            this.chapterTitle = chapterTitle; this.hazard = hazard; this.groundY = groundY; this.fireRain = rain;
            this.targetCenter = targetCenter; this.targetWidth = targetWidth;
        }
    }
    private static final Stage[] STAGES = {
        new Stage(0, "First light", "Laser descent", LASER, 4200, false, 240, 164),
        new Stage(1, "Tight beam", "Laser descent", LASER, 5000, false, 210, 90),
        new Stage(2, "Sharp turn", "Spike canyon", SPIKES, 5800, false, 270, 86),
        new Stage(3, "Needle run", "Spike canyon", SPIKES, 6600, false, 210, 82),
        new Stage(4, "Warm welcome", "Fire fall", FIRE, 7400, false, 270, 82),
        new Stage(5, "Sky on fire", "Fire fall", FIRE, 8200, true, 240, 100)
    };
    public static Stage get(int index) { return STAGES[Math.max(0, Math.min(COUNT - 1, index))]; }
    private StageCatalog() { }
}
