package com.perfgames.falley.tests;

import android.app.Activity;
import android.app.Application;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.TypedValue;
import android.text.Spanned;
import android.text.style.LocaleSpan;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityNodeProvider;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/** Real Android pointer navigation; terminal landing is explicitly a controlled fixture. */
public final class NativeFalleyTest extends Instrumentation {
    private String APP;
    private static final String ACTIVITY = "com.perfgames.ropelab.MainActivity";
    private static final String BACKUP_MARKER = "fixture-original-ready";
    private static final int HOME=0, PLAY=1, SETTINGS=2, HELP=3, PAUSE=4, RESULT=5, SPLASH=6, CHAPTERS=7, ARCADE_SETUP=8, PRIVACY=9, LANGUAGE=10;
    private Activity activity;
    private View arena;
    private Object physics;
    private SharedPreferences storage, backup;
    private String mode, shot;
    private File output;
    private int checks, captures;
    private final StringBuilder log = new StringBuilder();
    private interface Step { void run() throws Exception; }

    @Override public void onCreate(Bundle arguments) {
        super.onCreate(arguments);
        Bundle args = arguments == null ? new Bundle() : arguments;
        mode=args.getString("mode","suite"); shot=args.getString("shot","small");
        if ((!mode.equals("suite") && !mode.equals("effects") && !mode.equals("v31") && !mode.equals("release") && !mode.equals("privacy") && !mode.equals("store") && !mode.equals("motion") && !mode.equals("progression") && !mode.equals("features") && !mode.equals("splash") && !mode.equals("revision") && !mode.equals("revision_smoke") && !mode.equals("restore")) || !shot.matches("[a-z0-9-]+"))
            throw new IllegalArgumentException("Falley test harness arguments");
        start();
    }

    @Override public void onStart() {
        APP=getTargetContext().getPackageName();
        storage=getTargetContext().getSharedPreferences("falley-v1",Context.MODE_PRIVATE);
        backup=getTargetContext().getSharedPreferences("falley-native-original",Context.MODE_PRIVATE);
        Throwable failure=null;
        try {
            if (!mode.equals("restore")) {
                require(!backup.getBoolean(BACKUP_MARKER,false),"no interrupted fixture; recover with -RestoreOnly if needed");
                copy(backup,storage.getAll());
                require(backup.edit().putBoolean(BACKUP_MARKER,true).commit(),"original preferences backed up");
                SharedPreferences.Editor fixture=storage.edit().clear().putBoolean("motion",false)
                        .putBoolean("sound",false).putBoolean("haptics",false).putBoolean("blood",true);
                if (!mode.equals("features")) fixture.putInt("preset",1);
                require(fixture.commit(),"deterministic emulator settings installed");
                launch();
                if (mode.equals("revision")||mode.equals("revision_smoke")) {
                    waitScreen(HOME,4000); revisionRegression(mode.equals("revision_smoke"));
                } else if (mode.equals("splash")) {
                    splashRegression(); waitScreen(HOME,4000);
                } else if (mode.equals("features")) {
                    featureRegression();
                } else if (mode.equals("progression")) {
                    waitScreen(HOME,4000); progressionRegression();
                } else if (mode.equals("motion")) {
                    waitScreen(HOME,4000); motionRegression();
                } else if (mode.equals("store")) {
                    waitScreen(HOME,4000); storeCaptures();
                } else if (mode.equals("privacy")) {
                    waitScreen(HOME,4000); privacyContactRegression();
                } else if (mode.equals("release")) {
                    waitScreen(HOME,4000); privacyAccessibilityAndAwake();
                    legacyMaskMigration(); fifthHazardGuard(); sawVisuals(); audioLifecycle();
                } else if (mode.equals("effects")) {
                    waitScreen(HOME,4000); fireRain(); threeBeamVisuals();
                } else if (mode.equals("v31")) {
                    waitScreen(HOME,4000); legacyMaskMigration(); fifthHazardGuard(); sawVisuals(); audioLifecycle();
                } else {
                homeAndHelp();
                privacyAccessibilityAndAwake();
                settingsAndRecreation();
                chaptersAndPersistence();
                swipesPauseAndRestart();
                allStageLandings();
                chapterHazards();
                fireRain();
                fatalFloor();
                arcadeSetupAndPersistence();
                arcadeSurvivalAndRecords();
                arcadeBeamHazards();
                legacyMaskMigration(); sawVisuals(); audioLifecycle();
                capture("home-final");
                }
            }
        } catch (Throwable error) {
            failure=error;
            if (activity!=null && arena!=null && output!=null) {
                try { capture("failure-screen-"+screen()); } catch (Throwable diagnostic) { failure.addSuppressed(diagnostic); }
            }
        }
        finally {
            try { close(); restoreOriginal(); }
            catch (Throwable cleanup) { if (failure==null) failure=cleanup; else failure.addSuppressed(cleanup); }
        }
        String coverage=mode.equals("effects") ? "focused fire/three-beam warnings, collisions and early/late visual effects"
                : mode.equals("v31") ? "focused V3.1 mask migration, five hazards, saw physics and native SoundPool counters/lifecycle; audibility not assessed"
                : mode.equals("release") ? "V3.2 focused privacy, virtual accessibility actions, active-play screen awake, five hazards and audio lifecycle"
                : mode.equals("privacy") ? "focused EN/HI/ID offline policy, contact/URL visibility and accessibility, long-token glyph fit, real-touch browser/mail intent handoff interception, Back and no requested app permissions"
                : mode.equals("store") ? "V3.2 high-resolution actual native Canvas store captures and measured crop metadata"
                : mode.equals("motion") ? "focused debug motion start/restart/resume recentering, neutral hold, bilateral tilt, release braking and touch handoff; controlled real-time accelerometer samples, not a phone comfort test"
                : mode.equals("progression") ? "focused debug post30%-cut/Wave-2 steering progression and bounded nudge, fresh centering, actual Canvas HUD glyph gaps and native captures; controlled fixtures"
                : mode.equals("features") ? "focused local debug three-language UI/a11y, defaults/persistence, twelve-stage paging, jump budget/refills, actual ceiling collision and splash; controlled fixtures"
                : mode.equals("splash") ? "focused launcher-style starting-window capture, dedicated splash resource and timed custom splash; visual artwork review required"
                : mode.equals("revision")||mode.equals("revision_smoke") ? "focused campaign lock/entry/unlock persistence, repeatable burst recovery/finite total, reduced drift, visible Language and visual Touch/Motion Help; controlled fixtures"
                : mode.equals("restore") ? "preference recovery only" : "V3.2 full touch/Arcade/campaign, privacy, accessibility, screen awake, saw physics and SoundPool lifecycle; audibility not assessed";
        String summary=failure==null
            ? "PASS: native Falley "+mode+"; checks="+checks+"; captures="+captures+"; viewport="+shot
                +"; "+coverage+"; controlled collision/death fixtures, not a natural phone playtest; original preferences restored\n"
            : "FAIL: native Falley "+mode+"; checks="+checks+"; captures="+captures+"; viewport="+shot
                +"\n"+android.util.Log.getStackTraceString(failure);
        if (output!=null) {
            try (FileOutputStream stream=new FileOutputStream(new File(output,"checks.txt"))) {
                stream.write((log.toString()+summary).getBytes("UTF-8"));
            } catch (Exception error) { summary+="Artifact log failure: "+error+"\n"; }
        }
        Bundle result=new Bundle(); result.putString("stream",summary);
        finish(failure==null?Activity.RESULT_OK:Activity.RESULT_CANCELED,result);
    }

    private void homeAndHelp() throws Exception {
        requireScreen(SPLASH,"cold launch begins custom intro");
        // Let the OS starting window yield to the custom intro before capturing its compositor.
        runFrames(620); requireScreen(SPLASH,"custom intro remains visible before its 1.5s transition"); capture("splash");
        requireButtonsAbsent("zero","play","settings");
        waitScreen(HOME,4000); capture("home"); freeze("home physics frozen");
        requireButtonsAbsent("zero","restart","pause");
        click("help"); requireScreen(HELP,"How to Play opened by touch"); capture("how-to-play");
        freeze("help physics frozen"); back(); requireScreen(HOME,"Android back leaves help");
    }

    private void settingsAndRecreation() throws Exception {
        // Each selection is made by a physical screen tap and checked after a real Activity.recreate().
        for (int preset=0;preset<3;preset++) {
            click("settings"); requireScreen(SETTINGS,"home settings opened"); requireButtonsAbsent("zero");
            click("preset_"+preset);
            final int expected=preset;
            ui(() -> require(((Number)call(read(arena,"motion"),"preset")).intValue()==expected,"active motion preset "+expected));
            require(storage.getInt("preset",-1)==preset,"preset "+preset+" saved");
            capture("settings-preset-"+preset); freeze("settings physics frozen preset "+preset);
            recreate(); requireScreen(HOME,"recreated Activity opens safe home");
            ui(() -> require(((Number)call(read(arena,"motion"),"preset")).intValue()==expected,"preset "+expected+" survives Activity recreation"));
        }
        click("settings"); requireScreen(SETTINGS,"settings reopened after recreation");
        boolean motionBefore=storage.getBoolean("motion",false);
        boolean hasMotion=bool("hasMotion");
        click("motion");
        require(storage.getBoolean("motion",false)==(hasMotion?!motionBefore:motionBefore),"sensor switch respects hardware availability");
        // Keep deterministic touch steering during gameplay; Android sensor comfort needs a phone.
        if (storage.getBoolean("motion",false)) click("motion");
        click("sound"); click("haptics");
        click("blood"); require(!storage.getBoolean("blood",true),"blood effects switch saves");
        require(storage.getBoolean("sound",false),"sound setting saves by touch");
        require(storage.getBoolean("haptics",false),"haptics setting saves by touch");
        recreate();
        require(storage.getBoolean("sound",false)&&storage.getBoolean("haptics",false),"sound and haptics survive Activity recreation");
        require(!storage.getBoolean("motion",true),"touch-only setting survives Activity recreation");
        require(!storage.getBoolean("blood",true),"blood effects setting survives Activity recreation");
        click("settings"); click("sound"); click("haptics"); click("blood"); click("back");
        requireScreen(HOME,"Done leaves home settings");
    }

    private void silencePlatformSensors() throws Exception {
        ui(() -> {
            SensorManager manager=(SensorManager)read(activity,"sensors");
            if (manager!=null) manager.unregisterListener((SensorEventListener)activity);
        });
    }
    private Object steering() throws Exception { final Object[] result={null}; ui(() -> result[0]=read(arena,"motion")); return result[0]; }
    private float motionOutput() throws Exception {
        final float[] result={0}; ui(() -> result[0]=((Number)call(read(arena,"motion"),"acceleration",SystemClock.elapsedRealtimeNanos())).floatValue()); return result[0];
    }
    private void feedPose(long milliseconds,float ax,boolean advance) throws Exception {
        // Real elapsedRealtimeNanos timestamps and actual sample intervals. Only vertical
        // placement is held clear of ropes/floors so horizontal steering can be measured.
        long deadline=SystemClock.uptimeMillis()+milliseconds+5000;
        final long[] first={0},last={0},largestGap={0}; final int[] samples={0};
        do {
            SystemClock.sleep(20);
            ui(() -> {
                long now=SystemClock.elapsedRealtimeNanos();
                long gap=last[0]==0?20000000L:now-last[0];
                if (first[0]==0) first[0]=now; else largestGap[0]=Math.max(largestGap[0],gap);
                last[0]=now; samples[0]++;
                call(read(arena,"motion"),"acceptAccelerometer",now,ax,7f,6.5f);
                if (advance) {
                    require(((Number)read(arena,"screen")).intValue()==PLAY,"motion sample advances only live gameplay");
                    write(physics,"y",310f); write(physics,"vy",0f); write(physics,"cameraY",0f);
                    call(arena,"advance",Math.min(.05f,gap*1e-9f),SystemClock.uptimeMillis());
                    require(!(Boolean)read(physics,"lost")&&!(Boolean)read(physics,"won"),"horizontal motion fixture remains alive");
                }
                arena.invalidate();
            });
            require(SystemClock.uptimeMillis()<deadline,"controlled sensor feed stays within bounded wall time");
        } while (last[0]-first[0]<milliseconds*1000000L);
        log.append("SENSOR samples=").append(samples[0]).append(" spanMs=").append((last[0]-first[0])/1000000L).append(" maxGapMs=").append(largestGap[0]/1000000L).append('\n');
        require(largestGap[0]<250000000L,"controlled sensor samples stay fresh without emulator scheduling gaps");
    }
    private void requireFreshCenter(String context) throws Exception {
        ui(() -> {
            Object motion=read(arena,"motion");
            require(!(Boolean)call(motion,"hasSample")&&(Boolean)call(motion,"isCentering"),context+" waits for fresh hold samples");
            require(((Number)read(physics,"vx")).floatValue()==0&&((Number)read(physics,"gx")).floatValue()==0,context+" clears old velocity and tilt");
        });
    }
    private void settleHold(float ax,String context) throws Exception {
        float before=physicsFloat("x"); feedPose(420,ax,true);
        ui(() -> require(!(Boolean)call(read(arena,"motion"),"isCentering"),context+" finishes automatic centering"));
        require(Math.abs(motionOutput())<1&&Math.abs(physicsFloat("vx"))<1,context+" centered hand produces no steering");
        require(Math.abs(physicsFloat("x")-before)<2,context+" does not pull the avatar sideways while settling");
    }
    private void motionRegression() throws Exception {
        require(APP.equals("com.perfgames.falley.motiontest"),"motion check targets the isolated debug test app");
        require((getTargetContext().getApplicationInfo().flags&android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE)!=0,"motion target is debuggable");
        try {
            for (int preset=1;preset<=2;preset++) {
                goHome(); click("settings"); require(bool("hasMotion"),"test emulator exposes motion hardware");
                if (!storage.getBoolean("motion",false)) click("motion");
                silencePlatformSensors(); click("preset_"+preset);
                require(storage.getInt("preset",-1)==preset,"motion response preset selected by actual touch: "+preset);
                feedPose(340,-1.5f,false); back(); click("play");
                requireFreshCenter("preset "+preset+" Play");
                // Simulate a tap-time tilt before the natural hold settles.
                feedPose(40,-2f,true); settleHold(1.6f,"preset "+preset+" natural hold");
                float neutralX=physicsFloat("x"); feedPose(620,1.6f,true);
                require(Math.abs(physicsFloat("x")-neutralX)<2,"preset "+preset+" stable hold has no constant left/right pull");
                float leftStart=physicsFloat("x"); feedPose(360,4.6f,true);
                require(motionOutput()<-35&&physicsFloat("vx")<-8&&physicsFloat("x")<leftStart-3,"preset "+preset+" deliberate left tilt steers left");
                float leftRelease=physicsFloat("x"); feedPose(600,1.6f,true);
                require(Math.abs(motionOutput())<1&&Math.abs(physicsFloat("vx"))<2,"preset "+preset+" release from left tilt brakes");
                require(Math.abs(physicsFloat("x")-leftRelease)<24,"preset "+preset+" left release avoids long coasting");
                float rightStart=physicsFloat("x"); feedPose(360,-1.4f,true);
                require(motionOutput()>35&&physicsFloat("vx")>8&&physicsFloat("x")>rightStart+3,"preset "+preset+" deliberate right tilt steers right");
                float rightRelease=physicsFloat("x"); feedPose(600,1.6f,true);
                require(Math.abs(motionOutput())<1&&Math.abs(physicsFloat("vx"))<2,"preset "+preset+" release from right tilt brakes");
                require(Math.abs(physicsFloat("x")-rightRelease)<24,"preset "+preset+" right release avoids long coasting");
                float stillX=physicsFloat("x"); feedPose(300,1.6f,true);
                require(Math.abs(physicsFloat("x")-stillX)<1,"preset "+preset+" neutral stays still after both turns");
                capture("motion-preset-"+preset+"-centered");
                click("restart"); requireFreshCenter("preset "+preset+" Restart"); settleHold(-2.1f,"preset "+preset+" Restart changed hold");
                click("pause"); feedPose(320,2.8f,false); click("resume");
                requireFreshCenter("preset "+preset+" Resume"); settleHold(2.8f,"preset "+preset+" Resume changed hold");
                back(); requireScreen(PAUSE,"system Back pauses motion gameplay"); feedPose(320,-1f,false); back();
                requireFreshCenter("preset "+preset+" system Back resume"); settleHold(-1f,"preset "+preset+" Back changed hold");
                click("pause"); click("settings"); feedPose(320,3.2f,false); back(); requireScreen(PAUSE,"Settings returns to Pause before motion resumes"); click("resume");
                requireFreshCenter("preset "+preset+" Settings/Pause Resume"); settleHold(3.2f,"preset "+preset+" Settings changed hold");
                dragHeld(240,390,295,390); feedPose(60,3.2f,true);
                require(physicsCallFloat("steeringVelocity")>0&&physicsFloat("vx")>20,"preset "+preset+" real swipe temporarily overrides motion");
                release(295,390);
                require(physicsCallFloat("steeringVelocity")==0&&physicsFloat("vx")==0,"preset "+preset+" swipe release clears touch velocity and wind carry");
                float handoffX=physicsFloat("x"); feedPose(320,3.2f,true);
                require(Math.abs(physicsFloat("x")-handoffX)<1&&Math.abs(physicsFloat("vx"))<1,"preset "+preset+" centered motion remains still after touch handoff");
                goHome();
            }
        } finally {
            if (activity!=null&&!activity.isDestroyed()) ui(() -> call(activity,"registerMotion"));
        }
    }

    private static final class Glyph {
        final String text; final RectF ink;
        Glyph(String text,RectF ink) { this.text=text; this.ink=ink; }
    }
    private static final class HudCanvas extends Canvas {
        final List<Glyph> glyphs=new ArrayList<>();
        RectF progress,arena; float jumpFillRight=Float.NaN;
        HudCanvas(Bitmap bitmap) { super(bitmap); }
        @Override public void drawText(String text,float x,float y,Paint paint) {
            Rect ink=new Rect(); paint.getTextBounds(text,0,text.length(),ink);
            float offset=paint.getTextAlign()==Paint.Align.CENTER?paint.measureText(text)/2:paint.getTextAlign()==Paint.Align.RIGHT?paint.measureText(text):0;
            glyphs.add(new Glyph(text,new RectF(x-offset+ink.left,y+ink.top,x-offset+ink.right,y+ink.bottom)));
            super.drawText(text,x,y,paint);
        }
        @Override public void drawRoundRect(float l,float t,float r,float b,float rx,float ry,Paint paint) {
            if (l==24&&r==456&&t>=94&&b<160&&b-t<=10) progress=new RectF(l,t,r,b);
            if (l==20&&r==460&&b-t>400) arena=new RectF(l,t,r,b);
            if (l==52&&t==802&&b==806) jumpFillRight=r;
            super.drawRoundRect(l,t,r,b,rx,ry,paint);
        }
    }
    private void hudSpacing(String counter) throws Exception {
        ui(() -> {
            Bitmap bitmap=Bitmap.createBitmap(Math.max(1,arena.getWidth()),Math.max(1,arena.getHeight()),Bitmap.Config.ARGB_8888);
            try {
                HudCanvas canvas=new HudCanvas(bitmap); arena.draw(canvas);
                require(canvas.progress!=null&&canvas.arena!=null,"native draw exposes progress bar and arena bounds");
                float buttonBottom=Math.max(((Number)read(button("restart"),"bottom")).floatValue(),((Number)read(button("pause"),"bottom")).floatValue());
                require(buttonBottom==94&&canvas.arena.top==160,"Restart/Pause bottom94 and actual arena top160 are unchanged");
                List<Glyph> stats=new ArrayList<>(); boolean foundCounter=false; int sublabels=0;
                for (Glyph glyph:canvas.glyphs) {
                    if (glyph.text.startsWith("SCORE ")||glyph.text.startsWith("WEIGHT ")||glyph.text.startsWith("CUTS ")||glyph.text.startsWith("WAVE ")) {
                        stats.add(glyph); if (glyph.text.equals(counter)) foundCounter=true;
                        require(glyph.ink.top>=buttonBottom+8,"rendered "+glyph.text+" glyphs clear header buttons by at least8px");
                        require(glyph.ink.bottom<=canvas.progress.top-5,"rendered "+glyph.text+" glyphs clear progress bar by at least5px");
                        log.append("HUD ").append(glyph.text).append(" ink=").append(glyph.ink).append(" buttonsBottom=").append(buttonBottom).append('\n');
                    }
                    if (glyph.text.contains("LAND ")||glyph.text.endsWith("SURVIVED")||glyph.text.equals("SWIPE + DRIFT")||glyph.text.equals("MOTION ON")||glyph.text.equals("HOLD COMFORTABLY")) {
                        require(glyph.ink.top>=canvas.progress.bottom+5&&glyph.ink.bottom<=canvas.arena.top-5,"rendered objective/control label fits between progress and arena: "+glyph.text); sublabels++;
                    }
                }
                require(stats.size()==3&&foundCounter,"actual rendered HUD includes expected counter "+counter);
                require(sublabels==2,"actual HUD retains objective and input labels above arena");
                for (int a=0;a<stats.size();a++) for (int b=a+1;b<stats.size();b++) require(!RectF.intersects(stats.get(a).ink,stats.get(b).ink),"rendered stat glyph groups do not overlap");
            } finally { bitmap.recycle(); }
        }); settle(45);
    }
    private void stageProgress(int cuts,float elapsed) throws Exception {
        ui(() -> {
            write(physics,"brokenCount",cuts); write(physics,"elapsed",elapsed); write(physics,"x",240f); write(physics,"y",310f);
            write(physics,"vy",0f); write(physics,"vx",0f); write(physics,"cameraY",0f); write(arena,"accumulator",0f); arena.invalidate();
        });
    }
    private float touchResponse(int cuts,float elapsed) throws Exception {
        stageProgress(cuts,elapsed); dragHeld(240,390,280,390); actualPhysicsFrame();
        float command=physicsCallFloat("steeringVelocity");
        require(command>20,"real pointer swipe supplies a positive steering command");
        float response=(physicsFloat("vx")-physicsCallFloat("driftVelocity")-physicsCallFloat("nudgeVelocity"))/command;
        release(280,390); return response;
    }
    private float motionResponse(int cuts) throws Exception {
        stageProgress(cuts,5f); feedPose(360,-1.4f,true); return physicsFloat("vx")-physicsCallFloat("nudgeVelocity");
    }
    private void progressionRegression() throws Exception {
        require(APP.equals("com.perfgames.falley.motiontest"),"progression targets isolated local debug app");
        require((getTargetContext().getApplicationInfo().flags&android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE)!=0,"progression target is debuggable");
        int[] totals={21,25,30,35,39,44,48,51,49,54,57,59};
        for (int stage=0;stage<totals.length;stage++) {
            selectStage(stage); require(physicsCallFloat("totalRopes")==totals[stage],"chapter "+(stage+1)+" exposes its full finite rope total"); goHome();
        }
        try {
            click("settings"); require(bool("hasMotion"),"progression emulator has motion input"); click("motion"); silencePlatformSensors(); click("preset_1"); back(); selectStage(2);
            requireFreshCenter("progression chapter start"); settleHold(1.6f,"progression chapter start hold");
            feedPose(600,-1.4f,false); float earlyMotion=motionResponse(0),thresholdMotion=motionResponse(9),lateMotion=motionResponse(30);
            require(earlyMotion>15&&Math.abs(thresholdMotion/earlyMotion-1)<.025f,"actual motion command response is unchanged at exactly30% cuts");
            require(lateMotion/earlyMotion>.86f&&lateMotion/earlyMotion<.91f,"actual late chapter motion response is gently reduced: "+earlyMotion+" -> "+lateMotion);
            feedPose(600,1.6f,true); require(Math.abs(motionOutput())<1&&Math.abs(physicsFloat("vx"))<=12.5f,"late neutral brakes the command and retains only the bounded12 nudge");
            goHome(); click("settings"); click("motion"); back(); selectStage(2);
            float early=touchResponse(0,5f),atThirty=touchResponse(9,5f),firstLate=touchResponse(10,5f),full=touchResponse(30,5f);
            require(Math.abs(early-1)<.001f&&Math.abs(atThirty-1)<.001f,"actual swipe response stays full strength through exactly30% cuts");
            require(firstLate<1&&firstLate>.99f,"first cut strictly past30% begins only a tiny gradual swipe reduction");
            require(full>.879f&&full<.881f,"late chapter swipe command reaches the12% maximum penalty");
            stageProgress(24,5f); actualPhysicsFrame(); hudSpacing("CUTS 24/30"); capture("chapter-late-cuts-hud-controlled");
            startArcade(1,1); require(physicsCallFloat("totalRopes")==0,"endless Arcade does not invent a finite rope total");
            float waveOne=touchResponse(0,10f),beforeTwo=touchResponse(0,19f),startTwo=touchResponse(0,20f),midTwo=touchResponse(0,30f),afterTwo=touchResponse(0,40f);
            require(Math.abs(waveOne-1)<.001f&&Math.abs(beforeTwo-1)<.001f,"Arcade Wave1 keeps full swipe response");
            require(startTwo>.999f&&midTwo<startTwo&&midTwo>afterTwo,"Arcade Wave2 introduces a gradual response change");
            require(midTwo>.93f&&midTwo<.95f&&afterTwo>.879f&&afterTwo<.881f,"Arcade progression remains bounded at12%");
            require(Math.abs(physicsCallFloat("nudgeVelocity"))<=12,"actual progression nudge is bounded by12");
            stageProgress(0,30f); actualPhysicsFrame();
            ui(() -> require(((Number)read(physics,"wave")).intValue()==2,"actual simulation updates staged Arcade to Wave2"));
            hudSpacing("WAVE 2"); capture("arcade-wave-2-hud-controlled"); goHome();
        } finally {
            if (activity!=null&&!activity.isDestroyed()) ui(() -> call(activity,"registerMotion"));
        }
    }

    private void renderedFit(String context,boolean document) throws Exception {
        ui(() -> {
            Bitmap bitmap=Bitmap.createBitmap(arena.getWidth(),arena.getHeight(),Bitmap.Config.ARGB_8888);
            try {
                HudCanvas canvas=new HudCanvas(bitmap); arena.draw(canvas); int rows=0;
                for (Glyph glyph:canvas.glyphs) if (!glyph.text.trim().isEmpty()) {
                    require(glyph.ink.left>=19&&glyph.ink.right<=461,context+" text fits horizontal app bounds: "+glyph.text);
                    if (!document) require(glyph.ink.top>=0&&glyph.ink.bottom<=820,context+" text fits vertical app bounds: "+glyph.text);
                    for (Object hit:(List<?>)read(arena,"buttons")) if (glyph.text.equals(read(hit,"label"))) {
                        require(glyph.ink.left>=((Number)read(hit,"left")).floatValue()+1&&glyph.ink.right<=((Number)read(hit,"right")).floatValue()-1,context+" button label fits its control: "+glyph.text);
                    }
                    rows++;
                }
                require(rows>0,context+" records real native text glyphs");
                log.append("FIT ").append(context).append(" rows=").append(rows).append('\n');
            } finally { bitmap.recycle(); }
        });
    }
    private void localizedNodes(int language,String context) throws Exception {
        accessibleMenu(context);
        final String expected=new String[]{"en","hi","id"}[language];
        ui(() -> {
            AccessibilityNodeProvider provider=arena.getAccessibilityNodeProvider();
            for (int i=0;i<((List<?>)read(arena,"buttons")).size();i++) {
                AccessibilityNodeInfo node=provider.createAccessibilityNodeInfo(i+1); CharSequence value=node.getText();
                require(value instanceof Spanned,context+" accessibility text carries its locale");
                LocaleSpan[] locales=((Spanned)value).getSpans(0,value.length(),LocaleSpan.class);
                require(locales.length>0&&expected.equals(locales[0].getLocale().getLanguage()),context+" accessibility locale is "+expected); node.recycle();
            }
        });
    }
    private void localizedHud() throws Exception {
        ui(() -> {
            Bitmap bitmap=Bitmap.createBitmap(arena.getWidth(),arena.getHeight(),Bitmap.Config.ARGB_8888);
            try {
                HudCanvas canvas=new HudCanvas(bitmap); arena.draw(canvas);
                require(canvas.arena!=null&&canvas.arena.top==160,"localized HUD retains arena top160");
                List<Glyph> stats=new ArrayList<>(),objectives=new ArrayList<>();
                for (Glyph glyph:canvas.glyphs) if (!glyph.text.isEmpty()) {
                    if (glyph.ink.top>=94&&glyph.ink.bottom<=129) stats.add(glyph);
                    if (glyph.ink.top>=134&&glyph.ink.bottom<=160) objectives.add(glyph);
                    if (glyph.ink.top<94) require(glyph.ink.right<300||glyph.ink.left>=300,"localized title clears Restart/Pause column");
                }
                require(stats.size()==3&&objectives.size()==2,"localized HUD retains three stats and two objectives");
                for (Glyph glyph:stats) require(glyph.ink.top>=102&&glyph.ink.bottom<=124,"localized stat has clear vertical header gap");
                for (int a=0;a<stats.size();a++) for (int b=a+1;b<stats.size();b++) require(!RectF.intersects(stats.get(a).ink,stats.get(b).ink),"localized stat text does not overlap");
                require(!RectF.intersects(objectives.get(0).ink,objectives.get(1).ink),"localized objectives do not overlap");
            } finally { bitmap.recycle(); }
        });
    }
    private void featureLanguage(int language) throws Exception {
        if (screen()!=HOME) goHome(); click("settings"); click("language"); requireScreen(LANGUAGE,"language choices open from Settings");
        click("language_"+language); require(storage.getInt("language",-1)==language,"chosen language persists by actual touch: "+language);
        localizedNodes(language,"language choices "+language); renderedFit("language choices "+language,false);
        if (!new File(output,"language-"+language+"-choices.png").exists()) capture("language-"+language+"-choices");
        back(); requireScreen(SETTINGS,"language Back returns to Settings");
    }
    private void ceilingFixture() throws Exception {
        ui(() -> {
            float radius=((Number)call(physics,"radius")).floatValue();
            write(physics,"x",240f); write(physics,"cameraY",0f); write(physics,"y",160+1.42f*radius+2);
            write(physics,"vx",0f); write(physics,"vy",-120f); arena.invalidate();
        });
        require(physicsCallFloat("headTop")>160,"ceiling fixture starts with visible hair below the lethal edge");
        runUntilPhysics("lost",true,1500);
        ui(() -> require("CEILING".equals(read(physics,"deathReason")),"actual physics head collision records CEILING"));
        require(Math.abs(physicsCallFloat("headTop")-160)<.01f,"ceiling collision clamps the visible hair to the edge");
    }
    private int jumps() throws Exception { return (int)physicsCallFloat("jumpsRemaining"); }
    private void redraw() throws Exception { ui(() -> arena.invalidate()); settle(60); }
    private boolean buttonEnabled(String id) throws Exception { final boolean[] value={false}; ui(() -> value[0]=(Boolean)read(button(id),"enabled")); return value[0]; }
    private void safeCooldown() throws Exception {
        ui(() -> {
            float dt=((Number)read(physics.getClass(),"DT")).floatValue();
            for (int i=0;i<(int)Math.ceil(.35f/dt)+1;i++) { write(physics,"y",310f); write(physics,"vy",0f); write(physics,"cameraY",0f); call(arena,"advance",dt,SystemClock.uptimeMillis()); }
            arena.invalidate();
        }); settle(50);
    }
    private void spendJumps() throws Exception {
        require(jumps()==2,"new support/run provides two jump charges");
        click("jump"); redraw(); require(jumps()==1&&physicsFloat("vy")<0,"button jump spends the first charge");
        require(!buttonEnabled("jump"),"jump control is disabled during cooldown");
        float impulse=physicsFloat("vy"); click("jump"); require(jumps()==1&&physicsFloat("vy")==impulse,"cooldown tap cannot spend another charge");
        safeCooldown(); require(buttonEnabled("jump"),"jump control enables again after actual cooldown steps");
        tap(240,390); redraw(); require(jumps()==0&&physicsFloat("vy")<0,"arena tap spends the second charge");
        require(!buttonEnabled("jump"),"zero-charge jump button is disabled");
        impulse=physicsFloat("vy"); click("jump"); tap(240,390);
        require(jumps()==0&&physicsFloat("vy")==impulse,"third button and arena taps cannot add a jump");
        ui(() -> {
            AccessibilityNodeInfo node=arena.getAccessibilityNodeProvider().createAccessibilityNodeInfo(virtualButtonId("jump"));
            require(!node.isEnabled()&&!node.isClickable(),"exhausted jump is disabled for accessibility too"); node.recycle();
            Bitmap bitmap=Bitmap.createBitmap(arena.getWidth(),arena.getHeight(),Bitmap.Config.ARGB_8888);
            try { HudCanvas canvas=new HudCanvas(bitmap); arena.draw(canvas); require(canvas.jumpFillRight==52,"zero-charge jump cooldown bar is visibly empty"); } finally { bitmap.recycle(); }
        });
    }
    private void stepUntilRefill() throws Exception {
        ui(() -> {
            float dt=((Number)read(physics.getClass(),"DT")).floatValue();
            for (int i=0;i<120&&((Number)call(physics,"jumpsRemaining")).intValue()<2&&!((Boolean)read(physics,"lost"));i++) call(arena,"advance",dt,SystemClock.uptimeMillis());
            require(((Number)call(physics,"jumpsRemaining")).intValue()==2&&((Number)read(physics,"landingPulse")).floatValue()>0,"actual supporting collision refills two jump charges");
            require(!(Boolean)read(physics,"lost"),"refill occurs while the avatar remains alive"); arena.invalidate();
        }); redraw();
    }
    private void featureJumps() throws Exception {
        selectStage(0); require(jumps()==2,"chapter begins with two jumps"); click("jump"); require(physicsFloat("vy")==-450,"light first jump uses450 impulse");
        click("restart"); ui(() -> write(physics,"score",1000)); click("jump"); require(physicsFloat("vy")==-350,"heavy jump retains350 impulse floor"); click("restart");
        spendJumps(); capture("jumps-zero-disabled");
        ui(() -> {
            Object rope=((Object[])read(physics,"ropes"))[0]; float[] ys=(float[])read(rope,"y");
            float radius=((Number)call(physics,"radius")).floatValue(); write(physics,"x",240f); write(physics,"y",ys[ys.length/2]-radius-2); write(physics,"vy",180f);
        });
        stepUntilRefill(); capture("jumps-rope-refill-controlled"); click("restart"); spendJumps();
        ui(() -> {
            float radius=((Number)call(physics,"radius")).floatValue(),ground=((Number)call(physics,"groundY")).floatValue(),left=((Number)call(physics,"targetLeft")).floatValue();
            write(physics,"x",left+radius-2); write(physics,"y",ground-radius-4); write(physics,"cameraY",ground-660); write(physics,"vx",0f); write(physics,"vy",120f);
        });
        stepUntilRefill(); require(!physicsBool("won"),"safe off-target floor refills without claiming victory"); capture("jumps-safe-floor-refill-controlled"); goHome();
    }
    private void splashRegression() throws Exception {
        requireScreen(SPLASH,"fresh local launch shows custom splash"); runFrames(620); requireScreen(SPLASH,"custom splash has a real timed frame"); capture("custom-splash");
        ui(() -> {
            TypedValue icon=new TypedValue(); require(activity.getTheme().resolveAttribute(android.R.attr.windowSplashScreenAnimatedIcon,icon,true),"actual launch theme declares system splash art");
            String art=activity.getResources().getResourceEntryName(icon.resourceId);
            require("splash_icon".equals(art)&&icon.resourceId!=activity.getApplicationInfo().icon,"system splash uses dedicated brand art instead of launcher icon");
        });
    }
    private void featureRegression() throws Exception {
        require(APP.equals("com.perfgames.falley.motiontest"),"feature check targets isolated debug app"); splashRegression();
        waitScreen(HOME,4000);
        ui(() -> require(((Number)read(read(arena,"profile"),"preset")).intValue()==0&&((Number)read(read(arena,"profile"),"language")).intValue()==0,"fresh profile defaults to Loose and English"));
        click("settings"); click("preset_2"); recreate();
        ui(() -> require(((Number)read(read(arena,"profile"),"preset")).intValue()==2,"saved Tight survives Activity recreation"));
        click("chapters"); require(numeric(arena,"chapterPage")==0,"first chapter page begins at stage1");
        for (int i=0;i<6;i++) require(button("stage_"+i)!=null,"first page exposes stage "+(i+1));
        require(!buttonEnabled("prev_chapters")&&buttonEnabled("next_chapters"),"chapter page1 navigation states are correct");
        accessibleClick("next_chapters"); require(numeric(arena,"chapterPage")==1,"accessible Next opens stages7to12");
        for (int i=6;i<12;i++) require(button("stage_"+i)!=null,"second page exposes stage "+(i+1));
        require(buttonEnabled("prev_chapters")&&!buttonEnabled("next_chapters"),"chapter page2 navigation states are correct"); capture("chapters-page-2");
        ui(() -> require(arena.getAccessibilityNodeProvider().performAction(-1,AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD,null),"accessible chapter scroll moves backward")); settle(60);
        require(numeric(arena,"chapterPage")==0,"accessible chapter scroll returns page1");
        pointer(MotionEvent.ACTION_DOWN,390,400); pointer(MotionEvent.ACTION_MOVE,100,400); release(100,400);
        requireScreen(CHAPTERS,"horizontal chapter swipe does not launch a stage"); require(numeric(arena,"chapterPage")==1,"horizontal chapter swipe opens page2");
        fixtureUnlockThrough(11); click("stage_11"); require(stageIndex()==11&&storage.getInt("selectedStage",-1)==11,"stage12 selection is playable and saved after explicit prerequisite fixture grants"); recreate();
        require(stageIndex()==11,"stage12 survives recreation"); click("chapters"); require(numeric(arena,"chapterPage")==1,"saved stage12 reopens correct chapter page"); back();
        Set<String> homeLabels=new HashSet<>(),policyTexts=new HashSet<>();
        for (int language=0;language<3;language++) {
            featureLanguage(language); localizedNodes(language,"Settings "+language); renderedFit("Settings "+language,false); capture("language-"+language+"-settings");
            click("privacy"); localizedNodes(language,"Privacy "+language); renderedFit("Privacy "+language,true); capture("language-"+language+"-privacy-top");
            ui(() -> { AccessibilityNodeInfo node=arena.getAccessibilityNodeProvider().createAccessibilityNodeInfo(10000); policyTexts.add(node.getText().toString()); node.recycle(); });
            for (int swipe=0;swipe<25&&numeric(arena,"privacyScroll")<numeric(arena,"privacyScrollLimit")-1;swipe++) { pointer(MotionEvent.ACTION_DOWN,240,620); pointer(MotionEvent.ACTION_MOVE,240,170); release(240,170); }
            require(numeric(arena,"privacyScroll")>=numeric(arena,"privacyScrollLimit")-1,"localized policy reaches end with bounded real swipes");
            capture("language-"+language+"-privacy-end"); back(); back(); recreate();
            require(storage.getInt("language",-1)==language,"language survives real Activity recreation: "+language);
            ui(() -> { require(((Number)read(read(arena,"profile"),"language")).intValue()==storage.getInt("language",-1),"loaded UI uses persisted language"); homeLabels.add(read(button("settings"),"label").toString()); });
            localizedNodes(language,"Home "+language); renderedFit("Home "+language,false); capture("language-"+language+"-home");
            click("help"); localizedNodes(language,"Help "+language); renderedFit("Help "+language,false); capture("language-"+language+"-help"); back();
            selectStage(0); localizedHud(); capture("language-"+language+"-hud"); click("pause"); click("settings"); click("language"); back(); requireScreen(SETTINGS,"language Back preserves Pause settings"); back(); requireScreen(PAUSE,"settings Back preserves paused game"); back();
            ceilingFixture(); runFrames(160); capture("language-"+language+"-ceiling-impact-controlled"); waitScreen(RESULT,3500);
            localizedNodes(language,"Result "+language); renderedFit("Result "+language,false); capture("language-"+language+"-result-controlled"); click("home");
        }
        require(homeLabels.size()==3&&policyTexts.size()==3,"all three menus and full offline policies contain distinct localized text");
        featureLanguage(0); back(); featureJumps(); selectStage(11); stageTargetFixture(); runUntilPhysics("won",true,1500); waitScreen(RESULT,2500);
        requireButtonsAbsent("next_stage"); capture("stage-12-victory-controlled"); click("home");
    }

    private int unlockedStage() throws Exception { final int[] value={0}; ui(() -> value[0]=((Number)call(read(arena,"profile"),"highestUnlockedStage")).intValue()); return value[0]; }
    private String helpContent() throws Exception {
        final String[] text={""}; ui(() -> { AccessibilityNodeInfo content=arena.getAccessibilityNodeProvider().createAccessibilityNodeInfo(10000); require(content!=null,"visual Help exposes current-page readable content"); text[0]=content.getText().toString(); content.recycle(); }); return text[0];
    }
    private void languageCaption() throws Exception {
        ui(() -> {
            Bitmap bitmap=Bitmap.createBitmap(arena.getWidth(),arena.getHeight(),Bitmap.Config.ARGB_8888);
            try {
                HudCanvas canvas=new HudCanvas(bitmap); arena.draw(canvas); Object hit=button("language"); String label=read(hit,"label").toString(); boolean found=false;
                for (Glyph glyph:canvas.glyphs) if (glyph.text.equals(label)&&glyph.ink.top>=20&&glyph.ink.bottom<=94) {
                    require(glyph.ink.left>=382&&glyph.ink.right<=456&&glyph.ink.height()>0,"Language caption is visibly drawn inside its accessible header control"); found=true;
                }
                require(found,"Language is a visible localized label rather than an icon-only control");
            } finally { bitmap.recycle(); }
        });
    }
    private void helpRegions(String context) throws Exception {
        renderedFit(context,false);
        ui(() -> {
            Bitmap bitmap=Bitmap.createBitmap(arena.getWidth(),arena.getHeight(),Bitmap.Config.ARGB_8888);
            try {
                HudCanvas canvas=new HudCanvas(bitmap); arena.draw(canvas); List<Glyph> body=new ArrayList<>(),tip=new ArrayList<>();
                for (Glyph glyph:canvas.glyphs) {
                    if (glyph.ink.top>=460&&glyph.ink.top<587) { require(glyph.ink.bottom<=579,context+" explanation stays in its panel"); body.add(glyph); }
                    if (glyph.ink.top>=598&&glyph.ink.top<700) { require(glyph.ink.bottom<=695,context+" tip stays above footer"); tip.add(glyph); }
                }
                require(!body.isEmpty()&&!tip.isEmpty(),context+" has visual explanation and tip rows");
                for (List<Glyph> rows:java.util.Arrays.asList(body,tip)) for (int a=0;a<rows.size();a++) for (int b=a+1;b<rows.size();b++) require(!RectF.intersects(rows.get(a).ink,rows.get(b).ink),context+" wrapped rows do not overlap");
                for (Glyph glyph:body) for (Glyph other:tip) require(!RectF.intersects(glyph.ink,other.ink),context+" explanation clears tip region");
            } finally { bitmap.recycle(); }
        });
    }
    private void revisionHelp(boolean smoke) throws Exception {
        int languages=smoke?1:3;
        for (int language=0;language<languages;language++) {
            featureLanguage(language); languageCaption(); capture("revision-language-"+language+"-settings"); back();
            click("help"); require(!bool("helpMotion")&&numeric(arena,"helpPage")==0,"Help opens on the current Touch control mode");
            click("help_touch"); String touch=helpContent(); localizedNodes(language,"Touch Help "+language); helpRegions("Touch Help "+language); capture("revision-language-"+language+"-help-touch");
            click("help_motion"); String motion=helpContent();
            require(bool("helpMotion")&&!touch.equals(motion),"Motion Help has distinct current-page content and diagram state");
            require(!bool("motionEnabled")&&!storage.getBoolean("motion",true),"previewing Motion Help does not change gameplay controls");
            localizedNodes(language,"Motion Help "+language); helpRegions("Motion Help "+language); capture("revision-language-"+language+"-help-motion");
            click("help_next"); require(numeric(arena,"helpPage")==1,"Help Next opens burst/recovery/budget guide"); helpRegions("Burst Help "+language); capture("revision-language-"+language+"-help-burst");
            ui(() -> require(arena.getAccessibilityNodeProvider().performAction(-1,AccessibilityNodeInfo.ACTION_SCROLL_FORWARD,null),"accessible Help forward opens danger guide")); settle(60);
            require(numeric(arena,"helpPage")==2,"Help exposes third visual guide"); require(!buttonEnabled("help_next"),"last Help page disables Next"); helpRegions("Danger Help "+language); capture("revision-language-"+language+"-help-danger");
            click("help_prev"); require(numeric(arena,"helpPage")==1,"Help Previous returns one visual page"); back(); requireScreen(HOME,"Help Done returns Home");
        }
        featureLanguage(0); back();
        if (!smoke) {
            click("settings"); if (bool("hasMotion")) {
                click("motion"); back(); click("help"); require(bool("helpMotion"),"Help initially selects Motion when actual motion steering is enabled");
                click("help_touch"); require(!bool("helpMotion")&&bool("motionEnabled"),"Touch preview leaves actual Motion setting enabled"); back(); click("settings"); click("motion");
            }
            back();
        }
    }
    private void revisionLocks() throws Exception {
        require(unlockedStage()==0,"fresh campaign opens only the first stage"); click("chapters");
        require(buttonEnabled("stage_0")&&!buttonEnabled("stage_1"),"stage0 is playable and stage1 visibly locked"); capture("revision-campaign-initial-locks");
        click("stage_1"); requireScreen(CHAPTERS,"real tap cannot enter a locked stage");
        ui(() -> require(!arena.getAccessibilityNodeProvider().performAction(virtualButtonId("stage_1"),AccessibilityNodeInfo.ACTION_CLICK,null),"locked stage rejects accessibility activation"));
        ui(() -> call(arena,"activate","stage_1")); requireScreen(CHAPTERS,"direct stage activation guard rejects locked stage");
        require(storage.getInt("selectedStage",0)==0&&unlockedStage()==0,"rejected entry does not advance stored campaign progress");
        click("next_chapters"); require(!buttonEnabled("stage_11"),"last campaign stage is locked on a fresh profile"); capture("revision-campaign-late-locks"); back();
        click("play"); stageTargetFixture(); runUntilPhysics("won",true,1500); waitScreen(RESULT,2500);
        require(unlockedStage()==1&&storage.getInt("stage_wins_0",0)==1,"actual completed stage unlocks only its immediate successor");
        require(buttonEnabled("next_stage"),"Next Stage becomes available after the recorded win"); capture("revision-next-stage-unlocked-controlled");
        click("next_stage"); require(stageIndex()==1,"Next Stage enters the newly unlocked stage"); recreate();
        require(unlockedStage()==1&&stageIndex()==1,"unlocked campaign and selection persist through Activity recreation");
        click("chapters"); require(buttonEnabled("stage_1")&&!buttonEnabled("stage_2"),"persisted progress leaves the following chapter locked"); capture("revision-campaign-unlock-persisted"); back();
    }
    private void prepareAirBurst() throws Exception {
        ui(() -> {
            write(physics,"x",240f); write(physics,"y",1000f); write(physics,"cameraY",570f); write(physics,"vx",0f); write(physics,"vy",0f);
            for (Object rope:(Object[])read(physics,"ropes")) write(rope,"cut",0);
            arena.invalidate();
        }); redraw();
    }
    private void freeBurstSteps(int steps) throws Exception {
        ui(() -> {
            float dt=((Number)read(physics.getClass(),"DT")).floatValue();
            for (int i=0;i<steps;i++) {
                write(physics,"x",240f); write(physics,"vx",0f);
                for (Object rope:(Object[])read(physics,"ropes")) write(rope,"cut",0);
                call(arena,"advance",dt,SystemClock.uptimeMillis());
                require(!(Boolean)read(physics,"lost")&&!(Boolean)read(physics,"won"),"controlled free-air burst remains alive");
                require(((Number)read(physics,"landingPulse")).floatValue()==0,"repeat burst has no rope or floor support contact");
            }
            arena.invalidate();
        });
    }
    private void recoverAirBurst(float startY,float lastJump) throws Exception {
        int remaining=0;
        for (int batch=0;batch<12;batch++) {
            freeBurstSteps(40); remaining=jumps(); if (remaining==2) break;
        }
        require(remaining==2,"burst refills while descending without any landing");
        require(physicsFloat("elapsed")-lastJump>=.899f&&physicsFloat("y")>=startY+44.9f&&physicsFloat("vy")>=80,"recovery waits for time, real descent below burst start and downward velocity");
        require(physicsCallFloat("burstRecoveryProgress")==1,"completed burst recovery reports full readiness"); redraw();
    }
    private void budgetHud(String expected) throws Exception {
        ui(() -> {
            Bitmap bitmap=Bitmap.createBitmap(arena.getWidth(),arena.getHeight(),Bitmap.Config.ARGB_8888);
            try {
                HudCanvas canvas=new HudCanvas(bitmap); arena.draw(canvas); boolean found=false; List<Glyph> footer=new ArrayList<>();
                for (Glyph glyph:canvas.glyphs) if (!glyph.text.trim().isEmpty()&&glyph.ink.top>=744&&glyph.ink.bottom<=816) { footer.add(glyph); if (glyph.text.equals(expected)) found=true; }
                require(found,"actual jump HUD displays "+expected);
                for (int a=0;a<footer.size();a++) for (int b=a+1;b<footer.size();b++) require(!RectF.intersects(footer.get(a).ink,footer.get(b).ink),"burst, total and recovery footer glyphs do not overlap");
            } finally { bitmap.recycle(); }
        });
    }
    private void revisionBursts() throws Exception {
        selectStage(0); require(physicsCallFloat("jumpLimit")==8&&physicsCallFloat("jumpAllowanceRemaining")==8,"first campaign stage has finite total allowance8"); prepareAirBurst();
        float dt=physicsFloatConstant("DT");
        for (int burst=0;burst<4;burst++) {
            require(jumps()==2&&buttonEnabled("jump"),"recovered burst can be used again before total is exhausted");
            float start=physicsFloat("y"); click("jump"); require(physicsCallFloat("jumpsUsed")==burst*2+1,"first UI jump spends one total allowance");
            freeBurstSteps((int)Math.ceil(.32f/dt)); redraw(); tap(240,390); redraw();
            require(jumps()==0&&physicsCallFloat("jumpsUsed")==burst*2+2,"second arena tap spends the second burst jump");
            float last=physicsFloat("elapsed"),used=physicsCallFloat("jumpsUsed"); click("jump"); tap(240,390);
            require(physicsCallFloat("jumpsUsed")==used,"exhausted burst rejects extra immediate button/arena taps");
            recoverAirBurst(start,last);
            require(physicsCallFloat("jumpsUsed")==used&&physicsCallFloat("jumpAllowanceRemaining")==8-used,"recovery restores the burst without replenishing the campaign total");
            if (burst==0) capture("revision-burst-recovered-without-landing");
        }
        require(physicsCallFloat("jumpsUsed")==8&&physicsCallFloat("jumpAllowanceRemaining")==0&&!buttonEnabled("jump"),"finite campaign budget blocks jumping even after burst recovery");
        float used=physicsCallFloat("jumpsUsed"); click("jump"); tap(240,390); require(physicsCallFloat("jumpsUsed")==used,"total-budget exhaustion rejects further jump attempts");
        budgetHud("TOTAL 0/8"); capture("revision-campaign-budget-exhausted"); goHome();
        startArcade(0,1); require(physicsCallFloat("jumpLimit")==0&&physicsCallFloat("jumpAllowanceRemaining")==-1,"Arcade has no lifetime jump limit"); prepareAirBurst();
        float start=physicsFloat("y"); click("jump"); freeBurstSteps((int)Math.ceil(.32f/dt)); redraw(); tap(240,390); recoverAirBurst(start,physicsFloat("elapsed")); click("jump");
        require(physicsCallFloat("jumpsUsed")==3,"Arcade permits a third manual jump after free-air recovery"); budgetHud("NO TOTAL LIMIT"); capture("revision-arcade-repeat-jumps"); goHome();
    }
    private float physicsFloatConstant(String name) throws Exception { return ((Number)read(physics.getClass(),name)).floatValue(); }
    private void revisionDrift() throws Exception {
        selectStage(0); float before=physicsFloat("x");
        ui(() -> {
            float dt=((Number)read(physics.getClass(),"DT")).floatValue(),sum=0,maximum=0;
            for (int i=0;i<(int)(1f/dt);i++) {
                write(physics,"y",310f); write(physics,"vy",0f); call(arena,"advance",dt,SystemClock.uptimeMillis());
                float drift=((Number)call(physics,"driftVelocity")).floatValue(); sum+=Math.abs(drift); maximum=Math.max(maximum,Math.abs(drift));
            }
            require(sum>1&&maximum<=35,"touch drift remains nonzero and bounded by35px/s"); arena.invalidate();
        });
        require(Math.abs(physicsFloat("x")-before)>1&&Math.abs(physicsFloat("x")-before)<35,"idle touch drift is small and still requires gentle correction");
        capture("revision-gentle-touch-drift"); goHome();
    }
    private void revisionRegression(boolean smoke) throws Exception {
        require(APP.equals(smoke?"com.perfgames.falley":"com.perfgames.falley.motiontest"),"revision targets the requested isolated debug or production release package");
        boolean debug=(getTargetContext().getApplicationInfo().flags&android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE)!=0;
        require(debug!=smoke,"revision package has expected debug/release flag");
        log.append("FONT_SCALE ").append(getTargetContext().getResources().getConfiguration().fontScale).append('\n');
        revisionLocks(); revisionHelp(smoke); revisionBursts(); revisionDrift();
    }

    private int virtualButtonId(String id) throws Exception {
        List<?> buttons=(List<?>)read(arena,"buttons");
        for (int i=0;i<buttons.size();i++) if (id.equals(read(buttons.get(i),"id"))) return i+1;
        throw new AssertionError("Missing accessibility button "+id);
    }
    private void accessibleClick(String id) throws Exception {
        ui(() -> require(arena.getAccessibilityNodeProvider().performAction(virtualButtonId(id),AccessibilityNodeInfo.ACTION_CLICK,null),"virtual accessibility click works for "+id));
        settle(65);
    }
    private void accessibleMenu(String name) throws Exception {
        ui(() -> {
            AccessibilityNodeProvider provider=arena.getAccessibilityNodeProvider();
            require(provider!=null,name+" exposes an accessibility provider");
            AccessibilityNodeInfo host=provider.createAccessibilityNodeInfo(-1);
            require(host!=null&&host.getChildCount()>=((List<?>)read(arena,"buttons")).size(),name+" host exposes every rendered control");
            require(host.getContentDescription()!=null&&host.getContentDescription().length()>0,name+" has an accessible screen name");
            host.recycle();
            List<?> buttons=(List<?>)read(arena,"buttons");
            for (int i=0;i<buttons.size();i++) {
                Object hit=buttons.get(i); AccessibilityNodeInfo node=provider.createAccessibilityNodeInfo(i+1);
                require(node!=null&&node.getText()!=null&&node.getText().length()>0,name+" control has a spoken label: "+read(hit,"id"));
                Rect bounds=new Rect(); node.getBoundsInScreen(bounds);
                require(bounds.width()>0&&bounds.height()>0,name+" control has real screen bounds: "+read(hit,"id"));
                require(node.isEnabled()==(Boolean)read(hit,"enabled")&&node.isClickable()==(Boolean)read(hit,"enabled"),name+" control action matches enabled state: "+read(hit,"id"));
                require(node.isCheckable()==(Boolean)read(hit,"checkable")&&node.isChecked()==(Boolean)read(hit,"checked"),name+" control announces its selected state: "+read(hit,"id"));
                node.recycle();
            }
        });
    }
    private void awake(boolean expected,String context) throws Exception {
        animation(true); SystemClock.sleep(100);
        ui(() -> require(arena.getKeepScreenOn()==expected,"screen awake is "+expected+" in "+context));
        animation(false); settle(60);
        ui(() -> require(!arena.getKeepScreenOn(),"inactive fixture releases screen awake in "+context));
    }
    private void privacyAccessibilityAndAwake() throws Exception {
        goHome(); accessibleMenu("Home"); awake(false,"Home");
        accessibleClick("settings"); requireScreen(SETTINGS,"accessibility action opens Settings"); accessibleMenu("Settings");
        click("privacy"); requireScreen(PRIVACY,"Privacy Policy opens by real touch");
        accessibleMenu("Privacy"); awake(false,"Privacy"); capture("privacy-top");
        float limit=numeric(arena,"privacyScrollLimit"); require(limit>0&&numeric(arena,"privacyScroll")==0,"policy starts at top and has scrollable content");
        pointer(MotionEvent.ACTION_DOWN,240,620); pointer(MotionEvent.ACTION_MOVE,240,210); release(240,210);
        require(numeric(arena,"privacyScroll")>0,"real pointer swipe scrolls policy text");
        for (int i=0;i<15&&numeric(arena,"privacyScroll")<limit-1;i++) {
            pointer(MotionEvent.ACTION_DOWN,240,620); pointer(MotionEvent.ACTION_MOVE,240,180); release(240,180);
        }
        require(Math.abs(numeric(arena,"privacyScroll")-limit)<1,"real swipes reach the final policy paragraph"); capture("privacy-end");
        ui(() -> {
            AccessibilityNodeProvider provider=arena.getAccessibilityNodeProvider();
            AccessibilityNodeInfo content=provider.createAccessibilityNodeInfo(10000);
            String text=content.getText().toString();
            require(content.isScrollable()&&text.contains("Perf Games")&&text.contains("Motion controls")&&text.contains("About this policy"),"accessible policy exposes full text including final section");
            content.recycle();
            require(provider.performAction(10000,AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS,null),"policy content accepts accessibility focus");
            AccessibilityNodeInfo focused=provider.findFocus(AccessibilityNodeInfo.FOCUS_ACCESSIBILITY);
            require(focused!=null&&focused.isAccessibilityFocused(),"policy accessibility focus can be retrieved"); if (focused!=null) focused.recycle();
            require(provider.performAction(10000,AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD,null),"policy supports accessible backward scrolling");
        }); settle(50);
        require(numeric(arena,"privacyScroll")<limit,"accessible backward scroll moves toward start");
        ui(() -> require(arena.getAccessibilityNodeProvider().performAction(-1,AccessibilityNodeInfo.ACTION_SCROLL_FORWARD,null),"policy host supports accessible forward scrolling")); settle(50);
        require(Math.abs(numeric(arena,"privacyScroll")-limit)<1,"accessible forward scroll returns to end");
        back(); requireScreen(SETTINGS,"system Back returns privacy to Home settings"); back(); requireScreen(HOME,"Home settings returns to Home");
        accessibleClick("arcade"); requireScreen(ARCADE_SETUP,"accessibility action opens Arcade"); accessibleMenu("Arcade setup");
        int mask=storage.getInt("arcadeHazards",31); accessibleClick("arcade_saw");
        require(storage.getInt("arcadeHazards",31)==(mask^16),"accessible hazard switch changes persisted checked state");
        accessibleClick("arcade_saw"); back();
        click("play"); awake(true,"active gameplay"); click("pause"); requireScreen(PAUSE,"pause after awake check");
        accessibleMenu("Pause"); awake(false,"Pause"); click("settings"); click("privacy");
        requireScreen(PRIVACY,"pause settings opens Privacy"); freeze("privacy menu freezes gameplay");
        // The footer is a second Back control. Use its actual physical touch area.
        tap(240,780); requireScreen(SETTINGS,"Privacy footer returns to Pause settings"); back(); requireScreen(PAUSE,"Pause settings retains its return context");
        accessibleClick("resume"); requireScreen(PLAY,"accessible Resume returns to gameplay"); goHome();
    }

    private static final String POLICY_URL="https://falley-privacy-policy.vercel.app/";
    private static final String PRIVACY_EMAIL="mathurrohitash16@gmail.com";
    private String visiblePrivacyText() throws Exception {
        final StringBuilder visible=new StringBuilder();
        ui(() -> {
            Bitmap bitmap=Bitmap.createBitmap(arena.getWidth(),arena.getHeight(),Bitmap.Config.ARGB_8888);
            try {
                HudCanvas canvas=new HudCanvas(bitmap); arena.draw(canvas);
                for (Glyph glyph:canvas.glyphs) if (glyph.ink.top>=128&&glyph.ink.bottom<=632) visible.append(glyph.text);
            } finally { bitmap.recycle(); }
        });
        return visible.toString().replaceAll("\\s+","");
    }
    private void privacyIntent(String id,String action,String uri,boolean accessible) throws Exception {
        final Intent[] captured={null};
        ActivityMonitor monitor=new ActivityMonitor() {
            @Override public ActivityResult onStartActivity(Intent intent) {
                captured[0]=new Intent(intent);
                return new ActivityResult(Activity.RESULT_CANCELED,null);
            }
        };
        addMonitor(monitor);
        try {
            if (accessible) accessibleClick(id); else click(id);
            require(captured[0]!=null,"privacy action requests an external app: "+id);
            require(action.equals(captured[0].getAction()),"privacy action uses the appropriate browser/mail action: "+id);
            android.net.Uri expected=android.net.Uri.parse(uri),actual=captured[0].getData();
            boolean destination=actual!=null&&expected.getScheme().equals(actual.getScheme())
                    &&expected.getSchemeSpecificPart().equals(actual.getSchemeSpecificPart())&&actual.getFragment()==null;
            require(destination,"privacy action uses the exact public destination, allowing safe URI escaping: "+id);
            requireScreen(PRIVACY,"intercepted privacy handoff keeps the offline policy available: "+id);
            log.append("INTERCEPTED ").append(action).append(' ').append(uri).append("; external activity and message sending blocked by test monitor\n");
        } finally { removeMonitor(monitor); }
    }
    private void privacyContactRegression() throws Exception {
        String[] requested=getTargetContext().getPackageManager().getPackageInfo(APP,android.content.pm.PackageManager.GET_PERMISSIONS).requestedPermissions;
        require(requested==null||requested.length==0,"policy handoffs add no requested app permissions, including no Internet permission");
        log.append("FONT_SCALE ").append(getTargetContext().getResources().getConfiguration().fontScale).append('\n');
        Set<String> localizedPolicies=new HashSet<>();
        for (int language=0;language<3;language++) {
            featureLanguage(language); click("privacy"); requireScreen(PRIVACY,"privacy opens from Settings by real touch in language "+language);
            localizedNodes(language,"Privacy contact "+language); renderedFit("Privacy contact "+language,true);
            require(button("privacy_web")!=null&&button("privacy_email")!=null,"both public policy/contact controls are present in language "+language);
            capture("privacy-contact-"+language+"-top");
            ui(() -> {
                AccessibilityNodeInfo content=arena.getAccessibilityNodeProvider().createAccessibilityNodeInfo(10000);
                require(content!=null&&content.isScrollable(),"policy content is readable and scrollable through accessibility");
                String text=content.getText().toString(); localizedPolicies.add(text);
                require(text.contains(POLICY_URL)&&text.contains(PRIVACY_EMAIL),"accessible full policy contains exact online URL and monitored contact");
                content.recycle();
            });
            boolean sawUrl=false,sawEmail=false;
            for (int swipe=0;swipe<75;swipe++) {
                String visible=visiblePrivacyText();
                if (!sawUrl&&visible.contains(POLICY_URL)) { sawUrl=true; capture("privacy-contact-"+language+"-url"); }
                sawEmail|=visible.contains(PRIVACY_EMAIL);
                if (numeric(arena,"privacyScroll")>=numeric(arena,"privacyScrollLimit")-1) break;
                // Overlap successive viewports so a wrapped address is observed together.
                pointer(MotionEvent.ACTION_DOWN,240,540); pointer(MotionEvent.ACTION_MOVE,240,320); release(240,320);
            }
            require(numeric(arena,"privacyScroll")>=numeric(arena,"privacyScrollLimit")-1,"real swipes reach the final privacy contact in language "+language);
            require(sawUrl&&sawEmail,"scrolling exposes the entire wrapped policy URL and email as visible text in language "+language+"; URL="+sawUrl+"; email="+sawEmail);
            renderedFit("Privacy contact end "+language,true); capture("privacy-contact-"+language+"-end");
            privacyIntent("privacy_web",Intent.ACTION_VIEW,POLICY_URL,false);
            privacyIntent("privacy_email",Intent.ACTION_SENDTO,"mailto:"+PRIVACY_EMAIL,true);
            back(); requireScreen(SETTINGS,"Android Back returns privacy to Settings in language "+language);
            click("privacy"); click("back"); requireScreen(SETTINGS,"privacy Back button returns to Settings in language "+language);
            back(); requireScreen(HOME,"Settings Back returns to Home after policy in language "+language);
        }
        require(localizedPolicies.size()==3,"all three full policies keep distinct localized copy with identical contact details");
    }

    private void chaptersAndPersistence() throws Exception {
        click("chapters"); requireScreen(CHAPTERS,"chapter selection opens"); capture("chapters"); freeze("chapter menu physics frozen");
        for (int i=0;i<6;i++) require(button("stage_"+i)!=null,"stage "+(i+1)+" has a real rendered card");
        click("stage_3"); requireScreen(PLAY,"stage card begins its run");
        require(stageIndex()==3&&storage.getInt("selectedStage",-1)==3,"chapter card selects and persists stage 4");
        recreate(); requireScreen(HOME,"recreation skips intro and opens home");
        require(stageIndex()==3&&storage.getInt("selectedStage",-1)==3,"selected stage survives Activity recreation");
        click("play"); requireScreen(PLAY,"home Play uses selected stage"); require(stageIndex()==3,"home retains selected stage 4");
        goHome(); selectStage(0);
    }

    private void swipesPauseAndRestart() throws Exception {
        requireScreen(PLAY,"stage 1 ready for touch tests"); capture("gameplay");
        requireButtonsAbsent("zero","cycle_preset","motion");
        ui(() -> {
            require(((Number)read(button("restart"),"bottom")).floatValue()<=94,"Restart stays in header");
            require(((Number)read(button("pause"),"bottom")).floatValue()<=94,"Pause stays in header");
            require(((Number)read(button("jump"),"left")).floatValue()==24&&((Number)read(button("jump"),"right")).floatValue()==456,"Jump uses full bottom width");
            write(physics,"y",350f); write(physics,"vy",120f);
        });
        float before=physicsFloat("elapsed"); runFrames(260);
        require(physicsFloat("elapsed")>before+.10f,"gameplay advances under gravity");
        click("restart"); ui(() -> { write(physics,"y",350f); write(physics,"vy",120f); });
        pointer(MotionEvent.ACTION_DOWN,240,390); settle(25);
        require(physicsFloat("vy")==120f,"touch down alone does not jump");
        pointer(MotionEvent.ACTION_MOVE,295,390); settle(25);
        require(touchVelocity()>0,"right swipe chooses positive touch velocity"); runFrames(110);
        require(physicsFloat("x")>240f&&physicsFloat("vy")>0,"right swipe moves right without an automatic jump");
        capture("swipe-right-held"); release(295,390); require(touchVelocity()==0,"swipe release clears touch velocity");
        float releasedX=physicsFloat("x"); runFrames(100);
        require(Math.abs(physicsFloat("x")-releasedX)<20f,"released environmental drift stays bounded");
        require(physicsCallFloat("steeringVelocity")==0,"release clears the model steering command");
        ui(() -> { write(physics,"x",240f); write(physics,"y",350f); write(physics,"vy",120f); write(physics,"vx",0f); });
        dragHeld(240,390,185,390); runFrames(110);
        require(physicsFloat("x")<240f&&physicsFloat("vy")>0,"left swipe moves left without an automatic jump");
        pointer(MotionEvent.ACTION_CANCEL,185,390); settle(35); require(touchVelocity()==0,"cancel clears swipe velocity");
        releasedX=physicsFloat("x"); runFrames(100);
        require(Math.abs(physicsFloat("x")-releasedX)<20f,"canceled environmental drift stays bounded");
        require(physicsCallFloat("steeringVelocity")==0,"cancel clears the model steering command");
        ui(() -> { write(physics,"x",200f); write(physics,"y",447f); write(physics,"vy",120f); write(physics,"vx",0f); });
        runFrames(260); require(Math.abs(physicsFloat("x")-200f)<40f,"idle rope motion and wind stay controllable");
        ui(() -> { write(physics,"x",240f); write(physics,"y",350f); write(physics,"vy",0f); write(physics,"vx",0f); });
        float drift=physicsCallFloat("driftVelocity");
        float counter=drift>=0?-65f:65f;
        dragHeld(240,390,240+counter,390); runFrames(140);
        require((physicsFloat("x")-240f)*counter>0,"countersteering overcomes environmental drift");
        pointer(MotionEvent.ACTION_CANCEL,240+counter,390); settle(35);
        click("restart"); click("jump"); require(physicsFloat("vy")<0,"Jump button sends character upward");
        click("restart"); tap(240,390); require(physicsFloat("vy")<0,"short arena tap jumps on release");
        click("pause"); requireScreen(PAUSE,"Pause opens by touch"); capture("pause"); freeze("pause physics frozen");
        click("settings"); requireScreen(SETTINGS,"pause settings opened"); requireButtonsAbsent("zero"); freeze("pause settings physics frozen");
        back(); requireScreen(PAUSE,"Android back returns settings to pause"); freeze("return to pause stays frozen");
        before=physicsFloat("elapsed"); back(); requireScreen(PLAY,"Android back resumes pause");
        runFrames(220); require(physicsFloat("elapsed")>before+.09f,"resume advances the gameplay clock");
        click("restart"); requireScreen(PLAY,"header Restart starts a fresh run");
        require(physicsFloat("elapsed")<.25f,"Restart resets elapsed time");
        require(!physicsBool("lost")&&!physicsBool("won"),"Restart clears terminal states");
        click("pause"); click("restart"); requireScreen(PLAY,"pause Restart resumes fresh run");
        require(physicsFloat("elapsed")<.25f,"pause Restart resets elapsed time");
        goHome(); freeze("home freezes an abandoned run"); click("play"); requireScreen(PLAY,"new run begins from home");
        require(physicsFloat("elapsed")<.25f,"new home run resets timer");
    }

    private void allStageLandings() throws Exception {
        // This fixture stages a center descent immediately above the floor. Actual frame updates
        // must perform collision, bounce, win animation, result transition and persistence.
        final int stageCount=((Number)read(Class.forName("com.perfgames.ropelab.StageCatalog",true,getTargetContext().getClassLoader()),"COUNT")).intValue();
        final int priorGlobalWins=storage.getInt("wins",0);
        for (int stage=0;stage<stageCount;stage++) {
            int priorStageWins=storage.getInt("stage_wins_"+stage,0);
            require(stageIndex()==stage,"next stage is "+(stage+1));
            if (stage==0||stage==2||stage==4) capture("chapter-"+(stage/2+1)+"-gameplay");
            stageTargetFixture(); runUntilPhysics("won",true,2000);
            require(physicsBool("won")&&!physicsBool("lost"),"stage "+(stage+1)+" target landing wins");
            require(physicsFloat("vy")<0&&physicsFloat("groundBounce")>0,"stage "+(stage+1)+" target physically bounces");
            if (stage==0||stage==stageCount-1) capture("stage-"+(stage+1)+"-target-bounce-controlled");
            waitScreen(RESULT,2500); if (stage==0) { accessibleMenu("Result"); awake(false,"Result"); } capture("stage-"+(stage+1)+"-victory-controlled");
            require(storage.getInt("wins",0)==priorGlobalWins+stage+1&&storage.getInt("stage_wins_"+stage,0)==priorStageWins+1,"stage "+(stage+1)+" records a single additional win");
            freeze("stage "+(stage+1)+" result physics frozen");
            require(storage.getInt("stage_wins_"+stage,0)==priorStageWins+1,"stage result redraw cannot duplicate win");
            if (stage<stageCount-1) { click("next_stage"); requireScreen(PLAY,"Next Stage starts gameplay"); }
            else requireButtonsAbsent("next_stage");
        }
        click("restart"); requireScreen(PLAY,"last stage Replay restarts"); require(!physicsBool("won"),"replay clears terminal victory");
        goHome(); recreate(); requireScreen(HOME,"records survive recreation"); require(stageIndex()==stageCount-1,"last selected stage persists");
        ui(() -> {
            Object profile=read(arena,"profile"); int[] wins=(int[])read(profile,"stageWins"), best=(int[])read(profile,"best"); float[] times=(float[])read(profile,"stageTimes");
            for (int i=0;i<stageCount;i++) require(wins[i]>=1&&best[i]>=100&&times[i]>0,"persisted record for stage "+(i+1));
        });
    }

    private void stageTargetFixture() throws Exception {
        ui(() -> {
            call(physics,"reset"); float ground=((Number)call(physics,"groundY")).floatValue();
            float radius=((Number)call(physics,"radius")).floatValue();
            float left=((Number)call(physics,"targetLeft")).floatValue(),right=((Number)call(physics,"targetRight")).floatValue();
            write(physics,"x",(left+right)/2f); write(physics,"y",ground-radius-4f);
            write(physics,"vx",0f); write(physics,"vy",120f); write(physics,"gx",0f);
            write(physics,"cameraY",ground-660f); arena.invalidate();
        });
    }

    private void chapterHazards() throws Exception {
        String[] expected={"LASER","SPIKES","FIRE"};
        for (int chapter=0;chapter<3;chapter++) {
            final String reason=expected[chapter];
            selectStage(chapter*2); ui(() -> { write(physics,"x",10f); write(physics,"vx",0f); write(physics,"gx",0f); arena.invalidate(); });
            runUntilPhysics("lost",true,1500); runFrames(160);
            capture("chapter-"+(chapter+1)+"-death-effect-controlled");
            waitScreen(RESULT,3500); require(physicsBool("lost")&&!physicsBool("won"),"chapter "+(chapter+1)+" hazard collision loses");
            ui(() -> require(reason.equals(read(physics,"deathReason")),"chapter hazard reason matches its visuals: "+reason));
            capture("chapter-"+(chapter+1)+"-hazard-death-controlled"); click("home");
        }
    }

    private void fireRain() throws Exception {
        int previousWins=storage.getInt("stage_wins_5",0);
        selectStage(5);
        ui(() -> {
            require((Boolean)read(read(physics,"stage"),"fireRain"),"last stage enables falling fire");
            write(physics,"x",240f); write(physics,"y",1000f); write(physics,"vy",0f); write(physics,"cameraY",570f);
            Object fire=((Object[])read(physics,"fireHazards"))[0];
            write(fire,"active",true); write(fire,"warningTime",.5f); write(fire,"x",240f); write(fire,"y",725f);
            arena.invalidate();
        });
        capture("last-stage-fire-warning-controlled"); runFrames(120);
        require(!physicsBool("lost"),"warning telegraph is safe before fire falls");
        final float[] fireStart={0};
        ui(() -> {
            Object fire=((Object[])read(physics,"fireHazards"))[0];
            require(((Number)read(fire,"warningTime")).floatValue()>0,"warning countdown remains visible");
            fireStart[0]=((Number)read(physics,"y")).floatValue()-100;
            write(fire,"warningTime",0f); write(fire,"y",fireStart[0]); write(fire,"speed",600f); arena.invalidate();
        });
        runFrames(50);
        ui(() -> require(((Number)read(((Object[])read(physics,"fireHazards"))[0],"y")).floatValue()>fireStart[0],"active fire actually descends"));
        require(!physicsBool("lost"),"visible falling fire starts clear of avatar"); capture("last-stage-fire-falling-controlled");
        ui(() -> {
            Object fire=((Object[])read(physics,"fireHazards"))[0];
            write(fire,"x",read(physics,"x")); write(fire,"y",read(physics,"y")); write(fire,"speed",0f);
            write(fire,"warningTime",0f); arena.invalidate();
        });
        runUntilPhysics("lost",true,1500); runFrames(160); capture("last-stage-fire-death-effect-controlled");
        runFrames(1240); requireScreen(PLAY,"fire dissolves before the result overlay"); capture("last-stage-fire-death-ash-controlled");
        waitScreen(RESULT,3500);
        require(physicsBool("lost")&&!physicsBool("won"),"falling fire overlap kills avatar");
        ui(() -> require("FALLING FIRE".equals(read(physics,"deathReason")),"falling fire uses its specific death reason"));
        require(storage.getInt("stage_wins_5",0)==previousWins,"fire death cannot add a victory");
        capture("last-stage-fire-death-controlled"); click("home");
    }

    private void fatalFloor() throws Exception {
        selectStage(3);
        ui(() -> {
            float ground=((Number)call(physics,"groundY")).floatValue(), radius=((Number)call(physics,"radius")).floatValue();
            write(physics,"x",290f); write(physics,"y",ground-radius-4f); write(physics,"vy",120f);
            write(physics,"vx",0f); write(physics,"cameraY",ground-660f); arena.invalidate();
        });
        runUntilPhysics("lost",true,1500);
        ui(() -> require("HARD LANDING".equals(read(physics,"deathReason")),"late-stage off-target floor landing is fatal"));
        require(!physicsBool("won"),"hard landing cannot win"); runFrames(160); capture("hard-floor-death-effect-controlled");
        waitScreen(RESULT,3500); capture("hard-floor-result-controlled");
        require(storage.getInt("stage_wins_3",0)==1,"hard landing leaves campaign victory count unchanged"); click("home");
    }

    private void arcadeSetupAndPersistence() throws Exception {
        click("arcade"); requireScreen(ARCADE_SETUP,"Arcade opens its setup screen"); freeze("Arcade setup freezes gameplay");
        for (int difficulty=0;difficulty<3;difficulty++) {
            click("difficulty_"+difficulty);
            require(storage.getInt("arcadeDifficulty",-1)==difficulty,"Arcade difficulty "+difficulty+" saved by touch");
            capture("arcade-setup-difficulty-"+difficulty);
            recreate(); requireScreen(HOME,"Arcade setup recreation returns to home"); click("arcade");
            require(storage.getInt("arcadeDifficulty",-1)==difficulty,"Arcade difficulty survives Activity recreation");
        }
        click("arcade_fire"); click("arcade_top"); click("arcade_side"); click("arcade_lightning");
        require(storage.getInt("arcadeHazards",-1)==16,"four hazard toggles leave only buzzsaws enabled");
        click("arcade_saw"); require(storage.getInt("arcadeHazards",-1)==16,"last enabled saw hazard cannot be disabled");
        capture("arcade-last-hazard-protected");
        click("arcade_fire"); click("arcade_top"); click("arcade_side"); click("arcade_lightning");
        require(storage.getInt("arcadeHazards",-1)==31,"all five hazard groups can be re-enabled");
        recreate(); click("arcade"); require(storage.getInt("arcadeHazards",-1)==31,"hazard configuration survives recreation");
        back(); requireScreen(HOME,"Android Back leaves Arcade setup");
    }

    private void arcadeSurvivalAndRecords() throws Exception {
        Map<String,Object> campaign=campaignRecords();
        startArcade(1,31); require(physicsBool("arcade"),"Start Arcade uses survival physics");
        runFrames(650); require(!physicsBool("won")&&!physicsBool("lost"),"Arcade run remains live after gravity frames");
        ui(() -> {
            write(physics,"x",240f); write(physics,"y",10000f); write(physics,"vy",0f); write(physics,"vx",0f); write(physics,"cameraY",9570f);
            // A staged score above all campaign fixture records makes record isolation observable.
            write(physics,"elapsed",40f);
        });
        runFrames(100); require(physicsFloat("y")>10000f&&!physicsBool("won")&&!physicsBool("lost"),"Arcade continues below every campaign floor");
        capture("arcade-survival-controlled");
        click("pause"); freeze("Arcade pause freezes survival clock"); click("settings"); requireScreen(SETTINGS,"Arcade pause opens settings");
        freeze("Arcade settings freeze survival clock"); back(); requireScreen(PAUSE,"Arcade settings return to pause"); back(); requireScreen(PLAY,"Back resumes Arcade");
        stageBeam(2,0f); runUntilPhysics("lost",true,1500); runFrames(160); capture("arcade-first-death-effect-controlled");
        waitScreen(RESULT,3500); capture("arcade-result-controlled");
        require(storage.getInt("arcade_runs_1",0)==1&&storage.getInt("arcade_best_1",0)>=400&&storage.getFloat("arcade_longest_1",0)>=40f,"Arcade writes its separate score, time and run record");
        require(campaign.equals(campaignRecords()),"Arcade results do not alter campaign records");
        freeze("Arcade result freezes physics"); require(storage.getInt("arcade_runs_1",0)==1,"Arcade result redraw records the run once");
        click("restart"); requireScreen(PLAY,"Arcade Replay starts a new survival run");
        require(physicsBool("arcade")&&!physicsBool("lost"),"Arcade Replay retains its mode and clears death");
        ui(() -> require(((Number)read(physics,"arcadeDifficulty")).intValue()==1&&((Number)read(physics,"arcadeHazards")).intValue()==31,"Arcade Replay retains difficulty and hazard configuration"));
        goHome(); recreate();
        require(storage.getInt("arcade_runs_1",0)==1,"Arcade record survives Activity recreation");
        require(campaign.equals(campaignRecords()),"Arcade recreation preserves campaign records");
    }

    private void arcadeBeamHazards() throws Exception {
        threeBeamVisuals();
        beamSweepFixture();
        require(storage.getInt("arcade_runs_0",0)==0&&storage.getInt("arcade_runs_2",0)==0,"Standard Arcade records do not contaminate other difficulties");
        click("play"); requireScreen(PLAY,"campaign Play remains available after Arcade");
        require(!physicsBool("arcade")&&stageIndex()==storage.getInt("selectedStage",-1),"home Campaign restores the selected chapter rather than Arcade mode");
        goHome();
    }

    private void threeBeamVisuals() throws Exception {
        int[] kinds={2,4,8}; String[] reasons={"TOP LASER","SIDE LASER","LIGHTNING"};
        for (int i=0;i<kinds.length;i++) {
            int kind=kinds[i]; final String reason=reasons[i]; String name=kind==2?"top-laser":kind==4?"side-laser":"lightning";
            startArcade(1,kind); stageBeam(kind,.5f); capture("arcade-"+name+"-warning-controlled");
            runFrames(100); require(!physicsBool("lost"),reason+" warning is nonlethal");
            stageBeam(kind,0f); runUntilPhysics("lost",true,1500);
            ui(() -> require(reason.equals(read(physics,"deathReason")),"distinct Arcade collision reason: "+reason));
            runFrames(160); capture("arcade-"+name+"-death-effect-controlled");
            if (kind==8) { runFrames(1240); requireScreen(PLAY,"lightning disintegrates before the result overlay"); capture("arcade-lightning-death-ash-controlled"); }
            waitScreen(RESULT,3500); capture("arcade-"+name+"-result-controlled"); click("home");
        }
    }

    private void stageBeam(int kind,float warning) throws Exception {
        ui(() -> {
            Object beam=((Object[])read(physics,"beamHazards"))[0]; float x=((Number)read(physics,"x")).floatValue();
            float y=((Number)read(physics,"y")).floatValue(),camera=((Number)read(physics,"cameraY")).floatValue();
            write(beam,"active",true); write(beam,"followCamera",false); write(beam,"kind",kind); write(beam,"warningTime",warning); write(beam,"liveTime",1f); write(beam,"width",kind==8?14f:8f);
            float left=((Number)call(physics,"leftWall")).floatValue()+((Number)call(physics,"hazardInset")).floatValue();
            float right=((Number)call(physics,"rightWall")).floatValue()-((Number)call(physics,"hazardInset")).floatValue();
            if (kind==4) {
                float length=(right-left)*.56f;
                write(beam,"x1",left); write(beam,"y1",y); write(beam,"x2",left+length); write(beam,"y2",y);
                if (warning>0) write(physics,"x",left+length*.65f);
            } else if (kind==8) {
                float x1=left+8f,x2=right-8f,y1=camera+180f,y2=camera+710f;
                write(beam,"x1",x1); write(beam,"y1",y1); write(beam,"x2",x2); write(beam,"y2",y2);
                if (warning>0) write(physics,"x",x1+(x2-x1)*(y-y1)/(y2-y1));
            } else { write(beam,"x1",x); write(beam,"y1",camera+170f); write(beam,"x2",x); write(beam,"y2",camera+700f); }
            arena.invalidate();
        });
    }

    private void beamSweepFixture() throws Exception {
        startArcade(1,2);
        ui(() -> {
            // A tiny grazing pass is clear at BOTH endpoints but intersects a beam's round tip
            // between them. Endpoint-only collision checks miss this at normal capped speed.
            float dt=((Number)read(physics.getClass(),"DT")).floatValue();
            float reach=((Number)call(physics,"radius")).floatValue()+2f;
            float dx=950f*.9995f*dt, startX=240f-dx/2f, bodyY=350f, tipY=bodyY-reach+.005f;
            Object beam=((Object[])read(physics,"beamHazards"))[0];
            write(beam,"active",true); write(beam,"followCamera",false); write(beam,"kind",2); write(beam,"warningTime",0f); write(beam,"liveTime",1f); write(beam,"width",4f);
            write(beam,"x1",240f); write(beam,"y1",200f); write(beam,"x2",240f); write(beam,"y2",tipY);
            write(physics,"x",startX); write(physics,"y",bodyY); write(physics,"vx",950f); write(physics,"vy",-900f*dt); write(physics,"gx",0f);
            require(Math.hypot(startX-240f,bodyY-tipY)>reach,"swept beam fixture begins outside its collision radius");
            boolean original=(Boolean)read(arena,"motionEnabled");
            try {
                // Route one actual Arena frame through its normal collision/death/record path.
                write(arena,"motionEnabled",true); write(arena,"accumulator",0f);
                call(arena,"advance",dt,SystemClock.uptimeMillis());
            } finally { write(arena,"motionEnabled",original); }
            float endX=((Number)read(physics,"x")).floatValue(), endY=((Number)read(physics,"y")).floatValue();
            require(Math.hypot(endX-240f,endY-tipY)>reach,"swept beam fixture ends outside its collision radius");
            require((Boolean)read(physics,"lost")&&"TOP LASER".equals(read(physics,"deathReason")),"swept collision catches the between-endpoints grazing contact");
            arena.invalidate();
        });
        runFrames(160); capture("arcade-swept-beam-death-effect-controlled"); waitScreen(RESULT,3500); click("home");
    }

    private void storeShot(JSONObject sources,String name,String description) throws Exception {
        require(!physicsBool("lost"),"store capture keeps avatar alive: "+name); capture(name);
        final int[] crop=new int[4];
        ui(() -> {
            int[] location=new int[2]; arena.getLocationOnScreen(location);
            float scale=((Number)read(arena,"scale")).floatValue(),ox=((Number)read(arena,"ox")).floatValue(),oy=((Number)read(arena,"oy")).floatValue();
            crop[0]=(int)Math.floor(location[0]+ox); crop[1]=(int)Math.floor(location[1]+oy);
            crop[2]=(int)Math.ceil(location[0]+ox+480*scale); crop[3]=(int)Math.ceil(location[1]+oy+820*scale);
        });
        require(crop[0]>=0&&crop[1]>=0&&crop[2]<=1080&&crop[3]<=1920&&crop[2]>crop[0]&&crop[3]>crop[1],"measured native Canvas crop excludes system bars: "+name);
        JSONObject value=new JSONObject(); value.put("path","build/store-captures/"+name+".png");
        value.put("app_crop",new JSONArray(crop)); value.put("controlled",true); value.put("source_version","3.2.0"); value.put("description",description); sources.put(name,value);
    }
    private void storeCaptures() throws Exception {
        String version=getTargetContext().getPackageManager().getPackageInfo(APP,0).versionName;
        require("3.2.0".equals(version),"store captures come from installed version 3.2.0");
        Bitmap size=getUiAutomation().takeScreenshot(); require(size!=null&&size.getWidth()==1080&&size.getHeight()==1920,"store device compositor is 1080 by 1920"); size.recycle();
        JSONObject sources=new JSONObject(); selectStage(0);
        ui(() -> { write(physics,"x",240f); write(physics,"y",430f); write(physics,"vy",100f); arena.invalidate(); });
        runFrames(70); require(!physicsBool("lost")&&!physicsBool("won"),"campaign rope reaction is live gameplay");
        storeShot(sources,"campaign","Version 3.2.0 release-derived APK: living avatar reacting to a bouncy rope in campaign stage 1; controlled position followed by actual physics frames.");
        stageTargetFixture(); runUntilPhysics("won",true,1500);
        require(physicsFloat("vy")<0&&physicsFloat("groundBounce")>0,"store target scene shows actual successful bounce");
        storeShot(sources,"landing","Version 3.2.0 release-derived APK: controlled target approach resolved by actual collision into a winning bounce, before the result overlay.");
        startArcade(1,8); stageBeam(8,.7f); runFrames(65);
        storeShot(sources,"lightning","Version 3.2.0 release-derived APK: living avatar with a diagonal lightning telegraph in Arcade; controlled warning fixture after actual frames.");
        startArcade(1,16); stageSaw(.7f); runFrames(65);
        storeShot(sources,"saw","Version 3.2.0 release-derived APK: living avatar with the advertised sideways buzzsaw warning path; controlled warning fixture after actual frames.");
        startArcade(1,31); goHome(); click("arcade"); runFrames(65);
        storeShot(sources,"arcade","Version 3.2.0 release-derived APK: actual Arcade setup with Standard difficulty and all five hazard families enabled.");
        back(); click("settings"); require(bool("hasMotion"),"store emulator exposes motion hardware");
        if (!storage.getBoolean("motion",false)) click("motion"); click("preset_1"); runFrames(65);
        require(storage.getBoolean("motion",false),"store motion settings visibly enabled through touch");
        storeShot(sources,"settings","Version 3.2.0 release-derived APK: actual settings with motion steering enabled and Balanced response selected through touch.");
        try (FileOutputStream stream=new FileOutputStream(new File(output,"sources.json"))) { stream.write(sources.toString(2).getBytes("UTF-8")); }
        back(); requireScreen(HOME,"store fixtures return safely Home");
    }

    private void legacyMaskMigration() throws Exception {
        goHome(); close();
        Map<String,Object> saved=new HashMap<>(storage.getAll());
        require(storage.edit().putInt("arcadeHazards",15).commit(),"legacy four-family mask fixture stored");
        launch(false); waitScreen(HOME,4000); click("arcade");
        requireScreen(ARCADE_SETUP,"legacy profile opens five-family setup");
        require(storage.getInt("arcadeHazards",-1)==15,"saved legacy mask 15 preserves original choices");
        ui(() -> {
            require(((Number)read(read(arena,"profile"),"arcadeHazards")).intValue()==15,"loaded legacy mask does not silently enable saws");
            require(button("arcade_saw")!=null,"new saw toggle available for legacy profile");
        });
        Map<String,Object> actual=new HashMap<>(storage.getAll()), expected=new HashMap<>(saved);
        expected.put("arcadeHazards",15);
        require(actual.equals(expected),"legacy migration preserves every unrelated saved choice and record");
        capture("legacy-mask-15-preserved");
        close(); copy(storage,saved); launch(false); waitScreen(HOME,4000);
        require(storage.getAll().equals(saved),"legacy fixture restores current test profile exactly");
    }

    private void fifthHazardGuard() throws Exception {
        click("arcade"); requireScreen(ARCADE_SETUP,"five-family setup opens by pointer");
        String[] ids={"arcade_fire","arcade_top","arcade_side","arcade_lightning"};
        if ((storage.getInt("arcadeHazards",31)&16)==0) click("arcade_saw");
        for (int i=0;i<4;i++) if ((storage.getInt("arcadeHazards",31)&(1<<i))!=0) click(ids[i]);
        require(storage.getInt("arcadeHazards",-1)==16,"saw-only Arcade configuration is allowed");
        click("arcade_saw"); require(storage.getInt("arcadeHazards",-1)==16,"last enabled saw toggle cannot disable all hazards");
        capture("arcade-saw-only-protected");
        for (String id:ids) click(id);
        require(storage.getInt("arcadeHazards",-1)==31,"all five Arcade families can be enabled together");
        capture("arcade-five-hazards"); back();
    }

    private void stageSaw(float warning) throws Exception {
        ui(() -> {
            write(physics,"x",240f); write(physics,"y",310f); write(physics,"vy",0f); write(physics,"cameraY",0f);
            Object saw=((Object[])read(physics,"sawHazards"))[0];
            write(saw,"active",true); write(saw,"followCamera",false); write(saw,"warningTime",warning);
            write(saw,"x",90f); write(saw,"previousX",90f); write(saw,"y",430f); write(saw,"previousY",430f);
            write(saw,"pathX1",90f); write(saw,"pathX2",340f); write(saw,"velocityX",520f); write(saw,"radius",16f); write(saw,"liveTime",2f);
            arena.invalidate();
        });
    }

    private void sawVisuals() throws Exception {
        Map<String,Object> campaign=campaignRecords(); int runs=storage.getInt("arcade_runs_1",0);
        startArcade(1,16); stageSaw(.5f);
        // Present a real warning frame before the compositor screenshot; an
        // invalidate on a deliberately frozen fixture can leave the old frame visible.
        runFrames(40);
        require(numeric(((Object[])read(physics,"sawHazards"))[0],"warningTime")>0,"saw remains in warning phase for its screenshot");
        capture("arcade-saw-warning-controlled");
        runFrames(100); require(!physicsBool("lost"),"saw warning is safe while its advertised path is visible");
        final Object[] saw={null}; ui(() -> { saw[0]=((Object[])read(physics,"sawHazards"))[0]; write(saw[0],"warningTime",0f); });
        float x=numeric(saw[0],"x"); runFrames(60);
        require(numeric(saw[0],"x")>x&&!physicsBool("lost"),"live saw travels along its announced path");
        capture("arcade-saw-active-controlled"); x=numeric(saw[0],"x"); float clock=numeric(arena,"visualTime"); runFrames(80);
        require(numeric(saw[0],"x")>x&&numeric(arena,"visualTime")>clock,"second live frame advances saw travel and its spin clock");
        capture("arcade-saw-spinning-controlled");
        ui(() -> {
            float px=((Number)read(physics,"x")).floatValue(),py=((Number)read(physics,"y")).floatValue();
            write(saw[0],"x",px); write(saw[0],"previousX",px); write(saw[0],"y",py); write(saw[0],"previousY",py); write(saw[0],"velocityX",0f);
        });
        runUntilPhysics("lost",true,1000);
        ui(() -> require("BUZZSAW".equals(read(physics,"deathReason")),"saw contact uses BUZZSAW collision reason"));
        runFrames(160); capture("arcade-saw-impact-controlled"); waitScreen(RESULT,3500); capture("arcade-saw-result-controlled");
        require(storage.getInt("arcade_runs_1",0)==runs+1,"saw loss records exactly one Arcade run");
        require(campaignRecords().equals(campaign),"saw Arcade loss preserves campaign records");
        click("home");
    }

    private Object audio() throws Exception { final Object[] value={null}; ui(() -> value[0]=read(activity,"audio")); return value[0]; }
    private int audioCount(Object audio,String method) throws Exception { final int[] value={0}; ui(() -> value[0]=((Number)call(audio,method)).intValue()); return value[0]; }
    private boolean audioFlag(Object audio,String method) throws Exception { final boolean[] value={false}; ui(() -> value[0]=(Boolean)call(audio,method)); return value[0]; }
    private void awaitAudio(Object audio) throws Exception {
        long deadline=SystemClock.uptimeMillis()+7000;
        while (audioCount(audio,"loadedCount")<11&&SystemClock.uptimeMillis()<deadline) settle(30);
        require(audioCount(audio,"loadedCount")==11,"all 11 native SoundPool samples completed asynchronous loading");
    }
    private void actualPhysicsFrame() throws Exception {
        ui(() -> {
            float dt=((Number)read(physics.getClass(),"DT")).floatValue();
            write(arena,"active",true); write(arena,"accumulator",0f);
            try { call(arena,"advance",dt,SystemClock.uptimeMillis()); }
            finally { write(arena,"active",false); }
            arena.invalidate();
        });
    }
    private void soundSetting(boolean enabled) throws Exception {
        if (screen()==PLAY) click("pause");
        click("settings"); requireScreen(SETTINGS,"sound controls open through actual navigation");
        if (storage.getBoolean("sound",false)!=enabled) click("sound");
        require(storage.getBoolean("sound",!enabled)==enabled,"sound setting is "+enabled);
    }
    private void audioLifecycle() throws Exception {
        goHome(); soundSetting(true); Object audio=audio(); awaitAudio(audio);
        require(audioFlag(audio,"isEnabled")&&audioFlag(audio,"isForeground"),"native audio enabled only in foreground");
        capture("settings-sound-on"); back(); startArcade(1,16); stageSaw(0f);
        int before=audioCount(audio,"playedCount"); actualPhysicsFrame();
        require(physicsFloat("soundEvent")==5&&physicsFloat("soundSequence")>0,"actual saw activation produces its physics motor cue");
        require(audioCount(audio,"playedCount")==before+1,"actual physics cue starts exactly one native SoundPool stream ("+before+" -> "+audioCount(audio,"playedCount")+")");
        actualPhysicsFrame(); actualPhysicsFrame();
        require(audioCount(audio,"playedCount")==before+1,"subsequent physics frames do not duplicate the consumed cue");
        soundSetting(false); capture("settings-muted-audio"); back(); click("resume"); click("restart"); stageSaw(0f);
        before=audioCount(audio,"playedCount"); actualPhysicsFrame();
        require(physicsFloat("soundEvent")==5,"muted live physics still emits the source event");
        require(audioCount(audio,"playedCount")==before,"sound off suppresses native hazard playback");
        soundSetting(true); back(); click("resume"); before=audioCount(audio,"playedCount"); actualPhysicsFrame();
        require(audioCount(audio,"playedCount")==before,"unmuting does not replay an already consumed physics cue");
        click("pause"); before=audioCount(audio,"playedCount");
        ui(() -> { write(arena,"active",true); try { call(activity,"hazardEffect",5); } finally { write(arena,"active",false); } });
        require(audioCount(audio,"playedCount")==before,"pause screen suppresses hazard audio even if active flag is set");
        click("home"); before=audioCount(audio,"playedCount");
        ui(() -> { write(arena,"active",true); try { call(activity,"hazardEffect",5); } finally { write(arena,"active",false); } });
        require(audioCount(audio,"playedCount")==before,"home screen suppresses hazard audio even if active flag is set");
        startArcade(1,16); before=audioCount(audio,"playedCount");
        final Activity foregroundActivity=activity;
        sendKeyDownUpSync(KeyEvent.KEYCODE_HOME); settle(150);
        long deadline=SystemClock.uptimeMillis()+5000;
        while (audioFlag(audio,"isForeground")&&SystemClock.uptimeMillis()<deadline) settle(30);
        require(!audioFlag(audio,"isForeground")&&!audioFlag(audio,"isReleased"),"actual Android background stops streams without destroying the reusable pool");
        final Object backgroundAudio=audio;
        ui(() -> { call(foregroundActivity,"hazardEffect",5); call(backgroundAudio,"hazard",5); });
        require(audioCount(audio,"playedCount")==before,"background owner and SoundPool guards suppress playback");
        ui(() -> getTargetContext().startActivity(new Intent().setClassName(APP,ACTIVITY).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)));
        deadline=SystemClock.uptimeMillis()+5000;
        while ((!audioFlag(audio,"isForeground")||!activity.hasWindowFocus())&&SystemClock.uptimeMillis()<deadline) SystemClock.sleep(25);
        animation(false); require(audioFlag(audio,"isForeground")&&audioFlag(audio,"isEnabled"),"foreground resume re-enables the retained audio pool");
        requireScreen(PAUSE,"backgrounding live gameplay resumes safely paused");
        require(audioCount(audio,"playedCount")==before,"foreground resume does not replay stale hazard audio");
        goHome(); final Object oldAudio=audio; recreate(); audio=audio();
        require(audio!=oldAudio&&audioFlag(oldAudio,"isReleased")&&audioCount(oldAudio,"loadedCount")==0,"destroyed Activity releases its SoundPool and sample handles");
        before=audioCount(oldAudio,"playedCount"); ui(() -> call(oldAudio,"hazard",5));
        require(audioCount(oldAudio,"playedCount")==before,"released SoundPool cannot play a stale event");
        awaitAudio(audio); require(audioFlag(audio,"isForeground")&&audioFlag(audio,"isEnabled"),"recreated Activity owns a fresh foreground audio pool");
        require(audioCount(audio,"playedCount")==0,"fresh Activity does not replay old hazard events");
        soundSetting(false); capture("settings-muted-after-recreation"); back();
        requireScreen(HOME,"audio lifecycle checks return to home");
    }

    private Map<String,Object> campaignRecords() {
        Map<String,Object> values=new HashMap<>();
        for (Map.Entry<String,?> entry:storage.getAll().entrySet()) if (entry.getKey().startsWith("stage_")
                || entry.getKey().equals("bestScore")||entry.getKey().equals("wins")||entry.getKey().equals("fastestWin")) values.put(entry.getKey(),entry.getValue());
        return values;
    }
    private void startArcade(int difficulty,int hazards) throws Exception {
        if (screen()!=HOME) goHome(); click("arcade"); requireScreen(ARCADE_SETUP,"Arcade setup opened for configured run"); click("difficulty_"+difficulty);
        int[] bits={1,2,4,8,16}; String[] ids={"arcade_fire","arcade_top","arcade_side","arcade_lightning","arcade_saw"};
        for (int i=0;i<bits.length;i++) if ((hazards&bits[i])!=0&&(storage.getInt("arcadeHazards",31)&bits[i])==0) click(ids[i]);
        for (int i=0;i<bits.length;i++) if ((hazards&bits[i])==0&&(storage.getInt("arcadeHazards",31)&bits[i])!=0) click(ids[i]);
        require(storage.getInt("arcadeHazards",-1)==hazards,"requested Arcade hazard mask saved: "+hazards);
        click("start_arcade"); requireScreen(PLAY,"Start Arcade begins gameplay");
    }

    private void goHome() throws Exception {
        if (screen()==PLAY) click("pause");
        if (screen()==PAUSE||screen()==RESULT) click("home");
        else if (screen()==ARCADE_SETUP||screen()==CHAPTERS||screen()==HELP) back();
        requireScreen(HOME,"return to home");
    }
    private void selectStage(int index) throws Exception {
        fixtureUnlockThrough(index);
        if (screen()!=HOME) goHome(); click("chapters"); requireScreen(CHAPTERS,"chapter menu opens by touch");
        final int[] page={0};
        ui(() -> { try { page[0]=((Number)read(arena,"chapterPage")).intValue(); } catch (NoSuchFieldException olderPrototype) { page[0]=0; } });
        while (page[0]<index/6) { click("next_chapters"); page[0]++; }
        while (page[0]>index/6) { click("prev_chapters"); page[0]--; }
        click("stage_"+index); requireScreen(PLAY,"stage card launches "+(index+1)); require(stageIndex()==index,"stage selection matches card "+index);
    }
    private void fixtureUnlockThrough(int index) throws Exception {
        if (index==0) return;
        ui(() -> {
            Object profile=read(arena,"profile"); boolean unlocked;
            try { unlocked=(Boolean)call(profile,"isStageUnlocked",index); } catch (NoSuchMethodException legacyProfile) { return; }
            if (unlocked) return;
            require(!mode.startsWith("revision"),"revision lock tests require actual completion instead of fixture grants");
            int[] wins=(int[])read(profile,"stageWins");
            for (int i=0;i<index;i++) wins[i]=Math.max(1,wins[i]);
            call(arena,"saveProfile"); log.append("FIXTURE prerequisite wins explicitly granted before selecting stage ").append(index+1).append('\n'); arena.invalidate();
        }); settle(60);
    }
    private int stageIndex() throws Exception { final int[] index={0}; ui(() -> index[0]=((Number)read(read(physics,"stage"),"index")).intValue()); return index[0]; }
    private float touchVelocity() throws Exception { final float[] value={0}; ui(() -> value[0]=((Number)call(read(arena,"touch"),"velocity")).floatValue()); return value[0]; }
    private Object button(String id) throws Exception {
        for (Object hit:(List<?>)read(arena,"buttons")) if (id.equals(read(hit,"id"))) return hit;
        throw new AssertionError("Missing actual rendered button "+id+" on screen "+read(arena,"screen"));
    }
    private void click(String id) throws Exception {
        float[] center=new float[2]; ui(() -> {
            Object hit=button(id); center[0]=(((Number)read(hit,"left")).floatValue()+((Number)read(hit,"right")).floatValue())/2;
            center[1]=(((Number)read(hit,"top")).floatValue()+((Number)read(hit,"bottom")).floatValue())/2;
        }); tap(center[0],center[1]);
    }
    private void requireButtonsAbsent(String... ids) throws Exception {
        ui(() -> { for (String id:ids) {
            boolean found=false; for (Object hit:(List<?>)read(arena,"buttons")) if (id.equals(read(hit,"id"))) found=true;
            require(!found,"removed control absent: "+id);
        } });
    }

    private void launch() throws Exception { launch(true); }
    private void launch(boolean prepareArtifacts) throws Exception {
        // Stop the intro clock immediately after actual onResume, before slow emulator idle waits
        // can consume the entire splash. Native frames then verify the real timed transition.
        final Throwable[] earlyFailure={null}; Application app=(Application)getTargetContext().getApplicationContext();
        final boolean starting=(mode.equals("features")||mode.equals("splash"))&&prepareArtifacts;
        final CountDownLatch startingEntered=new CountDownLatch(1), releaseStarting=new CountDownLatch(1);
        final Bitmap[] startingShot={null};
        final Activity[] resumedActivity={null};
        Application.ActivityLifecycleCallbacks callback=new Application.ActivityLifecycleCallbacks() {
            @Override public void onActivityPreCreated(Activity a,Bundle b) {
                if (starting&&a.getClass().getName().equals(ACTIVITY)) {
                    startingEntered.countDown();
                    try { releaseStarting.await(2000,TimeUnit.MILLISECONDS); } catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); }
                }
            }
            @Override public void onActivityCreated(Activity a,Bundle b) { }
            @Override public void onActivityStarted(Activity a) { }
            @Override public void onActivityResumed(Activity a) { }
            @Override public void onActivityPostResumed(Activity a) {
                if (a.getClass().getName().equals(ACTIVITY)) try {
                    Object view=read(a,"arena"); write(view,"active",false); ((View)view).invalidate();
                    resumedActivity[0]=a;
                } catch (Throwable error) { earlyFailure[0]=error; }
            }
            @Override public void onActivityPaused(Activity a) { }
            @Override public void onActivityStopped(Activity a) { }
            @Override public void onActivitySaveInstanceState(Activity a,Bundle b) { }
            @Override public void onActivityDestroyed(Activity a) { }
        };
        ui(() -> app.registerActivityLifecycleCallbacks(callback));
        try {
            if (starting) {
                getUiAutomation();
                getTargetContext().startActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setClassName(APP,ACTIVITY).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));
                try {
                    require(startingEntered.await(5000,TimeUnit.MILLISECONDS),"actual Activity enters bounded system-splash capture window");
                    SystemClock.sleep(750); startingShot[0]=getUiAutomation().takeScreenshot();
                } finally { releaseStarting.countDown(); }
                long resumedDeadline=SystemClock.uptimeMillis()+5000;
                while (resumedActivity[0]==null&&earlyFailure[0]==null&&SystemClock.uptimeMillis()<resumedDeadline) SystemClock.sleep(20);
                require(resumedActivity[0]!=null,"startup capture releases Activity creation promptly"); activity=resumedActivity[0];
            } else activity=startActivitySync(new Intent().setClassName(APP,ACTIVITY).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));
        }
        finally { ui(() -> app.unregisterActivityLifecycleCallbacks(callback)); }
        if (earlyFailure[0]!=null) throw new Exception("initial intro clock fixture",earlyFailure[0]);
        bind(); if (!prepareArtifacts) return;
        output=new File(activity.getExternalFilesDir(null),(mode.equals("privacy")?"falley-native-privacy/":mode.startsWith("revision")?"falley-native-revision/":mode.equals("splash")?"falley-native-splash/":mode.equals("features")?"falley-native-features/":mode.equals("motion")?"falley-native-motion/":mode.equals("progression")?"falley-native-progression/":"falley-native-v32/")+shot);
        require(output.isDirectory()||output.mkdirs(),"artifact directory ready");
        // This test-owned directory must describe only this run, not retain an old failure PNG.
        File[] prior=output.listFiles();
        if (prior!=null) for (File file:prior) if (file.isFile() && (file.getName().endsWith(".png") || file.getName().equals("checks.txt")))
            require(file.delete(),"prior test artifact cleared "+file.getName());
        if (startingShot[0]!=null) {
            try (FileOutputStream stream=new FileOutputStream(new File(output,"system-splash.png"))) {
                require(startingShot[0].compress(Bitmap.CompressFormat.PNG,100,stream),"native system starting-window screenshot saved"); captures++;
            } finally { startingShot[0].recycle(); }
        }
    }
    private void bind() throws Exception {
        long deadline=SystemClock.uptimeMillis()+5000;
        do {
            ui(() -> { arena=(View)read(activity,"arena"); physics=read(arena,"physics"); });
            if (arena.getWidth()>0&&arena.getHeight()>0) break;
            SystemClock.sleep(20);
        } while (SystemClock.uptimeMillis()<deadline);
        require(arena.getWidth()>0&&arena.getHeight()>0,"native Canvas view laid out");
        // A live animator can keep waitForIdleSync blocked long enough for a whole run to finish.
        // Freeze between input assertions; run real frames explicitly for time/collision checks.
        ui(() -> { write(arena,"active",false); arena.invalidate(); }); settle(70);
    }
    private void recreate() throws Exception {
        final Activity previous=activity; final Activity[] replacement={null};
        Application app=previous.getApplication();
        Application.ActivityLifecycleCallbacks callback=new Application.ActivityLifecycleCallbacks() {
            @Override public void onActivityCreated(Activity created,Bundle state) { }
            @Override public void onActivityStarted(Activity a) { }
            @Override public void onActivityResumed(Activity a) { if (a!=previous&&a.getClass().getName().equals(ACTIVITY)) replacement[0]=a; }
            @Override public void onActivityPaused(Activity a) { }
            @Override public void onActivityStopped(Activity a) { }
            @Override public void onActivitySaveInstanceState(Activity a,Bundle state) { }
            @Override public void onActivityDestroyed(Activity a) { }
        };
        ui(() -> { app.registerActivityLifecycleCallbacks(callback); previous.recreate(); });
        try {
            long deadline=SystemClock.uptimeMillis()+5000;
            while (replacement[0]==null&&SystemClock.uptimeMillis()<deadline) settle(25);
            require(replacement[0]!=null&&replacement[0]!=previous,"Android created a distinct Activity instance");
            activity=replacement[0]; bind();
        } finally { ui(() -> app.unregisterActivityLifecycleCallbacks(callback)); }
    }
    private void close() throws Exception {
        if (activity==null) return;
        ui(() -> activity.finish()); long deadline=SystemClock.uptimeMillis()+5000;
        while (!activity.isDestroyed()&&SystemClock.uptimeMillis()<deadline) settle(20);
        require(activity.isDestroyed(),"Activity finished before preferences restored"); activity=null;
    }
    private void restoreOriginal() {
        if (backup.getBoolean(BACKUP_MARKER,false)) {
            Map<String,Object> original=new HashMap<>(backup.getAll()); original.remove(BACKUP_MARKER);
            copy(storage,original); require(storage.getAll().equals(original),"original preferences restored exactly");
            require(backup.edit().clear().commit(),"verified backup cleared");
        }
    }
    @SuppressWarnings("unchecked") private void copy(SharedPreferences destination,Map<String,?> values) {
        SharedPreferences.Editor edit=destination.edit().clear();
        for (Map.Entry<String,?> entry:values.entrySet()) {
            Object value=entry.getValue(); String key=entry.getKey();
            if (value instanceof String) edit.putString(key,(String)value);
            else if (value instanceof Integer) edit.putInt(key,(Integer)value);
            else if (value instanceof Boolean) edit.putBoolean(key,(Boolean)value);
            else if (value instanceof Long) edit.putLong(key,(Long)value);
            else if (value instanceof Float) edit.putFloat(key,(Float)value);
            else if (value instanceof Set) edit.putStringSet(key,new HashSet<>((Set<String>)value));
            else throw new IllegalArgumentException("Unsupported preference "+key);
        }
        require(edit.commit(),"preferences committed");
    }
    private void capture(String name) throws Exception {
        settle(45); Bitmap bitmap=getUiAutomation().takeScreenshot();
        require(bitmap!=null,"native compositor screenshot "+name);
        try (FileOutputStream stream=new FileOutputStream(new File(output,name+".png"))) {
            require(bitmap.compress(Bitmap.CompressFormat.PNG,100,stream),"screenshot saved "+name);
        } finally { bitmap.recycle(); }
        captures++;
    }
    private void freeze(String message) throws Exception {
        float elapsed=physicsFloat("elapsed"),y=physicsFloat("y"); runFrames(260);
        require(elapsed==physicsFloat("elapsed")&&y==physicsFloat("y"),message);
    }
    private void animation(boolean enabled) throws Exception {
        ui(() -> { write(arena,"active",enabled); write(arena,"last",0L); write(arena,"accumulator",0f); arena.invalidate(); });
    }
    private void runFrames(long milliseconds) throws Exception {
        animation(true); SystemClock.sleep(milliseconds); animation(false); waitForIdleSync();
    }
    private void runUntilPhysics(String field,boolean expected,long timeout) throws Exception {
        animation(true); long deadline=SystemClock.uptimeMillis()+timeout; final boolean[] matched={false};
        do {
            SystemClock.sleep(12);
            ui(() -> { matched[0]=(Boolean)read(physics,field)==expected; if (matched[0]) write(arena,"active",false); });
        } while (!matched[0]&&SystemClock.uptimeMillis()<deadline);
        animation(false); require(matched[0],"native frame transition "+field+"="+expected);
    }
    private void requireScreen(int expected,String message) throws Exception {
        ui(() -> { int actual=((Number)read(arena,"screen")).intValue(); require(actual==expected,message+" (expected "+expected+", actual "+actual+")"); });
    }
    private void waitScreen(int expected,long timeout) throws Exception {
        animation(true); long deadline=SystemClock.uptimeMillis()+timeout; final boolean[] matched={false};
        do {
            SystemClock.sleep(15);
            ui(() -> { matched[0]=((Number)read(arena,"screen")).intValue()==expected; if (matched[0]) write(arena,"active",false); });
        } while (!matched[0]&&SystemClock.uptimeMillis()<deadline);
        animation(false);
        requireScreen(expected,"native screen transition "+expected);
    }
    private int screen() throws Exception { final int[] value={0}; ui(() -> value[0]=((Number)read(arena,"screen")).intValue()); return value[0]; }
    private float physicsFloat(String field) throws Exception { return numeric(physics,field); }
    private float physicsCallFloat(String method) throws Exception { final float[] value={0}; ui(() -> value[0]=((Number)call(physics,method)).floatValue()); return value[0]; }
    private float numeric(Object object,String field) throws Exception { final float[] value={0}; ui(() -> value[0]=((Number)read(object,field)).floatValue()); return value[0]; }
    private boolean physicsBool(String field) throws Exception { final boolean[] value={false}; ui(() -> value[0]=(Boolean)read(physics,field)); return value[0]; }
    private boolean bool(String field) throws Exception { final boolean[] value={false}; ui(() -> value[0]=(Boolean)read(arena,field)); return value[0]; }
    private void settle(long milliseconds) { SystemClock.sleep(milliseconds); waitForIdleSync(); }
    private void back() { sendKeyDownUpSync(KeyEvent.KEYCODE_BACK); settle(60); }
    private float[] coordinates(float x,float y) throws Exception {
        float[] physical=new float[2]; ui(() -> {
            int[] location=new int[2]; arena.getLocationOnScreen(location); float scale=((Number)read(arena,"scale")).floatValue();
            physical[0]=location[0]+((Number)read(arena,"ox")).floatValue()+x*scale;
            physical[1]=location[1]+((Number)read(arena,"oy")).floatValue()+y*scale;
            require(physical[0]>=0&&physical[0]<activity.getWindow().getDecorView().getWidth(),"tap x inside display");
        }); return physical;
    }
    private long touchDown;
    private void pointer(int action,float x,float y) throws Exception {
        float[] p=coordinates(x,y); long now=SystemClock.uptimeMillis();
        if (action==MotionEvent.ACTION_DOWN) touchDown=now;
        MotionEvent event=MotionEvent.obtain(touchDown,now,action,p[0],p[1],0); event.setSource(InputDevice.SOURCE_TOUCHSCREEN);
        try { sendPointerSync(event); } finally { event.recycle(); }
    }
    private void tap(float x,float y) throws Exception { pointer(MotionEvent.ACTION_DOWN,x,y); SystemClock.sleep(18); pointer(MotionEvent.ACTION_UP,x,y); settle(55); }
    private void dragHeld(float x,float y,float endX,float endY) throws Exception {
        pointer(MotionEvent.ACTION_DOWN,x,y); SystemClock.sleep(20); pointer(MotionEvent.ACTION_MOVE,endX,endY); settle(35);
    }
    private void release(float x,float y) throws Exception { pointer(MotionEvent.ACTION_UP,x,y); settle(35); }
    private void ui(Step step) throws Exception {
        Throwable[] failure={null}; runOnMainSync(() -> { try { step.run(); } catch (Throwable error) { failure[0]=error; } });
        if (failure[0]!=null) throw new Exception("Native Falley UI check",failure[0]);
    }
    private void require(boolean condition,String message) {
        if (!condition) throw new AssertionError(message); checks++; log.append("PASS ").append(message).append('\n');
    }
    private static Field field(Object object,String name) throws Exception {
        Class<?> type=object instanceof Class?(Class<?>)object:object.getClass();
        while (type!=null) { try { Field field=type.getDeclaredField(name); field.setAccessible(true); return field; } catch (NoSuchFieldException missing) { type=type.getSuperclass(); } }
        throw new NoSuchFieldException(name);
    }
    private static Object read(Object object,String name) throws Exception { return field(object,name).get(object instanceof Class?null:object); }
    private static void write(Object object,String name,Object value) throws Exception { field(object,name).set(object,value); }
    private static Object call(Object object,String name,Object... args) throws Exception {
        for (Method method:object.getClass().getDeclaredMethods()) if (method.getName().equals(name)&&method.getParameterTypes().length==args.length) {
            method.setAccessible(true);
            try { return method.invoke(object,args); } catch (InvocationTargetException error) { throw new Exception(name,error.getCause()); }
        }
        throw new NoSuchMethodException(name);
    }
}
