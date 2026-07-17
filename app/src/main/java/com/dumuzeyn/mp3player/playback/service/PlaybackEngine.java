package com.dumuzeyn.mp3player.playback.service;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.PowerManager;
import android.util.Log;
import com.dumuzeyn.mp3player.Track;

/** Owns low-level MediaPlayer setup, data sources, state reads, and release. */
public final class PlaybackEngine {
    private static final String TAG = "VoltuneService";

    private PlaybackEngine() {
    }

    public static String configure(Context context, MediaPlayer player, Track track)
            throws Exception {
        player.setAudioAttributes(new AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .build());
        player.setWakeMode(context.getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        return setDataSource(context, player, track);
    }

    public static int duration(MediaPlayer player, boolean preparing, int fallback) {
        if (player == null || preparing) {
            return Math.max(0, fallback);
        }
        try {
            int duration = player.getDuration();
            return duration > 0 ? duration : Math.max(0, fallback);
        } catch (RuntimeException ignored) {
            return Math.max(0, fallback);
        }
    }

    public static int position(MediaPlayer player, boolean preparing, int fallback) {
        if (player == null) {
            return 0;
        }
        if (preparing) {
            return 0;
        }
        try {
            return player.getCurrentPosition();
        } catch (RuntimeException ignored) {
            return Math.max(0, fallback);
        }
    }

    public static boolean isPlaying(MediaPlayer player, boolean preparing) {
        if (player == null || preparing) {
            return false;
        }
        try {
            return player.isPlaying();
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    public static void release(MediaPlayer player) {
        if (player == null) {
            return;
        }
        try {
            player.setOnPreparedListener(null);
            player.setOnCompletionListener(null);
            player.setOnErrorListener(null);
            player.stop();
        } catch (RuntimeException ignored) {
        }
        try {
            player.release();
        } catch (RuntimeException error) {
            Log.e(TAG, "release_failed", error);
        }
    }

    private static String setDataSource(Context context, MediaPlayer player, Track track)
            throws Exception {
        try {
            player.setDataSource(context, track.asUri());
            return "context_uri";
        } catch (Exception directError) {
            Log.w(TAG, "set_data_source_direct_failed uri=" + track.uri
                    + " error=" + directError.getMessage());
        }
        try (AssetFileDescriptor descriptor = context.getContentResolver()
                .openAssetFileDescriptor(track.asUri(), "r")) {
            if (descriptor == null) {
                throw new IllegalStateException("openAssetFileDescriptor returned null");
            }
            long declaredLength = descriptor.getDeclaredLength();
            if (declaredLength >= 0) {
                player.setDataSource(descriptor.getFileDescriptor(), descriptor.getStartOffset(),
                        declaredLength);
                return "asset_fd_range";
            }
            player.setDataSource(descriptor.getFileDescriptor());
            return "asset_fd";
        }
    }
}
