package com.dumuzeyn.mp3player;

import com.dumuzeyn.mp3player.data.playback.PlaybackStateManager;

import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

final class SongsRenderer {
    private final MainActivityCore host;
    private final ExecutorService metadataExecutor = Executors.newSingleThreadExecutor();
    private volatile boolean closed;
    private ArrayList<Track> pendingTracks;
    private int pendingStart;
    private int pendingGeneration = -1;

    SongsRenderer(MainActivityCore host) {
        this.host = host;
    }

    void restoreRecentPlayback() {
        PlayerService.refreshSnapshot();
        boolean liveSession = PlayerService.hasPlaybackSession();
        if (!liveSession && host.resumeWindowMinutes <= 0) {
            return;
        }
        PlaybackStateManager.State savedState = new PlaybackStateManager(host).load();
        long savedAt = savedState.savedAt;
        long resumeWindow = (long) host.resumeWindowMinutes * 60000L;
        if (!liveSession && (savedAt <= 0L || System.currentTimeMillis() - savedAt > resumeWindow)) {
            return;
        }
        String savedUri = savedState.uri;
        String uri = liveSession && !PlayerService.lastUri.isEmpty()
                ? PlayerService.lastUri
                : savedUri;
        Track track = host.findTrack(uri);
        if (track == null) {
            return;
        }
        host.currentIndex = host.tracks.indexOf(track);
        host.playing = liveSession && PlayerService.lastPlaying;
        host.resumePosition = liveSession
                ? Math.max(0, PlayerService.lastPosition)
                : savedState.position;
        host.loopMode = liveSession
                ? PlayerService.lastLoopMode
                : savedState.loopMode;
        host.shuffleMode = savedState.shuffle;
        host.playbackQueue.clear();
        host.playbackQueue.addAll(PlaybackQueueResolver.restore(
                host.tracks,
                savedState.queueUris,
                track));
        if (!liveSession) {
            PlayerService.lastIndex = host.currentIndex;
            PlayerService.lastPlaying = false;
            PlayerService.lastPosition = host.resumePosition;
            PlayerService.lastDuration = Math.max(track.durationMs,
                    savedState.duration);
            PlayerService.lastUri = uri;
            PlayerService.lastLoopMode = host.loopMode;
        }
    }

    void refreshMissingMetadataAsync() {
        final ArrayList<Track> snapshot = new ArrayList<>(host.tracks);
        try {
            metadataExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    final Map<String, Track> refreshed = new HashMap<>();
                    for (Track track : snapshot) {
                        if (closed) {
                            return;
                        }
                        if (!needsMetadataRefresh(track)) {
                            continue;
                        }
                        Track updated = TrackStore.refreshMetadata(host, track);
                        if (metadataChanged(track, updated)) {
                            TrackStore.updateMetadata(host, updated);
                            refreshed.put(updated.uri, updated);
                        }
                    }
                    if (refreshed.isEmpty() || closed) {
                        return;
                    }
                    host.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (closed) {
                                return;
                            }
                            for (int index = 0; index < host.tracks.size(); index++) {
                                Track current = host.tracks.get(index);
                                Track updated = refreshed.get(current.uri);
                                if (updated != null) {
                                    host.tracks.set(index, updated);
                                }
                            }
                            host.render();
                        }
                    });
                }
            });
        } catch (RejectedExecutionException ignored) {
            // Activity is already closing.
        }
    }

    void close() {
        closed = true;
        metadataExecutor.shutdownNow();
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
        pendingTracks = null;
        pendingStart = 0;
        pendingGeneration = -1;
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
        pendingTracks = new ArrayList<>(tracks);
        pendingGeneration = host.songRenderGeneration;
        appendNextSongBatch();
    }

    void loadMoreIfNearBottom() {
        if (host.contentScroll == null || pendingTracks == null || pendingStart >= pendingTracks.size()) {
            return;
        }
        View child = host.contentScroll.getChildAt(0);
        if (child == null || child.getBottom() - (host.contentScroll.getHeight()
                + host.contentScroll.getScrollY()) > host.dp(900)) {
            return;
        }
        appendNextSongBatch();
    }

    void prepareForScrollRestore(int scrollY) {
        if (scrollY <= 0 || pendingTracks == null) {
            return;
        }
        int approximateRows = Math.max(24, scrollY / Math.max(1, host.dp(62)) + 18);
        while (pendingTracks != null && pendingStart < approximateRows) {
            appendNextSongBatch();
        }
    }

    private void appendNextSongBatch() {
        if (pendingTracks == null || pendingGeneration != host.songRenderGeneration || host.tabIndex > 1) {
            pendingTracks = null;
            return;
        }
        ArrayList<Track> tracksToRender = pendingTracks;
        int start = pendingStart;
        int batchSize = host.renderingTabPreview ? 15 : 24;
        int end = Math.min(tracksToRender.size(), start + batchSize);
        for (int i = start; i < end; i++) {
            host.list.addView(songRow(tracksToRender.get(i), true, true));
        }
        pendingStart = end;
        if (end >= tracksToRender.size()) {
            pendingTracks = null;
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
        FrameLayout container = new FrameLayout(host);
        LinearLayout row = new LinearLayout(host);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(16);
        row.setPadding(host.dp(8), host.dp(4), host.dp(8), host.dp(4));
        host.applyCardStyle(row, host.tabIndex == 1
                ? host.favoriteCardOpacity : host.songCardOpacity);

        View marker = new View(host);
        marker.setBackgroundColor(host.yellow);
        marker.setVisibility(host.isCurrent(track) ? View.VISIBLE : View.INVISIBLE);
        if (!host.renderingTabPreview) {
            host.songRows.registerCurrentMarker(track.uri, marker);
        }

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
        row.addView(cover, host.square(52));

        LinearLayout textColumn = new LinearLayout(host);
        textColumn.setOrientation(LinearLayout.VERTICAL);
        textColumn.setPadding(host.dp(10), 0, host.dp(6), 0);
        TextView title = host.text(track.title, 16, true);
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
        metaRow.addView(waveform, new LinearLayout.LayoutParams(0, host.dp(26), 1.0f));
        TextView duration = host.text(host.formatTrackDuration(track), 12, false);
        duration.setGravity(17);
        duration.setTextColor(host.secondaryText);
        metaRow.addView(duration, new LinearLayout.LayoutParams(host.dp(46), host.dp(26)));
        textColumn.addView(metaRow);
        row.addView(textColumn, new LinearLayout.LayoutParams(0, host.dp(62), 1.0f));

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
            row.addView(favorite, host.square(40));
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
            row.addView(actions, host.square(44));
        }

        Button play = host.icon("");
        host.applyPrimaryButtonStyle(play);
        SongRowStateRegistry.applyPlayState(play, host.isCurrent(track) && host.playing);
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
        row.addView(play, host.square(44));
        container.addView(row, new FrameLayout.LayoutParams(-1, -2));
        FrameLayout.LayoutParams markerParams = new FrameLayout.LayoutParams(host.dp(4), host.dp(52));
        markerParams.gravity = android.view.Gravity.START | android.view.Gravity.CENTER_VERTICAL;
        markerParams.setMargins(host.dp(2), 0, 0, 0);
        container.addView(marker, markerParams);
        return host.spaced(container);
    }

    View queueRow(final Track track, final Runnable removeAction, final Runnable playAction) {
        LinearLayout row = new LinearLayout(host);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(16);
        row.setPadding(host.dp(8), host.dp(4), host.dp(8), host.dp(4));
        host.setSurface(row, host.isCurrent(track) ? host.fg : host.panel, false,
                host.songCardOpacity);

        ImageView cover = host.coverView();
        host.loadCover(cover, track, host.dark ? android.graphics.Color.rgb(28, 28, 28) : android.graphics.Color.rgb(235, 235, 235));
        row.addView(cover, host.square(52));

        TextView title = host.text(track.title, 17, true);
        title.setSingleLine(true);
        title.setEllipsize(TextUtils.TruncateAt.END);
        title.setPadding(host.dp(12), 0, host.dp(8), 0);
        title.setTextColor(host.isCurrent(track) ? host.bg : host.fg);
        row.addView(title, new LinearLayout.LayoutParams(0, host.dp(62), 1.0f));

        Button remove = host.icon("−");
        host.applyPlainIconStyle(remove, host.isCurrent(track) ? host.bg : android.graphics.Color.rgb(190, 45, 45));
        remove.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                removeAction.run();
            }
        });
        row.addView(remove, host.square(44));

        Button play = host.icon("");
        host.applyPlainIconStyle(play, host.isCurrent(track) ? host.bg : host.purple);
        SongRowStateRegistry.applyPlayState(play, host.isCurrent(track) && host.playing);
        play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                playAction.run();
            }
        });
        row.addView(play, host.square(44));
        return host.spaced(row);
    }
}
