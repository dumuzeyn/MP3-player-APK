package com.dumuzeyn.mp3player;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import org.junit.Test;

public class TrackStoreTest {
    @Test
    public void sortOrdersTracksByTitleIgnoringCase() {
        ArrayList<Track> tracks = new ArrayList<>();
        tracks.add(new Track("uri:2", "beta", "artist"));
        tracks.add(new Track("uri:1", "Alpha", "artist"));
        tracks.add(new Track("uri:3", "gamma", "artist"));

        TrackStore.sort(tracks);

        assertEquals("Alpha", tracks.get(0).title);
        assertEquals("beta", tracks.get(1).title);
        assertEquals("gamma", tracks.get(2).title);
    }

    @Test
    public void loadFromJsonKeepsDurationAndMetadata() {
        ArrayList<Track> tracks = TrackStore.loadFromJson("[{\"uri\":\"content://song\",\"title\":\"Song\",\"artist\":\"Artist\",\"album\":\"Album\",\"genre\":\"Genre\",\"durationMs\":12345}]");

        assertEquals(1, tracks.size());
        assertEquals("content://song", tracks.get(0).uri);
        assertEquals("Song", tracks.get(0).title);
        assertEquals("Artist", tracks.get(0).artist);
        assertEquals("Album", tracks.get(0).album);
        assertEquals("Genre", tracks.get(0).genre);
        assertEquals(12345, tracks.get(0).durationMs);
    }
}
