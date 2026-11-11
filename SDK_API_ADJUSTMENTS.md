# SDK API 方法名调整说明

## 已修复的导入问题

1. **LogLevel** 
   - 错误导入：`com.iflytek.avalibrary.LogLevel`
   - 正确导入：`com.iflytek.ys.core.log.LogLevel`

2. **AvatarError**
   - 错误导入：`com.iflytek.avalibrary.AvatarError`
   - 正确导入：`com.iflytek.avalibrary.constant.AvatarError`

## 需要根据实际SDK文档调整的方法名

以下方法名可能需要根据实际的SDK API文档进行调整：

### 1. Dispatch.setInteractiveMod / setInteractiveMode
**位置：** `MainActivity.java` 第673行，`MainLocalActivity.java` 第574行

**当前使用：** `dispatch.setInteractiveMode()`

**可能的变体：**
- `setInteractiveMod()` (原始方法名)
- `setInteractiveMode()` (当前使用)
- `setInteractive()`

**说明：** 用于设置交互模式（追加/打断），参数：1-打断, 0-追加

### 2. AudioRecorder.addPcmListener ✅ 已修复
**位置：** `MainActivity.java` 第738行，`MainLocalActivity.java` 第624行

**正确方法名：** `audioRecorder.addPcmListener()`

**说明：** 用于设置PCM数据监听器

### 3. AudioRecorder.startRecoder ✅ 已修复
**位置：** `MainActivity.java` 第771行，`MainLocalActivity.java` 第653行

**正确方法名：** `recorder.startRecoder()`

**说明：** 用于启动录音（注意：方法名是 `startRecoder` 而不是 `startRecorder`）

## 如何调整

如果编译时出现方法不存在的错误，请：

1. 查看SDK文档，确认正确的方法名
2. 在代码中搜索并替换相应的方法调用
3. 确保方法签名（参数类型和数量）匹配

## 检查方法是否存在的方法

可以使用以下PowerShell命令检查AAR文件中的类和方法：

```powershell
$aarPath = "libs\avatar-core-v3.1.5.aar"
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [System.IO.Compression.ZipFile]::OpenRead($aarPath)
$classesJar = $zip.Entries | Where-Object { $_.Name -eq "classes.jar" } | Select-Object -First 1
$tempPath = "$env:TEMP\classes_temp.jar"
[System.IO.Compression.ZipFileExtensions]::ExtractToFile($classesJar, $tempPath, $true)
$jarZip = [System.IO.Compression.ZipFile]::OpenRead($tempPath)
# 查找特定类的方法
$jarZip.Entries | Where-Object { $_.FullName -like "*ClassName*" }
$jarZip.Dispose()
Remove-Item $tempPath -ErrorAction SilentlyContinue
$zip.Dispose()
```

