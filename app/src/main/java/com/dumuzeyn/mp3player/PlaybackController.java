package com.dumuzeyn.mp3player;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Random;
import org.json.JSONArray;

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
        if (host.currentIndex < 0) {
            restoreCurrentFromPlaybackState();
        }
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
            startServiceAction(PlayerService.ACTION_TOGGLE, queueIndexOf(host.tracks.get(host.currentIndex)), false);
            if (!shouldPlay) {
                host.resumePosition = Math.max(host.resumePosition, PlayerService.lastPosition);
            }
        }
        startPlaybackWatcher();
        host.updateMini();
        host.refreshAfterTrackChange();
    }

    private boolean restoreCurrentFromPlaybackState() {
        PlayerService.refreshSnapshot();
        Track current = host.findTrack(PlayerService.lastUri);
        int position = Math.max(0, PlayerService.lastPosition);
        int loopMode = PlayerService.lastLoopMode;
        boolean wasPlaying = PlayerService.lastPlaying;
        if (current == null) {
            SharedPreferences prefs = host.getSharedPreferences(PlayerService.RESUME_PREFS, 0);
            current = restoreFromSavedResume(prefs);
            position = Math.max(0, prefs.getInt(PlayerService.RESUME_POSITION, 0));
            loopMode = prefs.getInt(PlayerService.RESUME_LOOP_MODE, host.loopMode);
            wasPlaying = prefs.getBoolean(PlayerService.RESUME_PLAYING, false);
        } else if (host.playbackQueue.isEmpty()) {
            restoreQueueFromPrefs(host.getSharedPreferences(PlayerService.RESUME_PREFS, 0), current);
        }
        if (current == null) {
            return false;
        }
        host.currentIndex = host.tracks.indexOf(current);
        host.resumePosition = position;
        host.loopMode = loopMode;
        host.playing = wasPlaying;
        if (host.playbackQueue.isEmpty()) {
            host.playbackQueue.add(current);
        }
        return host.currentIndex >= 0;
    }

    private Track restoreFromSavedResume(SharedPreferences prefs) {
        long savedAt = prefs.getLong(PlayerService.RESUME_SAVED_AT, 0L);
        if (host.resumeWindowMinutes > 0 && savedAt > 0 && System.currentTimeMillis() - savedAt > ((long) host.resumeWindowMinutes) * 60000L) {
            return null;
        }
        Track current = host.findTrack(prefs.getString(PlayerService.RESUME_URI, ""));
        restoreQueueFromPrefs(prefs, current);
        return current;
    }

    private void restoreQueueFromPrefs(SharedPreferences prefs, Track fallback) {
        host.playbackQueue.clear();
        try {
            JSONArray queue = new JSONArray(prefs.getString(PlayerService.RESUME_QUEUE, "[]"));
            for (int i = 0; i < queue.length(); i++) {
                Track queueTrack = host.findTrack(queue.optString(i, ""));
                if (queueTrack != null) {
                    host.playbackQueue.add(queueTrack);
                }
            }
        } catch (Exception ignored) {
        }
        if (host.playbackQueue.isEmpty() && fallback != null) {
            host.playbackQueue.add(fallback);
        }
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

    void cycleLoopMode() {
        host.loopMode = (host.loopMode + 1) % 3;
        Intent intent = new Intent(host, (Class<?>) PlayerService.class);
        intent.setAction(PlayerService.ACTION_LOOP);
        intent.putExtra(PlayerService.EXTRA_LOOP_MODE, host.loopMode);
        startService(intent);
    }

    void clearPlaybackMemory() {
        host.playbackQueue.clear();
        host.currentIndex = -1;
        host.playing = false;
        host.resumePosition = 0;
        host.playbackHandler.removeCallbacksAndMessages(null);
        PlayerService.lastIndex = -1;
        PlayerService.lastPlaying = false;
        PlayerService.lastDuration = 0;
        PlayerService.lastPosition = 0;
        PlayerService.lastUri = "";
        host.getSharedPreferences(PlayerService.RESUME_PREFS, 0).edit().clear().apply();
        Intent intent = new Intent(host, (Class<?>) PlayerService.class);
        intent.setAction(PlayerService.ACTION_STOP);
        startService(intent);
    }

    void addToQueue(Track track) {
        if (track == null) {
            return;
        }
        if (host.playbackQueue.isEmpty() && host.currentIndex >= 0 && host.currentIndex < host.tracks.size()) {
            host.playbackQueue.add(host.tracks.get(host.currentIndex));
        }
        if (!host.playbackQueue.contains(track)) {
            host.playbackQueue.add(track);
        }
        if (host.currentIndex >= 0) {
            Intent intent = new Intent(host, PlayerService.class);
            intent.setAction(PlayerService.ACTION_UPDATE_QUEUE);
            intent.putStringArrayListExtra(PlayerService.EXTRA_QUEUE_URIS, queueUris());
            startService(intent);
        }
    }

    String loopLabel() {
        if (host.loopMode == 1) {
            return host.tr("Track ↻", "Песня ↻");
        }
        if (host.loopMode == 2) {
            return host.tr("Queue ↻", "Очередь ↻");
        }
        return host.tr("Off ↻", "Выкл ↻");
    }

    void seekTo(int position) {
        Intent intent = new Intent(host, (Class<?>) PlayerService.class);
        intent.setAction(PlayerService.ACTION_SEEK);
        intent.putExtra(PlayerService.EXTRA_POSITION, Math.max(0, position));
        startService(intent);
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
        startService(intent);
    }

    private void startService(Intent intent) {
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
