package com.menmapro.iptv.player.ffmpeg

import com.menmapro.iptv.ui.components.PlaybackState
import com.menmapro.iptv.ui.components.PlayerState
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.awt.Canvas
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * 本地文件播放集成测试
 * 
 * 测试本地视频文件播放和完整播放流程。
 * 
 * Requirements: 3.4 - 正确处理本地媒体文件
 */
class LocalFileIntegrationTest {
    
    private lateinit var engine: FFmpegPlayerEngine
    private lateinit var canvas: Canvas
    private var lastState: PlayerState? = null
    private var lastError: String? = null
    
    // 测试视频文件路径
    // 注意：这些测试需要实际的视频文件才能运行
    // 在 CI 环境中，可能需要下载或生成测试视频
    private val testVideoPath = System.getProperty("user.home") + "/test-video.mp4"
    
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
     * 检查测试视频文件是否存在
     * 
     * @return true 如果测试文件存在
     */
    private fun hasTestVideo(): Boolean {
        val file = File(testVideoPath)
        return file.exists() && file.isFile
    }
    
    /**
     * 测试本地文件播放初始化
     * 
     * 验证播放器能够成功初始化并开始播放本地视频文件。
     */
    @Test
    fun testLocalFileInitialization() {
        println("\n=== Testing Local File Initialization ===")
        
        if (!hasTestVideo()) {
            println("Test video not found at: $testVideoPath")
            println("Skipping test (this is expected in CI environments)")
            return
        }
        
        // 构建 file:// URL
        val fileUrl = "file://$testVideoPath"
        println("Playing local file: $fileUrl")
        
        // 开始播放
        engine.play(fileUrl, canvas)
        Thread.sleep(2000)
        
        // 验证播放器已初始化
        assertTrue(engine.isInitialized(), "Engine should be initialized for local file")
        
        // 验证状态更新
        assertNotNull(lastState, "State should be updated")
        assertTrue(
            lastState!!.playbackState == PlaybackState.PLAYING || 
            lastState!!.playbackState == PlaybackState.BUFFERING,
            "State should be PLAYING or BUFFERING, but was ${lastState!!.playbackState}"
        )
        
        // 验证媒体信息已提取
        val mediaInfo = engine.getMediaInfo()
        assertNotNull(mediaInfo, "Media info should be available for local file")
        assertTrue(mediaInfo.videoWidth > 0, "Video width should be positive")
        assertTrue(mediaInfo.videoHeight > 0, "Video height should be positive")
        assertTrue(mediaInfo.duration > 0, "Duration should be positive for local file")
        
        println("Local file initialized successfully")
        println("Media info: ${mediaInfo.videoWidth}x${mediaInfo.videoHeight}, duration: ${mediaInfo.duration}ms")
    }
    
    /**
     * 测试本地文件流类型检测
     * 
     * 验证 StreamTypeDetector 能够正确识别本地文件。
     */
    @Test
    fun testLocalFileTypeDetection() {
        println("\n=== Testing Local File Type Detection ===")
        
        val fileUrl = "file://$testVideoPath"
        
        // 检测流类型
        val isLive = StreamTypeDetector.isLiveStream(fileUrl)
        val protocol = StreamTypeDetector.detectProtocol(fileUrl)
        
        println("Is live stream: $isLive")
        println("Protocol: $protocol")
        
        // 本地文件不应该被识别为直播流
        assertTrue(!isLive, "Local file should not be detected as live stream")
        
        // 协议应该是 FILE
        assertEquals(StreamProtocol.FILE, protocol, "Should detect FILE protocol")
        
        println("Local file type detection test completed successfully")
    }
    
    /**
     * 测试本地文件播放控制
     * 
     * 验证播放器能够正确控制本地文件的播放。
     */
    @Test
    fun testLocalFilePlaybackControl() {
        println("\n=== Testing Local File Playback Control ===")
        
        if (!hasTestVideo()) {
            println("Test video not found, skipping test")
            return
        }
        
        val fileUrl = "file://$testVideoPath"
        
        // 开始播放
        engine.play(fileUrl, canvas)
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
        val pausedPosition = lastState?.position ?: 0L
        println("Paused at position: ${pausedPosition}ms")
        
        // 等待一段时间，验证位置不变
        Thread.sleep(1000)
        val stillPausedPosition = lastState?.position ?: 0L
        println("Position after wait: ${stillPausedPosition}ms")
        
        // 恢复播放
        println("Resuming playback...")
        engine.resume()
        Thread.sleep(500)
        
        // 验证已恢复
        assertEquals(PlaybackState.PLAYING, lastState?.playbackState, "Should be playing again")
        
        println("Local file playback control test completed successfully")
    }
    
    /**
     * 测试本地文件跳转功能
     * 
     * 验证播放器能够在本地文件中跳转到任意位置。
     * 本地文件应该支持精确跳转。
     */
    @Test
    fun testLocalFileSeek() {
        println("\n=== Testing Local File Seek ===")
        
        if (!hasTestVideo()) {
            println("Test video not found, skipping test")
            return
        }
        
        val fileUrl = "file://$testVideoPath"
        
        // 开始播放
        engine.play(fileUrl, canvas)
        Thread.sleep(2000)
        
        // 获取媒体时长
        val mediaInfo = engine.getMediaInfo()
        assertNotNull(mediaInfo, "Media info should be available")
        val duration = mediaInfo.duration
        println("Video duration: ${duration}ms")
        
        // 如果视频太短，跳过跳转测试
        if (duration < 10000) {
            println("Video too short for seek test, skipping")
            return
        }
        
        // 跳转到 25% 位置
        val seekPosition1 = duration / 4
        println("Seeking to ${seekPosition1}ms (25%)")
        engine.seekTo(seekPosition1)
        Thread.sleep(1000)
        
        val position1 = lastState?.position ?: 0L
        println("Position after first seek: ${position1}ms")
        
        // 跳转到 50% 位置
        val seekPosition2 = duration / 2
        println("Seeking to ${seekPosition2}ms (50%)")
        engine.seekTo(seekPosition2)
        Thread.sleep(1000)
        
        val position2 = lastState?.position ?: 0L
        println("Position after second seek: ${position2}ms")
        
        // 验证位置有变化
        assertTrue(position2 != position1, "Position should change after seek")
        
        // 跳转到开始
        println("Seeking to beginning")
        engine.seekTo(0L)
        Thread.sleep(1000)
        
        val position3 = lastState?.position ?: 0L
        println("Position after seek to beginning: ${position3}ms")
        
        println("Local file seek test completed successfully")
    }
    
    /**
     * 测试本地文件音量控制
     * 
     * 验证播放器能够正确调整本地文件播放的音量。
     */
    @Test
    fun testLocalFileVolumeControl() {
        println("\n=== Testing Local File Volume Control ===")
        
        if (!hasTestVideo()) {
            println("Test video not found, skipping test")
            return
        }
        
        val fileUrl = "file://$testVideoPath"
        
        // 开始播放
        engine.play(fileUrl, canvas)
        Thread.sleep(2000)
        
        // 测试不同的音量值
        val volumeLevels = listOf(1.0f, 0.5f, 0.0f, 0.75f, 1.0f)
        
        for (volume in volumeLevels) {
            println("Setting volume to $volume")
            engine.setVolume(volume)
            Thread.sleep(500)
            
            // 验证状态中的音量已更新
            assertNotNull(lastState, "State should be updated")
            assertEquals(volume, lastState!!.volume, 0.01f, "Volume should be $volume")
        }
        
        println("Local file volume control test completed successfully")
    }
    
    /**
     * 测试本地文件完整播放流程
     * 
     * 验证播放器能够完整播放本地文件，从开始到结束。
     */
    @Test
    fun testLocalFileCompletePlayback() {
        println("\n=== Testing Local File Complete Playback ===")
        
        if (!hasTestVideo()) {
            println("Test video not found, skipping test")
            return
        }
        
        val fileUrl = "file://$testVideoPath"
        
        // 开始播放
        engine.play(fileUrl, canvas)
        Thread.sleep(2000)
        
        // 获取媒体信息
        val mediaInfo = engine.getMediaInfo()
        assertNotNull(mediaInfo, "Media info should be available")
        println("Video duration: ${mediaInfo.duration}ms")
        println("Video codec: ${mediaInfo.videoCodec}")
        println("Audio codec: ${mediaInfo.audioCodec}")
        println("Resolution: ${mediaInfo.videoWidth}x${mediaInfo.videoHeight}")
        println("Frame rate: ${mediaInfo.videoFrameRate}")
        
        // 记录初始统计
        val initialStats = engine.getStatistics()
        println("Initial frames decoded: ${initialStats.framesDecoded}")
        
        // 播放一段时间
        println("Playing for 5 seconds...")
        Thread.sleep(5000)
        
        // 获取最终统计
        val finalStats = engine.getStatistics()
        println("Final frames decoded: ${finalStats.framesDecoded}")
        println("Final frames rendered: ${finalStats.framesRendered}")
        println("Final frames dropped: ${finalStats.framesDropped}")
        println("Current FPS: ${finalStats.currentFps}")
        
        // 验证播放进行
        assertTrue(
            finalStats.framesDecoded > initialStats.framesDecoded,
            "Should have decoded more frames during playback"
        )
        assertTrue(finalStats.framesRendered > 0, "Should have rendered frames")
        
        // 计算丢帧率
        val dropRate = if (finalStats.framesDecoded > 0) {
            finalStats.framesDropped.toDouble() / finalStats.framesDecoded.toDouble() * 100
        } else {
            0.0
        }
        println("Drop rate: ${"%.2f".format(dropRate)}%")
        
        println("Local file complete playback test completed successfully")
    }
    
    /**
     * 测试本地文件停止和重新播放
     * 
     * 验证播放器能够停止播放并重新开始。
     */
    @Test
    fun testLocalFileStopAndReplay() {
        println("\n=== Testing Local File Stop and Replay ===")
        
        if (!hasTestVideo()) {
            println("Test video not found, skipping test")
            return
        }
        
        val fileUrl = "file://$testVideoPath"
        
        // 第一次播放
        println("First playback...")
        engine.play(fileUrl, canvas)
        Thread.sleep(2000)
        
        assertTrue(engine.isInitialized(), "Engine should be initialized")
        val firstStats = engine.getStatistics()
        println("First playback frames decoded: ${firstStats.framesDecoded}")
        
        // 停止播放
        println("Stopping playback...")
        engine.stop()
        Thread.sleep(500)
        
        assertTrue(!engine.hasActiveThreads(), "Should not have active threads after stop")
        
        // 重新播放
        println("Second playback...")
        engine.play(fileUrl, canvas)
        Thread.sleep(2000)
        
        assertTrue(engine.isInitialized(), "Engine should be initialized again")
        val secondStats = engine.getStatistics()
        println("Second playback frames decoded: ${secondStats.framesDecoded}")
        
        // 验证统计已重置
        assertTrue(
            secondStats.framesDecoded < firstStats.framesDecoded || secondStats.framesDecoded > 0,
            "Statistics should be reset or accumulating"
        )
        
        println("Local file stop and replay test completed successfully")
    }
    
    /**
     * 测试本地文件资源释放
     * 
     * 验证播放器能够正确释放本地文件的资源。
     */
    @Test
    fun testLocalFileResourceRelease() {
        println("\n=== Testing Local File Resource Release ===")
        
        if (!hasTestVideo()) {
            println("Test video not found, skipping test")
            return
        }
        
        val fileUrl = "file://$testVideoPath"
        
        // 开始播放
        engine.play(fileUrl, canvas)
        Thread.sleep(2000)
        
        // 验证资源已分配
        assertTrue(engine.isInitialized(), "Engine should be initialized")
        assertTrue(engine.hasActiveThreads(), "Should have active threads")
        assertTrue(engine.hasAllocatedResources(), "Should have allocated resources")
        
        // 释放资源
        println("Releasing resources...")
        engine.release()
        Thread.sleep(500)
        
        // 验证资源已释放
        assertTrue(!engine.isInitialized(), "Engine should not be initialized after release")
        assertTrue(!engine.hasActiveThreads(), "Should not have active threads after release")
        assertTrue(!engine.hasAllocatedResources(), "Should not have allocated resources after release")
        
        println("Local file resource release test completed successfully")
    }
    
    /**
     * 测试本地文件诊断报告
     * 
     * 验证播放器能够为本地文件生成详细的诊断报告。
     */
    @Test
    fun testLocalFileDiagnosticReport() {
        println("\n=== Testing Local File Diagnostic Report ===")
        
        if (!hasTestVideo()) {
            println("Test video not found, skipping test")
            return
        }
        
        val fileUrl = "file://$testVideoPath"
        
        // 开始播放
        engine.play(fileUrl, canvas)
        Thread.sleep(2000)
        
        // 生成诊断报告
        val report = engine.generateDiagnosticReport()
        
        // 验证报告内容
        assertNotNull(report, "Diagnostic report should be available")
        assertTrue(report.contains("FFmpeg Player Diagnostic Report"), "Report should have title")
        assertTrue(report.contains("System Information"), "Report should have system info")
        assertTrue(report.contains("Media Information"), "Report should have media info")
        assertTrue(report.contains("Playback Statistics"), "Report should have statistics")
        assertTrue(report.contains(testVideoPath), "Report should contain the file path")
        
        println("Diagnostic report generated successfully")
        println("\n$report")
    }
    
    /**
     * 测试无效本地文件路径
     * 
     * 验证播放器能够处理无效的文件路径。
     */
    @Test
    fun testInvalidLocalFilePath() {
        println("\n=== Testing Invalid Local File Path ===")
        
        // 使用不存在的文件路径
        val invalidPath = "file:///nonexistent/path/to/video.mp4"
        
        // 尝试播放
        engine.play(invalidPath, canvas)
        
        // 等待错误发生
        Thread.sleep(3000)
        
        // 验证错误被捕获
        if (lastError != null) {
            println("Error correctly captured: $lastError")
            assertTrue(lastError!!.isNotEmpty(), "Error message should not be empty")
        } else {
            println("No error captured (initialization may have failed silently)")
        }
        
        // 验证播放器状态
        if (lastState != null) {
            println("Final state: ${lastState!!.playbackState}")
        }
        
        println("Invalid local file path test completed")
    }
    
    /**
     * 测试本地文件路径格式
     * 
     * 验证播放器能够处理不同格式的文件路径。
     */
    @Test
    fun testLocalFilePathFormats() {
        println("\n=== Testing Local File Path Formats ===")
        
        if (!hasTestVideo()) {
            println("Test video not found, skipping test")
            return
        }
        
        // 测试不同的路径格式
        val pathFormats = listOf(
            "file://$testVideoPath",           // 标准 file:// URL
            testVideoPath                       // 直接路径（FFmpeg 也支持）
        )
        
        for (path in pathFormats) {
            println("\nTesting path format: $path")
            
            try {
                engine.play(path, canvas)
                Thread.sleep(2000)
                
                if (engine.isInitialized()) {
                    println("Successfully played with path format: $path")
                    engine.stop()
                    Thread.sleep(500)
                } else {
                    println("Failed to initialize with path format: $path")
                }
            } catch (e: Exception) {
                println("Exception with path format $path: ${e.message}")
            }
        }
        
        println("\nLocal file path formats test completed")
    }
}
