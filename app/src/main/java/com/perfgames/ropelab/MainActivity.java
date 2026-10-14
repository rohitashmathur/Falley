package com.perfgames.ropelab;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

/** Small Android shell using Frontline's profile, simple screens and preference pattern. */
public class MainActivity extends Activity implements SensorEventListener {
    private Arena arena;
    private SensorManager sensors;
    private Sensor accelerometer, gyroscope;
    private ToneGenerator effects;
    private Object backCallback;
    private boolean resumed;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        sensors = (SensorManager)getSystemService(SENSOR_SERVICE);
        if (sensors != null) {
            accelerometer = sensors.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            gyroscope = sensors.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        }
        arena = new Arena(this, accelerometer != null, gyroscope != null);
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
        try { effects = new ToneGenerator(AudioManager.STREAM_MUSIC, 30); } catch (RuntimeException unavailable) { effects = null; }
        registerMotion(); arena.invalidate();
    }
    @Override protected void onPause() {
        resumed = false; arena.active = false;
        if (arena.screen == Arena.PLAY) arena.screen = Arena.PAUSE;
        arena.clearTouch(); arena.motion.resetSamples(); arena.saveProfile();
        if (sensors != null) sensors.unregisterListener(this);
        if (effects != null) { effects.release(); effects = null; }
        super.onPause();
    }
    @Override public void onBackPressed() {
        // Android 7-12 dispatch here; Android 13+ uses the supported callback.
        navigateBack();
    }
    private void navigateBack() {
        if (arena.screen == Arena.HOME) finish(); else arena.back();
    }
    @Override protected void onDestroy() {
        if (Build.VERSION.SDK_INT >= 33 && backCallback != null) {
            Api33Back.unregister(this, backCallback);
            backCallback = null;
        }
        super.onDestroy();
    }
    /** Keep API 33 class references out of fields verified on older devices. */
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
        if (effects == null || !arena.profile.sound) return;
        int tone = kind == 2 ? ToneGenerator.TONE_PROP_ACK : kind == 3 ? ToneGenerator.TONE_PROP_NACK
                : kind == 1 ? ToneGenerator.TONE_PROP_BEEP2 : ToneGenerator.TONE_PROP_BEEP;
        effects.startTone(tone, kind == 2 ? 140 : kind == 3 ? 100 : kind == 4 ? 25 : 45);
    }
    void stopEffects() { if (effects != null) effects.stopTone(); }
    @Override public void onAccuracyChanged(Sensor sensor, int accuracy) { }
    @Override public void onSensorChanged(SensorEvent event) {
        if (!resumed || !arena.motionEnabled) return;
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
    }

    static final class Arena extends View {
        static final int HOME = 0, PLAY = 1, SETTINGS = 2, HELP = 3, PAUSE = 4, RESULT = 5;
        private static final float TOP = 190, BOTTOM = 620;
        private static final String[] PRESETS = {"LOOSE", "BALANCED", "TIGHT"};
        final Physics physics = new Physics();
        final MotionSteering motion;
        final FalleyProfile profile = new FalleyProfile();
        final MainActivity owner;
        final SharedPreferences storage;
        final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        final Path path = new Path();
        final int mint = 0xff5de0ba, coral = 0xffff816c, gold = 0xfff6d477;
        final int white = 0xfff0f7f7, muted = 0xffa2aba9, background = 0xff17191b, panel = 0xff232629, border = 0xff41494a;
        boolean active, motionEnabled, hasMotion, hasGyro, resultRecorded;
        int screen = HOME, settingsReturn = HOME;
        long last, trailTime, lastLandingCue;
        float accumulator, scale = 1, ox, oy, visualTime, touchOrigin, touchSteering;
        float burstTime = 10, burstX, burstY, zeroMessage;
        int pointer = -1, buttonPointer = -1, safeTop, safeBottom, safeLeft, safeRight, trailIndex;
        final float[] trailX = new float[18], trailY = new float[18];
        final boolean[] trailValid = new boolean[18];
        final ArrayList<Hit> buttons = new ArrayList<>();
        Hit pressed;
        private static final class Hit {
            final String id; final float left, top, right, bottom;
            Hit(String id,float l,float t,float r,float b) { this.id = id; left = l; top = t; right = r; bottom = b; }
            boolean contains(float x,float y) { return x >= left && x <= right && y >= top && y <= bottom; }
        }

        Arena(MainActivity context,boolean hasAccelerometer,boolean hasGyroscope) {
            super(context); owner = context; hasMotion = hasAccelerometer; hasGyro = hasGyroscope;
            storage = context.getSharedPreferences("falley-v1", Context.MODE_PRIVATE);
            profile.motion = storage.getBoolean("motion",true); profile.sound = storage.getBoolean("sound",true);
            profile.haptics = storage.getBoolean("haptics",true); profile.preset = storage.getInt("preset",MotionSteering.BALANCED);
            profile.bestScore = storage.getInt("bestScore",0); profile.wins = storage.getInt("wins",0);
            profile.fastestWin = storage.getFloat("fastestWin",0); profile.sanitize();
            motion = new MotionSteering(hasGyroscope); motion.setPreset(profile.preset);
            motionEnabled = hasMotion && profile.motion;
            setContentDescription("Falley. Physics at play. Play Mode 1, land the human on the bouncy target. "
                    + "Tap to jump and tilt or drag to steer. Settings include motion feel, sound effects and vibration.");
            setFocusable(true);
        }
        void saveProfile() {
            storage.edit().putBoolean("motion",profile.motion).putBoolean("sound",profile.sound)
                    .putBoolean("haptics",profile.haptics).putInt("preset",profile.preset)
                    .putInt("bestScore",profile.bestScore).putInt("wins",profile.wins)
                    .putFloat("fastestWin",profile.fastestWin).apply();
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
            buttons.add(new Hit(id,l,t,r,b));
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
                last = now; visualTime += frameDt; zeroMessage = Math.max(0,zeroMessage-frameDt);
                if (screen == PLAY) advance(frameDt,now);
                else {
                    accumulator = 0;
                    // The loss effect finishes behind the result card without
                    // advancing gameplay while a menu is open.
                    if (screen == RESULT && physics.lost) burstTime += frameDt;
                }
            }
            buttons.clear(); c.drawColor(background); c.save(); c.translate(ox,oy); c.scale(scale,scale);
            if (screen == HOME) drawHome(c);
            else if (screen == SETTINGS) drawSettings(c);
            else if (screen == HELP) drawHelp(c);
            else {
                drawPlay(c);
                if (screen == PAUSE || screen == RESULT) { buttons.clear(); drawOverlay(c); }
            }
            c.restore(); if (active) postInvalidateOnAnimation();
        }
        private void advance(float dt,long now) {
            float oldPulse = physics.landingPulse;
            boolean oldWon = physics.won, oldLost = physics.lost;
            burstTime += dt; accumulator += dt;
            physics.gx = physics.won ? 0 : clamp((motionEnabled ? motion.acceleration() : 0)+touchSteering,-1700,1700);
            while (accumulator >= Physics.DT) { physics.step(); accumulator -= Physics.DT; }
            if (physics.landingPulse > oldPulse+.015f && now-lastLandingCue > 160) {
                cue(4); lastLandingCue = now;
            }
            if (physics.lost && !oldLost) {
                burstX = physics.x; burstY = physics.y; burstTime = 0; clearTouch(); cue(3);
            }
            if (physics.won && !oldWon) { clearTouch(); cue(2); }
            if ((physics.won || physics.lost) && !resultRecorded) {
                if (profile.recordResult(physics.won,physics.score,physics.elapsed)) saveProfile();
                resultRecorded = true;
            }
            if (physics.won && physics.winTime > 1.2f || physics.lost && burstTime > .8f) {
                screen = RESULT; clearTouch();
            }
            if (!physics.lost && now-trailTime > 30) {
                trailTime = now; trailX[trailIndex] = physics.x; trailY[trailIndex] = physics.y;
                trailValid[trailIndex] = true; trailIndex = (trailIndex+1)%trailX.length;
            }
        }
        private void drawHome(Canvas c) {
            text(c,"FALLEY",24,81,45,white); text(c,"Physics at play",26,116,20,mint);
            button(c,"settings",382,20,456,94,"",white,16); gear(c,419,57,white);
            rect(c,24,152,456,392,16,panel);
            for (float y = 181; y < 365; y += 38) line(c,46,y,434,y,0xff2d3438,1);
            line(c,68,195,68,359,coral,3); line(c,412,195,412,359,coral,3);
            path.reset(); path.moveTo(68,248); path.quadTo(240,288,412,248);
            paint.setStyle(Paint.Style.STROKE); paint.setColor(mint); paint.setStrokeWidth(4); c.drawPath(path,paint); paint.setStyle(Paint.Style.FILL);
            float bob = (float)Math.sin(visualTime*3)*7;
            drawHuman(c,242,236+bob,25,-150,0);
            rect(c,68,345,412,364,8,0xff41494a); rect(c,180,345,300,354,4,mint);
            circle(c,240,350,15,0xff29584d); circle(c,240,350,10,mint); circle(c,240,350,4,panel);
            center(c,"A little bounce. A careful landing.",240,382,13,muted);
            rect(c,24,420,456,554,14,mint); buttons.add(new Hit("play",24,420,456,554));
            text(c,"PLAY MODE 1",45,468,27,background); text(c,"Land on the bouncy target.",45,505,17,background);
            line(c,412,479,432,479,background,3); line(c,432,479,422,469,background,3); line(c,432,479,422,489,background,3);
            button(c,"help",24,574,456,648,"HOW TO PLAY",white,17);
            button(c,"settings",24,662,456,736,"SETTINGS",white,17);
            center(c,"BEST "+profile.bestScore+"   /   "+profile.wins+" LANDINGS",240,767,12,muted);
            center(c,"PERF GAMES / V1",240,797,11,muted);
        }
        private void toggleRow(Canvas c,String id,String title,float top,boolean value,boolean available) {
            rect(c,24,top,456,top+74,9,panel); text(c,title,44,top+44,18,available ? white : muted);
            rect(c,376,top+23,436,top+51,14,value && available ? mint : border);
            circle(c,value && available ? 421 : 391,top+37,10,value && available ? background : muted);
            buttons.add(new Hit(id,24,top,456,top+74));
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
            button(c,"zero",24,391,456,465,"ZERO / CENTER MY HOLD",motionEnabled ? white : muted,17);
            text(c,zeroMessage > 0 ? "Centered. Hold comfortably, then tilt to steer." :
                    !hasMotion ? "No sensor available. Drag left or right to steer." :
                    !motionEnabled ? "Motion is off. Drag left or right to steer." :
                    !motion.hasSample() ? "Waiting for a sensor reading..." : "Current hold centers automatically after resume.",24,489,12,muted);
            line(c,24,513,456,513,border,1); text(c,"FEEDBACK",24,537,13,muted);
            toggleRow(c,"sound","Sound Effects",548,profile.sound,true);
            toggleRow(c,"haptics","Vibration",634,profile.haptics,true);
            button(c,"back",24,734,456,808,"DONE",mint,17);
        }
        private void drawHelp(Canvas c) {
            header(c,"HOW TO PLAY"); text(c,"One fall. One bouncy target.",24,157,23,mint);
            helpStep(c,"01","TAP TO JUMP","Tap the arena or the Jump button.",198);
            helpStep(c,"02","TILT OR DRAG","Tilt to steer. Drag horizontally for touch play.",304);
            helpStep(c,"03","LET THE ROPES BOUNCE","Landing bends your knees, then springs you up.",410);
            helpStep(c,"04","LAND ON THE TARGET","Stay clear of the narrowing coral rails.",516);
            text(c,"Weight grows with score and can snap ropes.",24,658,15,muted);
            text(c,"Adjust motion feel or center your hold in Settings.",24,686,14,muted);
            button(c,"back",24,734,456,808,"GOT IT",mint,17);
        }
        private void helpStep(Canvas c,String number,String title,String detail,float y) {
            circle(c,48,y+20,22,panel); center(c,number,48,y+25,14,mint);
            text(c,title,85,y+12,17,white); text(c,detail,24,y+64,13,muted);
            line(c,24,y+83,456,y+83,border,1);
        }
        private void drawPlay(Canvas c) {
            text(c,"FALLEY / MODE 1",24,30,13,mint); text(c,"The first fall",24,72,29,white);
            button(c,"pause",382,20,456,94,"",white,17);
            line(c,413,45,413,69,white,5); line(c,425,45,425,69,white,5);
            text(c,"SCORE "+physics.score,24,108,14,gold);
            text(c,String.format(Locale.US,"WEIGHT %.1fx",physics.mass()),177,108,14,white);
            text(c,"CUTS "+physics.brokenCount,374,108,14,mint);
            text(c,physics.won ? "LANDED! ENJOY THE BOUNCE." : "LAND ON THE BOUNCY TARGET",24,140,17,physics.won ? mint : white);
            float progress = clamp((physics.y-310)/(Physics.GROUND_Y-physics.radius()-310),0,1);
            rect(c,24,153,456,160,3,panel); rect(c,24,153,24+432*progress,160,3,mint);
            text(c,Math.max(0,(int)((Physics.GROUND_Y-physics.y-physics.radius())/10))+"m TO GO / AVOID CORAL RAILS",24,179,12,muted);
            drawArena(c); drawPlayControls(c);
        }
        private void drawArena(Canvas c) {
            c.save(); c.clipRect(20,TOP,460,BOTTOM); rect(c,20,TOP,460,BOTTOM,0,0xff121a20);
            float gridOffset = physics.cameraY%45;
            for (float y = TOP-gridOffset; y <= BOTTOM; y += 45) line(c,20,y,460,y,0xff23313b,1);
            for (int x = 60; x < 460; x += 60) line(c,x,TOP,x,BOTTOM,0xff1b2832,1);
            float left = physics.leftWall(), right = physics.rightWall();
            rect(c,20,TOP,left,BOTTOM,0,0x22ff816c); rect(c,right,TOP,460,BOTTOM,0,0x22ff816c);
            line(c,left,TOP,left,BOTTOM,0x36ff816c,13); line(c,right,TOP,right,BOTTOM,0x36ff816c,13);
            line(c,left,TOP,left,BOTTOM,coral,3); line(c,right,TOP,right,BOTTOM,coral,3);
            for (float y = TOP-28+(visualTime*30)%42; y < BOTTOM+28; y += 42) {
                line(c,left-12,y-8,left-4,y,0x99ff816c,2); line(c,left-4,y,left-12,y+8,0x99ff816c,2);
                line(c,right+12,y-8,right+4,y,0x99ff816c,2); line(c,right+4,y,right+12,y+8,0x99ff816c,2);
            }
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
            if (!physics.lost) {
                for (int k = 0; k < trailX.length; k++) {
                    int i = (trailIndex+k)%trailX.length;
                    if (trailValid[i]) circle(c,trailX[i],trailY[i]-physics.cameraY,1.5f+k*.1f,
                            (coral & 0xffffff)|((k*3)<<24));
                }
                drawHuman(c,physics.x,physics.y-physics.cameraY,physics.radius(),physics.vy,physics.landingPulse);
            }
            drawBurst(c); c.restore();
            line(c,20,TOP,460,TOP,border,1); line(c,20,BOTTOM,460,BOTTOM,border,1);
        }
        private void drawFloor(Canvas c) {
            float y = Physics.GROUND_Y-physics.cameraY;
            if (y > BOTTOM+24 || y < TOP-50) return;
            float squash = physics.groundBounce*6;
            rect(c,24,y+squash,456,y+34,10,border);
            rect(c,Physics.TARGET_LEFT,y+squash,Physics.TARGET_RIGHT,y+12,5,mint);
            line(c,Physics.TARGET_LEFT,y-3,Physics.TARGET_RIGHT,y-3,0x885de0ba,4);
            for (int i = 0; i < 5; i++) rect(c,180+i*25,y+18,190+i*25,y+23,2,0xff273f38);
            float middle = (Physics.TARGET_LEFT+Physics.TARGET_RIGHT)/2;
            circle(c,middle,y+7,16,0xff29584d); circle(c,middle,y+7,11,mint); circle(c,middle,y+7,4,background);
            center(c,"LAND HERE",middle,y-21,13,mint);
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
        private void drawBurst(Canvas c) {
            if (burstTime >= 1.6f) return;
            float t = burstTime, alpha = Math.max(0,1-t/1.6f);
            for (int i = 0; i < 24; i++) {
                double a = i*2.399963; float speed = 65+(i%7)*24;
                int color = i%3 == 0 ? gold : i%3 == 1 ? coral : 0xff65b9f2;
                circle(c,burstX+(float)Math.cos(a)*speed*t,burstY-physics.cameraY+(float)Math.sin(a)*speed*t+75*t*t,
                        Math.max(1,4*alpha),(color & 0xffffff)|((int)(alpha*220)<<24));
            }
        }
        private String motionStatus() {
            return !hasMotion ? "NO MOTION SENSOR / DRAG TO STEER" : !motionEnabled ? "MOTION OFF / DRAG TO STEER" :
                    !motion.hasSample() ? "WAITING FOR SENSOR / DRAG TO STEER" : zeroMessage > 0 ? "CENTERED / TILT LEFT OR RIGHT" :
                    hasGyro ? "GYRO + ACCEL / ZERO CENTERS YOUR HOLD" : "ACCEL ONLY / ZERO CENTERS YOUR HOLD";
        }
        private void drawPlayControls(Canvas c) {
            text(c,motionStatus(),24,641,11,muted);
            button(c,"motion",24,660,184,734,hasMotion ? "MOTION: "+(motionEnabled ? "ON" : "OFF") : "MOTION: N/A",
                    hasMotion ? mint : muted,15);
            button(c,"cycle_preset",194,660,352,734,"FEEL: "+PRESETS[profile.preset],gold,13);
            button(c,"zero",362,660,456,734,"ZERO",motionEnabled ? white : muted,15);
            button(c,"restart",24,742,218,816,"RESTART",coral,17);
            button(c,"jump",234,742,456,816,physics.lost || physics.won ? "JUMP" : physics.jumpRemaining() > 0 ? "JUMP ..." : "JUMP ↑",
                    physics.lost || physics.won ? muted : gold,20);
            float progress = 1-clamp(physics.jumpRemaining()/.30f,0,1);
            rect(c,253,802,437,806,2,0x44f6d477); rect(c,253,802,253+184*progress,806,2,gold);
        }
        private void drawOverlay(Canvas c) {
            rect(c,0,0,480,820,0,0xb317191b); rect(c,52,220,428,675,17,panel);
            if (screen == PAUSE) {
                center(c,"PAUSED",240,267,29,white);
                button(c,"resume",74,292,406,366,"RESUME",mint,18);
                button(c,"restart",74,380,406,454,"RESTART",white,18);
                button(c,"settings",74,468,406,542,"SETTINGS",white,18);
                button(c,"home",74,556,406,630,"HOME",muted,18);
            } else {
                center(c,physics.won ? "LANDED!" : "FALL ENDED",240,280,35,physics.won ? mint : coral);
                center(c,physics.won ? "A soft landing. A good bounce." : "Keep clear of the side rails.",240,319,15,white);
                center(c,"SCORE "+physics.score,240,371,27,gold);
                center(c,String.format(Locale.US,"%.1fs / %d ROPES CUT",physics.elapsed,physics.brokenCount),240,404,14,muted);
                center(c,"BEST "+profile.bestScore+" / "+profile.wins+" LANDINGS",240,435,12,muted);
                button(c,"restart",74,468,406,542,"PLAY AGAIN",mint,18);
                button(c,"home",74,556,406,630,"HOME",white,18);
            }
        }
        private void cue(int kind) {
            if (profile.haptics) performHapticFeedback(kind == 2 || kind == 3 ? HapticFeedbackConstants.LONG_PRESS :
                    kind == 0 ? HapticFeedbackConstants.KEYBOARD_TAP : HapticFeedbackConstants.VIRTUAL_KEY);
            owner.effect(kind);
        }
        void clearTouch() { pointer = buttonPointer = -1; pressed = null; touchSteering = 0; physics.gx = 0; }
        private void restart() {
            physics.reset(); clearTouch(); accumulator = 0; Arrays.fill(trailValid,false); trailIndex = 0;
            burstTime = 10; resultRecorded = false; lastLandingCue = 0; screen = PLAY;
        }
        private void jump() { if (screen == PLAY && !physics.won && physics.jump()) cue(1); }
        void back() {
            if (screen == PLAY) screen = PAUSE;
            else if (screen == PAUSE) screen = PLAY;
            else if (screen == SETTINGS) screen = settingsReturn;
            else screen = HOME;
            clearTouch(); accumulator = 0; invalidate();
        }
        void activate(String id) {
            if (id.equals("play") || id.equals("restart")) restart();
            else if (id.equals("home")) { screen = HOME; clearTouch(); }
            else if (id.equals("help")) { screen = HELP; clearTouch(); }
            else if (id.equals("settings")) { settingsReturn = screen == HOME ? HOME : PAUSE; screen = SETTINGS; clearTouch(); }
            else if (id.equals("back")) back();
            else if (id.equals("pause")) { screen = PAUSE; clearTouch(); }
            else if (id.equals("resume")) { screen = PLAY; clearTouch(); }
            else if (id.equals("jump")) { jump(); return; }
            else if (id.equals("motion")) owner.toggleMotion();
            else if (id.startsWith("preset_") || id.equals("cycle_preset")) {
                profile.preset = id.equals("cycle_preset") ? (profile.preset+1)%3 : Integer.parseInt(id.substring(7));
                motion.setPreset(profile.preset); saveProfile();
            } else if (id.equals("zero")) { if (motionEnabled && motion.calibrate()) zeroMessage = 2; }
            else if (id.equals("sound")) { profile.sound = !profile.sound; if (!profile.sound) owner.stopEffects(); saveProfile(); }
            else if (id.equals("haptics")) { profile.haptics = !profile.haptics; saveProfile(); }
            cue(0); accumulator = 0; invalidate();
        }
        @Override public boolean performClick() { super.performClick(); return true; }
        @Override public boolean onTouchEvent(MotionEvent event) {
            int action = event.getActionMasked(), index = event.getActionIndex();
            float x = (event.getX(index)-ox)/scale, y = (event.getY(index)-oy)/scale;
            if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) {
                for (int i = buttons.size()-1; i >= 0; i--) if (buttons.get(i).contains(x,y)) {
                    if (buttonPointer < 0) { pressed = buttons.get(i); buttonPointer = event.getPointerId(index); }
                    return true;
                }
                if (screen == PLAY && pointer < 0 && !physics.lost && !physics.won && x >= 20 && x <= 460 && y >= TOP && y <= BOTTOM) {
                    pointer = event.getPointerId(index); touchOrigin = x; touchSteering = 0; jump();
                }
            } else if (action == MotionEvent.ACTION_MOVE && pointer >= 0) {
                int current = event.findPointerIndex(pointer);
                if (current >= 0) touchSteering = clamp(((event.getX(current)-ox)/scale-touchOrigin)*10,-850,850);
                else clearTouch();
            } else if (action == MotionEvent.ACTION_CANCEL) clearTouch();
            else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP) {
                int id = event.getPointerId(index);
                if (id == buttonPointer) {
                    Hit hit = pressed; pressed = null; buttonPointer = -1;
                    if (hit != null && hit.contains(x,y)) { activate(hit.id); performClick(); }
                }
                if (id == pointer) { pointer = -1; touchSteering = 0; physics.gx = 0; performClick(); }
            }
            return true;
        }
        private static float clamp(float value,float low,float high) { return Math.max(low,Math.min(high,value)); }
    }
}
