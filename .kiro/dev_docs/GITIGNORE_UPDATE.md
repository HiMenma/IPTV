# .gitignore 更新说明

## 更新内容

### ✅ 添加了完整的Flutter/Dart忽略规则

#### Flutter相关
- `.dart_tool/` - Dart工具缓存目录
- `.flutter-plugins` - Flutter插件列表
- `.flutter-plugins-dependencies` - Flutter插件依赖
- `.packages` - Dart包配置（已废弃）
- `.pub-cache/` - Pub缓存
- `.pub/` - Pub目录
- `build/` - 构建输出目录

#### 平台特定
- **Android**: 
  - `android/.gradle/`
  - `android/local.properties`
  - `android/app/debug/`
  - `android/app/profile/`
  - `android/app/release/`
  
- **iOS**:
  - `**/ios/**/Pods/`
  - `**/ios/**/DerivedData/`
  - `**/ios/**/xcuserdata`
  - `**/ios/Flutter/ephemeral/`

- **Web**:
  - `lib/generated_plugin_registrant.dart`

- **Windows**:
  - `**/windows/flutter/ephemeral/`

- **Linux**:
  - `**/linux/flutter/ephemeral/`

#### IDE相关
- `.idea/` - IntelliJ IDEA
- `.vscode/` - Visual Studio Code
- `*.iml` - IntelliJ模块文件

#### 其他
- `.DS_Store` - macOS系统文件
- `coverage/` - 测试覆盖率报告
- `*.log` - 日志文件
- `.env*` - 环境变量文件

### ✅ 从Git中移除已跟踪的文件

执行的操作：
```bash
# 移除.dart_tool目录
git rm -r --cached .dart_tool

# 移除.flutter-plugins-dependencies
git rm --cached .flutter-plugins-dependencies
```

### ✅ 保留的文件

以下文件被保留用于CI/CD：
- `release.keystore.base64` - Base64编码的密钥库（用于CI/CD）
- `gradle/wrapper/gradle-wrapper.jar` - Gradle包装器

## 文件结构

### 应该被忽略的
```
.dart_tool/              ✅ 已忽略
.flutter-plugins         ✅ 已忽略
.flutter-plugins-dependencies  ✅ 已忽略
build/                   ✅ 已忽略
.gradle/                 ✅ 已忽略
.kotlin/                 ✅ 已忽略
.idea/                   ✅ 已忽略
.vscode/                 ✅ 已忽略
.DS_Store                ✅ 已忽略
*.log                    ✅ 已忽略
local.properties         ✅ 已忽略
*.keystore               ✅ 已忽略（除了例外）
*.apk                    ✅ 已忽略
```

### 应该被跟踪的
```
lib/                     ✅ 源代码
test/                    ✅ 测试代码
android/                 ✅ Android配置（部分）
ios/                     ✅ iOS配置（部分）
pubspec.yaml             ✅ 依赖配置
pubspec.lock             ✅ 依赖锁定
.gitignore               ✅ Git忽略规则
README.md                ✅ 项目说明
release.keystore.base64  ✅ CI/CD密钥
```

## 验证

### 检查忽略规则
```bash
# 查看哪些文件会被忽略
git status --ignored

# 检查特定文件是否被忽略
git check-ignore -v .dart_tool/
```

### 清理未跟踪的文件
```bash
# 查看会被删除的文件（不实际删除）
git clean -ndX

# 实际删除被忽略的文件
git clean -fdX
```

## 提交更改

```bash
# 查看状态
git status

# 添加.gitignore更改
git add .gitignore

# 提交
git commit -m "chore: update .gitignore for Flutter project

- Add comprehensive Flutter/Dart ignore rules
- Remove .dart_tool from git tracking
- Remove .flutter-plugins-dependencies from tracking
- Add platform-specific ignore rules (iOS, Android, Web, etc.)
- Add IDE-specific ignore rules (IntelliJ, VSCode)
- Keep release.keystore.base64 for CI/CD"
```

## 最佳实践

### 1. 定期清理
```bash
# 清理构建缓存
flutter clean

# 清理git忽略的文件
git clean -fdX
```

### 2. 检查大文件
```bash
# 查找大于1MB的文件
find . -type f -size +1M -not -path "./.git/*"
```

### 3. 验证忽略规则
```bash
# 测试文件是否会被忽略
git check-ignore -v <file_path>
```

## 常见问题

### Q: 为什么.dart_tool还在我的工作目录中？
A: `.gitignore`只是告诉git不要跟踪这些文件，但不会删除它们。这些文件是Flutter工作所需的。

### Q: 如何完全删除被忽略的文件？
A: 使用`git clean -fdX`，但要小心，这会删除所有被忽略的文件。

### Q: 为什么保留release.keystore.base64？
A: 这是Base64编码的密钥库文件，用于CI/CD自动构建。实际的`.keystore`文件不应该提交。

### Q: pubspec.lock应该被提交吗？
A: 是的，对于应用项目应该提交`pubspec.lock`以确保依赖版本一致。

## 相关文件

- `.gitignore` - Git忽略规则
- `.gitattributes` - Git属性配置（如果需要）

## 参考

- [Flutter官方.gitignore模板](https://github.com/flutter/flutter/blob/master/.gitignore)
- [GitHub Flutter .gitignore](https://github.com/github/gitignore/blob/main/Flutter.gitignore)
- [Git文档 - gitignore](https://git-scm.com/docs/gitignore)

---

**`.gitignore`已更新并优化！** ✅
