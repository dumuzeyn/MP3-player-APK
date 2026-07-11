package com.dumuzeyn.mp3player;

import android.widget.FrameLayout;
import android.widget.ImageView;

final class FrameLayoutCover extends FrameLayout {
    private final MainActivityCore host;
    private final ImageView front;
    private final ImageView back;
    private boolean frontVisible = true;
    private String currentUri = "";
    private int fallback;

    FrameLayoutCover(MainActivityCore host) {
        super(host);
        this.host = host;
        this.front = host.coverView();
        this.back = host.coverView();
        this.back.setAlpha(0.0f);
        addView(this.front, new FrameLayout.LayoutParams(-1, -1));
        addView(this.back, new FrameLayout.LayoutParams(-1, -1));
    }

    void setFallback(int fallback) {
        this.fallback = fallback;
        this.front.setBackgroundColor(fallback);
        this.back.setBackgroundColor(fallback);
    }

    void bindTrack(Track track, int generation) {
        if (track == null || track.uri.equals(this.currentUri)) {
            return;
        }
        this.currentUri = track.uri;
        ImageView visible = this.frontVisible ? this.front : this.back;
        ImageView incoming = this.frontVisible ? this.back : this.front;
        incoming.animate().cancel();
        visible.animate().cancel();
        incoming.setAlpha(0.0f);
        host.loadCover(incoming, track, this.fallback);
        incoming.animate().alpha(1.0f).setDuration(host.animations ? 520L : 0L).withEndAction(new Runnable() {
            @Override
            public void run() {
                frontVisible = !frontVisible;
            }
        }).start();
        visible.animate().alpha(0.0f).setDuration(host.animations ? 520L : 0L).start();
    }
}
