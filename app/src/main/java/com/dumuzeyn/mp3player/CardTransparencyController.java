package com.dumuzeyn.mp3player;

import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.ScrollView;
import android.widget.TextView;

final class CardTransparencyController {
    private static final int MIN_OPACITY = 35;
    private static final int MAX_OPACITY = 100;
    private final MainActivityCore host;

    CardTransparencyController(MainActivityCore host) {
        this.host = host;
    }

    String settingLabel() {
        return host.tr("Card opacity by section", "Прозрачность карточек по разделам");
    }

    void openDialog() {
        FrameLayout shade = host.shade();
        LinearLayout panel = host.panelCard();
        panel.setPadding(host.dp(16), host.dp(12), host.dp(16), host.dp(12));
        TextView title = host.text(settingLabel(), 18, true);
        title.setPadding(0, 0, 0, host.dp(8));
        panel.addView(title, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout controls = new LinearLayout(host);
        controls.setOrientation(LinearLayout.VERTICAL);
        addControl(controls, host.tr("Songs", "Песни"),
                new OpacityValue() {
                    @Override
                    public int get() {
                        return host.songCardOpacity;
                    }

                    @Override
                    public void set(int value) {
                        host.songCardOpacity = value;
                    }
                });
        addControl(controls, host.tr("Favorites", "Избранное"),
                new OpacityValue() {
                    @Override
                    public int get() {
                        return host.favoriteCardOpacity;
                    }

                    @Override
                    public void set(int value) {
                        host.favoriteCardOpacity = value;
                    }
                });
        addControl(controls, host.tr("Playlists", "Плейлисты"),
                new OpacityValue() {
                    @Override
                    public int get() {
                        return host.playlistCardOpacity;
                    }

                    @Override
                    public void set(int value) {
                        host.playlistCardOpacity = value;
                    }
                });
        addControl(controls, host.tr("Genres", "Жанры"),
                new OpacityValue() {
                    @Override
                    public int get() {
                        return host.genreCardOpacity;
                    }

                    @Override
                    public void set(int value) {
                        host.genreCardOpacity = value;
                    }
                });
        addControl(controls, host.tr("Artists", "Исполнители"),
                new OpacityValue() {
                    @Override
                    public int get() {
                        return host.artistCardOpacity;
                    }

                    @Override
                    public void set(int value) {
                        host.artistCardOpacity = value;
                    }
                });
        addControl(controls, host.tr("Albums", "Альбомы"),
                new OpacityValue() {
                    @Override
                    public int get() {
                        return host.albumCardOpacity;
                    }

                    @Override
                    public void set(int value) {
                        host.albumCardOpacity = value;
                    }
                });
        addControl(controls, host.tr("Settings", "Настройки"),
                new OpacityValue() {
                    @Override
                    public int get() {
                        return host.settingsCardOpacity;
                    }

                    @Override
                    public void set(int value) {
                        host.settingsCardOpacity = value;
                    }
                });
        addControl(controls, host.tr("Mini-player", "Мини-плеер"),
                new OpacityValue() {
                    @Override
                    public int get() {
                        return host.miniPlayerCardOpacity;
                    }

                    @Override
                    public void set(int value) {
                        host.miniPlayerCardOpacity = value;
                    }
                });
        addControl(controls, host.tr("Application header", "Шапка приложения"),
                new OpacityValue() {
                    @Override
                    public int get() {
                        return host.headerCardOpacity;
                    }

                    @Override
                    public void set(int value) {
                        host.headerCardOpacity = value;
                    }
                });
        addControl(controls, host.tr("Dialogs", "Диалоговые окна"),
                new OpacityValue() {
                    @Override
                    public int get() {
                        return host.dialogCardOpacity;
                    }

                    @Override
                    public void set(int value) {
                        host.dialogCardOpacity = value;
                    }
                });

        ScrollView scroll = new ScrollView(host);
        scroll.addView(controls, new ScrollView.LayoutParams(-1, -2));
        panel.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1.0f));

        Button done = host.button(host.tr("Done", "Готово"));
        host.applyPrimaryButtonStyle(done);
        done.setOnClickListener(view -> {
            host.saveState();
            host.overlayHost.removeView(shade);
            host.rebuildUi();
        });
        panel.addView(done, new LinearLayout.LayoutParams(-1, host.dp(48)));
        int maxHeight = host.getResources().getDisplayMetrics().heightPixels - host.dp(96);
        shade.addView(panel, host.centerParams(host.dp(350), Math.min(host.dp(600), maxHeight)));
        host.overlayHost.addView(shade);
        host.updateMini();
    }

    private void addControl(LinearLayout panel, String title, OpacityValue value) {
        TextView label = host.text(labelText(title, value.get()), 14, true);
        panel.addView(label, new LinearLayout.LayoutParams(-1, host.dp(24)));

        SeekBar seek = new SeekBar(host);
        seek.setMax(MAX_OPACITY - MIN_OPACITY);
        seek.setProgress(value.get() - MIN_OPACITY);
        host.applySeekBarColors(seek);
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    value.set(MIN_OPACITY + progress);
                    label.setText(labelText(title, value.get()));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                host.saveState();
            }
        });
        panel.addView(seek, new LinearLayout.LayoutParams(-1, host.dp(32)));
    }

    private String labelText(String title, int value) {
        return title + ": " + value + "%";
    }

    private interface OpacityValue {
        int get();

        void set(int value);
    }
}
