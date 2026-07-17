package com.dumuzeyn.mp3player.ui.player;

import java.util.Locale;

public final class PlaybackTimeFormatter {
    private PlaybackTimeFormatter() {
    }

    public static String formatMilliseconds(int milliseconds) {
        return formatSeconds(Math.max(0, milliseconds) / 1000L);
    }

    public static String formatSeconds(long seconds) {
        long safeSeconds = Math.max(0L, seconds);
        return (safeSeconds / 60)
                + ":"
                + String.format(Locale.ROOT, "%02d", Long.valueOf(safeSeconds % 60));
    }
}
