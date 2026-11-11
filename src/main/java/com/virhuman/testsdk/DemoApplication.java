package com.virhuman.testsdk;

import android.app.Activity;
import android.app.Application;
import android.util.Log;
import android.os.Bundle;

import com.iflytek.avalibrary.AvatarPlatform;
import com.virhuman.testsdk.config.AvatarConfig;
import com.virhuman.testsdk.utils.StatusBarUtils;


public class DemoApplication extends Application {
    private static final String TAG = "DemoApplication";
    private static DemoApplication instance;
    
    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        
        // 初始化全局配置
        initGlobalConfig();
        
        // 设置全局异常处理
        setupGlobalExceptionHandler();

        // 设置全局透明状态栏
        setupTransparentStatusBar();
        
        Log.d(TAG, "应用程序启动完成");
    }
    
    /**
     * 获取应用实例
     * @return 应用实例
     */
    public static DemoApplication getInstance() {
        return instance;
    }
    
    /**
     * 初始化全局配置
     */
    private void initGlobalConfig() {
        try {
            // 可以在这里设置一些全局默认配置
            // 应用保存的字体缩放
            com.virhuman.testsdk.utils.FontUtils.applyFontScale(this);
            Log.d(TAG, "全局配置初始化完成");
        } catch (Exception e) {
            Log.e(TAG, "全局配置初始化失败", e);
        }
    }
    
    /**
     * 设置全局异常处理
     */
    private void setupGlobalExceptionHandler() {
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable ex) {
                Log.e(TAG, "应用程序崩溃", ex);

                // 这里原本尝试调用 AvatarPlatform.destroy() 释放 SDK 资源
                // 但当前 SDK 版本未提供该静态方法，为避免编译错误，移除直接调用，
                // 仅记录日志并交由默认异常处理器处理。

                // 调用默认异常处理器
                Thread.getDefaultUncaughtExceptionHandler().uncaughtException(thread, ex);
            }
        });
    }

    /**
     * 全局透明状态栏
     */
    private void setupTransparentStatusBar() {
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
                // 默认使用浅色文字（深色文字），如需深色背景可在具体页面覆盖
                StatusBarUtils.setTransparentStatusBar(activity, true);
            }
            @Override public void onActivityStarted(Activity activity) { }
            @Override public void onActivityResumed(Activity activity) { }
            @Override public void onActivityPaused(Activity activity) { }
            @Override public void onActivityStopped(Activity activity) { }
            @Override public void onActivitySaveInstanceState(Activity activity, Bundle outState) { }
            @Override public void onActivityDestroyed(Activity activity) { }
        });
    }
    
    @Override
    public void onTerminate() {
        super.onTerminate();
        
        // 应用程序终止时理论上可以在此处释放 SDK 资源。
        // 由于当前 SDK 未提供静态 destroy/release 方法，这里仅打印日志占位。
        Log.d(TAG, "onTerminate 调用，建议在具体 Activity 中显式停止虚拟人和播放器。");
    }
    
    @Override
    public void onLowMemory() {
        super.onLowMemory();
        Log.w(TAG, "系统内存不足");
    }
    
    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        Log.d(TAG, "系统要求释放内存，级别: " + level);
    }
}


