package com.dumuzeyn.mp3player;

import android.net.Uri;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public final class MediaItemMapper {
    public MediaItem toMediaItem(Track track) {
        MediaMetadata metadata = new MediaMetadata.Builder()
                .setTitle(track.title)
                .setArtist(track.artist)
                .setAlbumTitle(track.album)
                .setArtworkUri(track.asUri())
                .build();
        return new MediaItem.Builder()
                .setMediaId(mediaId(track))
                .setUri(Uri.parse(track.uri))
                .setMediaMetadata(metadata)
                .build();
    }

    public String mediaId(Track track) {
        return stableHash(track == null ? "" : track.uri);
    }

    public static String stableHash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder("track-");
            for (int index = 0; index < 16; index++) {
                result.append(String.format(java.util.Locale.ROOT, "%02x", digest[index]));
            }
            return result.toString();
        } catch (Exception ignored) {
            return "track-" + Integer.toHexString(value.hashCode());
        }
    }
}
