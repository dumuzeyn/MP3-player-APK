package com.dumuzeyn.mp3player;

import android.graphics.Color;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.Locale;

final class SettingsRenderer {
    private static final String PREFS = "mp3_player_ui";
    private static final String ADVANCED = "advancedSettingsVisible";
    private final MainActivityCore host;

    SettingsRenderer(MainActivityCore host) {
        this.host = host;
    }

    void render() {
        section(host.tr("General", "Основные"));
        addButton(host.tr("Language: ", "Язык: ") + host.languageName(),
                view -> host.settingsController.openLanguageDialog());
        addButton(host.tr("Mini-player memory: ", "Память мини-плеера: ")
                        + host.settingsController.resumeWindowText(),
                view -> host.settingsController.openResumeWindowDialog());
        reset(SettingsSectionResetter.Section.GENERAL);

        section(host.tr("Playback", "Воспроизведение"));
        addButton(host.uninterruptedPlaybackController.settingLabel(),
                view -> host.uninterruptedPlaybackController.toggle());
        addButton(host.backgroundPlaybackSettingsController.settingLabel(),
                view -> host.backgroundPlaybackSettingsController.openDialog());
        reset(SettingsSectionResetter.Section.PLAYBACK);

        section(host.tr("Sound", "Звук"));
        addButton(host.tr("Equalizer", "Эквалайзер"),
                view -> host.equalizerController.openDialog());
        addButton(host.stableVolumeController.settingLabel(),
                view -> host.stableVolumeController.toggle());
        reset(SettingsSectionResetter.Section.SOUND);

        section(host.tr("Appearance", "Внешний вид"));
        subsection(host.tr("Ready themes and accent colors", "Готовые темы и акцентные цвета"));
        addButton(host.tr("Theme: ", "Тема: ") + host.themeName(),
                view -> host.openThemeDialog());
        subsection(host.tr("Text, outline, and background", "Текст, контур и фон"));
        addButton(host.tr("Background", "Фон"),
                view -> host.backgroundSettingsController.openDialog());
        addButton(host.tr("Export theme", "Экспорт темы"),
                view -> host.settingsController.exportTheme());
        addButton(host.tr("Import theme", "Импорт темы"),
                view -> host.settingsController.importTheme());
        subsection(host.tr("Cards and artwork", "Карточки и обложки"));
        addButton(host.cardTransparencyController.settingLabel(),
                view -> host.cardTransparencyController.openDialog());
        addButton(host.tr("Cover style: ", "Стиль обложек: ")
                        + host.tr(host.circularCovers ? "spinning circles" : "rounded squares",
                        host.circularCovers ? "вращающиеся круги" : "скруглённые квадраты"),
                view -> toggleCoverStyle());
        reset(SettingsSectionResetter.Section.APPEARANCE);

        section(host.tr("Full player", "Большой плеер"));
        addButton(host.coverRotationSettingsController.settingLabel(),
                view -> host.coverRotationSettingsController.openDialog());
        reset(SettingsSectionResetter.Section.FULL_PLAYER);

        section(host.tr("Animations", "Анимации"));
        addButton(host.tr("Animations: ", "Анимации: ")
                        + host.tr(host.animations ? "on" : "off",
                        host.animations ? "вкл" : "выкл"), view -> toggleAnimations());
        addButton(host.tr("Particles: ", "Частицы: ")
                        + host.tr(host.particlesEnabled ? "on" : "off",
                        host.particlesEnabled ? "вкл" : "выкл"), view -> toggleParticles());
        addButton(host.tr("Particle settings", "Настройка частиц"),
                view -> host.particleSettingsController.openDialog());
        addButton(host.playlistTickerSettingsController.settingLabel(),
                view -> host.playlistTickerSettingsController.openDialog());
        reset(SettingsSectionResetter.Section.ANIMATIONS);

        section(host.tr("Library", "Библиотека"));
        addButton(host.tr("Check songs", "Проверить песни"),
                view -> host.openSongDiagnostics());
        addButton(host.tr("Rescan music folders", "Повторно сканировать папки"),
                view -> host.rescanMusicFolders());
        addButton(host.tr("Export playlists and settings", "Экспорт плейлистов и настроек"),
                view -> host.settingsController.exportLibraryBackup());
        addButton(host.tr("Import playlists and settings", "Импорт плейлистов и настроек"),
                view -> host.settingsController.importLibraryBackup());

        boolean advanced = host.getSharedPreferences(PREFS, 0).getBoolean(ADVANCED, false);
        addPrimaryButton(host.tr(advanced ? "Hide advanced settings" : "Advanced settings",
                        advanced ? "Скрыть расширенные настройки" : "Расширенные настройки"),
                view -> {
                    host.getSharedPreferences(PREFS, 0).edit().putBoolean(ADVANCED,
                            !advanced).apply();
                    host.render();
                });
        if (advanced) {
            renderAdvanced();
        }

        section(host.tr("About", "О приложении"));
        addButton(host.tr("GitHub project", "Проект на GitHub"),
                view -> host.settingsController.openGithub());
        addButton(host.tr("Support the author", "Поддержка автора"),
                view -> host.settingsController.openAuthorSupport());
    }

    private void renderAdvanced() {
        section(host.tr("Advanced library", "Расширенная библиотека"));
        addButton(host.tr("Remove unavailable songs", "Удалить недоступные песни"),
                view -> host.settingsController.confirmRemoveUnavailableSongs());
        addButton(host.tr("Delete all songs from app", "Удалить все песни из приложения"),
                view -> host.settingsController.confirmDeleteAllSongs());
        addButton(host.tr("Delete all playlists", "Удалить все плейлисты"),
                view -> host.settingsController.confirmDeleteAllPlaylists());

        section(host.tr("Diagnostics", "Диагностика"));
        addButton(host.tr("Crash reports: ", "Отчёты о сбоях: ")
                        + CrashReportStore.count(host),
                view -> host.settingsController.openCrashReports());
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

    private void section(String label) {
        TextView title = host.text(label, 20, true);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, host.dp(42));
        params.setMargins(host.dp(4), host.dp(12), host.dp(4), 0);
        host.list.addView(title, params);
    }

    private void subsection(String label) {
        TextView title = host.text(label, 14, true);
        title.setTextColor(host.secondaryText);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, host.dp(32));
        params.setMargins(host.dp(10), host.dp(4), host.dp(10), 0);
        host.list.addView(title, params);
    }

    private void reset(SettingsSectionResetter.Section section) {
        addCompactButton(host.tr("Reset section", "Сбросить раздел"),
                view -> SettingsSectionResetter.reset(host, section));
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
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, host.dp(54));
        params.setMargins(0, host.dp(2), 0, host.dp(2));
        host.list.addView(button, params);
    }

    private void addCompactButton(String label, View.OnClickListener listener) {
        Button button = host.button(label);
        button.setTextSize(14.0f);
        host.applySecondaryButtonStyle(button, host.settingsCardOpacity);
        button.setOnClickListener(listener);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, host.dp(40));
        params.setMargins(host.dp(18), 0, host.dp(18), host.dp(4));
        host.list.addView(button, params);
    }

    private void addPrimaryButton(String label, View.OnClickListener listener) {
        Button button = host.button(label);
        host.applyPrimaryButtonStyle(button);
        button.setOnClickListener(listener);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, host.dp(50));
        params.setMargins(0, host.dp(12), 0, host.dp(4));
        host.list.addView(button, params);
    }
}
