package com.perfgames.ropelab;

public final class TelemetryControllerTest {
    static int checks;
    static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
    static final class Memory implements TelemetryController.Store{int flags=TelemetryController.UNSET;boolean reject;public int read(){return flags;}public boolean write(int next){if(reject)return false;flags=next;return true;}}
    static final class Sink implements TelemetryController.Backend{boolean crash,analytics;int applies;public void apply(boolean c,boolean a){crash=c;analytics=a;applies++;}}
    public static void main(String[] args){
        Memory s=new Memory();Sink b=new Sink();TelemetryController c=new TelemetryController(s,b);check(c.crashes()&&c.analytics()&&b.crash&&b.analytics,"fresh default-on");
        c.setCrash(false);c.setAnalytics(false);c=new TelemetryController(s,b);check(!c.crashes()&&!c.analytics(),"saved opt-out survives new defaults");
        for(int i=0;i<200;i++){
            check(c.setCrash(true)&&b.crash&&!b.analytics,"crash independent");check(c.setAnalytics(true)&&b.crash&&b.analytics,"both on");
            c=new TelemetryController(s,b);check(c.crashes()&&c.analytics(),"choices survive reconstruction");
            int before=b.applies;s.reject=true;check(!c.setCrash(false)&&c.crashes()&&b.crash&&b.applies==before,"save failure cannot silently change consent");s.reject=false;
            check(c.setCrash(false)&&!b.crash&&b.analytics,"crash off leaves analytics");check(c.setAnalytics(false)&&!b.crash&&!b.analytics,"both off");
            before=b.applies;check(c.setAnalytics(false)&&b.applies==before,"no duplicate change");
        }
        s.flags=255;c.reload();check(c.crashes()&&c.analytics(),"unknown flags ignored");s.flags=0;c.reload();check(!c.crashes()&&!c.analytics(),"fixture/cold state reload");
        System.out.println("PASS: default-on with preserved opt-out, independent choices, durable restart, save-failure rollback and flag migration ("+checks+" checks)");
    }
}
