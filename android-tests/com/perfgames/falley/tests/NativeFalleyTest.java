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
import java.util.Map;
import java.util.Set;

/** Real Android pointer navigation; terminal landing is explicitly a controlled fixture. */
public final class NativeFalleyTest extends Instrumentation {
    private static final String APP = "com.perfgames.falley";
    private static final String ACTIVITY = "com.perfgames.ropelab.MainActivity";
    private static final String BACKUP_MARKER = "fixture-original-ready";
    private static final int HOME=0, PLAY=1, SETTINGS=2, HELP=3, PAUSE=4, RESULT=5;
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
                playPauseAndRestart();
                controlledLanding();
                controlledDeath();
                controlledMiss();
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
                +"; actual pointer navigation and Activity recreation; controlled landing/death fixtures, not a natural phone playtest; original preferences restored\n"
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
        requireScreen(HOME,"initial home"); capture("home"); freeze("home physics frozen");
        tap(240,611); requireScreen(HELP,"How to Play opened by touch"); capture("how-to-play");
        freeze("help physics frozen"); back(); requireScreen(HOME,"Android back leaves help");
    }

    private void settingsAndRecreation() throws Exception {
        // Each selection is made by a physical screen tap and checked after a real Activity.recreate().
        for (int preset=0;preset<3;preset++) {
            tap(240,699); requireScreen(SETTINGS,"home settings opened");
            tap(preset==0?89:preset==1?233:384,310);
            final int expected=preset;
            ui(() -> require(((Number)call(read(arena,"motion"),"preset")).intValue()==expected,"active motion preset "+expected));
            require(storage.getInt("preset",-1)==preset,"preset "+preset+" saved");
            capture("settings-preset-"+preset); freeze("settings physics frozen preset "+preset);
            recreate(); requireScreen(HOME,"recreated Activity opens safe home");
            ui(() -> require(((Number)call(read(arena,"motion"),"preset")).intValue()==expected,"preset "+expected+" survives Activity recreation"));
        }
        tap(240,699); requireScreen(SETTINGS,"settings reopened after recreation");
        boolean motionBefore=storage.getBoolean("motion",false);
        boolean hasMotion=bool("hasMotion");
        tap(240,191);
        require(storage.getBoolean("motion",false)==(hasMotion?!motionBefore:motionBefore),"sensor switch respects hardware availability");
        // Keep deterministic touch steering during gameplay; Android sensor comfort needs a phone.
        if (storage.getBoolean("motion",false)) tap(240,191);
        tap(240,585); tap(240,671);
        require(storage.getBoolean("sound",false),"sound setting saves by touch");
        require(storage.getBoolean("haptics",false),"haptics setting saves by touch");
        recreate();
        require(storage.getBoolean("sound",false)&&storage.getBoolean("haptics",false),"sound and haptics survive Activity recreation");
        require(!storage.getBoolean("motion",true),"touch-only setting survives Activity recreation");
        tap(240,699); tap(240,585); tap(240,671); tap(240,771);
        requireScreen(HOME,"Done leaves home settings");
    }

    private void playPauseAndRestart() throws Exception {
        tap(240,487); requireScreen(PLAY,"Play begins Mode 1"); capture("gameplay");
        float before=physicsFloat("elapsed"); runFrames(260);
        require(physicsFloat("elapsed")>before+.10f,"gameplay advances under gravity");
        tap(345,779);
        require(physicsFloat("vy")<0,"Jump touch sends character upward");
        dragHeld(240,390,295,390);
        require(Math.abs(arenaFloat("touchSteering"))>0,"horizontal touch drag steers");
        release(295,390); require(arenaFloat("touchSteering")==0,"touch release clears steering");
        tap(419,57); requireScreen(PAUSE,"Pause opens by touch"); capture("pause"); freeze("pause physics frozen");
        tap(240,505); requireScreen(SETTINGS,"pause settings opened"); freeze("pause settings physics frozen");
        back(); requireScreen(PAUSE,"Android back returns settings to pause"); freeze("return to pause stays frozen");
        before=physicsFloat("elapsed"); back(); requireScreen(PLAY,"Android back resumes pause");
        runFrames(220); require(physicsFloat("elapsed")>before+.09f,"resume advances the gameplay clock");
        tap(121,779); requireScreen(PLAY,"gameplay Restart starts a fresh run");
        require(physicsFloat("elapsed")<.25f,"Restart resets elapsed time");
        require(!physicsBool("lost")&&!physicsBool("won"),"Restart clears terminal states");
        tap(419,57); tap(240,417); requireScreen(PLAY,"pause Restart resumes fresh run");
        require(physicsFloat("elapsed")<.25f,"pause Restart resets elapsed time");
        tap(419,57); tap(240,593); requireScreen(HOME,"pause Main Menu returns home");
        freeze("home freezes an abandoned run"); tap(240,487); requireScreen(PLAY,"new run begins from home");
        require(physicsFloat("elapsed")<.25f,"new home run resets timer");
    }

    private void controlledLanding() throws Exception {
        // This fixture stages a center descent immediately above the floor. Actual frame updates
        // must perform collision, bounce, win animation, result transition and persistence.
        ui(() -> {
            call(physics,"reset");
            float ground=((Number)read(physics.getClass(),"GROUND_Y")).floatValue();
            float radius=((Number)call(physics,"radius")).floatValue();
            write(physics,"x",240f); write(physics,"y",ground-radius-4f);
            write(physics,"vx",0f); write(physics,"vy",120f); write(physics,"gx",0f);
            write(physics,"cameraY",ground-530f); write(arena,"touchSteering",0f);
            arena.invalidate();
        });
        runUntilPhysics("won",true,2000);
        require(physicsBool("won"),"actual frame physics detects staged target landing");
        require(!physicsBool("lost"),"target landing wins without a death");
        require(physicsFloat("vy")<0,"target floor bounces character upward");
        require(physicsFloat("landingPulse")>0&&physicsFloat("groundBounce")>0,"landing triggers human crouch and target bounce animation");
        requireScreen(PLAY,"floor bounce animates before result overlay"); capture("target-bounce-controlled");
        waitScreen(RESULT,2500); capture("victory-controlled");
        require(storage.getInt("wins",0)==1,"victory persists once");
        freeze("win result physics frozen");
        require(storage.getInt("wins",0)==1,"result redraw does not duplicate win count");
        tap(240,505); requireScreen(PLAY,"win Play Again restarts by touch");
        require(!physicsBool("won")&&!physicsBool("lost"),"replay clears win");
        require(physicsFloat("elapsed")<.25f,"replay resets timer");
    }

    private void controlledDeath() throws Exception {
        ui(() -> { write(physics,"x",10f); write(physics,"vx",0f); write(physics,"gx",0f); arena.invalidate(); });
        waitScreen(RESULT,2500); require(physicsBool("lost"),"actual physics detects staged rail overlap");
        require(!physicsBool("won"),"death cannot award a win"); capture("death-controlled");
        tap(240,593); requireScreen(HOME,"result Main Menu works by touch");
    }

    private void controlledMiss() throws Exception {
        tap(240,487); requireScreen(PLAY,"new run starts after a death");
        ui(() -> {
            call(physics,"reset");
            float ground=((Number)read(physics.getClass(),"GROUND_Y")).floatValue();
            float radius=((Number)call(physics,"radius")).floatValue();
            write(physics,"x",120f); write(physics,"y",ground-radius-4f);
            write(physics,"vx",0f); write(physics,"vy",120f); write(physics,"gx",0f);
            write(physics,"cameraY",ground-530f); arena.invalidate();
        });
        runUntilGroundBounce(1500);
        require(physicsFloat("groundBounce")>0&&physicsFloat("vy")<0,"outside-target floor remains bouncy");
        require(!physicsBool("lost")&&!physicsBool("won"),"outside-target bounce permits recovery without awarding a win");
        requireScreen(PLAY,"off-target landing keeps gameplay available");
        require(storage.getInt("wins",0)==1,"missed landing leaves wins unchanged");
        capture("off-target-bounce-controlled");
        ui(() -> {
            float ground=((Number)read(physics.getClass(),"GROUND_Y")).floatValue();
            float radius=((Number)call(physics,"radius")).floatValue();
            write(physics,"x",240f); write(physics,"y",ground-radius-4f);
            write(physics,"vx",0f); write(physics,"vy",120f); write(physics,"gx",0f); arena.invalidate();
        });
        waitScreen(RESULT,2500);
        require(physicsBool("won")&&!physicsBool("lost"),"corrected target landing wins after a miss");
        require(storage.getInt("wins",0)==2,"recovered landing records exactly one additional victory");
        tap(240,593); requireScreen(HOME,"recovered result returns home");
    }

    private void launch() throws Exception {
        activity=startActivitySync(new Intent().setClassName(APP,ACTIVITY).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        bind(); output=new File(activity.getExternalFilesDir(null),"falley-native/"+shot);
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
            settle(20);
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
    private void runUntilGroundBounce(long timeout) throws Exception {
        animation(true); long deadline=SystemClock.uptimeMillis()+timeout; final boolean[] matched={false};
        do {
            SystemClock.sleep(12);
            ui(() -> { matched[0]=((Number)read(physics,"groundBounce")).floatValue()>0; if (matched[0]) write(arena,"active",false); });
        } while (!matched[0]&&SystemClock.uptimeMillis()<deadline);
        animation(false); require(matched[0],"native frame detects off-target floor contact");
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
    private float arenaFloat(String field) throws Exception { return numeric(arena,field); }
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
