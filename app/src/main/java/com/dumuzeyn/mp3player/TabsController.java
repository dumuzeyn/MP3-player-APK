package com.dumuzeyn.mp3player;

import android.view.MotionEvent;

final class TabsController {
    private final MainActivityCore host;

    TabsController(MainActivityCore host) {
        this.host = host;
    }

    void buildTabs() {
        host.buildTabsInternal();
    }

    int directionTo(int targetIndex) {
        return host.tabDirectionToInternal(targetIndex);
    }

    boolean isInsideTabs(MotionEvent event) {
        return host.isInsideTabsInternal(event);
    }
}
