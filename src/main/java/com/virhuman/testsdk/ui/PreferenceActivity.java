package com.virhuman.testsdk.ui;

import android.os.Bundle;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.virhuman.testsdk.R;

/**
 * 倾向切换页（占位UI）：后续根据文档把这些偏好映射到虚拟人参数/对话策略
 */
public class PreferenceActivity extends AppCompatActivity {

    private TextView chipStyleWarm;
    private TextView chipStyleLively;
    private TextView chipStyleProfessional;

    private Switch switchProactive;
    private Switch switchHealthRemind;
    private SeekBar sbSpeed;
    private TextView tvSpeedValue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preference);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        chipStyleWarm = findViewById(R.id.chip_style_warm);
        chipStyleLively = findViewById(R.id.chip_style_lively);
        chipStyleProfessional = findViewById(R.id.chip_style_professional);

        switchProactive = findViewById(R.id.switch_proactive);
        switchHealthRemind = findViewById(R.id.switch_health_remind);
        sbSpeed = findViewById(R.id.sb_speed);
        tvSpeedValue = findViewById(R.id.tv_speed_value);

        Button btnSave = findViewById(R.id.btn_save);

        // 默认值（占位）
        setStyleSelected(chipStyleWarm);
        sbSpeed.setProgress(50);
        tvSpeedValue.setText("中等");

        chipStyleWarm.setOnClickListener(v -> setStyleSelected(chipStyleWarm));
        chipStyleLively.setOnClickListener(v -> setStyleSelected(chipStyleLively));
        chipStyleProfessional.setOnClickListener(v -> setStyleSelected(chipStyleProfessional));

        sbSpeed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvSpeedValue.setText(speedLabel(progress));
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        btnSave.setOnClickListener(v -> {
            // 占位：仅提示，后续接入真实保存/下发
            Toast.makeText(this, "已保存（占位）", Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    private void setStyleSelected(TextView selected) {
        chipStyleWarm.setSelected(selected == chipStyleWarm);
        chipStyleLively.setSelected(selected == chipStyleLively);
        chipStyleProfessional.setSelected(selected == chipStyleProfessional);
    }

    private String speedLabel(int progress) {
        if (progress < 33) return "慢";
        if (progress < 67) return "中等";
        return "快";
    }
}


