package com.perfgames.ropelab;

import android.content.SharedPreferences;
import java.util.*;

/** Runs on a JVM against Android's real preference interface, with deterministic disk snapshots. */
public final class AvatarRewardsStorageTest {
    private static int checks;
    private static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    private static void earn(FalleyProfile p,int chapter){for(int i=chapter*4;i<chapter*4+4;i++)p.recordResult(i,true,100+i,20,0,240,240,100);}
    private static FalleyProfile load(Fake s){FalleyProfile p=new FalleyProfile();ProfileStorage.load(s,p);return p;}
    private static void persistence(){
        Fake s=new Fake();FalleyProfile p=load(s);check(!p.languageConfirmed&&p.language==0&&AvatarRewards.pending(p)==-1,"fresh language and reward defaults remain independent");check(ProfileStorage.save(s,p)&&s.commits==1,"reward format is initialized durably once");
        earn(p,0);int edits=s.edits,commits=s.commits,applies=s.applies;check(ProfileStorage.save(s,p),"completion plus new offer save succeeds");check(s.edits==edits+1&&s.commits==commits+1&&s.applies==applies,"new result and pending reward use one durable editor write");
        Fake restarted=s.restart();p=load(restarted);check(AvatarRewards.pending(p)==1&&p.stageWins[3]==1&&p.selectedAvatar==0,"process death retains completion and the same unequipped new offer");check(!p.languageConfirmed,"cold pending offer does not force language confirmation");
        commits=restarted.commits;edits=restarted.edits;check(ProfileStorage.chooseAvatarReward(restarted,p,true),"Use commits successfully");check(restarted.commits==commits+1&&restarted.edits==edits+1,"Use equipment and acknowledgement are exactly one write");
        p=load(restarted.restart());check(p.selectedAvatar==1&&AvatarRewards.pending(p)==-1&&(p.avatarRewardsSeenMask&2)!=0,"Use survives process death without reoffering");
        s=restarted.restart();p=load(s);earn(p,1);check(ProfileStorage.save(s,p)&&AvatarRewards.pending(p)==2,"a later chapter offers its own new avatar");p.selectedAvatar=1;commits=s.commits;edits=s.edits;
        check(ProfileStorage.chooseAvatarReward(s,p,false)&&s.commits==commits+1&&s.edits==edits+1,"Keep/Back acknowledges in one durable write");p=load(s.restart());check(p.selectedAvatar==1&&AvatarRewards.pending(p)==-1,"Keep preserves actual Demon selection, not hard-coded Human");
        earn(p,1);check(AvatarRewards.pending(p)==-1,"replaying an acknowledged chapter does not offer it again");edits=s.edits;check(!ProfileStorage.chooseAvatarReward(s,p,true)&&s.edits==edits,"choice without an offer performs no write");
    }
    private static void upgrades(){
        Fake old=new Fake();old.memory.put("progression_version",2);old.memory.put("language",2);old.memory.put("avatar",6);old.memory.put("wins",24);old.memory.put("bestScore",4567);old.memory.put("rope_type",2);
        for(int i=0;i<24;i++){old.memory.put("stage_wins_"+i,1);old.memory.put("stage_stars_"+i,7);}old.disk.putAll(old.memory);
        FalleyProfile p=load(old);check(p.languageConfirmed&&p.language==2,"old explicit language migrates confirmed");check(AvatarRewards.pending(p)==-1&&p.avatarRewardsSeenMask==127,"all historical avatar rewards migrate seen without popup cascade");check(p.selectedAvatar==6&&p.wins==24&&p.bestScore==4567&&p.totalStars()==72&&p.selectedRope==2,"upgrade preserves selected appearance, legacy rope guide preference and records");check(ProfileStorage.save(old,p),"upgrade reward schema persists");
        p=load(old.restart());check(AvatarRewards.pending(p)==-1&&p.selectedAvatar==6,"recreated upgrade stays acknowledged");for(int chapter=0;chapter<6;chapter++)earn(p,chapter);check(AvatarRewards.pending(p)==-1,"all old finale replays remain quiet");
        Fake partial=new Fake();partial.memory.put("progression_version",2);for(int i=0;i<4;i++)partial.memory.put("stage_wins_"+i,1);partial.disk.putAll(partial.memory);p=load(partial);check(p.avatarRewardsSeenMask==3,"only already earned chapter rewards migrate seen");earn(p,1);check(AvatarRewards.pending(p)==2,"a newly earned post-upgrade chapter still queues its avatar");
    }
    private static void commitFailures(){
        Fake s=new Fake();FalleyProfile p=load(s);check(ProfileStorage.save(s,p),"failure fixture schema initializes");earn(p,0);s.failCommits=1;check(!ProfileStorage.save(s,p)&&p.avatarRewardsNeedCommit,"failed result commit reports failure and retains retry marker");int commits=s.commits;check(ProfileStorage.save(s,p)&&s.commits==commits+1&&!p.avatarRewardsNeedCommit,"result save retries durably even when failed commit changed preference memory");
        s.failCommits=1;commits=s.commits;check(!ProfileStorage.chooseAvatarReward(s,p,true),"failed choice does not claim acknowledgement success");check(s.commits==commits+2,"failure has a compensating snapshot write; successful choice remains one write");check(p.selectedAvatar==0&&AvatarRewards.pending(p)==1,"failed choice restores selected and pending model state");
        check(AvatarRewards.pending(load(s))==1&&load(s).selectedAvatar==0,"same-process recreation cannot accept failed commit memory as a choice");check(AvatarRewards.pending(load(s.restart()))==1,"process death still restores the unacknowledged reward");
        commits=s.commits;check(ProfileStorage.chooseAvatarReward(s,p,false)&&s.commits==commits+1,"retry acknowledges once with one successful write");check(AvatarRewards.pending(load(s.restart()))==-1,"successful retry is durable");
    }
    public static void main(String[] args){persistence();upgrades();commitFailures();System.out.println("PASS: durable avatar result/choice snapshots, process recreation, legacy upgrade and commit-failure rollback ("+checks+" checks)");}
    private static final class Fake implements SharedPreferences {
        final Map<String,Object> memory=new HashMap<>(),disk=new HashMap<>();int edits,commits,applies,failCommits;
        Fake restart(){Fake next=new Fake();next.memory.putAll(disk);next.disk.putAll(disk);return next;}
        @Override public Map<String,?> getAll(){return new HashMap<>(memory);}
        @Override public boolean contains(String k){return memory.containsKey(k);}
        @Override public String getString(String k,String d){return memory.containsKey(k)?(String)memory.get(k):d;}
        @SuppressWarnings("unchecked") @Override public Set<String> getStringSet(String k,Set<String>d){return memory.containsKey(k)?new HashSet<>((Set<String>)memory.get(k)):d;}
        @Override public int getInt(String k,int d){return memory.containsKey(k)?(Integer)memory.get(k):d;}
        @Override public long getLong(String k,long d){return memory.containsKey(k)?(Long)memory.get(k):d;}
        @Override public float getFloat(String k,float d){return memory.containsKey(k)?(Float)memory.get(k):d;}
        @Override public boolean getBoolean(String k,boolean d){return memory.containsKey(k)?(Boolean)memory.get(k):d;}
        @Override public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener l){}
        @Override public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener l){}
        @Override public Editor edit(){edits++;return new Editor(){
            final Map<String,Object> put=new HashMap<>();final Set<String> remove=new HashSet<>();boolean clear;
            public Editor putString(String k,String v){put.put(k,v);return this;}public Editor putStringSet(String k,Set<String>v){put.put(k,new HashSet<>(v));return this;}
            public Editor putInt(String k,int v){put.put(k,v);return this;}public Editor putLong(String k,long v){put.put(k,v);return this;}public Editor putFloat(String k,float v){put.put(k,v);return this;}public Editor putBoolean(String k,boolean v){put.put(k,v);return this;}
            public Editor remove(String k){remove.add(k);return this;}public Editor clear(){clear=true;return this;}
            private void memoryWrite(){if(clear)memory.clear();for(String k:remove)memory.remove(k);memory.putAll(put);}
            public boolean commit(){commits++;memoryWrite();if(failCommits>0){failCommits--;return false;}disk.clear();disk.putAll(memory);return true;}
            public void apply(){applies++;memoryWrite();disk.clear();disk.putAll(memory);}
        };}
    }
}