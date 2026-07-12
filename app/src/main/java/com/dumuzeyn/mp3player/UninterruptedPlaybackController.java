package com.dumuzeyn.mp3player;

import android.content.SharedPreferences;

final class UninterruptedPlaybackController {
    static final String PREFS = "playback_behavior";
    static final String ENABLED = "uninterrupted_playback";
    private final MainActivityCore host;

    UninterruptedPlaybackController(MainActivityCore host) {
        this.host = host;
    }

    String settingLabel() {
        return host.tr("Always play: ", "Всегда играть: ")
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
        return host.getSharedPreferences(PREFS, 0);
    }
}
