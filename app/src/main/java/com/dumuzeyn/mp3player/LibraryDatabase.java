package com.dumuzeyn.mp3player;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class LibraryDatabase extends SQLiteOpenHelper {
    private static final String DB_NAME = "mp3_player_library.db";
    private static final int DB_VERSION = 1;
    private static final String DEBUG_TAG = "MP3PlayerDebug";
    private static final String PREFS_STORE = "mp3_player_store";
    private static final String PREFS_UI = "mp3_player_ui";
    private static final String PREFS_MIGRATED = "sqlite_migrated";

    LibraryDatabase(Context context) {
        super(context.getApplicationContext(), DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE tracks (uri TEXT PRIMARY KEY NOT NULL, title TEXT NOT NULL, artist TEXT NOT NULL, album TEXT NOT NULL, genre TEXT NOT NULL, duration_ms INTEGER NOT NULL DEFAULT 0)");
        db.execSQL("CREATE TABLE favorites (uri TEXT PRIMARY KEY NOT NULL)");
        db.execSQL("CREATE TABLE playlists (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, position INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE playlist_tracks (playlist_id INTEGER NOT NULL, uri TEXT NOT NULL, position INTEGER NOT NULL, PRIMARY KEY (playlist_id, uri))");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    static void migrateLegacyIfNeeded(Context context) {
        SharedPreferences storePrefs = context.getSharedPreferences(PREFS_STORE, Context.MODE_PRIVATE);
        if (storePrefs.getBoolean(PREFS_MIGRATED, false)) {
            return;
        }
        LibraryDatabase database = new LibraryDatabase(context);
        SQLiteDatabase db = database.getWritableDatabase();
        try {
            db.beginTransaction();
            if (countRows(db, "tracks") == 0) {
                ArrayList<Track> legacyTracks = TrackStore.loadFromJson(storePrefs.getString("tracks", "[]"));
                saveTracks(db, legacyTracks);
            }
            SharedPreferences uiPrefs = context.getSharedPreferences(PREFS_UI, Context.MODE_PRIVATE);
            if (countRows(db, "favorites") == 0) {
                saveFavorites(db, uiPrefs.getStringSet("favorites", new HashSet<String>()));
            }
            if (countRows(db, "playlists") == 0) {
                savePlaylists(db, PlaylistManager.fromJson(uiPrefs.getString("playlists", "[]")));
            }
            db.setTransactionSuccessful();
            storePrefs.edit().putBoolean(PREFS_MIGRATED, true).apply();
        } catch (Exception e) {
            Log.w(DEBUG_TAG, "sqlite_migration_failed error=" + e.getMessage());
        } finally {
            db.endTransaction();
            database.close();
        }
    }

    ArrayList<Track> loadTracks() {
        ArrayList<Track> tracks = new ArrayList<>();
        Cursor cursor = null;
        try {
            cursor = getReadableDatabase().query("tracks", null, null, null, null, null, "title COLLATE NOCASE ASC");
            while (cursor.moveToNext()) {
                tracks.add(new Track(
                        cursor.getString(cursor.getColumnIndexOrThrow("uri")),
                        cursor.getString(cursor.getColumnIndexOrThrow("title")),
                        cursor.getString(cursor.getColumnIndexOrThrow("artist")),
                        cursor.getString(cursor.getColumnIndexOrThrow("album")),
                        cursor.getString(cursor.getColumnIndexOrThrow("genre")),
                        cursor.getInt(cursor.getColumnIndexOrThrow("duration_ms"))
                ));
            }
        } catch (Exception e) {
            Log.w(DEBUG_TAG, "sqlite_track_load_failed error=" + e.getMessage());
        } finally {
            closeQuietly(cursor);
        }
        TrackStore.sort(tracks);
        return tracks;
    }

    void saveTracks(List<Track> tracks) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            saveTracks(db, tracks);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    void updateDuration(String uri, int durationMs) {
        ContentValues values = new ContentValues();
        values.put("duration_ms", durationMs);
        getWritableDatabase().update("tracks", values, "uri=?", new String[]{uri});
    }

    HashSet<String> loadFavorites() {
        HashSet<String> favorites = new HashSet<>();
        Cursor cursor = null;
        try {
            cursor = getReadableDatabase().query("favorites", new String[]{"uri"}, null, null, null, null, null);
            while (cursor.moveToNext()) {
                favorites.add(cursor.getString(0));
            }
        } finally {
            closeQuietly(cursor);
        }
        return favorites;
    }

    void saveFavorites(Set<String> favorites) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            saveFavorites(db, favorites);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    ArrayList<Playlist> loadPlaylists() {
        ArrayList<Playlist> playlists = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor playlistsCursor = null;
        try {
            playlistsCursor = db.query("playlists", new String[]{"id", "name"}, null, null, null, null, "position ASC, id ASC");
            while (playlistsCursor.moveToNext()) {
                long id = playlistsCursor.getLong(0);
                Playlist playlist = new Playlist(playlistsCursor.getString(1));
                Cursor songsCursor = null;
                try {
                    songsCursor = db.query("playlist_tracks", new String[]{"uri"}, "playlist_id=?", new String[]{String.valueOf(id)}, null, null, "position ASC");
                    while (songsCursor.moveToNext()) {
                        playlist.uris.add(songsCursor.getString(0));
                    }
                } finally {
                    closeQuietly(songsCursor);
                }
                playlists.add(playlist);
            }
        } finally {
            closeQuietly(playlistsCursor);
        }
        return playlists;
    }

    void savePlaylists(List<Playlist> playlists) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            savePlaylists(db, playlists);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    private static void saveTracks(SQLiteDatabase db, List<Track> tracks) {
        db.delete("tracks", null, null);
        for (Track track : tracks) {
            ContentValues values = new ContentValues();
            values.put("uri", track.uri);
            values.put("title", track.title);
            values.put("artist", track.artist);
            values.put("album", track.album);
            values.put("genre", track.genre);
            values.put("duration_ms", track.durationMs);
            db.insertWithOnConflict("tracks", null, values, SQLiteDatabase.CONFLICT_REPLACE);
        }
    }

    private static void saveFavorites(SQLiteDatabase db, Set<String> favorites) {
        db.delete("favorites", null, null);
        for (String uri : favorites) {
            ContentValues values = new ContentValues();
            values.put("uri", uri);
            db.insertWithOnConflict("favorites", null, values, SQLiteDatabase.CONFLICT_REPLACE);
        }
    }

    private static void savePlaylists(SQLiteDatabase db, List<Playlist> playlists) {
        db.delete("playlist_tracks", null, null);
        db.delete("playlists", null, null);
        for (int index = 0; index < playlists.size(); index++) {
            Playlist playlist = playlists.get(index);
            ContentValues values = new ContentValues();
            values.put("name", PlaylistManager.cleanName(playlist.name));
            values.put("position", index);
            long id = db.insert("playlists", null, values);
            for (int songIndex = 0; songIndex < playlist.uris.size(); songIndex++) {
                ContentValues song = new ContentValues();
                song.put("playlist_id", id);
                song.put("uri", playlist.uris.get(songIndex));
                song.put("position", songIndex);
                db.insertWithOnConflict("playlist_tracks", null, song, SQLiteDatabase.CONFLICT_REPLACE);
            }
        }
    }

    private static long countRows(SQLiteDatabase db, String table) {
        Cursor cursor = null;
        try {
            cursor = db.rawQuery("SELECT COUNT(*) FROM " + table, null);
            return cursor.moveToFirst() ? cursor.getLong(0) : 0;
        } finally {
            closeQuietly(cursor);
        }
    }

    private static void closeQuietly(Cursor cursor) {
        if (cursor != null) {
            cursor.close();
        }
    }
}
