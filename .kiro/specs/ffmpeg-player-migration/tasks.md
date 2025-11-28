# Implementation Plan

- [x] 1. 项目配置和依赖设置
  - 在 `composeApp/build.gradle.kts` 中添加 JavaCV 和 FFmpeg 依赖
  - 配置平台特定的 FFmpeg 库
  - 添加必要的 JVM 参数配置
  - _Requirements: 1.1_

- [x] 2. 实现核心数据模型和工具类
- [x] 2.1 创建音频时钟（AudioClock）
  - 实现时间戳跟踪和更新逻辑
  - 提供线程安全的时间查询接口
  - _Requirements: 6.1_

- [x] 2.2 创建媒体信息模型（MediaInfo）
  - 定义媒体元数据结构
  - 实现从 FFmpeg 提取媒体信息的方法
  - _Requirements: 7.1_

- [x] 2.3 创建播放统计模型（PlaybackStatistics）
  - 定义统计指标数据结构
  - 实现统计数据收集和更新逻辑
  - _Requirements: 7.2, 7.4_

- [ ]* 2.4 编写数据模型单元测试
  - 测试 AudioClock 的时间跟踪准确性
  - 测试 MediaInfo 的数据提取
  - 测试 PlaybackStatistics 的统计更新
  - _Requirements: 6.1, 7.1, 7.2_

- [x] 3. 实现硬件加速管理器
- [x] 3.1 创建 HardwareAccelerationManager
  - 实现平台检测逻辑（macOS/Linux/Windows）
  - 实现硬件加速能力检测
  - 提供平台特定的硬件加速配置
  - _Requirements: 4.1, 4.2, 4.3, 4.4_

- [x] 3.2 实现硬件加速回退机制
  - 检测硬件加速失败
  - 自动切换到软件解码
  - 记录回退事件
  - _Requirements: 4.5_

- [ ]* 3.3 编写硬件加速属性测试
  - **Property 10: 硬件加速检测和启用**
  - **Validates: Requirements 4.1**

- [ ]* 3.4 编写硬件加速回退属性测试
  - **Property 11: 硬件加速回退**
  - **Validates: Requirements 4.5**

- [ ]* 3.5 编写硬件加速单元测试
  - 测试平台检测逻辑
  - 测试配置生成
  - 测试回退机制
  - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5_

- [x] 4. 实现流类型检测器
- [x] 4.1 创建 StreamTypeDetector
  - 实现直播流检测逻辑
  - 实现协议识别（HTTP/HTTPS/RTSP/HLS/FILE）
  - 生成推荐的缓冲配置
  - _Requirements: 3.1, 3.2, 3.3, 3.4, 5.1_

- [ ]* 4.2 编写流类型检测单元测试
  - 测试直播流识别
  - 测试协议检测
  - 测试缓冲配置生成
  - _Requirements: 3.1, 3.2, 3.3, 3.4, 5.1_

- [x] 5. 实现音视频同步器
- [x] 5.1 创建 AudioVideoSynchronizer
  - 实现同步延迟计算
  - 实现丢帧判断逻辑
  - 实现同步误差监控
  - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5_

- [ ]* 5.2 编写音视频同步属性测试
  - **Property 17: 音视频同步**
  - **Validates: Requirements 6.1**

- [ ]* 5.3 编写同步恢复属性测试
  - **Property 18: 同步恢复机制**
  - **Validates: Requirements 6.2, 6.3, 6.4**

- [ ]* 5.4 编写同步监控属性测试
  - **Property 19: 同步误差监控**
  - **Validates: Requirements 6.5**

- [ ]* 5.5 编写同步器单元测试
  - 测试延迟计算准确性
  - 测试丢帧判断逻辑
  - 测试误差阈值
  - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5_

- [x] 6. 实现 FFmpeg 解码器
- [x] 6.1 创建 FFmpegDecoder 类
  - 实现解码线程主循环
  - 实现视频帧和音频帧分离
  - 实现帧队列管理
  - 处理解码错误和异常
  - _Requirements: 1.1, 1.4_

- [x] 6.2 实现 FFmpegFrameGrabber 配置
  - 根据流类型配置 grabber 参数
  - 应用硬件加速配置
  - 设置直播流优化选项
  - _Requirements: 3.1, 3.2, 3.3, 3.4, 4.1, 5.1_

- [ ]* 6.3 编写解码器属性测试
  - **Property 1: FFmpeg 解码初始化**
  - **Validates: Requirements 1.1**

- [ ]* 6.4 编写解码错误处理属性测试
  - **Property 4: 错误处理和通知**
  - **Validates: Requirements 1.4**

- [ ]* 6.5 编写解码器单元测试
  - 测试帧解码和分离
  - 测试队列管理
  - 测试错误处理
  - _Requirements: 1.1, 1.4_

- [x] 7. 实现视频渲染器
- [x] 7.1 创建 VideoRenderer 类
  - 实现渲染线程主循环
  - 实现帧格式转换（Frame -> BufferedImage）
  - 实现 Canvas 渲染逻辑
  - 集成音视频同步器
  - _Requirements: 1.2, 6.1, 6.2, 6.3, 6.4_

- [x] 7.2 实现双缓冲渲染优化
  - 创建后缓冲区
  - 实现缓冲区交换
  - 处理尺寸变化
  - _Requirements: 1.2_

- [ ]* 7.3 编写视频渲染属性测试
  - **Property 2: 视频帧渲染**
  - **Validates: Requirements 1.2**

- [ ]* 7.4 编写渲染器单元测试
  - 测试帧转换
  - 测试 Canvas 渲染
  - 测试双缓冲机制
  - _Requirements: 1.2_

- [x] 8. 实现音频播放器
- [x] 8.1 创建 AudioPlayer 类
  - 实现音频线程主循环
  - 初始化 SourceDataLine
  - 实现音频帧播放
  - 更新音频时钟
  - _Requirements: 1.3, 6.1_

- [x] 8.2 实现音量控制
  - 实现音量调整逻辑
  - 应用音量到音频样本
  - _Requirements: 2.4_

- [ ]* 8.3 编写音频播放属性测试
  - **Property 3: 音频帧播放**
  - **Validates: Requirements 1.3**

- [ ]* 8.4 编写音量控制属性测试
  - **Property 8: 音量控制**
  - **Validates: Requirements 2.4**

- [ ]* 8.5 编写音频播放器单元测试
  - 测试音频线初始化
  - 测试音频播放
  - 测试音量控制
  - _Requirements: 1.3, 2.4, 6.1_

- [x] 9. 实现核心播放引擎
- [x] 9.1 创建 FFmpegPlayerEngine 类
  - 实现播放器初始化逻辑
  - 创建和管理三个工作线程
  - 实现播放状态管理
  - _Requirements: 1.1, 1.2, 1.3_

- [x] 9.2 实现播放控制方法
  - 实现 play() 方法
  - 实现 pause() 和 resume() 方法
  - 实现 seekTo() 方法
  - 实现 setVolume() 方法
  - 实现 stop() 方法
  - _Requirements: 2.1, 2.2, 2.3, 2.4_

- [x] 9.3 实现资源管理和释放
  - 实现 release() 方法
  - 确保线程正确停止
  - 清理队列和资源
  - 处理释放失败情况
  - _Requirements: 1.5, 8.1, 8.2, 8.3, 8.5_

- [ ]* 9.4 编写播放控制属性测试
  - **Property 6: 播放控制响应**
  - **Validates: Requirements 2.1, 2.2**

- [ ]* 9.5 编写跳转功能属性测试
  - **Property 7: 跳转功能**
  - **Validates: Requirements 2.3**

- [ ]* 9.6 编写控制错误处理属性测试
  - **Property 9: 控制命令错误处理**
  - **Validates: Requirements 2.5**

- [ ]* 9.7 编写资源释放属性测试
  - **Property 5: 资源释放完整性**
  - **Validates: Requirements 1.5, 8.1**

- [ ]* 9.8 编写资源切换属性测试
  - **Property 24: 资源切换管理**
  - **Validates: Requirements 8.2**

- [ ]* 9.9 编写播放引擎单元测试
  - 测试初始化逻辑
  - 测试播放控制
  - 测试资源管理
  - _Requirements: 1.1, 1.2, 1.3, 2.1, 2.2, 2.3, 2.4, 1.5, 8.1, 8.2_

- [x] 10. 实现直播流优化功能
- [x] 10.1 实现低延迟缓冲策略
  - 为直播流配置小缓冲区
  - 实现动态缓冲调整
  - _Requirements: 5.1, 5.2_

- [x] 10.2 实现延迟累积处理
  - 检测延迟累积
  - 实现自动跳帧逻辑
  - _Requirements: 5.3_

- [x] 10.3 实现自动重连机制
  - 检测连接中断
  - 实现重连逻辑（指数退避）
  - 恢复播放位置
  - _Requirements: 5.4, 5.5_

- [ ]* 10.4 编写直播流缓冲属性测试
  - **Property 12: 直播流低延迟缓冲**
  - **Validates: Requirements 5.1**

- [ ]* 10.5 编写网络自适应属性测试
  - **Property 13: 网络抖动自适应**
  - **Validates: Requirements 5.2**

- [ ]* 10.6 编写跳帧属性测试
  - **Property 14: 延迟累积跳帧**
  - **Validates: Requirements 5.3**

- [ ]* 10.7 编写重连属性测试
  - **Property 15: 直播流重连**
  - **Validates: Requirements 5.4**

- [ ]* 10.8 编写恢复播放属性测试
  - **Property 16: 直播流恢复**
  - **Validates: Requirements 5.5**

- [ ]* 10.9 编写直播流优化单元测试
  - 测试缓冲策略
  - 测试跳帧逻辑
  - 测试重连机制
  - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5_

- [x] 11. 实现诊断和监控功能
- [x] 11.1 实现播放信息日志记录
  - 记录媒体格式和编解码器信息
  - 记录流信息
  - _Requirements: 7.1_

- [x] 11.2 实现播放统计更新
  - 定期更新帧率、比特率
  - 更新缓冲状态
  - _Requirements: 7.2_

- [x] 11.3 实现错误日志记录
  - 记录解码错误详情
  - 记录时间戳和上下文
  - _Requirements: 7.3_

- [x] 11.4 实现性能监控
  - 监控 CPU 使用率
  - 监控内存占用
  - _Requirements: 7.4_

- [x] 11.5 实现诊断报告生成
  - 收集所有关键指标
  - 生成格式化报告
  - _Requirements: 7.5_

- [ ]* 11.6 编写日志记录属性测试
  - **Property 20: 播放信息日志**
  - **Validates: Requirements 7.1**

- [ ]* 11.7 编写统计更新属性测试
  - **Property 21: 播放统计更新**
  - **Validates: Requirements 7.2**

- [ ]* 11.8 编写错误日志属性测试
  - **Property 22: 解码错误日志**
  - **Validates: Requirements 7.3**

- [ ]* 11.9 编写性能监控属性测试
  - **Property 23: 性能监控**
  - **Validates: Requirements 7.4**

- [ ]* 11.10 编写诊断功能单元测试
  - 测试日志记录
  - 测试统计更新
  - 测试报告生成
  - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5_

- [x] 12. 实现全屏播放支持
- [x] 12.1 实现全屏模式切换
  - 检测全屏状态变化
  - 调整渲染目标
  - _Requirements: 10.1_

- [x] 12.2 实现宽高比保持
  - 计算正确的渲染尺寸
  - 居中显示视频
  - _Requirements: 10.2_

- [x] 12.3 实现全屏退出恢复
  - 恢复窗口模式渲染
  - 保持播放状态
  - _Requirements: 10.3_

- [x] 12.4 实现动态尺寸调整
  - 监听窗口大小变化
  - 更新渲染尺寸
  - _Requirements: 10.4_

- [ ]* 12.5 编写全屏切换属性测试
  - **Property 28: 全屏模式切换**
  - **Validates: Requirements 10.1**

- [ ]* 12.6 编写宽高比属性测试
  - **Property 29: 全屏宽高比保持**
  - **Validates: Requirements 10.2**

- [ ]* 12.7 编写全屏退出属性测试
  - **Property 30: 全屏退出恢复**
  - **Validates: Requirements 10.3**

- [ ]* 12.8 编写尺寸调整属性测试
  - **Property 31: 全屏尺寸自适应**
  - **Validates: Requirements 10.4**

- [ ]* 12.9 编写全屏错误处理属性测试
  - **Property 32: 全屏切换错误处理**
  - **Validates: Requirements 10.5**

- [ ]* 12.10 编写全屏功能单元测试
  - 测试模式切换
  - 测试宽高比计算
  - 测试尺寸调整
  - _Requirements: 10.1, 10.2, 10.3, 10.4, 10.5_

- [x] 13. 创建 FFmpeg VideoPlayer Composable
- [x] 13.1 创建 FFmpegVideoPlayer.desktop.kt
  - 实现 VideoPlayer Composable 函数
  - 集成 FFmpegPlayerEngine
  - 使用 SwingPanel 嵌入 Canvas
  - 保持与现有 API 完全兼容
  - _Requirements: 9.1, 9.2, 9.3, 9.4, 9.5_

- [x] 13.2 实现 PlayerControls 接口
  - 实现所有控制方法
  - 委托给 FFmpegPlayerEngine
  - _Requirements: 9.2_

- [x] 13.3 实现 PlayerState 更新
  - 监听播放器状态变化
  - 更新 Compose 状态
  - _Requirements: 9.3_

- [ ]* 13.4 编写 Composable 集成测试
  - 测试 API 兼容性
  - 测试状态更新
  - 测试控制接口
  - _Requirements: 9.1, 9.2, 9.3, 9.4, 9.5_

- [x] 14. 实现配置开关和迁移支持
- [x] 14.1 添加播放器实现选择配置
  - 在 Koin 中添加配置开关
  - 支持通过配置选择 VLC 或 FFmpeg
  - _Requirements: 9.1_

- [x] 14.2 创建播放器实现抽象层
  - 定义通用播放器接口
  - 实现 VLC 和 FFmpeg 适配器
  - _Requirements: 9.1, 9.2_

- [ ]* 14.3 编写配置切换测试
  - 测试 VLC 实现选择
  - 测试 FFmpeg 实现选择
  - 测试运行时切换
  - _Requirements: 9.1_

- [x] 15. 第一次检查点 - 基础功能验证
  - 确保所有测试通过
  - 验证基础播放功能
  - 验证资源管理
  - 询问用户是否有问题

- [x] 16. 端到端集成测试
- [x] 16.1 编写 HTTP/HTTPS 流播放测试
  - 测试网络流播放
  - 验证播放控制
  - _Requirements: 3.1_

- [x] 16.2 编写 HLS 流播放测试
  - 测试 m3u8 播放
  - 验证自适应流
  - _Requirements: 3.3_

- [x] 16.3 编写本地文件播放测试
  - 测试本地视频文件
  - 验证完整播放流程
  - _Requirements: 3.4_

- [ ]* 16.4 编写格式支持测试
  - 测试多种视频格式（MP4、MKV、AVI）
  - 测试多种编解码器（H.264、H.265、VP9）
  - _Requirements: 3.1, 3.2, 3.3, 3.4_

- [-] 17. 性能测试和优化
- [ ]* 17.1 编写性能基准测试
  - 测试首帧时间
  - 测试 CPU 使用率
  - 测试内存占用
  - 对比 VLC 实现
  - _Requirements: 1.1, 1.2, 1.3_

- [x] 17.2 优化关键路径
  - 优化解码线程
  - 优化渲染流程
  - 优化队列管理
  - _Requirements: 1.1, 1.2, 1.3_

- [ ]* 17.3 编写内存泄漏测试
  - 长时间播放测试
  - 频繁切换测试
  - 验证资源释放
  - _Requirements: 8.1, 8.2, 8.3_

- [ ] 18. 第二次检查点 - 完整功能验证
  - 确保所有测试通过
  - 验证所有播放功能
  - 验证性能指标
  - 询问用户是否有问题

- [x] 19. 文档和示例
- [x] 19.1 编写迁移指南
  - 说明如何启用 FFmpeg 播放器
  - 说明配置选项
  - 提供故障排除指南
  - _Requirements: 9.1_

- [x] 19.2 编写 API 文档
  - 文档化所有公共接口
  - 提供使用示例
  - 说明与 VLC 的差异
  - _Requirements: 9.1, 9.2, 9.3, 9.4, 9.5_

- [x] 19.3 更新 README
  - 添加 FFmpeg 播放器说明
  - 更新依赖列表
  - 更新构建说明
  - _Requirements: 9.1_

- [x] 20. 最终检查点 - 发布准备
  - 确保所有测试通过
  - 验证文档完整性
  - 验证迁移路径
  - 询问用户是否准备发布

