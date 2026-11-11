package com.virhuman.testsdk.ui;

import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.virhuman.testsdk.R;

public class PrivacyPolicyActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_privacy_policy);

        ImageButton btnBack = findViewById(R.id.btn_back);
        TextView btnAck = findViewById(R.id.btn_ack);

        // body textviews to set formatted content
        TextView section1Body = findViewById(R.id.section1_body);
        TextView section2Body = findViewById(R.id.section2_body);
        TextView section3Body = findViewById(R.id.section3_body);
        TextView section4Body = findViewById(R.id.section4_body);
        TextView section5Body = findViewById(R.id.section5_body);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        if (btnAck != null) {
            btnAck.setOnClickListener(v -> finish());
        }

        if (section1Body != null) {
            String s1 = "我们非常重视您的隐私保护。<b>“陪伴智慧虚拟人”</b>承诺严格遵守法律法规，保护您的个人信息安全。本政策将向您说明我们如何收集、使用和存储您的信息。";
            section1Body.setText(Html.fromHtml(s1, Html.FROM_HTML_MODE_LEGACY));
        }
        if (section2Body != null) {
            String s2 = "为了提供服务，我们可能会收集：<br/><br/>" +
                    "&#8226; <font color=\"#FF5A5F\"><b>基本信息：</b></font> 手机号（用于注册）、昵称、头像。<br/>" +
                    "&#8226; <font color=\"#FF5A5F\"><b>健康数据：</b></font> 您主动录入的血压、心率、步数（用于生成健康报告）。<br/>" +
                    "&#8226; <font color=\"#FF5A5F\"><b>语音数据：</b></font> 您与虚拟人对话时的语音（仅用于语义识别，不做他用）。";
            section2Body.setText(Html.fromHtml(s2, Html.FROM_HTML_MODE_LEGACY));
        }
        if (section3Body != null) {
            String s3 = "麦克风：用于语音聊天和发布语音动态。<br/>" +
                    "相册/相机：用于修改头像或发布图片动态。<br/>" +
                    "定位：用于获取当地天气信息。您可以在系统设置中随时关闭这些权限。";
            section3Body.setText(Html.fromHtml(s3, Html.FROM_HTML_MODE_LEGACY));
        }
        if (section4Body != null) {
            String s4 = "您的信息仅用于提供和改进本应用的服务。我们绝不会向任何第三方出售您的个人信息。我们会采取合理的安全措施保护您的信息安全。";
            section4Body.setText(Html.fromHtml(s4, Html.FROM_HTML_MODE_LEGACY));
        }
        if (section5Body != null) {
            String s5 = "如您对隐私保护有任何疑问，请通过“我的 - 关于我们”中的联系方式与我们联系。";
            section5Body.setText(Html.fromHtml(s5, Html.FROM_HTML_MODE_LEGACY));
        }
    }
}



