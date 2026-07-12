package com.dumuzeyn.mp3player;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.View;

final class ThemeColorWheelView extends View {
    interface Listener {
        void onColorPicked(int color);
    }

    private final MainActivityCore host;
    private final Listener listener;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float[] hsv = {0.0f, 0.0f, 1.0f};
    private Bitmap wheelBitmap;
    private int wheelSize;
    private int wheelRadius;
    private int wheelCenterX;
    private int wheelCenterY;
    private int brightnessTop;
    private int brightnessHeight;

    ThemeColorWheelView(MainActivityCore host, int initialColor, Listener listener) {
        super(host);
        this.host = host;
        this.listener = listener;
        Color.colorToHSV(initialColor, hsv);
        hsv[2] = Math.max(0.02f, hsv[2]);
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        brightnessHeight = host.dp(28);
        wheelSize = Math.max(1, Math.min(width, height - host.dp(52)));
        wheelRadius = Math.max(1, wheelSize / 2);
        wheelCenterX = width / 2;
        wheelCenterY = wheelRadius;
        brightnessTop = wheelSize + host.dp(18);
        wheelBitmap = buildWheelBitmap(wheelSize);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (wheelBitmap == null) {
            return;
        }
        canvas.drawBitmap(wheelBitmap, wheelCenterX - wheelRadius, 0, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(host.dp(2));
        paint.setColor(ThemeManager.mixColor(host.fg, host.bg, 0.65f));
        canvas.drawCircle(wheelCenterX, wheelCenterY, wheelRadius - host.dp(1), paint);
        float angle = (float) Math.toRadians(hsv[0]);
        float selectorRadius = hsv[1] * wheelRadius;
        float selectorX = wheelCenterX + ((float) Math.cos(angle) * selectorRadius);
        float selectorY = wheelCenterY + ((float) Math.sin(angle) * selectorRadius);
        paint.setStrokeWidth(host.dp(3));
        paint.setColor(ThemeManager.readableOn(Color.HSVToColor(hsv)));
        canvas.drawCircle(selectorX, selectorY, host.dp(9), paint);
        drawBrightness(canvas);
    }

    private Bitmap buildWheelBitmap(int size) {
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        int radius = Math.max(1, size / 2);
        float[] pixelHsv = {0.0f, 0.0f, 1.0f};
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                float dx = x - radius;
                float dy = y - radius;
                float distance = (float) Math.sqrt((dx * dx) + (dy * dy));
                if (distance > radius) {
                    bitmap.setPixel(x, y, Color.TRANSPARENT);
                    continue;
                }
                float hue = (float) Math.toDegrees(Math.atan2(dy, dx));
                pixelHsv[0] = hue < 0.0f ? hue + 360.0f : hue;
                pixelHsv[1] = Math.min(1.0f, distance / radius);
                bitmap.setPixel(x, y, Color.HSVToColor(pixelHsv));
            }
        }
        return bitmap;
    }

    private void drawBrightness(Canvas canvas) {
        int left = host.dp(2);
        int right = getWidth() - host.dp(2);
        for (int x = left; x <= right; x++) {
            float value = (float) (x - left) / Math.max(1, right - left);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(1.0f);
            paint.setColor(Color.HSVToColor(new float[]{hsv[0], hsv[1], value}));
            canvas.drawLine(x, brightnessTop, x, brightnessTop + brightnessHeight, paint);
        }
        paint.setStrokeWidth(host.dp(2));
        paint.setColor(ThemeManager.mixColor(host.fg, host.bg, 0.65f));
        canvas.drawRect(left, brightnessTop, right, brightnessTop + brightnessHeight, paint);
        float knobX = left + (hsv[2] * (right - left));
        paint.setStrokeWidth(host.dp(3));
        paint.setColor(ThemeManager.readableOn(Color.HSVToColor(hsv)));
        canvas.drawCircle(knobX, brightnessTop + (brightnessHeight / 2.0f), host.dp(8), paint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getActionMasked() != MotionEvent.ACTION_DOWN
                && event.getActionMasked() != MotionEvent.ACTION_MOVE) {
            return true;
        }
        if (event.getY() >= brightnessTop - host.dp(8)) {
            int left = host.dp(2);
            int right = getWidth() - host.dp(2);
            hsv[2] = Math.max(0.0f, Math.min(1.0f,
                    (event.getX() - left) / Math.max(1, right - left)));
        } else {
            float dx = event.getX() - wheelCenterX;
            float dy = event.getY() - wheelCenterY;
            float distance = (float) Math.sqrt((dx * dx) + (dy * dy));
            if (distance <= wheelRadius) {
                float hue = (float) Math.toDegrees(Math.atan2(dy, dx));
                hsv[0] = hue < 0.0f ? hue + 360.0f : hue;
                hsv[1] = Math.min(1.0f, distance / wheelRadius);
            }
        }
        listener.onColorPicked(Color.HSVToColor(hsv));
        invalidate();
        return true;
    }
}
