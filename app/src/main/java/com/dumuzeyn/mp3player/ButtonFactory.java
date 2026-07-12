package com.dumuzeyn.mp3player;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.widget.Button;

final class ButtonFactory {
    private final MainActivityCore host;

    ButtonFactory(MainActivityCore host) {
        this.host = host;
    }

    Button button(String label) {
        Button button = new Button(host);
        button.setText(label);
        button.setTextColor(host.fg);
        button.setTextSize(14.0f);
        button.setAllCaps(false);
        button.setGravity(17);
        button.setIncludeFontPadding(true);
        button.setPadding(0, 0, 0, 0);
        button.setStateListAnimator(null);
        button.setElevation(0.0f);
        button.setTranslationZ(0.0f);
        button.setMinWidth(0);
        button.setMinHeight(0);
        button.setBackgroundColor(Color.TRANSPARENT);
        return button;
    }

    Button icon(String symbol) {
        Button button = button(symbol);
        button.setTextSize(24.0f);
        return button;
    }

    Button shuffleButton() {
        Button button = icon("⇄");
        button.setTextSize(31.0f);
        button.setTypeface(Typeface.DEFAULT_BOLD);
        return button;
    }

    Button searchButton() {
        Button button = icon("⌕");
        button.setTextSize(31.0f);
        button.setTypeface(Typeface.DEFAULT_BOLD);
        return button;
    }

    void applyColors(Button button, int background, int foreground) {
        button.setTextColor(foreground);
        button.setBackground(background(background, false));
    }

    void applyPlainIcon(Button button, int color) {
        button.setTextColor(color);
        button.setBackgroundColor(Color.TRANSPARENT);
        button.setElevation(0.0f);
        button.setTranslationZ(0.0f);
    }

    void applyPrimary(Button button) {
        button.setTextColor(Color.WHITE);
        button.setBackground(background(host.purple, false));
    }

    void applySecondary(Button button) {
        button.setTextColor(host.primaryText);
        GradientDrawable drawable = background(host.card, true);
        drawable.setStroke(host.dp(1), host.cardStroke);
        button.setBackground(drawable);
    }

    void applyPlayerTool(Button button, boolean active) {
        button.setSingleLine(true);
        button.setTextSize(13.0f);
        button.setTextColor(active ? host.yellow : host.primaryText);
        GradientDrawable drawable = background(Color.TRANSPARENT, false);
        drawable.setStroke(host.dp(1), active ? host.purple : host.cardStroke);
        button.setBackground(drawable);
    }

    private GradientDrawable background(int color, boolean outlined) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(host.dp(16));
        if (outlined) {
            drawable.setStroke(host.dp(1), host.cardStroke);
        }
        return drawable;
    }
}
