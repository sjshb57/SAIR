package com.github.angads25.filepicker.utils;

import android.content.Context;
import android.util.TypedValue;

public class ColorUtils {

    public static int getAccentColor(Context context) {
        int color;
        TypedValue accentColor = new TypedValue();
        context.getTheme().resolveAttribute(android.R.attr.colorAccent, accentColor, true);
        color = accentColor.data;

        return color;
    }

}
