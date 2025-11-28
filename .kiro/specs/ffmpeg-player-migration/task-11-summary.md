# Task 11: 诊断和监控功能实现总结

## 概述

成功实现了完整的诊断和监控功能，包括播放信息日志记录、播放统计更新、错误日志记录、性能监控和诊断报告生成。

## 实现的组件

### 11.1 PlaybackLogger - 播放日志记录器

**文件**: `composeApp/src/desktopMain/kotlin/com/menmapro/iptv/player/ffmpeg/PlaybackLogger.kt`

**功能**:
- 记录播放开始和停止信息
- 记录媒体格式和编解码器信息
- 记录流信息（分辨率、帧率、比特率等）
- 记录解码错误、网络错误、渲染错误、音频错误
- 记录同步警告和性能警告
- 支持多种日志级别（DEBUG, INFO, WARN, ERROR）
- 内存中保留最近 1000 条日志
- 可选的文件日志记录
- 提供日志查询和过滤功能

**关键特性**:
- 线程安全的日志队列
- 结构化的日志条目（包含时间戳、级别、类别、消息和上下文）
- 格式化的日志输出
- 日志摘要生成

**Requirements**: 7.1 - 播放信息日志记录, 7.3 - 错误日志记录

### 11.2 StatisticsMonitor - 统计监控器

**文件**: `composeApp/src/desktopMain/kotlin/com/menmapro/iptv/player/ffmpeg/StatisticsMonitor.kt`

**功能**:
- 定期更新帧率和比特率
- 更新缓冲状态（视频和音频队列）
- 监控 CPU 使用率
- 监控内存占用
- 检查缓冲区健康状态
- 检查性能问题（高 CPU、高内存、高丢帧率、低帧率）
- 生成统计报告

**关键特性**:
- 后台监控线程（每秒更新一次）
- 性能指标每 5 秒更新一次
- 自动检测和警告性能问题
- 缓冲区健康检查

**Requirements**: 7.2 - 播放统计更新, 7.4 - 性能监控

### 11.3 ErrorHandler - 错误处理器

**文件**: `composeApp/src/desktopMain/kotlin/com/menmapro/iptv/player/ffmpeg/ErrorHandler.kt`

**功能**:
- 分类和处理解码错误
- 分类和处理网络错误
- 分类和处理渲染错误
- 分类和处理音频错误
- 处理初始化错误和资源错误
- 记录详细的错误信息（类型、时间戳、上下文）
- 错误严重程度分类（FATAL, RECOVERABLE, WARNING）
- 错误历史记录（最多 100 个）

**关键特性**:
- 智能错误分类（根据异常消息判断严重程度）
- 结构化的错误信息（包含异常、时间戳、上下文）
- 错误查询和过滤（按类型、严重程度）
- 错误报告生成

**Requirements**: 7.3 - 错误日志记录

### 11.4 PerformanceMonitor - 性能监控器

**文件**: `composeApp/src/desktopMain/kotlin/com/menmapro/iptv/player/ffmpeg/PerformanceMonitor.kt`

**功能**:
- 监控 CPU 使用率（使用 OperatingSystemMXBean 或线程 CPU 时间）
- 监控内存占用（总内存和堆内存）
- 监控线程状态
- 监控 GC 活动（GC 次数、GC 时间、GC 频率）
- 性能历史记录（最多 60 个快照）
- 性能问题检测和警告
- 平均性能指标计算

**关键特性**:
- 多种 CPU 测量方法（支持不同 JVM 实现）
- 详细的内存使用信息（总内存、堆内存、百分比）
- GC 活动监控
- 性能快照和历史记录
- 自动检测性能问题（高 CPU、高内存、高 GC 活动、高线程数）

**Requirements**: 7.4 - 性能监控

### 11.5 DiagnosticReportGenerator - 诊断报告生成器

**文件**: `composeApp/src/desktopMain/kotlin/com/menmapro/iptv/player/ffmpeg/DiagnosticReportGenerator.kt`

**功能**:
- 生成完整的诊断报告
- 收集所有关键指标
- 生成格式化报告
- 支持多种报告格式（完整、摘要、JSON）
- 报告保存到文件

**报告包含的部分**:
1. 系统信息（操作系统、Java 运行时、硬件）
2. 播放信息（URL、硬件加速、流类型、运行时间）
3. 媒体信息（格式、时长、编解码器、分辨率、帧率、比特率）
4. 播放统计（帧数、FPS、丢帧率、缓冲区、CPU、内存）
5. 音视频同步信息
6. 直播流优化信息（如果适用）
7. 性能指标（CPU、内存、堆、线程、GC）
8. 错误历史
9. 日志摘要

**关键特性**:
- 格式化的文本报告（易于阅读）
- JSON 格式报告（易于解析）
- 简化报告（仅关键信息）
- 报告保存到文件

**Requirements**: 7.5 - 诊断报告生成

## 集成说明

这些组件设计为可以集成到 `FFmpegPlayerEngine` 中：

```kotlin
class FFmpegPlayerEngine(
    private val onStateChange: (PlayerState) -> Unit,
    private val onError: (String) -> Unit
) {
    // 创建日志记录器
    private val logger = PlaybackLogger(
        enableFileLogging = true,
        logFilePath = "ffmpeg-player.log"
    )
    
    // 创建错误处理器
    private val errorHandler = ErrorHandler(logger, onError)
    
    // 创建性能监控器
    private val performanceMonitor = PerformanceMonitor(statistics, logger)
    
    // 创建统计监控器
    private var statisticsMonitor: StatisticsMonitor? = null
    
    fun play(url: String, canvas: Canvas) {
        // ... 初始化代码 ...
        
        // 记录播放开始
        logger.logPlaybackStart(url, mediaInfo, hardwareAccelerationType)
        
        // 启动统计监控
        statisticsMonitor = StatisticsMonitor(
            statistics = statistics,
            videoQueue = videoFrameQueue,
            audioQueue = audioFrameQueue,
            isPlaying = isPlaying,
            logger = logger
        )
        statisticsMonitor?.start()
        
        // ... 其他代码 ...
    }
    
    fun release() {
        // 停止统计监控
        statisticsMonitor?.stop()
        
        // 记录播放停止
        logger.logPlaybackStop(statistics)
        
        // ... 清理代码 ...
    }
    
    fun generateDiagnosticReport(): String {
        val generator = DiagnosticReportGenerator(
            mediaInfo = mediaInfo,
            statistics = statistics,
            synchronizer = synchronizer,
            liveStreamOptimizer = liveStreamOptimizer,
            hardwareAccelerationType = hardwareAccelerationType,
            currentUrl = currentUrl,
            logger = logger,
            errorHandler = errorHandler,
            performanceMonitor = performanceMonitor
        )
        
        return generator.generateFullReport()
    }
}
```

## 使用示例

### 1. 日志记录

```kotlin
// 记录播放开始
logger.logPlaybackStart(url, mediaInfo, hardwareAccelerationType)

// 记录解码错误
logger.logDecodingError(
    error = "Failed to decode frame",
    timestamp = frameTimestamp,
    context = mapOf("frame_type" to "video")
)

// 记录性能警告
logger.logPerformanceWarning(
    message = "High CPU usage",
    context = mapOf("cpu_usage" to 85.5)
)

// 获取最近的日志
val recentLogs = logger.getRecentLogs(100)

// 生成日志摘要
val summary = logger.generateLogSummary()
```

### 2. 错误处理

```kotlin
// 处理解码错误
val severity = errorHandler.handleDecodingError(
    exception = e,
    frameTimestamp = timestamp,
    context = mapOf("codec" to "h264")
)

// 根据严重程度决定操作
when (severity) {
    ErrorSeverity.FATAL -> stop()
    ErrorSeverity.RECOVERABLE -> continue()
    ErrorSeverity.WARNING -> log()
}

// 获取错误历史
val errors = errorHandler.getErrorHistory(10)

// 生成错误报告
val report = errorHandler.generateErrorReport()
```

### 3. 性能监控

```kotlin
// 更新性能指标
performanceMonitor.updateMetrics()

// 获取平均 CPU 使用率
val avgCpu = performanceMonitor.getAverageCpuUsage(10)

// 获取性能历史
val history = performanceMonitor.getPerformanceHistory(60)

// 生成性能报告
val report = performanceMonitor.generateReport()
```

### 4. 诊断报告

```kotlin
// 生成完整报告
val fullReport = generator.generateFullReport()

// 生成摘要报告
val summary = generator.generateSummaryReport()

// 生成 JSON 报告
val json = generator.generateJsonReport()

// 保存报告到文件
generator.saveToFile("diagnostic-report.txt", "full")
generator.saveToFile("diagnostic-report.json", "json")
```

## 验证

所有实现的文件都已通过编译检查，没有语法错误或类型错误。

## 下一步

这些诊断和监控组件现在可以集成到 `FFmpegPlayerEngine` 中，以提供：
- 详细的播放日志
- 实时性能监控
- 错误跟踪和分析
- 全面的诊断报告

这将大大提高播放器的可维护性和可调试性，帮助快速定位和解决问题。

## Requirements 覆盖

- ✅ 7.1: 播放信息日志记录 - PlaybackLogger
- ✅ 7.2: 播放统计更新 - StatisticsMonitor
- ✅ 7.3: 错误日志记录 - ErrorHandler
- ✅ 7.4: 性能监控 - PerformanceMonitor
- ✅ 7.5: 诊断报告生成 - DiagnosticReportGenerator
