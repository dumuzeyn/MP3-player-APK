package com.dumuzeyn.mp3player;

import android.graphics.Color;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import java.util.Locale;

final class GradientSettingsController {
    private static final int MAIN_START = 0;
    private static final int MAIN_END = 1;
    private static final int PLAYER_START = 2;
    private static final int PLAYER_END = 3;
    private final MainActivityCore host;

    GradientSettingsController(MainActivityCore host) {
        this.host = host;
    }

    void openDialog() {
        FrameLayout shade = host.shade();
        LinearLayout panel = host.panelCard();
        panel.setPadding(host.dp(16), host.dp(16), host.dp(16), host.dp(16));
        panel.addView(host.text(host.tr("Gradient backgrounds", "Градиентные фоны"), 22, true),
                new LinearLayout.LayoutParams(-1, host.dp(46)));
        addAction(panel, host.tr("Main screen: ", "Основной экран: ")
                + host.tr(host.gradientMainBackground ? "gradient" : "solid",
                host.gradientMainBackground ? "градиент" : "однотонный"), () -> {
            host.gradientMainBackground = !host.gradientMainBackground;
            saveAndReopen(shade);
        });
        addColor(panel, host.tr("Main color 1", "Основной цвет 1"), MAIN_START, host.mainGradientStart, shade);
        addColor(panel, host.tr("Main color 2", "Основной цвет 2"), MAIN_END, host.mainGradientEnd, shade);
        addAction(panel, host.tr("Full player: ", "Большой плеер: ")
                + host.tr(host.gradientPlayerBackground ? "gradient" : "solid",
                host.gradientPlayerBackground ? "градиент" : "однотонный"), () -> {
            host.gradientPlayerBackground = !host.gradientPlayerBackground;
            saveAndReopen(shade);
        });
        addColor(panel, host.tr("Player color 1", "Цвет плеера 1"), PLAYER_START, host.playerGradientStart, shade);
        addColor(panel, host.tr("Player color 2", "Цвет плеера 2"), PLAYER_END, host.playerGradientEnd, shade);
        Button done = host.button(host.tr("Done", "Готово"));
        host.applyPrimaryButtonStyle(done);
        done.setOnClickListener(view -> {
            host.saveState();
            host.overlayHost.removeView(shade);
            host.rebuildUi();
        });
        panel.addView(done, new LinearLayout.LayoutParams(-1, host.dp(50)));
        shade.addView(panel, host.centerParams(host.dp(350), -2));
        host.overlayHost.addView(shade);
        host.updateMini();
    }

    private void addColor(LinearLayout panel, String label, int target, int color, FrameLayout parentShade) {
        Button button = host.button(label + "  " + colorHex(color));
        button.setTextColor(ThemeManager.readableOn(color));
        host.setSurface(button, color, false);
        button.setOnClickListener(view -> {
            host.overlayHost.removeView(parentShade);
            openColorPicker(label, target, color);
        });
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, host.dp(48));
        params.setMargins(0, host.dp(3), 0, host.dp(3));
        panel.addView(button, params);
    }

    private void openColorPicker(String title, int target, int initialColor) {
        FrameLayout shade = host.shade();
        LinearLayout panel = host.panelCard();
        panel.setPadding(host.dp(16), host.dp(16), host.dp(16), host.dp(16));
        panel.addView(host.text(title, 21, true), new LinearLayout.LayoutParams(-1, host.dp(46)));
        View preview = new View(host);
        preview.setBackgroundColor(initialColor);
        panel.addView(preview, new LinearLayout.LayoutParams(-1, host.dp(36)));
        ThemeColorWheelView wheel = new ThemeColorWheelView(host, initialColor, color -> {
            setColor(target, color);
            preview.setBackgroundColor(color);
        });
        panel.addView(wheel, new LinearLayout.LayoutParams(-1, host.dp(280)));
        Button done = host.button(host.tr("Done", "Готово"));
        host.applyPrimaryButtonStyle(done);
        done.setOnClickListener(view -> {
            host.saveState();
            host.overlayHost.removeView(shade);
            openDialog();
        });
        panel.addView(done, new LinearLayout.LayoutParams(-1, host.dp(52)));
        shade.addView(panel, host.centerParams(host.dp(350), -2));
        host.overlayHost.addView(shade);
    }

    private void addAction(LinearLayout panel, String label, Runnable action) {
        Button button = host.button(label);
        host.applySecondaryButtonStyle(button);
        button.setOnClickListener(view -> action.run());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, host.dp(48));
        params.setMargins(0, host.dp(3), 0, host.dp(3));
        panel.addView(button, params);
    }

    private void saveAndReopen(FrameLayout shade) {
        host.saveState();
        host.overlayHost.removeView(shade);
        openDialog();
    }

    private void setColor(int target, int color) {
        if (target == MAIN_START) {
            host.mainGradientStart = color;
        } else if (target == MAIN_END) {
            host.mainGradientEnd = color;
        } else if (target == PLAYER_START) {
            host.playerGradientStart = color;
        } else {
            host.playerGradientEnd = color;
        }
    }

    private String colorHex(int color) {
        return String.format(Locale.ROOT, "#%02X%02X%02X",
                Color.red(color), Color.green(color), Color.blue(color));
    }
}
