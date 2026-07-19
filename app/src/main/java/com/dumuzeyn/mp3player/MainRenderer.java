package com.dumuzeyn.mp3player;

import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ScrollView;
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
        final ScrollView targetScroll = host.contentScroll;
        if (scrollY <= 0) {
            targetScroll.scrollTo(0, 0);
            targetScroll.setVisibility(View.VISIBLE);
            return;
        }
        targetScroll.setVisibility(View.INVISIBLE);
        targetScroll.getViewTreeObserver().addOnPreDrawListener(
                new ViewTreeObserver.OnPreDrawListener() {
                    @Override
                    public boolean onPreDraw() {
                        ViewTreeObserver observer = targetScroll.getViewTreeObserver();
                        if (observer.isAlive()) {
                            observer.removeOnPreDrawListener(this);
                        }
                        if (host.contentScroll != targetScroll
                                || !targetKey.equals(menuKey(host.tabIndex, host.search))) {
                            targetScroll.setVisibility(View.VISIBLE);
                            return true;
                        }
                        targetScroll.scrollTo(0, scrollY);
                        targetScroll.setVisibility(View.VISIBLE);
                        return false;
                    }
                });
    }

    private String menuKey(int tabIndex, String search) {
        return tabIndex + "\n" + (search == null ? "" : search);
    }

    int renderPreview(android.widget.LinearLayout target, int targetIndex, String targetSearch) {
        android.widget.LinearLayout previousList = host.list;
        ButtonState previousButton = new ButtonState(host.sourcePlayButton);
        SongsRenderer.BatchState previousBatchState = host.songsRenderer.captureBatchState();
        int previousTab = host.tabIndex;
        String previousSearch = host.search;
        boolean previousPreview = host.renderingTabPreview;
        int scrollY = scrollPositionFor(targetIndex, targetSearch);
        try {
            host.list = target;
            host.tabIndex = targetIndex;
            host.search = targetSearch == null ? "" : targetSearch;
            host.renderingTabPreview = true;
            host.sourcePlayButton = null;
            target.removeAllViews();
            host.renderSectionHeader();
            MenuRenderer renderer = rendererForTab(targetIndex);
            renderer.render();
            if (targetIndex <= 1) {
                host.songsRenderer.prepareForScrollRestore(scrollY);
            }
            if (renderer.needsMiniSpacer()) {
                host.addMiniSpacerIfNeeded();
            }
        } finally {
            host.list = previousList;
            host.tabIndex = previousTab;
            host.search = previousSearch;
            host.renderingTabPreview = previousPreview;
            host.sourcePlayButton = previousButton.button;
            host.songsRenderer.restoreBatchState(previousBatchState);
        }
        return scrollY;
    }

    private int scrollPositionFor(int tabIndex, String search) {
        Integer position = scrollPositions.get(menuKey(tabIndex, search));
        return position == null ? 0 : Math.max(0, position);
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
