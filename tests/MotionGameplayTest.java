package com.perfgames.ropelab;

/** Sensor-to-physics acceptance: neutral grip, braking, reversal and real rope catches. */
public final class MotionGameplayTest {
    private static final long START=1_000_000_000L, STEP_NS=4_166_667L;
    private static int checks;
    private static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    private static MotionSteering centered(boolean gyro,float gripX) {
        MotionSteering m=new MotionSteering(gyro);m.acceptAccelerometer(START,gripX,0,9.3f);
        for(int i=1;i<=24;i++){long t=START+i*16_666_667L;m.acceptGyroscope(t,0,0,0);m.acceptAccelerometer(t,gripX,0,9.3f);}return m;
    }
    private static Physics isolated() {
        Physics p=new Physics();p.touchControlled(false);p.y=2000;p.cameraY=1570;
        // Incoming attacks are separate from this isolated horizontal sensor-response measurement.
        try {java.lang.reflect.Field f=Physics.class.getDeclaredField("attackCountdown");f.setAccessible(true);f.setFloat(p,100000);}
        catch(ReflectiveOperationException e){throw new AssertionError(e);}
        // Keep course outcomes/rope recycling outside the horizontal response measurement.
        for(Physics.Rope r:p.ropes){r.cut=12;r.base=1_000_000;for(int i=0;i<Physics.Rope.N;i++)r.y[i]=r.py[i]=r.oldY[i]=r.base;}
        return p;
    }
    private static void levelStep(Physics p) {p.y=2000;p.vy=0;p.cameraY=1570;p.step();}
    private static int phase(MotionSteering m,Physics p,int startStep,int steps,float x) {
        for(int i=1;i<=steps;i++) {
            int step=startStep+i;long now=START+400_000_008L+step*STEP_NS;
            if(step%4==0){m.acceptGyroscope(now,0,0,0);m.acceptAccelerometer(now,x,0,9.3f);}
            p.gx=m.acceleration(now);levelStep(p);
        }
        return startStep+steps;
    }
    private static void neutralAndNoise() {
        for(boolean gyro:new boolean[]{false,true})for(int rate:new int[]{30,120}) {
            MotionSteering m=centered(gyro,2);Physics p=isolated(),environment=isolated();float peak=0;
            for(int i=1;i<=240*8;i++) {
                long now=START+400_000_008L+i*STEP_NS;
                if(i%(240/rate)==0) {
                    m.acceptGyroscope(now,.001f,.003f,-.001f);
                    m.acceptAccelerometer(now,2+.03f*(float)Math.sin(i*.07),.01f*(float)Math.cos(i*.11),9.3f);
                }
                p.gx=m.acceleration(now);levelStep(p);levelStep(environment);peak=Math.max(peak,Math.abs(p.x-environment.x));
            }
            check(!p.lost && peak<2 && Math.abs(p.vx-environment.vx)<1 && Math.abs(p.x-240)<26,"eight-second noisy neutral follows only the bounded intentional gust, without sensor runaway");
        }
    }
    private static void mirrorBrakingAndReverse() {
        float[] travel=new float[2];int index=0;
        for(int direction:new int[]{-1,1}) {
            MotionSteering m=centered(false,0);Physics p=isolated();
            int steps=phase(m,p,0,144,-direction*3);travel[index++]=p.x-240;
            check(!p.lost && (p.x-240)*direction>20 && p.vx*direction>35,"matching phone tilt moves the actual body in either signed direction");
            float speed=Math.abs(p.vx),releaseX=p.x;steps=phase(m,p,steps,120,0);
            check(Math.abs(p.vx)<12 && Math.abs(p.x-releaseX)<20 && Math.abs(p.vx)<speed*.15f,
                    "returning the phone to neutral stops filtered sensor-to-game movement within half a second");
            p=isolated();m=centered(false,0);steps=phase(m,p,0,144,-direction*3);float reversalX=p.x;
            steps=phase(m,p,steps,120,direction*3);
            check(!p.lost && p.vx*direction<-30 && (p.x-reversalX)*direction<0,
                    "opposite tilt reverses real horizontal travel within half a second");
        }
        check(Math.abs(travel[0]+travel[1])<1,"mirrored sensor streams yield symmetric actual-body displacement");
    }
    private static void momentumAndTouchIsolation() {
        for(int direction:new int[]{-1,1}) {
            Physics p=isolated();p.vx=direction*120;float start=p.x;
            for(int i=0;i<120;i++){p.gx=0;levelStep(p);}
            check(Math.abs(p.vx)<1 && Math.abs(p.x-start)<12,"neutral servo brakes a pre-existing rope/input velocity rather than coasting for seconds");
        }
        Physics p=isolated();p.touchControlled(true);p.steerTouch(-220);p.touchControlled(false);
        for(int i=0;i<120;i++){p.gx=0;levelStep(p);}
        check(Math.abs(p.x-240)<1 && p.steeringVelocity()==0,"switching from swipe/current to motion cannot preserve a hidden leftward command");
        p=isolated();p.touchControlled(true);p.gx=-950;
        for(int i=0;i<120;i++){p.steerTouch(0);levelStep(p);}
        check(p.x>244 && Math.abs(p.vx-p.driftVelocity())<.01f && Math.abs(p.driftVelocity())<=35.01f,
                "touch-only mode retains its intentional environmental drift and ignores stale motion input");
    }
    private static void realRopeContacts() {
        for(float startX:new float[]{170,310}) {
            Physics p=new Physics();p.touchControlled(false);p.x=startX;
            Physics.Rope rope=p.ropes[0];int node=startX<240?8:16;
            p.y=rope.y[node]-p.radius()-6;p.vy=160;
            MotionSteering m=centered(false,0);int steps=0;
            while(p.landingPulse==0 && !p.lost && steps<120) {
                long now=START+400_000_008L+(++steps)*STEP_NS;
                if(steps%4==0)m.acceptAccelerometer(now,0,0,9.3f);
                p.gx=m.acceleration(now);p.step();
            }
            check(!p.lost && p.landingPulse>0 && rope.cut<0 && p.vy<0,
                    "off-center fixture actually catches an intact elastic rope and auto-jumps");
            float landingX=p.x;
            for(int i=1;i<=108;i++) {
                long now=START+400_000_008L+(steps+i)*STEP_NS;
                if((steps+i)%4==0)m.acceptAccelerometer(now,0,0,9.3f);
                p.gx=m.acceleration(now);p.step();
            }
            check(!p.lost && Math.abs(p.vx)<10 && Math.abs(p.x-landingX)<20,
                    "a genuine rope's lateral impulse is braked during the following neutral jump instead of becoming persistent drift");
        }
    }
    private static int uprightPhase(MotionSteering m,Physics p,int startStep,int steps,float fromAngle,float toAngle) {
        float omega=-(toAngle-fromAngle)/(steps*Physics.DT);
        for(int i=1;i<=steps;i++) {
            int step=startStep+i;long now=START+400_000_008L+step*STEP_NS;
            if(step%4==0) {
                float angle=fromAngle+(toAngle-fromAngle)*i/steps;
                m.acceptGyroscope(now,0,0,omega);
                m.acceptAccelerometer(now,-9.6f*(float)Math.sin(angle),9.6f*(float)Math.cos(angle),2);
            }
            p.gx=m.acceleration(now);levelStep(p);
        }
        return startStep+steps;
    }
    private static MotionSteering upright(int preset) {
        MotionSteering m=new MotionSteering(true);m.setPreset(preset);m.acceptAccelerometer(START,0,9.6f,2);
        for(int i=1;i<=24;i++){long t=START+i*16_666_667L;m.acceptGyroscope(t,0,0,0);m.acceptAccelerometer(t,0,9.6f,2);}return m;
    }
    private static void uprightPortraitGameplay() {
        float angle=(float)Math.toRadians(15);
        for(int preset:new int[]{MotionSteering.BALANCED,MotionSteering.TIGHT}) {
            MotionSteering m=upright(preset);Physics p=isolated(),environment=isolated();float peak=0;
            for(int i=1;i<=240*6;i++) {
                long now=START+400_000_008L+i*STEP_NS;
                if(i%4==0){m.acceptGyroscope(now,.001f,.001f,.003f);m.acceptAccelerometer(now,.025f*(float)Math.sin(i*.11),9.6f,2);}
                p.gx=m.acceleration(now);levelStep(p);levelStep(environment);peak=Math.max(peak,Math.abs(p.x-environment.x));
            }
            check(!p.lost && peak<2 && Math.abs(p.vx-environment.vx)<1 && Math.abs(p.x-240)<26,"upright Balanced/Tight sensor neutrality stays independent of the bounded deliberate gust");
            float[] travel=new float[2];int index=0;
            for(int direction:new int[]{-1,1}) {
                m=upright(preset);p=isolated();
                int steps=uprightPhase(m,p,0,36,0,direction*angle);
                steps=uprightPhase(m,p,steps,108,direction*angle,direction*angle);
                travel[index++]=p.x-240;
                check(!p.lost && (p.x-240)*direction>12 && p.vx*direction>20,
                        "upright gyro-z-assisted lean moves the actual body left and right for both feels");
                float releaseX=p.x;steps=uprightPhase(m,p,steps,36,direction*angle,0);
                uprightPhase(m,p,steps,84,0,0);
                check(!p.lost && Math.abs(p.vx)<12 && Math.abs(p.x-releaseX)<20,
                        "returning an upright phone to neutral brakes actual travel within half a second");
            }
            check(Math.abs(travel[0]+travel[1])<1,"upright mirrored lean produces matching signed travel in each feel");
        }
    }
    public static void main(String[] args) {
        neutralAndNoise();mirrorBrakingAndReverse();momentumAndTouchIsolation();realRopeContacts();uprightPortraitGameplay();
        System.out.println("PASS: real motion-to-gameplay neutral/noise, mirrored travel, half-second stop/reversal, momentum braking, touch-current isolation, intact off-center rope catches and upright Balanced/Tight gyro-z play ("+checks+" checks)");
    }
}
