package com.perfgames.ropelab;
import java.util.Map;
public final class TelemetryEventsTest {
    static int checks;static void check(boolean v,String why){checks++;if(!v)throw new AssertionError(why);}
    public static void main(String[] args){
        for(int avatar=-2;avatar<10;avatar++)for(int stage=-1;stage<25;stage++)for(int mode=0;mode<4;mode++)for(int motion=0;motion<2;motion++){
            Map<String,Object> start=TelemetryEvents.start(mode==2,mode==1,mode==3,stage,avatar,motion==1,1);
            check(start.size()<=5,"bounded start schema");check((mode!=1&&mode!=2)==start.containsKey("stage"),"Daily date/seed and Arcade stage excluded");
            Map<String,Object> end=TelemetryEvents.end(start,false,false,Integer.MAX_VALUE,-2,Float.NaN,"private@email.example user-name 012345");
            check(!end.toString().contains("private")&&!end.toString().contains("user-name"),"arbitrary error text never serialized");check(end.get("hazard").equals("other"),"closed hazard vocabulary");
            check(end.get("wave").equals(99L)&&end.get("jumps").equals(0L)&&end.get("time_bucket").equals("under_10s"),"invalid numbers bounded");
            check(!start.containsKey("result")&&!start.containsKey("time_bucket"),"immutable start snapshot");
            try{start.put("raw_motion",1L);throw new AssertionError("mutable payload");}catch(UnsupportedOperationException expected){checks++;}
        }
        check(TelemetryEvents.hazard("FIREBALL").equals("fire"),"fire");check(TelemetryEvents.hazard("LIGHTNING").equals("lightning"),"lightning");check(TelemetryEvents.hazard(null).equals("other"),"null");
        System.out.println("PASS: minimized Campaign/Arcade/Daily events, immutable snapshots, invalid numbers and private-string exclusion ("+checks+" checks)");
    }
}
