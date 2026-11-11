package com.virhuman.testsdk.ui;

import android.content.Intent;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.text.Html;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.virhuman.testsdk.R;
import android.content.Intent;
import android.net.Uri;
import com.virhuman.testsdk.net.ApiConfig;
import com.virhuman.testsdk.net.ApiClient;
import com.virhuman.testsdk.net.ArticleApi;
import com.virhuman.testsdk.net.OperaApi;
import com.virhuman.testsdk.utils.AuthStore;

import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;

/**
 * 通用详情页：用于展示资讯（article）和戏曲（opera）的详情
 * Intent extras:
 * - "type": "article" 或 "opera"
 * - "id": 整数 id
 */
public class InfoDetailActivity extends AppCompatActivity {
    private static final String TAG = "InfoDetailActivity";

    private ImageView ivCover;
    private TextView tvTitle;
    private TextView tvAuthor;
    private TextView tvViews;
    private TextView tvContent;
    private ImageButton btnPlayAudio;
    private ProgressBar progress;

    private MediaPlayer mediaPlayer;
    private Handler handler;
    private SeekBar seekAudio;
    private TextView tvAudioDuration;
    private ImageButton btnBack;
    private ImageButton btnFavorite;
    private ImageButton btnShare;
    private boolean isFavorite = false;
    private String currentType;
    private int currentId;
    private Runnable audioProgressRunnable;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info_detail);

        ivCover = findViewById(R.id.iv_detail_cover);
        tvTitle = findViewById(R.id.tv_detail_title);
        tvAuthor = findViewById(R.id.tv_detail_author);
        tvViews = findViewById(R.id.tv_detail_views);
        tvContent = findViewById(R.id.tv_detail_content);
        btnPlayAudio = findViewById(R.id.btn_detail_play_audio);
        progress = findViewById(R.id.progress_loading);

        handler = new Handler(Looper.getMainLooper());

        Intent it = getIntent();
        String type = it != null ? it.getStringExtra("type") : null;
        int id = it != null ? it.getIntExtra("id", -1) : -1;
        currentType = type;
        currentId = id;
        if (type == null || id <= 0) {
            Toast.makeText(this, "无效的详情参数", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        progress.setVisibility(View.VISIBLE);
        fetchDetail(type, id);

        btnPlayAudio.setOnClickListener(v -> {
            try {
                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    mediaPlayer.pause();
                    btnPlayAudio.setImageResource(R.drawable.ic_play);
                } else if (mediaPlayer != null) {
                    mediaPlayer.start();
                    btnPlayAudio.setImageResource(R.drawable.ic_pause);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        // find new UI
        seekAudio = findViewById(R.id.seek_audio);
        tvAudioDuration = findViewById(R.id.tv_audio_duration);
        btnBack = findViewById(R.id.btn_detail_back);
        btnFavorite = findViewById(R.id.btn_detail_favorite);
        btnShare = findViewById(R.id.btn_detail_share);
        ImageButton btnContentLike = findViewById(R.id.btn_content_like);
        ImageButton btnContentShare = findViewById(R.id.btn_content_share);

        btnBack.setOnClickListener(v -> finish());
        btnShare.setOnClickListener(v -> {
            try {
                String shareUrl;
                if ("article".equals(currentType)) shareUrl = ApiConfig.API_ARTICLE + "/" + currentId;
                else if ("opera".equals(currentType)) shareUrl = ApiConfig.API_OPERA + "/" + currentId;
                else shareUrl = ApiConfig.POSTS_URL + "/" + currentId;
                Intent share = new Intent(Intent.ACTION_SEND);
                share.setType("text/plain");
                share.putExtra(Intent.EXTRA_TEXT, tvTitle.getText().toString() + " " + shareUrl);
                startActivity(Intent.createChooser(share, "分享"));
            } catch (Exception e) { e.printStackTrace(); }
        });

        btnFavorite.setOnClickListener(v -> {
            int userId = AuthStore.getUserId(this);
            if (userId <= 0) {
                Toast.makeText(this, "请先登录后再收藏", Toast.LENGTH_SHORT).show();
                return;
            }
            if ("article".equals(currentType)) {
                if (!isFavorite) {
                    ArticleApi.favoriteArticle(this, currentId, userId, new ArticleApi.SimpleCallback() {
                        @Override public void onSuccess() {
                            isFavorite = true;
                            btnFavorite.setImageResource(R.drawable.ic_heart_solid);
                            Toast.makeText(InfoDetailActivity.this, "已收藏", Toast.LENGTH_SHORT).show();
                        }
                        @Override public void onFailure(String errorMessage) { Toast.makeText(InfoDetailActivity.this, errorMessage, Toast.LENGTH_SHORT).show(); }
                    });
                } else {
                    ArticleApi.unfavoriteArticle(this, currentId, userId, new ArticleApi.SimpleCallback() {
                        @Override public void onSuccess() {
                            isFavorite = false;
                            btnFavorite.setImageResource(R.drawable.ic_heart_outline);
                            Toast.makeText(InfoDetailActivity.this, "已取消收藏", Toast.LENGTH_SHORT).show();
                        }
                        @Override public void onFailure(String errorMessage) { Toast.makeText(InfoDetailActivity.this, errorMessage, Toast.LENGTH_SHORT).show(); }
                    });
                }
            } else if ("opera".equals(currentType)) {
                if (!isFavorite) {
                    OperaApi.favoriteOpera(this, currentId, userId, new OperaApi.SimpleCallback() {
                        @Override public void onSuccess() {
                            isFavorite = true;
                            btnFavorite.setImageResource(R.drawable.ic_heart_solid);
                            Toast.makeText(InfoDetailActivity.this, "已收藏", Toast.LENGTH_SHORT).show();
                        }
                        @Override public void onFailure(String errorMessage) { Toast.makeText(InfoDetailActivity.this, errorMessage, Toast.LENGTH_SHORT).show(); }
                    });
                } else {
                    OperaApi.unfavoriteOpera(this, currentId, userId, new OperaApi.SimpleCallback() {
                        @Override public void onSuccess() {
                            isFavorite = false;
                            btnFavorite.setImageResource(R.drawable.ic_heart_outline);
                            Toast.makeText(InfoDetailActivity.this, "已取消收藏", Toast.LENGTH_SHORT).show();
                        }
                        @Override public void onFailure(String errorMessage) { Toast.makeText(InfoDetailActivity.this, errorMessage, Toast.LENGTH_SHORT).show(); }
                    });
                }
            }
        });
        // content-level share & like
        // content-level share & like (persistent)
        final String likeKey = "liked_" + (currentType != null ? currentType : "unknown") + "_" + currentId;
        final android.content.SharedPreferences prefs = getSharedPreferences("info_likes", MODE_PRIVATE);
        if (btnContentShare != null) {
            // tint share icon to blue for clarity
            try { btnContentShare.setColorFilter(getResources().getColor(R.color.icon_virtual_blue)); } catch (Exception ignored) {}
            btnContentShare.setOnClickListener(v -> {
                try {
                    String shareUrl;
                    if ("article".equals(currentType)) shareUrl = ApiConfig.API_ARTICLE + "/" + currentId;
                    else if ("opera".equals(currentType)) shareUrl = ApiConfig.API_OPERA + "/" + currentId;
                    else shareUrl = ApiConfig.POSTS_URL + "/" + currentId;
                    Intent share = new Intent(Intent.ACTION_SEND);
                    share.setType("text/plain");
                    share.putExtra(Intent.EXTRA_TEXT, tvTitle.getText().toString() + " " + shareUrl);
                    startActivity(Intent.createChooser(share, "分享"));
                } catch (Exception e) { e.printStackTrace(); }
            });
        }
        if (btnContentLike != null) {
            // initialize from prefs
            boolean likedInitial = prefs.getBoolean(likeKey, false);
            if (likedInitial) {
                try { btnContentLike.setImageResource(R.drawable.likecolour); btnContentLike.setColorFilter(getResources().getColor(R.color.profile_accent_red)); } catch (Exception ignored) {}
                btnContentLike.setTag(true);
            } else {
                try { btnContentLike.setImageResource(R.drawable.like); btnContentLike.setColorFilter(getResources().getColor(R.color.general_text_medium)); } catch (Exception ignored) {}
                btnContentLike.setTag(false);
            }

            btnContentLike.setOnClickListener(v -> {
                Object tag = btnContentLike.getTag();
                boolean liked = tag != null && (boolean) tag;
                if (!liked) {
                    try { btnContentLike.setImageResource(R.drawable.likecolour); btnContentLike.setColorFilter(getResources().getColor(R.color.profile_accent_red)); } catch (Exception ignored) {}
                    btnContentLike.setTag(true);
                    prefs.edit().putBoolean(likeKey, true).apply();
                } else {
                    try { btnContentLike.setImageResource(R.drawable.like); btnContentLike.setColorFilter(getResources().getColor(R.color.general_text_medium)); } catch (Exception ignored) {}
                    btnContentLike.setTag(false);
                    prefs.edit().putBoolean(likeKey, false).apply();
                }
            });
        }
    }

    private void fetchDetail(String type, int id) {
        try {
            String url;
            if ("article".equals(type)) {
                url = ApiConfig.API_ARTICLE + "/" + id;
            } else if ("opera".equals(type)) {
                url = ApiConfig.API_OPERA + "/" + id;
            } else {
                // fallback to posts
                url = ApiConfig.POSTS_URL + "/" + id;
            }

            Request.Builder rb = new Request.Builder().url(url).get();
            String token = AuthStore.getToken(this);
            if (token != null && !token.trim().isEmpty()) rb.addHeader("Authorization", "Bearer " + token);
            Request req = rb.build();

            ApiClient.get().newCall(req).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    handler.post(() -> {
                        progress.setVisibility(View.GONE);
                        Toast.makeText(InfoDetailActivity.this, "请求失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        finish();
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    final String body = response.body() != null ? response.body().string() : null;
                    if (!response.isSuccessful() || body == null) {
                        handler.post(() -> {
                            progress.setVisibility(View.GONE);
                            Toast.makeText(InfoDetailActivity.this, "获取详情失败", Toast.LENGTH_SHORT).show();
                            finish();
                        });
                        return;
                    }

                    try {
                        JSONObject root = new JSONObject(body);
                        JSONObject data = root.optJSONObject("data");
                        if (data == null) {
                            // some endpoints return data as array; try parsing first element
                            handler.post(() -> {
                                progress.setVisibility(View.GONE);
                                Toast.makeText(InfoDetailActivity.this, "详情数据为空", Toast.LENGTH_SHORT).show();
                                finish();
                            });
                            return;
                        }

                        final String title = data.optString("title", "");
                        // 对于戏曲类型，使用artist字段作为作者；其他类型使用author字段
                        final String author = "opera".equals(currentType) ?
                                data.optString("artist", "") : data.optString("author", "");
                        // 对于戏曲类型，使用description字段作为内容；其他类型使用content字段
                        final String content = "opera".equals(currentType) ?
                                data.optString("description", "") : data.optString("content", "");
                        final String coverUrl = data.optString("coverUrl", null);
                        final String audioUrl = data.optString("audioUrl", null);
                        final int views = data.optInt("viewCount", 0);

                        handler.post(() -> {
                            progress.setVisibility(View.GONE);
                            tvTitle.setText(title);
                            tvAuthor.setText(author);
                            tvViews.setText(String.valueOf(views));
                            tvContent.setText(content);
                            if (coverUrl != null && !coverUrl.trim().isEmpty()) {
                                Glide.with(InfoDetailActivity.this).load(coverUrl).centerCrop().into(ivCover);
                            }
                            if (audioUrl != null && !audioUrl.trim().isEmpty()) {
                                setupAudioPlayer(audioUrl);
                                btnPlayAudio.setVisibility(View.VISIBLE);
                                seekAudio.setVisibility(View.VISIBLE);
                                tvAudioDuration.setVisibility(View.VISIBLE);
                            } else {
                                btnPlayAudio.setVisibility(View.GONE);
                                seekAudio.setVisibility(View.GONE);
                                tvAudioDuration.setVisibility(View.GONE);
                            }
                        // 报告阅读数（异步）
                        reportView(currentType, currentId);
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                        handler.post(() -> {
                            progress.setVisibility(View.GONE);
                            Toast.makeText(InfoDetailActivity.this, "解析详情失败", Toast.LENGTH_SHORT).show();
                            finish();
                        });
                    }
                }
            });
        } catch (Exception e) {
            progress.setVisibility(View.GONE);
            Toast.makeText(this, "无法请求详情: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void setupAudioPlayer(String audioUrl) {
        try {
            if (mediaPlayer != null) {
                try { mediaPlayer.release(); } catch (Exception ignored) {}
                mediaPlayer = null;
            }
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build());
            mediaPlayer.setDataSource(audioUrl);
            mediaPlayer.prepareAsync();
            mediaPlayer.setOnPreparedListener(mp -> {
                // ready to play
                btnPlayAudio.setImageResource(R.drawable.ic_play);
                try {
                    int total = mp.getDuration();
                    if (total > 0 && seekAudio != null && tvAudioDuration != null) {
                        seekAudio.setMax(total);
                        tvAudioDuration.setText(formatMs(0) + " / " + formatMs(total));
                        // update progress runnable
                        if (audioProgressRunnable != null) handler.removeCallbacks(audioProgressRunnable);
                        audioProgressRunnable = new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    if (mediaPlayer != null && seekAudio != null && tvAudioDuration != null) {
                                        int cur = mediaPlayer.getCurrentPosition();
                                        seekAudio.setProgress(cur);
                                        int dur = mediaPlayer.getDuration();
                                        tvAudioDuration.setText(formatMs(cur) + " / " + formatMs(dur));
                                    }
                                } catch (Exception ignored) {}
                                handler.postDelayed(this, 500);
                            }
                        };
                        handler.post(audioProgressRunnable);
                    }
                } catch (Exception ignored) {}
            });
            mediaPlayer.setOnCompletionListener(mp -> btnPlayAudio.setImageResource(R.drawable.ic_play));
            // seek listener
            if (seekAudio != null) {
                seekAudio.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        if (fromUser && mediaPlayer != null) {
                            try { mediaPlayer.seekTo(progress); } catch (Exception ignored) {}
                        }
                    }
                    @Override public void onStartTrackingTouch(SeekBar seekBar) {}
                    @Override public void onStopTrackingTouch(SeekBar seekBar) {}
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "音频初始化失败", Toast.LENGTH_SHORT).show();
            btnPlayAudio.setVisibility(View.GONE);
        }
    }

    private void reportView(String type, int id) {
        try {
            String url;
            if ("article".equals(type)) {
                url = ApiConfig.BASE_URL + "/api/article/" + id + "/view";
            } else if ("opera".equals(type)) {
                url = ApiConfig.BASE_URL + "/api/opera/" + id + "/view";
            } else {
                url = ApiConfig.BASE_URL + "/api/posts/" + id + "/view";
            }
            Request req = new Request.Builder().url(url).post(okhttp3.RequestBody.create(null, new byte[0])).build();
            ApiClient.get().newCall(req).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) { /* ignore */ }
                @Override
                public void onResponse(Call call, Response response) throws IOException { /* ignore */ }
            });
        } catch (Exception ignored) {}
    }

    private String formatMs(int ms) {
        if (ms <= 0) return "00:00";
        int s = ms / 1000;
        return String.format("%02d:%02d", s/60, s%60);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            try { mediaPlayer.stop(); } catch (Exception ignored) {}
            try { mediaPlayer.release(); } catch (Exception ignored) {}
            mediaPlayer = null;
        }
        if (audioProgressRunnable != null && handler != null) {
            handler.removeCallbacks(audioProgressRunnable);
        }
    }
}


