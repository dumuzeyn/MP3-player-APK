package com.dumuzeyn.mp3player;

import android.view.MotionEvent;
import android.view.View;

final class PlayerUiController {
    private final MainActivityCore host;
    private final MiniPlayerController miniPlayerController;
    private final FullPlayerController fullPlayerController;

    PlayerUiController(MainActivityCore host) {
        this.host = host;
        this.miniPlayerController = new MiniPlayerController(host);
        this.fullPlayerController = new FullPlayerController(host);
    }

    void buildMini() {
        miniPlayerController.build();
    }

    void openFullPlayer() {
        fullPlayerController.open();
    }

    void updateMini() {
        miniPlayerController.updateState();
    }

    void syncPlaybackUi() {
        miniPlayerController.updateState();
        if (fullPlayerController.isOpen()) {
            fullPlayerController.refresh();
        }
    }

    boolean isInsideMiniPlayer(MotionEvent event) {
        return miniPlayerController.isInsideMiniPlayer(event);
    }

    boolean closeFullPlayerIfTop(View top) {
        return fullPlayerController.closeIfTop(top);
    }
}
