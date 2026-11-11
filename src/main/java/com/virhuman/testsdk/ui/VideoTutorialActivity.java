package com.virhuman.testsdk.ui;

import android.os.Bundle;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.virhuman.testsdk.R;

public class VideoTutorialActivity extends AppCompatActivity {
    public static final String EXTRA_URL = "extra_url";

    private WebView webView;
    private Button btnPlay;
    private Button btnPause;
    private Button btnReplay;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_tutorial);

        webView = findViewById(R.id.webview_tutorial);
        btnPlay = findViewById(R.id.btn_play);
        btnPause = findViewById(R.id.btn_pause);
        btnReplay = findViewById(R.id.btn_replay);

        WebSettings ws = webView.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setDomStorageEnabled(true);
        ws.setMediaPlaybackRequiresUserGesture(false);

        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient());

        String url = getIntent().getStringExtra(EXTRA_URL);
        if (url == null || url.trim().isEmpty()) {
            Toast.makeText(this, "未提供视频地址", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        webView.loadUrl(url);

        btnPlay.setOnClickListener(v -> {
            // Try to play any HTML5 <video> element on the page
            webView.evaluateJavascript(
                    "(function(){var v=document.querySelector('video'); if(v){v.play(); return true;} return false;})()", null);
        });

        btnPause.setOnClickListener(v -> {
            webView.evaluateJavascript(
                    "(function(){var v=document.querySelector('video'); if(v){v.pause(); return true;} return false;})()", null);
        });

        btnReplay.setOnClickListener(v -> {
            webView.evaluateJavascript(
                    "(function(){var v=document.querySelector('video'); if(v){v.currentTime=0; v.play(); return true;} return false;})()", null);
        });
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.loadUrl("about:blank");
            webView.stopLoading();
            webView.setWebChromeClient(null);
            webView.setWebViewClient(null);
            webView.destroy();
        }
        super.onDestroy();
    }
}


