package com.dumuzeyn.mp3player;

import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.SeekBar;
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
        panel.addView(host.text(settingLabel(), 19, true),
                new LinearLayout.LayoutParams(-1, host.dp(42)));

        addControl(panel, host.tr("Songs and favorites", "Песни и избранное"),
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
        addControl(panel, host.tr("Playlists", "Плейлисты"),
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
        addControl(panel, host.tr("Genres", "Жанры"),
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
        addControl(panel, host.tr("Artists", "Исполнители"),
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
        addControl(panel, host.tr("Albums", "Альбомы"),
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
        addControl(panel, host.tr("Settings", "Настройки"),
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

        Button done = host.button(host.tr("Done", "Готово"));
        host.applyPrimaryButtonStyle(done);
        done.setOnClickListener(view -> {
            host.saveState();
            host.overlayHost.removeView(shade);
            host.rebuildUi();
        });
        panel.addView(done, new LinearLayout.LayoutParams(-1, host.dp(48)));
        shade.addView(panel, host.centerParams(host.dp(350), -2));
        host.overlayHost.addView(shade);
        host.updateMini();
    }

    private void addControl(LinearLayout panel, String title, OpacityValue value) {
        TextView label = host.text(labelText(title, value.get()), 14, true);
        panel.addView(label, new LinearLayout.LayoutParams(-1, host.dp(28)));

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
        panel.addView(seek, new LinearLayout.LayoutParams(-1, host.dp(38)));
    }

    private String labelText(String title, int value) {
        return title + ": " + value + "%";
    }

    private interface OpacityValue {
        int get();

        void set(int value);
    }
}
