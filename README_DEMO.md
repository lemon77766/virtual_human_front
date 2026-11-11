# 虚拟人SDK Android Demo

这是基于科大讯飞虚拟人SDK的Android演示应用，包含了云模式和端模式的完整实现。

## 📱 功能特性

### 核心功能
- ✅ **云模式支持** - 通过云端服务器进行虚拟人渲染
- ✅ **端模式支持** - 本地进行虚拟人渲染
- ✅ **完整的SDK初始化流程**
- ✅ **文本驱动虚拟人播报**
- ✅ **实时语音交互**
- ✅ **动作指令控制**
- ✅ **播放器音量控制**
- ✅ **字幕显示支持**
- ✅ **变声功能**
- ✅ **场景配置**

### 技术特性
- 🎯 **模块化设计** - 配置、工具类、权限管理分离
- 🔧 **简化API** - 使用工具类简化SDK操作
- 🛡️ **完善的错误处理** - 友好的错误提示和异常管理
- 🔐 **权限管理** - 自动处理应用所需权限
- 📝 **详细日志** - 便于调试和问题定位

## 🏗️ 项目结构

```
src/main/java/com/virhuman/testsdk/
├── config/                     # 配置类
│   └── AvatarConfig.java      # SDK配置常量和默认值
├── utils/                      # 工具类
│   ├── AvatarUtils.java       # SDK操作工具类
│   └── PermissionUtils.java   # 权限管理工具类
├── ui/                         # UI界面
│   ├── EntryActivity.java     # 入口页面
│   ├── MainActivity.java      # 云模式主页面（完整版）
│   ├── MainActivitySimple.java # 云模式主页面（简化版）
│   └── MainLocalActivity.java # 端模式主页面
├── permission/                 # 权限相关
│   └── RequestPermissionActivity.java
└── DemoApplication.java       # 应用程序入口
```

## 🚀 快速开始

### 1. 配置SDK信息

在运行应用之前，需要配置科大讯飞虚拟人SDK的认证信息：

```java
// 在MainActivity中设置你的配置信息
etAppId.setText("your_app_id");
etApiKey.setText("your_api_key");
etApiSecret.setText("your_api_secret");
```

### 2. 权限配置

应用会自动请求以下权限：

**必需权限：**
- `INTERNET` - 网络访问
- `ACCESS_NETWORK_STATE` - 网络状态检查
- `ACCESS_WIFI_STATE` - WiFi状态检查
- `RECORD_AUDIO` - 录音
- `MODIFY_AUDIO_SETTINGS` - 音频设置修改

**可选权限：**
- `READ_EXTERNAL_STORAGE` / `WRITE_EXTERNAL_STORAGE` - 存储访问（Android 11以下）
- `BLUETOOTH` / `BLUETOOTH_ADMIN` - 蓝牙设备

### 3. 运行步骤

1. **初始化SDK**
   - 填写APPID、APIKEY、APISECRET
   - 点击"初始化SDK"

2. **创建播放器**
   - 点击"创建播放器"
   - 等待首帧渲染完成

3. **配置虚拟人**
   - 设置虚拟人ID（如：default）
   - 设置发音人（如：xiaofeng）
   - 可选择语义理解模式

4. **启动虚拟人**
   - 点击"启动虚拟人"
   - 等待服务连接成功

5. **文本交互**
   - 在文本框输入要播报的内容
   - 点击"发送文本"
   - 虚拟人将进行播报

## 📖 使用指南

### 云模式 vs 端模式

| 特性 | 云模式 | 端模式 |
|------|--------|--------|
| 渲染方式 | 云端渲染 | 本地渲染 |
| 资源占用 | 较低 | 较高 |
| 延迟 | 网络延迟 | 实时响应 |
| 配置复杂度 | 较简单 | 需要本地资源 |

### 基本操作流程

```java
// 1. 初始化SDK
AvatarUtils.initializeSDK(context, appId, apiKey, apiSecret, listener);

// 2. 创建播放器
IStreamPlayer player = AvatarUtils.createStreamPlayer(context, renderView, listener);

// 3. 设置参数
controller.setGlobalParams(AvatarUtils.buildDefaultParams(avatarId, vcn));

// 4. 启动虚拟人
controller.start();

// 5. 发送文本
controller.writeText(text, textParams);
```

### 高级功能配置

#### 1. 语义理解模式
```java
TextParams textParams = AvatarUtils.buildTextParams(text, true, vcn, true);
controller.writeText(text, textParams);
```

#### 2. 动作控制
```java
String actionText = AvatarUtils.buildActionText("A_Hello_l_F");
controller.writeText(actionText);
```

#### 3. 音量控制
```java
player.setVolume(0.8); // 0.0 - 1.0
```

## 🔧 配置说明

### 常用虚拟人ID
- `default` - 默认虚拟人
- `xiaofeng` - 小峰
- `xiaoxue` - 小雪
- `aijia` - 爱家
- `ayong` - 阿勇

### 常用发音人
- `xiaofeng` - 男声，成熟稳重
- `xiaoxue` - 女声，温柔甜美
- `aijia` - 女声，亲切自然
- `ayong` - 男声，阳光活力

### 视频参数配置
```java
// 支持的视频尺寸
1280x720 (推荐)
720x480
640x360

// 支持的帧率
15, 20, 25 (推荐)
```

## 🐛 常见问题

### 1. SDK初始化失败
- **检查网络连接**：确保设备可以访问互联网
- **验证认证信息**：确认APPID、APIKEY、APISECRET正确
- **检查权限**：确认已授予网络相关权限

### 2. 播放器创建失败
- **检查OpenGL ES支持**：确保设备支持OpenGL ES 2.0+
- **确认初始化**：确保SDK已成功初始化
- **查看日志**：通过Logcat查看详细错误信息

### 3. 虚拟人不显示
- **等待连接**：虚拟人连接需要几秒钟时间
- **检查播放器**：确认播放器已创建并设置渲染区域
- **验证参数**：确认虚拟人ID和其他参数正确

### 4. 语音播报无声音
- **检查音量**：确认系统音量和应用音量正常
- **验证音频权限**：确认已授予录音和音频修改权限
- **检查发音人**：确认发音人参数正确

## 📝 开发注意事项

### 1. 资源管理
- 在Activity销毁时及时释放播放器和录音器资源
- 避免重复初始化SDK
- 合理控制视频分辨率以平衡性能和效果

### 2. 线程安全
- SDK回调都在子线程中执行，UI更新需切换到主线程
- 使用runOnUiThread()进行UI操作

### 3. 错误处理
- 实现完整的错误监听和处理机制
- 为用户提供友好的错误提示
- 记录详细的日志以便调试

### 4. 性能优化
- 根据设备性能选择合适的视频参数
- 避免频繁创建和销毁播放器
- 合理使用缓存机制

## 📚 相关文档

- [科大讯飞虚拟人SDK官方文档](https://www.xfyun.cn/services/Avatar)
- [Android权限最佳实践](https://developer.android.com/guide/topics/permissions/overview)
- [Android多媒体开发指南](https://developer.android.com/guide/topics/media)

## 🤝 技术支持

如遇到问题，可以通过以下方式获取帮助：
1. 查看科大讯飞官方文档
2. 检查Logcat中的详细错误日志
3. 确认SDK版本和API兼容性
4. 联系科大讯飞技术支持

---

**注意：** 本Demo仅供学习和参考使用，实际项目中请根据具体需求进行优化和调整。