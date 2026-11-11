package com.virhuman.testsdk.utils;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.iflytek.avalibrary.AvatarPlatform;
import com.iflytek.avalibrary.AvatarPlatformConfig;
import com.iflytek.avalibrary.AvatarPlayController;
import com.iflytek.avalibrary.IAvatarListener;
import com.iflytek.avalibrary.constant.AvatarError;
import com.iflytek.avalibrary.params.AvatarParams;
import com.iflytek.avalibrary.params.TextParams;
import com.iflytek.avalibrary.params.AudioParams;
import com.iflytek.avalibrary.videoplayer.rt.StreamPlayerFactory;
import com.iflytek.avalibrary.videoplayer.rt.interfaces.IStreamPlayer;
import com.iflytek.avalibrary.videoplayer.params.StreamPlayerParams;
import com.iflytek.ys.core.log.LogLevel;
import com.iflytek.ys.core.recorder.AudioRecorder;
import com.virhuman.testsdk.config.AvatarConfig;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.MediaRecorder;

/**
 * 虚拟人SDK工具类
 * 提供简化的SDK操作接口
 */
public class AvatarUtils {
    private static final String TAG = "AvatarUtils";
    
    /**
     * 初始化SDK
     * @param context 上下文
     * @param appId 应用ID
     * @param apiKey API密钥
     * @param apiSecret API密钥
     * @param listener 初始化监听器
     * @return 是否成功开始初始化
     */
    public static boolean initializeSDK(Context context, String appId, String apiKey, String apiSecret, 
                                       AvatarPlatform.IInitListener listener) {
        if (!AvatarConfig.isValidConfig(appId, apiKey, apiSecret)) {
            Log.e(TAG, "配置参数无效");
            return false;
        }
        
        try {
            AvatarPlatformConfig.Builder config = new AvatarPlatformConfig.Builder();
            config.setAppId(appId);
            config.setApikey(apiKey);
            config.setApiSecret(apiSecret);
            config.setLogLevel(LogLevel.INFO);
            config.setServerUrl(AvatarConfig.DEFAULT_SERVER_URL);
            config.setUid(AvatarConfig.DEFAULT_UID);
            config.setLoggingPre(AvatarConfig.DEFAULT_LOG_PREFIX);
            
            AvatarPlatform.initialize(context, config.build(), listener);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "SDK初始化异常", e);
            return false;
        }
    }
    
    /**
     * 创建流媒体播放器
     * @param context 上下文
     * @param renderView 渲染视图
     * @param listener 播放器监听器
     * @return 播放器实例
     */
    public static IStreamPlayer createStreamPlayer(Context context, android.view.ViewGroup renderView, 
                                                   IAvatarListener listener) {
        try {
            IStreamPlayer player = StreamPlayerFactory.createPlayer(context, AvatarConfig.DEFAULT_PROTOCOL);
            player.setDebugLogging(true);
            
            if (renderView != null) {
                player.setRenderArea(renderView);
            }
            
            // 设置播放器参数
            StreamPlayerParams params = new StreamPlayerParams();
            // 播放器音量使用 0-1 的浮点配置
            params.setVolume(AvatarConfig.DEFAULT_PLAYER_VOLUME);
            params.setRenderTimeout(AvatarConfig.DEFAULT_TIMEOUT);
            params.setStreamType(AudioManager.STREAM_MUSIC);
            player.setPlayerParams(params);
            
            // 设置监听器
            if (listener != null) {
                player.setPlayerListener(listener);
            }
            
            return player;
        } catch (Exception e) {
            Log.e(TAG, "创建播放器失败", e);
            return null;
        }
    }
    
    /**
     * 创建录音器
     * @return 录音器实例
     */
    public static AudioRecorder createAudioRecorder() {
        try {
            Log.d(TAG, "创建录音器，参数: source=" + AvatarConfig.AUDIO_SOURCE +
                    ", sampleRate=" + AvatarConfig.SAMPLE_RATE +
                    ", channelConfig=" + AvatarConfig.CHANNEL_CONFIG +
                    ", audioFormat=" + AvatarConfig.AUDIO_FORMAT);
            return new AudioRecorder(
                AvatarConfig.AUDIO_SOURCE,
                AvatarConfig.SAMPLE_RATE,
                AvatarConfig.AUDIO_FORMAT,
                AvatarConfig.CHANNEL_CONFIG
            );
        } catch (Exception e) {
            Log.e(TAG, "创建录音器失败", e);
            return null;
        }
    }
    
    /**
     * 构建默认的全局参数
     * @param avatarId 虚拟人ID
     * @param vcn 发音人
     * @return 全局参数对象
     */
    public static AvatarParams buildDefaultParams(String avatarId, String vcn) {
        AvatarParams params = new AvatarParams();
        
        // 设置Avatar参数
        AvatarParams.Avatar avatar = new AvatarParams.Avatar();
        // avatar_id 是必填参数，不能为null
        String finalAvatarId = TextUtils.isEmpty(avatarId) ? AvatarConfig.DEFAULT_ANCHOR_ID : avatarId;
        // 如果还是空的，使用默认值 "xiaofeng"
        if (TextUtils.isEmpty(finalAvatarId)) {
            finalAvatarId = "xiaofeng";
        }
        avatar.setAvatarId(finalAvatarId);
        
        AvatarParams.Stream stream = new AvatarParams.Stream();
        stream.setProtocol(AvatarConfig.DEFAULT_PROTOCOL);
        stream.setFps(AvatarConfig.DEFAULT_FPS);
        stream.setBitrate(AvatarConfig.DEFAULT_BITRATE);
        stream.setAlpha(AvatarConfig.DEFAULT_ALPHA);
        avatar.setStream(stream);
        
        params.setAvatar(avatar);
        
        // 设置场景ID（必需参数，用于语音交互和NLP）
        // sceneId对应交互平台-接口服务-"接口服务ID"
        // 如果不设置sceneId，会导致错误码10107 (asr config error / tdp config error)
        if (!TextUtils.isEmpty(AvatarConfig.SERVICE_ID)) {
            AvatarParams.Scene scene = new AvatarParams.Scene();
            scene.setSceneId(AvatarConfig.SERVICE_ID);
            params.setScene(scene);
            Log.d(TAG, "✓✓✓ 已设置场景ID（sceneId）: " + AvatarConfig.SERVICE_ID + " - 这是语音交互的关键参数");
        } else {
            Log.e(TAG, "✗✗✗ 错误：场景ID（SERVICE_ID）为空！这会导致语音交互失败（错误码10107）");
            Log.e(TAG, "请在AvatarConfig.SERVICE_ID中设置接口服务ID");
        }
        
        // 设置TTS参数
        AvatarParams.TTS tts = new AvatarParams.TTS();
        // VCN（发音人）可选，如果不设置或认证失败，系统会使用虚拟人自带的默认发音
        String finalVcn = TextUtils.isEmpty(vcn) ? AvatarConfig.DEFAULT_VCN : vcn;
        // 如果VCN不为空，则设置；如果为空字符串，则不设置VCN，让系统使用默认发音
        if (!TextUtils.isEmpty(finalVcn)) {
            tts.setVcn(finalVcn);
        }
        tts.setSpeed(AvatarConfig.DEFAULT_SPEED);
        tts.setPitch(AvatarConfig.DEFAULT_PITCH);
        // TTS 音量使用 0-100 的整型配置
        tts.setVolume(AvatarConfig.DEFAULT_TTS_VOLUME);
        params.setTTS(tts);
        
        // 启用交互模式（NLP）和情感分析，支持语音和文字交互
        AvatarParams.Dispatch dispatch = new AvatarParams.Dispatch();
        dispatch.setContentAnalysis(1); // 启用情感分析
        // 启用交互模式，让音频数据自动进行NLP语义理解
        dispatch.setInteractiveMode(1); // 1-打断模式，支持实时交互
        params.setDispatch(dispatch);
        
        // 输出关键参数摘要
        Log.d(TAG, "=== 全局参数配置摘要 ===");
        Log.d(TAG, "avatarId: " + finalAvatarId);
        Log.d(TAG, "sceneId: " + (params.getScene() != null ? params.getScene().getSceneId() : "未设置"));
        Log.d(TAG, "vcn: " + (TextUtils.isEmpty(finalVcn) ? "使用默认" : finalVcn));
        Log.d(TAG, "contentAnalysis: " + dispatch.getContentAnalysis());
        Log.d(TAG, "interactiveMode: " + dispatch.getInteractiveMode());
        Log.d(TAG, "======================");
        
        return params;
    }
    
    /**
     * 构建文本参数
     * @param text 文本内容
     * @param isNlp 是否启用语义理解
     * @param vcn 发音人（可选）
     * @param isInteractive 是否交互模式
     * @return 文本参数对象
     */
    public static TextParams buildTextParams(String text, boolean isNlp, String vcn, boolean isInteractive) {
        if (TextUtils.isEmpty(text)) {
            return null;
        }
        
        TextParams textParams = new TextParams();
        textParams.setNlp(isNlp);
        
        if (isNlp) {
            // 设置交互模式
            AvatarParams.Dispatch dispatch = new AvatarParams.Dispatch();
            dispatch.setInteractiveMode(isInteractive ? 1 : 0); // 1-打断模式，0-追加模式
            textParams.setDispatch(dispatch);
            
            // 启用自动动作，让虚拟人根据语义自动做出动作
            AvatarParams.AIR air = new AvatarParams.AIR();
            air.setAir(1); // 启用自动动作
            textParams.setAIR(air);
        }
        
        // 设置TTS参数（如果指定了发音人）
        if (!TextUtils.isEmpty(vcn)) {
            AvatarParams.TTS tts = new AvatarParams.TTS();
            tts.setVcn(vcn);
            tts.setSpeed(AvatarConfig.DEFAULT_SPEED);
            tts.setPitch(AvatarConfig.DEFAULT_PITCH);
            tts.setVolume(AvatarConfig.DEFAULT_TTS_VOLUME);
            textParams.setTTS(tts);
        }
        
        return textParams;
    }
    
    /**
     * 构建动作指令文本
     * @param action 动作名称
     * @return 指令文本
     */
    public static String buildActionText(String action) {
        if (TextUtils.isEmpty(action)) {
            return "";
        }
        return "[[action=" + action + "]]";
    }
    
    /**
     * 检查SDK是否已初始化
     * @return 是否已初始化
     */
    public static boolean isSDKInitialized() {
        try {
            AvatarPlayController controller = AvatarPlatform.getController();
            return controller != null;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 安全获取控制器
     * @return 控制器实例，可能为null
     */
    public static AvatarPlayController getControllerSafely() {
        try {
            return AvatarPlatform.getController();
        } catch (Exception e) {
            Log.e(TAG, "获取控制器失败", e);
            return null;
        }
    }

    /**
     * 安全的写入音频到 SDK，包含空检查与异常保护，避免向服务端发送空 payload 导致错误码10106。
     * @param audio pcm 数据
     * @param status 帧状态
     * @param params 单次交互参数
     */
    public static void safeWriteAudio(byte[] audio, int status, AudioParams params) {
        try {
            if (audio == null || audio.length == 0) {
                Log.w(TAG, "safeWriteAudio: audio null or empty, skip write");
                return;
            }
            AvatarPlayController controller = getControllerSafely();
            if (controller == null) {
                Log.w(TAG, "safeWriteAudio: controller is null, skip write");
                return;
            }
            controller.writeAudio(audio, status, params);
        } catch (Throwable t) {
            Log.e(TAG, "safeWriteAudio failed", t);
        }
    }
    
    /**
     * 安全停止控制器并释放与播放器的绑定，避免 SDK 在销毁/停止时抛出未捕获异常（如 TelephonyManager 相关 NPE）
     */
    public static void safeStopController() {
        try {
            AvatarPlayController controller = getControllerSafely();
            if (controller == null) {
                Log.d(TAG, "safeStopController: controller is null, nothing to stop");
                return;
            }
            try {
                // 先解除播放器绑定以避免回调在停止过程中触发异常
                controller.setStreamPlayer(null);
            } catch (Throwable ignored) {
                Log.w(TAG, "safeStopController: setStreamPlayer failed", ignored);
            }
            try {
                controller.stop();
                Log.d(TAG, "safeStopController: controller.stop() success");
            } catch (Throwable t) {
                Log.e(TAG, "safeStopController: controller.stop() failed", t);
            }
        } catch (Throwable t) {
            Log.e(TAG, "safeStopController failed", t);
        }
    }
    
    /**
     * 显示Toast消息
     * @param context 上下文
     * @param message 消息内容
     */
    public static void showToast(Context context, String message) {
        if (context != null && !TextUtils.isEmpty(message)) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * 验证虚拟人ID格式
     * @param avatarId 虚拟人ID
     * @return 是否有效
     */
    public static boolean isValidAvatarId(String avatarId) {
        return !TextUtils.isEmpty(avatarId) && avatarId.matches("^[a-zA-Z0-9_-]+$");
    }
    
    /**
     * 验证发音人格式
     * @param vcn 发音人
     * @return 是否有效
     */
    public static boolean isValidVoiceName(String vcn) {
        return !TextUtils.isEmpty(vcn) && vcn.matches("^[a-zA-Z0-9_-]+$");
    }
}