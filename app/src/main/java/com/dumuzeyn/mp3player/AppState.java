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
    private PlaybackSnapshot playbackSnapshot = PlaybackSnapshot.empty();

    int tabIndex = 0;
    int customTimerMinutes = 10;
    int resumeWindowMinutes = 120;
    int particleFrequency = 45;
    int particleSize = 100;
    int particleLifetime = 100;
    int particlePrimaryColor = 0;
    int particleSecondaryColor = 0;
    int fullPlayerRotationSpeed = 100;
    int playlistTickerSpeed = 100;
    int cardOpacity = 82;
    int songCardOpacity = 82;
    int favoriteCardOpacity = 82;
    int playlistCardOpacity = 82;
    int genreCardOpacity = 82;
    int artistCardOpacity = 82;
    int albumCardOpacity = 82;
    int settingsCardOpacity = 82;
    int miniPlayerCardOpacity = 82;
    int headerCardOpacity = 82;
    int dialogCardOpacity = 82;
    long sleepTimerEndsAt = 0L;
    boolean dark = false;
    boolean animations = true;
    boolean particlesEnabled = true;
    int playerBackgroundMode = BackgroundSettingsController.MODE_GRADIENT;
    int mainBackgroundMode = BackgroundSettingsController.MODE_SOLID;
    int mainSolidBackground = 0;
    int playerSolidBackground = 0;
    String mainBackgroundMediaUri = "";
    String playerBackgroundMediaUri = "";
    int mainBackgroundBlur = 20;
    int playerBackgroundBlur = 20;
    boolean circularCovers = false;
    int mainGradientStart = 0xff351b5d;
    int mainGradientEnd = 0xff3a3013;
    int playerGradientStart = 0xff351b5d;
    int playerGradientEnd = 0xff3a3013;
    String language = "ru";
    String themeMode = "light";
    int customBg = -1;
    int customFg = -16777216;
    int customSecondaryAccent = 0xffffd000;
    int customTextColor = 0;
    boolean textOutlineEnabled = false;
    int textOutlineColor = 0;
    int preferredTabDirection = 0;
    boolean tabAnimating = false;
    String search = "";
    boolean fullPlayerOpening = false;
    int songRenderGeneration = 0;
    boolean renderingTabPreview = false;

    final PlaybackSnapshot playbackSnapshot() {
        return playbackSnapshot;
    }

    final void updatePlaybackSnapshot(PlaybackSnapshot snapshot) {
        playbackSnapshot = snapshot == null ? PlaybackSnapshot.empty() : snapshot;
    }

    final int currentTrackIndex() {
        if (playbackSnapshot.currentMediaId.isEmpty()) {
            return -1;
        }
        for (int index = 0; index < tracks.size(); index++) {
            if (MediaItemMapper.matchesMediaId(tracks.get(index),
                    playbackSnapshot.currentMediaId)) {
                return index;
            }
        }
        return -1;
    }

    final boolean isPlaybackPlaying() {
        return playbackSnapshot.playWhenReady
                && playbackSnapshot.phase != PlaybackPhase.ENDED
                && playbackSnapshot.phase != PlaybackPhase.ERROR;
    }

    final int repeatMode() {
        return RepeatModeMapper.fromMedia3(playbackSnapshot.repeatMode);
    }

    final boolean isShuffleEnabled() {
        return playbackSnapshot.shuffleEnabled;
    }
}
