package com.menmapro.iptv.player.ffmpeg

import com.menmapro.iptv.ui.components.PlaybackState
import com.menmapro.iptv.ui.components.PlayerState
import org.junit.Test
import java.awt.Canvas
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * FFmpegPlayerEngine 基础测试
 * 
 * 测试播放器引擎的基本功能和状态管理
 */
class FFmpegPlayerEngineTest {
    
    /**
     * 测试播放器引擎创建
     */
    @Test
    fun testEngineCreation() {
        var lastState: PlayerState? = null
        var lastError: String? = null
        
        val engine = FFmpegPlayerEngine(
            onStateChange = { lastState = it },
            onError = { lastError = it }
        )
        
        assertNotNull(engine)
        assertFalse(engine.isInitialized())
        assertFalse(engine.hasActiveThreads())
        assertFalse(engine.hasAllocatedResources())
    }
    
    /**
     * 测试初始状态
     */
    @Test
    fun testInitialState() {
        var lastState: PlayerState? = null
        
        val engine = FFmpegPlayerEngine(
            onStateChange = { lastState = it },
            onError = { }
        )
        
        // 初始状态应该是 IDLE
        assertFalse(engine.isInitialized())
        assertEquals(0L, engine.getSyncDrift())
        assertNotNull(engine.getStatistics())
    }
    
    /**
     * 测试音量设置（未初始化时）
     */
    @Test
    fun testSetVolumeBeforeInit() {
        val engine = FFmpegPlayerEngine(
            onStateChange = { },
            onError = { }
        )
        
        // 设置音量不应该抛出异常，即使未初始化
        engine.setVolume(0.5f)
        engine.setVolume(0.0f)
        engine.setVolume(1.0f)
    }
    
    /**
     * 测试暂停/恢复（未初始化时）
     */
    @Test
    fun testPauseResumeBeforeInit() {
        val engine = FFmpegPlayerEngine(
            onStateChange = { },
            onError = { }
        )
        
        // 这些操作不应该抛出异常，即使未初始化
        engine.pause()
        engine.resume()
    }
    
    /**
     * 测试停止（未初始化时）
     */
    @Test
    fun testStopBeforeInit() {
        val engine = FFmpegPlayerEngine(
            onStateChange = { },
            onError = { }
        )
        
        // 停止操作不应该抛出异常，即使未初始化
        engine.stop()
    }
    
    /**
     * 测试释放（未初始化时）
     */
    @Test
    fun testReleaseBeforeInit() {
        val engine = FFmpegPlayerEngine(
            onStateChange = { },
            onError = { }
        )
        
        // 释放操作不应该抛出异常，即使未初始化
        engine.release()
        
        assertFalse(engine.isInitialized())
        assertFalse(engine.hasActiveThreads())
        assertFalse(engine.hasAllocatedResources())
    }
    
    /**
     * 测试跳转（未初始化时）
     */
    @Test
    fun testSeekBeforeInit() {
        val engine = FFmpegPlayerEngine(
            onStateChange = { },
            onError = { }
        )
        
        // 跳转操作不应该抛出异常，即使未初始化
        engine.seekTo(1000L)
    }
    
    /**
     * 测试诊断报告生成
     */
    @Test
    fun testDiagnosticReport() {
        val engine = FFmpegPlayerEngine(
            onStateChange = { },
            onError = { }
        )
        
        val report = engine.generateDiagnosticReport()
        
        assertNotNull(report)
        assertTrue(report.contains("FFmpeg Player Diagnostic Report"))
        assertTrue(report.contains("System Information"))
        assertTrue(report.contains("Hardware Acceleration"))
        assertTrue(report.contains("Playback Statistics"))
    }
    
    /**
     * 测试获取统计信息
     */
    @Test
    fun testGetStatistics() {
        val engine = FFmpegPlayerEngine(
            onStateChange = { },
            onError = { }
        )
        
        val stats = engine.getStatistics()
        
        assertNotNull(stats)
        assertEquals(0L, stats.framesDecoded)
        assertEquals(0L, stats.framesRendered)
        assertEquals(0L, stats.framesDropped)
    }
    
    /**
     * 测试获取媒体信息（未初始化时）
     */
    @Test
    fun testGetMediaInfoBeforeInit() {
        val engine = FFmpegPlayerEngine(
            onStateChange = { },
            onError = { }
        )
        
        val mediaInfo = engine.getMediaInfo()
        
        // 未初始化时应该返回 null
        assertEquals(null, mediaInfo)
    }
    
    /**
     * 测试获取硬件加速类型（未初始化时）
     */
    @Test
    fun testGetHardwareAccelerationTypeBeforeInit() {
        val engine = FFmpegPlayerEngine(
            onStateChange = { },
            onError = { }
        )
        
        val hwType = engine.getHardwareAccelerationType()
        
        // 未初始化时应该是 NONE
        assertEquals(HardwareAccelerationType.NONE, hwType)
    }
    
    /**
     * 测试多次释放
     */
    @Test
    fun testMultipleRelease() {
        val engine = FFmpegPlayerEngine(
            onStateChange = { },
            onError = { }
        )
        
        // 多次释放不应该抛出异常
        engine.release()
        engine.release()
        engine.release()
        
        assertFalse(engine.isInitialized())
    }
    
    /**
     * 测试全屏模式初始状态
     */
    @Test
    fun testFullscreenInitialState() {
        val engine = FFmpegPlayerEngine(
            onStateChange = { },
            onError = { }
        )
        
        // 初始状态应该不是全屏
        assertFalse(engine.isFullscreen())
    }
    
    /**
     * 测试进入全屏模式（未初始化时）
     */
    @Test
    fun testEnterFullscreenBeforeInit() {
        val engine = FFmpegPlayerEngine(
            onStateChange = { },
            onError = { }
        )
        
        val canvas = Canvas()
        
        // 未初始化时进入全屏不应该抛出异常
        engine.enterFullscreen(canvas)
        
        // 状态不应该改变
        assertFalse(engine.isFullscreen())
    }
    
    /**
     * 测试退出全屏模式（未初始化时）
     */
    @Test
    fun testExitFullscreenBeforeInit() {
        val engine = FFmpegPlayerEngine(
            onStateChange = { },
            onError = { }
        )
        
        val canvas = Canvas()
        
        // 未初始化时退出全屏不应该抛出异常
        engine.exitFullscreen(canvas)
        
        // 状态不应该改变
        assertFalse(engine.isFullscreen())
    }
    
    /**
     * 测试处理尺寸变化（未初始化时）
     */
    @Test
    fun testHandleSizeChangeBeforeInit() {
        val engine = FFmpegPlayerEngine(
            onStateChange = { },
            onError = { }
        )
        
        // 未初始化时处理尺寸变化不应该抛出异常
        engine.handleSizeChange()
    }
    
    /**
     * 测试全屏模式切换（无 Canvas）
     */
    @Test
    fun testFullscreenToggleWithoutCanvas() {
        val engine = FFmpegPlayerEngine(
            onStateChange = { },
            onError = { }
        )
        
        // 测试不提供 Canvas 的情况
        engine.enterFullscreen(null)
        engine.exitFullscreen(null)
        
        // 不应该抛出异常
        assertFalse(engine.isFullscreen())
    }
}
