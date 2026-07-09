package com.dumuzeyn.mp3player;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class TrackStore {
    private static final int MAX_TEXT_LENGTH = 160;
    private static final String DEBUG_TAG = "MP3PlayerDebug";

    private TrackStore() {
    }

    public static ArrayList<Track> load(Context context) {
        LibraryDatabase.migrateLegacyIfNeeded(context);
        LibraryDatabase database = new LibraryDatabase(context);
        try {
            return database.loadTracks();
        } finally {
            database.close();
        }
    }

    static ArrayList<Track> loadFromJson(String raw) {
        ArrayList<Track> tracks = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(raw == null ? "[]" : raw);
            for (int index = 0; index < array.length(); index++) {
                JSONObject item = array.getJSONObject(index);
                tracks.add(new Track(
                        item.getString("uri"),
                        item.optString("title", "Song"),
                        item.optString("artist", "Unknown artist"),
                        item.optString("album", "Unknown album"),
                        item.optString("genre", "Unknown genre"),
                        item.optInt("durationMs", 0)
                ));
            }
        } catch (Exception e) {
            Log.w(DEBUG_TAG, "track_load_failed error=" + e.getMessage());
            tracks.clear();
        }
        sort(tracks);
        return tracks;
    }

    public static void save(Context context, List<Track> tracks) {
        LibraryDatabase database = new LibraryDatabase(context);
        try {
            database.saveTracks(tracks);
        } finally {
            database.close();
        }
    }

    public static Track fromUri(Context context, Uri uri) {
        boolean canOpen = canOpenForRead(context, uri);
        if (!canOpen) {
            Log.w(DEBUG_TAG, "add_track uri=" + uri + " canOpen=false");
            return null;
        }

        Metadata metadata = readMetadata(context, uri);
        String title = metadata.title;
        if (title == null || title.trim().isEmpty()) {
            String path = uri.getLastPathSegment();
            title = path == null ? "Song" : path.substring(path.lastIndexOf('/') + 1);
        }
        String artist = isBlank(metadata.artist) ? "Unknown artist" : metadata.artist;
        String album = isBlank(metadata.album) ? "Unknown album" : metadata.album;
        String genre = isBlank(metadata.genre) ? "Unknown genre" : metadata.genre;

        Track track = new Track(uri.toString(), cleanText(title), cleanText(artist), cleanText(album), cleanText(genre), metadata.durationMs);
        Log.i(DEBUG_TAG, "add_track uri=" + uri + " title=" + track.title + " duration=" + track.durationMs + " canOpen=true");
        return track;
    }

    public static Track refreshMetadata(Context context, Track oldTrack) {
        Track fresh = fromUri(context, Uri.parse(oldTrack.uri));
        if (fresh == null) {
            return oldTrack;
        }
        String title = isBlank(fresh.title) ? oldTrack.title : fresh.title;
        String artist = isBlank(fresh.artist) ? oldTrack.artist : fresh.artist;
        String album = isBlank(fresh.album) ? oldTrack.album : fresh.album;
        String genre = isBlank(fresh.genre) ? oldTrack.genre : fresh.genre;
        int durationMs = fresh.durationMs > 0 ? fresh.durationMs : oldTrack.durationMs;
        return new Track(oldTrack.uri, title, artist, album, genre, durationMs);
    }

    public static boolean canOpenForRead(Context context, Uri uri) {
        AssetFileDescriptor descriptor = null;
        try {
            descriptor = context.getContentResolver().openAssetFileDescriptor(uri, "r");
            return descriptor != null;
        } catch (Exception e) {
            Log.w(DEBUG_TAG, "read_check_failed uri=" + uri + " error=" + e.getMessage());
            return false;
        } finally {
            closeQuietly(descriptor);
        }
    }

    public static void updateDuration(Context context, String uri, int durationMs) {
        if (durationMs <= 0 || uri == null || uri.isEmpty()) {
            return;
        }
        LibraryDatabase database = new LibraryDatabase(context);
        try {
            database.updateDuration(uri, durationMs);
        } finally {
            database.close();
        }
        Log.i(DEBUG_TAG, "duration_updated uri=" + uri + " durationMs=" + durationMs);
    }

    public static void sort(List<Track> tracks) {
        Collections.sort(tracks, new Comparator<Track>() {
            @Override
            public int compare(Track left, Track right) {
                return left.title.toLowerCase(Locale.ROOT).compareTo(right.title.toLowerCase(Locale.ROOT));
            }
        });
    }

    private static Metadata readMetadata(Context context, Uri uri) {
        Metadata metadata = readMetadataDirect(context, uri);
        if (metadata.durationMs <= 0) {
            Metadata fallback = readMetadataWithFileDescriptor(context, uri);
            metadata.mergeMissing(fallback);
        }
        if (metadata.durationMs <= 0) {
            metadata.durationMs = readDurationWithMediaPlayer(context, uri);
        }
        if (metadata.durationMs <= 0) {
            Log.w(DEBUG_TAG, "duration_missing uri=" + uri);
        }
        return metadata;
    }

    private static Metadata readMetadataDirect(Context context, Uri uri) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(context, uri);
            return Metadata.from(retriever);
        } catch (Throwable e) {
            Log.w(DEBUG_TAG, "metadata_direct_failed uri=" + uri + " error=" + e.getMessage());
            return new Metadata();
        } finally {
            releaseQuietly(retriever);
        }
    }

    private static Metadata readMetadataWithFileDescriptor(Context context, Uri uri) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        AssetFileDescriptor descriptor = null;
        try {
            descriptor = context.getContentResolver().openAssetFileDescriptor(uri, "r");
            if (descriptor == null) {
                return new Metadata();
            }
            long declaredLength = descriptor.getDeclaredLength();
            if (declaredLength >= 0) {
                retriever.setDataSource(descriptor.getFileDescriptor(), descriptor.getStartOffset(), declaredLength);
            } else {
                retriever.setDataSource(descriptor.getFileDescriptor());
            }
            return Metadata.from(retriever);
        } catch (Throwable e) {
            Log.w(DEBUG_TAG, "metadata_fd_failed uri=" + uri + " error=" + e.getMessage());
            return new Metadata();
        } finally {
            releaseQuietly(retriever);
            closeQuietly(descriptor);
        }
    }

    private static int readDurationWithMediaPlayer(Context context, Uri uri) {
        MediaPlayer player = new MediaPlayer();
        AssetFileDescriptor descriptor = null;
        try {
            try {
                player.setDataSource(context, uri);
            } catch (Exception directError) {
                Log.w(DEBUG_TAG, "duration_player_direct_failed uri=" + uri + " error=" + directError.getMessage());
                descriptor = context.getContentResolver().openAssetFileDescriptor(uri, "r");
                if (descriptor == null) {
                    return 0;
                }
                long declaredLength = descriptor.getDeclaredLength();
                if (declaredLength >= 0) {
                    player.setDataSource(descriptor.getFileDescriptor(), descriptor.getStartOffset(), declaredLength);
                } else {
                    player.setDataSource(descriptor.getFileDescriptor());
                }
            }
            player.prepare();
            int duration = Math.max(0, player.getDuration());
            Log.i(DEBUG_TAG, "duration_player_fallback uri=" + uri + " durationMs=" + duration);
            return duration;
        } catch (Throwable e) {
            Log.w(DEBUG_TAG, "duration_player_failed uri=" + uri + " error=" + e.getMessage());
            return 0;
        } finally {
            try {
                player.release();
            } catch (Exception ignored) {
            }
            closeQuietly(descriptor);
        }
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

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static int parseDurationMs(String raw) {
        if (isBlank(raw)) {
            return 0;
        }
        try {
            long value = Long.parseLong(raw.trim());
            return value <= 0L ? 0 : (int) Math.min(Integer.MAX_VALUE, value);
        } catch (Exception e) {
            Log.w(DEBUG_TAG, "duration_parse_failed value=" + raw + " error=" + e.getMessage());
            return 0;
        }
    }

    private static void releaseQuietly(MediaMetadataRetriever retriever) {
        try {
            retriever.release();
        } catch (Exception ignored) {
        }
    }

    private static void closeQuietly(AssetFileDescriptor descriptor) {
        if (descriptor == null) {
            return;
        }
        try {
            descriptor.close();
        } catch (Exception ignored) {
        }
    }

    private static final class Metadata {
        String album;
        String artist;
        int durationMs;
        String genre;
        String title;

        static Metadata from(MediaMetadataRetriever retriever) {
            Metadata metadata = new Metadata();
            metadata.title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
            metadata.artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
            metadata.album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM);
            metadata.genre = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE);
            metadata.durationMs = parseDurationMs(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
            return metadata;
        }

        void mergeMissing(Metadata fallback) {
            if (fallback == null) {
                return;
            }
            if (isBlank(this.title)) {
                this.title = fallback.title;
            }
            if (isBlank(this.artist)) {
                this.artist = fallback.artist;
            }
            if (isBlank(this.album)) {
                this.album = fallback.album;
            }
            if (isBlank(this.genre)) {
                this.genre = fallback.genre;
            }
            if (this.durationMs <= 0) {
                this.durationMs = fallback.durationMs;
            }
        }
    }
}
