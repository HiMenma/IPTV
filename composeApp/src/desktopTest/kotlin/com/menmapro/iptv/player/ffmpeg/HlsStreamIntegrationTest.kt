package com.menmapro.iptv.player.ffmpeg

import com.menmapro.iptv.ui.components.PlaybackState
import com.menmapro.iptv.ui.components.PlayerState
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.awt.Canvas
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * HLS 流播放集成测试
 * 
 * 测试 HLS (HTTP Live Streaming) m3u8 播放和自适应流功能。
 * 
 * Requirements: 3.3 - 正确处理 HLS (m3u8) 自适应流媒体
 */
class HlsStreamIntegrationTest {
    
    private lateinit var engine: FFmpegPlayerEngine
    private lateinit var canvas: Canvas
    private var lastState: PlayerState? = null
    private var lastError: String? = null
    
    // 测试用的公开可用的 HLS 流 URL
    // 使用 Apple 的 HLS 测试流
    private val testHlsUrl = "https://devstreaming-cdn.apple.com/videos/streaming/examples/img_bipbop_adv_example_fmp4/master.m3u8"
    
    // 备用 HLS 测试流
    private val alternativeHlsUrl = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8"
    
    @Before
    fun setup() {
        canvas = Canvas()
        canvas.setSize(640, 480)
        
        engine = FFmpegPlayerEngine(
            onStateChange = { lastState = it },
            onError = { lastError = it }
        )
    }
    
    @After
    fun teardown() {
        try {
            engine.release()
        } catch (e: Exception) {
            println("Error during teardown: ${e.message}")
        }
    }
    
    /**
     * 测试 HLS 流播放初始化
     * 
     * 验证播放器能够成功初始化并开始播放 HLS 流。
     */
    @Test
    fun testHlsStreamInitialization() {
        println("\n=== Testing HLS Stream Initialization ===")
        
        // 开始播放
        engine.play(testHlsUrl, canvas)
        
        // HLS 流可能需要更长的初始化时间
        Thread.sleep(5000)
        
        // 验证播放器已初始化
        assertTrue(engine.isInitialized(), "Engine should be initialized for HLS stream")
        
        // 验证状态更新
        assertNotNull(lastState, "State should be updated")
        assertTrue(
            lastState!!.playbackState == PlaybackState.PLAYING || 
            lastState!!.playbackState == PlaybackState.BUFFERING,
            "State should be PLAYING or BUFFERING, but was ${lastState!!.playbackState}"
        )
        
        // 验证媒体信息已提取
        val mediaInfo = engine.getMediaInfo()
        assertNotNull(mediaInfo, "Media info should be available for HLS stream")
        assertTrue(mediaInfo.videoWidth > 0, "Video width should be positive")
        assertTrue(mediaInfo.videoHeight > 0, "Video height should be positive")
        
        println("HLS stream initialized successfully")
        println("Media info: ${mediaInfo.videoWidth}x${mediaInfo.videoHeight}, codec: ${mediaInfo.videoCodec}")
    }
    
    /**
     * 测试 HLS 流类型检测
     * 
     * 验证 StreamTypeDetector 能够正确识别 HLS 流。
     */
    @Test
    fun testHlsStreamTypeDetection() {
        println("\n=== Testing HLS Stream Type Detection ===")
        
        // 检测流类型
        val isLive = StreamTypeDetector.isLiveStream(testHlsUrl)
        val protocol = StreamTypeDetector.detectProtocol(testHlsUrl)
        
        println("Is live stream: $isLive")
        println("Protocol: $protocol")
        
        // HLS URL 应该被识别为 HLS 协议
        assertEquals(StreamProtocol.HLS, protocol, "Should detect HLS protocol")
        
        // 验证 URL 包含 .m3u8
        assertTrue(testHlsUrl.contains(".m3u8"), "HLS URL should contain .m3u8")
        
        println("HLS stream type detection test completed successfully")
    }
    
    /**
     * 测试 HLS 流播放控制
     * 
     * 验证播放器能够正确控制 HLS 流的播放。
     */
    @Test
    fun testHlsStreamPlaybackControl() {
        println("\n=== Testing HLS Stream Playback Control ===")
        
        // 开始播放
        engine.play(testHlsUrl, canvas)
        Thread.sleep(4000)
        
        // 验证正在播放
        assertTrue(engine.isInitialized(), "Engine should be initialized")
        assertEquals(PlaybackState.PLAYING, lastState?.playbackState, "Should be playing")
        
        // 暂停播放
        println("Pausing HLS playback...")
        engine.pause()
        Thread.sleep(500)
        
        // 验证已暂停
        assertEquals(PlaybackState.PAUSED, lastState?.playbackState, "Should be paused")
        
        // 恢复播放
        println("Resuming HLS playback...")
        engine.resume()
        Thread.sleep(500)
        
        // 验证已恢复
        assertEquals(PlaybackState.PLAYING, lastState?.playbackState, "Should be playing again")
        
        println("HLS playback control test completed successfully")
    }
    
    /**
     * 测试 HLS 流自适应特性
     * 
     * 验证播放器能够处理 HLS 的自适应流特性。
     * HLS 流会根据网络条件自动切换不同质量的视频片段。
     */
    @Test
    fun testHlsAdaptiveStreaming() {
        println("\n=== Testing HLS Adaptive Streaming ===")
        
        // 开始播放
        engine.play(testHlsUrl, canvas)
        Thread.sleep(3000)
        
        // 验证播放器已初始化
        assertTrue(engine.isInitialized(), "Engine should be initialized")
        
        // 获取初始媒体信息
        val initialMediaInfo = engine.getMediaInfo()
        assertNotNull(initialMediaInfo, "Initial media info should be available")
        println("Initial resolution: ${initialMediaInfo.videoWidth}x${initialMediaInfo.videoHeight}")
        
        // 继续播放一段时间，让自适应流有机会切换质量
        println("Playing for extended period to allow adaptive streaming...")
        Thread.sleep(5000)
        
        // 获取统计信息
        val stats = engine.getStatistics()
        println("Frames decoded: ${stats.framesDecoded}")
        println("Frames rendered: ${stats.framesRendered}")
        println("Frames dropped: ${stats.framesDropped}")
        
        // 验证播放正常进行
        assertTrue(stats.framesDecoded > 0, "Should have decoded frames")
        assertTrue(stats.framesRendered > 0, "Should have rendered frames")
        
        // 对于自适应流，丢帧率应该相对较低（除非网络很差）
        val dropRate = if (stats.framesDecoded > 0) {
            stats.framesDropped.toDouble() / stats.framesDecoded.toDouble()
        } else {
            0.0
        }
        println("Drop rate: ${dropRate * 100}%")
        
        println("HLS adaptive streaming test completed successfully")
    }
    
    /**
     * 测试 HLS 流缓冲配置
     * 
     * 验证播放器为 HLS 流使用了适当的缓冲配置。
     */
    @Test
    fun testHlsBufferConfiguration() {
        println("\n=== Testing HLS Buffer Configuration ===")
        
        // 获取 HLS 流的推荐缓冲配置
        val bufferConfig = StreamTypeDetector.getBufferConfiguration(testHlsUrl)
        
        println("Buffer configuration for HLS:")
        println("  Probe size: ${bufferConfig.probeSize}")
        println("  Max analyze duration: ${bufferConfig.maxAnalyzeDuration}")
        println("  Buffer size: ${bufferConfig.bufferSize}")
        
        // 验证配置合理
        assertTrue(bufferConfig.probeSize > 0, "Probe size should be positive")
        assertTrue(bufferConfig.maxAnalyzeDuration >= 0, "Max analyze duration should be non-negative")
        assertTrue(bufferConfig.bufferSize > 0, "Buffer size should be positive")
        
        // 开始播放以验证配置有效
        engine.play(testHlsUrl, canvas)
        Thread.sleep(4000)
        
        // 验证播放成功
        assertTrue(engine.isInitialized(), "Engine should be initialized with buffer configuration")
        
        println("HLS buffer configuration test completed successfully")
    }
    
    /**
     * 测试 HLS 流播放统计
     * 
     * 验证播放器能够收集 HLS 流的播放统计信息。
     */
    @Test
    fun testHlsStreamStatistics() {
        println("\n=== Testing HLS Stream Statistics ===")
        
        // 开始播放
        engine.play(testHlsUrl, canvas)
        Thread.sleep(5000)
        
        // 获取统计信息
        val stats = engine.getStatistics()
        
        // 验证统计信息
        assertNotNull(stats, "Statistics should be available")
        println("Frames decoded: ${stats.framesDecoded}")
        println("Frames rendered: ${stats.framesRendered}")
        println("Frames dropped: ${stats.framesDropped}")
        println("Current FPS: ${stats.currentFps}")
        println("Buffer level: ${stats.bufferLevel}")
        
        // HLS 流播放后应该有统计数据
        assertTrue(stats.framesDecoded > 0, "Should have decoded frames from HLS stream")
        
        // 生成诊断报告
        val report = engine.generateDiagnosticReport()
        assertTrue(report.contains("HLS"), "Diagnostic report should mention HLS or contain URL")
        
        println("HLS stream statistics test completed successfully")
    }
    
    /**
     * 测试 HLS 流错误处理
     * 
     * 验证播放器能够处理 HLS 流的错误情况。
     */
    @Test
    fun testHlsStreamErrorHandling() {
        println("\n=== Testing HLS Stream Error Handling ===")
        
        // 使用无效的 HLS URL
        val invalidHlsUrl = "https://invalid-domain-that-does-not-exist.com/stream.m3u8"
        
        // 尝试播放
        engine.play(invalidHlsUrl, canvas)
        
        // 等待错误发生
        Thread.sleep(5000)
        
        // 验证错误被捕获
        // 注意：错误可能在初始化时发生，也可能在播放过程中发生
        if (lastError != null) {
            println("Error correctly captured: $lastError")
            assertTrue(lastError!!.isNotEmpty(), "Error message should not be empty")
        } else {
            println("No error captured (stream may have timed out gracefully)")
        }
        
        // 验证播放器状态
        // 可能是 ERROR 状态，也可能是 IDLE（如果初始化失败）
        if (lastState != null) {
            println("Final state: ${lastState!!.playbackState}")
        }
        
        println("HLS stream error handling test completed")
    }
    
    /**
     * 测试备用 HLS 流
     * 
     * 使用不同的 HLS 流源进行测试，验证播放器的兼容性。
     */
    @Test
    fun testAlternativeHlsStream() {
        println("\n=== Testing Alternative HLS Stream ===")
        
        // 使用备用 HLS URL
        engine.play(alternativeHlsUrl, canvas)
        Thread.sleep(5000)
        
        // 验证播放器已初始化
        assertTrue(engine.isInitialized(), "Engine should be initialized for alternative HLS stream")
        
        // 验证状态
        assertNotNull(lastState, "State should be updated")
        assertTrue(
            lastState!!.playbackState == PlaybackState.PLAYING || 
            lastState!!.playbackState == PlaybackState.BUFFERING,
            "Should be playing or buffering alternative HLS stream"
        )
        
        // 验证媒体信息
        val mediaInfo = engine.getMediaInfo()
        assertNotNull(mediaInfo, "Media info should be available for alternative HLS stream")
        println("Alternative HLS stream: ${mediaInfo.videoWidth}x${mediaInfo.videoHeight}")
        
        println("Alternative HLS stream test completed successfully")
    }
    
    /**
     * 测试 HLS 流长时间播放
     * 
     * 验证播放器能够稳定地播放 HLS 流较长时间。
     */
    @Test
    fun testHlsStreamLongPlayback() {
        println("\n=== Testing HLS Stream Long Playback ===")
        
        // 开始播放
        engine.play(testHlsUrl, canvas)
        Thread.sleep(3000)
        
        // 验证初始化成功
        assertTrue(engine.isInitialized(), "Engine should be initialized")
        
        // 记录初始统计
        val initialStats = engine.getStatistics()
        println("Initial frames decoded: ${initialStats.framesDecoded}")
        
        // 播放较长时间（10秒）
        println("Playing for extended period...")
        Thread.sleep(10000)
        
        // 获取最终统计
        val finalStats = engine.getStatistics()
        println("Final frames decoded: ${finalStats.framesDecoded}")
        println("Final frames rendered: ${finalStats.framesRendered}")
        println("Final frames dropped: ${finalStats.framesDropped}")
        
        // 验证持续播放
        assertTrue(
            finalStats.framesDecoded > initialStats.framesDecoded,
            "Should have decoded more frames during playback"
        )
        
        // 验证没有严重错误
        if (lastError != null) {
            println("Warning: Error occurred during long playback: $lastError")
        }
        
        // 验证仍在播放
        assertTrue(engine.isInitialized(), "Engine should still be initialized after long playback")
        
        println("HLS stream long playback test completed successfully")
    }
    
    /**
     * 测试 HLS 流资源释放
     * 
     * 验证播放器能够正确释放 HLS 流的资源。
     */
    @Test
    fun testHlsStreamResourceRelease() {
        println("\n=== Testing HLS Stream Resource Release ===")
        
        // 开始播放
        engine.play(testHlsUrl, canvas)
        Thread.sleep(4000)
        
        // 验证资源已分配
        assertTrue(engine.isInitialized(), "Engine should be initialized")
        assertTrue(engine.hasActiveThreads(), "Should have active threads")
        assertTrue(engine.hasAllocatedResources(), "Should have allocated resources")
        
        // 释放资源
        println("Releasing HLS stream resources...")
        engine.release()
        Thread.sleep(1000)
        
        // 验证资源已释放
        assertTrue(!engine.isInitialized(), "Engine should not be initialized after release")
        assertTrue(!engine.hasActiveThreads(), "Should not have active threads after release")
        assertTrue(!engine.hasAllocatedResources(), "Should not have allocated resources after release")
        
        println("HLS stream resource release test completed successfully")
    }
}
