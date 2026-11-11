package com.virhuman.testsdk.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import com.virhuman.testsdk.R;

public class PrivacySettingsActivity extends AppCompatActivity {
    private static final String PREFS_NAME = "privacy_prefs";
    private static final String KEY_PERSONALIZATION = "personalization_enabled";

    private SwitchCompat swPersonalization;
    private ImageButton btnBack;
    private android.view.View rowMessageSettings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_privacy_settings);

        swPersonalization = findViewById(R.id.sw_personalization);
        btnBack = findViewById(R.id.btn_back_privacy);

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean enabled = prefs.getBoolean(KEY_PERSONALIZATION, false);
        swPersonalization.setChecked(enabled);

        swPersonalization.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean(KEY_PERSONALIZATION, isChecked).apply();
            Toast.makeText(PrivacySettingsActivity.this,
                    isChecked ? "已启用个性化设置" : "已关闭个性化设置",
                    Toast.LENGTH_SHORT).show();
        });

        btnBack.setOnClickListener(v -> finish());

        rowMessageSettings = findViewById(R.id.row_message_settings);
        if (rowMessageSettings != null) {
            rowMessageSettings.setOnClickListener(v -> {
                try {
                    startActivity(new android.content.Intent(PrivacySettingsActivity.this, MessageSettingsActivity.class));
                } catch (Exception e) {
                    // ignore
                }
            });
        }
    }
}


