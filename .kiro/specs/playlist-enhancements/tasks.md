  # Implementation Plan

- [x] 1. 扩展数据库Schema支持分类和重命名
  - 在IptvDatabase.sq中添加Category表定义
  - 添加updatePlaylistName查询用于更新播放列表名称
  - 添加selectCategoriesByPlaylistId查询用于获取分类列表
  - 添加insertCategory查询用于插入分类
  - 添加countChannelsByCategory查询用于统计分类频道数
  - 为Channel表添加categoryId字段的迁移逻辑
  - _Requirements: 1.3, 4.1, 4.2, 4.4, 4.5_

- [x] 1.1 编写数据库Schema扩展的单元测试
  - 测试Category表的创建和查询
  - 测试播放列表重命名查询
  - 测试分类频道统计查询
  - _Requirements: 1.3, 4.1, 4.2_

- [x] 2. 扩展数据模型支持分类
  - 在Channel模型中添加categoryId字段
  - 确保Category模型包含所有必要字段
  - 更新序列化配置以支持新字段
  - _Requirements: 4.2, 4.4_

- [x] 3. 扩展PlaylistRepository接口和实现
  - 添加renamePlaylist方法实现播放列表重命名
  - 添加getCategories方法获取播放列表的分类列表
  - 添加getChannelsByCategory方法按分类获取频道
  - 添加getCategoryChannelCount方法统计各分类的频道数
  - 实现输入验证（空名称、名称长度限制）
  - _Requirements: 1.3, 1.4, 3.2, 4.3, 4.4, 4.5_

- [x] 3.1 编写Repository方法的单元测试
  - 测试renamePlaylist的正常流程和边界情况
  - 测试getCategories对不同播放列表类型的处理
  - 测试getChannelsByCategory的过滤逻辑
  - 测试getCategoryChannelCount的统计准确性
  - _Requirements: 1.3, 1.4, 3.2, 4.4_

- [x] 4. 扩展XtreamClient支持分类获取
  - 在XtreamClient中添加getLiveCategories方法
  - 实现分类数据的解析和映射
  - 添加错误处理和重试逻辑
  - 在addXtreamAccount时同时获取并存储分类信息
  - _Requirements: 3.1, 3.6_

- [x] 5. 创建播放列表类型工具函数
  - 创建PlaylistTypeExtensions.kt文件
  - 实现getIcon()扩展函数返回类型对应的图标
  - 实现getDisplayName()扩展函数返回类型的显示名称
  - _Requirements: 1.1, 5.1_

- [x] 6. 实现RenamePlaylistDialog组件
  - 创建RenamePlaylistDialog可组合函数
  - 实现名称输入框和验证逻辑
  - 显示播放列表类型标识（只读）
  - 实现确认和取消按钮逻辑
  - 添加错误提示显示
  - _Requirements: 1.2, 1.3, 1.4_

- [x] 6.1 编写重命名对话框的属性测试
  - **Property 2: Rename dialog preserves type**
  - **Validates: Requirements 1.2**

- [x] 6.2 编写重命名对话框的属性测试
  - **Property 4: Empty name rejection**
  - **Validates: Requirements 1.4**

- [x] 7. 更新PlaylistScreen支持重命名
  - 在PlaylistRow中添加类型图标显示
  - 添加长按或编辑按钮触发重命名对话框
  - 在PlaylistScreenModel中添加renamePlaylist方法
  - 实现重命名成功后的UI更新
  - _Requirements: 1.1, 1.2, 1.5, 5.1_

- [x] 7.1 编写播放列表显示的属性测试
  - **Property 1: Playlist display completeness**
  - **Validates: Requirements 1.1, 5.1**

- [x] 7.2 编写重命名持久化的属性测试
  - **Property 3: Rename persistence**
  - **Validates: Requirements 1.3, 4.5**

- [x] 7.3 编写UI响应式更新的属性测试
  - **Property 5: Reactive UI updates**
  - **Validates: Requirements 1.5**

- [x] 8. 创建CategoryListScreen组件
  - 创建CategoryListScreen类和Content可组合函数
  - 实现CategoryListScreenModel管理分类状态
  - 创建CategoryRow组件显示分类和频道数
  - 实现加载状态、错误状态和空状态的UI
  - 添加点击分类导航到频道列表的逻辑
  - _Requirements: 3.1, 3.3, 3.5, 3.6, 5.2_

- [x] 8.1 编写分类列表的属性测试
  - **Property 8: Xtream category-first navigation**
  - **Validates: Requirements 3.1**

- [x] 8.2 编写分类显示的属性测试
  - **Property 16: Category display completeness**
  - **Validates: Requirements 5.2**

- [x] 8.3 编写加载状态的属性测试
  - **Property 11: Loading state visibility**
  - **Validates: Requirements 3.5, 5.5**

- [x] 8.4 编写错误恢复的属性测试
  - **Property 12: Error recovery options**
  - **Validates: Requirements 3.6**

- [x] 9. 修改ChannelListScreen支持分类过滤
  - 修改ChannelListScreen构造函数接受可选的categoryId参数
  - 更新ChannelListScreenModel根据categoryId过滤频道
  - 添加返回分类列表的导航按钮（仅Xtream且有categoryId时）
  - 保持现有的搜索和收藏功能
  - _Requirements: 3.2, 3.4, 4.4_

- [x] 9.1 编写分类过滤的属性测试
  - **Property 9: Category filtering**
  - **Validates: Requirements 3.2**

- [x] 9.2 编写分类导航的属性测试
  - **Property 10: Category navigation availability**
  - **Validates: Requirements 3.4**

- [x] 9.3 编写分类查询的属性测试
  - **Property 15: Category-based channel filtering**
  - **Validates: Requirements 4.4**

- [x] 10. 更新PlaylistScreen的导航逻辑
  - 修改PlaylistRow的点击处理
  - 对于Xtream类型播放列表，导航到CategoryListScreen
  - 对于M3U类型播放列表，直接导航到ChannelListScreen
  - 确保导航参数正确传递
  - _Requirements: 3.1_

- [x] 11. 改进PlayerScreen的错误处理
  - 在PlayerScreen中添加错误状态管理
  - 创建ErrorScreen组件显示错误消息和操作按钮
  - 在VideoPlayer初始化前验证URL有效性
  - 添加try-catch包裹VideoPlayer组件
  - 实现重试和返回频道列表的功能
  - _Requirements: 2.1, 2.2, 2.3, 2.5_

- [x] 11.1 编写播放安全性的属性测试
  - **Property 6: M3U playback safety**
  - **Validates: Requirements 2.1, 2.2, 2.5**

- [x] 12. 改进VideoPlayer组件的错误处理
  - 添加onError和onPlayerInitFailed回调参数
  - 在DisposableEffect中添加URL验证
  - 使用try-catch包裹播放器初始化
  - 添加MediaPlayerEventListener监听播放错误
  - 实现PlayerState枚举跟踪播放器状态
  - _Requirements: 2.1, 2.2, 2.3, 2.5_

- [x] 13. 改进VideoPlayer的资源释放逻辑
  - 创建safeReleasePlayer辅助函数
  - 在切换URL前先释放当前播放器资源
  - 确保所有释放操作都在try-catch中
  - 添加释放状态标志防止重复释放
  - _Requirements: 2.4_

- [x] 13.1 编写资源清理的属性测试
  - **Property 7: Resource cleanup on channel switch**
  - **Validates: Requirements 2.4**

- [x] 14. 更新DAO层支持新查询
  - 在PlaylistDao中添加updatePlaylistName方法
  - 创建CategoryDao接口和实现
  - 实现selectCategoriesByPlaylistId方法
  - 实现insertCategory方法
  - 实现countChannelsByCategory方法
  - 在PlaylistDao中添加getChannelsByCategory方法
  - _Requirements: 1.3, 3.1, 3.2, 4.4_

- [x] 14.1 编写数据完整性的属性测试
  - **Property 13: Playlist data completeness**
  - **Validates: Requirements 4.1, 4.3**

- [x] 14.2 编写Xtream频道分类的属性测试
  - **Property 14: Xtream channel category association**
  - **Validates: Requirements 4.2**

- [x] 15. 实现数据库迁移逻辑
  - 在DatabaseDriver中添加schema版本管理
  - 实现从版本1到版本2的迁移
  - 添加Category表创建语句
  - 添加Channel表的categoryId字段
  - 测试迁移在Android和Desktop平台的执行
  - _Requirements: 4.2_

- [x] 16. 更新Koin依赖注入配置
  - 在appModule中注册CategoryDao
  - 确保PlaylistRepository可以访问CategoryDao
  - 验证所有新组件的依赖注入配置
  - _Requirements: 3.1, 3.2_

- [x] 17. 添加通用UI组件
  - 创建ErrorView组件用于显示错误和重试按钮
  - 创建EmptyView组件用于显示空状态提示
  - 创建LoadingView组件用于显示加载状态
  - 在多个Screen中复用这些组件
  - _Requirements: 3.3, 3.5, 3.6, 5.4_

- [x] 18. 测试和验证所有功能
  - 测试播放列表重命名功能（M3U和Xtream）
  - 测试M3U频道播放不再崩溃
  - 测试Xtream播放列表显示分类列表
  - 测试分类导航和频道过滤
  - 测试错误处理和重试机制
  - 验证数据库迁移正确执行
  - 测试在Android和Desktop平台的表现
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 2.1, 2.2, 2.3, 2.4, 2.5, 3.1, 3.2, 3.3, 3.4, 3.5, 3.6_
