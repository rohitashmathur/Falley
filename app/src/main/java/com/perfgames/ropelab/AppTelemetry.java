package com.perfgames.ropelab;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import com.google.firebase.FirebaseApp;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import java.util.EnumMap;
import java.util.Map;

/** Optional Firebase adapter. Gameplay and saves never depend on a network result. */
public final class AppTelemetry implements TelemetryController.Backend {
    static final String KEY="telemetry_flags";
    private final Context context;
    final TelemetryController choices;
    private FirebaseAnalytics analytics;
    private FirebaseCrashlytics crashes;
    private boolean initialized,analyticsWasEnabled,available=true;
    private Map<String,Object> run;
    private boolean runLogged,runActive;
    volatile int eventsSubmitted;
    private final Map<String,Integer> debugCounts=new java.util.HashMap<>();
    synchronized int eventCount(String event){Integer n=debugCounts.get(event);return n==null?0:n;}
    final TelemetryBuffer buffer=new TelemetryBuffer();
    private android.os.Handler worker;
    private boolean flushScheduled;
    private long generation;
    private long runStarted;
    private String lastScreen;
    private final Runnable drainTask=()->drain();
    private synchronized void schedule(boolean immediate){
        if(!analyticsWasEnabled||buffer.pending()==0)return;
        if(worker==null){android.os.HandlerThread thread=new android.os.HandlerThread("Falley-analytics",android.os.Process.THREAD_PRIORITY_BACKGROUND);thread.start();worker=new android.os.Handler(thread.getLooper());}
        if(immediate&&flushScheduled)worker.removeCallbacks(drainTask);
        if(immediate||!flushScheduled){flushScheduled=true;worker.postDelayed(drainTask,immediate?0:TelemetryBuffer.FLUSH_MS);}
    }
    private void drain(){
        final java.util.List<TelemetryBuffer.Packet> packets;final long epoch;
        synchronized(this){flushScheduled=false;if(!analyticsWasEnabled){buffer.enabled(false);return;}packets=buffer.drain();epoch=generation;}
        for(TelemetryBuffer.Packet p:packets){synchronized(this){
            // Opt-out invalidates even packets already removed from the queue; re-enable cannot replay them.
            if(!analyticsWasEnabled||epoch!=generation)return;send(p.name,p.values);
        }}
    }
    synchronized void flush(){buffer.flushSamples();schedule(true);}
    private long elapsed(){return Math.max(0,android.os.SystemClock.elapsedRealtime()-runStarted);}
    private Map<String,Object> detail(){return run==null?new java.util.LinkedHashMap<>():new java.util.LinkedHashMap<>(run);}
    synchronized void navigation(int screen){String[] names={"home","play","settings","help","pause","result","splash","campaign","arcade_setup","privacy","language","daily","collection","briefing","avatar_unlock","diagnostics"};String next=screen>=0&&screen<names.length?names[screen]:"unknown";
        if(next.equals(lastScreen))return;Map<String,Object> v=new java.util.LinkedHashMap<>();v.put("from_screen",lastScreen==null?"launch":lastScreen);v.put("to_screen",next);lastScreen=next;submit("falley_navigation",v);}
    synchronized void language(int before,int after){if(before==after)return;String[] names={"en","hi","id"};if(before<0||before>2||after<0||after>2)return;Map<String,Object> v=new java.util.LinkedHashMap<>();v.put("from_language",names[before]);v.put("to_language",names[after]);submit("falley_language_change",v);}
    synchronized void unlocked(int mask){for(int a=1;a<AvatarCatalog.COUNT;a++)if((mask&(1<<a))!=0){Map<String,Object> v=detail();v.put("unlocked_avatar",AvatarCatalog.name(a).toLowerCase(java.util.Locale.ROOT));submit("falley_avatar_unlock",v);}}
    synchronized void share(){Map<String,Object> v=detail();v.put("action","button_tap");v.put("surface","daily_result");submit("falley_share",v);}
    synchronized void jump(Physics p,boolean accepted){if(!runLogged)return;Map<String,Object> v=detail();v.put("time_ms",elapsed());v.put("accepted",accepted?1L:0L);v.put("jumps_used",(long)p.jumpsUsed());v.put("x",(double)p.x);v.put("y",(double)p.y);submit("falley_jump",v);}
    synchronized void swipe(float x0,float y0,float x1,float y1,long duration){if(!runLogged)return;Map<String,Object> v=detail();v.put("time_ms",elapsed());v.put("duration_ms",Math.max(0,duration));v.put("x0",(double)x0);v.put("y0",(double)y0);v.put("x1",(double)x1);v.put("y1",(double)y1);v.put("direction",x1<x0?"left":"right");submit("falley_swipe",v);}
    synchronized void rope(int index,int type,boolean cut){if(!runLogged)return;Map<String,Object> v=detail();v.put("time_ms",elapsed());v.put("rope_index",(long)index);v.put("rope_type",type==1?"spring":type==2?"static":"classic");v.put("encounter",cut?"cut":"landing");submit("falley_rope",v);}
    synchronized void motion(int type,long nanos,float x,float y,float z,int rotation){if(!runLogged||!analyticsWasEnabled)return;buffer.motion(type==1?"accel":type==4?"gyro":"other",Math.max(0,nanos/1000000-runStarted),x,y,z,rotation);schedule(false);}
    synchronized void touch(int phase,float x,float y,int width,int height){if(!runLogged||!analyticsWasEnabled)return;buffer.touch(elapsed(),phase,x,y,width,height);schedule(false);}

    AppTelemetry(Context context){
        this.context=context.getApplicationContext();final SharedPreferences prefs=this.context.getSharedPreferences("falley-v1",Context.MODE_PRIVATE);
        choices=new TelemetryController(new TelemetryController.Store(){public int read(){return prefs.getInt(KEY,TelemetryController.UNSET);}public boolean write(int flags){return prefs.edit().putInt(KEY,flags).commit();}},this);
    }
    @Override public synchronized void apply(boolean crash,boolean analyze){
        if(analyticsWasEnabled!=analyze)generation++;
        buffer.enabled(analyze);
        if(!analyze){runLogged=false;lastScreen=null;if(worker!=null)worker.removeCallbacks(drainTask);flushScheduled=false;}
        if(!initialized&&!crash&&!analyze)return;
        try{
            if(!initialized){
                FirebaseApp app=FirebaseApp.initializeApp(context);if(app==null){available=false;return;}app.setDataCollectionDefaultEnabled(false);
                analytics=FirebaseAnalytics.getInstance(context);crashes=FirebaseCrashlytics.getInstance();initialized=true;
            }
            EnumMap<FirebaseAnalytics.ConsentType,FirebaseAnalytics.ConsentStatus> consent=new EnumMap<>(FirebaseAnalytics.ConsentType.class);
            consent.put(FirebaseAnalytics.ConsentType.ANALYTICS_STORAGE,analyze?FirebaseAnalytics.ConsentStatus.GRANTED:FirebaseAnalytics.ConsentStatus.DENIED);
            consent.put(FirebaseAnalytics.ConsentType.AD_STORAGE,FirebaseAnalytics.ConsentStatus.DENIED);
            consent.put(FirebaseAnalytics.ConsentType.AD_USER_DATA,FirebaseAnalytics.ConsentStatus.DENIED);
            consent.put(FirebaseAnalytics.ConsentType.AD_PERSONALIZATION,FirebaseAnalytics.ConsentStatus.DENIED);
            Bundle defaults=new Bundle();defaults.putString("build_channel",BuildConfig.DEBUG?"debug":"release");analytics.setDefaultEventParameters(defaults);
            analytics.setConsent(consent);analytics.setAnalyticsCollectionEnabled(analyze);
            if(analyticsWasEnabled&&!analyze){analytics.resetAnalyticsData();runLogged=false;}
            analyticsWasEnabled=analyze;
            // Firebase documents that disabling automatic crash collection fully applies next launch.
            crashes.setCrashlyticsCollectionEnabled(crash);
            if(crash){crashes.setCustomKey("build_channel",BuildConfig.DEBUG?"debug":"release");if(run!=null)context(run);}
            available=true;
        }catch(RuntimeException optionalUnavailable){available=false;Log.w("FalleyTelemetry","Optional diagnostics unavailable ("+optionalUnavailable.getClass().getSimpleName()+")");}
    }
    boolean initialized(){return initialized;}
    boolean available(){return available;}
    void reload(){choices.reload();}
    private void context(Map<String,Object> values){if(!choices.crashes()||crashes==null)return;try{for(Map.Entry<String,Object> e:values.entrySet())crashes.setCustomKey(e.getKey(),String.valueOf(e.getValue()));}catch(RuntimeException optionalUnavailable){available=false;}}
    synchronized void start(Physics p,int avatar,boolean motion,boolean practice){
        // Callers close explicit abandonment before Physics.reset; never reuse new-run stats.
        run=null;runLogged=false;runActive=true;
        run=new java.util.LinkedHashMap<>(TelemetryEvents.start(p.daily,p.arcade,practice,p.stage.index,avatar,motion,p.arcadeDifficulty));
        run.put("run_id",java.util.UUID.randomUUID().toString().substring(0,18));runStarted=android.os.SystemClock.elapsedRealtime();buffer.begin(run);
        java.util.Map<String,Object> initial=new java.util.LinkedHashMap<>();initial.put("stage",0L);initial.put("difficulty","n/a");initial.put("result","playing");initial.put("hazard","none");initial.put("wave",0L);initial.put("jumps",0L);initial.put("time_bucket","under_10s");initial.putAll(run);context(initial);
        runLogged=choices.analytics()&&initialized;submit("falley_run_start",run);
    }
    synchronized void end(Physics p,boolean won,boolean abandoned){
        if(run==null||!runActive)return;Map<String,Object> data=TelemetryEvents.end(run,won,abandoned,p.arcade?p.wave:0,p.jumpsUsed(),p.elapsed,p.deathReason);context(data);
        if(runLogged)submit("falley_run_end",data);flush();runLogged=false;runActive=false;
    }
    private void submit(String event,Map<String,Object> values){
        if(!analyticsWasEnabled||!initialized)return;
        try{buffer.offer(event,values);schedule(false);}catch(RuntimeException invalidPacket){available=false;}
    }
    private void send(String event,Map<String,Object> values){
        if(!choices.analytics()||!initialized)return;
        try{Bundle b=new Bundle();for(Map.Entry<String,Object> e:values.entrySet()){if(e.getValue() instanceof Long)b.putLong(e.getKey(),(Long)e.getValue());else if(e.getValue() instanceof Double)b.putDouble(e.getKey(),(Double)e.getValue());else b.putString(e.getKey(),String.valueOf(e.getValue()));}b.putString("build_channel",BuildConfig.DEBUG?"debug":"release");analytics.logEvent(event,b);eventsSubmitted++;if(BuildConfig.DEBUG)debugCounts.put(event,eventCount(event)+1);}
        catch(RuntimeException optionalUnavailable){available=false;}
    }
    /** Instrumentation-only call; constant-folded out of optimized production builds. */
    void debugTestCrash(){if(!BuildConfig.DEBUG||!choices.crashes())throw new IllegalStateException("Debug crash reporting must be enabled");throw new RuntimeException("Falley debug Crashlytics verification");}
}
