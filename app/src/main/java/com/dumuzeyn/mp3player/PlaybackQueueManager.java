package com.dumuzeyn.mp3player;

import java.util.ArrayList;
import java.util.List;

final class PlaybackQueueManager {
    private final ArrayList<Track> tracks = new ArrayList<>();

    void replace(List<Track> source) {
        tracks.clear();
        tracks.addAll(source);
    }

    void rebuild(List<Track> library, List<String> queueUris) {
        replace(PlaybackQueueResolver.restore(library, queueUris, null));
    }

    boolean isEmpty() {
        return tracks.isEmpty();
    }

    int size() {
        return tracks.size();
    }

    Track get(int index) {
        return tracks.get(index);
    }

    List<Track> tracks() {
        return tracks;
    }

    int normalizeIndex(int index) {
        if (tracks.isEmpty()) {
            return -1;
        }
        return Math.max(0, Math.min(index, tracks.size() - 1));
    }

    int indexOfUri(String uri) {
        if (uri == null || uri.isEmpty()) {
            return -1;
        }
        for (int index = 0; index < tracks.size(); index++) {
            if (uri.equals(tracks.get(index).uri)) {
                return index;
            }
        }
        return -1;
    }

    String uriAt(int index) {
        return index < 0 || index >= tracks.size() ? "" : tracks.get(index).uri;
    }
}
