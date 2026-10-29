package com.perfgames.falley.tests;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityNodeProvider;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/** External production smoke: framework APIs and exposed virtual accessibility nodes only. */
public final class ReleaseSmokeTest extends Instrumentation {
    private static final String APP="com.perfgames.falley",ACTIVITY="com.perfgames.ropelab.MainActivity";
    private static final String BACKUP_MARKER="release-smoke-original-ready";
    private Activity activity;
    private View canvas;
    private SharedPreferences storage,backup;
    private File output;
    private String mode,shot,expectedVersion;
    private int checks,captures,expectedCode;
    private final StringBuilder log=new StringBuilder();
    private interface Step { void run() throws Exception; }
    private static final class Control {
        final String text;final Rect bounds;final boolean enabled,checked;
        Control(String text,Rect bounds,boolean enabled,boolean checked){this.text=text;this.bounds=bounds;this.enabled=enabled;this.checked=checked;}
    }
    @Override public void onCreate(Bundle arguments){
        super.onCreate(arguments);Bundle args=arguments==null?new Bundle():arguments;
        mode=args.getString("mode","smoke");shot=args.getString("shot","small");expectedCode=Integer.parseInt(args.getString("versionCode","18"));expectedVersion=args.getString("versionName","4.4.1");
        if((!mode.equals("smoke")&&!mode.equals("restore"))||!shot.matches("[a-z0-9-]+"))throw new IllegalArgumentException("Release smoke arguments");start();
    }
    @Override public void onStart(){
        storage=getTargetContext().getSharedPreferences("falley-v1",Context.MODE_PRIVATE);
        backup=getTargetContext().getSharedPreferences("falley-release-smoke-original",Context.MODE_PRIVATE);
        output=new File(getTargetContext().getExternalFilesDir(null),"falley-release-smoke/"+shot);
        Throwable failure=null;
        try{
            require(output.isDirectory()||output.mkdirs(),"release smoke artifact directory ready");
            if(!mode.equals("restore")){File[] oldFiles=output.listFiles();if(oldFiles!=null)for(File file:oldFiles)if(file.isFile()&&(file.getName().endsWith(".png")||file.getName().equals("checks.txt")))require(file.delete(),"prior smoke artifact cleared "+file.getName());}
            if(!mode.equals("restore")){
                require(!backup.getBoolean(BACKUP_MARKER,false),"no interrupted fixture; recover with -RestoreOnly if needed");copy(backup,storage.getAll());require(backup.edit().putBoolean(BACKUP_MARKER,true).commit(),"original production preferences backed up");
                require(storage.edit().clear().putInt("telemetry_flags",0).putBoolean("motion",false).putBoolean("music",true).putBoolean("sound",true).putBoolean("haptics",false).putBoolean("blood",false).putInt("preset",0).commit(),"fresh disposable emulator fixture installed without a saved language");
                identityAndResources();launch();freshLanguage();menusAndSettings();actualCampaign();
            }
        }catch(Throwable error){failure=error;if(activity!=null&&canvas!=null)try{capture("failure");}catch(Throwable diagnostic){failure.addSuppressed(diagnostic);}}
        finally{try{close();restoreOriginal();}catch(Throwable cleanup){if(failure==null)failure=cleanup;else failure.addSuppressed(cleanup);}}
        String summary=failure==null?"PASS: Falley release smoke; checks="+checks+"; captures="+captures+"; viewport="+shot+"; mode="+mode+"; reflection-free production launch/onboarding/navigation/resources and explicitly opted-out diagnostics, original preferences restored; no physical-phone audio or sensor comfort claim\n":"FAIL: Falley release smoke; viewport="+shot+"\n"+android.util.Log.getStackTraceString(failure);
        try(FileOutputStream file=new FileOutputStream(new File(output,"checks.txt"))){file.write((log.toString()+summary).getBytes("UTF-8"));}catch(Exception error){summary+="Artifact log failure: "+error+"\n";}
        Bundle result=new Bundle();result.putString("stream",summary);finish(failure==null?Activity.RESULT_OK:Activity.RESULT_CANCELED,result);
    }
    private void identityAndResources() throws Exception{
        PackageInfo info=getTargetContext().getPackageManager().getPackageInfo(APP,android.content.pm.PackageManager.GET_PERMISSIONS|android.content.pm.PackageManager.GET_PROVIDERS|android.content.pm.PackageManager.GET_META_DATA);
        require(APP.equals(getTargetContext().getPackageName()),"smoke targets the production package only");require((info.applicationInfo.flags&ApplicationInfo.FLAG_DEBUGGABLE)==0,"installed production app is not debuggable");
        require((Build.VERSION.SDK_INT>=28?info.getLongVersionCode():info.versionCode)==expectedCode,"installed production version code matches this release");require(expectedVersion.equals(info.versionName),"installed production version matches this release");
        Set<String> allowed=new HashSet<>(java.util.Arrays.asList("android.permission.INTERNET","android.permission.ACCESS_NETWORK_STATE","android.permission.WAKE_LOCK","com.google.android.finsky.permission.BIND_GET_INSTALL_REFERRER_SERVICE",APP+".DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION"));
        Set<String> requested=new HashSet<>();if(info.requestedPermissions!=null)java.util.Collections.addAll(requested,info.requestedPermissions);
        for(String permission:requested)require(allowed.contains(permission),"production permission is bounded to Firebase network/background/install attribution or the app-scoped receiver: "+permission);
        for(String permission:new String[]{"android.permission.INTERNET","android.permission.ACCESS_NETWORK_STATE","android.permission.WAKE_LOCK"})require(requested.contains(permission),"production retains required normal Firebase permission: "+permission);
        if(requested.contains(APP+".DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION")){
            android.content.pm.PermissionInfo permission=getTargetContext().getPackageManager().getPermissionInfo(APP+".DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION",0);
            require((permission.protectionLevel&android.content.pm.PermissionInfo.PROTECTION_MASK_BASE)==android.content.pm.PermissionInfo.PROTECTION_SIGNATURE,"AndroidX app-scoped receiver permission is signature protected");
        }
        if(info.providers!=null)for(android.content.pm.ProviderInfo provider:info.providers)require(!provider.name.equals("com.google.firebase.provider.FirebaseInitProvider")&&!provider.name.equals("com.google.android.gms.measurement.AppMeasurementContentProvider"),"automatic Firebase/measurement provider is absent from optimized app");
        for(String flag:new String[]{"firebase_data_collection_default_enabled","firebase_analytics_collection_enabled","firebase_crashlytics_collection_enabled","google_analytics_adid_collection_enabled","google_analytics_ssaid_collection_enabled","google_analytics_default_allow_analytics_storage","google_analytics_default_allow_ad_storage","google_analytics_default_allow_ad_user_data","google_analytics_default_allow_ad_personalization_signals"})require(info.applicationInfo.metaData!=null&&info.applicationInfo.metaData.containsKey(flag)&&!info.applicationInfo.metaData.getBoolean(flag,true),"optimized manifest retains disabled collection/ad default: "+flag);
        int appId=getTargetContext().getResources().getIdentifier("google_app_id","string",APP);require(appId!=0&&getTargetContext().getString(appId).contains(":android:"),"optimized app retains its generated Android Firebase configuration without logging its values");
        String[] clips={"falley_fire_launch","falley_fire_burn","falley_laser_zap","falley_lightning_crack","falley_saw_motor","falley_saw_impact","falley_ui","falley_jump","falley_win","falley_loss","falley_rope","falley_retro_loop"};
        for(String name:clips){int id=getTargetContext().getResources().getIdentifier(name,"raw",APP);require(id!=0,"optimized package retains audio resource "+name);try(InputStream stream=getTargetContext().getResources().openRawResource(id)){byte[] header=new byte[12];require(stream.read(header)==12,"packaged audio is readable: "+name);require(header[0]=='R'&&header[1]=='I'&&header[2]=='F'&&header[3]=='F',"packaged audio has a WAV header: "+name);}}
        log.append("Resource names above are inspected by this test only; production audio uses direct IDs. No keep rules are required for this harness.\n");
    }
    private void launch() throws Exception{
        activity=startActivitySync(new Intent().setClassName(APP,ACTIVITY).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));
        ui(() -> {View content=activity.findViewById(android.R.id.content);require(content instanceof ViewGroup&&((ViewGroup)content).getChildCount()>0,"public Activity content hierarchy exposes the game view");canvas=((ViewGroup)content).getChildAt(0);require(canvas.getAccessibilityNodeProvider()!=null,"game view exposes virtual accessibility nodes");});
    }
    private void freshLanguage() throws Exception{
        awaitHost("Language selection",5000);List<Control> nodes=controls();int languages=0;for(Control c:nodes)if(c.text.equals("English")||c.text.equals("हिन्दी")||c.text.equals("Bahasa Indonesia"))languages++;
        require(languages==3,"fresh launch presents exactly three supported language choices");require(find("English",false).checked,"fresh English language choice is selected by default");require(!storage.getBoolean("language_confirmed",false),"onboarding awaits explicit confirmation");capture("fresh-language-english");
        click("CONTINUE",false);awaitHost("Falley home",2000);require(storage.getBoolean("language_confirmed",false)&&storage.getInt("language",-1)==0,"real Continue confirms English and reaches Home");capture("home");
    }
    private void menusAndSettings() throws Exception{
        click("Collection",false);awaitHost("Collection",2000);require(find("AVATARS",false)!=null&&find("ROPES",false)!=null,"Collection exposes the two focused tabs");require(!has("TRAILS",false)&&!has("LANDING",false),"extra Collection tabs are absent");capture("collection-avatars");click("ROPES",false);capture("collection-ropes");
        click("Settings",false);awaitHost("Settings",2000);require(has("Retro music",true),"independent music setting is discoverable");capture("settings");
        click("Retro music",true);require(!storage.getBoolean("music",true)&&storage.getBoolean("sound",false),"music turns off independently of sound effects");click("Sound Effects",false);require(!storage.getBoolean("sound",true)&&!storage.getBoolean("music",true),"sound effects also turn off independently");click("Retro music",true);require(storage.getBoolean("music",false)&&!storage.getBoolean("sound",true),"music can remain on while effects are off");click("Sound Effects",false);
        click("DIAGNOSTICS & ANALYTICS",false);awaitHost("Diagnostics & analytics",2000);
        require(!find("Crash reports",true).checked&&!find("Gameplay analytics",true).checked,"production preserves both explicitly saved OFF choices");
        require(storage.getInt("telemetry_flags",0)==0,"release smoke explicitly keeps production reporting OFF");
        final String[] notice={""};ui(() -> {AccessibilityNodeInfo node=canvas.getAccessibilityNodeProvider().createAccessibilityNodeInfo(10000);require(node!=null,"diagnostics exposes readable consent details");notice[0]=String.valueOf(node.getText());node.recycle();});
        require(notice[0].contains("next app launch")&&notice[0].contains("saved on this device")&&notice[0].contains("No ads"),"optimized diagnostics discloses crash timing, cached reports and excluded advertising");capture("diagnostics-off");
        click("PRIVACY POLICY",false);awaitHost("Falley Privacy Policy",2000);back();awaitHost("Diagnostics & analytics",2000);click("DONE",false);awaitHost("Settings",2000);
        click("Language",true);awaitHost("Language selection",2000);
        click("हिन्दी",false);require(storage.getInt("language",-1)==1,"Hindi choice saves through real touch");capture("language-hindi");click("Bahasa Indonesia",false);require(storage.getInt("language",-1)==2,"Indonesian choice saves through real touch");capture("language-indonesian");click("English",false);click(has("DONE",false)?"DONE":"CONTINUE",false);awaitHost("Settings",2000);
        click("PRIVACY POLICY",false);awaitHost("Falley Privacy Policy",2000);final String[] policy={""};ui(() -> {AccessibilityNodeInfo node=canvas.getAccessibilityNodeProvider().createAccessibilityNodeInfo(10000);require(node!=null,"policy exposes complete readable content");policy[0]=String.valueOf(node.getText());node.recycle();});
        require(policy[0].contains("mathurrohitash16@gmail.com")&&policy[0].contains("https://falley-privacy-policy.vercel.app/"),"release policy includes support and public URL");capture("privacy");back();awaitHost("Settings",2000);click("Play",false);awaitHost("Falley home",2000);
    }
    private void actualCampaign() throws Exception{
        click("Continue stage",true);awaitHost("First light",2000);require(has("TOUCH",false),"campaign briefing exposes Touch controls");capture("briefing");click("TOUCH",false);require(!storage.getBoolean("motion",true),"Touch selection remains saved without sensor requirements");click("START DROP",false);awaitHost("Falley gameplay",2000);require(has("Jump.",true),"actual live gameplay exposes its jump control");capture("gameplay-touch");
        click("Pause",false);awaitHost("Paused",2000);capture("paused");click("Settings",false);awaitHost("Settings",2000);back();awaitHost("Paused",2000);click("RESTART",false);awaitHost("Falley gameplay",2000);
        // Actual Back dispatch exercises the supported API 33+ callback on current Android.
        back();awaitHost("Paused",2000);click("HOME",false);awaitHost("Falley home",2000);require(storage.getInt("stage_wins_0",0)==0,"smoke navigation cannot fabricate a campaign win");capture("home-after-play");
    }
    private List<Control> controls() throws Exception{
        final List<Control> out=new ArrayList<>();ui(() -> {AccessibilityNodeProvider provider=canvas.getAccessibilityNodeProvider();for(int id=1;id<128;id++){AccessibilityNodeInfo node=provider.createAccessibilityNodeInfo(id);if(node==null)break;Rect bounds=new Rect();node.getBoundsInScreen(bounds);out.add(new Control(String.valueOf(node.getText()),bounds,node.isEnabled(),node.isChecked()));node.recycle();}});return out;
    }
    private Control find(String text,boolean prefix) throws Exception{
        String wanted=text.toLowerCase(Locale.ROOT);Control match=null;for(Control c:controls()){String label=c.text.toLowerCase(Locale.ROOT);if((prefix?label.startsWith(wanted):label.equals(wanted))&&(match==null||c.bounds.bottom>match.bounds.bottom))match=c;}
        if(match==null)throw new AssertionError("Missing visible release control "+text+" on "+host());return match;
    }
    private boolean has(String text,boolean prefix) throws Exception{String wanted=text.toLowerCase(Locale.ROOT);for(Control c:controls()){String label=c.text.toLowerCase(Locale.ROOT);if(prefix?label.startsWith(wanted):label.equals(wanted))return true;}return false;}
    private void click(String text,boolean prefix) throws Exception{
        Control c=find(text,prefix);require(c.enabled,"release control is enabled: "+text);require(c.bounds.width()>0&&c.bounds.height()>0,"release control has native screen bounds: "+text);
        long now=SystemClock.uptimeMillis();for(int action:new int[]{MotionEvent.ACTION_DOWN,MotionEvent.ACTION_UP}){MotionEvent event=MotionEvent.obtain(now,SystemClock.uptimeMillis(),action,c.bounds.exactCenterX(),c.bounds.exactCenterY(),0);event.setSource(InputDevice.SOURCE_TOUCHSCREEN);try{sendPointerSync(event);}finally{event.recycle();}SystemClock.sleep(20);}SystemClock.sleep(160);
    }
    private String host() throws Exception{final String[] name={""};ui(() -> {AccessibilityNodeInfo node=canvas.getAccessibilityNodeProvider().createAccessibilityNodeInfo(AccessibilityNodeProvider.HOST_VIEW_ID);require(node!=null,"release host accessibility node exists");name[0]=String.valueOf(node.getContentDescription());node.recycle();});return name[0];}
    private void awaitHost(String expected,long timeout) throws Exception{long deadline=SystemClock.uptimeMillis()+timeout;String actual;do{actual=host();if(expected.equalsIgnoreCase(actual)){require(true,"native release screen is "+expected);return;}SystemClock.sleep(35);}while(SystemClock.uptimeMillis()<deadline);throw new AssertionError("Expected release screen "+expected+", got "+actual);}
    private void back(){sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);SystemClock.sleep(160);}
    private void capture(String name) throws Exception{
        final CountDownLatch committed=new CountDownLatch(1);final boolean[] hardware={false};ui(() -> {hardware[0]=Build.VERSION.SDK_INT>=29&&canvas.isHardwareAccelerated();if(hardware[0])canvas.getViewTreeObserver().registerFrameCommitCallback(committed::countDown);canvas.invalidate();});if(hardware[0])require(committed.await(1500,TimeUnit.MILLISECONDS),"release capture has a submitted window frame: "+name);SystemClock.sleep(160);
        Bitmap image=getUiAutomation().takeScreenshot();require(image!=null,"actual compositor capture "+name);try(FileOutputStream file=new FileOutputStream(new File(output,name+".png"))){require(image.compress(Bitmap.CompressFormat.PNG,100,file),"release screenshot saved "+name);}finally{image.recycle();}captures++;
    }
    private void close() throws Exception{if(activity==null)return;ui(() -> activity.finish());long end=SystemClock.uptimeMillis()+4000;while(!activity.isDestroyed()&&SystemClock.uptimeMillis()<end)SystemClock.sleep(20);require(activity.isDestroyed(),"release Activity closes before preference recovery");activity=null;}
    private void restoreOriginal(){if(!backup.getBoolean(BACKUP_MARKER,false))return;Map<String,Object> original=new HashMap<>(backup.getAll());original.remove(BACKUP_MARKER);copy(storage,original);require(storage.getAll().equals(original),"production preferences restored exactly");require(backup.edit().clear().commit(),"verified release backup marker cleared");}
    @SuppressWarnings("unchecked") private void copy(SharedPreferences dest,Map<String,?> values){SharedPreferences.Editor edit=dest.edit().clear();for(Map.Entry<String,?> e:values.entrySet()){Object v=e.getValue();String k=e.getKey();if(v instanceof String)edit.putString(k,(String)v);else if(v instanceof Integer)edit.putInt(k,(Integer)v);else if(v instanceof Long)edit.putLong(k,(Long)v);else if(v instanceof Float)edit.putFloat(k,(Float)v);else if(v instanceof Boolean)edit.putBoolean(k,(Boolean)v);else if(v instanceof Set)edit.putStringSet(k,new HashSet<>((Set<String>)v));else throw new IllegalArgumentException("Unsupported preference "+k);}require(edit.commit(),"release preference snapshot committed");}
    private void ui(Step step) throws Exception{final Throwable[] error={null};runOnMainSync(() -> {try{step.run();}catch(Throwable e){error[0]=e;}});if(error[0]!=null)throw new Exception("Release smoke framework UI action",error[0]);}
    private void require(boolean value,String message){if(!value)throw new AssertionError(message);checks++;log.append("PASS ").append(message).append('\n');}
}