package com.perfgames.ropelab;

/** Gesture-level regressions for touch-only steering and tap/swipe separation. */
public final class TouchSteeringTest {
    private static int checks;
    private static void check(boolean ok,String message) {
        checks++;if(!ok)throw new AssertionError(message);
    }
    public static void main(String[] args) {
        TouchSteering t=new TouchSteering();
        t.move(80,200);
        check(!t.active() && t.velocity()==0 && !t.release(80,200),"movement without a live touch cannot steer or jump");
        t.begin(240,400);
        check(t.active() && !t.dragging() && t.velocity()==0,"touch begins at neutral without automatic drift");
        t.move(244,403);
        check(!t.dragging() && t.velocity()==0,"small finger tremor does not steer");
        check(t.release(244,403) && !t.active() && t.velocity()==0,"a true tap jumps only on release and clears input");
        for(int direction:new int[]{-1,1}) {
            t.begin(240,400);t.move(240+direction*90,400);
            check(t.dragging() && t.velocity()*direction>100 && Math.abs(t.velocity())<=TouchSteering.MAX_VELOCITY,
                    "deliberate swipes give bounded velocity in either direction");
            check(!t.release(240+direction*90,400) && !t.active() && t.velocity()==0,
                    "releasing a swipe stops movement immediately and cannot accidentally jump");
        }
        t.begin(240,400);t.move(190,400);t.move(280,400);
        check(t.velocity()>0,"a gesture can change direction instead of sticking left");
        t.move(240,400);
        check(t.dragging() && t.velocity()==0 && !t.release(240,400),"returning a swipe to its start stops steering without turning it into a tap");
        t.begin(240,400);t.move(240,450);
        check(t.dragging() && t.velocity()==0 && !t.release(240,450),"a vertical swipe neither moves sideways nor causes an accidental jump");
        t.begin(240,400);t.move(170,400);t.cancel();
        check(!t.active() && !t.dragging() && t.velocity()==0 && !t.release(170,400),"cancel ends steering and cannot create a tap");
        t.begin(Float.NaN,400);
        check(!t.active() && t.velocity()==0,"an invalid new pointer cannot create a gesture");
        t.begin(240,400);t.move(310,400);t.release(Float.POSITIVE_INFINITY,400);
        check(!t.active() && t.velocity()==0,"invalid release coordinates still clear the previous velocity");
        t.begin(240,400);t.move(1e30f,400);
        check(Float.isFinite(t.velocity()) && t.velocity()==TouchSteering.MAX_VELOCITY,"extreme coordinates cannot produce unbounded steering");
        t.cancel();t.begin(240,400);
        check(t.release(240,400),"a fresh tap works after an extreme/canceled gesture");
        System.out.println("PASS: touch startup/no drift, tap tremor, bilateral swipe direction, bounded velocity, release/cancel stop, reversal, return-to-start, vertical swipe and invalid coordinates ("+checks+" checks)");
    }
}
