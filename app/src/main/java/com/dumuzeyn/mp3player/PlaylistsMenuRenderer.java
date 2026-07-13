package com.dumuzeyn.mp3player;

import android.graphics.Color;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;

final class PlaylistsMenuRenderer implements MenuRenderer {
    private final MainActivityCore host;

    PlaylistsMenuRenderer(MainActivityCore host) {
        this.host = host;
    }

    void loadPlaylists() {
        LibraryDatabase database = new LibraryDatabase(host);
        try {
            host.playlists.clear();
            host.playlists.addAll(database.loadPlaylists());
        } finally {
            database.close();
        }
    }

    @Override
    public boolean needsMiniSpacer() {
        return true;
    }

    @Override
    public void render() {
        ArrayList<Playlist> playlists = host.playlistController.filteredPlaylists(host.search);
        if (playlists.isEmpty()) {
            TextView empty = host.text(host.tr3("No playlists yet", "Плейлистов пока нет", "∅ ▤"), 18, true);
            empty.setPadding(host.dp(12), host.dp(24), host.dp(12), host.dp(24));
            host.list.addView(empty);
            return;
        }
        int limit = host.renderingTabPreview ? Math.min(3, playlists.size()) : playlists.size();
        for (int index = 0; index < limit; index++) {
            Playlist playlist = playlists.get(index);
            host.list.addView(host.spaced(playlistCard(playlist)));
        }
    }

    private View playlistCard(final Playlist playlist) {
        final ArrayList<Track> tracks = host.playlistController.sortedPlaylistTracks(playlist);
        LinearLayout card = new LinearLayout(host);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(host.dp(10), host.dp(8), host.dp(10), host.dp(8));
        host.setSurface(card, host.panel, false);

        View marker = new View(host);
        marker.setBackgroundColor(host.yellow);
        marker.setVisibility(host.isCurrentCollection(tracks) ? View.VISIBLE : View.INVISIBLE);

        LinearLayout header = host.row();
        LinearLayout titleColumn = new LinearLayout(host);
        titleColumn.setOrientation(LinearLayout.VERTICAL);
        TextView title = host.text(playlist.name, 20, true);
        host.makeMarquee(title);
        TextView count = host.text(playlist.uris.size() + " " + host.tr3("songs", "песен", "♪"), 13, false);
        titleColumn.addView(title);
        titleColumn.addView(count);
        header.addView(titleColumn, new LinearLayout.LayoutParams(0, -2, 1.0f));

        Button delete = host.icon("×");
        host.applyPlainIconStyle(delete, Color.rgb(190, 45, 45));
        delete.setOnClickListener(view -> host.confirmDeletePlaylist(playlist));
        header.addView(delete, host.square(44));

        Button rename = host.icon("✎");
        host.applyPlainIconStyle(rename);
        rename.setOnClickListener(view -> host.renamePlaylistDialog(playlist));
        header.addView(rename, host.square(44));

        Button play = host.icon(host.isPlayingCollection(tracks) ? "Ⅱ" : "▶");
        host.applyPlainIconStyle(play, host.purple);
        SongRowStateRegistry.applyPlayState(play, host.isPlayingCollection(tracks));
        play.setOnClickListener(view -> {
            if (host.isCurrentCollection(tracks)) {
                host.toggleCurrent();
            } else {
                host.playList(tracks, false);
            }
            updatePlaybackState(play, marker, tracks);
        });
        header.addView(play, host.square(44));

        Button shuffle = host.shuffleButton();
        host.applyPlainIconStyle(shuffle);
        shuffle.setOnClickListener(view -> {
            host.playList(tracks, true);
            updatePlaybackState(play, marker, tracks);
        });
        header.addView(shuffle, host.square(44));
        card.addView(header);

        LinearLayout body = host.row();
        FrameLayoutCover cover = new FrameLayoutCover(host);
        int fallback = host.dark ? 28 : 235;
        cover.setFallback(Color.rgb(fallback, fallback, fallback));
        body.addView(cover, host.square(72));

        SmoothPlaylistTicker ticker = new SmoothPlaylistTicker(host);
        ticker.setPadding(host.dp(12), 0, 0, 0);
        body.addView(ticker, new LinearLayout.LayoutParams(0, -2, 1.0f));
        card.addView(body);
        host.playlistController.bindRollingPreview(ticker, cover, tracks, host.songRenderGeneration);

        card.setOnClickListener(view -> host.openPlaylist(playlist));

        FrameLayout container = new FrameLayout(host);
        container.addView(card, new FrameLayout.LayoutParams(-1, -2));
        FrameLayout.LayoutParams markerParams = new FrameLayout.LayoutParams(host.dp(4), -1);
        markerParams.gravity = android.view.Gravity.START;
        markerParams.setMargins(host.dp(2), host.dp(10), 0, host.dp(10));
        container.addView(marker, markerParams);
        return container;
    }

    private void updatePlaybackState(Button play, View marker, ArrayList<Track> tracks) {
        marker.setVisibility(host.isCurrentCollection(tracks) ? View.VISIBLE : View.INVISIBLE);
        SongRowStateRegistry.applyPlayState(play, host.isPlayingCollection(tracks));
    }
}
