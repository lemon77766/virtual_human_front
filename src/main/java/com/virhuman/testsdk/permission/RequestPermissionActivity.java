package com.virhuman.testsdk.permission;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.virhuman.testsdk.utils.PermissionUtils;

/**
 * 权限请求Activity - 使用工具类优化
 */
public class RequestPermissionActivity extends AppCompatActivity {
    private static final String TAG = "RequestPermissionActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestPermissions();
    }

    private void requestPermissions() {
        Log.d(TAG, "开始检查权限");
        
        if (PermissionUtils.hasRequiredPermissions(this) && 
            PermissionUtils.hasStoragePermissions(this)) {
            Log.d(TAG, "所有权限已授予");
            finish();
            return;
        }

        // 请求所有缺失的权限
        PermissionUtils.requestAllPermissions(this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, 
                                         @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == PermissionUtils.PERMISSION_REQUEST_CODE) {
            if (PermissionUtils.isPermissionGranted(grantResults)) {
                Toast.makeText(this, "权限授予成功", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "权限授予成功");
            } else {
                Toast.makeText(this, "部分权限被拒绝，应用功能可能受限", Toast.LENGTH_LONG).show();
                Log.w(TAG, "权限被拒绝");
                
                // 显示权限说明
                showPermissionExplanation();
            }
            finish();
        }
    }
    
    /**
     * 显示权限说明
     */
    private void showPermissionExplanation() {
        // 可以在这里添加更详细的权限说明界面
        // 或者跳转到应用设置页面
        Log.i(TAG, PermissionUtils.getPermissionDescription());
    }
}
