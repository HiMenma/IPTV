# Implementation Plan

- [x] 1. 修复Desktop视频播放器资源释放问题
  - 在VideoPlayer.desktop.kt中添加安全的资源释放逻辑
  - 实现释放状态跟踪防止重复释放
  - 在释放前停止播放并移除事件监听器
  - 使用try-catch包裹所有VLC调用
  - _Requirements: 1.1, 1.2, 1.3, 1.4_

- [x] 2. 修复Android数据库驱动初始化问题
  - 修改MainActivity.kt以在onCreate中初始化Koin并传递Context
  - 更新Koin.kt中的appModule以使用androidContext()创建数据库驱动
  - 移除DatabaseDriver.android.kt中会抛出异常的actual函数实现
  - 添加平台检查确保正确的驱动被使用
  - _Requirements: 2.1, 2.2, 2.3, 2.4_

- [x] 3. 改进Desktop数据库驱动实现
  - 在DatabaseDriver.desktop.kt中添加错误处理
  - 确保数据库目录创建成功
  - 添加数据库初始化失败时的日志记录
  - 验证数据库schema创建成功
  - _Requirements: 2.1, 2.3, 2.4_

- [x] 4. 添加VLC可用性检查
  - 创建VlcAvailabilityChecker工具类
  - 在VideoPlayer初始化前检查VLC是否可用
  - 当VLC不可用时显示友好的错误消息和安装指引
  - 提供不同操作系统的安装说明
  - _Requirements: 5.1, 5.2, 5.3, 5.4_

- [x] 5. 改进视频播放器URL切换逻辑
  - 在LaunchedEffect(url)中先停止当前播放
  - 添加延迟确保资源正确释放
  - 使用try-catch包裹媒体加载逻辑
  - 更新错误状态提供详细错误信息
  - _Requirements: 3.1, 3.2, 3.3, 3.4_

- [x] 6. 增强错误处理和日志记录
  - 在PlaylistRepository中添加详细的错误日志
  - 为网络请求添加超时配置
  - 在数据库操作中添加try-catch
  - 记录所有关键操作的成功/失败状态
  - _Requirements: 4.1, 4.2, 4.3, 4.4_

- [x] 7. 添加网络请求重试机制
  - 在PlaylistRepository中实现retryWithBackoff函数
  - 为M3U URL下载添加重试逻辑
  - 为Xtream API调用添加重试逻辑
  - 配置合理的重试次数和延迟
  - _Requirements: 4.4_

- [x] 8. 优化Koin依赖注入配置
  - 确保App.kt不重复初始化Koin
  - 移除App.kt中的initKoin注释
  - 验证平台特定的初始化逻辑
  - 添加Koin初始化状态检查
  - _Requirements: 2.1, 2.4_

- [x] 9. 改进VideoPlayer事件监听器管理
  - 确保DisposableEffect正确移除事件监听器
  - 添加监听器引用跟踪
  - 在dispose时验证所有监听器已移除
  - 防止内存泄漏
  - _Requirements: 3.2, 3.3_

- [x] 10. 添加播放器状态验证
  - 在执行播放器操作前检查播放器是否已初始化
  - 在PlayerControls实现中添加状态检查
  - 防止在播放器释放后调用方法
  - 提供清晰的错误消息
  - _Requirements: 3.4, 4.1_

- [x] 11. 测试和验证修复
  - 测试Desktop版本频道切换不再崩溃
  - 测试Android版本数据库正常工作
  - 测试VLC未安装时的错误提示
  - 测试网络请求失败时的重试机制
  - 验证没有内存泄漏
  - _Requirements: 1.1, 2.1, 4.1, 4.4, 5.1_


- [ ] 12. 添加ExoPlayer流媒体格式支持
  - 在build.gradle.kts的androidMain中添加media3-exoplayer-hls依赖
  - 添加media3-exoplayer-dash依赖
  - 添加media3-exoplayer-smoothstreaming依赖
  - 更新libs.versions.toml确保版本一致
  - _Requirements: 6.1, 6.2, 6.3_

- [ ] 13. 改进ExoPlayer错误消息
  - 在VideoPlayer.android.kt的onPlayerError中添加更多错误代码处理
  - 将ERROR_CODE_DECODER_INIT_FAILED映射为用户友好消息
  - 将ERROR_CODE_UNSPECIFIED映射为"不支持的媒体格式"消息
  - 在错误消息中包含错误代码以便调试
  - _Requirements: 6.4_

- [ ] 14. 实现数据库迁移系统
  - 创建DatabaseMigration.kt工具类
  - 实现版本检测逻辑使用DataStore存储版本号
  - 实现migrateV1ToV2函数添加categoryId列
  - 添加迁移日志记录
  - _Requirements: 7.1, 7.2, 7.3_

- [ ] 15. 集成数据库迁移到应用启动流程
  - 在Koin.kt的数据库初始化前调用DatabaseMigration.migrate
  - 添加错误处理捕获迁移失败
  - 在迁移失败时记录详细错误信息
  - 提供数据库重置选项作为备用方案
  - _Requirements: 7.1, 7.4_

- [ ] 16. 添加数据库备份和恢复功能
  - 实现backupDatabase函数在迁移前备份数据
  - 实现resetDatabase函数用于错误恢复
  - 在Android平台使用Context.getDatabasePath获取数据库文件
  - 在Desktop平台使用文件系统路径
  - _Requirements: 7.4_

- [ ] 17. 测试流媒体播放和数据库迁移
  - 测试Android版本可以播放HLS流媒体
  - 测试DASH和SmoothStreaming格式
  - 测试数据库从v1迁移到v2
  - 测试迁移后categoryId列存在且可用
  - 验证错误消息更加用户友好
  - _Requirements: 6.1, 6.2, 6.3, 6.4, 7.1, 7.2, 7.3_
