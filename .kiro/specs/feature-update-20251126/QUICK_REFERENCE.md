# 快速参考卡片

## 🎯 4个新功能速览

| 功能 | 入口 | 平台 | 说明 |
|------|------|------|------|
| 全屏播放优化 | 播放界面 → 全屏按钮 | Android | 自动横屏 + 自动隐藏控制 |
| 刷新Xtream | 播放列表 → ⚙️ → 刷新 | 全平台 | 重新获取最新频道 |
| 导出M3U | 播放列表 → ⚙️ → 导出为M3U | 全平台 | 保存为标准M3U文件 |
| 导入本地M3U | + → 本地文件 → 选择文件 | 全平台 | 从设备导入M3U文件 |

## 📋 常用操作

### 添加播放列表
```
点击 + 按钮
├─ M3U URL: 输入URL地址
├─ 本地文件: 选择M3U文件 (新)
└─ Xtream: 输入服务器信息
```

### 管理播放列表
```
点击播放列表的 ⚙️ 按钮
├─ 刷新: 更新频道列表 (新)
├─ 导出为M3U: 保存为文件 (新)
└─ 重命名: 修改名称
```

### 全屏播放
```
播放频道
└─ 点击"全屏"按钮
   ├─ 自动横屏 (Android) (新)
   ├─ 3秒后自动隐藏控制 (新)
   └─ 点击屏幕显示控制 (新)
```

## 🔧 技术要点

### 数据库
- **版本**: v3
- **新增字段**: xtreamServerUrl, xtreamUsername, xtreamPassword
- **迁移**: 自动执行

### 文件格式
```m3u
#EXTM3U
#EXTINF:-1 tvg-id="id" tvg-logo="url" group-title="group",频道名
http://stream-url
```

### API
```kotlin
// 刷新播放列表
repository.refreshPlaylist(playlistId)

// 导出M3U
val content = repository.exportPlaylistToM3u(playlistId)

// 选择文件
val content = fileManager.pickM3uFile()

// 保存文件
fileManager.saveM3uFile(fileName, content)
```

## ⚡ 快捷键（未来）

| 操作 | 快捷键 | 说明 |
|------|--------|------|
| 全屏 | F | 切换全屏模式 |
| 退出全屏 | ESC | 退出全屏 |
| 刷新 | Ctrl+R | 刷新当前播放列表 |
| 导出 | Ctrl+E | 导出当前播放列表 |

## 🐛 故障排除

| 问题 | 解决方案 |
|------|----------|
| 无法刷新Xtream | 删除后重新添加播放列表 |
| 导入失败 | 检查M3U文件格式 |
| 全屏不横屏 | 检查系统旋转锁定 |
| 文件选择器不显示 | 检查存储权限 |

## 📱 平台差异

| 功能 | Android | Desktop |
|------|---------|---------|
| 自动横屏 | ✅ | ❌ |
| 自动隐藏控制 | ✅ | ✅ |
| 文件选择器 | 系统选择器 | Swing对话框 |
| 文件保存 | SAF | JFileChooser |

## 📊 性能指标

| 操作 | 预期时间 |
|------|----------|
| 导入100频道 | < 5秒 |
| 导出100频道 | < 3秒 |
| 刷新Xtream | < 30秒 |
| 全屏切换 | 即时 |

## 🔗 相关文档

- [NEW_FEATURES.md](NEW_FEATURES.md) - 详细功能说明
- [TESTING_GUIDE.md](TESTING_GUIDE.md) - 测试指南
- [使用说明.md](使用说明.md) - 用户使用说明
- [IMPLEMENTATION_SUMMARY.md](IMPLEMENTATION_SUMMARY.md) - 技术实现总结

---

**版本**: 1.0.0
**更新日期**: 2025-11-26
