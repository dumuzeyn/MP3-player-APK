package com.dumuzeyn.mp3player;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.provider.DocumentsContract;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputFilter;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.provider.OpenableColumns;
import android.util.Log;
import android.util.LruCache;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.JSONArray;

class MainActivityCore extends Activity {
    private static final String CUSTOM_TIMER = "customTimer";
    private static final String ANIMATIONS = "animations";
    private static final String DEBUG_TAG = "MP3PlayerDebug";
    private static final String FAVORITES = "favorites";
    private static final String LANGUAGE = "language";
    private static final long MAX_AUDIO_BYTES = 220L * 1024L * 1024L;
    private static final int MAX_COVER_BYTES = 8 * 1024 * 1024;
    private static final int COVER_THUMB_SIZE = 256;
    static final int COVER_FULL_SIZE = 1024;
    private static final int SWIPE_START_DP = 21;
    private static final int SWIPE_COMMIT_DP = 52;
    private static final int PICK_AUDIO = 2001;
    private static final int PICK_AUDIO_FOLDER = 2002;
    private static final int MAX_FOLDER_IMPORT = 3000;
    private static final String PLAYLISTS = "playlists";
    private static final String PREFS = "mp3_player_ui";
    private static final String RESUME_WINDOW_MINUTES = "resumeWindowMinutes";
    static final int TAB_CYCLES = 21;
    private static final String THEME = "theme";
    private static final String CUSTOM_BG = "customBg";
    private static final String CUSTOM_FG = "customFg";
    int bg;
    int fg;
    private int line;
    LinearLayout list;
    Button miniButton;
    LinearLayout miniPlayer;
    TextView miniSub;
    TextView miniTitle;
    int muted;
    int purple;
    private int purpleDark;
    int purpleSoft;
    int yellow;
    private int yellowDark;
    private int yellowSoft;
    private int card;
    int cardStroke;
    int primaryText;
    int secondaryText;
    FrameLayout overlayHost;
    LinearLayout page;
    int panel;
    private SharedPreferences prefs;
    FrameLayout root;
    LinearLayout tabRow;
    String[] tabs;
    HorizontalScrollView tabsScroll;
    final ArrayList<Track> tracks = new ArrayList<>();
    final HashSet<String> favorites = new HashSet<>();
    final ArrayList<Playlist> playlists = new ArrayList<>();
    final ArrayList<Track> playbackQueue = new ArrayList<>();
    private final LruCache<String, Bitmap> coverCache = createCoverCache();
    private final Map<String, ArrayList<ImageView>> pendingCoverTargets = new LinkedHashMap<>();
    private final ExecutorService coverExecutor = Executors.newFixedThreadPool(2);
    final Handler uiHandler = new Handler(Looper.getMainLooper());
    final Handler playbackHandler = new Handler(Looper.getMainLooper());
    final Handler sleepHandler = new Handler(Looper.getMainLooper());
    final SongRowStateRegistry songRows = new SongRowStateRegistry();
    final SongsRenderer songsRenderer = new SongsRenderer(this);
    private final PlayerUiController playerUiController = new PlayerUiController(this);
    private final SettingsRenderer settingsRenderer = new SettingsRenderer(this);
    private final TabsController tabsController = new TabsController(this);
    private final PlaybackController playbackController = new PlaybackController(this);
    final SleepTimerController sleepTimerController = new SleepTimerController(this);
    final LibraryListController libraryListController = new LibraryListController(this);
    private final PlaylistController playlistController = new PlaylistController(this);
    private final MainRenderer mainRenderer = new MainRenderer(this);
    Button sourcePlayButton;
    int tabIndex = 0;
    int currentIndex = -1;
    boolean playing = false;
    int loopMode = 0;
    int customTimerMinutes = 10;
    int resumeWindowMinutes = 120;
    int resumePosition = 0;
    long sleepTimerEndsAt = 0;
    boolean dark = false;
    boolean animations = true;
    boolean shuffleMode = false;
    private String language = "en";
    private String themeMode = "light";
    private int customBg = -1;
    private int customFg = -16777216;
    int preferredTabDirection = 0;
    private float swipeStartX = 0.0f;
    private float swipeStartY = 0.0f;
    boolean tabAnimating = false;
    private boolean swipeStartedOnTabs = false;
    private boolean swipeStartedOnMiniPlayer = false;
    private boolean pageSwipeConsuming = false;
    String search = "";
    boolean fullPlayerOpening = false;
    int songRenderGeneration = 0;

    private static LruCache<String, Bitmap> createCoverCache() {
        int maxMemoryKb = (int) (Runtime.getRuntime().maxMemory() / 1024L);
        int cacheSizeKb = Math.max(4 * 1024, maxMemoryKb / 16);
        return new LruCache<String, Bitmap>(cacheSizeKb) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                return Math.max(1, bitmap.getByteCount() / 1024);
            }
        };
    }

    interface InputDone {
        void done(String str);
    }

    private interface PanelAction {
        void add();

        void remove(Track track);
    }

    private interface PickDone {
        void done(Set<String> set);
    }

    private interface ColorPickDone {
        void picked(int color);
    }

    static int accessBackgroundColor(MainActivityCore mainActivity) {
        return mainActivity.bg;
    }

    static int accessCurrentIndex(MainActivityCore mainActivity) {
        return mainActivity.currentIndex;
    }

    static int accessCustomTimerMinutes(MainActivityCore mainActivity) {
        return mainActivity.customTimerMinutes;
    }

    static boolean accessDarkMode(MainActivityCore mainActivity) {
        return mainActivity.dark;
    }

    static HashSet accessFavorites(MainActivityCore mainActivity) {
        return mainActivity.favorites;
    }

    static int accessForegroundColor(MainActivityCore mainActivity) {
        return mainActivity.fg;
    }

    static LinearLayout accessListView(MainActivityCore mainActivity) {
        return mainActivity.list;
    }

    static int accessLoopMode(MainActivityCore mainActivity) {
        return mainActivity.loopMode;
    }

    static FrameLayout accessOverlayHost(MainActivityCore mainActivity) {
        return mainActivity.overlayHost;
    }

    static int accessPanelColor(MainActivityCore mainActivity) {
        return mainActivity.panel;
    }

    static ArrayList accessPlaybackQueue(MainActivityCore mainActivity) {
        return mainActivity.playbackQueue;
    }

    static ArrayList accessPlaylists(MainActivityCore mainActivity) {
        return mainActivity.playlists;
    }

    static LinearLayout accessTabRow(MainActivityCore mainActivity) {
        return mainActivity.tabRow;
    }

    static String[] accessTabs(MainActivityCore mainActivity) {
        return mainActivity.tabs;
    }

    static HorizontalScrollView accessTabsScroll(MainActivityCore mainActivity) {
        return mainActivity.tabsScroll;
    }

    static ArrayList accessTracks(MainActivityCore mainActivity) {
        return mainActivity.tracks;
    }

    static void setCurrentIndexValue(MainActivityCore mainActivity, int i) {
        mainActivity.currentIndex = i;
    }

    static void setCustomTimerMinutesValue(MainActivityCore mainActivity, int i) {
        mainActivity.customTimerMinutes = i;
    }

    static void setDarkModeValue(MainActivityCore mainActivity, boolean z) {
        mainActivity.dark = z;
    }

    static void setLanguageValue(MainActivityCore mainActivity, String str) {
        mainActivity.language = str;
    }

    static void setLoopModeValue(MainActivityCore mainActivity, int i) {
        mainActivity.loopMode = i;
    }

    static void setPlayingValue(MainActivityCore mainActivity, boolean z) {
        mainActivity.playing = z;
    }

    static void setSearchValue(MainActivityCore mainActivity, String str) {
        mainActivity.search = str;
    }

    static void setSleepTimerEndsAtValue(MainActivityCore mainActivity, long j) {
        mainActivity.sleepTimerEndsAt = j;
    }

    static void setTabAnimatingValue(MainActivityCore mainActivity, boolean z) {
        mainActivity.tabAnimating = z;
    }

    static void setTabIndexValue(MainActivityCore mainActivity, int i) {
        mainActivity.tabIndex = i;
    }

    static void callApplyButtonColors(MainActivityCore mainActivity, Button button, int i, int i2) {
        mainActivity.applyButtonColors(button, i, i2);
    }

    static void callBuildUi(MainActivityCore mainActivity) {
        mainActivity.buildUi();
    }

    static void callCancelSleepTimer(MainActivityCore mainActivity) {
        mainActivity.cancelSleepTimer();
    }

    static void callChoosePlaylistForTrack(MainActivityCore mainActivity, Track track) {
        mainActivity.choosePlaylistForTrack(track);
    }

    static void callConfirmDeletePlaylist(MainActivityCore mainActivity, Playlist playlist) {
        mainActivity.confirmDeletePlaylist(playlist);
    }

    static void callConfirmDeleteTrack(MainActivityCore mainActivity, Track track) {
        mainActivity.confirmDeleteTrack(track);
    }

    static void callCreatePlaylistAndAdd(MainActivityCore mainActivity, Track track) {
        mainActivity.createPlaylistAndAdd(track);
    }

    static void callCreatePlaylistDialog(MainActivityCore mainActivity) {
        mainActivity.createPlaylistDialog();
    }

    static ArrayList callCurrentVisibleTracks(MainActivityCore mainActivity) {
        return mainActivity.currentVisibleTracks();
    }

    static void callCustomTimerDialog(MainActivityCore mainActivity) {
        mainActivity.customTimerDialog();
    }

    static Track callFindTrack(MainActivityCore mainActivity, String str) {
        return mainActivity.findTrack(str);
    }

    static String callFormatMs(MainActivityCore mainActivity, int i) {
        return mainActivity.formatMs(i);
    }

    static boolean callIsCurrent(MainActivityCore mainActivity, Track track) {
        return mainActivity.isCurrent(track);
    }

    static boolean callIsInPlaybackQueue(MainActivityCore mainActivity, Track track) {
        return mainActivity.isInPlaybackQueue(track);
    }

    static void callNext(MainActivityCore mainActivity) {
        mainActivity.next();
    }

    static void callOpenAddFavorites(MainActivityCore mainActivity) {
        mainActivity.openAddFavorites();
    }

    static void callOpenAddToPlaylist(MainActivityCore mainActivity, Playlist playlist) {
        mainActivity.openAddToPlaylist(playlist);
    }

    static void callOpenAddToQueue(MainActivityCore mainActivity) {
        mainActivity.openAddToQueue();
    }

    static void callOpenFullPlayer(MainActivityCore mainActivity) {
        mainActivity.openFullPlayer();
    }

    static void callOpenGroupSongs(MainActivityCore mainActivity, String str, ArrayList arrayList) {
        mainActivity.openGroupSongs(str, arrayList);
    }

    static void callOpenPicker(MainActivityCore mainActivity) {
        mainActivity.openPicker();
    }

    static void callOpenPlaylist(MainActivityCore mainActivity, Playlist playlist) {
        mainActivity.openPlaylist(playlist);
    }

    static void callOpenQueuePanel(MainActivityCore mainActivity) {
        mainActivity.openQueuePanel();
    }

    static void callOpenSearch(MainActivityCore mainActivity) {
        mainActivity.openSearch();
    }

    static void callPlayList(MainActivityCore mainActivity, ArrayList arrayList, boolean z) {
        mainActivity.playList(arrayList, z);
    }

    static void callPlayTrack(MainActivityCore mainActivity, Track track) {
        mainActivity.playTrack(track);
    }

    static void callPlayTrackWithRender(MainActivityCore mainActivity, Track track, boolean z) {
        mainActivity.playTrack(track, z);
    }

    static ArrayList callPlaylistTracks(MainActivityCore mainActivity, Playlist playlist) {
        return mainActivity.playlistTracks(playlist);
    }

    static void callPrevious(MainActivityCore mainActivity) {
        mainActivity.previous();
    }

    static void callRender(MainActivityCore mainActivity) {
        mainActivity.render();
    }

    static void callSaveState(MainActivityCore mainActivity) {
        mainActivity.saveState();
    }

    static void callScrollTabsToActive(MainActivityCore mainActivity, boolean z) {
        mainActivity.scrollTabsToActive(z);
    }

    static void callSetSurface(MainActivityCore mainActivity, View view, int i, boolean z) {
        mainActivity.setSurface(view, i, z);
    }

    static void callShowPanel(MainActivityCore mainActivity, String str, ArrayList arrayList, PanelAction panelAction) {
        mainActivity.showPanel(str, arrayList, panelAction);
    }

    static void callStartSleepTimer(MainActivityCore mainActivity, int i) {
        mainActivity.startSleepTimer(i);
    }

    static void callStopPlaybackAndClearQueue(MainActivityCore mainActivity) {
        mainActivity.stopPlaybackAndClearQueue();
    }

    static void callSwitchTabAnimated(MainActivityCore mainActivity, int i, int i2) {
        mainActivity.switchTabAnimated(i, i2);
    }

    static String callTimerButtonText(MainActivityCore mainActivity) {
        return mainActivity.timerButtonText();
    }

    static void callTimerDialog(MainActivityCore mainActivity) {
        mainActivity.timerDialog();
    }

    static void callToggleCurrent(MainActivityCore mainActivity) {
        mainActivity.toggleCurrent();
    }

    static void callToggleFavorite(MainActivityCore mainActivity, Track track) {
        mainActivity.toggleFavorite(track);
    }

    static String callTranslate(MainActivityCore mainActivity, String str, String str2) {
        return mainActivity.tr(str, str2);
    }

    static void callUpdateMini(MainActivityCore mainActivity) {
        mainActivity.updateMini();
    }

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.prefs = getSharedPreferences(PREFS, 0);
        this.themeMode = this.prefs.getString(THEME, "light");
        if (!"light".equals(this.themeMode) && !"dark".equals(this.themeMode) && !"custom".equals(this.themeMode)) {
            this.themeMode = "light";
        }
        this.customBg = this.prefs.getInt(CUSTOM_BG, -1);
        this.customFg = this.prefs.getInt(CUSTOM_FG, -16777216);
        this.animations = this.prefs.getBoolean(ANIMATIONS, true);
        this.language = this.prefs.getString(LANGUAGE, "en");
        if (!"en".equals(this.language) && !"ru".equals(this.language)) {
            this.language = "en";
        }
        this.customTimerMinutes = this.prefs.getInt(CUSTOM_TIMER, 10);
        this.resumeWindowMinutes = Math.max(0, this.prefs.getInt(RESUME_WINDOW_MINUTES, 120));
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission("android.permission.POST_NOTIFICATIONS") != 0) {
            requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, 33);
        }
        loadState();
        restoreRecentPlayback();
        colors();
        updateLauncherIcon();
        buildUi();
        refreshMissingMetadataAsync();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent motionEvent) {
        if (handlePageSwipe(motionEvent)) {
            return true;
        }
        return super.dispatchTouchEvent(motionEvent);
    }

    private void loadState() {
        this.tracks.clear();
        this.tracks.addAll(TrackStore.load(this));
        LibraryDatabase database = new LibraryDatabase(this);
        this.favorites.clear();
        this.favorites.addAll(database.loadFavorites());
        this.playlists.clear();
        this.playlists.addAll(database.loadPlaylists());
        database.close();
    }

    private void restoreRecentPlayback() {
        if (this.resumeWindowMinutes <= 0) {
            return;
        }
        SharedPreferences sharedPreferences = getSharedPreferences(PlayerService.RESUME_PREFS, 0);
        long savedAt = sharedPreferences.getLong(PlayerService.RESUME_SAVED_AT, 0L);
        if (savedAt <= 0 || System.currentTimeMillis() - savedAt > ((long) this.resumeWindowMinutes) * 60000L) {
            return;
        }
        String uri = sharedPreferences.getString(PlayerService.RESUME_URI, "");
        Track track = findTrack(uri);
        if (track == null) {
            return;
        }
        this.currentIndex = this.tracks.indexOf(track);
        this.playing = false;
        this.resumePosition = Math.max(0, sharedPreferences.getInt(PlayerService.RESUME_POSITION, 0));
        PlayerService.lastIndex = this.currentIndex;
        PlayerService.lastPlaying = false;
        PlayerService.lastPosition = this.resumePosition;
        PlayerService.lastDuration = Math.max(0, sharedPreferences.getInt(PlayerService.RESUME_DURATION, 0));
        if (PlayerService.lastDuration <= 0 && track.durationMs > 0) {
            PlayerService.lastDuration = track.durationMs;
        }
        PlayerService.lastUri = uri;
        PlayerService.lastLoopMode = sharedPreferences.getInt(PlayerService.RESUME_LOOP_MODE, 0);
        this.loopMode = PlayerService.lastLoopMode;
        this.playbackQueue.clear();
        try {
            JSONArray queue = new JSONArray(sharedPreferences.getString(PlayerService.RESUME_QUEUE, "[]"));
            for (int i = 0; i < queue.length(); i++) {
                Track queueTrack = findTrack(queue.optString(i, ""));
                if (queueTrack != null) {
                    this.playbackQueue.add(queueTrack);
                }
            }
        } catch (Exception e) {
        }
        if (this.playbackQueue.isEmpty()) {
            this.playbackQueue.add(track);
        }
    }

    class UiAction1 implements Runnable {
        UiAction1() {
        }

        @Override
        public void run() {
            ArrayList arrayList = new ArrayList(MainActivityCore.accessTracks(MainActivityCore.this));
            boolean z = false;
            for (int i = 0; i < arrayList.size(); i++) {
                Track track = (Track) arrayList.get(i);
                if (track.durationMs <= 0 || "Неизвестный альбом".equals(track.album) || "Неизвестный жанр".equals(track.genre)) {
                    Track trackRefreshMetadata = TrackStore.refreshMetadata(MainActivityCore.this, track);
                    if (trackRefreshMetadata.durationMs != track.durationMs || !trackRefreshMetadata.album.equals(track.album) || !trackRefreshMetadata.genre.equals(track.genre) || !trackRefreshMetadata.artist.equals(track.artist)) {
                        arrayList.set(i, trackRefreshMetadata);
                        z = true;
                    }
                }
            }
            if (z) {
                TrackStore.save(MainActivityCore.this, arrayList);
                MainActivityCore.this.runOnUiThread(new MetadataRefreshResult(this, arrayList));
            }
        }

        class MetadataRefreshResult implements Runnable {
            final UiAction1 this$1;
            final ArrayList val$freshTracks;

            MetadataRefreshResult(UiAction1 anonymousClass1, ArrayList arrayList) {
                this.val$freshTracks = arrayList;
                this.this$1 = anonymousClass1;
            }

            @Override
            public void run() {
                MainActivityCore.accessTracks(MainActivityCore.this).clear();
                MainActivityCore.accessTracks(MainActivityCore.this).addAll(this.val$freshTracks);
                MainActivityCore.callRender(MainActivityCore.this);
            }
        }
    }

    private void refreshMissingMetadataAsync() {
        new Thread(new UiAction1()).start();
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

    private boolean handlePageSwipe(MotionEvent motionEvent) {
        int length;
        int length2;
        if (this.tabs == null || this.tabs.length == 0) {
            return false;
        }
        if (this.overlayHost != null && this.overlayHost.getChildCount() > 0) {
            return false;
        }
        if (motionEvent.getActionMasked() == 0) {
            this.swipeStartedOnTabs = isInsideTabs(motionEvent);
            this.swipeStartedOnMiniPlayer = this.playerUiController.isInsideMiniPlayer(motionEvent);
            this.pageSwipeConsuming = false;
            this.swipeStartX = motionEvent.getX();
            this.swipeStartY = motionEvent.getY();
            this.tabsController.cancelScrollAnimation();
            return false;
        }
        if (motionEvent.getActionMasked() == 2) {
            if (this.swipeStartedOnTabs || this.swipeStartedOnMiniPlayer) {
                return false;
            }
            float x = motionEvent.getX() - this.swipeStartX;
            float y = motionEvent.getY() - this.swipeStartY;
            if (!this.pageSwipeConsuming && Math.abs(x) > dp(SWIPE_START_DP) && Math.abs(x) > Math.abs(y)) {
                this.pageSwipeConsuming = true;
                if (this.root != null && this.root.getParent() != null) {
                    this.root.getParent().requestDisallowInterceptTouchEvent(true);
                }
            }
            if (this.pageSwipeConsuming && this.animations && this.list != null) {
                float width = (this.root == null || this.root.getWidth() <= 0) ? getResources().getDisplayMetrics().widthPixels : this.root.getWidth();
                float drag = Math.max(-width * 0.82f, Math.min(width * 0.82f, x));
                this.list.setTranslationX(drag);
                this.list.setAlpha(Math.max(0.72f, 1.0f - (Math.abs(drag) / width) * 0.24f));
            }
            return this.pageSwipeConsuming;
        }
        if (motionEvent.getActionMasked() != 1 && motionEvent.getActionMasked() != 3) {
            return false;
        }
        if (this.swipeStartedOnTabs) {
            this.swipeStartedOnTabs = false;
            this.pageSwipeConsuming = false;
            return false;
        }
        if (this.swipeStartedOnMiniPlayer) {
            this.swipeStartedOnMiniPlayer = false;
            this.pageSwipeConsuming = false;
            return false;
        }
        float x2 = motionEvent.getX() - this.swipeStartX;
        float y2 = motionEvent.getY() - this.swipeStartY;
        boolean z = this.pageSwipeConsuming;
        this.pageSwipeConsuming = false;
        if (motionEvent.getActionMasked() == 1 && Math.abs(x2) > dp(SWIPE_COMMIT_DP) && Math.abs(x2) > Math.abs(y2)) {
            int i = this.tabIndex;
            if (x2 < 0.0f) {
                length = i + 1;
                length2 = this.tabs.length;
            } else {
                length = (i - 1) + this.tabs.length;
                length2 = this.tabs.length;
            }
            switchTabAnimated(length % length2, x2 < 0.0f ? 1 : -1);
            return true;
        }
        if (z && this.animations && this.list != null) {
            this.list.animate().translationX(0.0f).alpha(1.0f).setDuration(90L).setInterpolator(new DecelerateInterpolator()).start();
        }
        if (z && !this.animations && this.list != null) {
            this.list.setTranslationX(0.0f);
            this.list.setAlpha(1.0f);
        }
        return z;
    }

    private boolean isInsideTabs(MotionEvent motionEvent) {
        return this.tabsController.isInsideTabs(motionEvent);
    }

    void saveState() {
        this.prefs.edit().putString(THEME, this.themeMode).putInt(CUSTOM_BG, this.customBg).putInt(CUSTOM_FG, this.customFg).putBoolean(ANIMATIONS, this.animations).putString(LANGUAGE, this.language).putInt(CUSTOM_TIMER, this.customTimerMinutes).putInt(RESUME_WINDOW_MINUTES, this.resumeWindowMinutes).apply();
        LibraryDatabase database = new LibraryDatabase(this);
        try {
            database.saveFavorites(this.favorites);
            database.savePlaylists(this.playlists);
        } finally {
            database.close();
        }
    }

    private void colors() {
        this.dark = "dark".equals(this.themeMode) || ("custom".equals(this.themeMode) && isDarkColor(this.customBg));
        if ("custom".equals(this.themeMode)) {
            this.bg = this.customBg;
            this.fg = this.customFg;
            this.primaryText = this.customFg;
            this.secondaryText = mixColor(this.customFg, this.customBg, 0.58f);
            this.card = mixColor(this.customBg, this.customFg, this.dark ? 0.92f : 0.96f);
            this.cardStroke = mixColor(this.customFg, this.customBg, 0.18f);
            this.purple = this.customFg;
            this.purpleDark = mixColor(this.customFg, this.customBg, 0.82f);
            this.purpleSoft = mixColor(this.customFg, this.customBg, 0.18f);
            this.yellow = Color.rgb(255, 208, 0);
            this.yellowDark = Color.rgb(231, 185, 0);
            this.yellowSoft = Color.rgb(255, 245, 190);
        } else if (this.dark) {
            this.bg = Color.rgb(17, 16, 21);
            this.fg = Color.WHITE;
            this.primaryText = Color.WHITE;
            this.secondaryText = Color.rgb(170, 164, 178);
            this.card = Color.rgb(26, 24, 31);
            this.cardStroke = Color.rgb(51, 46, 58);
            this.purple = Color.rgb(163, 92, 255);
            this.purpleDark = Color.rgb(124, 50, 232);
            this.purpleSoft = Color.rgb(44, 35, 58);
            this.yellow = Color.rgb(255, 212, 56);
            this.yellowDark = Color.rgb(231, 185, 0);
            this.yellowSoft = Color.rgb(72, 62, 33);
        } else {
            this.bg = Color.WHITE;
            this.fg = Color.rgb(24, 21, 29);
            this.primaryText = Color.rgb(24, 21, 29);
            this.secondaryText = Color.rgb(118, 113, 125);
            this.card = Color.rgb(250, 249, 252);
            this.cardStroke = Color.rgb(233, 229, 238);
            this.purple = Color.rgb(124, 50, 232);
            this.purpleDark = Color.rgb(97, 32, 197);
            this.purpleSoft = Color.rgb(238, 228, 255);
            this.yellow = Color.rgb(255, 208, 0);
            this.yellowDark = Color.rgb(231, 185, 0);
            this.yellowSoft = Color.rgb(255, 245, 190);
        }
        this.muted = this.secondaryText;
        this.line = this.cardStroke;
        this.panel = this.card;
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
                return MainActivityCore.this.playing;
            }

            @Override
            public int activeColor() {
                return MainActivityCore.this.purple;
            }

            @Override
            public int inactiveColor() {
                return MainActivityCore.this.purpleSoft;
            }
        });
        if (this.sourcePlayButton != null) {
            this.sourcePlayButton.setText(isPlayingSource(currentVisibleTracks()) ? "Ⅱ" : "▶");
        }
        updateMini();
    }

    private boolean isDarkColor(int color) {
        return ThemeManager.isDarkColor(color);
    }

    private int mixColor(int first, int second, float amount) {
        return ThemeManager.mixColor(first, second, amount);
    }

    private void updateLauncherIcon() {
        PackageManager packageManager = getPackageManager();
        int enabled = PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
        int disabled = PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
        int flags = PackageManager.DONT_KILL_APP;
        ComponentName light = new ComponentName(this, getPackageName() + ".LauncherLight");
        ComponentName dark = new ComponentName(this, getPackageName() + ".LauncherDark");
        try {
            boolean darkIcon = "dark".equals(this.themeMode) || ("custom".equals(this.themeMode) && isDarkColor(this.customBg));
            packageManager.setComponentEnabledSetting(darkIcon ? dark : light, enabled, flags);
            packageManager.setComponentEnabledSetting(darkIcon ? light : dark, disabled, flags);
        } catch (Exception e) {
        }
    }

    private void updateTaskPreview() {
        if (Build.VERSION.SDK_INT < 21) {
            return;
        }
        try {
            setTaskDescription(new ActivityManager.TaskDescription("MP3 Player", launcherPreviewIcon(), this.bg));
        } catch (Exception ignored) {
        }
    }

    private Bitmap launcherPreviewIcon() {
        try {
            Drawable drawable = getResources().getDrawable(this.dark ? R.mipmap.ic_launcher_dark : R.mipmap.ic_launcher_home);
            int size = Math.max(1, dp(64));
            Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, size, size);
            drawable.draw(canvas);
            return bitmap;
        } catch (Exception e) {
            return BitmapFactory.decodeResource(getResources(), getApplicationInfo().icon);
        }
    }

    private void buildUi() {
        colors();
        getWindow().setBackgroundDrawable(new ColorDrawable(this.bg));
        getWindow().setStatusBarColor(this.bg);
        getWindow().setNavigationBarColor(this.bg);
        if (Build.VERSION.SDK_INT >= 23) {
            getWindow().getDecorView().setSystemUiVisibility(this.dark ? 0 : 8192);
        }
        updateTaskPreview();
        refreshTabLabels();
        this.root = new FrameLayout(this);
        this.root.setBackgroundColor(this.bg);
        this.page = new LinearLayout(this);
        this.page.setOrientation(1);
        this.page.setPadding(dp(8), dp(14), dp(8), dp(8));
        this.root.addView(this.page, new FrameLayout.LayoutParams(-1, -1));
        buildHeader();
        this.tabsController.buildTabs();
        ScrollView scrollView = new ScrollView(this);
        this.list = new LinearLayout(this);
        this.list.setOrientation(1);
        scrollView.addView(this.list, new FrameLayout.LayoutParams(-1, -2));
        this.page.addView(scrollView, new LinearLayout.LayoutParams(-1, 0, 1.0f));
        this.overlayHost = new FrameLayout(this);
        this.root.addView(this.overlayHost, new FrameLayout.LayoutParams(-1, -1));
        buildMiniPlayer();
        setContentView(this.root);
        render();
    }

    private void buildHeader() {
        FrameLayout header = new FrameLayout(this);
        header.setBackground(createCardBackground());
        header.setPadding(dp(12), 0, dp(12), 0);
        LinearLayout linearLayoutRow = row();
        linearLayoutRow.setGravity(16);
        ImageView imageView = new ImageView(this);
        imageView.setImageResource(getResources().getIdentifier("ic_music_vector_user", "drawable", getPackageName()));
        imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        imageView.clearColorFilter();
        imageView.setContentDescription("MP3 Player");
        LinearLayout.LayoutParams iconParams = square(40);
        iconParams.setMargins(0, 0, dp(8), 0);
        linearLayoutRow.addView(imageView, iconParams);
        TextView title = text("MP3 Player", 20, true);
        title.setTextColor(this.primaryText);
        linearLayoutRow.addView(title, new LinearLayout.LayoutParams(0, dp(60), 1.0f));
        linearLayoutRow.addView(createTriangleArtwork(TriangleDecorView.HEADER), new LinearLayout.LayoutParams(dp(76), dp(52)));
        header.addView(linearLayoutRow, new FrameLayout.LayoutParams(-1, -1));
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-1, dp(70));
        layoutParams.setMargins(0, 0, 0, dp(10));
        this.page.addView(header, layoutParams);
    }

    private void buildTabs() {
        this.tabsController.buildTabs();
    }

    void refreshTabs() {
        this.tabsController.refreshTabs();
    }

    void switchTabAnimated(int i, int i2) {
        if (this.tabs == null || i == this.tabIndex || this.tabAnimating) {
            return;
        }
        this.preferredTabDirection = i2;
        if (!this.animations) {
            this.tabIndex = i;
            this.search = "";
            scrollTabsToActive(false, i);
            render();
            return;
        }
        this.tabAnimating = true;
        int width = (this.root == null || this.root.getWidth() <= 0) ? getResources().getDisplayMetrics().widthPixels : this.root.getWidth();
        if (this.list == null) {
            this.tabIndex = i;
            this.search = "";
            render();
            this.tabAnimating = false;
            return;
        }
        scrollTabsToActive(true, i);
        this.list.animate().translationX(i2 < 0 ? width : -width).alpha(0.0f).setDuration(48L).setInterpolator(new DecelerateInterpolator()).withEndAction(new UiAction4(this, i, i2, width)).start();
    }

    class UiAction4 implements Runnable {
        final MainActivityCore this$0;
        final int val$direction;
        final int val$nextIndex;
        final int val$width;

        UiAction4(MainActivityCore mainActivity, int i, int i2, int i3) {
            this.val$nextIndex = i;
            this.val$direction = i2;
            this.val$width = i3;
            this.this$0 = mainActivity;
        }

        @Override
        public void run() {
            MainActivityCore.setTabIndexValue(this.this$0, this.val$nextIndex);
            MainActivityCore.setSearchValue(this.this$0, "");
            MainActivityCore.callRender(this.this$0);
            MainActivityCore.accessListView(this.this$0).setTranslationX(this.val$direction < 0 ? -this.val$width : this.val$width);
            MainActivityCore.accessListView(this.this$0).setAlpha(0.0f);
            MainActivityCore.accessListView(this.this$0).animate().translationX(0.0f).alpha(1.0f).setDuration(92L).setInterpolator(new DecelerateInterpolator()).withEndAction(new UiAction1()).start();
        }

        class UiAction1 implements Runnable {
            UiAction1() {
            }

            @Override
            public void run() {
                MainActivityCore.setTabAnimatingValue(UiAction4.this.this$0, false);
            }
        }
    }

    void scrollTabsToActive(boolean z) {
        this.tabsController.scrollToActive(z, this.tabIndex);
    }

    void scrollTabsToActive(boolean z, int i) {
        this.tabsController.scrollToActive(z, i);
    }

    private int tabDirectionTo(int targetIndex) {
        return this.tabsController.directionTo(targetIndex);
    }

    private void buildMiniPlayer() {
        this.playerUiController.buildMini();
    }

    void render() {
        this.mainRenderer.render();
    }

    void renderSectionHeader() {
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(1);
        String str = this.tabs[this.tabIndex];
        if (this.tabIndex == 0) {
            str = tr3("Songs ", "Песен ", "♪ ") + this.tracks.size();
        }
        TextView textViewText = text(str, 22, true);
        textViewText.setSingleLine(true);
        linearLayout.addView(textViewText, new LinearLayout.LayoutParams(-1, dp(48)));
        if (this.tabIndex == 0 || this.tabIndex == 1) {
            LinearLayout linearLayoutRow = row();
            if (this.tabIndex == 0) {
                Button buttonIcon2 = icon("+");
                buttonIcon2.setOnClickListener(new UiAction11());
                linearLayoutRow.addView(buttonIcon2, square(52));
                Button buttonFolder = icon("▣");
                buttonFolder.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        openFolderPicker();
                    }
                });
                linearLayoutRow.addView(buttonFolder, square(52));
            } else {
                Button buttonIcon3 = icon("+");
                buttonIcon3.setOnClickListener(new UiAction12());
                linearLayoutRow.addView(buttonIcon3, square(52));
            }
            Button buttonSearchButton = searchButton();
            buttonSearchButton.setOnClickListener(new UiAction13());
            linearLayoutRow.addView(buttonSearchButton, square(52));
            Button buttonIcon = icon(isPlayingSource(currentVisibleTracks()) ? "Ⅱ" : "▶");
            buttonIcon.setOnClickListener(new UiAction9());
            this.sourcePlayButton = buttonIcon;
            linearLayoutRow.addView(buttonIcon, square(52));
            Button buttonShuffleButton = shuffleButton();
            buttonShuffleButton.setOnClickListener(new UiAction10());
            linearLayoutRow.addView(buttonShuffleButton, square(52));
            linearLayout.addView(linearLayoutRow, new LinearLayout.LayoutParams(-1, dp(62)));
        } else if (this.tabIndex == 2) {
            LinearLayout linearLayoutRow2 = row();
            Button buttonIcon4 = icon("+");
            buttonIcon4.setOnClickListener(new UiAction14());
            linearLayoutRow2.addView(buttonIcon4, square(52));
            Button buttonSearchButton2 = searchButton();
            buttonSearchButton2.setOnClickListener(new UiAction15());
            linearLayoutRow2.addView(buttonSearchButton2, square(52));
            linearLayout.addView(linearLayoutRow2, new LinearLayout.LayoutParams(-1, dp(62)));
        }
        this.list.addView(linearLayout);
    }

    class UiAction9 implements View.OnClickListener {
        UiAction9() {
        }

        @Override
        public void onClick(View view) {
            ArrayList arrayList = MainActivityCore.callCurrentVisibleTracks(MainActivityCore.this);
            if (MainActivityCore.this.isPlayingSource(arrayList)) {
                MainActivityCore.callToggleCurrent(MainActivityCore.this);
            } else {
                MainActivityCore.callPlayList(MainActivityCore.this, arrayList, false);
            }
        }
    }

    class UiAction10 implements View.OnClickListener {
        UiAction10() {
        }

        @Override
        public void onClick(View view) {
            MainActivityCore.callPlayList(MainActivityCore.this, MainActivityCore.callCurrentVisibleTracks(MainActivityCore.this), true);
        }
    }

    class UiAction11 implements View.OnClickListener {
        UiAction11() {
        }

        @Override
        public void onClick(View view) {
            MainActivityCore.callOpenPicker(MainActivityCore.this);
        }
    }

    class UiAction12 implements View.OnClickListener {
        UiAction12() {
        }

        @Override
        public void onClick(View view) {
            MainActivityCore.callOpenAddFavorites(MainActivityCore.this);
        }
    }

    class UiAction13 implements View.OnClickListener {
        UiAction13() {
        }

        @Override
        public void onClick(View view) {
            MainActivityCore.callOpenSearch(MainActivityCore.this);
        }
    }

    class UiAction14 implements View.OnClickListener {
        UiAction14() {
        }

        @Override
        public void onClick(View view) {
            MainActivityCore.callCreatePlaylistDialog(MainActivityCore.this);
        }
    }

    class UiAction15 implements View.OnClickListener {
        UiAction15() {
        }

        @Override
        public void onClick(View view) {
            MainActivityCore.callOpenSearch(MainActivityCore.this);
        }
    }

    class UiAction16 implements View.OnClickListener {
        UiAction16() {
        }

        @Override
        public void onClick(View view) {
            MainActivityCore.setDarkModeValue(MainActivityCore.this, !MainActivityCore.accessDarkMode(MainActivityCore.this));
            MainActivityCore.callSaveState(MainActivityCore.this);
            MainActivityCore.this.updateLauncherIcon();
            MainActivityCore.callBuildUi(MainActivityCore.this);
        }
    }

    void renderSettings() {
        this.settingsRenderer.render();
    }

    void renderSettingsInternal() {
        this.settingsRenderer.renderSettingsState();
    }

    String themeName() {
        if ("dark".equals(this.themeMode)) {
            return tr("Dark", "Темная");
        }
        if ("custom".equals(this.themeMode)) {
            return tr("Custom", "Своя");
        }
        return tr("Light", "Светлая");
    }

    void openThemeDialog() {
        final FrameLayout frameLayoutShade = shade();
        LinearLayout linearLayoutPanelCard = panelCard();
        linearLayoutPanelCard.setPadding(dp(16), dp(16), dp(16), dp(16));
        linearLayoutPanelCard.addView(text(tr("Theme", "Тема"), 22, true), new LinearLayout.LayoutParams(-1, dp(46)));
        addChoiceButton(linearLayoutPanelCard, tr("Light", "Светлая"), "light".equals(this.themeMode), new Runnable() {
            @Override
            public void run() {
                MainActivityCore.this.themeMode = "light";
                MainActivityCore.this.dark = false;
                MainActivityCore.this.saveState();
                MainActivityCore.this.overlayHost.removeView(frameLayoutShade);
                MainActivityCore.this.updateLauncherIcon();
                MainActivityCore.this.buildUi();
            }
        });
        addChoiceButton(linearLayoutPanelCard, tr("Dark", "Темная"), "dark".equals(this.themeMode), new Runnable() {
            @Override
            public void run() {
                MainActivityCore.this.themeMode = "dark";
                MainActivityCore.this.dark = true;
                MainActivityCore.this.saveState();
                MainActivityCore.this.overlayHost.removeView(frameLayoutShade);
                MainActivityCore.this.updateLauncherIcon();
                MainActivityCore.this.buildUi();
            }
        });
        addChoiceButton(linearLayoutPanelCard, tr("Custom", "Своя"), "custom".equals(this.themeMode), new Runnable() {
            @Override
            public void run() {
                MainActivityCore.this.themeMode = "custom";
                MainActivityCore.this.dark = MainActivityCore.this.isDarkColor(MainActivityCore.this.customBg);
                MainActivityCore.this.saveState();
                MainActivityCore.this.overlayHost.removeView(frameLayoutShade);
                MainActivityCore.this.updateLauncherIcon();
                MainActivityCore.this.buildUi();
            }
        });
        linearLayoutPanelCard.addView(text(tr("Background", "Фон"), 16, true), new LinearLayout.LayoutParams(-1, dp(34)));
        addColorPickerButton(linearLayoutPanelCard, true);
        linearLayoutPanelCard.addView(text(tr("Text and accent", "Текст и акцент"), 16, true), new LinearLayout.LayoutParams(-1, dp(34)));
        addColorPickerButton(linearLayoutPanelCard, false);
        frameLayoutShade.addView(linearLayoutPanelCard, centerParams(dp(340), -2));
        this.overlayHost.addView(frameLayoutShade);
        updateMini();
    }

    private void addColorPickerButton(LinearLayout parent, final boolean background) {
        int color = background ? this.customBg : this.customFg;
        Button button = button(colorHex(color));
        applyButtonColors(button, color, readableOn(color));
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MainActivityCore.this.overlayHost.removeAllViews();
                MainActivityCore.this.openColorPickerDialog(background);
            }
        });
        parent.addView(button, new LinearLayout.LayoutParams(-1, dp(52)));
    }

    private String colorHex(int color) {
        return String.format(Locale.ROOT, "#%02X%02X%02X", Integer.valueOf(Color.red(color)), Integer.valueOf(Color.green(color)), Integer.valueOf(Color.blue(color)));
    }

    private void openColorPickerDialog(final boolean background) {
        final FrameLayout frameLayoutShade = shade();
        LinearLayout linearLayoutPanelCard = panelCard();
        linearLayoutPanelCard.setPadding(dp(16), dp(16), dp(16), dp(16));
        linearLayoutPanelCard.addView(text(background ? tr("Background", "Фон") : tr("Text and accent", "Текст и акцент"), 22, true), new LinearLayout.LayoutParams(-1, dp(46)));
        final View preview = new View(this);
        preview.setBackgroundColor(background ? this.customBg : this.customFg);
        linearLayoutPanelCard.addView(preview, new LinearLayout.LayoutParams(-1, dp(34)));
        ColorWheelView colorWheelView = new ColorWheelView(background ? this.customBg : this.customFg, new ColorPickDone() {
            @Override
            public void picked(int color) {
                MainActivityCore.this.themeMode = "custom";
                if (background) {
                    MainActivityCore.this.customBg = color;
                } else {
                    MainActivityCore.this.customFg = color;
                }
                preview.setBackgroundColor(color);
            }
        });
        LinearLayout.LayoutParams wheelParams = new LinearLayout.LayoutParams(-1, dp(280));
        wheelParams.setMargins(0, dp(12), 0, dp(12));
        linearLayoutPanelCard.addView(colorWheelView, wheelParams);
        LinearLayout linearLayoutRow = row();
        Button buttonBack = button(tr("Back", "Назад"));
        buttonBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MainActivityCore.this.overlayHost.removeView(frameLayoutShade);
                MainActivityCore.this.openThemeDialog();
            }
        });
        linearLayoutRow.addView(buttonBack, new LinearLayout.LayoutParams(0, dp(54), 1.0f));
        Button buttonDone = button(tr3("Done", "Готово", "✓"));
        applyButtonColors(buttonDone, this.fg, this.bg);
        buttonDone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MainActivityCore.this.dark = MainActivityCore.this.isDarkColor(MainActivityCore.this.customBg);
                MainActivityCore.this.saveState();
                MainActivityCore.this.overlayHost.removeView(frameLayoutShade);
                MainActivityCore.this.updateLauncherIcon();
                MainActivityCore.this.buildUi();
            }
        });
        linearLayoutRow.addView(buttonDone, new LinearLayout.LayoutParams(0, dp(54), 1.0f));
        linearLayoutPanelCard.addView(linearLayoutRow);
        frameLayoutShade.addView(linearLayoutPanelCard, centerParams(dp(340), -2));
        this.overlayHost.addView(frameLayoutShade);
    }

    private void addColorRow(LinearLayout parent, final boolean background, int[] colors) {
        LinearLayout linearLayoutRow = row();
        for (final int color : colors) {
            Button button = icon(background && color == this.customBg || !background && color == this.customFg ? "✓" : "");
            button.setTextSize(18.0f);
            applyButtonColors(button, color, readableOn(color));
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    MainActivityCore.this.themeMode = "custom";
                    if (background) {
                        MainActivityCore.this.customBg = color;
                    } else {
                        MainActivityCore.this.customFg = color;
                    }
                    MainActivityCore.this.dark = MainActivityCore.this.isDarkColor(MainActivityCore.this.customBg);
                    MainActivityCore.this.saveState();
                    MainActivityCore.this.updateLauncherIcon();
                    MainActivityCore.this.overlayHost.removeAllViews();
                    MainActivityCore.this.buildUi();
                    MainActivityCore.this.openThemeDialog();
                }
            });
            linearLayoutRow.addView(button, square(44));
        }
        parent.addView(linearLayoutRow, new LinearLayout.LayoutParams(-1, dp(56)));
    }

    private int readableOn(int color) {
        return ThemeManager.readableOn(color);
    }

    class UiAction17 implements View.OnClickListener {
        UiAction17() {
        }

        @Override
        public void onClick(View view) {
            MainActivityCore.setLanguageValue(MainActivityCore.this, "en");
            MainActivityCore.callSaveState(MainActivityCore.this);
            MainActivityCore.callBuildUi(MainActivityCore.this);
        }
    }

    class UiAction18 implements View.OnClickListener {
        UiAction18() {
        }

        @Override
        public void onClick(View view) {
            MainActivityCore.setLanguageValue(MainActivityCore.this, "ru");
            MainActivityCore.callSaveState(MainActivityCore.this);
            MainActivityCore.callBuildUi(MainActivityCore.this);
        }
    }

    String resumeWindowText() {
        if (this.resumeWindowMinutes <= 0) {
            return tr3("off", "выкл", "○");
        }
        if (this.resumeWindowMinutes % 60 == 0) {
            int hours = this.resumeWindowMinutes / 60;
            return hours + " " + tr3(hours == 1 ? "hour" : "hours", "ч", "◷");
        }
        return this.resumeWindowMinutes + " " + tr3("min", "мин", "′");
    }

    void openLanguageDialog() {
        final FrameLayout frameLayoutShade = shade();
        LinearLayout linearLayoutPanelCard = panelCard();
        linearLayoutPanelCard.setPadding(dp(16), dp(16), dp(16), dp(16));
        linearLayoutPanelCard.addView(text(tr3("Language", "Язык", "◐"), 22, true), new LinearLayout.LayoutParams(-1, dp(50)));
        addChoiceButton(linearLayoutPanelCard, "English", english(), new Runnable() {
            @Override
            public void run() {
                MainActivityCore.this.language = "en";
                MainActivityCore.this.saveState();
                MainActivityCore.this.overlayHost.removeView(frameLayoutShade);
                MainActivityCore.this.buildUi();
            }
        });
        addChoiceButton(linearLayoutPanelCard, "Русский", "ru".equals(MainActivityCore.this.language), new Runnable() {
            @Override
            public void run() {
                MainActivityCore.this.language = "ru";
                MainActivityCore.this.saveState();
                MainActivityCore.this.overlayHost.removeView(frameLayoutShade);
                MainActivityCore.this.buildUi();
            }
        });
        frameLayoutShade.addView(linearLayoutPanelCard, centerParams(dp(330), -2));
        this.overlayHost.addView(frameLayoutShade);
        updateMini();
    }

    void openResumeWindowDialog() {
        final FrameLayout frameLayoutShade = shade();
        LinearLayout linearLayoutPanelCard = panelCard();
        linearLayoutPanelCard.setPadding(dp(16), dp(16), dp(16), dp(16));
        linearLayoutPanelCard.addView(text(tr("Mini-player memory", "Память мини-плеера"), 22, true), new LinearLayout.LayoutParams(-1, dp(50)));
        int[] values = new int[]{30, 60, 120, 240, 480, 0};
        for (final int value : values) {
            String label;
            if (value == 0) {
                label = tr("Off", "Отключено");
            } else if (value % 60 == 0) {
                label = (value / 60) + " " + tr(value == 60 ? "hour" : "hours", "ч");
            } else {
                label = value + " " + tr("minutes", "мин");
            }
            addChoiceButton(linearLayoutPanelCard, label, this.resumeWindowMinutes == value, new Runnable() {
                @Override
                public void run() {
                    MainActivityCore.this.resumeWindowMinutes = value;
                    MainActivityCore.this.saveState();
                    MainActivityCore.this.overlayHost.removeView(frameLayoutShade);
                    MainActivityCore.this.render();
                }
            });
        }
        frameLayoutShade.addView(linearLayoutPanelCard, centerParams(dp(330), -2));
        this.overlayHost.addView(frameLayoutShade);
        updateMini();
    }

    private void addChoiceButton(LinearLayout linearLayoutPanelCard, String label, boolean selected, final Runnable action) {
        Button button = button(label);
        button.setTextSize(17.0f);
        button.setGravity(8388627);
        button.setPadding(dp(18), 0, dp(12), 0);
        applyButtonColors(button, selected ? this.fg : this.bg, selected ? this.bg : this.fg);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                action.run();
            }
        });
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-1, dp(54));
        layoutParams.setMargins(0, dp(5), 0, dp(5));
        linearLayoutPanelCard.addView(button, layoutParams);
    }

    void addSettingsButton(String str, View.OnClickListener onClickListener) {
        Button button = button(str);
        button.setTextSize(17.0f);
        button.setGravity(8388627);
        button.setPadding(dp(18), 0, dp(12), 0);
        applySecondaryButtonStyle(button);
        if (str.toLowerCase(Locale.ROOT).contains("delete") || str.toLowerCase(Locale.ROOT).contains("удал")) {
            button.setTextColor(Color.rgb(190, 45, 45));
        }
        button.setOnClickListener(onClickListener);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-1, dp(58));
        layoutParams.setMargins(0, dp(6), 0, dp(6));
        this.list.addView(button, layoutParams);
    }

    void openGithub() {
        try {
            Intent intent = new Intent("android.intent.action.VIEW", Uri.parse("https://github.com/dumuzeyn/MP3-player"));
            intent.addCategory("android.intent.category.BROWSABLE");
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
            }
        } catch (Exception e) {
        }
    }

    void confirmDeleteAllSongs() {
        showConfirmPanel(tr("Delete all songs?", "Удалить все песни?"), tr("Songs will disappear only from this app. Files on the phone will stay untouched.", "Песни исчезнут только из приложения. Файлы на телефоне останутся."), new UiAction22());
    }

    class UiAction22 implements Runnable {
        UiAction22() {
        }

        @Override
        public void run() {
            MainActivityCore.callStopPlaybackAndClearQueue(MainActivityCore.this);
            MainActivityCore.accessTracks(MainActivityCore.this).clear();
            MainActivityCore.accessFavorites(MainActivityCore.this).clear();
            Iterator it = MainActivityCore.accessPlaylists(MainActivityCore.this).iterator();
            while (it.hasNext()) {
                ((Playlist) it.next()).uris.clear();
            }
            TrackStore.save(MainActivityCore.this, MainActivityCore.accessTracks(MainActivityCore.this));
            MainActivityCore.callSaveState(MainActivityCore.this);
            MainActivityCore.callRender(MainActivityCore.this);
        }
    }

    void confirmDeleteAllPlaylists() {
        showConfirmPanel(tr("Delete all playlists?", "Удалить все плейлисты?"), tr("Songs will stay in the app.", "Песни останутся в приложении."), new UiAction23());
    }

    class UiAction23 implements Runnable {
        UiAction23() {
        }

        @Override
        public void run() {
            MainActivityCore.accessPlaylists(MainActivityCore.this).clear();
            MainActivityCore.callSaveState(MainActivityCore.this);
            MainActivityCore.callRender(MainActivityCore.this);
        }
    }

    void stopPlaybackAndClearQueue() {
        this.playbackQueue.clear();
        this.currentIndex = -1;
        this.playing = false;
        this.playbackHandler.removeCallbacksAndMessages(null);
        Intent intent = new Intent(this, (Class<?>) PlayerService.class);
        intent.setAction(PlayerService.ACTION_STOP);
        try {
            startService(intent);
        } catch (Exception e) {
        }
        updateMini();
        refreshAfterTrackChange();
    }

    private ArrayList<Track> currentVisibleTracks() {
        return this.libraryListController.currentVisibleTracks();
    }

    private ArrayList<Track> favoriteTracks() {
        return this.libraryListController.favoriteTracks();
    }

    private ArrayList<Track> filter(ArrayList<Track> arrayList) {
        return this.libraryListController.filter(arrayList);
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

    void renderSongsInternal(ArrayList<Track> arrayList) {
        this.songsRenderer.renderSongsState(arrayList);
    }

    void renderPlaylists() {
        ArrayList<Playlist> arrayList = this.playlistController.filteredPlaylists(this.search);
        if (arrayList.isEmpty()) {
            TextView textViewText = text(tr3("No playlists yet", "Плейлистов пока нет", "∅ ▤"), 18, true);
            textViewText.setPadding(dp(12), dp(24), dp(12), dp(24));
            this.list.addView(textViewText);
            return;
        }
        for (Playlist playlist2 : arrayList) {
            LinearLayout linearLayout = new LinearLayout(this);
            linearLayout.setOrientation(1);
            linearLayout.setPadding(dp(14), dp(12), dp(14), dp(12));
            setSurface(linearLayout, this.panel, false);
            LinearLayout linearLayoutRow = row();
            ArrayList<Track> arrayListPlaylistTracks = this.playlistController.sortedPlaylistTracks(playlist2);
            LinearLayout linearLayout2 = new LinearLayout(this);
            linearLayout2.setOrientation(1);
            TextView textViewText2 = text(playlist2.name, 22, true);
            makeMarquee(textViewText2);
            TextView textViewText3 = text(playlist2.uris.size() + " " + tr3("songs", "песен", "♪"), 13, false);
            linearLayout2.addView(textViewText2);
            linearLayout2.addView(textViewText3);
            linearLayoutRow.addView(linearLayout2, new LinearLayout.LayoutParams(0, -2, 1.0f));
            Button buttonIcon2 = icon("×");
            applyPlainIconStyle(buttonIcon2, Color.rgb(190, 45, 45));
            buttonIcon2.setOnClickListener(new UiAction30(this, playlist2));
            linearLayoutRow.addView(buttonIcon2, square(48));
            Button rename = icon("✎");
            applyPlainIconStyle(rename);
            rename.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    MainActivityCore.this.renamePlaylistDialog(playlist2);
                }
            });
            linearLayoutRow.addView(rename, square(48));
            Button buttonIcon = icon(isPlayingSource(arrayListPlaylistTracks) ? "Ⅱ" : "▶");
            applyPlainIconStyle(buttonIcon, this.purple);
            buttonIcon.setOnClickListener(new UiAction28(this, playlist2));
            linearLayoutRow.addView(buttonIcon, square(48));
            Button buttonShuffleButton = shuffleButton();
            applyPlainIconStyle(buttonShuffleButton);
            buttonShuffleButton.setOnClickListener(new UiAction29(this, playlist2));
            linearLayoutRow.addView(buttonShuffleButton, square(48));
            linearLayout.addView(linearLayoutRow);
            LinearLayout linearLayoutRow2 = row();
            ImageView imageViewCoverView = coverView();
            int i = this.dark ? 28 : 235;
            if (arrayListPlaylistTracks.isEmpty()) {
                imageViewCoverView.setBackgroundColor(Color.rgb(i, i, i));
            } else {
                loadCover(imageViewCoverView, arrayListPlaylistTracks.get(0), Color.rgb(i, i, i));
            }
            linearLayoutRow2.addView(imageViewCoverView, square(86));
            TextView textViewText4 = text("", 16, true);
            textViewText4.setPadding(dp(12), 0, 0, 0);
            linearLayoutRow2.addView(textViewText4, new LinearLayout.LayoutParams(0, dp(96), 1.0f));
            this.playlistController.bindRollingPreview(textViewText4, imageViewCoverView, arrayListPlaylistTracks, this.songRenderGeneration);
            linearLayout.addView(linearLayoutRow2);
            linearLayout.setOnClickListener(new UiAction31(this, playlist2));
            this.list.addView(spaced(linearLayout));
        }
    }

    class UiAction28 implements View.OnClickListener {
        final MainActivityCore this$0;
        final Playlist val$playlist;

        UiAction28(MainActivityCore mainActivity, Playlist playlist) {
            this.val$playlist = playlist;
            this.this$0 = mainActivity;
        }

        @Override
        public void onClick(View view) {
            ArrayList arrayList = MainActivityCore.callPlaylistTracks(this.this$0, this.val$playlist);
            if (this.this$0.isPlayingSource(arrayList)) {
                MainActivityCore.callToggleCurrent(this.this$0);
            } else {
                MainActivityCore.callPlayList(this.this$0, arrayList, false);
            }
        }
    }

    class UiAction29 implements View.OnClickListener {
        final MainActivityCore this$0;
        final Playlist val$playlist;

        UiAction29(MainActivityCore mainActivity, Playlist playlist) {
            this.val$playlist = playlist;
            this.this$0 = mainActivity;
        }

        @Override
        public void onClick(View view) {
            MainActivityCore.callPlayList(this.this$0, MainActivityCore.callPlaylistTracks(this.this$0, this.val$playlist), true);
        }
    }

    class UiAction30 implements View.OnClickListener {
        final MainActivityCore this$0;
        final Playlist val$playlist;

        UiAction30(MainActivityCore mainActivity, Playlist playlist) {
            this.val$playlist = playlist;
            this.this$0 = mainActivity;
        }

        @Override
        public void onClick(View view) {
            MainActivityCore.callConfirmDeletePlaylist(this.this$0, this.val$playlist);
        }
    }

    class UiAction31 implements View.OnClickListener {
        final MainActivityCore this$0;
        final Playlist val$playlist;

        UiAction31(MainActivityCore mainActivity, Playlist playlist) {
            this.val$playlist = playlist;
            this.this$0 = mainActivity;
        }

        @Override
        public void onClick(View view) {
            MainActivityCore.callOpenPlaylist(this.this$0, this.val$playlist);
        }
    }

    private ArrayList<Track> playlistTracks(Playlist playlist) {
        return this.playlistController.playlistTracks(playlist);
    }

    private boolean playlistContainsSearch(Playlist playlist, String query) {
        return this.playlistController.playlistContainsSearch(playlist, query);
    }

    void renderGroups(String str) {
        for (Map.Entry<String, ArrayList<Track>> entry : groupedTracks().entrySet()) {
            String lowerCase = this.search.toLowerCase(Locale.ROOT);
            if (!this.search.trim().isEmpty() && !containsSearch(entry.getKey(), lowerCase) && !groupContainsSearch(entry.getValue(), lowerCase)) {
                continue;
            }
            LinearLayout linearLayoutRow = row();
            linearLayoutRow.setPadding(dp(12), dp(12), dp(12), dp(12));
            setSurface(linearLayoutRow, this.panel, false);
            ImageView imageViewCoverView = coverView();
            int i = this.dark ? 28 : 235;
            if (entry.getValue().isEmpty()) {
                imageViewCoverView.setBackgroundColor(Color.rgb(i, i, i));
            } else {
                loadCover(imageViewCoverView, entry.getValue().get(0), Color.rgb(i, i, i));
            }
            linearLayoutRow.addView(imageViewCoverView, square(72));
            LinearLayout linearLayout = new LinearLayout(this);
            linearLayout.setOrientation(1);
            linearLayout.setPadding(dp(12), 0, dp(8), 0);
            TextView textViewText = text(entry.getKey(), 20, true);
            textViewText.setSingleLine(true);
            textViewText.setEllipsize(TextUtils.TruncateAt.END);
            TextView textViewText2 = text(entry.getValue().size() + " " + tr3("songs", "песен", "♪"), 13, false);
            linearLayout.addView(textViewText);
            linearLayout.addView(textViewText2);
            linearLayoutRow.addView(linearLayout, new LinearLayout.LayoutParams(0, dp(72), 1.0f));
            Button buttonIcon = icon(isPlayingSource(entry.getValue()) ? "Ⅱ" : "▶");
            applyPlainIconStyle(buttonIcon, this.purple);
            buttonIcon.setOnClickListener(new UiAction32(this, entry));
            linearLayoutRow.addView(buttonIcon, square(52));
            Button buttonShuffleButton = shuffleButton();
            applyPlainIconStyle(buttonShuffleButton);
            buttonShuffleButton.setOnClickListener(new UiAction33(this, entry));
            linearLayoutRow.addView(buttonShuffleButton, square(52));
            linearLayoutRow.setOnClickListener(new UiAction34(this, entry));
            this.list.addView(spaced(linearLayoutRow));
        }
    }

    private boolean groupContainsSearch(ArrayList<Track> tracks) {
        String lowerCase = this.search.toLowerCase(Locale.ROOT);
        return groupContainsSearch(tracks, lowerCase);
    }

    private boolean groupContainsSearch(ArrayList<Track> tracks, String query) {
        Iterator<Track> it = tracks.iterator();
        while (it.hasNext()) {
            if (matchesTrackSearch(it.next(), query)) {
                return true;
            }
        }
        return false;
    }

    class UiAction32 implements View.OnClickListener {
        final MainActivityCore this$0;
        final Map.Entry val$entry;

        UiAction32(MainActivityCore mainActivity, Map.Entry entry) {
            this.val$entry = entry;
            this.this$0 = mainActivity;
        }

        @Override
        public void onClick(View view) {
            ArrayList arrayList = (ArrayList) this.val$entry.getValue();
            if (this.this$0.isPlayingSource(arrayList)) {
                MainActivityCore.callToggleCurrent(this.this$0);
            } else {
                MainActivityCore.callPlayList(this.this$0, arrayList, false);
            }
        }
    }

    class UiAction33 implements View.OnClickListener {
        final MainActivityCore this$0;
        final Map.Entry val$entry;

        UiAction33(MainActivityCore mainActivity, Map.Entry entry) {
            this.val$entry = entry;
            this.this$0 = mainActivity;
        }

        @Override
        public void onClick(View view) {
            MainActivityCore.callPlayList(this.this$0, (ArrayList) this.val$entry.getValue(), true);
        }
    }

    class UiAction34 implements View.OnClickListener {
        final MainActivityCore this$0;
        final Map.Entry val$entry;

        UiAction34(MainActivityCore mainActivity, Map.Entry entry) {
            this.val$entry = entry;
            this.this$0 = mainActivity;
        }

        @Override
        public void onClick(View view) {
            MainActivityCore.callOpenGroupSongs(this.this$0, (String) this.val$entry.getKey(), (ArrayList) this.val$entry.getValue());
        }
    }

    private Map<String, ArrayList<Track>> groupedTracks() {
        String strCleanGroup;
        LinkedHashMap linkedHashMap = new LinkedHashMap();
        for (Track track : this.tracks) {
            if (this.tabIndex == 4) {
                strCleanGroup = cleanGroup(track.artist, tr("Unknown artist", "Неизвестный исполнитель"));
            } else {
                strCleanGroup = this.tabIndex == 5 ? cleanGroup(track.album, tr("Unknown album", "Неизвестный альбом")) : cleanGroup(track.genre, tr("Unknown genre", "Неизвестный жанр"));
            }
            if (!linkedHashMap.containsKey(strCleanGroup)) {
                linkedHashMap.put(strCleanGroup, new ArrayList());
            }
            ((ArrayList) linkedHashMap.get(strCleanGroup)).add(track);
        }
        return linkedHashMap;
    }

    private String cleanGroup(String str, String str2) {
        return (str == null || str.trim().isEmpty()) ? str2 : str.trim();
    }

    private void openGroupSongs(String str, ArrayList<Track> arrayList) {
        showPanel(str, arrayList, null);
    }

    class UiAction35 implements PanelAction {
        final MainActivityCore this$0;
        final Playlist val$playlist;

        UiAction35(MainActivityCore mainActivity, Playlist playlist) {
            this.val$playlist = playlist;
            this.this$0 = mainActivity;
        }

        @Override
        public void add() {
            MainActivityCore.callOpenAddToPlaylist(this.this$0, this.val$playlist);
        }

        @Override
        public void remove(Track track) {
            this.val$playlist.uris.remove(track.uri);
            MainActivityCore.callSaveState(this.this$0);
            MainActivityCore.callRender(this.this$0);
            MainActivityCore.accessOverlayHost(this.this$0).removeAllViews();
            MainActivityCore.callOpenPlaylist(this.this$0, this.val$playlist);
        }
    }

    private void openPlaylist(Playlist playlist) {
        showPanel(playlist.name, playlistTracks(playlist), new UiAction35(this, playlist));
    }

    private void showPanel(String str, ArrayList<Track> arrayList, PanelAction panelAction) {
        PanelAction panelAction2 = panelAction;
        FrameLayout frameLayoutShade = shade();
        LinearLayout linearLayoutPanelCard = panelCard();
        LinearLayout linearLayoutRow = row();
        TextView panelTitle = text(str, 20, true);
        makeMarquee(panelTitle);
        linearLayoutRow.addView(panelTitle, new LinearLayout.LayoutParams(0, dp(58), 1.0f));
        Button buttonIcon = icon(isPlayingSource(arrayList) ? "Ⅱ" : "▶");
        buttonIcon.setOnClickListener(new UiAction36(this, arrayList, frameLayoutShade, str, panelAction));
        linearLayoutRow.addView(buttonIcon, square(52));
        Button buttonShuffleButton = shuffleButton();
        buttonShuffleButton.setOnClickListener(new UiAction37(this, arrayList, frameLayoutShade, str, panelAction));
        linearLayoutRow.addView(buttonShuffleButton, square(52));
        if (panelAction2 != null) {
            Button buttonIcon2 = icon("+");
            buttonIcon2.setOnClickListener(new UiAction38(this, panelAction2));
            linearLayoutRow.addView(buttonIcon2, square(52));
        }
        Button buttonIcon3 = icon("×");
        buttonIcon3.setOnClickListener(new UiAction39(this, frameLayoutShade));
        linearLayoutRow.addView(buttonIcon3, square(52));
        linearLayoutPanelCard.addView(linearLayoutRow);
        ScrollView scrollView = new ScrollView(this);
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(1);
        for (Track track : arrayList) {
            if (panelAction2 == null) {
                linearLayout.addView(this.songsRenderer.songRow(track, false, true, new UiAction40(this, frameLayoutShade, str, arrayList, panelAction)));
            } else {
                LinearLayout linearLayout2 = new LinearLayout(this);
                linearLayout2.setOrientation(0);
                linearLayout2.setGravity(16);
                linearLayout2.setPadding(dp(10), dp(8), dp(10), dp(8));
                setSurface(linearLayout2, isCurrent(track) ? this.fg : this.panel, false);
                ImageView imageViewCoverView = coverView();
                loadCover(imageViewCoverView, track, isCurrent(track) ? this.bg : this.dark ? -16777216 : Color.rgb(235, 235, 235));
                imageViewCoverView.setOnClickListener(new UiAction41(this, track, frameLayoutShade));
                linearLayout2.addView(imageViewCoverView, square(58));
                LinearLayout linearLayout3 = new LinearLayout(this);
                linearLayout3.setOrientation(LinearLayout.VERTICAL);
                linearLayout3.setPadding(dp(12), 0, dp(8), 0);
                TextView textViewText = text(track.title, 17, true);
                textViewText.setSingleLine(true);
                textViewText.setEllipsize(TextUtils.TruncateAt.END);
                textViewText.setTextColor(isCurrent(track) ? this.bg : this.fg);
                linearLayout3.addView(textViewText);
                linearLayout3.addView(wave(track, isCurrent(track)));
                linearLayout2.addView(linearLayout3, new LinearLayout.LayoutParams(0, dp(70), 1.0f));
                Button buttonIcon4 = icon("−");
                applyButtonColors(buttonIcon4, isCurrent(track) ? this.fg : this.bg, isCurrent(track) ? this.bg : this.fg);
                buttonIcon4.setOnClickListener(new UiAction42(this, panelAction2, track));
                linearLayout2.addView(buttonIcon4, square(48));
                Button buttonIcon5 = icon((isCurrent(track) && this.playing) ? "Ⅱ" : "▶");
                applyButtonColors(buttonIcon5, isCurrent(track) ? this.fg : this.bg, isCurrent(track) ? this.bg : this.fg);
                buttonIcon5.setOnClickListener(new UiAction43(this, track, frameLayoutShade, str, arrayList, panelAction));
                linearLayout2.addView(buttonIcon5, square(48));
                linearLayout.addView(spaced(linearLayout2));
            }
            panelAction2 = panelAction;
        }
        scrollView.addView(linearLayout);
        linearLayoutPanelCard.addView(scrollView, new LinearLayout.LayoutParams(-1, 0, 1.0f));
        frameLayoutShade.addView(linearLayoutPanelCard, bottomParams());
        this.overlayHost.addView(frameLayoutShade);
        updateMini();
    }

    class UiAction36 implements View.OnClickListener {
        final MainActivityCore this$0;
        final PanelAction val$action;
        final FrameLayout val$shade;
        final ArrayList val$source;
        final String val$title;

        UiAction36(MainActivityCore mainActivity, ArrayList arrayList, FrameLayout frameLayout, String str, PanelAction panelAction) {
            this.val$source = arrayList;
            this.val$shade = frameLayout;
            this.val$title = str;
            this.val$action = panelAction;
            this.this$0 = mainActivity;
        }

        @Override
        public void onClick(View view) {
            if (this.this$0.isPlayingSource(this.val$source)) {
                MainActivityCore.callToggleCurrent(this.this$0);
            } else {
                MainActivityCore.callPlayList(this.this$0, this.val$source, false);
            }
            if (this.val$shade.getParent() != null) {
                MainActivityCore.accessOverlayHost(this.this$0).removeView(this.val$shade);
            }
            MainActivityCore.callShowPanel(this.this$0, this.val$title, this.val$source, this.val$action);
        }
    }

    class UiAction37 implements View.OnClickListener {
        final MainActivityCore this$0;
        final PanelAction val$action;
        final FrameLayout val$shade;
        final ArrayList val$source;
        final String val$title;

        UiAction37(MainActivityCore mainActivity, ArrayList arrayList, FrameLayout frameLayout, String str, PanelAction panelAction) {
            this.val$source = arrayList;
            this.val$shade = frameLayout;
            this.val$title = str;
            this.val$action = panelAction;
            this.this$0 = mainActivity;
        }

        @Override
        public void onClick(View view) {
            MainActivityCore.callPlayList(this.this$0, this.val$source, true);
            if (this.val$shade.getParent() != null) {
                MainActivityCore.accessOverlayHost(this.this$0).removeView(this.val$shade);
            }
            MainActivityCore.callShowPanel(this.this$0, this.val$title, this.val$source, this.val$action);
        }
    }

    class UiAction38 implements View.OnClickListener {
        final MainActivityCore this$0;
        final PanelAction val$action;

        UiAction38(MainActivityCore mainActivity, PanelAction panelAction) {
            this.val$action = panelAction;
            this.this$0 = mainActivity;
        }

        @Override
        public void onClick(View view) {
            this.val$action.add();
        }
    }

    class UiAction39 implements View.OnClickListener {
        final MainActivityCore this$0;
        final FrameLayout val$shade;

        UiAction39(MainActivityCore mainActivity, FrameLayout frameLayout) {
            this.val$shade = frameLayout;
            this.this$0 = mainActivity;
        }

        @Override
        public void onClick(View view) {
            MainActivityCore.accessOverlayHost(this.this$0).removeView(this.val$shade);
            MainActivityCore.callUpdateMini(this.this$0);
        }
    }

    class UiAction40 implements Runnable {
        final MainActivityCore this$0;
        final PanelAction val$action;
        final FrameLayout val$shade;
        final ArrayList val$source;
        final String val$title;

        UiAction40(MainActivityCore mainActivity, FrameLayout frameLayout, String str, ArrayList arrayList, PanelAction panelAction) {
            this.val$shade = frameLayout;
            this.val$title = str;
            this.val$source = arrayList;
            this.val$action = panelAction;
            this.this$0 = mainActivity;
        }

        @Override
        public void run() {
            if (this.val$shade.getParent() != null) {
                MainActivityCore.accessOverlayHost(this.this$0).removeView(this.val$shade);
            }
            MainActivityCore.callShowPanel(this.this$0, this.val$title, this.val$source, this.val$action);
        }
    }

    class UiAction41 implements View.OnClickListener {
        final MainActivityCore this$0;
        final FrameLayout val$shade;
        final Track val$track;

        UiAction41(MainActivityCore mainActivity, Track track, FrameLayout frameLayout) {
            this.val$track = track;
            this.val$shade = frameLayout;
            this.this$0 = mainActivity;
        }

        @Override
        public void onClick(View view) {
            MainActivityCore.callPlayTrack(this.this$0, this.val$track);
            MainActivityCore.accessOverlayHost(this.this$0).removeView(this.val$shade);
            this.this$0.fullPlayerOpening = true;
            MainActivityCore.callOpenFullPlayer(this.this$0);
        }
    }

    class UiAction42 implements View.OnClickListener {
        final MainActivityCore this$0;
        final PanelAction val$action;
        final Track val$track;

        UiAction42(MainActivityCore mainActivity, PanelAction panelAction, Track track) {
            this.val$action = panelAction;
            this.val$track = track;
            this.this$0 = mainActivity;
        }

        @Override
        public void onClick(View view) {
            this.val$action.remove(this.val$track);
        }
    }

    class UiAction43 implements View.OnClickListener {
        final MainActivityCore this$0;
        final PanelAction val$action;
        final FrameLayout val$shade;
        final ArrayList val$source;
        final String val$title;
        final Track val$track;

        UiAction43(MainActivityCore mainActivity, Track track, FrameLayout frameLayout, String str, ArrayList arrayList, PanelAction panelAction) {
            this.val$track = track;
            this.val$shade = frameLayout;
            this.val$title = str;
            this.val$source = arrayList;
            this.val$action = panelAction;
            this.this$0 = mainActivity;
        }

        @Override
        public void onClick(View view) {
            if (MainActivityCore.callIsCurrent(this.this$0, this.val$track)) {
                MainActivityCore.callToggleCurrent(this.this$0);
            } else {
                MainActivityCore.callPlayTrack(this.this$0, this.val$track);
            }
            MainActivityCore.accessOverlayHost(this.this$0).removeView(this.val$shade);
            MainActivityCore.callShowPanel(this.this$0, this.val$title, this.val$source, this.val$action);
        }
    }

    void openQueuePanel() {
        if (this.playbackQueue.isEmpty() && this.currentIndex >= 0 && this.currentIndex < this.tracks.size()) {
            this.playbackQueue.add(this.tracks.get(this.currentIndex));
        }
        FrameLayout frameLayoutShade = shade();
        LinearLayout linearLayoutPanelCard = panelCard();
        LinearLayout linearLayoutRow = row();
        linearLayoutRow.addView(text(tr3("Now playing", "Список проигрывания", "▶ ▤"), 20, true), new LinearLayout.LayoutParams(0, dp(58), 1.0f));
        Button buttonIcon = icon("+");
        buttonIcon.setOnClickListener(new UiAction44());
        linearLayoutRow.addView(buttonIcon, square(52));
        Button buttonIcon2 = icon("×");
        buttonIcon2.setOnClickListener(new UiAction45(this, frameLayoutShade));
        linearLayoutRow.addView(buttonIcon2, square(52));
        linearLayoutPanelCard.addView(linearLayoutRow);
        ScrollView scrollView = new ScrollView(this);
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(1);
        for (final Track track : new ArrayList<Track>(activeQueue())) {
            linearLayout.addView(this.songsRenderer.queueRow(track, new Runnable() {
                @Override
                public void run() {
                    removeFromQueue(track);
                    overlayHost.removeView(frameLayoutShade);
                    openQueuePanel();
                }
            }, new Runnable() {
                @Override
                public void run() {
                    playQueueTrack(track);
                    overlayHost.removeView(frameLayoutShade);
                    openQueuePanel();
                }
            }));
        }
        scrollView.addView(linearLayout);
        linearLayoutPanelCard.addView(scrollView, new LinearLayout.LayoutParams(-1, 0, 1.0f));
        frameLayoutShade.addView(linearLayoutPanelCard, bottomParams());
        this.overlayHost.addView(frameLayoutShade);
        updateMini();
    }

    class UiAction44 implements View.OnClickListener {
        UiAction44() {
        }

        @Override
        public void onClick(View view) {
            MainActivityCore.callOpenAddToQueue(MainActivityCore.this);
        }
    }

    class UiAction45 implements View.OnClickListener {
        final MainActivityCore this$0;
        final FrameLayout val$shade;

        UiAction45(MainActivityCore mainActivity, FrameLayout frameLayout) {
            this.val$shade = frameLayout;
            this.this$0 = mainActivity;
        }

        @Override
        public void onClick(View view) {
            MainActivityCore.accessOverlayHost(this.this$0).removeView(this.val$shade);
            MainActivityCore.callUpdateMini(this.this$0);
        }
    }

    class UiAction48 implements PickDone {
        UiAction48() {
        }

        @Override
        public void done(Set<String> set) {
            if (MainActivityCore.accessPlaybackQueue(MainActivityCore.this).isEmpty() && MainActivityCore.accessCurrentIndex(MainActivityCore.this) >= 0 && MainActivityCore.accessCurrentIndex(MainActivityCore.this) < MainActivityCore.accessTracks(MainActivityCore.this).size()) {
                MainActivityCore.accessPlaybackQueue(MainActivityCore.this).add((Track) MainActivityCore.accessTracks(MainActivityCore.this).get(MainActivityCore.accessCurrentIndex(MainActivityCore.this)));
            }
            Iterator<String> it = set.iterator();
            while (it.hasNext()) {
                Track resolvedTrack = MainActivityCore.callFindTrack(MainActivityCore.this, it.next());
                if (resolvedTrack != null && !MainActivityCore.callIsInPlaybackQueue(MainActivityCore.this, resolvedTrack)) {
                    MainActivityCore.accessPlaybackQueue(MainActivityCore.this).add(resolvedTrack);
                }
            }
            MainActivityCore.accessOverlayHost(MainActivityCore.this).removeAllViews();
            MainActivityCore.callOpenQueuePanel(MainActivityCore.this);
        }
    }

    private void openAddToQueue() {
        showPickPanel(tr3("Add to queue", "Добавить в список", "+ ▤"), new HashSet<>(), new UiAction48());
    }

    void removeFromQueue(Track track) {
        for (int size = this.playbackQueue.size() - 1; size >= 0; size--) {
            if (this.playbackQueue.get(size).uri.equals(track.uri)) {
                this.playbackQueue.remove(size);
            }
        }
        if (isCurrent(track)) {
            Intent intent = new Intent(this, (Class<?>) PlayerService.class);
            intent.setAction(PlayerService.ACTION_STOP);
            if (Build.VERSION.SDK_INT >= 26) {
                startForegroundService(intent);
            } else {
                startService(intent);
            }
            this.currentIndex = -1;
            this.playing = false;
        }
    }

    void playQueueTrack(Track track) {
        int iQueueIndexOf = queueIndexOf(track);
        if (iQueueIndexOf < 0) {
            return;
        }
        this.currentIndex = this.tracks.indexOf(track);
        this.playing = true;
        this.resumePosition = 0;
        startServiceAction(PlayerService.ACTION_PLAY_INDEX, iQueueIndexOf, false);
        startPlaybackWatcher();
        render();
    }

    private boolean isInPlaybackQueue(Track track) {
        Iterator<Track> it = this.playbackQueue.iterator();
        while (it.hasNext()) {
            if (it.next().uri.equals(track.uri)) {
                return true;
            }
        }
        return false;
    }

    class UiAction49 implements PickDone {
        UiAction49() {
        }

        @Override
        public void done(Set<String> set) {
            MainActivityCore.accessFavorites(MainActivityCore.this).addAll(set);
            MainActivityCore.callSaveState(MainActivityCore.this);
            MainActivityCore.callRender(MainActivityCore.this);
        }
    }

    private void openAddFavorites() {
        showPickPanel(tr3("Add to favorites", "Добавить в избранное", "+ ♥"), new HashSet<>(), new UiAction49());
    }

    class UiAction50 implements PickDone {
        final MainActivityCore this$0;
        final Playlist val$playlist;

        UiAction50(MainActivityCore mainActivity, Playlist playlist) {
            this.val$playlist = playlist;
            this.this$0 = mainActivity;
        }

        @Override
        public void done(Set<String> set) {
            this.this$0.playlistController.addTracksToPlaylist(this.val$playlist, set);
            MainActivityCore.callRender(this.this$0);
            MainActivityCore.accessOverlayHost(this.this$0).removeAllViews();
            MainActivityCore.callOpenPlaylist(this.this$0, this.val$playlist);
        }
    }

    private void openAddToPlaylist(Playlist playlist) {
        showPickPanel(tr3("Add to ", "Добавить в ", "+ ") + playlist.name, new HashSet<>(), new UiAction50(this, playlist));
    }

    private void showPickPanel(String str, HashSet<String> hashSet, PickDone pickDone) {
        FrameLayout frameLayoutShade = shade();
        LinearLayout linearLayoutPanelCard = panelCard();
        LinearLayout linearLayoutRow = row();
        linearLayoutRow.addView(text(str, 20, true), new LinearLayout.LayoutParams(0, dp(58), 1.0f));
        Button buttonIcon = icon("+");
        buttonIcon.setOnClickListener(new UiAction51(this, frameLayoutShade, pickDone, hashSet));
        linearLayoutRow.addView(buttonIcon, square(52));
        Button buttonIcon2 = icon("×");
        buttonIcon2.setOnClickListener(new UiAction52(this, frameLayoutShade));
        linearLayoutRow.addView(buttonIcon2, square(52));
        linearLayoutPanelCard.addView(linearLayoutRow);
        EditText searchField = new EditText(this);
        searchField.setSingleLine(true);
        searchField.setHint(tr3("Search songs", "Поиск песен", "⌕ ♪"));
        searchField.setTextColor(this.fg);
        searchField.setHintTextColor(this.muted);
        searchField.setTextSize(16.0f);
        searchField.setPadding(dp(14), 0, dp(14), 0);
        searchField.setInputType(1);
        searchField.setFilters(new InputFilter[]{new InputFilter.LengthFilter(80)});
        setSurface(searchField, this.panel, true);
        LinearLayout.LayoutParams searchParams = new LinearLayout.LayoutParams(-1, dp(52));
        searchParams.setMargins(0, 0, 0, dp(8));
        linearLayoutPanelCard.addView(searchField, searchParams);
        ScrollView scrollView = new ScrollView(this);
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(1);
        renderPickRows(linearLayout, hashSet, "");
        searchField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                renderPickRows(linearLayout, hashSet, s == null ? "" : s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        scrollView.addView(linearLayout);
        linearLayoutPanelCard.addView(scrollView, new LinearLayout.LayoutParams(-1, 0, 1.0f));
        frameLayoutShade.addView(linearLayoutPanelCard, bottomParams());
        this.overlayHost.addView(frameLayoutShade);
        updateMini();
    }

    private void renderPickRows(LinearLayout parent, HashSet<String> selected, String query) {
        parent.removeAllViews();
        String normalized = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        int count = 0;
        for (Track track : this.tracks) {
            if (normalized.isEmpty() || matchesTrackSearch(track, normalized)) {
                parent.addView(pickSongRow(track, selected));
                count++;
            }
        }
        if (count == 0) {
            TextView empty = text(tr3("Nothing found", "Ничего не найдено", "∅"), 16, true);
            empty.setPadding(dp(12), dp(18), dp(12), dp(18));
            parent.addView(empty);
        }
    }

    class UiAction51 implements View.OnClickListener {
        final MainActivityCore this$0;
        final PickDone val$done;
        final HashSet val$selected;
        final FrameLayout val$shade;

        UiAction51(MainActivityCore mainActivity, FrameLayout frameLayout, PickDone pickDone, HashSet hashSet) {
            this.val$shade = frameLayout;
            this.val$done = pickDone;
            this.val$selected = hashSet;
            this.this$0 = mainActivity;
        }

        @Override
        public void onClick(View view) {
            MainActivityCore.accessOverlayHost(this.this$0).removeView(this.val$shade);
            this.val$done.done(this.val$selected);
            MainActivityCore.callUpdateMini(this.this$0);
        }
    }

    class UiAction52 implements View.OnClickListener {
        final MainActivityCore this$0;
        final FrameLayout val$shade;

        UiAction52(MainActivityCore mainActivity, FrameLayout frameLayout) {
            this.val$shade = frameLayout;
            this.this$0 = mainActivity;
        }

        @Override
        public void onClick(View view) {
            MainActivityCore.accessOverlayHost(this.this$0).removeView(this.val$shade);
            MainActivityCore.callUpdateMini(this.this$0);
        }
    }

    private View pickSongRow(Track track, HashSet<String> hashSet) {
        int iRgb;
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(0);
        linearLayout.setGravity(16);
        linearLayout.setPadding(dp(10), dp(8), dp(10), dp(8));
        setSurface(linearLayout, hashSet.contains(track.uri) ? this.fg : this.panel, false);
        ImageView imageViewCoverView = coverView();
        if (hashSet.contains(track.uri)) {
            iRgb = this.bg;
        } else {
            int i = this.dark ? 28 : 235;
            iRgb = Color.rgb(i, i, i);
        }
        loadCover(imageViewCoverView, track, iRgb);
        linearLayout.addView(imageViewCoverView, square(58));
        LinearLayout linearLayout2 = new LinearLayout(this);
        linearLayout2.setOrientation(1);
        linearLayout2.setPadding(dp(12), 0, dp(8), 0);
        TextView textViewText = text(track.title, 17, true);
        textViewText.setTextColor(hashSet.contains(track.uri) ? this.bg : this.fg);
        textViewText.setSingleLine(true);
        textViewText.setEllipsize(TextUtils.TruncateAt.END);
        linearLayout2.addView(textViewText);
        linearLayout2.addView(wave(track, hashSet.contains(track.uri)));
        linearLayout.addView(linearLayout2, new LinearLayout.LayoutParams(0, dp(70), 1.0f));
        Button buttonIcon = icon(hashSet.contains(track.uri) ? "✔" : "+");
        Button buttonIcon2 = icon((isCurrent(track) && this.playing) ? "Ⅱ" : "▶");
        buttonIcon2.setTag(track.uri);
        buttonIcon2.setOnClickListener(new UiAction54(this, track));
        buttonIcon.setOnClickListener(new UiAction53(this, hashSet, track, linearLayout, textViewText, buttonIcon, buttonIcon2));
        applyButtonColors(buttonIcon, hashSet.contains(track.uri) ? this.fg : this.bg, hashSet.contains(track.uri) ? this.bg : this.fg);
        linearLayout.addView(buttonIcon, square(48));
        applyButtonColors(buttonIcon2, hashSet.contains(track.uri) ? this.fg : this.bg, hashSet.contains(track.uri) ? this.bg : this.fg);
        linearLayout.addView(buttonIcon2, square(48));
        return spaced(linearLayout);
    }

    class UiAction53 implements View.OnClickListener {
        final MainActivityCore this$0;
        final Button val$mark;
        final Button val$play;
        final LinearLayout val$row;
        final HashSet val$selected;
        final TextView val$title;
        final Track val$track;

        UiAction53(MainActivityCore mainActivity, HashSet hashSet, Track track, LinearLayout linearLayout, TextView textView, Button button, Button button2) {
            this.val$selected = hashSet;
            this.val$track = track;
            this.val$row = linearLayout;
            this.val$title = textView;
            this.val$mark = button;
            this.val$play = button2;
            this.this$0 = mainActivity;
        }

        @Override
        public void onClick(View view) {
            if (this.val$selected.contains(this.val$track.uri)) {
                this.val$selected.remove(this.val$track.uri);
            } else {
                this.val$selected.add(this.val$track.uri);
            }
            boolean zContains = this.val$selected.contains(this.val$track.uri);
            MainActivityCore mainActivity = this.this$0;
            LinearLayout linearLayout = this.val$row;
            MainActivityCore mainActivity2 = this.this$0;
            MainActivityCore.callSetSurface(mainActivity, linearLayout, zContains ? MainActivityCore.accessForegroundColor(mainActivity2) : MainActivityCore.accessPanelColor(mainActivity2), false);
            TextView textView = this.val$title;
            MainActivityCore mainActivity3 = this.this$0;
            textView.setTextColor(zContains ? MainActivityCore.accessBackgroundColor(mainActivity3) : MainActivityCore.accessForegroundColor(mainActivity3));
            this.val$mark.setText(zContains ? "✔" : "+");
            MainActivityCore.callApplyButtonColors(this.this$0, this.val$mark, zContains ? MainActivityCore.accessForegroundColor(this.this$0) : MainActivityCore.accessBackgroundColor(this.this$0), zContains ? MainActivityCore.accessBackgroundColor(this.this$0) : MainActivityCore.accessForegroundColor(this.this$0));
            MainActivityCore.callApplyButtonColors(this.this$0, this.val$play, zContains ? MainActivityCore.accessForegroundColor(this.this$0) : MainActivityCore.accessBackgroundColor(this.this$0), zContains ? MainActivityCore.accessBackgroundColor(this.this$0) : MainActivityCore.accessForegroundColor(this.this$0));
        }
    }

    class UiAction54 implements View.OnClickListener {
        final MainActivityCore this$0;
        final Track val$track;

        UiAction54(MainActivityCore mainActivity, Track track) {
            this.val$track = track;
            this.this$0 = mainActivity;
        }

        @Override
        public void onClick(View view) {
            if (MainActivityCore.callIsCurrent(this.this$0, this.val$track)) {
                MainActivityCore.callToggleCurrent(this.this$0);
            } else {
                MainActivityCore.callPlayTrackWithRender(this.this$0, this.val$track, false);
            }
            this.this$0.syncPickPlayButtons(this.val$track);
        }
    }

    private void syncPickPlayButtons(Track activeTrack) {
        syncPickPlayButtons(this.overlayHost, activeTrack == null ? "" : activeTrack.uri);
    }

    private void syncPickPlayButtons(View view, String activeUri) {
        if (view == null) {
            return;
        }
        if (view instanceof Button && ((Button) view).getTag() instanceof String) {
            String uri = (String) ((Button) view).getTag();
            ((Button) view).setText(uri.equals(activeUri) && this.playing ? "Ⅱ" : "▶");
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                syncPickPlayButtons(group.getChildAt(i), activeUri);
            }
        }
    }

    void openSongActions(Track track) {
        String str;
        String str2;
        FrameLayout frameLayoutShade = shade();
        LinearLayout linearLayoutPanelCard = panelCard();
        linearLayoutPanelCard.addView(text(track.title, 20, true), new LinearLayout.LayoutParams(-1, dp(52)));
        if (this.favorites.contains(track.uri)) {
            str = "? Remove from favorites";
            str2 = "♥ Убрать из избранного";
        } else {
            str = "? Add to favorites";
            str2 = "♡ Добавить в избранное";
        }
        Button button = button(tr(str, str2));
        button.setOnClickListener(new UiAction55(this, track, frameLayoutShade));
        linearLayoutPanelCard.addView(button, new LinearLayout.LayoutParams(-1, dp(54)));
        Button button2 = button(tr3("+ Add to playlist", "+ Добавить в плейлист", "+ ▤"));
        button2.setOnClickListener(new UiAction56(this, frameLayoutShade, track));
        linearLayoutPanelCard.addView(button2, new LinearLayout.LayoutParams(-1, dp(54)));
        Button button3 = button(tr3("× Remove from app", "× Удалить из приложения", "⌫ ♪"));
        button3.setOnClickListener(new UiAction57(this, frameLayoutShade, track));
        linearLayoutPanelCard.addView(button3, new LinearLayout.LayoutParams(-1, dp(54)));
        Button button4 = button(tr3("Close", "Закрыть", "×"));
        button4.setOnClickListener(new UiAction58(this, frameLayoutShade));
        linearLayoutPanelCard.addView(button4, new LinearLayout.LayoutParams(-1, dp(54)));
        frameLayoutShade.addView(linearLayoutPanelCard, bottomParams());
        this.overlayHost.addView(frameLayoutShade);
        updateMini();
    }

    class UiAction55 implements View.OnClickListener {
        final MainActivityCore this$0;
        final FrameLayout val$shade;
        final Track val$track;

        UiAction55(MainActivityCore mainActivity, Track track, FrameLayout frameLayout) {
            this.val$track = track;
            this.val$shade = frameLayout;
            this.this$0 = mainActivity;
        }

        @Override
        public void onClick(View view) {
            MainActivityCore.callToggleFavorite(this.this$0, this.val$track);
            MainActivityCore.accessOverlayHost(this.this$0).removeView(this.val$shade);
            MainActivityCore.callRender(this.this$0);
        }
    }

    class UiAction56 implements View.OnClickListener {
        final MainActivityCore this$0;
        final FrameLayout val$shade;
        final Track val$track;

        UiAction56(MainActivityCore mainActivity, FrameLayout frameLayout, Track track) {
            this.val$shade = frameLayout;
            this.val$track = track;
            this.this$0 = mainActivity;
        }

        @Override
        public void onClick(View view) {
            MainActivityCore.accessOverlayHost(this.this$0).removeView(this.val$shade);
            MainActivityCore.callChoosePlaylistForTrack(this.this$0, this.val$track);
        }
    }

    class UiAction57 implements View.OnClickListener {
        final MainActivityCore this$0;
        final FrameLayout val$shade;
        final Track val$track;

        UiAction57(MainActivityCore mainActivity, FrameLayout frameLayout, Track track) {
            this.val$shade = frameLayout;
            this.val$track = track;
            this.this$0 = mainActivity;
        }

        @Override
        public void onClick(View view) {
            MainActivityCore.accessOverlayHost(this.this$0).removeView(this.val$shade);
            MainActivityCore.callConfirmDeleteTrack(this.this$0, this.val$track);
        }
    }

    class UiAction58 implements View.OnClickListener {
        final MainActivityCore this$0;
        final FrameLayout val$shade;

        UiAction58(MainActivityCore mainActivity, FrameLayout frameLayout) {
            this.val$shade = frameLayout;
            this.this$0 = mainActivity;
        }

        @Override
        public void onClick(View view) {
            MainActivityCore.accessOverlayHost(this.this$0).removeView(this.val$shade);
            MainActivityCore.callUpdateMini(this.this$0);
        }
    }

    private void choosePlaylistForTrack(Track track) {
        FrameLayout frameLayoutShade = shade();
        LinearLayout linearLayoutPanelCard = panelCard();
        linearLayoutPanelCard.setPadding(dp(16), dp(16), dp(16), dp(16));
        linearLayoutPanelCard.addView(text(tr3("Add to playlist", "Добавить в плейлист", "+ ▤"), 22, true), new LinearLayout.LayoutParams(-1, dp(48)));
        ScrollView scrollView = new ScrollView(this);
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(1);
        for (Playlist playlist : this.playlists) {
            Button button = button(playlist.name);
            button.setOnClickListener(new UiAction59(this, playlist, track, frameLayoutShade));
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-1, dp(52));
            layoutParams.setMargins(0, dp(4), 0, dp(4));
            linearLayout.addView(button, layoutParams);
        }
        Button button2 = button(tr3("Create new", "Создать новый", "+"));
        applyButtonColors(button2, this.fg, this.bg);
        button2.setOnClickListener(new UiAction60(this, frameLayoutShade, track));
        LinearLayout.LayoutParams layoutParams2 = new LinearLayout.LayoutParams(-1, dp(52));
        layoutParams2.setMargins(0, dp(8), 0, 0);
        linearLayout.addView(button2, layoutParams2);
        scrollView.addView(linearLayout);
        linearLayoutPanelCard.addView(scrollView, new LinearLayout.LayoutParams(-1, 0, 1.0f));
        frameLayoutShade.addView(linearLayoutPanelCard, centerParams(dp(330), dp(420)));
        this.overlayHost.addView(frameLayoutShade);
        updateMini();
    }

    class UiAction59 implements View.OnClickListener {
        final MainActivityCore this$0;
        final Playlist val$playlist;
        final FrameLayout val$shade;
        final Track val$track;

        UiAction59(MainActivityCore mainActivity, Playlist playlist, Track track, FrameLayout frameLayout) {
            this.val$playlist = playlist;
            this.val$track = track;
            this.val$shade = frameLayout;
            this.this$0 = mainActivity;
        }

        @Override
        public void onClick(View view) {
            this.this$0.playlistController.addTrackToPlaylist(this.val$playlist, this.val$track);
            MainActivityCore.accessOverlayHost(this.this$0).removeView(this.val$shade);
            MainActivityCore.callRender(this.this$0);
        }
    }

    class UiAction60 implements View.OnClickListener {
        final MainActivityCore this$0;
        final FrameLayout val$shade;
        final Track val$track;

        UiAction60(MainActivityCore mainActivity, FrameLayout frameLayout, Track track) {
            this.val$shade = frameLayout;
            this.val$track = track;
            this.this$0 = mainActivity;
        }

        @Override
        public void onClick(View view) {
            MainActivityCore.accessOverlayHost(this.this$0).removeView(this.val$shade);
            MainActivityCore.callCreatePlaylistAndAdd(this.this$0, this.val$track);
        }
    }

    class UiAction61 implements InputDone {
        final MainActivityCore this$0;
        final Track val$track;

        UiAction61(MainActivityCore mainActivity, Track track) {
            this.val$track = track;
            this.this$0 = mainActivity;
        }

        @Override
        public void done(String str) {
            this.this$0.playlistController.createPlaylistWithTrack(str, this.val$track);
            MainActivityCore.callRender(this.this$0);
        }
    }

    private void createPlaylistAndAdd(Track track) {
        showInputPanel(tr3("New playlist", "Новый плейлист", "+ ▤"), tr3("Playlist name", "Название плейлиста", "▤"), "", false, new UiAction61(this, track));
    }

    private void confirmDeleteTrack(Track track) {
        showConfirmPanel("Удалить песню?", "Песня исчезнет из приложения, но файл останется на телефоне.", new UiAction62(this, track));
    }

    class UiAction62 implements Runnable {
        final MainActivityCore this$0;
        final Track val$track;

        UiAction62(MainActivityCore mainActivity, Track track) {
            this.val$track = track;
            this.this$0 = mainActivity;
        }

        @Override
        public void run() {
            MainActivityCore.accessTracks(this.this$0).remove(this.val$track);
            MainActivityCore.accessFavorites(this.this$0).remove(this.val$track.uri);
            this.this$0.playlistController.removeTrackFromAllPlaylists(this.val$track);
            TrackStore.save(this.this$0, MainActivityCore.accessTracks(this.this$0));
            MainActivityCore.callSaveState(this.this$0);
            MainActivityCore.callRender(this.this$0);
        }
    }

    private void confirmDeletePlaylist(Playlist playlist) {
        showConfirmPanel("Удалить плейлист?", "Песни останутся в приложении.", new UiAction63(this, playlist));
    }

    class UiAction63 implements Runnable {
        final MainActivityCore this$0;
        final Playlist val$playlist;

        UiAction63(MainActivityCore mainActivity, Playlist playlist) {
            this.val$playlist = playlist;
            this.this$0 = mainActivity;
        }

        @Override
        public void run() {
            this.this$0.playlistController.deletePlaylist(this.val$playlist);
            MainActivityCore.callRender(this.this$0);
        }
    }

    class UiAction64 implements InputDone {
        UiAction64() {
        }

        @Override
        public void done(String str) {
            MainActivityCore.this.playlistController.createPlaylist(str);
            MainActivityCore.callRender(MainActivityCore.this);
        }
    }

    private void createPlaylistDialog() {
        showInputPanel(tr3("Create playlist", "Создать плейлист", "+ ▤"), tr3("Playlist name", "Название плейлиста", "▤"), "", false, new UiAction64());
    }

    private void renamePlaylistDialog(final Playlist playlist) {
        showInputPanel(tr3("Rename playlist", "Переименовать плейлист", "✎ ▤"), tr3("Playlist name", "Название плейлиста", "▤"), playlist.name, false, new InputDone() {
            @Override
            public void done(String value) {
                playlistController.renamePlaylist(playlist, value);
                render();
            }
        });
    }

    private void openSearch() {
        FrameLayout frameLayoutShade = shade();
        LinearLayout linearLayoutPanelCard = panelCard();
        linearLayoutPanelCard.setPadding(dp(16), dp(16), dp(16), dp(16));
        linearLayoutPanelCard.addView(text(tr3("Search", "Поиск", "⌕"), 22, true), new LinearLayout.LayoutParams(-1, dp(48)));
        EditText editText = new EditText(this);
        editText.setSingleLine(true);
        editText.setText(this.search);
        editText.setHint(tr3("Find", "Найти", "⌕"));
        editText.setTextColor(this.fg);
        editText.setHintTextColor(this.muted);
        editText.setTextSize(18.0f);
        editText.setPadding(dp(14), 0, dp(14), 0);
        editText.setInputType(1);
        editText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(80)});
        setSurface(editText, this.panel, true);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-1, dp(58));
        layoutParams.setMargins(0, dp(10), 0, dp(16));
        linearLayoutPanelCard.addView(editText, layoutParams);
        LinearLayout linearLayoutRow = row();
        Button button = button(tr3("Reset", "Сброс", "↺"));
        button.setOnClickListener(new UiAction65(this, frameLayoutShade));
        linearLayoutRow.addView(button, new LinearLayout.LayoutParams(0, dp(54), 1.0f));
        Button button2 = button(tr3("Find", "Найти", "⌕"));
        applyButtonColors(button2, this.fg, this.bg);
        button2.setOnClickListener(new UiAction66(this, editText, frameLayoutShade));
        linearLayoutRow.addView(button2, new LinearLayout.LayoutParams(0, dp(54), 1.0f));
        linearLayoutPanelCard.addView(linearLayoutRow);
        frameLayoutShade.addView(linearLayoutPanelCard, centerParams(dp(330), dp(230)));
        this.overlayHost.addView(frameLayoutShade);
        editText.requestFocus();
        updateMini();
    }

    class UiAction65 implements View.OnClickListener {
        final MainActivityCore this$0;
        final FrameLayout val$shade;

        UiAction65(MainActivityCore mainActivity, FrameLayout frameLayout) {
            this.val$shade = frameLayout;
            this.this$0 = mainActivity;
        }

        @Override
        public void onClick(View view) {
            MainActivityCore.setSearchValue(this.this$0, "");
            MainActivityCore.accessOverlayHost(this.this$0).removeView(this.val$shade);
            MainActivityCore.callRender(this.this$0);
        }
    }

    class UiAction66 implements View.OnClickListener {
        final MainActivityCore this$0;
        final EditText val$input;
        final FrameLayout val$shade;

        UiAction66(MainActivityCore mainActivity, EditText editText, FrameLayout frameLayout) {
            this.val$input = editText;
            this.val$shade = frameLayout;
            this.this$0 = mainActivity;
        }

        @Override
        public void onClick(View view) {
            MainActivityCore.setSearchValue(this.this$0, this.val$input.getText().toString());
            MainActivityCore.accessOverlayHost(this.this$0).removeView(this.val$shade);
            MainActivityCore.callRender(this.this$0);
        }
    }

    void showInputPanel(String str, String str2, String str3, boolean z, InputDone inputDone) {
        FrameLayout frameLayoutShade = shade();
        LinearLayout linearLayoutPanelCard = panelCard();
        linearLayoutPanelCard.setPadding(dp(16), dp(16), dp(16), dp(16));
        LinearLayout linearLayoutRow = row();
        linearLayoutRow.addView(text(str, 22, true), new LinearLayout.LayoutParams(0, dp(58), 1.0f));
        Button buttonIcon = icon("×");
        buttonIcon.setOnClickListener(new UiAction67(this, frameLayoutShade));
        linearLayoutRow.addView(buttonIcon, square(52));
        linearLayoutPanelCard.addView(linearLayoutRow);
        EditText editText = new EditText(this);
        editText.setSingleLine(true);
        editText.setText(str3);
        editText.setHint(str2);
        editText.setTextColor(this.fg);
        editText.setHintTextColor(this.muted);
        editText.setTextSize(18.0f);
        editText.setPadding(dp(14), 0, dp(14), 0);
        editText.setInputType(z ? 2 : 1);
        editText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(z ? 3 : 80)});
        setSurface(editText, this.panel, true);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-1, dp(58));
        layoutParams.setMargins(0, dp(10), 0, dp(16));
        linearLayoutPanelCard.addView(editText, layoutParams);
        LinearLayout linearLayoutRow2 = row();
        Button button = button(tr3("Cancel", "Отмена", "×"));
        button.setOnClickListener(new UiAction68(this, frameLayoutShade));
        linearLayoutRow2.addView(button, new LinearLayout.LayoutParams(0, dp(54), 1.0f));
        Button button2 = button(tr3("Done", "Готово", "✓"));
        applyButtonColors(button2, this.fg, this.bg);
        button2.setOnClickListener(new UiAction69(this, editText, frameLayoutShade, inputDone));
        linearLayoutRow2.addView(button2, new LinearLayout.LayoutParams(0, dp(54), 1.0f));
        linearLayoutPanelCard.addView(linearLayoutRow2);
        frameLayoutShade.addView(linearLayoutPanelCard, centerParams(dp(330), dp(230)));
        this.overlayHost.addView(frameLayoutShade);
        editText.requestFocus();
    }

    class UiAction67 implements View.OnClickListener {
        final MainActivityCore this$0;
        final FrameLayout val$shade;

        UiAction67(MainActivityCore mainActivity, FrameLayout frameLayout) {
            this.val$shade = frameLayout;
            this.this$0 = mainActivity;
        }

        @Override
        public void onClick(View view) {
            MainActivityCore.accessOverlayHost(this.this$0).removeView(this.val$shade);
            MainActivityCore.callUpdateMini(this.this$0);
        }
    }

    class UiAction68 implements View.OnClickListener {
        final MainActivityCore this$0;
        final FrameLayout val$shade;

        UiAction68(MainActivityCore mainActivity, FrameLayout frameLayout) {
            this.val$shade = frameLayout;
            this.this$0 = mainActivity;
        }

        @Override
        public void onClick(View view) {
            MainActivityCore.accessOverlayHost(this.this$0).removeView(this.val$shade);
            MainActivityCore.callUpdateMini(this.this$0);
        }
    }

    class UiAction69 implements View.OnClickListener {
        final MainActivityCore this$0;
        final InputDone val$done;
        final EditText val$input;
        final FrameLayout val$shade;

        UiAction69(MainActivityCore mainActivity, EditText editText, FrameLayout frameLayout, InputDone inputDone) {
            this.val$input = editText;
            this.val$shade = frameLayout;
            this.val$done = inputDone;
            this.this$0 = mainActivity;
        }

        @Override
        public void onClick(View view) {
            String string = this.val$input.getText().toString();
            MainActivityCore.accessOverlayHost(this.this$0).removeView(this.val$shade);
            this.val$done.done(string);
            MainActivityCore.callUpdateMini(this.this$0);
        }
    }

    void closeFullPlayer(FrameLayout frameLayout, boolean animate) {
        if (frameLayout == null || frameLayout.getParent() == null) {
            updateMini();
            return;
        }
        if (animate && this.animations) {
            frameLayout.animate().translationY(getResources().getDisplayMetrics().heightPixels).alpha(0.0f).setDuration(135L).setInterpolator(new DecelerateInterpolator()).withEndAction(new FullPlayerCloseAction(this, frameLayout)).start();
            return;
        }
        this.overlayHost.removeView(frameLayout);
        updateMini();
    }

    class FullPlayerCloseAction implements Runnable {
        final MainActivityCore this$0;
        final FrameLayout val$sheet;

        FullPlayerCloseAction(MainActivityCore mainActivity, FrameLayout frameLayout) {
            this.val$sheet = frameLayout;
            this.this$0 = mainActivity;
        }

        @Override
        public void run() {
            if (this.val$sheet.getParent() != null) {
                MainActivityCore.accessOverlayHost(this.this$0).removeView(this.val$sheet);
            }
            MainActivityCore.callUpdateMini(this.this$0);
        }
    }

    void openFullPlayer() {
        this.playerUiController.openFullPlayer();
    }

    void renderFullPlayerSheet() {
        this.playerUiController.renderFullPlayerSheet();
    }

    void renderFullPlayerSheetInternal() {
        if (this.currentIndex < 0 && !this.tracks.isEmpty()) {
            this.currentIndex = 0;
        }
        if (this.currentIndex < 0) {
            return;
        }
        this.miniPlayer.setVisibility(8);
        Track track = this.tracks.get(this.currentIndex);
        FrameLayout frameLayout = new FrameLayout(this) {
            private boolean draggingDown = false;
            private boolean closingDown = false;
            private float startX = 0.0f;
            private float startY = 0.0f;
            private float startTranslationY = 0.0f;

            @Override
            public boolean dispatchTouchEvent(MotionEvent motionEvent) {
                int action = motionEvent.getActionMasked();
                if (action == MotionEvent.ACTION_DOWN) {
                    this.draggingDown = false;
                    this.closingDown = false;
                    this.startX = motionEvent.getRawX();
                    this.startY = motionEvent.getRawY();
                    this.startTranslationY = getTranslationY();
                    animate().cancel();
                    setAlpha(1.0f);
                    super.dispatchTouchEvent(motionEvent);
                    return true;
                } else if (action == MotionEvent.ACTION_MOVE) {
                    if (this.closingDown) {
                        return true;
                    }
                    float dx = motionEvent.getRawX() - this.startX;
                    float dy = motionEvent.getRawY() - this.startY;
                    if (!this.draggingDown && dy > dp(8) && dy > Math.abs(dx) * 0.75f) {
                        this.draggingDown = true;
                        MotionEvent cancelEvent = MotionEvent.obtain(motionEvent);
                        cancelEvent.setAction(MotionEvent.ACTION_CANCEL);
                        super.dispatchTouchEvent(cancelEvent);
                        cancelEvent.recycle();
                        getParent().requestDisallowInterceptTouchEvent(true);
                    }
                    if (this.draggingDown) {
                        float drag = Math.max(0.0f, this.startTranslationY + dy);
                        setTranslationY(drag);
                        setAlpha(Math.max(0.55f, 1.0f - (drag / Math.max(1, getHeight()))));
                        return true;
                    }
                    super.dispatchTouchEvent(motionEvent);
                    return true;
                } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                    if (this.closingDown) {
                        return true;
                    }
                    if (this.draggingDown) {
                        this.draggingDown = false;
                        float dy = motionEvent.getRawY() - this.startY;
                        float drag = Math.max(0.0f, this.startTranslationY + dy);
                        if (action == MotionEvent.ACTION_UP && drag > dp(56)) {
                            this.closingDown = true;
                            MainActivityCore.this.closeFullPlayer(this, true);
                        } else if (MainActivityCore.this.animations) {
                            animate().translationY(0.0f).alpha(1.0f).setDuration(120L).setInterpolator(new DecelerateInterpolator()).start();
                        } else {
                            setTranslationY(0.0f);
                            setAlpha(1.0f);
                        }
                        return true;
                    }
                    super.dispatchTouchEvent(motionEvent);
                    return true;
                }
                super.dispatchTouchEvent(motionEvent);
                return true;
            }
        };
        frameLayout.setBackgroundColor(this.bg);
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(1);
        linearLayout.setPadding(dp(16), dp(18), dp(16), dp(20));
        frameLayout.addView(linearLayout, new FrameLayout.LayoutParams(-1, -1));
        LinearLayout linearLayoutRow = row();
        Button buttonIcon = icon("←");
        buttonIcon.setTextSize(34.0f);
        buttonIcon.setTypeface(Typeface.DEFAULT_BOLD);
        buttonIcon.setOnClickListener(new UiAction70(this, frameLayout));
        linearLayoutRow.addView(buttonIcon, square(58));
        linearLayoutRow.addView(new View(this), new LinearLayout.LayoutParams(0, 1, 1.0f));
        Button buttonIcon2 = icon("☰");
        buttonIcon2.setOnClickListener(new UiAction71());
        linearLayoutRow.addView(buttonIcon2, square(58));
        linearLayout.addView(linearLayoutRow, new LinearLayout.LayoutParams(-1, dp(72)));
        ImageView imageViewCoverView = coverView();
        loadCover(imageViewCoverView, track, this.dark ? Color.rgb(28, 28, 28) : Color.rgb(235, 235, 235), COVER_FULL_SIZE);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(dp(280), dp(280));
        layoutParams.gravity = 1;
        linearLayout.addView(imageViewCoverView, layoutParams);
        TextView textViewText = text(track.title, 24, true);
        textViewText.setGravity(17);
        linearLayout.addView(textViewText, new LinearLayout.LayoutParams(-1, dp(54)));
        TextView textViewText2 = text(track.artist + " · " + (queueIndexOf(track) + 1) + " " + tr3("of", "из", "/") + " " + (this.playbackQueue.isEmpty() ? this.tracks : this.playbackQueue).size(), 15, false);
        textViewText2.setGravity(17);
        linearLayout.addView(textViewText2, new LinearLayout.LayoutParams(-1, dp(34)));
        LinearLayout linearLayoutRow2 = row();
        Button button = button(timerButtonText());
        button.setOnClickListener(new UiAction72());
        linearLayoutRow2.addView(button, new LinearLayout.LayoutParams(0, dp(58), 1.0f));
        Button button2 = button(tr3("Like", this.favorites.contains(track.uri) ? "♥︎ Лайк" : "♡︎ Лайк", this.favorites.contains(track.uri) ? "♥" : "♡"));
        button2.setOnClickListener(new UiAction73(this, track, frameLayout));
        linearLayoutRow2.addView(button2, new LinearLayout.LayoutParams(0, dp(58), 1.0f));
        Button button3 = button(loopLabel());
        button3.setOnClickListener(new UiAction74(this, frameLayout));
        linearLayoutRow2.addView(button3, new LinearLayout.LayoutParams(0, dp(58), 1.0f));
        linearLayout.addView(linearLayoutRow2);
        SeekBar seekBar = new SeekBar(this);
        applySeekBarColors(seekBar);
        int displayDuration = playbackDurationFor(track);
        seekBar.setMax(Math.max(1, displayDuration));
        seekBar.setProgress(Math.max(0, PlayerService.lastPosition));
        linearLayout.addView(seekBar, new LinearLayout.LayoutParams(-1, dp(42)));
        LinearLayout linearLayoutRow3 = row();
        TextView textViewText3 = text(formatMs(PlayerService.lastPosition), 13, false);
        TextView textViewText4 = text("-" + formatMs(Math.max(0, displayDuration - PlayerService.lastPosition)), 13, false);
        textViewText4.setGravity(TAB_CYCLES);
        linearLayoutRow3.addView(textViewText3, new LinearLayout.LayoutParams(0, dp(28), 1.0f));
        linearLayoutRow3.addView(textViewText4, new LinearLayout.LayoutParams(0, dp(28), 1.0f));
        linearLayout.addView(linearLayoutRow3);
        seekBar.setOnSeekBarChangeListener(new UiAction75(this, track, textViewText3, textViewText4));
        Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(new UiAction76(this, frameLayout, track, seekBar, textViewText3, textViewText4, button, handler), 700L);
        linearLayout.addView(new View(this), new LinearLayout.LayoutParams(-1, 0, 1.0f));
        LinearLayout linearLayoutRow4 = row();
        linearLayoutRow4.setGravity(17);
        Button buttonIcon3 = icon("⏮");
        buttonIcon3.setOnClickListener(new UiAction77(this, frameLayout));
        linearLayoutRow4.addView(buttonIcon3, square(68));
        Button buttonIcon4 = icon(this.playing ? "Ⅱ" : "▶");
        buttonIcon4.setOnClickListener(new UiAction78(this, frameLayout));
        linearLayoutRow4.addView(buttonIcon4, square(84));
        Button buttonIcon5 = icon("⏭");
        buttonIcon5.setOnClickListener(new UiAction79(this, frameLayout));
        linearLayoutRow4.addView(buttonIcon5, square(68));
        linearLayout.addView(linearLayoutRow4, new LinearLayout.LayoutParams(-1, dp(112)));
        boolean animateOpen = this.animations && this.fullPlayerOpening;
        this.fullPlayerOpening = false;
        this.overlayHost.addView(frameLayout, new FrameLayout.LayoutParams(-1, -1));
        if (animateOpen) {
            frameLayout.setTranslationY(getResources().getDisplayMetrics().heightPixels);
            frameLayout.animate().translationY(0.0f).setDuration(145L).setInterpolator(new DecelerateInterpolator()).start();
        }
    }

    class UiAction70 implements View.OnClickListener {
        final MainActivityCore this$0;
        final FrameLayout val$sheet;

        UiAction70(MainActivityCore mainActivity, FrameLayout frameLayout) {
            this.val$sheet = frameLayout;
            this.this$0 = mainActivity;
        }

        @Override
        public void onClick(View view) {
            MainActivityCore.accessOverlayHost(this.this$0).removeView(this.val$sheet);
            MainActivityCore.callUpdateMini(this.this$0);
        }
    }

    class UiAction71 implements View.OnClickListener {
        UiAction71() {
        }

        @Override
        public void onClick(View view) {
            MainActivityCore.callOpenQueuePanel(MainActivityCore.this);
        }
    }

    class UiAction72 implements View.OnClickListener {
        UiAction72() {
        }

        @Override
        public void onClick(View view) {
            MainActivityCore.callTimerDialog(MainActivityCore.this);
        }
    }

    class UiAction73 implements View.OnClickListener {
        final MainActivityCore this$0;
        final FrameLayout val$sheet;
        final Track val$track;

        UiAction73(MainActivityCore mainActivity, Track track, FrameLayout frameLayout) {
            this.val$track = track;
            this.val$sheet = frameLayout;
            this.this$0 = mainActivity;
        }

        @Override
        public void onClick(View view) {
            MainActivityCore.callToggleFavorite(this.this$0, this.val$track);
            MainActivityCore.accessOverlayHost(this.this$0).removeView(this.val$sheet);
            MainActivityCore.callOpenFullPlayer(this.this$0);
        }
    }

    class UiAction74 implements View.OnClickListener {
        final MainActivityCore this$0;
        final FrameLayout val$sheet;

        UiAction74(MainActivityCore mainActivity, FrameLayout frameLayout) {
            this.val$sheet = frameLayout;
            this.this$0 = mainActivity;
        }

        @Override
        public void onClick(View view) {
            MainActivityCore.setLoopModeValue(this.this$0, (MainActivityCore.accessLoopMode(this.this$0) + 1) % 3);
            Intent intent = new Intent(this.this$0, (Class<?>) PlayerService.class);
            intent.setAction(PlayerService.ACTION_LOOP);
            intent.putExtra(PlayerService.EXTRA_LOOP_MODE, MainActivityCore.accessLoopMode(this.this$0));
            if (Build.VERSION.SDK_INT >= 26) {
                this.this$0.startForegroundService(intent);
            } else {
                this.this$0.startService(intent);
            }
            MainActivityCore.accessOverlayHost(this.this$0).removeView(this.val$sheet);
            MainActivityCore.callOpenFullPlayer(this.this$0);
        }
    }

    class UiAction75 implements SeekBar.OnSeekBarChangeListener {
        final MainActivityCore this$0;
        final TextView val$elapsed;
        final TextView val$remain;
        final Track val$track;

        UiAction75(MainActivityCore mainActivity, Track track, TextView textView, TextView textView2) {
            this.val$track = track;
            this.val$elapsed = textView;
            this.val$remain = textView2;
            this.this$0 = mainActivity;
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int i, boolean z) {
            if (z) {
                this.val$elapsed.setText(MainActivityCore.callFormatMs(this.this$0, i));
                this.val$remain.setText("-" + MainActivityCore.callFormatMs(this.this$0, Math.max(0, this.this$0.playbackDurationFor(this.val$track) - i)));
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            Intent intent = new Intent(this.this$0, (Class<?>) PlayerService.class);
            intent.setAction(PlayerService.ACTION_SEEK);
            intent.putExtra(PlayerService.EXTRA_POSITION, seekBar.getProgress());
            if (Build.VERSION.SDK_INT < 26) {
                this.this$0.startService(intent);
            } else {
                this.this$0.startForegroundService(intent);
            }
        }
    }

    class UiAction76 implements Runnable {
        final MainActivityCore this$0;
        final TextView val$elapsed;
        final Handler val$handler;
        final TextView val$remain;
        final SeekBar val$seek;
        final FrameLayout val$sheet;
        final Button val$timer;
        final Track val$track;

        UiAction76(MainActivityCore mainActivity, FrameLayout frameLayout, Track track, SeekBar seekBar, TextView textView, TextView textView2, Button button, Handler handler) {
            this.val$sheet = frameLayout;
            this.val$track = track;
            this.val$seek = seekBar;
            this.val$elapsed = textView;
            this.val$remain = textView2;
            this.val$timer = button;
            this.val$handler = handler;
            this.this$0 = mainActivity;
        }

        @Override
        public void run() {
            Track resolvedTrack;
            if (this.val$sheet.getParent() == null) {
                return;
            }
            PlayerService.refreshSnapshot();
            if (PlayerService.lastIndex < 0) {
                MainActivityCore.setCurrentIndexValue(this.this$0, -1);
                MainActivityCore.setPlayingValue(this.this$0, false);
                MainActivityCore.accessOverlayHost(this.this$0).removeView(this.val$sheet);
                MainActivityCore.callUpdateMini(this.this$0);
                MainActivityCore.callRender(this.this$0);
                return;
            }
            if (PlayerService.lastUri != null && !PlayerService.lastUri.isEmpty() && !PlayerService.lastUri.equals(this.val$track.uri) && (resolvedTrack = MainActivityCore.callFindTrack(this.this$0, PlayerService.lastUri)) != null) {
                MainActivityCore.setCurrentIndexValue(this.this$0, MainActivityCore.accessTracks(this.this$0).indexOf(resolvedTrack));
                MainActivityCore.accessOverlayHost(this.this$0).removeView(this.val$sheet);
                MainActivityCore.callOpenFullPlayer(this.this$0);
                MainActivityCore.callRender(this.this$0);
                return;
            }
            int displayDuration = this.this$0.playbackDurationFor(this.val$track);
            this.val$seek.setMax(Math.max(1, displayDuration));
            this.val$seek.setProgress(Math.max(0, PlayerService.lastPosition));
            this.val$elapsed.setText(MainActivityCore.callFormatMs(this.this$0, PlayerService.lastPosition));
            this.val$remain.setText("-" + MainActivityCore.callFormatMs(this.this$0, Math.max(0, displayDuration - PlayerService.lastPosition)));
            this.val$timer.setText(MainActivityCore.callTimerButtonText(this.this$0));
            this.val$handler.postDelayed(this, 250L);
        }
    }

    class UiAction77 implements View.OnClickListener {
        final MainActivityCore this$0;
        final FrameLayout val$sheet;

        UiAction77(MainActivityCore mainActivity, FrameLayout frameLayout) {
            this.val$sheet = frameLayout;
            this.this$0 = mainActivity;
        }

        @Override
        public void onClick(View view) {
            MainActivityCore.callPrevious(this.this$0);
            MainActivityCore.accessOverlayHost(this.this$0).removeView(this.val$sheet);
            MainActivityCore.callOpenFullPlayer(this.this$0);
        }
    }

    class UiAction78 implements View.OnClickListener {
        final MainActivityCore this$0;
        final FrameLayout val$sheet;

        UiAction78(MainActivityCore mainActivity, FrameLayout frameLayout) {
            this.val$sheet = frameLayout;
            this.this$0 = mainActivity;
        }

        @Override
        public void onClick(View view) {
            MainActivityCore.callToggleCurrent(this.this$0);
            MainActivityCore.accessOverlayHost(this.this$0).removeView(this.val$sheet);
            MainActivityCore.callOpenFullPlayer(this.this$0);
        }
    }

    class UiAction79 implements View.OnClickListener {
        final MainActivityCore this$0;
        final FrameLayout val$sheet;

        UiAction79(MainActivityCore mainActivity, FrameLayout frameLayout) {
            this.val$sheet = frameLayout;
            this.this$0 = mainActivity;
        }

        @Override
        public void onClick(View view) {
            MainActivityCore.callNext(this.this$0);
            MainActivityCore.accessOverlayHost(this.this$0).removeView(this.val$sheet);
            MainActivityCore.callOpenFullPlayer(this.this$0);
        }
    }

    String loopLabel() {
        return this.playbackController.loopLabel();
    }

    String formatMs(int i) {
        int iMax = Math.max(0, i / 1000);
        return (iMax / 60) + ":" + String.format(Locale.ROOT, "%02d", Integer.valueOf(iMax % 60));
    }

    String formatTrackDuration(Track track) {
        return track.durationMs > 0 ? formatMs(track.durationMs) : "--:--";
    }

    int playbackDurationFor(Track track) {
        int serviceDuration = Math.max(0, PlayerService.lastDuration);
        if (serviceDuration > 0) {
            return serviceDuration;
        }
        return track == null ? 0 : Math.max(0, track.durationMs);
    }

    String formatSeconds(long j) {
        long jMax = Math.max(0L, j);
        return (jMax / 60) + ":" + String.format(Locale.ROOT, "%02d", Long.valueOf(jMax % 60));
    }

    void timerDialog() {
        this.sleepTimerController.openDialog();
    }

    class UiAction80 implements View.OnClickListener {
        final MainActivityCore this$0;
        final FrameLayout val$shade;
        final int val$value;

        UiAction80(MainActivityCore mainActivity, FrameLayout frameLayout, int i) {
            this.val$shade = frameLayout;
            this.val$value = i;
            this.this$0 = mainActivity;
        }

        @Override
        public void onClick(View view) {
            MainActivityCore.accessOverlayHost(this.this$0).removeView(this.val$shade);
            MainActivityCore.callStartSleepTimer(this.this$0, this.val$value);
        }
    }

    class UiAction81 implements View.OnClickListener {
        final MainActivityCore this$0;
        final FrameLayout val$shade;

        UiAction81(MainActivityCore mainActivity, FrameLayout frameLayout) {
            this.val$shade = frameLayout;
            this.this$0 = mainActivity;
        }

        @Override
        public void onClick(View view) {
            MainActivityCore.accessOverlayHost(this.this$0).removeView(this.val$shade);
            MainActivityCore.callCustomTimerDialog(this.this$0);
        }
    }

    class UiAction82 implements View.OnClickListener {
        final MainActivityCore this$0;
        final FrameLayout val$shade;

        UiAction82(MainActivityCore mainActivity, FrameLayout frameLayout) {
            this.val$shade = frameLayout;
            this.this$0 = mainActivity;
        }

        @Override
        public void onClick(View view) {
            MainActivityCore.accessOverlayHost(this.this$0).removeView(this.val$shade);
            MainActivityCore.callCancelSleepTimer(this.this$0);
        }
    }

    class UiAction83 implements InputDone {
        UiAction83() {
        }

        @Override
        public void done(String str) {
            try {
                MainActivityCore.setCustomTimerMinutesValue(MainActivityCore.this, Math.max(1, Integer.parseInt(str.trim())));
                MainActivityCore.callSaveState(MainActivityCore.this);
                MainActivityCore.callStartSleepTimer(MainActivityCore.this, MainActivityCore.accessCustomTimerMinutes(MainActivityCore.this));
            } catch (Exception e) {
            }
        }
    }

    void customTimerDialog() {
        this.sleepTimerController.openCustomDialog();
    }

    void startSleepTimer(int i) {
        this.sleepTimerController.start(i);
    }

    class UiAction84 implements Runnable {
        UiAction84() {
        }

        @Override
        public void run() {
            Intent intent = new Intent(MainActivityCore.this, (Class<?>) PlayerService.class);
            intent.setAction(PlayerService.ACTION_STOP);
            if (Build.VERSION.SDK_INT >= 26) {
                MainActivityCore.this.startForegroundService(intent);
            } else {
                MainActivityCore.this.startService(intent);
            }
            MainActivityCore.setPlayingValue(MainActivityCore.this, false);
            MainActivityCore.setCurrentIndexValue(MainActivityCore.this, -1);
            MainActivityCore.setSleepTimerEndsAtValue(MainActivityCore.this, 0L);
            MainActivityCore.callUpdateMini(MainActivityCore.this);
            MainActivityCore.callRender(MainActivityCore.this);
        }
    }

    void cancelSleepTimer() {
        this.sleepTimerController.cancel();
    }

    String timerButtonText() {
        return this.sleepTimerController.buttonText();
    }

    void playTrack(Track track) {
        this.playbackController.playTrack(track);
    }

    void playTrackInternal(Track track) {
        this.playbackController.playTrack(track);
    }

    void playTrack(Track track, boolean z) {
        this.playbackController.playTrack(track, z);
    }

    void playTrackInternal(Track track, boolean z) {
        this.playbackController.playTrack(track, z);
    }

    void playList(ArrayList<Track> arrayList, boolean z) {
        this.playbackController.playList(arrayList, z);
    }

    void toggleCurrent() {
        this.playbackController.toggleCurrent();
    }

    void toggleCurrentInternal() {
        this.playbackController.toggleCurrent();
    }

    private void next() {
        this.playbackController.next();
    }

    void nextInternal() {
        this.playbackController.next();
    }

    private void previous() {
        this.playbackController.previous();
    }

    void previousInternal() {
        this.playbackController.previous();
    }

    void startPlaybackWatcher() {
        this.playbackController.startPlaybackWatcher();
    }

    void cycleLoopMode() {
        this.playbackController.cycleLoopMode();
    }

    void seekTo(int position) {
        this.playbackController.seekTo(position);
    }

    void startServiceAction(String str, int i) {
        this.playbackController.startServiceAction(str, i);
    }

    void startServiceAction(String str, int i, boolean z) {
        this.playbackController.startServiceAction(str, i, z);
    }

    void startServiceAction(String str, int i, boolean z, int position) {
        this.playbackController.startServiceAction(str, i, z, position);
    }

    ArrayList<String> queueUris() {
        return this.playbackController.queueUris();
    }

    ArrayList<Track> activeQueue() {
        return this.playbackController.activeQueue();
    }

    boolean isPlayingSource(ArrayList<Track> arrayList) {
        return this.playbackController.isPlayingSource(arrayList);
    }

    int queueIndexOf(Track track) {
        return this.playbackController.queueIndexOf(track);
    }

    void updateMini() {
        this.playerUiController.updateMini();
    }

    void updateMiniInternal() {
        this.playerUiController.updateMiniState();
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
        return this.currentIndex >= 0 && this.currentIndex < this.tracks.size() && this.tracks.get(this.currentIndex).uri.equals(track.uri);
    }

    Track findTrack(String str) {
        for (Track track : this.tracks) {
            if (track.uri.equals(str)) {
                return track;
            }
        }
        return null;
    }

    private Bitmap cover(Track track) {
        String key = coverCacheKey(track, COVER_THUMB_SIZE);
        Bitmap cached = this.coverCache.get(key);
        if (cached != null) {
            return cached;
        }
        Bitmap cover = readCover(track);
        if (cover != null) {
            this.coverCache.put(key, cover);
        }
        return cover;
    }

    void loadCover(final ImageView imageView, final Track track, int fallbackColor) {
        loadCover(imageView, track, fallbackColor, COVER_THUMB_SIZE);
    }

    void loadCover(final ImageView imageView, final Track track, int fallbackColor, final int maxSize) {
        final String key = coverCacheKey(track, maxSize);
        Object currentTag = imageView.getTag();
        if (key.equals(currentTag) && imageView.getDrawable() != null) {
            return;
        }
        imageView.setTag(key);
        Bitmap cached = this.coverCache.get(key);
        if (cached != null) {
            imageView.setImageBitmap(cached);
            return;
        }
        Bitmap thumbCached = maxSize == COVER_THUMB_SIZE ? null : this.coverCache.get(coverCacheKey(track, COVER_THUMB_SIZE));
        if (thumbCached != null) {
            imageView.setImageBitmap(thumbCached);
        } else {
            imageView.setImageDrawable(null);
            imageView.setBackgroundColor(fallbackColor);
        }
        synchronized (this.pendingCoverTargets) {
            ArrayList<ImageView> waitingTargets = this.pendingCoverTargets.get(key);
            if (waitingTargets != null) {
                waitingTargets.add(imageView);
                return;
            }
            waitingTargets = new ArrayList<>();
            waitingTargets.add(imageView);
            this.pendingCoverTargets.put(key, waitingTargets);
        }
        this.coverExecutor.execute(new Runnable() {
            @Override
            public void run() {
                final Bitmap bitmap = MainActivityCore.this.readCover(track, maxSize);
                if (bitmap != null) {
                    MainActivityCore.this.coverCache.put(key, bitmap);
                    if (maxSize != COVER_THUMB_SIZE) {
                        MainActivityCore.this.cacheThumbnailFromFullCover(track, bitmap);
                    }
                }
                final ArrayList<ImageView> targets;
                synchronized (MainActivityCore.this.pendingCoverTargets) {
                    targets = MainActivityCore.this.pendingCoverTargets.remove(key);
                }
                MainActivityCore.this.uiHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (bitmap == null || targets == null) {
                            return;
                        }
                        for (ImageView target : targets) {
                            if (target != null && key.equals(target.getTag())) {
                                target.setImageBitmap(bitmap);
                            }
                        }
                    }
                });
            }
        });
    }

    void seedCoverCacheFromView(ImageView imageView, Track track) {
        if (imageView == null || track == null || !(imageView.getDrawable() instanceof BitmapDrawable)) {
            return;
        }
        Bitmap bitmap = ((BitmapDrawable) imageView.getDrawable()).getBitmap();
        if (bitmap == null || bitmap.isRecycled()) {
            return;
        }
        this.coverCache.put(coverCacheKey(track, COVER_THUMB_SIZE), bitmap);
    }

    private void cacheThumbnailFromFullCover(Track track, Bitmap fullCover) {
        String thumbKey = coverCacheKey(track, COVER_THUMB_SIZE);
        if (this.coverCache.get(thumbKey) != null || fullCover == null || fullCover.isRecycled()) {
            return;
        }
        int width = fullCover.getWidth();
        int height = fullCover.getHeight();
        if (width <= 0 || height <= 0) {
            return;
        }
        float scale = Math.min((float) COVER_THUMB_SIZE / (float) width, (float) COVER_THUMB_SIZE / (float) height);
        if (scale >= 1.0f) {
            this.coverCache.put(thumbKey, fullCover);
            return;
        }
        Bitmap thumb = Bitmap.createScaledBitmap(fullCover, Math.max(1, Math.round(width * scale)), Math.max(1, Math.round(height * scale)), true);
        this.coverCache.put(thumbKey, thumb);
    }

    private String coverCacheKey(Track track, int maxSize) {
        return track.uri + "#" + maxSize;
    }

    private Bitmap readCover(Track track) {
        return readCover(track, COVER_THUMB_SIZE);
    }

    private Bitmap readCover(Track track, int maxSize) {
        MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
        try {
            mediaMetadataRetriever.setDataSource(this, track.asUri());
            byte[] embeddedPicture = mediaMetadataRetriever.getEmbeddedPicture();
            if (embeddedPicture == null || embeddedPicture.length > MAX_COVER_BYTES) {
                try {
                    mediaMetadataRetriever.release();
                } catch (Exception e) {
                }
                return null;
            }
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(embeddedPicture, 0, embeddedPicture.length, bounds);
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = coverSampleSize(bounds, maxSize);
            Bitmap bitmapDecodeByteArray = BitmapFactory.decodeByteArray(embeddedPicture, 0, embeddedPicture.length, options);
            if (bitmapDecodeByteArray != null && (bitmapDecodeByteArray.getWidth() > maxSize || bitmapDecodeByteArray.getHeight() > maxSize)) {
                float scale = Math.min((float) maxSize / (float) bitmapDecodeByteArray.getWidth(), (float) maxSize / (float) bitmapDecodeByteArray.getHeight());
                int width = Math.max(1, Math.round(bitmapDecodeByteArray.getWidth() * scale));
                int height = Math.max(1, Math.round(bitmapDecodeByteArray.getHeight() * scale));
                Bitmap scaled = Bitmap.createScaledBitmap(bitmapDecodeByteArray, width, height, true);
                if (scaled != bitmapDecodeByteArray) {
                    bitmapDecodeByteArray.recycle();
                }
                bitmapDecodeByteArray = scaled;
            }
            try {
                mediaMetadataRetriever.release();
            } catch (Exception e2) {
            }
            return bitmapDecodeByteArray;
        } catch (Throwable e3) {
            try {
                mediaMetadataRetriever.release();
            } catch (Exception e4) {
            }
            return null;
        }
    }

    private int coverSampleSize(BitmapFactory.Options options, int maxSize) {
        int sampleSize = 1;
        int width = options.outWidth;
        int height = options.outHeight;
        while (width / sampleSize > maxSize * 2 || height / sampleSize > maxSize * 2) {
            sampleSize *= 2;
        }
        return Math.max(1, sampleSize);
    }

    WaveformView wave(Track track, boolean z) {
        WaveformView waveformView = new WaveformView(this, track.title + track.uri, z ? this.purple : this.purpleSoft, z && this.playing);
        waveformView.setMinimumHeight(dp(28));
        waveformView.setPadding(0, dp(3), 0, dp(3));
        waveformView.setLayoutParams(new LinearLayout.LayoutParams(dp(190), dp(30)));
        return waveformView;
    }

    private void openPicker() {
        Intent intent = new Intent("android.intent.action.OPEN_DOCUMENT");
        intent.addCategory("android.intent.category.OPENABLE");
        intent.setType("audio/*");
        intent.putExtra("android.intent.extra.ALLOW_MULTIPLE", true);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(Intent.createChooser(intent, tr3("Choose music", "Выберите музыку", "+ ♪")), PICK_AUDIO);
    }

    private void openFolderPicker() {
        Intent intent = new Intent("android.intent.action.OPEN_DOCUMENT_TREE");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION | Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);
        startActivityForResult(Intent.createChooser(intent, tr3("Choose music folder", "Выберите папку с музыкой", "▣ ♪")), PICK_AUDIO_FOLDER);
    }

    @Override
    protected void onActivityResult(int i, int i2, Intent intent) {
        super.onActivityResult(i, i2, intent);
        if (i2 != -1 || intent == null) {
            return;
        }
        if (i == PICK_AUDIO) {
            if (intent.getClipData() != null) {
                for (int i3 = 0; i3 < intent.getClipData().getItemCount(); i3++) {
                    addTrack(intent.getClipData().getItemAt(i3).getUri(), intent.getFlags(), true);
                }
            } else if (intent.getData() != null) {
                addTrack(intent.getData(), intent.getFlags(), true);
            }
        } else if (i == PICK_AUDIO_FOLDER && intent.getData() != null) {
            importFolder(intent.getData(), intent.getFlags());
        } else {
            return;
        }
        TrackStore.sort(this.tracks);
        TrackStore.save(this, this.tracks);
        render();
    }

    private void importFolder(Uri treeUri, int flags) {
        if (treeUri == null || !"content".equalsIgnoreCase(treeUri.getScheme())) {
            return;
        }
        int takeFlags = flags & Intent.FLAG_GRANT_READ_URI_PERMISSION;
        try {
            getContentResolver().takePersistableUriPermission(treeUri, takeFlags);
        } catch (Exception ignored) {
        }
        int[] imported = new int[]{0};
        try {
            scanDocumentTree(treeUri, DocumentsContract.getTreeDocumentId(treeUri), imported);
        } catch (Exception ignored) {
        }
    }

    private void scanDocumentTree(Uri treeUri, String documentId, int[] imported) {
        if (imported[0] >= MAX_FOLDER_IMPORT) {
            return;
        }
        Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, documentId);
        Cursor cursor = null;
        try {
            cursor = getContentResolver().query(childrenUri, new String[]{
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_MIME_TYPE,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME
            }, null, null, null);
            while (cursor != null && cursor.moveToNext() && imported[0] < MAX_FOLDER_IMPORT) {
                String childId = cursor.getString(0);
                String mimeType = cursor.getString(1);
                String displayName = cursor.getString(2);
                Uri childUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, childId);
                if (DocumentsContract.Document.MIME_TYPE_DIR.equals(mimeType)) {
                    scanDocumentTree(treeUri, childId, imported);
                } else if (isAudioDocument(mimeType, displayName)) {
                    int before = this.tracks.size();
                    addTrack(childUri, 0, false);
                    if (this.tracks.size() > before) {
                        imported[0]++;
                    }
                }
            }
        } catch (Exception ignored) {
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private boolean isAudioDocument(String mimeType, String displayName) {
        if (mimeType != null && mimeType.toLowerCase(Locale.ROOT).startsWith("audio/")) {
            return true;
        }
        if (displayName == null) {
            return false;
        }
        String lower = displayName.toLowerCase(Locale.ROOT);
        return lower.endsWith(".mp3") || lower.endsWith(".m4a") || lower.endsWith(".aac") || lower.endsWith(".wav") || lower.endsWith(".ogg") || lower.endsWith(".flac");
    }

    private void addTrack(Uri uri) {
        addTrack(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION, true);
    }

    private void addTrack(Uri uri, int permissionFlags, boolean persistPermission) {
        if (!isSafeAudioUri(uri)) {
            Log.w(DEBUG_TAG, "add_track_rejected uri=" + uri + " reason=unsafe");
            return;
        }
        if (persistPermission) {
            int takeFlags = permissionFlags & Intent.FLAG_GRANT_READ_URI_PERMISSION;
            if (takeFlags == 0) {
                takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION;
            }
            try {
                getContentResolver().takePersistableUriPermission(uri, takeFlags);
            } catch (Exception e) {
                Log.w(DEBUG_TAG, "persist_permission_failed uri=" + uri + " error=" + e.getMessage());
            }
        }
        String string = uri.toString();
        Iterator<Track> it = this.tracks.iterator();
        while (it.hasNext()) {
            if (it.next().uri.equals(string)) {
                return;
            }
        }
        try {
            String mime = getContentResolver().getType(uri);
            String displayName = queryDisplayName(uri);
            long size = querySize(uri);
            boolean canOpen = TrackStore.canOpenForRead(this, uri);
            Log.i(DEBUG_TAG, "add_track_candidate uri=" + uri + " mime=" + mime + " displayName=" + displayName + " size=" + size + " canOpen=" + canOpen);
            if (!canOpen) {
                return;
            }
            Track track = TrackStore.fromUri(this, uri);
            if (track != null) {
                this.tracks.add(track);
                Log.i(DEBUG_TAG, "add_track_saved uri=" + uri + " title=" + track.title + " durationMs=" + track.durationMs);
            }
        } catch (Throwable th) {
            Log.e(DEBUG_TAG, "add_track_failed uri=" + uri + " error=" + th.getMessage(), th);
        }
    }

    private boolean isSafeAudioUri(Uri uri) {
        if (uri == null || uri.getScheme() == null || !"content".equalsIgnoreCase(uri.getScheme())) {
            return false;
        }
        try {
            String type = getContentResolver().getType(uri);
            String name = queryDisplayName(uri);
            boolean nameLooksAudio = false;
            if (name != null) {
                String lower = name.toLowerCase(Locale.ROOT);
                nameLooksAudio = lower.endsWith(".mp3") || lower.endsWith(".m4a") || lower.endsWith(".aac") || lower.endsWith(".wav") || lower.endsWith(".ogg") || lower.endsWith(".flac");
            }
            if (type != null && !type.toLowerCase(Locale.ROOT).startsWith("audio/") && !nameLooksAudio) {
                return false;
            }
            if (type == null && !nameLooksAudio) {
                return false;
            }
            long size = querySize(uri);
            return size <= 0 || size <= MAX_AUDIO_BYTES;
        } catch (Exception ignored) {
            return false;
        }
    }

    private String queryDisplayName(Uri uri) {
        Cursor cursor = null;
        try {
            cursor = getContentResolver().query(uri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getString(0);
            }
        } catch (Exception e) {
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return uri.getLastPathSegment();
    }

    private long querySize(Uri uri) {
        Cursor cursor = null;
        try {
            cursor = getContentResolver().query(uri, new String[]{OpenableColumns.SIZE}, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getLong(0);
            }
        } catch (Exception e) {
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return -1L;
    }

    LinearLayout row() {
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(0);
        linearLayout.setGravity(16);
        return linearLayout;
    }

    TextView text(String str, int i, boolean z) {
        TextView textView = new TextView(this);
        textView.setText(str);
        textView.setTextColor(this.fg);
        textView.setTextSize(i);
        textView.setGravity(16);
        textView.setTypeface(null, z ? 1 : 0);
        textView.setSingleLine(false);
        return textView;
    }

    void makeMarquee(TextView textView) {
        textView.setSingleLine(true);
        textView.setEllipsize(TextUtils.TruncateAt.MARQUEE);
        textView.setMarqueeRepeatLimit(-1);
        textView.setSelected(true);
        textView.setFocusable(true);
        textView.setFocusableInTouchMode(true);
    }

    Button button(String str) {
        Button button = new Button(this);
        button.setText(str);
        button.setTextColor(this.fg);
        button.setTextSize(14.0f);
        button.setAllCaps(false);
        button.setGravity(17);
        button.setIncludeFontPadding(true);
        button.setPadding(0, 0, 0, 0);
        button.setStateListAnimator(null);
        button.setElevation(0.0f);
        button.setTranslationZ(0.0f);
        button.setMinWidth(0);
        button.setMinHeight(0);
        button.setBackgroundColor(Color.TRANSPARENT);
        return button;
    }

    Button icon(String str) {
        Button button = button(str);
        button.setTextSize(24.0f);
        return button;
    }

    private Button shuffleButton() {
        Button buttonIcon = icon("⇄");
        buttonIcon.setTextSize(31.0f);
        buttonIcon.setTypeface(Typeface.DEFAULT_BOLD);
        buttonIcon.setPadding(0, 0, 0, 0);
        return buttonIcon;
    }

    private Button searchButton() {
        Button buttonIcon = icon("⌕");
        buttonIcon.setTextSize(31.0f);
        buttonIcon.setTypeface(Typeface.DEFAULT_BOLD);
        buttonIcon.setPadding(0, 0, 0, 0);
        return buttonIcon;
    }

    private void applyButtonColors(Button button, int i, int i2) {
        button.setTextColor(i2);
        GradientDrawable gradientDrawableRounded = rounded(i, false);
        gradientDrawableRounded.setStroke(1, i);
        button.setBackground(gradientDrawableRounded);
    }

    void applyPlainIconStyle(Button button) {
        applyPlainIconStyle(button, this.dark ? Color.rgb(230, 226, 236) : this.primaryText);
    }

    void applyPlainIconStyle(Button button, int color) {
        button.setTextColor(color);
        button.setBackgroundColor(Color.TRANSPARENT);
        button.setElevation(0.0f);
        button.setTranslationZ(0.0f);
    }

    private GradientDrawable createCardBackground() {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(this.card);
        drawable.setCornerRadius(dp(16));
        drawable.setStroke(dp(1), this.cardStroke);
        return drawable;
    }

    private GradientDrawable createPrimaryButtonBackground() {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(this.purple);
        drawable.setCornerRadius(dp(16));
        return drawable;
    }

    private GradientDrawable createSecondaryButtonBackground() {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(this.purpleSoft);
        drawable.setCornerRadius(dp(16));
        drawable.setStroke(dp(1), this.cardStroke);
        return drawable;
    }

    private GradientDrawable createIconButtonBackground() {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(this.dark ? this.card : Color.WHITE);
        drawable.setCornerRadius(dp(999));
        drawable.setStroke(dp(1), this.cardStroke);
        return drawable;
    }

    void applyCardStyle(View view) {
        view.setBackground(createCardBackground());
        view.setElevation(dp(1));
    }

    void applyPrimaryButtonStyle(Button button) {
        button.setTextColor(Color.WHITE);
        button.setBackground(createPrimaryButtonBackground());
    }

    private void applySecondaryButtonStyle(Button button) {
        button.setTextColor(this.primaryText);
        button.setBackground(createCardBackground());
    }

    void applySeekBarColors(SeekBar seekBar) {
        if (Build.VERSION.SDK_INT >= 21) {
            seekBar.setProgressTintList(ColorStateList.valueOf(this.purple));
            seekBar.setThumbTintList(ColorStateList.valueOf(this.yellow));
            seekBar.setProgressBackgroundTintList(ColorStateList.valueOf(this.purpleSoft));
        }
    }

    private TriangleDecorView createTriangleArtwork(int mode) {
        TriangleDecorView view = new TriangleDecorView(this);
        view.setMode(mode);
        view.setColors(this.purple, this.yellow);
        view.setDecorAlpha(this.dark ? 0.78f : 0.9f);
        view.setStrokeWidth(dp(2));
        return view;
    }

    LinearLayout.LayoutParams square(int i) {
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(dp(i), dp(i));
        layoutParams.setMargins(dp(4), dp(4), dp(4), dp(4));
        return layoutParams;
    }

    private View framed(View view) {
        return spaced(view);
    }

    View spaced(View view) {
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-1, -2);
        layoutParams.setMargins(0, dp(5), 0, dp(5));
        view.setLayoutParams(layoutParams);
        return view;
    }

    private GradientDrawable rounded(int i, boolean z) {
        GradientDrawable gradientDrawable = new GradientDrawable();
        gradientDrawable.setColor(i);
        gradientDrawable.setCornerRadius(dp(z ? 16 : 14));
        if (z) {
            i = this.cardStroke;
        }
        gradientDrawable.setStroke(z ? 1 : 0, i);
        return gradientDrawable;
    }

    void setSurface(View view, int i, boolean z) {
        view.setBackground(rounded(i, z));
    }

    View lineView() {
        View view = new View(this);
        view.setBackgroundColor(this.line);
        return view;
    }

    ImageView coverView() {
        ImageView imageView = new ImageView(this);
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        GradientDrawable gradientDrawable = new GradientDrawable();
        gradientDrawable.setColor(0);
        gradientDrawable.setCornerRadius(0.0f);
        imageView.setBackground(gradientDrawable);
        imageView.setClipToOutline(false);
        return imageView;
    }

    private class ColorWheelView extends View {
        private final Paint paint = new Paint(1);
        private final float[] hsv = new float[]{0.0f, 0.0f, 1.0f};
        private final ColorPickDone done;
        private Bitmap wheelBitmap;
        private int wheelSize;
        private int wheelRadius;
        private int wheelCenterX;
        private int wheelCenterY;
        private int brightnessTop;
        private int brightnessHeight;

        ColorWheelView(int initialColor, ColorPickDone colorPickDone) {
            super(MainActivityCore.this);
            this.done = colorPickDone;
            Color.colorToHSV(initialColor, this.hsv);
            this.hsv[2] = Math.max(0.02f, this.hsv[2]);
        }

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            this.brightnessHeight = dp(28);
            this.wheelSize = Math.max(1, Math.min(w, h - dp(52)));
            this.wheelRadius = Math.max(1, this.wheelSize / 2);
            this.wheelCenterX = w / 2;
            this.wheelCenterY = this.wheelRadius;
            this.brightnessTop = this.wheelSize + dp(18);
            this.wheelBitmap = buildWheelBitmap(this.wheelSize);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            if (this.wheelBitmap == null) {
                return;
            }
            int left = this.wheelCenterX - this.wheelRadius;
            canvas.drawBitmap(this.wheelBitmap, left, 0, this.paint);
            this.paint.setStyle(Paint.Style.STROKE);
            this.paint.setStrokeWidth(dp(2));
            this.paint.setColor(mixColor(fg, bg, 0.65f));
            canvas.drawCircle(this.wheelCenterX, this.wheelCenterY, this.wheelRadius - dp(1), this.paint);
            float angle = (float) Math.toRadians(this.hsv[0]);
            float selectorRadius = this.hsv[1] * (float) this.wheelRadius;
            float selectorX = this.wheelCenterX + ((float) Math.cos(angle) * selectorRadius);
            float selectorY = this.wheelCenterY + ((float) Math.sin(angle) * selectorRadius);
            this.paint.setStyle(Paint.Style.STROKE);
            this.paint.setStrokeWidth(dp(3));
            this.paint.setColor(readableOn(Color.HSVToColor(this.hsv)));
            canvas.drawCircle(selectorX, selectorY, dp(9), this.paint);
            drawBrightness(canvas);
        }

        private Bitmap buildWheelBitmap(int size) {
            Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
            int radius = Math.max(1, size / 2);
            float[] pixelHsv = new float[]{0.0f, 0.0f, 1.0f};
            for (int y = 0; y < size; y++) {
                for (int x = 0; x < size; x++) {
                    float dx = x - radius;
                    float dy = y - radius;
                    float distance = (float) Math.sqrt((dx * dx) + (dy * dy));
                    if (distance > radius) {
                        bitmap.setPixel(x, y, Color.TRANSPARENT);
                    } else {
                        float hue = (float) Math.toDegrees(Math.atan2(dy, dx));
                        if (hue < 0.0f) {
                            hue += 360.0f;
                        }
                        pixelHsv[0] = hue;
                        pixelHsv[1] = Math.min(1.0f, distance / (float) radius);
                        pixelHsv[2] = 1.0f;
                        bitmap.setPixel(x, y, Color.HSVToColor(pixelHsv));
                    }
                }
            }
            return bitmap;
        }

        private void drawBrightness(Canvas canvas) {
            int left = dp(2);
            int right = getWidth() - dp(2);
            for (int x = left; x <= right; x++) {
                float value = (float) (x - left) / (float) Math.max(1, right - left);
                float[] barHsv = new float[]{this.hsv[0], this.hsv[1], value};
                this.paint.setStyle(Paint.Style.STROKE);
                this.paint.setStrokeWidth(1.0f);
                this.paint.setColor(Color.HSVToColor(barHsv));
                canvas.drawLine(x, this.brightnessTop, x, this.brightnessTop + this.brightnessHeight, this.paint);
            }
            this.paint.setStyle(Paint.Style.STROKE);
            this.paint.setStrokeWidth(dp(2));
            this.paint.setColor(mixColor(fg, bg, 0.65f));
            canvas.drawRect(left, this.brightnessTop, right, this.brightnessTop + this.brightnessHeight, this.paint);
            float knobX = left + (this.hsv[2] * (right - left));
            this.paint.setStrokeWidth(dp(3));
            this.paint.setColor(readableOn(Color.HSVToColor(this.hsv)));
            canvas.drawCircle(knobX, this.brightnessTop + (this.brightnessHeight / 2.0f), dp(8), this.paint);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (event.getActionMasked() != MotionEvent.ACTION_DOWN && event.getActionMasked() != MotionEvent.ACTION_MOVE) {
                return true;
            }
            if (event.getY() >= this.brightnessTop - dp(8)) {
                int left = dp(2);
                int right = getWidth() - dp(2);
                this.hsv[2] = Math.max(0.0f, Math.min(1.0f, (event.getX() - left) / (float) Math.max(1, right - left)));
            } else {
                float dx = event.getX() - this.wheelCenterX;
                float dy = event.getY() - this.wheelCenterY;
                float distance = (float) Math.sqrt((dx * dx) + (dy * dy));
                if (distance <= this.wheelRadius) {
                    float hue = (float) Math.toDegrees(Math.atan2(dy, dx));
                    if (hue < 0.0f) {
                        hue += 360.0f;
                    }
                    this.hsv[0] = hue;
                    this.hsv[1] = Math.min(1.0f, distance / (float) this.wheelRadius);
                }
            }
            this.done.picked(Color.HSVToColor(this.hsv));
            invalidate();
            return true;
        }
    }

    FrameLayout shade() {
        FrameLayout frameLayout = new FrameLayout(this);
        int i = this.dark ? 0 : 255;
        frameLayout.setBackgroundColor(Color.argb(190, i, i, i));
        frameLayout.setOnClickListener(new UiAction87());
        return frameLayout;
    }

    class UiAction87 implements View.OnClickListener {
        UiAction87() {
        }

        @Override
        public void onClick(View view) {
            MainActivityCore.accessOverlayHost(MainActivityCore.this).removeView(view);
            MainActivityCore.callUpdateMini(MainActivityCore.this);
        }
    }

    LinearLayout panelCard() {
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(1);
        linearLayout.setPadding(dp(12), dp(12), dp(12), dp(12));
        applyCardStyle(linearLayout);
        linearLayout.setOnClickListener(new UiAction88());
        return linearLayout;
    }

    class UiAction88 implements View.OnClickListener {
        UiAction88() {
        }

        @Override
        public void onClick(View view) {
        }
    }

    void addMiniSpacerIfNeeded() {
        if (this.currentIndex < 0 || this.currentIndex >= this.tracks.size() || this.overlayHost.getChildCount() > 0) {
            return;
        }
        View view = new View(this);
        view.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(88)));
        this.list.addView(view);
    }

    void openSongDiagnostics() {
        int available = 0;
        int unavailable = 0;
        int withDuration = 0;
        int withoutDuration = 0;
        StringBuilder broken = new StringBuilder();
        for (Track track : this.tracks) {
            boolean canOpen = TrackStore.canOpenForRead(this, track.asUri());
            if (canOpen) {
                available++;
            } else {
                unavailable++;
                if (broken.length() < 500) {
                    broken.append("\n- ").append(track.title);
                }
            }
            if (track.durationMs > 0) {
                withDuration++;
            } else {
                withoutDuration++;
            }
        }
        String message = tr("Available: ", "Доступно: ") + available
                + "\n" + tr("Unavailable: ", "Недоступно: ") + unavailable
                + "\n" + tr("With duration: ", "С длительностью: ") + withDuration
                + "\n" + tr("Without duration: ", "Без длительности: ") + withoutDuration
                + (broken.length() > 0 ? "\n" + tr("Problem tracks:", "Проблемные треки:") + broken : "");
        Log.i(DEBUG_TAG, "song_diagnostics available=" + available + " unavailable=" + unavailable + " withDuration=" + withDuration + " withoutDuration=" + withoutDuration);
        showConfirmPanel(tr("Song check", "Проверка песен"), message, new Runnable() {
            @Override
            public void run() {
            }
        });
    }

    private void showConfirmPanel(String str, String str2, Runnable runnable) {
        FrameLayout frameLayoutShade = shade();
        LinearLayout linearLayoutPanelCard = panelCard();
        linearLayoutPanelCard.setPadding(dp(16), dp(16), dp(16), dp(16));
        linearLayoutPanelCard.addView(text(str, 22, true), new LinearLayout.LayoutParams(-1, dp(46)));
        TextView textViewText = text(str2, 16, false);
        textViewText.setTextColor(this.muted);
        textViewText.setPadding(0, dp(4), 0, dp(14));
        linearLayoutPanelCard.addView(textViewText, new LinearLayout.LayoutParams(-1, -2));
        LinearLayout linearLayoutRow = row();
        Button button = button(tr3("No", "Нет", "×"));
        button.setOnClickListener(new UiAction89(this, frameLayoutShade));
        linearLayoutRow.addView(button, new LinearLayout.LayoutParams(0, dp(54), 1.0f));
        Button button2 = button(tr3("Yes", "Да", "✓"));
        applyButtonColors(button2, this.fg, this.bg);
        button2.setOnClickListener(new UiAction90(this, frameLayoutShade, runnable));
        linearLayoutRow.addView(button2, new LinearLayout.LayoutParams(0, dp(54), 1.0f));
        linearLayoutPanelCard.addView(linearLayoutRow);
        frameLayoutShade.addView(linearLayoutPanelCard, centerParams(dp(330), -2));
        this.overlayHost.addView(frameLayoutShade);
        updateMini();
    }

    class UiAction89 implements View.OnClickListener {
        final MainActivityCore this$0;
        final FrameLayout val$shade;

        UiAction89(MainActivityCore mainActivity, FrameLayout frameLayout) {
            this.val$shade = frameLayout;
            this.this$0 = mainActivity;
        }

        @Override
        public void onClick(View view) {
            MainActivityCore.accessOverlayHost(this.this$0).removeView(this.val$shade);
            MainActivityCore.callUpdateMini(this.this$0);
        }
    }

    class UiAction90 implements View.OnClickListener {
        final MainActivityCore this$0;
        final FrameLayout val$shade;
        final Runnable val$yesAction;

        UiAction90(MainActivityCore mainActivity, FrameLayout frameLayout, Runnable runnable) {
            this.val$shade = frameLayout;
            this.val$yesAction = runnable;
            this.this$0 = mainActivity;
        }

        @Override
        public void onClick(View view) {
            MainActivityCore.accessOverlayHost(this.this$0).removeView(this.val$shade);
            this.val$yesAction.run();
            MainActivityCore.callUpdateMini(this.this$0);
        }
    }

    FrameLayout.LayoutParams centerParams(int i, int i2) {
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(i, i2, 17);
        layoutParams.setMargins(dp(14), dp(14), dp(14), dp(14));
        return layoutParams;
    }

    FrameLayout.LayoutParams bottomParams() {
        return new FrameLayout.LayoutParams(-1, (int) (getResources().getDisplayMetrics().heightPixels * 0.78f), 80);
    }

    int dp(int i) {
        return Math.round(i * getResources().getDisplayMetrics().density);
    }

    static class Playlist {
        String name;
        final ArrayList<String> uris = new ArrayList<>();

        Playlist(String str) {
            this.name = str;
        }
    }
}
