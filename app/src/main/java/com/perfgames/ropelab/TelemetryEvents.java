package com.perfgames.ropelab;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** A closed vocabulary: no names, country, sensor samples, daily date or arbitrary messages. */
public final class TelemetryEvents {
    private static final String[] AVATARS={"human","demon","angel","military","astronaut","robot","shadow"};
    public static Map<String,Object> start(boolean daily,boolean arcade,boolean practice,int stage,int avatar,boolean motion,int difficulty){
        Map<String,Object> p=new LinkedHashMap<>();p.put("mode",daily?"daily":arcade?"arcade":practice?"practice":"campaign");
        if(!daily&&!arcade)p.put("stage",(long)Math.max(1,Math.min(24,stage+1)));
        if(arcade)p.put("difficulty",difficulty==0?"chill":difficulty==2?"brutal":"standard");
        p.put("avatar",AVATARS[avatar>=0&&avatar<AVATARS.length?avatar:0]);p.put("control",motion?"motion":"touch");return Collections.unmodifiableMap(p);
    }
    public static Map<String,Object> end(Map<String,Object> start,boolean won,boolean abandoned,int wave,int jumps,float seconds,String reason){
        Map<String,Object> p=new LinkedHashMap<>(start);p.put("result",abandoned?"abandon":won?"win":"loss");
        p.put("wave",(long)Math.max(0,Math.min(99,wave)));p.put("jumps",(long)Math.max(0,Math.min(1000,jumps)));
        p.put("time_bucket",!Float.isFinite(seconds)||seconds<10?"under_10s":seconds<30?"10_29s":seconds<60?"30_59s":seconds<120?"60_119s":"120s_plus");
        p.put("hazard",hazard(reason));return Collections.unmodifiableMap(p);
    }
    static String hazard(String reason){String s=reason==null?"":reason.toLowerCase(java.util.Locale.ROOT);if(s.contains("fire"))return "fire";if(s.contains("lightning")||s.contains("thunder"))return "lightning";if(s.contains("laser"))return "laser";if(s.contains("saw"))return "saw";if(s.contains("spike"))return "spikes";if(s.contains("ceiling"))return "ceiling";if(s.contains("landing")||s.contains("floor")||s.contains("bones"))return "floor";if(s.contains("wall")||s.contains("side"))return "boundary";return "other";}
    private TelemetryEvents(){}
}
