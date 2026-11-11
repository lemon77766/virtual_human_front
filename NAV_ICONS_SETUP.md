# 底部导航栏图标设置说明

## 📁 图片放置位置

请将您的8张图片放到以下文件夹：
```
src/main/res/drawable/
```

## 📝 图片命名规范

请按照以下命名规则重命名您的图片：

1. **首页图标**
   - 未选中状态：`ic_nav_home_normal.png` (或 `.jpg`)
   - 选中状态：`ic_nav_home_selected.png` (或 `.jpg`)

2. **功能图标**
   - 未选中状态：`ic_nav_features_normal.png` (或 `.jpg`)
   - 选中状态：`ic_nav_features_selected.png` (或 `.jpg`)

3. **消息图标**
   - 未选中状态：`ic_nav_messages_normal.png` (或 `.jpg`)
   - 选中状态：`ic_nav_messages_selected.png` (或 `.jpg`)

4. **我的图标**
   - 未选中状态：`ic_nav_profile_normal.png` (或 `.jpg`)
   - 选中状态：`ic_nav_profile_selected.png` (或 `.jpg`)

## ✅ 已完成的工作

1. ✅ 已创建4个selector文件（selector_nav_*.xml）
2. ✅ 已修改布局文件（activity_home.xml），将TextView改为ImageView
3. ✅ 已更新Java代码，添加选中状态处理
4. ✅ 已添加点击效果（使用系统默认的selectableItemBackground）

## 🎨 图标尺寸建议

- 推荐尺寸：24dp × 24dp 或 48px × 48px（@2x）
- 格式：PNG（支持透明背景）或 JPG
- 颜色：建议使用矢量图或高分辨率图片，确保在不同设备上清晰显示

## 📱 使用说明

1. 将8张图片按照命名规范放入 `src/main/res/drawable/` 文件夹
2. 重新编译项目
3. 运行应用，底部导航栏将显示图标
4. 点击导航项时会有选中效果和点击反馈

## 🔧 自定义图标大小

如果需要调整图标大小，可以修改 `activity_home.xml` 中的以下属性：
```xml
android:layout_width="24dp"  <!-- 修改这里的数值 -->
android:layout_height="24dp" <!-- 修改这里的数值 -->
```

## 📋 检查清单

- [ ] 8张图片已放入 `src/main/res/drawable/` 文件夹
- [ ] 图片命名符合规范
- [ ] 图片格式正确（PNG或JPG）
- [ ] 重新编译项目
- [ ] 测试点击效果和选中状态



























