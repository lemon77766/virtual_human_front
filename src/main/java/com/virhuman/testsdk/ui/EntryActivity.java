package com.virhuman.testsdk.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.virhuman.testsdk.R;
import com.virhuman.testsdk.utils.PermissionUtils;

/**
 * 入口Activity，选择云模式或端模式
 */
public class EntryActivity extends AppCompatActivity {

    private Button btCloud;
    private Button btLocal;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entry);

        initViews();
        setListeners();
        
        // 检查权限
        checkPermissions();
    }

    private void initViews() {
        btCloud = findViewById(R.id.bt_cloud);
        btLocal = findViewById(R.id.bt_local);
    }

    private void setListeners() {
        btCloud.setOnClickListener(v -> {
            // 检查权限后跳转到云模式
            if (PermissionUtils.hasRequiredPermissions(this)) {
                startActivity(new Intent(EntryActivity.this, MainActivity.class));
            } else {
                PermissionUtils.requestRequiredPermissions(this);
            }
        });

        btLocal.setOnClickListener(v -> {
            // 检查权限后跳转到端模式
            if (PermissionUtils.hasRequiredPermissions(this)) {
                startActivity(new Intent(EntryActivity.this, MainLocalActivity.class));
            } else {
                PermissionUtils.requestRequiredPermissions(this);
            }
        });
    }
    
    /**
     * 检查应用权限
     */
    private void checkPermissions() {
        if (!PermissionUtils.hasRequiredPermissions(this)) {
            Toast.makeText(this, "应用需要相关权限才能正常使用", Toast.LENGTH_SHORT).show();
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == PermissionUtils.PERMISSION_REQUEST_CODE) {
            if (PermissionUtils.isPermissionGranted(grantResults)) {
                Toast.makeText(this, "权限授予成功", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "权限被拒绝，某些功能可能无法正常使用", Toast.LENGTH_LONG).show();
            }
        }
    }
}































