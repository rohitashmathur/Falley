package com.perfgames.ropelab;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import java.util.Arrays;
import java.util.Locale;

/** Android shell. Physics and motion filtering stay portable for deterministic tests. */
public class MainActivity extends Activity implements SensorEventListener {
    private Arena arena;
    private SensorManager sensors;
    private Sensor accelerometer, gyroscope;
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
            arena.safeTop = insets.getSystemWindowInsetTop();
            arena.safeBottom = insets.getSystemWindowInsetBottom();
            arena.safeLeft = insets.getSystemWindowInsetLeft();
            arena.safeRight = insets.getSystemWindowInsetRight();
            arena.viewport();
            return insets;
        });
        arena.requestApplyInsets();
    }
    @Override protected void onResume() {
        super.onResume(); resumed = true; arena.active = true;
        arena.last = 0L; arena.accumulator = 0f; arena.motion.resetSamples();
        registerMotion(); arena.invalidate();
    }
    @Override protected void onPause() {
        resumed = false; arena.active = false; arena.clearTouch(); arena.motion.resetSamples();
        if (sensors != null) sensors.unregisterListener(this);
        super.onPause();
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
        arena.motionEnabled = !arena.motionEnabled; arena.motion.resetSamples(); arena.physics.gx = 0f;
        getSharedPreferences("rope-mode1", 0).edit().putBoolean("motion", arena.motionEnabled).apply();
        registerMotion();
    }
    @Override public void onAccuracyChanged(Sensor sensor, int accuracy) { }
    @Override public void onSensorChanged(SensorEvent event) {
        if (!resumed || !arena.motionEnabled) return;
        float x = event.values[0], y = event.values[1], z = event.values[2];
        // Raw axes stay tied to natural orientation. Remap to portrait screen
        // axes, including landscape-native tablets locked in portrait.
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
        private static final float TOP = 190f, BOTTOM = 620f;
        private static final String[] PRESETS = {"LOOSE", "BALANCED", "TIGHT"};
        final Physics physics = new Physics();
        final MotionSteering motion;
        final MainActivity owner;
        final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        final Path path = new Path();
        final int mint = Color.rgb(67,244,203), pink = Color.rgb(255,88,151);
        final int gold = Color.rgb(255,225,124), white = Color.rgb(230,239,255);
        final int muted = Color.rgb(145,162,190), background = Color.rgb(8,13,26);
        boolean active, motionEnabled, hasMotion, hasGyro, wasLost;
        long last, trailTime;
        float accumulator, scale = 1f, ox, oy, visualTime, touchOrigin, touchSteering;
        float burstTime = 10f, burstX, burstY, zeroMessage;
        int pointer = -1, best, safeTop, safeBottom, safeLeft, safeRight, trailIndex;
        final float[] trailX = new float[24], trailY = new float[24];
        final boolean[] trailValid = new boolean[24];

        Arena(MainActivity context, boolean hasAccelerometer, boolean hasGyroscope) {
            super(context); owner = context; hasMotion = hasAccelerometer; hasGyro = hasGyroscope;
            motion = new MotionSteering(hasGyroscope);
            motion.setPreset(context.getSharedPreferences("rope-mode1",0).getInt("preset",MotionSteering.BALANCED));
            motionEnabled = hasMotion && context.getSharedPreferences("rope-mode1",0).getBoolean("motion",true);
            best = context.getSharedPreferences("rope-mode1",0).getInt("best",0);
            setContentDescription("Rope Lab, Mode 1. Tap the arena or Jump to bounce. Tilt to steer, "
                    + "or drag horizontally while touching. Avoid the narrowing pink rails. "
                    + "Motion, sensor sensitivity, zero calibration, restart and jump controls are below.");
            setFocusable(true);
        }
        @Override protected void onSizeChanged(int w,int h,int oldW,int oldH) { viewport(); }
        void viewport() {
            int width = getWidth()-safeLeft-safeRight, height = getHeight()-safeTop-safeBottom;
            if (width <= 0 || height <= 0) return;
            scale = Math.min(width/480f,height/820f);
            ox = safeLeft+(width-480f*scale)/2f; oy = safeTop+(height-820f*scale)/2f;
        }
        private void text(Canvas c,String label,float x,float y,float size,int color) {
            paint.setStyle(Paint.Style.FILL); paint.setColor(color); paint.setTextSize(size);
            paint.setTypeface(Typeface.create("sans-serif",Typeface.BOLD)); c.drawText(label,x,y,paint);
        }
        private void line(Canvas c,float x,float y,float x2,float y2,int color,float width) {
            paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(width);
            paint.setStrokeCap(Paint.Cap.ROUND); paint.setColor(color); c.drawLine(x,y,x2,y2,paint);
            paint.setStyle(Paint.Style.FILL);
        }
        private void circle(Canvas c,float x,float y,float r,int color) {
            paint.setStyle(Paint.Style.FILL); paint.setColor(color); c.drawCircle(x,y,r,paint);
        }
        private void button(Canvas c,float left,float top,float right,float bottom,String label,int color,float textSize) {
            paint.setColor(Color.rgb(23,34,52)); c.drawRoundRect(left,top,right,bottom,13f,13f,paint);
            paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(1.5f); paint.setColor(color);
            c.drawRoundRect(left,top,right,bottom,13f,13f,paint); paint.setStyle(Paint.Style.FILL);
            paint.setTextAlign(Paint.Align.CENTER);
            text(c,label,(left+right)/2f,(top+bottom)/2f+textSize*.35f,textSize,color);
            paint.setTextAlign(Paint.Align.LEFT);
        }
        @Override protected void onDraw(Canvas c) {
            long now = SystemClock.uptimeMillis();
            float frameDt = last == 0L ? 0f : Math.min((now-last)/1000f,.05f);
            if (active) {
                last = now; visualTime += frameDt; zeroMessage = Math.max(0f,zeroMessage-frameDt);
                burstTime += frameDt; accumulator += frameDt;
                physics.gx = clamp((motionEnabled ? motion.acceleration() : 0f)+touchSteering,-1700f,1700f);
                while (accumulator >= Physics.DT) { physics.step(); accumulator -= Physics.DT; }
            }
            if (physics.lost && !wasLost) { burstX = physics.x; burstY = physics.y; burstTime = 0f; clearTouch(); }
            wasLost = physics.lost;
            if (physics.score > best) {
                best = physics.score; getContext().getSharedPreferences("rope-mode1",0).edit().putInt("best",best).apply();
            }
            if (!physics.lost && now-trailTime > 26L) {
                trailTime = now; trailX[trailIndex] = physics.x; trailY[trailIndex] = physics.y;
                trailValid[trailIndex] = true; trailIndex = (trailIndex+1)%trailX.length;
            }
            c.drawColor(background); c.save(); c.translate(ox,oy); c.scale(scale,scale);
            text(c,"PERF GAMES / MODE 01",24,29,13,mint); text(c,"ROPE LAB",24,70,34,white);
            paint.setTextAlign(Paint.Align.RIGHT);
            text(c,String.format(Locale.US,"%04d",physics.score),456,70,33,gold);
            text(c,"POINTS / BEST "+best,456,89,11,muted); paint.setTextAlign(Paint.Align.LEFT);
            text(c,String.format(Locale.US,"WEIGHT %.1fx",physics.mass()),24,110,14,gold);
            text(c,"DEPTH "+Math.max(0,(int)((physics.y-310)/10))+"m",198,110,14,white);
            text(c,"CUTS "+physics.brokenCount,374,110,14,mint);
            text(c,"Tap to jump. Tilt to steer.",24,145,20,white);
            text(c,"Get heavier. Slice ropes. Avoid pink rails.",24,168,13,muted);
            drawArena(c); drawControls(c); c.restore(); if (active) postInvalidateOnAnimation();
        }
        private void drawArena(Canvas c) {
            c.save(); c.clipRect(20f,TOP,460f,BOTTOM);
            paint.setColor(Color.rgb(10,18,33)); c.drawRect(20f,TOP,460f,BOTTOM,paint);
            float gridOffset = physics.cameraY%45f;
            for (float y = TOP-gridOffset; y <= BOTTOM; y += 45f) line(c,20f,y,460f,y,Color.rgb(21,33,51),1f);
            for (int x = 60; x < 460; x += 60) line(c,x,TOP,x,BOTTOM,Color.rgb(17,29,45),1f);
            float left = physics.leftWall(), right = physics.rightWall();
            paint.setColor(0x22ff5897); c.drawRect(20,TOP,left,BOTTOM,paint); c.drawRect(right,TOP,460,BOTTOM,paint);
            line(c,left,TOP,left,BOTTOM,0x36ff5897,15f); line(c,right,TOP,right,BOTTOM,0x36ff5897,15f);
            line(c,left,TOP,left,BOTTOM,pink,3f); line(c,right,TOP,right,BOTTOM,pink,3f);
            for (float y = TOP-28+(visualTime*30)%42f; y < BOTTOM+28; y += 42f) {
                line(c,left-12,y-8,left-4,y,0x99ff5897,2f); line(c,left-4,y,left-12,y+8,0x99ff5897,2f);
                line(c,right+12,y-8,right+4,y,0x99ff5897,2f); line(c,right+4,y,right+12,y+8,0x99ff5897,2f);
            }
            for (int j = 0; j < physics.ropes.length; j++) {
                Physics.Rope rope = physics.ropes[j]; int color = j%2 == 0 ? mint : Color.rgb(132,134,255);
                float tension = clamp(rope.tension,0f,1f);
                path.reset(); path.moveTo(rope.x[0],rope.y[0]-physics.cameraY);
                for (int i = 1; i < rope.x.length; i++) {
                    if (i-1 == rope.cut) path.moveTo(rope.x[i],rope.y[i]-physics.cameraY);
                    else path.lineTo(rope.x[i],rope.y[i]-physics.cameraY);
                }
                paint.setStyle(Paint.Style.STROKE); paint.setStrokeCap(Paint.Cap.ROUND); paint.setStrokeJoin(Paint.Join.ROUND);
                paint.setColor((color & 0xffffff)|0x20000000); paint.setStrokeWidth(11f+5f*tension); c.drawPath(path,paint);
                paint.setColor(rope.cut >= 0 ? (color & 0xffffff)|0x88000000 : color);
                paint.setStrokeWidth(3f+2f*tension); c.drawPath(path,paint); paint.setStyle(Paint.Style.FILL);
                circle(c,rope.x[0],rope.y[0]-physics.cameraY,5f,color); int end = rope.x.length-1;
                circle(c,rope.x[end],rope.y[end]-physics.cameraY,5f,color);
                if (rope.cut >= 0) for (int i = rope.cut; i <= rope.cut+1 && i < rope.x.length; i++) {
                    circle(c,rope.x[i],rope.y[i]-physics.cameraY,7f,0x55ffe17c);
                    circle(c,rope.x[i],rope.y[i]-physics.cameraY,3.5f,gold);
                }
            }
            if (!physics.lost) {
                for (int k = 0; k < trailX.length; k++) {
                    int index = (trailIndex+k)%trailX.length;
                    if (trailValid[index]) circle(c,trailX[index],trailY[index]-physics.cameraY,2f+k*.22f,
                            (gold & 0xffffff)|((k*4)<<24));
                }
                float y = physics.y-physics.cameraY, radius = physics.radius();
                circle(c,physics.x,y,radius+7f,0x22ffe17c); circle(c,physics.x,y,radius,gold);
                circle(c,physics.x-5f,y-3f,2.3f,background); circle(c,physics.x+5f,y-3f,2.3f,background);
                line(c,physics.x-4f,y+5f,physics.x+4f,y+5f,background,2f);
                if (physics.flash > 0f) {
                    paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(2f);
                    paint.setColor((gold & 0xffffff)|((int)(clamp(physics.flash,0,1)*160)<<24));
                    c.drawCircle(physics.x,y,radius+8f+(1f-physics.flash)*24f,paint); paint.setStyle(Paint.Style.FILL);
                }
            }
            drawBlast(c);
            if (physics.lost) {
                paint.setColor(0xe9080d1a); c.drawRoundRect(52,330,428,466,18,18,paint);
                paint.setTextAlign(Paint.Align.CENTER); text(c,"BALL BLASTED",240,373,26,pink);
                text(c,physics.deathReason,240,405,15,white);
                text(c,"Score "+physics.score+"  /  Tap Restart",240,439,17,gold); paint.setTextAlign(Paint.Align.LEFT);
            }
            c.restore(); line(c,20,TOP,460,TOP,Color.rgb(38,53,76),1f); line(c,20,BOTTOM,460,BOTTOM,Color.rgb(38,53,76),1f);
        }
        private void drawBlast(Canvas c) {
            if (burstTime >= 1.6f) return;
            float t = burstTime, alpha = Math.max(0,1f-t/1.6f);
            int color = (pink & 0xffffff)|((int)(alpha*230)<<24);
            for (int i = 0; i < 28; i++) {
                double angle = i*2.399963; float speed = 65f+(i%7)*24f;
                float x = burstX+(float)Math.cos(angle)*speed*t;
                float y = burstY-physics.cameraY+(float)Math.sin(angle)*speed*t+75*t*t;
                circle(c,x,y,Math.max(1f,4f*alpha),i%3 == 0 ? (gold & 0xffffff)|((int)(alpha*230)<<24) : color);
            }
            paint.setStyle(Paint.Style.STROKE); paint.setColor(color); paint.setStrokeWidth(2f);
            c.drawCircle(burstX,burstY-physics.cameraY,15f+t*100f,paint); paint.setStyle(Paint.Style.FILL);
        }
        private void drawControls(Canvas c) {
            String sensorStatus = !hasMotion ? "NO MOTION SENSOR / DRAG TO STEER" :
                    !motionEnabled ? "MOTION OFF / DRAG TO STEER" :
                    !motion.hasSample() ? "WAITING FOR SENSOR / DRAG TO STEER" :
                    zeroMessage > 0f ? "NEUTRAL SET / TILT LEFT OR RIGHT" :
                    hasGyro ? "GYRO + ACCEL / ZERO CENTERS YOUR HOLD" : "ACCEL ONLY / ZERO CENTERS YOUR HOLD";
            text(c,sensorStatus,24,641,11,muted);
            button(c,24,660,184,734,hasMotion ? "MOTION: "+(motionEnabled ? "ON" : "OFF") : "MOTION: N/A",
                    hasMotion ? mint : muted,15);
            button(c,194,660,352,734,"FEEL: "+PRESETS[motion.preset()],gold,13);
            button(c,362,660,456,734,"ZERO",hasMotion && motionEnabled ? white : muted,15);
            button(c,24,742,218,816,"RESTART",pink,17);
            button(c,234,742,456,816,physics.lost ? "JUMP" : physics.jumpRemaining() > 0f ? "JUMP ..." : "JUMP ↑",
                    physics.lost ? muted : gold,20);
            float progress = 1f-clamp(physics.jumpRemaining()/.30f,0f,1f);
            paint.setColor(0x44ffe17c); c.drawRoundRect(253,802,437,806,2,2,paint);
            paint.setColor(gold); c.drawRoundRect(253,802,253+184*progress,806,2,2,paint);
        }
        void clearTouch() { pointer = -1; touchSteering = 0f; physics.gx = 0f; }
        private void restart() {
            physics.reset(); clearTouch(); accumulator = 0f; Arrays.fill(trailValid,false); trailIndex = 0;
            burstTime = 10f; wasLost = false;
        }
        private void jump() { if (!physics.lost) physics.jump(); }
        @Override public boolean performClick() { super.performClick(); return true; }
        @Override public boolean onTouchEvent(MotionEvent event) {
            int action = event.getActionMasked(), index = event.getActionIndex();
            float x = (event.getX(index)-ox)/scale, y = (event.getY(index)-oy)/scale;
            if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) {
                if (y >= 660 && y <= 734) {
                    if (x >= 24 && x <= 184) owner.toggleMotion();
                    else if (x >= 194 && x <= 352) {
                        motion.setPreset((motion.preset()+1)%3);
                        getContext().getSharedPreferences("rope-mode1",0).edit().putInt("preset",motion.preset()).apply();
                    } else if (x >= 362 && x <= 456 && motionEnabled && motion.calibrate()) zeroMessage = 2f;
                    performClick(); return true;
                }
                if (y >= 742 && y <= 816) {
                    if (x >= 24 && x <= 218) restart(); else if (x >= 234 && x <= 456) jump();
                    performClick(); return true;
                }
                if (pointer < 0 && !physics.lost && x >= 20 && x <= 460 && y >= TOP && y <= BOTTOM) {
                    pointer = event.getPointerId(index); touchOrigin = x; touchSteering = 0f; jump();
                }
            } else if (action == MotionEvent.ACTION_MOVE && pointer >= 0) {
                int current = event.findPointerIndex(pointer);
                if (current >= 0) touchSteering = clamp(((event.getX(current)-ox)/scale-touchOrigin)*10f,-850f,850f);
                else clearTouch();
            } else if (action == MotionEvent.ACTION_CANCEL ||
                    (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP)
                    && event.getPointerId(index) == pointer) {
                clearTouch(); if (action == MotionEvent.ACTION_UP) performClick();
            }
            return true;
        }
        private static float clamp(float value,float low,float high) { return Math.max(low,Math.min(high,value)); }
    }
}
