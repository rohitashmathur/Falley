package com.perfgames.ropelab;

/** Independent, durable choices. Saving succeeds before an optional service is enabled. */
public final class TelemetryController {
    public static final int CRASH=1, ANALYTICS=2;
    public interface Store { int read(); boolean write(int flags); }
    public interface Backend { void apply(boolean crash,boolean analytics); }
    private final Store store; private final Backend backend; private int flags;
    public TelemetryController(Store store,Backend backend){this.store=store;this.backend=backend;reload();}
    public void reload(){flags=store.read()&3;backend.apply(crashes(),analytics());}
    public boolean crashes(){return (flags&CRASH)!=0;}
    public boolean analytics(){return (flags&ANALYTICS)!=0;}
    public boolean setCrash(boolean enabled){return change(CRASH,enabled);}
    public boolean setAnalytics(boolean enabled){return change(ANALYTICS,enabled);}
    private boolean change(int bit,boolean enabled){int next=enabled?flags|bit:flags&~bit;if(next==flags)return true;if(!store.write(next))return false;flags=next;backend.apply(crashes(),analytics());return true;}
}
