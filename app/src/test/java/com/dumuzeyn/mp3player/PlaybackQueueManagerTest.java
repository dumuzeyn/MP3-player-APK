package com.dumuzeyn.mp3player;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import org.junit.Test;

public class PlaybackQueueManagerTest {
    @Test
    public void rebuildPreservesOrderAndSkipsRemovedTracks() {
        ArrayList<Track> library = new ArrayList<>();
        library.add(new Track("uri:first", "First", "Artist"));
        library.add(new Track("uri:second", "Second", "Artist"));
        PlaybackQueueManager manager = new PlaybackQueueManager();

        manager.rebuild(library, Arrays.asList("uri:second", "uri:missing", "uri:first"));

        assertEquals(2, manager.size());
        assertEquals("uri:second", manager.get(0).uri);
        assertEquals("uri:first", manager.get(1).uri);
    }

    @Test
    public void normalizeIndexClampsToAvailableQueue() {
        PlaybackQueueManager manager = new PlaybackQueueManager();
        assertEquals(-1, manager.normalizeIndex(4));

        manager.replace(Arrays.asList(
                new Track("uri:first", "First", "Artist"),
                new Track("uri:second", "Second", "Artist")));

        assertEquals(0, manager.normalizeIndex(-5));
        assertEquals(1, manager.normalizeIndex(8));
        assertTrue(manager.indexOfUri("uri:missing") < 0);
    }
}
