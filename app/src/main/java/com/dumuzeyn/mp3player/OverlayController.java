package com.dumuzeyn.mp3player;

import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

final class OverlayController {
    private interface SelectionDone {
        void done(Set<String> selected);
    }

    private final MainActivityCore host;

    OverlayController(MainActivityCore host) {
        this.host = host;
    }

    void openGroup(String title, ArrayList<Track> tracks) {
        openTrackPanel(title, tracks, null);
    }

    void openPlaylist(final Playlist playlist) {
        openTrackPanel(playlist.name, host.playlistController.playlistTracks(playlist), playlist);
    }

    private void openTrackPanel(String title, ArrayList<Track> tracks, Playlist playlist) {
        final FrameLayout shade = host.shade();
        LinearLayout panel = host.panelCard();
        LinearLayout header = host.row();
        header.addView(host.text(title, 20, true), new LinearLayout.LayoutParams(0, host.dp(58), 1.0f));
        Button play = host.icon(host.isPlayingSource(tracks) ? "Ⅱ" : "▶");
        play.setOnClickListener(view -> {
            if (host.isPlayingSource(tracks)) {
                host.toggleCurrent();
            } else {
                host.playList(tracks, false);
            }
        });
        header.addView(play, host.square(52));
        Button shuffle = host.shuffleButton();
        shuffle.setOnClickListener(view -> host.playList(tracks, true));
        header.addView(shuffle, host.square(52));
        if (playlist != null) {
            Button add = host.icon("+");
            add.setOnClickListener(view -> {
                host.overlayHost.removeView(shade);
                openAddToPlaylist(playlist);
            });
            header.addView(add, host.square(52));
        }
        Button close = host.icon("×");
        close.setOnClickListener(view -> close(shade));
        header.addView(close, host.square(52));
        panel.addView(header);

        ScrollView scroll = new ScrollView(host);
        LinearLayout rows = new LinearLayout(host);
        rows.setOrientation(LinearLayout.VERTICAL);
        for (Track track : tracks) {
            rows.addView(trackPanelRow(track, tracks, playlist, shade, title));
        }
        scroll.addView(rows);
        panel.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1.0f));
        shade.addView(panel, host.bottomParams());
        host.overlayHost.addView(shade);
        host.updateMini();
    }

    private View trackPanelRow(Track track, ArrayList<Track> source, Playlist playlist,
                               FrameLayout shade, String title) {
        LinearLayout container = new LinearLayout(host);
        container.setOrientation(LinearLayout.VERTICAL);
        container.addView(host.songsRenderer.songRow(track, true, true, () -> {
            host.overlayHost.removeView(shade);
            if (playlist == null) {
                openGroup(title, source);
            } else {
                openPlaylist(playlist);
            }
        }, () -> {
            host.overlayHost.removeView(shade);
            openSongActions(track, playlist);
        }));
        return container;
    }

    void openQueue() {
        if (host.playbackQueue.isEmpty() && host.currentIndex >= 0 && host.currentIndex < host.tracks.size()) {
            host.playbackQueue.add(host.tracks.get(host.currentIndex));
        }
        final FrameLayout shade = host.shade();
        LinearLayout panel = host.panelCard();
        LinearLayout header = host.row();
        header.addView(host.text(host.tr("Now playing", "Список проигрывания"), 20, true),
                new LinearLayout.LayoutParams(0, host.dp(58), 1.0f));
        Button add = host.icon("+");
        add.setOnClickListener(view -> {
            host.overlayHost.removeView(shade);
            openSelection(host.tr("Add to queue", "Добавить в список"), new HashSet<>(), selected -> {
                if (host.playbackQueue.isEmpty() && host.currentIndex >= 0 && host.currentIndex < host.tracks.size()) {
                    host.playbackQueue.add(host.tracks.get(host.currentIndex));
                }
                for (String uri : selected) {
                    Track track = host.findTrack(uri);
                    if (track != null && !isInQueue(track)) {
                        host.playbackQueue.add(track);
                    }
                }
                openQueue();
            });
        });
        header.addView(add, host.square(52));
        Button close = host.icon("×");
        close.setOnClickListener(view -> close(shade));
        header.addView(close, host.square(52));
        panel.addView(header);
        ScrollView scroll = new ScrollView(host);
        LinearLayout rows = new LinearLayout(host);
        rows.setOrientation(LinearLayout.VERTICAL);
        for (Track track : new ArrayList<>(host.activeQueue())) {
            rows.addView(host.songsRenderer.queueRow(track,
                    () -> {
                        removeFromQueue(track);
                        host.overlayHost.removeView(shade);
                        openQueue();
                    },
                    () -> {
                        playQueueTrack(track);
                        host.overlayHost.removeView(shade);
                        openQueue();
                    }));
        }
        scroll.addView(rows);
        panel.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1.0f));
        shade.addView(panel, host.bottomParams());
        host.overlayHost.addView(shade);
        host.updateMini();
    }

    void openAddFavorites() {
        openSelection(host.tr("Add to favorites", "Добавить в избранное"),
                new HashSet<>(), selected -> {
                    host.favorites.addAll(selected);
                    host.saveState();
                    host.render();
                });
    }

    private void openAddToPlaylist(Playlist playlist) {
        openSelection(host.tr("Add to ", "Добавить в ") + playlist.name,
                new HashSet<>(), selected -> {
                    host.playlistController.addTracksToPlaylist(playlist, selected);
                    host.render();
                    host.overlayHost.removeAllViews();
                    openPlaylist(playlist);
                });
    }

    private void openSelection(String title, HashSet<String> selected, SelectionDone done) {
        final FrameLayout shade = host.shade();
        LinearLayout panel = host.panelCard();
        LinearLayout header = host.row();
        header.addView(host.text(title, 20, true), new LinearLayout.LayoutParams(0, host.dp(58), 1.0f));
        Button complete = host.icon("✓");
        complete.setOnClickListener(view -> {
            host.overlayHost.removeView(shade);
            done.done(selected);
            host.updateMini();
        });
        header.addView(complete, host.square(52));
        Button close = host.icon("×");
        close.setOnClickListener(view -> close(shade));
        header.addView(close, host.square(52));
        panel.addView(header);
        EditText search = searchField(host.tr("Search songs", "Поиск песен"));
        panel.addView(search, searchParams());
        ScrollView scroll = new ScrollView(host);
        LinearLayout rows = new LinearLayout(host);
        rows.setOrientation(LinearLayout.VERTICAL);
        renderSelectionRows(rows, selected, "");
        search.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void onTextChanged(CharSequence value, int start, int before, int count) {
                renderSelectionRows(rows, selected, value == null ? "" : value.toString());
            }
        });
        scroll.addView(rows);
        panel.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1.0f));
        shade.addView(panel, host.bottomParams());
        host.overlayHost.addView(shade);
        host.updateMini();
    }

    private void renderSelectionRows(LinearLayout parent, HashSet<String> selected, String query) {
        parent.removeAllViews();
        String normalized = query.trim().toLowerCase(Locale.ROOT);
        for (Track track : host.tracks) {
            if (!normalized.isEmpty() && !host.matchesTrackSearch(track, normalized)) {
                continue;
            }
            LinearLayout row = host.row();
            row.setPadding(host.dp(10), host.dp(8), host.dp(10), host.dp(8));
            host.setSurface(row, selected.contains(track.uri) ? host.fg : host.panel, false);
            ImageView cover = host.coverView();
            int fallback = selected.contains(track.uri) ? host.bg : Color.rgb(host.dark ? 28 : 235, host.dark ? 28 : 235, host.dark ? 28 : 235);
            host.loadCover(cover, track, fallback);
            row.addView(cover, host.square(58));
            TextView title = host.text(track.title, 17, true);
            title.setSingleLine(true);
            title.setEllipsize(TextUtils.TruncateAt.END);
            title.setTextColor(selected.contains(track.uri) ? host.bg : host.fg);
            title.setPadding(host.dp(12), 0, host.dp(8), 0);
            row.addView(title, new LinearLayout.LayoutParams(0, host.dp(70), 1.0f));
            Button mark = host.icon(selected.contains(track.uri) ? "✓" : "+");
            mark.setOnClickListener(view -> {
                if (!selected.add(track.uri)) {
                    selected.remove(track.uri);
                }
                renderSelectionRows(parent, selected, query);
            });
            row.addView(mark, host.square(48));
            Button play = host.icon(host.isCurrent(track) && host.playing ? "Ⅱ" : "▶");
            play.setOnClickListener(view -> {
                if (host.isCurrent(track)) {
                    host.toggleCurrent();
                } else {
                    host.playTrack(track, false);
                }
                renderSelectionRows(parent, selected, query);
            });
            row.addView(play, host.square(48));
            parent.addView(host.spaced(row));
        }
    }

    void openSongActions(Track track) {
        openSongActions(track, null);
    }

    void openSongActions(Track track, Playlist sourcePlaylist) {
        final FrameLayout shade = host.shade();
        LinearLayout panel = host.panelCard();
        panel.addView(host.text(track.title, 20, true), new LinearLayout.LayoutParams(-1, host.dp(52)));
        addPanelButton(panel, host.favorites.contains(track.uri)
                ? host.tr("Remove from favorites", "Убрать из избранного")
                : host.tr("Add to favorites", "Добавить в избранное"), () -> {
            host.toggleFavorite(track);
            close(shade);
            host.render();
        });
        addPanelButton(panel, host.tr("Add to playlist", "Добавить в плейлист"), () -> {
            host.overlayHost.removeView(shade);
            choosePlaylist(track);
        });
        addPanelButton(panel, host.tr("Add to queue", "Добавить в очередь"), () -> {
            host.addTrackToQueue(track);
            close(shade);
        });
        if (sourcePlaylist != null) {
            addPanelButton(panel, host.tr("Remove from playlist", "Убрать из плейлиста"), () -> {
                sourcePlaylist.uris.remove(track.uri);
                host.saveState();
                host.overlayHost.removeView(shade);
                openPlaylist(sourcePlaylist);
            });
        }
        addPanelButton(panel, host.tr("Remove from app", "Удалить из приложения"), () -> {
            host.overlayHost.removeView(shade);
            confirmDeleteTrack(track);
        });
        addPanelButton(panel, host.tr("Close", "Закрыть"), () -> close(shade));
        shade.addView(panel, host.bottomParams());
        host.overlayHost.addView(shade);
        host.updateMini();
    }

    private void choosePlaylist(Track track) {
        openCollectionChooser(track, false);
    }

    void chooseCollection(Track track) {
        openCollectionChooser(track, true);
    }

    private void openCollectionChooser(Track track, boolean includeFavorites) {
        final FrameLayout shade = host.shade();
        LinearLayout panel = host.panelCard();
        panel.addView(host.text(includeFavorites
                        ? host.tr("Save track", "Сохранить песню")
                        : host.tr("Add to playlist", "Добавить в плейлист"), 22, true),
                new LinearLayout.LayoutParams(-1, host.dp(48)));
        ScrollView scroll = new ScrollView(host);
        LinearLayout rows = new LinearLayout(host);
        rows.setOrientation(LinearLayout.VERTICAL);
        if (includeFavorites) {
            addPanelButton(rows, host.favorites.contains(track.uri)
                    ? host.tr("Remove from favorites", "Убрать из избранного")
                    : host.tr("Add to favorites", "Добавить в избранное"), () -> {
                host.toggleFavorite(track);
                close(shade);
                host.render();
                host.playerUiController.syncPlaybackUi();
            });
        }
        for (Playlist playlist : host.playlists) {
            boolean alreadyAdded = playlist.uris.contains(track.uri);
            addPanelButton(rows, alreadyAdded
                    ? playlist.name + " " + host.tr("(added)", "(добавлено)")
                    : playlist.name, () -> {
                host.playlistController.addTrackToPlaylist(playlist, track);
                close(shade);
                host.render();
                host.playerUiController.syncPlaybackUi();
            });
        }
        addPanelButton(rows, host.tr("Create new", "Создать новый"), () -> {
            host.overlayHost.removeView(shade);
            showInput(host.tr("New playlist", "Новый плейлист"),
                    host.tr("Playlist name", "Название плейлиста"), "", false,
                    value -> {
                        host.playlistController.createPlaylistWithTrack(value, track);
                        host.render();
                    });
        });
        scroll.addView(rows);
        panel.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1.0f));
        shade.addView(panel, host.centerParams(host.dp(330), host.dp(420)));
        host.overlayHost.addView(shade);
    }

    void createPlaylist() {
        showInput(host.tr("Create playlist", "Создать плейлист"),
                host.tr("Playlist name", "Название плейлиста"), "", false,
                value -> {
                    host.playlistController.createPlaylist(value);
                    host.render();
                });
    }

    void renamePlaylist(Playlist playlist) {
        showInput(host.tr("Rename playlist", "Переименовать плейлист"),
                host.tr("Playlist name", "Название плейлиста"), playlist.name, false,
                value -> {
                    host.playlistController.renamePlaylist(playlist, value);
                    host.render();
                });
    }

    void confirmDeletePlaylist(Playlist playlist) {
        host.showConfirmPanel(host.tr("Delete playlist?", "Удалить плейлист?"),
                host.tr("Songs will stay in the app.", "Песни останутся в приложении."),
                () -> {
                    host.playlistController.deletePlaylist(playlist);
                    host.render();
                });
    }

    private void confirmDeleteTrack(Track track) {
        host.showConfirmPanel(host.tr("Delete song?", "Удалить песню?"),
                host.tr("The song will disappear from the app, but the file will stay on the phone.",
                        "Песня исчезнет из приложения, но файл останется на телефоне."), () -> {
                    host.tracks.remove(track);
                    host.favorites.remove(track.uri);
                    host.playlistController.removeTrackFromAllPlaylists(track);
                    TrackStore.save(host, host.tracks);
                    host.saveState();
                    host.render();
                });
    }

    void openSearch() {
        final FrameLayout shade = host.shade();
        LinearLayout panel = host.panelCard();
        panel.setPadding(host.dp(16), host.dp(16), host.dp(16), host.dp(16));
        panel.addView(host.text(host.tr("Search", "Поиск"), 22, true),
                new LinearLayout.LayoutParams(-1, host.dp(48)));
        EditText input = searchField(host.tr("Find", "Найти"));
        input.setText(host.search);
        panel.addView(input, searchParams());
        LinearLayout actions = host.row();
        Button reset = host.button(host.tr("Reset", "Сброс"));
        reset.setOnClickListener(view -> {
            host.search = "";
            close(shade);
            host.render();
        });
        actions.addView(reset, new LinearLayout.LayoutParams(0, host.dp(54), 1.0f));
        Button find = host.button(host.tr("Find", "Найти"));
        host.applyPrimaryButtonStyle(find);
        find.setOnClickListener(view -> {
            host.search = input.getText().toString();
            close(shade);
            host.render();
        });
        actions.addView(find, new LinearLayout.LayoutParams(0, host.dp(54), 1.0f));
        panel.addView(actions);
        shade.addView(panel, host.centerParams(host.dp(330), host.dp(230)));
        host.overlayHost.addView(shade);
        input.requestFocus();
        host.updateMini();
    }

    void showInput(String title, String hint, String value, boolean numeric, MainActivityCore.InputDone done) {
        final FrameLayout shade = host.shade();
        LinearLayout panel = host.panelCard();
        panel.setPadding(host.dp(16), host.dp(16), host.dp(16), host.dp(16));
        panel.addView(host.text(title, 22, true), new LinearLayout.LayoutParams(-1, host.dp(48)));
        EditText input = searchField(hint);
        input.setText(value);
        input.setSelection(input.length());
        input.setInputType(numeric ? 2 : 1);
        panel.addView(input, searchParams());
        LinearLayout actions = host.row();
        Button cancel = host.button(host.tr("Cancel", "Отмена"));
        cancel.setOnClickListener(view -> close(shade));
        actions.addView(cancel, new LinearLayout.LayoutParams(0, host.dp(54), 1.0f));
        Button save = host.button(host.tr("Done", "Готово"));
        host.applyPrimaryButtonStyle(save);
        save.setOnClickListener(view -> {
            String result = input.getText().toString();
            close(shade);
            done.done(result);
        });
        actions.addView(save, new LinearLayout.LayoutParams(0, host.dp(54), 1.0f));
        panel.addView(actions);
        shade.addView(panel, host.centerParams(host.dp(330), host.dp(230)));
        host.overlayHost.addView(shade);
        input.requestFocus();
    }

    private void removeFromQueue(Track track) {
        for (int i = host.playbackQueue.size() - 1; i >= 0; i--) {
            if (host.playbackQueue.get(i).uri.equals(track.uri)) {
                host.playbackQueue.remove(i);
            }
        }
        if (host.isCurrent(track)) {
            Intent intent = new Intent(host, PlayerService.class);
            intent.setAction(PlayerService.ACTION_STOP);
            if (Build.VERSION.SDK_INT >= 26) {
                host.startForegroundService(intent);
            } else {
                host.startService(intent);
            }
            host.currentIndex = -1;
            host.playing = false;
        }
    }

    private void playQueueTrack(Track track) {
        int index = host.queueIndexOf(track);
        if (index < 0) {
            return;
        }
        host.currentIndex = host.tracks.indexOf(track);
        host.playing = true;
        host.resumePosition = 0;
        host.startServiceAction(PlayerService.ACTION_PLAY_INDEX, index, false);
        host.startPlaybackWatcher();
        host.render();
    }

    private boolean isInQueue(Track track) {
        for (Track queued : host.playbackQueue) {
            if (queued.uri.equals(track.uri)) {
                return true;
            }
        }
        return false;
    }

    private EditText searchField(String hint) {
        EditText input = new EditText(host);
        input.setSingleLine(true);
        input.setHint(hint);
        input.setTextColor(host.fg);
        input.setHintTextColor(host.muted);
        input.setTextSize(16.0f);
        input.setPadding(host.dp(14), 0, host.dp(14), 0);
        input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(80)});
        host.setSurface(input, host.panel, true);
        return input;
    }

    private LinearLayout.LayoutParams searchParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, host.dp(58));
        params.setMargins(0, host.dp(8), 0, host.dp(12));
        return params;
    }

    private void addPanelButton(LinearLayout panel, String label, Runnable action) {
        Button button = host.button(label);
        button.setOnClickListener(view -> action.run());
        panel.addView(button, new LinearLayout.LayoutParams(-1, host.dp(54)));
    }

    private void close(FrameLayout shade) {
        if (shade.getParent() != null) {
            host.overlayHost.removeView(shade);
        }
        host.updateMini();
    }

    private abstract static class SimpleTextWatcher implements TextWatcher {
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
        @Override public void afterTextChanged(Editable s) { }
    }
}
