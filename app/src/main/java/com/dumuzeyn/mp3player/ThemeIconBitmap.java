package com.dumuzeyn.mp3player;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;

/** Builds the Voltune mark with the same two accents used by the active theme. */
final class ThemeIconBitmap {
    private static final int SOURCE_PRIMARY = 0xff8a2cc8;
    private static final int SOURCE_SECONDARY = 0xffffd000;

    private ThemeIconBitmap() {
    }

    static Bitmap createLogo(Context context, int primaryColor, int secondaryColor, int size) {
        int safeSize = Math.max(1, size);
        Bitmap bitmap = Bitmap.createBitmap(safeSize, safeSize, Bitmap.Config.ARGB_8888);
        Drawable logo = context.getResources().getDrawable(R.drawable.ic_music_vector_user);
        logo.setBounds(0, 0, safeSize, safeSize);
        logo.draw(new Canvas(bitmap));
        replaceAccentColors(bitmap, primaryColor, secondaryColor);
        return bitmap;
    }

    static Bitmap createTile(Context context, int backgroundColor,
            int primaryColor, int secondaryColor, int size) {
        int safeSize = Math.max(1, size);
        Bitmap bitmap = Bitmap.createBitmap(safeSize, safeSize, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint background = new Paint(Paint.ANTI_ALIAS_FLAG);
        background.setColor(backgroundColor);
        float radius = safeSize * 0.22f;
        canvas.drawRoundRect(0, 0, safeSize, safeSize, radius, radius, background);

        int inset = Math.round(safeSize * 0.08f);
        Bitmap logo = createLogo(context, primaryColor, secondaryColor, safeSize - inset * 2);
        canvas.drawBitmap(logo, inset, inset, null);
        logo.recycle();
        return bitmap;
    }

    private static void replaceAccentColors(Bitmap bitmap, int primaryColor, int secondaryColor) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int[] pixels = new int[width * height];
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
        for (int index = 0; index < pixels.length; index++) {
            int pixel = pixels[index];
            int alpha = Color.alpha(pixel);
            if (alpha == 0) {
                continue;
            }
            int replacement = colorDistance(pixel, SOURCE_SECONDARY)
                    < colorDistance(pixel, SOURCE_PRIMARY) ? secondaryColor : primaryColor;
            pixels[index] = Color.argb(alpha, Color.red(replacement),
                    Color.green(replacement), Color.blue(replacement));
        }
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
    }

    private static int colorDistance(int first, int second) {
        int red = Color.red(first) - Color.red(second);
        int green = Color.green(first) - Color.green(second);
        int blue = Color.blue(first) - Color.blue(second);
        return red * red + green * green + blue * blue;
    }
}
