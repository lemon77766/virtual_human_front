# 虚拟人SDK配置示例

## 配置信息获取

请从科大讯飞开放平台获取以下配置信息：
- [科大讯飞开放平台](https://www.xfyun.cn/)

## 基本配置

```java
// 必填配置
String APP_ID = "your_app_id_here";
String API_KEY = "your_api_key_here"; 
String API_SECRET = "your_api_secret_here";

// 可选配置
String SERVER_URL = "wss://avatar.cn-huadong-1.xf-yun.com/v1/interact"; // 默认服务地址
String UID = "12345678"; // 用户ID，自定义
```

## 虚拟人配置

```java
// 虚拟人形象ID
String ANCHOR_ID = "default"; // 可选：xiaofeng, xiaoxue, aijia, ayong

// 发音人配置
String VCN = "xiaofeng"; // 发音人
int SPEED = 50; // 语速 0-100
int PITCH = 50; // 音调 0-100  
int VOLUME = 50; // 音量 0-100
```

## 高级配置

```java
// 视频配置
int VIDEO_WIDTH = 1280;
int VIDEO_HEIGHT = 720;
double SCALE = 1.0; // 缩放比例
int MOVE_H = 0; // 水平位移
int MOVE_V = 0; // 垂直位移

// 流媒体配置
String PROTOCOL = "xrtc"; // 协议类型
int FPS = 25; // 帧率：15/20/25
int BITRATE = 2000; // 码率kbps
int ALPHA = 0; // 透明度 0-255
```

## 使用步骤

1. 在MainActivity中替换配置信息
2. 确保设备网络连接正常
3. 检查应用权限是否已授予
4. 按照初始化→创建播放器→启动虚拟人→发送文本的顺序操作

## 常见问题

- **配置错误**：请检查APP_ID、API_KEY、API_SECRET是否正确
- **网络问题**：确认设备可以访问互联网
- **权限问题**：确保已授予录音和网络权限
- **版本兼容**：确认SDK版本与API文档一致