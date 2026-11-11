package com.virhuman.testsdk.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.virhuman.testsdk.R;

public class AboutActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        ImageButton btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());
        // set version dynamically using PackageManager to avoid BuildConfig issues
        TextView ver = findViewById(R.id.app_version);
        if (ver != null) {
            try {
                String version = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
                ver.setText("Version " + (version != null ? version : "1.0"));
            } catch (Exception e) {
                ver.setText("Version 1.0");
            }
        }
    }
}





