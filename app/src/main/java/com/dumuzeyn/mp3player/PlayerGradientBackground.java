package com.dumuzeyn.mp3player;

import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.os.SystemClock;
import android.view.View;

final class PlayerGradientBackground extends View {
    private final MainActivityCore host;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final int startColor;
    private final int endColor;

    PlayerGradientBackground(MainActivityCore host, int startColor, int endColor) {
        super(host);
        this.host = host;
        this.startColor = startColor;
        this.endColor = endColor;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float phase = host.animations
                ? (SystemClock.uptimeMillis() % 12000L) / 12000.0f
                : 0.18f;
        double angle = phase * Math.PI * 2.0;
        float centerX = getWidth() * 0.5f;
        float centerY = getHeight() * 0.5f;
        float radius = Math.max(getWidth(), getHeight()) * 0.72f;
        float dx = (float) Math.cos(angle) * radius;
        float dy = (float) Math.sin(angle) * radius;
        int purpleTint = blend(host.bg, this.startColor, host.dark ? 0.82f : 0.38f);
        int yellowTint = blend(host.bg, this.endColor, host.dark ? 0.72f : 0.30f);
        paint.setShader(new LinearGradient(centerX - dx, centerY - dy, centerX + dx, centerY + dy,
                new int[]{host.bg, purpleTint, yellowTint, host.bg},
                new float[]{0.0f, 0.38f, 0.68f, 1.0f}, Shader.TileMode.CLAMP));
        canvas.drawRect(0.0f, 0.0f, getWidth(), getHeight(), paint);
        if (host.animations && isAttachedToWindow()) {
            postInvalidateDelayed(40L);
        }
    }

    private static int blend(int first, int second, float amount) {
        float inverse = 1.0f - amount;
        int red = Math.round(((first >> 16) & 255) * inverse + ((second >> 16) & 255) * amount);
        int green = Math.round(((first >> 8) & 255) * inverse + ((second >> 8) & 255) * amount);
        int blue = Math.round((first & 255) * inverse + (second & 255) * amount);
        return 0xff000000 | (red << 16) | (green << 8) | blue;
    }
}
