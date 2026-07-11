package com.dumuzeyn.mp3player;

import android.content.Intent;
import android.os.Build;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Random;

final class PlaybackController {
    private final MainActivityCore host;

    PlaybackController(MainActivityCore host) {
        this.host = host;
    }

    void playTrack(Track track) {
        playTrack(track, true);
    }

    void playTrack(Track track, boolean refreshList) {
        int index = host.tracks.indexOf(track);
        if (index < 0) {
            return;
        }
        host.playbackQueue.clear();
        host.playbackQueue.add(track);
        host.shuffleMode = false;
        host.currentIndex = index;
        host.playing = true;
        host.resumePosition = 0;
        startServiceAction(PlayerService.ACTION_PLAY_INDEX, 0, true);
        startPlaybackWatcher();
        host.updateMini();
        if (refreshList) {
            host.refreshAfterTrackChange();
        }
    }

    void playList(ArrayList<Track> source, boolean shuffle) {
        if (source.isEmpty()) {
            return;
        }
        ArrayList<Track> queue = new ArrayList<>(source);
        if (shuffle) {
            Collections.shuffle(queue, new Random());
        }
        int index = host.tracks.indexOf(queue.get(0));
        if (index < 0) {
            return;
        }
        host.playbackQueue.clear();
        host.playbackQueue.addAll(queue);
        host.shuffleMode = shuffle;
        host.currentIndex = index;
        host.playing = true;
        host.resumePosition = 0;
        startServiceAction(PlayerService.ACTION_PLAY_INDEX, 0, false);
        startPlaybackWatcher();
        host.refreshAfterTrackChange();
    }

    void toggleCurrent() {
        if (host.currentIndex < 0 && !host.tracks.isEmpty()) {
            playList(host.tracks, false);
            return;
        }
        if (host.currentIndex < 0) {
            return;
        }
        boolean shouldPlay = !host.playing;
        host.playing = shouldPlay;
        if (shouldPlay && host.resumePosition > 0) {
            startServiceAction(PlayerService.ACTION_PLAY_INDEX, queueIndexOf(host.tracks.get(host.currentIndex)), false, host.resumePosition);
        } else {
            startServiceAction(PlayerService.ACTION_TOGGLE, host.currentIndex, false);
            if (!shouldPlay) {
                host.resumePosition = Math.max(host.resumePosition, PlayerService.lastPosition);
            }
        }
        startPlaybackWatcher();
        host.updateMini();
        host.refreshAfterTrackChange();
    }

    void next() {
        ArrayList<Track> queue = activeQueue();
        if (queue.isEmpty()) {
            return;
        }
        int queueIndex = host.currentIndex < 0 ? 0 : (queueIndexOf(host.tracks.get(host.currentIndex)) + 1) % queue.size();
        host.currentIndex = host.tracks.indexOf(queue.get(queueIndex));
        host.playing = true;
        host.resumePosition = 0;
        startServiceAction(PlayerService.ACTION_PLAY_INDEX, queueIndex, false);
        startPlaybackWatcher();
        host.refreshAfterTrackChange();
    }

    void previous() {
        ArrayList<Track> queue = activeQueue();
        if (queue.isEmpty()) {
            return;
        }
        int queueIndex = host.currentIndex < 0 ? 0 : queueIndexOf(host.tracks.get(host.currentIndex));
        if (queueIndex <= 0) {
            queueIndex = queue.size();
        }
        int previousIndex = queueIndex - 1;
        host.currentIndex = host.tracks.indexOf(queue.get(previousIndex));
        host.playing = true;
        host.resumePosition = 0;
        startServiceAction(PlayerService.ACTION_PLAY_INDEX, previousIndex, false);
        startPlaybackWatcher();
        host.refreshAfterTrackChange();
    }

    void startPlaybackWatcher() {
        host.playbackHandler.removeCallbacksAndMessages(null);
        host.playbackHandler.postDelayed(new PlaybackWatcher(), 900L);
    }

    void startServiceAction(String action, int index) {
        startServiceAction(action, index, false);
    }

    void startServiceAction(String action, int index, boolean oneShot) {
        startServiceAction(action, index, oneShot, 0);
    }

    void startServiceAction(String action, int index, boolean oneShot, int position) {
        Intent intent = new Intent(host, (Class<?>) PlayerService.class);
        intent.setAction(action);
        intent.putExtra(PlayerService.EXTRA_INDEX, index);
        intent.putExtra(PlayerService.EXTRA_ONE_SHOT, oneShot);
        intent.putExtra(PlayerService.EXTRA_SHUFFLE, host.shuffleMode);
        intent.putExtra(PlayerService.EXTRA_LOOP_MODE, host.loopMode);
        intent.putExtra(PlayerService.EXTRA_POSITION, Math.max(0, position));
        intent.putStringArrayListExtra(PlayerService.EXTRA_QUEUE_URIS, queueUris());
        if (Build.VERSION.SDK_INT < 26) {
            host.startService(intent);
        } else {
            host.startForegroundService(intent);
        }
    }

    ArrayList<String> queueUris() {
        ArrayList<String> uris = new ArrayList<>();
        Iterator<Track> iterator = activeQueue().iterator();
        while (iterator.hasNext()) {
            uris.add(iterator.next().uri);
        }
        return uris;
    }

    ArrayList<Track> activeQueue() {
        return host.playbackQueue.isEmpty() ? host.tracks : host.playbackQueue;
    }

    boolean isPlayingSource(ArrayList<Track> source) {
        if (!host.playing || source == null || source.isEmpty() || host.playbackQueue.size() != source.size()) {
            return false;
        }
        for (int i = 0; i < source.size(); i++) {
            if (!host.playbackQueue.get(i).uri.equals(source.get(i).uri)) {
                return false;
            }
        }
        return true;
    }

    int queueIndexOf(Track track) {
        ArrayList<Track> queue = activeQueue();
        for (int i = 0; i < queue.size(); i++) {
            if (queue.get(i).uri.equals(track.uri)) {
                return i;
            }
        }
        return Math.max(0, host.tracks.indexOf(track));
    }

    private final class PlaybackWatcher implements Runnable {
        @Override
        public void run() {
            Track resolvedTrack;
            PlayerService.refreshSnapshot();
            if (PlayerService.lastIndex < 0) {
                if (host.resumeWindowMinutes <= 0) {
                    host.currentIndex = -1;
                }
                host.playing = false;
                host.resumePosition = Math.max(0, PlayerService.lastPosition);
                host.updateMini();
                host.render();
                return;
            }
            host.playing = PlayerService.lastPlaying;
            host.resumePosition = Math.max(0, PlayerService.lastPosition);
            if (PlayerService.lastUri != null && !PlayerService.lastUri.isEmpty() && (resolvedTrack = host.findTrack(PlayerService.lastUri)) != null && !host.isCurrent(resolvedTrack)) {
                host.currentIndex = host.tracks.indexOf(resolvedTrack);
                host.refreshAfterTrackChange();
            } else {
                host.updateMini();
            }
            if (host.playing || host.currentIndex >= 0) {
                host.playbackHandler.postDelayed(this, 900L);
            }
        }
    }
}
