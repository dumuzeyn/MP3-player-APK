package com.dumuzeyn.mp3player;

import java.util.ArrayList;

final class SongsRenderer {
    private final MainActivity host;

    SongsRenderer(MainActivity host) {
        this.host = host;
    }

    void render(ArrayList<Track> tracks) {
        host.renderSongsInternal(tracks);
    }
}
