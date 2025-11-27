# 桌面平台打包问题修复

## 问题描述

1. **图标未正确显示**
2. **启动时报错：Error: java/sql/DriverManager**

## 问题原因

### 1. 图标问题
- Windows 平台配置了不存在的 `.ico` 文件
- 图标文件路径配置不正确

### 2. DriverManager 错误
- 打包后的应用缺少 `java.sql` 模块
- SQLite JDBC 驱动需要 Java SQL 模块才能正常工作
- Compose Desktop 默认不包含所有 JDK 模块

## 解决方案

### 修复内容

在 `composeApp/build.gradle.kts` 中进行了以下修改：

1. **添加 java.sql 模块支持**
   ```kotlin
   macOS {
       modules("java.sql")
   }
   windows {
       modules("java.sql")
   }
   linux {
       modules("java.sql")
   }
   ```

2. **启用所有运行时模块**
   ```kotlin
   includeAllModules = true
   ```

3. **修复图标配置**
   - macOS: 使用 `app_icon.icns`
   - Windows: 使用 `app_icon_windows.png`
   - Linux: 使用 `app_icon.png`

4. **禁用 ProGuard**
   ```kotlin
   buildTypes.release.proguard {
       isEnabled.set(false)
   }
   ```

## 重新打包

### 清理并重新构建

```bash
# 清理之前的构建
./gradlew clean

# 为 macOS 打包
./gradlew packageDmg

# 为 Windows 打包
./gradlew packageMsi
# 或
./gradlew packageExe

# 为 Linux 打包
./gradlew packageDeb
```

### 验证修复

1. **图标验证**
   - macOS: 检查 DMG 和安装后的应用图标
   - Windows: 检查 MSI/EXE 安装程序和应用图标
   - Linux: 检查 DEB 包和应用图标

2. **数据库验证**
   - 启动应用，不应再出现 DriverManager 错误
   - 添加播放列表，验证数据库功能正常
   - 检查 `~/.iptv/iptv.db` 文件是否正常创建

## 技术说明

### java.sql 模块的必要性

SQLite JDBC 驱动 (`JdbcSqliteDriver`) 依赖于 Java 标准库中的 `java.sql` 包：
- `java.sql.DriverManager`
- `java.sql.Connection`
- `java.sql.Statement`
- `java.sql.ResultSet`

在 Java 9+ 的模块化系统中，这些类属于 `java.sql` 模块。Compose Desktop 打包时需要显式声明包含此模块。

### 图标格式要求

- **macOS**: `.icns` 格式（推荐包含多种尺寸：16x16 到 1024x1024）
- **Windows**: `.ico` 或 `.png` 格式（推荐 256x256 或更大）
- **Linux**: `.png` 格式（推荐 512x512 或更大）

## 可选优化

### 创建 Windows ICO 文件

如果需要更好的 Windows 图标支持，可以将 PNG 转换为 ICO：

```bash
# 使用 ImageMagick
convert app_icon_windows.png -define icon:auto-resize=256,128,64,48,32,16 app_icon.ico

# 或使用在线工具
# https://convertio.co/png-ico/
```

然后更新 `build.gradle.kts`：
```kotlin
windows {
    iconFile.set(project.file("src/desktopMain/resources/app_icon.ico"))
}
```

## 故障排除

### 如果仍然出现 DriverManager 错误

1. 检查打包日志中是否包含 java.sql 模块
2. 尝试添加更多 SQL 相关模块：
   ```kotlin
   modules("java.sql", "java.sql.rowset")
   ```

3. 验证 SQLite JDBC 驱动版本兼容性

### 如果图标仍未显示

1. 确认图标文件存在且路径正确
2. 检查图标文件格式和尺寸
3. 清理构建缓存后重新打包：
   ```bash
   ./gradlew clean
   rm -rf build/
   rm -rf composeApp/build/
   ```

## 相关文件

- `composeApp/build.gradle.kts` - 构建配置
- `composeApp/src/desktopMain/resources/` - 图标资源目录
- `composeApp/src/desktopMain/kotlin/com/menmapro/iptv/data/database/` - 数据库驱动实现
