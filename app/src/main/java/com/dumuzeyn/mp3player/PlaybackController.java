package com.dumuzeyn.mp3player;

final class PlaybackController {
    private final MainActivityCore host;

    PlaybackController(MainActivityCore host) {
        this.host = host;
    }

    void playTrack(Track track) {
        host.playTrackInternal(track);
    }

    void playTrack(Track track, boolean refreshList) {
        host.playTrackInternal(track, refreshList);
    }

    void toggleCurrent() {
        host.toggleCurrentInternal();
    }

    void next() {
        host.nextInternal();
    }

    void previous() {
        host.previousInternal();
    }
}
