package com.dumuzeyn.mp3player;

import android.app.Activity;
import java.util.ArrayList;
import java.util.HashSet;

/** Mutable application state shared by the activity and its controllers. */
abstract class AppState extends Activity {
    final ArrayList<Track> tracks = new ArrayList<>();
    final HashSet<String> favorites = new HashSet<>();
    final ArrayList<Playlist> playlists = new ArrayList<>();
    final ArrayList<Track> playbackQueue = new ArrayList<>();

    int tabIndex = 0;
    int currentIndex = -1;
    boolean playing = false;
    int loopMode = 0;
    int customTimerMinutes = 10;
    int resumeWindowMinutes = 120;
    int particleFrequency = 45;
    int particleSize = 100;
    int particleLifetime = 100;
    int playlistTickerSpeed = 100;
    int cardOpacity = 82;
    int songCardOpacity = 82;
    int playlistCardOpacity = 82;
    int genreCardOpacity = 82;
    int artistCardOpacity = 82;
    int albumCardOpacity = 82;
    int settingsCardOpacity = 82;
    int resumePosition = 0;
    long sleepTimerEndsAt = 0L;
    boolean dark = false;
    boolean animations = true;
    boolean particlesEnabled = true;
    boolean gradientPlayerBackground = true;
    boolean gradientMainBackground = false;
    boolean circularCovers = false;
    int mainGradientStart = 0xff351b5d;
    int mainGradientEnd = 0xff3a3013;
    int playerGradientStart = 0xff351b5d;
    int playerGradientEnd = 0xff3a3013;
    boolean shuffleMode = false;
    String language = "en";
    String themeMode = "light";
    int customBg = -1;
    int customFg = -16777216;
    int preferredTabDirection = 0;
    boolean tabAnimating = false;
    String search = "";
    boolean fullPlayerOpening = false;
    int songRenderGeneration = 0;
    boolean renderingTabPreview = false;
}
