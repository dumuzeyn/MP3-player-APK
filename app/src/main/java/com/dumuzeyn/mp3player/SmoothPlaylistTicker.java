package com.dumuzeyn.mp3player;

import android.text.TextUtils;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.TextView;
import java.util.ArrayList;

final class SmoothPlaylistTicker extends FrameLayout {
    private final MainActivityCore host;
    private final TextView textView;
    private String contentKey = "";

    SmoothPlaylistTicker(MainActivityCore host) {
        super(host);
        this.host = host;
        setClipChildren(true);
        setClipToPadding(true);
        this.textView = host.text("", 16, true);
        this.textView.setSingleLine(false);
        this.textView.setLineSpacing(0.0f, 1.0f);
        this.textView.setEllipsize(TextUtils.TruncateAt.END);
        this.textView.setGravity(Gravity.START);
        addView(this.textView, new FrameLayout.LayoutParams(-1, -2));
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int lineHeight = Math.max(1, this.textView.getLineHeight());
        setMeasuredDimension(getMeasuredWidth(), (lineHeight * 3) + host.dp(4));
    }

    void bindTracks(ArrayList<Track> tracks) {
        String key = buildKey(tracks);
        if (key.equals(this.contentKey)) {
            return;
        }
        this.contentKey = key;
        this.textView.animate().cancel();
        this.textView.setTranslationY(0.0f);
        this.textView.setText(buildCreditsText(tracks));
        if (tracks.size() <= 3) {
            return;
        }
        post(new Runnable() {
            @Override
            public void run() {
                startCredits(tracks.size());
            }
        });
    }

    private void startCredits(final int trackCount) {
        if (getHeight() <= 0 || textView.getLineHeight() <= 0) {
            postDelayed(new Runnable() {
                @Override
                public void run() {
                    startCredits(trackCount);
                }
            }, 400L);
            return;
        }
        float travel = Math.max(1.0f, (float) (textView.getLineHeight() * (trackCount + 1)));
        long duration = Math.max(16000L, trackCount * 4200L);
        textView.setTranslationY(0.0f);
        textView.animate()
                .translationY(-travel)
                .setDuration(host.animations ? duration : 0L)
                .withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        if (getParent() != null) {
                            startCredits(trackCount);
                        }
                    }
                })
                .start();
    }

    private String buildCreditsText(ArrayList<Track> tracks) {
        if (tracks.isEmpty()) {
            return host.tr3("No songs in this playlist yet.", "В плейлисте пока нет песен.", "∅ ♪");
        }
        StringBuilder builder = new StringBuilder();
        appendTitles(builder, tracks);
        if (tracks.size() > 3) {
            builder.append("\n");
            appendTitles(builder, tracks);
        }
        return builder.toString();
    }

    private void appendTitles(StringBuilder builder, ArrayList<Track> tracks) {
        for (int i = 0; i < tracks.size(); i++) {
            if (builder.length() > 0) {
                builder.append("\n");
            }
            builder.append(tracks.get(i).title);
        }
    }

    private String buildKey(ArrayList<Track> tracks) {
        StringBuilder builder = new StringBuilder();
        for (Track track : tracks) {
            builder.append(track.uri).append('|');
        }
        return builder.toString();
    }
}
