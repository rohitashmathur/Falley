package com.perfgames.ropelab;

import android.content.SharedPreferences;

/** Versioned local save format; preserves the old twelve-stage campaign. */
final class ProfileStorage {
    static void load(SharedPreferences s,FalleyProfile p) {
        p.music=s.getBoolean("music",true);
        p.languageConfirmed=s.getBoolean("language_confirmed",s.contains("language"));
        p.selectedRope=s.getInt("rope_type",0);
        p.motion=s.getBoolean("motion",true); p.sound=s.getBoolean("sound",true); p.haptics=s.getBoolean("haptics",true);
        p.preset=s.getInt("preset",MotionSteering.LOOSE); p.language=s.getInt("language",0); p.blood=s.getBoolean("blood",true);
        p.bestScore=s.getInt("bestScore",0); p.wins=s.getInt("wins",0); p.fastestWin=s.getFloat("fastestWin",0);
        p.arcadeDifficulty=s.getInt("arcadeDifficulty",1); p.arcadeHazards=s.getInt("arcadeHazards",ArcadeRules.ALL);
        for(int i=0;i<3;i++){p.arcadeBest[i]=s.getInt("arcade_best_"+i,0);p.arcadeRuns[i]=s.getInt("arcade_runs_"+i,0);p.arcadeLongest[i]=s.getFloat("arcade_longest_"+i,0);}
        p.selectedStage=s.getInt("selectedStage",0);
        if(s.getInt("progression_version",0)<2) {
            int[] best=new int[12],wins=new int[12]; float[] times=new float[12];
            for(int i=0;i<12;i++){best[i]=s.getInt("stage_best_"+i,i==0?p.bestScore:0);wins[i]=s.getInt("stage_wins_"+i,i==0?p.wins:0);times[i]=s.getFloat("stage_time_"+i,i==0?p.fastestWin:0);}
            p.migrateLegacyCampaign(best,wins,times,p.selectedStage);
        } else for(int i=0;i<StageCatalog.COUNT;i++) {
            p.best[i]=s.getInt("stage_best_"+i,0);p.stageWins[i]=s.getInt("stage_wins_"+i,0);p.stageTimes[i]=s.getFloat("stage_time_"+i,0);p.stars[i]=s.getInt("stage_stars_"+i,0);
        }
        p.selectedAvatar=s.getInt("avatar",0);p.selectedPalette=s.getInt("rope_palette",0);p.selectedTrail=s.getInt("trail_palette",0);p.selectedLanding=s.getInt("landing_palette",0);
        p.dailyGeneratorVersion=s.getInt("daily_generator",1);
        p.dailyDay=s.getLong("daily_day",DailyDrop.NO_DAY);p.lastDailyCompletedDay=s.getLong("daily_completed",DailyDrop.NO_DAY);p.lastObservedDay=s.getLong("daily_observed",DailyDrop.NO_DAY);
        p.dailyBestScore=s.getInt("daily_best_score",0);p.dailyAttempts=s.getInt("daily_attempts",0);p.dailyWins=s.getInt("daily_wins",0);p.dailyBestJumps=s.getInt("daily_best_jumps",-1);
        p.dailyBestControlMode=s.getInt("daily_control",0);p.dailyBestTime=s.getFloat("daily_best_time",0);p.dailyStreak=s.getInt("daily_streak",0);p.dailyBestStreak=s.getInt("daily_best_streak",0);
        p.avatarRewardsVersion=s.getInt("avatar_rewards_version",0);
        p.avatarRewardsSeenMask=s.getInt("avatar_rewards_seen",AvatarRewards.STARTER_MASK);
        p.avatarRewardsPendingMask=s.getInt("avatar_rewards_pending",0);
        p.avatarRewardsNeedCommit=false;
        p.sanitize();
    }
    static boolean save(SharedPreferences s,FalleyProfile p) {
        boolean durable=p.avatarRewardsNeedCommit||s.getInt("avatar_rewards_version",0)!=p.avatarRewardsVersion
                ||s.getInt("avatar_rewards_seen",AvatarRewards.STARTER_MASK)!=p.avatarRewardsSeenMask
                ||s.getInt("avatar_rewards_pending",0)!=p.avatarRewardsPendingMask;
        SharedPreferences.Editor e=s.edit().putInt("avatar_rewards_version",p.avatarRewardsVersion)
                .putInt("avatar_rewards_seen",p.avatarRewardsSeenMask).putInt("avatar_rewards_pending",p.avatarRewardsPendingMask).putInt("daily_generator",p.dailyGeneratorVersion).putBoolean("music",p.music).putBoolean("language_confirmed",p.languageConfirmed).putInt("rope_type",p.selectedRope).putInt("progression_version",2).putBoolean("motion",p.motion).putBoolean("sound",p.sound).putBoolean("haptics",p.haptics)
                .putInt("preset",p.preset).putInt("language",p.language).putBoolean("blood",p.blood).putInt("bestScore",p.bestScore).putInt("wins",p.wins)
                .putFloat("fastestWin",p.fastestWin).putInt("selectedStage",p.selectedStage).putInt("arcadeDifficulty",p.arcadeDifficulty).putInt("arcadeHazards",p.arcadeHazards);
        for(int i=0;i<3;i++)e.putInt("arcade_best_"+i,p.arcadeBest[i]).putInt("arcade_runs_"+i,p.arcadeRuns[i]).putFloat("arcade_longest_"+i,p.arcadeLongest[i]);
        for(int i=0;i<StageCatalog.COUNT;i++)e.putInt("stage_best_"+i,p.best[i]).putInt("stage_wins_"+i,p.stageWins[i]).putFloat("stage_time_"+i,p.stageTimes[i]).putInt("stage_stars_"+i,p.stars[i]);
        e.putInt("avatar",p.selectedAvatar).putInt("rope_palette",p.selectedPalette).putInt("trail_palette",p.selectedTrail).putInt("landing_palette",p.selectedLanding)
                .putLong("daily_day",p.dailyDay).putLong("daily_completed",p.lastDailyCompletedDay).putLong("daily_observed",p.lastObservedDay)
                .putInt("daily_best_score",p.dailyBestScore).putInt("daily_attempts",p.dailyAttempts).putInt("daily_wins",p.dailyWins).putInt("daily_best_jumps",p.dailyBestJumps)
                .putInt("daily_control",p.dailyBestControlMode).putFloat("daily_best_time",p.dailyBestTime).putInt("daily_streak",p.dailyStreak).putInt("daily_best_streak",p.dailyBestStreak);
        if(durable){boolean saved=e.commit();if(saved)p.avatarRewardsNeedCommit=false;return saved;}
        e.apply();return true;
    }
    /** Acknowledgement and optional equipment are one durable write; failed writes keep the offer. */
    static boolean chooseAvatarReward(SharedPreferences s,FalleyProfile p,boolean useNew){
        int avatar=p.selectedAvatar,version=p.avatarRewardsVersion,seen=p.avatarRewardsSeenMask,pending=p.avatarRewardsPendingMask;
        if(!AvatarRewards.choose(p,useNew))return false;
        boolean saved=false;
        try{saved=save(s,p);}catch(RuntimeException unavailable){/* Keep the same offer in memory. */}
        if(!saved){
            p.selectedAvatar=avatar;p.avatarRewardsVersion=version;p.avatarRewardsSeenMask=seen;p.avatarRewardsPendingMask=pending;p.avatarRewardsNeedCommit=true;
            // Android commit(false) can still change its in-memory preference map.
            // Restore the old snapshot too, so Activity recreation cannot mistake
            // an uncommitted choice for an acknowledgement. Success uses one write.
            try{save(s,p);}catch(RuntimeException unavailable){/* Keep dirty for a later retry. */}
        }
        return saved;
    }
    private ProfileStorage(){}
}
