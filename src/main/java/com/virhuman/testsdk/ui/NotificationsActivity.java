package com.virhuman.testsdk.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.virhuman.testsdk.R;

/**
 * 消息通知页面
 */
public class NotificationsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        // 模拟数据（前端显示用，后续接入后端替换）
        android.widget.LinearLayout container = findViewById(getResources().getIdentifier("notifications_container", "id", getPackageName()));
        if (container != null) {
            addNotificationItem(container, R.drawable.laoren2, "李奶奶", "赞了你的动态", "10分钟前", "今天公园里的花开得真好，心情舒畅...");
            addNotificationItem(container, R.drawable.laoren3, "王叔叔", "评论了你", "2小时前", "拍得真不错！改天我也去看看。");
            addNotificationItem(container, R.drawable.background1, "赵阿姨", "收藏了你的动态", "5小时前", "新学了一段京剧《空城计》，大伙...");
            addSystemNotice(container, "欢迎加入悦享生活社区！快来发布你的第一条动态吧。", "昨天 09:00");
        }
    }

    private void setupThankButton(int containerId, int imgId, int txtId) {
        // left for compatibility but not used in simulated layout
    }

    private void addNotificationItem(android.widget.LinearLayout parent, int avatarRes, String name, String actionText, String time, String quote) {
        android.widget.LinearLayout item = new android.widget.LinearLayout(this);
        item.setLayoutParams(new android.widget.LinearLayout.LayoutParams(android.widget.LinearLayout.LayoutParams.MATCH_PARENT, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT));
        item.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        int margin = (int) (12 * getResources().getDisplayMetrics().density + 0.5f);
        ((android.widget.LinearLayout.LayoutParams)item.getLayoutParams()).setMargins(margin, margin, margin, 0);
        item.setPadding(margin, margin, margin, margin);
        item.setBackgroundColor(getResources().getColor(android.R.color.white));

        ImageView avatar = new ImageView(this);
        android.widget.LinearLayout.LayoutParams avatarLp = new android.widget.LinearLayout.LayoutParams((int)(48 * getResources().getDisplayMetrics().density), (int)(48 * getResources().getDisplayMetrics().density));
        avatarLp.setMargins(0, 0, (int)(12 * getResources().getDisplayMetrics().density), 0);
        avatar.setLayoutParams(avatarLp);
        avatar.setScaleType(ImageView.ScaleType.CENTER_CROP);
        avatar.setImageResource(avatarRes);
        item.addView(avatar);

        android.widget.LinearLayout content = new android.widget.LinearLayout(this);
        content.setOrientation(android.widget.LinearLayout.VERTICAL);
        content.setLayoutParams(new android.widget.LinearLayout.LayoutParams(android.widget.LinearLayout.LayoutParams.MATCH_PARENT, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT));

        android.widget.LinearLayout titleRow = new android.widget.LinearLayout(this);
        titleRow.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        android.widget.TextView tvName = new android.widget.TextView(this);
        tvName.setText(name);
        tvName.setTextSize(15);
        tvName.setTypeface(null, android.graphics.Typeface.BOLD);
        titleRow.addView(tvName);
        android.widget.TextView tvAction = new android.widget.TextView(this);
        tvAction.setText("  " + actionText);
        tvAction.setTextSize(13);
        tvAction.setTextColor(getResources().getColor(android.R.color.darker_gray));
        titleRow.addView(tvAction);
        content.addView(titleRow);

        android.widget.TextView tvTime = new android.widget.TextView(this);
        tvTime.setText(time);
        tvTime.setTextSize(11);
        tvTime.setTextColor(getResources().getColor(android.R.color.darker_gray));
        tvTime.setPadding(0, (int)(4 * getResources().getDisplayMetrics().density), 0, 0);
        content.addView(tvTime);

        android.widget.TextView tvQuote = new android.widget.TextView(this);
        tvQuote.setText(quote);
        tvQuote.setBackgroundColor(getResources().getColor(android.R.color.background_light));
        tvQuote.setPadding((int)(8 * getResources().getDisplayMetrics().density), (int)(8 * getResources().getDisplayMetrics().density), (int)(8 * getResources().getDisplayMetrics().density), (int)(8 * getResources().getDisplayMetrics().density));
        tvQuote.setTextColor(getResources().getColor(android.R.color.darker_gray));
        android.widget.LinearLayout.LayoutParams quoteLp = new android.widget.LinearLayout.LayoutParams(android.widget.LinearLayout.LayoutParams.MATCH_PARENT, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
        quoteLp.setMargins(0, (int)(8 * getResources().getDisplayMetrics().density), 0, 0);
        tvQuote.setLayoutParams(quoteLp);
        content.addView(tvQuote);

        item.addView(content);

        // thank button
        android.widget.LinearLayout right = new android.widget.LinearLayout(this);
        right.setOrientation(android.widget.LinearLayout.VERTICAL);
        right.setLayoutParams(new android.widget.LinearLayout.LayoutParams(android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, android.widget.LinearLayout.LayoutParams.MATCH_PARENT));
        right.setGravity(android.view.Gravity.CENTER);
        android.widget.LinearLayout btn = new android.widget.LinearLayout(this);
        btn.setOrientation(android.widget.LinearLayout.VERTICAL);
        btn.setGravity(android.view.Gravity.CENTER);
        btn.setPadding((int)(6 * getResources().getDisplayMetrics().density), (int)(6 * getResources().getDisplayMetrics().density), (int)(6 * getResources().getDisplayMetrics().density), (int)(6 * getResources().getDisplayMetrics().density));
        btn.setBackgroundColor(getResources().getColor(android.R.color.background_light));
        android.widget.ImageView img = new android.widget.ImageView(this);
        img.setImageResource(R.drawable.like);
        img.setLayoutParams(new android.widget.LinearLayout.LayoutParams((int)(20 * getResources().getDisplayMetrics().density), (int)(20 * getResources().getDisplayMetrics().density)));
        android.widget.TextView txt = new android.widget.TextView(this);
        txt.setText("感谢");
        txt.setTextSize(10);
        txt.setTextColor(getResources().getColor(android.R.color.darker_gray));
        btn.addView(img);
        btn.addView(txt);
        btn.setOnClickListener(v -> {
            boolean active = btn.getTag() != null && (boolean) btn.getTag();
            if (active) {
                btn.setTag(false);
                img.setImageResource(R.drawable.like);
                txt.setText("感谢");
            } else {
                btn.setTag(true);
                img.setImageResource(R.drawable.likecolour);
                txt.setText("已感谢");
            }
        });
        right.addView(btn);
        item.addView(right);

        parent.addView(item);
    }

    private void addSystemNotice(android.widget.LinearLayout parent, String text, String time) {
        android.widget.LinearLayout item = new android.widget.LinearLayout(this);
        item.setLayoutParams(new android.widget.LinearLayout.LayoutParams(android.widget.LinearLayout.LayoutParams.MATCH_PARENT, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT));
        item.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        int margin = (int) (12 * getResources().getDisplayMetrics().density + 0.5f);
        ((android.widget.LinearLayout.LayoutParams)item.getLayoutParams()).setMargins(margin, margin, margin, 0);
        item.setPadding(margin, margin, margin, margin);

        android.widget.FrameLayout iconBox = new android.widget.FrameLayout(this);
        iconBox.setLayoutParams(new android.widget.LinearLayout.LayoutParams((int)(48 * getResources().getDisplayMetrics().density), (int)(48 * getResources().getDisplayMetrics().density)));
        iconBox.setBackgroundColor(0xFFFF4757);
        android.widget.ImageView iv = new android.widget.ImageView(this);
        iv.setLayoutParams(new android.widget.FrameLayout.LayoutParams(android.widget.FrameLayout.LayoutParams.MATCH_PARENT, android.widget.FrameLayout.LayoutParams.MATCH_PARENT));
        iv.setImageResource(R.drawable.ic_notification_icon);
        iv.setColorFilter(0xFFFFFFFF);
        iconBox.addView(iv);
        item.addView(iconBox);

        android.widget.LinearLayout content = new android.widget.LinearLayout(this);
        content.setOrientation(android.widget.LinearLayout.VERTICAL);
        content.setLayoutParams(new android.widget.LinearLayout.LayoutParams(android.widget.LinearLayout.LayoutParams.MATCH_PARENT, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT));
        android.widget.TextView tvTitle = new android.widget.TextView(this);
        tvTitle.setText("系统通知");
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        content.addView(tvTitle);
        android.widget.TextView tvTime = new android.widget.TextView(this);
        tvTime.setText(time);
        tvTime.setTextSize(11);
        tvTime.setTextColor(getResources().getColor(android.R.color.darker_gray));
        content.addView(tvTime);
        android.widget.TextView tvText = new android.widget.TextView(this);
        tvText.setText(text);
        tvText.setTextColor(getResources().getColor(android.R.color.darker_gray));
        tvText.setPadding(0, (int)(8 * getResources().getDisplayMetrics().density), 0, 0);
        content.addView(tvText);
        item.addView(content);
        parent.addView(item);
    }
}


