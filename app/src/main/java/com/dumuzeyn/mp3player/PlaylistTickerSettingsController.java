package com.dumuzeyn.mp3player;

import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

final class PlaylistTickerSettingsController {
    private final MainActivityCore host;

    PlaylistTickerSettingsController(MainActivityCore host) {
        this.host = host;
    }

    String settingLabel() {
        return host.tr("Playlist title speed: ", "Скорость титров плейлистов: ")
                + host.playlistTickerSpeed + "%";
    }

    void openDialog() {
        FrameLayout shade = host.shade();
        LinearLayout panel = host.panelCard();
        panel.setPadding(host.dp(16), host.dp(16), host.dp(16), host.dp(16));
        TextView label = host.text(settingLabel(), 17, true);
        panel.addView(label, new LinearLayout.LayoutParams(-1, host.dp(52)));
        SeekBar seek = new SeekBar(host);
        seek.setMax(150);
        seek.setProgress(host.playlistTickerSpeed - 50);
        host.applySeekBarColors(seek);
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    host.playlistTickerSpeed = 50 + progress;
                    label.setText(settingLabel());
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
        panel.addView(seek, new LinearLayout.LayoutParams(-1, host.dp(48)));
        Button done = host.button(host.tr("Done", "Готово"));
        host.applyPrimaryButtonStyle(done);
        done.setOnClickListener(view -> {
            host.saveState();
            host.overlayHost.removeView(shade);
            host.render();
        });
        panel.addView(done, new LinearLayout.LayoutParams(-1, host.dp(50)));
        shade.addView(panel, host.centerParams(host.dp(340), -2));
        host.overlayHost.addView(shade);
        host.updateMini();
    }
}
