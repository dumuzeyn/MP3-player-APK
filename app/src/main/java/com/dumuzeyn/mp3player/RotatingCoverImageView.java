package com.dumuzeyn.mp3player;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.graphics.Outline;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.view.animation.DecelerateInterpolator;
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
    private String boundTrackUri = "";
    private String lastObservedTrackUri = "";
    private boolean seeking;
    private int seekStartPosition;
    private int lastSeekPosition;

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
        String nextTrackUri = track == null || track.uri == null ? "" : track.uri;
        if (!nextTrackUri.equals(this.boundTrackUri)) {
            seeking = false;
            stopRotation(true);
            this.boundTrackUri = nextTrackUri;
        }
        this.trackUris.clear();
        this.sourceTracks.clear();
        this.requireActiveQueue = false;
        if (!nextTrackUri.isEmpty()) {
            this.trackUris.add(nextTrackUri);
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
        if (!currentUri.isEmpty() && !currentUri.equals(this.lastObservedTrackUri)) {
            if (!this.lastObservedTrackUri.isEmpty()) {
                stopRotation(true);
            }
            this.lastObservedTrackUri = currentUri;
        }
        boolean shouldRotate = host.circularCovers && host.playing
                && host.currentIndex >= 0 && host.currentIndex < host.tracks.size()
                && trackUris.contains(host.tracks.get(host.currentIndex).uri)
                && (!requireActiveQueue || host.isCurrentCollection(sourceTracks));
        if (seeking) {
            return;
        }
        if (shouldRotate && isAttachedToWindow()) {
            startRotation();
        } else {
            stopRotation(!host.circularCovers);
        }
    }

    void beginSeekSpin(int positionMs) {
        if (!host.circularCovers) {
            return;
        }
        seeking = true;
        seekStartPosition = positionMs;
        lastSeekPosition = positionMs;
        stopRotation(false);
    }

    void updateSeekSpin(int positionMs) {
        if (!seeking || !host.circularCovers) {
            return;
        }
        int deltaMs = positionMs - lastSeekPosition;
        if (deltaMs == 0) {
            return;
        }
        float deltaDegrees = clamp(deltaMs * 0.036f, -270.0f, 270.0f);
        setRotation(getRotation() + deltaDegrees);
        lastSeekPosition = positionMs;
    }

    void endSeekSpin(int positionMs) {
        if (!seeking) {
            return;
        }
        updateSeekSpin(positionMs);
        seeking = false;
        if (!host.circularCovers) {
            updatePlaybackState();
            return;
        }
        int totalDeltaMs = positionMs - seekStartPosition;
        if (totalDeltaMs == 0) {
            updatePlaybackState();
            return;
        }
        float direction = totalDeltaMs > 0 ? 1.0f : -1.0f;
        float turns = clamp(Math.abs(totalDeltaMs) / 15000.0f, 0.45f, 3.0f);
        float start = getRotation();
        ValueAnimator animator = ValueAnimator.ofFloat(start, start + direction * turns * 360.0f);
        rotationAnimator = animator;
        animator.setDuration((long) clamp(220.0f + turns * 150.0f, 280.0f, 720.0f));
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(value -> setRotation((Float) value.getAnimatedValue()));
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (rotationAnimator != animator) {
                    return;
                }
                rotationAnimator = null;
                setRotation(getRotation() % 360.0f);
                updatePlaybackState();
            }
        });
        animator.start();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        updatePlaybackState();
    }

    @Override
    protected void onDetachedFromWindow() {
        seeking = false;
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
        ValueAnimator animator = this.rotationAnimator;
        this.rotationAnimator = null;
        if (animator != null) {
            animator.cancel();
        }
        if (reset) {
            setRotation(0.0f);
        }
    }

    private static float clamp(float value, float minimum, float maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}
