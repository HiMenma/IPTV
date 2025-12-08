# 收藏夹和历史记录修复

## 修复内容

### 1. 点击导航栏自动刷新收藏夹和历史记录
**文件**: `lib/main.dart`

**问题**: 切换到Favorites或History标签时，不会自动刷新数据，需要手动点击刷新按钮。

**解决方案**: 在底部导航栏的`onDestinationSelected`回调中，检测切换到Favorites（index=1）或History（index=2）标签时，自动调用相应的加载方法：

```dart
onDestinationSelected: (index) {
  setState(() {
    _currentIndex = index;
  });
  
  // Refresh data when switching to Favorites or History tabs
  if (index == 1) {
    // Favorites tab
    context.read<ChannelViewModel>().loadFavorites();
  } else if (index == 2) {
    // History tab
    context.read<ChannelViewModel>().loadHistory();
  }
}
```

### 2. 历史记录列表不显示记录
**文件**: `lib/viewmodels/player_viewmodel.dart`

**问题**: PlayerViewModel使用的是旧的`HistoryRepository`（SharedPreferences版本），而不是新的`HistoryRepositorySQLite`，导致播放记录没有保存到SQLite数据库中。

**解决方案**: 
- 将import从`history_repository.dart`改为`history_repository_sqlite.dart`
- 将`HistoryRepository`类型改为`HistoryRepositorySQLite`
- 更新构造函数参数和默认实例化

**修改前**:
```dart
import '../repositories/history_repository.dart';

class PlayerViewModel extends ChangeNotifier {
  final HistoryRepository _historyRepository;
  
  PlayerViewModel({
    HistoryRepository? historyRepository,
  }) : _historyRepository = historyRepository ?? HistoryRepository() {
    ...
  }
}
```

**修改后**:
```dart
import '../repositories/history_repository_sqlite.dart';

class PlayerViewModel extends ChangeNotifier {
  final HistoryRepositorySQLite _historyRepository;
  
  PlayerViewModel({
    HistoryRepositorySQLite? historyRepository,
  }) : _historyRepository = historyRepository ?? HistoryRepositorySQLite() {
    ...
  }
}
```

## 测试建议

1. **测试收藏夹自动刷新**:
   - 在频道列表中添加一个收藏
   - 切换到Favorites标签，应该立即看到新添加的收藏

2. **测试历史记录自动刷新**:
   - 播放一个频道
   - 切换到History标签，应该立即看到播放记录

3. **测试历史记录保存**:
   - 播放多个不同的频道
   - 关闭应用后重新打开
   - 切换到History标签，应该能看到之前的播放记录

## 相关文件
- `lib/main.dart` - 底部导航栏逻辑
- `lib/viewmodels/player_viewmodel.dart` - 播放器视图模型
- `lib/repositories/history_repository_sqlite.dart` - SQLite历史记录存储
- `lib/views/screens/favorites_screen.dart` - 收藏夹界面
- `lib/views/screens/history_screen.dart` - 历史记录界面
