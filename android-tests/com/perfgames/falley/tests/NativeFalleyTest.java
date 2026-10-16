package com.perfgames.falley.tests;

import android.app.Activity;
import android.app.Application;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.SystemClock;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Real Android pointer navigation; terminal landing is explicitly a controlled fixture. */
public final class NativeFalleyTest extends Instrumentation {
    private static final String APP = "com.perfgames.falley";
    private static final String ACTIVITY = "com.perfgames.ropelab.MainActivity";
    private static final String BACKUP_MARKER = "fixture-original-ready";
    private static final int HOME=0, PLAY=1, SETTINGS=2, HELP=3, PAUSE=4, RESULT=5, SPLASH=6, CHAPTERS=7, ARCADE_SETUP=8, PRIVACY=9;
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
        if ((!mode.equals("suite") && !mode.equals("effects") && !mode.equals("v31") && !mode.equals("release") && !mode.equals("store") && !mode.equals("restore")) || !shot.matches("[a-z0-9-]+"))
            throw new IllegalArgumentException("Falley test harness arguments");
        start();
    }

    @Override public void onStart() {
        storage=getTargetContext().getSharedPreferences("falley-v1",Context.MODE_PRIVATE);
        backup=getTargetContext().getSharedPreferences("falley-native-original",Context.MODE_PRIVATE);
        Throwable failure=null;
        try {
            if (!mode.equals("restore")) {
                require(!backup.getBoolean(BACKUP_MARKER,false),"no interrupted fixture; recover with -RestoreOnly if needed");
                copy(backup,storage.getAll());
                require(backup.edit().putBoolean(BACKUP_MARKER,true).commit(),"original preferences backed up");
                require(storage.edit().clear().putInt("preset",1).putBoolean("motion",false)
                        .putBoolean("sound",false).putBoolean("haptics",false).putBoolean("blood",true).commit(),"deterministic emulator settings installed");
                launch();
                if (mode.equals("store")) {
                    waitScreen(HOME,4000); storeCaptures();
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
                : mode.equals("store") ? "V3.2 high-resolution actual native Canvas store captures and measured crop metadata"
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
        for (int stage=0;stage<6;stage++) {
            require(stageIndex()==stage,"next stage is "+(stage+1));
            if (stage==0||stage==2||stage==4) capture("chapter-"+(stage/2+1)+"-gameplay");
            stageTargetFixture(); runUntilPhysics("won",true,2000);
            require(physicsBool("won")&&!physicsBool("lost"),"stage "+(stage+1)+" target landing wins");
            require(physicsFloat("vy")<0&&physicsFloat("groundBounce")>0,"stage "+(stage+1)+" target physically bounces");
            if (stage==0||stage==5) capture("stage-"+(stage+1)+"-target-bounce-controlled");
            waitScreen(RESULT,2500); if (stage==0) { accessibleMenu("Result"); awake(false,"Result"); } capture("stage-"+(stage+1)+"-victory-controlled");
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

    private void launch() throws Exception { launch(true); }
    private void launch(boolean prepareArtifacts) throws Exception {
        // Stop the intro clock immediately after actual onResume, before slow emulator idle waits
        // can consume the entire splash. Native frames then verify the real timed transition.
        final Throwable[] earlyFailure={null}; Application app=(Application)getTargetContext().getApplicationContext();
        Application.ActivityLifecycleCallbacks callback=new Application.ActivityLifecycleCallbacks() {
            @Override public void onActivityCreated(Activity a,Bundle b) { }
            @Override public void onActivityStarted(Activity a) { }
            @Override public void onActivityResumed(Activity a) { }
            @Override public void onActivityPostResumed(Activity a) {
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
        bind(); if (!prepareArtifacts) return;
        output=new File(activity.getExternalFilesDir(null),"falley-native-v32/"+shot);
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
