package com.rasul.mp3player;

import android.net.Uri;

public class Track {
    public final String uri;
    public final String title;
    public final String artist;

    public Track(String uri, String title, String artist) {
        this.uri = uri;
        this.title = title;
        this.artist = artist;
    }

    public Uri asUri() {
        return Uri.parse(uri);
    }
}
