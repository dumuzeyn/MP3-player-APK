package com.dumuzeyn.mp3player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;

final class PlaybackQueueResolver {
    private PlaybackQueueResolver() {
    }

    static ArrayList<Track> restore(List<Track> library, String queueJson, Track fallback) {
        Map<String, Track> tracksByUri = new HashMap<>();
        for (Track track : library) {
            tracksByUri.put(track.uri, track);
        }

        ArrayList<Track> restored = new ArrayList<>();
        try {
            JSONArray savedQueue = new JSONArray(queueJson == null ? "[]" : queueJson);
            for (int index = 0; index < savedQueue.length(); index++) {
                Track track = tracksByUri.get(savedQueue.optString(index, ""));
                if (track != null) {
                    restored.add(track);
                }
            }
        } catch (Exception ignored) {
            // A damaged saved queue falls back to the current track.
        }
        if (restored.isEmpty() && fallback != null) {
            restored.add(fallback);
        }
        return restored;
    }
}
