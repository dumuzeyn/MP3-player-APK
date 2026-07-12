package com.dumuzeyn.mp3player;

import android.graphics.Color;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

abstract class TrackGroupMenuRenderer implements MenuRenderer {
    final MainActivityCore host;

    TrackGroupMenuRenderer(MainActivityCore host) {
        this.host = host;
    }

    abstract String groupValue(Track track);

    abstract String unknownGroupName();

    @Override
    public boolean needsMiniSpacer() {
        return true;
    }

    @Override
    public void render() {
        String query = host.search.trim().toLowerCase(Locale.ROOT);
        for (Map.Entry<String, ArrayList<Track>> entry : groupedTracks().entrySet()) {
            if (!query.isEmpty()
                    && !host.containsSearch(entry.getKey(), query)
                    && !containsTrack(entry.getValue(), query)) {
                continue;
            }
            host.list.addView(host.spaced(groupCard(entry.getKey(), entry.getValue())));
        }
    }

    private Map<String, ArrayList<Track>> groupedTracks() {
        Map<String, ArrayList<Track>> result = new LinkedHashMap<>();
        for (Track track : host.tracks) {
            String rawValue = groupValue(track);
            String name = rawValue == null || rawValue.trim().isEmpty()
                    ? unknownGroupName()
                    : rawValue.trim();
            ArrayList<Track> group = result.get(name);
            if (group == null) {
                group = new ArrayList<>();
                result.put(name, group);
            }
            group.add(track);
        }
        return result;
    }

    private boolean containsTrack(ArrayList<Track> tracks, String query) {
        for (Track track : tracks) {
            if (host.matchesTrackSearch(track, query)) {
                return true;
            }
        }
        return false;
    }

    private LinearLayout groupCard(final String name, final ArrayList<Track> tracks) {
        LinearLayout row = host.row();
        row.setPadding(host.dp(12), host.dp(12), host.dp(12), host.dp(12));
        host.setSurface(row, host.panel, false);

        ImageView cover = host.coverView();
        int fallback = host.dark ? 28 : 235;
        if (tracks.isEmpty()) {
            cover.setBackgroundColor(Color.rgb(fallback, fallback, fallback));
        } else {
            host.loadCover(cover, tracks.get(0), Color.rgb(fallback, fallback, fallback));
        }
        row.addView(cover, host.square(72));

        LinearLayout labels = new LinearLayout(host);
        labels.setOrientation(LinearLayout.VERTICAL);
        labels.setPadding(host.dp(12), 0, host.dp(8), 0);
        TextView title = host.text(name, 20, true);
        title.setSingleLine(true);
        title.setEllipsize(TextUtils.TruncateAt.END);
        labels.addView(title);
        labels.addView(host.text(tracks.size() + " " + host.tr("songs", "песен"), 13, false));
        row.addView(labels, new LinearLayout.LayoutParams(0, host.dp(72), 1.0f));

        Button play = host.icon(host.isPlayingSource(tracks) ? "Ⅱ" : "▶");
        host.applyPlainIconStyle(play, host.purple);
        play.setOnClickListener(view -> {
            if (host.isPlayingSource(tracks)) {
                host.toggleCurrent();
            } else {
                host.playList(tracks, false);
            }
        });
        row.addView(play, host.square(52));

        Button shuffle = host.shuffleButton();
        host.applyPlainIconStyle(shuffle);
        shuffle.setOnClickListener(view -> host.playList(tracks, true));
        row.addView(shuffle, host.square(52));
        row.setOnClickListener(view -> host.openGroupSongs(name, tracks));
        return row;
    }
}
