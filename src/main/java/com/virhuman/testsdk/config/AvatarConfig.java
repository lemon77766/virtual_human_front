package com.virhuman.testsdk.config;

/**
 * 虚拟人SDK配置管理类
 * 包含默认配置和常用配置项
 */
public class AvatarConfig {
    
    // 默认配置值（已替换为用户提供的凭据）
    public static final String DEFAULT_APP_ID = "05df0ba0";
    public static final String DEFAULT_API_KEY = "2ecc97a82028cbfc4a62bae3d310db54";
    public static final String DEFAULT_API_SECRET = "YmIzNThmYWRiNjVmMDVkOWZkZGUzNWYx";
    // 接口服务ID（可选，某些场景下可能需要）
    public static final String SERVICE_ID = "259137167920599040";
    // 根据当前 Avatar 安卓 SDK 文档的云端服务地址
    public static final String DEFAULT_SERVER_URL = "wss://avatar.cn-huadong-1.xf-yun.com/v1/interact";
    public static final String DEFAULT_UID = "12345678";
    public static final String DEFAULT_LOG_PREFIX = "AVA_";
    
    // 虚拟人默认配置
    // 注意：avatar_id是必填参数，不能为null
    // 虚拟人ID：从科大讯飞控制台获取
    public static final String DEFAULT_ANCHOR_ID = "110026013";
    public static final int DEFAULT_VIDEO_WIDTH = 1280;
    public static final int DEFAULT_VIDEO_HEIGHT = 720;
    public static final double DEFAULT_SCALE = 1.0;
    public static final int DEFAULT_MOVE_H = 0;
    public static final int DEFAULT_MOVE_V = 0;
    
    // TTS默认配置
    // VCN（发音人）：从科大讯飞控制台获取
    public static final String DEFAULT_VCN = "x4_mingge";
    public static final int DEFAULT_SPEED = 50;
    public static final int DEFAULT_PITCH = 50;
    // TTS 音量（0-100 的整型）
    public static final int DEFAULT_TTS_VOLUME = 50;
    
    // 流媒体默认配置
    public static final String DEFAULT_PROTOCOL = "xrtc";
    public static final int DEFAULT_FPS = 25;
    public static final int DEFAULT_BITRATE = 2000;
    // 开启透明背景（xrtc 协议下有效）
    public static final int DEFAULT_ALPHA = 1;
    
    // 播放器默认配置
    // 播放器音量（0-1 的浮点数）
    public static final double DEFAULT_PLAYER_VOLUME = 0.5;
    // 首帧渲染超时（ms）。30s 在弱网/首次连接时可能超时，提高到 60s。
    // 如果网络较差，可以进一步增加到 90s 或 120s
    public static final int DEFAULT_TIMEOUT = 60000;
    
    // 音频录制配置
    public static final int SAMPLE_RATE = 16000;
    public static final int CHANNEL_CONFIG = android.media.AudioFormat.CHANNEL_IN_MONO;
    public static final int AUDIO_FORMAT = android.media.AudioFormat.ENCODING_PCM_16BIT;
    public static final int AUDIO_SOURCE = android.media.MediaRecorder.AudioSource.MIC;
    
    // 常用虚拟人形象ID
    public static class AvatarId {
        public static final String XIAOFENG = "xiaofeng";
        public static final String XIAOXUE = "xiaoxue";
        public static final String AIJIA = "aijia";
        public static final String AYONG = "ayong";
    }
    
    // 常用发音人
    public static class VoiceName {
        public static final String XIAOFENG = "xiaofeng";
        public static final String XIAOXUE = "xiaoxue";
        public static final String AIJIA = "aijia";
        public static final String AYONG = "ayong";
    }
    
    // 错误码常量
    public static class ErrorCodes {
        public static final String SUCCESS = "0";
        public static final String INVALID_PARAM = "10001";
        public static final String AUTH_FAILED = "10002";
        public static final String NETWORK_ERROR = "10003";
        public static final String RESOURCE_NOT_FOUND = "10004";
    }
    
    // 日志级别
    public enum LogLevel {
        VERBOSE(0), DEBUG(1), INFO(2), WARN(3), ERROR(4);
        
        private final int value;
        LogLevel(int value) {
            this.value = value;
        }
        
        public int getValue() {
            return value;
        }
    }
    
    // 验证配置参数
    public static boolean isValidConfig(String appId, String apiKey, String apiSecret) {
        return appId != null && !appId.trim().isEmpty() &&
               apiKey != null && !apiKey.trim().isEmpty() &&
               apiSecret != null && !apiSecret.trim().isEmpty();
    }
    
    // 验证数字范围
    public static boolean isValidRange(int value, int min, int max) {
        return value >= min && value <= max;
    }
    
    // 验证视频尺寸
    public static boolean isValidVideoSize(int width, int height) {
        return (width == 1280 && height == 720) || 
               (width == 720 && height == 480) ||
               (width == 640 && height == 360);
    }
    
    // 验证帧率
    public static boolean isValidFps(int fps) {
        return fps == 15 || fps == 20 || fps == 25;
    }
}