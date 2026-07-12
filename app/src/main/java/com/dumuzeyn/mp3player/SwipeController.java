package com.dumuzeyn.mp3player;

import android.view.MotionEvent;
import android.view.animation.DecelerateInterpolator;

final class SwipeController {
    private static final int SWIPE_START_DP = 21;
    private static final int SWIPE_COMMIT_DP = 52;

    private final MainActivityCore host;
    private float startX;
    private float startY;
    private boolean startedOnTabs;
    private boolean startedOnMiniPlayer;
    private boolean consuming;

    SwipeController(MainActivityCore host) {
        this.host = host;
    }

    boolean handle(MotionEvent event) {
        if (host.tabs == null || host.tabs.length == 0) {
            return false;
        }
        if (host.overlayHost != null && host.overlayHost.getChildCount() > 0) {
            return false;
        }
        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN) {
            startedOnTabs = host.tabsController.isInsideTabs(event);
            startedOnMiniPlayer = host.playerUiController.isInsideMiniPlayer(event);
            consuming = false;
            startX = event.getX();
            startY = event.getY();
            host.tabsController.cancelScrollAnimation();
            return false;
        }
        if (action == MotionEvent.ACTION_MOVE) {
            if (startedOnTabs || startedOnMiniPlayer) {
                return false;
            }
            float deltaX = event.getX() - startX;
            float deltaY = event.getY() - startY;
            if (!consuming && Math.abs(deltaX) > host.dp(SWIPE_START_DP) && Math.abs(deltaX) > Math.abs(deltaY)) {
                consuming = true;
                if (host.root != null && host.root.getParent() != null) {
                    host.root.getParent().requestDisallowInterceptTouchEvent(true);
                }
            }
            if (consuming && host.animations && host.list != null) {
                float width = host.root == null || host.root.getWidth() <= 0
                        ? host.getResources().getDisplayMetrics().widthPixels
                        : host.root.getWidth();
                float drag = Math.max(-width * 0.82f, Math.min(width * 0.82f, deltaX));
                host.list.setTranslationX(drag);
                host.list.setAlpha(Math.max(0.72f, 1.0f - (Math.abs(drag) / width) * 0.24f));
            }
            return consuming;
        }
        if (action != MotionEvent.ACTION_UP && action != MotionEvent.ACTION_CANCEL) {
            return false;
        }
        if (startedOnTabs || startedOnMiniPlayer) {
            startedOnTabs = false;
            startedOnMiniPlayer = false;
            consuming = false;
            return false;
        }
        float deltaX = event.getX() - startX;
        float deltaY = event.getY() - startY;
        boolean wasConsuming = consuming;
        consuming = false;
        if (action == MotionEvent.ACTION_UP
                && Math.abs(deltaX) > host.dp(SWIPE_COMMIT_DP)
                && Math.abs(deltaX) > Math.abs(deltaY)) {
            int length = host.tabs.length;
            int target = deltaX < 0.0f
                    ? (host.tabIndex + 1) % length
                    : (host.tabIndex - 1 + length) % length;
            host.switchTabAnimated(target, deltaX < 0.0f ? 1 : -1);
            return true;
        }
        resetListPosition(wasConsuming);
        return wasConsuming;
    }

    private void resetListPosition(boolean wasConsuming) {
        if (!wasConsuming || host.list == null) {
            return;
        }
        if (host.animations) {
            host.list.animate().translationX(0.0f).alpha(1.0f).setDuration(90L)
                    .setInterpolator(new DecelerateInterpolator()).start();
        } else {
            host.list.setTranslationX(0.0f);
            host.list.setAlpha(1.0f);
        }
    }
}
