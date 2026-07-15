package com.dumuzeyn.mp3player;

import android.animation.ValueAnimator;
import android.graphics.Outline;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import java.util.ArrayList;
import java.util.HashSet;

final class RotatingCoverImageView extends ImageView {
    private final MainActivityCore host;
    private final HashSet<String> trackUris = new HashSet<>();
    private ArrayList<Track> sourceTracks = new ArrayList<>();
    private boolean requireActiveQueue;
    private ValueAnimator rotationAnimator;
    private String lastObservedTrackUri = "";

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
        stopRotation(true);
        this.trackUris.clear();
        this.sourceTracks.clear();
        this.requireActiveQueue = false;
        this.lastObservedTrackUri = track == null || track.uri == null ? "" : track.uri;
        if (track != null && track.uri != null) {
            this.trackUris.add(track.uri);
        }
        updatePlaybackState();
    }

    void bindTracks(ArrayList<Track> tracks) {
        bindSourceTracks(tracks, false);
    }

    void bindPlaylistTracks(ArrayList<Track> tracks) {
        bindSourceTracks(tracks, true);
    }

    private void bindSourceTracks(ArrayList<Track> tracks, boolean requireActiveQueue) {
        this.trackUris.clear();
        this.sourceTracks = tracks == null ? new ArrayList<>() : new ArrayList<>(tracks);
        this.requireActiveQueue = requireActiveQueue;
        if (tracks != null) {
            for (Track track : tracks) {
                if (track != null && track.uri != null) {
                    this.trackUris.add(track.uri);
                }
            }
        }
        updatePlaybackState();
    }

    void updatePlaybackState() {
        invalidateOutline();
        String currentUri = "";
        if (host.currentIndex >= 0 && host.currentIndex < host.tracks.size()
                && host.tracks.get(host.currentIndex) != null
                && host.tracks.get(host.currentIndex).uri != null) {
            currentUri = host.tracks.get(host.currentIndex).uri;
        }
        if (!currentUri.equals(this.lastObservedTrackUri)) {
            stopRotation(true);
            this.lastObservedTrackUri = currentUri;
        }
        boolean shouldRotate = host.circularCovers && host.playing
                && host.currentIndex >= 0 && host.currentIndex < host.tracks.size()
                && trackUris.contains(host.tracks.get(host.currentIndex).uri)
                && (!requireActiveQueue || host.isCurrentCollection(sourceTracks));
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
        stopRotation(true);
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
