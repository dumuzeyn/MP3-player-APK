package com.dumuzeyn.mp3player;

import android.view.View;

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

    void updateMiniState() {
        if (host.miniPlayer == null) {
            return;
        }
        if (host.currentIndex < 0 || host.currentIndex >= host.tracks.size() || host.overlayHost.getChildCount() > 0) {
            host.miniPlayer.setVisibility(View.GONE);
            return;
        }
        Track track = host.tracks.get(host.currentIndex);
        host.miniTitle.setText(track.title);
        host.miniSub.setText(track.artist);
        host.miniButton.setText(host.playing ? "Ⅱ" : "▶");
        host.miniPlayer.setVisibility(View.VISIBLE);
    }
}
