package com.dumuzeyn.mp3player;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;

public class TriangleDecorView extends View {
    public static final int HEADER = 0;
    public static final int CORNER_LEFT = 1;
    public static final int CORNER_RIGHT = 2;
    public static final int COVER = 3;
    public static final int MINI = 4;
    public static final int EMPTY_STATE = 5;

    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private int mode = HEADER;
    private int purple = Color.rgb(124, 50, 232);
    private int yellow = Color.rgb(255, 208, 0);
    private float decorAlpha = 0.9f;
    private float strokeWidth = 3f;

    public TriangleDecorView(Context context) {
        super(context);
        setClickable(false);
        setFocusable(false);
        setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        setBackgroundColor(Color.TRANSPARENT);
        fillPaint.setStyle(Paint.Style.FILL);
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeJoin(Paint.Join.ROUND);
    }

    public void setMode(int mode) {
        this.mode = mode;
        invalidate();
    }

    public void setColors(int purple, int yellow) {
        this.purple = purple;
        this.yellow = yellow;
        invalidate();
    }

    public void setDecorAlpha(float decorAlpha) {
        this.decorAlpha = Math.max(0f, Math.min(1f, decorAlpha));
        invalidate();
    }

    public void setStrokeWidth(float strokeWidth) {
        this.strokeWidth = Math.max(1f, strokeWidth);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int width = getWidth();
        int height = getHeight();
        if (width <= 0 || height <= 0) {
            return;
        }
        strokePaint.setStrokeWidth(strokeWidth);
        int alpha = Math.round(255f * decorAlpha);
        fillPaint.setAlpha(alpha);
        strokePaint.setAlpha(alpha);
        if (mode == HEADER) {
            drawTriangle(canvas, width * 0.64f, height * 0.24f, width * 0.24f, -18f, purple, false);
            drawTriangle(canvas, width * 0.82f, height * 0.58f, width * 0.28f, 18f, yellow, true);
            drawTriangle(canvas, width * 0.42f, height * 0.68f, width * 0.18f, -42f, purple, true);
        } else if (mode == COVER) {
            drawTriangle(canvas, width * 0.22f, height * 0.24f, width * 0.32f, -16f, purple, true);
            drawTriangle(canvas, width * 0.68f, height * 0.36f, width * 0.34f, 26f, yellow, false);
            drawTriangle(canvas, width * 0.44f, height * 0.72f, width * 0.44f, -36f, purple, false);
            drawTriangle(canvas, width * 0.82f, height * 0.80f, width * 0.24f, 12f, yellow, true);
        } else if (mode == MINI) {
            drawTriangle(canvas, width * 0.30f, height * 0.35f, width * 0.30f, -25f, purple, true);
            drawTriangle(canvas, width * 0.70f, height * 0.62f, width * 0.26f, 20f, yellow, false);
        } else if (mode == EMPTY_STATE) {
            drawTriangle(canvas, width * 0.50f, height * 0.26f, width * 0.36f, 0f, purple, false);
            drawTriangle(canvas, width * 0.30f, height * 0.62f, width * 0.34f, -30f, yellow, true);
            drawTriangle(canvas, width * 0.70f, height * 0.70f, width * 0.30f, 32f, purple, true);
            drawTriangle(canvas, width * 0.54f, height * 0.52f, width * 0.22f, -8f, yellow, false);
        } else {
            float mirror = mode == CORNER_RIGHT ? -1f : 1f;
            canvas.save();
            if (mirror < 0f) {
                canvas.scale(-1f, 1f, width / 2f, height / 2f);
            }
            drawTriangle(canvas, width * 0.02f, height * 0.92f, width * 0.52f, -20f, purple, false);
            drawTriangle(canvas, width * 0.34f, height * 0.72f, width * 0.28f, 28f, yellow, true);
            drawTriangle(canvas, width * 0.22f, height * 0.42f, width * 0.22f, -36f, purple, true);
            canvas.restore();
        }
    }

    private void drawTriangle(Canvas canvas, float cx, float cy, float size, float rotation, int color, boolean filled) {
        path.reset();
        double radians = Math.toRadians(rotation - 90f);
        for (int i = 0; i < 3; i++) {
            double angle = radians + (Math.PI * 2d * i / 3d);
            float x = cx + (float) Math.cos(angle) * size * 0.58f;
            float y = cy + (float) Math.sin(angle) * size * 0.58f;
            if (i == 0) {
                path.moveTo(x, y);
            } else {
                path.lineTo(x, y);
            }
        }
        path.close();
        if (filled) {
            fillPaint.setColor(color);
            canvas.drawPath(path, fillPaint);
        } else {
            strokePaint.setColor(color);
            canvas.drawPath(path, strokePaint);
        }
    }
}
