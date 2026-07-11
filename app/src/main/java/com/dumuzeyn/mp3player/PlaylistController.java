package com.dumuzeyn.mp3player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Set;
import android.graphics.Color;
import android.widget.ImageView;
import android.widget.TextView;

final class PlaylistController {
    private final MainActivityCore host;

    PlaylistController(MainActivityCore host) {
        this.host = host;
    }

    ArrayList<MainActivityCore.Playlist> filteredPlaylists(String query) {
        ArrayList<MainActivityCore.Playlist> result = new ArrayList<>();
        String normalized = query == null ? "" : query.trim().toLowerCase(java.util.Locale.ROOT);
        for (MainActivityCore.Playlist playlist : host.playlists) {
            if (normalized.isEmpty()
                    || host.containsSearch(playlist.name, normalized)
                    || playlistContainsSearch(playlist, normalized)) {
                result.add(playlist);
            }
        }
        return result;
    }

    ArrayList<Track> playlistTracks(MainActivityCore.Playlist playlist) {
        ArrayList<Track> result = new ArrayList<>();
        for (String uri : playlist.uris) {
            Track track = host.findTrack(uri);
            if (track != null) {
                result.add(track);
            }
        }
        return result;
    }

    ArrayList<Track> sortedPlaylistTracks(MainActivityCore.Playlist playlist) {
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

    void bindRollingPreview(final TextView target, final ImageView cover, final ArrayList<Track> sortedTracks, final int generation) {
        if (sortedTracks.isEmpty()) {
            target.setText(host.tr3("No songs in this playlist yet.", "В плейлисте пока нет песен.", "∅ ♪"));
            return;
        }
        target.setSingleLine(false);
        target.setEllipsize(null);
        target.setSelected(false);
        bindPreviewFrame(target, cover, sortedTracks, 0);
        if (sortedTracks.size() <= 1) {
            return;
        }
        host.uiHandler.postDelayed(new Runnable() {
            private int index = 1;

            @Override
            public void run() {
                if (generation != host.songRenderGeneration || target.getParent() == null) {
                    return;
                }
                bindPreviewFrame(target, cover, sortedTracks, index);
                index = (index + 1) % sortedTracks.size();
                host.uiHandler.postDelayed(this, 6200L);
            }
        }, 6200L);
    }

    private void bindPreviewFrame(TextView target, ImageView cover, ArrayList<Track> sortedTracks, int startIndex) {
        StringBuilder text = new StringBuilder();
        int visibleCount = Math.min(3, sortedTracks.size());
        for (int offset = 0; offset < visibleCount; offset++) {
            Track track = sortedTracks.get((startIndex + offset) % sortedTracks.size());
            if (offset > 0) {
                text.append("\n");
            }
            text.append(track.title);
        }
        target.setText(text.toString());
        Track coverTrack = sortedTracks.get(startIndex % sortedTracks.size());
        int fallback = host.dark ? 28 : 235;
        host.loadCover(cover, coverTrack, Color.rgb(fallback, fallback, fallback));
    }

    boolean playlistContainsSearch(MainActivityCore.Playlist playlist, String query) {
        for (Track track : playlistTracks(playlist)) {
            if (host.matchesTrackSearch(track, query)) {
                return true;
            }
        }
        return false;
    }

    void addTracksToPlaylist(MainActivityCore.Playlist playlist, Set<String> uris) {
        for (String uri : uris) {
            if (!playlist.uris.contains(uri)) {
                playlist.uris.add(uri);
            }
        }
        host.saveState();
    }

    void addTrackToPlaylist(MainActivityCore.Playlist playlist, Track track) {
        if (!playlist.uris.contains(track.uri)) {
            playlist.uris.add(track.uri);
        }
        host.saveState();
    }

    MainActivityCore.Playlist createPlaylist(String rawName) {
        String name = cleanPlaylistName(rawName);
        MainActivityCore.Playlist playlist = new MainActivityCore.Playlist(name);
        host.playlists.add(playlist);
        host.saveState();
        return playlist;
    }

    MainActivityCore.Playlist createPlaylistWithTrack(String rawName, Track track) {
        MainActivityCore.Playlist playlist = createPlaylist(rawName);
        if (!playlist.uris.contains(track.uri)) {
            playlist.uris.add(track.uri);
            host.saveState();
        }
        return playlist;
    }

    void renamePlaylist(MainActivityCore.Playlist playlist, String rawName) {
        playlist.name = cleanPlaylistName(rawName);
        host.saveState();
    }

    void deletePlaylist(MainActivityCore.Playlist playlist) {
        host.playlists.remove(playlist);
        host.saveState();
    }

    void removeTrackFromAllPlaylists(Track track) {
        for (MainActivityCore.Playlist playlist : host.playlists) {
            playlist.uris.remove(track.uri);
        }
        host.saveState();
    }

    private String cleanPlaylistName(String rawName) {
        String name = PlaylistManager.cleanName(rawName);
        return name.isEmpty() ? host.tr("Playlist", "Плейлист") : name;
    }
}
