package com.dumuzeyn.mp3player;

import android.app.PendingIntent;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.session.DefaultMediaNotificationProvider;
import androidx.media3.session.MediaSession;
import androidx.media3.session.MediaSessionService;
import androidx.media3.session.SessionCommand;
import androidx.media3.session.SessionCommands;
import androidx.media3.session.SessionError;
import androidx.media3.session.SessionResult;
import com.dumuzeyn.mp3player.data.playback.PlaybackStateManager;
import com.dumuzeyn.mp3player.playback.service.PlaybackErrorRecovery;
import com.dumuzeyn.mp3player.playback.service.PlaybackSleepTimer;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.ArrayList;
import java.util.List;

@SuppressLint("UnsafeOptInUsageError")
public final class Media3PlayerService extends MediaSessionService {
    private static final String TAG = "VoltuneMedia3";
    private static final int NOTIFICATION_ID = 7;
    private static final long POSITION_SAVE_INTERVAL_MS = 7000L;

    private final MediaItemMapper mapper = new MediaItemMapper();
    private final PlaybackTransitionPolicy transitionPolicy = new PlaybackTransitionPolicy();
    private final PlaybackErrorRecovery errorRecovery = new PlaybackErrorRecovery();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable positionSaver = new Runnable() {
        @Override
        public void run() {
            if (player != null && player.isPlaying()) {
                persistState(false);
                handler.postDelayed(this, POSITION_SAVE_INTERVAL_MS);
            }
        }
    };

    private ExoPlayer player;
    private MediaSession mediaSession;
    private MediaArtworkProvider artworkProvider;
    private PlaybackStateManager stateManager;
    private PlaybackSleepTimer sleepTimer;
    private AudioEffectsManager audioEffects;
    private TrackLoudnessNormalizer loudnessNormalizer;
    private PauseReason pauseReason = PauseReason.NONE;
    private StopReason stopReason = StopReason.NONE;
    @Nullable private PlaybackErrorInfo lastError;
    private int audioSessionId = C.AUDIO_SESSION_ID_UNSET;

    @Override
    public void onCreate() {
        super.onCreate();
        stateManager = new PlaybackStateManager(this);
        sleepTimer = new PlaybackSleepTimer(this, this::onSleepTimerExpired);
        audioEffects = new AudioEffectsManager(this);
        loudnessNormalizer = new TrackLoudnessNormalizer(this);
        artworkProvider = new MediaArtworkProvider(this);

        boolean uninterrupted = getSharedPreferences(
                UninterruptedPlaybackController.PREFS, MODE_PRIVATE)
                .getBoolean(UninterruptedPlaybackController.ENABLED, false);
        AudioAttributes attributes = new AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build();
        player = new ExoPlayer.Builder(this)
                .setAudioAttributes(attributes, !uninterrupted)
                .setHandleAudioBecomingNoisy(!uninterrupted)
                .build();
        player.addListener(new PlayerEvents());
        mediaSession = new MediaSession.Builder(this, player)
                .setBitmapLoader(artworkProvider)
                .setCallback(new SessionCallback())
                .setSessionActivity(PendingIntent.getActivity(this, 0,
                        new Intent(this, MainActivity.class),
                        PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT))
                .build();
        DefaultMediaNotificationProvider provider =
                new DefaultMediaNotificationProvider.Builder(this)
                        .setNotificationId(NOTIFICATION_ID)
                        .build();
        provider.setSmallIcon(R.drawable.ic_notification_music);
        setMediaNotificationProvider(provider);
        restoreState();
        sleepTimer.restore();
        Log.i(TAG, "service_created");
    }

    @Nullable
    @Override
    public MediaSession onGetSession(MediaSession.ControllerInfo controllerInfo) {
        return mediaSession;
    }

    @Override
    public void onTaskRemoved(@Nullable Intent rootIntent) {
        if (player == null || (!player.isPlaying()
                && player.getPlaybackState() != Player.STATE_BUFFERING)) {
            stopSelf();
        }
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        if (sleepTimer != null) {
            sleepTimer.close();
        }
        if (player != null) {
            if (stopReason == StopReason.NONE) {
                stopReason = StopReason.SERVICE_DESTROYED;
            }
            persistState(true);
        }
        if (mediaSession != null) {
            mediaSession.release();
            mediaSession = null;
        }
        if (player != null) {
            player.release();
            player = null;
        }
        if (audioEffects != null) {
            audioEffects.release();
        }
        if (loudnessNormalizer != null) {
            loudnessNormalizer.release();
        }
        if (artworkProvider != null) {
            artworkProvider.close();
        }
        super.onDestroy();
    }

    private void restoreState() {
        PlaybackStateManager.State state = stateManager.load();
        ArrayList<Track> queue = PlaybackQueueResolver.restore(
                TrackStore.load(this), state.queueUris, null);
        if (queue.isEmpty()) {
            return;
        }
        int index = Math.max(0, Math.min(state.index, queue.size() - 1));
        player.setMediaItems(toMediaItems(queue), index, Math.max(0, state.position));
        player.setRepeatMode(toMedia3RepeatMode(state.loopMode));
        player.setShuffleModeEnabled(false);
        player.prepare();
        if (state.playing) {
            player.play();
        }
    }

    private ArrayList<MediaItem> toMediaItems(List<Track> tracks) {
        ArrayList<MediaItem> items = new ArrayList<>();
        for (Track track : tracks) {
            items.add(mapper.toMediaItem(track));
        }
        return items;
    }

    private void onSleepTimerExpired() {
        pauseReason = PauseReason.SLEEP_TIMER;
        stopReason = StopReason.SLEEP_TIMER;
        player.pause();
        player.stop();
        persistState(true);
    }

    private void applyAudioEffects() {
        if (audioSessionId == C.AUDIO_SESSION_ID_UNSET) {
            return;
        }
        audioEffects.apply(audioSessionId, loudnessNormalizer.cachedGainDb(currentUri()));
    }

    private void prefetchLoudness() {
        List<Track> queue = currentTracks();
        if (!queue.isEmpty()) {
            loudnessNormalizer.prefetch(queue,
                    Math.max(0, Math.min(player.getCurrentMediaItemIndex(), queue.size() - 1)));
        }
    }

    private void recoverFromError(PlaybackException error) {
        int failures = errorRecovery.recordError();
        int queueSize = player.getMediaItemCount();
        boolean recoverable = queueSize > 1;
        lastError = new PlaybackErrorInfo(error.errorCode, error.getErrorCodeName(),
                recoverable, error.getMessage(), currentMediaId());
        if (transitionPolicy.shouldSkipError(failures, queueSize, recoverable)) {
            int next = (Math.max(0, player.getCurrentMediaItemIndex()) + 1) % queueSize;
            player.seekToDefaultPosition(next);
            player.prepare();
            player.play();
            return;
        }
        stopReason = transitionPolicy.stopReasonForError(failures, queueSize, recoverable);
        player.stop();
        persistState(true);
    }

    private void persistState(boolean includeQueue) {
        if (player == null) {
            return;
        }
        stateManager.save(new PlaybackStateManager.Snapshot(
                currentUri(), safeInt(player.getCurrentPosition()), safeInt(player.getDuration()),
                player.getCurrentMediaItemIndex(), fromMedia3RepeatMode(player.getRepeatMode()),
                player.getPlayWhenReady(), false, currentTracks()), includeQueue);
    }

    private ArrayList<Track> currentTracks() {
        ArrayList<Track> queue = new ArrayList<>();
        List<Track> library = TrackStore.load(this);
        for (int itemIndex = 0; itemIndex < player.getMediaItemCount(); itemIndex++) {
            String mediaId = player.getMediaItemAt(itemIndex).mediaId;
            for (Track track : library) {
                if (mapper.mediaId(track).equals(mediaId)) {
                    queue.add(track);
                    break;
                }
            }
        }
        return queue;
    }

    private String currentUri() {
        MediaItem current = player.getCurrentMediaItem();
        return current == null || current.localConfiguration == null
                ? "" : current.localConfiguration.uri.toString();
    }

    private String currentMediaId() {
        MediaItem current = player.getCurrentMediaItem();
        return current == null ? "" : current.mediaId;
    }

    private PlaybackPhase phase() {
        if (lastError != null && player.getPlaybackState() == Player.STATE_IDLE) {
            return PlaybackPhase.ERROR;
        }
        switch (player.getPlaybackState()) {
            case Player.STATE_BUFFERING:
                return PlaybackPhase.BUFFERING;
            case Player.STATE_READY:
                return PlaybackPhase.READY;
            case Player.STATE_ENDED:
                return PlaybackPhase.ENDED;
            default:
                return PlaybackPhase.IDLE;
        }
    }

    private PlaybackSnapshot snapshot() {
        ArrayList<String> ids = new ArrayList<>();
        for (int index = 0; index < player.getMediaItemCount(); index++) {
            ids.add(player.getMediaItemAt(index).mediaId);
        }
        return new PlaybackSnapshot(ids, currentMediaId(), player.getCurrentMediaItemIndex(),
                player.getCurrentPosition(), player.getDuration(), player.getPlayWhenReady(),
                player.getPlaybackState(), player.getRepeatMode(), false, phase(), pauseReason,
                stopReason, lastError, System.currentTimeMillis());
    }

    private Bundle snapshotBundle() {
        PlaybackSnapshot value = snapshot();
        Bundle bundle = new Bundle();
        bundle.putString("mediaId", value.currentMediaId);
        bundle.putInt("index", value.currentIndex);
        bundle.putLong("position", value.positionMs);
        bundle.putLong("duration", value.durationMs);
        bundle.putString("phase", value.phase.name());
        bundle.putString("pauseReason", value.pauseReason.name());
        bundle.putString("stopReason", value.stopReason.name());
        return bundle;
    }

    static int toMedia3RepeatMode(int mode) {
        return mode == 1 ? Player.REPEAT_MODE_ONE
                : mode == 2 ? Player.REPEAT_MODE_ALL : Player.REPEAT_MODE_OFF;
    }

    static int fromMedia3RepeatMode(int mode) {
        return mode == Player.REPEAT_MODE_ONE ? 1
                : mode == Player.REPEAT_MODE_ALL ? 2 : 0;
    }

    private static int safeInt(long value) {
        return value == C.TIME_UNSET || value < 0L
                ? 0 : (int) Math.min(Integer.MAX_VALUE, value);
    }

    private final class PlayerEvents implements Player.Listener {
        @Override
        public void onIsPlayingChanged(boolean isPlaying) {
            handler.removeCallbacks(positionSaver);
            if (isPlaying) {
                pauseReason = PauseReason.NONE;
                stopReason = StopReason.NONE;
                lastError = null;
                errorRecovery.resetConsecutiveErrors();
                transitionPolicy.onUserPlay();
                prefetchLoudness();
                handler.postDelayed(positionSaver, POSITION_SAVE_INTERVAL_MS);
            }
            persistState(true);
        }

        @Override
        public void onPlayWhenReadyChanged(boolean playWhenReady, int reason) {
            if (!playWhenReady) {
                if (reason == Player.PLAY_WHEN_READY_CHANGE_REASON_AUDIO_FOCUS_LOSS) {
                    pauseReason = transitionPolicy.onTemporaryAudioFocusLoss(true);
                } else if (reason == Player.PLAY_WHEN_READY_CHANGE_REASON_AUDIO_BECOMING_NOISY) {
                    pauseReason = transitionPolicy.onAudioBecomingNoisy();
                } else if (pauseReason == PauseReason.NONE) {
                    pauseReason = PauseReason.USER;
                    transitionPolicy.onUserPause();
                }
            }
            persistState(false);
        }

        @Override
        public void onPlaybackStateChanged(int state) {
            if (state == Player.STATE_READY) {
                errorRecovery.resetConsecutiveErrors();
                TrackStore.updateDuration(Media3PlayerService.this, currentUri(),
                        safeInt(player.getDuration()));
            } else if (state == Player.STATE_ENDED
                    && player.getRepeatMode() == Player.REPEAT_MODE_OFF) {
                stopReason = StopReason.QUEUE_ENDED;
            }
            persistState(true);
        }

        @Override
        public void onMediaItemTransition(@Nullable MediaItem mediaItem, int reason) {
            audioEffects.release();
            errorRecovery.resetConsecutiveErrors();
            lastError = null;
            persistState(true);
        }

        @Override
        public void onPlayerError(PlaybackException error) {
            recoverFromError(error);
        }

        @Override
        public void onAudioSessionIdChanged(int id) {
            audioSessionId = id;
            applyAudioEffects();
        }
    }

    private final class SessionCallback implements MediaSession.Callback {
        @Override
        public MediaSession.ConnectionResult onConnect(MediaSession session,
                MediaSession.ControllerInfo controller) {
            SessionCommands commands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS
                    .buildUpon()
                    .add(Media3Commands.TIMER_START_COMMAND)
                    .add(Media3Commands.TIMER_CANCEL_COMMAND)
                    .add(Media3Commands.AUDIO_EFFECTS_COMMAND)
                    .add(Media3Commands.CLEAR_QUEUE_COMMAND)
                    .add(Media3Commands.DIAGNOSTIC_SNAPSHOT_COMMAND)
                    .build();
            return new MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                    .setAvailableSessionCommands(commands)
                    .build();
        }

        @Override
        public ListenableFuture<SessionResult> onCustomCommand(MediaSession session,
                MediaSession.ControllerInfo controller, SessionCommand command, Bundle args) {
            String action = command.customAction;
            if (Media3Commands.TIMER_START.equals(action)) {
                sleepTimer.start(args.getLong(Media3Commands.ARG_TIMER_MS, 0L));
                return success();
            }
            if (Media3Commands.TIMER_CANCEL.equals(action)) {
                sleepTimer.cancel();
                return success();
            }
            if (Media3Commands.AUDIO_EFFECTS.equals(action)) {
                applyAudioEffects();
                return success();
            }
            if (Media3Commands.CLEAR_QUEUE.equals(action)) {
                stopReason = StopReason.USER;
                player.stop();
                player.clearMediaItems();
                stateManager.clear();
                return success();
            }
            if (Media3Commands.DIAGNOSTIC_SNAPSHOT.equals(action)) {
                return Futures.immediateFuture(new SessionResult(
                        SessionResult.RESULT_SUCCESS, snapshotBundle()));
            }
            return Futures.immediateFuture(
                    new SessionResult(SessionError.ERROR_NOT_SUPPORTED));
        }

        private ListenableFuture<SessionResult> success() {
            return Futures.immediateFuture(new SessionResult(SessionResult.RESULT_SUCCESS));
        }
    }
}
