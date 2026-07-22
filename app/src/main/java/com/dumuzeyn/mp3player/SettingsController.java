package com.dumuzeyn.mp3player;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.app.Activity;
import android.widget.Toast;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

final class SettingsController {
    private static final int EXPORT_BACKUP = 5201;
    private static final int IMPORT_BACKUP = 5202;
    private static final String SUPPORT_URL = "https://pay.cloudtips.ru/p/54e5a4f9";
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
        addDoneButton(panel, shade);
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
                openResumeWindowDialog();
            });
        }
        addDoneButton(panel, shade);
        shade.addView(panel, host.centerParams(host.dp(330), -2));
        host.overlayHost.addView(shade);
        host.updateMini();
    }

    void openGithub() {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://github.com/dumuzeyn/MP3-Player-Voltune"));
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

    void openAuthorSupport() {
        host.showActionPanel(
                host.tr("Support the author", "Поддержка автора"),
                host.tr(
                        "You will be taken to an external CloudTips page.\n\n"
                                + "Support is voluntary and non-refundable. It does not unlock "
                                + "additional features, a subscription, or other benefits. "
                                + "All application features remain free.",
                        "Вы перейдёте на внешнюю страницу CloudTips.\n\n"
                                + "Поддержка является добровольной и безвозмездной. Она не открывает "
                                + "дополнительные функции, подписку или другие преимущества. "
                                + "Все возможности приложения остаются бесплатными."),
                host.tr("Cancel", "Отмена"),
                host.tr("Support", "Поддержать"),
                false,
                this::openSupportPage);
    }

    private void openSupportPage() {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(SUPPORT_URL));
            intent.addCategory(Intent.CATEGORY_BROWSABLE);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            host.startActivity(intent);
        } catch (RuntimeException error) {
            host.showConfirmPanel(
                    host.tr("CloudTips is unavailable", "CloudTips недоступен"),
                    host.tr("No application can open the support page.",
                            "Нет приложения, которое может открыть страницу поддержки."),
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

    void exportLibraryBackup() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        intent.putExtra(Intent.EXTRA_TITLE, "Voltune-backup.json");
        host.startActivityForResult(intent, EXPORT_BACKUP);
    }

    void importLibraryBackup() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        host.startActivityForResult(intent, IMPORT_BACKUP);
    }

    boolean handleActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != EXPORT_BACKUP && requestCode != IMPORT_BACKUP) {
            return false;
        }
        if (resultCode != Activity.RESULT_OK || data == null || data.getData() == null) {
            return true;
        }
        try {
            if (requestCode == EXPORT_BACKUP) {
                String backup = LibraryBackupManager.exportBackup(host, host.tracks,
                        host.playlists);
                OutputStream output = host.getContentResolver().openOutputStream(data.getData(),
                        "wt");
                if (output == null) {
                    throw new IllegalStateException("Output file is unavailable");
                }
                try {
                    output.write(backup.getBytes(StandardCharsets.UTF_8));
                } finally {
                    output.close();
                }
                Toast.makeText(host, host.tr("Backup exported", "Резервная копия сохранена"),
                        Toast.LENGTH_SHORT).show();
            } else {
                InputStream input = host.getContentResolver().openInputStream(data.getData());
                if (input == null) {
                    throw new IllegalStateException("Input file is unavailable");
                }
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                try {
                    byte[] buffer = new byte[8192];
                    int total = 0;
                    int count;
                    while ((count = input.read(buffer)) >= 0) {
                        total += count;
                        if (total > LibraryBackupManager.MAX_BACKUP_BYTES) {
                            throw new IllegalArgumentException("Backup is too large");
                        }
                        bytes.write(buffer, 0, count);
                    }
                } finally {
                    input.close();
                }
                LibraryBackupManager.ImportResult imported =
                        LibraryBackupManager.importBackup(host,
                                new String(bytes.toByteArray(), StandardCharsets.UTF_8),
                                host.tracks);
                host.playlists.clear();
                host.playlists.addAll(imported.playlists);
                host.saveState();
                host.rebuildUi();
                Toast.makeText(host, host.tr("Backup restored", "Резервная копия восстановлена"),
                        Toast.LENGTH_SHORT).show();
            }
        } catch (Exception error) {
            host.showConfirmPanel(host.tr("Backup error", "Ошибка резервной копии"),
                    host.tr("The selected file is damaged or unsupported.",
                            "Выбранный файл повреждён или не поддерживается."), () -> {
                    });
        }
        return true;
    }

    void confirmRemoveUnavailableSongs() {
        LibraryFileAccessManager.Result result = LibraryFileAccessManager.inspect(host,
                host.tracks);
        if (result.unavailable.isEmpty()) {
            host.showConfirmPanel(
                    host.tr("File access", "Доступ к файлам"),
                    host.tr("All library files are available.",
                            "Все файлы медиатеки доступны."),
                    () -> {
                    });
            return;
        }
        host.showConfirmPanel(
                host.tr("Remove unavailable songs?", "Удалить недоступные песни?"),
                host.tr("Unavailable records: ", "Недоступных записей: ")
                        + result.unavailable.size() + "\n\n"
                        + host.tr("The audio files themselves will not be deleted.",
                                "Сами аудиофайлы удалены не будут."),
                () -> {
                    LibraryFileAccessManager.removeUnavailable(host, host.tracks,
                            host.favorites, host.playlists);
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
        openLanguageDialog();
    }

    private void addDoneButton(LinearLayout parent, FrameLayout shade) {
        Button done = host.button(host.tr("Done", "Готово"));
        host.applyPrimaryButtonStyle(done);
        done.setOnClickListener(view -> {
            if (shade.getParent() != null) {
                host.overlayHost.removeView(shade);
            }
            host.updateMini();
        });
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, host.dp(50));
        params.setMargins(0, host.dp(8), 0, 0);
        parent.addView(done, params);
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
