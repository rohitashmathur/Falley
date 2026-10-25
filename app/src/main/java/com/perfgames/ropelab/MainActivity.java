package com.perfgames.ropelab;

import android.app.Activity;
import android.content.Context;
import android.content.ActivityNotFoundException;
import android.content.Intent;
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
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.LocaleSpan;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityNodeProvider;
import android.widget.Toast;
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
        if(isLargeDisplay())setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_FULL_USER);
        sensors = (SensorManager)getSystemService(SENSOR_SERVICE);
        if (sensors != null) {
            accelerometer = sensors.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            gyroscope = sensors.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        }
        arena = new Arena(this, accelerometer != null, gyroscope != null);
        audio = new FalleyAudio(this);
        if (state != null) {arena.restoreMenu();if(arena.screen==Arena.HOME){arena.selectHomeChapter(state.getInt("homeChapter",arena.chapterPage));int selected=state.getInt("homeStage",arena.homeStage);if(arena.profile.isStageUnlocked(selected)&&selected/4==arena.chapterPage)arena.homeStage=selected;}}
        setContentView(arena);
        arena.setOnApplyWindowInsetsListener((view, insets) -> {
            if(Build.VERSION.SDK_INT>=30){
                int[] safe=Api30Insets.safe(insets);arena.safeLeft=safe[0];arena.safeTop=safe[1];arena.safeRight=safe[2];arena.safeBottom=safe[3];
            }else{
                arena.safeTop=insets.getSystemWindowInsetTop();arena.safeBottom=insets.getSystemWindowInsetBottom();
                arena.safeLeft=insets.getSystemWindowInsetLeft();arena.safeRight=insets.getSystemWindowInsetRight();
            }
            arena.viewport();arena.invalidate();return insets;
        });
        arena.requestApplyInsets();
        if (Build.VERSION.SDK_INT >= 33) backCallback = Api33Back.register(this, this::navigateBack);
    }
    @Override protected void onResume() {
        super.onResume(); resumed = true; arena.active = true; arena.last = 0;
        arena.accumulator = 0; arena.motion.resetSamples();
        arena.soundCue.reset(arena.physics.soundSequence);
        audio.setEnabled(arena.profile.sound); audio.setMusicEnabled(arena.profile.music); updateMusic(); audio.setForeground(true);
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
    @Override public void onConfigurationChanged(android.content.res.Configuration configuration){
        super.onConfigurationChanged(configuration);
        if(arena==null)return;
        if(arena.screen==Arena.PLAY){stopEffects();arena.screen=Arena.PAUSE;}
        arena.clearTouch();arena.motion.resetSamples();arena.accumulator=0;arena.last=0;
        arena.viewport();arena.invalidate();updateMusic();
    }
    @Override protected void onSaveInstanceState(Bundle state) {
        state.putBoolean("falleyStarted",true);state.putInt("homeChapter",arena.chapterPage);state.putInt("homeStage",arena.homeStage);
        super.onSaveInstanceState(state);
    }
    @Override public void onBackPressed() {
        // Android 7-12 dispatch here; Android 13+ uses the supported callback.
        navigateBack();
    }
    private void navigateBack() {
        if (arena.screen == Arena.HOME || arena.screen == Arena.LANGUAGE && !arena.profile.languageConfirmed) finish(); else arena.back();
    }
    @Override protected void onDestroy() {
        if (audio != null) audio.release();
        if (Build.VERSION.SDK_INT >= 33 && backCallback != null) {
            Api33Back.unregister(this, backCallback);
            backCallback = null;
        }
        super.onDestroy();
    }
    /** Current portrait app bounds may already be letterboxed; inspect the unrestricted display. */
    private boolean isLargeDisplay(){
        if(Build.VERSION.SDK_INT>=30)return Api30Insets.largeDisplay(this);
        android.util.DisplayMetrics display=new android.util.DisplayMetrics();
        getWindowManager().getDefaultDisplay().getRealMetrics(display);
        return Math.min(display.widthPixels,display.heightPixels)/display.density>=600;
    }
    /** Reserve status/task/caption bars, cutouts and mandatory touch regions on modern windows. */
    @android.annotation.TargetApi(30)
    private static final class Api30Insets {
        static boolean largeDisplay(Activity activity){
            Rect bounds=activity.getWindowManager().getMaximumWindowMetrics().getBounds();
            return Math.min(bounds.width(),bounds.height())/activity.getResources().getDisplayMetrics().density>=600;
        }
        static int[] safe(android.view.WindowInsets insets){
            int types=android.view.WindowInsets.Type.systemBars()|android.view.WindowInsets.Type.displayCutout()
                    |android.view.WindowInsets.Type.tappableElement()|android.view.WindowInsets.Type.mandatorySystemGestures();
            android.graphics.Insets safe=android.graphics.Insets.max(insets.getInsets(types),insets.getInsetsIgnoringVisibility(types));
            return new int[]{safe.left,safe.top,safe.right,safe.bottom};
        }
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
        arena.motion.resetSamples(); arena.clearTouch(); arena.physics.vx = 0; arena.saveProfile(); registerMotion();
    }
    void effect(int kind) {
        if (audio != null && arena.profile.sound) audio.generic(kind);
    }
    void hazardEffect(int kind) {
        if (audio != null && resumed && arena.active && arena.screen == Arena.PLAY && arena.profile.sound) audio.hazard(kind);
    }
    void stopEffects() { if (audio != null) audio.stopAll(); }
    void setSoundEnabled(boolean enabled) { if (audio != null) audio.setEnabled(enabled); }
    void setMusicEnabled(boolean enabled) { if(audio!=null)audio.setMusicEnabled(enabled); }
    void updateMusic() {
        boolean pausedMenu=arena.settingsReturn==Arena.PAUSE && (arena.screen==Arena.SETTINGS||arena.screen==Arena.LANGUAGE||arena.screen==Arena.PRIVACY||arena.screen==Arena.HELP);
        if(audio!=null)audio.setMusicPlaying(arena.active && arena.profile.languageConfirmed && arena.screen!=Arena.SPLASH && arena.screen!=Arena.PAUSE && !pausedMenu);
    }
    void openPrivacyLink(boolean email) {
        Intent intent = email ? new Intent(Intent.ACTION_SENDTO,Uri.fromParts("mailto",PrivacyPolicy.CONTACT_EMAIL,null))
                : new Intent(Intent.ACTION_VIEW,Uri.parse(PrivacyPolicy.PUBLIC_URL));
        try { startActivity(intent); }
        catch (ActivityNotFoundException | SecurityException unavailable) {
            Toast.makeText(this,FalleyStrings.text(arena.profile.language,"No app is available to open this link."),Toast.LENGTH_LONG).show();
        }
    }
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
        static final int HOME = 0, PLAY = 1, SETTINGS = 2, HELP = 3, PAUSE = 4, RESULT = 5, SPLASH = 6, CHAPTERS = 7, ARCADE_SETUP = 8, PRIVACY = 9, LANGUAGE = 10, DAILY = 11, WARDROBE = 12, BRIEF = 13, UNLOCK = 14;
        private static final float TOP = 160;
        private float BOTTOM=730;
        ViewportLayout layout=ViewportLayout.create(480,820,0,0,0,0,1,false);
        int homeStage=-1;
        private static final float NAV_DIVIDER = 724, NAV_TOP = 734, CONTENT_BOTTOM = 700;
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
        final int mint = 0xff70e2c2, coral = 0xffff816c, gold = 0xfff6d477;
        final int white = 0xffedf5f2, muted = 0xffa8b9b4, background = 0xff171b1c, panel = 0xff232b2c, border = 0xff374744;
        boolean active, motionEnabled, hasMotion, hasGyro, resultRecorded;
        int screen = SPLASH, settingsReturn = HOME, helpReturn = SETTINGS, briefReturn = HOME;
        int chapterPage, closetTab, runControl;
        int resultStarsBefore, resultStarsThisRun, resultStarsNew;
        String contextCue = "";
        long runDailyDay = DailyDrop.NO_DAY;
        boolean dailyClockValid = true, runMirror, steeredThisRun;
        int helpPage;
        boolean helpMotion;
        int chapterSwipePointer = -1;
        float chapterSwipeX;
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
        final CountryRules.Hint countryHint;
        boolean practiceExpanded;
        float avatarPreviewTime=2, unlockTime;
        int rewardNextStage=-1;
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
            ProfileStorage.load(storage,profile); ProfileStorage.save(storage,profile);
            countryHint=CountryDetector.detect(context);
            physics.configureAvatar(profile.selectedAvatar); physics.reset(profile.selectedStage); chapterPage = profile.selectedStage/4;
            motion = new MotionSteering(hasGyroscope); motion.setPreset(profile.preset);
            motionEnabled = hasMotion && profile.motion;
            setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
            setFocusable(true);
        }
        boolean saveProfile() { return ProfileStorage.save(storage,profile); }
        @Override protected void onSizeChanged(int w,int h,int oldW,int oldH) { viewport(); }
        void viewport() {
            if(getWidth()<=0||getHeight()<=0)return;
            boolean gameplay=screen==PLAY||screen==PAUSE||screen==RESULT;
            layout=ViewportLayout.create(getWidth(),getHeight(),safeLeft,safeTop,safeRight,safeBottom,getResources().getDisplayMetrics().density,gameplay);
            scale=layout.scale;ox=layout.left;oy=layout.top;BOTTOM=layout.arenaBottom;
        }
        private int section(Canvas c,float shift){int mark=buttons.size();c.save();c.translate(0,shift);return mark;}
        private void endSection(Canvas c,int mark,float shift){
            c.restore();for(int i=mark;i<buttons.size();i++){Hit h=buttons.get(i);buttons.set(i,new Hit(h.id,h.left,h.top+shift,h.right,h.bottom+shift,h.label,h.enabled,h.checkable,h.checked));}
        }
        private String t(String english,Object... args) { return ReleaseStrings.text(profile.language,english,args); }
        private Locale languageLocale() { return profile.language == FalleyStrings.HINDI ? new Locale("hi") : profile.language == FalleyStrings.INDONESIAN ? new Locale("id") : Locale.ENGLISH; }
        private CharSequence spoken(String value) {
            SpannableString result = new SpannableString(value);
            result.setSpan(new LocaleSpan(languageLocale()),0,result.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            return result;
        }
        private void text(Canvas c,String label,float x,float y,float size,int color) {
            label = t(label);
            paint.setTextLocale(languageLocale());
            paint.setStyle(Paint.Style.FILL); paint.setColor(color); paint.setTextSize(size);
            paint.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));
            float available = paint.getTextAlign() == Paint.Align.CENTER ? 2*Math.min(x-24,456-x) :
                    paint.getTextAlign() == Paint.Align.RIGHT ? x-24 : 456-x;
            if (available > 0 && paint.measureText(label) > available) paint.setTextSize(size*Math.max(.5f,available/paint.measureText(label)));
            c.drawText(label,x,y,paint);
        }
        private void fitText(Canvas c,String label,float x,float y,float size,int color,float width) {
            label = t(label); paint.setTypeface(Typeface.create("sans-serif",Typeface.BOLD)); paint.setTextSize(size);
            float fitted = paint.measureText(label) > width ? size*width/paint.measureText(label) : size;
            text(c,label,x,y,fitted,color);
        }
        private void center(Canvas c,String label,float x,float y,float size,int color) {
            paint.setTextAlign(Paint.Align.CENTER);
            if (screen == PAUSE || screen == RESULT) fitText(c,label,x,y,size,color,350);
            else text(c,label,x,y,size,color);
            paint.setTextAlign(Paint.Align.LEFT);
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

        private void button(Canvas c,String id,float l,float top,float r,float b,String label,int color,float size) {
            boolean enabled = !id.equals("jump") || physics.canJump();
            boolean choice = id.startsWith("preset_") || id.startsWith("difficulty_") || id.startsWith("language_")&&!id.equals("language_done") || id.startsWith("closet_tab_");
            boolean checked = id.startsWith("preset_") ? id.equals("preset_"+profile.preset) : id.startsWith("language_") ? id.equals("language_"+profile.language) : id.startsWith("difficulty_") ? id.equals("difficulty_"+profile.arcadeDifficulty) : id.startsWith("closet_tab_") && id.equals("closet_tab_"+closetTab);
            if(id.equals("prev_chapters"))enabled=chapterPage>0;
            if(id.equals("next_chapters"))enabled=chapterPage+1<StageCatalog.CHAPTER_COUNT;
            if(id.equals("help_prev"))enabled=helpPage>0;
            if(id.equals("help_next"))enabled=helpPage<5;
            if(id.equals("help_touch")||id.equals("help_motion")){choice=true;checked=id.equals("help_motion")==helpMotion;}
            if(id.equals("control_touch")||id.equals("control_motion")){choice=true;checked=id.equals("control_motion")==motionEnabled;if(id.equals("control_motion"))enabled=hasMotion;}
            if(id.equals("next_stage"))enabled=!physics.daily&&!physics.arcade&&!runMirror&&profile.isStageUnlocked(physics.stage.index+1);
            if(id.equals("start_daily"))enabled=DailyDrop.validDay(DailyDrop.today());
            boolean primary = screen==LANGUAGE&&id.equals("language_done") || screen==HOME&&id.equals("play") || screen==BRIEF&&id.equals("begin_campaign") || screen==DAILY&&id.equals("start_daily") || screen==UNLOCK&&id.equals("reward_use") || screen==ARCADE_SETUP&&id.equals("start_arcade") || screen==PAUSE&&id.equals("resume") || screen==RESULT&&(id.equals("reward_continue")||id.equals("next_stage")||id.equals("restart")&&(!physics.won||physics.daily||physics.arcade||runMirror||!profile.isStageUnlocked(physics.stage.index+1)));
            int fill=primary&&enabled?mint:checked?0xff2c4c43:panel;
            rect(c,l,top,r,b,13,fill);
            if(checked)line(c,l+12,b-5,r-12,b-5,mint,2);
            int ink=!enabled?muted:primary?background:checked?mint:color;
            if(!id.equals("jump")){
                paint.setTextAlign(Paint.Align.CENTER);fitText(c,label,(l+r)/2,(top+b)/2+size*.35f,size,ink,r-l-24);paint.setTextAlign(Paint.Align.LEFT);
            }
            String accessibleLabel=t(id.equals("prev_chapters")?"PREVIOUS":id.equals("next_chapters")?"NEXT":label.isEmpty()?id.equals("back")?"Back":id.equals("restart")?"Restart":id.equals("language")?"Language":"Pause":label);
            buttons.add(new Hit(id,l,top,r,b,accessibleLabel,enabled,choice,checked));
        }
        private void gear(Canvas c,float x,float y,int color) {
            circle(c,x,y,11,color); circle(c,x,y,7,panel); circle(c,x,y,2.5f,color);
            for (int i = 0; i < 8; i++) {
                double a = i*Math.PI/4;
                line(c,x+(float)Math.cos(a)*11,y+(float)Math.sin(a)*11,x+(float)Math.cos(a)*15,y+(float)Math.sin(a)*15,color,3);
            }
        }

        private void header(Canvas c,String title) {
            button(c,"back",24,20,88,84,"",white,18);
            FalleyUi.icon(c,"back",56,52,21,white);
            fitText(c,title,108,62,27,white,348);
        }
        @Override protected void onDraw(Canvas c) {
            long now = SystemClock.uptimeMillis();
            float frameDt = last == 0 ? 0 : Math.min((now-last)/1000f,.05f);
            if (active) {
                last = now; visualTime += frameDt; setupMessage = Math.max(0,setupMessage-frameDt);
                if (screen == SPLASH) {
                    splashElapsed += frameDt;
                    if (splashElapsed >= 1.5f) { if(profile.languageConfirmed)enterHome();else{screen=LANGUAGE;clearTouch();} }
                }
                if (screen == HOME) homeElapsed += frameDt;
                if(screen==WARDROBE && avatarPreviewTime<1.1f)avatarPreviewTime+=frameDt;
                if(screen==UNLOCK)unlockTime+=frameDt;
                if (screen == PLAY) advance(frameDt,now);
                else {
                    accumulator = 0;
                    // The loss effect finishes behind the result card without
                    // advancing gameplay while a menu is open.
                    if (screen == RESULT && physics.lost) burstTime += frameDt;
                }
            }
            viewport(); buttons.clear(); c.drawColor(background); c.save(); c.translate(ox,oy); c.scale(scale,scale);
            if (screen == SPLASH) drawSplash(c);
            else if (screen == HOME) drawHome(c);
            else if (screen == SETTINGS) drawSettings(c);
            else if (screen == HELP) drawHelp(c);
            else if (screen == CHAPTERS) drawChapters(c);
            else if (screen == ARCADE_SETUP) drawArcadeSetup(c);
            else if (screen == PRIVACY) drawPrivacy(c);
            else if (screen == LANGUAGE) drawLanguage(c);
            else if (screen == DAILY) drawDaily(c);
            else if (screen == WARDROBE) drawWardrobe(c);
            else if (screen == BRIEF) drawBrief(c);
            else if (screen == UNLOCK) drawAvatarUnlock(c);
            else {
                drawPlay(c);
                if (screen == PAUSE || screen == RESULT) { buttons.clear(); drawOverlay(c); }
            }
            c.restore();
            setKeepScreenOn(active && screen == PLAY);
            owner.updateMusic();
            menuAccessibility.updateScreen();
            // Static menus need no render loop. Home retains its gentle title
            // animation at 30fps; gameplay and its short effects use vsync.
            if (active) {
                if (screen == PLAY || screen == SPLASH || screen == RESULT && physics.lost && burstTime < 2.8f)
                    postInvalidateOnAnimation();
                else if(screen==WARDROBE && avatarPreviewTime<1.1f && !reducedMotion())postInvalidateOnAnimation();
                else if(screen==UNLOCK&&!reducedMotion())postInvalidateDelayed(33);
                else if (screen == HOME && !reducedMotion() || screen == ARCADE_SETUP && setupMessage > 0) postInvalidateDelayed(33);
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
                resultStarsThisRun=!runMirror?UxRules.starsForRun(physics):0;
                resultStarsNew=resultStarsThisRun&~resultStarsBefore;
                boolean changed;
                if (physics.daily) changed = profile.recordDaily(runDailyDay,physics.won,physics.score,physics.elapsed,physics.jumpsUsed(),runControl);
                else if (physics.arcade) changed = profile.recordArcadeResult(physics.arcadeDifficulty,physics.score,physics.elapsed);
                else changed = !runMirror && profile.recordResult(physics.stage.index,physics.won,physics.score,physics.elapsed,physics.jumpsUsed(),physics.landingX,physics.landingTargetCenter,physics.landingTargetWidth);
                if (changed) saveProfile();
                resultRecorded = true;
            }
            if (physics.won && physics.winTime > 1.2f || physics.lost && burstTime > .32f) {
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
                float t = reducedMotion()?1:clamp((elapsed-i*.055f)/.6f,0,1), ease = 1-(float)Math.pow(1-t,3);
                float idle = !reducedMotion() && screen == HOME && t >= 1 ? (float)Math.sin(visualTime*1.8f+i*.45f)*2 : 0;
                text(c,letter,x,baseline-32*(1-ease)+idle,size,white);
                x += paint.measureText(letter)+4;
            }
        }
        private void drawSplash(Canvas c) {
            AvatarRenderer.draw(c,240,310,45,-160,0,AvatarCatalog.DEMON,0);
            titleLetters(c,426,54,splashElapsed);
            center(c,"by Perf Games",240,469,19,mint);
            rect(c,150,520,330,525,3,panel);
            rect(c,150,520,150+180*(reducedMotion()?1:clamp(splashElapsed/1.5f,0,1)),525,3,mint);
        }

        private boolean reducedMotion(){return Build.VERSION.SDK_INT>=26&&!android.animation.ValueAnimator.areAnimatorsEnabled();}
        private float copy(Canvas c,String value,float x,float baseline,float width,float size,int maxLines,int color){
            paint.setTextLocale(languageLocale());paint.setTypeface(Typeface.create("sans-serif",Typeface.NORMAL));paint.setTextSize(size);paint.setColor(color);paint.setTextAlign(Paint.Align.LEFT);
            ArrayList<String> rows=new ArrayList<>();String row="";
            for(String word:t(value).split(" ")){
                String next=row.isEmpty()?word:row+" "+word;
                if(!row.isEmpty()&&paint.measureText(next)>width){rows.add(row);row=word;}else row=next;
            }
            if(!row.isEmpty())rows.add(row);
            float spacing=size*1.4f;
            for(int i=0;i<Math.min(maxLines,rows.size());i++){
                String line=rows.get(i);if(i==maxLines-1&&rows.size()>maxLines){while(!line.isEmpty()&&paint.measureText(line+"…")>width)line=line.substring(0,line.length()-1);line+="…";}
                c.drawText(line,x,baseline,paint);baseline+=spacing;
            }
            return baseline;
        }
        private void rootHeader(Canvas c,String title,String subtitle){text(c,title,24,62,33,white);copy(c,subtitle,24,98,432,15,1,muted);}
        private void bottomNav(Canvas c,int selected){
            float extra=layout.extra;line(c,24,layout.navDivider(),456,layout.navDivider(),border,1);
            String[] ids={"home","wardrobe","settings"},names={"Play","Collection","Settings"},icons={"play","collection","settings"};
            for(int i=0;i<3;i++){
                float left=24+i*146,right=Math.min(456,left+140);boolean on=i==selected;
                if(on)rect(c,left,layout.navTop(),right,818+extra,14,0xff203a32);
                FalleyUi.icon(c,icons[i],(left+right)/2,759+extra,22,on?mint:muted);
                paint.setTextAlign(Paint.Align.CENTER);fitText(c,names[i],(left+right)/2,802+extra,16,on?mint:muted,132);paint.setTextAlign(Paint.Align.LEFT);
                buttons.add(new Hit(ids[i],left,layout.navTop(),right,layout.height,t(names[i]),true,true,on));
            }
        }
        private void compactToggle(Canvas c,String id,String title,float top,float height,boolean value,boolean available){
            rect(c,24,top,456,top+height,13,panel);fitText(c,title,44,top+height/2+6,19,available?white:muted,305);
            rect(c,376,top+height/2-14,436,top+height/2+14,14,value&&available?mint:border);
            circle(c,value&&available?421:391,top+height/2,10,value&&available?background:muted);
            buttons.add(new Hit(id,24,top,456,top+height,t(title),available,true,value&&available));
        }
        private void homeWordmark(Canvas c){
            float x=24;paint.setTextSize(35);paint.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));
            for(int i=0;i<6;i++){String letter="FALLEY".substring(i,i+1);float f=reducedMotion()?1:clamp((homeElapsed-i*.035f)/.45f,0,1);float offset=reducedMotion()?0:(1-f)*(1-f)*12+(float)Math.sin(visualTime*1.8f+i*.45f)*1.2f;text(c,letter,x,64-offset,35,white);x+=paint.measureText(letter)+3;}
        }
        private void homeCard(Canvas c,String id,float left,float right,float top,String title,String detail,String status,String icon){
            float height=96;rect(c,left,top,right,top+height,16,panel);
            // Icon and title occupy independent columns; translations never share the icon box.
            FalleyUi.icon(c,icon,left+27,top+25,22,mint);fitText(c,title,left+49,top+32,17,white,right-left-65);
            copy(c,detail,left+16,top+58,right-left-32,13,1,muted);copy(c,status,left+16,top+82,right-left-32,12,1,gold);
            buttons.add(new Hit(id,left,top,right,top+height,t(title)+". "+t(detail)+". "+t(status),true,false,false));
        }
        private void selectHomeChapter(int chapter){
            chapterPage=Math.max(0,Math.min(StageCatalog.CHAPTER_COUNT-1,chapter));
            int first=chapterPage*4,recommended=UxRules.continueStage(profile);homeStage=-1;
            if(recommended/4==chapterPage&&profile.isStageUnlocked(recommended))homeStage=recommended;
            else for(int i=first;i<first+4;i++)if(profile.isStageUnlocked(i)){homeStage=i;break;}
        }
        private void drawHome(Canvas c) {
            int chosen=homeStage>=0?homeStage:chapterPage*4;StageCatalog.Stage stage=StageCatalog.get(chosen);
            float extra=layout.extra,hero=Math.min(140,extra*.45f),modeShift=hero+extra*.24f;
            homeWordmark(c);paint.setTextAlign(Paint.Align.RIGHT);fitText(c,t("%d / %d STARS",profile.totalStars(),72),456,55,14,gold,180);paint.setTextAlign(Paint.Align.LEFT);
            fitText(c,motionEnabled?t("MOTION / %s",t(PRESETS[profile.preset])):"TOUCH / SWIPE TO STEER",24,94,12,muted,432);
            rect(c,24,110,456,513+hero,20,0xff22332f);fitText(c,t("CHAPTER %d",chapterPage+1),44,145,13,mint,392);
            fitText(c,stage.chapterTitle,44,184,29,white,392);fitText(c,homeStage<0?"Chapter locked":t("STAGE %d",chosen+1)+" / "+t(stage.title),44,210,14,muted,392);
            FalleyUi.chapterScene(c,43,225,437,389+hero,chapterPage,profile.selectedAvatar,profile.selectedPalette,visualTime,reducedMotion());
            button(c,"play",44,405+hero,436,477+hero,homeStage<0?"Choose a stage":profile.stageWins[chosen]>0?t("Start stage %d",chosen+1):t("Continue stage %d",chosen+1),mint,21);
            copy(c,t("%d of 4 stages complete",UxRules.completedInChapter(profile,chapterPage)),44,498+hero,300,12,1,muted);
            text(c,"Another way to fall",24,540+modeShift,13,muted);
            boolean dailyDone=profile.dailyDay==DailyDrop.today()&&profile.dailyWins>0;
            homeCard(c,"daily",24,234,554+modeShift,"DAILY DROP","Fresh each day",dailyDone?"DAILY COMPLETE":"DAILY READY","daily");
            homeCard(c,"arcade",246,456,554+modeShift,"ARCADE","Choose your challenge.","Survive the waves","arcade");
            int reward=UxRules.nextRewardAvatar(profile);fitText(c,reward>=0?t("Next reward: %s",t(AvatarCatalog.name(reward))):"All avatars unlocked",24,698+extra*.9f,14,mint,432);
            bottomNav(c,0);
        }
        private void drawArcadeSetup(Canvas c) {
            header(c,"ARCADE");copy(c,"Build your survival run.",24,119,432,16,1,muted);
            float extra=layout.extra;int mark=section(c,extra*.08f);text(c,"DIFFICULTY",24,145,12,muted);
            String[] descriptions={"Longer warnings","A steady challenge","Faster attacks"};
            for(int i=0;i<3;i++){
                float left=24+i*146,right=Math.min(456,left+140);boolean selected=profile.arcadeDifficulty==i;
                FalleyUi.card(c,left,157,right,245,selected);
                for(int bar=0;bar<3;bar++)rect(c,left+16+bar*9,172-bar*2,left+21+bar*9,184,2,bar<=i?(i==2?coral:mint):border);
                if(selected)checkMark(c,right-22,177,mint);
                fitText(c,DIFFICULTIES[i],left+15,207,16,selected?mint:white,right-left-30);
                copy(c,descriptions[i],left+15,229,right-left-30,11,1,muted);
                buttons.add(new Hit("difficulty_"+i,left,157,right,245,t(DIFFICULTIES[i])+". "+t(descriptions[i]),true,true,selected));
            }
            endSection(c,mark,extra*.08f);mark=section(c,extra*.35f);text(c,"CHOOSE HAZARDS",24,267,12,muted);
            String[] ids={"arcade_fire","arcade_top","arcade_side","arcade_lightning","arcade_saw"};
            String[] labels={"Fireballs","Top Lasers","Side Lasers","Lightning","Buzzsaws"};
            for(int i=0;i<5;i++){
                float left=24+(i%2)*222,top=279+(i/2)*96;boolean selected=(profile.arcadeHazards&(1<<i))!=0;
                FalleyUi.card(c,left,top,left+210,top+86,selected);
                hazardCardIcon(c,i,left+32,top+28,selected?mint:muted);
                if(selected)checkMark(c,left+184,top+22,mint);
                fitText(c,labels[i],left+16,top+68,17,selected?white:muted,178);
                buttons.add(new Hit(ids[i],left,top,left+210,top+86,t(labels[i]),true,true,selected));
            }
            rect(c,246,471,456,557,17,0xff2d2940);
            text(c,"ROPE MIX",262,496,14,0xffc7b8ff);
            for(int i=0;i<3;i++)line(c,263+i*58,512,304+i*58,512,RopeCatalog.color(i),3);
            copy(c,"Special ropes from Wave 2",262,537,178,12,1,muted);
            endSection(c,mark,extra*.35f);mark=section(c,extra*.72f);copy(c,setupMessage>0?"Keep at least one hazard enabled.":"Tap cards to add or remove hazards.",24,585,432,13,1,setupMessage>0?coral:muted);
            button(c,"control_touch",24,600,234,658,"TOUCH",!motionEnabled?mint:muted,16);
            button(c,"control_motion",246,600,456,658,"MOTION",motionEnabled?mint:muted,16);
            copy(c,UxRules.controlCue(motionEnabled),24,681,432,13,1,muted);
            endSection(c,mark,extra*.72f);mark=section(c,extra);button(c,"start_arcade",24,696,456,770,"START SURVIVAL",mint,21);
            center(c,t("BEST %d / LONGEST %.1fs",profile.arcadeBest[profile.arcadeDifficulty],profile.arcadeLongest[profile.arcadeDifficulty]),240,802,12,muted);endSection(c,mark,extra);
        }
        private void checkMark(Canvas c,float x,float y,int color){line(c,x-5,y,x-1,y+4,color,2);line(c,x-1,y+4,x+7,y-5,color,2);}
        private void hazardCardIcon(Canvas c,int kind,float x,float y,int color){
            if(kind==0){circle(c,x,y+2,10,color);line(c,x-8,y-3,x-15,y-13,color,3);line(c,x-1,y-7,x-5,y-18,color,3);}
            else if(kind==1){line(c,x-12,y-13,x+12,y-13,color,3);line(c,x,y-8,x,y+11,color,3);arrow(c,x,y-8,x,y+12,color);}
            else if(kind==2){line(c,x-14,y-12,x-14,y+12,color,3);line(c,x+14,y-12,x+14,y+12,color,3);line(c,x-9,y,x+9,y,color,3);}
            else if(kind==3)FalleyUi.icon(c,"arcade",x,y,30,color);
            else{for(int i=0;i<10;i++){double a=i*Math.PI/5;line(c,x+(float)Math.cos(a)*9,y+(float)Math.sin(a)*9,x+(float)Math.cos(a)*15,y+(float)Math.sin(a)*15,color,2);}circle(c,x,y,10,color);circle(c,x,y,4,panel);}
        }

        private void drawChapters(Canvas c) {
            header(c,t("CHAPTER %d / %d",chapterPage+1,6));
            fitText(c,StageCatalog.get(chapterPage*4).chapterTitle,24,127,26,white,432);
            FalleyUi.chapterScene(c,24,142,456,240,chapterPage,profile.selectedAvatar,profile.selectedPalette,visualTime,true);
            for(int slot=0;slot<4;slot++){
                int i=chapterPage*4+slot;StageCatalog.Stage stage=StageCatalog.get(i);boolean unlocked=profile.isStageUnlocked(i);float top=254+slot*96;
                rect(c,24,top,456,top+84,14,unlocked?panel:0xff202627);
                if(i==UxRules.continueStage(profile))line(c,28,top+15,28,top+69,mint,3);
                fitText(c,stage.title,44,top+30,22,unlocked?white:muted,330);
                text(c,stage.bossKind>0?"CHAPTER FINALE":t("STAGE %d",i+1),44,top+54,12,stage.bossKind>0?gold:muted);
                if(unlocked){drawStars(c,profile.stars[i],294,top+59,8);paint.setTextAlign(Paint.Align.RIGHT);fitText(c,t("PAR %d JUMPS",stage.parJumps),438,top+71,11,muted,100);paint.setTextAlign(Paint.Align.LEFT);}
                else drawLock(c,421,top+44,muted);
                buttons.add(new Hit("stage_"+i,24,top,456,top+84,t(stage.title)+". "+t(unlocked?"UNLOCKED":"LOCKED"),unlocked,false,false));
            }
            fitText(c,t("REWARD: %s",t(AvatarCatalog.name(chapterPage+1))),24,658,17,profile.isChapterComplete(chapterPage)?mint:muted,432);
            copy(c,"Finish stages. Collect stars. Beat the finale.",24,683,432,14,1,muted);
            button(c,"prev_chapters",24,710,154,794,"PREVIOUS",white,14);
            button(c,"back",164,710,302,794,"DONE",mint,17);
            button(c,"next_chapters",312,710,456,794,"NEXT",white,14);
        }
        private int appearanceColor(int id){return new int[]{mint,0xff78e7d0,0xffb49bff,gold}[Math.max(0,Math.min(3,id))];}
        private void drawStars(Canvas c,int mask,float x,float y,float size){
            for(int i=0;i<3;i++){
                path.reset();for(int k=0;k<10;k++){double a=-Math.PI/2+k*Math.PI/5;float r=k%2==0?size:size*.45f;
                    float px=x+i*size*2.7f+(float)Math.cos(a)*r,py=y+(float)Math.sin(a)*r;if(k==0)path.moveTo(px,py);else path.lineTo(px,py);}
                path.close();paint.setColor((mask&(1<<i))!=0?gold:border);c.drawPath(path,paint);
            }
        }
        private float paragraph(Canvas c,String copy,float y,float size,int color){return policyParagraph(c,copy,30,y,size,size*1.45f,false,color);}

        private void drawDaily(Canvas c){
            header(c,"DAILY DROP");long day=DailyDrop.today();boolean valid=DailyDrop.validDay(day);
            float extra=layout.extra;int mark=section(c,extra*.10f);rect(c,24,117,456,412,20,0xff22332f);
            text(c,DailyDrop.dateLabel(day),44,161,28,white);
            copy(c,"NEW COURSE AT 00:00 UTC",44,190,392,13,1,mint);
            int chapter=valid?StageCatalog.daily(DailyDrop.seed(day)).chapterIndex:0;
            FalleyUi.chapterScene(c,43,214,437,396,chapter,profile.selectedAvatar,profile.selectedPalette,visualTime,true);
            endSection(c,mark,extra*.10f);mark=section(c,extra*.40f);rect(c,24,428,456,591,17,panel);
            center(c,t("STREAK %d / BEST %d",profile.dailyStreak,profile.dailyBestStreak),240,468,22,gold);
            center(c,t("TODAY: %d WINS / %d TRIES",profile.dailyWins,profile.dailyAttempts),240,505,15,muted);
            center(c,t("BEST SCORE %d",profile.dailyBestScore),240,542,18,white);
            center(c,"Standard Daily physics",240,575,13,mint);
            endSection(c,mark,extra*.40f);mark=section(c,extra*.70f);copy(c,"One seeded course for everyone. Tilt or swipe. Land safely to complete today's drop.",24,626,432,16,2,white);
            fitText(c,"Same hitbox. Daily uses Human abilities.",24,678,12,mint,432);
            if(!dailyClockValid)fitText(c,valid?"CLOCK CHANGED: PRACTICE ONLY":"CHECK DEVICE DATE",24,697,14,coral,432);
            endSection(c,mark,extra*.70f);mark=section(c,extra);button(c,"start_daily",24,714,456,794,"START DAILY DROP",mint,21);endSection(c,mark,extra);
        }

        private void avatarTrait(Canvas c,int avatar,float left,float top,float width){
            fitText(c,t("Jump %s · Weight %s",String.format(Locale.US,"%.2f×",AvatarAbilities.jumpHeight(avatar)),String.format(Locale.US,"%.2f×",AvatarAbilities.massScale(avatar))),left,top,12,gold,width);
            copy(c,AvatarCatalog.detail(avatar),left,top+20,width,12,2,muted);
        }
        private void drawBrief(Canvas c){
            StageCatalog.Stage stage=physics.stage;float extra=layout.extra;header(c,"Before the drop");
            int mark=section(c,extra*.16f);
            button(c,"prev_chapters",24,105,84,165,"‹",white,27);button(c,"next_chapters",396,105,456,165,"›",white,27);
            center(c,t("CHAPTER %d / %d",chapterPage+1,6),240,130,14,mint);paint.setTextAlign(Paint.Align.CENTER);fitText(c,StageCatalog.get(chapterPage*4).chapterTitle,240,159,22,white,300);paint.setTextAlign(Paint.Align.LEFT);
            int next=UxRules.continueStage(profile);
            for(int slot=0;slot<4;slot++){
                int index=chapterPage*4+slot;boolean unlocked=profile.isStageUnlocked(index),selected=index==homeStage;float left=24+slot*111,top=183;
                rect(c,left,top,left+99,top+70,12,selected?0xff34574a:panel);if(index==next)line(c,left+12,top+4,left+87,top+4,mint,2);
                paint.setTextAlign(Paint.Align.CENTER);fitText(c,t("STAGE %d",index+1),left+49.5f,top+28,13,unlocked?white:muted,87);paint.setTextAlign(Paint.Align.LEFT);
                if(unlocked)drawStars(c,profile.stars[index],left+28,top+52,7);else drawLock(c,left+49.5f,top+51,muted);
                buttons.add(new Hit("stage_"+index,left,top,left+99,top+70,t("STAGE %d",index+1)+". "+t(StageCatalog.get(index).title)+". "+t(unlocked?selected?"SELECTED":"UNLOCKED":"LOCKED"),unlocked,true,selected));
            }
            if(homeStage<0){
                center(c,"Chapter locked",240,299,24,white);FalleyUi.chapterScene(c,24,323,456,527,chapterPage,profile.selectedAvatar,profile.selectedPalette,0,true);
                copy(c,"Finish the previous stage",24,568,432,18,2,muted);endSection(c,mark,extra*.16f);return;
            }
            fitText(c,stage.title,24,291,27,white,300);
            if(homeStage==next){paint.setTextAlign(Paint.Align.RIGHT);fitText(c,"Recommended",456,289,11,mint,125);paint.setTextAlign(Paint.Align.LEFT);}
            copy(c,ChapterStories.intro(stage.chapterIndex),24,313,432,12,2,muted);            rect(c,24,342,456,435,14,panel);FalleyUi.miniAvatar(c,55,389,21,profile.selectedAvatar,profile.selectedPalette,0,true);
            fitText(c,t(AvatarCatalog.name(profile.selectedAvatar))+" / "+t(AvatarCatalog.trait(profile.selectedAvatar)),91,367,16,mint,343);
            avatarTrait(c,profile.selectedAvatar,91,388,343);endSection(c,mark,extra*.16f);
            mark=section(c,extra*.42f);
            copy(c,mechanicCopy(stage),24,457,432,14,2,white);
            fitText(c,profile.selectedAvatar==AvatarCatalog.ROBOT||profile.selectedAvatar==AvatarCatalog.SHADOW?"Attack protection only. Walls and spikes still kill.":stage.index<2?"Mint chevrons: Spring rebound 1.5×":"Mint chevrons: Spring / Violet bolt: Static",24,501,11,mint,432);
            if(practiceExpanded){
                rect(c,24,526,456,586,12,panel);text(c,"Mirror practice",40,551,16,white);copy(c,"No progress or stars",40,575,295,12,1,muted);
                rect(c,377,543,437,571,13,runMirror?mint:border);circle(c,runMirror?423:391,557,9,runMirror?background:muted);
                buttons.add(new Hit(runMirror?"standard_run":"mirror_run",24,526,456,586,t("Mirror practice")+". "+t("No progress or stars"),true,true,runMirror));
            }else{
                text(c,"Three star goals",24,521,12,mint);String[] goals={"Finish",t("Par ≤ %d",stage.parJumps),"Gold centre"};String[] icons={"play","restart","star"};
                for(int i=0;i<3;i++){float x=24+i*147;rect(c,x,532,x+138,586,12,panel);FalleyUi.icon(c,icons[i],x+20,558,15,gold);fitText(c,goals[i],x+35,563,12,white,96);}
            }
            copy(c,"Finish to progress. Other stars are optional.",24,608,432,12,1,muted);endSection(c,mark,extra*.42f);
            mark=section(c,extra*.72f);
            button(c,"control_touch",24,623,234,679,"TOUCH",!motionEnabled?mint:muted,16);button(c,"control_motion",246,623,456,679,"MOTION",motionEnabled?mint:muted,16);
            copy(c,UxRules.controlCue(motionEnabled),24,701,432,12,1,muted);
            button(c,"practice_options",24,710,456,754,practiceExpanded?"Hide practice options":"Practice options",muted,14);endSection(c,mark,extra*.72f);
            button(c,"begin_campaign",24,767+extra,456,817+extra,"START DROP",mint,20);
        }
        private String mechanicCopy(StageCatalog.Stage s){
            if(s.bossKind>0)return "Finale: read the machine's warnings, take the safe gap, then land on the target.";
            ArrayList<String> hints=new ArrayList<>();
            if(s.hasMechanic(StageCatalog.MOVING_TARGET))hints.add("The landing target moves. Follow its centre.");
            if(s.hasMechanic(StageCatalog.SHRINKING_TARGET))hints.add("The target pulses smaller. Time your landing.");
            if(s.hasMechanic(StageCatalog.WIND_ZONES))hints.add("Marked wind zones push sideways. Countersteer gently.");
            if(s.hasMechanic(StageCatalog.BOUNCE_PADS))hints.add("Cyan pads bounce you. Leave room above.");
            if(s.hasMechanic(StageCatalog.BREAKABLE_ANCHORS))hints.add("Amber anchors crack under load. Move before they break.");
            if(s.hasMechanic(StageCatalog.SLOW_PICKUPS))hints.add("Collect the hourglass to slow danger briefly.");
            if(hints.isEmpty())return t("Read the warnings. Let ropes bend and carry you down.");
            // Select complete hints before translation so Hindi punctuation cannot break the bounds.
            StringBuilder concise=new StringBuilder();
            for(int i=0;i<Math.min(2,hints.size());i++)concise.append(t(hints.get(i))).append(' ');
            return concise.toString();
        }
        private String modifierCopy(StageCatalog.Stage s){
            if(s.hasModifier(StageCatalog.LOW_GRAVITY))return "LOW GRAVITY";
            if(s.hasModifier(StageCatalog.GUSTS))return "WIND GUSTS";
            if(s.hasModifier(StageCatalog.HEAVY_LOAD))return "HEAVY ROPE LOAD";
            if(s.hasModifier(StageCatalog.SINGLE_JUMP))return "ONE JUMP PER BURST";
            return "READ THE WARNINGS";
        }

        private String avatarStyleCopy(int id){String[] styles={"Classic jump","Wings spread on takeoff","Graceful wing flare","Tucked knees","Floating jump pose","Mechanical spring pose","Shadow stretch"};return styles[Math.max(0,Math.min(6,id))];}
        private void ropeBadge(Canvas c,int type,float x,float y,int color){
            if(type==RopeCatalog.SPRING){line(c,x-7,y+4,x,y-4,color,2);line(c,x,y-4,x+7,y+4,color,2);line(c,x-7,y+11,x,y+3,color,2);line(c,x,y+3,x+7,y+11,color,2);}
            else if(type==RopeCatalog.STATIC){line(c,x+4,y-9,x-5,y+1,color,2);line(c,x-5,y+1,x+4,y+1,color,2);line(c,x+4,y+1,x-5,y+11,color,2);}
            else line(c,x-9,y,x+9,y,color,3);
        }
        private void drawAvatarUnlock(Canvas c){
            int reward=AvatarRewards.pending(profile);if(reward<0){enterHome();drawHome(c);return;}
            float extra=layout.extra;rootHeader(c,"New avatar unlocked","A new way to fall.");
            int mark=section(c,extra*.25f);rect(c,24,145,456,533,22,0xff263e36);
            float phase=reducedMotion()?1.5f:unlockTime%1.8f,lift=phase<1.2f?(float)Math.sin(phase/1.2f*Math.PI)*30:0,velocity=phase<.6f?-260:phase<1.2f?210:0;
            circle(c,240,299,100,0x2270e2c2);AvatarRenderer.draw(c,240,312-lift,62,velocity,0,reward,profile.selectedPalette);
            center(c,AvatarCatalog.name(reward),240,408,32,white);center(c,AvatarCatalog.trait(reward),240,440,17,mint);
            avatarTrait(c,reward,44,465,392);copy(c,AvatarCatalog.story(reward),44,519,392,12,1,muted);endSection(c,mark,extra*.25f);
            mark=section(c,extra*.65f);FalleyUi.miniAvatar(c,59,575,22,profile.selectedAvatar,profile.selectedPalette,0,true);
            fitText(c,t("Current avatar: %s",t(AvatarCatalog.name(profile.selectedAvatar))),103,566,18,white,340);copy(c,AvatarCatalog.trait(profile.selectedAvatar),103,591,329,14,1,muted);endSection(c,mark,extra*.65f);
            button(c,"reward_use",24,619+extra,456,689+extra,t("Use %s",t(AvatarCatalog.name(reward))),mint,21);
            button(c,"reward_keep",24,705+extra,456,775+extra,t("Keep %s",t(AvatarCatalog.name(profile.selectedAvatar))),white,19);
        }
        private void drawWardrobe(Canvas c){
            rootHeader(c,"Collection","Avatars and ropes. Make the fall yours.");
            button(c,"closet_tab_0",24,122,234,180,"AVATARS",closetTab==0?mint:muted,16);
            button(c,"closet_tab_1",246,122,456,180,"ROPES",closetTab==1?mint:muted,16);
            if(closetTab==0){
                int mark=section(c,layout.extra*.10f);rect(c,24,194,456,354,17,0xff223b34);
                float time=reducedMotion()?2:avatarPreviewTime;
                float y=274-(time<1.1f?(float)Math.sin(time/1.1f*Math.PI)*18:0);
                float velocity=time<.5f?-240:time<1?180:0;
                AvatarRenderer.draw(c,88,y,31,velocity,time>1&&time<1.1f?.1f:0,profile.selectedAvatar,profile.selectedPalette);
                fitText(c,AvatarCatalog.name(profile.selectedAvatar),148,225,24,white,288);
                copy(c,AvatarCatalog.trait(profile.selectedAvatar),148,249,288,14,1,mint);
                copy(c,AvatarCatalog.detail(profile.selectedAvatar),148,271,288,12,2,muted);
                button(c,"avatar_preview",148,306,436,350,"PREVIEW JUMP",mint,14);endSection(c,mark,layout.extra*.10f);
                for(int i=0;i<7;i++){
                    int row=i/3,col=i%3;float left=24+col*148,top=367+row*102+layout.extra*(.20f+row*.22f);boolean unlocked=Cosmetics.isAvatarUnlocked(profile,i),selected=profile.selectedAvatar==i;
                    rect(c,left,top,left+136,top+94,13,selected?0xff2c4c43:panel);
                    AvatarRenderer.draw(c,left+68,top+42,16,0,0,i,0);
                    if(!unlocked)drawLock(c,left+119,top+11,muted);
                    paint.setTextAlign(Paint.Align.CENTER);fitText(c,AvatarCatalog.name(i),left+68,top+68,15,unlocked?white:muted,120);fitText(c,selected?"SELECTED":unlocked?"UNLOCKED":t("CHAPTER %d",i),left+68,top+87,11,selected?mint:muted,120);paint.setTextAlign(Paint.Align.LEFT);
                    buttons.add(new Hit("avatar_"+i,left,top,left+136,top+94,t(AvatarCatalog.name(i))+". "+t(unlocked?"UNLOCKED":"LOCKED"),unlocked,true,selected));
                }
                copy(c,AvatarCatalog.story(profile.selectedAvatar),24,684+layout.extra*.86f,432,12,1,muted);
                copy(c,profile.selectedAvatar==AvatarCatalog.ROBOT||profile.selectedAvatar==AvatarCatalog.SHADOW?"Attack protection only. Walls and spikes still kill.":"Every avatar uses the same hitbox.",24,707+layout.extra*.92f,432,11,1,muted);
            }else{
                for(int i=0;i<RopeCatalog.COUNT;i++){
                    float top=201+i*143+layout.extra*(.16f+i*.25f);int color=RopeCatalog.color(i);
                    rect(c,24,top,456,top+129,16,panel);
                    fitText(c,RopeCatalog.name(i),44,top+32,24,white,290);
                    copy(c,RopeCatalog.description(i),44,top+60,358,14,2,muted);
                    String schedule=i==0?"Every course":i==1?"Campaign Stage 1 onward / Arcade Wave 2":"Campaign Stage 3 onward / Arcade Wave 2";
                    fitText(c,schedule,44,top+111,11,color,335);
                    ropeBadge(c,i,421,top+27,color);
                }
                copy(c,"Ropes change within each course. Read their colour and symbol. Daily uses Classic.",24,654+layout.extra*.86f,432,14,2,muted);
            }
            bottomNav(c,1);
        }

        private void toggleRow(Canvas c,String id,String title,float top,boolean value,boolean available) {
            rect(c,24,top,456,top+74,9,panel); fitText(c,title,44,top+44,18,available ? white : muted,310);
            rect(c,376,top+23,436,top+51,14,value && available ? mint : border);
            circle(c,value && available ? 421 : 391,top+37,10,value && available ? background : muted);
            buttons.add(new Hit(id,24,top,456,top+74,available ? t(title) : t("%s, unavailable",t(title)),available,true,value && available));
        }
        private void drawLock(Canvas c,float x,float y,int color) {
            rect(c,x-9,y-1,x+9,y+14,3,color);
            paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(2.5f); paint.setColor(color);
            c.drawArc(x-6,y-11,x+6,y+4,180,180,false,paint); paint.setStyle(Paint.Style.FILL);
            circle(c,x,y+5,2,panel);
        }

        private void drawSettings(Canvas c) {
            boolean paused=settingsReturn==PAUSE;
            if(paused){header(c,"SETTINGS");copy(c,"Your way to play.",24,108,432,14,1,muted);}else rootHeader(c,"Settings","Your way to play.");
            float extra=layout.extra;int mark=section(c,extra*.05f);compactToggle(c,"motion","Motion steering",120,60,motionEnabled,hasMotion);
            text(c,"MOTION FEEL",24,202,12,muted);
            for(int i=0;i<3;i++)button(c,"preset_"+i,24+i*146,214,Math.min(456,164+i*146),268,PRESETS[i],profile.preset==i?mint:muted,14);
            String detail=profile.preset==0?"Quick response. Small tilts move you more.":profile.preset==2?"Calmer response. More tilt, less movement.":"A steady balance of response and control.";
            copy(c,detail,24,290,432,13,1,muted);
            copy(c,!hasMotion?"No sensor available. Swipe left or right to steer.":!motionEnabled?"Motion is off. Swipe left or right to steer.":!motion.hasSample()?"Waiting for a sensor reading...":"Your hold centers at every start and resume.",24,319,432,12,1,muted);
            endSection(c,mark,extra*.05f);mark=section(c,extra*.23f);rect(c,24,323,456,379,13,panel);text(c,"Language",44,358,18,white);paint.setTextAlign(Paint.Align.RIGHT);fitText(c,FalleyStrings.languageName(profile.language),436,358,15,mint,210);paint.setTextAlign(Paint.Align.LEFT);
            buttons.add(new Hit("language",24,323,456,379,t("Language")+". "+FalleyStrings.languageName(profile.language),true,false,false));
            endSection(c,mark,extra*.23f);mark=section(c,extra*.36f);compactToggle(c,"music","Retro music",391,52,profile.music,true);
            endSection(c,mark,extra*.36f);mark=section(c,extra*.48f);compactToggle(c,"sound","Sound Effects",455,52,profile.sound,true);
            endSection(c,mark,extra*.48f);mark=section(c,extra*.60f);compactToggle(c,"haptics","Vibration",519,52,profile.haptics,true);
            endSection(c,mark,extra*.60f);mark=section(c,extra*.72f);compactToggle(c,"blood","Blood Effects",583,52,profile.blood,true);
            endSection(c,mark,extra*.72f);mark=section(c,extra*.84f);button(c,"help",24,647,234,703,"HOW TO PLAY",white,15);button(c,"privacy",246,647,456,703,"PRIVACY POLICY",white,15);
            endSection(c,mark,extra*.84f);if(paused)button(c,"back",24,744+extra,456,816+extra,"BACK TO PAUSE",mint,17);else bottomNav(c,2);
        }
        private void drawLanguage(Canvas c) {
            boolean first=!profile.languageConfirmed;
            if(first){rootHeader(c,"Choose your language","English is selected by default.");}else header(c,"LANGUAGE");
            copy(c,"You can change this anytime in Settings.",24,145,432,16,2,muted);
            if(!countryHint.country.isEmpty()){
                String region=new Locale("",countryHint.country).getDisplayCountry(languageLocale());
                fitText(c,t("Region hint: %s",region),24,204,15,mint,432);
            }
            for(int i=0;i<3;i++){
                float top=234+i*108+layout.extra*(.12f+i*.22f);
                button(c,"language_"+i,24,top,456,top+84,FalleyStrings.languageName(i),profile.language==i?mint:white,22);
                if(profile.language==i)circle(c,426,top+42,5,mint);
                if(!countryHint.country.isEmpty()&&i==countryHint.suggestedLanguage())fitText(c,"Available for your region",40,top+74,11,muted,370);
            }
            copy(c,"Your choice stays on this device.",24,609+layout.extra*.75f,432,15,2,muted);
            button(c,"language_done",24,734+layout.extra,456,808+layout.extra,first?"CONTINUE":"DONE",mint,19);
        }

        private void drawHelp(Canvas c) {
            header(c,"HOW TO PLAY");
            float extra=layout.extra;int mark=section(c,extra*.16f);button(c,"help_touch",24,132,234,206,"TOUCH",!helpMotion ? mint : muted,17);
            button(c,"help_motion",246,132,456,206,"MOTION",helpMotion ? mint : muted,17);
            rect(c,24,222,456,410,14,panel);
            if (helpPage == 0) drawControlGuide(c);
            else if (helpPage == 1) drawJumpGuide(c);
            else if(helpPage==2)drawRuleGuide(c);
            else drawExpansionGuide(c);
            endSection(c,mark,extra*.16f);mark=section(c,extra*.52f);fitText(c,helpTitle(),34,447,22,mint,412);
            helpBody(c,helpExplanation(),466,578);
            line(c,34,587,446,587,border,1);
            helpBody(c,helpTip(),601,693);
            center(c,t("GUIDE %d/%d",helpPage+1,6),240,718,12,muted);
            endSection(c,mark,extra*.52f);mark=section(c,extra);button(c,"help_prev",24,734,154,808,"PREVIOUS",helpPage > 0 ? white : muted,14);
            button(c,"back",164,734,302,808,"GOT IT",mint,15);
            button(c,"help_next",312,734,456,808,"NEXT",helpPage < 5 ? white : muted,14);endSection(c,mark,extra);
        }
        private void drawExpansionGuide(Canvas c){
            if(helpPage==3){
                line(c,68,264,210,282,gold,4);rect(c,62,252,78,274,3,gold);line(c,67,256,73,263,background,2);
                rect(c,276,354,394,365,6,mint);for(int k=0;k<4;k++){float x=285+k*30;line(c,x,369,x+11,380,mint,2);line(c,x+11,380,x,391,mint,2);}
                drawHuman(c,335,307,26,-200,0);arrow(c,335,277,335,247,mint);center(c,"BOUNCE",335,339,11,mint);
                center(c,"ANCHOR BREAKING",139,322,10,gold);
            }else if(helpPage==4){
                for(int i=0;i<3;i++)arrow(c,65,271+i*27,160,271+i*27,mint);
                drawHuman(c,225,314,29,150,0);rect(c,285,360,410,374,6,mint);arrow(c,290,343,403,343,gold);
                circle(c,368,273,22,0xff3c3159);line(c,358,261,378,285,white,3);line(c,378,261,358,285,white,3);
            }else{
                drawStars(c,7,183,284,22);drawHuman(c,240,352,24,170,0);
                rect(c,168,382,312,392,5,mint);line(c,217,379,263,379,gold,3);
            }
        }
        private String helpTitle() {
            if(helpPage==3)return "ROPE TRICKS";
            if(helpPage==4)return "READ THE AIR";
            if(helpPage==5)return "MASTER THE DROP";
            return helpPage == 0 ? helpMotion ? "TILT TO STEER" : "SWIPE TO STEER" :
                    helpPage == 1 ? "TWO QUICK JUMPS" : "READ THE DANGER";
        }
        private String helpExplanation() {
            if(helpPage==3)return "Cyan pads bounce you without spending a jump. Amber anchors crack under load and then break.";
            if(helpPage==4)return "Wind arrows show the push. Moving targets need timing. The hourglass briefly slows the world.";
            if(helpPage==5)return "Earn a star for finishing, meeting the jump par and landing in the golden centre. Stars combine across wins.";
            if (helpPage == 0) return helpMotion ?
                    "Hold the phone comfortably. Tilt left or right to steer. Your hold is centered at each start and resume." :
                    "Swipe left or right to steer. Release to stop the swipe. A light air current still nudges you.";
            if (helpPage == 1) return "Tap up to twice in a burst. Descend below where the burst began to recover. Wait briefly after the last jump.";
            return "The ceiling and side hazards are lethal. Read warnings and avoid live fire, lasers, lightning and saws.";
        }
        private String helpTip() {
            if(helpPage==3)return "Leave room below the lethal ceiling. One-jump stages allow one jump per burst, then descent restores it.";
            if(helpPage==4)return "Each chapter ends with a machine finale. Follow its warnings and safe gap. Mirror Practice is optional and awards no stars.";
            if(helpPage==5)return "Complete chapters for avatars. Campaign and Arcade mix rope types; Daily uses Classic.";
            if (helpPage == 0) return helpMotion ? "Tap the arena or JUMP to jump. You can still swipe to take over steering briefly." :
                    "Tap the arena or JUMP to jump. Watch the burst and total jump counters.";
            if (helpPage == 1) return "Campaign has a total jump budget for each stage. Recovery refills the burst, not the total. Arcade has no total limit.";
            return "Campaign: land on the target to unlock the next stage. Later missed landings are fatal. Arcade: survive.";
        }
        private ArrayList<String> helpLines(String label,float size) {
            paint.setTextLocale(languageLocale()); paint.setTypeface(Typeface.create("sans-serif",Typeface.NORMAL)); paint.setTextSize(size);
            ArrayList<String> lines = new ArrayList<>(); String row = "";
            for (String word : t(label).split(" ")) {
                String candidate = row.isEmpty() ? word : row+" "+word;
                if (!row.isEmpty() && paint.measureText(candidate) > 404) { lines.add(row); row = word; }
                else row = candidate;
            }
            if (!row.isEmpty()) lines.add(row);
            return lines;
        }
        private void helpBody(Canvas c,String label,float top,float bottom) {
            float size = 18*clamp(getResources().getConfiguration().fontScale,1,1.35f), spacing;
            ArrayList<String> rows;
            do {
                rows = helpLines(label,size);
                Paint.FontMetrics metrics = paint.getFontMetrics();
                spacing = Math.max(size*1.42f,metrics.bottom-metrics.top+2);
                if (rows.size()*spacing <= bottom-top || size <= 13) break;
                size -= .5f;
            } while (true);
            paint.setStyle(Paint.Style.FILL); paint.setColor(white);
            float baseline = top-paint.getFontMetrics().top;
            for (String row : rows) { c.drawText(row,38,baseline,paint); baseline += spacing; }
        }
        private void arrow(Canvas c,float x,float y,float x2,float y2,int color) {
            line(c,x,y,x2,y2,color,3);
            float a = (float)Math.atan2(y2-y,x2-x), spread = .65f;
            line(c,x2,y2,x2-11*(float)Math.cos(a-spread),y2-11*(float)Math.sin(a-spread),color,3);
            line(c,x2,y2,x2-11*(float)Math.cos(a+spread),y2-11*(float)Math.sin(a+spread),color,3);
        }
        private void drawControlGuide(Canvas c) {
            c.save(); c.translate(240,317); if (helpMotion) c.rotate(-13);
            rect(c,-43,-70,43,70,10,border); rect(c,-36,-60,36,53,5,background);
            line(c,-28,25,28,25,mint,2); drawHuman(c,0,6,17,-120,0); circle(c,0,62,4,muted); c.restore();
            arrow(c,106,317,163,317,mint); arrow(c,374,317,317,317,mint);
            if (helpMotion) {
                paint.setStyle(Paint.Style.STROKE); paint.setColor(gold); paint.setStrokeWidth(3);
                c.drawArc(167,235,313,386,205,75,false,paint); c.drawArc(167,235,313,386,25,75,false,paint); paint.setStyle(Paint.Style.FILL);
            } else {
                circle(c,335,268,23,0x335de0ba); circle(c,335,268,14,0x665de0ba); circle(c,335,268,6,mint);
                line(c,347,281,362,299,white,5);
            }
        }
        private void drawJumpGuide(Canvas c) {
            path.reset(); path.moveTo(112,332); path.quadTo(235,228,365,332);
            paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(2); paint.setColor(mint); c.drawPath(path,paint); paint.setStyle(Paint.Style.FILL);
            drawHuman(c,112,330,23,-180,0); drawHuman(c,238,279,23,-180,0); drawHuman(c,365,330,23,180,0);
            circle(c,210,376,9,mint); circle(c,270,376,9,mint);
            arrow(c,392,270,392,350,gold);
            center(c,"RECOVERY",365,393,11,gold);
        }
        private void drawRuleGuide(Canvas c) {
            line(c,76,244,404,244,coral,4); center(c,"CEILING",240,265,11,coral);
            line(c,76,244,76,384,coral,3); line(c,404,244,404,384,coral,3);
            for (float y = 276; y < 366; y += 28) {
                path.reset(); path.moveTo(76,y); path.lineTo(88,y+10); path.lineTo(76,y+20); path.close(); paint.setColor(coral); c.drawPath(path,paint);
            }
            drawHuman(c,240,319,24,180,0); arrow(c,286,313,286,355,mint);
            rect(c,80,379,400,391,5,border); rect(c,185,379,295,391,5,mint);
            circle(c,240,384,10,background); circle(c,240,384,5,mint);
            center(c,"LAND HERE",240,405,10,mint);
        }
        private void drawPrivacy(Canvas c) {
            header(c,"PRIVACY POLICY");float extra=layout.extra;
            float fontScale = clamp(getResources().getConfiguration().fontScale,1,1.6f);
            float size = 20*fontScale, lineHeight = size*1.42f;
            // The policy supports Android text-size preferences independently of
            // the game's fixed arena scale. Only this document scrolls.
            float y = 159-privacyScroll;
            c.save(); c.clipRect(24,128,446,632+extra);
            y = policyParagraph(c,PrivacyPolicy.TITLE,24,y,size+2,lineHeight,true,white)+8;
            y = policyParagraph(c,PrivacyPolicy.PUBLISHER,24,y,size*.85f,lineHeight*.85f,false,muted);
            y = policyParagraph(c,PrivacyPolicy.EFFECTIVE_DATE,24,y,size*.85f,lineHeight*.85f,false,muted);
            y = policyParagraph(c,PrivacyPolicy.UPDATED_DATE,24,y,size*.85f,lineHeight*.85f,false,muted)+24;
            for (String[] section : PrivacyPolicy.SECTIONS) {
                y = policyParagraph(c,section[0],24,y,size,lineHeight,true,mint)+2;
                y = policyParagraph(c,section[1],24,y,size,lineHeight,false,white)+25;
            }
            c.restore();
            privacyScrollLimit = Math.max(0,y+privacyScroll-642-extra);
            privacyScroll = clamp(privacyScroll,0,privacyScrollLimit);
            float viewport = 504+extra, content = viewport+privacyScrollLimit, thumb = viewport*viewport/content;
            rect(c,452,128,456,632+extra,2,panel);
            rect(c,452,128+(viewport-thumb)*(privacyScrollLimit == 0 ? 0 : privacyScroll/privacyScrollLimit),
                    456,128+(viewport-thumb)*(privacyScrollLimit == 0 ? 0 : privacyScroll/privacyScrollLimit)+thumb,2,mint);
            center(c,privacyScroll >= privacyScrollLimit-1 ? "END OF POLICY" : "SWIPE UP TO READ MORE",240,655+extra,11,muted);
            button(c,"privacy_web",24,674+extra,234,728+extra,"ONLINE POLICY",mint,14);
            button(c,"privacy_email",246,674+extra,456,728+extra,"EMAIL PERF GAMES",white,14);
            button(c,"back",24,744+extra,456,816+extra,"BACK TO SETTINGS",mint,17);
        }
        private float policyParagraph(Canvas c,String value,float x,float y,float size,float lineHeight,boolean bold,int color) {
            value = t(value);
            paint.setTextLocale(languageLocale());
            paint.setStyle(Paint.Style.FILL); paint.setColor(color); paint.setTextSize(size);
            paint.setTypeface(Typeface.create("sans-serif",bold ? Typeface.BOLD : Typeface.NORMAL));
            StringBuilder row = new StringBuilder();
            for (String sourceWord : value.split(" ")) {
                String word = sourceWord;
                String candidate = row.length() == 0 ? word : row+" "+word;
                if (row.length() > 0 && paint.measureText(candidate) > 414) {
                    c.drawText(row.toString(),x,y,paint); y += lineHeight; row.setLength(0);
                }
                // Long web/email addresses must stay inside the document at large text sizes.
                while (paint.measureText(word) > 414) {
                    int count = Math.max(1,paint.breakText(word,true,414,null));
                    if (count < word.length() && Character.isHighSurrogate(word.charAt(count-1))) count--;
                    if (count == 0) count = Character.charCount(word.codePointAt(0));
                    c.drawText(word.substring(0,count),x,y,paint); y += lineHeight;
                    word = word.substring(count);
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
            fitText(c,title,85,y+12,17,white,365);
            policyParagraph(c,detail,24,y+46,14,18,false,muted);
            line(c,24,y+83,456,y+83,border,1);
        }

        private void drawPlay(Canvas c) {
            fitText(c,physics.arcade?t("FALLEY / ARCADE"):physics.daily?t("DAILY DROP"):runMirror?t("MIRROR PRACTICE"):t("FALLEY / STAGE %d",physics.stage.index+1),24,29,12,mint,260);
            fitText(c,physics.arcade?DIFFICULTIES[physics.arcadeDifficulty]:physics.daily?DailyDrop.dateLabel(runDailyDay):physics.stage.title,24,67,25,white,260);
            button(c,"restart",300,20,374,94,"",white,17);FalleyUi.icon(c,"restart",337,57,23,white);
            button(c,"pause",382,20,456,94,"",white,17);FalleyUi.icon(c,"pause",419,57,23,white);
            fitText(c,physics.arcade?"STAY ALIVE":"LAND ON THE TARGET",24,116,15,mint,282);
            paint.setTextAlign(Paint.Align.RIGHT);fitText(c,physics.arcade?t("WAVE %d",physics.wave):t("CUTS %d/%d",physics.brokenCount,physics.totalRopes()),456,116,12,muted,140);paint.setTextAlign(Paint.Align.LEFT);
            float progress=physics.arcade?(physics.elapsed%20)/20:clamp((physics.y-310)/(physics.groundY()-physics.radius()-310),0,1);
            rect(c,24,129,456,134,3,panel);rect(c,24,129,24+432*progress,134,3,mint);
            float targetCenter=(physics.targetLeft()+physics.targetRight())/2;String side=t(targetCenter<239?"LEFT":targetCenter>241?"RIGHT":"CENTER");
            String objective=physics.bossActive?t(physics.bossWarning>0?"BOSS WARNING":"BOSS ACTIVE"):physics.arcade?t("%.1fs SURVIVED",physics.elapsed):physics.won?t("LANDED!"):t("%dm / LAND %s",Math.max(0,(int)((physics.groundY()-physics.y-physics.radius())/10)),side);
            fitText(c,objective,24,152,12,physics.bossActive?gold:physics.won?mint:muted,272);
            boolean centering=motionEnabled&&motion.isCentering();
            boolean breeze=motionEnabled&&!centering&&!touch.dragging()&&Math.abs(physics.motionGustVelocity()+physics.nudgeVelocity())>.8f&&!physics.lost&&!physics.won;
            paint.setTextAlign(Paint.Align.RIGHT);fitText(c,centering?"HOLD COMFORTABLY":breeze?"BREEZE":motionEnabled?"MOTION":"TOUCH",456,152,11,breeze?mint:muted,breeze?96:168);paint.setTextAlign(Paint.Align.LEFT);
            if(breeze){float direction=Math.signum(physics.motionGustVelocity()+physics.nudgeVelocity());arrow(c,330+(direction<0?20:0),148,330+(direction>0?20:0),148,mint);}
            drawArena(c);drawPlayControls(c);
        }
        private void drawArena(Canvas c) {
            c.save(); c.clipRect(20,TOP,460,BOTTOM); rect(c,20,TOP,460,BOTTOM,0,0xff121a20);
            float gridOffset = physics.cameraY%45;
            for (float y = TOP-gridOffset; y <= BOTTOM; y += 45) line(c,20,y,460,y,0xff23313b,1);
            for (int x = 60; x < 460; x += 60) line(c,x,TOP,x,BOTTOM,0xff1b2832,1);
            float left = physics.leftWall(), right = physics.rightWall();
            drawRails(c,left,right);
            float ceilingLeft = left+physics.hazardInset()+3, ceilingRight = right-physics.hazardInset()-3;
            line(c,ceilingLeft,TOP+3,ceilingRight,TOP+3,coral,4);
            for (float x = ceilingLeft+7; x < ceilingRight-5; x += 18)
                line(c,x,TOP+3,x+5,TOP+9,coral,1.5f);
            drawFloor(c); drawMechanisms(c);
            for (int j = 0; j < physics.ropes.length; j++) {
                Physics.Rope rope = physics.ropes[j]; int color = RopeCatalog.color(rope.type);
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
                if(rope.cut<0&&rope.type!=RopeCatalog.CLASSIC)ropeBadge(c,rope.type,rope.x[0]+22,rope.y[0]-physics.cameraY-16,color);
                if (rope.cut >= 0) for (int i = rope.cut; i <= rope.cut+1 && i < rope.x.length; i++) {
                    circle(c,rope.x[i],rope.y[i]-physics.cameraY,7,0x55f6d477);
                    circle(c,rope.x[i],rope.y[i]-physics.cameraY,3.5f,gold);
                }
            }
            drawFireHazards(c); drawBeamHazards(c); drawSawHazards(c);
            if (!physics.lost) {
                drawHuman(c,physics.x,physics.y-physics.cameraY,physics.radius(),physics.vy,physics.landingPulse);
                if(physics.immunityPulse>0){
                    float pulse=clamp(physics.immunityPulse/.45f,0,1),y=physics.y-physics.cameraY,r=physics.radius()+8+(1-pulse)*18;
                    int color=physics.immunityKind==ArcadeRules.FIREBALLS?gold:mint;paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(2.5f);paint.setColor((color&0xffffff)|((int)(pulse*220)<<24));c.drawCircle(physics.x,y,r,paint);paint.setStyle(Paint.Style.FILL);
                    for(int i=0;i<6;i++){double a=visualTime*2+i*Math.PI/3;line(c,physics.x+(float)Math.cos(a)*(r+3),y+(float)Math.sin(a)*(r+3),physics.x+(float)Math.cos(a)*(r+8),y+(float)Math.sin(a)*(r+8),(color&0xffffff)|((int)(pulse*200)<<24),2);}
                }
            }
            drawDeath(c);

            contextCue=contextualCue();if(!contextCue.isEmpty()){rect(c,60,TOP+18,420,TOP+80,13,0xee232b2c);copy(c,contextCue,76,TOP+44,328,15,2,white);}
            c.restore();
            line(c,20,TOP,460,TOP,border,1); line(c,20,BOTTOM,460,BOTTOM,border,1);
        }
        private void drawMechanisms(Canvas c){
            if(physics.arcade)return;
            float left=physics.leftWall()+physics.hazardInset()+4,right=physics.rightWall()-physics.hazardInset()-4;
            for(Physics.WindZone wind:physics.windZones)if(wind.active){
                float top=Math.max(TOP,wind.top-physics.cameraY),bottom=Math.min(BOTTOM,wind.bottom-physics.cameraY);
                if(top>=bottom)continue;
                rect(c,left,top,right,bottom,0,0x195de0ba);
                for(float y=top+25;y<bottom;y+=54)for(float x=left+22;x<right-15;x+=54){
                    float direction=wind.strength>0?1:-1;
                    line(c,x-direction*11,y,x+direction*11,y,0x885de0ba,2);
                    line(c,x+direction*11,y,x+direction*5,y-5,0x885de0ba,2);line(c,x+direction*11,y,x+direction*5,y+5,0x885de0ba,2);
                }
                center(c,"WIND",240,top+15,10,mint);
            }
            for(Physics.BouncePad pad:physics.bouncePads)if(pad.active){
                float y=pad.y-physics.cameraY;if(y<TOP-40||y>BOTTOM+40)continue;
                int color=pad.cooldown>0?muted:mint;
                rect(c,pad.x-pad.width/2,y-4,pad.x+pad.width/2,y+9,5,color);
                for(int i=-1;i<=1;i++){float x=pad.x+i*21;line(c,x-5,y+18,x+5,y+12,color,2);line(c,x+5,y+12,x-5,y+25,color,2);}
                center(c,"BOUNCE",pad.x,y-15,9,color);
            }
            for(Physics.SlowPickup pickup:physics.slowPickups)if(pickup.active&&!pickup.collected){
                float y=pickup.y-physics.cameraY;if(y<TOP-24||y>BOTTOM+24)continue;
                circle(c,pickup.x,y,19,0x44b49bff);circle(c,pickup.x,y,15,0xff3c3159);
                line(c,pickup.x-7,y-9,pickup.x+7,y-9,white,2);line(c,pickup.x-7,y+9,pickup.x+7,y+9,white,2);
                line(c,pickup.x-6,y-8,pickup.x+6,y+8,gold,2);line(c,pickup.x+6,y-8,pickup.x-6,y+8,gold,2);
            }
            for(Physics.Rope rope:physics.ropes)if(rope.fragile&&rope.cut<0){
                for(int index:new int[]{0,Physics.Rope.N-1}){
                    float x=rope.x[index],y=rope.y[index]-physics.cameraY;if(y<TOP-20||y>BOTTOM+20)continue;
                    rect(c,x-8,y-10,x+8,y+10,3,gold);
                    line(c,x-3,y-8,x+3,y-2,background,2);line(c,x+3,y-2,x-3,y+4,background,2);
                    if(rope.anchorStress>.3f)circle(c,x,y,12+rope.anchorStress*5,0x55ff816c);
                }
            }
            if(physics.slowTimeRemaining>0){
                rect(c,166,TOP+14,314,TOP+45,8,0xdd3c3159);center(c,"SLOW TIME",240,TOP+35,12,white);
            }
            if(physics.bossActive){
                int color=physics.stage.bossKind==StageCatalog.BOSS_FIRE?coral:physics.stage.bossKind==StageCatalog.BOSS_STORM?gold:0xffb49bff;
                BossRenderer.draw(c,physics,visualTime,TOP,BOTTOM);
                if(physics.bossWarning>0){
                    center(c,"SAFE GAP",physics.bossSafeX,TOP+78,10,mint);
                    line(c,physics.bossSafeX,TOP+87,physics.bossSafeX,TOP+111,mint,3);
                    line(c,physics.bossSafeX,TOP+111,physics.bossSafeX-6,TOP+104,mint,2);line(c,physics.bossSafeX,TOP+111,physics.bossSafeX+6,TOP+104,mint,2);
                }
            }
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
            int landingColor=mint;
            circle(c,middle,y+7,16,0xff29584d); circle(c,middle,y+7,11,landingColor); circle(c,middle,y+7,4,background);
            float precise=physics.targetWidth()*FalleyProfile.PRECISION_FRACTION;
            line(c,middle-precise/2,y-7,middle+precise/2,y-7,gold,3);
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
                    float vy=Math.max(1,fire.verticalSpeed()),start=Math.max(TOP+42,fire.screenY+25);
                    for(float y=start;y<BOTTOM;y+=28){float x=fire.x+fire.velocityX*(y-fire.screenY)/vy;line(c,x,y,x+fire.velocityX*11/vy,y+11,(0xff9c52)|alpha<<24,2);}
                    float marker=clamp(fire.screenY,TOP+24,BOTTOM-24);circle(c,fire.x,marker,17,0xff583c29);center(c,"!",fire.x,marker+7,20,gold);
                    continue;
                }
                float y = fire.y-physics.cameraY;
                if (y < TOP-60 || y > BOTTOM+40) continue;
                float length=Math.max(1,(float)Math.hypot(fire.velocityX,fire.verticalSpeed()));
                for (int k = 5; k > 0; k--) circle(c,fire.x-fire.velocityX/length*k*12,y-fire.verticalSpeed()/length*k*12,Math.max(2,fire.radius-k),
                        0x00ff9c52|((110-k*15)<<24));
                circle(c,fire.x,y,fire.radius+4,0x55ff9c52);
                circle(c,fire.x,y,fire.radius,0xffff9c52);
                circle(c,fire.x,y+2,fire.radius*.48f,gold);
            }
        }
        private void drawBeamHazards(Canvas c) {
            for (Physics.BeamHazard beam : physics.beamHazards) {
                if (!beam.active) continue;
                if(beam.bossLane){BossRenderer.drawBeamLane(c,physics,beam,TOP,BOTTOM);continue;}
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
                if(saw.bossRail){BossRenderer.drawSawRail(c,physics,saw,visualTime,TOP,BOTTOM);continue;}
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
            AvatarRenderer.draw(c,x,y,r,vy,landingPulse,profile.selectedAvatar,profile.selectedPalette);
        }
        private void deadFigure(Canvas c,float x,float y,float r,float angle,boolean charred,boolean crumpled) {
            if(profile.selectedAvatar!=AvatarCatalog.HUMAN&&!charred){
                c.save();c.translate(x,y);c.rotate(angle);if(crumpled)c.scale(1.1f,.65f);
                AvatarRenderer.draw(c,0,0,r,180,crumpled?.18f:0,profile.selectedAvatar,profile.selectedPalette);c.restore();return;
            }
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
            boolean saw = reason.contains("BUZZSAW"), spikes = reason.contains("SPIKE"), ceiling = reason.contains("CEILING"), floor = reason.contains("FLOOR") || reason.contains("HARD LANDING"),
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
            } else if (floor || ceiling) {
                if (floor) y = physics.groundY()-physics.cameraY-7;
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
                        py = y+(float)Math.sin(a)*speed*age+(floor || ceiling || spikes || laser || saw ? 70 : -14)*age*age;
                if ((floor || ceiling) && profile.blood && i%4 == 0) {
                    int bone = (white & 0xffffff)|((int)(fade*240)<<24);
                    float dx = (float)Math.cos(a)*4, dy = (float)Math.sin(a)*4;
                    line(c,x-dx,py-dy,x+dx,py+dy,bone,2);
                    circle(c,x-dx,py-dy,1.7f,bone); circle(c,x+dx,py+dy,1.7f,bone);
                } else if (floor || ceiling || spikes || laser || saw) {
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
            float shift=layout.extra;int mark=section(c,shift);boolean ready=physics.canJump();int remaining=physics.jumpAllowanceRemaining();
            String status=remaining==0?"LEVEL BUDGET USED":physics.jumpsRemaining()==0?"DESCEND TO RECOVER":ready?"READY":"WAIT";
            String accessible=remaining<0?t("Jump. Burst %d of %d. No total limit.",physics.jumpsRemaining(),physics.maxBurstJumps()):t("Jump. Burst %d of %d. Stage budget %d of %d remaining.",physics.jumpsRemaining(),physics.maxBurstJumps(),remaining,physics.jumpLimit());
            button(c,"jump",24,744,456,816,accessible+" "+t(status),ready?gold:muted,18);
            if(ready)rect(c,24,744,456,816,13,0xff363427);
            text(c,"JUMP",44,774,19,ready?gold:muted);
            for(int i=0;i<physics.maxBurstJumps();i++)circle(c,194+i*26,770,8,i<physics.jumpsRemaining()?gold:border);
            paint.setTextAlign(Paint.Align.RIGHT);fitText(c,remaining<0?"NO TOTAL LIMIT":t("%d jumps left",remaining),436,774,14,muted,160);paint.setTextAlign(Paint.Align.LEFT);
            String ability=physics.staticJumpReady?"Static charged: next jump 0.8×":physics.ropeSteeringScale()<1?"Static current":physics.ropeAbilityTime>0&&physics.lastSupportRope==RopeCatalog.SPRING?"Spring rebound 1.5×":"";
            String feedback=t(status)+(remaining!=0&&!ability.isEmpty()?" / "+t(ability):"");
            fitText(c,feedback,44,797,12,remaining==0?coral:physics.staticJumpReady?mint:muted,392);
            float progress=remaining==0?0:physics.jumpsRemaining()==0?physics.burstRecoveryProgress():1-clamp(physics.jumpRemaining()/.30f,0,1);
            rect(c,44,804,436,808,2,0x44f6d477);rect(c,44,804,44+392*progress,808,2,ready?gold:mint);endSection(c,mark,shift);
        }

        private String contextualCue(){
            if(physics.arcade||physics.daily||runMirror||physics.stage.index!=0||profile.stageWins[0]>0||physics.elapsed>6||physics.won||physics.lost)return "";
            if(!steeredThisRun&&physics.elapsed<3.2f)return UxRules.controlCue(motionEnabled);
            return physics.jumpsUsed()==0?"Jump when ready. Fall to refill.":"";
        }
        private String starStatus(int bit){return (resultStarsNew&bit)!=0?"New":(resultStarsThisRun&bit)!=0?"Earned":(resultStarsBefore&bit)!=0?"Saved":"Not yet";}
        private void masteryRows(Canvas c){
            int total=profile.stars[physics.stage.index];
            for(int i=0;i<3;i++){
                int bit=1<<i;float top=318+i*65;boolean obtained=(total&bit)!=0;
                rect(c,44,top,436,top+58,12,(resultStarsNew&bit)!=0?0xff29483c:0xff202b29);
                FalleyUi.icon(c,"star",65,top+28,17,obtained?gold:border);
                fitText(c,UxRules.starLabel(bit),92,top+24,18,white,225);
                String detail=i==0?t("Finish the stage"):i==1?t("≤ %d jumps",physics.stage.parJumps):t("Land in the gold centre");
                copy(c,detail,92,top+46,242,12,1,muted);
                paint.setTextAlign(Paint.Align.RIGHT);fitText(c,starStatus(bit),421,top+31,12,(resultStarsNew&bit)!=0?mint:muted,90);paint.setTextAlign(Paint.Align.LEFT);
            }
        }


        private void drawOverlay(Canvas c) {
            rect(c,0,0,480,layout.height,0,0xc8171b1c);int mark=section(c,layout.extra*.5f);
            if(screen==PAUSE){
                rect(c,52,202,428,636,20,panel);center(c,"PAUSED",240,249,31,white);
                center(c,physics.arcade?"ARCADE":physics.daily?"DAILY DROP":physics.stage.title,240,280,15,muted);
                button(c,"resume",74,308,406,382,"RESUME",mint,20);button(c,"restart",74,394,406,468,"RESTART",white,18);
                button(c,"settings",74,480,234,554,"SETTINGS",white,15);button(c,"home",246,480,406,554,"HOME",muted,15);endSection(c,mark,layout.extra*.5f);return;
            }
            if(!physics.won){
                rect(c,24,188,456,718,21,panel);
                center(c,physics.arcade?"RUN ENDED":"FALL ENDED",240,237,31,coral);
                center(c,t("HIT BY %s",t(physics.deathReason)),240,276,19,white);
                String progress=physics.arcade?t("WAVE %d",physics.wave):t("%d%% OF THE DROP",Math.max(0,Math.min(100,(int)(100*(physics.y-310)/(physics.groundY()-physics.radius()-310)))));
                center(c,progress,240,307,15,muted);
                text(c,"Try this",44,350,13,mint);copy(c,UxRules.deathTip(physics.deathReason),44,381,392,18,3,white);
                center(c,t("SCORE %d",physics.score)+" / "+t("%.1fs / %d JUMPS",physics.elapsed,physics.jumpsUsed()),240,459,14,muted);
                if(physics.daily){
                    button(c,"share_daily",44,481,436,535,"SHARE RESULT",white,16);
                    button(c,"restart",44,547,436,613,"TRY AGAIN",mint,20);
                    button(c,"daily",44,625,234,689,"DAILY DROP",white,15);button(c,"home",246,625,436,689,"HOME",muted,16);
                }else{
                    button(c,"restart",44,491,436,561,"TRY AGAIN",mint,21);
                    button(c,physics.arcade?"arcade":"chapters",44,577,234,641,physics.arcade?"CHANGE SETUP":"CHAPTERS",white,15);
                    button(c,"home",246,577,436,641,"HOME",muted,17);
                    if(runMirror)center(c,"PRACTICE / NO STARS",240,684,13,muted);
                }
                endSection(c,mark,layout.extra*.5f);return;
            }
            rect(c,24,128,456,812,21,panel);
            int fresh=Integer.bitCount(resultStarsNew);
            boolean campaign=!physics.daily&&!physics.arcade&&!runMirror;
            center(c,campaign&&fresh>0?(fresh==1?t("1 NEW STAR"):t("%d NEW STARS",fresh)):"LANDED!",240,182,31,mint);
            if(campaign)drawStars(c,profile.stars[physics.stage.index],202,226,14);
            else center(c,"A soft landing. A good bounce.",240,221,15,white);
            center(c,t("SCORE %d",physics.score),240,270,20,gold);
            center(c,t("%.1fs / %d JUMPS",physics.elapsed,physics.jumpsUsed()),240,298,13,muted);
            if(physics.daily){
                center(c,DailyDrop.courseId(runDailyDay),240,351,17,mint);center(c,runControl==2?"MIXED CONTROLS":runControl==1?"MOTION":"TOUCH",240,385,13,muted);
                center(c,t("STREAK %d / BEST %d",profile.dailyStreak,profile.dailyBestStreak),240,440,22,gold);
                button(c,"share_daily",60,528,420,594,"SHARE RESULT",white,17);button(c,"restart",44,624,436,694,"TRY AGAIN",mint,20);
                button(c,"daily",44,706,234,780,"DAILY DROP",white,16);button(c,"home",246,706,436,780,"HOME",muted,17);
            }else{
                boolean hasNext=!runMirror&&profile.isStageUnlocked(physics.stage.index+1);
                if(campaign){
                    masteryRows(c);
                    if(physics.stage.bossKind>0){copy(c,ChapterStories.outro(physics.stage.chapterIndex),44,544,392,14,2,mint);if(AvatarRewards.pending(profile)>=0)fitText(c,t("UNLOCKED: %s",t(AvatarCatalog.name(AvatarRewards.pending(profile)))),44,599,15,gold,392);}
                    else {int reward=UxRules.nextRewardAvatar(profile);copy(c,reward<0?"Chase the stars":t("%d stages until %s",4-UxRules.completedInChapter(profile,reward-1),t(AvatarCatalog.name(reward))),44,561,392,16,2,mint);}
                }else{
                    center(c,"Practice only",240,356,24,mint);FalleyUi.miniAvatar(c,240,438,38,profile.selectedAvatar,profile.selectedPalette,0,true);copy(c,"Practice does not award stars.",44,539,392,17,3,muted);
                }
                boolean reward=campaign&&AvatarRewards.pending(profile)>=0;
                button(c,reward?"reward_continue":hasNext?"next_stage":"restart",44,624,436,694,reward?"MEET YOUR AVATAR":hasNext?"NEXT STAGE":"REPLAY",mint,20);
                button(c,hasNext?"restart":"chapters",44,706,234,780,hasNext?"REPLAY":"CHAPTERS",white,16);button(c,"home",246,706,436,780,"HOME",muted,17);
            }
            endSection(c,mark,layout.extra*.5f);
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
            if(physics.daily && (runControl==0 && motionEnabled || runControl==1 && (!motionEnabled || touch.dragging())))runControl=2;
            boolean controlled = !motionEnabled || touch.dragging();
            physics.touchControlled(controlled);
            if (controlled){physics.steerTouch(touch.velocity()*(runMirror?-1:1));if(Math.abs(touch.velocity())>25)steeredThisRun=true;}
            else{physics.gx=motion.acceleration(android.os.SystemClock.elapsedRealtimeNanos())*(runMirror?-1:1);if(Math.abs(physics.gx)>40)steeredThisRun=true;}
        }
        void clearTouch() {
            pointer = buttonPointer = privacyPointer = chapterSwipePointer = -1; pressed = null; touch.cancel();
            physics.touchControlled(!motionEnabled); physics.steerTouch(0); physics.gx = 0;
        }
        private void restoreMenu(){if(!profile.languageConfirmed){screen=LANGUAGE;clearTouch();}else enterHome();}
        private void enterHome() {
            owner.stopEffects();homeElapsed=0;clearTouch();selectHomeChapter(UxRules.continueStage(profile)/4);
            if(profile.languageConfirmed&&AvatarRewards.pending(profile)>=0&&saveProfile()){
                rewardNextStage=UxRules.campaignComplete(profile)?-1:UxRules.continueStage(profile);screen=UNLOCK;unlockTime=0;
            }else screen=HOME;
        }
        private void openAvatarReward(){
            if(AvatarRewards.pending(profile)<0)return;
            if(!saveProfile()){Toast.makeText(owner,t("Could not save. Please try again."),Toast.LENGTH_SHORT).show();return;}
            rewardNextStage=physics.stage.index+1<StageCatalog.COUNT&&profile.isStageUnlocked(physics.stage.index+1)?physics.stage.index+1:-1;
            screen=UNLOCK;unlockTime=0;clearTouch();
        }
        private void finishAvatarReward(boolean useNew){
            if(!ProfileStorage.chooseAvatarReward(storage,profile,useNew)){
                Toast.makeText(owner,t("Could not save. Please try again."),Toast.LENGTH_SHORT).show();return;
            }
            if(AvatarRewards.pending(profile)>=0){unlockTime=0;return;}
            if(rewardNextStage>=0&&profile.isStageUnlocked(rewardNextStage)){profile.selectedStage=rewardNextStage;saveProfile();startCampaign();}
            else enterHome();
        }
        private void restart() {
            if(screen==RESULT&&!physics.daily&&!physics.arcade&&!runMirror&&AvatarRewards.pending(profile)>=0){openAvatarReward();if(screen==UNLOCK)rewardNextStage=physics.stage.index;return;}
            physics.configureAvatar(profile.selectedAvatar);if(physics.daily)physics.resetDaily(DailyDrop.seed(runDailyDay));else physics.reset();beginRun();
        }
        private void refreshCampaignSelection(){
            if(screen!=BRIEF||homeStage<0)return;
            profile.selectedStage=homeStage;saveProfile();runMirror=false;practiceExpanded=false;
            physics.configureAvatar(profile.selectedAvatar);physics.reset(homeStage);clearTouch();
        }
        private void startCampaign() {
            if (!profile.isStageUnlocked(profile.selectedStage)) return;
            homeStage=profile.selectedStage;chapterPage=homeStage/4;briefReturn=screen==CHAPTERS?CHAPTERS:HOME;
            runMirror=false;practiceExpanded=false;runDailyDay=DailyDrop.NO_DAY;physics.configureAvatar(profile.selectedAvatar);physics.reset(profile.selectedStage);screen=BRIEF;clearTouch();
        }
        private void openDaily(){
            long day=DailyDrop.today();dailyClockValid=DailyDrop.validDay(day)&&day>=profile.lastObservedDay;profile.beginDaily(day);saveProfile();screen=DAILY;clearTouch();
        }
        private void startDaily(){
            long day=DailyDrop.today();
            if(!DailyDrop.validDay(day)){dailyClockValid=false;screen=DAILY;clearTouch();return;}
            dailyClockValid=day>=profile.lastObservedDay;profile.beginDaily(day);runMirror=false;runDailyDay=day;saveProfile();physics.configureAvatar(profile.selectedAvatar);physics.resetDaily(DailyDrop.seed(day));beginRun();
        }
        private void beginRun() {
            owner.stopEffects(); soundCue.reset(physics.soundSequence);
            clearTouch(); accumulator = 0; Arrays.fill(trailValid,false); trailIndex = 0;
            burstTime=10;resultRecorded=false;lastLandingCue=0;runControl=motionEnabled?1:0;
            resultStarsBefore=physics.daily||physics.arcade?0:profile.stars[physics.stage.index];resultStarsThisRun=resultStarsNew=0;contextCue="";steeredThisRun=false;resumeMotion();screen=PLAY;
        }
        private void resumeMotion() {
            if (motionEnabled) { motion.resetSamples(); physics.vx = 0; physics.gx = 0; }
        }
        private void jump() { if (screen == PLAY && !physics.won && physics.jump()) cue(1); }
        void back() {
            if (screen == PLAY) { owner.stopEffects(); screen = PAUSE; }
            else if (screen == PAUSE) { resumeMotion(); screen = PLAY; }
            else if(screen==UNLOCK){finishAvatarReward(false);}
            else if (screen == PRIVACY) screen = SETTINGS;
            else if(screen==LANGUAGE){if(!profile.languageConfirmed)return;screen=SETTINGS;}
            else if(screen==HELP)screen=helpReturn;
            else if(screen==BRIEF)screen=briefReturn;
            else if (screen == SETTINGS) { if (settingsReturn == PLAY) resumeMotion(); screen = settingsReturn; }
            else enterHome();
            clearTouch(); accumulator = 0; invalidate();
        }
        void activate(String id) {
            if(id.equals("play")){if(screen==HOME&&homeStage<0){briefReturn=HOME;screen=BRIEF;clearTouch();}else{int selected=screen==HOME?homeStage:UxRules.continueStage(profile);if(!profile.isStageUnlocked(selected))return;profile.selectedStage=selected;saveProfile();startCampaign();}}
            else if (id.equals("restart")) restart();
            else if(id.equals("daily"))openDaily();
            else if(id.equals("start_daily"))startDaily();
            else if(id.equals("share_daily") && screen==RESULT && physics.daily)DailyShare.send(owner,physics,runDailyDay,runControl,profile.selectedAvatar,profile.language);
            else if(id.equals("wardrobe")){if(screen!=WARDROBE)closetTab=0;screen=WARDROBE;clearTouch();}
            else if(id.startsWith("closet_tab_"))closetTab=Math.max(0,Math.min(1,Integer.parseInt(id.substring(11))));
            else if(id.equals("avatar_preview")){avatarPreviewTime=0;}
            else if(id.startsWith("avatar_")){int choice=Integer.parseInt(id.substring(7));if(Cosmetics.isAvatarUnlocked(profile,choice)){profile.selectedAvatar=choice;saveProfile();}}
            else if(id.equals("reward_continue")&&screen==RESULT)openAvatarReward();
            else if(id.equals("reward_use")&&screen==UNLOCK)finishAvatarReward(true);
            else if(id.equals("reward_keep")&&screen==UNLOCK)finishAvatarReward(false);
            else if(id.equals("practice_options")&&screen==BRIEF)practiceExpanded=!practiceExpanded;
            else if(id.equals("begin_campaign")&&screen==BRIEF)beginRun();
            else if((id.equals("control_touch")||id.equals("control_motion"))&&(screen==BRIEF||screen==ARCADE_SETUP)){boolean wanted=id.equals("control_motion");if((!wanted||hasMotion)&&motionEnabled!=wanted)owner.toggleMotion();}
            else if(id.equals("standard_run")&&screen==BRIEF)runMirror=false;
            else if(id.equals("mirror_run")&&screen==BRIEF)runMirror=true;
            else if (id.equals("arcade")) { owner.stopEffects(); screen = ARCADE_SETUP; setupMessage = 0; clearTouch(); }
            else if (id.equals("start_arcade")) { runMirror=false;physics.configureAvatar(profile.selectedAvatar);physics.resetArcade(profile.arcadeDifficulty,profile.arcadeHazards); beginRun(); }
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
            else if (id.equals("chapters")) { int chapter=physics.stage.chapterIndex;enterHome();if(screen==HOME){selectHomeChapter(chapter);briefReturn=HOME;screen=BRIEF;refreshCampaignSelection();} }
            else if (id.equals("prev_chapters")){selectHomeChapter(chapterPage-1);refreshCampaignSelection();}
            else if (id.equals("next_chapters")){selectHomeChapter(chapterPage+1);refreshCampaignSelection();}
            else if (id.startsWith("stage_")) {
                int requestedStage = Integer.parseInt(id.substring(6));
                if (!profile.isStageUnlocked(requestedStage)) return;
                profile.selectedStage = requestedStage;
                saveProfile();homeStage=requestedStage;if(screen==BRIEF)refreshCampaignSelection();else startCampaign();
            } else if (id.equals("next_stage") && !physics.daily && !physics.arcade && !runMirror && physics.won && physics.stage.index < StageCatalog.COUNT-1) {
                if(AvatarRewards.pending(profile)>=0){openAvatarReward();return;}
                if (!profile.isStageUnlocked(physics.stage.index+1)) return;
                profile.selectedStage = physics.stage.index+1; saveProfile(); startCampaign();
            }
            else if (id.equals("help")) { helpReturn=screen==SETTINGS?SETTINGS:HOME;helpPage = 0; helpMotion = motionEnabled; screen = HELP; clearTouch(); }
            else if (id.equals("help_touch")) helpMotion = false;
            else if (id.equals("help_motion")) helpMotion = true;
            else if (id.equals("help_prev")) helpPage = Math.max(0,helpPage-1);
            else if (id.equals("help_next")) helpPage = Math.min(5,helpPage+1);
            else if(id.equals("settings")){if(screen!=SETTINGS){owner.stopEffects();settingsReturn=screen==PLAY||screen==PAUSE?PAUSE:HOME;screen=SETTINGS;clearTouch();}}
            else if (id.equals("privacy")) { screen = PRIVACY; privacyScroll = 0; clearTouch(); }
            else if (id.equals("privacy_web") && screen == PRIVACY) owner.openPrivacyLink(false);
            else if (id.equals("privacy_email") && screen == PRIVACY) owner.openPrivacyLink(true);
            else if (id.equals("language")) { screen = LANGUAGE; clearTouch(); }
            else if(id.equals("language_done")){boolean first=!profile.languageConfirmed;profile.languageConfirmed=true;saveProfile();if(first)enterHome();else screen=SETTINGS;}
            else if (id.startsWith("language_")) { profile.language = FalleyStrings.sanitize(Integer.parseInt(id.substring(9))); saveProfile(); }
            else if (id.equals("back")) back();
            else if (id.equals("pause")) { owner.stopEffects(); screen = PAUSE; clearTouch(); }
            else if (id.equals("resume")) { resumeMotion(); screen = PLAY; clearTouch(); }
            else if (id.equals("jump")) { jump(); return; }
            else if (id.equals("motion")) owner.toggleMotion();
            else if (id.startsWith("preset_")) {
                profile.preset = Integer.parseInt(id.substring(7));
                motion.setPreset(profile.preset); saveProfile();
            }
            else if(id.equals("music")){profile.music=!profile.music;owner.setMusicEnabled(profile.music);saveProfile();}
            else if (id.equals("sound")) { profile.sound = !profile.sound; owner.setSoundEnabled(profile.sound); saveProfile(); }
            else if (id.equals("haptics")) { profile.haptics = !profile.haptics; saveProfile(); }
            else if (id.equals("blood")) { profile.blood = !profile.blood; saveProfile(); }
            owner.updateMusic(); cue(0); accumulator = 0; invalidate(); menuAccessibility.contentChanged();
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
                    case HOME: return t("Falley home");
                    case SETTINGS: return t("Settings");
                    case HELP: return t("How to play");
                    case PAUSE: return t("Paused");
                    case RESULT: return t("Result");
                    case CHAPTERS: return t("CHAPTER %d / %d",chapterPage+1,StageCatalog.CHAPTER_COUNT);
                    case DAILY: return t("DAILY DROP");
                    case WARDROBE: return t("Collection");
                    case UNLOCK: return t("New avatar unlocked");
                    case BRIEF: return homeStage<0?t("Chapter locked"):t(physics.stage.title);
                    case ARCADE_SETUP: return t("Arcade setup");
                    case PRIVACY: return t(PrivacyPolicy.TITLE);
                    case LANGUAGE: return t("Language selection");
                    case SPLASH: return t("Falley by Perf Games");
                    default: return t("Falley gameplay");
                }
            }
            private String content() {
                if (screen == PRIVACY) {
                    StringBuilder policy = new StringBuilder(t(PrivacyPolicy.TITLE)).append('\n').append(t(PrivacyPolicy.PUBLISHER)).append('\n').append(t(PrivacyPolicy.EFFECTIVE_DATE)).append('\n').append(t(PrivacyPolicy.UPDATED_DATE));
                    for (String[] section : PrivacyPolicy.SECTIONS) policy.append("\n\n").append(t(section[0])).append('\n').append(t(section[1]));
                    return policy.toString();
                }
                if (screen == HELP) return t(helpMotion ? "MOTION" : "TOUCH")+". "+t(helpTitle())+". "+t(helpExplanation())+" "+t(helpTip());
                if(screen==BRIEF&&homeStage<0)return t("CHAPTER %d / %d",chapterPage+1,StageCatalog.CHAPTER_COUNT)+". "+t(StageCatalog.get(chapterPage*4).chapterTitle)+". "+t("Chapter locked")+". "+t("Finish the previous stage");
                if(screen==BRIEF)return t(physics.stage.title)+". "+t(ChapterStories.intro(physics.stage.chapterIndex))+" "+t(mechanicCopy(physics.stage))+" "+t(AvatarCatalog.name(profile.selectedAvatar))+". "+t(AvatarCatalog.trait(profile.selectedAvatar))+". "+t(AvatarCatalog.detail(profile.selectedAvatar))+" "+t("PAR %d JUMPS",physics.stage.parJumps);
                if(screen==ARCADE_SETUP)return t("Build your survival run.")+" "+t("Tap cards to add or remove hazards.")+" "+t("Special ropes from Wave 2")+". "+t(UxRules.controlCue(motionEnabled));
                if(screen==DAILY)return DailyDrop.courseId(DailyDrop.today())+". "+t("NEW COURSE AT 00:00 UTC")+". "+t("STREAK %d / BEST %d",profile.dailyStreak,profile.dailyBestStreak);
                if(screen==LANGUAGE)return t("Choose your language")+". "+(!profile.languageConfirmed?t("English is selected by default.")+" ":"")+t("Your choice stays on this device.");
                if(screen==WARDROBE){String detail=t("Collection")+". "+t(AvatarCatalog.name(profile.selectedAvatar))+". "+t(AvatarCatalog.trait(profile.selectedAvatar))+". "+t(AvatarCatalog.detail(profile.selectedAvatar))+". "+t(AvatarCatalog.story(profile.selectedAvatar))+". "+t("Ropes change within each course. Read their colour and symbol. Daily uses Classic.");if(closetTab==1)for(int type=0;type<RopeCatalog.COUNT;type++)detail+=" "+t(RopeCatalog.name(type))+". "+t(RopeCatalog.description(type));return detail;}
                if(screen==UNLOCK)return t("New avatar unlocked")+". "+t(AvatarCatalog.name(AvatarRewards.pending(profile)))+". "+t(AvatarCatalog.trait(AvatarRewards.pending(profile)))+". "+t(AvatarCatalog.detail(AvatarRewards.pending(profile)))+". "+t("Current avatar: %s",t(AvatarCatalog.name(profile.selectedAvatar)));
                if(screen==PLAY&&!contextCue.isEmpty())return t(contextCue);
                if(screen==RESULT){
                    String result=physics.won?t("Landed. Score %d. %.1f seconds.",physics.score,physics.elapsed):t("Run ended. Hit by %s. Score %d. %.1f seconds.",t(physics.deathReason),physics.score,physics.elapsed)+" "+t(UxRules.deathTip(physics.deathReason));
                    if(physics.won&&!physics.arcade&&!physics.daily&&!runMirror)for(int bit:new int[]{1,2,4})result+=" "+t(UxRules.starLabel(bit))+": "+t(starStatus(bit))+".";
                    if(runMirror)result+=" "+t("Practice does not award stars.");return result;
                }
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
                event.getText().add(spoken(virtualId == CONTENT ? content() : hit(virtualId).label));
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
                    node.setClassName("android.view.ViewGroup"); node.setContentDescription(spoken(screenName()));
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
                    if (screen == CHAPTERS || screen == BRIEF) {
                        node.setScrollable(true);
                        if (chapterPage+1 < StageCatalog.CHAPTER_COUNT) node.addAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD);
                        if (chapterPage > 0) node.addAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD);
                    }
                    if (screen == HELP) {
                        node.setScrollable(true);
                        if (helpPage < 5) node.addAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD);
                        if (helpPage > 0) node.addAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD);
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
                    node.setClassName("android.widget.TextView"); node.setText(spoken(content())); node.setEnabled(true);
                    bounds(node,24,128,446,screen == PRIVACY ? 632+layout.extra : layout.contentBottom());
                    if (screen == PRIVACY) {
                        node.setScrollable(true);
                        if (privacyScroll < privacyScrollLimit) node.addAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD);
                        if (privacyScroll > 0) node.addAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD);
                    }
                } else {
                    Hit button = hit(virtualId);
                    node.setClassName(button.checkable ? "android.widget.Switch" : "android.widget.Button");
                    node.setText(spoken(button.label)); node.setEnabled(button.enabled); node.setClickable(button.enabled);
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
                if (screen == HELP && (action == AccessibilityNodeInfo.ACTION_SCROLL_FORWARD || action == AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD)) {
                    activate(action == AccessibilityNodeInfo.ACTION_SCROLL_FORWARD ? "help_next" : "help_prev"); return true;
                }
                if ((screen == CHAPTERS || screen == BRIEF) && (action == AccessibilityNodeInfo.ACTION_SCROLL_FORWARD || action == AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD)) {
                    activate(action == AccessibilityNodeInfo.ACTION_SCROLL_FORWARD ? "next_chapters" : "prev_chapters"); return true;
                }
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
                    if (next == NONE && content() != null && x >= 24 && x <= 446 && y >= 128 && y <= (screen == PRIVACY ? 632+layout.extra : layout.contentBottom())) next = CONTENT;
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
                if ((screen == CHAPTERS || screen == BRIEF) && chapterSwipePointer < 0 && y >= 128 && y <= 714) {
                    chapterSwipePointer = event.getPointerId(index); chapterSwipeX = x;
                }
                for (int i = buttons.size()-1; i >= 0; i--) if (buttons.get(i).contains(x,y)) {
                    if (buttonPointer < 0 && buttons.get(i).enabled) { pressed = buttons.get(i); buttonPointer = event.getPointerId(index); }
                    return true;
                }
                if (screen == PRIVACY && privacyPointer < 0 && x >= 24 && x <= 456 && y >= 128 && y <= 632+layout.extra) {
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
                if (id == chapterSwipePointer) {
                    chapterSwipePointer = -1;
                    if ((screen == CHAPTERS || screen == BRIEF) && Math.abs(x-chapterSwipeX) > 70) {
                        buttonPointer = -1; pressed = null;
                        activate(x < chapterSwipeX ? "next_chapters" : "prev_chapters"); return true;
                    }
                }
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
