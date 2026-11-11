package com.virhuman.testsdk.utils;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * 登录态存储
 */
public final class AuthStore {
    private static final String SP_NAME = "auth_store";
    private static final String KEY_LOGGED_IN = "logged_in";
    private static final String KEY_TOKEN = "token";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_ACCOUNT = "account";
    private static final String KEY_USERNAME = "username";
    // 是否已经看过引导页
    private static final String KEY_ONBOARDING_SHOWN = "onboarding_shown";

    private AuthStore() {}

    public static boolean isLoggedIn(Context context) {
        SharedPreferences sp = sp(context);
        // 兼容旧逻辑：如果 token 存在就认为已登录
        String token = sp.getString(KEY_TOKEN, null);
        if (token != null && !token.trim().isEmpty()) return true;
        return sp.getBoolean(KEY_LOGGED_IN, false);
    }

    public static void setLoggedIn(Context context, boolean loggedIn) {
        sp(context).edit().putBoolean(KEY_LOGGED_IN, loggedIn).apply();
    }

    /**
     * 是否已经展示过引导页
     */
    public static boolean isOnboardingShown(Context context) {
        return sp(context).getBoolean(KEY_ONBOARDING_SHOWN, false);
    }

    /**
     * 记录引导页已展示
     */
    public static void setOnboardingShown(Context context, boolean shown) {
        sp(context).edit().putBoolean(KEY_ONBOARDING_SHOWN, shown).apply();
    }

    public static void saveAuth(Context context, String token, int userId, String account, String username) {
        sp(context).edit()
                .putBoolean(KEY_LOGGED_IN, true)
                .putString(KEY_TOKEN, token)
                .putInt(KEY_USER_ID, userId)
                .putString(KEY_ACCOUNT, account)
                .putString(KEY_USERNAME, username)
                .apply();
    }

    public static void clear(Context context) {
        sp(context).edit().clear().apply();
    }

    public static String getToken(Context context) {
        return sp(context).getString(KEY_TOKEN, null);
    }

    public static int getUserId(Context context) {
        return sp(context).getInt(KEY_USER_ID, 0);
    }

    public static String getAccount(Context context) {
        return sp(context).getString(KEY_ACCOUNT, null);
    }

    public static String getUsername(Context context) {
        return sp(context).getString(KEY_USERNAME, null);
    }

    private static SharedPreferences sp(Context context) {
        return context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
    }
}


