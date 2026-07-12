package com.dumuzeyn.mp3player;

import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

final class DialogController {
    private final MainActivityCore host;

    DialogController(MainActivityCore host) {
        this.host = host;
    }

    void showConfirmation(String title, String message, Runnable yesAction) {
        final FrameLayout shade = host.shade();
        LinearLayout panel = host.panelCard();
        panel.setPadding(host.dp(16), host.dp(16), host.dp(16), host.dp(16));
        panel.addView(host.text(title, 22, true), new LinearLayout.LayoutParams(-1, host.dp(46)));
        TextView messageView = host.text(message, 16, false);
        messageView.setTextColor(host.muted);
        messageView.setPadding(0, host.dp(4), 0, host.dp(14));
        panel.addView(messageView, new LinearLayout.LayoutParams(-1, -2));
        LinearLayout actions = host.row();
        Button no = host.button(host.tr("No", "Нет"));
        no.setOnClickListener(view -> close(shade));
        actions.addView(no, new LinearLayout.LayoutParams(0, host.dp(54), 1.0f));
        Button yes = host.button(host.tr("Yes", "Да"));
        host.applyPrimaryButtonStyle(yes);
        yes.setOnClickListener(view -> {
            close(shade);
            yesAction.run();
        });
        actions.addView(yes, new LinearLayout.LayoutParams(0, host.dp(54), 1.0f));
        panel.addView(actions);
        shade.addView(panel, host.centerParams(host.dp(330), -2));
        host.overlayHost.addView(shade);
        host.updateMini();
    }

    private void close(FrameLayout shade) {
        if (shade.getParent() != null) {
            host.overlayHost.removeView(shade);
        }
        host.updateMini();
    }
}
