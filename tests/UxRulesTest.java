package com.perfgames.ropelab;

/** Home recommendations, reward previews and honest current-run star evidence. */
public final class UxRulesTest {
    private static int checks;
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    private static void recommendations(){
        check(UxRules.continueStage(null)==0&&!UxRules.campaignComplete(null),"missing profile starts at first stage");
        check(UxRules.nextRewardAvatar(null)==AvatarCatalog.DEMON,"starter previews Demon");
        for(int prefix=0;prefix<=StageCatalog.COUNT;prefix++){
            FalleyProfile p=new FalleyProfile();
            for(int i=0;i<prefix;i++){p.stageWins[i]=1;p.stars[i]=FalleyProfile.COMPLETE;}
            p.selectedStage=0;
            check(UxRules.campaignComplete(p)==(prefix==StageCatalog.COUNT),"completion checks all stages, not selection");
            check(UxRules.continueStage(p)==(prefix<StageCatalog.COUNT?prefix:0),"continue follows progress then missing mastery");
            check(UxRules.nextRewardAvatar(p)==(prefix==StageCatalog.COUNT?-1:prefix/4+1),"avatar reward follows first unfinished chapter");
            for(int chapter=0;chapter<6;chapter++)check(UxRules.completedInChapter(p,chapter)==Math.max(0,Math.min(4,prefix-chapter*4)),"chapter completion count");
            if(prefix<StageCatalog.COUNT-1){p.stageWins[StageCatalog.COUNT-1]=1;check(UxRules.continueStage(p)==prefix,"out-of-order later record cannot skip frontier");}
        }
        FalleyProfile p=new FalleyProfile();
        for(int i=0;i<StageCatalog.COUNT;i++){p.stageWins[i]=1;p.stars[i]=FalleyProfile.ALL_STARS;}
        p.stars[7]=FalleyProfile.COMPLETE;p.selectedStage=2;
        check(UxRules.continueStage(p)==7,"completed campaign recommends missing mastery");
        p.stars[7]=FalleyProfile.ALL_STARS;
        p.selectedStage=999;check(UxRules.continueStage(p)==23,"all mastered fallback clamps high selection");
        p.selectedStage=-7;check(UxRules.continueStage(p)==0,"all mastered fallback clamps low selection");
        p.selectedStage=13;check(UxRules.continueStage(p)==13,"all mastered allows selected replay");
        check(UxRules.completedInChapter(p,-1)==0&&UxRules.completedInChapter(p,6)==0,"invalid chapter has no preview count");
    }
    private static void stars(){
        float[][] landings={{0,0,100},{17.5f,0,100},{17.501f,0,100},{Float.NaN,0,100},{0,Float.NaN,100},{0,0,Float.NaN},{0,0,Float.POSITIVE_INFINITY},{0,0,-1},{0,0,0}};
        for(int stage=0;stage<StageCatalog.COUNT;stage++)for(boolean won:new boolean[]{false,true})for(int jumps:new int[]{-1,0,StageCatalog.get(stage).parJumps,StageCatalog.get(stage).parJumps+1})for(float[] landing:landings){
            int mask=UxRules.maskForResult(won,stage,jumps,landing[0],landing[1],landing[2]);
            FalleyProfile p=new FalleyProfile();p.recordResult(stage,won,20,20,jumps,landing[0],landing[1],landing[2]);
            check(p.stars[stage]==mask,"result UI exactly matches saved stars including raw landing boundary");
            for(int prior=0;prior<=7;prior++){
                UxRules.StarEvidence e=UxRules.starEvidence(prior,stage,won,jumps,landing[0],landing[1],landing[2]);
                check(e.runMask==mask&&e.previousMask==prior,"current evidence is independent of old stars");
                check(e.newMask==(mask&~prior)&&e.totalMask==(mask|prior),"earned and cumulative masks remain distinct");
                for(int bit:new int[]{1,2,4})check(e.earnedThisRun(bit)==((mask&bit)!=0)&&e.newlyEarned(bit)==((mask&bit)!=0&&(prior&bit)==0)&&e.previouslyEarned(bit)==((prior&bit)!=0),"per-row status matches evidence");
            }
        }
        Physics physics=new Physics();physics.won=true;physics.landingX=0;physics.landingTargetCenter=0;physics.landingTargetWidth=100;
        check(UxRules.starsForRun(physics)==7,"campaign result uses actual run values");
        physics.daily=true;check(UxRules.starsForRun(physics)==0,"daily does not claim campaign stars");
        physics.daily=false;physics.arcade=true;check(UxRules.starsForRun(physics)==0,"arcade does not claim campaign stars");
        check(UxRules.starsForRun(null)==0,"no run has no evidence");
        check(UxRules.starLabel(1).equals("Finish")&&UxRules.starLabel(2).equals("Jump par")&&UxRules.starLabel(4).equals("Gold centre"),"stable named star goals");
    }
    public static void main(String[] args){recommendations();stars();for(String reason:new String[]{null,"CEILING","HARD LANDING","FALLING FIRE","LIGHTNING","BUZZSAW","TOP LASER","SIDE LASER","SPIKES","LASER","FIRE"})check(!UxRules.deathTip(reason).isEmpty(),"all loss types have a short recovery tip");check(!UxRules.controlCue(true).equals(UxRules.controlCue(false)),"motion and touch explain different gestures");System.out.println("PASS: UX recommendations, rewards and honest star evidence ("+checks+" checks)");}
}