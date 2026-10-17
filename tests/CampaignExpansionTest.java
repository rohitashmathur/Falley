package com.perfgames.ropelab;

/** Catalog compatibility and complete, unmodified physics runs through the new chapters. */
public final class CampaignExpansionTest {
    private static int checks;
    private static void check(boolean ok, String message) {
        checks++;
        if (!ok) throw new AssertionError(message);
    }

    private static void originalStagesRemainStable() {
        String[] titles = {"First light", "Tight beam", "Sharp turn", "Needle run", "Warm welcome", "Sky on fire"};
        float[] heights = {4200, 5000, 5800, 6600, 7400, 8200};
        float[] centers = {240, 210, 270, 210, 270, 240};
        float[] widths = {164, 90, 86, 82, 82, 100};
        int[] attacks = {0, ArcadeRules.TOP_LASERS, 0, ArcadeRules.SIDE_LASERS | ArcadeRules.BUZZSAWS,
                ArcadeRules.LIGHTNING, ArcadeRules.ALL};
        for (int i = 0; i < titles.length; i++) {
            StageCatalog.Stage stage = StageCatalog.get(i);
            check(stage.index == i && stage.title.equals(titles[i]) && stage.chapterIndex == i / 2,
                    "old stage identities and chapter membership keep saved records meaningful");
            check(stage.groundY == heights[i] && stage.targetCenter == centers[i] && stage.targetWidth == widths[i],
                    "the first six landing layouts keep their original dimensions");
            check(stage.hazard == i / 2 && stage.extraHazards == attacks[i] && stage.fireRain == (i == 5)
                            && stage.fatalMiss == (i >= 3) && stage.attackDifficulty == i / 2,
                    "old rail, attack and floor rules are explicit and retain their behavior");
            check(stage.openingHazard == (i == 3 ? ArcadeRules.BUZZSAWS : 0),
                    "the original saw introduction is data instead of a hidden stage-index rule");
        }
    }

    private static void chapterProgression() {
        check(StageCatalog.COUNT == 12 && StageCatalog.CHAPTER_COUNT == 6,
                "the campaign offers twelve stages across six chapters");
        java.util.Set<String> chapterNames = new java.util.HashSet<>(), stageNames = new java.util.HashSet<>();
        int covered = 0;
        for (int chapter = 0; chapter < StageCatalog.CHAPTER_COUNT; chapter++) {
            int first = StageCatalog.firstStageInChapter(chapter), count = StageCatalog.stageCountInChapter(chapter);
            check(count == 2 && first == covered, "chapter pages have two contiguous stages without gaps");
            StageCatalog.Stage introduction = StageCatalog.get(first), mixed = StageCatalog.get(first + 1);
            check(chapterNames.add(introduction.chapterTitle) && introduction.chapterTitle.equals(mixed.chapterTitle),
                    "every chapter has one distinct display title shared by its pair");
            check(introduction.attackDifficulty <= mixed.attackDifficulty
                            && mixed.groundY > introduction.groundY
                            && (introduction.extraHazards & mixed.extraHazards) == introduction.extraHazards,
                    "paired stages add challenge to the same chapter vocabulary");
            for (int offset = 0; offset < count; offset++) {
                StageCatalog.Stage stage = StageCatalog.get(first + offset);
                check(stage.index == first + offset && stage.chapterIndex == chapter && stageNames.add(stage.title),
                        "every stage has a unique identity and belongs to its displayed chapter");
                check(stage.hazard >= StageCatalog.LASER && stage.hazard <= StageCatalog.FIRE
                                && (stage.extraHazards & ~ArcadeRules.ALL) == 0
                                && stage.attackDifficulty >= 0 && stage.attackDifficulty <= 2,
                        "every stage uses supported rail hazards, attack masks and readable difficulty bounds");
                if (stage.index >= 6) {
                    check(stage.targetWidth >= 96 && stage.groundY <= 11000 && stage.fatalMiss,
                            "longer new drops retain generous targets and a deliberate strict landing");
                    check(stage.targetCenter - stage.targetWidth / 2 >= 150
                                    && stage.targetCenter + stage.targetWidth / 2 <= 330,
                            "new targets remain centered inside the final narrowing corridor");
                }
            }
            covered += count;
        }
        check(covered == StageCatalog.COUNT, "chapter pagination covers all stages exactly once");
        check(StageCatalog.get(-5).index == 0 && StageCatalog.get(100).index == StageCatalog.COUNT - 1
                        && StageCatalog.firstStageInChapter(-1) == 0
                        && StageCatalog.firstStageInChapter(100) == 10
                        && StageCatalog.stageCountInChapter(-1) == 2 && StageCatalog.stageCountInChapter(100) == 2,
                "persisted or UI selection values are bounded at the catalog boundary");
        check(StageCatalog.get(6).extraHazards == ArcadeRules.LIGHTNING
                        && StageCatalog.get(8).extraHazards == ArcadeRules.BUZZSAWS
                        && StageCatalog.get(6).attackDifficulty == 1 && StageCatalog.get(8).attackDifficulty == 1,
                "storm and foundry start with one attack family and longer warnings before mixing attacks");
        check(StageCatalog.get(8).groundY < StageCatalog.get(7).groundY,
                "the foundry offers a shorter pattern-focused opening instead of only increasing drop length");
        check(StageCatalog.get(10).extraHazards == (ArcadeRules.FIREBALLS | ArcadeRules.LIGHTNING)
                        && StageCatalog.get(11).extraHazards == ArcadeRules.ALL,
                "the finale develops a fire/storm pairing before the full mixed challenge");
    }

    private static float clearance(Physics p, float x) {
        float clear = 10000;
        for (Physics.FireHazard fire : p.fireHazards)
            if (fire.active && (fire.warningTime > 0 || fire.y < p.y + 90))
                clear = Math.min(clear, Math.abs(x - fire.x) - fire.radius);
        for (Physics.BeamHazard beam : p.beamHazards) if (beam.active) {
            float distance = beam.kind == ArcadeRules.SIDE_LASERS
                    ? Math.abs(x - Physics.clamp(x, Math.min(beam.x1, beam.x2), Math.max(beam.x1, beam.x2)))
                    : (float) Math.sqrt(Physics.pointSegmentDistanceSquared(x, p.y, beam.x1, beam.y1, beam.x2, beam.y2));
            clear = Math.min(clear, distance - beam.width / 2);
        }
        for (Physics.SawHazard saw : p.sawHazards) if (saw.active)
            clear = Math.min(clear, Math.abs(x - Physics.clamp(x,
                    Math.min(saw.warningTime > 0 ? saw.pathX1 : saw.x, saw.pathX2),
                    Math.max(saw.warningTime > 0 ? saw.pathX1 : saw.x, saw.pathX2))) - saw.radius);
        return clear;
    }

    private static void steer(Physics p) {
        // Leave room to correct the mild progression nudge before touching a rail.
        float low = p.leftWall() + p.hazardInset() + p.radius() + 8;
        float high = p.rightWall() - p.hazardInset() - p.radius() - 8;
        if (p.y > p.groundY() - 350) {
            low = Math.max(low, p.targetLeft() + p.radius() + 3);
            high = Math.min(high, p.targetRight() - p.radius() - 3);
        }
        float center = Physics.clamp(p.stage.targetCenter, low, high), best = center, bestClear = -10000;
        for (float candidate : new float[]{low, center, high}) {
            float clear = clearance(p, candidate);
            if (clear > bestClear || clear == bestClear && candidate == center) { bestClear = clear; best = candidate; }
        }
        if (clearance(p, center) > p.radius() + 9) best = center;
        // Only player-style, bounded steering input. No teleporting, cut forcing or attack removal.
        p.steerTouch(Physics.clamp((best - p.x) * 10 - p.driftVelocity(), -300, 300));
    }

    private static void newChaptersArePlayable() {
        for (int index = 6; index < StageCatalog.COUNT; index++) {
            Physics p = new Physics(); p.reset(index); p.touchControlled(true);
            int warningMask = 0, liveMask = 0, maxAttacks = 0, steps = 0;
            boolean finite = true, narrowed = true;
            float oldWall = p.leftWall();
            while (!p.lost && !p.won && steps++ < 240 * 60) {
                steer(p); p.step();
                finite &= Float.isFinite(p.x) && Float.isFinite(p.y) && Float.isFinite(p.vx) && Float.isFinite(p.vy);
                narrowed &= p.leftWall() >= oldWall - .0001f; oldWall = p.leftWall();
                int attacks = 0;
                for (Physics.FireHazard fire : p.fireHazards) if (fire.active) {
                    attacks++; if (fire.warningTime > 0) warningMask |= ArcadeRules.FIREBALLS; else liveMask |= ArcadeRules.FIREBALLS;
                }
                for (Physics.BeamHazard beam : p.beamHazards) if (beam.active) {
                    attacks++; if (beam.warningTime > 0) warningMask |= beam.kind; else liveMask |= beam.kind;
                }
                for (Physics.SawHazard saw : p.sawHazards) if (saw.active) {
                    attacks++; if (saw.warningTime > 0) warningMask |= ArcadeRules.BUZZSAWS; else liveMask |= ArcadeRules.BUZZSAWS;
                }
                maxAttacks = Math.max(maxAttacks, attacks);
            }
            check(p.won && !p.lost, "new stage " + (index + 1) + " completes with bounded steering through real ropes and attacks; "
                    + p.deathReason + " x=" + p.x + " y=" + p.y + " t=" + p.elapsed);
            check(finite && narrowed && p.elapsed > 10 && p.brokenCount > 20,
                    "new courses exercise a stable, narrowing descent and actual rope recycling");
            check(maxAttacks == 1 && warningMask != 0 && liveMask != 0
                            && ((warningMask | liveMask) & ~p.stage.extraHazards) == 0,
                    "scheduled attacks honor the chapter mask and retain exclusive warning/live phases");
            check(p.x - p.radius() >= p.targetLeft() && p.x + p.radius() <= p.targetRight()
                            && p.y + p.radius() <= p.groundY() + .01f,
                    "new-stage success comes from a complete avatar landing inside its real target");
            check(p.groundY() - p.cameraY <= Physics.H && p.cameraY > 0,
                    "the final landing remains visible at the end of each expanded course");
            System.out.printf("Expanded stage %d (%s) PASS: %.2fs, %d cuts, warning/live masks %d/%d%n",
                    index + 1, p.stage.title, p.elapsed, p.brokenCount, warningMask, liveMask);
        }
    }

    public static void main(String[] args) {
        originalStagesRemainStable(); chapterProgression(); newChaptersArePlayable();
        System.out.println("PASS: original stage compatibility, six chapter pagination, specialist-to-mixed progression, generous targets and six complete natural new courses (" + checks + " checks)");
    }
}
