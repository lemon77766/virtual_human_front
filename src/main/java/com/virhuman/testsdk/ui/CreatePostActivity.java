package com.virhuman.testsdk.ui;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.virhuman.testsdk.R;

import java.io.IOException;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONObject;

/**
 * 发帖界面（独立）
 */
public class CreatePostActivity extends AppCompatActivity {
    private EditText etContent;
    private Button btnSubmit;
    private static final int REQ_PICK = 1001;
    private static final int REQ_CAMERA = 1002;
    private static final int REQ_PERMS = 1003;
    private int pendingAction = 0; // 1 = camera, 2 = gallery
    private android.widget.LinearLayout mediaContainer;
    private android.widget.FrameLayout addMediaBox;
    private java.util.List<android.net.Uri> selectedUris = new java.util.ArrayList<>();
    private java.util.List<android.graphics.Bitmap> selectedBitmaps = new java.util.ArrayList<>();
    // audio recording
    private String audioFilePath = null;
    private android.media.MediaRecorder mediaRecorder = null;
    private boolean isRecordingAudio = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_post);
        etContent = findViewById(R.id.et_post_content);
        btnSubmit = findViewById(R.id.btn_publish);
        mediaContainer = findViewById(R.id.media_container);
        addMediaBox = findViewById(R.id.add_media_box);
        TextView btnCancel = findViewById(R.id.btn_cancel);

        btnCancel.setOnClickListener(v -> {
            // 返回社区交互界面
            finish();
        });

        btnSubmit.setOnClickListener(v -> {
            String text = etContent.getText().toString().trim();
            if (text.isEmpty()) {
                Toast.makeText(CreatePostActivity.this, "请输入内容", Toast.LENGTH_SHORT).show();
                return;
            }
            // 发送到后端
            String title = text.length() > 20 ? text.substring(0, 20) : text;
            uploadPostToServer(title, text);
        });

        addMediaBox.setOnClickListener(v -> showMediaChooser());
        // setup voice recorder touch (press and hold)
        android.view.View voiceBox = findViewById(R.id.voice_recorder_box);
        if (voiceBox != null) {
            voiceBox.setOnTouchListener((v, ev) -> {
                switch (ev.getAction()) {
                    case android.view.MotionEvent.ACTION_DOWN:
                        // check permission
                        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, REQ_PERMS);
                            return true;
                        }
                        startAudioRecording();
                        return true;
                    case android.view.MotionEvent.ACTION_UP:
                    case android.view.MotionEvent.ACTION_CANCEL:
                        stopAudioRecording();
                        return true;
                }
                return false;
            });
        }
    }

    private void showMediaChooser() {
        String[] options = new String[]{"拍照", "从相册选择", "取消"};
        new AlertDialog.Builder(this)
                .setTitle("选择媒体")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        // 拍照
                        pendingAction = 1;
                        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQ_PERMS);
                        } else {
                            startCamera();
                        }
                    } else if (which == 1) {
                        // 相册
                        pendingAction = 2;
                        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQ_PERMS);
                        } else {
                            startGallery();
                        }
                    } else {
                        dialog.dismiss();
                    }
                })
                .show();
    }

    private void startCamera() {
        Intent camera = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (camera.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(camera, REQ_CAMERA);
        } else {
            Toast.makeText(this, "设备不支持拍照", Toast.LENGTH_SHORT).show();
        }
    }

    private void startAudioRecording() {
        try {
            if (isRecordingAudio) return;
            java.io.File outDir = getCacheDir();
            String fname = "post_audio_" + System.currentTimeMillis() + ".m4a";
            java.io.File outFile = new java.io.File(outDir, fname);
            audioFilePath = outFile.getAbsolutePath();

            mediaRecorder = new android.media.MediaRecorder();
            mediaRecorder.setAudioSource(android.media.MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(android.media.MediaRecorder.OutputFormat.MPEG_4);
            mediaRecorder.setAudioEncoder(android.media.MediaRecorder.AudioEncoder.AAC);
            mediaRecorder.setAudioSamplingRate(16000);
            mediaRecorder.setOutputFile(audioFilePath);
            mediaRecorder.prepare();
            mediaRecorder.start();
            isRecordingAudio = true;
            runOnUiThread(() -> Toast.makeText(CreatePostActivity.this, "开始录音", Toast.LENGTH_SHORT).show());
        } catch (Exception e) {
            e.printStackTrace();
            runOnUiThread(() -> Toast.makeText(CreatePostActivity.this, "录音开始失败: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        }
    }

    private void stopAudioRecording() {
        try {
            if (!isRecordingAudio) return;
            if (mediaRecorder != null) {
                try { mediaRecorder.stop(); } catch (Throwable ignored) {}
                mediaRecorder.reset();
                mediaRecorder.release();
                mediaRecorder = null;
            }
            isRecordingAudio = false;
            runOnUiThread(() -> Toast.makeText(CreatePostActivity.this, "录音已保存", Toast.LENGTH_SHORT).show());
            // show audio thumbnail in mediaContainer
            addAudioThumbnail(audioFilePath);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addAudioThumbnail(String path) {
        if (path == null) return;
        FrameLayout audioBox = createAudioThumbBox(path);
        audioBox.setTag(path);
        insertThumbnailView(audioBox);
    }

    private FrameLayout createAudioThumbBox(String path) {
        FrameLayout thumb = new FrameLayout(this);
        float density = getResources().getDisplayMetrics().density;
        int widthPx = (int) (160 * density + 0.5f);
        int heightPx = (int) (80 * density + 0.5f);
        android.widget.LinearLayout.LayoutParams lp = new android.widget.LinearLayout.LayoutParams(widthPx, heightPx);
        lp.setMargins(0, 0, (int)(8 * density), 0);
        thumb.setLayoutParams(lp);

        // background rounded card
        android.view.View bgCard = new android.view.View(this);
        FrameLayout.LayoutParams bgLp = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
        bgCard.setLayoutParams(bgLp);
        bgCard.setBackgroundResource(R.drawable.bg_profile_card); // reused white card background
        thumb.addView(bgCard);

        // left mic icon circle
        ImageView micCircle = new ImageView(this);
        micCircle.setImageResource(R.drawable.ic_mic);
        int micSize = (int)(44 * density);
        FrameLayout.LayoutParams micLp = new FrameLayout.LayoutParams(micSize, micSize);
        micLp.setMargins((int)(10 * density), 0, 0, 0);
        micLp.gravity = android.view.Gravity.CENTER_VERTICAL | android.view.Gravity.START;
        micCircle.setLayoutParams(micLp);
        micCircle.setBackgroundResource(R.drawable.bg_circle_primary);
        micCircle.setPadding((int)(8*density),(int)(8*density),(int)(8*density),(int)(8*density));
        thumb.addView(micCircle);

        // center text (Audio)
        TextView tvLabel = new TextView(this);
        tvLabel.setText("语音留言");
        tvLabel.setTextColor(getResources().getColor(R.color.home_text_primary));
        tvLabel.setTextSize(14);
        FrameLayout.LayoutParams labelLp = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        labelLp.gravity = android.view.Gravity.CENTER_VERTICAL | android.view.Gravity.START;
        labelLp.setMargins((int)(70 * density), 0, 0, 0);
        tvLabel.setLayoutParams(labelLp);
        thumb.addView(tvLabel);

        // duration badge bottom-right
        TextView tvDur = new TextView(this);
        tvDur.setText("00:00");
        tvDur.setTextSize(12);
        tvDur.setTextColor(getResources().getColor(android.R.color.white));
        tvDur.setBackgroundResource(R.drawable.bg_circle_primary);
        FrameLayout.LayoutParams durLp = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        durLp.gravity = android.view.Gravity.END | android.view.Gravity.BOTTOM;
        durLp.setMargins(0,0,(int)(8*density),(int)(8*density));
        tvDur.setLayoutParams(durLp);
        thumb.addView(tvDur);

        // play overlay (center)
        ImageButton play = new ImageButton(this);
        play.setBackgroundResource(android.R.color.transparent);
        play.setImageResource(R.drawable.ic_play);
        int playSize = (int)(36 * density);
        FrameLayout.LayoutParams playLp = new FrameLayout.LayoutParams(playSize, playSize);
        playLp.gravity = android.view.Gravity.CENTER;
        play.setLayoutParams(playLp);
        thumb.addView(play);

        // close button top-right
        ImageView close = new ImageView(this);
        close.setImageResource(R.drawable.ic_close);
        FrameLayout.LayoutParams closeLp = new FrameLayout.LayoutParams((int)(20 * density),(int)(20 * density));
        closeLp.gravity = android.view.Gravity.END | android.view.Gravity.TOP;
        closeLp.setMargins(0,(int)(6*density),(int)(6*density),0);
        close.setLayoutParams(closeLp);
        thumb.addView(close);

        // set duration if available
        try {
            android.media.MediaMetadataRetriever mmr = new android.media.MediaMetadataRetriever();
            mmr.setDataSource(path);
            String dur = mmr.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION);
            if (dur != null) {
                int ms = Integer.parseInt(dur);
                int s = ms / 1000;
                String durText = String.format("%02d:%02d", s/60, s%60);
                tvDur.setText(durText);
            }
            mmr.release();
        } catch (Throwable ignored) {}

        // play preview
        play.setOnClickListener(v -> {
            try {
                android.media.MediaPlayer mp = new android.media.MediaPlayer();
                mp.setDataSource(path);
                mp.setOnPreparedListener(mediaPlayer -> mediaPlayer.start());
                mp.setOnCompletionListener(mediaPlayer -> {
                    try { mediaPlayer.release(); } catch (Exception ignored) {}
                });
                mp.prepareAsync();
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "音频预览失败", Toast.LENGTH_SHORT).show();
            }
        });

        close.setOnClickListener(v -> {
            mediaContainer.removeView(thumb);
            try {
                java.io.File f = new java.io.File(path);
                if (f.exists()) f.delete();
            } catch (Exception ignored) {}
        });

        return thumb;
    }

    private void startGallery() {
        Intent pick = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(pick, REQ_PICK);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_PERMS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (pendingAction == 1) startCamera();
                else if (pendingAction == 2) startGallery();
            } else {
                Toast.makeText(this, "权限被拒绝，无法完成操作", Toast.LENGTH_SHORT).show();
            }
            pendingAction = 0;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != Activity.RESULT_OK || data == null) return;
        if (requestCode == REQ_CAMERA) {
            Bundle extras = data.getExtras();
            if (extras != null && extras.get("data") instanceof Bitmap) {
                Bitmap bmp = (Bitmap) extras.get("data");
                addThumbnail(bmp);
            }
        } else if (requestCode == REQ_PICK) {
            Uri uri = data.getData();
            if (uri != null) {
                try {
                    // 将选中的图片添加为缩略图
                    addThumbnail(uri);
                } catch (Exception e) {
                    Toast.makeText(this, "加载图片失败", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    /* 添加缩略图到 mediaContainer（Bitmap 版本） */
    private void addThumbnail(Bitmap bmp) {
        FrameLayout thumbBox = createThumbBox();
        ImageView iv = thumbBox.findViewWithTag("thumb_image");
        if (iv != null) iv.setImageBitmap(bmp);
        // store bitmap reference for upload and deletion
        selectedBitmaps.add(bmp);
        thumbBox.setTag(bmp);
        insertThumbnailView(thumbBox);
    }

    /* 添加缩略图到 mediaContainer（Uri 版本） */
    private void addThumbnail(Uri uri) {
        FrameLayout thumbBox = createThumbBox();
        ImageView iv = thumbBox.findViewWithTag("thumb_image");
        if (iv != null) iv.setImageURI(uri);
        // store uri reference for upload and deletion
        selectedUris.add(uri);
        thumbBox.setTag(uri);
        insertThumbnailView(thumbBox);
    }

    private void insertThumbnailView(FrameLayout thumbBox) {
        // 插入到 addMediaBox 之前，这样添加的缩略图会显示在媒体块左侧，媒体块向右移动
        int insertIndex = mediaContainer.indexOfChild(addMediaBox);
        if (insertIndex < 0) {
            // fallback: append
            mediaContainer.addView(thumbBox);
        } else {
            mediaContainer.addView(thumbBox, insertIndex);
        }
    }

    /**
     * 点击发布时调用：构建 multipart/form-data 请求上传 postJson 和 images
     */
    private void uploadPostToServer(String title, String content) {
        // 构建 JSON 对象
        JSONObject postJson = new JSONObject();
        try {
            int authorId = 1;
            // 尝试从 AuthStore 获取 userId
            try {
                authorId = com.virhuman.testsdk.utils.AuthStore.getUserId(this);
                if (authorId == 0) authorId = 1;
            } catch (Throwable ignored) {}
            postJson.put("authorId", authorId);
            postJson.put("title", title);
            postJson.put("content", content);
            postJson.put("postTags", "");
            postJson.put("locationTags", "");
        } catch (Exception e) {
            e.printStackTrace();
        }

        final String postJsonStr = postJson.toString();

        // 检查 token（UI 线程）
        String tokenCheck = com.virhuman.testsdk.utils.AuthStore.getToken(this);
        if (tokenCheck == null || tokenCheck.trim().isEmpty()) {
            Toast.makeText(this, "未登录：请先登录后再发布", Toast.LENGTH_LONG).show();
            return;
        }

        new Thread(() -> {
            try {
                MultipartBody.Builder mb = new MultipartBody.Builder().setType(MultipartBody.FORM);
                mb.addFormDataPart("postJson", postJsonStr);

                int idx = 0;
                // URIs
                for (android.net.Uri uri : selectedUris) {
                    try {
                        java.io.InputStream is = getContentResolver().openInputStream(uri);
                        if (is == null) continue;
                        byte[] bytes = readAllBytes(is);
                        is.close();
                        RequestBody rb = RequestBody.create(MediaType.parse("image/*"), bytes);
                        mb.addFormDataPart("images", "img_" + (idx++) + ".jpg", rb);
                    } catch (Throwable t) { t.printStackTrace(); }
                }

                // Bitmaps
                for (android.graphics.Bitmap bmp : selectedBitmaps) {
                    java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                    bmp.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, baos);
                    byte[] bytes = baos.toByteArray();
                    RequestBody rb = RequestBody.create(MediaType.parse("image/*"), bytes);
                    mb.addFormDataPart("images", "img_bmp_" + (idx++) + ".jpg", rb);
                }
                // audio file
                if (audioFilePath != null) {
                    try {
                        java.io.File audioF = new java.io.File(audioFilePath);
                        if (audioF.exists()) {
                            RequestBody rbAudio = RequestBody.create(MediaType.parse("audio/m4a"), audioF);
                            mb.addFormDataPart("audio", audioF.getName(), rbAudio);
                        }
                    } catch (Throwable ignored) {}
                }

                RequestBody requestBody = mb.build();
                Request.Builder reqBuilder = new Request.Builder()
                        .url(com.virhuman.testsdk.net.ApiConfig.BASE_URL + "/api/posts/with-images")
                        .post(requestBody);
                // 添加 Authorization Header
                String token = com.virhuman.testsdk.utils.AuthStore.getToken(CreatePostActivity.this);
                if (token != null && !token.trim().isEmpty()) {
                    reqBuilder.header("Authorization", "Bearer " + token);
                }
                Request request = reqBuilder.build();

                Response resp = com.virhuman.testsdk.net.ApiClient.get().newCall(request).execute();
                final String respBody = resp.body() != null ? resp.body().string() : null;
                runOnUiThread(() -> {
                    if (resp.isSuccessful()) {
                        android.widget.Toast.makeText(CreatePostActivity.this, "发布成功", android.widget.Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        android.widget.Toast.makeText(CreatePostActivity.this, "发布失败: " + respBody, android.widget.Toast.LENGTH_LONG).show();
                    }
                });
            } catch (Throwable t) {
                t.printStackTrace();
                runOnUiThread(() -> android.widget.Toast.makeText(CreatePostActivity.this, "发布异常: " + t.getMessage(), android.widget.Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private static byte[] readAllBytes(java.io.InputStream is) throws java.io.IOException {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int r;
        while ((r = is.read(buf)) != -1) {
            baos.write(buf, 0, r);
        }
        return baos.toByteArray();
    }

    private FrameLayout createThumbBox() {
        FrameLayout thumb = new FrameLayout(this);
        float density = getResources().getDisplayMetrics().density;
        int sizePx = (int) (120 * density + 0.5f);
        android.widget.LinearLayout.LayoutParams lp = new android.widget.LinearLayout.LayoutParams(sizePx, sizePx);
        lp.setMargins(0, 0, (int)(8 * getResources().getDisplayMetrics().density), 0);
        thumb.setLayoutParams(lp);

        ImageView iv = new ImageView(this);
        iv.setId(View.generateViewId());
        iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
        FrameLayout.LayoutParams ivLp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
        iv.setLayoutParams(ivLp);
        iv.setTag("thumb_image");
        thumb.addView(iv);

        ImageView close = new ImageView(this);
        close.setImageResource(R.drawable.ic_close);
        FrameLayout.LayoutParams closeLp = new FrameLayout.LayoutParams((int)(24 * getResources().getDisplayMetrics().density),
                (int)(24 * getResources().getDisplayMetrics().density));
        closeLp.gravity = android.view.Gravity.END | android.view.Gravity.TOP;
        closeLp.setMargins(0, (int)(4 * getResources().getDisplayMetrics().density), (int)(4 * getResources().getDisplayMetrics().density), 0);
        close.setLayoutParams(closeLp);
        close.setBackgroundResource(android.R.color.transparent);
        thumb.addView(close);

        close.setOnClickListener(v -> {
            // 从容器移除该缩略图
            mediaContainer.removeView(thumb);
        });

        return thumb;
    }
}


