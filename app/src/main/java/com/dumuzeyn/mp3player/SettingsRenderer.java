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
        addButton(host.tr("Theme: ", "Тема: ") + host.themeName(), view -> host.openThemeDialog());
        addButton(host.tr(host.animations ? "Turn animations off" : "Turn animations on",
                host.animations ? "Отключить анимации" : "Включить анимации"), view -> toggleAnimations());
        addButton(host.tr("Language: ", "Язык: ") + host.languageName(),
                view -> host.settingsController.openLanguageDialog());
        addButton(host.tr("Mini-player memory: ", "Память мини-плеера: ")
                        + host.settingsController.resumeWindowText(),
                view -> host.settingsController.openResumeWindowDialog());
        addButton(host.tr("Check songs", "Проверить песни"), view -> host.openSongDiagnostics());
        addButton(host.tr("Delete all songs from app", "Удалить все песни из приложения"),
                view -> host.settingsController.confirmDeleteAllSongs());
        addButton(host.tr("Delete all playlists", "Удалить все плейлисты"),
                view -> host.settingsController.confirmDeleteAllPlaylists());
        addButton(host.tr("GitHub project", "GitHub проект"), view -> host.settingsController.openGithub());
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

    private void addButton(String label, View.OnClickListener listener) {
        Button button = host.button(label);
        button.setTextSize(17.0f);
        button.setGravity(8388627);
        button.setPadding(host.dp(18), 0, host.dp(12), 0);
        host.applySecondaryButtonStyle(button);
        String lower = label.toLowerCase(Locale.ROOT);
        if (lower.contains("delete") || lower.contains("удал")) {
            button.setTextColor(Color.rgb(190, 45, 45));
        }
        button.setOnClickListener(listener);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, host.dp(58));
        params.setMargins(0, host.dp(6), 0, host.dp(6));
        host.list.addView(button, params);
    }
}
