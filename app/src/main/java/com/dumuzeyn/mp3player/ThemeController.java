package com.dumuzeyn.mp3player;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import java.util.Locale;

final class ThemeController {
    private static final String THEME = "theme";
    private static final String CUSTOM_BG = "customBg";
    private static final String CUSTOM_FG = "customFg";

    private final MainActivityCore host;
    private boolean launcherUpdatePending;

    ThemeController(MainActivityCore host) {
        this.host = host;
    }

    void load(SharedPreferences prefs) {
        host.themeMode = prefs.getString(THEME, "light");
        if (!"light".equals(host.themeMode)
                && !"dark".equals(host.themeMode)
                && !"custom".equals(host.themeMode)) {
            host.themeMode = "light";
        }
        host.customBg = prefs.getInt(CUSTOM_BG, Color.WHITE);
        host.customFg = prefs.getInt(CUSTOM_FG, Color.BLACK);
    }

    String themeName() {
        if ("dark".equals(host.themeMode)) {
            return host.tr("Dark", "Темная");
        }
        if ("custom".equals(host.themeMode)) {
            return host.tr("Custom", "Своя");
        }
        return host.tr("Light", "Светлая");
    }

    void applyPalette() {
        host.dark = isDarkTheme(host.themeMode, host.customBg);
        if ("custom".equals(host.themeMode)) {
            host.bg = host.customBg;
            host.fg = host.customFg;
            host.primaryText = host.customFg;
            host.secondaryText = mixColor(host.customFg, host.customBg, 0.58f);
            host.card = mixColor(host.customBg, host.customFg, host.dark ? 0.92f : 0.96f);
            host.cardStroke = mixColor(host.customFg, host.customBg, 0.18f);
            host.purple = host.customFg;
            host.purpleDark = mixColor(host.customFg, host.customBg, 0.82f);
            host.purpleSoft = mixColor(host.customFg, host.customBg, 0.18f);
            host.yellow = Color.rgb(255, 208, 0);
            host.yellowDark = Color.rgb(231, 185, 0);
            host.yellowSoft = Color.rgb(255, 245, 190);
        } else if (host.dark) {
            host.bg = Color.rgb(17, 16, 21);
            host.fg = Color.WHITE;
            host.primaryText = Color.WHITE;
            host.secondaryText = Color.rgb(170, 164, 178);
            host.card = Color.rgb(26, 24, 31);
            host.cardStroke = Color.rgb(51, 46, 58);
            host.purple = Color.rgb(163, 92, 255);
            host.purpleDark = Color.rgb(124, 50, 232);
            host.purpleSoft = Color.rgb(44, 35, 58);
            host.yellow = Color.rgb(255, 212, 56);
            host.yellowDark = Color.rgb(231, 185, 0);
            host.yellowSoft = Color.rgb(72, 62, 33);
        } else {
            host.bg = Color.WHITE;
            host.fg = Color.rgb(24, 21, 29);
            host.primaryText = Color.rgb(24, 21, 29);
            host.secondaryText = Color.rgb(118, 113, 125);
            host.card = Color.rgb(250, 249, 252);
            host.cardStroke = Color.rgb(233, 229, 238);
            host.purple = Color.rgb(124, 50, 232);
            host.purpleDark = Color.rgb(97, 32, 197);
            host.purpleSoft = Color.rgb(238, 228, 255);
            host.yellow = Color.rgb(255, 208, 0);
            host.yellowDark = Color.rgb(231, 185, 0);
            host.yellowSoft = Color.rgb(255, 245, 190);
        }
        host.muted = host.secondaryText;
        host.line = host.cardStroke;
        host.panel = host.card;
    }

    void applyWindow() {
        host.getWindow().setBackgroundDrawable(new ColorDrawable(host.bg));
        host.getWindow().setStatusBarColor(host.bg);
        host.getWindow().setNavigationBarColor(host.bg);
        if (Build.VERSION.SDK_INT >= 23) {
            host.getWindow().getDecorView().setSystemUiVisibility(host.dark ? 0 : 8192);
        }
        updateTaskPreview();
    }

    void openDialog() {
        final FrameLayout shade = host.shade();
        LinearLayout panel = host.panelCard();
        panel.setPadding(host.dp(16), host.dp(16), host.dp(16), host.dp(16));
        panel.addView(host.text(host.tr("Theme", "Тема"), 22, true),
                new LinearLayout.LayoutParams(-1, host.dp(46)));
        addChoice(panel, host.tr("Light", "Светлая"), "light");
        addChoice(panel, host.tr("Dark", "Темная"), "dark");
        addChoice(panel, host.tr("Custom", "Своя"), "custom");
        panel.addView(host.text(host.tr("Background", "Фон"), 16, true),
                new LinearLayout.LayoutParams(-1, host.dp(34)));
        addColorButton(panel, true);
        panel.addView(host.text(host.tr("Text and accent", "Текст и акцент"), 16, true),
                new LinearLayout.LayoutParams(-1, host.dp(34)));
        addColorButton(panel, false);
        shade.addView(panel, host.centerParams(host.dp(340), -2));
        host.overlayHost.addView(shade);
        host.updateMini();
    }

    private void addChoice(LinearLayout parent, String label, final String mode) {
        Button button = host.button(label);
        button.setTextSize(17.0f);
        button.setGravity(8388627);
        button.setPadding(host.dp(18), 0, host.dp(12), 0);
        if (mode.equals(host.themeMode)) {
            host.applyPrimaryButtonStyle(button);
        } else {
            host.applySecondaryButtonStyle(button);
        }
        button.setOnClickListener(view -> applyTheme(mode));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, host.dp(54));
        params.setMargins(0, host.dp(5), 0, host.dp(5));
        parent.addView(button, params);
    }

    private void addColorButton(LinearLayout parent, final boolean background) {
        int color = background ? host.customBg : host.customFg;
        Button button = host.button(colorHex(color));
        button.setTextColor(ThemeManager.readableOn(color));
        host.setSurface(button, color, false);
        button.setOnClickListener(view -> {
            host.overlayHost.removeAllViews();
            openColorPicker(background);
        });
        parent.addView(button, new LinearLayout.LayoutParams(-1, host.dp(52)));
    }

    private void openColorPicker(final boolean background) {
        final FrameLayout shade = host.shade();
        LinearLayout panel = host.panelCard();
        panel.setPadding(host.dp(16), host.dp(16), host.dp(16), host.dp(16));
        panel.addView(host.text(background
                        ? host.tr("Background", "Фон")
                        : host.tr("Text and accent", "Текст и акцент"), 22, true),
                new LinearLayout.LayoutParams(-1, host.dp(46)));
        final View preview = new View(host);
        preview.setBackgroundColor(background ? host.customBg : host.customFg);
        panel.addView(preview, new LinearLayout.LayoutParams(-1, host.dp(34)));

        ThemeColorWheelView wheel = new ThemeColorWheelView(
                host,
                background ? host.customBg : host.customFg,
                color -> {
                    host.themeMode = "custom";
                    if (background) {
                        host.customBg = color;
                    } else {
                        host.customFg = color;
                    }
                    preview.setBackgroundColor(color);
                });
        LinearLayout.LayoutParams wheelParams = new LinearLayout.LayoutParams(-1, host.dp(280));
        wheelParams.setMargins(0, host.dp(12), 0, host.dp(12));
        panel.addView(wheel, wheelParams);

        LinearLayout actions = host.row();
        Button back = host.button(host.tr("Back", "Назад"));
        back.setOnClickListener(view -> {
            host.overlayHost.removeView(shade);
            openDialog();
        });
        actions.addView(back, new LinearLayout.LayoutParams(0, host.dp(54), 1.0f));
        Button done = host.button(host.tr("Done", "Готово"));
        host.applyPrimaryButtonStyle(done);
        done.setOnClickListener(view -> applyTheme("custom"));
        actions.addView(done, new LinearLayout.LayoutParams(0, host.dp(54), 1.0f));
        panel.addView(actions);
        shade.addView(panel, host.centerParams(host.dp(340), -2));
        host.overlayHost.addView(shade);
    }

    private void applyTheme(String mode) {
        host.themeMode = mode;
        host.dark = isDarkTheme(mode, host.customBg);
        host.saveState();
        if (host.overlayHost != null) {
            host.overlayHost.removeAllViews();
        }
        host.rebuildUiForTheme();
        launcherUpdatePending = true;
    }

    void onHostStopped() {
        if (!launcherUpdatePending) {
            return;
        }
        launcherUpdatePending = false;
        updateLauncherIcon();
    }

    void updateLauncherIcon() {
        PackageManager packageManager = host.getPackageManager();
        ComponentName light = new ComponentName(host, host.getPackageName() + ".LauncherLight");
        ComponentName dark = new ComponentName(host, host.getPackageName() + ".LauncherDark");
        boolean useDark = isDarkTheme(host.themeMode, host.customBg);
        try {
            packageManager.setComponentEnabledSetting(
                    useDark ? dark : light,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP);
            packageManager.setComponentEnabledSetting(
                    useDark ? light : dark,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP);
        } catch (RuntimeException ignored) {
            // A launcher may reject alias changes while the task is visible.
        }
    }

    private void updateTaskPreview() {
        if (Build.VERSION.SDK_INT < 21) {
            return;
        }
        try {
            host.setTaskDescription(new ActivityManager.TaskDescription(
                    "MP3 Player", launcherPreviewIcon(), host.bg));
        } catch (RuntimeException ignored) {
        }
    }

    private Bitmap launcherPreviewIcon() {
        try {
            Drawable drawable = host.getResources().getDrawable(
                    host.dark ? R.mipmap.ic_launcher_dark : R.mipmap.ic_launcher_home);
            int size = Math.max(1, host.dp(64));
            Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, size, size);
            drawable.draw(canvas);
            return bitmap;
        } catch (RuntimeException error) {
            return BitmapFactory.decodeResource(host.getResources(), host.getApplicationInfo().icon);
        }
    }

    private String colorHex(int color) {
        return String.format(Locale.ROOT, "#%02X%02X%02X",
                Color.red(color), Color.green(color), Color.blue(color));
    }

    static boolean isDarkTheme(String themeMode, int customBackground) {
        return "dark".equals(themeMode)
                || ("custom".equals(themeMode) && ThemeManager.isDarkColor(customBackground));
    }

    static int mixColor(int first, int second, float amount) {
        return ThemeManager.mixColor(first, second, amount);
    }
}
