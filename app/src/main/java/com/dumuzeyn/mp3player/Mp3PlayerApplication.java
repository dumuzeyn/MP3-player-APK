package com.dumuzeyn.mp3player;

import android.app.Application;

public final class Mp3PlayerApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        CrashReportStore.install(this);
    }
}
