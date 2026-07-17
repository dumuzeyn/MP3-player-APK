package com.dumuzeyn.mp3player.playback.service;

/** Tracks bounded playback retries independently from MediaPlayer lifecycle code. */
public final class PlaybackErrorRecovery {
    private int consecutiveErrors;
    private int repeatOneRetries;

    public int recordError() {
        return ++consecutiveErrors;
    }

    public int consecutiveErrors() {
        return consecutiveErrors;
    }

    public boolean exhausted(int queueSize) {
        return queueSize <= 0 || consecutiveErrors >= queueSize;
    }

    public void resetConsecutiveErrors() {
        consecutiveErrors = 0;
    }

    public int repeatOneRetries() {
        return repeatOneRetries;
    }

    public void setRepeatOneRetries(int retries) {
        repeatOneRetries = Math.max(0, retries);
    }

    public void resetRepeatOneRetries() {
        repeatOneRetries = 0;
    }
}
