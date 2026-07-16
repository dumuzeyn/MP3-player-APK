package com.dumuzeyn.mp3player;

import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;

/** Applies the requested 2.4.3 settings reset once without touching the SQLite library. */
final class SettingsDefaults {
    private static final String MIGRATION_PREFS = "voltune_migrations";
    private static final String RESET_2_4_3 = "settings_reset_2_4_3";

    private SettingsDefaults() {
    }

    static void resetForVersion243(Context context) {
        SharedPreferences migrations = context.getSharedPreferences(
                MIGRATION_PREFS, Context.MODE_PRIVATE);
        if (migrations.getBoolean(RESET_2_4_3, false)) {
            return;
        }

        // Move legacy favorites and playlists to SQLite before clearing old UI preferences.
        LibraryDatabase.migrateLegacyIfNeeded(context);
        clear(context, "mp3_player_ui");
        clear(context, UninterruptedPlaybackController.PREFS);
        clear(context, EqualizerController.PREFS);
        clear(context, PlaybackStateRepository.PREFS);
        clear(context, "player_sleep_timer");
        resetLauncherAlias(context);
        migrations.edit().putBoolean(RESET_2_4_3, true).commit();
    }

    private static void clear(Context context, String name) {
        context.getSharedPreferences(name, Context.MODE_PRIVATE).edit().clear().commit();
    }

    private static void resetLauncherAlias(Context context) {
        PackageManager manager = context.getPackageManager();
        ComponentName light = LauncherComponents.forTheme(context, false);
        ComponentName dark = LauncherComponents.forTheme(context, true);
        try {
            manager.setComponentEnabledSetting(light,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP);
            manager.setComponentEnabledSetting(dark,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP);
        } catch (RuntimeException ignored) {
            // Some launchers postpone alias updates until the activity is no longer visible.
        }
    }
}
