package com.dumuzeyn.mp3player;

import android.content.SharedPreferences;

final class StableVolumeController {
    static final String ENABLED = "stable_volume";
    private final MainActivityCore host;

    StableVolumeController(MainActivityCore host) {
        this.host = host;
    }

    String settingLabel() {
        return host.tr("No volume ducking: ", "Без приглушения громкости: ")
                + host.tr(enabled() ? "on" : "off", enabled() ? "вкл" : "выкл");
    }

    void toggle() {
        prefs().edit().putBoolean(ENABLED, !enabled()).apply();
        host.render();
    }

    private boolean enabled() {
        return prefs().getBoolean(ENABLED, false);
    }

    private SharedPreferences prefs() {
        return host.getSharedPreferences(UninterruptedPlaybackController.PREFS, 0);
    }
}
