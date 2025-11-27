# GitHub Actions 配置检查清单

在使用自动构建功能之前，请确保完成以下配置。

## ✅ 必需配置

### 1. 更新 README 徽章

编辑 `README.md`，将以下内容中的 `YOUR_USERNAME` 替换为你的 GitHub 用户名：

```markdown
[![构建状态](https://github.com/himenma/IPTV/actions/workflows/build-release.yml/badge.svg)](https://github.com/himenma/IPTV/actions/workflows/build-release.yml)
[![最新版本](https://img.shields.io/github/v/release/himenma/IPTV)](https://github.com/himenma/IPTV/releases/latest)
[![下载量](https://img.shields.io/github/downloads/himenma/IPTV/total)](https://github.com/himenma/IPTV/releases)
```

### 2. 启用 GitHub Actions

1. 进入仓库的 **Settings** 页面
2. 点击左侧的 **Actions** > **General**
3. 在 **Actions permissions** 部分，选择：
   - ✅ Allow all actions and reusable workflows
4. 在 **Workflow permissions** 部分，选择：
   - ✅ Read and write permissions
   - ✅ Allow GitHub Actions to create and approve pull requests
5. 点击 **Save** 保存设置

### 3. 验证工作流文件

确认 `.github/workflows/build-release.yml` 文件已正确创建并提交到仓库。

## 🔐 可选配置（Android 签名）

如果需要发布签名的 Android APK，需要配置以下 Secrets：

### 创建 Keystore

```bash
# 生成 keystore 文件
keytool -genkey -v -keystore release.keystore -alias iptv-player \
  -keyalg RSA -keysize 2048 -validity 10000

# 转换为 Base64
base64 release.keystore > release.keystore.base64
```

### 添加 GitHub Secrets

1. 进入仓库的 **Settings** > **Secrets and variables** > **Actions**
2. 点击 **New repository secret**
3. 添加以下 Secrets：

| Secret 名称 | 说明 | 示例 |
|------------|------|------|
| `KEYSTORE_FILE` | Base64 编码的 keystore 文件内容 | 从 `release.keystore.base64` 复制 |
| `KEYSTORE_PASSWORD` | Keystore 密码 | `your_keystore_password` |
| `KEY_ALIAS` | 密钥别名 | `iptv-player` |
| `KEY_PASSWORD` | 密钥密码 | `your_key_password` |

### 更新工作流文件

在 `.github/workflows/build-release.yml` 的 Android 构建部分添加签名步骤：

```yaml
- name: 解码 Keystore
  run: |
    echo "${{ secrets.KEYSTORE_FILE }}" | base64 -d > release.keystore

- name: 构建签名的 Release APK
  run: |
    ./gradlew assembleRelease \
      -Pandroid.injected.signing.store.file=release.keystore \
      -Pandroid.injected.signing.store.password=${{ secrets.KEYSTORE_PASSWORD }} \
      -Pandroid.injected.signing.key.alias=${{ secrets.KEY_ALIAS }} \
      -Pandroid.injected.signing.key.password=${{ secrets.KEY_PASSWORD }}
```

## 🍎 可选配置（macOS 签名和公证）

如果需要发布签名和公证的 macOS 应用：

### 前置要求

- Apple Developer 账号
- 开发者证书
- App-specific 密码

### 添加 GitHub Secrets

| Secret 名称 | 说明 |
|------------|------|
| `MACOS_CERTIFICATE` | Base64 编码的开发者证书 |
| `MACOS_CERTIFICATE_PWD` | 证书密码 |
| `MACOS_NOTARIZATION_APPLE_ID` | Apple ID |
| `MACOS_NOTARIZATION_TEAM_ID` | Team ID |
| `MACOS_NOTARIZATION_PWD` | App-specific 密码 |

### 更新 build.gradle.kts

```kotlin
compose.desktop {
    application {
        nativeDistributions {
            macOS {
                signing {
                    sign.set(true)
                    identity.set("Developer ID Application: Your Name (TEAM_ID)")
                }
                notarization {
                    appleID.set(System.getenv("MACOS_NOTARIZATION_APPLE_ID"))
                    password.set(System.getenv("MACOS_NOTARIZATION_PWD"))
                    teamID.set(System.getenv("MACOS_NOTARIZATION_TEAM_ID"))
                }
            }
        }
    }
}
```

## 🧪 测试配置

### 测试工作流

1. 手动触发一次构建：
   - 进入 **Actions** 页面
   - 选择 "构建发布包" 工作流
   - 点击 **Run workflow**
   - 输入测试版本号（如 `0.0.1`）
   - 点击 **Run workflow**

2. 等待构建完成（约 15-20 分钟）

3. 检查构建结果：
   - 所有平台都应该成功构建
   - 在 **Artifacts** 部分应该能看到所有安装包

### 测试发布流程

```bash
# 创建测试标签
git tag v0.0.1

# 推送标签
git push origin v0.0.1

# 检查 Actions 页面的构建状态
# 检查 Releases 页面是否自动创建了 Release
```

如果测试成功，删除测试标签和 Release：

```bash
# 删除本地标签
git tag -d v0.0.1

# 删除远程标签
git push origin :refs/tags/v0.0.1

# 在 GitHub 上手动删除 Release
```

## 📋 配置完成检查

- [ ] GitHub Actions 已启用
- [ ] 工作流权限已设置为读写
- [ ] README 徽章已更新
- [ ] 工作流文件已提交
- [ ] 手动触发测试成功
- [ ] 标签触发测试成功
- [ ] （可选）Android 签名已配置
- [ ] （可选）macOS 签名已配置

## 🎉 开始使用

配置完成后，参考以下文档开始使用：

- **[快速发布指南](../RELEASE_GUIDE.md)** - 发布新版本的步骤
- **[GitHub Actions 指南](../GITHUB_ACTIONS_GUIDE.md)** - 详细使用说明
- **[本地构建指南](../BUILD_PACKAGES.md)** - 本地构建方法

## ❓ 遇到问题？

查看 [GitHub Actions 指南的常见问题部分](../GITHUB_ACTIONS_GUIDE.md#常见问题)
