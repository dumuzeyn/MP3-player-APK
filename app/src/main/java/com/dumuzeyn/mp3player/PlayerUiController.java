package com.dumuzeyn.mp3player;

final class PlayerUiController {
    private final MainActivity host;

    PlayerUiController(MainActivity host) {
        this.host = host;
    }

    void openFullPlayer() {
        host.openFullPlayerInternal();
    }

    void updateMini() {
        host.updateMiniInternal();
    }
}
