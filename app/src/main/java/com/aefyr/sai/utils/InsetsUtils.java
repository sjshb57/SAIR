package com.aefyr.sai.utils;

import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * Insets a view out from under the system bars, which every screen has to do itself once the app
 * targets API 35 or above.
 * <p>
 * The padding variants remember the padding the view was laid out with and add the inset on top of
 * it, so they stay correct when insets are dispatched again on rotation or keyboard changes.
 */
public class InsetsUtils {

    private InsetsUtils() {
    }

    public static void applySystemBarInsetsAsPadding(@NonNull View view) {
        apply(view, true, true, true, true);
    }

    /**
     * Only safe on views that honour padding. Custom ViewGroups laying children out from a fixed
     * origin (Coolbar) and CardView (whose setPadding is a no-op) need
     * {@link #applyTopInsetAsMargin} instead.
     */
    public static void applyTopInsetAsPadding(@NonNull View view) {
        apply(view, false, true, false, false);
    }

    public static void applyBottomInsetAsPadding(@NonNull View view) {
        apply(view, false, false, false, true);
    }

    public static void applyBottomInsetAndKeyboardAsPadding(@NonNull View view) {
        final int left = view.getPaddingLeft();
        final int top = view.getPaddingTop();
        final int right = view.getPaddingRight();
        final int bottom = view.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(view, (v, windowInsets) -> {
            Insets bars = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            Insets keyboard = windowInsets.getInsets(WindowInsetsCompat.Type.ime());

            v.setPadding(left, top, right, bottom + Math.max(bars.bottom, keyboard.bottom));
            return windowInsets;
        });
        requestInsets(view);
    }

    public static void applyTopInsetAsMargin(@NonNull View view) {
        applyInsetAsMargin(view, true);
    }

    public static void applyBottomInsetAsMargin(@NonNull View view) {
        applyInsetAsMargin(view, false);
    }

    private static void applyInsetAsMargin(@NonNull View view, boolean top) {
        if (!(view.getLayoutParams() instanceof ViewGroup.MarginLayoutParams))
            return;

        ViewGroup.MarginLayoutParams initial = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
        final int margin = top ? initial.topMargin : initial.bottomMargin;

        ViewCompat.setOnApplyWindowInsetsListener(view, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());

            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
            if (top)
                params.topMargin = margin + insets.top;
            else
                params.bottomMargin = margin + insets.bottom;
            v.setLayoutParams(params);
            return windowInsets;
        });
        requestInsets(view);
    }

    private static void apply(@NonNull View view, boolean start, boolean top, boolean end, boolean bottom) {
        final int paddingLeft = view.getPaddingLeft();
        final int paddingTop = view.getPaddingTop();
        final int paddingRight = view.getPaddingRight();
        final int paddingBottom = view.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(view, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());

            v.setPadding(
                    paddingLeft + (start ? insets.left : 0),
                    paddingTop + (top ? insets.top : 0),
                    paddingRight + (end ? insets.right : 0),
                    paddingBottom + (bottom ? insets.bottom : 0));
            return windowInsets;
        });
        requestInsets(view);
    }

    /** A listener attached after the first dispatch would not run until something triggers another. */
    private static void requestInsets(@NonNull View view) {
        if (view.isAttachedToWindow()) {
            ViewCompat.requestApplyInsets(view);
            return;
        }

        view.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(@NonNull View v) {
                v.removeOnAttachStateChangeListener(this);
                ViewCompat.requestApplyInsets(v);
            }

            @Override
            public void onViewDetachedFromWindow(@NonNull View v) {
                v.removeOnAttachStateChangeListener(this);
            }
        });
    }
}
