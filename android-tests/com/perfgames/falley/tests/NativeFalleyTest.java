package com.perfgames.falley.tests;

import android.app.Activity;
import android.app.Application;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Real Android pointer navigation; terminal landing is explicitly a controlled fixture. */
public final class NativeFalleyTest extends Instrumentation {
    private static final String APP = "com.perfgames.falley";
    private static final String ACTIVITY = "com.perfgames.ropelab.MainActivity";
    private static final String BACKUP_MARKER = "fixture-original-ready";
    private static final int HOME=0, PLAY=1, SETTINGS=2, HELP=3, PAUSE=4, RESULT=5, SPLASH=6, CHAPTERS=7;
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
        if ((!mode.equals("suite") && !mode.equals("restore")) || !shot.matches("[a-z0-9-]+"))
            throw new IllegalArgumentException("Falley test harness arguments");
        start();
    }

    @Override public void onStart() {
        storage=getTargetContext().getSharedPreferences("falley-v1",Context.MODE_PRIVATE);
        backup=getTargetContext().getSharedPreferences("falley-native-original",Context.MODE_PRIVATE);
        Throwable failure=null;
        try {
            if (mode.equals("suite")) {
                require(!backup.getBoolean(BACKUP_MARKER,false),"no interrupted fixture; recover with -RestoreOnly if needed");
                copy(backup,storage.getAll());
                require(backup.edit().putBoolean(BACKUP_MARKER,true).commit(),"original preferences backed up");
                require(storage.edit().clear().putInt("preset",1).putBoolean("motion",false)
                        .putBoolean("sound",false).putBoolean("haptics",false).commit(),"deterministic emulator settings installed");
                launch();
                homeAndHelp();
                settingsAndRecreation();
                chaptersAndPersistence();
                swipesPauseAndRestart();
                allStageLandings();
                chapterHazards();
                fireRain();
                capture("home-final");
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
        String summary=failure==null
            ? "PASS: native Falley "+mode+"; checks="+checks+"; captures="+captures+"; viewport="+shot
                +"; V2 actual touch swipes, chapter navigation and Activity recreation; controlled stage landing/hazard fixtures, not a natural phone playtest; original preferences restored\n"
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
        require(storage.getBoolean("sound",false),"sound setting saves by touch");
        require(storage.getBoolean("haptics",false),"haptics setting saves by touch");
        recreate();
        require(storage.getBoolean("sound",false)&&storage.getBoolean("haptics",false),"sound and haptics survive Activity recreation");
        require(!storage.getBoolean("motion",true),"touch-only setting survives Activity recreation");
        click("settings"); click("sound"); click("haptics"); click("back");
        requireScreen(HOME,"Done leaves home settings");
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
        require(Math.abs(physicsFloat("x")-releasedX)<1f,"release stops horizontal drift");
        ui(() -> { write(physics,"x",240f); write(physics,"y",350f); write(physics,"vy",120f); });
        dragHeld(240,390,185,390); runFrames(110);
        require(physicsFloat("x")<240f&&physicsFloat("vy")>0,"left swipe moves left without an automatic jump");
        pointer(MotionEvent.ACTION_CANCEL,185,390); settle(35); require(touchVelocity()==0,"cancel clears swipe velocity");
        releasedX=physicsFloat("x"); runFrames(100);
        require(Math.abs(physicsFloat("x")-releasedX)<1f,"cancel stops horizontal drift");
        ui(() -> { write(physics,"x",200f); write(physics,"y",447f); write(physics,"vy",120f); });
        runFrames(260); require(Math.abs(physicsFloat("x")-200f)<1f,"rope contact cannot push an idle touch-only avatar sideways");
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
        for (int stage=0;stage<6;stage++) {
            require(stageIndex()==stage,"next stage is "+(stage+1));
            if (stage==0||stage==2||stage==4) capture("chapter-"+(stage/2+1)+"-gameplay");
            stageTargetFixture(); runUntilPhysics("won",true,2000);
            require(physicsBool("won")&&!physicsBool("lost"),"stage "+(stage+1)+" target landing wins");
            require(physicsFloat("vy")<0&&physicsFloat("groundBounce")>0,"stage "+(stage+1)+" target physically bounces");
            if (stage==0||stage==5) capture("stage-"+(stage+1)+"-target-bounce-controlled");
            waitScreen(RESULT,2500); capture("stage-"+(stage+1)+"-victory-controlled");
            require(storage.getInt("wins",0)==stage+1&&storage.getInt("stage_wins_"+stage,0)==1,"stage "+(stage+1)+" records a single win");
            freeze("stage "+(stage+1)+" result physics frozen");
            require(storage.getInt("stage_wins_"+stage,0)==1,"stage result redraw cannot duplicate win");
            if (stage<5) { click("next_stage"); requireScreen(PLAY,"Next Stage starts gameplay"); }
            else requireButtonsAbsent("next_stage");
        }
        click("restart"); requireScreen(PLAY,"last stage Replay restarts"); require(!physicsBool("won"),"replay clears terminal victory");
        goHome(); recreate(); requireScreen(HOME,"records survive recreation"); require(stageIndex()==5,"last selected stage persists");
        ui(() -> {
            Object profile=read(arena,"profile"); int[] wins=(int[])read(profile,"stageWins"), best=(int[])read(profile,"best"); float[] times=(float[])read(profile,"stageTimes");
            for (int i=0;i<6;i++) require(wins[i]==1&&best[i]>=100&&times[i]>0,"persisted record for stage "+(i+1));
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
            waitScreen(RESULT,2500); require(physicsBool("lost")&&!physicsBool("won"),"chapter "+(chapter+1)+" hazard collision loses");
            ui(() -> require(reason.equals(read(physics,"deathReason")),"chapter hazard reason matches its visuals: "+reason));
            capture("chapter-"+(chapter+1)+"-hazard-death-controlled"); click("home");
        }
    }

    private void fireRain() throws Exception {
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
        waitScreen(RESULT,2500);
        require(physicsBool("lost")&&!physicsBool("won"),"falling fire overlap kills avatar");
        ui(() -> require("FALLING FIRE".equals(read(physics,"deathReason")),"falling fire uses its specific death reason"));
        require(storage.getInt("stage_wins_5",0)==1,"fire death cannot add a victory");
        capture("last-stage-fire-death-controlled"); click("home");
    }

    private void goHome() throws Exception {
        if (screen()==PLAY) click("pause");
        if (screen()==PAUSE||screen()==RESULT) click("home");
        requireScreen(HOME,"return to home");
    }
    private void selectStage(int index) throws Exception {
        if (screen()!=HOME) goHome(); click("chapters"); requireScreen(CHAPTERS,"chapter menu opens by touch");
        click("stage_"+index); requireScreen(PLAY,"stage card launches "+(index+1)); require(stageIndex()==index,"stage selection matches card "+index);
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

    private void launch() throws Exception {
        // Stop the intro clock immediately after actual onResume, before slow emulator idle waits
        // can consume the entire splash. Native frames then verify the real timed transition.
        final Throwable[] earlyFailure={null}; Application app=(Application)getTargetContext().getApplicationContext();
        Application.ActivityLifecycleCallbacks callback=new Application.ActivityLifecycleCallbacks() {
            @Override public void onActivityCreated(Activity a,Bundle b) { }
            @Override public void onActivityStarted(Activity a) { }
            @Override public void onActivityResumed(Activity a) {
                if (a.getClass().getName().equals(ACTIVITY)) try {
                    Object view=read(a,"arena"); write(view,"active",false); ((View)view).invalidate();
                } catch (Throwable error) { earlyFailure[0]=error; }
            }
            @Override public void onActivityPaused(Activity a) { }
            @Override public void onActivityStopped(Activity a) { }
            @Override public void onActivitySaveInstanceState(Activity a,Bundle b) { }
            @Override public void onActivityDestroyed(Activity a) { }
        };
        ui(() -> app.registerActivityLifecycleCallbacks(callback));
        try { activity=startActivitySync(new Intent().setClassName(APP,ACTIVITY).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK)); }
        finally { ui(() -> app.unregisterActivityLifecycleCallbacks(callback)); }
        if (earlyFailure[0]!=null) throw new Exception("initial intro clock fixture",earlyFailure[0]);
        bind(); output=new File(activity.getExternalFilesDir(null),"falley-native-v2/"+shot);
        require(output.isDirectory()||output.mkdirs(),"artifact directory ready");
        // This test-owned directory must describe only this run, not retain an old failure PNG.
        File[] prior=output.listFiles();
        if (prior!=null) for (File file:prior) if (file.isFile() && (file.getName().endsWith(".png") || file.getName().equals("checks.txt")))
            require(file.delete(),"prior test artifact cleared "+file.getName());
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
