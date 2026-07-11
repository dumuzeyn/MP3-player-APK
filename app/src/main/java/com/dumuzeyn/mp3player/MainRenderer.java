package com.dumuzeyn.mp3player;

final class MainRenderer {
    private final MainActivityCore host;
    private final MenuRenderer songsRenderer;
    private final MenuRenderer favoritesRenderer;
    private final MenuRenderer playlistsRenderer;
    private final MenuRenderer groupsRenderer;
    private final MenuRenderer settingsRenderer;

    MainRenderer(MainActivityCore host) {
        this.host = host;
        this.songsRenderer = new SongsMenuRenderer(host);
        this.favoritesRenderer = new FavoritesMenuRenderer(host);
        this.playlistsRenderer = new PlaylistsMenuRenderer(host);
        this.groupsRenderer = new GroupsMenuRenderer(host);
        this.settingsRenderer = new SettingsMenuRenderer(host);
    }

    void render() {
        host.refreshTabs();
        host.songRenderGeneration++;
        host.list.removeAllViews();
        host.songRows.clear();
        host.sourcePlayButton = null;
        host.renderSectionHeader();
        MenuRenderer renderer = rendererForTab();
        renderer.render();
        if (renderer.needsMiniSpacer()) {
            host.addMiniSpacerIfNeeded();
        }
        host.updateMini();
    }

    private MenuRenderer rendererForTab() {
        if (host.tabIndex == 0) {
            return songsRenderer;
        }
        if (host.tabIndex == 1) {
            return favoritesRenderer;
        }
        if (host.tabIndex == 2) {
            return playlistsRenderer;
        }
        if (host.tabIndex == 6) {
            return settingsRenderer;
        }
        return groupsRenderer;
    }
}
