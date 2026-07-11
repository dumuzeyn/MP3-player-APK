package com.dumuzeyn.mp3player;

final class ThemeController {
    private ThemeController() {
    }

    static boolean isDarkTheme(String themeMode, int customBackground) {
        return "dark".equals(themeMode) || ("custom".equals(themeMode) && ThemeManager.isDarkColor(customBackground));
    }

    static int mixColor(int first, int second, float amount) {
        return ThemeManager.mixColor(first, second, amount);
    }
}
