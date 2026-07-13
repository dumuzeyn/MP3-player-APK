package com.dumuzeyn.mp3player;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.os.Build;
import android.util.Log;

final class AudioFocusController {
    interface Callback {
        boolean isPlaying();

        boolean isPausedByUser();

        void pauseForInterruption(String reason);

        void pauseForDisconnectedOutput();

        void resumeAfterInterruption();

        void setPlayerVolume(float volume);
    }

    private static final String TAG = "MP3PlayerService";

    private final Context context;
    private final Callback callback;
    private final AudioManager audioManager;
    private final BroadcastReceiver noisyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context receiverContext, Intent intent) {
            if (!AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
                return;
            }
            if (uninterruptedPlaybackEnabled()) {
                Log.i(TAG, "audio_becoming_noisy_ignored");
                return;
            }
            resumeAfterTransientLoss = false;
            callback.pauseForDisconnectedOutput();
        }
    };
    private final AudioManager.OnAudioFocusChangeListener focusChangeListener =
            new AudioManager.OnAudioFocusChangeListener() {
                @Override
                public void onAudioFocusChange(int focusChange) {
                    handleFocusChange(focusChange);
                }
            };

    private AudioFocusRequest focusRequest;
    private boolean hasAudioFocus;
    private boolean receiverRegistered;
    private boolean resumeAfterTransientLoss;
    private boolean ducked;

    AudioFocusController(Context context, Callback callback) {
        this.context = context.getApplicationContext();
        this.callback = callback;
        this.audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
    }

    boolean requestFocus() {
        if (uninterruptedPlaybackEnabled() || hasAudioFocus || audioManager == null) {
            return true;
        }
        int result;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (focusRequest == null) {
                focusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                        .setAudioAttributes(new AudioAttributes.Builder()
                                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                .setUsage(AudioAttributes.USAGE_MEDIA)
                                .build())
                        .setOnAudioFocusChangeListener(focusChangeListener)
                        .build();
            }
            result = audioManager.requestAudioFocus(focusRequest);
        } else {
            result = audioManager.requestAudioFocus(
                    focusChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        }
        hasAudioFocus = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
        return hasAudioFocus;
    }

    void updateNoisyReceiver(boolean playing) {
        if (playing) {
            registerNoisyReceiver();
        } else {
            unregisterNoisyReceiver();
        }
    }

    void applyPlaybackVolume() {
        callback.setPlayerVolume(ducked ? 0.35f : 1.0f);
    }

    void resetInterruptionState() {
        resumeAfterTransientLoss = false;
        ducked = false;
    }

    void onUserPause() {
        resumeAfterTransientLoss = false;
    }

    void stop() {
        unregisterNoisyReceiver();
        abandonFocus();
        resetInterruptionState();
    }

    private void handleFocusChange(int focusChange) {
        if (focusChange == AudioManager.AUDIOFOCUS_LOSS
                || focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
            hasAudioFocus = false;
        } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
            hasAudioFocus = true;
        }
        if (uninterruptedPlaybackEnabled()) {
            Log.i(TAG, "audio_focus_change_ignored value=" + focusChange);
            restoreFullVolume();
            return;
        }
        if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
            Log.i(TAG, "audio_focus_loss");
            resumeAfterTransientLoss = false;
            ducked = false;
            callback.pauseForInterruption("focus_loss");
        } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
            Log.i(TAG, "audio_focus_loss_transient");
            resumeAfterTransientLoss = callback.isPlaying() && !callback.isPausedByUser();
            ducked = false;
            callback.pauseForInterruption("focus_loss_transient");
        } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
            Log.i(TAG, "audio_focus_duck");
            if (stableVolumeEnabled()) {
                restoreFullVolume();
            } else {
                callback.setPlayerVolume(0.35f);
                ducked = true;
            }
        } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
            Log.i(TAG, "audio_focus_gain");
            restoreFullVolume();
            if (resumeAfterTransientLoss && !callback.isPausedByUser()) {
                resumeAfterTransientLoss = false;
                callback.resumeAfterInterruption();
            }
        }
    }

    private boolean uninterruptedPlaybackEnabled() {
        SharedPreferences preferences = context.getSharedPreferences(
                UninterruptedPlaybackController.PREFS, Context.MODE_PRIVATE);
        return preferences.getBoolean(UninterruptedPlaybackController.ENABLED, false);
    }

    private boolean stableVolumeEnabled() {
        SharedPreferences preferences = context.getSharedPreferences(
                UninterruptedPlaybackController.PREFS, Context.MODE_PRIVATE);
        return preferences.getBoolean(StableVolumeController.ENABLED, false);
    }

    private void restoreFullVolume() {
        callback.setPlayerVolume(1.0f);
        ducked = false;
    }

    private void abandonFocus() {
        if (audioManager == null) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && focusRequest != null) {
            audioManager.abandonAudioFocusRequest(focusRequest);
        } else {
            audioManager.abandonAudioFocus(focusChangeListener);
        }
        hasAudioFocus = false;
    }

    private void registerNoisyReceiver() {
        if (receiverRegistered) {
            return;
        }
        context.registerReceiver(
                noisyReceiver, new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY));
        receiverRegistered = true;
    }

    private void unregisterNoisyReceiver() {
        if (!receiverRegistered) {
            return;
        }
        try {
            context.unregisterReceiver(noisyReceiver);
        } catch (RuntimeException ignored) {
        }
        receiverRegistered = false;
    }
}
