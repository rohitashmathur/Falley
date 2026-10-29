package com.perfgames.ropelab;
import java.util.*;
public final class TelemetryBufferTest {
    static int checks;
    static void check(boolean b,String why){checks++;if(!b)throw new AssertionError(why);}
    static Map<String,Object> context(String id){Map<String,Object> m=new LinkedHashMap<>();m.put("run_id",id);m.put("mode","campaign");m.put("stage",1L);m.put("avatar","human");m.put("control","motion");return m;}
    static int countSamples(List<TelemetryBuffer.Packet> packets){int n=0;for(TelemetryBuffer.Packet p:packets)if(p.name.equals("falley_input_batch"))n+=((Long)p.values.get("sample_count")).intValue();return n;}
    public static void main(String[] args)throws Exception{
        TelemetryBuffer b=new TelemetryBuffer();b.begin(context("off"));b.motion("accel",0,1,2,3,0);b.offer("falley_jump",context("off"));check(b.pending()==0,"disabled buffer collects nothing");
        b.enabled(true);b.begin(context("one"));
        for(int t=0;t<5000;t+=10){b.motion("accel",t,1.234567f,9.81f,-2.5f,1);b.motion("gyro",t,.1f,.2f,.3f,1);b.touch(t,1,120,220,720,1600);}
        List<TelemetryBuffer.Packet> data=b.drain();check(countSamples(data)==70,"2Hz per sensor plus10Hz touch, not render rate");
        int accel=0,gyro=0,touch=0;
        for(TelemetryBuffer.Packet p:data){check(p.values.size()+1<=25,"Firebase parameter limit including build channel");check(p.values.get("run_id").equals("one"),"batches retain run context");for(Object v:p.values.values())if(v instanceof String)check(((String)v).length()<=100,"no truncated strings");String stream=(String)p.values.get("stream");if(stream.equals("accel")){accel++;check(((String)p.values.get("s0")).contains("1.234567"),"unfiltered float precision retained");}if(stream.equals("gyro"))gyro++;if(stream.equals("touch"))touch++;}
        check(accel==1&&gyro==1&&touch==5,"separate bounded10-sample streams");
        b.motion("accel",5000,Float.NaN,0,0,0);b.touch(5000,1,Float.POSITIVE_INFINITY,0,720,1600);check(b.pending()==0,"invalid samples discarded");
        b.motion("accel",5500,1,2,3,0);b.begin(context("two"));b.motion("accel",0,4,5,6,0);data=b.drain();check(data.size()==2&&data.get(0).values.get("run_id").equals("one")&&data.get(1).values.get("run_id").equals("two"),"run rollover does not mix samples or sampling clocks");
        b.touch(0,0,1,1,720,1600);b.touch(1,2,1,1,720,1600);check(countSamples(b.drain())==2,"short tap retains both boundaries");
        for(int i=0;i<10000;i++)b.offer("falley_jump",context("flood"));check(b.pending()==TelemetryBuffer.CAPACITY&&b.dropped()==10000-TelemetryBuffer.CAPACITY,"overload memory bound and loss count");
        b.enabled(false);check(b.pending()==0&&b.dropped()==0&&b.drain().isEmpty(),"opt-out purges queue and partial samples");b.enabled(true);check(b.drain().isEmpty(),"re-enable does not resurrect old data");
        b.begin(context("parallel"));Thread producer=new Thread(()->{for(int i=0;i<5000;i++)b.offer("falley_jump",context("parallel"));});producer.start();for(int i=0;i<500;i++){data=b.drain();check(data.size()<=193,"concurrent drain remains bounded");}producer.join();b.enabled(false);check(b.pending()==0,"purge after producer");
        try{Map<String,Object> v=context("bad");v.put("invalid",Double.NaN);b.enabled(true);b.offer("falley_jump",v);throw new AssertionError("invalid float accepted");}catch(IllegalArgumentException expected){checks++;}
        System.out.println("PASS: bounded background analytics buffer, raw sample rates/precision, SDK limits, run isolation, overflow, opt-out purge and concurrent drain ("+checks+" checks)");
    }
}
