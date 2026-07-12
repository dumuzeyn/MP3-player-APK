package com.dumuzeyn.mp3player;

import java.util.ArrayList;

final class Playlist {
    String name;
    final ArrayList<String> uris = new ArrayList<>();

    Playlist(String name) {
        this.name = name;
    }
}
