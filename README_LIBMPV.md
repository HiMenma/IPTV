# libmpv 集成 - 快速开始

## 🎯 目标

将 libmpv 库集成到项目中，使应用可以独立运行，无需用户单独安装 libmpv。

## ✅ 已完成

1. **创建资源目录结构** - 用于存放不同平台的库文件
2. **修改 LibmpvLoader** - 支持从项目资源自动加载库
3. **创建自动化脚本** - 一键复制库文件到项目
4. **复制 macOS 库文件** - 当前系统的库已集成

## 🚀 快速使用

### 1. 复制库文件（首次或更新时）

```bash
# macOS / Linux
./scripts/copy-libmpv.sh

# Windows
.\scripts\copy-libmpv.ps1
```

### 2. 编译和运行

```bash
# 编译
./gradlew :composeApp:desktopJar

# 运行
./gradlew :composeApp:run

# 打包
./gradlew :composeApp:packageDistributionForCurrentOS
```

## 📁 目录结构

```
composeApp/src/desktopMain/resources/native/
├── macos-aarch64/     ✅ 已复制 (3.6 MB)
│   └── libmpv.2.dylib
├── macos-x86_64/      ⏳ 待复制
│   └── libmpv.2.dylib
├── linux-x86_64/      ⏳ 待复制
│   └── libmpv.so.2
└── windows-x86_64/    ⏳ 待复制
    └── libmpv-2.dll
```

## 🔄 加载策略

LibmpvLoader 使用三层加载策略：

1. **优先**: 从项目资源加载（打包的库）
2. **备用**: 从系统路径加载（用户安装的库）
3. **最后**: 使用系统库搜索

## 📊 当前状态

### ✅ 已修复的问题
1. API 符号解析错误
2. 音频输出配置
3. 视频质量选择

### ⚠️ 待解决的问题
1. 嵌入式播放（当前使用独立窗口）

### 📦 集成状态
- ✅ macOS Apple Silicon - 已集成
- ⏳ macOS Intel - 待复制库文件
- ⏳ Linux - 待复制库文件
- ⏳ Windows - 待复制库文件

## 📚 详细文档

- **[LIBMPV_INTEGRATION.md](LIBMPV_INTEGRATION.md)** - 完整集成指南
- **[LIBMPV_BUNDLE_GUIDE.md](LIBMPV_BUNDLE_GUIDE.md)** - 库文件打包指南
- **[LIBMPV_SETUP_GUIDE.md](LIBMPV_SETUP_GUIDE.md)** - 系统安装指南
- **[LIBMPV_API_FIX.md](LIBMPV_API_FIX.md)** - API 修复说明
- **[LIBMPV_CURRENT_STATUS.md](LIBMPV_CURRENT_STATUS.md)** - 当前状态

## 🎬 测试

运行应用并查看日志：

```bash
./gradlew :composeApp:run
```

成功的日志：
```
Attempting to load bundled libmpv from resources...
Looking for bundled library at: /native/macos-aarch64/libmpv.2.dylib
Extracting bundled library to: /tmp/libmpv-native.../libmpv.2.dylib
✓ Library extracted successfully
✓ Successfully loaded bundled libmpv from: /tmp/libmpv-native.../libmpv.2.dylib
```

## 📝 许可证

libmpv 使用 **LGPL 2.1+** 许可证。

需要在应用中声明：
- 使用了 libmpv
- 提供源代码链接
- 说明是动态链接

详见 [LIBMPV_INTEGRATION.md](LIBMPV_INTEGRATION.md#许可证合规)

## 🔧 故障排除

### 库文件未找到
```bash
# 重新运行复制脚本
./scripts/copy-libmpv.sh

# 检查文件是否存在
ls -lh composeApp/src/desktopMain/resources/native/macos-aarch64/
```

### 加载失败
```bash
# 查看详细日志
./gradlew :composeApp:run --info
```

## 🎯 下一步

### 短期
- [x] 集成 macOS Apple Silicon 库
- [ ] 集成其他平台库文件
- [ ] 测试所有平台

### 中期
- [ ] 实现嵌入式播放
- [ ] 优化库文件大小
- [ ] 添加自动更新机制

### 长期
- [ ] 支持更多平台
- [ ] 优化加载性能
- [ ] 添加库版本管理

---

**快速链接**:
- 🐛 [报告问题](https://github.com/YOUR_USERNAME/IPTV/issues)
- 📖 [完整文档](LIBMPV_INTEGRATION.md)
- 🔗 [libmpv 官网](https://mpv.io/)
