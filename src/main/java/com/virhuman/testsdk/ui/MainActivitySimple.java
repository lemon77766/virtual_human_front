package com.virhuman.testsdk.ui;

import android.media.AudioFormat;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.iflytek.avalibrary.params.AudioParams;
import com.iflytek.avalibrary.params.AvatarParams;
import com.iflytek.avalibrary.videoplayer.params.StreamPlayerParams;
import com.virhuman.testsdk.R;
import com.virhuman.testsdk.config.AvatarConfig;
import com.virhuman.testsdk.utils.AvatarUtils;

// 科大讯飞虚拟人SDK导入
import com.iflytek.avalibrary.AvatarPlatform;
import com.iflytek.avalibrary.AvatarPlayController;
import com.iflytek.avalibrary.IAvatarListener;
import com.iflytek.avalibrary.constant.AvatarError;
import com.iflytek.avalibrary.params.TextParams;
import com.iflytek.avalibrary.videoplayer.rt.interfaces.IStreamPlayer;
import com.iflytek.ys.core.recorder.AudioRecorder;
import com.virhuman.testsdk.utils.PermissionUtils;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.content.pm.PackageManager;

/**
 * 简化版云模式Activity - 使用工具类优化代码
 */
public class MainActivitySimple extends AppCompatActivity {
    private static final String TAG = "MainActivitySimple";

    // UI控件
    private TextView tvVersionShow;
    private Spinner spinnerLog;
    private EditText etAppId;
    private EditText etApiKey;
    private EditText etApiSecret;
    private EditText etAnchorId;
    private EditText etVcn;
    private EditText etInputBox;
    private Switch switchNlp;
    private Switch switchAutoAction;
    private TextView tvInitResult;
    private TextView tvCreatePlayer;
    private TextView tvStart;
    private TextView tvSendText;
    private SeekBar sbPlayerVolume;
    private TextView tvStop;
    private TextView tvOneKeyStart;
    private FrameLayout videoViewAva;
    // programmatic controls for explicit audio flow
    private Button btnStartRecordCtrl;
    private Button btnStopRecordCtrl;
    // overlay container for audio control buttons
    private ViewGroup audioOverlayContainer;
    // 音频交互
    private TextView tvCreateRecorder;
    private TextView tvStartRecord;
    private TextView tvStopRecord;
    
    // SDK对象
    private IStreamPlayer player;
    private AudioRecorder recorder;
    private boolean isInitialized = false;
    private boolean isPlayerCreated = false;
    private boolean isStarted = false;
    private boolean isRecording = false;
    // 虚拟人已启动标志（用于延后创建/启动录音器）
    private boolean isAvatarStarted = false;
    // 在虚拟人启动前授予权限时记录为待创建录音器
    private boolean pendingStartRecordAfterStart = false;
    private static final int REQUEST_RECORD_AUDIO = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_simple);
        
        initViews();
        initSpinner();
        setListeners();
        setDefaultValues();
    }

    private void initViews() {
        // SDK版本
        tvVersionShow = findViewById(R.id.tv_version_show);
        
        // 初始化相关
        spinnerLog = findViewById(R.id.spinner_log);
        etAppId = findViewById(R.id.et_app_id);
        etApiKey = findViewById(R.id.et_api_key);
        etApiSecret = findViewById(R.id.et_api_secret);
        tvInitResult = findViewById(R.id.tv_init_result);
        
        // 配置参数
        etAnchorId = findViewById(R.id.et_anchor_id);
        etVcn = findViewById(R.id.et_vcn);
        
        // 控制
        switchNlp = findViewById(R.id.switch_nlp);
        switchAutoAction = findViewById(R.id.switch_auto_action);
        
        // 文本输入
        etInputBox = findViewById(R.id.et_input_box);
        tvSendText = findViewById(R.id.tv_send_text);
        
        // 播放器
        tvCreatePlayer = findViewById(R.id.tv_create_player);
        tvStart = findViewById(R.id.tv_start);
        sbPlayerVolume = findViewById(R.id.sb_player_volume);
        tvOneKeyStart = findViewById(R.id.tv_one_key_start);
        
        // 视频视图
        videoViewAva = findViewById(R.id.video_view_ava);
        
        // 停止按钮
        tvStop = findViewById(R.id.tv_stop);

        // 语音交互
        tvCreateRecorder = findViewById(R.id.tv_create_recorder);
        tvStartRecord = findViewById(R.id.tv_start_record);
        tvStopRecord = findViewById(R.id.tv_stop_record);
        
        // 显示SDK版本
        try {
            String sdkVersion = AvatarPlatform.getVersion();
            tvVersionShow.setText(sdkVersion);
        } catch (Exception e) {
            tvVersionShow.setText("获取版本失败");
            Log.e(TAG, "获取SDK版本失败", e);
        }
    }

    private void initSpinner() {
        String[] logLevels = {"VERBOSE", "DEBUG", "INFO", "WARN", "ERROR"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, logLevels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerLog.setAdapter(adapter);
        spinnerLog.setSelection(2); // 默认INFO
    }
    
    private void setDefaultValues() {
        // 设置默认值（示例，实际使用时需要替换为真实的配置）
        etAppId.setText("your_app_id");
        etApiKey.setText("your_api_key");
        etApiSecret.setText("your_api_secret");
        etAnchorId.setText(AvatarConfig.DEFAULT_ANCHOR_ID);
        etVcn.setText(AvatarConfig.DEFAULT_VCN);
    }

    private void setListeners() {
        // 初始化SDK
        tvInitResult.setOnClickListener(v -> initSDK());
        
        // 创建播放器
        tvCreatePlayer.setOnClickListener(v -> createPlayer());
        
        // 启动虚拟人
        tvStart.setOnClickListener(v -> startAvatar());
        
        // 发送文本
        tvSendText.setOnClickListener(v -> sendText());

        // 语音交互：先校验权限再创建录音器并显示开始/结束控制按钮
        tvCreateRecorder.setOnClickListener(v -> {
            if (!PermissionUtils.hasRequiredPermissions(MainActivitySimple.this)) {
                AvatarUtils.showToast(this, "请先授予麦克风权限");
                PermissionUtils.requestRequiredPermissions(MainActivitySimple.this);
                pendingStartRecordAfterStart = true;
                return;
            }
            createRecorder();
            showAudioInteractionButtons();
        });
        tvStartRecord.setOnClickListener(v -> startRecord());
        tvStopRecord.setOnClickListener(v -> stopRecord());

        // 一键启动：初始化 → 创建播放器 → 启动虚拟人 → 创建录音器并开始录音
        tvOneKeyStart.setOnClickListener(v -> oneKeyStart());
        
        // 播放器音量
        sbPlayerVolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && player != null) {
                    player.setVolume(progress / 100.0);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        
        // 停止虚拟人
        tvStop.setOnClickListener(v -> stopAvatar());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_RECORD_AUDIO) {
            if (grantResults != null && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                AvatarUtils.showToast(this, "权限授予成功");
                Log.d(TAG, "录音权限授予");
                if (isAvatarStarted || isStarted) {
                    try {
                        if (recorder == null) {
                            createRecorder();
                        }
                    } catch (Exception e) {
                        Log.w(TAG, "权限回调中创建录音器失败", e);
                    }
                } else {
                    pendingStartRecordAfterStart = true;
                    Log.d(TAG, "录音权限已授予，待虚拟人启动后创建录音器");
                }
            } else {
                AvatarUtils.showToast(this, "权限被拒绝，语音功能可能不可用");
                Log.w(TAG, "录音权限被拒绝");
            }
        }
    }

    /**
     * 初始化SDK
     */
    private void initSDK() {
        initSDKAndThen(null);
    }

    private void initSDKAndThen(@androidx.annotation.Nullable Runnable onSuccess) {
        String appId = etAppId.getText().toString().trim();
        String apiKey = etApiKey.getText().toString().trim();
        String apiSecret = etApiSecret.getText().toString().trim();

        if (!AvatarConfig.isValidConfig(appId, apiKey, apiSecret)) {
            AvatarUtils.showToast(this, "请填写完整的配置信息（APPID、APIKEY、APISECRET）");
            return;
        }

        boolean success = AvatarUtils.initializeSDK(this, appId, apiKey, apiSecret, 
            new AvatarPlatform.IInitListener() {
                @Override
                public void onResult(String code, String desc) {
                    runOnUiThread(() -> {
                        if (AvatarError.SUCCESS.getErrorCode().equals(code)) {
                            isInitialized = true;
                            tvInitResult.setText("初始化成功");
                            AvatarUtils.showToast(MainActivitySimple.this, "SDK初始化成功");
                            Log.d(TAG, "SDK初始化成功");
                            if (onSuccess != null) {
                                onSuccess.run();
                            }
                        } else {
                            tvInitResult.setText("初始化失败: " + desc);
                            AvatarUtils.showToast(MainActivitySimple.this, "SDK初始化失败: " + desc);
                            Log.e(TAG, "SDK初始化失败: code=" + code + ", desc=" + desc);
                        }
                    });
                }
            });
        
        if (!success) {
            tvInitResult.setText("初始化失败：配置错误");
        }
    }

    /**
     * 创建播放器
     */
    private void createPlayer() {
        if (!isInitialized) {
            AvatarUtils.showToast(this, "请先初始化SDK");
            return;
        }

        // 录音器的创建应延后到虚拟人启动成功后进行，这里仅创建播放器

        IAvatarListener playerListener = new IAvatarListener() {
            @Override
            public void onResult(String dataType, byte[] data, String extra) {
                // 处理播放器结果
            }
            
            @Override
            public void onEvent(String eventType, String extra) {
                runOnUiThread(() -> {
                    if ("first_frame".equals(eventType)) {
                        AvatarUtils.showToast(MainActivitySimple.this, "首帧渲染完成");
                    }
                    Log.d(TAG, "播放器事件: " + eventType);
                });
            }
            
            @Override
            public void onError(String errorCode, String errorDesc, String extra) {
                runOnUiThread(() -> {
                    AvatarUtils.showToast(MainActivitySimple.this, "播放器错误: " + errorDesc);
                    Log.e(TAG, "播放器错误: " + errorDesc);
                });
            }
        };

        player = AvatarUtils.createStreamPlayer(this, videoViewAva, playerListener);
        if (player != null) {
            isPlayerCreated = true;
            AvatarUtils.showToast(this, "播放器创建成功");
        } else {
            AvatarUtils.showToast(this, "播放器创建失败");
        }
    }

    private void showAudioInteractionButtons() {
        try {
            if (etInputBox == null) return;
            etInputBox.setVisibility(View.GONE);
            if (tvSendText != null) tvSendText.setVisibility(View.GONE);

            ViewGroup root = findViewById(android.R.id.content);
            if (root == null) return;

            if (audioOverlayContainer == null) {
                FrameLayout overlay = new FrameLayout(this);
                audioOverlayContainer = overlay;
                FrameLayout.LayoutParams overlayLp = new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
                overlayLp.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
                overlayLp.setMargins(0, 0, 0, (int) (getResources().getDisplayMetrics().density * 120));
                root.addView(audioOverlayContainer, overlayLp);
            } else {
                audioOverlayContainer.removeAllViews();
            }

            android.widget.LinearLayout btnLayout = new android.widget.LinearLayout(this);
            btnLayout.setOrientation(android.widget.LinearLayout.HORIZONTAL);
            btnLayout.setPadding(8, 8, 8, 8);

            if (btnStartRecordCtrl == null) {
                btnStartRecordCtrl = new Button(this);
                btnStartRecordCtrl.setText("开始录音");
                btnStartRecordCtrl.setAllCaps(false);
                btnStartRecordCtrl.setFocusable(true);
                btnStartRecordCtrl.setClickable(true);
                btnStartRecordCtrl.setOnClickListener(x -> {
                    Log.d(TAG, "btnStartRecordCtrl clicked");
                    if (recorder == null) {
                        createRecorder();
                        Toast.makeText(this, "正在创建录音器，请稍候再点击开始", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    startRecord();
                });
            }

            if (btnStopRecordCtrl == null) {
                btnStopRecordCtrl = new Button(this);
                btnStopRecordCtrl.setText("结束录音");
                btnStopRecordCtrl.setAllCaps(false);
                btnStopRecordCtrl.setFocusable(true);
                btnStopRecordCtrl.setClickable(true);
                btnStopRecordCtrl.setOnClickListener(x -> {
                    Log.d(TAG, "btnStopRecordCtrl clicked");
                    if (!isRecording) {
                        Toast.makeText(this, "当前未处于录音中", Toast.LENGTH_SHORT).show();
                    } else {
                        stopRecord();
                    }
                    hideAudioInteractionButtons();
                });
            }

            android.widget.LinearLayout.LayoutParams childLp = new android.widget.LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            int childMargin = (int) (12 * getResources().getDisplayMetrics().density);
            childLp.setMargins(childMargin, 0, childMargin, 0);
            btnStartRecordCtrl.setEnabled(true);
            btnStopRecordCtrl.setEnabled(true);
            btnLayout.addView(btnStartRecordCtrl, childLp);
            btnLayout.addView(btnStopRecordCtrl, childLp);
            audioOverlayContainer.addView(btnLayout);
            btnStartRecordCtrl.setVisibility(View.VISIBLE);
            btnStopRecordCtrl.setVisibility(View.VISIBLE);
        } catch (Throwable t) {
            Log.w(TAG, "showAudioInteractionButtons failed", t);
        }
    }

    private void hideAudioInteractionButtons() {
        try {
            if (btnStartRecordCtrl != null) btnStartRecordCtrl.setVisibility(View.GONE);
            if (btnStopRecordCtrl != null) btnStopRecordCtrl.setVisibility(View.GONE);
            if (etInputBox != null) etInputBox.setVisibility(View.VISIBLE);
            if (tvSendText != null) tvSendText.setVisibility(View.VISIBLE);
        } catch (Throwable t) {
            Log.w(TAG, "hideAudioInteractionButtons failed", t);
        }
    }

    /**
     * 启动虚拟人
     */
    private void startAvatar() {
        if (!isInitialized) {
            AvatarUtils.showToast(this, "请先初始化SDK");
            return;
        }

        if (!isPlayerCreated) {
            AvatarUtils.showToast(this, "请先创建播放器");
            return;
        }

        try {
            AvatarPlayController controller = AvatarUtils.getControllerSafely();
            if (controller == null) {
                AvatarUtils.showToast(this, "获取控制器失败");
                return;
            }

            // 设置全局参数
            String anchorId = etAnchorId.getText().toString().trim();
            String vcn = etVcn.getText().toString().trim();
            controller.setGlobalParams(AvatarUtils.buildDefaultParams(anchorId, vcn));
            
            // 设置播放器
            controller.setStreamPlayer(player);
            
            // 注册监听器
            controller.addAvatarListener(new IAvatarListener() {
                @Override
                public void onResult(String dataType, byte[] data, String extra) {
                    runOnUiThread(() -> {
                        if ("nlp".equals(dataType)) {
                            Log.d(TAG, "语义理解结果: " + new String(data));
                        } else if ("asr".equals(dataType)) {
                            Log.d(TAG, "语音识别结果: " + new String(data));
                        }
                    });
                }
                
                @Override
                public void onEvent(String eventType, String extra) {
                    runOnUiThread(() -> {
                        if ("server_connect_success".equals(eventType)) {
                            AvatarUtils.showToast(MainActivitySimple.this, "服务连接成功");
                        } else if ("stream_start".equals(eventType)) {
                            AvatarUtils.showToast(MainActivitySimple.this, "开始推流");
                        }
                        Log.d(TAG, "事件: " + eventType);
                    });
                }
                
                @Override
                public void onError(String errorCode, String errorDesc, String extra) {
                    runOnUiThread(() -> {
                        AvatarUtils.showToast(MainActivitySimple.this, "错误: " + errorDesc);
                        Log.e(TAG, "虚拟人错误: " + errorDesc);
                    });
                }
            });
            
            controller.start();
            isStarted = true;
            isAvatarStarted = true;
            AvatarUtils.showToast(this, "虚拟人启动成功");

            // 启动后尝试创建录音器（若权限允许则创建，否则请求权限）
            try {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                        == PackageManager.PERMISSION_GRANTED) {
                    createRecorder();
                } else {
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.RECORD_AUDIO},
                            REQUEST_RECORD_AUDIO);
                }
            } catch (Exception e) {
                Log.w(TAG, "启动后创建录音器失败", e);
            }
            
        } catch (Exception e) {
            AvatarUtils.showToast(this, "虚拟人启动失败: " + e.getMessage());
            Log.e(TAG, "虚拟人启动失败", e);
        }
    }

    /**
     * 发送文本
     */
    private void sendText() {
        if (!isStarted) {
            AvatarUtils.showToast(this, "请先启动虚拟人");
            return;
        }

        String text = etInputBox.getText().toString().trim();
        if (TextUtils.isEmpty(text)) {
            AvatarUtils.showToast(this, "请输入文本");
            return;
        }

        try {
            AvatarPlayController controller = AvatarUtils.getControllerSafely();
            if (controller == null) {
                AvatarUtils.showToast(this, "控制器未初始化");
                return;
            }

            boolean isNlpMode = switchNlp.isChecked();
            String vcn = etVcn.getText().toString().trim();
            boolean isInteractive = true; // 默认交互模式
            
            if (isNlpMode) {
                TextParams textParams = AvatarUtils.buildTextParams(text, true, vcn, isInteractive);
                if (switchAutoAction.isChecked()) {
                    // 设置自动动作
                    // 这里可以根据需要添加更多配置
                }
                controller.writeText(text, textParams);
            } else {
                controller.writeText(text);
            }
            
            AvatarUtils.showToast(this, "文本发送成功");
            Log.d(TAG, "发送文本: " + text + ", 模式: " + (isNlpMode ? "交互" : "驱动"));
            
        } catch (Exception e) {
            AvatarUtils.showToast(this, "文本发送失败: " + e.getMessage());
            Log.e(TAG, "文本发送失败", e);
        }
    }

    /**
     * 停止虚拟人
     */
    private void stopAvatar() {
        try {
            AvatarPlayController controller = AvatarUtils.getControllerSafely();
            if (controller != null) {
                AvatarUtils.safeStopController();
                isStarted = false;
                AvatarUtils.showToast(this, "虚拟人已停止 (safe)");
            }
            
            if (player != null) {
                player.stopPlay(true);
                isPlayerCreated = false;
            }
            
        } catch (Exception e) {
            AvatarUtils.showToast(this, "停止失败: " + e.getMessage());
            Log.e(TAG, "停止失败", e);
        }
    }

    /**
     * 一键启动：初始化 → 创建播放器 → 启动虚拟人 → 创建录音器 → 开始录音
     */
    private void oneKeyStart() {
        Runnable flow = () -> {
            runOnUiThread(() -> {
                if (!isPlayerCreated) {
                    createPlayer();
                }
                // 启动虚拟人（内部会设置全局参数并绑定播放器）
                startAvatar();
                // 录音器
                if (recorder == null) {
                    createRecorder();
                }
                // 开始录音
                if (recorder != null && !isRecording) {
                    startRecord();
                }
            });
        };

        if (!isInitialized) {
            initSDKAndThen(flow);
        } else {
            flow.run();
        }
    }

    /**
     * 创建录音器并绑定控制器
     */
    private void createRecorder() {
        if (!isInitialized) {
            AvatarUtils.showToast(this, "请先初始化SDK");
            return;
        }
        try {
            Log.d(TAG, "尝试根据 demo 创建录音器...");
            int audioSource = MediaRecorder.AudioSource.MIC;
            AudioRecorder audioRecorder = new AudioRecorder(
                    audioSource,
                    16000,
                    AudioFormat.ENCODING_PCM_16BIT,
                    AudioFormat.CHANNEL_IN_MONO
            );
            this.recorder = audioRecorder;

            if (player != null) {
                try {
                    StreamPlayerParams params = player.getPlayerParams();
                    if (params != null) {
                        params.setAudioSessionId(audioRecorder.getAudioSessionId());
                        player.setPlayerParams(params);
                    }
                } catch (Throwable t) {
                    Log.w(TAG, "注入 audioSessionId 到播放器失败", t);
                }
            }

            AudioParams audioParams = new AudioParams();
            try { audioParams.setNlp(true); } catch (Throwable ignored) {}
            try {
                AvatarParams.ASR asr = new AvatarParams.ASR();
                asr.setFullDuplex(0);
                audioParams.setASR(asr);
            } catch (Throwable ignored) {}

            AvatarPlayController controller = AvatarUtils.getControllerSafely();
            if (controller != null) {
                try {
                    controller.setAudioRecorder(audioRecorder, audioParams);
                    AvatarUtils.showToast(this, "录音器创建成功");
                    Log.d(TAG, "录音器创建并设置到控制器");
                } catch (Throwable t) {
                    Log.e(TAG, "将录音器设置到控制器失败", t);
                    AvatarUtils.showToast(this, "设置录音器失败: " + t.getMessage());
                }
            } else {
                AvatarUtils.showToast(this, "录音器已创建（待控制器就绪绑定）");
                Log.w(TAG, "控制器未就绪，录音器已创建但未绑定");
            }
        } catch (Exception e) {
            AvatarUtils.showToast(this, "录音器创建异常: " + e.getMessage());
            Log.e(TAG, "录音器创建异常", e);
        }
    }

    /**
     * 开始录音
     */
    private void startRecord() {
        if (recorder == null) {
            AvatarUtils.showToast(this, "请先创建录音器");
            return;
        }
        try {
            recorder.startRecord();
            isRecording = true;
            AvatarUtils.showToast(this, "开始录音");
            Log.d(TAG, "开始录音");
        } catch (Exception e) {
            AvatarUtils.showToast(this, "开始录音失败: " + e.getMessage());
            Log.e(TAG, "开始录音失败", e);
        }
    }

    /**
     * 停止录音
     */
    private void stopRecord() {
        if (recorder == null) {
            AvatarUtils.showToast(this, "未创建录音器");
            return;
        }
        try {
            if (isRecording) {
                recorder.stopRecord();
                isRecording = false;
                AvatarUtils.showToast(this, "停止录音");
                Log.d(TAG, "停止录音");
            } else {
                AvatarUtils.showToast(this, "录音器未运行");
            }
        } catch (Exception e) {
            AvatarUtils.showToast(this, "停止录音失败: " + e.getMessage());
            Log.e(TAG, "停止录音失败", e);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isRecording && recorder != null) {
            try {
                recorder.stopRecord();
            } catch (Exception ignored) {}
        }
        recorder = null;
        stopAvatar();
    }
}