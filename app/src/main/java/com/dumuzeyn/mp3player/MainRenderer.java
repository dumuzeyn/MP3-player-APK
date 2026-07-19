package com.dumuzeyn.mp3player;

import java.util.HashMap;
import java.util.Map;

final class MainRenderer {
    private final MainActivityCore host;
    private final SongsMenuRenderer songsRenderer;
    private final FavoritesMenuRenderer favoritesRenderer;
    private final PlaylistsMenuRenderer playlistsRenderer;
    private final MenuRenderer genresRenderer;
    private final MenuRenderer artistsRenderer;
    private final MenuRenderer albumsRenderer;
    private final MenuRenderer settingsRenderer;
    private final Map<String, Integer> scrollPositions = new HashMap<>();
    private String renderedMenuKey;

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
        rememberCurrentScrollPosition();
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
        restoreCurrentScrollPosition();
        host.updateMini();
    }

    void captureScrollBeforeUiRebuild() {
        rememberCurrentScrollPosition();
        renderedMenuKey = null;
    }

    private void rememberCurrentScrollPosition() {
        if (renderedMenuKey == null || host.contentScroll == null) {
            return;
        }
        scrollPositions.put(renderedMenuKey, Math.max(0, host.contentScroll.getScrollY()));
    }

    private void restoreCurrentScrollPosition() {
        renderedMenuKey = menuKey(host.tabIndex, host.search);
        final String targetKey = renderedMenuKey;
        if (host.contentScroll == null) {
            return;
        }
        int scrollY = scrollPositions.containsKey(renderedMenuKey)
                ? scrollPositions.get(renderedMenuKey) : 0;
        if (host.tabIndex <= 1) {
            host.songsRenderer.prepareForScrollRestore(scrollY);
        }
        host.contentScroll.post(() -> {
            if (host.contentScroll != null && targetKey.equals(menuKey(host.tabIndex, host.search))) {
                host.contentScroll.scrollTo(0, scrollY);
            }
        });
    }

    private String menuKey(int tabIndex, String search) {
        return tabIndex + "\n" + (search == null ? "" : search);
    }

    void renderPreview(android.widget.LinearLayout target, int targetIndex, String targetSearch) {
        android.widget.LinearLayout previousList = host.list;
        ButtonState previousButton = new ButtonState(host.sourcePlayButton);
        int previousTab = host.tabIndex;
        String previousSearch = host.search;
        boolean previousPreview = host.renderingTabPreview;
        try {
            host.list = target;
            host.tabIndex = targetIndex;
            host.search = targetSearch == null ? "" : targetSearch;
            host.renderingTabPreview = true;
            host.sourcePlayButton = null;
            target.removeAllViews();
            host.renderSectionHeader();
            rendererForTab(targetIndex).render();
        } finally {
            host.list = previousList;
            host.tabIndex = previousTab;
            host.search = previousSearch;
            host.renderingTabPreview = previousPreview;
            host.sourcePlayButton = previousButton.button;
        }
    }

    private MenuRenderer rendererForTab() {
        return rendererForTab(host.tabIndex);
    }

    private MenuRenderer rendererForTab(int tabIndex) {
        if (tabIndex == 0) {
            return songsRenderer;
        }
        if (tabIndex == 1) {
            return favoritesRenderer;
        }
        if (tabIndex == 2) {
            return playlistsRenderer;
        }
        if (tabIndex == 3) {
            return genresRenderer;
        }
        if (tabIndex == 4) {
            return artistsRenderer;
        }
        if (tabIndex == 5) {
            return albumsRenderer;
        }
        if (tabIndex == 6) {
            return settingsRenderer;
        }
        return songsRenderer;
    }

    private static final class ButtonState {
        final android.widget.Button button;

        ButtonState(android.widget.Button button) {
            this.button = button;
        }
    }
}
