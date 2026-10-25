package com.perfgames.ropelab;

/** Persisted, once-per-earned-avatar acknowledgements; selected equipment stays independent. */
public final class AvatarRewards {
    public static final int VERSION=1, NONE=-1;
    public static final int STARTER_MASK=1<<AvatarCatalog.HUMAN;
    private static final int ALL=(1<<AvatarCatalog.COUNT)-1;
    public static int unlockedMask(FalleyProfile p){
        if(p==null)return STARTER_MASK;
        int mask=STARTER_MASK;
        for(int avatar=1;avatar<AvatarCatalog.COUNT;avatar++)if(Cosmetics.isAvatarUnlocked(p,avatar))mask|=1<<avatar;
        return mask;
    }
    /** An older save acknowledges historical rewards instead of queueing a cascade. */
    public static void sanitize(FalleyProfile p){
        if(p==null)return;
        int unlocked=unlockedMask(p);
        if(p.avatarRewardsVersion!=VERSION){
            p.avatarRewardsVersion=VERSION;p.avatarRewardsSeenMask=unlocked;p.avatarRewardsPendingMask=0;p.avatarRewardsNeedCommit=true;return;
        }
        int seen=(p.avatarRewardsSeenMask|STARTER_MASK)&unlocked&ALL;
        int pending=p.avatarRewardsPendingMask&unlocked&~seen&~STARTER_MASK&ALL;
        if(seen!=p.avatarRewardsSeenMask||pending!=p.avatarRewardsPendingMask)p.avatarRewardsNeedCommit=true;
        p.avatarRewardsSeenMask=seen;p.avatarRewardsPendingMask=pending;
    }
    /** Called around actual campaign recording, never on a mere render or replay selection. */
    public static boolean noteUnlocked(FalleyProfile p,int previouslyUnlockedMask){
        if(p==null)return false;
        if(p.avatarRewardsVersion!=VERSION){
            p.avatarRewardsVersion=VERSION;p.avatarRewardsSeenMask=(previouslyUnlockedMask|STARTER_MASK)&ALL;p.avatarRewardsPendingMask=0;p.avatarRewardsNeedCommit=true;
        }
        sanitize(p);
        int fresh=unlockedMask(p)&~previouslyUnlockedMask&~p.avatarRewardsSeenMask&~STARTER_MASK&ALL;
        if(fresh==0)return false;
        p.avatarRewardsPendingMask|=fresh;p.avatarRewardsNeedCommit=true;return true;
    }
    /** Returns the oldest valid queued reward. Showing it does not acknowledge it. */
    public static int pending(FalleyProfile p){
        if(p==null||p.avatarRewardsVersion!=VERSION)return NONE;
        int mask=p.avatarRewardsPendingMask&unlockedMask(p)&~p.avatarRewardsSeenMask&~STARTER_MASK&ALL;
        return mask==0?NONE:Integer.numberOfTrailingZeros(mask);
    }
    /** Pure model choice; storage commits this plus equipment in the same editor transaction. */
    public static boolean choose(FalleyProfile p,boolean useNew){
        int avatar=pending(p);if(avatar==NONE)return false;
        if(useNew)p.selectedAvatar=avatar;
        p.avatarRewardsSeenMask|=1<<avatar;p.avatarRewardsPendingMask&=~(1<<avatar);p.avatarRewardsNeedCommit=true;return true;
    }
    private AvatarRewards(){}
}