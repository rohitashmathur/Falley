package com.perfgames.ropelab;

/** Appearance unlocks are earned locally and can never modify campaign or Arcade results. */
public final class CosmeticsTest {
    private static int checks;
    private static void check(boolean value, String message) {
        checks++; if (!value) throw new AssertionError(message);
    }
    private static void earnStars(FalleyProfile p, int count) {
        java.util.Arrays.fill(p.stageWins, 0); java.util.Arrays.fill(p.stars, 0);
        for (int i = 0; i < p.stars.length && count > 0; i++) {
            p.stageWins[i] = 1; int amount = Math.min(3, count);
            p.stars[i] = amount == 3 ? 7 : amount == 2 ? 3 : 1; count -= amount;
        }
    }
    public static void main(String[] args) {
        FalleyProfile p = new FalleyProfile();
        check(AvatarCatalog.COUNT == StageCatalog.CHAPTER_COUNT + 1, "starter plus one earned avatar per chapter");
        check(Cosmetics.isAvatarUnlocked(p, AvatarCatalog.HUMAN) && !Cosmetics.isAvatarUnlocked(p, -1)
                && !Cosmetics.isAvatarUnlocked(p, AvatarCatalog.COUNT), "Human is free and invalid avatar ids are unavailable");
        for (int avatar = 1; avatar < AvatarCatalog.COUNT; avatar++) check(!Cosmetics.isAvatarUnlocked(p, avatar), "future chapter avatars start locked");
        for (int chapter = 0; chapter < StageCatalog.CHAPTER_COUNT; chapter++) {
            int first = StageCatalog.firstStageInChapter(chapter), count = StageCatalog.stageCountInChapter(chapter);
            for (int i = first; i < first + count - 1; i++) p.recordResult(i, true, 10, 10);
            check(!Cosmetics.isAvatarUnlocked(p, chapter + 1), "chapter introductions alone cannot unlock its finale reward");
            p.recordResult(first + count - 1, true, 20, 10);
            check(Cosmetics.isAvatarUnlocked(p, chapter + 1), "clearing every stage and the boss unlocks the chapter avatar");
        }
        for (int stars = 0; stars <= StageCatalog.COUNT * 3; stars++) {
            earnStars(p, stars);
            for (int id = 0; id < 4; id++) {
                check(Cosmetics.isPaletteUnlocked(p, id) == (stars >= Cosmetics.paletteStars(id)), "rope palette follows its advertised star threshold");
                check(Cosmetics.isTrailUnlocked(p, id) == (stars >= Cosmetics.trailStars(id)), "trail follows its advertised star threshold");
                check(Cosmetics.isLandingUnlocked(p, id) == (stars >= Cosmetics.landingStars(id)), "landing effect follows its advertised star threshold");
                check(!Cosmetics.paletteName(id).isEmpty() && !Cosmetics.trailName(id).isEmpty()
                        && !Cosmetics.landingName(id).isEmpty(), "every selectable appearance has a readable name");
            }
        }
        FalleyProfile fresh = new FalleyProfile();
        fresh.selectedAvatar = AvatarCatalog.COUNT - 1; fresh.selectedPalette = fresh.selectedTrail = fresh.selectedLanding = 3;
        fresh.sanitize();
        check(fresh.selectedAvatar == AvatarCatalog.HUMAN && fresh.selectedPalette == 0
                && fresh.selectedTrail == 0 && fresh.selectedLanding == 0, "loading a locked or invalid choice restores its free appearance");
        earnStars(fresh, 72); fresh.selectedAvatar = AvatarCatalog.COUNT - 1;
        fresh.selectedPalette = fresh.selectedTrail = fresh.selectedLanding = 3; fresh.sanitize();
        check(fresh.selectedAvatar == AvatarCatalog.COUNT - 1 && fresh.selectedPalette == 3
                && fresh.selectedTrail == 3 && fresh.selectedLanding == 3, "earned appearance selections survive sanitization");
        check(!Cosmetics.isPaletteUnlocked(fresh, 4) && !Cosmetics.isTrailUnlocked(fresh, -1)
                && !Cosmetics.isLandingUnlocked(fresh, 100), "unknown appearance ids cannot leak into rendering");
        check(fresh.wins == 0 && fresh.bestScore == 0 && fresh.arcadeRuns[0] == 0
                && fresh.dailyAttempts == 0, "appearance selection never awards gameplay records");
        System.out.println("PASS: starter plus six chapter avatar unlocks, all star thresholds, selection persistence and gameplay isolation (" + checks + " checks)");
    }
}
