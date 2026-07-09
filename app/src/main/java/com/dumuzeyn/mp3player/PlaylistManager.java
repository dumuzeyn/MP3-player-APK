package com.dumuzeyn.mp3player;

import java.util.ArrayList;
import java.util.Iterator;
import org.json.JSONArray;
import org.json.JSONObject;

final class PlaylistManager {
    private PlaylistManager() {
    }

    static ArrayList<MainActivity.Playlist> fromJson(String raw) {
        ArrayList<MainActivity.Playlist> playlists = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(raw == null ? "[]" : raw);
            for (int i = 0; i < array.length(); i++) {
                JSONObject object = array.getJSONObject(i);
                MainActivity.Playlist playlist = new MainActivity.Playlist(cleanName(object.optString("name", "Playlist")));
                JSONArray songs = object.optJSONArray("songs");
                if (songs != null) {
                    for (int songIndex = 0; songIndex < songs.length(); songIndex++) {
                        playlist.uris.add(songs.getString(songIndex));
                    }
                }
                playlists.add(playlist);
            }
        } catch (Exception ignored) {
        }
        return playlists;
    }

    static String toJson(ArrayList<MainActivity.Playlist> playlists) {
        JSONArray array = new JSONArray();
        for (MainActivity.Playlist playlist : playlists) {
            try {
                JSONObject object = new JSONObject();
                object.put("name", playlist.name);
                JSONArray songs = new JSONArray();
                Iterator<String> iterator = playlist.uris.iterator();
                while (iterator.hasNext()) {
                    songs.put(iterator.next());
                }
                object.put("songs", songs);
                array.put(object);
            } catch (Exception ignored) {
            }
        }
        return array.toString();
    }

    static String cleanName(String value) {
        if (value == null) {
            return "";
        }
        String cleaned = value.replaceAll("[\\p{Cntrl}&&[^\n\t]]", "").replace('\n', ' ').replace('\t', ' ').trim();
        return cleaned.length() > 80 ? cleaned.substring(0, 80).trim() : cleaned;
    }
}
