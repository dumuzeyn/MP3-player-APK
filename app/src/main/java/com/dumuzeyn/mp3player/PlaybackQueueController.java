package com.dumuzeyn.mp3player;

import com.dumuzeyn.mp3player.data.playback.PlaybackStateManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;

/** Owns library-facing queue decisions; ExoPlayer remains the active queue owner. */
final class PlaybackQueueController {
    private final MainActivityCore host;
    private final PlaybackController playback;

    PlaybackQueueController(MainActivityCore host, PlaybackController playback) {
        this.host = host;
        this.playback = playback;
    }

    void playTrack(Track track, boolean refreshList) {
        if (track == null || host.tracks.indexOf(track) < 0) {
            return;
        }
        ArrayList<Track> queue = new ArrayList<>();
        queue.add(track);
        playback.submitQueue(queue, 0, 0, host.repeatMode(), true);
        if (refreshList) {
            host.refreshAfterTrackChange();
        }
    }

    void playList(ArrayList<Track> source, boolean shuffle) {
        if (source == null || source.isEmpty()) {
            return;
        }
        ArrayList<Track> queue = new ArrayList<>(source);
        if (shuffle) {
            Collections.shuffle(queue, new Random());
        }
        playback.submitQueue(queue, 0, 0, host.repeatMode(), true);
    }

    void toggleOrStart() {
        if (playback.hasPlaybackSession()) {
            playback.toggle();
        } else if (!host.tracks.isEmpty()) {
            playList(host.tracks, false);
        }
    }

    void restore(PlaybackStateManager.State state) {
        Track current = host.findTrack(state.uri);
        ArrayList<Track> queue = PlaybackQueueResolver.restore(
                host.tracks, state.queueUris, current);
        if (!queue.isEmpty()) {
            playback.submitQueue(queue, Math.min(state.index, queue.size() - 1),
                    state.position, state.loopMode, state.playing);
        }
    }

    void clear() {
        new PlaybackStateManager(host).clear();
        playback.clearQueue();
    }

    void add(Track track) {
        if (track == null || containsUri(activeQueue(), track.uri)) {
            return;
        }
        playback.addQueueItem(track);
    }

    void remove(Track track) {
        if (track == null) {
            return;
        }
        int index = indexOfUri(activeQueue(), track.uri);
        if (index >= 0) {
            playback.removeQueueItem(index);
        }
    }

    void removeFromLibrary(Track track) {
        Track stored = track == null ? null : host.findTrack(track.uri);
        if (stored == null) {
            return;
        }
        remove(stored);
        host.tracks.remove(stored);
        host.favorites.remove(stored.uri);
        host.playlistController.removeTrackFromAllPlaylists(stored);
        TrackStore.save(host, host.tracks);
        host.saveState();
        host.render();
    }

    void playIndex(int index, int position) {
        ArrayList<Track> queue = new ArrayList<>(activeQueue());
        if (!queue.isEmpty()) {
            playback.submitQueue(queue, Math.max(0, Math.min(index, queue.size() - 1)),
                    position, host.repeatMode(), true);
        }
    }

    String loopLabel() {
        if (host.repeatMode() == 1) {
            return host.tr("Track repeat", "Повтор песни");
        }
        if (host.repeatMode() == 2) {
            return host.tr("Queue repeat", "Повтор очереди");
        }
        return host.tr("Repeat off", "Повтор выключен");
    }

    ArrayList<Track> activeQueue() {
        return host.playbackQueue.isEmpty() ? host.tracks : host.playbackQueue;
    }

    ArrayList<String> queueUris() {
        ArrayList<String> uris = new ArrayList<>();
        Iterator<Track> iterator = activeQueue().iterator();
        while (iterator.hasNext()) {
            uris.add(iterator.next().uri);
        }
        return uris;
    }

    boolean isPlayingSource(ArrayList<Track> source) {
        return host.isPlaybackPlaying() && source != null
                && sameOrderedQueue(source, host.playbackQueue);
    }

    boolean isPlayingCollection(ArrayList<Track> source) {
        return host.isPlaybackPlaying() && isCurrentCollection(source);
    }

    boolean isCurrentCollection(ArrayList<Track> source) {
        if (source == null || source.isEmpty() || host.playbackQueue.size() != source.size()) {
            return false;
        }
        HashSet<String> expected = new HashSet<>();
        HashSet<String> active = new HashSet<>();
        for (Track track : source) {
            expected.add(track.uri);
        }
        for (Track track : host.playbackQueue) {
            active.add(track.uri);
        }
        return expected.size() == source.size() && expected.equals(active);
    }

    int indexOf(Track track) {
        int index = indexOfUri(activeQueue(), track.uri);
        return index >= 0 ? index : Math.max(0, host.tracks.indexOf(track));
    }

    private static boolean sameOrderedQueue(ArrayList<Track> first, ArrayList<Track> second) {
        if (first.size() != second.size()) {
            return false;
        }
        for (int index = 0; index < first.size(); index++) {
            if (!first.get(index).uri.equals(second.get(index).uri)) {
                return false;
            }
        }
        return true;
    }

    private static boolean containsUri(ArrayList<Track> tracks, String uri) {
        return indexOfUri(tracks, uri) >= 0;
    }

    private static int indexOfUri(ArrayList<Track> tracks, String uri) {
        for (int index = 0; index < tracks.size(); index++) {
            if (tracks.get(index).uri.equals(uri)) {
                return index;
            }
        }
        return -1;
    }
}
