package com.virhuman.testsdk.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.virhuman.testsdk.R;

import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONObject;

import android.widget.ImageView;

import java.util.ArrayList;
import java.util.List;
/**
 * 社区内的个人主页（张大爷示例）
 */
public class MyHomeActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_community_profile);

        // 优先使用当前登录账户的 userId 作为 authorId；若未登录则回退到 intent 指定或默认 1
        int authorId = 0;
        try {
            authorId = com.virhuman.testsdk.utils.AuthStore.getUserId(this);
        } catch (Throwable ignored) {}
        if (authorId == 0) {
            authorId = getIntent().getIntExtra("author_id", 1);
        }

        // 返回按钮
        findViewById(R.id.btn_back_profile).setOnClickListener(v -> finish());

        final TextView btnFollow = findViewById(R.id.btn_follow_profile);
        btnFollow.setOnClickListener(v -> {
            if (btnFollow.getText().toString().contains("关注")) {
                btnFollow.setText("已关注");
                btnFollow.setBackgroundResource(android.R.color.transparent);
                btnFollow.setTextColor(getResources().getColor(android.R.color.darker_gray));
            } else {
                btnFollow.setText("+ 关注");
                btnFollow.setBackgroundResource(R.drawable.bg_circle_primary);
                btnFollow.setTextColor(getResources().getColor(android.R.color.white));
            }
        });

        // Tabs
        final TextView tabDynamic = findViewById(R.id.tab_dynamic);
        final TextView tabAlbum = findViewById(R.id.tab_album);
        final TextView tabLikes = findViewById(R.id.tab_likes);
        final LinearLayout contentDynamic = findViewById(R.id.content_dynamic);
        final LinearLayout contentAlbum = findViewById(R.id.content_album);
        final LinearLayout contentLikes = findViewById(R.id.content_likes);

        View.OnClickListener switcher = v -> {
            tabDynamic.setTextColor(getResources().getColor(R.color.home_text_secondary));
            tabAlbum.setTextColor(getResources().getColor(R.color.home_text_secondary));
            tabLikes.setTextColor(getResources().getColor(R.color.home_text_secondary));
            contentDynamic.setVisibility(View.GONE);
            contentAlbum.setVisibility(View.GONE);
            contentLikes.setVisibility(View.GONE);

            if (v.getId() == R.id.tab_dynamic) {
                tabDynamic.setTextColor(getResources().getColor(R.color.home_primary));
                contentDynamic.setVisibility(View.VISIBLE);
            } else if (v.getId() == R.id.tab_album) {
                tabAlbum.setTextColor(getResources().getColor(R.color.home_primary));
                contentAlbum.setVisibility(View.VISIBLE);
            } else {
                tabLikes.setTextColor(getResources().getColor(R.color.home_primary));
                contentLikes.setVisibility(View.VISIBLE);
            }
        };

        tabDynamic.setOnClickListener(switcher);
        tabAlbum.setOnClickListener(switcher);
        tabLikes.setOnClickListener(switcher);

        // 请求作者帖子（需要 token）
        fetchAuthorPosts(authorId);
    }

    private void fetchAuthorPosts(int authorId) {
        String token = com.virhuman.testsdk.utils.AuthStore.getToken(this);
        if (token == null || token.trim().isEmpty()) {
            // 未登录，保留默认内容
            return;
        }

        final android.widget.LinearLayout container = findViewById(R.id.content_dynamic);
        new Thread(() -> {
            try {
                Request req = new Request.Builder()
                        .url(com.virhuman.testsdk.net.ApiConfig.POSTS_URL + "/author/" + authorId)
                        .header("Authorization", "Bearer " + token)
                        .get()
                        .build();
                Response resp = com.virhuman.testsdk.net.ApiClient.get().newCall(req).execute();
                if (!resp.isSuccessful()) return;
                String body = resp.body() != null ? resp.body().string() : null;
                if (body == null) return;
                JSONObject root = new JSONObject(body);
                JSONArray data = root.optJSONArray("data");
                final List<JSONObject> posts = new ArrayList<>();
                if (data != null) {
                    for (int i = 0; i < data.length(); i++) {
                        posts.add(data.getJSONObject(i));
                    }
                }
                runOnUiThread(() -> {
                    if (container == null) return;
                    container.removeAllViews();
                    for (JSONObject p : posts) {
                        android.widget.LinearLayout item = new android.widget.LinearLayout(MyHomeActivity.this);
                        item.setOrientation(android.widget.LinearLayout.HORIZONTAL);
                        int pad = (int)(8 * getResources().getDisplayMetrics().density);
                        item.setPadding(pad, pad, pad, pad);
                        item.setBackgroundColor(getResources().getColor(android.R.color.white));
                        // date box
                        android.widget.LinearLayout dateBox = new android.widget.LinearLayout(MyHomeActivity.this);
                        dateBox.setOrientation(android.widget.LinearLayout.VERTICAL);
                        android.widget.LinearLayout.LayoutParams dbLp = new android.widget.LinearLayout.LayoutParams((int)(45 * getResources().getDisplayMetrics().density), android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
                        dateBox.setLayoutParams(dbLp);
                        try {
                            String createTime = p.optString("createTime", "");
                            String day = "01";
                            String month = "01";
                            if (!createTime.isEmpty() && createTime.length() >= 10) {
                                String date = createTime.substring(0,10);
                                String[] parts = date.split("-");
                                if (parts.length>=3) {
                                    day = parts[2];
                                    month = parts[1];
                                }
                            }
                            android.widget.TextView tvDay = new android.widget.TextView(MyHomeActivity.this);
                            tvDay.setText(day);
                            tvDay.setTextSize(20);
                            tvDay.setTypeface(null, android.graphics.Typeface.BOLD);
                            dateBox.addView(tvDay);
                            android.widget.TextView tvMonth = new android.widget.TextView(MyHomeActivity.this);
                            tvMonth.setText(month + "月");
                            dateBox.addView(tvMonth);
                        } catch (Exception ignored) {}
                        item.addView(dateBox);

                        // content
                        android.widget.LinearLayout content = new android.widget.LinearLayout(MyHomeActivity.this);
                        content.setOrientation(android.widget.LinearLayout.VERTICAL);
                        android.widget.LinearLayout.LayoutParams contentLp = new android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
                        content.setLayoutParams(contentLp);
                        try {
                            android.widget.TextView tvContent = new android.widget.TextView(MyHomeActivity.this);
                            tvContent.setText(p.optString("content", ""));
                            tvContent.setTextSize(15);
                            content.addView(tvContent);
                            // images
                            String imgs = p.optString("images", "");
                            if (imgs != null && !imgs.trim().isEmpty()) {
                                String[] urls = imgs.split(",");
                                android.widget.LinearLayout imgRow = new android.widget.LinearLayout(MyHomeActivity.this);
                                imgRow.setOrientation(android.widget.LinearLayout.HORIZONTAL);
                                for (String u : urls) {
                                    String url = u.trim();
                                    if (url.isEmpty()) continue;
                                    android.widget.ImageView iv = new android.widget.ImageView(MyHomeActivity.this);
                                    int dp = (int)(80 * getResources().getDisplayMetrics().density + 0.5f);
                                    android.widget.LinearLayout.LayoutParams lp = new android.widget.LinearLayout.LayoutParams(dp, dp);
                                    lp.setMargins(0,0,(int)(6 * getResources().getDisplayMetrics().density),0);
                                    iv.setLayoutParams(lp);
                                    iv.setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP);
                                    imgRow.addView(iv);
                                    loadImageAsync(url, iv);
                                }
                                content.addView(imgRow);
                            }
                        } catch (Exception ignored) {}
                        item.addView(content);
                        container.addView(item);
                    }
                });
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }).start();
    }

    private void loadImageAsync(String url, android.widget.ImageView target) {
        new Thread(() -> {
            try {
                Request r = new Request.Builder().url(url).get().build();
                Response resp = com.virhuman.testsdk.net.ApiClient.get().newCall(r).execute();
                if (resp.isSuccessful() && resp.body() != null) {
                    byte[] bytes = resp.body().bytes();
                    final android.graphics.Bitmap bmp = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                    runOnUiThread(() -> {
                        try {
                            target.setImageBitmap(bmp);
                        } catch (Throwable ignored) {}
                    });
                }
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }).start();
    }
}


