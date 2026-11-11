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

import com.virhuman.testsdk.R;

// 科大讯飞虚拟人SDK导入
import com.iflytek.avalibrary.AvatarPlatform;
import com.iflytek.avalibrary.AvatarPlatformConfig;
import com.iflytek.avalibrary.AvatarPlayController;
import com.iflytek.avalibrary.IAvatarListener;
import com.iflytek.avalibrary.constant.AvatarError;
import com.iflytek.ys.core.log.LogLevel;
import com.iflytek.avalibrary.params.AvatarParams;
import com.iflytek.avalibrary.params.TextParams;
import com.iflytek.avalibrary.videoplayer.rt.StreamPlayerFactory;
import com.iflytek.avalibrary.videoplayer.rt.interfaces.IStreamPlayer;
import com.iflytek.avalibrary.videoplayer.params.StreamPlayerParams;
import com.iflytek.ys.core.recorder.AudioRecorder;
import com.virhuman.testsdk.utils.AvatarUtils;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.MediaRecorder;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.content.pm.PackageManager;

/**
 * 端模式Activity - 科大讯飞虚拟人SDK端模式演示
 */
public class MainLocalActivity extends AppCompatActivity {
    private static final String TAG = "MainLocalActivity";

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
    private EditText etAnchorId;
    private EditText etVideoFps;
    private EditText etVcn;
    private EditText etSpeed;
    private EditText etPitch;
    private EditText etVolume;
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
    
    // 文本驱动/交互
    private EditText etInputBox;
    private EditText etVcnRt;
    private EditText etSpeedRt;
    private EditText etPitchRt;
    private EditText etVolumeRt;
    private Switch switchInteractMode;
    private Switch switchAutoAction;
    private Switch switchAddNonsemanti;
    private TextView tvSendText;
    
    // 音频驱动/交互
    private TextView tvCreateRecorder;
    private TextView tvStartRecord;
    private TextView tvStopRecord;
    
    // 独立指令
    private EditText etCmdValue;
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
    private boolean isRecording = false;
    // 标记虚拟人已启动（用于延后创建/启动录音器）
    private boolean isAvatarStarted = false;
    // 在虚拟人启动前授予权限时记录为待创建录音器
    private boolean pendingStartRecordAfterStart = false;
    private static final int REQUEST_RECORD_AUDIO = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_local_main);
        
        initViews();
        initSpinner();
        setListeners();
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
        etAnchorId = findViewById(R.id.et_anchor_id);
        etVideoFps = findViewById(R.id.et_video_fps);
        etVcn = findViewById(R.id.et_vcn);
        etSpeed = findViewById(R.id.et_speed);
        etPitch = findViewById(R.id.et_pitch);
        etVolume = findViewById(R.id.et_volume);
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
        
        // 文本驱动/交互
        etInputBox = findViewById(R.id.et_input_box);
        etVcnRt = findViewById(R.id.et_vcn_rt);
        etSpeedRt = findViewById(R.id.et_speed_rt);
        etPitchRt = findViewById(R.id.et_pitch_rt);
        etVolumeRt = findViewById(R.id.et_volume_rt);
        switchInteractMode = findViewById(R.id.switch_interact_mode);
        switchAutoAction = findViewById(R.id.switch_auto_action);
        switchAddNonsemanti = findViewById(R.id.switch_add_nonsemanti);
        tvSendText = findViewById(R.id.tv_send_text);
        
        // 音频驱动/交互
        tvCreateRecorder = findViewById(R.id.tv_create_recorder);
        tvStartRecord = findViewById(R.id.tv_start_record);
        tvStopRecord = findViewById(R.id.tv_stop_record);
        
        // 独立指令
        etCmdValue = findViewById(R.id.et_cmd_value);
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
        // 这里使用常用的几个级别
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
        
        // 创建录音器
        tvCreateRecorder.setOnClickListener(v -> createRecorder());
        
        // 开始录音
        tvStartRecord.setOnClickListener(v -> startRecord());
        
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

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_RECORD_AUDIO) {
            if (grantResults != null && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 权限授予：如果虚拟人已启动则立即创建录音器；否则延后到虚拟人启动后创建
                if (isAvatarStarted) {
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
                Toast.makeText(this, "需要麦克风权限以使用语音交互", Toast.LENGTH_LONG).show();
                Log.w(TAG, "录音权限被拒绝");
            }
        }
    }

    /**
     * 初始化SDK
     * 根据科大讯飞虚拟人SDK文档实现（端模式）
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
            
            // 初始化SDK（端模式和云模式使用相同的初始化方法）
            AvatarPlatform.initialize(this, platformConfig.build(), new AvatarPlatform.IInitListener() {
                @Override
                public void onResult(String code, String desc) {
                    runOnUiThread(() -> {
                        if (AvatarError.SUCCESS.getErrorCode().equals(code)) {
                            tvInitResult.setText("初始化成功");
                            Toast.makeText(MainLocalActivity.this, "SDK初始化成功", Toast.LENGTH_SHORT).show();
                            Log.d(TAG, "SDK初始化成功（端模式）");
                        } else {
                            tvInitResult.setText("初始化失败: " + desc);
                            Toast.makeText(MainLocalActivity.this, "SDK初始化失败: " + desc, Toast.LENGTH_SHORT).show();
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
            // 创建播放器（端模式和云模式使用相同的API）
            IStreamPlayer streamPlayer = StreamPlayerFactory.createPlayer(this, "xrtc");
            streamPlayer.setDebugLogging(true);
            streamPlayer.setRenderArea(videoViewAva);
            
            StreamPlayerParams params = new StreamPlayerParams();
            params.setVolume(0.5);
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
            
            streamPlayer.setPlayerListener(new IAvatarListener() {
                @Override
                public void onResult(String dataType, byte[] data, String extra) {}
                
                @Override
                public void onEvent(String eventType, String extra) {
                    runOnUiThread(() -> {
                        if ("first_frame".equals(eventType)) {
                            Toast.makeText(MainLocalActivity.this, "首帧渲染完成", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
                
                @Override
                public void onError(String errorCode, String errorDesc, String extra) {
                    runOnUiThread(() -> {
                        tvErrorListenerPlayer.setText("错误: " + errorDesc);
                        Log.e(TAG, "播放器错误: " + errorDesc);
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
     * 根据文档使用AvatarPlayController.setGlobalParams设置（端模式）
     */
    private void setParams() {
        try {
            // 获取控制器
            AvatarPlayController controller = AvatarPlatform.getController();
            
            AvatarParams avatarParams = new AvatarParams();
            
            // 设置Avatar参数
            AvatarParams.Avatar avatar = new AvatarParams.Avatar();
            avatar.setAvatarId(etAnchorId.getText().toString().trim());
            
            // 设置Stream参数（端模式有帧率设置）
            AvatarParams.Stream stream = new AvatarParams.Stream();
            stream.setProtocol("xrtc");
            String fpsStr = etVideoFps.getText().toString().trim();
            if (!TextUtils.isEmpty(fpsStr)) {
                stream.setFps(Integer.parseInt(fpsStr)); // 支持15,20,25
            } else {
                stream.setFps(25); // 默认25
            }
            stream.setBitrate(2000);
            stream.setAlpha(0);
            avatar.setStream(stream);
            
            avatarParams.setAvatar(avatar);
            
            // 设置TTS参数（与云模式相同）
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
            
            controller.setGlobalParams(avatarParams);
            
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
                            Log.d(TAG, "事件: " + eventType);
                            if ("server_connect_success".equals(eventType)) {
                                Toast.makeText(MainLocalActivity.this, "服务连接成功", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                    
                    @Override
                    public void onError(String errorCode, String errorDesc, String extra) {
                        runOnUiThread(() -> {
                            tvErrorListener.setText("错误: " + errorDesc);
                            Log.e(TAG, "虚拟人错误: " + errorDesc);
                        });
                    }
                };
                controller.addAvatarListener(avatarListener);
            }
            
            controller.start();
            isAvatarStarted = true;
            
            Toast.makeText(this, "虚拟人启动成功", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "虚拟人启动成功");

            // 启动后尝试创建录音器：有权限则创建，否则请求权限；并处理之前的 pending 标志
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

            try {
                if (pendingStartRecordAfterStart) {
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                            == PackageManager.PERMISSION_GRANTED) {
                        if (recorder == null) {
                            createRecorder();
                        }
                        pendingStartRecordAfterStart = false;
                        Log.d(TAG, "检测到待创建录音请求，已在虚拟人启动后创建录音器");
                    } else {
                        Log.d(TAG, "检测到待创建录音请求但权限未授予，继续等待权限回调");
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, "处理 pendingStartRecordAfterStart 时发生异常", e);
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
            
            boolean isNlpMode = switchNlp.isChecked();
            
            if (isNlpMode) {
                // 交互模式
                TextParams textParams = new TextParams();
                textParams.setNlp(true);
                
                // 设置TTS参数
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
                
                // 设置交互模式
                AvatarParams.Dispatch dispatch = new AvatarParams.Dispatch();
                // 注意：根据实际SDK API，方法名可能是setInteractiveMod、setInteractiveMode或setInteractive
                dispatch.setInteractiveMode(switchInteractMode.isChecked() ? 1 : 0);
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
                
                controller.writeText(text, textParams);
            } else {
                // 驱动模式
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
            // 根据文档创建录音器（与云模式相同）
            AudioRecorder audioRecorder = new AudioRecorder(
                MediaRecorder.AudioSource.MIC,
                16000,
                AudioFormat.ENCODING_PCM_16BIT,
                AudioFormat.CHANNEL_IN_MONO
            );
            
            // 注意：PCM数据监听器可能通过控制器设置，而不是直接在AudioRecorder上设置
            this.recorder = audioRecorder;
            
            AvatarPlayController controller = AvatarPlatform.getController();
            if (controller != null) {
                controller.setAudioRecorder(audioRecorder);
            }
            
            Toast.makeText(this, "录音器创建成功", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "录音器创建成功");
        } catch (Exception e) {
            Toast.makeText(this, "录音器创建失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e(TAG, "录音器创建失败", e);
        }
    }

    /**
     * 开始录音
     */
    private void startRecord() {
        try {
            if (recorder != null) {
                // 根据当前 AudioRecorder 接口，使用 startRecord() 方法
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
        String cmdValue = etCmdValue.getText().toString().trim();

        if (TextUtils.isEmpty(cmdValue)) {
            Toast.makeText(this, "请输入指令内容", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // 根据文档，独立指令主要用于发送动作等
            // 可以通过writeText发送动作标签，格式如：[[action=A_Hello_l_F]]
            String actionText = "[[action=" + cmdValue + "]]";
            AvatarPlayController controller = AvatarPlatform.getController();
            if (controller != null) {
                controller.writeText(actionText);
                Toast.makeText(this, "指令发送成功", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "发送指令: value=" + cmdValue);
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
            AvatarPlayController controller = AvatarPlatform.getController();
            if (controller != null) {
                try {
                    controller.interrupt();
                    Toast.makeText(this, "打断成功", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "打断 (interrupt)");
                } catch (Throwable t) {
                    Log.w(TAG, "controller.interrupt() failed, skipping sending empty text", t);
                    Toast.makeText(this, "打断失败", Toast.LENGTH_SHORT).show();
                }
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
            // 使用安全停止，避免 SDK 在停止时抛出未捕获异常
            try {
                AvatarUtils.safeStopController();
            } catch (Throwable t) {
                Log.w(TAG, "safeStopController failed", t);
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

