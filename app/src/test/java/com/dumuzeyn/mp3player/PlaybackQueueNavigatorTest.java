package com.dumuzeyn.mp3player;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PlaybackQueueNavigatorTest {
    @Test
    public void finishedTrackAdvancesToNext() {
        assertPlay(decide(3, 0, 0, false, PlaybackQueueNavigator.Reason.FINISHED, 0), 1, 0);
    }

    @Test
    public void playlistContinuesThroughEveryTrackWithoutOneShot() {
        assertPlay(decide(3, 0, 0, false, PlaybackQueueNavigator.Reason.FINISHED, 0), 1, 0);
        assertPlay(decide(3, 1, 0, false, PlaybackQueueNavigator.Reason.FINISHED, 0), 2, 0);
        assertTrue(decide(3, 2, 0, false, PlaybackQueueNavigator.Reason.FINISHED, 0).stop);
    }

    @Test
    public void noRepeatStopsAtQueueEnd() {
        assertTrue(decide(3, 2, 0, false, PlaybackQueueNavigator.Reason.FINISHED, 0).stop);
    }

    @Test
    public void repeatOneReplaysCurrentTrack() {
        assertPlay(decide(3, 1, 1, false, PlaybackQueueNavigator.Reason.FINISHED, 0), 1, 0);
    }

    @Test
    public void repeatAllWrapsToFirstTrack() {
        assertPlay(decide(3, 2, 2, false, PlaybackQueueNavigator.Reason.FINISHED, 0), 0, 0);
    }

    @Test
    public void manualPreviousWrapsToQueueEnd() {
        assertPlay(decide(3, 0, 0, false, PlaybackQueueNavigator.Reason.MANUAL_PREVIOUS, 0), 2, 0);
    }

    @Test
    public void oneShotStopsAfterCurrentTrack() {
        assertTrue(decide(3, 0, 2, true, PlaybackQueueNavigator.Reason.FINISHED, 0).stop);
    }

    @Test
    public void repeatOneRetriesErrorOnlyOnce() {
        PlaybackQueueNavigator.Decision retry = decide(3, 1, 1, false,
                PlaybackQueueNavigator.Reason.ERROR, 0);
        PlaybackQueueNavigator.Decision skip = decide(3, 1, 1, false,
                PlaybackQueueNavigator.Reason.ERROR, retry.loopOneErrorRetries);

        assertPlay(retry, 1, 1);
        assertPlay(skip, 2, 0);
    }

    private PlaybackQueueNavigator.Decision decide(int queueSize, int currentIndex, int loopMode,
            boolean oneShot, PlaybackQueueNavigator.Reason reason, int retries) {
        return PlaybackQueueNavigator.decide(queueSize, currentIndex, loopMode, oneShot, reason, retries);
    }

    private void assertPlay(PlaybackQueueNavigator.Decision decision, int index, int retries) {
        assertFalse(decision.stop);
        assertEquals(index, decision.nextIndex);
        assertEquals(retries, decision.loopOneErrorRetries);
    }
}
