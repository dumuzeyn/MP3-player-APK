package com.dumuzeyn.mp3player;

final class SettingsRenderer {
    private final MainActivityCore host;

    SettingsRenderer(MainActivityCore host) {
        this.host = host;
    }

    void render() {
        host.renderSettingsInternal();
    }
}
