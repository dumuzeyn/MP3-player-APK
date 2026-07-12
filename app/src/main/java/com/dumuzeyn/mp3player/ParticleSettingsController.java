package com.dumuzeyn.mp3player;

import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

final class ParticleSettingsController {
    private final MainActivityCore host;

    ParticleSettingsController(MainActivityCore host) {
        this.host = host;
    }

    void openDialog() {
        FrameLayout shade = host.shade();
        LinearLayout panel = host.panelCard();
        panel.setPadding(host.dp(16), host.dp(16), host.dp(16), host.dp(16));
        panel.addView(host.text(host.tr("Particle effects", "Эффекты частиц"), 22, true),
                new LinearLayout.LayoutParams(-1, host.dp(48)));
        addSlider(panel, host.tr("Frequency", "Частота"), 10, 100, host.particleFrequency,
                value -> host.particleFrequency = value);
        addSlider(panel, host.tr("Size", "Размер"), 60, 150, host.particleSize,
                value -> host.particleSize = value);
        addSlider(panel, host.tr("Lifetime", "Время существования"), 50, 180, host.particleLifetime,
                value -> host.particleLifetime = value);

        Button close = host.button(host.tr("Done", "Готово"));
        host.applyPrimaryButtonStyle(close);
        close.setOnClickListener(view -> {
            host.saveState();
            host.refreshParticleSettings();
            host.overlayHost.removeView(shade);
        });
        LinearLayout.LayoutParams closeParams = new LinearLayout.LayoutParams(-1, host.dp(50));
        closeParams.setMargins(0, host.dp(10), 0, 0);
        panel.addView(close, closeParams);
        shade.addView(panel, host.centerParams(host.dp(340), -2));
        host.overlayHost.addView(shade);
        host.updateMini();
    }

    private void addSlider(LinearLayout panel, String name, int minimum, int maximum, int initial,
            ValueChanged listener) {
        int value = Math.max(minimum, Math.min(maximum, initial));
        TextView label = host.text(name + ": " + value + "%", 15, false);
        panel.addView(label, new LinearLayout.LayoutParams(-1, host.dp(34)));
        SeekBar seek = new SeekBar(host);
        seek.setMax(maximum - minimum);
        seek.setProgress(value - minimum);
        host.applySeekBarColors(seek);
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (!fromUser) {
                    return;
                }
                int selected = minimum + progress;
                label.setText(name + ": " + selected + "%");
                listener.changed(selected);
                host.refreshParticleSettings();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                host.saveState();
            }
        });
        panel.addView(seek, new LinearLayout.LayoutParams(-1, host.dp(44)));
    }

    private interface ValueChanged {
        void changed(int value);
    }
}
