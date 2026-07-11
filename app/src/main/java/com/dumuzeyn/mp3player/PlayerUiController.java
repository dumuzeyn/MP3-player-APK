package com.dumuzeyn.mp3player;

import android.view.MotionEvent;

final class PlayerUiController {
    private final MiniPlayerController miniPlayerController;
    private final FullPlayerController fullPlayerController;

    PlayerUiController(MainActivityCore host) {
        this.miniPlayerController = new MiniPlayerController(host);
        this.fullPlayerController = new FullPlayerController(host);
    }

    void buildMini() {
        miniPlayerController.build();
    }

    void openFullPlayer() {
        fullPlayerController.open();
    }

    void renderFullPlayerSheet() {
        fullPlayerController.open();
    }

    void updateMini() {
        miniPlayerController.updateState();
    }

    void updateMiniState() {
        miniPlayerController.updateState();
    }

    boolean isInsideMiniPlayer(MotionEvent event) {
        return miniPlayerController.isInsideMiniPlayer(event);
    }
}
