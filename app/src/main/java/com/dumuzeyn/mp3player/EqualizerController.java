package com.dumuzeyn.mp3player;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

final class EqualizerController {
    static final String PREFS = "audio_effects";
    static final String ENABLED = "equalizer_enabled";
    static final String BAND_PREFIX = "equalizer_band_";
    static final int BAND_COUNT = 5;
    private static final String[] BAND_NAMES = {"60 Hz", "230 Hz", "910 Hz", "3.6 kHz", "14 kHz"};
    private static final int MIN_DB = -12;
    private static final int MAX_DB = 12;
    private final MainActivityCore host;
    private Button playerButton;

    EqualizerController(MainActivityCore host) {
        this.host = host;
    }

    Button createPlayerButton() {
        Button button = host.button(host.tr("Equalizer ≋", "Эквалайзер ≋"));
        button.setSingleLine(true);
        button.setTextSize(14.0f);
        button.setOnClickListener(view -> openDialog());
        this.playerButton = button;
        refreshButton();
        return button;
    }

    void openDialog() {
        final FrameLayout shade = host.shade();
        LinearLayout panel = host.panelCard();
        panel.setPadding(host.dp(16), host.dp(16), host.dp(16), host.dp(16));
        panel.addView(host.text(host.tr("Equalizer", "Эквалайзер"), 22, true),
                new LinearLayout.LayoutParams(-1, host.dp(46)));

        Button enabled = host.button(enabled()
                ? host.tr("Enabled", "Включён")
                : host.tr("Disabled", "Выключен"));
        styleToggle(enabled);
        enabled.setOnClickListener(view -> {
            setEnabled(!enabled());
            enabled.setText(enabled()
                    ? host.tr("Enabled", "Включён")
                    : host.tr("Disabled", "Выключен"));
            styleToggle(enabled);
        });
        panel.addView(enabled, new LinearLayout.LayoutParams(-1, host.dp(48)));

        for (int band = 0; band < BAND_COUNT; band++) {
            addBandControl(panel, band);
        }

        Button reset = host.button(host.tr("Reset bands", "Сбросить полосы"));
        host.applySecondaryButtonStyle(reset);
        reset.setOnClickListener(view -> {
            SharedPreferences.Editor editor = prefs().edit();
            for (int band = 0; band < BAND_COUNT; band++) {
                editor.putInt(BAND_PREFIX + band, 0);
            }
            editor.apply();
            dispatchSettings();
            host.overlayHost.removeView(shade);
            openDialog();
        });
        LinearLayout.LayoutParams resetParams = new LinearLayout.LayoutParams(-1, host.dp(48));
        resetParams.setMargins(0, host.dp(8), 0, 0);
        panel.addView(reset, resetParams);

        shade.addView(panel, host.centerParams(host.dp(340), -2));
        host.overlayHost.addView(shade);
        host.updateMini();
    }

    private void addBandControl(LinearLayout panel, int band) {
        int value = bandLevel(band);
        TextView label = host.text(bandLabel(band, value), 14, false);
        panel.addView(label, new LinearLayout.LayoutParams(-1, host.dp(28)));
        SeekBar seek = new SeekBar(host);
        seek.setMax(MAX_DB - MIN_DB);
        seek.setProgress(value - MIN_DB);
        host.applySeekBarColors(seek);
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (!fromUser) {
                    return;
                }
                int level = progress + MIN_DB;
                label.setText(bandLabel(band, level));
                prefs().edit().putInt(BAND_PREFIX + band, level).apply();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                dispatchSettings();
            }
        });
        panel.addView(seek, new LinearLayout.LayoutParams(-1, host.dp(38)));
    }

    private String bandLabel(int band, int level) {
        return BAND_NAMES[band] + "  " + (level > 0 ? "+" : "") + level + " dB";
    }

    private int bandLevel(int band) {
        return Math.max(MIN_DB, Math.min(MAX_DB, prefs().getInt(BAND_PREFIX + band, 0)));
    }

    private boolean enabled() {
        return prefs().getBoolean(ENABLED, false);
    }

    private void setEnabled(boolean value) {
        prefs().edit().putBoolean(ENABLED, value).apply();
        dispatchSettings();
        refreshButton();
    }

    private void refreshButton() {
        if (this.playerButton == null) {
            return;
        }
        if (enabled()) {
            host.applyPlayerToolStyle(this.playerButton, true);
        } else {
            host.applyPlayerToolStyle(this.playerButton, false);
        }
    }

    private void styleToggle(Button button) {
        if (enabled()) {
            host.applyPrimaryButtonStyle(button);
        } else {
            host.applySecondaryButtonStyle(button);
        }
    }

    private SharedPreferences prefs() {
        return host.getSharedPreferences(PREFS, 0);
    }

    private void dispatchSettings() {
        Intent intent = new Intent(host, PlayerService.class);
        intent.setAction(PlayerService.ACTION_AUDIO_EFFECTS);
        if (Build.VERSION.SDK_INT >= 26) {
            host.startForegroundService(intent);
        } else {
            host.startService(intent);
        }
    }
}
