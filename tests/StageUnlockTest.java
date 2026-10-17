package com.perfgames.ropelab;

/** Sequential unlocks are derived from records without discarding legacy sparse wins. */
public final class StageUnlockTest {
    private static int checks;
    private static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    private static void sequentialWins() {
        FalleyProfile p=new FalleyProfile();
        check(p.highestUnlockedStage()==0 && p.isStageUnlocked(0) && p.isChapterUnlocked(0),"fresh installs can enter stage zero and its chapter");
        check(!p.isStageUnlocked(-1) && !p.isStageUnlocked(StageCatalog.COUNT)
                && !p.isChapterUnlocked(-1) && !p.isChapterUnlocked(StageCatalog.CHAPTER_COUNT),"invalid indices never grant access");
        for(int completed=0;completed<StageCatalog.COUNT;completed++) {
            for(int stage=0;stage<StageCatalog.COUNT;stage++)
                check(p.isStageUnlocked(stage)==(stage<=completed),"only the contiguous completed prefix and its next stage are reachable");
            for(int chapter=0;chapter<StageCatalog.CHAPTER_COUNT;chapter++)
                check(p.isChapterUnlocked(chapter)==(StageCatalog.firstStageInChapter(chapter)<=completed),"a new chapter opens only after all stages of preceding chapters are won");
            p.recordResult(completed,false,100,10);
            check(p.highestUnlockedStage()==completed,"a loss/best score cannot open the next stage");
            p.recordResult(completed,true,200,9);
            check(p.highestUnlockedStage()==Math.min(StageCatalog.COUNT-1,completed+1),"winning the current stage opens exactly the next sequential stage");
        }
        check(p.highestUnlockedStage()==StageCatalog.COUNT-1,"a completed campaign remains bounded to its final stage");
    }
    private static void migrationAndSparseRecords() {
        FalleyProfile p=new FalleyProfile();p.wins=50;p.bestScore=9999;p.selectedStage=11;
        p.best[10]=800;p.stageWins[10]=4;p.stageTimes[10]=7.5f;
        p.stageWins[3]=2;p.best[3]=500;p.stageTimes[3]=8;
        p.sanitize();
        check(p.highestUnlockedStage()==0 && p.selectedStage==0,"legacy global or out-of-order wins do not bypass missing early stages");
        check(p.wins==50 && p.bestScore==9999 && p.stageWins[10]==4 && p.stageTimes[10]==7.5f && p.best[10]==800
                && p.stageWins[3]==2 && p.best[3]==500,"migration preserves all later/global records rather than deleting locked progress");
        p.recordResult(0,true,100,10);p.recordResult(1,true,100,10);
        check(p.highestUnlockedStage()==2 && p.isChapterUnlocked(1) && !p.isChapterUnlocked(2),"two first-chapter wins open the second chapter without granting later chapters");
        p.recordResult(2,true,100,10);
        check(p.highestUnlockedStage()==4 && p.isChapterUnlocked(2),"filling a prefix gap correctly reuses preserved later-stage wins");
        p.selectedStage=11;p.sanitize();check(p.selectedStage==4,"an unreachable saved selection clamps to the currently reachable frontier");
        p.selectedStage=1;p.sanitize();check(p.selectedStage==1,"a valid earlier saved selection stays selected");
        p.stageWins[1]=-1;p.selectedStage=4;p.sanitize();
        check(p.stageWins[1]==0 && p.highestUnlockedStage()==1 && p.selectedStage==1,"record sanitization precedes the selected-stage reachability clamp");
        check(p.stageWins[3]==2 && p.stageWins[10]==4,"repairing a corrupt early record preserves unrelated historical wins");
        int highest=p.highestUnlockedStage();p.recordArcadeResult(2,5000,60);
        check(p.highestUnlockedStage()==highest,"Arcade survival cannot open campaign stages");
    }
    public static void main(String[] args) {
        sequentialWins();migrationAndSparseRecords();
        System.out.println("PASS: contiguous stage/chapter unlocks, loss/Arcade isolation, saved-selection clamp and sparse legacy record preservation ("+checks+" checks)");
    }
}
