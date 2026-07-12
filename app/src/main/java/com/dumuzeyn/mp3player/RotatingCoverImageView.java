package com.dumuzeyn.mp3player;

import android.animation.ValueAnimator;
import android.graphics.Outline;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;

final class RotatingCoverImageView extends ImageView {
    private final MainActivityCore host;
    private String trackUri = "";
    private ValueAnimator rotationAnimator;

    RotatingCoverImageView(MainActivityCore host) {
        super(host);
        this.host = host;
        setScaleType(ScaleType.CENTER_CROP);
        setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                float radius = host.circularCovers
                        ? Math.min(view.getWidth(), view.getHeight()) * 0.5f
                        : host.dp(8);
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), radius);
            }
        });
        setClipToOutline(true);
    }

    void bindTrack(Track track) {
        this.trackUri = track == null ? "" : track.uri;
        updatePlaybackState();
    }

    void updatePlaybackState() {
        invalidateOutline();
        boolean shouldRotate = host.circularCovers && host.playing
                && host.currentIndex >= 0 && host.currentIndex < host.tracks.size()
                && host.tracks.get(host.currentIndex).uri.equals(this.trackUri);
        if (shouldRotate && isAttachedToWindow()) {
            startRotation();
        } else {
            stopRotation(!host.circularCovers);
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        updatePlaybackState();
    }

    @Override
    protected void onDetachedFromWindow() {
        stopRotation(false);
        super.onDetachedFromWindow();
    }

    private void startRotation() {
        if (this.rotationAnimator != null && this.rotationAnimator.isRunning()) {
            return;
        }
        float start = getRotation() % 360.0f;
        this.rotationAnimator = ValueAnimator.ofFloat(start, start + 360.0f);
        this.rotationAnimator.setDuration(18000L);
        this.rotationAnimator.setRepeatCount(ValueAnimator.INFINITE);
        this.rotationAnimator.setInterpolator(new LinearInterpolator());
        this.rotationAnimator.addUpdateListener(animator -> setRotation((Float) animator.getAnimatedValue()));
        this.rotationAnimator.start();
    }

    private void stopRotation(boolean reset) {
        if (this.rotationAnimator != null) {
            this.rotationAnimator.cancel();
            this.rotationAnimator = null;
        }
        if (reset) {
            setRotation(0.0f);
        }
    }
}
