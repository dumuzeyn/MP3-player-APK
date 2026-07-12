package com.dumuzeyn.mp3player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Set;

final class PlaylistController {
    private final MainActivityCore host;

    PlaylistController(MainActivityCore host) {
        this.host = host;
    }

    ArrayList<Playlist> filteredPlaylists(String query) {
        ArrayList<Playlist> result = new ArrayList<>();
        String normalized = query == null ? "" : query.trim().toLowerCase(java.util.Locale.ROOT);
        for (Playlist playlist : host.playlists) {
            if (normalized.isEmpty()
                    || host.containsSearch(playlist.name, normalized)
                    || playlistContainsSearch(playlist, normalized)) {
                result.add(playlist);
            }
        }
        return result;
    }

    ArrayList<Track> playlistTracks(Playlist playlist) {
        ArrayList<Track> result = new ArrayList<>();
        for (String uri : playlist.uris) {
            Track track = host.findTrack(uri);
            if (track != null) {
                result.add(track);
            }
        }
        return result;
    }

    ArrayList<Track> sortedPlaylistTracks(Playlist playlist) {
        ArrayList<Track> result = playlistTracks(playlist);
        Collections.sort(result, new Comparator<Track>() {
            @Override
            public int compare(Track left, Track right) {
                String leftTitle = left == null || left.title == null ? "" : left.title;
                String rightTitle = right == null || right.title == null ? "" : right.title;
                return leftTitle.compareToIgnoreCase(rightTitle);
            }
        });
        return result;
    }

    void bindRollingPreview(final SmoothPlaylistTicker ticker, final FrameLayoutCover cover, final ArrayList<Track> sortedTracks, final int generation) {
        if (sortedTracks.isEmpty()) {
            ticker.bindTracks(sortedTracks);
            return;
        }
        ticker.bindTracks(sortedTracks);
        cover.bindTrack(sortedTracks.get(0), generation);
        if (sortedTracks.size() <= 1) {
            return;
        }
        host.uiHandler.postDelayed(new Runnable() {
            private int index = 1;

            @Override
            public void run() {
                if (generation != host.songRenderGeneration || !ticker.isAttachedToWindow()) {
                    return;
                }
                cover.bindTrack(sortedTracks.get(index), generation);
                index = (index + 1) % sortedTracks.size();
                host.uiHandler.postDelayed(this, 14500L);
            }
        }, 14500L);
    }

    boolean playlistContainsSearch(Playlist playlist, String query) {
        for (Track track : playlistTracks(playlist)) {
            if (host.matchesTrackSearch(track, query)) {
                return true;
            }
        }
        return false;
    }

    void addTracksToPlaylist(Playlist playlist, Set<String> uris) {
        for (String uri : uris) {
            if (!playlist.uris.contains(uri)) {
                playlist.uris.add(uri);
            }
        }
        host.saveState();
    }

    void addTrackToPlaylist(Playlist playlist, Track track) {
        if (!playlist.uris.contains(track.uri)) {
            playlist.uris.add(track.uri);
        }
        host.saveState();
    }

    Playlist createPlaylist(String rawName) {
        String name = cleanPlaylistName(rawName);
        Playlist playlist = new Playlist(name);
        host.playlists.add(playlist);
        host.saveState();
        return playlist;
    }

    Playlist createPlaylistWithTrack(String rawName, Track track) {
        Playlist playlist = createPlaylist(rawName);
        if (!playlist.uris.contains(track.uri)) {
            playlist.uris.add(track.uri);
            host.saveState();
        }
        return playlist;
    }

    void renamePlaylist(Playlist playlist, String rawName) {
        playlist.name = cleanPlaylistName(rawName);
        host.saveState();
    }

    void deletePlaylist(Playlist playlist) {
        host.playlists.remove(playlist);
        host.saveState();
    }

    void removeTrackFromAllPlaylists(Track track) {
        for (Playlist playlist : host.playlists) {
            playlist.uris.remove(track.uri);
        }
        host.saveState();
    }

    private String cleanPlaylistName(String rawName) {
        String name = PlaylistManager.cleanName(rawName);
        return name.isEmpty() ? host.tr("Playlist", "Плейлист") : name;
    }
}
