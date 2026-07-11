package com.dumuzeyn.mp3player;

import android.view.View;

final class SettingsRenderer {
    private final MainActivityCore host;

    SettingsRenderer(MainActivityCore host) {
        this.host = host;
    }

    void render() {
        host.renderSettingsInternal();
    }

    void renderSettingsState() {
        host.addSettingsButton(host.tr("Theme: ", "Тема: ") + host.themeName(), new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                host.openThemeDialog();
            }
        });
        host.addSettingsButton(host.tr3(host.animations ? "Turn animations off" : "Turn animations on", host.animations ? "Отключить анимации" : "Включить анимации", host.animations ? "◌" : "◍"), new View.OnClickListener() {
            @Override
            public void onClick(View view) {
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
        });
        host.addSettingsButton(host.tr3("Language: ", "Язык: ", "◐ ") + host.languageName(), new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                host.openLanguageDialog();
            }
        });
        host.addSettingsButton(host.tr3("Mini-player memory: ", "Память мини-плеера: ", "▣ ") + host.resumeWindowText(), new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                host.openResumeWindowDialog();
            }
        });
        host.addSettingsButton(host.tr3("Check songs", "Проверить песни", "✓ ♪"), new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                host.openSongDiagnostics();
            }
        });
        host.addSettingsButton(host.tr3("Delete all songs from app", "Удалить все песни из приложения", "⌫ ♪"), new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                host.confirmDeleteAllSongs();
            }
        });
        host.addSettingsButton(host.tr3("Delete all playlists", "Удалить все плейлисты", "⌫ ▤"), new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                host.confirmDeleteAllPlaylists();
            }
        });
        host.addSettingsButton(host.tr3("GitHub project", "GitHub проект", "⌘"), new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                host.openGithub();
            }
        });
    }
}
