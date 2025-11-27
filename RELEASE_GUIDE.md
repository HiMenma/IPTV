# 快速发布指南

## 🚀 一键发布新版本

### 步骤 1：更新版本号

编辑 `composeApp/build.gradle.kts`：

```kotlin
// 桌面应用版本
compose.desktop {
    application {
        nativeDistributions {
            packageVersion = "1.0.0"  // 👈 修改这里
        }
    }
}

// Android 应用版本
android {
    defaultConfig {
        versionCode = 1      // 👈 每次发布递增
        versionName = "1.0"  // 👈 修改这里
    }
}
```

### 步骤 2：提交并推送标签

```bash
# 提交版本更改
git add .
git commit -m "chore: release v1.0.0"

# 创建标签
git tag v1.0.0

# 推送到 GitHub
git push origin main
git push origin v1.0.0
```

### 步骤 3：等待构建完成

1. 打开 GitHub 仓库的 **Actions** 页面
2. 查看 "构建发布包" 工作流的运行状态
3. 等待所有平台构建完成（约 15-20 分钟）

### 步骤 4：检查发布

1. 进入 **Releases** 页面
2. 找到新创建的 Release
3. 确认所有安装包都已上传：
   - ✅ Windows (MSI)
   - ✅ macOS (DMG)
   - ✅ Linux (DEB)
   - ✅ Android (APK)

## 🧪 测试构建（不发布）

如果只想测试构建，不创建正式发布：

1. 打开 GitHub 仓库的 **Actions** 页面
2. 选择 "构建发布包" 工作流
3. 点击 **Run workflow**
4. 输入版本号
5. 点击 **Run workflow** 确认

构建产物会在 Actions 页面的 **Artifacts** 部分提供下载。

## 📋 版本号规范

使用语义化版本号：`主版本.次版本.修订号`

- **主版本**：不兼容的 API 修改
- **次版本**：向下兼容的功能性新增
- **修订号**：向下兼容的问题修正

示例：
- `v1.0.0` - 首次正式发布
- `v1.1.0` - 添加新功能
- `v1.1.1` - 修复 bug

## ⚠️ 注意事项

1. **标签必须以 `v` 开头**，例如 `v1.0.0`
2. **版本号要保持一致**：标签、packageVersion、versionName
3. **Android versionCode 必须递增**，不能重复
4. **推送标签前确保代码已测试**，标签推送后会自动发布

## 🔄 回滚发布

如果需要删除错误的发布：

```bash
# 删除本地标签
git tag -d v1.0.0

# 删除远程标签
git push origin :refs/tags/v1.0.0

# 在 GitHub 上手动删除 Release
```

## 📞 需要帮助？

查看完整文档：[GITHUB_ACTIONS_GUIDE.md](GITHUB_ACTIONS_GUIDE.md)
