package com.dumuzeyn.mp3player;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.MediaMetadataRetriever;
import android.net.Uri;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class TrackStore {
    private static final String PREFS = "mp3_player_store";
    private static final String TRACKS = "tracks";
    private static final int MAX_TEXT_LENGTH = 160;

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
                        item.optString("artist", "Неизвестный исполнитель"),
                        item.optString("album", "Неизвестный альбом"),
                        item.optString("genre", "Неизвестный жанр")
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
                item.put("album", track.album);
                item.put("genre", track.genre);
                array.put(item);
            } catch (Exception ignored) {}
        }
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        prefs.edit().putString(TRACKS, array.toString()).apply();
    }

    public static Track fromUri(Context context, Uri uri) {
        String title = null;
        String artist = null;
        String album = null;
        String genre = null;
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(context, uri);
            title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
            artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
            album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM);
            genre = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE);
        } catch (Throwable ignored) {
        } finally {
            try {
                retriever.release();
            } catch (Exception ignored) {}
        }

        if (title == null || title.trim().isEmpty()) {
            String path = uri.getLastPathSegment();
            title = path == null ? "Песня" : path.substring(path.lastIndexOf('/') + 1);
        }
        if (artist == null || artist.trim().isEmpty()) {
            artist = "Неизвестный исполнитель";
        }
        if (album == null || album.trim().isEmpty()) {
            album = "Неизвестный альбом";
        }
        if (genre == null || genre.trim().isEmpty()) {
            genre = "Неизвестный жанр";
        }
        return new Track(uri.toString(), cleanText(title), cleanText(artist), cleanText(album), cleanText(genre));
    }

    public static Track refreshMetadata(Context context, Track oldTrack) {
        Track fresh = fromUri(context, Uri.parse(oldTrack.uri));
        String title = fresh.title == null || fresh.title.trim().isEmpty() ? oldTrack.title : fresh.title;
        String artist = fresh.artist == null || fresh.artist.trim().isEmpty() ? oldTrack.artist : fresh.artist;
        String album = fresh.album == null || fresh.album.trim().isEmpty() ? oldTrack.album : fresh.album;
        String genre = fresh.genre == null || fresh.genre.trim().isEmpty() ? oldTrack.genre : fresh.genre;
        return new Track(oldTrack.uri, title, artist, album, genre);
    }

    public static void sort(List<Track> tracks) {
        Collections.sort(tracks, new Comparator<Track>() {
            @Override
            public int compare(Track left, Track right) {
                return left.title.toLowerCase(Locale.ROOT).compareTo(right.title.toLowerCase(Locale.ROOT));
            }
        });
    }

    private static String cleanText(String value) {
        if (value == null) {
            return "";
        }
        String cleaned = value.replace('\u0000', ' ').replace('\n', ' ').replace('\r', ' ').trim();
        if (cleaned.length() > MAX_TEXT_LENGTH) {
            cleaned = cleaned.substring(0, MAX_TEXT_LENGTH).trim();
        }
        return cleaned;
    }
}
