package com.dumuzeyn.mp3player;

import android.content.SharedPreferences;

/** Loads and persists UI preferences without coupling them to Activity lifecycle code. */
final class UiPreferencesStore {
    private static final String PREFS = "mp3_player_ui";
    private static final String THEME = "theme";
    private static final String CUSTOM_BG = "customBg";
    private static final String CUSTOM_FG = "customFg";
    private static final String ANIMATIONS = "animations";
    private static final String LANGUAGE = "language";
    private static final String CUSTOM_TIMER = "customTimer";
    private static final String RESUME_WINDOW_MINUTES = "resumeWindowMinutes";
    private static final String PARTICLE_FREQUENCY = "particleFrequency";
    private static final String PARTICLE_SIZE = "particleSize";
    private static final String PARTICLE_LIFETIME = "particleLifetime";
    private static final String PLAYLIST_TICKER_SPEED = "playlistTickerSpeed";
    private static final String CARD_OPACITY = "cardOpacity";
    private static final String SONG_CARD_OPACITY = "songCardOpacity";
    private static final String FAVORITE_CARD_OPACITY = "favoriteCardOpacity";
    private static final String PLAYLIST_CARD_OPACITY = "playlistCardOpacity";
    private static final String GENRE_CARD_OPACITY = "genreCardOpacity";
    private static final String ARTIST_CARD_OPACITY = "artistCardOpacity";
    private static final String ALBUM_CARD_OPACITY = "albumCardOpacity";
    private static final String SETTINGS_CARD_OPACITY = "settingsCardOpacity";
    private static final String MINI_PLAYER_CARD_OPACITY = "miniPlayerCardOpacity";
    private static final String HEADER_CARD_OPACITY = "headerCardOpacity";
    private static final String DIALOG_CARD_OPACITY = "dialogCardOpacity";
    private static final String PARTICLES_ENABLED = "particlesEnabled";
    private static final String PLAYER_GRADIENT = "playerGradient";
    private static final String CIRCULAR_COVERS = "circularCovers";
    private static final String MAIN_GRADIENT = "mainGradient";
    private static final String MAIN_GRADIENT_START = "mainGradientStart";
    private static final String MAIN_GRADIENT_END = "mainGradientEnd";
    private static final String PLAYER_GRADIENT_START = "playerGradientStart";
    private static final String PLAYER_GRADIENT_END = "playerGradientEnd";

    private final MainActivityCore host;

    UiPreferencesStore(MainActivityCore host) {
        this.host = host;
    }

    void load() {
        SharedPreferences preferences = preferences();
        host.themeController.load(preferences);
        host.animations = preferences.getBoolean(ANIMATIONS, true);
        host.language = preferences.getString(LANGUAGE, "en");
        if (!"en".equals(host.language) && !"ru".equals(host.language)) {
            host.language = "en";
        }
        host.customTimerMinutes = preferences.getInt(CUSTOM_TIMER, 10);
        host.resumeWindowMinutes = Math.max(0, preferences.getInt(RESUME_WINDOW_MINUTES, 120));
        host.particleFrequency = clamp(preferences.getInt(PARTICLE_FREQUENCY, 45), 10, 100);
        host.particleSize = clamp(preferences.getInt(PARTICLE_SIZE, 100), 60, 150);
        host.particleLifetime = clamp(preferences.getInt(PARTICLE_LIFETIME, 100), 50, 180);
        host.playlistTickerSpeed = clamp(preferences.getInt(PLAYLIST_TICKER_SPEED, 100), 50, 200);
        host.cardOpacity = clamp(preferences.getInt(CARD_OPACITY, 82), 35, 100);
        host.songCardOpacity = clamp(
                preferences.getInt(SONG_CARD_OPACITY, host.cardOpacity), 35, 100);
        host.favoriteCardOpacity = clamp(
                preferences.getInt(FAVORITE_CARD_OPACITY, host.songCardOpacity), 35, 100);
        host.playlistCardOpacity = clamp(
                preferences.getInt(PLAYLIST_CARD_OPACITY, host.cardOpacity), 35, 100);
        host.genreCardOpacity = clamp(
                preferences.getInt(GENRE_CARD_OPACITY, host.cardOpacity), 35, 100);
        host.artistCardOpacity = clamp(
                preferences.getInt(ARTIST_CARD_OPACITY, host.cardOpacity), 35, 100);
        host.albumCardOpacity = clamp(
                preferences.getInt(ALBUM_CARD_OPACITY, host.cardOpacity), 35, 100);
        host.settingsCardOpacity = clamp(
                preferences.getInt(SETTINGS_CARD_OPACITY, host.cardOpacity), 35, 100);
        host.miniPlayerCardOpacity = clamp(
                preferences.getInt(MINI_PLAYER_CARD_OPACITY, host.cardOpacity), 35, 100);
        host.headerCardOpacity = clamp(
                preferences.getInt(HEADER_CARD_OPACITY, host.cardOpacity), 35, 100);
        host.dialogCardOpacity = clamp(
                preferences.getInt(DIALOG_CARD_OPACITY, host.cardOpacity), 35, 100);
        host.particlesEnabled = preferences.getBoolean(PARTICLES_ENABLED, true);
        host.gradientPlayerBackground = preferences.getBoolean(PLAYER_GRADIENT, true);
        host.circularCovers = preferences.getBoolean(CIRCULAR_COVERS, false);
        host.gradientMainBackground = preferences.getBoolean(MAIN_GRADIENT, false);
        host.mainGradientStart = preferences.getInt(MAIN_GRADIENT_START, 0xff351b5d);
        host.mainGradientEnd = preferences.getInt(MAIN_GRADIENT_END, 0xff3a3013);
        host.playerGradientStart = preferences.getInt(PLAYER_GRADIENT_START, 0xff351b5d);
        host.playerGradientEnd = preferences.getInt(PLAYER_GRADIENT_END, 0xff3a3013);
    }

    void save() {
        preferences().edit()
                .putString(THEME, host.themeMode)
                .putInt(CUSTOM_BG, host.customBg)
                .putInt(CUSTOM_FG, host.customFg)
                .putBoolean(ANIMATIONS, host.animations)
                .putString(LANGUAGE, host.language)
                .putInt(CUSTOM_TIMER, host.customTimerMinutes)
                .putInt(RESUME_WINDOW_MINUTES, host.resumeWindowMinutes)
                .putInt(PARTICLE_FREQUENCY, host.particleFrequency)
                .putInt(PARTICLE_SIZE, host.particleSize)
                .putInt(PARTICLE_LIFETIME, host.particleLifetime)
                .putInt(PLAYLIST_TICKER_SPEED, host.playlistTickerSpeed)
                .putInt(CARD_OPACITY, host.cardOpacity)
                .putInt(SONG_CARD_OPACITY, host.songCardOpacity)
                .putInt(FAVORITE_CARD_OPACITY, host.favoriteCardOpacity)
                .putInt(PLAYLIST_CARD_OPACITY, host.playlistCardOpacity)
                .putInt(GENRE_CARD_OPACITY, host.genreCardOpacity)
                .putInt(ARTIST_CARD_OPACITY, host.artistCardOpacity)
                .putInt(ALBUM_CARD_OPACITY, host.albumCardOpacity)
                .putInt(SETTINGS_CARD_OPACITY, host.settingsCardOpacity)
                .putInt(MINI_PLAYER_CARD_OPACITY, host.miniPlayerCardOpacity)
                .putInt(HEADER_CARD_OPACITY, host.headerCardOpacity)
                .putInt(DIALOG_CARD_OPACITY, host.dialogCardOpacity)
                .putBoolean(PARTICLES_ENABLED, host.particlesEnabled)
                .putBoolean(PLAYER_GRADIENT, host.gradientPlayerBackground)
                .putBoolean(CIRCULAR_COVERS, host.circularCovers)
                .putBoolean(MAIN_GRADIENT, host.gradientMainBackground)
                .putInt(MAIN_GRADIENT_START, host.mainGradientStart)
                .putInt(MAIN_GRADIENT_END, host.mainGradientEnd)
                .putInt(PLAYER_GRADIENT_START, host.playerGradientStart)
                .putInt(PLAYER_GRADIENT_END, host.playerGradientEnd)
                .apply();
    }

    private SharedPreferences preferences() {
        return host.getSharedPreferences(PREFS, 0);
    }

    private static int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}
