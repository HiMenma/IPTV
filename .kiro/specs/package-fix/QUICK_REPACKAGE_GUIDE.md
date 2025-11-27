# 快速重新打包指南

## 修复内容总结

✅ **已修复 java.sql.DriverManager 错误** - 添加了 java.sql 模块支持  
✅ **已修复图标显示问题** - 更正了图标文件路径配置  
✅ **已启用完整运行时** - 确保所有必要的依赖都被包含  

## 立即重新打包

### macOS (DMG)

```bash
./gradlew clean
./gradlew packageDmg
```

打包文件位置：`composeApp/build/compose/binaries/main/dmg/`

### Windows (MSI)

```bash
./gradlew clean
./gradlew packageMsi
```

打包文件位置：`composeApp/build/compose/binaries/main/msi/`

### Windows (EXE)

```bash
./gradlew clean
./gradlew packageExe
```

打包文件位置：`composeApp/build/compose/binaries/main/exe/`

### Linux (DEB)

```bash
./gradlew clean
./gradlew packageDeb
```

打包文件位置：`composeApp/build/compose/binaries/main/deb/`

## 验证步骤

### 1. 安装并启动应用

安装打包好的应用，然后启动。

### 2. 检查图标

- 应用图标应该正确显示
- 任务栏/Dock 中的图标应该正确显示

### 3. 测试数据库功能

1. 点击"添加播放列表"
2. 输入任意 M3U URL 或 Xtream 信息
3. 确认没有出现 DriverManager 错误
4. 检查数据是否正常保存

### 4. 验证数据库文件

```bash
# 检查数据库文件是否创建
ls -la ~/.iptv/iptv.db
```

## 预期结果

✅ 应用正常启动，无错误提示  
✅ 图标正确显示  
✅ 可以添加和管理播放列表  
✅ 数据库文件正常创建在 `~/.iptv/iptv.db`  
✅ 所有功能正常工作  

## 如果还有问题

查看详细的故障排除指南：`DESKTOP_PACKAGING_FIX.md`
