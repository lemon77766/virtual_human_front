# 科大讯飞虚拟人SDK包名信息

## 已确认的SDK包名

通过分析 `avatar-core-v3.1.5.aar` 文件，已确认以下类的实际包名：

### 核心类
- **AvatarPlatform**: `com.iflytek.avalibrary.AvatarPlatform`
- **AvatarPlatformConfig**: `com.iflytek.avalibrary.AvatarPlatformConfig`
- **AvatarPlayController**: `com.iflytek.avalibrary.AvatarPlayController`
- **IAvatarListener**: `com.iflytek.avalibrary.IAvatarListener`

### 参数类
- **AvatarParams**: `com.iflytek.avalibrary.params.AvatarParams`
- **TextParams**: `com.iflytek.avalibrary.params.TextParams`

### 播放器相关
- **StreamPlayerFactory**: `com.iflytek.avalibrary.videoplayer.rt.StreamPlayerFactory`
- **IStreamPlayer**: `com.iflytek.avalibrary.videoplayer.rt.interfaces.IStreamPlayer`

### 录音器相关
- **AudioRecorder**: `com.iflytek.ys.core.recorder.AudioRecorder`

## 常用Import语句

```java
// 核心平台类
import com.iflytek.avalibrary.AvatarPlatform;
import com.iflytek.avalibrary.AvatarPlatformConfig;
import com.iflytek.avalibrary.AvatarPlayController;
import com.iflytek.avalibrary.IAvatarListener;
import com.iflytek.avalibrary.LogLevel;
import com.iflytek.avalibrary.AvatarError;

// 参数类
import com.iflytek.avalibrary.params.AvatarParams;
import com.iflytek.avalibrary.params.TextParams;

// 播放器
import com.iflytek.avalibrary.videoplayer.rt.StreamPlayerFactory;
import com.iflytek.avalibrary.videoplayer.rt.interfaces.IStreamPlayer;

// 录音器
import com.iflytek.ys.core.recorder.AudioRecorder;
```

## 注意事项

1. SDK包名与文档中提到的可能略有不同，实际包名以aar文件为准
2. ProGuard规则中已配置 `-keep class com.iflytek.** { *; }` 以保护所有SDK类
3. 代码中的import语句已更新为正确的包名































