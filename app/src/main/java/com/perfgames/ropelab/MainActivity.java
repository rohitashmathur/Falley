package com.perfgames.ropelab;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityNodeProvider;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

/** Small Android shell using Frontline's profile, simple screens and preference pattern. */
public class MainActivity extends Activity implements SensorEventListener {
    private Arena arena;
    private SensorManager sensors;
    private Sensor accelerometer, gyroscope;
    private FalleyAudio audio;
    private Object backCallback;
    private boolean resumed;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        sensors = (SensorManager)getSystemService(SENSOR_SERVICE);
        if (sensors != null) {
            accelerometer = sensors.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            gyroscope = sensors.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        }
        arena = new Arena(this, accelerometer != null, gyroscope != null);
        audio = new FalleyAudio(this);
        if (state != null) arena.screen = Arena.HOME;
        setContentView(arena);
        arena.setOnApplyWindowInsetsListener((view, insets) -> {
            arena.safeTop = insets.getSystemWindowInsetTop(); arena.safeBottom = insets.getSystemWindowInsetBottom();
            arena.safeLeft = insets.getSystemWindowInsetLeft(); arena.safeRight = insets.getSystemWindowInsetRight();
            arena.viewport(); return insets;
        });
        arena.requestApplyInsets();
        if (Build.VERSION.SDK_INT >= 33) backCallback = Api33Back.register(this, this::navigateBack);
    }
    @Override protected void onResume() {
        super.onResume(); resumed = true; arena.active = true; arena.last = 0;
        arena.accumulator = 0; arena.motion.resetSamples();
        arena.soundCue.reset(arena.physics.soundSequence);
        audio.setEnabled(arena.profile.sound); audio.setForeground(true);
        registerMotion(); arena.invalidate();
    }
    @Override protected void onPause() {
        resumed = false; arena.active = false;
        arena.setKeepScreenOn(false);
        if (arena.screen == Arena.PLAY) arena.screen = Arena.PAUSE;
        arena.clearTouch(); arena.motion.resetSamples(); arena.saveProfile();
        if (sensors != null) sensors.unregisterListener(this);
        audio.setForeground(false);
        super.onPause();
    }
    @Override protected void onSaveInstanceState(Bundle state) {
        state.putBoolean("falleyStarted",true);
        super.onSaveInstanceState(state);
    }
    @Override public void onBackPressed() {
        // Android 7-12 dispatch here; Android 13+ uses the supported callback.
        navigateBack();
    }
    private void navigateBack() {
        if (arena.screen == Arena.HOME) finish(); else arena.back();
    }
    @Override protected void onDestroy() {
        if (audio != null) audio.release();
        if (Build.VERSION.SDK_INT >= 33 && backCallback != null) {
            Api33Back.unregister(this, backCallback);
            backCallback = null;
        }
        super.onDestroy();
    }
    /** Keep API 33 class references out of fields verified on older devices. */
    @android.annotation.TargetApi(33)
    private static final class Api33Back {
        static Object register(Activity activity, Runnable action) {
            android.window.OnBackInvokedCallback callback = action::run;
            activity.getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                    android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT, callback);
            return callback;
        }
        static void unregister(Activity activity, Object callback) {
            activity.getOnBackInvokedDispatcher().unregisterOnBackInvokedCallback(
                    (android.window.OnBackInvokedCallback)callback);
        }
    }
    private void registerMotion() {
        if (sensors == null) return;
        sensors.unregisterListener(this);
        if (!resumed || !arena.motionEnabled || accelerometer == null) return;
        if (!sensors.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME)) {
            arena.hasMotion = false; arena.motionEnabled = false; return;
        }
        arena.hasGyro = gyroscope != null && sensors.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_GAME);
    }
    void toggleMotion() {
        if (!arena.hasMotion) return;
        arena.profile.motion = !arena.profile.motion; arena.motionEnabled = arena.profile.motion;
        arena.motion.resetSamples(); arena.physics.gx = 0; arena.saveProfile(); registerMotion();
    }
    void effect(int kind) {
        if (audio != null && arena.profile.sound) audio.generic(kind);
    }
    void hazardEffect(int kind) {
        if (audio != null && resumed && arena.active && arena.screen == Arena.PLAY && arena.profile.sound) audio.hazard(kind);
    }
    void stopEffects() { if (audio != null) audio.stopAll(); }
    void setSoundEnabled(boolean enabled) { if (audio != null) audio.setEnabled(enabled); }
    @Override public void onAccuracyChanged(Sensor sensor, int accuracy) { }
    @Override public void onSensorChanged(SensorEvent event) {
        if (!resumed || !arena.motionEnabled) return;
        boolean hadSample = arena.motion.hasSample();
        float x = event.values[0], y = event.values[1], z = event.values[2];
        // Sensor axes stay tied to natural orientation, including tablets.
        switch (getWindowManager().getDefaultDisplay().getRotation()) {
            case Surface.ROTATION_90: { float oldX = x; x = -y; y = oldX; break; }
            case Surface.ROTATION_180: x = -x; y = -y; break;
            case Surface.ROTATION_270: { float oldX = x; x = y; y = -oldX; break; }
            default: break;
        }
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
            arena.motion.acceptAccelerometer(event.timestamp, x, y, z);
        else if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE)
            arena.motion.acceptGyroscope(event.timestamp, x, y, z);
        // Settings is static: repaint only when its sensor status first changes.
        if (!hadSample && arena.motion.hasSample() && arena.screen == Arena.SETTINGS) arena.invalidate();
    }

    static final class Arena extends View {
        static final int HOME = 0, PLAY = 1, SETTINGS = 2, HELP = 3, PAUSE = 4, RESULT = 5, SPLASH = 6, CHAPTERS = 7, ARCADE_SETUP = 8, PRIVACY = 9;
        private static final float TOP = 160, BOTTOM = 730;
        private static final String[] PRESETS = {"LOOSE", "BALANCED", "TIGHT"};
        private static final String[] DIFFICULTIES = {"CHILL", "STANDARD", "BRUTAL"};
        final Physics physics = new Physics();
        final MotionSteering motion;
        final TouchSteering touch = new TouchSteering();
        final HazardAudioCue soundCue = new HazardAudioCue();
        final FalleyProfile profile = new FalleyProfile();
        final MainActivity owner;
        final SharedPreferences storage;
        final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        final Path path = new Path();
        final int mint = 0xff5de0ba, coral = 0xffff816c, gold = 0xfff6d477;
        final int white = 0xfff0f7f7, muted = 0xffa2aba9, background = 0xff17191b, panel = 0xff232629, border = 0xff41494a;
        boolean active, motionEnabled, hasMotion, hasGyro, resultRecorded;
        int screen = SPLASH, settingsReturn = HOME;
        long last, trailTime, lastLandingCue;
        float accumulator, scale = 1, ox, oy, visualTime, splashElapsed, homeElapsed;
        float burstTime = 10, burstX, burstY, setupMessage;
        float privacyScroll, privacyScrollLimit, privacyLastY;
        int privacyPointer = -1;
        int pointer = -1, buttonPointer = -1, safeTop, safeBottom, safeLeft, safeRight, trailIndex;
        final float[] trailX = new float[18], trailY = new float[18];
        final boolean[] trailValid = new boolean[18];
        final ArrayList<Hit> buttons = new ArrayList<>();
        final MenuAccessibility menuAccessibility = new MenuAccessibility();
        Hit pressed;
        private static final class Hit {
            final String id, label; final float left, top, right, bottom;
            final boolean enabled, checkable, checked;
            Hit(String id,float l,float t,float r,float b) { this(id,l,t,r,b,id,true,false,false); }
            Hit(String id,float l,float t,float r,float b,String label,boolean enabled,boolean checkable,boolean checked) {
                this.id = id; left = l; top = t; right = r; bottom = b; this.label = label;
                this.enabled = enabled; this.checkable = checkable; this.checked = checked;
            }
            boolean contains(float x,float y) { return x >= left && x <= right && y >= top && y <= bottom; }
        }

        Arena(MainActivity context,boolean hasAccelerometer,boolean hasGyroscope) {
            super(context); owner = context; hasMotion = hasAccelerometer; hasGyro = hasGyroscope;
            storage = context.getSharedPreferences("falley-v1", Context.MODE_PRIVATE);
            profile.motion = storage.getBoolean("motion",true); profile.sound = storage.getBoolean("sound",true);
            profile.haptics = storage.getBoolean("haptics",true); profile.preset = storage.getInt("preset",MotionSteering.BALANCED);
            profile.bestScore = storage.getInt("bestScore",0); profile.wins = storage.getInt("wins",0);
            profile.fastestWin = storage.getFloat("fastestWin",0);
            profile.blood = storage.getBoolean("blood",true);
            profile.arcadeDifficulty = storage.getInt("arcadeDifficulty",1);
            profile.arcadeHazards = storage.getInt("arcadeHazards",ArcadeRules.ALL);
            for (int i = 0; i < 3; i++) {
                profile.arcadeBest[i] = storage.getInt("arcade_best_"+i,0);
                profile.arcadeRuns[i] = storage.getInt("arcade_runs_"+i,0);
                profile.arcadeLongest[i] = storage.getFloat("arcade_longest_"+i,0);
            }
            profile.selectedStage = storage.getInt("selectedStage",0);
            for (int i = 0; i < StageCatalog.COUNT; i++) {
                profile.best[i] = storage.getInt("stage_best_"+i,i == 0 ? profile.bestScore : 0);
                profile.stageWins[i] = storage.getInt("stage_wins_"+i,i == 0 ? profile.wins : 0);
                profile.stageTimes[i] = storage.getFloat("stage_time_"+i,i == 0 ? profile.fastestWin : 0);
            }
            profile.sanitize(); physics.reset(profile.selectedStage);
            motion = new MotionSteering(hasGyroscope); motion.setPreset(profile.preset);
            motionEnabled = hasMotion && profile.motion;
            setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
            setFocusable(true);
        }
        void saveProfile() {
            SharedPreferences.Editor editor = storage.edit().putBoolean("motion",profile.motion).putBoolean("sound",profile.sound)
                    .putBoolean("haptics",profile.haptics).putInt("preset",profile.preset)
                    .putInt("bestScore",profile.bestScore).putInt("wins",profile.wins)
                    .putFloat("fastestWin",profile.fastestWin).putInt("selectedStage",profile.selectedStage);
            editor.putBoolean("blood",profile.blood).putInt("arcadeDifficulty",profile.arcadeDifficulty).putInt("arcadeHazards",profile.arcadeHazards);
            for (int i = 0; i < 3; i++)
                editor.putInt("arcade_best_"+i,profile.arcadeBest[i]).putInt("arcade_runs_"+i,profile.arcadeRuns[i]).putFloat("arcade_longest_"+i,profile.arcadeLongest[i]);
            for (int i = 0; i < StageCatalog.COUNT; i++)
                editor.putInt("stage_best_"+i,profile.best[i]).putInt("stage_wins_"+i,profile.stageWins[i]).putFloat("stage_time_"+i,profile.stageTimes[i]);
            editor.apply();
        }
        @Override protected void onSizeChanged(int w,int h,int oldW,int oldH) { viewport(); }
        void viewport() {
            int width = getWidth()-safeLeft-safeRight, height = getHeight()-safeTop-safeBottom;
            if (width <= 0 || height <= 0) return;
            scale = Math.min(width/480f,height/820f);
            ox = safeLeft+(width-480f*scale)/2; oy = safeTop+(height-820f*scale)/2;
        }
        private void text(Canvas c,String label,float x,float y,float size,int color) {
            paint.setStyle(Paint.Style.FILL); paint.setColor(color); paint.setTextSize(size);
            paint.setTypeface(Typeface.create("sans-serif",Typeface.BOLD)); c.drawText(label,x,y,paint);
        }
        private void center(Canvas c,String label,float x,float y,float size,int color) {
            paint.setTextAlign(Paint.Align.CENTER); text(c,label,x,y,size,color); paint.setTextAlign(Paint.Align.LEFT);
        }
        private void line(Canvas c,float x,float y,float x2,float y2,int color,float width) {
            paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(width); paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setColor(color); c.drawLine(x,y,x2,y2,paint); paint.setStyle(Paint.Style.FILL);
        }
        private void circle(Canvas c,float x,float y,float r,int color) {
            paint.setStyle(Paint.Style.FILL); paint.setColor(color); c.drawCircle(x,y,r,paint);
        }
        private void rect(Canvas c,float l,float t,float r,float b,float radius,int color) {
            paint.setStyle(Paint.Style.FILL); paint.setColor(color); c.drawRoundRect(l,t,r,b,radius,radius,paint);
        }
        private void button(Canvas c,String id,float l,float t,float r,float b,String label,int color,float size) {
            rect(c,l,t,r,b,10,panel); paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(1.5f); paint.setColor(color);
            c.drawRoundRect(l,t,r,b,10,10,paint); paint.setStyle(Paint.Style.FILL);
            center(c,label,(l+r)/2,(t+b)/2+size*.35f,size,color);
            String accessibleLabel = label.isEmpty() ? id.equals("back") ? "Back" : id.equals("restart") ? "Restart" : "Pause" : label;
            boolean enabled = !id.equals("jump") || !physics.lost && !physics.won;
            boolean choice = id.startsWith("preset_") || id.startsWith("difficulty_");
            boolean checked = choice && (id.startsWith("preset_") ? id.equals("preset_"+profile.preset) : id.equals("difficulty_"+profile.arcadeDifficulty));
            buttons.add(new Hit(id,l,t,r,b,accessibleLabel,enabled,choice,checked));
        }
        private void gear(Canvas c,float x,float y,int color) {
            circle(c,x,y,11,color); circle(c,x,y,7,panel); circle(c,x,y,2.5f,color);
            for (int i = 0; i < 8; i++) {
                double a = i*Math.PI/4;
                line(c,x+(float)Math.cos(a)*11,y+(float)Math.sin(a)*11,x+(float)Math.cos(a)*15,y+(float)Math.sin(a)*15,color,3);
            }
        }
        private void header(Canvas c,String title) {
            button(c,"back",24,20,98,94,"",white,18);
            line(c,48,57,74,57,white,2); line(c,48,57,58,47,white,2); line(c,48,57,58,67,white,2);
            text(c,title,118,68,28,white); line(c,24,110,456,110,border,1);
        }
        @Override protected void onDraw(Canvas c) {
            long now = SystemClock.uptimeMillis();
            float frameDt = last == 0 ? 0 : Math.min((now-last)/1000f,.05f);
            if (active) {
                last = now; visualTime += frameDt; setupMessage = Math.max(0,setupMessage-frameDt);
                if (screen == SPLASH) {
                    splashElapsed += frameDt;
                    if (splashElapsed >= 1.5f) enterHome();
                }
                if (screen == HOME) homeElapsed += frameDt;
                if (screen == PLAY) advance(frameDt,now);
                else {
                    accumulator = 0;
                    // The loss effect finishes behind the result card without
                    // advancing gameplay while a menu is open.
                    if (screen == RESULT && physics.lost) burstTime += frameDt;
                }
            }
            buttons.clear(); c.drawColor(background); c.save(); c.translate(ox,oy); c.scale(scale,scale);
            if (screen == SPLASH) drawSplash(c);
            else if (screen == HOME) drawHome(c);
            else if (screen == SETTINGS) drawSettings(c);
            else if (screen == HELP) drawHelp(c);
            else if (screen == CHAPTERS) drawChapters(c);
            else if (screen == ARCADE_SETUP) drawArcadeSetup(c);
            else if (screen == PRIVACY) drawPrivacy(c);
            else {
                drawPlay(c);
                if (screen == PAUSE || screen == RESULT) { buttons.clear(); drawOverlay(c); }
            }
            c.restore();
            setKeepScreenOn(active && screen == PLAY);
            menuAccessibility.updateScreen();
            // Static menus need no render loop. Home retains its gentle title
            // animation at 30fps; gameplay and its short effects use vsync.
            if (active) {
                if (screen == PLAY || screen == SPLASH || screen == RESULT && physics.lost && burstTime < 2.8f)
                    postInvalidateOnAnimation();
                else if (screen == HOME || screen == ARCADE_SETUP && setupMessage > 0) postInvalidateDelayed(33);
                else last = 0;
            }
        }
        private void advance(float dt,long now) {
            float oldPulse = physics.landingPulse;
            boolean oldWon = physics.won, oldLost = physics.lost;
            burstTime += dt; accumulator += dt;
            applySteering();
            while (accumulator >= Physics.DT) { physics.step(); pollHazardAudio(); accumulator -= Physics.DT; }
            pollHazardAudio();
            if (physics.landingPulse > oldPulse+.015f && now-lastLandingCue > 160) {
                cue(4); lastLandingCue = now;
            }
            if (physics.lost && !oldLost) {
                burstX = physics.x; burstY = physics.y; burstTime = 0; clearTouch(); cue(3);
            }
            if (physics.won && !oldWon) { clearTouch(); cue(2); }
            if ((physics.won || physics.lost) && !resultRecorded) {
                boolean changed = physics.arcade ? profile.recordArcadeResult(physics.arcadeDifficulty,physics.score,physics.elapsed) :
                        profile.recordResult(physics.stage.index,physics.won,physics.score,physics.elapsed);
                if (changed) saveProfile();
                resultRecorded = true;
            }
            if (physics.won && physics.winTime > 1.2f || physics.lost && burstTime > 2f) {
                screen = RESULT; clearTouch();
            }
            if (!physics.lost && now-trailTime > 30) {
                trailTime = now; trailX[trailIndex] = physics.x; trailY[trailIndex] = physics.y;
                trailValid[trailIndex] = true; trailIndex = (trailIndex+1)%trailX.length;
            }
        }
        private void pollHazardAudio() {
            int event = soundCue.consume(physics.soundSequence,physics.soundEvent);
            if (event != 0 && active && screen == PLAY && profile.sound) owner.hazardEffect(event);
        }
        private void titleLetters(Canvas c,float baseline,float size,float elapsed) {
            String title = "FALLEY";
            paint.setTextSize(size); paint.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));
            float width = paint.measureText(title)+20, x = (480-width)/2;
            for (int i = 0; i < title.length(); i++) {
                String letter = title.substring(i,i+1);
                float t = clamp((elapsed-i*.055f)/.6f,0,1), ease = 1-(float)Math.pow(1-t,3);
                float idle = screen == HOME && t >= 1 ? (float)Math.sin(visualTime*1.8f+i*.45f)*2 : 0;
                text(c,letter,x,baseline-32*(1-ease)+idle,size,white);
                x += paint.measureText(letter)+4;
            }
        }
        private void drawSplash(Canvas c) {
            drawHuman(c,240,310,45,-160,0);
            titleLetters(c,426,54,splashElapsed);
            center(c,"by Perf Games",240,469,19,mint);
            rect(c,150,520,330,525,3,panel);
            rect(c,150,520,150+180*clamp(splashElapsed/1.5f,0,1),525,3,mint);
        }
        private void drawHome(Canvas c) {
            titleLetters(c,100,49,homeElapsed);
            rect(c,24,132,456,318,16,panel);
            for (float y = 153; y < 289; y += 34) line(c,46,y,434,y,0xff2d3438,1);
            line(c,68,153,68,282,coral,3); line(c,412,153,412,282,coral,3);
            path.reset(); path.moveTo(68,211); path.quadTo(240,249,412,211);
            paint.setStyle(Paint.Style.STROKE); paint.setColor(mint); paint.setStrokeWidth(4); c.drawPath(path,paint); paint.setStyle(Paint.Style.FILL);
            drawHuman(c,242,202+(float)Math.sin(visualTime*3)*7,22,-150,0);
            rect(c,68,268,412,286,8,border); rect(c,180,268,300,279,4,mint);
            circle(c,240,274,14,0xff29584d); circle(c,240,274,9,mint); circle(c,240,274,4,panel);
            center(c,"CAMPAIGN / ARCADE SURVIVAL",240,305,12,muted);
            rect(c,24,338,456,420,14,mint); buttons.add(new Hit("play",24,338,456,420,
                    "Campaign. Stage "+(profile.selectedStage+1)+", "+StageCatalog.get(profile.selectedStage).title,true,false,false));
            text(c,"CAMPAIGN",45,372,22,background);
            text(c,"STAGE "+(profile.selectedStage+1)+" / "+StageCatalog.get(profile.selectedStage).title,45,402,14,background);
            line(c,412,381,432,381,background,3); line(c,432,381,422,371,background,3); line(c,432,381,422,391,background,3);
            button(c,"arcade",24,434,456,508,"ARCADE / SKIRMISH",gold,18);
            button(c,"chapters",24,522,456,596,"CHAPTERS",white,17);
            button(c,"help",24,610,456,684,"HOW TO PLAY",white,17);
            button(c,"settings",24,698,456,772,"SETTINGS",white,17);
            center(c,"BEST "+profile.best[profile.selectedStage]+" / "+profile.stageWins[profile.selectedStage]+" LANDINGS",240,796,12,muted);
        }
        private void drawArcadeSetup(Canvas c) {
            header(c,"ARCADE"); text(c,"Survive the hazards. Chase a better run.",24,137,15,muted);
            text(c,"DIFFICULTY",24,163,13,muted);
            for (int i = 0; i < 3; i++) {
                float left = i == 0 ? 24 : i == 1 ? 164 : 312, right = i == 0 ? 154 : i == 1 ? 302 : 456;
                button(c,"difficulty_"+i,left,172,right,246,DIFFICULTIES[i],profile.arcadeDifficulty == i ? gold : muted,14);
                if (profile.arcadeDifficulty == i) {
                    rect(c,left+3,175,right-3,243,8,gold);
                    center(c,DIFFICULTIES[i],(left+right)/2,214,14,background);
                }
            }
            toggleRow(c,"arcade_fire","Fireballs",264,(profile.arcadeHazards & ArcadeRules.FIREBALLS) != 0,true);
            toggleRow(c,"arcade_top","Top Lasers",344,(profile.arcadeHazards & ArcadeRules.TOP_LASERS) != 0,true);
            toggleRow(c,"arcade_side","Side Lasers",424,(profile.arcadeHazards & ArcadeRules.SIDE_LASERS) != 0,true);
            toggleRow(c,"arcade_lightning","Lightning",504,(profile.arcadeHazards & ArcadeRules.LIGHTNING) != 0,true);
            toggleRow(c,"arcade_saw","Buzzsaws",584,(profile.arcadeHazards & ArcadeRules.BUZZSAWS) != 0,true);
            text(c,setupMessage > 0 ? "Keep at least one hazard enabled." : "No finish line. New attacks arrive in waves.",24,681,13,setupMessage > 0 ? coral : muted);
            button(c,"start_arcade",24,698,456,772,"START SURVIVAL",gold,18);
            center(c,String.format(Locale.US,"BEST %d / LONGEST %.1fs",profile.arcadeBest[profile.arcadeDifficulty],
                    profile.arcadeLongest[profile.arcadeDifficulty]),240,798,12,muted);
        }
        private void drawChapters(Canvas c) {
            header(c,"CHAPTERS"); text(c,"Pick a fall. Every stage is ready to play.",24,137,15,muted);
            float[] tops = {176,258,366,448,556,638};
            for (int i = 0; i < StageCatalog.COUNT; i++) {
                StageCatalog.Stage stage = StageCatalog.get(i);
                if (i%2 == 0) text(c,stage.chapterTitle.toUpperCase(Locale.US),24,tops[i]-12,13,mint);
                rect(c,24,tops[i],456,tops[i]+74,10,panel);
                if (i == profile.selectedStage) line(c,26,tops[i]+12,26,tops[i]+62,mint,4);
                text(c,stage.title,44,tops[i]+31,20,white);
                text(c,"STAGE "+(i+1)+" / BEST "+profile.best[i]+" / "+profile.stageWins[i]+" LANDINGS",44,tops[i]+57,11,muted);
                line(c,416,tops[i]+29,426,tops[i]+37,mint,2); line(c,426,tops[i]+37,416,tops[i]+45,mint,2);
                buttons.add(new Hit("stage_"+i,24,tops[i],456,tops[i]+74,
                        "Stage "+(i+1)+", "+stage.title+". Best "+profile.best[i]+". "+profile.stageWins[i]+" landings",true,false,false));
            }
            button(c,"back",24,730,456,806,"DONE",mint,17);
        }
        private void toggleRow(Canvas c,String id,String title,float top,boolean value,boolean available) {
            rect(c,24,top,456,top+74,9,panel); text(c,title,44,top+44,18,available ? white : muted);
            rect(c,376,top+23,436,top+51,14,value && available ? mint : border);
            circle(c,value && available ? 421 : 391,top+37,10,value && available ? background : muted);
            buttons.add(new Hit(id,24,top,456,top+74,title+(available ? "" : ", unavailable"),available,true,value && available));
        }
        private void drawSettings(Canvas c) {
            header(c,"SETTINGS"); text(c,hasMotion ? "Make motion feel right." : "Touch controls work without sensors.",24,137,16,muted);
            toggleRow(c,"motion","Motion steering",154,motionEnabled,hasMotion);
            text(c,"MOTION FEEL",24,257,13,muted);
            for (int i = 0; i < 3; i++) {
                float l = i == 0 ? 24 : i == 1 ? 164 : 312, r = i == 0 ? 154 : i == 1 ? 302 : 456;
                button(c,"preset_"+i,l,273,r,347,PRESETS[i],profile.preset == i ? mint : muted,14);
                if (profile.preset == i) {
                    rect(c,l+3,276,r-3,344,8,mint);
                    center(c,PRESETS[i],(l+r)/2,315,14,background);
                }
            }
            String detail = profile.preset == 0 ? "Quick response. Small tilts move you more." :
                    profile.preset == 2 ? "Calmer response. More tilt, less movement." : "A steady balance of response and control.";
            text(c,detail,24,374,13,muted);
            text(c,!hasMotion ? "No sensor available. Swipe left or right to steer." :
                    !motionEnabled ? "Motion is off. Swipe left or right to steer." :
                    !motion.hasSample() ? "Waiting for a sensor reading..." : "Current hold centers automatically after resume.",24,397,12,muted);
            line(c,24,405,456,405,border,1); text(c,"FEEDBACK",24,433,13,muted);
            toggleRow(c,"sound","Sound Effects",444,profile.sound,true);
            toggleRow(c,"haptics","Vibration",532,profile.haptics,true);
            toggleRow(c,"blood","Blood Effects",620,profile.blood,true);
            button(c,"privacy",24,734,306,808,"PRIVACY POLICY",white,16);
            button(c,"back",320,734,456,808,"DONE",mint,17);
        }
        private void drawHelp(Canvas c) {
            header(c,"HOW TO PLAY"); text(c,"Longer falls. One bouncy target.",24,157,22,mint);
            helpStep(c,"01","TAP TO JUMP","Tap the arena or the Jump button.",198);
            helpStep(c,"02","STEER AGAINST THE DRIFT","Air currents push you. Swipe against the drift.",304);
            helpStep(c,"03","LET THE ROPES BOUNCE","Landing bends your knees, then springs you up.",410);
            helpStep(c,"04","LAND SOFTLY OR SURVIVE","Campaign: land on the target. Arcade: stay alive.",516);
            text(c,"Buzzsaws warn, then sweep sideways. Move clear.",24,631,13,muted);
            text(c,"Hard floor outside the target is dangerous.",24,658,15,muted);
            text(c,"Motion ON? Tilt to steer. Adjust feel in Settings.",24,686,14,muted);
            button(c,"back",24,734,456,808,"GOT IT",mint,17);
        }
        private void drawPrivacy(Canvas c) {
            header(c,"PRIVACY POLICY");
            float fontScale = clamp(getResources().getConfiguration().fontScale,1,1.6f);
            float size = 20*fontScale, lineHeight = size*1.42f;
            // The policy supports Android text-size preferences independently of
            // the game's fixed arena scale. Only this document scrolls.
            float y = 159-privacyScroll;
            c.save(); c.clipRect(24,128,446,704);
            y = policyParagraph(c,PrivacyPolicy.TITLE,24,y,size+2,lineHeight,true,white)+8;
            y = policyParagraph(c,PrivacyPolicy.PUBLISHER,24,y,size*.85f,lineHeight*.85f,false,muted);
            y = policyParagraph(c,PrivacyPolicy.EFFECTIVE_DATE,24,y,size*.85f,lineHeight*.85f,false,muted)+24;
            for (String[] section : PrivacyPolicy.SECTIONS) {
                y = policyParagraph(c,section[0],24,y,size,lineHeight,true,mint)+2;
                y = policyParagraph(c,section[1],24,y,size,lineHeight,false,white)+25;
            }
            c.restore();
            privacyScrollLimit = Math.max(0,y+privacyScroll-714);
            privacyScroll = clamp(privacyScroll,0,privacyScrollLimit);
            float viewport = 576, content = viewport+privacyScrollLimit, thumb = viewport*viewport/content;
            rect(c,452,128,456,704,2,panel);
            rect(c,452,128+(viewport-thumb)*(privacyScrollLimit == 0 ? 0 : privacyScroll/privacyScrollLimit),
                    456,128+(viewport-thumb)*(privacyScrollLimit == 0 ? 0 : privacyScroll/privacyScrollLimit)+thumb,2,mint);
            center(c,privacyScroll >= privacyScrollLimit-1 ? "END OF POLICY" : "SWIPE UP TO READ MORE",240,726,11,muted);
            button(c,"back",24,744,456,816,"BACK TO SETTINGS",mint,17);
        }
        private float policyParagraph(Canvas c,String value,float x,float y,float size,float lineHeight,boolean bold,int color) {
            paint.setStyle(Paint.Style.FILL); paint.setColor(color); paint.setTextSize(size);
            paint.setTypeface(Typeface.create("sans-serif",bold ? Typeface.BOLD : Typeface.NORMAL));
            StringBuilder row = new StringBuilder();
            for (String word : value.split(" ")) {
                String candidate = row.length() == 0 ? word : row+" "+word;
                if (row.length() > 0 && paint.measureText(candidate) > 414) {
                    c.drawText(row.toString(),x,y,paint); y += lineHeight; row.setLength(0);
                }
                if (row.length() > 0) row.append(' ');
                row.append(word);
            }
            if (row.length() > 0) { c.drawText(row.toString(),x,y,paint); y += lineHeight; }
            return y;
        }
        private void scrollPrivacy(float amount) {
            privacyScroll = clamp(privacyScroll+amount,0,privacyScrollLimit);
            invalidate();
            menuAccessibility.contentChanged();
        }
        private void helpStep(Canvas c,String number,String title,String detail,float y) {
            circle(c,48,y+20,22,panel); center(c,number,48,y+25,14,mint);
            text(c,title,85,y+12,17,white); text(c,detail,24,y+64,13,muted);
            line(c,24,y+83,456,y+83,border,1);
        }
        private void drawPlay(Canvas c) {
            text(c,physics.arcade ? "FALLEY / ARCADE" : "FALLEY / STAGE "+(physics.stage.index+1),24,29,12,mint);
            text(c,physics.arcade ? DIFFICULTIES[physics.arcadeDifficulty] : physics.stage.title,24,67,24,white);
            button(c,"restart",300,20,374,94,"",white,17);
            paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(2); paint.setColor(white);
            c.drawArc(323,43,351,71,40,295,false,paint); paint.setStyle(Paint.Style.FILL);
            line(c,352,43,352,54,white,2); line(c,352,43,341,45,white,2);
            button(c,"pause",382,20,456,94,"",white,17);
            line(c,413,45,413,69,white,5); line(c,425,45,425,69,white,5);
            text(c,"SCORE "+physics.score,24,104,13,gold);
            text(c,String.format(Locale.US,"WEIGHT %.1fx",physics.mass()),171,104,13,white);
            text(c,physics.arcade ? "WAVE "+physics.wave : "CUTS "+physics.brokenCount,367,104,13,mint);
            float progress = physics.arcade ? (physics.elapsed%20)/20 : clamp((physics.y-310)/(physics.groundY()-physics.radius()-310),0,1);
            rect(c,24,116,456,122,3,panel); rect(c,24,116,24+432*progress,122,3,mint);
            float targetCenter = (physics.targetLeft()+physics.targetRight())/2;
            String landingSide = targetCenter < 239 ? "LEFT" : targetCenter > 241 ? "RIGHT" : "CENTER";
            String objective = physics.arcade ? String.format(Locale.US,"%.1fs SURVIVED",physics.elapsed) :
                    physics.won ? "LANDED!" : Math.max(0,(int)((physics.groundY()-physics.y-physics.radius())/10))+"m / LAND "+landingSide;
            text(c,objective,24,144,12,physics.won ? mint : muted);
            paint.setTextAlign(Paint.Align.RIGHT);
            text(c,motionEnabled ? "MOTION ON" : "SWIPE + DRIFT",456,144,11,muted);
            paint.setTextAlign(Paint.Align.LEFT);
            drawArena(c); drawPlayControls(c);
        }
        private void drawArena(Canvas c) {
            c.save(); c.clipRect(20,TOP,460,BOTTOM); rect(c,20,TOP,460,BOTTOM,0,0xff121a20);
            float gridOffset = physics.cameraY%45;
            for (float y = TOP-gridOffset; y <= BOTTOM; y += 45) line(c,20,y,460,y,0xff23313b,1);
            for (int x = 60; x < 460; x += 60) line(c,x,TOP,x,BOTTOM,0xff1b2832,1);
            float left = physics.leftWall(), right = physics.rightWall();
            drawRails(c,left,right);
            drawFloor(c);
            for (int j = 0; j < physics.ropes.length; j++) {
                Physics.Rope rope = physics.ropes[j]; int color = j%2 == 0 ? mint : 0xffac9eef;
                float tension = clamp(rope.tension,0,1);
                path.reset(); path.moveTo(rope.x[0],rope.y[0]-physics.cameraY);
                for (int i = 1; i < rope.x.length; i++) {
                    if (i-1 == rope.cut) path.moveTo(rope.x[i],rope.y[i]-physics.cameraY);
                    else path.lineTo(rope.x[i],rope.y[i]-physics.cameraY);
                }
                paint.setStyle(Paint.Style.STROKE); paint.setStrokeCap(Paint.Cap.ROUND); paint.setStrokeJoin(Paint.Join.ROUND);
                paint.setColor((color & 0xffffff)|0x20000000); paint.setStrokeWidth(11+5*tension); c.drawPath(path,paint);
                paint.setColor(rope.cut >= 0 ? (color & 0xffffff)|0x88000000 : color);
                paint.setStrokeWidth(3+2*tension); c.drawPath(path,paint); paint.setStyle(Paint.Style.FILL);
                circle(c,rope.x[0],rope.y[0]-physics.cameraY,5,color); int end = rope.x.length-1;
                circle(c,rope.x[end],rope.y[end]-physics.cameraY,5,color);
                if (rope.cut >= 0) for (int i = rope.cut; i <= rope.cut+1 && i < rope.x.length; i++) {
                    circle(c,rope.x[i],rope.y[i]-physics.cameraY,7,0x55f6d477);
                    circle(c,rope.x[i],rope.y[i]-physics.cameraY,3.5f,gold);
                }
            }
            drawFireHazards(c); drawBeamHazards(c); drawSawHazards(c);
            if (!physics.lost) {
                for (int k = 0; k < trailX.length; k++) {
                    int i = (trailIndex+k)%trailX.length;
                    if (trailValid[i]) circle(c,trailX[i],trailY[i]-physics.cameraY,1.5f+k*.1f,
                            (coral & 0xffffff)|((k*3)<<24));
                }
                drawHuman(c,physics.x,physics.y-physics.cameraY,physics.radius(),physics.vy,physics.landingPulse);
            }
            drawDeath(c); c.restore();
            line(c,20,TOP,460,TOP,border,1); line(c,20,BOTTOM,460,BOTTOM,border,1);
        }
        private void drawFloor(Canvas c) {
            if (physics.arcade) return;
            float y = physics.groundY()-physics.cameraY;
            if (y > BOTTOM+24 || y < TOP-50) return;
            float squash = physics.groundBounce*6;
            rect(c,24,y+squash,456,y+34,10,border);
            if (physics.stage.fatalMiss) {
                rect(c,24,y,physics.targetLeft(),y+34,7,0xff663541);
                rect(c,physics.targetRight(),y,456,y+34,7,0xff663541);
                center(c,"HARD FLOOR",(24+physics.targetLeft())/2,y+25,9,coral);
                center(c,"HARD FLOOR",(456+physics.targetRight())/2,y+25,9,coral);
            }
            rect(c,physics.targetLeft(),y+squash,physics.targetRight(),y+12,5,mint);
            line(c,physics.targetLeft(),y-3,physics.targetRight(),y-3,0x885de0ba,4);
            float middle = (physics.targetLeft()+physics.targetRight())/2;
            float gap = (physics.targetRight()-physics.targetLeft())/6, barWidth = Math.min(10,gap*.52f);
            for (int i = 0; i < 5; i++) {
                float x = middle+(i-2)*gap;
                rect(c,x-barWidth/2,y+18,x+barWidth/2,y+23,2,0xff273f38);
            }
            circle(c,middle,y+7,16,0xff29584d); circle(c,middle,y+7,11,mint); circle(c,middle,y+7,4,background);
            center(c,"LAND HERE",middle,y-21,13,mint);
        }
        private int hazardColor() {
            return physics.stage.hazard == StageCatalog.LASER ? 0xffff5f9a :
                    physics.stage.hazard == StageCatalog.SPIKES ? 0xffac9eef : 0xffff9c52;
        }
        private void drawRails(Canvas c,float left,float right) {
            int color = hazardColor();
            rect(c,20,TOP,left,BOTTOM,0,(color & 0xffffff)|0x22000000);
            rect(c,right,TOP,460,BOTTOM,0,(color & 0xffffff)|0x22000000);
            line(c,left,TOP,left,BOTTOM,(color & 0xffffff)|0x33000000,12);
            line(c,right,TOP,right,BOTTOM,(color & 0xffffff)|0x33000000,12);
            line(c,left,TOP,left,BOTTOM,color,3); line(c,right,TOP,right,BOTTOM,color,3);
            if (physics.stage.hazard == StageCatalog.LASER) {
                float pulse = 2+(float)Math.sin(visualTime*8)*.8f;
                line(c,left,TOP,left,BOTTOM,0xffffc6dc,pulse); line(c,right,TOP,right,BOTTOM,0xffffc6dc,pulse);
                for (float y = TOP+(visualTime*90)%70; y < BOTTOM; y += 70) {
                    circle(c,left,y,4,white); circle(c,right,y,4,white);
                }
            } else if (physics.stage.hazard == StageCatalog.SPIKES) {
                for (float y = TOP-20; y < BOTTOM; y += 32) {
                    path.reset(); path.moveTo(left,y); path.lineTo(left+physics.hazardInset(),y+16); path.lineTo(left,y+32); path.close();
                    paint.setColor(color); c.drawPath(path,paint);
                    path.reset(); path.moveTo(right,y); path.lineTo(right-physics.hazardInset(),y+16); path.lineTo(right,y+32); path.close();
                    c.drawPath(path,paint);
                    line(c,left+3,y+7,left+physics.hazardInset()-3,y+16,0xffddd5ff,1);
                    line(c,right-3,y+7,right-physics.hazardInset()+3,y+16,0xffddd5ff,1);
                }
            } else {
                for (float y = TOP-20; y < BOTTOM; y += 28) {
                    float flicker = 5+(float)Math.sin(visualTime*10+y*.13f)*3;
                    float tip = physics.hazardInset();
                    path.reset(); path.moveTo(left,y); path.quadTo(left+tip+flicker,y+14,left,y+28); path.quadTo(left-4,y+14,left,y); path.close();
                    paint.setColor(0xeeff9c52); c.drawPath(path,paint);
                    path.reset(); path.moveTo(right,y); path.quadTo(right-tip-flicker,y+14,right,y+28); path.quadTo(right+4,y+14,right,y); path.close(); c.drawPath(path,paint);
                    circle(c,left+tip*.35f,y+16,2.5f,gold); circle(c,right-tip*.35f,y+16,2.5f,gold);
                }
            }
        }
        private void drawFireHazards(Canvas c) {
            for (Physics.FireHazard fire : physics.fireHazards) {
                if (!fire.active) continue;
                if (fire.warningTime > 0) {
                    int alpha = 100+(int)(80*(.5f+.5f*Math.sin(visualTime*16)));
                    for (float y = TOP+52; y < BOTTOM; y += 28)
                        line(c,fire.x,y,fire.x,y+11,(0xff9c52)|alpha<<24,2);
                    circle(c,fire.x,TOP+24,17,0xff583c29);
                    center(c,"!",fire.x,TOP+31,20,gold);
                    continue;
                }
                float y = fire.y-physics.cameraY;
                if (y < TOP-60 || y > BOTTOM+40) continue;
                for (int k = 5; k > 0; k--) circle(c,fire.x,y-k*12,Math.max(2,fire.radius-k),
                        0x00ff9c52|((110-k*15)<<24));
                circle(c,fire.x,y,fire.radius+4,0x55ff9c52);
                circle(c,fire.x,y,fire.radius,0xffff9c52);
                circle(c,fire.x,y+2,fire.radius*.48f,gold);
            }
        }
        private void drawBeamHazards(Canvas c) {
            for (Physics.BeamHazard beam : physics.beamHazards) {
                if (!beam.active) continue;
                float y1 = beam.y1-physics.cameraY, y2 = beam.y2-physics.cameraY;
                int color = beam.kind == ArcadeRules.LIGHTNING ? 0xff8fd7ff : 0xffff5f9a;
                if (beam.warningTime > 0) {
                    float dx = beam.x2-beam.x1, dy = y2-y1;
                    float length = (float)Math.hypot(dx,dy);
                    for (float d = 0; d < length; d += 26) {
                        float a = d/Math.max(1,length), b = Math.min(length,d+13)/Math.max(1,length);
                        line(c,beam.x1+dx*a,y1+dy*a,beam.x1+dx*b,y1+dy*b,(color & 0xffffff)|0x99000000,2);
                    }
                    float markerX = clamp(beam.x1,40,440), markerY = clamp(y1,TOP+24,BOTTOM-24);
                    circle(c,markerX,markerY,16,0xff49313e); center(c,"!",markerX,markerY+7,20,color);
                } else if (beam.kind == ArcadeRules.LIGHTNING) {
                    path.reset(); path.moveTo(beam.x1,y1);
                    for (int i = 1; i <= 18; i++) {
                        float f = i/18f, jitter = i == 18 ? 0 : (float)Math.sin(visualTime*28+i*2.1f)*beam.width*.15f;
                        path.lineTo(beam.x1+(beam.x2-beam.x1)*f+jitter,y1+(y2-y1)*f);
                    }
                    paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(beam.width+8); paint.setColor(0x448fd7ff);
                    c.drawPath(path,paint); paint.setStrokeWidth(beam.width*.65f); paint.setColor(color); c.drawPath(path,paint);
                    paint.setStrokeWidth(2); paint.setColor(white); c.drawPath(path,paint); paint.setStyle(Paint.Style.FILL);
                } else {
                    line(c,beam.x1,y1,beam.x2,y2,0x55ff5f9a,beam.width+10);
                    line(c,beam.x1,y1,beam.x2,y2,color,beam.width);
                    line(c,beam.x1,y1,beam.x2,y2,0xffffd4e6,2);
                }
            }
        }
        private void drawSawHazards(Canvas c) {
            for (Physics.SawHazard saw : physics.sawHazards) {
                if (!saw.active) continue;
                float y = saw.y-physics.cameraY;
                if (y < TOP-50 || y > BOTTOM+50) continue;
                boolean warning = saw.warningTime > 0;
                if (warning) {
                    float from = Math.min(saw.pathX1,saw.pathX2), to = Math.max(saw.pathX1,saw.pathX2);
                    for (float x = from; x < to; x += 25) line(c,x,y,Math.min(to,x+12),y,0x99f6d477,2);
                    circle(c,saw.x,y,saw.radius+8,0x33f6d477);
                } else {
                    float direction = saw.velocityX < 0 ? -1 : 1;
                    circle(c,saw.x-direction*13,y,saw.radius*.6f,0x33788795);
                    circle(c,saw.x-direction*25,y,saw.radius*.4f,0x22788795);
                }
                c.save(); c.translate(saw.x,y); c.rotate(visualTime*570*(saw.velocityX < 0 ? -1 : 1));
                path.reset();
                for (int i = 0; i < 48; i++) {
                    double a = i*Math.PI/24;
                    float r = saw.radius*(i%4 == 1 || i%4 == 2 ? 1 : .76f);
                    float x = (float)Math.cos(a)*r, yy = (float)Math.sin(a)*r;
                    if (i == 0) path.moveTo(x,yy); else path.lineTo(x,yy);
                }
                path.close(); paint.setColor(warning ? 0x667f8d9e : 0xffc6d0db); c.drawPath(path,paint);
                circle(c,0,0,saw.radius*.66f,warning ? 0x66545f70 : 0xff545f70);
                for (int i = 0; i < 4; i++) {
                    double a = i*Math.PI/2;
                    circle(c,(float)Math.cos(a)*saw.radius*.38f,(float)Math.sin(a)*saw.radius*.38f,saw.radius*.1f,background);
                }
                circle(c,0,0,saw.radius*.17f,warning ? gold : coral); c.restore();
                // Tooth tips sit exactly on the circular physics boundary.
                paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(1); paint.setColor(warning ? 0x88f6d477 : 0xffe3e8ef);
                c.drawCircle(saw.x,y,saw.radius,paint); paint.setStyle(Paint.Style.FILL);
                if (warning) center(c,"!",saw.x,y+6,18,gold);
            }
        }
        /** Feet align with y+radius, the same proxy used by rope/floor contact. */
        private void drawHuman(Canvas c,float x,float y,float r,float vy,float landingPulse) {
            float crouch = clamp(landingPulse/.18f,0,1), pelvis = .28f+crouch*.22f;
            boolean rising = vy < -55, falling = vy > 80;
            float bob = (float)Math.sin(visualTime*11)*.06f;
            int skin = 0xffffd4ae, trousers = 0xff65b9f2;
            c.save(); c.translate(x,y); c.scale(r,r);
            float kneeY = .60f+crouch*.09f-(rising ? .08f : 0),
                    kneeX = .34f+crouch*.23f+(falling ? bob*1.8f : 0);
            line(c,-.17f,pelvis,-kneeX,kneeY, trousers,.22f);
            line(c,kneeX,kneeY,.17f,pelvis,trousers,.22f);
            line(c,-kneeX,kneeY,-.31f,1, trousers,.18f);
            line(c,kneeX,kneeY,.31f,1,trousers,.18f);
            line(c,-.40f,1,-.23f,1,white,.15f); line(c,.23f,1,.40f,1,white,.15f);
            float shoulder = -.44f+crouch*.29f, head = -.95f+crouch*.30f;
            float handY = crouch > .12f ? .37f : rising ? -.85f : falling ? -.25f+bob : .28f;
            float handX = crouch > .12f ? .67f : rising ? .66f : falling ? .95f : .57f;
            line(c,-.32f,shoulder,-handX,handY,skin,.16f); line(c,.32f,shoulder,handX,handY,skin,.16f);
            circle(c,-handX,handY,.11f,skin); circle(c,handX,handY,.11f,skin);
            rect(c,-.34f,shoulder-.02f,.34f,pelvis+.06f,.13f,coral);
            line(c,-.13f,shoulder+.12f,.13f,shoulder+.12f,0xffb9584b,.065f);
            circle(c,0,head,.34f,skin); circle(c,0,head-.20f,.26f,0xff353133);
            rect(c,-.30f,head-.17f,.30f,head+.08f,.1f,skin);
            circle(c,-.105f,head+.035f,.036f,background); circle(c,.105f,head+.035f,.036f,background);
            line(c,-.08f,head+.17f,.08f,head+.17f,0xff9b6653,.035f);
            c.restore();
        }
        private void deadFigure(Canvas c,float x,float y,float r,float angle,boolean charred,boolean crumpled) {
            c.save(); c.translate(x,y); c.rotate(angle); c.scale(r,r);
            int body = charred ? 0xff1e1c20 : coral, skin = charred ? 0xff302b30 : 0xffffd4ae;
            int legs = charred ? 0xff242329 : 0xff65b9f2;
            circle(c,.08f,-.83f,.33f,skin);
            line(c,0,-.42f,.05f,.33f,body,.54f);
            line(c,-.15f,-.3f,-.62f,.25f,skin,.15f); line(c,.18f,-.3f,.69f,.38f,skin,.15f);
            line(c,.02f,.31f,crumpled ? -.75f : -.27f,.62f,legs,.21f);
            line(c,.06f,.31f,.56f,crumpled ? .43f : .72f,legs,.21f);
            line(c,crumpled ? -.75f : -.27f,.62f,crumpled ? -.38f : -.35f,.96f,skin,.14f);
            line(c,.56f,crumpled ? .43f : .72f,crumpled ? .78f : .38f,.98f,skin,.14f);
            if (charred) {
                line(c,-.11f,-.14f,.19f,.03f,0xff9b5b38,.04f);
                circle(c,.17f,-.7f,.045f,gold);
            }
            c.restore();
        }
        private void drawDeath(Canvas c) {
            if (!physics.lost) return;
            float t = burstTime, y = burstY-physics.cameraY, r = physics.radius();
            String reason = physics.deathReason;
            boolean saw = reason.contains("BUZZSAW"), spikes = reason.contains("SPIKE"), floor = reason.contains("FLOOR") || reason.contains("HARD LANDING"),
                    laser = reason.contains("LASER"), thunder = reason.contains("LIGHTNING") || reason.contains("THUNDER");
            int blood = 0xffa62b3b;
            if (saw) {
                float split = clamp(t/.45f,0,1)*r*.5f, drop = Math.min(t*t*24,65);
                c.save(); c.translate(-split,drop-6*clamp(t,0,1));
                c.clipRect(burstX-r*2,y-r*2,burstX+r*2,y+r*.1f);
                deadFigure(c,burstX,y,r,-8,false,false); c.restore();
                c.save(); c.translate(split,drop+6*clamp(t,0,1));
                c.clipRect(burstX-r*2,y+r*.1f,burstX+r*2,y+r*2);
                deadFigure(c,burstX,y,r,10,false,false); c.restore();
                if (profile.blood) line(c,burstX-r*.6f,y+r*.1f+drop,burstX+r*.6f,y+r*.1f+drop,blood,4);
            } else if (spikes) {
                float pinnedX = clamp(burstX,physics.leftWall()+physics.hazardInset()+r*.25f,
                        physics.rightWall()-physics.hazardInset()-r*.25f);
                deadFigure(c,pinnedX,y,r,burstX < 240 ? -75 : 75,false,true);
                if (profile.blood) {
                    float side = burstX < 240 ? physics.leftWall()+physics.hazardInset() : physics.rightWall()-physics.hazardInset();
                    for (int i = 0; i < 8; i++)
                        line(c,side+(i%3-1)*3,y-8+i*3,side+(i%3-1)*3,y+Math.min(65,t*30)+i*3,blood,2+i%3);
                }
            } else if (floor) {
                y = physics.groundY()-physics.cameraY-7;
                deadFigure(c,burstX,y,r,88,false,true);
                if (profile.blood) {
                    paint.setColor(blood); c.drawOval(burstX-r*1.5f,y-1,burstX+r*1.5f,y+9,paint);
                }
            } else if (laser) {
                float melt = clamp(t/1.2f,0,1);
                c.save(); c.translate(burstX,y+r); c.scale(1,1-melt*.9f);
                deadFigure(c,0,-r,r,0,false,true); c.restore();
                if (profile.blood) {
                    paint.setColor(blood); c.drawOval(burstX-r*(.5f+melt),y+r-3,burstX+r*(.5f+melt),y+r+5,paint);
                    line(c,burstX-r*.55f,y-r*.2f,burstX+r*.55f,y-r*.2f,(blood & 0xffffff)|0xcc000000,3);
                }
            } else {
                float dissolved = clamp((t-.25f)/1.25f,0,1), size = r*(1-.9f*dissolved), ashBase = y+r*.7f;
                deadFigure(c,burstX,ashBase-size*.75f,size,thunder ? -10 : 12,true,true);
                paint.setColor(0xff18161b);
                c.drawOval(burstX-r*(.25f+dissolved*.7f),ashBase-2,
                        burstX+r*(.25f+dissolved*.7f),ashBase+4+dissolved*3,paint);
                for (int i = 0; i < 7; i++)
                    circle(c,burstX+(i-3)*r*.22f*dissolved,ashBase-(i%3)*2,1.5f+dissolved,0xff302a31);
                for (int i = 0; i < 9; i++) {
                    float ashX = burstX+(float)Math.sin(i*2.1f+t)*r*(.5f+t*.4f);
                    float ashY = y-r*.5f-i*3-Math.min(t,3)*18;
                    circle(c,ashX,ashY,2+i%2,0xff373138);
                }
            }
            if (t > 2.7f) return;
            float fade = clamp(1-t/2.7f,0,1), age = Math.min(t,2.1f);
            for (int i = 0; i < 28; i++) {
                double a = i*2.399963; float speed = 23+(i%7)*17;
                float x = burstX+(float)Math.cos(a)*speed*age,
                        py = y+(float)Math.sin(a)*speed*age+(floor || spikes || laser || saw ? 70 : -14)*age*age;
                if (floor && profile.blood && i%4 == 0) {
                    int bone = (white & 0xffffff)|((int)(fade*240)<<24);
                    float dx = (float)Math.cos(a)*4, dy = (float)Math.sin(a)*4;
                    line(c,x-dx,py-dy,x+dx,py+dy,bone,2);
                    circle(c,x-dx,py-dy,1.7f,bone); circle(c,x+dx,py+dy,1.7f,bone);
                } else if (floor || spikes || laser || saw) {
                    int color = profile.blood ? blood : muted;
                    circle(c,x,py,profile.blood ? 2+i%3 : 1.5f,(color & 0xffffff)|((int)(fade*220)<<24));
                } else {
                    int spark = thunder ? 0xff8fd7ff : i%2 == 0 ? gold : 0xffff9c52;
                    line(c,x,py,x+2,py-4,(spark & 0xffffff)|((int)(fade*240)<<24),1.5f);
                    circle(c,x+(i%3)*2,py+8,2,0xff242127);
                }
            }
        }
        private void drawPlayControls(Canvas c) {
            button(c,"jump",24,744,456,816,physics.lost || physics.won ? "JUMP" : physics.jumpRemaining() > 0 ? "JUMP ..." : "JUMP ↑",
                    physics.lost || physics.won ? muted : gold,20);
            float progress = 1-clamp(physics.jumpRemaining()/.30f,0,1);
            rect(c,52,802,428,806,2,0x44f6d477); rect(c,52,802,52+376*progress,806,2,gold);
        }
        private void drawOverlay(Canvas c) {
            boolean hasNext = !physics.arcade && physics.won && physics.stage.index < StageCatalog.COUNT-1;
            rect(c,0,0,480,820,0,0xb317191b); rect(c,52,220,428,(hasNext || physics.arcade) && screen == RESULT ? 744 : 675,17,panel);
            if (screen == PAUSE) {
                center(c,"PAUSED",240,267,29,white);
                button(c,"resume",74,292,406,366,"RESUME",mint,18);
                button(c,"restart",74,380,406,454,"RESTART",white,18);
                button(c,"settings",74,468,406,542,"SETTINGS",white,18);
                button(c,"home",74,556,406,630,"HOME",muted,18);
            } else {
                center(c,physics.won ? "LANDED!" : physics.arcade ? "RUN ENDED" : "FALL ENDED",240,280,35,physics.won ? mint : coral);
                center(c,physics.won ? "A soft landing. A good bounce." : "HIT BY "+physics.deathReason,240,319,15,white);
                center(c,"SCORE "+physics.score,240,371,27,gold);
                center(c,physics.arcade ? String.format(Locale.US,"%.1fs / WAVE %d",physics.elapsed,physics.wave) :
                        String.format(Locale.US,"%.1fs / %d ROPES CUT",physics.elapsed,physics.brokenCount),240,404,14,muted);
                center(c,physics.arcade ? "ARCADE BEST "+profile.arcadeBest[physics.arcadeDifficulty] :
                        "STAGE "+(physics.stage.index+1)+" BEST "+profile.best[physics.stage.index],240,435,12,muted);
                if (hasNext) {
                    button(c,"next_stage",74,468,406,542,"NEXT STAGE",mint,18);
                    button(c,"restart",74,556,406,630,"REPLAY",white,18);
                    button(c,"home",74,644,406,718,"HOME",muted,18);
                } else {
                    button(c,"restart",74,468,406,542,physics.arcade ? "TRY AGAIN" : "REPLAY",mint,18);
                    button(c,"home",74,556,406,630,"HOME",white,18);
                    if (physics.arcade) button(c,"arcade",74,644,406,718,"CHANGE SETUP",muted,18);
                }
            }
        }
        private void cue(int kind) {
            if (profile.haptics) performHapticFeedback(kind == 2 || kind == 3 ? HapticFeedbackConstants.LONG_PRESS :
                    kind == 0 ? HapticFeedbackConstants.KEYBOARD_TAP : HapticFeedbackConstants.VIRTUAL_KEY);
            String reason = physics.deathReason;
            boolean specificDeath = reason.contains("FIRE") || reason.contains("LASER") || reason.contains("LIGHTNING") ||
                    reason.contains("THUNDER") || reason.contains("BUZZSAW");
            if (kind != 3 || !specificDeath) owner.effect(kind);
        }
        private void applySteering() {
            if (physics.won) { physics.gx = 0; return; }
            boolean controlled = !motionEnabled || touch.dragging();
            physics.touchControlled(controlled);
            if (controlled) physics.steerTouch(touch.velocity());
            else physics.gx = motion.acceleration();
        }
        void clearTouch() {
            pointer = buttonPointer = privacyPointer = -1; pressed = null; touch.cancel();
            physics.steerTouch(0); physics.touchControlled(!motionEnabled); physics.gx = 0;
        }
        private void enterHome() { owner.stopEffects(); screen = HOME; homeElapsed = 0; clearTouch(); }
        private void restart() {
            physics.reset(); beginRun();
        }
        private void startCampaign() { physics.reset(profile.selectedStage); beginRun(); }
        private void beginRun() {
            owner.stopEffects(); soundCue.reset(physics.soundSequence);
            clearTouch(); accumulator = 0; Arrays.fill(trailValid,false); trailIndex = 0;
            burstTime = 10; resultRecorded = false; lastLandingCue = 0; screen = PLAY;
        }
        private void jump() { if (screen == PLAY && !physics.won && physics.jump()) cue(1); }
        void back() {
            if (screen == PLAY) { owner.stopEffects(); screen = PAUSE; }
            else if (screen == PAUSE) screen = PLAY;
            else if (screen == PRIVACY) screen = SETTINGS;
            else if (screen == SETTINGS) screen = settingsReturn;
            else enterHome();
            clearTouch(); accumulator = 0; invalidate();
        }
        void activate(String id) {
            if (id.equals("play")) startCampaign();
            else if (id.equals("restart")) restart();
            else if (id.equals("arcade")) { owner.stopEffects(); screen = ARCADE_SETUP; setupMessage = 0; clearTouch(); }
            else if (id.equals("start_arcade")) { physics.resetArcade(profile.arcadeDifficulty,profile.arcadeHazards); beginRun(); }
            else if (id.startsWith("difficulty_")) {
                profile.arcadeDifficulty = ArcadeRules.clampDifficulty(Integer.parseInt(id.substring(11))); saveProfile();
            } else if (id.startsWith("arcade_")) {
                int bit = id.equals("arcade_fire") ? ArcadeRules.FIREBALLS : id.equals("arcade_top") ? ArcadeRules.TOP_LASERS :
                        id.equals("arcade_side") ? ArcadeRules.SIDE_LASERS : id.equals("arcade_saw") ? ArcadeRules.BUZZSAWS : ArcadeRules.LIGHTNING;
                int next = profile.arcadeHazards ^ bit;
                if (next == 0) setupMessage = 2.5f;
                else { profile.arcadeHazards = next; setupMessage = 0; saveProfile(); }
            }
            else if (id.equals("home")) enterHome();
            else if (id.equals("chapters")) { screen = CHAPTERS; clearTouch(); }
            else if (id.startsWith("stage_")) {
                profile.selectedStage = Math.max(0,Math.min(StageCatalog.COUNT-1,Integer.parseInt(id.substring(6))));
                saveProfile(); startCampaign();
            } else if (id.equals("next_stage") && physics.won && physics.stage.index < StageCatalog.COUNT-1) {
                profile.selectedStage = physics.stage.index+1; saveProfile(); startCampaign();
            }
            else if (id.equals("help")) { screen = HELP; clearTouch(); }
            else if (id.equals("settings")) { owner.stopEffects(); settingsReturn = screen == HOME ? HOME : PAUSE; screen = SETTINGS; clearTouch(); }
            else if (id.equals("privacy")) { screen = PRIVACY; privacyScroll = 0; clearTouch(); }
            else if (id.equals("back")) back();
            else if (id.equals("pause")) { owner.stopEffects(); screen = PAUSE; clearTouch(); }
            else if (id.equals("resume")) { screen = PLAY; clearTouch(); }
            else if (id.equals("jump")) { jump(); return; }
            else if (id.equals("motion")) owner.toggleMotion();
            else if (id.startsWith("preset_")) {
                profile.preset = Integer.parseInt(id.substring(7));
                motion.setPreset(profile.preset); saveProfile();
            }
            else if (id.equals("sound")) { profile.sound = !profile.sound; owner.setSoundEnabled(profile.sound); saveProfile(); }
            else if (id.equals("haptics")) { profile.haptics = !profile.haptics; saveProfile(); }
            else if (id.equals("blood")) { profile.blood = !profile.blood; saveProfile(); }
            cue(0); accumulator = 0; invalidate(); menuAccessibility.contentChanged();
        }
        @Override public boolean performClick() { super.performClick(); return true; }
        @Override public AccessibilityNodeProvider getAccessibilityNodeProvider() { return menuAccessibility; }
        @Override public boolean dispatchHoverEvent(MotionEvent event) {
            if (menuAccessibility.hover(event)) return true;
            return super.dispatchHoverEvent(event);
        }

        /** Expose the Canvas menu controls as real, individually actionable nodes. */
        private final class MenuAccessibility extends AccessibilityNodeProvider {
            private static final int CONTENT = 10000, NONE = Integer.MIN_VALUE;
            private final AccessibilityManager manager = (AccessibilityManager)getContext().getSystemService(Context.ACCESSIBILITY_SERVICE);
            private int previousScreen = -1, focused = NONE, hovered = NONE;

            private String screenName() {
                switch (screen) {
                    case HOME: return "Falley home";
                    case SETTINGS: return "Settings";
                    case HELP: return "How to play";
                    case PAUSE: return "Paused";
                    case RESULT: return "Result";
                    case CHAPTERS: return "Chapters";
                    case ARCADE_SETUP: return "Arcade setup";
                    case PRIVACY: return PrivacyPolicy.TITLE;
                    case SPLASH: return "Falley by Perf Games";
                    default: return "Falley gameplay";
                }
            }
            private String content() {
                if (screen == PRIVACY) return PrivacyPolicy.fullText();
                if (screen == HELP) return "Tap the arena or Jump button to jump. Swipe left or right to steer against air currents. "
                        + "Ropes bend, then bounce you. In Campaign, land on the bouncy target. In Arcade, survive as long as you can. "
                        + "Hazards warn before they activate. Buzzsaws sweep sideways. Move clear. Hard floor outside the target is dangerous. "
                        + "Enable motion steering in Settings to tilt instead of swipe. Loose, Balanced and Tight adjust motion response.";
                if (screen == RESULT) return (physics.won ? "Landed. " : "Run ended. Hit by "+physics.deathReason+". ")
                        + "Score "+physics.score+". "+String.format(Locale.US,"%.1f seconds.",physics.elapsed);
                return null;
            }
            private Hit hit(int virtualId) {
                return virtualId > 0 && virtualId <= buttons.size() ? buttons.get(virtualId-1) : null;
            }
            private boolean valid(int virtualId) { return hit(virtualId) != null || virtualId == CONTENT && content() != null; }
            void updateScreen() {
                if (previousScreen == screen) return;
                previousScreen = screen; focused = hovered = NONE; contentChanged();
            }
            void contentChanged() {
                if (manager == null || !manager.isEnabled() || getParent() == null) return;
                AccessibilityEvent event = AccessibilityEvent.obtain(AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED);
                event.setContentChangeTypes(AccessibilityEvent.CONTENT_CHANGE_TYPE_SUBTREE);
                event.setPackageName(getContext().getPackageName()); event.setClassName(View.class.getName());
                event.setSource(Arena.this); getParent().requestSendAccessibilityEvent(Arena.this,event);
            }
            private void event(int virtualId,int type) {
                if (manager == null || !manager.isEnabled() || getParent() == null || !valid(virtualId)) return;
                AccessibilityEvent event = AccessibilityEvent.obtain(type);
                event.setPackageName(getContext().getPackageName()); event.setClassName("android.widget.Button");
                event.setSource(Arena.this,virtualId);
                event.getText().add(virtualId == CONTENT ? content() : hit(virtualId).label);
                getParent().requestSendAccessibilityEvent(Arena.this,event);
            }
            private void bounds(AccessibilityNodeInfo node,float l,float t,float r,float b) {
                Rect parent = new Rect((int)(ox+l*scale),(int)(oy+t*scale),(int)Math.ceil(ox+r*scale),(int)Math.ceil(oy+b*scale));
                node.setBoundsInParent(parent);
                int[] position = new int[2]; getLocationOnScreen(position); parent.offset(position[0],position[1]);
                node.setBoundsInScreen(parent);
            }
            @Override public AccessibilityNodeInfo createAccessibilityNodeInfo(int virtualId) {
                if (virtualId == HOST_VIEW_ID) {
                    AccessibilityNodeInfo node = AccessibilityNodeInfo.obtain(Arena.this);
                    Arena.this.onInitializeAccessibilityNodeInfo(node);
                    node.setClassName("android.view.ViewGroup"); node.setContentDescription(screenName());
                    for (int i = 0; i < buttons.size(); i++) {
                        node.addChild(Arena.this,i+1);
                        if (i == 0 && content() != null) node.addChild(Arena.this,CONTENT);
                    }
                    if (buttons.isEmpty() && content() != null) node.addChild(Arena.this,CONTENT);
                    if (screen == PRIVACY) {
                        node.setScrollable(true);
                        if (privacyScroll < privacyScrollLimit) node.addAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD);
                        if (privacyScroll > 0) node.addAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD);
                    }
                    return node;
                }
                if (!valid(virtualId)) return null;
                AccessibilityNodeInfo node = AccessibilityNodeInfo.obtain();
                node.setSource(Arena.this,virtualId); node.setParent(Arena.this);
                node.setPackageName(getContext().getPackageName()); node.setVisibleToUser(isShown());
                node.setFocusable(true); node.setAccessibilityFocused(focused == virtualId);
                node.addAction(focused == virtualId ? AccessibilityNodeInfo.ACTION_CLEAR_ACCESSIBILITY_FOCUS : AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS);
                if (virtualId == CONTENT) {
                    node.setClassName("android.widget.TextView"); node.setText(content()); node.setEnabled(true);
                    bounds(node,24,128,446,704);
                    if (screen == PRIVACY) {
                        node.setScrollable(true);
                        if (privacyScroll < privacyScrollLimit) node.addAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD);
                        if (privacyScroll > 0) node.addAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD);
                    }
                } else {
                    Hit button = hit(virtualId);
                    node.setClassName(button.checkable ? "android.widget.Switch" : "android.widget.Button");
                    node.setText(button.label); node.setEnabled(button.enabled); node.setClickable(button.enabled);
                    node.setCheckable(button.checkable); node.setChecked(button.checked);
                    if (button.enabled) node.addAction(AccessibilityNodeInfo.ACTION_CLICK);
                    bounds(node,button.left,button.top,button.right,button.bottom);
                }
                return node;
            }
            @Override public AccessibilityNodeInfo findFocus(int focus) {
                return focus == AccessibilityNodeInfo.FOCUS_ACCESSIBILITY && valid(focused) ? createAccessibilityNodeInfo(focused) : null;
            }
            @Override public java.util.List<AccessibilityNodeInfo> findAccessibilityNodeInfosByText(String query,int virtualId) {
                ArrayList<AccessibilityNodeInfo> matches = new ArrayList<>();
                if (query == null) return matches;
                String needle = query.toLowerCase(Locale.ROOT);
                if (content() != null && content().toLowerCase(Locale.ROOT).contains(needle)) matches.add(createAccessibilityNodeInfo(CONTENT));
                for (int i = 0; i < buttons.size(); i++)
                    if (buttons.get(i).label.toLowerCase(Locale.ROOT).contains(needle)) matches.add(createAccessibilityNodeInfo(i+1));
                return matches;
            }
            @Override public boolean performAction(int virtualId,int action,Bundle arguments) {
                if (screen == PRIVACY && (action == AccessibilityNodeInfo.ACTION_SCROLL_FORWARD || action == AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD)) {
                    scrollPrivacy(action == AccessibilityNodeInfo.ACTION_SCROLL_FORWARD ? 470 : -470); return true;
                }
                if (!valid(virtualId)) return false;
                if (action == AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS) {
                    if (focused == virtualId) return false;
                    if (valid(focused)) event(focused,AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUS_CLEARED);
                    focused = virtualId; event(virtualId,AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED); return true;
                }
                if (action == AccessibilityNodeInfo.ACTION_CLEAR_ACCESSIBILITY_FOCUS && focused == virtualId) {
                    focused = NONE; event(virtualId,AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUS_CLEARED); return true;
                }
                Hit button = hit(virtualId);
                if (action == AccessibilityNodeInfo.ACTION_CLICK && button != null && button.enabled) {
                    event(virtualId,AccessibilityEvent.TYPE_VIEW_CLICKED); activate(button.id); return true;
                }
                return false;
            }
            boolean hover(MotionEvent event) {
                if (manager == null || !manager.isEnabled() || !manager.isTouchExplorationEnabled()) return false;
                int action = event.getActionMasked();
                if (action != MotionEvent.ACTION_HOVER_ENTER && action != MotionEvent.ACTION_HOVER_MOVE && action != MotionEvent.ACTION_HOVER_EXIT) return false;
                int next = NONE;
                if (action != MotionEvent.ACTION_HOVER_EXIT) {
                    float x = (event.getX()-ox)/scale, y = (event.getY()-oy)/scale;
                    for (int i = buttons.size()-1; i >= 0; i--) if (buttons.get(i).contains(x,y)) { next = i+1; break; }
                    if (next == NONE && content() != null && x >= 24 && x <= 446 && y >= 128 && y <= 704) next = CONTENT;
                }
                if (next != hovered) {
                    if (valid(next)) event(next,AccessibilityEvent.TYPE_VIEW_HOVER_ENTER);
                    if (valid(hovered)) event(hovered,AccessibilityEvent.TYPE_VIEW_HOVER_EXIT);
                    hovered = next;
                }
                return next != NONE || action == MotionEvent.ACTION_HOVER_EXIT;
            }
        }
        @Override public boolean onTouchEvent(MotionEvent event) {
            int action = event.getActionMasked(), index = event.getActionIndex();
            float x = (event.getX(index)-ox)/scale, y = (event.getY(index)-oy)/scale;
            if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) {
                for (int i = buttons.size()-1; i >= 0; i--) if (buttons.get(i).contains(x,y)) {
                    if (buttonPointer < 0 && buttons.get(i).enabled) { pressed = buttons.get(i); buttonPointer = event.getPointerId(index); }
                    return true;
                }
                if (screen == PRIVACY && privacyPointer < 0 && x >= 24 && x <= 456 && y >= 128 && y <= 704) {
                    privacyPointer = event.getPointerId(index); privacyLastY = y; return true;
                }
                if (screen == PLAY && pointer < 0 && !physics.lost && !physics.won && x >= 20 && x <= 460 && y >= TOP && y <= BOTTOM) {
                    pointer = event.getPointerId(index); touch.begin(x,y); applySteering();
                }
            } else if (action == MotionEvent.ACTION_MOVE && screen == PRIVACY && privacyPointer >= 0) {
                int current = event.findPointerIndex(privacyPointer);
                if (current >= 0) {
                    float nextY = (event.getY(current)-oy)/scale;
                    scrollPrivacy(privacyLastY-nextY); privacyLastY = nextY;
                } else privacyPointer = -1;
            } else if (action == MotionEvent.ACTION_MOVE && pointer >= 0) {
                int current = event.findPointerIndex(pointer);
                if (current >= 0) {
                    touch.move((event.getX(current)-ox)/scale,(event.getY(current)-oy)/scale); applySteering();
                }
                else clearTouch();
            } else if (action == MotionEvent.ACTION_CANCEL) clearTouch();
            else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP) {
                int id = event.getPointerId(index);
                if (id == privacyPointer) privacyPointer = -1;
                if (id == buttonPointer) {
                    Hit hit = pressed; pressed = null; buttonPointer = -1;
                    if (hit != null && hit.contains(x,y)) { activate(hit.id); performClick(); }
                }
                if (id == pointer) {
                    boolean tap = touch.release(x,y); pointer = -1;
                    physics.steerTouch(0); applySteering();
                    if (tap) jump();
                    performClick();
                }
            }
            return true;
        }
        private static float clamp(float value,float low,float high) { return Math.max(low,Math.min(high,value)); }
    }
}
