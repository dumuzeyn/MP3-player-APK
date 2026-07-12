package com.dumuzeyn.mp3player;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import org.junit.Test;

public class PlaylistManagerTest {
    @Test
    public void cleanNameRemovesControlCharactersAndLimitsLength() {
        String dirty = "  My\nPlaylist\t\u0001Name  ";

        assertEquals("My Playlist Name", PlaylistManager.cleanName(dirty));
    }

    @Test
    public void playlistsRoundTripThroughJson() {
        ArrayList<Playlist> playlists = new ArrayList<>();
        Playlist playlist = new Playlist("Road");
        playlist.uris.add("content://one");
        playlist.uris.add("content://two");
        playlists.add(playlist);

        ArrayList<Playlist> restored = PlaylistManager.fromJson(PlaylistManager.toJson(playlists));

        assertEquals(1, restored.size());
        assertEquals("Road", restored.get(0).name);
        assertEquals("content://one", restored.get(0).uris.get(0));
        assertEquals("content://two", restored.get(0).uris.get(1));
    }

    @Test
    public void fromJsonReturnsEmptyListForInvalidData() {
        ArrayList<Playlist> restored = PlaylistManager.fromJson("{broken");

        assertEquals(0, restored.size());
    }
}
