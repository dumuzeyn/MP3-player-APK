package com.dumuzeyn.mp3player;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;

final class ThemeIconBitmap {
    private ThemeIconBitmap() {
    }

    static Bitmap create(Context context, boolean dark, boolean custom,
            int backgroundColor, int foregroundColor, int size) {
        int safeSize = Math.max(1, size);
        Bitmap bitmap = Bitmap.createBitmap(safeSize, safeSize, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        if (custom) {
            Paint background = new Paint(Paint.ANTI_ALIAS_FLAG);
            background.setColor(backgroundColor);
            float radius = safeSize * 0.22f;
            canvas.drawRoundRect(0, 0, safeSize, safeSize, radius, radius, background);
            Drawable note = context.getResources().getDrawable(R.drawable.ic_notification_music);
            note.mutate().setColorFilter(new PorterDuffColorFilter(
                    foregroundColor, PorterDuff.Mode.SRC_IN));
            int inset = Math.round(safeSize * 0.22f);
            note.setBounds(inset, inset, safeSize - inset, safeSize - inset);
            note.draw(canvas);
            return bitmap;
        }
        Drawable launcher = context.getResources().getDrawable(
                dark ? R.mipmap.ic_launcher_dark : R.mipmap.ic_launcher_home);
        launcher.setBounds(0, 0, safeSize, safeSize);
        launcher.draw(canvas);
        return bitmap;
    }
}
