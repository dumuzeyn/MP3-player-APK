package com.dumuzeyn.mp3player;

import android.view.View;
import android.widget.Button;
import java.util.HashMap;
import java.util.Map;

final class SongRowStateRegistry {
    interface StateResolver {
        Track findTrack(String uri);
        boolean isCurrent(Track track);
        boolean isPlaying();
        int activeColor();
        int inactiveColor();
    }

    private final HashMap<String, Button> playButtons = new HashMap<>();
    private final HashMap<String, View> currentMarkers = new HashMap<>();
    private final HashMap<String, WaveformView> waveforms = new HashMap<>();

    void clear() {
        playButtons.clear();
        currentMarkers.clear();
        waveforms.clear();
    }

    void registerPlayButton(String uri, Button button) {
        playButtons.put(uri, button);
    }

    void registerCurrentMarker(String uri, View marker) {
        currentMarkers.put(uri, marker);
    }

    void registerWaveform(String uri, WaveformView waveform) {
        waveforms.put(uri, waveform);
    }

    void refresh(StateResolver resolver) {
        for (Map.Entry<String, Button> entry : playButtons.entrySet()) {
            Track track = resolver.findTrack(entry.getKey());
            boolean current = track != null && resolver.isCurrent(track);
            entry.getValue().setText((current && resolver.isPlaying()) ? "Ⅱ" : "▶");
        }
        for (Map.Entry<String, View> entry : currentMarkers.entrySet()) {
            Track track = resolver.findTrack(entry.getKey());
            entry.getValue().setVisibility(track != null && resolver.isCurrent(track) ? View.VISIBLE : View.INVISIBLE);
        }
        for (Map.Entry<String, WaveformView> entry : waveforms.entrySet()) {
            Track track = resolver.findTrack(entry.getKey());
            boolean current = track != null && resolver.isCurrent(track);
            entry.getValue().setState(current ? resolver.activeColor() : resolver.inactiveColor(), current && resolver.isPlaying());
        }
    }
}
