package com.virhuman.testsdk.ui;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.Switch;

import androidx.appcompat.app.AppCompatActivity;

import com.virhuman.testsdk.R;

public class MessageSettingsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message_settings);

        ImageButton btnBack = findViewById(R.id.btn_back_message);
        btnBack.setOnClickListener(v -> finish());

        // Placeholder: additional message settings wiring can be added here
    }
}










