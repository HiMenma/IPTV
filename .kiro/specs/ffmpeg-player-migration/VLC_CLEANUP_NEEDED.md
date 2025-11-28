# VLC清理待办事项

## 当前状态
VLC播放器的核心实现已被移除，但还有一些辅助文件仍然引用VLC API，导致编译错误。

## 需要处理的文件

### 1. 必须删除或重写的文件（导致编译错误）

#### VideoPlaybackPreCheck.kt
**路径:** `composeApp/src/desktopMain/kotlin/com/menmapro/iptv/ui/components/VideoPlaybackPreCheck.kt`
**问题:** 引用 `uk.co.caprica.vlcj` 和 `EmbeddedMediaPlayerComponent`
**建议:** 删除此文件（FFmpeg不需要VLC特定的预检查）

#### VideoRenderingDiagnostics.kt
**路径:** `composeApp/src/desktopMain/kotlin/com/menmapro/iptv/ui/components/VideoRenderingDiagnostics.kt`
**问题:** 引用 `uk.co.caprica.vlcj.player.base.MediaPlayer`
**建议:** 删除此文件（FFmpeg有自己的诊断系统）

#### VideoRenderingRecovery.kt
**路径:** `composeApp/src/desktopMain/kotlin/com/menmapro/iptv/ui/components/VideoRenderingRecovery.kt`
**问题:** 引用 `uk.co.caprica.vlcj` 和 `EmbeddedMediaPlayerComponent`
**建议:** 删除此文件（FFmpeg有自己的错误恢复）

#### VideoSurfaceValidator.kt
**路径:** `composeApp/src/desktopMain/kotlin/com/menmapro/iptv/ui/components/VideoSurfaceValidator.kt`
**问题:** 引用 `uk.co.caprica.vlcj` 和 `EmbeddedMediaPlayerComponent`
**建议:** 删除此文件（FFmpeg不使用VLC的视频表面）

#### VlcAvailabilityChecker.kt
**路径:** `composeApp/src/desktopMain/kotlin/com/menmapro/iptv/ui/components/VlcAvailabilityChecker.kt`
**问题:** 引用 `uk.co.caprica.vlcj.discovery.NativeDiscovery`
**建议:** 删除此文件（不再需要检查VLC）

#### VlcConfiguration.kt
**路径:** `composeApp/src/desktopMain/kotlin/com/menmapro/iptv/ui/components/VlcConfiguration.kt`
**问题:** VLC特定配置
**建议:** 删除此文件（FFmpeg有自己的配置）

### 2. 测试文件需要更新

#### VideoPlayerLifecycleTest.kt
**路径:** `composeApp/src/desktopTest/kotlin/com/menmapro/iptv/ui/components/VideoPlayerLifecycleTest.kt`
**问题:** 可能引用VLC相关功能
**建议:** 检查并更新为FFmpeg测试

#### VideoPlaybackPreCheckTest.kt
**路径:** `composeApp/src/desktopTest/kotlin/com/menmapro/iptv/ui/components/VideoPlaybackPreCheckTest.kt`
**问题:** 测试VLC预检查
**建议:** 删除此测试文件

#### VideoRenderingIntegrationTest.kt
**路径:** `composeApp/src/desktopTest/kotlin/com/menmapro/iptv/ui/components/VideoRenderingIntegrationTest.kt`
**问题:** 可能测试VLC渲染
**建议:** 检查并更新为FFmpeg测试

#### VideoRenderingRecoveryTest.kt
**路径:** `composeApp/src/desktopTest/kotlin/com/menmapro/iptv/ui/components/VideoRenderingRecoveryTest.kt`
**问题:** 测试VLC恢复
**建议:** 删除此测试文件

### 3. 配置和文档文件

#### PlayerConfigurationTest.kt
**路径:** `composeApp/src/desktopTest/kotlin/com/menmapro/iptv/player/PlayerConfigurationTest.kt`
**问题:** 测试播放器配置选择
**建议:** 简化或删除（不再需要选择逻辑）

## 快速修复方案

### 选项1: 删除所有VLC相关文件（推荐）
```bash
# 删除VLC工具文件
rm composeApp/src/desktopMain/kotlin/com/menmapro/iptv/ui/components/VideoPlaybackPreCheck.kt
rm composeApp/src/desktopMain/kotlin/com/menmapro/iptv/ui/components/VideoRenderingDiagnostics.kt
rm composeApp/src/desktopMain/kotlin/com/menmapro/iptv/ui/components/VideoRenderingRecovery.kt
rm composeApp/src/desktopMain/kotlin/com/menmapro/iptv/ui/components/VideoSurfaceValidator.kt
rm composeApp/src/desktopMain/kotlin/com/menmapro/iptv/ui/components/VlcAvailabilityChecker.kt
rm composeApp/src/desktopMain/kotlin/com/menmapro/iptv/ui/components/VlcConfiguration.kt

# 删除VLC测试文件
rm composeApp/src/desktopTest/kotlin/com/menmapro/iptv/ui/components/VideoPlaybackPreCheckTest.kt
rm composeApp/src/desktopTest/kotlin/com/menmapro/iptv/ui/components/VideoRenderingRecoveryTest.kt
rm composeApp/src/desktopTest/kotlin/com/menmapro/iptv/player/PlayerConfigurationTest.kt
```

### 选项2: 保留文件但注释掉（用于参考）
将所有VLC相关代码注释掉，保留文件结构作为参考。

## 影响分析

### 删除这些文件的影响
- ✅ **编译错误修复** - 移除VLC依赖后的编译错误将被解决
- ✅ **代码简化** - 移除不再需要的VLC特定代码
- ✅ **维护简化** - 减少需要维护的代码量

### FFmpeg已有的替代功能
- ✅ **诊断** - FFmpegPlayerEngine有DiagnosticReportGenerator
- ✅ **错误处理** - FFmpegPlayerEngine有ErrorHandler
- ✅ **性能监控** - FFmpegPlayerEngine有PerformanceMonitor
- ✅ **统计** - FFmpegPlayerEngine有PlaybackStatistics

## 建议执行顺序

1. **立即删除** - 导致编译错误的6个文件
2. **验证编译** - 确保项目可以编译
3. **运行测试** - 确保FFmpeg测试通过
4. **更新文档** - 移除VLC相关文档
5. **清理测试** - 删除或更新VLC相关测试

## 验证清单

- [ ] 删除6个VLC工具文件
- [ ] 删除3个VLC测试文件
- [ ] 验证编译成功
- [ ] 运行FFmpeg测试
- [ ] 更新README
- [ ] 更新MIGRATION_GUIDE

## 注意事项

⚠️ **备份建议:** 在删除前，确保所有文件都在git历史中，以便需要时可以恢复。

⚠️ **测试建议:** 删除后立即运行完整测试套件，确保没有遗漏的依赖。

⚠️ **文档建议:** 更新所有提到这些文件的文档。
