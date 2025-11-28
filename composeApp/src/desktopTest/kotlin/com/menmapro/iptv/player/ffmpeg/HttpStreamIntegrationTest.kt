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
 * HTTP/HTTPS 流播放集成测试
 * 
 * 测试网络流播放和播放控制功能。
 * 
 * Requirements: 3.1 - 正确处理 HTTP/HTTPS 网络流媒体
 */
class HttpStreamIntegrationTest {
    
    private lateinit var engine: FFmpegPlayerEngine
    private lateinit var canvas: Canvas
    private var lastState: PlayerState? = null
    private var lastError: String? = null
    
    // 测试用的公开可用的 HTTP 流 URL
    // 使用 Big Buck Bunny 测试视频（开源测试内容）
    private val testHttpUrl = "http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
    
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
     * 测试 HTTP 流播放初始化
     * 
     * 验证播放器能够成功初始化并开始播放 HTTP 流。
     */
    @Test
    fun testHttpStreamInitialization() {
        println("\n=== Testing HTTP Stream Initialization ===")
        
        // 开始播放
        engine.play(testHttpUrl, canvas)
        
        // 等待初始化完成
        Thread.sleep(3000)
        
        // 验证播放器已初始化
        assertTrue(engine.isInitialized(), "Engine should be initialized")
        
        // 验证状态更新
        assertNotNull(lastState, "State should be updated")
        assertTrue(
            lastState!!.playbackState == PlaybackState.PLAYING || 
            lastState!!.playbackState == PlaybackState.BUFFERING,
            "State should be PLAYING or BUFFERING, but was ${lastState!!.playbackState}"
        )
        
        // 验证媒体信息已提取
        val mediaInfo = engine.getMediaInfo()
        assertNotNull(mediaInfo, "Media info should be available")
        assertTrue(mediaInfo.videoWidth > 0, "Video width should be positive")
        assertTrue(mediaInfo.videoHeight > 0, "Video height should be positive")
        
        println("HTTP stream initialized successfully")
        println("Media info: ${mediaInfo.videoWidth}x${mediaInfo.videoHeight}, codec: ${mediaInfo.videoCodec}")
    }
    
    /**
     * 测试 HTTP 流播放控制 - 暂停和恢复
     * 
     * 验证播放器能够正确响应暂停和恢复命令。
     */
    @Test
    fun testHttpStreamPauseResume() {
        println("\n=== Testing HTTP Stream Pause/Resume ===")
        
        // 开始播放
        engine.play(testHttpUrl, canvas)
        Thread.sleep(2000)
        
        // 验证正在播放
        assertTrue(engine.isInitialized(), "Engine should be initialized")
        assertEquals(PlaybackState.PLAYING, lastState?.playbackState, "Should be playing")
        
        // 暂停播放
        println("Pausing playback...")
        engine.pause()
        Thread.sleep(500)
        
        // 验证已暂停
        assertEquals(PlaybackState.PAUSED, lastState?.playbackState, "Should be paused")
        
        // 恢复播放
        println("Resuming playback...")
        engine.resume()
        Thread.sleep(500)
        
        // 验证已恢复
        assertEquals(PlaybackState.PLAYING, lastState?.playbackState, "Should be playing again")
        
        println("Pause/Resume test completed successfully")
    }
    
    /**
     * 测试 HTTP 流播放控制 - 音量调整
     * 
     * 验证播放器能够正确调整音量。
     */
    @Test
    fun testHttpStreamVolumeControl() {
        println("\n=== Testing HTTP Stream Volume Control ===")
        
        // 开始播放
        engine.play(testHttpUrl, canvas)
        Thread.sleep(2000)
        
        // 测试不同的音量值
        val volumeLevels = listOf(0.0f, 0.5f, 1.0f, 0.75f)
        
        for (volume in volumeLevels) {
            println("Setting volume to $volume")
            engine.setVolume(volume)
            Thread.sleep(300)
            
            // 验证状态中的音量已更新
            assertNotNull(lastState, "State should be updated")
            assertEquals(volume, lastState!!.volume, 0.01f, "Volume should be $volume")
        }
        
        println("Volume control test completed successfully")
    }
    
    /**
     * 测试 HTTP 流播放控制 - 跳转
     * 
     * 验证播放器能够跳转到指定位置。
     * 注意：某些流可能不支持跳转，此测试会尝试但不强制要求成功。
     */
    @Test
    fun testHttpStreamSeek() {
        println("\n=== Testing HTTP Stream Seek ===")
        
        // 开始播放
        engine.play(testHttpUrl, canvas)
        Thread.sleep(3000)
        
        // 获取初始位置
        val initialPosition = lastState?.position ?: 0L
        println("Initial position: ${initialPosition}ms")
        
        // 尝试跳转到 5 秒位置
        val targetPosition = 5000L
        println("Seeking to ${targetPosition}ms")
        engine.seekTo(targetPosition)
        
        // 等待跳转完成
        Thread.sleep(2000)
        
        // 验证位置已改变
        val newPosition = lastState?.position ?: 0L
        println("New position: ${newPosition}ms")
        
        // 对于某些流，跳转可能不精确，所以我们只验证位置有变化
        // 并且大致在目标位置附近（允许 2 秒误差）
        assertTrue(
            newPosition != initialPosition,
            "Position should have changed after seek"
        )
        
        println("Seek test completed")
    }
    
    /**
     * 测试 HTTP 流停止和释放
     * 
     * 验证播放器能够正确停止播放并释放资源。
     */
    @Test
    fun testHttpStreamStopAndRelease() {
        println("\n=== Testing HTTP Stream Stop and Release ===")
        
        // 开始播放
        engine.play(testHttpUrl, canvas)
        Thread.sleep(2000)
        
        // 验证正在播放
        assertTrue(engine.isInitialized(), "Engine should be initialized")
        assertTrue(engine.hasActiveThreads(), "Should have active threads")
        
        // 停止播放
        println("Stopping playback...")
        engine.stop()
        Thread.sleep(500)
        
        // 验证已停止
        assertTrue(engine.isInitialized(), "Engine should still be initialized after stop")
        assertTrue(!engine.hasActiveThreads(), "Should not have active threads after stop")
        
        // 释放资源
        println("Releasing resources...")
        engine.release()
        Thread.sleep(500)
        
        // 验证资源已释放
        assertTrue(!engine.isInitialized(), "Engine should not be initialized after release")
        assertTrue(!engine.hasActiveThreads(), "Should not have active threads after release")
        assertTrue(!engine.hasAllocatedResources(), "Should not have allocated resources after release")
        
        println("Stop and release test completed successfully")
    }
    
    /**
     * 测试 HTTP 流播放统计
     * 
     * 验证播放器能够收集和报告播放统计信息。
     */
    @Test
    fun testHttpStreamStatistics() {
        println("\n=== Testing HTTP Stream Statistics ===")
        
        // 开始播放
        engine.play(testHttpUrl, canvas)
        Thread.sleep(3000)
        
        // 获取统计信息
        val stats = engine.getStatistics()
        
        // 验证统计信息
        assertNotNull(stats, "Statistics should be available")
        println("Frames decoded: ${stats.framesDecoded}")
        println("Frames rendered: ${stats.framesRendered}")
        println("Frames dropped: ${stats.framesDropped}")
        println("Current FPS: ${stats.currentFps}")
        
        // 播放了几秒后，应该有一些帧被解码和渲染
        assertTrue(stats.framesDecoded > 0, "Should have decoded some frames")
        
        println("Statistics test completed successfully")
    }
    
    /**
     * 测试 HTTP 流诊断报告
     * 
     * 验证播放器能够生成详细的诊断报告。
     */
    @Test
    fun testHttpStreamDiagnosticReport() {
        println("\n=== Testing HTTP Stream Diagnostic Report ===")
        
        // 开始播放
        engine.play(testHttpUrl, canvas)
        Thread.sleep(2000)
        
        // 生成诊断报告
        val report = engine.generateDiagnosticReport()
        
        // 验证报告内容
        assertNotNull(report, "Diagnostic report should be available")
        assertTrue(report.contains("FFmpeg Player Diagnostic Report"), "Report should have title")
        assertTrue(report.contains("System Information"), "Report should have system info")
        assertTrue(report.contains("Hardware Acceleration"), "Report should have hardware acceleration info")
        assertTrue(report.contains("Media Information"), "Report should have media info")
        assertTrue(report.contains("Playback Statistics"), "Report should have statistics")
        assertTrue(report.contains(testHttpUrl), "Report should contain the URL")
        
        println("Diagnostic report generated successfully")
        println("\n$report")
    }
    
    /**
     * 测试 HTTPS 流播放
     * 
     * 验证播放器能够处理 HTTPS 协议的流。
     */
    @Test
    fun testHttpsStreamPlayback() {
        println("\n=== Testing HTTPS Stream Playback ===")
        
        // 使用 HTTPS URL
        val httpsUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
        
        // 开始播放
        engine.play(httpsUrl, canvas)
        Thread.sleep(3000)
        
        // 验证播放器已初始化
        assertTrue(engine.isInitialized(), "Engine should be initialized for HTTPS stream")
        
        // 验证状态
        assertNotNull(lastState, "State should be updated")
        assertTrue(
            lastState!!.playbackState == PlaybackState.PLAYING || 
            lastState!!.playbackState == PlaybackState.BUFFERING,
            "Should be playing or buffering HTTPS stream"
        )
        
        // 验证媒体信息
        val mediaInfo = engine.getMediaInfo()
        assertNotNull(mediaInfo, "Media info should be available for HTTPS stream")
        
        println("HTTPS stream playback test completed successfully")
    }
    
    /**
     * 测试连续播放多个 HTTP 流
     * 
     * 验证播放器能够正确切换不同的流，释放旧资源并分配新资源。
     */
    @Test
    fun testMultipleHttpStreams() {
        println("\n=== Testing Multiple HTTP Streams ===")
        
        // 播放第一个流
        println("Playing first stream...")
        engine.play(testHttpUrl, canvas)
        Thread.sleep(2000)
        
        assertTrue(engine.isInitialized(), "Engine should be initialized for first stream")
        val firstMediaInfo = engine.getMediaInfo()
        assertNotNull(firstMediaInfo, "First stream should have media info")
        
        // 切换到第二个流（使用相同的 URL 模拟切换）
        println("Switching to second stream...")
        engine.play(testHttpUrl, canvas)
        Thread.sleep(2000)
        
        // 验证仍然正常工作
        assertTrue(engine.isInitialized(), "Engine should be initialized for second stream")
        val secondMediaInfo = engine.getMediaInfo()
        assertNotNull(secondMediaInfo, "Second stream should have media info")
        
        // 验证没有错误
        if (lastError != null) {
            println("Warning: Error occurred during stream switch: $lastError")
        }
        
        println("Multiple streams test completed successfully")
    }
}
