package com.dumuzeyn.mp3player;

import android.graphics.Color;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import java.util.Locale;

final class SettingsRenderer {
    private final MainActivityCore host;

    SettingsRenderer(MainActivityCore host) {
        this.host = host;
    }

    void render() {
        addButton(host.tr("Language: ", "Язык: ") + host.languageName(),
                view -> host.settingsController.openLanguageDialog());
        addButton(host.tr("Theme: ", "Тема: ") + host.themeName(), view -> host.openThemeDialog());
        addButton(host.tr("Background", "Фон"),
                view -> host.backgroundSettingsController.openDialog());
        addButton(host.tr("Cover style: ", "Стиль обложек: ")
                        + host.tr(host.circularCovers ? "spinning circles" : "rounded squares",
                        host.circularCovers ? "вращающиеся круги" : "скруглённые квадраты"),
                view -> toggleCoverStyle());
        addButton(host.coverRotationSettingsController.settingLabel(),
                view -> host.coverRotationSettingsController.openDialog());
        addButton(host.tr("Animations: ", "Анимации: ")
                        + host.tr(host.animations ? "on" : "off", host.animations ? "вкл" : "выкл"),
                view -> toggleAnimations());
        addButton(host.tr("Particles: ", "Частицы: ")
                        + host.tr(host.particlesEnabled ? "on" : "off",
                        host.particlesEnabled ? "вкл" : "выкл"),
                view -> toggleParticles());
        addButton(host.tr("Particle settings", "Настройка частиц"),
                view -> host.particleSettingsController.openDialog());
        addButton(host.cardTransparencyController.settingLabel(),
                view -> host.cardTransparencyController.openDialog());
        addButton(host.playlistTickerSettingsController.settingLabel(),
                view -> host.playlistTickerSettingsController.openDialog());
        addButton(host.uninterruptedPlaybackController.settingLabel(),
                view -> host.uninterruptedPlaybackController.toggle());
        addButton(host.stableVolumeController.settingLabel(),
                view -> host.stableVolumeController.toggle());
        addButton(host.backgroundPlaybackSettingsController.settingLabel(),
                view -> host.backgroundPlaybackSettingsController.openDialog());
        addButton(host.tr("Mini-player memory: ", "Память мини-плеера: ")
                        + host.settingsController.resumeWindowText(),
                view -> host.settingsController.openResumeWindowDialog());
        addButton(host.tr("Check songs", "Проверить песни"), view -> host.openSongDiagnostics());
        addButton(host.tr("Crash reports: ", "Отчёты о сбоях: ") + CrashReportStore.count(host),
                view -> host.settingsController.openCrashReports());
        addButton(host.tr("Delete all songs from app", "Удалить все песни из приложения"),
                view -> host.settingsController.confirmDeleteAllSongs());
        addButton(host.tr("Delete all playlists", "Удалить все плейлисты"),
                view -> host.settingsController.confirmDeleteAllPlaylists());
        addButton(host.tr("GitHub project", "GitHub проект"), view -> host.settingsController.openGithub());
        addButton(host.tr("Support the author", "Поддержка автора"),
                view -> host.settingsController.openAuthorSupport());
    }

    private void toggleAnimations() {
        host.animations = !host.animations;
        host.tabAnimating = false;
        if (!host.animations && host.list != null) {
            host.list.animate().cancel();
            host.list.setTranslationX(0.0f);
            host.list.setAlpha(1.0f);
        }
        host.saveState();
        host.render();
    }

    private void toggleParticles() {
        host.particlesEnabled = !host.particlesEnabled;
        host.saveState();
        host.refreshParticleSettings();
        host.render();
    }

    private void toggleCoverStyle() {
        host.circularCovers = !host.circularCovers;
        host.saveState();
        host.refreshPlaybackAppearance();
        host.rebuildUi();
    }

    private void addButton(String label, View.OnClickListener listener) {
        Button button = host.button(label);
        button.setTextSize(17.0f);
        button.setGravity(8388627);
        button.setPadding(host.dp(18), 0, host.dp(12), 0);
        host.applySecondaryButtonStyle(button, host.settingsCardOpacity);
        String lower = label.toLowerCase(Locale.ROOT);
        if (lower.contains("delete") || lower.contains("удал")) {
            button.setTextColor(Color.rgb(190, 45, 45));
        }
        button.setOnClickListener(listener);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, host.dp(56));
        params.setMargins(0, host.dp(2), 0, host.dp(2));
        host.list.addView(button, params);
    }
}
