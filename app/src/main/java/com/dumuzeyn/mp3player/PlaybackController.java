package com.dumuzeyn.mp3player;

final class PlaybackController {
    private final MainActivity host;

    PlaybackController(MainActivity host) {
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
