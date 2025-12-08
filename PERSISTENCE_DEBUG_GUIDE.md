# 收藏和历史记录调试指南

## 问题
应用完全退出后重新打开，收藏和历史记录列表清空。

## 已实施的改进

### 1. 添加调试日志
- ✅ 在`ChannelViewModel.loadFavorites()`中添加日志
- ✅ 在`FavoritesScreen`和`HistoryScreen`中添加日志
- ✅ 记录收藏数量、配置数量等关键信息

### 2. 改进UI
- ✅ 在空状态页面添加刷新按钮
- ✅ 改进错误处理和显示

### 3. 添加调试工具
- ✅ 创建`StorageInspector`类
- ✅ 创建持久化测试

## 如何调试

### 方法1: 查看应用日志

```bash
# 运行应用并查看日志
flutter run --release

# 或使用adb
adb logcat | grep -E "flutter|Favorites|History|Loading"
```

**查找关键日志**：
```
Loading favorites: X favorite records found
Favorite IDs: {id1, id2, ...}
Found X configurations
Favorites loaded successfully
```

### 方法2: 使用StorageInspector

在应用中临时添加调试代码：

```dart
import 'package:iptv_player/debug/storage_inspector.dart';

// 在某个按钮或initState中
void _debugStorage() async {
  await StorageInspector.printAllData();
  
  final favData = await StorageInspector.getFavoritesData();
  print('Favorites data: $favData');
  
  final histData = await StorageInspector.getHistoryData();
  print('History data: $histData');
}
```

### 方法3: 直接检查SharedPreferences

```bash
# Android设备
adb shell
run-as com.example.iptv_player
cd shared_prefs
cat com.example.iptv_player_preferences.xml

# 查找favorites和history键
grep -E "favorites|history" com.example.iptv_player_preferences.xml
```

## 测试步骤

### 完整测试流程

1. **添加收藏**
   ```
   - 打开应用
   - 进入某个配置的频道列表
   - 点击3个频道的收藏按钮
   - 确认看到"Added to favorites"提示
   ```

2. **播放频道（记录历史）**
   ```
   - 点击3个不同的频道播放
   - 每个频道至少播放5秒
   - 确认视频开始播放
   ```

3. **检查数据是否保存**
   ```
   - 切换到Favorites标签
   - 应该看到3个收藏的频道
   - 切换到History标签
   - 应该看到3个播放过的频道
   ```

4. **完全关闭应用**
   ```
   - 按Home键
   - 打开最近任务
   - 向上滑动关闭应用
   - 等待5秒
   ```

5. **重新打开应用**
   ```
   - 从桌面图标打开应用
   - 等待应用完全加载
   - 切换到Favorites标签
   - 检查是否显示收藏的频道
   - 切换到History标签
   - 检查是否显示历史记录
   ```

6. **查看日志**
   ```bash
   adb logcat | grep -E "Loading favorites|Favorites loaded|favorite records"
   ```

## 可能的问题和解决方案

### 问题1: 数据确实保存了，但加载失败

**症状**：
- SharedPreferences中有数据
- 但Favorites/History页面为空
- 日志显示"0 favorite records found"或加载错误

**原因**：
- 配置加载失败
- 网络问题导致频道加载失败
- 服务器不可达

**解决方案**：
1. 检查网络连接
2. 点击刷新按钮重试
3. 检查配置是否有效

### 问题2: 数据根本没有保存

**症状**：
- SharedPreferences中没有favorites/history键
- 日志显示保存失败

**原因**：
- 存储权限问题
- 存储空间不足
- SharedPreferences初始化失败

**解决方案**：
1. 检查应用权限
2. 清理设备存储空间
3. 重新安装应用

### 问题3: 数据损坏

**症状**：
- SharedPreferences中有数据但格式错误
- 日志显示"Favorites data corrupted"

**原因**：
- JSON格式错误
- 数据写入中断

**解决方案**：
1. 应用会自动尝试从备份恢复
2. 如果失败，清除数据重新开始
3. 使用`StorageInspector.clearAll()`清除所有数据

## 调试命令速查

```bash
# 查看应用日志
adb logcat | grep -i flutter

# 查看收藏相关日志
adb logcat | grep -i favorite

# 查看历史相关日志
adb logcat | grep -i history

# 查看SharedPreferences
adb shell run-as com.example.iptv_player cat shared_prefs/com.example.iptv_player_preferences.xml

# 清除应用数据
adb shell pm clear com.example.iptv_player

# 重新安装应用
adb install -r build/app/outputs/flutter-apk/app-release.apk
```

## 预期日志输出

### 正常情况
```
I/flutter: Loading favorites: 3 favorite records found
I/flutter: Favorite IDs: {config1:channel1, config1:channel2, config1:channel3}
I/flutter: Found 1 configurations
I/flutter: Favorites loaded successfully
```

### 异常情况
```
I/flutter: Loading favorites: 3 favorite records found
I/flutter: Favorite IDs: {config1:channel1, config1:channel2, config1:channel3}
I/flutter: Found 0 configurations
E/flutter: Error loading favorites: No configurations found
```

## 临时解决方案

如果问题持续存在：

1. **使用刷新按钮**
   - 在Favorites页面点击刷新按钮
   - 在History页面点击刷新按钮

2. **检查配置**
   - 确保至少有一个有效的配置
   - 确保配置的服务器可访问

3. **重新添加数据**
   - 如果数据真的丢失了，重新添加收藏
   - 重新播放频道记录历史

## 报告问题

如果问题仍然存在，请提供：

1. **日志输出**
   ```bash
   adb logcat > logcat.txt
   # 重现问题
   # Ctrl+C停止
   ```

2. **SharedPreferences内容**
   ```bash
   adb shell run-as com.example.iptv_player cat shared_prefs/com.example.iptv_player_preferences.xml > prefs.xml
   ```

3. **重现步骤**
   - 详细的操作步骤
   - 预期结果 vs 实际结果
   - 设备型号和Android版本

---

**新版本APK已构建，包含调试日志和改进的错误处理** ✅

**APK路径**: `build/app/outputs/flutter-apk/app-release.apk`
**大小**: 53.1MB
