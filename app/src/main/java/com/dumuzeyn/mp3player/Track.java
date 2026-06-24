package com.dumuzeyn.mp3player;

import android.net.Uri;

public class Track {
    public final String album;
    public final String artist;
    public final String genre;
    public final String title;
    public final String uri;

    public Track(String uri, String title, String artist) {
        this(uri, title, artist, "Неизвестный альбом", "Неизвестный жанр");
    }

    public Track(String uri, String title, String artist, String album, String genre) {
        this.uri = uri;
        this.title = title;
        this.artist = artist;
        this.album = album;
        this.genre = genre;
    }

    public Uri asUri() {
        return Uri.parse(this.uri);
    }
}
