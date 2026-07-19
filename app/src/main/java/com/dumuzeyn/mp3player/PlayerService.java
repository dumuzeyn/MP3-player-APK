package com.dumuzeyn.mp3player;

import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.session.MediaSession;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import com.dumuzeyn.mp3player.data.playback.PlaybackStateManager;
import com.dumuzeyn.mp3player.playback.service.PlaybackCommandHandler;
import com.dumuzeyn.mp3player.playback.service.PlaybackEngine;
import com.dumuzeyn.mp3player.playback.service.PlaybackErrorRecovery;
import com.dumuzeyn.mp3player.playback.service.PlaybackSleepTimer;
import java.util.ArrayList;

public class PlayerService extends Service {
    public static final String ACTION_LOOP = "com.dumuzeyn.mp3player.LOOP";
    public static final String ACTION_NEXT = "com.dumuzeyn.mp3player.NEXT";
    public static final String ACTION_PLAY_INDEX = "com.dumuzeyn.mp3player.PLAY_INDEX";
    public static final String ACTION_PREV = "com.dumuzeyn.mp3player.PREV";
    public static final String ACTION_SEEK = "com.dumuzeyn.mp3player.SEEK";
    public static final String ACTION_STOP = "com.dumuzeyn.mp3player.STOP";
    public static final String ACTION_TOGGLE = "com.dumuzeyn.mp3player.TOGGLE";
    public static final String ACTION_AUDIO_EFFECTS = "com.dumuzeyn.mp3player.AUDIO_EFFECTS";
    public static final String ACTION_UPDATE_QUEUE = "com.dumuzeyn.mp3player.UPDATE_QUEUE";
    public static final String ACTION_TIMER_START = "com.dumuzeyn.mp3player.TIMER_START";
    public static final String ACTION_TIMER_CANCEL = "com.dumuzeyn.mp3player.TIMER_CANCEL";
    public static final String EXTRA_INDEX = "index";
    public static final String EXTRA_LOOP_MODE = "loopMode";
    public static final String EXTRA_ONE_SHOT = "oneShot";
    public static final String EXTRA_POSITION = "position";
    public static final String EXTRA_QUEUE_URIS = "queueUris";
    public static final String EXTRA_SHUFFLE = "shuffle";
    public static final String EXTRA_TIMER_MS = "timerMs";
    private static final String TAG = "VoltuneService";
    private static final String DEBUG_TAG = "VoltuneDebug";
    private static final int NOTIFICATION_ID = 7;
    private static PlayerService instance;

    public static int lastIndex = -1;
    public static boolean lastPlaying = false;
    public static int lastDuration = 0;
    public static int lastPosition = 0;
    public static int lastLoopMode = 0;
    public static String lastUri = "";

    private final PlaybackQueueManager queueManager = new PlaybackQueueManager();
    private final PlaybackCommandHandler commandHandler = new PlaybackCommandHandler();
    private MediaPlayer player;
    private AudioEffectsManager audioEffectsManager;
    private AudioFocusController audioFocusController;
    private MediaNotificationController notificationController;
    private boolean playerPreparing = false;
    private boolean playWhenPrepared = true;
    private boolean waitingForAudioFocus = false;
    private long lastResumePositionSavedAt = 0L;
    private PlaybackStateManager playbackStateManager;
    private int currentIndex = -1;
    private boolean oneShot = false;
    private boolean shuffle = false;
    private int loopMode = 0;
    private boolean pausedByUser = false;
    private boolean pausedForInterruption = false;
    private final PlaybackErrorRecovery errorRecovery = new PlaybackErrorRecovery();
    private final Handler timerHandler = new Handler(Looper.getMainLooper());
    private PlaybackSleepTimer sleepTimer;
    private final Runnable audioFocusRetry = new Runnable() {
        @Override
        public void run() {
            if (!waitingForAudioFocus || !playWhenPrepared || player == null) {
                return;
            }
            attemptStartReadyPlayer(player);
        }
    };
    private final Runnable playbackWatchdog = new Runnable() {
        @Override
        public void run() {
            if (player != null) {
                updateState();
            }
            if (loopMode != 0 && playWhenPrepared && !pausedByUser
                    && !pausedForInterruption && !queueManager.isEmpty()) {
                if (player == null) {
                    Log.w(TAG, "repeat_watchdog_restoring_player");
                    playIndex(currentIndex < 0 ? 0 : currentIndex);
                } else if (!playerPreparing && !waitingForAudioFocus && !safeIsPlaying()) {
                    int duration = safeDuration();
                    int position = safePosition();
                    if (duration > 0 && position >= Math.max(0, duration - 250)) {
                        Log.w(TAG, "repeat_watchdog_advancing_finished_track");
                        advanceQueue(PlaybackQueueNavigator.Reason.FINISHED, 0);
                    } else {
                        Log.w(TAG, "repeat_watchdog_restarting_playback");
                        play();
                    }
                }
            }
            timerHandler.postDelayed(this, 2500L);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        this.audioEffectsManager = new AudioEffectsManager(this);
        this.audioFocusController = new AudioFocusController(this, new AudioFocusCallback());
        this.playbackStateManager = new PlaybackStateManager(this);
        this.sleepTimer = new PlaybackSleepTimer(this, () -> {
            stopPlayback(true);
            stopSelf();
        });
        this.notificationController = new MediaNotificationController(
                this, new SessionCallback(), this::onNotificationCoverReady);
        this.queueManager.replace(TrackStore.load(this));
        this.sleepTimer.restore();
        this.timerHandler.postDelayed(this.playbackWatchdog, 2500L);
        Log.i(TAG, "service_created");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(NOTIFICATION_ID, currentNotification());
        handleCommand(this.commandHandler.read(intent), startId);
        return START_STICKY;
    }

    private void handleCommand(PlaybackCommandHandler.Command command, int startId) {
        switch (command.type) {
            case RESTORE:
                restorePlaybackAfterProcessDeath();
                break;
            case TIMER_START:
                this.sleepTimer.start(command.timerMs);
                break;
            case TIMER_CANCEL:
                this.sleepTimer.cancel();
                stopIfIdle();
                break;
            case PLAY_INDEX:
                startQueueCommand(command);
                break;
            case TOGGLE:
                if (this.player == null || this.currentIndex < 0 || this.queueManager.isEmpty()) {
                    prepareToggleQueue(command);
                }
                toggle();
                break;
            case NEXT:
                startManualTransition(PlaybackQueueNavigator.Reason.MANUAL_NEXT);
                break;
            case PREVIOUS:
                startManualTransition(PlaybackQueueNavigator.Reason.MANUAL_PREVIOUS);
                break;
            case SEEK:
                seekTo(command.position);
                break;
            case LOOP:
                updateLoopMode(command.loopMode == Integer.MIN_VALUE ? 0 : command.loopMode);
                break;
            case AUDIO_EFFECTS:
                applyAudioEffects(this.player);
                if (this.player == null) {
                    stopIfIdle();
                } else {
                    startForeground(NOTIFICATION_ID, currentNotification());
                }
                break;
            case UPDATE_QUEUE:
                updateQueue(command.queueUris);
                break;
            case STOP:
                Log.i(TAG, "explicit_stop");
                stopPlayback(true);
                stopSelf(startId);
                break;
            case UNKNOWN:
                Log.w(TAG, "unknown_command");
                break;
        }
    }

    private void startQueueCommand(PlaybackCommandHandler.Command command) {
        this.playWhenPrepared = true;
        this.waitingForAudioFocus = false;
        this.pausedForInterruption = false;
        this.oneShot = command.oneShot;
        this.shuffle = command.shuffle;
        if (command.loopMode != Integer.MIN_VALUE) {
            this.loopMode = command.loopMode;
        }
        lastLoopMode = this.loopMode;
        this.pausedByUser = false;
        this.audioFocusController.resetInterruptionState();
        saveResumeState(true, true);
        playIndex(command.index, command.queueUris, command.position, 0);
    }

    private void startManualTransition(PlaybackQueueNavigator.Reason reason) {
        this.playWhenPrepared = true;
        this.pausedForInterruption = false;
        this.oneShot = false;
        this.pausedByUser = false;
        advanceQueue(reason, 0);
    }

    private void updateLoopMode(int mode) {
        this.loopMode = mode;
        if (this.loopMode != 0) {
            this.oneShot = false;
        }
        lastLoopMode = this.loopMode;
        saveResumeState(true, true);
        logPlaybackState("loop_changed");
        startForeground(NOTIFICATION_ID, currentNotification());
    }

    private void updateQueue(ArrayList<String> queueUris) {
        String playingUri = currentUri();
        rebuildQueue(queueUris);
        if (this.queueManager.isEmpty()) {
            stopPlayback(true);
            stopSelf();
            return;
        }
        int updatedIndex = this.queueManager.indexOfUri(playingUri);
        if (updatedIndex >= 0) {
            this.currentIndex = updatedIndex;
            saveResumeState(true, true);
        } else if (this.player != null) {
            int replacementIndex = normalizeIndex(this.currentIndex);
            this.playWhenPrepared = true;
            playIndex(replacementIndex, null, 0, 0);
        }
    }

    private void stopIfIdle() {
        if (this.player == null) {
            stopForeground(true);
            stopSelf();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void playIndex(int index) {
        playIndex(index, null, 0, 0);
    }

    private void playIndex(int index, ArrayList<String> queueUris, int startPosition, int attempts) {
        this.playWhenPrepared = true;
        this.waitingForAudioFocus = false;
        this.timerHandler.removeCallbacks(this.audioFocusRetry);
        if (queueUris != null) {
            rebuildQueue(queueUris);
        } else if (this.queueManager.isEmpty()) {
            this.queueManager.replace(TrackStore.load(this));
        }
        if (this.queueManager.isEmpty()) {
            Log.w(TAG, "empty_queue");
            stopPlayback(false);
            stopSelf();
            return;
        }
        if (attempts >= this.queueManager.size()
                || this.errorRecovery.exhausted(this.queueManager.size())) {
            Log.e(DEBUG_TAG, "all_queue_items_failed errors="
                    + this.errorRecovery.consecutiveErrors()
                    + " queue=" + this.queueManager.size());
            stopPlayback(false);
            stopSelf();
            return;
        }
        this.currentIndex = normalizeIndex(index);
        Track track = this.queueManager.get(this.currentIndex);
        boolean canOpen = TrackStore.canOpenForRead(this, track.asUri());
        Log.i(DEBUG_TAG, "start_track index=" + this.currentIndex + " title=" + track.title + " uri=" + track.uri + " canOpen=" + canOpen + " attempt=" + attempts);
        if (!canOpen) {
            int errors = this.errorRecovery.recordError();
            Log.e(DEBUG_TAG, "start_track_unavailable uri=" + track.uri + " errors=" + errors);
            if (this.errorRecovery.exhausted(this.queueManager.size())) {
                stopPlayback(false);
                stopSelf();
            } else {
                advanceQueue(PlaybackQueueNavigator.Reason.ERROR, errors);
            }
            return;
        }
        releasePlayer();
        this.player = new MediaPlayer();
        try {
            String dataSourceMode = PlaybackEngine.configure(this, this.player, track);
            Log.i(DEBUG_TAG, "data_source_ready mode=" + dataSourceMode + " uri=" + track.uri);
            this.player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mediaPlayer) {
                    errorRecovery.resetConsecutiveErrors();
                    logPlaybackState("track_finished");
                    advanceQueue(PlaybackQueueNavigator.Reason.FINISHED, 0);
                }
            });
            this.player.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mediaPlayer, int what, int extra) {
                    playerPreparing = false;
                    int errors = errorRecovery.recordError();
                    Log.e(DEBUG_TAG, "media_error what=" + what + " extra=" + extra
                            + " index=" + currentIndex + " uri=" + currentUri()
                            + " errors=" + errors);
                    if (errorRecovery.exhausted(queueManager.size())) {
                        stopPlayback(false);
                        stopSelf();
                    } else {
                        advanceQueue(PlaybackQueueNavigator.Reason.ERROR, errors);
                    }
                    return true;
                }
            });
            this.player.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mediaPlayer) {
                    startPreparedPlayer(mediaPlayer, startPosition);
                }
            });
            this.playerPreparing = true;
            updateState();
            saveResumeState(true, true);
            this.audioFocusController.updateNoisyReceiver(safeIsPlaying());
            startForeground(NOTIFICATION_ID, currentNotification());
            this.player.prepareAsync();
            logPlaybackState("track_preparing");
        } catch (Exception e) {
            this.playerPreparing = false;
            int errors = this.errorRecovery.recordError();
            Log.e(DEBUG_TAG, "prepare_failed index=" + this.currentIndex + " uri=" + track.uri + " error=" + e.getMessage(), e);
            if (this.errorRecovery.exhausted(this.queueManager.size())) {
                stopPlayback(false);
                stopSelf();
            } else {
                advanceQueue(PlaybackQueueNavigator.Reason.ERROR, errors);
            }
        }
    }

    private void startPreparedPlayer(MediaPlayer preparedPlayer, int startPosition) {
        if (preparedPlayer != this.player) {
            return;
        }
        this.playerPreparing = false;
        try {
            this.errorRecovery.resetConsecutiveErrors();
            applyAudioEffects(preparedPlayer);
            if (startPosition > 0) {
                preparedPlayer.seekTo(Math.max(0, Math.min(startPosition, safeDuration())));
            }
            if (!this.playWhenPrepared) {
                updateState();
                saveResumeState(true, true);
                startForeground(NOTIFICATION_ID, currentNotification());
                return;
            }
            attemptStartReadyPlayer(preparedPlayer);
        } catch (Exception e) {
            int errors = this.errorRecovery.recordError();
            Log.e(DEBUG_TAG, "start_prepared_failed index=" + this.currentIndex + " uri=" + currentUri() + " error=" + e.getMessage(), e);
            advanceQueue(PlaybackQueueNavigator.Reason.ERROR, errors);
        }
    }

    private void attemptStartReadyPlayer(MediaPlayer preparedPlayer) {
        if (preparedPlayer != this.player || !this.playWhenPrepared) {
            return;
        }
        this.timerHandler.removeCallbacks(this.audioFocusRetry);
        if (!this.audioFocusController.requestFocus()) {
            this.waitingForAudioFocus = true;
            Log.w(TAG, "audio_focus_waiting");
            updateState();
            saveResumeState(true, true);
            startForeground(NOTIFICATION_ID, currentNotification());
            this.timerHandler.postDelayed(this.audioFocusRetry, 900L);
            return;
        }
        this.waitingForAudioFocus = false;
        try {
            preparedPlayer.start();
            this.audioFocusController.applyPlaybackVolume();
            this.errorRecovery.resetRepeatOneRetries();
            updateState();
            TrackStore.updateDuration(this, currentUri(), safeDuration());
            saveResumeState(true, true);
            this.audioFocusController.updateNoisyReceiver(true);
            startForeground(NOTIFICATION_ID, currentNotification());
            logPlaybackState("track_started");
        } catch (RuntimeException error) {
            int errors = this.errorRecovery.recordError();
            Log.e(DEBUG_TAG, "start_ready_failed index=" + this.currentIndex
                    + " uri=" + currentUri() + " error=" + error.getMessage(), error);
            advanceQueue(PlaybackQueueNavigator.Reason.ERROR, errors);
        }
    }

    private void rebuildQueue(ArrayList<String> queueUris) {
        this.queueManager.rebuild(TrackStore.load(this), queueUris);
        Log.i(TAG, "queue_loaded size=" + this.queueManager.size() + " loopMode=" + this.loopMode
                + " oneShot=" + this.oneShot + " shuffle=" + this.shuffle);
    }

    private void prepareToggleQueue(PlaybackCommandHandler.Command command) {
        if (command.hasShuffle) {
            this.shuffle = command.shuffle;
        }
        if (command.loopMode != Integer.MIN_VALUE) {
            this.loopMode = command.loopMode;
        }
        lastLoopMode = this.loopMode;
        ArrayList<String> queueUris = command.queueUris;
        if (queueUris != null && !queueUris.isEmpty()) {
            rebuildQueue(queueUris);
        } else if (this.queueManager.isEmpty()) {
            restoreQueueForToggle();
        }
        if (command.hasIndex) {
            this.currentIndex = normalizeIndex(command.index);
        } else if (this.currentIndex < 0) {
            this.currentIndex = normalizeIndex(this.playbackStateManager.load().index);
        }
    }

    private void restoreQueueForToggle() {
        PlaybackStateManager.State savedState = this.playbackStateManager.load();
        ArrayList<String> uris = new ArrayList<>(savedState.queueUris);
        if (uris.isEmpty()) {
            String uri = savedState.uri;
            if (!uri.isEmpty()) {
                uris.add(uri);
            }
        }
        if (!uris.isEmpty()) {
            rebuildQueue(uris);
        }
        if (this.queueManager.isEmpty()) {
            this.queueManager.replace(TrackStore.load(this));
        }
    }

    private void advanceQueue(PlaybackQueueNavigator.Reason reason, int attempts) {
        PlaybackQueueNavigator.Decision decision = PlaybackQueueNavigator.decide(
                this.queueManager.size(), this.currentIndex, this.loopMode, this.oneShot,
                reason, this.errorRecovery.repeatOneRetries());
        this.errorRecovery.setRepeatOneRetries(decision.loopOneErrorRetries);
        if (decision.stop) {
            stopPlayback(false);
            stopSelf();
            return;
        }
        if (this.currentIndex == this.queueManager.size() - 1 && decision.nextIndex == 0) {
            Log.i(TAG, "wrap_last_to_first");
        }
        Log.i(TAG, "advance reason=" + reason + " nextIndex=" + decision.nextIndex + " attempts=" + attempts);
        playIndex(decision.nextIndex, null, 0, attempts);
    }

    private int normalizeIndex(int index) {
        return this.queueManager.normalizeIndex(index);
    }

    private void restorePlaybackAfterProcessDeath() {
        if (this.player != null) {
            return;
        }
        PlaybackStateManager.State savedState = this.playbackStateManager.load();
        if (!savedState.playing) {
            return;
        }
        ArrayList<String> uris = new ArrayList<>(savedState.queueUris);
        if (uris.isEmpty()) {
            String uri = savedState.uri;
            if (!uri.isEmpty()) {
                uris.add(uri);
            }
        }
        this.loopMode = savedState.loopMode;
        this.shuffle = savedState.shuffle;
        this.oneShot = false;
        int index = savedState.index;
        int position = savedState.position;
        Log.i(TAG, "restore_after_process_death index=" + index + " queue=" + uris.size() + " loopMode=" + this.loopMode);
        playIndex(index, uris, position, 0);
    }

    private void toggle() {
        if (this.playerPreparing) {
            this.playWhenPrepared = !this.playWhenPrepared;
            this.pausedByUser = !this.playWhenPrepared;
            updateState();
            saveResumeState(true, true);
            startForeground(NOTIFICATION_ID, currentNotification());
            return;
        }
        if (this.waitingForAudioFocus) {
            this.playWhenPrepared = !this.playWhenPrepared;
            this.pausedByUser = !this.playWhenPrepared;
            if (this.playWhenPrepared) {
                attemptStartReadyPlayer(this.player);
            } else {
                this.waitingForAudioFocus = false;
                this.timerHandler.removeCallbacks(this.audioFocusRetry);
                updateState();
                saveResumeState(true, true);
                startForeground(NOTIFICATION_ID, currentNotification());
            }
            return;
        }
        if (this.player == null) {
            this.pausedByUser = false;
            playIndex(this.currentIndex < 0 ? 0 : this.currentIndex);
            return;
        }
        if (safeIsPlaying()) {
            this.pausedByUser = true;
            this.audioFocusController.onUserPause();
            pauseInternal("toggle_pause");
        } else {
            this.pausedByUser = false;
            play();
        }
    }

    private void play() {
        this.playWhenPrepared = true;
        this.pausedByUser = false;
        this.pausedForInterruption = false;
        if (this.player == null) {
            playIndex(this.currentIndex < 0 ? 0 : this.currentIndex);
            return;
        }
        if (this.playerPreparing) {
            return;
        }
        if (!safeIsPlaying()) {
            if (!this.audioFocusController.requestFocus()) {
                this.waitingForAudioFocus = true;
                this.timerHandler.removeCallbacks(this.audioFocusRetry);
                this.timerHandler.postDelayed(this.audioFocusRetry, 900L);
                updateState();
                saveResumeState(true, true);
                startForeground(NOTIFICATION_ID, currentNotification());
                return;
            }
            this.waitingForAudioFocus = false;
            try {
                this.player.start();
                this.audioFocusController.applyPlaybackVolume();
                updateState();
                saveResumeState(true, false);
                this.audioFocusController.updateNoisyReceiver(safeIsPlaying());
                startForeground(NOTIFICATION_ID, currentNotification());
                logPlaybackState("play");
            } catch (Exception e) {
                Log.e(TAG, "start_failed", e);
                advanceQueue(PlaybackQueueNavigator.Reason.ERROR, 1);
            }
        }
    }

    private void pauseInternal(String reason) {
        if (this.player != null && safeIsPlaying()) {
            try {
                this.player.pause();
            } catch (Exception e) {
                Log.e(TAG, "pause_failed reason=" + reason, e);
            }
            updateState();
            saveResumeState(true, false);
            this.audioFocusController.updateNoisyReceiver(safeIsPlaying());
            startForeground(NOTIFICATION_ID, currentNotification());
            logPlaybackState(reason);
        }
    }

    private void seekTo(int position) {
        if (this.player == null) {
            return;
        }
        try {
            this.player.seekTo(Math.max(0, Math.min(position, safeDuration())));
            updateState();
            saveResumeState(true, false);
            startForeground(NOTIFICATION_ID, currentNotification());
        } catch (Exception e) {
            Log.e(TAG, "seek_failed", e);
            advanceQueue(PlaybackQueueNavigator.Reason.ERROR, 1);
        }
    }

    private void stopPlayback(boolean explicit) {
        this.playWhenPrepared = false;
        this.waitingForAudioFocus = false;
        this.sleepTimer.cancel();
        releasePlayer();
        if (explicit) {
            this.currentIndex = -1;
            lastIndex = -1;
            lastUri = "";
            lastPosition = 0;
            lastDuration = 0;
            this.playbackStateManager.clear();
        } else {
            updateState();
            saveResumeState(true, true);
        }
        lastPlaying = false;
        this.audioFocusController.stop();
        stopForeground(true);
        logPlaybackState(explicit ? "stop_explicit" : "stop_queue_end");
    }

    static long getSleepTimerEndsAt(Context context) {
        if (instance != null) {
            return instance.sleepTimer.getEndsAt();
        }
        return PlaybackSleepTimer.readEndsAt(context);
    }

    private void releasePlayer() {
        this.timerHandler.removeCallbacks(this.audioFocusRetry);
        this.waitingForAudioFocus = false;
        this.audioEffectsManager.release();
        if (this.player == null) {
            return;
        }
        this.playerPreparing = false;
        PlaybackEngine.release(this.player);
        this.player = null;
    }

    private void applyAudioEffects(MediaPlayer targetPlayer) {
        this.audioEffectsManager.apply(targetPlayer);
    }

    private void updateState() {
        lastIndex = this.currentIndex;
        // Preparing the next queue item is still an active playback request. Persisting false
        // here makes a sticky service restart treat the queue as paused between two tracks.
        lastPlaying = this.player != null
                && ((this.playerPreparing && this.playWhenPrepared)
                || this.waitingForAudioFocus || safeIsPlaying());
        int currentDuration = safeDuration();
        if (currentDuration <= 0 && this.currentIndex >= 0
                && this.currentIndex < this.queueManager.size()) {
            currentDuration = Math.max(0, this.queueManager.get(this.currentIndex).durationMs);
        }
        lastDuration = currentDuration;
        lastPosition = safePosition();
        lastLoopMode = this.loopMode;
        lastUri = currentUri();
        if (lastPlaying && System.currentTimeMillis() - this.lastResumePositionSavedAt >= 7000L) {
            saveResumeState(false, false);
        }
    }

    private String currentUri() {
        return this.queueManager.uriAt(this.currentIndex);
    }

    private void saveResumeState(boolean forcePosition, boolean includeQueue) {
        if (this.currentIndex < 0 || this.currentIndex >= this.queueManager.size()) {
            return;
        }
        this.playbackStateManager.save(new PlaybackStateManager.Snapshot(
                this.queueManager.get(this.currentIndex).uri, lastPosition, lastDuration,
                this.currentIndex, this.loopMode, lastPlaying, this.shuffle,
                this.queueManager.tracks()), includeQueue);
        if (forcePosition || lastPlaying) {
            this.lastResumePositionSavedAt = System.currentTimeMillis();
        }
    }

    public static void refreshSnapshot() {
        if (instance != null) {
            instance.updateState();
        } else {
            lastPlaying = false;
        }
    }

    static boolean hasPlaybackSession() {
        return instance != null && instance.player != null && instance.currentIndex >= 0;
    }

    static void persistSnapshot() {
        if (instance != null && instance.player != null) {
            instance.updateState();
            instance.saveResumeState(true, true);
        }
    }

    public static void refreshAppearance() {
        if (instance != null && instance.player != null) {
            instance.startForeground(NOTIFICATION_ID, instance.currentNotification());
        }
    }

    private Notification currentNotification() {
        Track track = (this.currentIndex >= 0 && this.currentIndex < this.queueManager.size())
                ? this.queueManager.get(this.currentIndex)
                : new Track("", "MP3 Player Voltune", "Музыка готова");
        return this.notificationController.build(
                track, this.player != null, safeIsPlaying(), safePosition(), safeDuration());
    }

    private void onNotificationCoverReady(String uri) {
        if (uri == null || !uri.equals(currentUri()) || this.notificationController == null) {
            return;
        }
        startForeground(NOTIFICATION_ID, currentNotification());
    }

    private int safeDuration() {
        return PlaybackEngine.duration(
                this.player, this.playerPreparing, currentTrackDurationFallback());
    }

    private int safePosition() {
        return PlaybackEngine.position(this.player, this.playerPreparing, lastPosition);
    }

    private boolean safeIsPlaying() {
        return PlaybackEngine.isPlaying(this.player, this.playerPreparing);
    }

    private int currentTrackDurationFallback() {
        if (this.currentIndex >= 0 && this.currentIndex < this.queueManager.size()) {
            return Math.max(0, this.queueManager.get(this.currentIndex).durationMs);
        }
        return Math.max(0, lastDuration);
    }

    private void logPlaybackState(String event) {
        Log.i(TAG, event + " index=" + this.currentIndex + " queue=" + this.queueManager.size()
                + " loopMode=" + this.loopMode + " oneShot=" + this.oneShot + " shuffle="
                + this.shuffle + " player=" + (this.player != null) + " playing=" + safeIsPlaying());
    }

    @Override
    public void onDestroy() {
        logPlaybackState("on_destroy");
        if (this.player != null) {
            updateState();
            saveResumeState(true, true);
        }
        this.timerHandler.removeCallbacks(this.playbackWatchdog);
        releasePlayer();
        lastPlaying = false;
        this.sleepTimer.close();
        this.timerHandler.removeCallbacks(this.audioFocusRetry);
        this.audioFocusController.stop();
        this.notificationController.release();
        instance = null;
        super.onDestroy();
    }

    @Override
    public void onTaskRemoved(Intent intent) {
        logPlaybackState("on_task_removed");
        if (this.player != null) {
            updateState();
            saveResumeState(true, true);
        }
        if (this.player != null && safeIsPlaying()) {
            startForeground(NOTIFICATION_ID, currentNotification());
        }
        super.onTaskRemoved(intent);
    }

    private final class AudioFocusCallback implements AudioFocusController.Callback {
        @Override
        public boolean isPlaying() {
            return safeIsPlaying();
        }

        @Override
        public boolean isPausedByUser() {
            return pausedByUser;
        }

        @Override
        public void pauseForInterruption(String reason) {
            pausedForInterruption = true;
            pauseInternal(reason);
        }

        @Override
        public void pauseForDisconnectedOutput() {
            pausedByUser = true;
            pausedForInterruption = true;
            pauseInternal("audio_becoming_noisy");
        }

        @Override
        public void resumeAfterInterruption() {
            pausedForInterruption = false;
            play();
        }

        @Override
        public void setPlayerVolume(float volume) {
            if (player == null) {
                return;
            }
            try {
                player.setVolume(volume, volume);
            } catch (RuntimeException ignored) {
            }
        }
    }

    private class SessionCallback extends MediaSession.Callback {
        @Override
        public void onPlay() {
            pausedByUser = false;
            play();
        }

        @Override
        public void onPause() {
            pausedByUser = true;
            audioFocusController.onUserPause();
            pauseInternal("media_session_pause");
        }

        @Override
        public void onSkipToNext() {
            oneShot = false;
            advanceQueue(PlaybackQueueNavigator.Reason.MANUAL_NEXT, 0);
        }

        @Override
        public void onSkipToPrevious() {
            oneShot = false;
            advanceQueue(PlaybackQueueNavigator.Reason.MANUAL_PREVIOUS, 0);
        }

        @Override
        public void onSeekTo(long position) {
            seekTo((int) position);
        }

        @Override
        public void onStop() {
            stopPlayback(true);
            stopSelf();
        }
    }
}
