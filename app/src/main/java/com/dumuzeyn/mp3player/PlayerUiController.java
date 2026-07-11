package com.dumuzeyn.mp3player;

final class PlayerUiController {
    private final MainActivityCore host;

    PlayerUiController(MainActivityCore host) {
        this.host = host;
    }

    void openFullPlayer() {
        host.openFullPlayerInternal();
    }

    void updateMini() {
        host.updateMiniInternal();
    }
}
