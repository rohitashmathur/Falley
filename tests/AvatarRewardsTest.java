package com.perfgames.ropelab;

/** Campaign-earned acknowledgement queue, deliberately separate from equipped appearance. */
public final class AvatarRewardsTest {
    private static int checks;
    private static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    private static void win(FalleyProfile p,int stage){p.recordResult(stage,true,100+stage,20,0,240,240,100);}
    private static void earnChapter(FalleyProfile p,int chapter){for(int i=chapter*4;i<chapter*4+4;i++)win(p,i);}
    private static void earnedChoices(){
        FalleyProfile p=new FalleyProfile();check(AvatarRewards.pending(p)==-1,"fresh profile has no earned celebration");
        for(int chapter=0;chapter<6;chapter++){
            int avatar=chapter+1;
            for(int stage=chapter*4;stage<chapter*4+3;stage++){win(p,stage);check(AvatarRewards.pending(p)==-1,"ordinary chapter stages cannot unlock its avatar early");}
            p.recordResult(chapter*4+3,false,999,1);check(AvatarRewards.pending(p)==-1,"finale loss cannot create an avatar offer");
            int selected=p.selectedAvatar;win(p,chapter*4+3);check(AvatarRewards.pending(p)==avatar,"completed chapter queues its new reward");check(p.selectedAvatar==selected,"new reward does not silently replace the selected avatar");
            int mask=p.avatarRewardsPendingMask;for(int i=0;i<100;i++)check(AvatarRewards.pending(p)==avatar&&p.avatarRewardsPendingMask==mask,"rendering pending offer does not acknowledge it");
            win(p,chapter*4+3);check(p.avatarRewardsPendingMask==mask,"replaying an unacknowledged finale cannot duplicate its offer");
            if(chapter>0)p.selectedAvatar=chapter; // The player may have selected any prior earned appearance.
            int current=p.selectedAvatar;boolean use=(chapter%2)==0;
            check(AvatarRewards.choose(p,use),"available offer accepts a choice");check(p.selectedAvatar==(use?avatar:current),"Keep preserves current equipment; Use equips exactly the new reward");
            check(AvatarRewards.pending(p)==-1&&(p.avatarRewardsSeenMask&(1<<avatar))!=0,"choice acknowledges this reward once");
            check(!AvatarRewards.choose(p,!use),"repeated choice without an offer is a no-op");win(p,chapter*4+3);check(AvatarRewards.pending(p)==-1,"cleared finale never reoffers its acknowledged reward");
        }
        check(p.avatarRewardsSeenMask==127&&p.avatarRewardsPendingMask==0,"six rewards can each be acknowledged exactly once");
    }
    private static void queuesAndMigration(){
        FalleyProfile p=new FalleyProfile();for(int chapter=0;chapter<6;chapter++)earnChapter(p,chapter);
        check(p.avatarRewardsPendingMask==126,"multiple newly earned offers remain queued without overwriting each other");
        p.selectedAvatar=AvatarCatalog.ROBOT;
        for(int avatar=1;avatar<7;avatar++){check(AvatarRewards.pending(p)==avatar,"pending queue has stable chapter order");check(AvatarRewards.choose(p,false)&&p.selectedAvatar==AvatarCatalog.ROBOT,"queued Back/Keep never resets a later selected avatar to Human");}
        p.avatarRewardsVersion=0;p.avatarRewardsSeenMask=0;p.avatarRewardsPendingMask=126;p.sanitize();
        check(p.avatarRewardsSeenMask==127&&AvatarRewards.pending(p)==-1,"upgrade marks all old earned avatars seen without a six-screen cascade");
        check(p.selectedAvatar==AvatarCatalog.ROBOT,"upgrade preserves equipment and records");
        for(int chapter=0;chapter<6;chapter++){win(p,chapter*4+3);check(AvatarRewards.pending(p)==-1,"upgrade replays cannot recreate historical offers");}
        FalleyProfile partial=new FalleyProfile();for(int i=0;i<8;i++)partial.stageWins[i]=1;partial.avatarRewardsVersion=0;partial.sanitize();
        check(partial.avatarRewardsSeenMask==7&&AvatarRewards.pending(partial)==-1,"partial upgrade acknowledges only rewards already earned");earnChapter(partial,2);check(AvatarRewards.pending(partial)==3,"first genuinely new chapter after upgrade still celebrates");
    }
    private static void invalidAndIndependent(){
        FalleyProfile p=new FalleyProfile();p.avatarRewardsSeenMask=-1;p.avatarRewardsPendingMask=-1;p.sanitize();
        check(p.avatarRewardsSeenMask==1&&p.avatarRewardsPendingMask==0,"locked and out-of-range bits cannot be treated as a valid offer");
        check(!AvatarRewards.choose(p,true)&&p.selectedAvatar==0,"invalid offer cannot equip a locked appearance");
        p.recordDaily(20000,true,999,2,0,DailyDrop.TOUCH);p.recordArcadeResult(2,999,20);check(AvatarRewards.pending(p)==-1,"Daily and Arcade cannot unlock campaign rewards");
        for(int i=0;i<3;i++)win(p,i);p.selectedStage=3;p.recordResult(true,100,4);check(AvatarRewards.pending(p)==1,"legacy result overload also detects an earned chapter avatar");
        p.avatarRewardsVersion=Integer.MAX_VALUE;p.sanitize();check(AvatarRewards.pending(p)==-1&&p.avatarRewardsSeenMask==3,"unknown reward format safely acknowledges existing records");
        int[] wins=p.stageWins.clone(),stars=p.stars.clone();int total=p.wins;AvatarRewards.choose(p,true);p.sanitize();check(java.util.Arrays.equals(wins,p.stageWins)&&java.util.Arrays.equals(stars,p.stars)&&total==p.wins,"reward maintenance never rewrites campaign progress");
        check(AvatarRewards.pending(null)==-1&&!AvatarRewards.choose(null,true),"missing model has no offer or choice");
    }
    public static void main(String[] args){earnedChoices();queuesAndMigration();invalidAndIndependent();System.out.println("PASS: once-only avatar rewards, current-equipment choices, stable queue, legacy acknowledgement and mode isolation ("+checks+" checks)");}
}