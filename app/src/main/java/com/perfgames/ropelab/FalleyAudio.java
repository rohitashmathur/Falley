package com.perfgames.ropelab;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.SoundPool;
import java.util.HashSet;

/** Bounded one-shot effects with an independently enabled, lifecycle-gated music player. */
public final class FalleyAudio {
    private static final int[] CLIPS = {R.raw.falley_fire_launch,R.raw.falley_fire_burn,R.raw.falley_laser_zap,
            R.raw.falley_lightning_crack,R.raw.falley_saw_motor,R.raw.falley_saw_impact,R.raw.falley_ui,
            R.raw.falley_jump,R.raw.falley_win,R.raw.falley_loss,R.raw.falley_rope};
    private final Context context;
    private SoundPool pool;
    private final FalleyMusic music;
    private final int[] samples = new int[CLIPS.length], streams = new int[3];
    private final HashSet<Integer> loaded = new HashSet<>();
    private int nextStream, played;
    private boolean enabled = true, foreground, released, unavailable;

    public FalleyAudio(Context context) { this.context = context.getApplicationContext(); music=new FalleyMusic(this.context); }
    public synchronized void setMusicEnabled(boolean value) { music.setEnabled(value); }
    public synchronized void setMusicPlaying(boolean value) { music.setPlaying(value); }
    public synchronized boolean isMusicEnabled() { return music.isEnabled(); }
    public synchronized boolean isMusicPlaying() { return music.isPlaying(); }
    public synchronized boolean isMusicReleased() { return music.isReleased(); }
    public synchronized int musicStartCount() { return music.startCount(); }
    public synchronized void setEnabled(boolean value) {
        enabled = value;
        if (!value) stopAll(); else if (foreground) preload();
    }
    public synchronized void setForeground(boolean value) {
        foreground = value; music.setForeground(value);
        if (!value) stopAll(); else if (enabled) preload();
    }
    private void preload() {
        if (pool != null || released || unavailable) return;
        try {
            pool = new SoundPool.Builder().setMaxStreams(3).setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()).build();
            pool.setOnLoadCompleteListener((source,id,status) -> {
                synchronized (FalleyAudio.this) {
                    if (!released && source == pool && status == 0) loaded.add(id);
                }
            });
            for (int i = 0; i < CLIPS.length; i++) {
                samples[i] = pool.load(context,CLIPS[i],1);
            }
        } catch (RuntimeException noAudio) { unavailable = true; releasePool(); }
    }
    public synchronized void hazard(int event) { if (event >= 1 && event <= 6) play(event-1); }
    public synchronized void generic(int kind) {
        int clip = kind == 1 ? 7 : kind == 2 ? 8 : kind == 3 ? 9 : kind == 4 ? 10 : 6;
        play(clip);
    }
    private void play(int clip) {
        if (!enabled || !foreground || released || unavailable) return;
        preload();
        // An unloaded sample is dropped rather than queued: a pause or restart
        // must never cause stale hazard audio to arrive after loading completes.
        if (pool == null || samples[clip] == 0 || !loaded.contains(samples[clip])) return;
        try {
            if (streams[nextStream] != 0) pool.stop(streams[nextStream]);
            int stream = pool.play(samples[clip],.45f,.45f,1,0,1f);
            streams[nextStream] = stream; nextStream = (nextStream+1)%streams.length;
            if (stream != 0) played++;
        } catch (RuntimeException noAudio) { unavailable = true; releasePool(); }
    }
    public synchronized void stopAll() {
        for (int i = 0; i < streams.length; i++) {
            try { if (pool != null && streams[i] != 0) pool.stop(streams[i]); } catch (RuntimeException ignored) { }
            streams[i] = 0;
        }
    }
    private void releasePool() {
        stopAll();
        if (pool != null) {
            try { pool.setOnLoadCompleteListener(null); pool.release(); } catch (RuntimeException ignored) { }
            pool = null;
        }
        loaded.clear();
    }
    public synchronized void release() { released = true; foreground = false; releasePool(); music.release(); }
    public synchronized int loadedCount() { return loaded.size(); }
    public synchronized int playedCount() { return played; }
    public synchronized boolean isForeground() { return foreground; }
    public synchronized boolean isEnabled() { return enabled; }
    public synchronized boolean isReleased() { return released; }
}
