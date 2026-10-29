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
    private boolean runLogged;
    int eventsSubmitted;
    AppTelemetry(Context context){
        this.context=context.getApplicationContext();final SharedPreferences prefs=this.context.getSharedPreferences("falley-v1",Context.MODE_PRIVATE);
        choices=new TelemetryController(new TelemetryController.Store(){public int read(){return prefs.getInt(KEY,0);}public boolean write(int flags){return prefs.edit().putInt(KEY,flags).commit();}},this);
    }
    @Override public void apply(boolean crash,boolean analyze){
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
    void start(Physics p,int avatar,boolean motion,boolean practice){
        // Callers close explicit abandonment before Physics.reset; never reuse new-run stats.
        run=null;runLogged=false;
        run=TelemetryEvents.start(p.daily,p.arcade,practice,p.stage.index,avatar,motion,p.arcadeDifficulty);
        java.util.Map<String,Object> initial=new java.util.LinkedHashMap<>();initial.put("stage",0L);initial.put("difficulty","n/a");initial.put("result","playing");initial.put("hazard","none");initial.put("wave",0L);initial.put("jumps",0L);initial.put("time_bucket","under_10s");initial.putAll(run);context(initial);
        runLogged=choices.analytics()&&initialized;submit("falley_run_start",run);
    }
    void end(Physics p,boolean won,boolean abandoned){
        if(run==null)return;Map<String,Object> data=TelemetryEvents.end(run,won,abandoned,p.arcade?p.wave:0,p.jumpsUsed(),p.elapsed,p.deathReason);context(data);
        if(runLogged)submit("falley_run_end",data);run=null;runLogged=false;
    }
    private void submit(String event,Map<String,Object> values){
        if(!choices.analytics()||!initialized)return;
        try{Bundle b=new Bundle();for(Map.Entry<String,Object> e:values.entrySet()){if(e.getValue() instanceof Long)b.putLong(e.getKey(),(Long)e.getValue());else b.putString(e.getKey(),String.valueOf(e.getValue()));}b.putString("build_channel",BuildConfig.DEBUG?"debug":"release");analytics.logEvent(event,b);eventsSubmitted++;}
        catch(RuntimeException optionalUnavailable){available=false;}
    }
    /** Instrumentation-only call; constant-folded out of optimized production builds. */
    void debugTestCrash(){if(!BuildConfig.DEBUG||!choices.crashes())throw new IllegalStateException("Debug crash reporting must be enabled");throw new RuntimeException("Falley debug Crashlytics verification");}
}
