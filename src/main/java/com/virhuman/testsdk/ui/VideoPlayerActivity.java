package com.virhuman.testsdk.ui;

import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import androidx.core.content.ContextCompat;
import android.view.View;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.virhuman.testsdk.R;

import java.util.Locale;

public class VideoPlayerActivity extends AppCompatActivity {
    public static final String EXTRA_URL = "extra_url";

    private VideoView videoView;
    private ImageButton btnPlayPause;
    private SeekBar seekBar;
    private TextView tvTime;

    private Handler handler = new Handler();
    private Runnable progressRunnable;
    private boolean userSeeking = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // make activity fullscreen (hide status bar)
        try {
            getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN);
        } catch (Exception ignored) {}
        setContentView(R.layout.activity_video_player);

        videoView = findViewById(R.id.video_view_player);
        btnPlayPause = findViewById(R.id.btn_play_pause);
        seekBar = findViewById(R.id.seek_bar_player);
        tvTime = findViewById(R.id.tv_time);

        String url = getIntent().getStringExtra(EXTRA_URL);
        if (url == null || url.trim().isEmpty()) {
            Toast.makeText(this, "未提供视频地址", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        videoView.setVideoURI(Uri.parse(url));

        // set initial icon to pause (video will auto-start on prepared)
        try {
            btnPlayPause.setImageResource(R.drawable.ic_pause);
            btnPlayPause.setColorFilter(ContextCompat.getColor(this, android.R.color.white));
        } catch (Exception ignored) {}

        videoView.setOnPreparedListener(mp -> {
            try {
                int duration = mp.getDuration();
                if (duration > 0) seekBar.setMax(duration);
            } catch (Exception ignored) {}
            videoView.start();
            try { btnPlayPause.setImageResource(R.drawable.ic_pause); btnPlayPause.setColorFilter(ContextCompat.getColor(this, android.R.color.white)); } catch (Exception ignored) {}
            startProgressUpdater();
        });

        videoView.setOnCompletionListener(mp -> {
            try { btnPlayPause.setImageResource(R.drawable.ic_play); btnPlayPause.setColorFilter(ContextCompat.getColor(this, android.R.color.white)); } catch (Exception ignored) {}
            seekBar.setProgress(seekBar.getMax());
            updateTimeText();
        });

        videoView.setOnErrorListener((mp, what, extra) -> {
            Toast.makeText(VideoPlayerActivity.this, "视频播放失败", Toast.LENGTH_SHORT).show();
            return true;
        });

        btnPlayPause.setOnClickListener(v -> {
            try {
                if (videoView.isPlaying()) {
                    videoView.pause();
                    btnPlayPause.setImageResource(R.drawable.ic_play);
                    btnPlayPause.setColorFilter(ContextCompat.getColor(VideoPlayerActivity.this, android.R.color.white));
                } else {
                    videoView.start();
                    btnPlayPause.setImageResource(R.drawable.ic_pause);
                    btnPlayPause.setColorFilter(ContextCompat.getColor(VideoPlayerActivity.this, android.R.color.white));
                }
            } catch (Exception e) {
                Toast.makeText(this, "操作失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar1, int progress, boolean fromUser) {
                if (fromUser) {
                    updateTimeText(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar1) {
                userSeeking = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar1) {
                userSeeking = false;
                try {
                    int pos = seekBar1.getProgress();
                    videoView.seekTo(pos);
                } catch (Exception ignored) {}
            }
        });
    }

    private void startProgressUpdater() {
        if (progressRunnable != null) return;
        progressRunnable = new Runnable() {
            @Override
            public void run() {
                try {
                    if (videoView != null && seekBar != null && !userSeeking) {
                        int pos = videoView.getCurrentPosition();
                        seekBar.setProgress(pos);
                        updateTimeText(pos);
                    }
                } catch (Exception ignored) {}
                handler.postDelayed(this, 500);
            }
        };
        handler.post(progressRunnable);
    }

    private void stopProgressUpdater() {
        try {
            if (progressRunnable != null) {
                handler.removeCallbacks(progressRunnable);
                progressRunnable = null;
            }
        } catch (Exception ignored) {}
    }

    private void updateTimeText() {
        int pos = videoView != null ? videoView.getCurrentPosition() : 0;
        updateTimeText(pos);
    }

    private void updateTimeText(int pos) {
        int dur = seekBar != null ? seekBar.getMax() : 0;
        tvTime.setText(String.format(Locale.getDefault(), "%s/%s", formatMs(pos), formatMs(dur)));
    }

    private String formatMs(int ms) {
        if (ms <= 0) return "00:00";
        int totalSeconds = ms / 1000;
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
    }

    @Override
    protected void onDestroy() {
        stopProgressUpdater();
        if (videoView != null) {
            try { videoView.stopPlayback(); } catch (Exception ignored) {}
            videoView = null;
        }
        super.onDestroy();
    }
}


