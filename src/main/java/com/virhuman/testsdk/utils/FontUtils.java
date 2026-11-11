package com.virhuman.testsdk.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;

/**
 * 简单的字体缩放工具：保存用户选择并更新应用配置的 fontScale。
 */
public final class FontUtils {
    private static final String PREFS = "app_prefs";
    private static final String KEY_FONT_SCALE = "font_scale";

    private FontUtils() {}

    public static void setFontScale(Context context, float scale) {
        if (context == null) return;
        SharedPreferences sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        sp.edit().putFloat(KEY_FONT_SCALE, scale).apply();

        applyFontScale(context, scale);
    }

    public static float getSavedFontScale(Context context) {
        SharedPreferences sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return sp.getFloat(KEY_FONT_SCALE, 1.0f);
    }

    public static void applyFontScale(Context context) {
        float scale = getSavedFontScale(context);
        applyFontScale(context, scale);
    }

    public static void applyFontScale(Context context, float scale) {
        try {
            Configuration configuration = context.getResources().getConfiguration();
            if (configuration.fontScale == scale) return;
            configuration = new Configuration(configuration);
            configuration.fontScale = scale;
            context.getResources().updateConfiguration(configuration, context.getResources().getDisplayMetrics());
        } catch (Exception ignored) {}
    }
}


