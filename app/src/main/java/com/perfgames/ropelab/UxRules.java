package com.perfgames.ropelab;

/** Portable presentation decisions; never changes progress or physical outcomes. */
public final class UxRules {
    public static int continueStage(FalleyProfile p) {
        if(p==null)return 0;
        for(int i=0;i<StageCatalog.COUNT;i++)if(p.stageWins[i]<=0)return i;
        for(int i=0;i<StageCatalog.COUNT;i++)if(((p.stars[i]|FalleyProfile.COMPLETE)&FalleyProfile.ALL_STARS)!=FalleyProfile.ALL_STARS)return i;
        return clamp(p.selectedStage);
    }
    public static int recommendedStage(FalleyProfile p){return continueStage(p);}
    public static boolean campaignComplete(FalleyProfile p){if(p==null)return false;for(int n:p.stageWins)if(n<=0)return false;return true;}
    public static int nextRewardAvatar(FalleyProfile p){if(p==null)return AvatarCatalog.DEMON;for(int c=0;c<StageCatalog.CHAPTER_COUNT;c++)if(!p.isChapterComplete(c))return c+1;return -1;}
    public static int completedInChapter(FalleyProfile p,int chapter){if(p==null||chapter<0||chapter>=StageCatalog.CHAPTER_COUNT)return 0;int first=StageCatalog.firstStageInChapter(chapter),n=0;for(int i=first;i<first+StageCatalog.stageCountInChapter(chapter);i++)if(p.stageWins[i]>0)n++;return n;}
    public static int starsForRun(Physics p){if(p==null||p.arcade||p.daily)return 0;return maskForResult(p.won,p.stage.index,p.jumpsUsed(),p.landingX,p.landingTargetCenter,p.landingTargetWidth);}
    public static int maskForResult(boolean won,int stage,int jumps,float x,float centre,float width){if(!won)return 0;int mask=FalleyProfile.COMPLETE;if(jumps>=0&&jumps<=StageCatalog.get(clamp(stage)).parJumps)mask|=FalleyProfile.PAR;if(Float.isFinite(x)&&Float.isFinite(centre)&&Float.isFinite(width)&&width>0&&Math.abs(x-centre)<=width*FalleyProfile.PRECISION_FRACTION/2)mask|=FalleyProfile.PRECISION;return mask;}
    public static StarEvidence starEvidence(int previous,int stage,boolean won,int jumps,float x,float centre,float width){return new StarEvidence(previous,maskForResult(won,stage,jumps,x,centre,width));}
    public static StarEvidence starEvidence(int previous,Physics p){return new StarEvidence(previous,starsForRun(p));}
    public static final class StarEvidence {
        public final int previousMask,runMask,newMask,totalMask;
        private StarEvidence(int previous,int run){previousMask=previous&FalleyProfile.ALL_STARS;runMask=run&FalleyProfile.ALL_STARS;newMask=runMask&~previousMask;totalMask=previousMask|runMask;}
        public boolean earnedThisRun(int star){return(runMask&star)!=0;}
        public boolean newlyEarned(int star){return(newMask&star)!=0;}
        public boolean previouslyEarned(int star){return(previousMask&star)!=0;}
    }
    public static String starLabel(int star){return star==FalleyProfile.PAR?"Jump par":star==FalleyProfile.PRECISION?"Gold centre":"Finish";}
    public static String controlCue(boolean motion){return motion?"Tap to jump · Tilt to steer":"Tap to jump · Swipe to steer";}
    public static String deathTip(String reason){
        if("CEILING".equals(reason))return "Leave room above before jumping.";
        if("HARD LANDING".equals(reason))return "Aim for the target before the final drop.";
        if("FALLING FIRE".equals(reason))return "Watch the warning, then move out of the fire's path.";
        if("LIGHTNING".equals(reason))return "Move clear of the diagonal warning.";
        if("BUZZSAW".equals(reason))return "Let the saw pass before crossing its path.";
        if("TOP LASER".equals(reason)||"SIDE LASER".equals(reason))return "Move away from the laser warning before it fires.";
        if("SPIKES".equals(reason))return "Keep some distance from the spiked walls.";
        return "Stay near the centre as the walls close in.";
    }
    private static int clamp(int stage){return Math.max(0,Math.min(StageCatalog.COUNT-1,stage));}
    private UxRules(){}
}
