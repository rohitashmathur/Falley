#!/usr/bin/env python3
"""Check deterministic music PCM and actual playback controllers with fake Android media services.

No emulator is needed. This checks lifecycle/focus/error/resource behaviour; speaker balance and
hardware focus interactions still need an Android device. Use --jdk-root for a local JDK17.
"""
from pathlib import Path
import argparse
import os
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[1]
STUBS = {
'android/annotation/TargetApi.java': '''package android.annotation;import java.lang.annotation.*;@Retention(RetentionPolicy.CLASS)@Target({ElementType.TYPE,ElementType.METHOD,ElementType.CONSTRUCTOR})public @interface TargetApi{int value();}''',
'android/content/Context.java': '''package android.content;import android.media.AudioManager;public class Context{public static final String AUDIO_SERVICE="audio";public final AudioManager manager=new AudioManager();public Context getApplicationContext(){return this;}public Object getSystemService(String key){return manager;}}''',
'android/os/Build.java': '''package android.os;public class Build{public static class VERSION{public static int SDK_INT=35;}}''',
'android/media/AudioAttributes.java': '''package android.media;public class AudioAttributes{public static final int USAGE_GAME=14,CONTENT_TYPE_MUSIC=2,CONTENT_TYPE_SONIFICATION=4;public static class Builder{public Builder setUsage(int i){return this;}public Builder setContentType(int i){return this;}public AudioAttributes build(){return new AudioAttributes();}}}''',
'android/media/AudioFocusRequest.java': '''package android.media;public class AudioFocusRequest{public AudioManager.OnAudioFocusChangeListener listener;public static class Builder{private final AudioFocusRequest r=new AudioFocusRequest();public Builder(int i){}public Builder setAudioAttributes(AudioAttributes a){return this;}public Builder setOnAudioFocusChangeListener(AudioManager.OnAudioFocusChangeListener l){r.listener=l;return this;}public Builder setWillPauseWhenDucked(boolean b){return this;}public AudioFocusRequest build(){return r;}}}''',
'android/media/AudioManager.java': '''package android.media;public class AudioManager{public static final int AUDIOFOCUS_GAIN=1,AUDIOFOCUS_LOSS=-1,AUDIOFOCUS_LOSS_TRANSIENT=-2,AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK=-3,AUDIOFOCUS_REQUEST_GRANTED=1,STREAM_MUSIC=3;public int requests,abandons,status=1;public OnAudioFocusChangeListener listener;public interface OnAudioFocusChangeListener{void onAudioFocusChange(int change);}public int requestAudioFocus(OnAudioFocusChangeListener l,int stream,int gain){listener=l;requests++;return status;}public int requestAudioFocus(AudioFocusRequest r){listener=r.listener;requests++;return status;}public int abandonAudioFocus(OnAudioFocusChangeListener l){abandons++;return 1;}public int abandonAudioFocusRequest(AudioFocusRequest r){abandons++;return 1;}public void event(int change){if(listener!=null)listener.onAudioFocusChange(change);}}''',
'android/media/MediaPlayer.java': '''package android.media;import android.content.Context;public class MediaPlayer{public static int created,releases;public static MediaPlayer latest;public static boolean failCreate;public boolean playing,loop,released;public float left,right;private OnErrorListener error;public interface OnErrorListener{boolean onError(MediaPlayer p,int a,int b);}public static MediaPlayer create(Context c,int id,AudioAttributes a,int session){if(failCreate)return null;created++;latest=new MediaPlayer();return latest;}public void setOnErrorListener(OnErrorListener l){error=l;}public void setLooping(boolean b){loop=b;}public boolean isPlaying(){if(released)throw new IllegalStateException();return playing;}public void start(){if(released)throw new IllegalStateException();playing=true;}public void pause(){playing=false;}public void setVolume(float l,float r){left=l;right=r;}public void release(){if(released)throw new IllegalStateException("double release");released=true;playing=false;releases++;}public void fail(){if(error!=null)error.onError(this,1,0);}}''',
'android/media/SoundPool.java': '''package android.media;import android.content.Context;public class SoundPool{public static int released,plays;public int next;public OnLoadCompleteListener listener;public interface OnLoadCompleteListener{void onLoadComplete(SoundPool p,int id,int status);}public static class Builder{public Builder setMaxStreams(int i){return this;}public Builder setAudioAttributes(AudioAttributes a){return this;}public SoundPool build(){return new SoundPool();}}public void setOnLoadCompleteListener(OnLoadCompleteListener l){listener=l;}public int load(Context c,int id,int priority){int sample=++next;if(listener!=null)listener.onLoadComplete(this,sample,0);return sample;}public int play(int id,float l,float r,int priority,int loop,float rate){plays++;return plays;}public void stop(int stream){}public void release(){released++;}}''',
'com/perfgames/ropelab/R.java': '''package com.perfgames.ropelab;public class R{public static class raw{public static final int falley_retro_loop=1,falley_fire_launch=2,falley_fire_burn=3,falley_laser_zap=4,falley_lightning_crack=5,falley_saw_motor=6,falley_saw_impact=7,falley_ui=8,falley_jump=9,falley_win=10,falley_loss=11,falley_rope=12;}}''',
'com/perfgames/ropelab/MusicLifecycleTest.java': '''package com.perfgames.ropelab;
import android.content.Context;import android.media.*;import android.os.Build;
public class MusicLifecycleTest{
 private static int checks;
 private static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
 private static void scenario(int api){
  Build.VERSION.SDK_INT=api;Context c=new Context();FalleyAudio a=new FalleyAudio(c);
  int made=MediaPlayer.created,released=MediaPlayer.releases,pools=SoundPool.released;
  a.setMusicEnabled(true);a.setMusicPlaying(true);check(!a.isMusicPlaying()&&MediaPlayer.created==made,"no playback before foreground");
  a.setForeground(true);check(a.isMusicPlaying()&&MediaPlayer.created==made+1,"one foreground player starts");
  check(MediaPlayer.latest.loop&&Math.abs(MediaPlayer.latest.left-.16f)<.0001,"quiet dedicated loop");
  int starts=a.musicStartCount();for(int i=0;i<1000;i++){a.setMusicPlaying(true);a.setForeground(true);a.setMusicEnabled(true);}check(a.musicStartCount()==starts&&MediaPlayer.created==made+1,"idempotent draw updates do not restart or allocate");
  a.setEnabled(false);check(a.isMusicPlaying()&&!a.isEnabled(),"SFX mute does not mute music");
  a.stopAll();check(a.isMusicPlaying(),"effect reset does not restart soundtrack");
  a.setMusicEnabled(false);check(!a.isMusicPlaying()&&MediaPlayer.releases==released+1,"music off releases its player");
  a.setEnabled(true);int played=SoundPool.plays;a.generic(1);check(SoundPool.plays==played+1,"SFX remains playable while music off");
  a.setMusicEnabled(true);check(a.isMusicPlaying()&&MediaPlayer.created==made+2,"music can be enabled independently");
  a.setMusicPlaying(false);check(!a.isMusicPlaying(),"pause screen silences music");
  c.manager.event(AudioManager.AUDIOFOCUS_GAIN);check(!a.isMusicPlaying(),"late focus gain cannot play on pause");
  a.setMusicPlaying(true);check(a.isMusicPlaying()&&MediaPlayer.created==made+2,"resume reuses a single prepared loop");
  a.setForeground(false);check(!a.isMusicPlaying()&&!a.isForeground(),"background suspends both audio routes");
  c.manager.event(AudioManager.AUDIOFOCUS_GAIN);check(!a.isMusicPlaying(),"focus callback cannot resume background audio");
  a.setForeground(true);check(a.isMusicPlaying(),"foreground resumes eligible music");
  c.manager.event(AudioManager.AUDIOFOCUS_LOSS_TRANSIENT);check(!a.isMusicPlaying(),"transient focus loss pauses");
  c.manager.event(AudioManager.AUDIOFOCUS_GAIN);check(a.isMusicPlaying(),"focus restoration resumes only eligible foreground music");
  c.manager.event(AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK);check(MediaPlayer.latest.left<.05f,"duck keeps background music quiet");
  c.manager.event(AudioManager.AUDIOFOCUS_GAIN);check(Math.abs(MediaPlayer.latest.left-.16f)<.001,"unduck restores intended gain");
  c.manager.event(AudioManager.AUDIOFOCUS_LOSS);check(!a.isMusicPlaying(),"permanent focus loss stops playback");
  int requests=c.manager.requests;for(int i=0;i<100;i++)a.setMusicPlaying(true);check(!a.isMusicPlaying()&&c.manager.requests==requests,"draw loop does not steal back focus");
  a.setMusicEnabled(false);a.setMusicEnabled(true);check(a.isMusicPlaying(),"explicit user music action can request focus again");
  a.release();check(!a.isMusicPlaying()&&a.isMusicReleased()&&a.isReleased(),"destroy releases music and effects");
  check(MediaPlayer.releases==released+3&&SoundPool.released==pools+1,"every allocated player and pool released once");
  a.release();a.setMusicEnabled(true);a.setMusicPlaying(true);a.setForeground(true);c.manager.event(AudioManager.AUDIOFOCUS_GAIN);check(!a.isMusicPlaying()&&MediaPlayer.created==made+3,"released controller cannot recreate or restart");
 }
 public static void main(String[]args){scenario(24);scenario(35);
  Context denied=new Context();denied.manager.status=0;FalleyMusic m=new FalleyMusic(denied);m.setEnabled(true);m.setPlaying(true);m.setForeground(true);check(!m.isPlaying(),"denied focus produces no music");int requests=denied.manager.requests;for(int i=0;i<100;i++)m.setPlaying(true);check(denied.manager.requests==requests,"focus denial does not spin");denied.manager.status=1;m.setForeground(false);m.setForeground(true);check(m.isPlaying(),"fresh foreground can retry previously denied focus");m.release();
  Context error=new Context();FalleyMusic e=new FalleyMusic(error);e.setEnabled(true);e.setPlaying(true);e.setForeground(true);MediaPlayer.latest.fail();check(!e.isPlaying()&&!e.isAvailable()&&MediaPlayer.latest.released,"decoder error releases and safely disables loop");e.release();
  MediaPlayer.failCreate=true;FalleyMusic missing=new FalleyMusic(new Context());missing.setEnabled(true);missing.setPlaying(true);missing.setForeground(true);check(!missing.isPlaying()&&!missing.isAvailable(),"unavailable decoder fails quietly");missing.release();MediaPlayer.failCreate=false;
  check(.42f*FalleyMusic.VOLUME+3*.55f*.45f<.85f,"music and three maximum effects retain mixer headroom");
  System.out.println("PASS: music/SFX independence, API24/35 focus lifecycle, errors and resource release ("+checks+" checks)");
 }
}'''
}


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--jdk-root', type=Path, default=Path(os.environ.get('JAVA_HOME', '')))
    args = parser.parse_args()
    jdk = args.jdk_root / 'bin'
    if not (jdk / 'javac.exe').exists():
        raise SystemExit('Pass --jdk-root with a JDK17 directory.')
    subprocess.run([sys.executable, str(ROOT / 'scripts/generate_music.py'), '--check'], check=True)
    out = ROOT / 'build/music-lifecycle-tests'
    source = out / 'src'
    classes = out / 'classes'
    classes.mkdir(parents=True, exist_ok=True)
    files = []
    for name, content in STUBS.items():
        path = source / name
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(content, encoding='utf-8')
        files.append(path)
    files += [ROOT / f'app/src/main/java/com/perfgames/ropelab/{name}.java' for name in ['FalleyMusic', 'FalleyAudio']]
    subprocess.run([str(jdk / 'javac.exe'), '-encoding', 'UTF-8', '-d', str(classes), *map(str, files)], check=True)
    subprocess.run([str(jdk / 'java.exe'), '-cp', str(classes), 'com.perfgames.ropelab.MusicLifecycleTest'], check=True)


if __name__ == '__main__':
    main()
