package com.dumuzeyn.mp3player;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.widget.Button;

final class VolumeLevelingController {
    static final String ENABLED = "volume_leveling_enabled";
    private final MainActivityCore host;
    private Button playerButton;

    VolumeLevelingController(MainActivityCore host) {
        this.host = host;
    }

    Button createPlayerButton() {
        Button button = host.button(buttonText());
        button.setSingleLine(true);
        button.setTextSize(13.0f);
        button.setOnClickListener(view -> toggle());
        this.playerButton = button;
        refreshButton();
        return button;
    }

    private void toggle() {
        prefs().edit().putBoolean(ENABLED, !enabled()).apply();
        refreshButton();
        dispatchSettings();
    }

    private boolean enabled() {
        return prefs().getBoolean(ENABLED, false);
    }

    private String buttonText() {
        if (enabled()) {
            return host.tr("Level volume ●", "Единая громкость ●");
        }
        return host.tr("Level volume ○", "Единая громкость ○");
    }

    private void refreshButton() {
        if (this.playerButton == null) {
            return;
        }
        this.playerButton.setText(buttonText());
        if (enabled()) {
            host.applyPlayerToolStyle(this.playerButton, true);
        } else {
            host.applyPlayerToolStyle(this.playerButton, false);
        }
    }

    private SharedPreferences prefs() {
        return host.getSharedPreferences(EqualizerController.PREFS, 0);
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
