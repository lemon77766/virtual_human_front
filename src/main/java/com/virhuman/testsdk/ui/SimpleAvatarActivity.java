package com.virhuman.testsdk.ui;

import android.media.AudioFormat;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.iflytek.avalibrary.AvatarPlatform;
import com.iflytek.avalibrary.AvatarPlayController;
import com.iflytek.avalibrary.IAvatarListener;
import com.iflytek.avalibrary.constant.AvatarError;
import com.iflytek.avalibrary.params.AudioParams;
import com.iflytek.avalibrary.params.AvatarParams;
import com.iflytek.avalibrary.params.TextParams;
import com.iflytek.avalibrary.videoplayer.params.StreamPlayerParams;
import com.iflytek.avalibrary.videoplayer.rt.interfaces.IStreamPlayer;
import com.iflytek.ys.core.recorder.AudioRecorder;
import com.virhuman.testsdk.R;
import com.virhuman.testsdk.config.AvatarConfig;
import com.virhuman.testsdk.utils.AvatarUtils;
import com.virhuman.testsdk.utils.PermissionUtils;

/**
 * 简化版虚拟人Activity - 一键启动，支持文字和语音交互
 */
public class SimpleAvatarActivity extends AppCompatActivity {
    private static final String TAG = "SimpleAvatarActivity";

    // UI控件
    private FrameLayout videoViewAva;
    private Button btnStartAvatar;
    private Button btnStopAvatar;
    private EditText etInputText;
    private Button btnSendText;
    private ImageButton btnVoiceRecord;
    private TextView tvStatus;
    
    // SDK对象
    private IStreamPlayer player;
    private AudioRecorder recorder;
    // programmatic start/stop controls for explicit audio flow
    private Button btnStartRecordCtrl;
    private Button btnStopRecordCtrl;
    // overlay container for audio control buttons
    private ViewGroup audioOverlayContainer;
    
    // 状态标志
    private boolean isInitialized = false;
    private boolean isPlayerCreated = false;
    private boolean isStarted = false;
    private boolean isRecording = false;
    private boolean isStarting = false; // 防止重复点击
    // 虚拟人已启动标志（用于延后创建/启动录音器）
    private boolean isAvatarStarted = false;
    // 在虚拟人启动前授予权限时，记录为待创建录音器
    private boolean pendingStartRecordAfterStart = false;
    // Handler & flags for press-and-hold recording UX
    private final Handler recordHandler = new Handler(Looper.getMainLooper());
    private Runnable delayedStartRunnable;
    private long recordPressStartTime = 0L;
    // 最短按住时长阈值，低于此值按下视为误触（ms）
    private static final long MIN_PRESS_TO_START_RECORD_MS = 250L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simple_avatar);
        
        // 检查权限
        if (!PermissionUtils.hasRequiredPermissions(this)) {
            PermissionUtils.requestRequiredPermissions(this);
        }
        
        initViews();
        setListeners();
    }

    private void initViews() {
        videoViewAva = findViewById(R.id.video_view_ava);
        btnStartAvatar = findViewById(R.id.btn_start_avatar);
        btnStopAvatar = findViewById(R.id.btn_stop_avatar);
        etInputText = findViewById(R.id.et_input_text);
        btnSendText = findViewById(R.id.btn_send_text);
        btnVoiceRecord = findViewById(R.id.btn_voice_record);
        tvStatus = findViewById(R.id.tv_status);
        
        // 初始状态
        updateUIState();
    }

    private void setListeners() {
        // 一键启动虚拟人
        btnStartAvatar.setOnClickListener(v -> {
            if (isStarting) {
                Toast.makeText(this, "正在启动中，请稍候...", Toast.LENGTH_SHORT).show();
                return;
            }
            oneKeyStartAvatar();
        });
        
        // 停止虚拟人
        btnStopAvatar.setOnClickListener(v -> stopAvatar());
        
        // 发送文字
        btnSendText.setOnClickListener(v -> sendText());
        
        // 点击录音按钮：先校验权限，再创建录音器并显示开始/结束录音控件（显式交互）
        btnVoiceRecord.setOnClickListener(v -> {
            try {
                if (!PermissionUtils.hasRequiredPermissions(SimpleAvatarActivity.this)) {
                    Toast.makeText(SimpleAvatarActivity.this, "请先授予麦克风权限", Toast.LENGTH_SHORT).show();
                    PermissionUtils.requestRequiredPermissions(SimpleAvatarActivity.this);
                    pendingStartRecordAfterStart = true;
                    return;
                }

                if (recorder == null) {
                    createRecorder();
                }
                showAudioInteractionButtons();
            } catch (Exception e) {
                Log.w(TAG, "voice button click failed", e);
                Toast.makeText(SimpleAvatarActivity.this, "创建录音器失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showAudioInteractionButtons() {
        try {
            if (etInputText == null) return;
            // hide input
            etInputText.setVisibility(View.GONE);
            if (btnSendText != null) btnSendText.setVisibility(View.GONE);

            ViewGroup root = findViewById(android.R.id.content);
            if (root == null) {
                Log.w(TAG, "showAudioInteractionButtons: root not found");
                return;
            }

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
            int pad = (int) (8 * getResources().getDisplayMetrics().density);
            btnLayout.setPadding(pad, pad, pad, pad);

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
                    startVoiceRecord();
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
                        Toast.makeText(this, "当前未处于录音状态", Toast.LENGTH_SHORT).show();
                    } else {
                        stopVoiceRecord();
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
            if (etInputText != null) etInputText.setVisibility(View.VISIBLE);
            if (btnSendText != null) btnSendText.setVisibility(View.VISIBLE);
        } catch (Throwable t) {
            Log.w(TAG, "hideAudioInteractionButtons failed", t);
        }
    }

    /**
     * 一键启动虚拟人
     * 自动完成：初始化SDK -> 创建播放器 -> 设置参数 -> 启动虚拟人 -> 创建录音器
     */
    private void oneKeyStartAvatar() {
        if (isStarted) {
            Toast.makeText(this, "虚拟人已启动", Toast.LENGTH_SHORT).show();
            return;
        }
        
        isStarting = true;
        updateStatus("正在启动虚拟人...");
        btnStartAvatar.setEnabled(false);
        
        // 检查权限
        if (!PermissionUtils.hasRequiredPermissions(this)) {
            updateStatus("请先授予必要权限");
            Toast.makeText(this, "请先授予必要权限", Toast.LENGTH_SHORT).show();
            PermissionUtils.requestRequiredPermissions(this);
            isStarting = false;
            btnStartAvatar.setEnabled(true);
            return;
        }
        
        // 从配置中读取APP_ID等信息（这里使用默认值，实际应该从配置文件或SharedPreferences读取）
        String appId = AvatarConfig.DEFAULT_APP_ID;
        String apiKey = AvatarConfig.DEFAULT_API_KEY;
        String apiSecret = AvatarConfig.DEFAULT_API_SECRET;
        
        // 如果配置为空，提示用户
        if (TextUtils.isEmpty(appId) || TextUtils.isEmpty(apiKey) || TextUtils.isEmpty(apiSecret)) {
            updateStatus("请先配置APP_ID、API_KEY和API_SECRET");
            Toast.makeText(this, "请在AvatarConfig中配置APP_ID、API_KEY和API_SECRET", Toast.LENGTH_LONG).show();
            isStarting = false;
            btnStartAvatar.setEnabled(true);
            return;
        }
        
        // 步骤1: 初始化SDK
        updateStatus("正在初始化SDK...");
        AvatarUtils.initializeSDK(this, appId, apiKey, apiSecret, new AvatarPlatform.IInitListener() {
            @Override
            public void onResult(String code, String desc) {
                runOnUiThread(() -> {
                    if (AvatarError.SUCCESS.getErrorCode().equals(code)) {
                        isInitialized = true;
                        Log.d(TAG, "SDK初始化成功");
                        // 步骤2: 创建播放器
                        createPlayerAndStart();
                    } else {
                        updateStatus("SDK初始化失败: " + desc);
                        Toast.makeText(SimpleAvatarActivity.this, "SDK初始化失败: " + desc, Toast.LENGTH_SHORT).show();
                        isStarting = false;
                        btnStartAvatar.setEnabled(true);
                    }
                });
            }
        });
    }

    /**
     * 创建播放器并启动虚拟人
     */
    private void createPlayerAndStart() {
        updateStatus("正在创建播放器...");
        
        // 创建播放器监听器
        IAvatarListener playerListener = new IAvatarListener() {
            @Override
            public void onResult(String dataType, byte[] data, String extra) {}
            
            @Override
            public void onEvent(String eventType, String extra) {
                runOnUiThread(() -> {
                    if ("first_frame".equals(eventType)) {
                        updateStatus("虚拟人已就绪");
                        Toast.makeText(SimpleAvatarActivity.this, "虚拟人启动成功", Toast.LENGTH_SHORT).show();
                    }
                    Log.d(TAG, "播放器事件: " + eventType);
                });
            }
            
            @Override
            public void onError(String errorCode, String errorDesc, String extra) {
                runOnUiThread(() -> {
                    updateStatus("播放器错误: " + errorDesc);
                    Log.e(TAG, "播放器错误: " + errorDesc);
                });
            }
        };
        
        // 创建播放器
        player = AvatarUtils.createStreamPlayer(this, videoViewAva, playerListener);
        if (player == null) {
            updateStatus("播放器创建失败");
            Toast.makeText(this, "播放器创建失败", Toast.LENGTH_SHORT).show();
            isStarting = false;
            btnStartAvatar.setEnabled(true);
            return;
        }
        
        isPlayerCreated = true;
        Log.d(TAG, "播放器创建成功");
        
        // 步骤3: 设置参数并启动虚拟人
        startAvatarWithParams();
    }

    /**
     * 设置参数并启动虚拟人
     */
    private void startAvatarWithParams() {
        updateStatus("正在启动虚拟人...");
        
        try {
            AvatarPlayController controller = AvatarUtils.getControllerSafely();
            if (controller == null) {
                updateStatus("获取控制器失败");
                Toast.makeText(this, "获取控制器失败", Toast.LENGTH_SHORT).show();
                isStarting = false;
                btnStartAvatar.setEnabled(true);
                return;
            }
            
            // 设置全局参数（使用默认配置）
            String anchorId = AvatarConfig.DEFAULT_ANCHOR_ID;
            String vcn = AvatarConfig.DEFAULT_VCN;
            AvatarParams params = AvatarUtils.buildDefaultParams(anchorId, vcn);
            
            // 验证关键参数
            if (params.getScene() != null && params.getScene().getSceneId() != null) {
                Log.d(TAG, "✓ sceneId已设置: " + params.getScene().getSceneId());
            } else {
                Log.e(TAG, "✗ 错误：sceneId未设置！这会导致语音交互失败（错误码10107）");
            }
            
            if (params.getDispatch() != null) {
                Log.d(TAG, "✓ Dispatch参数 - contentAnalysis: " + params.getDispatch().getContentAnalysis() + 
                      ", interactiveMode: " + params.getDispatch().getInteractiveMode());
            } else {
                Log.w(TAG, "警告：Dispatch参数未设置");
            }
            
            controller.setGlobalParams(params);
            Log.d(TAG, "全局参数已设置 - avatarId: " + anchorId + ", vcn: " + vcn);
            
            // 设置播放器
            controller.setStreamPlayer(player);
            
            // 注册虚拟人监听器
            controller.addAvatarListener(new IAvatarListener() {
                @Override
                public void onResult(String dataType, byte[] data, String extra) {
                    runOnUiThread(() -> {
                        // 添加详细日志，记录所有回调
                        Log.d(TAG, "收到回调 - dataType: " + dataType + ", data长度: " + (data != null ? data.length : 0) + ", extra: " + extra);
                        
                        if (data == null || data.length == 0) {
                            Log.w(TAG, "警告：收到空数据，dataType: " + dataType);
                            return;
                        }
                        
                        try {
                            String result = new String(data, "UTF-8");
                            
                            if ("nlp".equals(dataType)) {
                                Log.d(TAG, "✓✓✓ 收到语义理解结果: " + result);
                                updateStatus("收到语义理解结果: " + result);
                                Toast.makeText(SimpleAvatarActivity.this, "语义理解: " + result, Toast.LENGTH_LONG).show();
                            } else if ("asr".equals(dataType)) {
                                Log.d(TAG, "✓✓✓ 收到语音识别结果: " + result);
                                updateStatus("语音识别: " + result);
                                Toast.makeText(SimpleAvatarActivity.this, "识别: " + result, Toast.LENGTH_LONG).show();
                            } else {
                                // 记录其他类型的数据，可能包含重要信息
                                Log.d(TAG, "收到其他数据类型: " + dataType + ", 内容: " + result);
                                // 如果是tts结果，也显示
                                if ("tts".equals(dataType)) {
                                    Log.d(TAG, "收到TTS结果: " + result);
                                }
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "处理回调数据失败", e);
                        }
                    });
                }
                
                @Override
                public void onEvent(String eventType, String extra) {
                    runOnUiThread(() -> {
                        Log.d(TAG, "虚拟人事件: " + eventType + ", extra: " + extra);
                        if ("server_connect_success".equals(eventType)) {
                            updateStatus("连接服务器成功");
                        } else if ("stream_start".equals(eventType)) {
                            updateStatus("开始推流");
                        } else if ("frame_start".equals(eventType)) {
                            updateStatus("开始播报");
                        } else if ("frame_end".equals(eventType)) {
                            updateStatus("结束播报");
                        }
                    });
                }
                
                @Override
                public void onError(String errorCode, String errorDesc, String extra) {
                    runOnUiThread(() -> {
                        updateStatus("错误: " + errorDesc);
                        Log.e(TAG, "✗✗✗ 虚拟人错误 - errorCode: " + errorCode + ", errorDesc: " + errorDesc + ", extra: " + extra);
                        
                        // 特殊处理常见错误码
                        if ("10107".equals(errorCode)) {
                            Log.e(TAG, "错误码10107通常表示ASR配置错误，请检查sceneId是否正确设置");
                            Toast.makeText(SimpleAvatarActivity.this, "ASR配置错误，请检查sceneId", Toast.LENGTH_LONG).show();
                        } else if ("10108".equals(errorCode)) {
                            Log.e(TAG, "错误码10108通常表示TDP配置错误，请检查sceneId是否正确设置");
                            Toast.makeText(SimpleAvatarActivity.this, "TDP配置错误，请检查sceneId", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(SimpleAvatarActivity.this, "错误: " + errorDesc, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            });
            
            // 启动虚拟人
            controller.start();
            isStarted = true;
            isAvatarStarted = true;

            // 启动后尝试创建录音器：有权限则创建，否则请求权限；处理此前的 pending 标志
            try {
                if (PermissionUtils.hasRequiredPermissions(this)) {
                    createRecorder();
                } else {
                    PermissionUtils.requestRequiredPermissions(this);
                }
            } catch (Exception e) {
                Log.w(TAG, "启动后创建录音器失败", e);
            }

            try {
                if (pendingStartRecordAfterStart) {
                    if (PermissionUtils.hasRequiredPermissions(this)) {
                        if (recorder == null) {
                            createRecorder();
                        }
                        pendingStartRecordAfterStart = false;
                        Log.d(TAG, "检测到待创建录音请求，已在虚拟人启动后创建录音器");
                    } else {
                        Log.d(TAG, "待创建录音请求存在但权限未授予，继续等待权限回调");
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, "处理 pendingStartRecordAfterStart 时发生异常", e);
            }

            updateStatus("虚拟人已启动，可以开始交互");
            updateUIState();
            isStarting = false;
            btnStartAvatar.setEnabled(true);
            
            Log.d(TAG, "虚拟人启动完成，监听器已注册，录音器已设置（如有权限）");
            
        } catch (Exception e) {
            updateStatus("启动失败: " + e.getMessage());
            Toast.makeText(this, "启动失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e(TAG, "启动虚拟人失败", e);
            isStarting = false;
            btnStartAvatar.setEnabled(true);
        }
    }

    /**
     * 创建录音器
     */
    private void createRecorder() {
        try {
            if (!isInitialized) {
                Log.w(TAG, "SDK未初始化，跳过录音器创建");
                return;
            }
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
                        Log.d(TAG, "已注入 audioSessionId 到播放器: " + audioRecorder.getAudioSessionId());
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
                    Log.d(TAG, "录音器已设置到控制器（含 audioParams）");
                    Toast.makeText(this, "录音器创建成功", Toast.LENGTH_SHORT).show();
                } catch (Throwable t) {
                    Log.e(TAG, "设置录音器到控制器失败", t);
                    Toast.makeText(this, "设置录音器失败: " + t.getMessage(), Toast.LENGTH_LONG).show();
                }
            } else {
                Log.w(TAG, "控制器未就绪，录音器已创建但未绑定到控制器");
                Toast.makeText(this, "录音器已创建（待控制器就绪绑定）", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "创建录音器失败", e);
        }
    }

    /**
     * 发送文字
     */
    private void sendText() {
        if (!isStarted) {
            Toast.makeText(this, "请先启动虚拟人", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String text = etInputText.getText().toString().trim();
        if (TextUtils.isEmpty(text)) {
            Toast.makeText(this, "请输入要发送的文字", Toast.LENGTH_SHORT).show();
            return;
        }
        
        try {
            AvatarPlayController controller = AvatarUtils.getControllerSafely();
            if (controller == null) {
                Toast.makeText(this, "控制器未初始化", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // 使用交互模式（启用语义理解）
            TextParams textParams = AvatarUtils.buildTextParams(text, true, AvatarConfig.DEFAULT_VCN, true);
            controller.writeText(text, textParams);
            
            // 清空输入框
            etInputText.setText("");
            Toast.makeText(this, "文字已发送", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "发送文字: " + text);
            
        } catch (Exception e) {
            Toast.makeText(this, "发送失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e(TAG, "发送文字失败", e);
        }
    }

    /**
     * 开始语音录音
     */
    private void startVoiceRecord() {
        if (!isStarted) {
            Toast.makeText(this, "请先启动虚拟人", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (recorder == null) {
            Toast.makeText(this, "录音器未创建", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (isRecording) {
            return;
        }
        
        try {
            // 验证录音器和控制器状态
            AvatarPlayController controller = AvatarUtils.getControllerSafely();
            if (controller == null) {
                Toast.makeText(this, "控制器未初始化，无法录音", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "错误：控制器为null，无法开始录音");
                return;
            }
            
            if (recorder == null) {
                Toast.makeText(this, "录音器未创建，请重新启动虚拟人", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "错误：录音器为null，无法开始录音");
                return;
            }
            
            recorder.startRecord();
            isRecording = true;
            btnVoiceRecord.setSelected(true);
            updateStatus("正在录音，请说话...");
            Toast.makeText(this, "开始录音，请说话", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "✓ 开始录音 - 音频数据将自动发送并启用NLP语义理解");
            Log.d(TAG, "录音器状态 - 已设置到控制器: " + (controller != null));
        } catch (Exception e) {
            Toast.makeText(this, "开始录音失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e(TAG, "开始录音失败", e);
        }
    }

    /**
     * 停止语音录音
     */
    private void stopVoiceRecord() {
        if (!isRecording || recorder == null) {
            return;
        }
        
        try {
            recorder.stopRecord();
            isRecording = false;
            btnVoiceRecord.setSelected(false);
            updateStatus("录音已停止，等待识别结果...");
            Toast.makeText(this, "停止录音，正在处理...", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "✓ 停止录音 - 等待ASR和NLP处理结果");
            
            // 检查控制器和录音器状态
            AvatarPlayController controller = AvatarUtils.getControllerSafely();
            if (controller != null) {
                Log.d(TAG, "✓ 控制器状态正常，录音器已设置");
            } else {
                Log.e(TAG, "✗ 错误：控制器为null，无法处理音频数据");
            }
            
            if (recorder != null) {
                Log.d(TAG, "✓ 录音器状态正常");
            } else {
                Log.e(TAG, "✗ 错误：录音器为null");
            }
            
            // 提示：如果长时间没有收到结果，可能是：
            // 1. sceneId未正确设置（检查日志中的sceneId设置）
            // 2. 后端服务配置问题（ASR/NLP服务未启用）
            // 3. 网络问题
            // 4. 音频数据格式问题
            Log.d(TAG, "提示：如果30秒内未收到ASR/NLP结果，请检查：");
            Log.d(TAG, "  1. sceneId是否正确设置（应在启动日志中看到）");
            Log.d(TAG, "  2. 后端服务是否已启用ASR和NLP");
            Log.d(TAG, "  3. 网络连接是否正常");
        } catch (Exception e) {
            Toast.makeText(this, "停止录音失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e(TAG, "停止录音失败", e);
        }
    }

    /**
     * 停止虚拟人
     */
    private void stopAvatar() {
        try {
            // 停止录音
            if (isRecording && recorder != null) {
                recorder.stopRecord();
                isRecording = false;
            }
            
            // 停止虚拟人（使用安全停止以避免 SDK 内部异常导致应用崩溃）
            AvatarUtils.safeStopController();
            
            // 停止播放器
            if (player != null) {
                player.stopPlay(true);
            }
            
            isStarted = false;
            isPlayerCreated = false;
            updateStatus("虚拟人已停止");
            updateUIState();
            Toast.makeText(this, "虚拟人已停止", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "虚拟人已停止");
            
        } catch (Exception e) {
            Toast.makeText(this, "停止失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e(TAG, "停止失败", e);
        }
    }

    /**
     * 更新UI状态
     */
    private void updateUIState() {
        btnStartAvatar.setEnabled(!isStarted && !isStarting);
        btnStopAvatar.setEnabled(isStarted);
        btnSendText.setEnabled(isStarted);
        btnVoiceRecord.setEnabled(isStarted);
        // 启用/禁用输入框
        etInputText.setEnabled(isStarted);
        etInputText.setFocusable(isStarted);
        etInputText.setFocusableInTouchMode(isStarted);
    }

    /**
     * 更新状态文本
     */
    private void updateStatus(String status) {
        if (tvStatus != null) {
            tvStatus.setText(status);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == PermissionUtils.PERMISSION_REQUEST_CODE) {
            if (PermissionUtils.isPermissionGranted(grantResults)) {
                Toast.makeText(this, "权限授予成功", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "权限授予成功");
                if (isStarted) {
                    try {
                        if (recorder == null) {
                            createRecorder();
                        }
                    } catch (Exception e) {
                        Log.w(TAG, "权限回调中创建录音器失败", e);
                    }
                } else {
                    pendingStartRecordAfterStart = true;
                    Log.d(TAG, "权限已授予，待虚拟人启动后创建录音器");
                }
            } else {
                Toast.makeText(this, "权限被拒绝，某些功能可能无法使用", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopAvatar();
        
        // 清理资源
        if (recorder != null) {
            try {
                if (isRecording) {
                    recorder.stopRecord();
                }
            } catch (Exception ignored) {}
            recorder = null;
        }
        
        if (player != null) {
            try {
                player.stopPlay(true);
            } catch (Exception ignored) {}
            player = null;
        }
    }
}

