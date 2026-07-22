package com.dumuzeyn.mp3player;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import com.dumuzeyn.mp3player.library.SongDiagnostics;
import com.dumuzeyn.mp3player.playback.service.PlaybackSleepTimer;
import com.dumuzeyn.mp3player.ui.permissions.NotificationPermissionController;
import com.dumuzeyn.mp3player.ui.player.PlaybackTimeFormatter;
import com.dumuzeyn.mp3player.ui.layout.ResponsiveLayoutController;
import java.util.ArrayList;

class MainActivityCore extends AppState {
    static final int COVER_FULL_SIZE = 1024;
    static final int TAB_CYCLES = 5;
    int bg;
    int fg;
    int line;
    LinearLayout list;
    Button miniButton;
    LinearLayout miniPlayer;
    TextView miniSub;
    TextView miniTitle;
    int muted;
    int purple;
    int purpleDark;
    int purpleSoft;
    int yellow;
    int yellowDark;
    int yellowSoft;
    int card;
    int cardStroke;
    int primaryText;
    int secondaryText;
    FrameLayout overlayHost;
    LinearLayout page;
    int panel;
    FrameLayout root;
    LinearLayout tabRow;
    String[] tabs;
    HorizontalScrollView tabsScroll;
    FrameLayout contentHost;
    ScrollView contentScroll;
    private ParticleEffectsView particleEffectsView;
    private final CoverLoader coverLoader = new CoverLoader(this);
    final Handler uiHandler = new Handler(Looper.getMainLooper());
    final Handler playbackHandler = new Handler(Looper.getMainLooper());
    final SongRowStateRegistry songRows = new SongRowStateRegistry();
    final SongsRenderer songsRenderer = new SongsRenderer(this);
    final PlayerUiController playerUiController = new PlayerUiController(this);
    private final SettingsRenderer settingsRenderer = new SettingsRenderer(this);
    final SettingsController settingsController = new SettingsController(this);
    final TabsController tabsController = new TabsController(this);
    private final SwipeController swipeController = new SwipeController(this);
    private final AudioImportController audioImportController = new AudioImportController(this);
    private final UiFactory uiFactory = new UiFactory(this);
    private final HeaderController headerController = new HeaderController(this);
    private final OverlayController overlayController = new OverlayController(this);
    private final DialogController dialogController = new DialogController(this);
    private final BackNavigationController backNavigationController = new BackNavigationController(this);
    final ThemeController themeController = new ThemeController(this);
    private final PlaybackController playbackController = new PlaybackController(this);
    private final PlaybackQueueController playbackQueueController =
            new PlaybackQueueController(this, playbackController);
    final SleepTimerController sleepTimerController = new SleepTimerController(this);
    final EqualizerController equalizerController = new EqualizerController(this);
    final VolumeLevelingController volumeLevelingController = new VolumeLevelingController(this);
    final ParticleSettingsController particleSettingsController = new ParticleSettingsController(this);
    final CoverRotationSettingsController coverRotationSettingsController =
            new CoverRotationSettingsController(this);
    final UninterruptedPlaybackController uninterruptedPlaybackController = new UninterruptedPlaybackController(this);
    final StableVolumeController stableVolumeController = new StableVolumeController(this);
    final BackgroundPlaybackSettingsController backgroundPlaybackSettingsController =
            new BackgroundPlaybackSettingsController(this);
    final PlaylistTickerSettingsController playlistTickerSettingsController = new PlaylistTickerSettingsController(this);
    final CardTransparencyController cardTransparencyController = new CardTransparencyController(this);
    final BackgroundSettingsController backgroundSettingsController = new BackgroundSettingsController(this);
    final LibraryListController libraryListController = new LibraryListController(this);
    final PlaylistController playlistController = new PlaylistController(this);
    private final MainRenderer mainRenderer = new MainRenderer(this);
    private final UiPreferencesStore uiPreferencesStore = new UiPreferencesStore(this);
    final ResponsiveLayoutController responsiveLayoutController =
            new ResponsiveLayoutController(this);
    Button sourcePlayButton;

    interface InputDone {
        void done(String str);
    }

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        SettingsDefaults.resetForVersion243(this);
        this.uiPreferencesStore.load();
        BenchmarkLibrarySeeder.seedIfRequested(this,
                getIntent().getIntExtra(BenchmarkLibrarySeeder.EXTRA_TRACK_COUNT, 0));
        this.sleepTimerEndsAt = PlaybackSleepTimer.readEndsAt(this);
        NotificationPermissionController.requestIfNeeded(this);
        this.mainRenderer.loadMenuData();
        this.playbackController.connect();
        this.themeController.applyPalette();
        buildUi();
        this.songsRenderer.refreshMissingMetadataAsync();
        this.uiHandler.postDelayed(
                this.backgroundPlaybackSettingsController::maybePromptOnce, 900L);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent motionEvent) {
        if (this.particleEffectsView != null) {
            this.particleEffectsView.observeTouch(motionEvent);
        }
        if (this.swipeController.handle(motionEvent)) {
            return true;
        }
        return super.dispatchTouchEvent(motionEvent);
    }

    @Override
    protected void onStop() {
        super.onStop();
        this.themeController.onHostStopped();
    }

    @Override
    protected void onResume() {
        super.onResume();
        this.sleepTimerEndsAt = PlaybackSleepTimer.readEndsAt(this);
        this.playerUiController.syncPlaybackUi();
        refreshAfterTrackChange();
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        this.coverLoader.trimMemory(level);
    }

    @Override
    protected void onDestroy() {
        this.audioImportController.close();
        this.backgroundSettingsController.close();
        this.songsRenderer.close();
        this.uiHandler.removeCallbacksAndMessages(null);
        this.playbackHandler.removeCallbacksAndMessages(null);
        this.playerUiController.onHostDestroyed();
        this.playbackController.release();
        this.coverLoader.close();
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (!this.backNavigationController.handleBack()) {
            super.onBackPressed();
        }
    }

    void restoreTabFromBack(int targetIndex, String previousSearch) {
        int direction = this.tabsController.directionTo(targetIndex);
        this.swipeController.animateToTab(targetIndex, direction, false, previousSearch);
    }

    private boolean english() {
        return "en".equals(this.language);
    }

    String tr(String str, String str2) {
        return english() ? str : str2;
    }

    String tr3(String str, String str2, String str3) {
        return english() ? str : str2;
    }

    String languageName() {
        return english() ? "English" : "Русский";
    }

    private void refreshTabLabels() {
        this.tabs = new String[]{tr3("Songs", "Песни", "♪"), tr3("Favorites", "Избранное", "♥"), tr3("Playlists", "Плейлисты", "▤"), tr3("Genres", "Жанры", "◇"), tr3("Artists", "Исполнители", "♙"), tr3("Albums", "Альбомы", "▣"), tr3("Settings", "Настройки", "⚙")};
        if (this.tabIndex >= this.tabs.length) {
            this.tabIndex = 0;
        }
    }

    void saveState() {
        this.uiPreferencesStore.save();
        LibraryDatabase database = new LibraryDatabase(this);
        try {
            database.saveFavorites(this.favorites);
            database.savePlaylists(this.playlists);
        } finally {
            database.close();
        }
    }

    private void colors() {
        this.themeController.applyPalette();
    }

    void refreshAfterTrackChange() {
        refreshPlaybackChrome();
    }

    private void refreshPlaybackChrome() {
        this.songRows.refresh(new SongRowStateRegistry.StateResolver() {
            @Override
            public Track findTrack(String uri) {
                return MainActivityCore.this.findTrack(uri);
            }

            @Override
            public boolean isCurrent(Track track) {
                return MainActivityCore.this.isCurrent(track);
            }

            @Override
            public boolean isPlaying() {
                return MainActivityCore.this.isPlaybackPlaying();
            }

            @Override
            public int activeColor() {
                return MainActivityCore.this.purple;
            }

            @Override
            public int secondaryActiveColor() {
                return MainActivityCore.this.yellow;
            }

            @Override
            public int inactiveColor() {
                return MainActivityCore.this.purpleSoft;
            }
        });
        this.playlistController.refreshPlaybackState();
        if (this.sourcePlayButton != null) {
            this.sourcePlayButton.setText(isPlayingSource(currentVisibleTracks()) ? "Ⅱ" : "▶");
        }
        updateMini();
    }

    private void buildUi() {
        colors();
        this.themeController.applyWindow();
        refreshTabLabels();
        this.root = new FrameLayout(this);
        this.root.setBackgroundColor(this.mainSolidBackground == 0 ? this.bg : this.mainSolidBackground);
        if (this.mainBackgroundMode == BackgroundSettingsController.MODE_GRADIENT) {
            this.root.addView(new PlayerGradientBackground(this, this.mainGradientStart, this.mainGradientEnd),
                    new FrameLayout.LayoutParams(-1, -1));
        } else if (this.mainBackgroundMode == BackgroundSettingsController.MODE_MEDIA
                && !this.mainBackgroundMediaUri.isEmpty()) {
            this.root.addView(new BackgroundMediaView(this, this.mainBackgroundMediaUri,
                            this.mainBackgroundBlur),
                    new FrameLayout.LayoutParams(-1, -1));
        }
        this.page = new LinearLayout(this);
        this.page.setOrientation(LinearLayout.VERTICAL);
        int pagePadding = this.responsiveLayoutController.pageHorizontalPadding();
        this.page.setPadding(
                pagePadding,
                this.responsiveLayoutController.pageTopPadding(),
                pagePadding,
                0);
        this.root.addView(this.page, this.responsiveLayoutController.mainPageParams());
        buildHeader();
        this.tabsController.buildTabs();
        this.contentHost = new FrameLayout(this);
        ScrollView scrollView = new ScrollView(this);
        this.contentScroll = scrollView;
        scrollView.setOnScrollChangeListener((view, scrollX, scrollY, oldScrollX, oldScrollY) ->
                songsRenderer.loadMoreIfNearBottom());
        this.list = new LinearLayout(this);
        this.list.setOrientation(LinearLayout.VERTICAL);
        scrollView.addView(this.list, new FrameLayout.LayoutParams(-1, -2));
        this.contentHost.addView(scrollView, new FrameLayout.LayoutParams(-1, -1));
        this.page.addView(this.contentHost, new LinearLayout.LayoutParams(-1, 0, 1.0f));
        this.overlayHost = new FrameLayout(this);
        this.root.addView(this.overlayHost, new FrameLayout.LayoutParams(-1, -1));
        buildMiniPlayer();
        this.particleEffectsView = new ParticleEffectsView(this);
        this.root.addView(this.particleEffectsView, new FrameLayout.LayoutParams(-1, -1));
        setContentView(this.root);
        render();
    }

    void rebuildUiForTheme() {
        this.mainRenderer.captureScrollBeforeUiRebuild();
        buildUi();
    }

    void rebuildUi() {
        this.mainRenderer.captureScrollBeforeUiRebuild();
        buildUi();
    }

    private void buildHeader() {
        this.headerController.buildAppHeader();
    }

    void refreshTabs() {
        this.tabsController.refreshTabs();
    }

    void switchTabAnimated(int i, int i2) {
        if (this.tabs == null || i == this.tabIndex || this.tabAnimating) {
            return;
        }
        this.preferredTabDirection = i2;
        this.swipeController.animateToTab(i, i2, true, "");
    }

    void completeTabTransition(int targetIndex, int direction, boolean recordHistory, String targetSearch) {
        if (recordHistory) {
            this.backNavigationController.recordTabState(this.tabIndex, this.search);
        }
        this.preferredTabDirection = direction;
        this.tabIndex = targetIndex;
        this.search = targetSearch == null ? "" : targetSearch;
        this.tabAnimating = false;
        render();
        this.tabsController.finishTransition(targetIndex);
    }

    void scrollTabsToActive(boolean z) {
        this.tabsController.scrollToActive(z, this.tabIndex);
    }

    void scrollTabsToActive(boolean z, int i) {
        this.tabsController.scrollToActive(z, i);
    }

    private void buildMiniPlayer() {
        this.playerUiController.buildMini();
    }

    void render() {
        this.mainRenderer.render();
    }

    int renderTabPreview(LinearLayout target, int targetIndex, String targetSearch) {
        return this.mainRenderer.renderPreview(target, targetIndex, targetSearch);
    }

    void renderSectionHeader() {
        this.headerController.renderSectionHeader();
    }

    void renderSettings() {
        this.settingsRenderer.render();
    }

    String themeName() {
        return this.themeController.themeName();
    }

    void openThemeDialog() {
        this.themeController.openDialog();
    }

    void stopPlaybackAndClearQueue() {
        this.playbackQueueController.clear();
        this.playerUiController.syncPlaybackUi();
        refreshAfterTrackChange();
    }

    ArrayList<Track> currentVisibleTracks() {
        return this.libraryListController.currentVisibleTracks();
    }

    boolean matchesTrackSearch(Track track, String query) {
        return this.libraryListController.matchesTrackSearch(track, query);
    }

    boolean containsSearch(String value, String query) {
        return this.libraryListController.containsSearch(value, query);
    }

    void renderSongs(ArrayList<Track> arrayList) {
        this.songsRenderer.render(arrayList);
    }

    void openGroupSongs(String title, ArrayList<Track> tracks) {
        this.overlayController.openGroup(title, tracks);
    }

    void openPlaylist(Playlist playlist) {
        this.overlayController.openPlaylist(playlist);
    }

    void openQueuePanel() {
        this.overlayController.openQueue();
    }

    void openAddFavorites() {
        this.overlayController.openAddFavorites();
    }

    void openSongActions(Track track) {
        this.overlayController.openSongActions(track);
    }

    void addTrackToQueue(Track track) {
        this.playbackQueueController.add(track);
    }

    void removeTrackFromQueue(Track track) {
        this.playbackQueueController.remove(track);
    }

    void removeTrackFromLibrary(Track track) {
        this.playbackQueueController.removeFromLibrary(track);
    }

    void confirmDeletePlaylist(Playlist playlist) {
        this.overlayController.confirmDeletePlaylist(playlist);
    }

    void createPlaylistDialog() {
        this.overlayController.createPlaylist();
    }

    void renamePlaylistDialog(Playlist playlist) {
        this.overlayController.renamePlaylist(playlist);
    }

    void openSearch() {
        this.overlayController.openSearch();
    }

    void showInputPanel(String title, String hint, String value, boolean numeric, InputDone done) {
        this.overlayController.showInput(title, hint, value, numeric, done);
    }

    void openFullPlayer() {
        this.playerUiController.openFullPlayer();
    }

    String loopLabel() {
        return this.playbackQueueController.loopLabel();
    }

    String formatMs(int i) {
        return PlaybackTimeFormatter.formatMilliseconds(i);
    }

    String formatTrackDuration(Track track) {
        return track.durationMs > 0 ? formatMs(track.durationMs) : "--:--";
    }

    int playbackDurationFor(Track track) {
        int serviceDuration = (int) Math.min(Integer.MAX_VALUE,
                Math.max(0L, this.playbackController.duration()));
        if (serviceDuration > 0) {
            return serviceDuration;
        }
        return track == null ? 0 : Math.max(0, track.durationMs);
    }

    String formatSeconds(long j) {
        return PlaybackTimeFormatter.formatSeconds(j);
    }

    void timerDialog() {
        this.sleepTimerController.openDialog();
    }

    void openSaveTrackDialog(Track track) {
        this.overlayController.chooseCollection(track);
    }

    void refreshParticleSettings() {
        if (this.particleEffectsView != null) {
            this.particleEffectsView.settingsChanged();
        }
    }

    void refreshPlaybackAppearance() {
        this.playbackController.refreshAudioEffects();
    }

    String timerButtonText() {
        return this.sleepTimerController.buttonText();
    }

    void playTrack(Track track) {
        this.playbackQueueController.playTrack(track, true);
    }

    void playTrack(Track track, boolean z) {
        this.playbackQueueController.playTrack(track, z);
    }

    void playList(ArrayList<Track> arrayList, boolean z) {
        this.playbackQueueController.playList(arrayList, z);
    }

    void toggleCurrent() {
        this.playbackQueueController.toggleOrStart();
    }

    void nextInternal() {
        this.playbackController.next();
    }

    void previousInternal() {
        this.playbackController.previous();
    }

    void cycleLoopMode() {
        this.playbackController.cycleRepeatMode();
    }

    void seekTo(int position) {
        this.playbackController.seekTo(position);
    }

    void playQueueIndex(int index, int position) {
        this.playbackQueueController.playIndex(index, position);
    }

    int playbackPosition() {
        return (int) Math.min(Integer.MAX_VALUE, this.playbackController.currentPosition());
    }

    void startSleepTimer(long delayMs) {
        this.playbackController.startSleepTimer(delayMs);
    }

    void cancelSleepTimer() {
        this.playbackController.cancelSleepTimer();
    }

    ArrayList<String> queueUris() {
        return this.playbackQueueController.queueUris();
    }

    ArrayList<Track> activeQueue() {
        return this.playbackQueueController.activeQueue();
    }

    boolean isPlayingSource(ArrayList<Track> arrayList) {
        return this.playbackQueueController.isPlayingSource(arrayList);
    }

    boolean isPlayingCollection(ArrayList<Track> tracks) {
        return this.playbackQueueController.isPlayingCollection(tracks);
    }

    boolean isCurrentCollection(ArrayList<Track> tracks) {
        return this.playbackQueueController.isCurrentCollection(tracks);
    }

    int queueIndexOf(Track track) {
        return this.playbackQueueController.indexOf(track);
    }

    void updateMini() {
        this.playerUiController.updateMini();
    }

    void toggleFavorite(Track track) {
        if (this.favorites.contains(track.uri)) {
            this.favorites.remove(track.uri);
        } else {
            this.favorites.add(track.uri);
        }
        saveState();
    }

    boolean isCurrent(Track track) {
        int currentIndex = currentTrackIndex();
        return currentIndex >= 0 && currentIndex < this.tracks.size()
                && this.tracks.get(currentIndex).uri.equals(track.uri);
    }

    Track findTrack(String str) {
        for (Track track : this.tracks) {
            if (track.uri.equals(str)) {
                return track;
            }
        }
        return null;
    }

    private static int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    void loadCover(ImageView view, Track track, int fallbackColor) {
        registerCoverState(view, track);
        this.coverLoader.load(view, track, fallbackColor);
    }

    void loadCover(ImageView view, Track track, int fallbackColor, int maxSize) {
        registerCoverState(view, track);
        this.coverLoader.load(view, track, fallbackColor, maxSize);
    }

    private void registerCoverState(ImageView view, Track track) {
        if (view instanceof RotatingCoverImageView) {
            RotatingCoverImageView cover = (RotatingCoverImageView) view;
            cover.bindTrack(track);
            if (!this.renderingTabPreview && track != null) {
                this.songRows.registerCover(track.uri, cover);
            }
        }
    }

    void seedCoverCacheFromView(ImageView view, Track track) {
        this.coverLoader.seedFromView(view, track);
    }

    WaveformView wave(Track track, boolean z) {
        WaveformView waveformView = new WaveformView(this, track.title + track.uri,
                z ? this.purple : this.purpleSoft, this.yellow,
                z && this.isPlaybackPlaying());
        waveformView.setMinimumHeight(dp(28));
        waveformView.setPadding(0, dp(3), 0, dp(3));
        waveformView.setLayoutParams(new LinearLayout.LayoutParams(dp(190), dp(30)));
        return waveformView;
    }

    void openPicker() {
        this.audioImportController.openFiles();
    }

    void openFolderPicker() {
        this.audioImportController.openFolder();
    }

    void rescanMusicFolders() {
        this.audioImportController.rescanPersistedFolders();
    }

    void reloadUiPreferences() {
        this.uiPreferencesStore.load();
        rebuildUi();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (this.settingsController.handleActivityResult(requestCode, resultCode, data)) {
            return;
        }
        if (this.backgroundPlaybackSettingsController.handleActivityResult(requestCode)) {
            return;
        }
        if (this.backgroundSettingsController.handleActivityResult(requestCode, resultCode, data)) {
            return;
        }
        this.audioImportController.handleActivityResult(requestCode, resultCode, data);
    }

    LinearLayout row() {
        return this.uiFactory.row();
    }

    TextView text(String value, int size, boolean bold) {
        return this.uiFactory.text(value, size, bold);
    }

    void makeMarquee(TextView text) {
        this.uiFactory.makeMarquee(text);
    }

    Button button(String label) {
        return this.uiFactory.button(label);
    }

    Button icon(String symbol) {
        return this.uiFactory.icon(symbol);
    }

    Button shuffleButton() {
        return this.uiFactory.shuffleButton();
    }

    void applyPlainIconStyle(Button button) {
        this.uiFactory.applyPlainIconStyle(button, this.dark ? Color.rgb(230, 226, 236) : this.primaryText);
    }

    void applyPlainIconStyle(Button button, int color) {
        this.uiFactory.applyPlainIconStyle(button, color);
    }

    void applyCardStyle(View view) {
        this.uiFactory.applyCardStyle(view);
    }

    void applyCardStyle(View view, int opacity) {
        this.uiFactory.applyCardStyle(view, opacity);
    }

    int cardSurfaceColor(int color) {
        return cardSurfaceColor(color, this.cardOpacity);
    }

    int cardSurfaceColor(int color, int opacity) {
        return Color.argb(Math.round(255.0f * opacity / 100.0f),
                Color.red(color), Color.green(color), Color.blue(color));
    }

    void applyPrimaryButtonStyle(Button button) {
        this.uiFactory.applyPrimaryButtonStyle(button);
    }

    void applySecondaryButtonStyle(Button button) {
        this.uiFactory.applySecondaryButtonStyle(button);
    }

    void applySecondaryButtonStyle(Button button, int opacity) {
        this.uiFactory.applySecondaryButtonStyle(button, opacity);
    }

    void applyPlayerToolStyle(Button button, boolean active) {
        this.uiFactory.applyPlayerToolStyle(button, active);
    }

    void applySeekBarColors(SeekBar seekBar) {
        this.uiFactory.applySeekBarColors(seekBar);
    }

    LinearLayout.LayoutParams square(int size) {
        return this.uiFactory.square(size);
    }

    View spaced(View view) {
        return this.uiFactory.spaced(view);
    }

    void setSurface(View view, int color, boolean outlined) {
        this.uiFactory.setSurface(view, color, outlined);
    }

    void setSurface(View view, int color, boolean outlined, int opacity) {
        this.uiFactory.setSurface(view, color, outlined, opacity);
    }

    View lineView() {
        return this.uiFactory.lineView();
    }

    ImageView coverView() {
        return this.uiFactory.coverView();
    }

    FrameLayout shade() {
        return this.uiFactory.shade();
    }

    LinearLayout panelCard() {
        return this.uiFactory.panelCard();
    }

    void addMiniSpacerIfNeeded() {
        int currentIndex = currentTrackIndex();
        if (currentIndex < 0 || currentIndex >= this.tracks.size()
                || this.overlayHost.getChildCount() > 0) {
            return;
        }
        View view = new View(this);
        view.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(88)));
        this.list.addView(view);
    }

    void openSongDiagnostics() {
        SongDiagnostics.Result result = SongDiagnostics.inspect(this, this.tracks);
        String message = tr("Available: ", "Доступно: ") + result.available
                + "\n" + tr("Unavailable: ", "Недоступно: ") + result.unavailable
                + "\n" + tr("With duration: ", "С длительностью: ") + result.withDuration
                + "\n" + tr("Without duration: ", "Без длительности: ") + result.withoutDuration
                + (result.problemTitles.isEmpty()
                        ? ""
                        : "\n" + tr("Problem tracks:", "Проблемные треки:") + result.problemTitles);
        showConfirmPanel(tr("Song check", "Проверка песен"), message, new Runnable() {
            @Override
            public void run() {
            }
        });
    }

    void showConfirmPanel(String title, String message, Runnable yesAction) {
        this.dialogController.showConfirmation(title, message, yesAction);
    }

    void showActionPanel(String title, String message, String negativeLabel,
            String positiveLabel, Runnable action) {
        this.dialogController.showConfirmation(
                title, message, negativeLabel, positiveLabel, action);
    }

    void showActionPanel(String title, String message, String negativeLabel,
            String positiveLabel, boolean emphasizePositive, Runnable action) {
        this.dialogController.showConfirmation(
                title, message, negativeLabel, positiveLabel, emphasizePositive, action);
    }

    FrameLayout.LayoutParams centerParams(int i, int i2) {
        return this.responsiveLayoutController.centeredPanelParams(i, i2);
    }

    FrameLayout.LayoutParams bottomParams() {
        return this.responsiveLayoutController.bottomPanelParams();
    }

    int dp(int i) {
        return Math.round(i * getResources().getDisplayMetrics().density);
    }

}
