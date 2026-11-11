package com.virhuman.testsdk.ui;

import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.virhuman.testsdk.R;

public class UserAgreementActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_agreement);

        ImageButton btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());

        Button btnAgree = findViewById(R.id.btn_agree);
        btnAgree.setOnClickListener(v -> {
            // return ok to caller
            setResult(RESULT_OK);
            finish();
        });
    }
}


