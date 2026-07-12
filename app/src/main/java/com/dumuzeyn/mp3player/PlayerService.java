package com.dumuzeyn.mp3player;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaMetadata;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.media.audiofx.DynamicsProcessing;
import android.media.audiofx.Equalizer;
import android.media.audiofx.LoudnessEnhancer;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;
import android.util.LruCache;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.json.JSONArray;

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
    public static final String EXTRA_INDEX = "index";
    public static final String EXTRA_LOOP_MODE = "loopMode";
    public static final String EXTRA_ONE_SHOT = "oneShot";
    public static final String EXTRA_POSITION = "position";
    public static final String EXTRA_QUEUE_URIS = "queueUris";
    public static final String EXTRA_SHUFFLE = "shuffle";
    public static final String RESUME_DURATION = "duration";
    public static final String RESUME_INDEX = "index";
    public static final String RESUME_LOOP_MODE = "loopMode";
    public static final String RESUME_PLAYING = "playing";
    public static final String RESUME_POSITION = "position";
    public static final String RESUME_PREFS = "player_resume";
    public static final String RESUME_QUEUE = "queue";
    public static final String RESUME_SAVED_AT = "savedAt";
    public static final String RESUME_SHUFFLE = "shuffle";
    public static final String RESUME_URI = "uri";

    private static final String CHANNEL_ID = "playback";
    private static final String TAG = "MP3PlayerService";
    private static final String DEBUG_TAG = "MP3PlayerDebug";
    private static final int NOTIFICATION_ID = 7;
    private static PlayerService instance;

    public static int lastIndex = -1;
    public static boolean lastPlaying = false;
    public static int lastDuration = 0;
    public static int lastPosition = 0;
    public static int lastLoopMode = 0;
    public static String lastUri = "";

    private enum AdvanceReason {
        FINISHED,
        MANUAL_NEXT,
        MANUAL_PREVIOUS,
        ERROR
    }

    private final ArrayList<Track> queue = new ArrayList<>();
    private final LruCache<String, Bitmap> coverCache = new LruCache<String, Bitmap>(8 * 1024) {
        @Override
        protected int sizeOf(String key, Bitmap value) {
            return Math.max(1, value.getByteCount() / 1024);
        }
    };
    private final BroadcastReceiver noisyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
                if (uninterruptedPlaybackEnabled()) {
                    Log.i(TAG, "audio_becoming_noisy_ignored");
                    return;
                }
                pausedByUser = true;
                pausedByTransientFocusLoss = false;
                pauseInternal("audio_becoming_noisy");
            }
        }
    };
    private final AudioManager.OnAudioFocusChangeListener audioFocusChangeListener = new AudioManager.OnAudioFocusChangeListener() {
        @Override
        public void onAudioFocusChange(int focusChange) {
            if (uninterruptedPlaybackEnabled()) {
                Log.i(TAG, "audio_focus_change_ignored value=" + focusChange);
                restoreFullVolume();
                return;
            }
            if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
                Log.i(TAG, "audio_focus_loss");
                pausedByTransientFocusLoss = false;
                duckedByFocusLoss = false;
                pauseInternal("focus_loss");
            } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                Log.i(TAG, "audio_focus_loss_transient");
                pausedByTransientFocusLoss = player != null && safeIsPlaying() && !pausedByUser;
                duckedByFocusLoss = false;
                pauseInternal("focus_loss_transient");
            } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
                Log.i(TAG, "audio_focus_duck");
                if (stableVolumeEnabled()) {
                    restoreFullVolume();
                } else if (player != null) {
                    player.setVolume(0.35f, 0.35f);
                    duckedByFocusLoss = true;
                }
            } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
                Log.i(TAG, "audio_focus_gain");
                if (player != null) {
                    player.setVolume(1.0f, 1.0f);
                }
                duckedByFocusLoss = false;
                if (pausedByTransientFocusLoss && !pausedByUser) {
                    pausedByTransientFocusLoss = false;
                    play();
                }
            }
        }
    };

    private MediaSession mediaSession;
    private MediaPlayer player;
    private Equalizer equalizer;
    private DynamicsProcessing dynamicsProcessing;
    private LoudnessEnhancer loudnessEnhancer;
    private AudioManager audioManager;
    private AudioFocusRequest audioFocusRequest;
    private boolean playerPreparing = false;
    private boolean noisyReceiverRegistered = false;
    private long lastResumePositionSavedAt = 0L;
    private String lastSavedQueueJson = "";
    private int currentIndex = -1;
    private boolean oneShot = false;
    private boolean shuffle = false;
    private int loopMode = 0;
    private boolean pausedByUser = false;
    private boolean pausedByTransientFocusLoss = false;
    private boolean duckedByFocusLoss = false;
    private boolean customDynamicsUnsupported = false;
    private int loopOneErrorRetries = 0;
    private int consecutivePlaybackErrors = 0;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        this.audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        createChannel();
        this.mediaSession = new MediaSession(this, "MP3 Player");
        this.mediaSession.setCallback(new SessionCallback());
        this.mediaSession.setActive(true);
        this.queue.addAll(TrackStore.load(this));
        Log.i(TAG, "service_created");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(NOTIFICATION_ID, buildNotification());
        if (intent == null) {
            restorePlaybackAfterProcessDeath();
            return START_STICKY;
        }
        String action = intent.getAction();
        if (ACTION_PLAY_INDEX.equals(action)) {
            this.oneShot = intent.getBooleanExtra(EXTRA_ONE_SHOT, false);
            this.shuffle = intent.getBooleanExtra(EXTRA_SHUFFLE, false);
            this.loopMode = intent.getIntExtra(EXTRA_LOOP_MODE, this.loopMode);
            lastLoopMode = this.loopMode;
            this.pausedByUser = false;
            this.pausedByTransientFocusLoss = false;
            this.duckedByFocusLoss = false;
            saveResumeState(true, true);
            playIndex(intent.getIntExtra(EXTRA_INDEX, 0), intent.getStringArrayListExtra(EXTRA_QUEUE_URIS), intent.getIntExtra(EXTRA_POSITION, 0), 0);
            return START_STICKY;
        }
        if (ACTION_TOGGLE.equals(action)) {
            if (this.player == null || this.currentIndex < 0 || this.queue.isEmpty()) {
                prepareToggleQueue(intent);
            }
            toggle();
            return START_STICKY;
        }
        if (ACTION_NEXT.equals(action)) {
            this.oneShot = false;
            this.pausedByUser = false;
            advanceQueue(AdvanceReason.MANUAL_NEXT, 0);
            return START_STICKY;
        }
        if (ACTION_PREV.equals(action)) {
            this.oneShot = false;
            this.pausedByUser = false;
            advanceQueue(AdvanceReason.MANUAL_PREVIOUS, 0);
            return START_STICKY;
        }
        if (ACTION_SEEK.equals(action)) {
            seekTo(intent.getIntExtra(EXTRA_POSITION, 0));
            return START_STICKY;
        }
        if (ACTION_LOOP.equals(action)) {
            this.loopMode = intent.getIntExtra(EXTRA_LOOP_MODE, 0);
            lastLoopMode = this.loopMode;
            saveResumeState(true, true);
            logPlaybackState("loop_changed");
            startForeground(NOTIFICATION_ID, buildNotification());
            return START_STICKY;
        }
        if (ACTION_AUDIO_EFFECTS.equals(action)) {
            applyAudioEffects(this.player);
            if (this.player == null) {
                stopForeground(true);
                stopSelf();
            } else {
                startForeground(NOTIFICATION_ID, buildNotification());
            }
            return START_STICKY;
        }
        if (ACTION_UPDATE_QUEUE.equals(action)) {
            String playingUri = currentUri();
            rebuildQueue(intent.getStringArrayListExtra(EXTRA_QUEUE_URIS));
            for (int index = 0; index < this.queue.size(); index++) {
                if (this.queue.get(index).uri.equals(playingUri)) {
                    this.currentIndex = index;
                    break;
                }
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
        } else if (this.queue.isEmpty()) {
            this.queue.addAll(TrackStore.load(this));
        }
        if (this.queue.isEmpty()) {
            Log.w(TAG, "empty_queue");
            stopPlayback(false);
            stopSelf();
            return;
        }
        if (attempts >= this.queue.size() || this.consecutivePlaybackErrors >= this.queue.size()) {
            Log.e(DEBUG_TAG, "all_queue_items_failed errors=" + this.consecutivePlaybackErrors + " queue=" + this.queue.size());
            stopPlayback(false);
            stopSelf();
            return;
        }
        this.currentIndex = normalizeIndex(index);
        Track track = this.queue.get(this.currentIndex);
        boolean canOpen = TrackStore.canOpenForRead(this, track.asUri());
        Log.i(DEBUG_TAG, "start_track index=" + this.currentIndex + " title=" + track.title + " uri=" + track.uri + " canOpen=" + canOpen + " attempt=" + attempts);
        if (!canOpen) {
            this.consecutivePlaybackErrors++;
            Log.e(DEBUG_TAG, "start_track_unavailable uri=" + track.uri + " errors=" + this.consecutivePlaybackErrors);
            if (this.consecutivePlaybackErrors >= this.queue.size()) {
                stopPlayback(false);
                stopSelf();
            } else {
                advanceQueue(AdvanceReason.ERROR, this.consecutivePlaybackErrors);
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
                    advanceQueue(AdvanceReason.FINISHED, 0);
                }
            });
            this.player.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mediaPlayer, int what, int extra) {
                    playerPreparing = false;
                    consecutivePlaybackErrors++;
                    Log.e(DEBUG_TAG, "media_error what=" + what + " extra=" + extra + " index=" + currentIndex + " uri=" + currentUri() + " errors=" + consecutivePlaybackErrors);
                    if (consecutivePlaybackErrors >= queue.size()) {
                        stopPlayback(false);
                        stopSelf();
                    } else {
                        advanceQueue(AdvanceReason.ERROR, consecutivePlaybackErrors);
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
            updateNoisyReceiver();
            startForeground(NOTIFICATION_ID, buildNotification());
            this.player.prepareAsync();
            logPlaybackState("track_preparing");
        } catch (Exception e) {
            this.playerPreparing = false;
            this.consecutivePlaybackErrors++;
            Log.e(DEBUG_TAG, "prepare_failed index=" + this.currentIndex + " uri=" + track.uri + " error=" + e.getMessage(), e);
            if (this.consecutivePlaybackErrors >= this.queue.size()) {
                stopPlayback(false);
                stopSelf();
            } else {
                advanceQueue(AdvanceReason.ERROR, this.consecutivePlaybackErrors);
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
            if (!requestAudioFocus()) {
                Log.w(TAG, "audio_focus_denied");
                stopPlayback(false);
                return;
            }
            preparedPlayer.start();
            preparedPlayer.setVolume(duckedByFocusLoss ? 0.35f : 1.0f, duckedByFocusLoss ? 0.35f : 1.0f);
            this.loopOneErrorRetries = 0;
            updateState();
            TrackStore.updateDuration(this, currentUri(), safeDuration());
            saveResumeState(true, true);
            updateNoisyReceiver();
            startForeground(NOTIFICATION_ID, buildNotification());
            logPlaybackState("track_started");
        } catch (Exception e) {
            this.consecutivePlaybackErrors++;
            Log.e(DEBUG_TAG, "start_prepared_failed index=" + this.currentIndex + " uri=" + currentUri() + " error=" + e.getMessage(), e);
            advanceQueue(AdvanceReason.ERROR, this.consecutivePlaybackErrors);
        }
    }

    private void rebuildQueue(ArrayList<String> queueUris) {
        this.queue.clear();
        ArrayList<Track> allTracks = TrackStore.load(this);
        Map<String, Track> tracksByUri = new HashMap<>();
        for (Track track : allTracks) {
            tracksByUri.put(track.uri, track);
        }
        for (String uri : queueUris) {
            Track track = tracksByUri.get(uri);
            if (track != null) {
                this.queue.add(track);
            }
        }
        Log.i(TAG, "queue_loaded size=" + this.queue.size() + " loopMode=" + this.loopMode + " oneShot=" + this.oneShot + " shuffle=" + this.shuffle);
    }

    private void prepareToggleQueue(Intent intent) {
        this.shuffle = intent.getBooleanExtra(EXTRA_SHUFFLE, this.shuffle);
        this.loopMode = intent.getIntExtra(EXTRA_LOOP_MODE, this.loopMode);
        lastLoopMode = this.loopMode;
        ArrayList<String> queueUris = intent.getStringArrayListExtra(EXTRA_QUEUE_URIS);
        if (queueUris != null && !queueUris.isEmpty()) {
            rebuildQueue(queueUris);
        } else if (this.queue.isEmpty()) {
            restoreQueueForToggle();
        }
        if (intent.hasExtra(EXTRA_INDEX)) {
            this.currentIndex = normalizeIndex(intent.getIntExtra(EXTRA_INDEX, this.currentIndex));
        } else if (this.currentIndex < 0) {
            this.currentIndex = normalizeIndex(getSharedPreferences(RESUME_PREFS, 0).getInt(RESUME_INDEX, 0));
        }
    }

    private void restoreQueueForToggle() {
        SharedPreferences prefs = getSharedPreferences(RESUME_PREFS, 0);
        ArrayList<String> uris = new ArrayList<>();
        try {
            JSONArray savedQueue = new JSONArray(prefs.getString(RESUME_QUEUE, "[]"));
            for (int i = 0; i < savedQueue.length(); i++) {
                String uri = savedQueue.optString(i, "");
                if (!uri.isEmpty()) {
                    uris.add(uri);
                }
            }
        } catch (Exception ignored) {
        }
        if (uris.isEmpty()) {
            String uri = prefs.getString(RESUME_URI, "");
            if (!uri.isEmpty()) {
                uris.add(uri);
            }
        }
        if (!uris.isEmpty()) {
            rebuildQueue(uris);
        }
        if (this.queue.isEmpty()) {
            this.queue.addAll(TrackStore.load(this));
        }
    }

    private void advanceQueue(AdvanceReason reason, int attempts) {
        if (this.queue.isEmpty()) {
            stopPlayback(false);
            stopSelf();
            return;
        }
        int nextIndex;
        if (reason == AdvanceReason.MANUAL_PREVIOUS) {
            nextIndex = this.currentIndex <= 0 ? this.queue.size() - 1 : this.currentIndex - 1;
            playIndex(nextIndex, null, 0, attempts);
            return;
        }
        if (reason == AdvanceReason.FINISHED && this.loopMode == 1) {
            playIndex(this.currentIndex, null, 0, attempts);
            return;
        }
        if (reason == AdvanceReason.ERROR && this.loopMode == 1 && this.loopOneErrorRetries == 0) {
            this.loopOneErrorRetries++;
            playIndex(this.currentIndex, null, 0, attempts);
            return;
        }
        this.loopOneErrorRetries = 0;
        if (this.oneShot && reason == AdvanceReason.FINISHED) {
            stopPlayback(false);
            stopSelf();
            return;
        }
        if (this.loopMode == 0 && reason == AdvanceReason.FINISHED && this.currentIndex >= this.queue.size() - 1) {
            stopPlayback(false);
            stopSelf();
            return;
        }
        nextIndex = this.currentIndex < 0 ? 0 : (this.currentIndex + 1) % this.queue.size();
        if (this.currentIndex == this.queue.size() - 1 && nextIndex == 0) {
            Log.i(TAG, "wrap_last_to_first");
        }
        Log.i(TAG, "advance reason=" + reason + " nextIndex=" + nextIndex + " attempts=" + attempts);
        playIndex(nextIndex, null, 0, attempts);
    }

    private int normalizeIndex(int index) {
        if (this.queue.isEmpty()) {
            return -1;
        }
        return Math.max(0, Math.min(index, this.queue.size() - 1));
    }

    private void restorePlaybackAfterProcessDeath() {
        if (this.player != null) {
            return;
        }
        SharedPreferences prefs = getSharedPreferences(RESUME_PREFS, 0);
        if (!prefs.getBoolean(RESUME_PLAYING, false)) {
            return;
        }
        JSONArray savedQueue;
        try {
            savedQueue = new JSONArray(prefs.getString(RESUME_QUEUE, "[]"));
        } catch (Exception e) {
            savedQueue = new JSONArray();
        }
        ArrayList<String> uris = new ArrayList<>();
        for (int index = 0; index < savedQueue.length(); index++) {
            uris.add(savedQueue.optString(index, ""));
        }
        if (uris.isEmpty()) {
            String uri = prefs.getString(RESUME_URI, "");
            if (!uri.isEmpty()) {
                uris.add(uri);
            }
        }
        this.loopMode = prefs.getInt(RESUME_LOOP_MODE, 0);
        this.shuffle = prefs.getBoolean(RESUME_SHUFFLE, false);
        this.oneShot = false;
        int index = prefs.getInt(RESUME_INDEX, 0);
        int position = Math.max(0, prefs.getInt(RESUME_POSITION, 0));
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
            this.pausedByTransientFocusLoss = false;
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
            if (!requestAudioFocus()) {
                return;
            }
            try {
                this.player.start();
                this.player.setVolume(duckedByFocusLoss ? 0.35f : 1.0f, duckedByFocusLoss ? 0.35f : 1.0f);
                updateState();
                saveResumeState(true, false);
                updateNoisyReceiver();
                startForeground(NOTIFICATION_ID, buildNotification());
                logPlaybackState("play");
            } catch (Exception e) {
                Log.e(TAG, "start_failed", e);
                advanceQueue(AdvanceReason.ERROR, 1);
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
            updateNoisyReceiver();
            startForeground(NOTIFICATION_ID, buildNotification());
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
            startForeground(NOTIFICATION_ID, buildNotification());
        } catch (Exception e) {
            Log.e(TAG, "seek_failed", e);
            advanceQueue(AdvanceReason.ERROR, 1);
        }
    }

    private void stopPlayback(boolean explicit) {
        releasePlayer();
        if (explicit) {
            this.currentIndex = -1;
            lastIndex = -1;
            lastUri = "";
            lastPosition = 0;
            lastDuration = 0;
            getSharedPreferences(RESUME_PREFS, 0).edit().clear().apply();
        } else {
            updateState();
            saveResumeState(true, true);
        }
        lastPlaying = false;
        unregisterNoisyReceiver();
        abandonAudioFocus();
        stopForeground(true);
        logPlaybackState(explicit ? "stop_explicit" : "stop_queue_end");
    }

    private void releasePlayer() {
        releaseAudioEffects();
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
        releaseAudioEffects();
        if (targetPlayer == null) {
            return;
        }
        SharedPreferences preferences = getSharedPreferences(EqualizerController.PREFS, 0);
        int audioSessionId;
        try {
            audioSessionId = targetPlayer.getAudioSessionId();
        } catch (RuntimeException error) {
            Log.w(DEBUG_TAG, "audio_effect_session_unavailable error=" + error.getMessage());
            return;
        }
        if (preferences.getBoolean(EqualizerController.ENABLED, false)) {
            applyEqualizer(preferences, audioSessionId);
        }
        if (preferences.getBoolean(VolumeLevelingController.ENABLED, false)) {
            applyVolumeLeveling(audioSessionId);
        }
    }

    private void applyEqualizer(SharedPreferences preferences, int audioSessionId) {
        try {
            Equalizer effect = new Equalizer(0, audioSessionId);
            short[] levelRange = effect.getBandLevelRange();
            short bandCount = effect.getNumberOfBands();
            for (short band = 0; band < bandCount; band++) {
                int profileIndex = bandCount <= 1
                        ? 0
                        : Math.round((band * (EqualizerController.BAND_COUNT - 1.0f)) / (bandCount - 1.0f));
                int db = preferences.getInt(EqualizerController.BAND_PREFIX + profileIndex, 0);
                short milliBel = (short) Math.max(levelRange[0], Math.min(levelRange[1], db * 100));
                effect.setBandLevel(band, milliBel);
            }
            effect.setEnabled(true);
            this.equalizer = effect;
            Log.i(DEBUG_TAG, "equalizer_applied session=" + audioSessionId + " bands=" + bandCount);
        } catch (RuntimeException error) {
            Log.w(DEBUG_TAG, "equalizer_unavailable session=" + audioSessionId + " error=" + error.getMessage());
        }
    }

    private void applyVolumeLeveling(int audioSessionId) {
        if (Build.VERSION.SDK_INT < 28) {
            Log.w(DEBUG_TAG, "volume_leveling_requires_api_28 sdk=" + Build.VERSION.SDK_INT);
            return;
        }
        if (this.customDynamicsUnsupported) {
            applyCompatibleVolumeLeveling(audioSessionId);
            return;
        }
        try {
            DynamicsProcessing.Mbc multibandCompressor = new DynamicsProcessing.Mbc(true, true, 1);
            multibandCompressor.setBand(0, new DynamicsProcessing.MbcBand(
                    true, 20000.0f, 12.0f, 250.0f, 4.0f, -18.0f, 6.0f,
                    -80.0f, 1.0f, 0.0f, 5.0f));
            DynamicsProcessing.Limiter limiter = new DynamicsProcessing.Limiter(
                    true, true, 0, 2.0f, 120.0f, 10.0f, -1.0f, 0.0f);
            DynamicsProcessing.Config config = new DynamicsProcessing.Config.Builder(
                    DynamicsProcessing.VARIANT_FAVOR_TIME_RESOLUTION,
                    2, false, 0, true, 1, false, 0, true)
                    .setMbcAllChannelsTo(multibandCompressor)
                    .setLimiterAllChannelsTo(limiter)
                    .build();
            DynamicsProcessing effect = new DynamicsProcessing(0, audioSessionId, config);
            effect.setEnabled(true);
            this.dynamicsProcessing = effect;
            Log.i(DEBUG_TAG, "volume_leveling_applied session=" + audioSessionId);
        } catch (RuntimeException customConfigError) {
            this.customDynamicsUnsupported = true;
            Log.w(DEBUG_TAG, "volume_leveling_custom_config_failed session=" + audioSessionId + " error=" + customConfigError.getMessage());
            applyCompatibleVolumeLeveling(audioSessionId);
        }
    }

    private void applyCompatibleVolumeLeveling(int audioSessionId) {
        try {
            DynamicsProcessing effect = new DynamicsProcessing(0, audioSessionId, null);
            int channelCount = effect.getChannelCount();
            for (int channel = 0; channel < channelCount; channel++) {
                DynamicsProcessing.Mbc compressor = effect.getMbcByChannelIndex(channel);
                for (int band = 0; band < compressor.getBandCount(); band++) {
                    DynamicsProcessing.MbcBand settings = compressor.getBand(band);
                    settings.setEnabled(true);
                    settings.setAttackTime(12.0f);
                    settings.setReleaseTime(250.0f);
                    settings.setRatio(4.0f);
                    settings.setThreshold(-18.0f);
                    settings.setKneeWidth(6.0f);
                    settings.setNoiseGateThreshold(-80.0f);
                    settings.setExpanderRatio(1.0f);
                    settings.setPostGain(5.0f);
                    effect.setMbcBandByChannelIndex(channel, band, settings);
                }
                DynamicsProcessing.Limiter limiter = effect.getLimiterByChannelIndex(channel);
                limiter.setEnabled(true);
                limiter.setAttackTime(2.0f);
                limiter.setReleaseTime(120.0f);
                limiter.setRatio(10.0f);
                limiter.setThreshold(-1.0f);
                effect.setLimiterByChannelIndex(channel, limiter);
            }
            effect.setEnabled(true);
            this.dynamicsProcessing = effect;
            Log.i(DEBUG_TAG, "volume_leveling_applied_compatible session=" + audioSessionId + " channels=" + channelCount);
        } catch (RuntimeException defaultConfigError) {
            Log.w(DEBUG_TAG, "volume_leveling_default_config_failed session=" + audioSessionId + " error=" + defaultConfigError.getMessage());
            applyLoudnessFallback(audioSessionId);
        }
    }

    private void applyLoudnessFallback(int audioSessionId) {
        try {
            LoudnessEnhancer effect = new LoudnessEnhancer(audioSessionId);
            effect.setTargetGain(350);
            effect.setEnabled(true);
            this.loudnessEnhancer = effect;
            Log.i(DEBUG_TAG, "volume_leveling_loudness_fallback session=" + audioSessionId);
        } catch (RuntimeException error) {
            Log.w(DEBUG_TAG, "volume_leveling_unavailable session=" + audioSessionId + " error=" + error.getMessage());
        }
    }

    private void releaseAudioEffects() {
        if (this.equalizer != null) {
            try {
                this.equalizer.release();
            } catch (RuntimeException ignored) {
            }
            this.equalizer = null;
        }
        if (this.dynamicsProcessing != null) {
            try {
                this.dynamicsProcessing.release();
            } catch (RuntimeException ignored) {
            }
            this.dynamicsProcessing = null;
        }
        if (this.loudnessEnhancer != null) {
            try {
                this.loudnessEnhancer.release();
            } catch (RuntimeException ignored) {
            }
            this.loudnessEnhancer = null;
        }
    }

    private void updateState() {
        lastIndex = this.currentIndex;
        lastPlaying = this.player != null && safeIsPlaying();
        int currentDuration = safeDuration();
        if (currentDuration <= 0 && this.currentIndex >= 0 && this.currentIndex < this.queue.size()) {
            currentDuration = Math.max(0, this.queue.get(this.currentIndex).durationMs);
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
        return (this.currentIndex < 0 || this.currentIndex >= this.queue.size()) ? "" : this.queue.get(this.currentIndex).uri;
    }

    private void saveResumeState(boolean forcePosition, boolean includeQueue) {
        if (this.currentIndex < 0 || this.currentIndex >= this.queue.size()) {
            return;
        }
        JSONArray queueJsonArray = new JSONArray();
        Iterator<Track> it = this.queue.iterator();
        while (it.hasNext()) {
            queueJsonArray.put(it.next().uri);
        }
        String queueJson = queueJsonArray.toString();
        SharedPreferences.Editor edit = getSharedPreferences(RESUME_PREFS, 0).edit();
        edit.putString(RESUME_URI, this.queue.get(this.currentIndex).uri);
        edit.putInt(RESUME_POSITION, Math.max(0, lastPosition));
        edit.putInt(RESUME_DURATION, Math.max(0, lastDuration));
        edit.putInt(RESUME_INDEX, Math.max(0, this.currentIndex));
        edit.putInt(RESUME_LOOP_MODE, this.loopMode);
        edit.putBoolean(RESUME_PLAYING, lastPlaying);
        edit.putBoolean(RESUME_SHUFFLE, this.shuffle);
        edit.putLong(RESUME_SAVED_AT, System.currentTimeMillis());
        if (includeQueue || !queueJson.equals(this.lastSavedQueueJson)) {
            edit.putString(RESUME_QUEUE, queueJson);
            this.lastSavedQueueJson = queueJson;
        }
        edit.apply();
        if (forcePosition || lastPlaying) {
            this.lastResumePositionSavedAt = System.currentTimeMillis();
        }
    }

    public static void refreshSnapshot() {
        if (instance != null) {
            instance.updateState();
        }
    }

    public static void refreshAppearance() {
        if (instance != null && instance.player != null) {
            instance.startForeground(NOTIFICATION_ID, instance.buildNotification());
        }
    }

    private Notification buildNotification() {
        Track track = (this.currentIndex >= 0 && this.currentIndex < this.queue.size()) ? this.queue.get(this.currentIndex) : new Track("", "MP3 Player", "Музыка готова");
        SharedPreferences uiPrefs = getSharedPreferences("mp3_player_ui", 0);
        String theme = uiPrefs.getString("theme", "light");
        int customBackground = uiPrefs.getInt("customBg", 0xffffffff);
        boolean darkTheme = "dark".equals(theme)
                || ("custom".equals(theme) && ThemeManager.isDarkColor(customBackground));
        boolean circularCover = uiPrefs.getBoolean("circularCovers", false);
        int accentColor = "custom".equals(theme)
                ? uiPrefs.getInt("customFg", 0xff7c32e8)
                : darkTheme ? 0xffa35cff : 0xff7c32e8;
        ComponentName launcher = new ComponentName(this,
                getPackageName() + (darkTheme ? ".LauncherDark" : ".LauncherLight"));
        Intent launchIntent = new Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LAUNCHER)
                .setComponent(launcher);
        PendingIntent activity = PendingIntent.getActivity(this, 1, launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder builder = Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(this, CHANNEL_ID) : new Notification.Builder(this);
        int playPauseIcon = safeIsPlaying() ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play;
        Bitmap rawCover = coverFor(track);
        Bitmap cover = circularCover ? circularBitmap(rawCover) : rawCover;
        Bitmap themedIcon = themedAppIcon(darkTheme);
        builder.setSmallIcon(getResources().getIdentifier("ic_notification_music", "drawable", getPackageName()))
                .setContentTitle(track.title)
                .setContentText(track.artist)
                .setLargeIcon(cover)
                .setContentIntent(activity)
                .setOngoing(this.player != null && safeIsPlaying())
                .setCategory(Notification.CATEGORY_TRANSPORT)
                .setPriority(Notification.PRIORITY_LOW)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setColor(accentColor)
                .addAction(android.R.drawable.ic_media_previous, "Назад", serviceIntent(ACTION_PREV, 2))
                .addAction(playPauseIcon, safeIsPlaying() ? "Пауза" : "Играть", serviceIntent(ACTION_TOGGLE, 3))
                .addAction(android.R.drawable.ic_media_next, "Дальше", serviceIntent(ACTION_NEXT, 4));
        if (Build.VERSION.SDK_INT >= 26) {
            builder.setColorized(true);
        }
        this.mediaSession.setSessionActivity(activity);
        updateMediaSession(track, cover, themedIcon);
        builder.setStyle(new Notification.MediaStyle().setMediaSession(this.mediaSession.getSessionToken()).setShowActionsInCompactView(0, 1, 2));
        return builder.build();
    }

    private void updateMediaSession(Track track, Bitmap cover, Bitmap themedIcon) {
        long currentPosition = safePosition();
        long duration = safeDuration();
        int state = safeIsPlaying() ? PlaybackState.STATE_PLAYING : PlaybackState.STATE_PAUSED;
        MediaMetadata.Builder metadata = new MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_TITLE, track.title)
                .putString(MediaMetadata.METADATA_KEY_ARTIST, track.artist)
                .putString(MediaMetadata.METADATA_KEY_ALBUM, track.album)
                .putLong(MediaMetadata.METADATA_KEY_DURATION, duration);
        if (cover != null) {
            metadata.putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, cover);
            metadata.putBitmap(MediaMetadata.METADATA_KEY_ART, cover);
        }
        if (themedIcon != null) {
            metadata.putBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON, themedIcon);
        }
        this.mediaSession.setMetadata(metadata.build());
        this.mediaSession.setPlaybackState(new PlaybackState.Builder().setActions(PlaybackState.ACTION_PLAY | PlaybackState.ACTION_PAUSE | PlaybackState.ACTION_PLAY_PAUSE | PlaybackState.ACTION_SKIP_TO_NEXT | PlaybackState.ACTION_SKIP_TO_PREVIOUS | PlaybackState.ACTION_SEEK_TO | PlaybackState.ACTION_STOP).setState(state, currentPosition, 1.0f).build());
    }

    private Bitmap coverFor(Track track) {
        if (track == null || track.uri == null || track.uri.isEmpty()) {
            return null;
        }
        Bitmap cached = this.coverCache.get(track.uri);
        if (cached != null) {
            return cached;
        }
        Bitmap cover = readCover(track);
        if (cover != null) {
            this.coverCache.put(track.uri, cover);
        }
        return cover;
    }

    private Bitmap readCover(Track track) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(this, track.asUri());
            byte[] data = retriever.getEmbeddedPicture();
            if (data == null || data.length == 0) {
                return null;
            }
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(data, 0, data.length, bounds);
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = coverSampleSize(bounds, 512);
            return BitmapFactory.decodeByteArray(data, 0, data.length, options);
        } catch (Exception e) {
            Log.w(DEBUG_TAG, "service_cover_failed uri=" + track.uri + " error=" + e.getMessage());
            return null;
        } finally {
            try {
                retriever.release();
            } catch (Exception ignored) {
            }
        }
    }

    private Bitmap circularBitmap(Bitmap source) {
        if (source == null || source.isRecycled()) {
            return source;
        }
        int size = Math.max(1, Math.min(source.getWidth(), source.getHeight()));
        Bitmap output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);
        BitmapShader shader = new BitmapShader(source, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
        float scale = Math.max((float) size / source.getWidth(), (float) size / source.getHeight());
        Matrix matrix = new Matrix();
        matrix.setScale(scale, scale);
        matrix.postTranslate((size - source.getWidth() * scale) * 0.5f,
                (size - source.getHeight() * scale) * 0.5f);
        shader.setLocalMatrix(matrix);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        paint.setShader(shader);
        canvas.drawCircle(size * 0.5f, size * 0.5f, size * 0.5f, paint);
        return output;
    }

    private Bitmap themedAppIcon(boolean darkTheme) {
        try {
            Drawable drawable = getResources().getDrawable(
                    darkTheme ? R.mipmap.ic_launcher_dark : R.mipmap.ic_launcher_home);
            int size = 128;
            Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, size, size);
            drawable.draw(canvas);
            return bitmap;
        } catch (RuntimeException error) {
            return null;
        }
    }

    private int coverSampleSize(BitmapFactory.Options options, int maxSize) {
        int sampleSize = 1;
        int width = options.outWidth;
        int height = options.outHeight;
        while (width / sampleSize > maxSize * 2 || height / sampleSize > maxSize * 2) {
            sampleSize *= 2;
        }
        return Math.max(1, sampleSize);
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
        if (this.currentIndex >= 0 && this.currentIndex < this.queue.size()) {
            return Math.max(0, this.queue.get(this.currentIndex).durationMs);
        }
        return Math.max(0, lastDuration);
    }

    private PendingIntent serviceIntent(String action, int requestCode) {
        Intent intent = new Intent(this, PlayerService.class);
        intent.setAction(action);
        return PendingIntent.getService(this, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private boolean requestAudioFocus() {
        if (uninterruptedPlaybackEnabled()) {
            return true;
        }
        if (this.audioManager == null) {
            return true;
        }
        int result;
        if (Build.VERSION.SDK_INT >= 26) {
            if (this.audioFocusRequest == null) {
                this.audioFocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                        .setAudioAttributes(new AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).setUsage(AudioAttributes.USAGE_MEDIA).build())
                        .setOnAudioFocusChangeListener(this.audioFocusChangeListener)
                        .build();
            }
            result = this.audioManager.requestAudioFocus(this.audioFocusRequest);
        } else {
            result = this.audioManager.requestAudioFocus(this.audioFocusChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        }
        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
    }

    private boolean uninterruptedPlaybackEnabled() {
        return getSharedPreferences(UninterruptedPlaybackController.PREFS, 0)
                .getBoolean(UninterruptedPlaybackController.ENABLED, false);
    }

    private boolean stableVolumeEnabled() {
        return getSharedPreferences(UninterruptedPlaybackController.PREFS, 0)
                .getBoolean(StableVolumeController.ENABLED, false);
    }

    private void restoreFullVolume() {
        if (this.player != null) {
            try {
                this.player.setVolume(1.0f, 1.0f);
            } catch (RuntimeException ignored) {
            }
        }
        this.duckedByFocusLoss = false;
    }

    private void abandonAudioFocus() {
        if (this.audioManager == null) {
            return;
        }
        if (Build.VERSION.SDK_INT >= 26 && this.audioFocusRequest != null) {
            this.audioManager.abandonAudioFocusRequest(this.audioFocusRequest);
        } else {
            this.audioManager.abandonAudioFocus(this.audioFocusChangeListener);
        }
    }

    private void updateNoisyReceiver() {
        if (this.player != null && safeIsPlaying()) {
            registerNoisyReceiver();
        } else {
            unregisterNoisyReceiver();
        }
    }

    private void registerNoisyReceiver() {
        if (this.noisyReceiverRegistered) {
            return;
        }
        registerReceiver(this.noisyReceiver, new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY));
        this.noisyReceiverRegistered = true;
    }

    private void unregisterNoisyReceiver() {
        if (!this.noisyReceiverRegistered) {
            return;
        }
        try {
            unregisterReceiver(this.noisyReceiver);
        } catch (Exception e) {
        }
        this.noisyReceiverRegistered = false;
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT < 26) {
            return;
        }
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Музыка", NotificationManager.IMPORTANCE_LOW);
        NotificationManager notificationManager = (NotificationManager) getSystemService(NotificationManager.class);
        if (notificationManager != null) {
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void logPlaybackState(String event) {
        Log.i(TAG, event + " index=" + this.currentIndex + " queue=" + this.queue.size() + " loopMode=" + this.loopMode + " oneShot=" + this.oneShot + " shuffle=" + this.shuffle + " player=" + (this.player != null) + " playing=" + safeIsPlaying());
    }

    @Override
    public void onDestroy() {
        logPlaybackState("on_destroy");
        releasePlayer();
        unregisterNoisyReceiver();
        abandonAudioFocus();
        if (this.mediaSession != null) {
            this.mediaSession.release();
        }
        instance = null;
        super.onDestroy();
    }

    @Override
    public void onTaskRemoved(Intent intent) {
        logPlaybackState("on_task_removed");
        if (this.player != null && safeIsPlaying()) {
            startForeground(NOTIFICATION_ID, buildNotification());
        }
        super.onTaskRemoved(intent);
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
            pausedByTransientFocusLoss = false;
            pauseInternal("media_session_pause");
        }

        @Override
        public void onSkipToNext() {
            oneShot = false;
            advanceQueue(AdvanceReason.MANUAL_NEXT, 0);
        }

        @Override
        public void onSkipToPrevious() {
            oneShot = false;
            advanceQueue(AdvanceReason.MANUAL_PREVIOUS, 0);
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
