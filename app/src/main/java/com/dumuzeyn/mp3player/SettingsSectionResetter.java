package com.dumuzeyn.mp3player;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.Arrays;
import java.util.List;

final class SettingsSectionResetter {
    enum Section { GENERAL, PLAYBACK, SOUND, APPEARANCE, FULL_PLAYER, ANIMATIONS }

    private SettingsSectionResetter() {
    }

    static void reset(MainActivityCore host, Section section) {
        SharedPreferences.Editor ui = host.getSharedPreferences("mp3_player_ui",
                Context.MODE_PRIVATE).edit();
        for (String key : keys(section)) {
            ui.remove(key);
        }
        ui.commit();
        if (section == Section.PLAYBACK) {
            host.getSharedPreferences(UninterruptedPlaybackController.PREFS, 0).edit()
                    .clear().commit();
        } else if (section == Section.SOUND) {
            host.getSharedPreferences(EqualizerController.PREFS, 0).edit().clear().commit();
        }
        host.reloadUiPreferences();
    }

    private static List<String> keys(Section section) {
        switch (section) {
            case GENERAL:
                return Arrays.asList("language", "resumeWindowMinutes", "customTimer");
            case APPEARANCE:
                return Arrays.asList("theme", "customBg", "customFg",
                        "customSecondaryAccent", "customTextColor", "textOutlineEnabled",
                        "textOutlineColor", "mainBackgroundMode", "mainSolidBackground",
                        "mainGradientStart", "mainGradientEnd", "mainBackgroundMediaUri",
                        "mainBackgroundBlur", "cardOpacity", "songCardOpacity",
                        "favoriteCardOpacity", "playlistCardOpacity", "genreCardOpacity",
                        "artistCardOpacity", "albumCardOpacity", "settingsCardOpacity",
                        "circularCovers");
            case FULL_PLAYER:
                return Arrays.asList("playerBackgroundMode", "playerSolidBackground",
                        "playerGradientStart", "playerGradientEnd", "playerBackgroundMediaUri",
                        "playerBackgroundBlur", "fullPlayerRotationSpeed");
            case ANIMATIONS:
                return Arrays.asList("animations", "particlesEnabled", "particleFrequency",
                        "particleSize", "particleLifetime", "particlePrimaryColor",
                        "particleSecondaryColor", "playlistTickerSpeed");
            case PLAYBACK:
                return Arrays.asList("resumeWindowMinutes");
            case SOUND:
            default:
                return java.util.Collections.emptyList();
        }
    }
}
