package com.virhuman.testsdk.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

/**
 * 权限管理工具类
 * 处理虚拟人SDK所需的各种权限
 */
public class PermissionUtils {
    
    // 权限请求码
    public static final int PERMISSION_REQUEST_CODE = 1001;
    
    // 必需权限
    public static final String[] REQUIRED_PERMISSIONS = {
        Manifest.permission.INTERNET,
        Manifest.permission.ACCESS_NETWORK_STATE,
        Manifest.permission.ACCESS_WIFI_STATE,
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.MODIFY_AUDIO_SETTINGS
    };
    
    // 存储权限（Android 11以下）
    public static final String[] STORAGE_PERMISSIONS = {
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    
    // 蓝牙权限（可选）
    public static final String[] BLUETOOTH_PERMISSIONS = {
        Manifest.permission.BLUETOOTH,
        Manifest.permission.BLUETOOTH_ADMIN
    };
    
    /**
     * 检查是否有所有必需权限
     * @param context 上下文
     * @return 是否有所有必需权限
     */
    public static boolean hasRequiredPermissions(Context context) {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * 检查是否有存储权限
     * @param context 上下文
     * @return 是否有存储权限
     */
    public static boolean hasStoragePermissions(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11及以上不需要存储权限
            return true;
        }
        
        for (String permission : STORAGE_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * 检查是否有蓝牙权限
     * @param context 上下文
     * @return 是否有蓝牙权限
     */
    public static boolean hasBluetoothPermissions(Context context) {
        for (String permission : BLUETOOTH_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * 获取缺失的必需权限
     * @param context 上下文
     * @return 缺失的权限列表
     */
    public static String[] getMissingRequiredPermissions(Context context) {
        List<String> missingPermissions = new ArrayList<>();
        
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(permission);
            }
        }
        
        return missingPermissions.toArray(new String[0]);
    }
    
    /**
     * 获取所有缺失的权限
     * @param context 上下文
     * @return 缺失的权限列表
     */
    public static String[] getAllMissingPermissions(Context context) {
        List<String> missingPermissions = new ArrayList<>();
        
        // 检查必需权限
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(permission);
            }
        }
        
        // 检查存储权限（Android 11以下）
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            for (String permission : STORAGE_PERMISSIONS) {
                if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    missingPermissions.add(permission);
                }
            }
        }
        
        // 检查蓝牙权限
        for (String permission : BLUETOOTH_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(permission);
            }
        }
        
        return missingPermissions.toArray(new String[0]);
    }
    
    /**
     * 请求必需权限
     * @param activity Activity
     */
    public static void requestRequiredPermissions(Activity activity) {
        String[] missingPermissions = getMissingRequiredPermissions(activity);
        if (missingPermissions.length > 0) {
            ActivityCompat.requestPermissions(activity, missingPermissions, PERMISSION_REQUEST_CODE);
        }
    }
    
    /**
     * 请求所有权限
     * @param activity Activity
     */
    public static void requestAllPermissions(Activity activity) {
        String[] missingPermissions = getAllMissingPermissions(activity);
        if (missingPermissions.length > 0) {
            ActivityCompat.requestPermissions(activity, missingPermissions, PERMISSION_REQUEST_CODE);
        }
    }
    
    /**
     * 检查权限请求结果
     * @param grantResults 权限请求结果
     * @return 是否所有权限都被授予
     */
    public static boolean isPermissionGranted(int[] grantResults) {
        if (grantResults.length == 0) {
            return false;
        }
        
        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * 获取权限的友好名称
     * @param permission 权限名称
     * @return 友好名称
     */
    public static String getPermissionFriendlyName(String permission) {
        switch (permission) {
            case Manifest.permission.INTERNET:
                return "网络访问";
            case Manifest.permission.ACCESS_NETWORK_STATE:
                return "网络状态检查";
            case Manifest.permission.ACCESS_WIFI_STATE:
                return "WiFi状态检查";
            case Manifest.permission.RECORD_AUDIO:
                return "录音";
            case Manifest.permission.MODIFY_AUDIO_SETTINGS:
                return "音频设置修改";
            case Manifest.permission.READ_EXTERNAL_STORAGE:
                return "读取外部存储";
            case Manifest.permission.WRITE_EXTERNAL_STORAGE:
                return "写入外部存储";
            case Manifest.permission.BLUETOOTH:
                return "蓝牙";
            case Manifest.permission.BLUETOOTH_ADMIN:
                return "蓝牙管理";
            default:
                return permission;
        }
    }
    
    /**
     * 获取权限说明文本
     * @return 权限说明
     */
    public static String getPermissionDescription() {
        return "虚拟人SDK需要以下权限：\n" +
               "• 网络访问：连接服务器\n" +
               "• 录音：实现语音交互\n" +
               "• 音频设置：配置音频参数\n" +
               "• 存储：缓存资源文件\n" +
               "• 蓝牙：连接蓝牙设备（可选）";
    }
}