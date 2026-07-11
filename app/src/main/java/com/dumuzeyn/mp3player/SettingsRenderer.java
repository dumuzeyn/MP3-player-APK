package com.dumuzeyn.mp3player;

final class SettingsRenderer {
    private final MainActivity host;

    SettingsRenderer(MainActivity host) {
        this.host = host;
    }

    void render() {
        host.renderSettingsInternal();
    }
}
