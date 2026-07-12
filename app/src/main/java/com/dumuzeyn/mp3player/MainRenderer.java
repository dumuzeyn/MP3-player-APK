package com.dumuzeyn.mp3player;

final class MainRenderer {
    private final MainActivityCore host;
    private final SongsMenuRenderer songsRenderer;
    private final FavoritesMenuRenderer favoritesRenderer;
    private final PlaylistsMenuRenderer playlistsRenderer;
    private final MenuRenderer genresRenderer;
    private final MenuRenderer artistsRenderer;
    private final MenuRenderer albumsRenderer;
    private final MenuRenderer settingsRenderer;

    MainRenderer(MainActivityCore host) {
        this.host = host;
        this.songsRenderer = new SongsMenuRenderer(host);
        this.favoritesRenderer = new FavoritesMenuRenderer(host);
        this.playlistsRenderer = new PlaylistsMenuRenderer(host);
        this.genresRenderer = new GenresMenuRenderer(host);
        this.artistsRenderer = new ArtistsMenuRenderer(host);
        this.albumsRenderer = new AlbumsMenuRenderer(host);
        this.settingsRenderer = new SettingsMenuRenderer(host);
    }

    void loadMenuData() {
        songsRenderer.loadSongs();
        favoritesRenderer.loadFavorites();
        playlistsRenderer.loadPlaylists();
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
        if (host.tabIndex == 3) {
            return genresRenderer;
        }
        if (host.tabIndex == 4) {
            return artistsRenderer;
        }
        if (host.tabIndex == 5) {
            return albumsRenderer;
        }
        if (host.tabIndex == 6) {
            return settingsRenderer;
        }
        return songsRenderer;
    }
}
