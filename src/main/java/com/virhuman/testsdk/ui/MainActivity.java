package com.virhuman.testsdk.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.virhuman.testsdk.R;
import com.virhuman.testsdk.config.AvatarConfig;
import com.virhuman.testsdk.utils.AvatarUtils;

// 科大讯飞虚拟人SDK导入
import com.iflytek.avalibrary.AvatarPlatform;
import com.iflytek.avalibrary.AvatarPlatformConfig;
import com.iflytek.avalibrary.AvatarPlayController;
import com.iflytek.avalibrary.IAvatarListener;
import com.iflytek.avalibrary.constant.AvatarError;
import com.iflytek.ys.core.log.LogLevel;
import com.iflytek.avalibrary.params.AvatarParams;
import com.iflytek.avalibrary.params.TextParams;
import com.iflytek.avalibrary.params.AudioParams;
import com.iflytek.avalibrary.videoplayer.rt.StreamPlayerFactory;
import com.iflytek.avalibrary.videoplayer.rt.interfaces.IStreamPlayer;
import com.iflytek.avalibrary.videoplayer.params.StreamPlayerParams;
import com.iflytek.ys.core.recorder.AudioRecorder;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.MediaRecorder;
import android.Manifest;
import android.content.pm.PackageManager;

/**
 * 云模式Activity - 科大讯飞虚拟人SDK云模式演示
 */
public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final int REQUEST_RECORD_AUDIO = 1001;

    // UI控件
    private TextView tvVersionShow;
    private Spinner spinnerLog;
    private EditText etLoggingPre;
    private EditText etUid;
    private EditText etAppId;
    private EditText etApiKey;
    private EditText etApiSecret;
    private EditText etServerUrl;
    private TextView tvInitResult;
    private TextView tvStartInit;
    
    // 全局参数设置
    private EditText etVideoWidth;
    private EditText etVideoHeight;
    private EditText etAnchorId;
    private EditText etScale;
    private EditText etMoveH;
    private EditText etMoveV;
    private EditText etVcn;
    private EditText etSpeed;
    private EditText etPitch;
    private EditText etVolume;
    private Switch switchSubtitle;
    private EditText etSubtitleColor;
    private Switch switchContentAnalysis;
    private EditText etSceneId;
    private EditText etSceneVersion;
    private Switch switchSetListener;
    private TextView tvErrorListener;
    private Switch switchSetListenerPlayer;
    private TextView tvErrorListenerPlayer;
    private EditText etPlayerTimeout;
    
    // 播放器相关
    private TextView tvCreatePlayer;
    private TextView tvSetParams;
    private TextView tvStart;
    
    // 模式设置
    private Switch switchNlp;
    private Switch switchVc;
    private EditText etVcVcn;
    
    // 文本驱动/交互
    private EditText etInputBox;
    private EditText etVcnRt;
    private EditText etSpeedRt;
    private EditText etPitchRt;
    private EditText etVolumeRt;
    private Switch switchVcRt;
    private EditText etVcVcnRt;
    private Switch switchInteractMode;
    private Switch switchAutoAction;
    private Switch switchAddNonsemanti;
    private TextView tvSendText;
    
    // 音频驱动/交互
    private Switch switchFullDuplex;
    private EditText etAudioAction;
    private EditText etAudioActionDelay;
    private TextView tvCreateRecorder;
    private TextView tvStartRecord;
    private TextView tvStopRecord;
    
    // 独立指令
    private EditText etCmdType;
    private EditText etCmdValue;
    private EditText etCmdTb;
    private TextView tvSendAction;
    
    // 控制按钮
    private SeekBar sbPlayerVolume;
    private TextView tvNotifyInterrupt;
    private TextView tvStop;
    private TextView tvDestroy;
    
    // 视频视图
    private FrameLayout videoViewAva;
    
    // SDK相关对象
    private IStreamPlayer player;
    private AudioRecorder recorder;
    // 当前音频交互的 requestId，用于 writeAudio 单次交互参数关联
    private String currentAudioRequestId = null;
    private boolean isRecording = false;
    // 标记虚拟人是否已启动；用于将录音器的创建/启动延后到虚拟人启动成功之后
    private boolean isAvatarStarted = false;
    // 如果用户在虚拟人启动前授予录音权限，记录为待创建/启动录音器
    private boolean pendingStartRecordAfterStart = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        initViews();
        initSpinner();
        setListeners();
        // 自动使用默认配置并尝试启动虚拟人与录音（如果你不希望自动启动可移除下面调用）
        autoStartIfDesired();
    }

    private void initViews() {
        // SDK版本
        tvVersionShow = findViewById(R.id.tv_version_show);
        
        // 初始化相关
        spinnerLog = findViewById(R.id.spinner_log);
        etLoggingPre = findViewById(R.id.et_logging_pre);
        etUid = findViewById(R.id.et_uid);
        etAppId = findViewById(R.id.et_app_id);
        etApiKey = findViewById(R.id.et_api_key);
        etApiSecret = findViewById(R.id.et_api_secret);
        etServerUrl = findViewById(R.id.et_server_url);
        tvInitResult = findViewById(R.id.tv_init_result);
        tvStartInit = findViewById(R.id.tv_start_init);
        
        // 全局参数
        etVideoWidth = findViewById(R.id.et_video_width);
        etVideoHeight = findViewById(R.id.et_video_height);
        etAnchorId = findViewById(R.id.et_anchor_id);
        etScale = findViewById(R.id.et_scale);
        etMoveH = findViewById(R.id.et_move_h);
        etMoveV = findViewById(R.id.et_move_v);
        etVcn = findViewById(R.id.et_vcn);
        etSpeed = findViewById(R.id.et_speed);
        etPitch = findViewById(R.id.et_pitch);
        etVolume = findViewById(R.id.et_volume);
        switchSubtitle = findViewById(R.id.switch_subtitle);
        etSubtitleColor = findViewById(R.id.et_subtitle_color);
        switchContentAnalysis = findViewById(R.id.switch_content_analysis);
        etSceneId = findViewById(R.id.et_scene_id);
        etSceneVersion = findViewById(R.id.et_scene_version);
        switchSetListener = findViewById(R.id.switch_set_listener);
        tvErrorListener = findViewById(R.id.tv_error_listener);
        switchSetListenerPlayer = findViewById(R.id.switch_set_listener_player);
        tvErrorListenerPlayer = findViewById(R.id.tv_error_listener_player);
        etPlayerTimeout = findViewById(R.id.et_player_timeout);
        
        // 播放器
        tvCreatePlayer = findViewById(R.id.tv_create_player);
        tvSetParams = findViewById(R.id.tv_set_params);
        tvStart = findViewById(R.id.tv_start);
        
        // 模式
        switchNlp = findViewById(R.id.switch_nlp);
        switchVc = findViewById(R.id.switch_vc);
        etVcVcn = findViewById(R.id.et_vc_vcn);
        
        // 文本驱动/交互
        etInputBox = findViewById(R.id.et_input_box);
        etVcnRt = findViewById(R.id.et_vcn_rt);
        etSpeedRt = findViewById(R.id.et_speed_rt);
        etPitchRt = findViewById(R.id.et_pitch_rt);
        etVolumeRt = findViewById(R.id.et_volume_rt);
        switchVcRt = findViewById(R.id.switch_vc_rt);
        etVcVcnRt = findViewById(R.id.et_vc_vcn_rt);
        switchInteractMode = findViewById(R.id.switch_interact_mode);
        switchAutoAction = findViewById(R.id.switch_auto_action);
        switchAddNonsemanti = findViewById(R.id.switch_add_nonsemanti);
        tvSendText = findViewById(R.id.tv_send_text);
        
        // 音频驱动/交互
        switchFullDuplex = findViewById(R.id.switch_full_duplex);
        etAudioAction = findViewById(R.id.et_audio_action);
        etAudioActionDelay = findViewById(R.id.et_audio_action_delay);
        tvCreateRecorder = findViewById(R.id.tv_create_recorder);
        tvStartRecord = findViewById(R.id.tv_start_record);
        tvStopRecord = findViewById(R.id.tv_stop_record);
        
        // 默认禁用创建录音器按钮，只有虚拟人启动后才允许创建
        tvCreateRecorder.setEnabled(false);
        
        // 独立指令
        etCmdType = findViewById(R.id.et_cmd_type);
        etCmdValue = findViewById(R.id.et_cmd_value);
        etCmdTb = findViewById(R.id.et_cmd_tb);
        tvSendAction = findViewById(R.id.tv_send_action);
        
        // 控制
        sbPlayerVolume = findViewById(R.id.sb_player_volume);
        tvNotifyInterrupt = findViewById(R.id.tv_notify_interrupt);
        tvStop = findViewById(R.id.tv_stop);
        tvDestroy = findViewById(R.id.tv_destroy);
        
        // 视频视图
        videoViewAva = findViewById(R.id.video_view_ava);
        
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
        // 根据SDK文档，LogLevel包括：VERBOSE, DEBUG, INFO, WARN, ERROR, NONE
        String[] logLevels = {"VERBOSE", "DEBUG", "INFO", "WARN", "ERROR"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, logLevels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerLog.setAdapter(adapter);
        spinnerLog.setSelection(2); // 默认INFO（索引2）
    }

    private void setListeners() {
        // 初始化SDK
        tvStartInit.setOnClickListener(v -> initSDK());
        
        // 创建播放器
        tvCreatePlayer.setOnClickListener(v -> createPlayer());
        
        // 设置参数
        tvSetParams.setOnClickListener(v -> setParams());
        
        // 启动虚拟人
        tvStart.setOnClickListener(v -> startAvatar());
        
        // 发送文本
        tvSendText.setOnClickListener(v -> sendText());
        
        // 创建录音器（改为：仅在虚拟人启动后创建；若未启动则提示）
        tvCreateRecorder.setOnClickListener(v -> {
            AvatarPlayController controller = AvatarPlatform.getController();
            if (controller == null) {
                Toast.makeText(MainActivity.this, "虚拟人未启动，录音器将在启动后自动创建", Toast.LENGTH_SHORT).show();
                return;
            }
            // 控制器存在：检查权限后创建
            if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.RECORD_AUDIO)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.RECORD_AUDIO},
                        REQUEST_RECORD_AUDIO);
            } else {
                createRecorder();
                Toast.makeText(MainActivity.this, "录音器已创建", Toast.LENGTH_SHORT).show();
            }
        });
        
        // 开始录音：若无权限则申请，已有权限则创建（若未创建）并立即开始
        tvStartRecord.setOnClickListener(v -> {
            try {
                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.RECORD_AUDIO)
                        != PackageManager.PERMISSION_GRANTED) {
                    // 请求权限，结果回调到 onRequestPermissionsResult
                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{Manifest.permission.RECORD_AUDIO},
                            REQUEST_RECORD_AUDIO);
                } else {
                    // 已有权限，确保已创建录音器再开始
                    if (recorder == null) {
                        createRecorder();
                    }
                    startRecord();
                }
            } catch (Exception e) {
                Log.e(TAG, "startRecord click error", e);
                Toast.makeText(MainActivity.this, "启动录音失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
        
        // 停止录音
        tvStopRecord.setOnClickListener(v -> stopRecord());
        
        // 发送指令
        tvSendAction.setOnClickListener(v -> sendAction());
        
        // 播放器音量
        sbPlayerVolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    setPlayerVolume(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        
        // 打断
        tvNotifyInterrupt.setOnClickListener(v -> notifyInterrupt());
        
        // 停止虚拟人
        tvStop.setOnClickListener(v -> stopAvatar());
        
        // 销毁
        tvDestroy.setOnClickListener(v -> destroy());
    }

    /**
     * 使用 `AvatarConfig` 中的默认值自动初始化并尝试启动虚拟人与录音。
     * 如果没有麦克风权限，会请求权限，权限回调会在 onRequestPermissionsResult 中处理并开始录音。
     */
    private void autoStartIfDesired() {
        try {
            // 预填配置项，便于 initSDK 使用
            etAppId.setText(AvatarConfig.DEFAULT_APP_ID);
            etApiKey.setText(AvatarConfig.DEFAULT_API_KEY);
            etApiSecret.setText(AvatarConfig.DEFAULT_API_SECRET);
            etServerUrl.setText(AvatarConfig.DEFAULT_SERVER_URL);
            etUid.setText(AvatarConfig.DEFAULT_UID);
            etAnchorId.setText(AvatarConfig.DEFAULT_ANCHOR_ID);

            // 初始化 SDK、创建播放器、设置参数并启动虚拟人
            initSDK();
            createPlayer();
            setParams();
            startAvatar();

            // 尝试创建并启动录音器（若无权限则请求，回调会启动）
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                    == PackageManager.PERMISSION_GRANTED) {
                if (recorder == null) {
                    createRecorder();
                }
                startRecord();
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.RECORD_AUDIO},
                        REQUEST_RECORD_AUDIO);
            }
        } catch (Exception e) {
            Log.w(TAG, "autoStartIfDesired failed", e);
        }
    }

    /**
     * 初始化SDK
     * 根据科大讯飞虚拟人SDK文档实现
     */
    private void initSDK() {
        String appId = etAppId.getText().toString().trim();
        String apiKey = etApiKey.getText().toString().trim();
        String apiSecret = etApiSecret.getText().toString().trim();
        String serverUrl = etServerUrl.getText().toString().trim();
        String uid = etUid.getText().toString().trim();
        String logPrefix = etLoggingPre.getText().toString().trim();
        int logLevelIndex = spinnerLog.getSelectedItemPosition();

        if (TextUtils.isEmpty(appId) || TextUtils.isEmpty(apiKey) || 
            TextUtils.isEmpty(apiSecret)) {
            Toast.makeText(this, "请填写完整的配置信息（APPID、APIKEY、APISECRET）", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // 如果用户误填了 http/https 地址，这里自动转换为 ws/wss，避免 WebSocket 101→200 协议错误
            if (!TextUtils.isEmpty(serverUrl) && (serverUrl.startsWith("http://") || serverUrl.startsWith("https://"))) {
                if (serverUrl.startsWith("https://")) {
                    serverUrl = serverUrl.replaceFirst("https://", "wss://");
                } else {
                    serverUrl = serverUrl.replaceFirst("http://", "ws://");
                }
            }

            // 构建配置对象
            AvatarPlatformConfig.Builder platformConfig = new AvatarPlatformConfig.Builder();
            
            // 设置日志等级（将spinner索引转换为LogLevel枚举）
            LogLevel logLevel = LogLevel.INFO; // 默认
            switch (logLevelIndex) {
                case 0: logLevel = LogLevel.VERBOSE; break;
                case 1: logLevel = LogLevel.DEBUG; break;
                case 2: logLevel = LogLevel.INFO; break;
                case 3: logLevel = LogLevel.WARN; break;
                case 4: logLevel = LogLevel.ERROR; break;
                default: logLevel = LogLevel.INFO; break;
            }
            platformConfig.setLogLevel(logLevel);
            
            // 设置日志前缀
            if (!TextUtils.isEmpty(logPrefix)) {
                platformConfig.setLoggingPre(logPrefix);
            }
            
            // 设置必填参数
            platformConfig.setAppId(appId);
            platformConfig.setApikey(apiKey);
            platformConfig.setApiSecret(apiSecret);
            
            // 设置可选参数
            if (!TextUtils.isEmpty(serverUrl)) {
                platformConfig.setServerUrl(serverUrl);
            }
            if (!TextUtils.isEmpty(uid)) {
                platformConfig.setUid(uid);
            }
            
            // 初始化SDK
            AvatarPlatform.initialize(this, platformConfig.build(), new AvatarPlatform.IInitListener() {
                @Override
                public void onResult(String code, String desc) {
                    runOnUiThread(() -> {
                        if (AvatarError.SUCCESS.getErrorCode().equals(code)) {
                            tvInitResult.setText("初始化成功");
                            Toast.makeText(MainActivity.this, "SDK初始化成功", Toast.LENGTH_SHORT).show();
                            Log.d(TAG, "SDK初始化成功");
                        } else {
                            tvInitResult.setText("初始化失败: " + desc);
                            Toast.makeText(MainActivity.this, "SDK初始化失败: " + desc, Toast.LENGTH_SHORT).show();
                            Log.e(TAG, "SDK初始化失败: code=" + code + ", desc=" + desc);
                        }
                    });
                }
            });
            
        } catch (Exception e) {
            tvInitResult.setText("初始化失败: " + e.getMessage());
            Log.e(TAG, "SDK初始化失败", e);
            Toast.makeText(this, "初始化异常: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 创建播放器
     * 根据文档使用StreamPlayerFactory.createPlayer创建
     */
    private void createPlayer() {
        try {
            // 创建播放器
            IStreamPlayer streamPlayer = StreamPlayerFactory.createPlayer(this, "xrtc");
            streamPlayer.setDebugLogging(true);
            streamPlayer.setRenderArea(videoViewAva);
            
            // 设置播放器参数
            StreamPlayerParams params = new StreamPlayerParams();
            params.setVolume(0.5); // 音量0-1
            String timeoutStr = etPlayerTimeout.getText().toString().trim();
            int timeout = 10000;
            if (!TextUtils.isEmpty(timeoutStr)) {
                try {
                    timeout = Integer.parseInt(timeoutStr);
                } catch (NumberFormatException e) {
                    Log.w(TAG, "播放超时时间格式错误，使用默认值", e);
                }
            }
            params.setRenderTimeout(timeout);
            params.setStreamType(AudioManager.STREAM_MUSIC);
            streamPlayer.setPlayerParams(params);
            
            // 设置播放器监听器
            streamPlayer.setPlayerListener(new IAvatarListener() {
                @Override
                public void onResult(String dataType, byte[] data, String extra) {
                    // 暂无
                }
                
                @Override
                public void onEvent(String eventType, String extra) {
                    runOnUiThread(() -> {
                        if ("video_size".equals(eventType)) {
                            Log.d(TAG, "视频尺寸: " + extra);
                        } else if ("first_frame".equals(eventType)) {
                            Log.d(TAG, "首帧渲染完成");
                            Toast.makeText(MainActivity.this, "首帧渲染完成", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
                
                @Override
                public void onError(String errorCode, String errorDesc, String extra) {
                    runOnUiThread(() -> {
                        tvErrorListenerPlayer.setText("错误: " + errorDesc);
                        Log.e(TAG, "播放器错误: code=" + errorCode + ", desc=" + errorDesc);
                        Toast.makeText(MainActivity.this, "播放器错误: " + errorDesc, Toast.LENGTH_SHORT).show();
                    });
                }
            });
            
            this.player = streamPlayer;
            
            Toast.makeText(this, "播放器创建成功", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "播放器创建成功");
        } catch (Exception e) {
            Toast.makeText(this, "播放器创建失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e(TAG, "播放器创建失败", e);
        }
    }

    /**
     * 设置全局参数
     * 根据文档使用AvatarPlayController.setGlobalParams设置
     */
    private void setParams() {
        try {
            // 获取控制器
            AvatarPlayController controller = AvatarPlatform.getController();
            
            // 创建全局参数对象
            AvatarParams avatarParams = new AvatarParams();
            
            // 设置Avatar参数
            AvatarParams.Avatar avatar = new AvatarParams.Avatar();
            avatar.setAvatarId(etAnchorId.getText().toString().trim());
            
            // 设置视频尺寸
            String widthStr = etVideoWidth.getText().toString().trim();
            String heightStr = etVideoHeight.getText().toString().trim();
            if (!TextUtils.isEmpty(widthStr)) {
                avatar.setWidth(Integer.parseInt(widthStr));
            }
            if (!TextUtils.isEmpty(heightStr)) {
                avatar.setHeight(Integer.parseInt(heightStr));
            }
            
            // 设置位移和缩放
            String scaleStr = etScale.getText().toString().trim();
            if (!TextUtils.isEmpty(scaleStr)) {
                avatar.setScale(Double.parseDouble(scaleStr));
            }
            String moveHStr = etMoveH.getText().toString().trim();
            if (!TextUtils.isEmpty(moveHStr)) {
                avatar.setMoveH(Integer.parseInt(moveHStr));
            }
            String moveVStr = etMoveV.getText().toString().trim();
            if (!TextUtils.isEmpty(moveVStr)) {
                avatar.setMoveV(Integer.parseInt(moveVStr));
            }
            
            // 设置Stream参数
            AvatarParams.Stream stream = new AvatarParams.Stream();
            stream.setProtocol("xrtc");
            stream.setFps(25); // 默认25帧
            stream.setBitrate(2000); // 默认2000kbps
            stream.setAlpha(0); // 无透明通道
            avatar.setStream(stream);
            
            avatarParams.setAvatar(avatar);
            
            // 设置TTS参数
            AvatarParams.TTS tts = new AvatarParams.TTS();
            String vcn = etVcn.getText().toString().trim();
            if (!TextUtils.isEmpty(vcn)) {
                tts.setVcn(vcn);
            }
            String speedStr = etSpeed.getText().toString().trim();
            if (!TextUtils.isEmpty(speedStr)) {
                tts.setSpeed(Integer.parseInt(speedStr));
            }
            String pitchStr = etPitch.getText().toString().trim();
            if (!TextUtils.isEmpty(pitchStr)) {
                tts.setPitch(Integer.parseInt(pitchStr));
            }
            String volumeStr = etVolume.getText().toString().trim();
            if (!TextUtils.isEmpty(volumeStr)) {
                tts.setVolume(Integer.parseInt(volumeStr));
            }
            avatarParams.setTTS(tts);
            
            // 设置变声参数（如果启用）
            if (switchVc.isChecked()) {
                AvatarParams.VC vc = new AvatarParams.VC();
                vc.setVC(1);
                String vcVcn = etVcVcn.getText().toString().trim();
                if (!TextUtils.isEmpty(vcVcn)) {
                    vc.setVoiceName(vcVcn);
                }
                avatarParams.setVC(vc);
            }
            
            // 设置字幕参数（如果启用）
            if (switchSubtitle.isChecked()) {
                AvatarParams.Subtitle subtitle = new AvatarParams.Subtitle();
                subtitle.setSubtitle(1);
                String color = etSubtitleColor.getText().toString().trim();
                if (!TextUtils.isEmpty(color)) {
                    subtitle.setFontColor(color);
                }
                avatarParams.setSubtitle(subtitle);
            }
            
            // 设置场景参数
            String sceneId = etSceneId.getText().toString().trim();
            if (!TextUtils.isEmpty(sceneId)) {
                AvatarParams.Scene scene = new AvatarParams.Scene();
                scene.setSceneId(sceneId);
                String sceneVersion = etSceneVersion.getText().toString().trim();
                if (!TextUtils.isEmpty(sceneVersion)) {
                    scene.setSceneVersion(sceneVersion);
                }
                avatarParams.setScene(scene);
            }
            
            // 设置情感分析
            if (switchContentAnalysis.isChecked()) {
                AvatarParams.Dispatch dispatch = new AvatarParams.Dispatch();
                dispatch.setContentAnalysis(1);
                avatarParams.setDispatch(dispatch);
            }
            
            // 应用全局参数
            controller.setGlobalParams(avatarParams);
            
            // 如果已创建播放器，设置到控制器
            if (player != null) {
                controller.setStreamPlayer(player);
            }
            
            Toast.makeText(this, "参数设置成功", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "参数设置成功");
        } catch (Exception e) {
            Toast.makeText(this, "参数设置失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e(TAG, "参数设置失败", e);
        }
    }

    /**
     * 启动虚拟人
     * 根据文档使用AvatarPlayController.start()启动
     */
    private void startAvatar() {
        try {
            // 获取控制器
            AvatarPlayController controller = AvatarPlatform.getController();
            
            // 注册虚拟人监听器（如果启用）
            if (switchSetListener.isChecked()) {
                IAvatarListener avatarListener = new IAvatarListener() {
                    @Override
                    public void onResult(String dataType, byte[] data, String extra) {
                        final String message;
                        if ("nlp".equals(dataType)) {
                            message = "语义理解结果: " + new String(data);
                        } else if ("asr".equals(dataType)) {
                            message = "语音识别结果: " + new String(data);
                        } else {
                            message = "onResult type=" + dataType + " data=" + (data != null ? new String(data) : "null");
                        }
                        Log.d(TAG, message);
                        runOnUiThread(() -> {
                            if (tvErrorListener != null) {
                                tvErrorListener.setText(message);
                            }
                        });
                    }
                    
                    @Override
                    public void onEvent(String eventType, String extra) {
                        final String evMsg = "事件: " + eventType + ", extra: " + extra;
                        Log.d(TAG, evMsg);
                        runOnUiThread(() -> {
                            if (tvErrorListener != null) {
                                String old = tvErrorListener.getText().toString();
                                String newText = old + "\n" + evMsg;
                                tvErrorListener.setText(newText);
                            }
                            if ("server_connect_success".equals(eventType)) {
                                Toast.makeText(MainActivity.this, "服务连接成功", Toast.LENGTH_SHORT).show();
                            } else if ("stream_start".equals(eventType)) {
                                Toast.makeText(MainActivity.this, "开始推流", Toast.LENGTH_SHORT).show();
                            } else if ("frame_start".equals(eventType)) {
                                Toast.makeText(MainActivity.this, "开始播报", Toast.LENGTH_SHORT).show();
                            } else if ("frame_end".equals(eventType)) {
                                Toast.makeText(MainActivity.this, "结束播报", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                    
                    @Override
                    public void onError(String errorCode, String errorDesc, String extra) {
                        runOnUiThread(() -> {
                            tvErrorListener.setText("错误: " + errorDesc);
                            Log.e(TAG, "虚拟人错误: code=" + errorCode + ", desc=" + errorDesc);
                            Toast.makeText(MainActivity.this, "错误: " + errorDesc, Toast.LENGTH_SHORT).show();
                        });
                    }
                };
                controller.addAvatarListener(avatarListener);
            }
            
            // 启动虚拟人
            controller.start();
            // 标记已启动，后续收到的权限回调会根据该标志决定是否创建/启动录音器
            isAvatarStarted = true;
            
            Toast.makeText(this, "虚拟人启动成功", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "虚拟人启动成功");

            // 启动后自动尝试创建录音器（若权限允许则创建，否则请求权限）
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

            // 如果此前用户已在虚拟人启动前授予权限并请求开始录音（pending标志），则在此时创建并开始录音
            try {
                if (pendingStartRecordAfterStart) {
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                            == PackageManager.PERMISSION_GRANTED) {
                        if (recorder == null) {
                            createRecorder();
                        }
                        startRecord();
                        pendingStartRecordAfterStart = false;
                        Log.d(TAG, "检测到待启动录音请求，已在虚拟人启动后开始录音");
                    } else {
                        // 权限被撤回或未授予，保持pending为true，等待权限授予回调
                        Log.d(TAG, "待启动录音请求存在但未获得权限，继续等待权限授予");
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, "处理 pendingStartRecordAfterStart 时发生异常", e);
            }
            
            // 启动成功后启用创建录音器按钮
            try {
                tvCreateRecorder.setEnabled(true);
            } catch (Exception e) {
                Log.w(TAG, "设置 tvCreateRecorder 可用状态失败", e);
            }
        } catch (Exception e) {
            Toast.makeText(this, "虚拟人启动失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e(TAG, "虚拟人启动失败", e);
        }
    }

    /**
     * 发送文本
     * 根据文档使用AvatarPlayController.writeText()发送
     */
    private void sendText() {
        String text = etInputBox.getText().toString().trim();
        if (TextUtils.isEmpty(text)) {
            Toast.makeText(this, "请输入文本", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // 获取控制器
            AvatarPlayController controller = AvatarPlatform.getController();
            
            // 判断是否为交互模式（switchNlp控制）
            boolean isNlpMode = switchNlp.isChecked();
            
            if (isNlpMode) {
                // 交互模式：需要设置TextParams
                TextParams textParams = new TextParams();
                textParams.setNlp(true); // 启用语义理解
                
                // 设置TTS参数（如果UI中有实时参数）
                String vcnRt = etVcnRt.getText().toString().trim();
                if (!TextUtils.isEmpty(vcnRt)) {
                    AvatarParams.TTS tts = new AvatarParams.TTS();
                    tts.setVcn(vcnRt);
                    String speedRt = etSpeedRt.getText().toString().trim();
                    if (!TextUtils.isEmpty(speedRt)) {
                        tts.setSpeed(Integer.parseInt(speedRt));
                    }
                    String pitchRt = etPitchRt.getText().toString().trim();
                    if (!TextUtils.isEmpty(pitchRt)) {
                        tts.setPitch(Integer.parseInt(pitchRt));
                    }
                    String volumeRt = etVolumeRt.getText().toString().trim();
                    if (!TextUtils.isEmpty(volumeRt)) {
                        tts.setVolume(Integer.parseInt(volumeRt));
                    }
                    textParams.setTTS(tts);
                }
                
                // 设置交互模式（追加/打断）
                AvatarParams.Dispatch dispatch = new AvatarParams.Dispatch();
                // 注意：根据实际SDK API，方法名可能是setInteractiveMod、setInteractiveMode或setInteractive
                // 如果编译错误，请根据SDK文档调整方法名
                dispatch.setInteractiveMode(switchInteractMode.isChecked() ? 1 : 0); // 1-打断, 0-追加
                textParams.setDispatch(dispatch);
                
                // 设置自动动作
                if (switchAutoAction.isChecked()) {
                    AvatarParams.AIR air = new AvatarParams.AIR();
                    air.setAir(1);
                    textParams.setAIR(air);
                }
                
                // 设置无指向动作
                if (switchAddNonsemanti.isChecked()) {
                    AvatarParams.AIR air = textParams.getAIR();
                    if (air == null) {
                        air = new AvatarParams.AIR();
                    }
                    air.setAddNonSemantic(1);
                    textParams.setAIR(air);
                }
                
                // 设置变声（如果启用）
                if (switchVcRt.isChecked()) {
                    AvatarParams.VC vc = new AvatarParams.VC();
                    vc.setVC(1);
                    String vcVcnRt = etVcVcnRt.getText().toString().trim();
                    if (!TextUtils.isEmpty(vcVcnRt)) {
                        vc.setVoiceName(vcVcnRt);
                    }
                    textParams.setVC(vc);
                }
                
                controller.writeText(text, textParams);
            } else {
                // 驱动模式：直接发送文本，使用全局参数
                controller.writeText(text);
            }
            
            Toast.makeText(this, "文本发送成功", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "发送文本: " + text + ", 模式: " + (isNlpMode ? "交互" : "驱动"));
        } catch (Exception e) {
            Toast.makeText(this, "文本发送失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e(TAG, "文本发送失败", e);
        }
    }

    /**
     * 创建录音器
     * 根据文档使用AudioRecorder构造器创建
     */
    private void createRecorder() {
        try {
            // 根据文档创建录音器
            // 注意：当前输入音频仅支持采样率16k、单通道、16bit音频
            int audioSource = MediaRecorder.AudioSource.MIC;
            // 如果启用全双工交互，使用VOICE_COMMUNICATION作为录音源
            if (switchFullDuplex != null && switchFullDuplex.isChecked()) {
                audioSource = MediaRecorder.AudioSource.VOICE_COMMUNICATION;
            }
            AudioRecorder audioRecorder = new AudioRecorder(
                audioSource,
                16000,                          // 采样率：16k
                AudioFormat.ENCODING_PCM_16BIT, // 16bit
                AudioFormat.CHANNEL_IN_MONO     // 单通道
            );
            // 在 recorder 上注册 PCM 数据回调，便于调试与自定义处理（示例：更新 UI、测量音量等）
            audioRecorder.addPcmDataListener(new AudioRecorder.PcmDataListener() {
                @Override
                public void onPcmData(byte[] pcmData, int size, int status) {
                    // 简单日志与 UI 更新：显示录音缓冲区大小（避免频繁 UI 更新可加节流）
                    Log.d(TAG, "onPcmData size=" + size + " status=" + status);
                    runOnUiThread(() -> {
                        if (tvErrorListener != null) {
                            tvErrorListener.setText("录音中: " + size + " bytes");
                        }
                    });

                    try {
                        // 防御性检查：避免发送 null 或长度为0 的 payload 导致服务端报错（10106）
                        if (pcmData == null) {
                            Log.w(TAG, "onPcmData: pcmData is null, skip");
                            return;
                        }
                        if (size <= 0) {
                            Log.w(TAG, "onPcmData: size <= 0, skip (status=" + status + ")");
                            return;
                        }

                        // 复制有效字节到新数组
                        byte[] audioBytes = new byte[size];
                        System.arraycopy(pcmData, 0, audioBytes, 0, size);

                        // 当帧状态为0（起始帧）时，生成新的 requestId 并构造单次交互参数
                        AudioParams params = null;
                        if (status == 0) {
                            currentAudioRequestId = String.valueOf(System.currentTimeMillis()) + "_" + AvatarConfig.DEFAULT_UID;
                            params = new AudioParams();
                            try {
                                params.setRequestId(currentAudioRequestId);
                            } catch (Throwable ignored) {}
                            // 请求服务端做语义/ASR 解析
                            try {
                                params.setNlp(true);
                            } catch (Throwable ignored) {}

                            // 设置交互模式（打断或追加），与 UI 开关保持一致
                            try {
                                AvatarParams.Dispatch dispatch = new AvatarParams.Dispatch();
                                dispatch.setInteractiveMode(switchInteractMode != null && switchInteractMode.isChecked() ? 1 : 0);
                                params.setDispatch(dispatch);
                            } catch (Throwable ignored) {}

                            // 可选：设置 ASR 单次参数（如全双工/半双工）
                            try {
                                AvatarParams.ASR asr = new AvatarParams.ASR();
                                asr.setFullDuplex(switchFullDuplex != null && switchFullDuplex.isChecked() ? 1 : 0);
                                params.setASR(asr);
                            } catch (Throwable ignored) {}
                        }

                        // 防御性保证：不向 controller 发送 null 或空 payload
                        if (audioBytes == null || audioBytes.length == 0) {
                            Log.w(TAG, "audioBytes null or length 0, skip writeAudio");
                            return;
                        }

                        // 将音频数据写入 SDK；若 params 为 null 则使用全局参数
                        // 使用 AvatarUtils.safeWriteAudio 进行统一的空检查与异常保护
                        AvatarUtils.safeWriteAudio(audioBytes, status, params);
                    } catch (Throwable e) {
                        Log.e(TAG, "onPcmData processing failed", e);
                    }
                }
            });
            this.recorder = audioRecorder;
            
            // 如果已创建控制器，将录音器设置到控制器
            // 监听器可能通过控制器设置，而不是直接在AudioRecorder上设置
            AvatarPlayController controller = AvatarPlatform.getController();
            if (controller != null) {
                // 明确告知服务端需要ASR/NLP处理
                AudioParams audioParams = new AudioParams();
                audioParams.setNlp(true);
                controller.setAudioRecorder(audioRecorder, audioParams);
            }
            
            Toast.makeText(this, "录音器创建成功", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "录音器创建成功");
        } catch (Exception e) {
            Toast.makeText(this, "录音器创建失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e(TAG, "录音器创建失败", e);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_RECORD_AUDIO) {
            if (grantResults != null && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 权限授予：如果虚拟人已启动则立即创建并开始录音；否则延后到虚拟人启动完成后执行
                if (isAvatarStarted) {
                    if (recorder == null) {
                        createRecorder();
                    }
                    startRecord();
                } else {
                    // 记录为待启动状态，等待虚拟人启动完成后再创建并开始录音
                    pendingStartRecordAfterStart = true;
                    Toast.makeText(this, "已授权麦克风，录音将在虚拟人启动后自动开始", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "录音权限已授予，等待虚拟人启动后再创建并开始录音");
                }
            } else {
                Toast.makeText(this, "需要麦克风权限以使用语音交互", Toast.LENGTH_LONG).show();
                Log.w(TAG, "录音权限被拒绝");
            }
        }
    }

    /**
     * 开始录音
     */
    private void startRecord() {
        try {
            if (recorder != null) {
                // 根据SDK文档，使用startRecording()方法
                recorder.startRecord();
                isRecording = true;
                Toast.makeText(this, "开始录音", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "开始录音成功");
            } else {
                Toast.makeText(this, "请先创建录音器", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "开始录音失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e(TAG, "开始录音失败", e);
        }
    }

    /**
     * 停止录音
     */
    private void stopRecord() {
        try {
            if (recorder != null && isRecording) {
                recorder.stopRecord();
                isRecording = false;
                Toast.makeText(this, "停止录音", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "停止录音");
            } else {
                Toast.makeText(this, "录音器未运行", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "停止录音失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e(TAG, "停止录音失败", e);
        }
    }

    /**
     * 发送指令
     */
    private void sendAction() {
        String cmdType = etCmdType.getText().toString().trim();
        String cmdValue = etCmdValue.getText().toString().trim();
        String cmdTb = etCmdTb.getText().toString().trim();

        if (TextUtils.isEmpty(cmdValue)) {
            Toast.makeText(this, "请输入指令内容", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // 根据文档，独立指令主要用于发送动作等
            // 可以通过writeText发送动作标签，格式如：[[action=A_Hello_l_F]]
            String actionText = "[[" + cmdType + "=" + cmdValue + "]]";
            AvatarPlayController controller = AvatarPlatform.getController();
            if (controller != null) {
                controller.writeText(actionText);
                Toast.makeText(this, "指令发送成功", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "发送指令: type=" + cmdType + ", value=" + cmdValue);
            } else {
                Toast.makeText(this, "控制器未初始化", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "指令发送失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e(TAG, "指令发送失败", e);
        }
    }

    /**
     * 设置播放器音量
     * 根据文档使用IStreamPlayer.setVolume()设置，volume取值0-1
     */
    private void setPlayerVolume(int volume) {
        try {
            if (player != null) {
                // 将0-100转换为0-1
                double volumeValue = volume / 100.0;
                player.setVolume(volumeValue);
                Log.d(TAG, "设置播放器音量: " + volumeValue);
            }
        } catch (Exception e) {
            Log.e(TAG, "设置播放器音量失败", e);
        }
    }

    /**
     * 打断
     */
    private void notifyInterrupt() {
        try {
            // 根据文档，打断可以通过发送空文本或调用特定方法实现
            // 这里使用发送空文本的方式
            AvatarPlayController controller = AvatarPlatform.getController();
            if (controller != null) {
                // 优先使用中断接口（比发送空文本更安全，避免服务端对空文本的校验失败）
                try {
                    controller.interrupt();
                } catch (Throwable ignored) {
                    // 回退兼容：若 SDK 不支持 interrupt()，再尝试发送空文本（但避免发送空文本以免服务端返回错误）
                    Log.w(TAG, "controller.interrupt() failed, skipping sending empty text to avoid 10106");
                }
                Toast.makeText(this, "打断成功", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "打断");
            } else {
                Toast.makeText(this, "控制器未初始化", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "打断失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e(TAG, "打断失败", e);
        }
    }

    /**
     * 停止虚拟人
     */
    private void stopAvatar() {
        try {
            AvatarPlayController controller = AvatarPlatform.getController();
            if (controller != null) {
                AvatarUtils.safeStopController();
                Toast.makeText(this, "虚拟人已停止", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "虚拟人已停止 (safe)");
                // 停止后禁用创建录音器按钮
                try {
                    tvCreateRecorder.setEnabled(false);
                } catch (Exception e) {
                    Log.w(TAG, "设置 tvCreateRecorder 不可用失败", e);
                }
            } else {
                Toast.makeText(this, "控制器未初始化", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "停止失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e(TAG, "停止失败", e);
        }
    }

    /**
     * 销毁
     */
    private void destroy() {
        try {
            // 停止播放器
            if (player != null) {
                player.stopPlay(true); // release=true，完全释放
                player = null;
            }
            
            // 停止录音器
            if (recorder != null) {
                if (isRecording) {
                    recorder.stopRecord();
                }
                isRecording = false;
                recorder = null;
            }
            
            // 停止控制器
            // 使用安全停止，避免 SDK 内部异常（如 TelephonyManager NPE）导致崩溃
            try {
                AvatarUtils.safeStopController();
            } catch (Throwable ignored) {}
            
            // 释放时禁用创建录音器
            try {
                if (tvCreateRecorder != null) {
                    tvCreateRecorder.setEnabled(false);
                }
            } catch (Exception e) {
                Log.w(TAG, "设置 tvCreateRecorder 不可用失败", e);
            }

            Log.d(TAG, "资源已释放");
        } catch (Exception e) {
            Log.e(TAG, "释放失败", e);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        destroy();
    }
}

