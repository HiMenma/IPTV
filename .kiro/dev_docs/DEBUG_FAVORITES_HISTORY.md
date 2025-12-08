# 收藏和历史记录调试指南

## 问题描述
- 收藏按钮显示了，但收藏页面没有显示内容
- 历史记录页面也没有显示内容

## 已修复
✅ 主界面添加了Favorites和History按钮（在AppBar右上角）

## 检查步骤

### 1. 检查主界面按钮
- [ ] 打开应用
- [ ] 查看AppBar右上角
- [ ] 应该看到两个图标：❤️（收藏）和🕐（历史）

### 2. 测试收藏功能

#### 添加收藏
1. 打开任意配置的频道列表
2. 找到任意频道
3. 点击频道右侧的❤️图标
4. 应该看到底部提示："Added [频道名] to favorites"
5. 图标应该变成实心❤️

#### 查看收藏
1. 返回主界面
2. 点击AppBar右上角的❤️图标
3. 应该看到刚才收藏的频道

#### 取消收藏
1. 在收藏页面或频道列表中
2. 点击实心❤️图标
3. 应该看到提示："Removed [频道名] from favorites"
4. 图标变回空心❤️

### 3. 测试历史记录功能

#### 记录历史
1. 打开任意配置的频道列表
2. 点击任意频道播放
3. 等待几秒（确保播放开始）
4. 返回主界面

#### 查看历史
1. 点击AppBar右上角的🕐图标
2. 应该看到刚才播放的频道
3. 最近播放的应该在最上面

#### 清除历史
1. 在历史页面
2. 点击右上角的垃圾桶图标
3. 确认清除
4. 历史应该被清空

## 可能的问题和解决方案

### 问题1：收藏后没有显示
**原因：** 数据保存了但UI没有刷新

**解决方案：**
1. 完全关闭应用（从最近任务中清除）
2. 重新打开应用
3. 再次尝试收藏和查看

### 问题2：历史记录没有显示
**原因：** 播放时间太短，还没来得及记录

**解决方案：**
1. 播放频道时等待至少3-5秒
2. 确保看到视频开始播放
3. 然后返回查看历史

### 问题3：数据不持久
**原因：** SharedPreferences可能没有正确保存

**检查方法：**
```bash
# 连接设备后运行
adb shell
run-as com.example.iptv_player
cd shared_prefs
ls -la
cat com.example.iptv_player_preferences.xml
```

应该看到类似这样的内容：
```xml
<string name="favorites">{"favorites":[...]}</string>
<string name="history">{"history":[...]}</string>
```

### 问题4：应用崩溃
**检查日志：**
```bash
flutter run --release
# 或
adb logcat | grep -i "flutter\|iptv"
```

## 数据结构

### 收藏数据格式
```json
{
  "favorites": [
    {
      "channelId": "config_id:channel_name",
      "addedAt": "2024-12-07T23:30:00.000Z"
    }
  ]
}
```

### 历史数据格式
```json
{
  "history": [
    {
      "channelId": "config_id:channel_name",
      "watchedAt": "2024-12-07T23:30:00.000Z"
    }
  ]
}
```

## 手动测试脚本

### 测试收藏功能
```dart
// 在 main.dart 中临时添加测试代码
void testFavorites() async {
  final repo = FavoriteRepository();
  
  // 添加收藏
  await repo.add('test_channel_1');
  print('Added favorite');
  
  // 检查是否收藏
  final isFav = await repo.isFavorite('test_channel_1');
  print('Is favorite: $isFav');
  
  // 获取所有收藏
  final all = await repo.getAll();
  print('Total favorites: ${all.length}');
  
  // 删除收藏
  await repo.remove('test_channel_1');
  print('Removed favorite');
}
```

### 测试历史记录
```dart
void testHistory() async {
  final repo = HistoryRepository();
  
  // 添加历史
  await repo.add('test_channel_1');
  print('Added to history');
  
  // 获取所有历史
  final all = await repo.getAll();
  print('Total history: ${all.length}');
  
  // 清除历史
  await repo.clear();
  print('Cleared history');
}
```

## 重新构建应用

如果修改了代码，需要重新构建：

```bash
# 清理
flutter clean

# 获取依赖
flutter pub get

# 构建APK
flutter build apk --release

# 安装到设备
adb install -r build/app/outputs/flutter-apk/app-release.apk
```

## 预期行为

### 收藏功能
1. ✅ 频道列表显示收藏按钮
2. ✅ 点击后图标状态改变
3. ✅ 显示操作提示
4. ✅ 收藏页面显示收藏的频道
5. ✅ 可以从收藏页面播放
6. ✅ 可以取消收藏

### 历史记录功能
1. ✅ 播放后自动记录
2. ✅ 历史页面显示播放过的频道
3. ✅ 按时间倒序排列
4. ✅ 可以从历史页面播放
5. ✅ 可以清除所有历史

## 注意事项

1. **Channel ID格式**：频道ID格式为`configId:channelName`，确保这个格式正确
2. **数据持久化**：使用SharedPreferences，数据保存在应用私有目录
3. **UI刷新**：使用Provider确保UI实时更新
4. **错误处理**：所有操作都有try-catch，不会导致崩溃

## 如果仍然有问题

请提供以下信息：
1. 设备型号和Android版本
2. 是否看到AppBar的两个按钮
3. 点击收藏后是否看到提示消息
4. 是否尝试过完全关闭并重新打开应用
5. logcat的相关日志
