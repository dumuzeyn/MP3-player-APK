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
    int resumePosition = 0;
    long sleepTimerEndsAt = 0L;
    boolean dark = false;
    boolean animations = true;
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
}
