package com.dumuzeyn.mp3player.playback.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PlaybackErrorRecoveryTest {
    @Test
    public void stopsAfterEveryQueuedTrackFails() {
        PlaybackErrorRecovery recovery = new PlaybackErrorRecovery();

        assertEquals(1, recovery.recordError());
        assertFalse(recovery.exhausted(2));
        assertEquals(2, recovery.recordError());
        assertTrue(recovery.exhausted(2));
    }

    @Test
    public void successfulPreparationResetsConsecutiveFailures() {
        PlaybackErrorRecovery recovery = new PlaybackErrorRecovery();
        recovery.recordError();
        recovery.recordError();

        recovery.resetConsecutiveErrors();

        assertEquals(0, recovery.consecutiveErrors());
        assertFalse(recovery.exhausted(3));
    }

    @Test
    public void repeatOneRetriesCannotBecomeNegative() {
        PlaybackErrorRecovery recovery = new PlaybackErrorRecovery();

        recovery.setRepeatOneRetries(-1);

        assertEquals(0, recovery.repeatOneRetries());
    }
}
