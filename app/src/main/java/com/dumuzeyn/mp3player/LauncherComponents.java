package com.dumuzeyn.mp3player;

import android.content.ComponentName;
import android.content.Context;

final class LauncherComponents {
    private static final String PACKAGE_NAME = MainActivity.class.getPackage().getName();

    private LauncherComponents() {
    }

    static ComponentName forTheme(Context context, boolean dark) {
        return new ComponentName(context,
                PACKAGE_NAME + (dark ? ".LauncherDark" : ".LauncherLight"));
    }
}
