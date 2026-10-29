package com.perfgames.ropelab;

import java.util.*;

/** Bounded in-memory batches. Firebase owns persistent offline upload/retry after handoff. */
public final class TelemetryBuffer {
    public static final int CAPACITY=192, SAMPLES=10;
    public static final long FLUSH_MS=15000;
    public static final class Packet {
        public final String name; public final Map<String,Object> values;
        Packet(String n,Map<String,Object> v){name=n;values=Collections.unmodifiableMap(new LinkedHashMap<>(v));}
    }
    private final ArrayDeque<Packet> queue=new ArrayDeque<>();
    private final Map<String,List<String>> samples=new LinkedHashMap<>();
    private final Map<String,Long> last=new HashMap<>();
    private Map<String,Object> sampleContext=Collections.emptyMap();
    private boolean enabled; private long dropped;
    public synchronized void enabled(boolean on){enabled=on;if(!on){queue.clear();samples.clear();last.clear();dropped=0;sampleContext=Collections.emptyMap();}}
    public synchronized boolean enabled(){return enabled;}
    public synchronized int pending(){int n=queue.size();for(List<String> s:samples.values())if(!s.isEmpty())n++;return n;}
    public synchronized long dropped(){return dropped;}
    public synchronized void begin(Map<String,Object> context){flushSamples();sampleContext=new LinkedHashMap<>(context);last.clear();}
    public synchronized void offer(String name,Map<String,Object> values){
        if(!enabled)return;
        if(!name.matches("falley_[a-z_]{1,33}")||values.size()>24)throw new IllegalArgumentException("Invalid analytics schema");
        for(Map.Entry<String,Object> e:values.entrySet()){
            Object v=e.getValue();if(e.getKey().length()>40||!(v instanceof String||v instanceof Long||v instanceof Double)||v instanceof String&&((String)v).length()>100||v instanceof Double&&!Double.isFinite((Double)v))throw new IllegalArgumentException("Invalid analytics parameter");
        }
        if(queue.size()==CAPACITY){queue.removeFirst();dropped++;}
        queue.addLast(new Packet(name,values));
    }
    /** Downsample unfiltered sensor axes, not steering/calibration output. Units are Android SI. */
    public synchronized void motion(String sensor,long ms,float x,float y,float z,int rotation){
        if(!sensor.equals("accel")&&!sensor.equals("gyro"))return;
        if(!finite(x,y,z)||!accept(sensor,ms,500))return;
        sample(sensor,ms+","+x+","+y+","+z+","+rotation);
    }
    /** View-relative physical pixels; only gameplay input is passed here. */
    public synchronized void touch(long ms,int phase,float x,float y,int width,int height){
        if(!finite(x,y)||width<=0||height<=0||phase<0||phase>3)return;
        if(!accept("touch",ms,phase==1?100:0))return;
        sample("touch",ms+","+phase+","+x+","+y+","+width+","+height);
    }
    private boolean accept(String stream,long ms,long interval){
        if(!enabled||sampleContext.isEmpty()||ms<0)return false;
        Long previous=last.get(stream);if(previous!=null&&(ms<previous||ms-previous<interval))return false;
        last.put(stream,ms);return true;
    }
    private static boolean finite(float... values){for(float v:values)if(!Float.isFinite(v)||Math.abs(v)>100000)return false;return true;}
    private void sample(String stream,String row){
        if(row.length()>100){dropped++;return;}
        List<String> rows=samples.get(stream);if(rows==null){rows=new ArrayList<>();samples.put(stream,rows);}rows.add(row);
        if(rows.size()==SAMPLES)flush(stream,rows);
    }
    private void flush(String stream,List<String> rows){
        if(rows.isEmpty())return;
        Map<String,Object> v=new LinkedHashMap<>(sampleContext);v.put("schema",1L);v.put("sample_count",(long)rows.size());v.put("stream",stream);
        for(int i=0;i<rows.size();i++)v.put("s"+i,rows.get(i));offer("falley_input_batch",v);rows.clear();
    }
    public synchronized void flushSamples(){for(Map.Entry<String,List<String>> e:samples.entrySet())flush(e.getKey(),e.getValue());}
    public synchronized List<Packet> drain(){
        if(!enabled)return Collections.emptyList();flushSamples();List<Packet> result=new ArrayList<>(queue);queue.clear();
        if(dropped>0){Map<String,Object> v=new LinkedHashMap<>();v.put("dropped",dropped);result.add(new Packet("falley_buffer_overflow",v));dropped=0;}return result;
    }
}
