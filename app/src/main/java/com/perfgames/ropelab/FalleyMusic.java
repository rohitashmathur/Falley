package com.perfgames.ropelab;

import android.annotation.TargetApi;
import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;

/** One quiet original loop. The activity controls consent, foreground and screen eligibility. */
public final class FalleyMusic {
    public static final float VOLUME = .16f;
    private final Context context;
    private final AudioManager manager;
    private final AudioAttributes attributes;
    private MediaPlayer player;
    private Object focusRequest;
    private boolean enabled, foreground, eligible, released, unavailable;
    private boolean focusRequested, hasFocus, ducked, focusBlocked;
    private int starts;
    private final AudioManager.OnAudioFocusChangeListener focusListener = change -> {
        synchronized (FalleyMusic.this) {
            if (released || !focusRequested) return;
            if (change == AudioManager.AUDIOFOCUS_GAIN) {
                hasFocus=true;ducked=false;
                reconcile();
            } else if (change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
                ducked=true;
                try{if(player!=null)safeVolume();}
                catch(RuntimeException noPlayer){unavailable=true;releasePlayer();abandonFocus();}
            } else if (change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                hasFocus=false;pausePlayer();
            } else if (change == AudioManager.AUDIOFOCUS_LOSS) {
                hasFocus=false;focusBlocked=true;pausePlayer();abandonFocus();
            }
        }
    };

    public FalleyMusic(Context context) {
        this.context=context.getApplicationContext();
        manager=(AudioManager)this.context.getSystemService(Context.AUDIO_SERVICE);
        attributes=new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build();
    }
    public synchronized void setEnabled(boolean value) {
        if(released||enabled==value)return;
        enabled=value;focusBlocked=false;
        if(!value){releasePlayer();abandonFocus();}else reconcile();
    }
    public synchronized void setForeground(boolean value) {
        if(released||foreground==value)return;
        foreground=value;focusBlocked=false;reconcile();
    }
    public synchronized void setPlaying(boolean value) {
        if(released||eligible==value)return;
        eligible=value;focusBlocked=false;reconcile();
    }
    private boolean desired(){return enabled&&foreground&&eligible&&!released&&!unavailable;}
    private void reconcile() {
        if(!desired()){pausePlayer();abandonFocus();return;}
        if(focusBlocked)return;
        if(!focusRequested&&!requestFocus()){focusBlocked=true;return;}
        if(!hasFocus)return;
        try {
            if(player==null){
                player=MediaPlayer.create(context,R.raw.falley_retro_loop,attributes,0);
                if(player==null){unavailable=true;abandonFocus();return;}
                player.setOnErrorListener((source,what,extra)->{
                    synchronized(FalleyMusic.this){
                        if(source==player){unavailable=true;releasePlayer();abandonFocus();}
                    }
                    return true;
                });
                player.setLooping(true);
            }
            safeVolume();
            if(!player.isPlaying()){player.start();starts++;}
        }catch(RuntimeException audioUnavailable){unavailable=true;releasePlayer();abandonFocus();}
    }
    private boolean requestFocus(){
        if(manager==null)return false;
        try{
            int status;
            if(Build.VERSION.SDK_INT>=26){
                if(focusRequest==null)focusRequest=Focus26.create(attributes,focusListener);
                status=Focus26.request(manager,focusRequest);
            }else status=manager.requestAudioFocus(focusListener,AudioManager.STREAM_MUSIC,AudioManager.AUDIOFOCUS_GAIN);
            hasFocus=status==AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
            focusRequested=hasFocus;
            return hasFocus;
        }catch(RuntimeException focusUnavailable){hasFocus=focusRequested=false;return false;}
    }
    private void abandonFocus(){
        boolean held=focusRequested;focusRequested=hasFocus=ducked=false;
        if(!held||manager==null)return;
        try{
            if(Build.VERSION.SDK_INT>=26&&focusRequest!=null)Focus26.abandon(manager,focusRequest);
            else manager.abandonAudioFocus(focusListener);
        }catch(RuntimeException ignored){ }
    }
    private void safeVolume(){
        if(player==null)return;
        float volume=ducked?VOLUME*.25f:VOLUME;
        player.setVolume(volume,volume);
    }
    private void pausePlayer(){
        try{if(player!=null&&player.isPlaying())player.pause();}
        catch(RuntimeException noPlayer){releasePlayer();}
    }
    private void releasePlayer(){
        if(player==null)return;
        MediaPlayer old=player;player=null;
        try{old.setOnErrorListener(null);}catch(RuntimeException ignored){ }
        try{old.release();}catch(RuntimeException ignored){ }
    }
    public synchronized void release(){
        if(released)return;
        released=true;foreground=eligible=false;releasePlayer();abandonFocus();
    }
    public synchronized boolean isPlaying(){
        try{return player!=null&&!released&&player.isPlaying();}catch(RuntimeException ignored){return false;}
    }
    public synchronized boolean isEnabled(){return enabled;}
    public synchronized boolean isReleased(){return released;}
    public synchronized boolean isAvailable(){return !unavailable;}
    public synchronized int startCount(){return starts;}
    /** Isolates API 26 types from devices on the API 24/25 compatibility path. */
    @TargetApi(26)
    private static final class Focus26{
        static Object create(AudioAttributes attrs,AudioManager.OnAudioFocusChangeListener listener){
            return new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).setAudioAttributes(attrs)
                    .setOnAudioFocusChangeListener(listener).setWillPauseWhenDucked(false).build();
        }
        static int request(AudioManager m,Object r){return m.requestAudioFocus((AudioFocusRequest)r);}
        static void abandon(AudioManager m,Object r){m.abandonAudioFocusRequest((AudioFocusRequest)r);}
    }
}
