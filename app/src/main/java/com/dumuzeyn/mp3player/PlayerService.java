package com.dumuzeyn.mp3player;

import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.session.MediaSession;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.util.Log;
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
    private static final String TAG = "MP3PlayerService";
    private static final String DEBUG_TAG = "MP3PlayerDebug";
    private static final String TIMER_PREFS = "player_sleep_timer";
    private static final String TIMER_ENDS_AT = "endsAt";
    private static final int NOTIFICATION_ID = 7;
    private static PlayerService instance;

    public static int lastIndex = -1;
    public static boolean lastPlaying = false;
    public static int lastDuration = 0;
    public static int lastPosition = 0;
    public static int lastLoopMode = 0;
    public static String lastUri = "";

    private final PlaybackQueueManager queueManager = new PlaybackQueueManager();
    private MediaPlayer player;
    private AudioEffectsManager audioEffectsManager;
    private AudioFocusController audioFocusController;
    private MediaNotificationController notificationController;
    private boolean playerPreparing = false;
    private long lastResumePositionSavedAt = 0L;
    private PlaybackStateRepository playbackStateRepository;
    private int currentIndex = -1;
    private boolean oneShot = false;
    private boolean shuffle = false;
    private int loopMode = 0;
    private boolean pausedByUser = false;
    private int loopOneErrorRetries = 0;
    private int consecutivePlaybackErrors = 0;
    private final Handler timerHandler = new Handler(Looper.getMainLooper());
    private long sleepTimerEndsAt = 0L;
    private final Runnable sleepTimerStop = new Runnable() {
        @Override
        public void run() {
            if (sleepTimerEndsAt <= 0L || System.currentTimeMillis() < sleepTimerEndsAt) {
                scheduleSleepTimer();
                return;
            }
            Log.i(DEBUG_TAG, "sleep_timer_expired");
            sleepTimerEndsAt = 0L;
            persistSleepTimer();
            stopPlayback(true);
            stopSelf();
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        this.audioEffectsManager = new AudioEffectsManager(this);
        this.audioFocusController = new AudioFocusController(this, new AudioFocusCallback());
        this.playbackStateRepository = new PlaybackStateRepository(this);
        this.notificationController = new MediaNotificationController(this, new SessionCallback());
        this.queueManager.replace(TrackStore.load(this));
        restoreSleepTimer();
        Log.i(TAG, "service_created");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(NOTIFICATION_ID, currentNotification());
        if (intent == null) {
            restorePlaybackAfterProcessDeath();
            return START_STICKY;
        }
        String action = intent.getAction();
        if (ACTION_TIMER_START.equals(action)) {
            startSleepTimer(intent.getLongExtra(EXTRA_TIMER_MS, 0L));
            return START_STICKY;
        }
        if (ACTION_TIMER_CANCEL.equals(action)) {
            cancelSleepTimer();
            if (this.player == null) {
                stopForeground(true);
                stopSelf();
            }
            return START_STICKY;
        }
        if (ACTION_PLAY_INDEX.equals(action)) {
            this.oneShot = intent.getBooleanExtra(EXTRA_ONE_SHOT, false);
            this.shuffle = intent.getBooleanExtra(EXTRA_SHUFFLE, false);
            this.loopMode = intent.getIntExtra(EXTRA_LOOP_MODE, this.loopMode);
            lastLoopMode = this.loopMode;
            this.pausedByUser = false;
            this.audioFocusController.resetInterruptionState();
            saveResumeState(true, true);
            playIndex(intent.getIntExtra(EXTRA_INDEX, 0), intent.getStringArrayListExtra(EXTRA_QUEUE_URIS), intent.getIntExtra(EXTRA_POSITION, 0), 0);
            return START_STICKY;
        }
        if (ACTION_TOGGLE.equals(action)) {
            if (this.player == null || this.currentIndex < 0 || this.queueManager.isEmpty()) {
                prepareToggleQueue(intent);
            }
            toggle();
            return START_STICKY;
        }
        if (ACTION_NEXT.equals(action)) {
            this.oneShot = false;
            this.pausedByUser = false;
            advanceQueue(PlaybackQueueNavigator.Reason.MANUAL_NEXT, 0);
            return START_STICKY;
        }
        if (ACTION_PREV.equals(action)) {
            this.oneShot = false;
            this.pausedByUser = false;
            advanceQueue(PlaybackQueueNavigator.Reason.MANUAL_PREVIOUS, 0);
            return START_STICKY;
        }
        if (ACTION_SEEK.equals(action)) {
            seekTo(intent.getIntExtra(EXTRA_POSITION, 0));
            return START_STICKY;
        }
        if (ACTION_LOOP.equals(action)) {
            this.loopMode = intent.getIntExtra(EXTRA_LOOP_MODE, 0);
            if (this.loopMode != 0) {
                this.oneShot = false;
            }
            lastLoopMode = this.loopMode;
            saveResumeState(true, true);
            logPlaybackState("loop_changed");
            startForeground(NOTIFICATION_ID, currentNotification());
            return START_STICKY;
        }
        if (ACTION_AUDIO_EFFECTS.equals(action)) {
            applyAudioEffects(this.player);
            if (this.player == null) {
                stopForeground(true);
                stopSelf();
            } else {
                startForeground(NOTIFICATION_ID, currentNotification());
            }
            return START_STICKY;
        }
        if (ACTION_UPDATE_QUEUE.equals(action)) {
            String playingUri = currentUri();
            rebuildQueue(intent.getStringArrayListExtra(EXTRA_QUEUE_URIS));
            int updatedIndex = this.queueManager.indexOfUri(playingUri);
            if (updatedIndex >= 0) {
                this.currentIndex = updatedIndex;
            }
            saveResumeState(true, true);
            return START_STICKY;
        }
        if (ACTION_STOP.equals(action)) {
            Log.i(TAG, "explicit_stop");
            stopPlayback(true);
            stopSelf();
            return START_STICKY;
        }
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void playIndex(int index) {
        playIndex(index, null, 0, 0);
    }

    private void playIndex(int index, ArrayList<String> queueUris, int startPosition, int attempts) {
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
                || this.consecutivePlaybackErrors >= this.queueManager.size()) {
            Log.e(DEBUG_TAG, "all_queue_items_failed errors=" + this.consecutivePlaybackErrors
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
            this.consecutivePlaybackErrors++;
            Log.e(DEBUG_TAG, "start_track_unavailable uri=" + track.uri + " errors=" + this.consecutivePlaybackErrors);
            if (this.consecutivePlaybackErrors >= this.queueManager.size()) {
                stopPlayback(false);
                stopSelf();
            } else {
                advanceQueue(PlaybackQueueNavigator.Reason.ERROR, this.consecutivePlaybackErrors);
            }
            return;
        }
        releasePlayer();
        this.player = new MediaPlayer();
        try {
            this.player.setAudioAttributes(new AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).setUsage(AudioAttributes.USAGE_MEDIA).build());
            this.player.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
            String dataSourceMode = setPlayerDataSource(this.player, track);
            Log.i(DEBUG_TAG, "data_source_ready mode=" + dataSourceMode + " uri=" + track.uri);
            this.player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mediaPlayer) {
                    consecutivePlaybackErrors = 0;
                    logPlaybackState("track_finished");
                    advanceQueue(PlaybackQueueNavigator.Reason.FINISHED, 0);
                }
            });
            this.player.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mediaPlayer, int what, int extra) {
                    playerPreparing = false;
                    consecutivePlaybackErrors++;
                    Log.e(DEBUG_TAG, "media_error what=" + what + " extra=" + extra + " index=" + currentIndex + " uri=" + currentUri() + " errors=" + consecutivePlaybackErrors);
                    if (consecutivePlaybackErrors >= queueManager.size()) {
                        stopPlayback(false);
                        stopSelf();
                    } else {
                        advanceQueue(PlaybackQueueNavigator.Reason.ERROR, consecutivePlaybackErrors);
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
            this.consecutivePlaybackErrors++;
            Log.e(DEBUG_TAG, "prepare_failed index=" + this.currentIndex + " uri=" + track.uri + " error=" + e.getMessage(), e);
            if (this.consecutivePlaybackErrors >= this.queueManager.size()) {
                stopPlayback(false);
                stopSelf();
            } else {
                advanceQueue(PlaybackQueueNavigator.Reason.ERROR, this.consecutivePlaybackErrors);
            }
        }
    }

    private String setPlayerDataSource(MediaPlayer targetPlayer, Track track) throws Exception {
        try {
            targetPlayer.setDataSource(this, track.asUri());
            return "context_uri";
        } catch (Exception directError) {
            Log.w(DEBUG_TAG, "set_data_source_direct_failed uri=" + track.uri + " error=" + directError.getMessage());
        }
        AssetFileDescriptor descriptor = null;
        try {
            descriptor = getContentResolver().openAssetFileDescriptor(track.asUri(), "r");
            if (descriptor == null) {
                throw new IllegalStateException("openAssetFileDescriptor returned null");
            }
            long declaredLength = descriptor.getDeclaredLength();
            if (declaredLength >= 0) {
                targetPlayer.setDataSource(descriptor.getFileDescriptor(), descriptor.getStartOffset(), declaredLength);
                return "asset_fd_range";
            } else {
                targetPlayer.setDataSource(descriptor.getFileDescriptor());
                return "asset_fd";
            }
        } finally {
            if (descriptor != null) {
                try {
                    descriptor.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    private void startPreparedPlayer(MediaPlayer preparedPlayer, int startPosition) {
        if (preparedPlayer != this.player) {
            return;
        }
        this.playerPreparing = false;
        try {
            this.consecutivePlaybackErrors = 0;
            applyAudioEffects(preparedPlayer);
            if (startPosition > 0) {
                preparedPlayer.seekTo(Math.max(0, Math.min(startPosition, safeDuration())));
            }
            if (!this.audioFocusController.requestFocus()) {
                Log.w(TAG, "audio_focus_denied");
                stopPlayback(false);
                return;
            }
            preparedPlayer.start();
            this.audioFocusController.applyPlaybackVolume();
            this.loopOneErrorRetries = 0;
            updateState();
            TrackStore.updateDuration(this, currentUri(), safeDuration());
            saveResumeState(true, true);
            this.audioFocusController.updateNoisyReceiver(safeIsPlaying());
            startForeground(NOTIFICATION_ID, currentNotification());
            logPlaybackState("track_started");
        } catch (Exception e) {
            this.consecutivePlaybackErrors++;
            Log.e(DEBUG_TAG, "start_prepared_failed index=" + this.currentIndex + " uri=" + currentUri() + " error=" + e.getMessage(), e);
            advanceQueue(PlaybackQueueNavigator.Reason.ERROR, this.consecutivePlaybackErrors);
        }
    }

    private void rebuildQueue(ArrayList<String> queueUris) {
        this.queueManager.rebuild(TrackStore.load(this), queueUris);
        Log.i(TAG, "queue_loaded size=" + this.queueManager.size() + " loopMode=" + this.loopMode
                + " oneShot=" + this.oneShot + " shuffle=" + this.shuffle);
    }

    private void prepareToggleQueue(Intent intent) {
        this.shuffle = intent.getBooleanExtra(EXTRA_SHUFFLE, this.shuffle);
        this.loopMode = intent.getIntExtra(EXTRA_LOOP_MODE, this.loopMode);
        lastLoopMode = this.loopMode;
        ArrayList<String> queueUris = intent.getStringArrayListExtra(EXTRA_QUEUE_URIS);
        if (queueUris != null && !queueUris.isEmpty()) {
            rebuildQueue(queueUris);
        } else if (this.queueManager.isEmpty()) {
            restoreQueueForToggle();
        }
        if (intent.hasExtra(EXTRA_INDEX)) {
            this.currentIndex = normalizeIndex(intent.getIntExtra(EXTRA_INDEX, this.currentIndex));
        } else if (this.currentIndex < 0) {
            this.currentIndex = normalizeIndex(this.playbackStateRepository.load().index);
        }
    }

    private void restoreQueueForToggle() {
        PlaybackStateRepository.State savedState = this.playbackStateRepository.load();
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
                reason, this.loopOneErrorRetries);
        this.loopOneErrorRetries = decision.loopOneErrorRetries;
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
        PlaybackStateRepository.State savedState = this.playbackStateRepository.load();
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
        if (this.player == null) {
            playIndex(this.currentIndex < 0 ? 0 : this.currentIndex);
            return;
        }
        if (this.playerPreparing) {
            return;
        }
        if (!safeIsPlaying()) {
            if (!this.audioFocusController.requestFocus()) {
                return;
            }
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
        cancelSleepTimer();
        releasePlayer();
        if (explicit) {
            this.currentIndex = -1;
            lastIndex = -1;
            lastUri = "";
            lastPosition = 0;
            lastDuration = 0;
            this.playbackStateRepository.clear();
        } else {
            updateState();
            saveResumeState(true, true);
        }
        lastPlaying = false;
        this.audioFocusController.stop();
        stopForeground(true);
        logPlaybackState(explicit ? "stop_explicit" : "stop_queue_end");
    }

    private void startSleepTimer(long delayMs) {
        long safeDelayMs = Math.max(1000L, delayMs);
        this.sleepTimerEndsAt = System.currentTimeMillis() + safeDelayMs;
        persistSleepTimer();
        scheduleSleepTimer();
        Log.i(DEBUG_TAG, "sleep_timer_started delayMs=" + safeDelayMs
                + " endsAt=" + this.sleepTimerEndsAt);
    }

    private void restoreSleepTimer() {
        this.sleepTimerEndsAt = getSharedPreferences(TIMER_PREFS, MODE_PRIVATE)
                .getLong(TIMER_ENDS_AT, 0L);
        if (this.sleepTimerEndsAt <= 0L) {
            return;
        }
        if (this.sleepTimerEndsAt <= System.currentTimeMillis()) {
            this.sleepTimerEndsAt = 0L;
            persistSleepTimer();
            return;
        }
        scheduleSleepTimer();
        Log.i(DEBUG_TAG, "sleep_timer_restored endsAt=" + this.sleepTimerEndsAt);
    }

    private void scheduleSleepTimer() {
        this.timerHandler.removeCallbacks(this.sleepTimerStop);
        if (this.sleepTimerEndsAt <= 0L) {
            return;
        }
        long remainingMs = this.sleepTimerEndsAt - System.currentTimeMillis();
        this.timerHandler.postDelayed(this.sleepTimerStop, Math.max(1000L, remainingMs));
    }

    private void cancelSleepTimer() {
        this.sleepTimerEndsAt = 0L;
        this.timerHandler.removeCallbacks(this.sleepTimerStop);
        persistSleepTimer();
        Log.i(DEBUG_TAG, "sleep_timer_cancelled");
    }

    private void persistSleepTimer() {
        getSharedPreferences(TIMER_PREFS, MODE_PRIVATE).edit()
                .putLong(TIMER_ENDS_AT, this.sleepTimerEndsAt)
                .commit();
    }

    static long getSleepTimerEndsAt(Context context) {
        if (instance != null) {
            return instance.sleepTimerEndsAt;
        }
        return context.getApplicationContext()
                .getSharedPreferences(TIMER_PREFS, Context.MODE_PRIVATE)
                .getLong(TIMER_ENDS_AT, 0L);
    }

    private void releasePlayer() {
        this.audioEffectsManager.release();
        if (this.player == null) {
            return;
        }
        this.playerPreparing = false;
        try {
            this.player.setOnPreparedListener(null);
            this.player.setOnCompletionListener(null);
            this.player.setOnErrorListener(null);
            this.player.stop();
        } catch (Exception e) {
        }
        try {
            this.player.release();
        } catch (Exception e) {
            Log.e(TAG, "release_failed", e);
        }
        this.player = null;
    }

    private void applyAudioEffects(MediaPlayer targetPlayer) {
        this.audioEffectsManager.apply(targetPlayer);
    }

    private void updateState() {
        lastIndex = this.currentIndex;
        // Preparing the next queue item is still an active playback request. Persisting false
        // here makes a sticky service restart treat the queue as paused between two tracks.
        lastPlaying = this.player != null && (this.playerPreparing || safeIsPlaying());
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
        this.playbackStateRepository.save(new PlaybackStateRepository.Snapshot(
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
                : new Track("", "MP3 Player", "Музыка готова");
        return this.notificationController.build(
                track, this.player != null, safeIsPlaying(), safePosition(), safeDuration());
    }

    private int safeDuration() {
        if (this.player == null || this.playerPreparing) {
            return currentTrackDurationFallback();
        }
        try {
            int duration = this.player.getDuration();
            return duration > 0 ? duration : currentTrackDurationFallback();
        } catch (Exception e) {
            return currentTrackDurationFallback();
        }
    }

    private int safePosition() {
        if (this.player == null) {
            return 0;
        }
        if (this.playerPreparing) {
            return 0;
        }
        try {
            return this.player.getCurrentPosition();
        } catch (Exception e) {
            return lastPosition;
        }
    }

    private boolean safeIsPlaying() {
        if (this.player == null || this.playerPreparing) {
            return false;
        }
        try {
            return this.player.isPlaying();
        } catch (Exception e) {
            return false;
        }
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
        releasePlayer();
        this.timerHandler.removeCallbacks(this.sleepTimerStop);
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
            pauseInternal(reason);
        }

        @Override
        public void pauseForDisconnectedOutput() {
            pausedByUser = true;
            pauseInternal("audio_becoming_noisy");
        }

        @Override
        public void resumeAfterInterruption() {
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
