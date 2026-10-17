package com.perfgames.ropelab;

/** Timed sensor playback: run calibration, stale delivery, symmetry and bias rejection. */
public final class MotionSteeringTest {
    private static final long START = 1_000_000_000L;
    private static int checks;
    private static void check(boolean ok, String message) { checks++; if (!ok) throw new AssertionError(message); }
    private static long feed(MotionSteering m,long start,int rate,float duration,float x,float y,float z,float gyroY) {
        int samples=Math.round(rate*duration);
        for(int i=1;i<=samples;i++) {
            long t=start+Math.round(i*1e9/rate);
            m.acceptGyroscope(t,0,gyroY,0); m.acceptAccelerometer(t,x,y,z);
        }
        return start+Math.round(samples*1e9/rate);
    }
    private static long prime(MotionSteering m,int rate,float x,float y,float z) {
        m.acceptAccelerometer(START,x,y,z);
        return feed(m,START,rate,.4f,x,y,z,0);
    }
    private static MotionSteering tilted(int preset,int rate,float x) {
        MotionSteering m=new MotionSteering(false);m.setPreset(preset);
        long t=prime(m,rate,0,0,9.80665f);feed(m,t,rate,2,x,0,9.3f,0);return m;
    }
    private static void startupAndRunCentering() {
        MotionSteering m=new MotionSteering(true);
        check(!m.hasSample() && m.acceleration()==0 && !m.calibrate(),"no valid gravity sample means no steering or false calibration");
        m.acceptGyroscope(START,0,1,0);
        check(!m.hasSample() && m.acceleration()==0,"gyro-only startup cannot invent a hold pose");
        m.acceptAccelerometer(START,3,1,9.3f);
        check(m.hasSample() && m.acceleration()==0,"first tilted hold sample starts at neutral");
        long t=START;
        for(int i=1;i<=12;i++) {
            t=START+i*16_666_667L;m.acceptGyroscope(t,0,.4f,0);m.acceptAccelerometer(t,3+.02f*(float)Math.sin(i),1,9.3f);
            check(m.acceleration()==0,"the initial200ms settling window never turns a pickup/tap into steering");
        }
        feed(m,t,60,1,3,1,9.3f,0);
        check(Math.abs(m.acceleration())<5,"a tilted natural grip stays neutral after settling");

        // Menu pose is deliberately different from the actual run grip.
        m=new MotionSteering(false);t=prime(m,60,0,0,9.80665f);
        t=feed(m,t,60,1,-3,0,9.3f,0);check(m.acceleration()>50,"old menu pose can produce a turn before the run boundary");
        m.resetSamples();check(!m.hasSample() && m.acceleration()==0,"run/restart/resume boundary clears old pose and output");
        t+=1_000_000_000L;m.acceptAccelerometer(t,4,1,8.9f);t=feed(m,t,60,1,4,1,8.9f,0);
        check(m.hasSample() && Math.abs(m.acceleration())<1,"starting a run centers the new grip instead of the launch/menu pose");
    }
    private static void calibrationPresetsAndRates() {
        MotionSteering right=tilted(MotionSteering.BALANCED,60,-3),left=tilted(MotionSteering.BALANCED,60,3);
        check(right.acceleration()>50 && left.acceleration()<-50,"accelerometer fallback steers both directions after settling");
        check(Math.abs(right.acceleration()+left.acceleration())<1,"opposite tilts around a level neutral are symmetric");
        check(right.calibrate() && right.acceleration()==0,"explicit internal calibration remains immediate");
        feed(right,START+2_400_000_000L,60,1,-3,0,9.3f,0);
        check(Math.abs(right.acceleration())<5,"holding an explicitly calibrated pose stays neutral");
        float loose=tilted(MotionSteering.LOOSE,60,-3).acceleration();
        float balanced=tilted(MotionSteering.BALANCED,60,-3).acceleration();
        float tight=tilted(MotionSteering.TIGHT,60,-3).acceleration();
        check(loose>balanced*1.15f && balanced>tight*1.15f,"Loose/Balanced/Tight retain distinct response gains");
        for(int preset=0;preset<3;preset++) {
            float slow=tilted(preset,30,-3).acceleration(),fast=tilted(preset,120,-3).acceleration();
            check(Math.abs(slow-fast)<15,"response follows elapsed time at30 and120Hz rather than sample count");
        }
        MotionSteering extreme=tilted(MotionSteering.LOOSE,60,-9.8f);
        check(Float.isFinite(extreme.acceleration()) && Math.abs(extreme.acceleration())<=1400,"extreme tilt stays finite and bounded");
    }
    private static void gyroAndInvalidSamples() {
        MotionSteering m=new MotionSteering(true);long t=prime(m,100,0,0,9.80665f),lastAccel=t;
        for(int i=1;i<=20;i++)m.acceptGyroscope(t+i*10_000_000L,0,.8f,0);
        t+=200_000_000L;
        check(m.acceleration()>50,"gyro rotation contributes promptly while the accelerometer reference is fresh");
        float before=m.acceleration();
        m.acceptAccelerometer(lastAccel-1,5,0,9);m.acceptAccelerometer(t+10_000_000L,Float.NaN,0,9);
        m.acceptAccelerometer(t+10_000_000L,0,0,0);m.acceptAccelerometer(t+10_000_000L,50,0,0);
        m.acceptGyroscope(t-1,0,-10,0);m.acceptGyroscope(t+10_000_000L,Float.POSITIVE_INFINITY,0,0);
        check(m.acceleration()==before,"old/invalid/freefall/shake samples cannot alter valid output");
        check(m.acceleration(lastAccel+200_000_000L)>50 && m.acceleration(lastAccel+251_000_000L)==0,
                "clock-aware output rejects an accelerometer reference older than250ms");
        m.acceptGyroscope(lastAccel+300_000_000L,0,.8f,0);
        check(m.acceleration(lastAccel+300_000_000L)==0 && m.acceleration()==0,
                "fresh gyro-only delivery cannot revive steering after its gravity reference goes stale");
        m.resetSamples();m.acceptGyroscope(t+20_000_000L,0,.8f,0);m.acceptGyroscope(t+30_000_000L,0,.8f,0);
        check(!m.hasSample() && m.acceleration()==0,"reset during gyro motion cannot preserve an integrated turn");
        t+=40_000_000L;m.acceptAccelerometer(t,-4,0,9);feed(m,t,100,.6f,-4,0,9,0);
        check(Math.abs(m.acceleration())<1,"post-reset gyro/accelerometer hold uses a fresh tilted neutral");

        m=new MotionSteering(true);t=prime(m,60,0,0,9.80665f);t=feed(m,t,60,1,-3,0,9.3f,0);
        check(m.acceleration()>50,"gap scenario begins with a real turn");
        t+=700_000_000L;m.acceptAccelerometer(t,3,0,9.3f);
        check(m.acceleration()==0,"a delivery gap over500ms immediately clears the old neutral/turn");
        for(int i=1;i<=12;i++){m.acceptAccelerometer(t+i*16_666_667L,3,0,9.3f);check(m.acceleration()==0,"gap recovery has a fresh settling window");}
        feed(m,t+200_000_004L,60,1,3,0,9.3f,0);
        check(Math.abs(m.acceleration())<1,"holding the new pose after a gap does not revive stale steering");
    }
    private static void noiseAndBias() {
        for(boolean gyro:new boolean[]{false,true})for(int preset=0;preset<3;preset++) {
            MotionSteering m=new MotionSteering(gyro);m.setPreset(preset);long t=prime(m,60,2,1,9.5f);float peak=0;
            for(int i=1;i<=480;i++) {
                long now=t+i*16_666_667L;
                m.acceptGyroscope(now,.001f,.003f,-.001f);
                m.acceptAccelerometer(now,2+.03f*(float)Math.sin(i*.37),1+.02f*(float)Math.cos(i*.51),9.5f+.01f*(float)Math.sin(i*.2));
                peak=Math.max(peak,Math.abs(m.acceleration(now)));
            }
            check(peak<10,"ordinary small noise and gyro bias around a tilted grip cannot accumulate into a turn");
        }
    }
    private static void uprightGyroAxis() {
        for(int preset:new int[]{MotionSteering.BALANCED,MotionSteering.TIGHT}) {
            float[] response=new float[2];int index=0;
            for(int direction:new int[]{-1,1}) {
                MotionSteering m=new MotionSteering(true);m.setPreset(preset);
                long t=prime(m,100,0,9.6f,2);
                check(!m.isCentering() && m.acceleration()==0,"upright portrait grip completes settling without a lean command");
                for(int i=1;i<=20;i++)m.acceptGyroscope(t+i*10_000_000L,0,0,-direction);
                response[index++]=m.acceleration(t+200_000_000L);
                check(m.acceleration()*direction>30,"upright hold responds to the physically relevant gyro z rotation in both directions");
            }
            check(Math.abs(response[0]+response[1])<1,"upright gyro z responses are mirror-symmetric for Balanced and Tight");
        }
    }
    public static void main(String[] args) {
        startupAndRunCentering();calibrationPresetsAndRates();gyroAndInvalidSamples();noiseAndBias();uprightGyroAxis();
        System.out.println("PASS: run-grip settling/recentering, opposite tilts/presets/sample rates, gyro/fallback, invalid/stale/gapped delivery, active-gyro reset, tilted-grip noise/bias and upright gyro-z response ("+checks+" checks)");
    }
}
