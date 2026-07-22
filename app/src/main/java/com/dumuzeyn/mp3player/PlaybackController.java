package com.dumuzeyn.mp3player;

import android.content.ComponentName;
import android.os.Bundle;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;
import com.dumuzeyn.mp3player.data.playback.PlaybackStateManager;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;

/** Bridges the activity UI to the single Media3 playback session. */
final class PlaybackController implements Player.Listener {
    private static final String TAG = "VoltuneMedia3";

    private final MainActivityCore host;
    private final MediaItemMapper mapper = new MediaItemMapper();
    private final ArrayDeque<Runnable> pendingCommands = new ArrayDeque<>();
    private ListenableFuture<MediaController> controllerFuture;
    private MediaController controller;
    private boolean released;

    PlaybackController(MainActivityCore host) {
        this.host = host;
    }

    void connect() {
        if (controllerFuture != null || released) {
            return;
        }
        SessionToken token = new SessionToken(host,
                new ComponentName(host, Media3PlayerService.class));
        controllerFuture = new MediaController.Builder(host, token).buildAsync();
        controllerFuture.addListener(() -> host.runOnUiThread(this::finishConnection),
                Runnable::run);
    }

    private void finishConnection() {
        if (released || controllerFuture == null) {
            return;
        }
        try {
            controller = controllerFuture.get();
            controller.addListener(this);
            synchronizeUi(false);
            while (!pendingCommands.isEmpty()) {
                pendingCommands.removeFirst().run();
            }
        } catch (Exception error) {
            Log.e(TAG, "media_controller_connection_failed", error);
            pendingCommands.clear();
        }
    }

    void release() {
        released = true;
        pendingCommands.clear();
        if (controller != null) {
            controller.removeListener(this);
            controller.release();
            controller = null;
        } else if (controllerFuture != null) {
            MediaController.releaseFuture(controllerFuture);
        }
        controllerFuture = null;
    }

    private void whenConnected(Runnable command) {
        if (released) {
            return;
        }
        if (controller != null) {
            command.run();
            return;
        }
        if (pendingCommands.size() >= 24) {
            pendingCommands.removeFirst();
        }
        pendingCommands.addLast(command);
        connect();
    }

    void playTrack(Track track) {
        playTrack(track, true);
    }

    void playTrack(Track track, boolean refreshList) {
        int libraryIndex = host.tracks.indexOf(track);
        if (libraryIndex < 0) {
            return;
        }
        host.playbackQueue.clear();
        host.playbackQueue.add(track);
        host.shuffleMode = false;
        host.currentIndex = libraryIndex;
        host.playing = true;
        host.resumePosition = 0;
        whenConnected(() -> startQueue(new ArrayList<>(host.playbackQueue), 0, 0));
        host.updateMini();
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
        int libraryIndex = host.tracks.indexOf(queue.get(0));
        if (libraryIndex < 0) {
            return;
        }
        host.playbackQueue.clear();
        host.playbackQueue.addAll(queue);
        host.shuffleMode = shuffle;
        host.currentIndex = libraryIndex;
        host.playing = true;
        host.resumePosition = 0;
        ArrayList<Track> submitted = new ArrayList<>(queue);
        whenConnected(() -> startQueue(submitted, 0, 0));
        host.refreshAfterTrackChange();
    }

    private void startQueue(ArrayList<Track> queue, int index, int positionMs) {
        if (controller == null || queue.isEmpty()) {
            return;
        }
        ArrayList<MediaItem> items = new ArrayList<>();
        for (Track item : queue) {
            items.add(mapper.toMediaItem(item));
        }
        controller.setMediaItems(items, Math.max(0, Math.min(index, items.size() - 1)),
                Math.max(0, positionMs));
        controller.setShuffleModeEnabled(false);
        controller.setRepeatMode(Media3PlayerService.toMedia3RepeatMode(host.loopMode));
        controller.prepare();
        controller.play();
    }

    void toggleCurrent() {
        whenConnected(() -> {
            if (controller == null) {
                return;
            }
            if (controller.getMediaItemCount() == 0) {
                PlaybackStateManager.State saved = new PlaybackStateManager(host).load();
                restoreSavedState(saved);
                if (controller.getMediaItemCount() == 0 && !host.tracks.isEmpty()) {
                    playList(host.tracks, false);
                }
                return;
            }
            if (controller.isPlaying() || controller.getPlayWhenReady()) {
                controller.pause();
            } else {
                if (controller.getPlaybackState() == Player.STATE_IDLE) {
                    controller.prepare();
                }
                controller.play();
            }
        });
    }

    private void restoreSavedState(PlaybackStateManager.State state) {
        Track current = host.findTrack(state.uri);
        ArrayList<Track> queue = PlaybackQueueResolver.restore(
                host.tracks, state.queueUris, current);
        if (queue.isEmpty()) {
            return;
        }
        host.playbackQueue.clear();
        host.playbackQueue.addAll(queue);
        host.loopMode = state.loopMode;
        host.shuffleMode = state.shuffle;
        startQueue(queue, Math.min(state.index, queue.size() - 1), state.position);
        if (!state.playing && controller != null) {
            controller.pause();
        }
    }

    void next() {
        whenConnected(() -> {
            if (controller != null && controller.hasNextMediaItem()) {
                controller.seekToNextMediaItem();
                controller.play();
            }
        });
    }

    void previous() {
        whenConnected(() -> {
            if (controller != null) {
                controller.seekToPreviousMediaItem();
                controller.play();
            }
        });
    }

    void cycleLoopMode() {
        host.loopMode = (host.loopMode + 1) % 3;
        whenConnected(() -> controller.setRepeatMode(
                Media3PlayerService.toMedia3RepeatMode(host.loopMode)));
    }

    void clearPlaybackMemory() {
        host.playbackQueue.clear();
        host.currentIndex = -1;
        host.playing = false;
        host.resumePosition = 0;
        new PlaybackStateManager(host).clear();
        whenConnected(() -> {
            controller.stop();
            controller.clearMediaItems();
            controller.sendCustomCommand(Media3Commands.CLEAR_QUEUE_COMMAND, Bundle.EMPTY);
        });
    }

    void addToQueue(Track track) {
        if (track == null) {
            return;
        }
        if (host.playbackQueue.isEmpty() && currentTrack() != null) {
            host.playbackQueue.add(currentTrack());
        }
        if (!containsUri(host.playbackQueue, track.uri)) {
            host.playbackQueue.add(track);
            whenConnected(() -> controller.addMediaItem(mapper.toMediaItem(track)));
        }
    }

    void removeFromQueue(Track track) {
        if (track == null) {
            return;
        }
        int index = indexOfUri(activeQueue(), track.uri);
        if (index < 0) {
            return;
        }
        host.playbackQueue.remove(index);
        whenConnected(() -> {
            if (index < controller.getMediaItemCount()) {
                controller.removeMediaItem(index);
            }
        });
        if (host.playbackQueue.isEmpty()) {
            clearPlaybackMemory();
        } else {
            host.refreshAfterTrackChange();
        }
    }

    void removeFromLibrary(Track track) {
        if (track == null) {
            return;
        }
        Track stored = host.findTrack(track.uri);
        if (stored == null) {
            return;
        }
        removeFromQueue(stored);
        host.tracks.remove(stored);
        host.favorites.remove(stored.uri);
        host.playlistController.removeTrackFromAllPlaylists(stored);
        TrackStore.save(host, host.tracks);
        host.saveState();
        host.render();
    }

    String loopLabel() {
        if (host.loopMode == 1) {
            return host.tr("Track repeat", "Повтор песни");
        }
        if (host.loopMode == 2) {
            return host.tr("Queue repeat", "Повтор очереди");
        }
        return host.tr("Repeat off", "Повтор выключен");
    }

    void seekTo(int position) {
        whenConnected(() -> controller.seekTo(Math.max(0, position)));
    }

    void startSleepTimer(long delayMs) {
        Bundle args = new Bundle();
        args.putLong(Media3Commands.ARG_TIMER_MS, Math.max(1000L, delayMs));
        whenConnected(() -> controller.sendCustomCommand(
                Media3Commands.TIMER_START_COMMAND, args));
    }

    void cancelSleepTimer() {
        whenConnected(() -> controller.sendCustomCommand(
                Media3Commands.TIMER_CANCEL_COMMAND, Bundle.EMPTY));
    }

    void refreshAudioEffects() {
        whenConnected(() -> controller.sendCustomCommand(
                Media3Commands.AUDIO_EFFECTS_COMMAND, Bundle.EMPTY));
    }

    void playQueueIndex(int index, int position) {
        ArrayList<Track> queue = new ArrayList<>(activeQueue());
        if (queue.isEmpty()) {
            return;
        }
        int safeIndex = Math.max(0, Math.min(index, queue.size() - 1));
        host.currentIndex = host.tracks.indexOf(queue.get(safeIndex));
        host.playing = true;
        whenConnected(() -> startQueue(queue, safeIndex, position));
    }

    long currentPosition() {
        return controller == null ? Math.max(0, host.resumePosition)
                : Math.max(0L, controller.getCurrentPosition());
    }

    long duration() {
        if (controller == null || controller.getDuration() == C.TIME_UNSET) {
            Track current = currentTrack();
            return current == null ? 0L : current.durationMs;
        }
        return Math.max(0L, controller.getDuration());
    }

    boolean hasPlaybackSession() {
        if (controller != null) {
            return controller.getMediaItemCount() > 0;
        }
        return !new PlaybackStateManager(host).load().queueUris.isEmpty();
    }

    @Nullable
    private Track currentTrack() {
        if (controller != null) {
            MediaItem item = controller.getCurrentMediaItem();
            if (item != null && item.localConfiguration != null) {
                Track resolved = host.findTrack(item.localConfiguration.uri.toString());
                if (resolved != null) {
                    return resolved;
                }
            }
        }
        return host.currentIndex >= 0 && host.currentIndex < host.tracks.size()
                ? host.tracks.get(host.currentIndex) : null;
    }

    private void synchronizeUi(boolean refreshRows) {
        if (controller == null) {
            return;
        }
        Track previous = host.currentIndex >= 0 && host.currentIndex < host.tracks.size()
                ? host.tracks.get(host.currentIndex) : null;
        Track current = currentTrack();
        host.playing = controller.isPlaying();
        host.resumePosition = (int) Math.min(Integer.MAX_VALUE, currentPosition());
        host.loopMode = Media3PlayerService.fromMedia3RepeatMode(controller.getRepeatMode());
        if (current != null) {
            host.currentIndex = host.tracks.indexOf(current);
        } else if (controller.getMediaItemCount() == 0) {
            host.currentIndex = -1;
        }
        boolean trackChanged = previous == null ? current != null
                : current == null || !previous.uri.equals(current.uri);
        host.updateMini();
        if (trackChanged || refreshRows) {
            host.refreshAfterTrackChange();
        }
        host.playerUiController.syncPlaybackUi();
    }

    @Override
    public void onEvents(Player player, Player.Events events) {
        boolean refreshRows = events.contains(Player.EVENT_MEDIA_ITEM_TRANSITION)
                || events.contains(Player.EVENT_IS_PLAYING_CHANGED)
                || events.contains(Player.EVENT_PLAYBACK_STATE_CHANGED)
                || events.contains(Player.EVENT_REPEAT_MODE_CHANGED);
        synchronizeUi(refreshRows);
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
        return host.playing && source != null && sameOrderedQueue(source, host.playbackQueue);
    }

    boolean isPlayingCollection(ArrayList<Track> source) {
        return host.playing && isCurrentCollection(source);
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

    int queueIndexOf(Track track) {
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
