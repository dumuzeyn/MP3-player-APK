package com.dumuzeyn.mp3player;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.media.audiofx.AudioEffect;
import android.media.audiofx.DynamicsProcessing;
import android.media.audiofx.Equalizer;
import android.media.audiofx.LoudnessEnhancer;
import android.os.Build;
import android.util.Log;

final class AudioEffectsManager {
    private static final String DEBUG_TAG = "VoltuneDebug";

    private final Context context;
    private Equalizer equalizer;
    private AudioEffect dynamicsProcessing;
    private LoudnessEnhancer loudnessEnhancer;
    private boolean customDynamicsUnsupported;

    AudioEffectsManager(Context context) {
        this.context = context.getApplicationContext();
    }

    void apply(MediaPlayer player) {
        release();
        if (player == null) {
            return;
        }
        SharedPreferences preferences = context.getSharedPreferences(EqualizerController.PREFS, 0);
        int audioSessionId;
        try {
            audioSessionId = player.getAudioSessionId();
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

    void release() {
        if (equalizer != null) {
            releaseEffect(equalizer);
            equalizer = null;
        }
        if (dynamicsProcessing != null) {
            releaseEffect(dynamicsProcessing);
            dynamicsProcessing = null;
        }
        if (loudnessEnhancer != null) {
            releaseEffect(loudnessEnhancer);
            loudnessEnhancer = null;
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
            equalizer = effect;
            Log.i(DEBUG_TAG, "equalizer_applied session=" + audioSessionId + " bands=" + bandCount);
        } catch (RuntimeException error) {
            Log.w(DEBUG_TAG, "equalizer_unavailable session=" + audioSessionId + " error=" + error.getMessage());
        }
    }

    private void applyVolumeLeveling(int audioSessionId) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            applyLoudnessFallback(audioSessionId);
            return;
        }
        if (customDynamicsUnsupported) {
            applyCompatibleVolumeLeveling(audioSessionId);
            return;
        }
        try {
            dynamicsProcessing = Api28VolumeLeveling.createCustom(audioSessionId);
            Log.i(DEBUG_TAG, "volume_leveling_applied session=" + audioSessionId);
        } catch (RuntimeException customConfigError) {
            customDynamicsUnsupported = true;
            Log.w(DEBUG_TAG, "volume_leveling_custom_config_failed session=" + audioSessionId
                    + " error=" + customConfigError.getMessage());
            applyCompatibleVolumeLeveling(audioSessionId);
        }
    }

    @TargetApi(Build.VERSION_CODES.P)
    private void applyCompatibleVolumeLeveling(int audioSessionId) {
        try {
            DynamicsProcessing effect = Api28VolumeLeveling.createCompatible(audioSessionId);
            dynamicsProcessing = effect;
            Log.i(DEBUG_TAG, "volume_leveling_applied_compatible session=" + audioSessionId
                    + " channels=" + effect.getChannelCount());
        } catch (RuntimeException defaultConfigError) {
            Log.w(DEBUG_TAG, "volume_leveling_default_config_failed session=" + audioSessionId
                    + " error=" + defaultConfigError.getMessage());
            applyLoudnessFallback(audioSessionId);
        }
    }

    private void applyLoudnessFallback(int audioSessionId) {
        try {
            LoudnessEnhancer effect = new LoudnessEnhancer(audioSessionId);
            effect.setTargetGain(150);
            effect.setEnabled(true);
            loudnessEnhancer = effect;
            Log.i(DEBUG_TAG, "volume_leveling_loudness_fallback session=" + audioSessionId);
        } catch (RuntimeException error) {
            Log.w(DEBUG_TAG, "volume_leveling_unavailable session=" + audioSessionId + " error=" + error.getMessage());
        }
    }

    private void releaseEffect(AudioEffect effect) {
        try {
            effect.release();
        } catch (RuntimeException ignored) {
        }
    }

    @TargetApi(Build.VERSION_CODES.P)
    private static final class Api28VolumeLeveling {
        private Api28VolumeLeveling() {
        }

        static DynamicsProcessing createCustom(int audioSessionId) {
            // A fast compressor audibly changes gain inside one song. Keep only a
            // slow peak guard so the effect cannot "pump" between quiet and loud parts.
            DynamicsProcessing.Limiter limiter = new DynamicsProcessing.Limiter(
                    true, true, 0, 10.0f, 1500.0f, 10.0f, -1.0f, 0.0f);
            DynamicsProcessing.Config config = new DynamicsProcessing.Config.Builder(
                    DynamicsProcessing.VARIANT_FAVOR_TIME_RESOLUTION,
                    2, false, 0, false, 0, false, 0, true)
                    .setLimiterAllChannelsTo(limiter)
                    .build();
            DynamicsProcessing effect = new DynamicsProcessing(0, audioSessionId, config);
            effect.setEnabled(true);
            return effect;
        }

        static DynamicsProcessing createCompatible(int audioSessionId) {
            DynamicsProcessing effect = new DynamicsProcessing(0, audioSessionId, null);
            for (int channel = 0; channel < effect.getChannelCount(); channel++) {
                DynamicsProcessing.Mbc compressor = effect.getMbcByChannelIndex(channel);
                for (int band = 0; band < compressor.getBandCount(); band++) {
                    DynamicsProcessing.MbcBand settings = compressor.getBand(band);
                    settings.setEnabled(false);
                    effect.setMbcBandByChannelIndex(channel, band, settings);
                }
                DynamicsProcessing.Limiter limiter = effect.getLimiterByChannelIndex(channel);
                limiter.setEnabled(true);
                limiter.setAttackTime(10.0f);
                limiter.setReleaseTime(1500.0f);
                limiter.setRatio(10.0f);
                limiter.setThreshold(-1.0f);
                effect.setLimiterByChannelIndex(channel, limiter);
            }
            effect.setEnabled(true);
            return effect;
        }
    }
}
