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
        if (!hasMiniPlayer()) {
            return;
        }
        Track track = currentTrack();
        if (track == null || isOverlayOpen()) {
            hideMiniPlayer();
            return;
        }
        bindMiniPlayer(track);
        showMiniPlayer();
    }

    private boolean hasMiniPlayer() {
        return host.miniPlayer != null;
    }

    private Track currentTrack() {
        if (host.currentIndex < 0 || host.currentIndex >= host.tracks.size()) {
            return null;
        }
        return host.tracks.get(host.currentIndex);
    }

    private boolean isOverlayOpen() {
        return host.overlayHost.getChildCount() > 0;
    }

    private void bindMiniPlayer(Track track) {
        host.miniTitle.setText(track.title);
        host.miniSub.setText(track.artist);
        host.miniButton.setText(playPauseLabel());
    }

    private String playPauseLabel() {
        return host.playing ? "Ⅱ" : "▶";
    }

    private void hideMiniPlayer() {
        host.miniPlayer.setVisibility(View.GONE);
    }

    private void showMiniPlayer() {
        host.miniPlayer.setVisibility(View.VISIBLE);
    }
}
