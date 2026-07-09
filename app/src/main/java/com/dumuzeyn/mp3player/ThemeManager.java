package com.dumuzeyn.mp3player;

import android.graphics.Color;

final class ThemeManager {
    private ThemeManager() {
    }

    static boolean isDarkColor(int color) {
        return ((Color.red(color) * 299) + (Color.green(color) * 587) + (Color.blue(color) * 114)) / 1000 < 128;
    }

    static int mixColor(int first, int second, float amount) {
        float clamped = Math.max(0.0f, Math.min(1.0f, amount));
        int red = Math.round((Color.red(first) * clamped) + (Color.red(second) * (1.0f - clamped)));
        int green = Math.round((Color.green(first) * clamped) + (Color.green(second) * (1.0f - clamped)));
        int blue = Math.round((Color.blue(first) * clamped) + (Color.blue(second) * (1.0f - clamped)));
        return Color.rgb(red, green, blue);
    }

    static int readableOn(int color) {
        return isDarkColor(color) ? Color.WHITE : Color.BLACK;
    }
}
