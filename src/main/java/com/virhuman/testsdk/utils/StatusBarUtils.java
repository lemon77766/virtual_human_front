package com.virhuman.testsdk.utils;

import android.app.Activity;
import android.graphics.Color;
import android.os.Build;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

/**
 * 状态栏工具类
 */
public class StatusBarUtils {
    
    /**
     * 设置透明状态栏
     * @param activity Activity
     * @param lightStatusBar 状态栏文字是否为浅色（true=深色文字，false=浅色文字）
     */
    public static void setTransparentStatusBar(Activity activity, boolean lightStatusBar) {
        if (activity == null) {
            return;
        }
        
        Window window = activity.getWindow();
        if (window == null) {
            return;
        }
        
        // 让内容绘制到状态栏区域
        WindowCompat.setDecorFitsSystemWindows(window, false);

        // 设置状态栏透明
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.TRANSPARENT);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }

        // 设置状态栏文字颜色
        View decorView = window.getDecorView();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int flags = decorView.getSystemUiVisibility();
            flags |= View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
            if (lightStatusBar) {
                // 深色文字（适合浅色背景）
                flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            } else {
                // 浅色文字（适合深色背景）
                flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            }
            decorView.setSystemUiVisibility(flags);
        }

        // 兼容 API 30+ 使用 WindowInsetsControllerCompat 控制文字颜色
        WindowInsetsControllerCompat windowInsetsController = WindowCompat.getInsetsController(window, decorView);
        if (windowInsetsController != null) {
            windowInsetsController.setAppearanceLightStatusBars(lightStatusBar);
        }
    }
    
    /**
     * 获取状态栏高度
     * @param activity Activity
     * @return 状态栏高度（像素）
     */
    public static int getStatusBarHeight(Activity activity) {
        if (activity == null) {
            return 0;
        }
        
        int result = 0;
        int resourceId = activity.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = activity.getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }
}

