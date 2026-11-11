# 简化版虚拟人使用说明

## 功能特点

1. **一键启动虚拟人**：点击"启动虚拟人"按钮，自动完成所有初始化步骤
2. **文字交互**：在输入框中输入文字，点击"发送"按钮与虚拟人对话
3. **语音交互**：按住语音按钮说话，松开后停止录音

## 配置步骤

### 1. 配置API密钥

打开 `src/main/java/com/virhuman/testsdk/config/AvatarConfig.java` 文件，修改以下配置：

```java
// 必填配置（从科大讯飞开放平台获取）
public static final String DEFAULT_APP_ID = "your_app_id_here";
public static final String DEFAULT_API_KEY = "your_api_key_here";
public static final String DEFAULT_API_SECRET = "your_api_secret_here";
```

### 2. 可选配置

如果需要自定义虚拟人形象和发音人，可以修改：

```java
// 虚拟人形象ID（可选：xiaofeng, xiaoxue, aijia, ayong）
public static final String DEFAULT_ANCHOR_ID = "xiaofeng";

// 发音人（可选：xiaofeng, xiaoxue, aijia, ayong）
public static final String DEFAULT_VCN = "xiaofeng";
```

## 使用流程

1. **启动应用**：打开应用后，会自动进入简化版虚拟人界面

2. **授予权限**：首次使用时，需要授予以下权限：
   - 网络访问权限
   - 录音权限
   - 音频设置权限

3. **启动虚拟人**：
   - 点击"启动虚拟人"按钮
   - 系统会自动完成：初始化SDK → 创建播放器 → 设置参数 → 启动虚拟人 → 创建录音器
   - 等待状态栏显示"虚拟人已就绪"

4. **文字交互**：
   - 在输入框中输入要发送的文字
   - 点击"发送"按钮
   - 虚拟人会自动播报并做出相应动作

5. **语音交互**：
   - 按住语音按钮（绿色圆形按钮）
   - 开始说话
   - 松开按钮停止录音
   - 虚拟人会识别语音并做出回应

6. **停止虚拟人**：
   - 点击"停止虚拟人"按钮
   - 所有资源会自动释放

## 界面说明

- **顶部状态栏**：显示当前操作状态
- **视频显示区域**：显示虚拟人画面
- **启动/停止按钮**：控制虚拟人的启动和停止
- **文字输入框**：输入要发送的文字
- **发送按钮**：发送文字给虚拟人
- **语音按钮**：按住进行语音交互

## 注意事项

1. **网络连接**：确保设备已连接到互联网
2. **权限授予**：必须授予所有必要权限，否则功能无法正常使用
3. **配置信息**：使用前必须配置正确的APP_ID、API_KEY和API_SECRET
4. **首次启动**：首次启动可能需要几秒钟时间，请耐心等待

## 获取API密钥

1. 访问 [科大讯飞开放平台](https://www.xfyun.cn/)
2. 注册/登录账号
3. 创建应用
4. 获取APP_ID、API_KEY和API_SECRET
5. 将信息填入 `AvatarConfig.java` 文件中

## 常见问题

**Q: 启动失败怎么办？**
A: 检查以下几点：
- 网络连接是否正常
- APP_ID、API_KEY、API_SECRET是否正确配置
- 权限是否已全部授予

**Q: 语音交互没有反应？**
A: 检查以下几点：
- 是否已启动虚拟人
- 录音权限是否已授予
- 麦克风是否正常工作

**Q: 虚拟人画面不显示？**
A: 检查以下几点：
- 网络连接是否正常
- 是否已成功启动虚拟人
- 查看状态栏的错误信息

