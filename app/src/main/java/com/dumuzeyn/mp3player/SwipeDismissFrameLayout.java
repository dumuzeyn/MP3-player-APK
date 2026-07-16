package com.dumuzeyn.mp3player;

import android.content.Context;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.SeekBar;

/** Root for modal panels that can be dismissed with a deliberate horizontal swipe. */
final class SwipeDismissFrameLayout extends FrameLayout {
    private static final float DIRECTION_BIAS = 1.35f;

    private final int dismissThreshold;
    private float downX;
    private float downY;
    private boolean dismissing;
    private boolean protectedGesture;
    private Runnable dismissAction;

    SwipeDismissFrameLayout(Context context) {
        super(context);
        dismissThreshold = Math.round(48.0f * getResources().getDisplayMetrics().density);
        setClickable(true);
    }

    void setDismissAction(Runnable action) {
        dismissAction = action;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN) {
            downX = event.getRawX();
            downY = event.getRawY();
            dismissing = false;
            protectedGesture = isProtectedControlTouched(this, downX, downY);
            View target = dismissTarget();
            if (target != null) {
                target.animate().cancel();
            }
            return false;
        }
        if (action == MotionEvent.ACTION_MOVE && !protectedGesture) {
            float dx = event.getRawX() - downX;
            float dy = event.getRawY() - downY;
            if (Math.abs(dx) >= dismissThreshold
                    && Math.abs(dx) > Math.abs(dy) * DIRECTION_BIAS) {
                dismissing = true;
                return true;
            }
        }
        return super.onInterceptTouchEvent(event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!dismissing) {
            return super.onTouchEvent(event);
        }
        float dx = event.getRawX() - downX;
        if (event.getActionMasked() == MotionEvent.ACTION_MOVE) {
            View target = dismissTarget();
            if (target != null) {
                target.setTranslationX(dx);
                target.setAlpha(Math.max(0.45f,
                        1.0f - Math.abs(dx) / Math.max(1.0f, target.getWidth())));
            }
            return true;
        }
        if (event.getActionMasked() == MotionEvent.ACTION_UP) {
            dismiss(dx);
            return true;
        }
        if (event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
            resetPosition();
            return true;
        }
        return true;
    }

    private void dismiss(float dx) {
        float direction = dx < 0.0f ? -1.0f : 1.0f;
        Runnable action = dismissAction;
        if (action == null) {
            resetPosition();
            return;
        }
        View target = dismissTarget();
        if (target == null) {
            action.run();
            return;
        }
        target.animate()
                .translationX(direction * Math.max(getWidth(), dismissThreshold * 2))
                .alpha(0.0f)
                .setDuration(120L)
                .withEndAction(action)
                .start();
    }

    private void resetPosition() {
        dismissing = false;
        View target = dismissTarget();
        if (target != null) {
            target.animate().translationX(0.0f).alpha(1.0f).setDuration(100L).start();
        }
    }

    private View dismissTarget() {
        for (int index = getChildCount() - 1; index >= 0; index--) {
            View child = getChildAt(index);
            if (child.getVisibility() == View.VISIBLE) {
                return child;
            }
        }
        return null;
    }

    private boolean isProtectedControlTouched(View view, float rawX, float rawY) {
        if (view.getVisibility() != View.VISIBLE || !isInside(view, rawX, rawY)) {
            return false;
        }
        if (view instanceof SeekBar || view instanceof ThemeColorWheelView) {
            return true;
        }
        if (!(view instanceof ViewGroup)) {
            return false;
        }
        ViewGroup group = (ViewGroup) view;
        for (int index = group.getChildCount() - 1; index >= 0; index--) {
            if (isProtectedControlTouched(group.getChildAt(index), rawX, rawY)) {
                return true;
            }
        }
        return false;
    }

    private boolean isInside(View view, float rawX, float rawY) {
        int[] location = new int[2];
        view.getLocationOnScreen(location);
        return rawX >= location[0] && rawX <= location[0] + view.getWidth()
                && rawY >= location[1] && rawY <= location[1] + view.getHeight();
    }
}
