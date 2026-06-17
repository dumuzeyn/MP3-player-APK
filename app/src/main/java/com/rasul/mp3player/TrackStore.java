package com.rasul.mp3player;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.MediaMetadataRetriever;
import android.net.Uri;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class TrackStore {
    private static final String PREFS = "mp3_player_store";
    private static final String TRACKS = "tracks";

    private TrackStore() {}

    public static ArrayList<Track> load(Context context) {
        ArrayList<Track> tracks = new ArrayList<>();
        String raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(TRACKS, "[]");
        try {
            JSONArray array = new JSONArray(raw);
            for (int index = 0; index < array.length(); index++) {
                JSONObject item = array.getJSONObject(index);
                tracks.add(new Track(
                        item.getString("uri"),
                        item.optString("title", "Песня"),
                        item.optString("artist", "Неизвестный исполнитель")
                ));
            }
        } catch (Exception ignored) {
            tracks.clear();
        }
        sort(tracks);
        return tracks;
    }

    public static void save(Context context, List<Track> tracks) {
        JSONArray array = new JSONArray();
        for (Track track : tracks) {
            JSONObject item = new JSONObject();
            try {
                item.put("uri", track.uri);
                item.put("title", track.title);
                item.put("artist", track.artist);
                array.put(item);
            } catch (Exception ignored) {}
        }
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        prefs.edit().putString(TRACKS, array.toString()).apply();
    }

    public static Track fromUri(Context context, Uri uri) {
        String title = null;
        String artist = null;
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(context, uri);
            title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
            artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
        } catch (Exception ignored) {
        } finally {
            try {
                retriever.release();
            } catch (Exception ignored) {}
        }

        if (title == null || title.trim().isEmpty()) {
            String path = uri.getLastPathSegment();
            title = path == null ? "Песня" : path.substring(path.lastIndexOf('/') + 1);
        }
        if (artist == null || artist.trim().isEmpty()) artist = "Неизвестный исполнитель";
        return new Track(uri.toString(), title.trim(), artist.trim());
    }

    public static void sort(List<Track> tracks) {
        tracks.sort(Comparator.comparing(track -> track.title.toLowerCase()));
    }
}
