package com.virhuman.testsdk.utils;

import android.content.Context;
import android.media.AudioFormat;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Looper;
import java.util.concurrent.atomic.AtomicReference;
import android.util.Log;

import com.iflytek.avalibrary.AvatarPlayController;
import com.iflytek.avalibrary.params.AudioParams;
import com.iflytek.avalibrary.params.AvatarParams;
import com.iflytek.avalibrary.videoplayer.rt.interfaces.IStreamPlayer;
import com.iflytek.avalibrary.videoplayer.params.StreamPlayerParams;
import com.iflytek.ys.core.recorder.AudioRecorder;
import com.virhuman.testsdk.config.AvatarConfig;
import com.virhuman.testsdk.utils.AvatarUtils;

/**
 * 音频交互管理器：封装录音器创建、绑定与启动/停止逻辑，复用 demo 中成熟实现方式。
 */
public class AudioInteractionManager {
    private static final String TAG = "AudioInteractionManager";

    /**
     * 创建录音器并绑定到 Avatar 控制器（同时尝试把 audioSessionId 注入到播放器）
     * @param context 上下文
     * @param fullDuplex 是否为全双工（使用 VOICE_COMMUNICATION）
     * @return AudioRecorder 实例（可能为 null）
     */
    public static AudioRecorder createAndBindRecorder(Context context, boolean fullDuplex) {
        try {
            int audioSource = fullDuplex ? MediaRecorder.AudioSource.VOICE_COMMUNICATION : MediaRecorder.AudioSource.MIC;
            AudioRecorder audioRecorder = new AudioRecorder(
                    audioSource,
                    AvatarConfig.SAMPLE_RATE,
                    AvatarConfig.AUDIO_FORMAT,
                    AvatarConfig.CHANNEL_CONFIG
            );
            Log.d(TAG, "createAndBindRecorder: audioSource=" + audioSource +
                    ", sampleRate=" + AvatarConfig.SAMPLE_RATE +
                    ", audioFormat=" + AvatarConfig.AUDIO_FORMAT +
                    ", channelConfig=" + AvatarConfig.CHANNEL_CONFIG);
            try {
                Log.d(TAG, "createAndBindRecorder: audioSessionId=" + audioRecorder.getAudioSessionId());
            } catch (Throwable t) {
                Log.w(TAG, "failed to get audioSessionId", t);
            }
            // 默认添加 PCM 回调：将音频数据写入 AvatarController（与 demo 保持一致）
            audioRecorder.addPcmDataListener(new AudioRecorder.PcmDataListener() {
                @Override
                public void onPcmData(byte[] pcmData, int size, int status) {
                    try {
                        if (pcmData == null) {
                            Log.w(TAG, "onPcmData: pcmData is null, skip");
                            return;
                        }
                        if (size <= 0) {
                            Log.w(TAG, "onPcmData: size <= 0, skip (status=" + status + ")");
                            return;
                        }

                        Log.d(TAG, "onPcmData: size=" + size + " status=" + status);
                        byte[] audioBytes = new byte[size];
                        System.arraycopy(pcmData, 0, audioBytes, 0, size);

                        AudioParams params = null;
                        if (status == 0) {
                            params = new AudioParams();
                            try {
                                params.setRequestId(String.valueOf(System.currentTimeMillis()) + "_" + AvatarConfig.DEFAULT_UID);
                            } catch (Throwable ignored) {}
                            try {
                                params.setNlp(true);
                            } catch (Throwable ignored) {}
                            try {
                                AvatarParams.Dispatch dispatch = new AvatarParams.Dispatch();
                                dispatch.setInteractiveMode(1); // 默认打断模式，复用 demo 的策略
                                params.setDispatch(dispatch);
                            } catch (Throwable ignored) {}
                        }

                        // 使用安全写音频的工具方法，统一做空检查与异常保护
                        AvatarUtils.safeWriteAudio(audioBytes, status, params);
                    } catch (Throwable e) {
                        Log.e(TAG, "onPcmData processing failed", e);
                    }
                }
            });

            // 绑定到 controller，并注入 audio params（启用 NLP）
            AvatarPlayController controller = AvatarUtils.getControllerSafely();
            if (controller != null) {
                AudioParams audioParams = new AudioParams();
                try { audioParams.setNlp(true); } catch (Throwable ignored) {}
                controller.setAudioRecorder(audioRecorder, audioParams);

                // 如果播放器已注入，尝试把 audioSessionId 注入播放器参数，避免音频冲突
                IStreamPlayer injectedStreamPlayer = controller.getStreamPlayer();
                if (injectedStreamPlayer != null) {
                    StreamPlayerParams params = injectedStreamPlayer.getPlayerParams();
                    try {
                        params.setAudioSessionId(audioRecorder.getAudioSessionId());
                        injectedStreamPlayer.setPlayerParams(params);
                    } catch (Throwable t) {
                        Log.w(TAG, "注入 audioSessionId 到播放器失败", t);
                    }
                }
            } else {
                Log.w(TAG, "controller is null when binding recorder");
            }

            return audioRecorder;
        } catch (Exception e) {
            Log.e(TAG, "createAndBindRecorder failed", e);
            return null;
        }
    }

    /**
     * 异步创建并绑定录音器，支持重试与指数回退。
     * 在主线程回调创建结果（成功或最终失败）。
     *
     * @param context 上下文
     * @param fullDuplex 是否使用 VOICE_COMMUNICATION
     * @param maxAttempts 最大尝试次数（>=1）
     * @param initialBackoffMs 初始回退时长（毫秒）
     * @param callback 创建结果回调（在主线程执行）
     */
    public static void createAndBindRecorderWithRetry(final Context context,
                                                      final boolean fullDuplex,
                                                      final int maxAttempts,
                                                      final long initialBackoffMs,
                                                      final RecorderCallback callback) {
        if (callback == null) {
            // 无回调则同步调用（保留旧行为）
            AudioRecorder r = createAndBindRecorder(context, fullDuplex);
            return;
        }

        new Thread(() -> {
            final AtomicReference<AudioRecorder> recorderRef = new AtomicReference<>(null);
            long backoff = Math.max(1, initialBackoffMs);
            int attempts = Math.max(1, maxAttempts);
            for (int i = 0; i < attempts; i++) {
                try {
                    Log.d(TAG, "createAndBindRecorderWithRetry attempt " + (i + 1) + "/" + attempts);
                    AudioRecorder created = createAndBindRecorder(context, fullDuplex);
                    recorderRef.set(created);
                    if (created != null) {
                        Log.d(TAG, "createAndBindRecorderWithRetry succeeded on attempt " + (i + 1));
                        break;
                    }
                } catch (Throwable t) {
                    Log.w(TAG, "createAndBindRecorderWithRetry attempt failed", t);
                    recorderRef.set(null);
                }

                if (i < attempts - 1) {
                    try {
                        Log.d(TAG, "backoff sleeping " + backoff + "ms before next attempt");
                        Thread.sleep(backoff);
                    } catch (InterruptedException ignored) {}
                    backoff = backoff * 2;
                }
            }

            // 在主线程回调结果
            new Handler(Looper.getMainLooper()).post(() -> {
                try {
                    callback.onRecorderCreated(recorderRef.get());
                } catch (Throwable ignored) {}
            });
        }, "CreateRecorderWithRetry").start();
    }

    /**
     * 录音器创建回调（主线程）
     */
    public interface RecorderCallback {
        void onRecorderCreated(AudioRecorder recorder);
    }

    /**
     * 启动控制器上的 recorder（如果存在），返回是否成功
     */
    public static boolean startRecording() {
        try {
            AvatarPlayController controller = AvatarUtils.getControllerSafely();
            if (controller != null && controller.getRecorder() != null) {
                return controller.getRecorder().startRecord();
            }
        } catch (Throwable t) {
            Log.e(TAG, "startRecording failed", t);
        }
        return false;
    }

    /**
     * 停止控制器上的 recorder（如果存在）
     */
    public static void stopRecording() {
        try {
            AvatarPlayController controller = AvatarUtils.getControllerSafely();
            if (controller != null && controller.getRecorder() != null) {
                controller.getRecorder().stopRecord();
            }
        } catch (Throwable t) {
            Log.e(TAG, "stopRecording failed", t);
        }
    }
}


