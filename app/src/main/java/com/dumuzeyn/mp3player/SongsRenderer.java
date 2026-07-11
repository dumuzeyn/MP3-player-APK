package com.dumuzeyn.mp3player;

import java.util.ArrayList;

final class SongsRenderer {
    private final MainActivityCore host;

    SongsRenderer(MainActivityCore host) {
        this.host = host;
    }

    void render(ArrayList<Track> tracks) {
        host.renderSongsInternal(tracks);
    }
}
