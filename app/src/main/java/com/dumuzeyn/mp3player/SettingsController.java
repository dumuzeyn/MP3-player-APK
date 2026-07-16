package com.dumuzeyn.mp3player;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

final class SettingsController {
    private final MainActivityCore host;

    SettingsController(MainActivityCore host) {
        this.host = host;
    }

    String resumeWindowText() {
        if (host.resumeWindowMinutes <= 0) {
            return host.tr("off", "выкл");
        }
        if (host.resumeWindowMinutes % 60 == 0) {
            int hours = host.resumeWindowMinutes / 60;
            return hours + " " + host.tr(hours == 1 ? "hour" : "hours", "ч");
        }
        return host.resumeWindowMinutes + " " + host.tr("min", "мин");
    }

    void openLanguageDialog() {
        final FrameLayout shade = host.shade();
        LinearLayout panel = host.panelCard();
        panel.setPadding(host.dp(16), host.dp(16), host.dp(16), host.dp(16));
        panel.addView(host.text(host.tr("Language", "Язык"), 22, true),
                new LinearLayout.LayoutParams(-1, host.dp(50)));
        addChoice(panel, "English", "en".equals(host.language), () -> applyLanguage("en", shade));
        addChoice(panel, "Русский", "ru".equals(host.language), () -> applyLanguage("ru", shade));
        shade.addView(panel, host.centerParams(host.dp(330), -2));
        host.overlayHost.addView(shade);
        host.updateMini();
    }

    void openResumeWindowDialog() {
        final FrameLayout shade = host.shade();
        LinearLayout panel = host.panelCard();
        panel.setPadding(host.dp(16), host.dp(16), host.dp(16), host.dp(16));
        panel.addView(host.text(host.tr("Mini-player memory", "Память мини-плеера"), 22, true),
                new LinearLayout.LayoutParams(-1, host.dp(50)));
        int[] values = {30, 60, 120, 240, 480, 0};
        for (final int value : values) {
            String label = value == 0
                    ? host.tr("Off", "Отключено")
                    : value % 60 == 0
                            ? (value / 60) + " " + host.tr(value == 60 ? "hour" : "hours", "ч")
                            : value + " " + host.tr("minutes", "мин");
            addChoice(panel, label, host.resumeWindowMinutes == value, () -> {
                host.resumeWindowMinutes = value;
                host.saveState();
                host.overlayHost.removeView(shade);
                host.render();
            });
        }
        shade.addView(panel, host.centerParams(host.dp(330), -2));
        host.overlayHost.addView(shade);
        host.updateMini();
    }

    void openGithub() {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://github.com/dumuzeyn/MP3-player-APK"));
            intent.addCategory(Intent.CATEGORY_BROWSABLE);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            host.startActivity(intent);
        } catch (RuntimeException error) {
            host.showConfirmPanel(
                    host.tr("GitHub is unavailable", "GitHub недоступен"),
                    host.tr("No application can open the project link.",
                            "Нет приложения, которое может открыть ссылку проекта."),
                    () -> {
                    });
        }
    }

    void openCrashReports() {
        int count = CrashReportStore.count(host);
        if (count == 0) {
            host.showConfirmPanel(
                    host.tr("Crash reports", "Отчёты о сбоях"),
                    host.tr("No local crash reports have been recorded.",
                            "Локальных отчётов о сбоях нет."),
                    () -> {
                    });
            return;
        }
        String latest = CrashReportStore.latestSummary(host);
        String message = host.tr("Saved reports: ", "Сохранено отчётов: ") + count
                + "\n" + host.tr("Latest: ", "Последний: ") + latest
                + "\n\n" + host.tr(
                        "Reports stay only on this device and do not contain music URIs. Clear them?",
                        "Отчёты хранятся только на устройстве и не содержат URI музыки. Очистить их?");
        host.showConfirmPanel(
                host.tr("Crash reports", "Отчёты о сбоях"),
                message,
                () -> {
                    CrashReportStore.clear(host);
                    host.render();
                });
    }

    void confirmDeleteAllSongs() {
        host.showConfirmPanel(
                host.tr("Delete all songs?", "Удалить все песни?"),
                host.tr("Songs will disappear only from this app. Files on the phone will stay untouched.",
                        "Песни исчезнут только из приложения. Файлы на телефоне останутся."),
                () -> {
                    host.stopPlaybackAndClearQueue();
                    host.tracks.clear();
                    host.favorites.clear();
                    for (Playlist playlist : host.playlists) {
                        playlist.uris.clear();
                    }
                    TrackStore.save(host, host.tracks);
                    host.saveState();
                    host.render();
                });
    }

    void confirmDeleteAllPlaylists() {
        host.showConfirmPanel(
                host.tr("Delete all playlists?", "Удалить все плейлисты?"),
                host.tr("Songs will stay in the app.", "Песни останутся в приложении."),
                () -> {
                    host.playlists.clear();
                    host.saveState();
                    host.render();
                });
    }

    private void applyLanguage(String language, FrameLayout shade) {
        host.language = language;
        host.saveState();
        host.overlayHost.removeView(shade);
        host.rebuildUi();
    }

    private void addChoice(LinearLayout parent, String label, boolean selected, Runnable action) {
        Button button = host.button(label);
        button.setTextSize(17.0f);
        button.setGravity(8388627);
        button.setPadding(host.dp(18), 0, host.dp(12), 0);
        if (selected) {
            host.applyPrimaryButtonStyle(button);
        } else {
            host.applySecondaryButtonStyle(button);
        }
        button.setOnClickListener(view -> action.run());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, host.dp(54));
        params.setMargins(0, host.dp(5), 0, host.dp(5));
        parent.addView(button, params);
    }
}
