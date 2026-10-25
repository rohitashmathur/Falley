package com.perfgames.ropelab;
public final class Profile42Test {
 private static int checks;private static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
 public static void main(String[]args){
  FalleyProfile p=new FalleyProfile();check(p.music&&!p.languageConfirmed&&p.language==0,"fresh player receives English confirmation and independent music default");
  p.selectedRope=2;p.sanitize();check(p.selectedRope==0,"unearned rope cannot enter gameplay from a corrupt save");
  for(int i=0;i<4;i++){p.stageWins[i]=1;p.stars[i]=7;}p.selectedRope=2;p.sanitize();check(p.selectedRope==2&&p.totalStars()==12,"earned Static selection survives sanitization");
  long day=DailyDrop.utcDay(1792886400000L);p.dailyDay=day;p.lastObservedDay=day;p.lastDailyCompletedDay=day;p.dailyBestScore=900;p.dailyBestJumps=3;p.dailyAttempts=9;p.dailyWins=2;p.dailyBestTime=12;p.dailyStreak=4;p.dailyBestStreak=6;p.dailyGeneratorVersion=1;
  p.language=2;p.languageConfirmed=true;p.music=false;p.sanitize();
  check(p.dailyGeneratorVersion==DailyDrop.GENERATOR_VERSION&&p.dailyAttempts==0&&p.dailyWins==0&&p.dailyBestScore==0&&p.dailyBestTime==0&&p.dailyBestJumps==-1,"changed generator cannot mix same-date v1 bests with v2 courses");
  check(p.dailyDay==day&&p.lastObservedDay==day&&p.lastDailyCompletedDay==day&&p.dailyStreak==4&&p.dailyBestStreak==6,"generator migration preserves historical completion streak and rollback protection");
  check(p.language==2&&p.languageConfirmed&&!p.music&&p.selectedRope==2&&p.totalStars()==12,"migration retains language, controls, equipment and campaign mastery");
  p.dailyBestScore=440;p.dailyAttempts=1;p.sanitize();check(p.dailyBestScore==440&&p.dailyAttempts==1,"current generator records are not cleared on each launch");
  p.selectedRope=Integer.MAX_VALUE;p.sanitize();check(p.selectedRope==0,"unknown future rope falls back safely");
  System.out.println("PASS: 4.2 language/music defaults, earned equipment and Daily v2 record migration ("+checks+" checks)");
 }
}
