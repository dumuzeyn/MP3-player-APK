package com.dumuzeyn.mp3player;

final class GroupsMenuRenderer implements MenuRenderer {
    private final MainActivityCore host;

    GroupsMenuRenderer(MainActivityCore host) {
        this.host = host;
    }

    @Override
    public boolean needsMiniSpacer() {
        return true;
    }

    @Override
    public void render() {
        host.renderGroups(host.tabs[host.tabIndex]);
    }
}
