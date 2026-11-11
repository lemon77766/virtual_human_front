package com.virhuman.testsdk.ui;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.graphics.Rect;
import android.graphics.Color;
import android.content.Context;
import android.view.inputmethod.InputMethodManager;
import android.view.WindowManager;
import android.view.ViewTreeObserver;
import android.animation.AnimatorListenerAdapter;
import android.animation.Animator;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Intent;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatActivity;

import com.iflytek.avalibrary.AvatarPlatform;
import com.iflytek.avalibrary.AvatarPlayController;
import com.iflytek.avalibrary.IAvatarListener;
import com.iflytek.avalibrary.constant.AvatarError;
import com.iflytek.avalibrary.params.AvatarParams;
import com.iflytek.avalibrary.params.TextParams;
import com.iflytek.avalibrary.params.AudioParams;
import com.iflytek.avalibrary.videoplayer.params.StreamPlayerParams;
import android.media.AudioFormat;
import android.media.MediaRecorder;
import com.iflytek.avalibrary.videoplayer.rt.interfaces.IStreamPlayer;
import com.iflytek.ys.core.recorder.AudioRecorder;
import com.virhuman.testsdk.R;
import com.virhuman.testsdk.config.AvatarConfig;
import com.virhuman.testsdk.utils.AvatarUtils;
import com.virhuman.testsdk.utils.PermissionUtils;
import com.virhuman.testsdk.utils.AudioInteractionManager;

/**
 * 首页 - 虚拟人居中展示 + 快捷提示语 + 文字/语音交互
 * 历史对话/切换形象页先不实现（仅占位按钮）
 */
public class HomeActivity extends AppCompatActivity {
    private static final String TAG = "HomeActivity";
    // 首帧超时时间，与播放器超时时间保持一致（60秒）
    private static final long FIRST_FRAME_TIMEOUT_MS = 60000L;
    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private Runnable firstFrameTimeoutRunnable;
    // Handler & flags for press-and-hold recording UX
    private final Handler recordHandler = new Handler(Looper.getMainLooper());
    private Runnable delayedStartRunnable;
    private long recordPressStartTime = 0L;
    // 最短按住时长阈值，低于此值按下视为误触（ms）
    private static final long MIN_PRESS_TO_START_RECORD_MS = 250L;

    // UI
    private FrameLayout videoViewAva;
    private ImageView ivAvatarBg;
    private ImageButton btnHistory;
    private ImageButton btnSwitchAvatar;
    private TextView tvStatus;
    private EditText etInputText;
    private ImageButton btnSend;
    private ImageButton btnVoiceRecord;
    // Programmatic controls for explicit start/stop flow
    private Button btnStartRecordCtrl;
    private Button btnStopRecordCtrl;
    // overlay container for audio control buttons
    private ViewGroup audioOverlayContainer;

    private TextView chipWeather;
    private TextView chipSchedule;
    private TextView chipMedicine;

    private View navHome;
    private View navFeatures;
    private View navMessages;
    private View navProfile;

    // SDK
    private IStreamPlayer player;
    private AudioRecorder recorder;

    // state
    private boolean isInitialized = false;
    private boolean isPlayerCreated = false;
    private boolean isStarted = false;
    private boolean isRecording = false;
    private boolean isStarting = false;
    // 标记虚拟人是否已启动（用于延后创建/启动录音器）
    private boolean isAvatarStarted = false;
    // 如果用户在虚拟人启动前授予录音权限，则记录为待创建录音器
    private boolean pendingStartRecordAfterStart = false;
    // keyboard handling
    private View rootViewChild;
    private boolean keyboardVisible = false;
    private ViewTreeObserver.OnGlobalLayoutListener keyboardListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // 确保键盘弹出时布局整体上移（避免遮挡底部输入区域）
        try {
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        } catch (Exception ignored) {}

        initViews();
        setListeners();
        // 监听键盘状态并在弹出时滚动/平移界面以保证输入框可见
        setupKeyboardListener();

        // 权限检查：若没权限先申请；申请后用户可再次进入页面自动唤醒
        if (!PermissionUtils.hasRequiredPermissions(this)) {
            PermissionUtils.requestRequiredPermissions(this);
            updateStatus("请授予麦克风/存储等权限后再开始交互");
        } else {
            // 启动即唤醒
            oneKeyStartAvatar();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 从权限页返回后自动唤醒
        if (!isStarted && !isStarting && PermissionUtils.hasRequiredPermissions(this)) {
            oneKeyStartAvatar();
        }
    }

    private void initViews() {
        videoViewAva = findViewById(R.id.video_view_ava);
        ivAvatarBg = findViewById(R.id.iv_avatar_bg);
        btnHistory = findViewById(R.id.btn_history);
        btnSwitchAvatar = findViewById(R.id.btn_switch_avatar);
        tvStatus = findViewById(R.id.tv_status);
        etInputText = findViewById(R.id.et_input_text);
        btnSend = findViewById(R.id.btn_send);
        btnVoiceRecord = findViewById(R.id.btn_voice_record);

        chipWeather = findViewById(R.id.chip_weather);
        chipSchedule = findViewById(R.id.chip_schedule);
        chipMedicine = findViewById(R.id.chip_medicine);

        navHome = findViewById(R.id.nav_home);
        navFeatures = findViewById(R.id.nav_features);
        navMessages = findViewById(R.id.nav_messages);
        navProfile = findViewById(R.id.nav_profile);

        updateUIState();
        updateStatus("准备就绪，正在唤醒虚拟人...");
        // 应用已保存的背景（如果有）
        applySavedBackground();
        // 更新问候语为选中形象的名称
        try {
            String name = AvatarSelectActivity.getSelectedAvatarName(this);
            if (name != null && !name.trim().isEmpty()) {
                TextView tvGreeting = findViewById(R.id.tv_greeting);
                if (tvGreeting != null) {
                    tvGreeting.setText("爷爷奶奶好！我是" + name + "，今天想聊点什么呢？");
                }
            }
        } catch (Exception ignored) {}
    }

    private void setListeners() {
        btnHistory.setOnClickListener(v ->
                Toast.makeText(this, "历史对话页暂未实现", Toast.LENGTH_SHORT).show()
        );
        btnSwitchAvatar.setOnClickListener(v -> {
            Intent intent = new Intent(this, AvatarSelectActivity.class);
            startActivityForResult(intent, 100);
        });

        btnSend.setOnClickListener(v -> sendTextFromInput());

        // 点击录音按钮：先校验权限，再创建录音器并显示开始/结束录音控件（显式交互）
        btnVoiceRecord.setOnClickListener(v -> {
            try {
                if (!PermissionUtils.hasRequiredPermissions(HomeActivity.this)) {
                    // 请求缺失的权限，回调中会创建录音器或在许可后继续
                    Toast.makeText(HomeActivity.this, "请先授予麦克风权限", Toast.LENGTH_SHORT).show();
                    PermissionUtils.requestRequiredPermissions(HomeActivity.this);
                    // 标记为待创建（onRequestPermissionsResult 中会处理）
                    pendingStartRecordAfterStart = true;
                    return;
                }

                if (recorder == null) {
                    createRecorder();
                }
                showAudioInteractionButtons();
            } catch (Exception e) {
                Log.w(TAG, "voice button click failed", e);
                Toast.makeText(HomeActivity.this, "创建录音器失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        chipWeather.setOnClickListener(v -> sendQuickPhrase(chipWeather.getText().toString()));
        chipSchedule.setOnClickListener(v -> sendQuickPhrase(chipSchedule.getText().toString()));
        chipMedicine.setOnClickListener(v -> sendQuickPhrase(chipMedicine.getText().toString()));

        // 底部导航
        // 设置首页为选中状态
        setNavSelected(navHome, true);
        
        navHome.setOnClickListener(v -> {
            // 当前页面，不需要跳转
            setNavSelected(navHome, true);
        });
        
        navFeatures.setOnClickListener(v -> {
            setNavSelected(navFeatures, true);
            Intent intent = new Intent(this, FeaturesActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        });
        
        navMessages.setOnClickListener(v -> {
            setNavSelected(navMessages, true);
            Intent intent = new Intent(this, MessagesActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        });
        
        navProfile.setOnClickListener(v -> {
            setNavSelected(navProfile, true);
            Intent intent = new Intent(this, ProfileActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        });
    }

    private void sendQuickPhrase(String text) {
        if (TextUtils.isEmpty(text)) return;
        etInputText.setText(text);
        etInputText.setSelection(text.length());
        sendText(text);
    }

    private void sendTextFromInput() {
        String text = etInputText.getText().toString().trim();
        if (TextUtils.isEmpty(text)) {
            Toast.makeText(this, "请输入要发送的文字", Toast.LENGTH_SHORT).show();
            return;
        }
        sendText(text);
    }

    private void sendText(String text) {
        if (!isStarted) {
            Toast.makeText(this, "虚拟人尚未就绪，正在唤醒...", Toast.LENGTH_SHORT).show();
            if (!isStarting && PermissionUtils.hasRequiredPermissions(this)) {
                oneKeyStartAvatar();
            }
            return;
        }

        // 确保SDK已初始化
        if (!isInitialized) {
            Toast.makeText(this, "SDK未初始化，请等待初始化完成", Toast.LENGTH_SHORT).show();
            Log.w(TAG, "尝试发送文字，但SDK未初始化");
            return;
        }

        try {
            AvatarPlayController controller = AvatarUtils.getControllerSafely();
            if (controller == null) {
                String errorMsg = "控制器未初始化 (错误码600001)";
                Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show();
                Log.e(TAG, "✗ " + errorMsg);
                // 重置状态，允许重新初始化
                isInitialized = false;
                isStarted = false;
                return;
            }
            // 使用保存的voiceId
            String vcn = AvatarSelectActivity.getSelectedVoiceId(this);
            TextParams textParams = AvatarUtils.buildTextParams(text, true, vcn, true);
            controller.writeText(text, textParams);
            etInputText.setText("");
            updateStatus("已发送：" + text);
            Log.d(TAG, "✓ 文字发送成功: " + text);
        } catch (Exception e) {
            Log.e(TAG, "✗ 发送文字失败", e);
            Toast.makeText(this, "发送失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 在输入框位置显示开始/结束录音两个按钮，替代文本输入与发送按钮
     */
    private void showAudioInteractionButtons() {
        try {
            if (etInputText == null) return;

            // 隐藏输入区
            etInputText.setVisibility(View.GONE);
            if (btnSend != null) btnSend.setVisibility(View.GONE);
            // 立即隐藏底部的小语音按钮，避免其在 overlay 区域被显示或影响布局
            if (btnVoiceRecord != null) btnVoiceRecord.setVisibility(View.GONE);

            ViewGroup root = findViewById(android.R.id.content);
            if (root == null) {
                Log.w(TAG, "showAudioInteractionButtons: root not found");
                return;
            }

            // density 在后续会用到，提前计算
            float density = getResources().getDisplayMetrics().density;
            // 计算输入区域在根视图中的位置（以便把 overlay 放在输入框位置）
            View inputArea = findViewById(R.id.input_area);
            int topMargin = (int) (density * 0); // fallback
            int overlayHeight = ViewGroup.LayoutParams.WRAP_CONTENT;
            if (inputArea != null) {
                try {
                    int[] locInput = new int[2];
                    int[] locRoot = new int[2];
                    inputArea.getLocationOnScreen(locInput);
                    root.getLocationOnScreen(locRoot);
                    topMargin = locInput[1] - locRoot[1];
                    overlayHeight = inputArea.getHeight() > 0 ? inputArea.getHeight() : (int) (56 * density);
                } catch (Exception ignored) {}
            } else {
                // fallback: place above bottom nav
                topMargin = root.getHeight() - (int) (density * 200);
                overlayHeight = (int) (56 * density);
            }

            // 如果 overlay 尚不存在，创建并添加到根视图；如果存在则更新其布局参数
            if (audioOverlayContainer == null) {
                FrameLayout overlay = new FrameLayout(this);
                audioOverlayContainer = overlay;
                audioOverlayContainer.setClickable(true);
                FrameLayout.LayoutParams overlayLp = new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT, overlayHeight);
                // 使用 topMargin 将 overlay 放在输入框位置
                overlayLp.gravity = Gravity.TOP | Gravity.START;
                overlayLp.setMargins(0, topMargin, 0, 0);
                root.addView(audioOverlayContainer, overlayLp);
                audioOverlayContainer.bringToFront();
            } else {
                // 清理 overlay 内的所有子视图，保证重建时只有期望的开始/结束按钮
                audioOverlayContainer.removeAllViews();
                // 更新 layout params 使其定位到输入框
                try {
                    ViewGroup.LayoutParams lp = audioOverlayContainer.getLayoutParams();
                    if (lp instanceof FrameLayout.LayoutParams) {
                        FrameLayout.LayoutParams flp = (FrameLayout.LayoutParams) lp;
                        flp.height = overlayHeight;
                        flp.topMargin = topMargin;
                        flp.gravity = Gravity.TOP | Gravity.START;
                        audioOverlayContainer.setLayoutParams(flp);
                    }
                } catch (Exception ignored) {}
                audioOverlayContainer.bringToFront();
            }

            // 创建横向布局放置按钮
            android.widget.LinearLayout btnLayout = new android.widget.LinearLayout(this);
            btnLayout.setOrientation(android.widget.LinearLayout.HORIZONTAL);
            int pad = (int) (8 * getResources().getDisplayMetrics().density);
            btnLayout.setPadding(pad, pad, pad, pad);
            // 背景设为白色（去掉浅绿色）
            try {
                btnLayout.setBackgroundColor(Color.parseColor("#FFFFFF"));
            } catch (Exception ignored) {}
            btnLayout.setGravity(Gravity.CENTER);

            // 每次展示时重新创建开始/结束按钮，避免复用导致状态/父视图异常
            btnStartRecordCtrl = new Button(this);
            btnStartRecordCtrl.setText("开始录音");
            btnStartRecordCtrl.setAllCaps(false);
            btnStartRecordCtrl.setFocusable(true);
            btnStartRecordCtrl.setClickable(true);
            btnStartRecordCtrl.setOnClickListener(v -> {
                Log.d(TAG, "btnStartRecordCtrl clicked");
                if (recorder == null) {
                    Toast.makeText(this, "正在创建录音器，请稍候后再点击开始", Toast.LENGTH_SHORT).show();
                    createRecorder();
                    return;
                }
                startVoiceRecord();
            });

            btnStopRecordCtrl = new Button(this);
            btnStopRecordCtrl.setText("结束录音");
            btnStopRecordCtrl.setAllCaps(false);
            btnStopRecordCtrl.setFocusable(true);
            btnStopRecordCtrl.setClickable(true);
            btnStopRecordCtrl.setOnClickListener(v -> {
                Log.d(TAG, "btnStopRecordCtrl clicked");
                if (!isRecording) {
                    Toast.makeText(this, "当前未处于录音状态", Toast.LENGTH_SHORT).show();
                } else {
                    stopVoiceRecord();
                }
                hideAudioInteractionButtons();
            });

            // 添加按钮到布局并显示（使用 LinearLayout.LayoutParams 保证在水平布局下正确显示）
            android.widget.LinearLayout.LayoutParams childLp = new android.widget.LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            int childMargin = (int) (12 * getResources().getDisplayMetrics().density);
            childLp.setMargins(childMargin, 0, childMargin, 0);
            btnStartRecordCtrl.setEnabled(true);
            btnStopRecordCtrl.setEnabled(true);
            // 确保按钮不重复添加（重复调用时可能已存在）
            if (btnStartRecordCtrl.getParent() == null) btnLayout.addView(btnStartRecordCtrl, childLp);
            if (btnStopRecordCtrl.getParent() == null) btnLayout.addView(btnStopRecordCtrl, childLp);
            // 将按钮容器填满 overlay 并居中显示（确保按钮位于输入框区域中心）
            FrameLayout.LayoutParams btnLp = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            btnLp.gravity = Gravity.CENTER;
            btnLp.setMargins(0, 0, 0, 0);
            audioOverlayContainer.addView(btnLayout, btnLp);
            btnStartRecordCtrl.setVisibility(View.VISIBLE);
            btnStopRecordCtrl.setVisibility(View.VISIBLE);
            // 设置按钮颜色样式
            try {
                btnStartRecordCtrl.setBackgroundColor(Color.parseColor("#388E3C"));
                btnStartRecordCtrl.setTextColor(Color.WHITE);
                btnStopRecordCtrl.setBackgroundColor(Color.parseColor("#D32F2F"));
                btnStopRecordCtrl.setTextColor(Color.WHITE);
            } catch (Exception ignored) {}
            // 底部小麦克风已在前面隐藏，无需重复操作
            // 确保 overlay 可见（可能之前被隐藏）
            audioOverlayContainer.setVisibility(View.VISIBLE);
        } catch (Throwable t) {
            Log.w(TAG, "showAudioInteractionButtons failed", t);
        }
    }

    /**
     * 隐藏开始/结束录音按钮，恢复文本输入与发送按钮
     */
    private void hideAudioInteractionButtons() {
        try {
            if (btnStartRecordCtrl != null) btnStartRecordCtrl.setVisibility(View.GONE);
            if (btnStopRecordCtrl != null) btnStopRecordCtrl.setVisibility(View.GONE);
            if (etInputText != null) etInputText.setVisibility(View.VISIBLE);
            if (btnSend != null) btnSend.setVisibility(View.VISIBLE);
            // 恢复底部的小语音按钮显示
            if (btnVoiceRecord != null) btnVoiceRecord.setVisibility(View.VISIBLE);
            // 清理并隐藏 overlay 容器，保留对象以便重用
            if (audioOverlayContainer != null) {
                audioOverlayContainer.removeAllViews();
                audioOverlayContainer.setVisibility(View.GONE);
            }
            // 清除持有的控件引用，下一次创建新的按钮实例
            btnStartRecordCtrl = null;
            btnStopRecordCtrl = null;
        } catch (Throwable t) {
            Log.w(TAG, "hideAudioInteractionButtons failed", t);
        }
    }

    /**
     * 启动即唤醒：初始化SDK -> 创建播放器 -> 设置参数 -> 启动虚拟人 -> 绑定录音器
     */
    private void oneKeyStartAvatar() {
        if (isStarted) {
            Log.d(TAG, "虚拟人已启动，跳过重复启动");
            return;
        }
        if (isStarting) {
            Log.d(TAG, "虚拟人正在启动中，跳过重复启动");
            return;
        }

        // 权限
        if (!PermissionUtils.hasRequiredPermissions(this)) {
            updateStatus("请先授予权限");
            PermissionUtils.requestRequiredPermissions(this);
            return;
        }

        isStarting = true;
        updateStatus("正在唤醒虚拟人...");
        updateUIState();

        String appId = AvatarConfig.DEFAULT_APP_ID;
        String apiKey = AvatarConfig.DEFAULT_API_KEY;
        String apiSecret = AvatarConfig.DEFAULT_API_SECRET;

        if (TextUtils.isEmpty(appId) || TextUtils.isEmpty(apiKey) || TextUtils.isEmpty(apiSecret)) {
            updateStatus("请先在 AvatarConfig 配置 APP_ID / API_KEY / API_SECRET");
            Toast.makeText(this, "请先配置 SDK 参数", Toast.LENGTH_LONG).show();
            Log.e(TAG, "SDK配置参数缺失 - appId: " + (!TextUtils.isEmpty(appId)) + 
                  ", apiKey: " + (!TextUtils.isEmpty(apiKey)) + 
                  ", apiSecret: " + (!TextUtils.isEmpty(apiSecret)));
            isStarting = false;
            updateUIState();
            return;
        }

        // 检查SDK是否已经初始化
        boolean sdkInitialized = AvatarUtils.isSDKInitialized();
        if (sdkInitialized && isInitialized) {
            Log.d(TAG, "SDK已初始化，直接创建播放器");
            // 再次验证控制器是否可用
            AvatarPlayController controller = AvatarUtils.getControllerSafely();
            if (controller != null) {
                createPlayerAndStart();
            } else {
                Log.w(TAG, "SDK标记为已初始化，但控制器为null，重新初始化");
                isInitialized = false;
                // 继续执行初始化流程
            }
        }
        
        // 如果SDK未初始化，需要初始化
        if (!isInitialized) {
            Log.d(TAG, "开始初始化SDK - appId: " + appId);
            boolean initStarted = AvatarUtils.initializeSDK(this, appId, apiKey, apiSecret, (code, desc) -> runOnUiThread(() -> {
                Log.d(TAG, "SDK初始化回调 - code: " + code + ", desc: " + desc);
                if (AvatarError.SUCCESS.getErrorCode().equals(code)) {
                    // 验证控制器是否真的可用
                    AvatarPlayController controller = AvatarUtils.getControllerSafely();
                    if (controller == null) {
                        isStarting = false;
                        String errorMsg = "SDK初始化成功，但控制器不可用";
                        updateStatus(errorMsg);
                        Log.e(TAG, errorMsg);
                        Toast.makeText(HomeActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                        updateUIState();
                        return;
                    }
                    isInitialized = true;
                    Log.d(TAG, "✓ SDK初始化成功，控制器可用，开始创建播放器");
                    createPlayerAndStart();
                } else {
                    isStarting = false;
                    String errorMsg = "SDK初始化失败: " + desc + " (code: " + code + ")";
                    updateStatus(errorMsg);
                    Log.e(TAG, "✗ " + errorMsg);
                    Toast.makeText(HomeActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                    updateUIState();
                }
            }));
            
            if (!initStarted) {
                isStarting = false;
                String errorMsg = "SDK初始化启动失败，请检查配置参数";
                updateStatus(errorMsg);
                Log.e(TAG, "✗ " + errorMsg);
                Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
                updateUIState();
            }
        }
    }

    private void createPlayerAndStart() {
        updateStatus("正在创建播放器...");
        Log.d(TAG, "开始创建播放器");

        IAvatarListener playerListener = new IAvatarListener() {
            @Override
            public void onResult(String dataType, byte[] data, String extra) {}

            @Override
            public void onEvent(String eventType, String extra) {
                runOnUiThread(() -> {
                    Log.d(TAG, "播放器事件: " + eventType + (extra != null ? ", extra: " + extra : ""));
                    if ("first_frame".equals(eventType)) {
                        updateStatus("虚拟人已就绪");
                        cancelFirstFrameWatchdog();
                        Log.d(TAG, "✓ 收到首帧，虚拟人已就绪");
                    }
                });
            }

            @Override
            public void onError(String errorCode, String errorDesc, String extra) {
                runOnUiThread(() -> {
                    String errorMsg = "播放器错误: " + errorDesc + (errorCode != null ? " (code: " + errorCode + ")" : "");
                    updateStatus(errorMsg);
                    Log.e(TAG, "✗ " + errorMsg + (extra != null ? ", extra: " + extra : ""));
                    
                    // 特殊处理播放器错误码
                    if ("700003".equals(errorCode)) {
                        // 700003: callback is empty, enter room fail
                        String callbackErrorMsg = "播放器回调错误 (700003)，正在重新创建播放器...";
                        Log.e(TAG, callbackErrorMsg);
                        Toast.makeText(HomeActivity.this, "播放器回调错误，正在重试...", Toast.LENGTH_SHORT).show();
                        
                        // 重新创建播放器并启动
                        if (isStarted || isStarting) {
                            Log.d(TAG, "播放器回调错误，尝试重新创建播放器");
                            uiHandler.postDelayed(() -> {
                                if (!isStarting) {
                                    stopAvatar(false);
                                    uiHandler.postDelayed(() -> {
                                        if (!isStarting && isInitialized) {
                                            Log.d(TAG, "重新创建播放器并启动虚拟人（修复回调问题）");
                                            isStarting = true;
                                            createPlayerAndStart();
                                        } else if (!isStarting) {
                                            oneKeyStartAvatar();
                                        }
                                    }, 800);
                                }
                            }, 500);
                        }
                    } else if ("700004".equals(errorCode)) {
                        String timeoutMsg = "播放超时 (700004)，可能原因：\n" +
                                           "1. 网络连接较慢或不稳定\n" +
                                           "2. 服务器响应延迟\n" +
                                           "3. 首次连接需要更长时间\n" +
                                           "建议：检查网络后重试";
                        Log.e(TAG, timeoutMsg);
                        Toast.makeText(HomeActivity.this, "播放超时，请检查网络后重试", Toast.LENGTH_LONG).show();
                        
                        // 如果虚拟人已启动，可以尝试重新启动
                        if (isStarted) {
                            Log.d(TAG, "播放超时，尝试重新启动虚拟人（保持SDK初始化状态）");
                            uiHandler.postDelayed(() -> {
                                if (!isStarting) {
                                    // 不重置初始化状态，直接重新创建播放器和启动
                                    stopAvatar(false);
                                    uiHandler.postDelayed(() -> {
                                        if (!isStarting && isInitialized) {
                                            Log.d(TAG, "重新创建播放器并启动虚拟人");
                                            createPlayerAndStart();
                                        } else if (!isStarting) {
                                            // 如果SDK未初始化，重新初始化
                                            oneKeyStartAvatar();
                                        }
                                    }, 1000);
                                }
                            }, 2000);
                        }
                    } else {
                        // 其他播放器错误
                        Toast.makeText(HomeActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        };

        try {
            player = AvatarUtils.createStreamPlayer(this, videoViewAva, playerListener);
            if (player == null) {
                isStarting = false;
                updateStatus("播放器创建失败");
                Log.e(TAG, "播放器创建失败 - createStreamPlayer返回null");
                Toast.makeText(this, "播放器创建失败，请检查日志", Toast.LENGTH_LONG).show();
                updateUIState();
                return;
            }
            isPlayerCreated = true;
            Log.d(TAG, "✓ 播放器创建成功");
            startFirstFrameWatchdog();
            startAvatarWithParams();
        } catch (Exception e) {
            isStarting = false;
            updateStatus("播放器创建异常: " + e.getMessage());
            Log.e(TAG, "播放器创建异常", e);
            Toast.makeText(this, "播放器创建异常: " + e.getMessage(), Toast.LENGTH_LONG).show();
            updateUIState();
        }
    }

    private void startFirstFrameWatchdog() {
        cancelFirstFrameWatchdog();
        firstFrameTimeoutRunnable = () -> {
            if (!isStarted) return;
            updateStatus("加载超时：未收到首帧（可尝试重试/检查网络）");
            Log.w(TAG, "首帧超时监控触发，但虚拟人可能仍在加载中");
            Toast.makeText(this, "虚拟人加载超时，请检查网络后重试", Toast.LENGTH_LONG).show();
        };
        // 使用更长的超时时间，与播放器超时时间一致
        uiHandler.postDelayed(firstFrameTimeoutRunnable, FIRST_FRAME_TIMEOUT_MS);
    }

    private void cancelFirstFrameWatchdog() {
        if (firstFrameTimeoutRunnable != null) {
            uiHandler.removeCallbacks(firstFrameTimeoutRunnable);
            firstFrameTimeoutRunnable = null;
        }
    }

    private void startAvatarWithParams() {
        updateStatus("正在启动虚拟人...");
        Log.d(TAG, "开始设置参数并启动虚拟人");
        
        // 确保SDK已初始化
        if (!isInitialized) {
            isStarting = false;
            updateStatus("SDK未初始化，无法启动虚拟人");
            Log.e(TAG, "✗ SDK未初始化，isInitialized=false");
            Toast.makeText(this, "SDK未初始化，请重新启动", Toast.LENGTH_LONG).show();
            updateUIState();
            return;
        }
        
        try {
            AvatarPlayController controller = AvatarUtils.getControllerSafely();
            if (controller == null) {
                isStarting = false;
                updateStatus("获取控制器失败 (错误码600001)");
                Log.e(TAG, "✗ 获取控制器失败 - AvatarPlatform.getController()返回null (错误码600001)");
                Log.e(TAG, "可能原因：1. SDK初始化未完成 2. 初始化失败但未正确处理");
                Toast.makeText(this, "获取控制器失败 (600001)，请检查SDK初始化状态", Toast.LENGTH_LONG).show();
                // 重置初始化状态，允许重新初始化
                isInitialized = false;
                updateUIState();
                return;
            }
            Log.d(TAG, "✓ 控制器获取成功");

            // 从SharedPreferences获取保存的虚拟人配置
            String anchorId = AvatarSelectActivity.getSelectedAvatarId(this);
            String vcn = AvatarSelectActivity.getSelectedVoiceId(this);
            Log.d(TAG, "配置参数 - anchorId: " + anchorId + ", vcn: " + vcn);
            
            AvatarParams params = AvatarUtils.buildDefaultParams(anchorId, vcn);
            // 尝试将 Avatar 的渲染尺寸设置为当前渲染容器的像素尺寸，确保虚拟人铺满容器
            try {
                AvatarParams.Avatar avatarCfg = params.getAvatar();
                if (avatarCfg != null && videoViewAva != null) {
                    int vw = videoViewAva.getWidth() > 0 ? videoViewAva.getWidth() : videoViewAva.getMeasuredWidth();
                    int vh = videoViewAva.getHeight() > 0 ? videoViewAva.getHeight() : videoViewAva.getMeasuredHeight();
                    if (vw > 0 && vh > 0) {
                        avatarCfg.setWidth(vw);
                        avatarCfg.setHeight(vh);
                        Log.d(TAG, "设置 avatar 渲染尺寸为: " + vw + "x" + vh);
                    } else {
                        Log.w(TAG, "videoViewAva 尚未测量，未设置 avatar 尺寸（vw=" + vw + ", vh=" + vh + "）");
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, "设置 avatar 渲染尺寸失败", e);
            }
            if (params == null) {
                isStarting = false;
                updateStatus("参数构建失败");
                Log.e(TAG, "✗ 参数构建失败");
                updateUIState();
                return;
            }
            
            controller.setGlobalParams(params);
            Log.d(TAG, "✓ 全局参数已设置");
            
            if (player == null) {
                isStarting = false;
                updateStatus("播放器未创建");
                Log.e(TAG, "✗ 播放器为null，无法设置到控制器");
                updateUIState();
                return;
            }
            
            // 确保播放器回调已设置（在设置到控制器之前）
            // 播放器的回调在createStreamPlayer时已经通过playerListener设置
            // 先确保控制器没有旧的播放器引用（在stopAvatar中已经清理，这里再次确保）
            try {
                controller.setStreamPlayer(null);
                Log.d(TAG, "✓ 已清理控制器旧播放器引用");
            } catch (Exception e) {
                Log.w(TAG, "清理控制器旧播放器引用时出现异常", e);
            }
            
            // 设置播放器到控制器（确保播放器回调已正确设置）
            controller.setStreamPlayer(player);
            Log.d(TAG, "✓ 播放器已设置到控制器（回调已设置）");

            controller.addAvatarListener(new IAvatarListener() {
                @Override
                public void onResult(String dataType, byte[] data, String extra) {
                    // 这里可接入"历史对话"数据源（先不做）
                    if (data == null || data.length == 0) return;
                    try {
                        String result = new String(data, "UTF-8");
                        if ("asr".equals(dataType)) {
                            Log.d(TAG, "✓ ASR识别结果: " + result);
                        } else if ("nlp".equals(dataType)) {
                            Log.d(TAG, "✓ NLP语义理解结果: " + result);
                        } else {
                            Log.d(TAG, "收到数据 - type: " + dataType + ", data: " + result);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "处理回调数据失败", e);
                    }
                }

                @Override
                public void onEvent(String eventType, String extra) {
                    Log.d(TAG, "Avatar事件: " + eventType + (extra != null ? ", extra: " + extra : ""));
                    runOnUiThread(() -> {
                        if ("server_connect_success".equals(eventType)) {
                            Log.d(TAG, "✓ 服务器连接成功");
                        } else if ("stream_start".equals(eventType)) {
                            Log.d(TAG, "✓ 开始推流");
                        } else if ("frame_start".equals(eventType)) {
                            Log.d(TAG, "✓ 开始播报");
                        } else if ("frame_end".equals(eventType)) {
                            Log.d(TAG, "✓ 结束播报");
                        }
                    });
                }

                @Override
                public void onError(String errorCode, String errorDesc, String extra) {
                    runOnUiThread(() -> {
                        String errorMsg = "错误: " + errorDesc + (errorCode != null ? " (code: " + errorCode + ")" : "");
                        updateStatus(errorMsg);
                        Log.e(TAG, "✗ Avatar错误 - code: " + errorCode + ", desc: " + errorDesc + 
                              (extra != null ? ", extra: " + extra : ""));
                        
                        // 特殊处理常见错误码
                        if ("10107".equals(errorCode)) {
                            Log.e(TAG, "错误码10107通常表示ASR配置错误，请检查sceneId是否正确设置");
                            Toast.makeText(HomeActivity.this, "ASR配置错误，请检查sceneId", Toast.LENGTH_LONG).show();
                        } else if ("10108".equals(errorCode)) {
                            Log.e(TAG, "错误码10108通常表示TDP配置错误，请检查sceneId是否正确设置");
                            Toast.makeText(HomeActivity.this, "TDP配置错误，请检查sceneId", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(HomeActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            });
            Log.d(TAG, "✓ 监听器已注册");

            Log.d(TAG, "调用controller.start()启动虚拟人");
            controller.start();
            isStarted = true;
            // 标记虚拟人已启动（供录音器延后启动逻辑使用）
            isAvatarStarted = true;
            Log.d(TAG, "✓ controller.start()调用成功，isStarted已设置为true");

            // 启动后尝试创建录音器：有权限则创建，否则请求权限；同时处理此前的 pending 标志
            try {
                if (PermissionUtils.hasRequiredPermissions(this)) {
                    createRecorder();
                } else {
                    PermissionUtils.requestRequiredPermissions(this);
                }
            } catch (Exception e) {
                Log.w(TAG, "启动后创建录音器失败", e);
            }

            // 如果此前权限已授予但在虚拟人启动前用户触发了录音请求（pending），则在此尝试创建录音器
            try {
                if (pendingStartRecordAfterStart) {
                    if (PermissionUtils.hasRequiredPermissions(this)) {
                        if (recorder == null) {
                            createRecorder();
                        }
                        // 不自动开始录音，保留给用户手动触发（按住说话等交互）
                        pendingStartRecordAfterStart = false;
                        Log.d(TAG, "检测到待创建录音请求，已在虚拟人启动后创建录音器");
                    } else {
                        Log.d(TAG, "检测到待创建录音请求但权限仍未授予，继续等待权限回调");
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, "处理 pendingStartRecordAfterStart 时发生异常", e);
            }

            isStarting = false;
            updateStatus("可以开始和我聊天啦～");
            updateUIState();
            Log.d(TAG, "✓ 虚拟人启动流程完成");
        } catch (Exception e) {
            Log.e(TAG, "✗ 启动失败", e);
            isStarting = false;
            String errorMsg = "启动失败: " + e.getMessage();
            updateStatus(errorMsg);
            Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
            updateUIState();
        }
    }

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

            // 如果已创建播放器，尝试将 audioSessionId 注入到播放器参数，提升回放/通话体验
            if (player != null) {
                try {
                    StreamPlayerParams params = player.getPlayerParams();
                    if (params != null) {
                        params.setAudioSessionId(audioRecorder.getAudioSessionId());
                        player.setPlayerParams(params);
                        Log.d(TAG, "已将 audioSessionId 注入播放器: " + audioRecorder.getAudioSessionId());
                    }
                } catch (Throwable t) {
                    Log.w(TAG, "注入 audioSessionId 到播放器失败", t);
                }
            }

            // 构造默认的 audioParams（复用 demo 设置：启用 NLP，设置 ASR/VC 如需）
            AudioParams audioParams = new AudioParams();
            try {
                audioParams.setNlp(true);
            } catch (Throwable ignored) {}
            try {
                AvatarParams.ASR asr = new AvatarParams.ASR();
                asr.setFullDuplex(0);
                audioParams.setASR(asr);
            } catch (Throwable ignored) {}

            AvatarPlayController controller = AvatarPlatform.getController();
            if (controller != null) {
                try {
                    controller.setAudioRecorder(audioRecorder, audioParams);
                    Log.d(TAG, "已将录音器设置到控制器（含 audioParams）");
                    runOnUiThread(() -> Toast.makeText(HomeActivity.this, "录音器创建成功", Toast.LENGTH_SHORT).show());
                } catch (Throwable t) {
                    Log.e(TAG, "将录音器设置到控制器失败", t);
                    runOnUiThread(() -> Toast.makeText(HomeActivity.this, "设置录音器失败: " + t.getMessage(), Toast.LENGTH_LONG).show());
                }
            } else {
                Log.w(TAG, "控制器未就绪，录音器已创建但未绑定到控制器");
                runOnUiThread(() -> Toast.makeText(HomeActivity.this, "录音器已创建（待控制器就绪绑定）", Toast.LENGTH_SHORT).show());
            }
        } catch (Exception e) {
            Log.e(TAG, "创建录音器失败", e);
            final String msg = e.getMessage() != null ? e.getMessage() : e.toString();
            runOnUiThread(() -> Toast.makeText(HomeActivity.this,
                    "创建录音器异常: " + msg,
                    Toast.LENGTH_LONG).show());
        }
    }

    private void startVoiceRecord() {
        if (!isStarted) {
            Toast.makeText(this, "虚拟人尚未就绪", Toast.LENGTH_SHORT).show();
            return;
        }
        if (isRecording) return;
        try {
            AvatarPlayController controller = AvatarUtils.getControllerSafely();
            if (controller == null || controller.getRecorder() == null) {
                Toast.makeText(this, "录音器未创建", Toast.LENGTH_SHORT).show();
                return;
            }

            // 在开始录音前，先中断虚拟人的当前回答
            try {
                controller.interrupt();
                Log.d(TAG, "已中断虚拟人的当前回答");
            } catch (Throwable t) {
                Log.w(TAG, "controller.interrupt() 失败，继续开始录音", t);
            }

            Log.d(TAG, "尝试启动录音器 (controller.getRecorder().startRecord())...");
            boolean started = controller.getRecorder().startRecord();
            if (started) {
                Log.d(TAG, "controller.getRecorder().startRecord() 返回 true");
                isRecording = true;
                btnVoiceRecord.setSelected(true);
                updateStatus("正在录音，请说话...");
                runOnUiThread(() -> Toast.makeText(HomeActivity.this,
                        "诊断：已调用 SDK 录音开始",
                        Toast.LENGTH_SHORT).show());
            } else {
                Log.w(TAG, "开始录音失败：startRecord() 返回 false");
                Toast.makeText(this, "录音开启失败：请检查权限或麦克风是否被占用", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "开始录音失败", e);
            Toast.makeText(this, "开始录音失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void stopVoiceRecord() {
        if (!isRecording) return;
        try {
            AvatarPlayController controller = AvatarUtils.getControllerSafely();
            if (controller != null && controller.getRecorder() != null) {
                Log.d(TAG, "尝试停止录音器 (controller.getRecorder().stopRecord())...");
                controller.getRecorder().stopRecord();
                Log.d(TAG, "controller.getRecorder().stopRecord() 调用已返回");
            }
        } catch (Exception e) {
            Log.e(TAG, "停止录音失败", e);
        } finally {
            isRecording = false;
            btnVoiceRecord.setSelected(false);
            updateStatus("录音已停止，等待识别结果...");
            runOnUiThread(() -> Toast.makeText(HomeActivity.this,
                    "诊断：已调用 SDK 录音停止",
                    Toast.LENGTH_SHORT).show());
        }
    }

    private void updateUIState() {
        boolean canInteract = isStarted && !isStarting;
        if (etInputText != null) etInputText.setEnabled(canInteract);
        if (btnSend != null) btnSend.setEnabled(canInteract);
        if (btnVoiceRecord != null) btnVoiceRecord.setEnabled(canInteract);
    }

    private void updateStatus(String status) {
        if (tvStatus != null) {
            tvStatus.setText(status);
        }
    }
    
    /**
     * 点击输入框外任意位置时，关闭键盘并清除焦点，提升用户体验
     */
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            View v = getCurrentFocus();
            if (v instanceof EditText) {
                Rect outRect = new Rect();
                v.getGlobalVisibleRect(outRect);
                int x = (int) ev.getRawX();
                int y = (int) ev.getRawY();
                if (!outRect.contains(x, y)) {
                    hideKeyboard();
                    v.clearFocus();
                }
            }
        }
        return super.dispatchTouchEvent(ev);
    }

    private void hideKeyboard() {
        View view = getCurrentFocus();
        if (view == null) return;
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
    
    /**
     * 监听键盘显隐并在弹出时移动内容区域和输入框区域，确保输入框不被遮挡，导航栏保持固定
     */
    private void setupKeyboardListener() {
        final android.view.ViewGroup content = (android.view.ViewGroup) findViewById(android.R.id.content);
        if (content == null) return;

        // 获取需要移动的视图：内容区域和输入区域
        View scrollContent = findViewById(R.id.scroll_content);
        View inputArea = findViewById(R.id.input_area);

        keyboardListener = new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                try {
                    Rect r = new Rect();
                    content.getWindowVisibleDisplayFrame(r);
                    int screenHeight = content.getRootView().getHeight();
                    int keypadHeight = screenHeight - (r.bottom - r.top);
                    boolean isNowVisible = keypadHeight > screenHeight * 0.15;
                    if (isNowVisible && !keyboardVisible) {
                        keyboardVisible = true;
                        try {
                            // 计算输入区域底部与键盘顶部的重叠量，仅移动需要的距离（避免移动过高）
                            int translatePx = 0;
                            if (inputArea != null) {
                                int[] loc = new int[2];
                                inputArea.getLocationOnScreen(loc);
                                int inputBottom = loc[1] + inputArea.getHeight();
                                int keyboardTop = r.bottom;
                                int margin = (int) (8 * getResources().getDisplayMetrics().density); // 8dp 空隙
                                if (inputBottom > keyboardTop - margin) {
                                    translatePx = inputBottom - (keyboardTop - margin);
                                }
                            }

                            if (translatePx > 0) {
                                if (scrollContent != null) {
                                    scrollContent.animate().translationY(-translatePx).setDuration(220).setListener(null).start();
                                }
                                if (inputArea != null) {
                                    inputArea.animate().translationY(-translatePx).setDuration(220).setListener(null).start();
                                }
                            }
                        } catch (Exception ignore) {
                            // 退回到安全方案：整体按键盘高度移动
                            if (scrollContent != null) {
                                scrollContent.animate().translationY(-keypadHeight).setDuration(220).setListener(null).start();
                            }
                            if (inputArea != null) {
                                inputArea.animate().translationY(-keypadHeight).setDuration(220).setListener(null).start();
                            }
                        }
                    } else if (!isNowVisible && keyboardVisible) {
                        keyboardVisible = false;
                        // 恢复内容区域位置
                        if (scrollContent != null) {
                            scrollContent.animate().translationY(0).setDuration(220).setListener(null).start();
                        }
                        // 恢复输入区域位置
                        if (inputArea != null) {
                            inputArea.animate().translationY(0).setDuration(220).setListener(new AnimatorListenerAdapter() {}).start();
                        }
                    }
                } catch (Exception e) {
                    Log.w(TAG, "keyboard global layout failed", e);
                }
            }
        };

        // 使用内容视图来监听键盘变化
        View rootView = content.getChildCount() > 0 ? content.getChildAt(0) : content;
        if (rootView != null) {
            rootView.getViewTreeObserver().addOnGlobalLayoutListener(keyboardListener);
        }
    }
    
    /**
     * 设置导航栏选中状态
     * @param selectedView 选中的视图
     * @param selected 是否选中
     */
    private void setNavSelected(View selectedView, boolean selected) {
        // 重置所有导航项的选中状态
        navHome.setSelected(selectedView == navHome);
        navFeatures.setSelected(selectedView == navFeatures);
        navMessages.setSelected(selectedView == navMessages);
        navProfile.setSelected(selectedView == navProfile);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PermissionUtils.PERMISSION_REQUEST_CODE) {
            if (PermissionUtils.isPermissionGranted(grantResults)) {
                Toast.makeText(this, "权限授予成功", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "权限授予成功");
                // 如果虚拟人已经启动，则立即创建录音器（不自动开始录音）
                if (isStarted) {
                    try {
                        if (recorder == null) {
                            createRecorder();
                        }
                    } catch (Exception e) {
                        Log.w(TAG, "权限回调中创建录音器失败", e);
                    }
                } else {
                    // 否则标记为待创建，虚拟人启动后会处理
                    pendingStartRecordAfterStart = true;
                    Log.d(TAG, "权限已授予，待虚拟人启动后创建录音器");
                }
                // 仍然尝试唤醒虚拟人（如果尚未启动）
                if (!isStarted && !isStarting) {
                    oneKeyStartAvatar();
                }
            } else {
                Toast.makeText(this, "权限被拒绝，语音交互可能不可用", Toast.LENGTH_LONG).show();
                updateStatus("权限被拒绝：可先使用文字输入");
            }
        }
    }

    /**
     * 停止虚拟人
     * @param resetInitialized 是否重置初始化状态（默认false，保持SDK初始化状态以便重试）
     */
    private void stopAvatar() {
        stopAvatar(false);
    }
    
    /**
     * 停止虚拟人
     * @param resetInitialized 是否重置初始化状态
     */
    private void stopAvatar(boolean resetInitialized) {
        cancelFirstFrameWatchdog();
        stopVoiceRecord();
        try {
            AvatarPlayController controller = AvatarUtils.getControllerSafely();
            if (controller != null) {
                // 先从控制器移除播放器，避免回调为空的问题
                try {
                    controller.setStreamPlayer(null);
                    Log.d(TAG, "✓ 已从控制器移除播放器");
                } catch (Exception e) {
                    Log.w(TAG, "从控制器移除播放器时出现异常", e);
                }
                // 使用安全停止以避免 SDK 内部异常导致崩溃
                AvatarUtils.safeStopController();
                Log.d(TAG, "✓ 控制器已安全停止");
            }
        } catch (Exception e) {
            Log.w(TAG, "停止控制器时出现异常", e);
        }
        try {
            if (player != null) {
                player.stopPlay(true);
                player = null;
                Log.d(TAG, "✓ 播放器已停止并释放");
            }
        } catch (Exception e) {
            Log.w(TAG, "停止播放器时出现异常", e);
        }
        isStarted = false;
        isPlayerCreated = false;
        // 只有在明确要求时才重置初始化状态（例如完全退出时）
        // 这样可以在播放超时后重试，而不需要重新初始化SDK
        if (resetInitialized) {
            isInitialized = false;
            Log.d(TAG, "已重置SDK初始化状态");
        } else {
            Log.d(TAG, "保持SDK初始化状态，便于重试");
        }
        updateUIState();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 移除键盘监听，防止内存泄漏
        try {
            if (rootViewChild != null && keyboardListener != null) {
                rootViewChild.getViewTreeObserver().removeOnGlobalLayoutListener(keyboardListener);
            }
        } catch (Exception ignored) {}
        // 完全退出时，重置所有状态
        stopAvatar(true);
        recorder = null;
        player = null;
    }

    
    /** 
     * 尝试快速切换虚拟人形象（无完整重启），成功返回 true，失败返回 false
     */
    private boolean fastSwitchAvatar(String avatarId, String voiceId, Integer backgroundRes, String avatarName) {
        try {
            AvatarPlayController controller = AvatarUtils.getControllerSafely();
            if (controller == null) {
                Log.w(TAG, "Controller unavailable for fast switch");
                return false;
            }
            if (!isInitialized || !isPlayerCreated || player == null) {
                Log.w(TAG, "SDK/player 未准备好，无法快速切换 - isInitialized: " + isInitialized +
                        ", isPlayerCreated: " + isPlayerCreated + ", playerNull: " + (player == null));
                return false;
            }

            // 构建新的全局参数并下发（仅修改 avatarId 与 vcn）
            AvatarParams params = AvatarUtils.buildDefaultParams(avatarId, voiceId);
            // 可选：将背景资源写入 params（如果 SDK 支持的话）
            // if (backgroundRes != null) { params.getAvatar().setBackgroundRes(backgroundRes); }

            // 尝试更可靠的切换流程：先 stop controller，再设置参数并重新 start
            try {
                AvatarUtils.safeStopController();
            } catch (Exception e) {
                Log.w(TAG, "safeStopController() failed during fast switch (ignored)", e);
            }
            controller.setGlobalParams(params);
            // 确保播放器继续绑定
            controller.setStreamPlayer(player);
            try {
                controller.start();
            } catch (Exception e) {
                Log.w(TAG, "controller.start() failed during fast switch", e);
                // 如果 start 失败，继续回退到重启流程（返回 false）
                return false;
            }

            // 本地保存所选形象（与 AvatarSelectActivity 的保存保持一致）
            AvatarSelectActivity.saveSelectedAvatar(this, avatarId, voiceId);
            // 额外保存名字（如果提供）
            if (!TextUtils.isEmpty(avatarName)) {
                SharedPreferences sp = getSharedPreferences("avatar_prefs", Context.MODE_PRIVATE);
                sp.edit().putString("selected_avatar_id_name", avatarName).apply();
            }

            runOnUiThread(() -> {
                Toast.makeText(HomeActivity.this, "形象切换成功", Toast.LENGTH_SHORT).show();
                updateStatus("已切换形象");
                // 更新欢迎语（立即生效）
                try {
                    if (!TextUtils.isEmpty(avatarName)) {
                        TextView tvGreeting = findViewById(R.id.tv_greeting);
                        if (tvGreeting != null) {
                            tvGreeting.setText("爷爷奶奶好！我是" + avatarName + "，今天想聊点什么呢？");
                        }
                    }
                } catch (Exception ignored) {}
            });
            Log.d(TAG, "fastSwitchAvatar success - avatarId: " + avatarId + ", voiceId: " + voiceId);
            return true;
        } catch (Exception e) {
            Log.w(TAG, "fastSwitchAvatar 失败，回退到重启流程", e);
            return false;
        }
    }
    
    private void applySavedBackground() {
        if (ivAvatarBg == null) return;
        try {
            SharedPreferences sp = getSharedPreferences("avatar_prefs", Context.MODE_PRIVATE);
            int bgRes = sp.getInt("selected_avatar_id_bg_res", -1);
            if (bgRes != -1) {
                ivAvatarBg.setImageResource(bgRes);
                ivAvatarBg.setVisibility(View.VISIBLE);
            } else {
                ivAvatarBg.setVisibility(View.GONE);
            }
        } catch (Exception e) {
            Log.w(TAG, "applySavedBackground failed", e);
        }
    }

    private void applyBackgroundResource(Integer res) {
        if (ivAvatarBg == null) return;
        if (res == null) {
            ivAvatarBg.setVisibility(View.GONE);
            return;
        }
        try {
            ivAvatarBg.setImageResource(res.intValue());
            ivAvatarBg.setVisibility(View.VISIBLE);
        } catch (Exception e) {
            Log.w(TAG, "applyBackgroundResource failed for res: " + res, e);
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK && data != null) {
            String avatarId = data.getStringExtra("avatar_id");
            String voiceId = data.getStringExtra("voice_id");
            int bgRes = data.getIntExtra("background_res", -1);
            Integer bg = bgRes == -1 ? null : Integer.valueOf(bgRes);
            if (avatarId != null && voiceId != null) {
            // 仅当返回中包含背景资源时才应用新的背景，若未包含则保留当前背景
            if (bgRes != -1) {
                applyBackgroundResource(bg);
            }
                // 优先尝试快速切换（无停止播放器的开销）
                boolean fastSwitched = false;
                String avatarName = data.getStringExtra("avatar_name");
                if (isStarted) {
                    runOnUiThread(() -> Toast.makeText(this, "正在切换虚拟人形象...", Toast.LENGTH_SHORT).show());
                    fastSwitched = fastSwitchAvatar(avatarId, voiceId, bg, avatarName);
                }

                // 更新问候语为新的形象名称（立即反映）
                if (!TextUtils.isEmpty(avatarName)) {
                    TextView tvGreeting = findViewById(R.id.tv_greeting);
                    if (tvGreeting != null) {
                        String greeting = "爷爷奶奶好！我是" + avatarName + "，今天想聊点什么呢？";
                        tvGreeting.setText(greeting);
                    }
                }

                if (!fastSwitched) {
                    // 无法快速切换或虚拟人未启动，回退到原有的停止/重启流程
                    Toast.makeText(this, "已切换虚拟人形象，正在重启虚拟人...", Toast.LENGTH_SHORT).show();
                    if (isStarted) {
                        // 切换虚拟人时需要完全重启，但保持SDK初始化状态
                        stopAvatar(false);
                        // 延迟重启，确保旧播放器完全释放
                        uiHandler.postDelayed(() -> {
                            if (isInitialized) {
                                // SDK已初始化，直接创建播放器
                                // 重置启动状态，允许重新启动
                                isStarting = false;
                                createPlayerAndStart();
                            } else {
                                // SDK未初始化，重新初始化
                                oneKeyStartAvatar();
                            }
                        }, 800); // 增加延迟时间，确保旧播放器完全释放
                    } else {
                        // 如果虚拟人未启动，直接启动
                        if (!isStarting && PermissionUtils.hasRequiredPermissions(this)) {
                            oneKeyStartAvatar();
                        }
                    }
                }
            }
        }
    }
}


