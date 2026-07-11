package com.dumuzeyn.mp3player;

final class SettingsMenuRenderer implements MenuRenderer {
    private final MainActivityCore host;

    SettingsMenuRenderer(MainActivityCore host) {
        this.host = host;
    }

    @Override
    public boolean needsMiniSpacer() {
        return true;
    }

    @Override
    public void render() {
        host.renderSettings();
    }
}
