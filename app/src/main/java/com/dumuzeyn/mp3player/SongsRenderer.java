package com.dumuzeyn.mp3player;

import android.content.SharedPreferences;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;
import org.json.JSONArray;

final class SongsRenderer {
    private final MainActivityCore host;

    SongsRenderer(MainActivityCore host) {
        this.host = host;
    }

    void restoreRecentPlayback() {
        if (host.resumeWindowMinutes <= 0) {
            return;
        }
        SharedPreferences prefs = host.getSharedPreferences(PlayerService.RESUME_PREFS, 0);
        long savedAt = prefs.getLong(PlayerService.RESUME_SAVED_AT, 0L);
        long resumeWindow = (long) host.resumeWindowMinutes * 60000L;
        if (savedAt <= 0L || System.currentTimeMillis() - savedAt > resumeWindow) {
            return;
        }
        String uri = prefs.getString(PlayerService.RESUME_URI, "");
        Track track = host.findTrack(uri);
        if (track == null) {
            return;
        }
        host.currentIndex = host.tracks.indexOf(track);
        host.playing = false;
        host.resumePosition = Math.max(0, prefs.getInt(PlayerService.RESUME_POSITION, 0));
        PlayerService.lastIndex = host.currentIndex;
        PlayerService.lastPlaying = false;
        PlayerService.lastPosition = host.resumePosition;
        PlayerService.lastDuration = Math.max(0, prefs.getInt(PlayerService.RESUME_DURATION, 0));
        if (PlayerService.lastDuration <= 0 && track.durationMs > 0) {
            PlayerService.lastDuration = track.durationMs;
        }
        PlayerService.lastUri = uri;
        PlayerService.lastLoopMode = prefs.getInt(PlayerService.RESUME_LOOP_MODE, 0);
        host.loopMode = PlayerService.lastLoopMode;
        host.playbackQueue.clear();
        try {
            JSONArray queue = new JSONArray(prefs.getString(PlayerService.RESUME_QUEUE, "[]"));
            for (int i = 0; i < queue.length(); i++) {
                Track queueTrack = host.findTrack(queue.optString(i, ""));
                if (queueTrack != null) {
                    host.playbackQueue.add(queueTrack);
                }
            }
        } catch (Exception ignored) {
            // A malformed saved queue must not prevent the current track from restoring.
        }
        if (host.playbackQueue.isEmpty()) {
            host.playbackQueue.add(track);
        }
    }

    void refreshMissingMetadataAsync() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                final ArrayList<Track> refreshed = new ArrayList<>(host.tracks);
                boolean changed = false;
                for (int i = 0; i < refreshed.size(); i++) {
                    Track track = refreshed.get(i);
                    if (!needsMetadataRefresh(track)) {
                        continue;
                    }
                    Track updated = TrackStore.refreshMetadata(host, track);
                    if (metadataChanged(track, updated)) {
                        refreshed.set(i, updated);
                        changed = true;
                    }
                }
                if (!changed) {
                    return;
                }
                TrackStore.save(host, refreshed);
                host.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        host.tracks.clear();
                        host.tracks.addAll(refreshed);
                        host.render();
                    }
                });
            }
        }).start();
    }

    private boolean needsMetadataRefresh(Track track) {
        return track.durationMs <= 0
                || track.artist == null || track.artist.trim().isEmpty()
                || track.album == null || track.album.trim().isEmpty()
                || track.genre == null || track.genre.trim().isEmpty();
    }

    private boolean metadataChanged(Track before, Track after) {
        return before.durationMs != after.durationMs
                || !safeEquals(before.artist, after.artist)
                || !safeEquals(before.album, after.album)
                || !safeEquals(before.genre, after.genre);
    }

    private boolean safeEquals(String left, String right) {
        return left == null ? right == null : left.equals(right);
    }

    void render(ArrayList<Track> tracks) {
        renderSongsState(tracks);
    }

    void renderSongsState(ArrayList<Track> tracks) {
        String title;
        String titleRu;
        if (tracks.isEmpty()) {
            if (host.tabIndex == 0) {
                title = "Add MP3 or another audio file";
                titleRu = "Добавьте MP3 или другой аудиофайл";
            } else {
                title = "Nothing here yet";
                titleRu = "Здесь пока пусто";
            }
            TextView empty = host.text(host.tr(title, titleRu), 18, true);
            empty.setPadding(host.dp(12), host.dp(24), host.dp(12), host.dp(24));
            host.list.addView(empty);
            host.addMiniSpacerIfNeeded();
            return;
        }
        appendSongRows(tracks, 0, host.songRenderGeneration);
    }

    private void appendSongRows(final ArrayList<Track> tracksToRender, int start, final int generation) {
        if (generation != host.songRenderGeneration || host.tabIndex > 1) {
            return;
        }
        int batchSize = host.renderingTabPreview ? 5 : 24;
        int end = Math.min(tracksToRender.size(), start + batchSize);
        for (int i = start; i < end; i++) {
            host.list.addView(songRow(tracksToRender.get(i), true, true));
        }
        if (!host.renderingTabPreview && end < tracksToRender.size()) {
            final int nextStart = end;
            host.uiHandler.post(new Runnable() {
                @Override
                public void run() {
                    appendSongRows(tracksToRender, nextStart, generation);
                }
            });
        } else {
            host.addMiniSpacerIfNeeded();
        }
    }

    View songRow(Track track, boolean showActions, boolean showFavoriteAction) {
        return songRow(track, showActions, showFavoriteAction, null);
    }

    View songRow(final Track track, boolean showActions, boolean showFavoriteAction, final Runnable afterPlay) {
        return songRow(track, showActions, showFavoriteAction, afterPlay, null);
    }

    View songRow(final Track track, boolean showActions, boolean showFavoriteAction, final Runnable afterPlay,
            final Runnable actionOverride) {
        LinearLayout row = new LinearLayout(host);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(16);
        row.setPadding(host.dp(8), host.dp(8), host.dp(10), host.dp(8));
        host.applyCardStyle(row);

        View marker = new View(host);
        marker.setBackgroundColor(host.yellow);
        marker.setVisibility(host.isCurrent(track) ? View.VISIBLE : View.INVISIBLE);
        if (!host.renderingTabPreview) {
            host.songRows.registerCurrentMarker(track.uri, marker);
        }
        LinearLayout.LayoutParams markerParams = new LinearLayout.LayoutParams(host.dp(4), host.dp(58));
        markerParams.setMargins(0, 0, host.dp(6), 0);
        row.addView(marker, markerParams);

        ImageView cover = host.coverView();
        host.loadCover(cover, track, host.purpleSoft);
        cover.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                host.seedCoverCacheFromView(cover, track);
                host.playTrack(track);
                host.fullPlayerOpening = true;
                host.openFullPlayer();
            }
        });
        row.addView(cover, host.square(58));

        LinearLayout textColumn = new LinearLayout(host);
        textColumn.setOrientation(LinearLayout.VERTICAL);
        textColumn.setPadding(host.dp(12), 0, host.dp(8), 0);
        TextView title = host.text(track.title, 17, true);
        title.setTextColor(host.primaryText);
        title.setSingleLine(true);
        title.setEllipsize(TextUtils.TruncateAt.END);
        textColumn.addView(title);

        LinearLayout metaRow = new LinearLayout(host);
        metaRow.setOrientation(LinearLayout.HORIZONTAL);
        metaRow.setGravity(16);
        WaveformView waveform = host.wave(track, host.isCurrent(track));
        if (!host.renderingTabPreview) {
            host.songRows.registerWaveform(track.uri, waveform);
        }
        metaRow.addView(waveform, new LinearLayout.LayoutParams(0, host.dp(30), 1.0f));
        TextView duration = host.text(host.formatTrackDuration(track), 12, false);
        duration.setGravity(17);
        duration.setTextColor(host.secondaryText);
        metaRow.addView(duration, new LinearLayout.LayoutParams(host.dp(48), host.dp(30)));
        textColumn.addView(metaRow);
        row.addView(textColumn, new LinearLayout.LayoutParams(0, host.dp(70), 1.0f));

        if (host.tabIndex == 1) {
            Button favorite = host.icon(host.favorites.contains(track.uri) ? "♥︎" : "♡︎");
            favorite.setTextSize(14.0f);
            host.applyPlainIconStyle(favorite, host.favorites.contains(track.uri) ? host.purple : host.secondaryText);
            favorite.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    host.toggleFavorite(track);
                    host.render();
                }
            });
            row.addView(favorite, host.square(42));
        } else if (showActions) {
            Button actions = host.icon("⋯");
            host.applyPlainIconStyle(actions);
            actions.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (actionOverride != null) {
                        actionOverride.run();
                    } else {
                        host.openSongActions(track);
                    }
                }
            });
            row.addView(actions, host.square(48));
        }

        Button play = host.icon((host.isCurrent(track) && host.playing) ? "Ⅱ" : "▶");
        host.applyPrimaryButtonStyle(play);
        play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (host.isCurrent(track)) {
                    host.toggleCurrent();
                } else {
                    host.playTrack(track);
                }
                if (afterPlay != null) {
                    afterPlay.run();
                }
            }
        });
        if (!host.renderingTabPreview) {
            host.songRows.registerPlayButton(track.uri, play);
        }
        row.addView(play, host.square(48));
        return host.spaced(row);
    }

    View queueRow(final Track track, final Runnable removeAction, final Runnable playAction) {
        LinearLayout row = new LinearLayout(host);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(16);
        row.setPadding(host.dp(10), host.dp(8), host.dp(10), host.dp(8));
        host.setSurface(row, host.isCurrent(track) ? host.fg : host.panel, false);

        ImageView cover = host.coverView();
        host.loadCover(cover, track, host.dark ? android.graphics.Color.rgb(28, 28, 28) : android.graphics.Color.rgb(235, 235, 235));
        row.addView(cover, host.square(58));

        TextView title = host.text(track.title, 17, true);
        title.setSingleLine(true);
        title.setEllipsize(TextUtils.TruncateAt.END);
        title.setPadding(host.dp(12), 0, host.dp(8), 0);
        title.setTextColor(host.isCurrent(track) ? host.bg : host.fg);
        row.addView(title, new LinearLayout.LayoutParams(0, host.dp(70), 1.0f));

        Button remove = host.icon("−");
        host.applyPlainIconStyle(remove, host.isCurrent(track) ? host.bg : android.graphics.Color.rgb(190, 45, 45));
        remove.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                removeAction.run();
            }
        });
        row.addView(remove, host.square(48));

        Button play = host.icon((host.isCurrent(track) && host.playing) ? "Ⅱ" : "▶");
        host.applyPlainIconStyle(play, host.isCurrent(track) ? host.bg : host.purple);
        play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                playAction.run();
            }
        });
        row.addView(play, host.square(48));
        return host.spaced(row);
    }
}
