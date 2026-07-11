package com.dumuzeyn.mp3player;

final class MainRenderer {
    private final MainActivityCore host;

    MainRenderer(MainActivityCore host) {
        this.host = host;
    }

    void render() {
        host.refreshTabs();
        host.songRenderGeneration++;
        host.list.removeAllViews();
        host.songRows.clear();
        host.sourcePlayButton = null;
        host.renderSectionHeader();
        boolean songTab = host.tabIndex == 0 || host.tabIndex == 1;
        if (host.tabIndex == 0) {
            host.renderSongs(host.libraryListController.filter(host.tracks));
        } else if (host.tabIndex == 1) {
            host.renderSongs(host.libraryListController.filter(host.libraryListController.favoriteTracks()));
        } else if (host.tabIndex == 2) {
            host.renderPlaylists();
        } else if (host.tabIndex == 6) {
            host.renderSettings();
        } else {
            host.renderGroups(host.tabs[host.tabIndex]);
        }
        if (!songTab) {
            host.addMiniSpacerIfNeeded();
        }
        host.updateMini();
    }
}
