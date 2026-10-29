package com.perfgames.ropelab;

import android.app.Application;

/** Firebase's automatic provider is removed; explicit player choices own initialization. */
public final class FalleyApplication extends Application {
    private AppTelemetry telemetry;
    @Override public void onCreate(){super.onCreate();telemetry=new AppTelemetry(this);}
    AppTelemetry telemetry(){return telemetry;}
}
