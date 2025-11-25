package com.menmapro.iptv.ui.components

import uk.co.caprica.vlcj.player.base.MediaPlayer
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * VideoRenderingDiagnostics - 视频渲染诊断工具类
 * 
 * 提供视频播放诊断功能，帮助快速识别视频渲染问题的根本原因。
 * 包括编解码器信息记录、渲染统计、黑屏检测和诊断报告生成。
 * 
 * Requirements: 5.1, 5.2, 5.3, 5.4
 */
object VideoRenderingDiagnostics {
    
    /**
     * 记录视频编解码器和格式信息
     * 
     * 在视频播放开始时调用，记录关键的视频信息以便诊断。
     * 
     * @param mediaPlayer VLC媒体播放器实例
     * 
     * Validates: Requirements 5.1
     */
    fun logVideoCodecInfo(mediaPlayer: MediaPlayer) {
        try {
            println("=== Video Codec Information ===")
            
            // Get video track information
            val videoTrack = mediaPlayer.media()?.info()?.videoTracks()?.firstOrNull()
            
            if (videoTrack != null) {
                println("Video Codec: ${videoTrack.codec()}")
                println("Video Resolution: ${videoTrack.width()}x${videoTrack.height()}")
                println("Video Frame Rate: ${videoTrack.frameRate()} fps")
                println("Video Bitrate: ${videoTrack.bitRate()} bps")
                
                // Additional codec details
                val codecDescription = videoTrack.codecDescription()
                if (codecDescription != null && codecDescription.isNotEmpty()) {
                    println("Codec Description: $codecDescription")
                }
            } else {
                println("⚠️ No video track information available")
                
                // Try alternative method to get basic video info
                try {
                    val videoDimension = mediaPlayer.video().videoDimension()
                    if (videoDimension != null) {
                        println("Video Size (alternative): ${videoDimension.width}x${videoDimension.height}")
                    }
                } catch (e: Exception) {
                    println("⚠️ Could not retrieve video dimensions: ${e.message}")
                }
            }
            
            // Get audio track information for completeness
            val audioTrack = mediaPlayer.media()?.info()?.audioTracks()?.firstOrNull()
            if (audioTrack != null) {
                println("Audio Codec: ${audioTrack.codec()}")
                println("Audio Channels: ${audioTrack.channels()}")
                println("Audio Sample Rate: ${audioTrack.rate()} Hz")
            }
            
            println("================================")
            
        } catch (e: Exception) {
            println("⚠️ Error logging video codec info: ${e.message}")
            e.printStackTrace()
        }
    }
    
    /**
     * 记录渲染统计信息
     * 
     * 定期调用以监控视频渲染性能。
     * 
     * @param mediaPlayer VLC媒体播放器实例
     * 
     * Validates: Requirements 5.2
     */
    fun logRenderingStats(mediaPlayer: MediaPlayer) {
        try {
            println("=== Rendering Statistics ===")
            
            // Playback state
            val isPlaying = mediaPlayer.status().isPlaying
            val isSeekable = mediaPlayer.status().isSeekable
            val length = mediaPlayer.status().length()
            val time = mediaPlayer.status().time()
            
            println("Is Playing: $isPlaying")
            println("Is Seekable: $isSeekable")
            println("Current Time: ${formatTime(time)}")
            println("Total Length: ${formatTime(length)}")
            
            // Video statistics
            try {
                // Note: FPS information may not be directly available from VLC API
                // Frame rate is typically available from video track info
                println("FPS: Available from video track info")
            } catch (e: Exception) {
                println("FPS: N/A")
            }
            
            // Audio volume
            val volume = mediaPlayer.audio().volume()
            println("Audio Volume: $volume%")
            
            // Media state
            val state = mediaPlayer.status().state()
            println("Media State: $state")
            
            // Check if video output is active
            val hasVideoOutput = try {
                val dimension = mediaPlayer.video().videoDimension()
                dimension != null && dimension.width > 0 && dimension.height > 0
            } catch (e: Exception) {
                false
            }
            println("Has Video Output: $hasVideoOutput")
            
            println("============================")
            
        } catch (e: Exception) {
            println("⚠️ Error logging rendering stats: ${e.message}")
            e.printStackTrace()
        }
    }
    
    /**
     * 检测黑屏问题
     * 
     * 分析当前播放状态，检测是否存在黑屏问题并提供可能的原因和解决方案。
     * 
     * @param mediaPlayer VLC媒体播放器实例
     * @return 黑屏诊断结果
     * 
     * Validates: Requirements 5.3
     */
    fun detectBlackScreen(mediaPlayer: MediaPlayer): BlackScreenDiagnosis {
        val possibleCauses = mutableListOf<String>()
        val suggestedFixes = mutableListOf<String>()
        
        try {
            val isPlaying = mediaPlayer.status().isPlaying
            val hasAudio = try {
                mediaPlayer.audio().volume() > 0
            } catch (e: Exception) {
                false
            }
            
            val hasVideoOutput = try {
                val dimension = mediaPlayer.video().videoDimension()
                dimension != null && dimension.width > 0 && dimension.height > 0
            } catch (e: Exception) {
                false
            }
            
            // Scenario 1: Playing with audio but no video output
            val isBlackScreen = isPlaying && hasAudio && !hasVideoOutput
            
            if (isBlackScreen) {
                // Analyze possible causes
                
                // Cause 1: Video output module issue
                possibleCauses.add("视频输出模块可能未正确配置")
                suggestedFixes.add("尝试使用备用视频输出模块（OpenGL）")
                
                // Cause 2: Video surface not initialized
                possibleCauses.add("视频表面可能未正确初始化")
                suggestedFixes.add("检查视频表面的可见性和尺寸设置")
                
                // Cause 3: Hardware acceleration issue
                possibleCauses.add("硬件加速可能导致兼容性问题")
                suggestedFixes.add("尝试禁用硬件加速")
                
                // Cause 4: Codec not supported
                possibleCauses.add("视频编解码器可能不受支持")
                suggestedFixes.add("确认VLC支持该视频格式")
                
                // Cause 5: Graphics driver issue
                possibleCauses.add("图形驱动可能存在问题")
                suggestedFixes.add("更新系统图形驱动程序")
            } else if (isPlaying && !hasAudio && !hasVideoOutput) {
                // No audio and no video - different issue
                possibleCauses.add("媒体流可能无法解码")
                possibleCauses.add("URL可能无效或无法访问")
                suggestedFixes.add("检查媒体URL的有效性")
                suggestedFixes.add("确认网络连接正常")
            } else if (!isPlaying) {
                // Not playing at all
                possibleCauses.add("媒体未开始播放")
                suggestedFixes.add("检查播放器状态和错误日志")
            }
            
            return BlackScreenDiagnosis(
                isBlackScreen = isBlackScreen,
                possibleCauses = possibleCauses,
                suggestedFixes = suggestedFixes
            )
            
        } catch (e: Exception) {
            println("⚠️ Error detecting black screen: ${e.message}")
            return BlackScreenDiagnosis(
                isBlackScreen = false,
                possibleCauses = listOf("无法检测黑屏状态: ${e.message}"),
                suggestedFixes = listOf("检查VLC播放器状态")
            )
        }
    }
    
    /**
     * 生成完整的诊断报告
     * 
     * 收集所有可用的诊断信息并生成详细报告。
     * 
     * @param mediaPlayer VLC媒体播放器实例
     * @return 格式化的诊断报告字符串
     * 
     * Validates: Requirements 5.4
     */
    fun generateDiagnosticReport(mediaPlayer: MediaPlayer): String {
        return buildString {
            appendLine("=== Video Rendering Diagnostic Report ===")
            appendLine("Timestamp: ${getCurrentTimestamp()}")
            appendLine()
            
            // System information
            appendLine("System Information:")
            appendLine(VideoOutputConfiguration.getPlatformInfo().prependIndent("  "))
            appendLine()
            
            // VLC version (if available)
            try {
                appendLine("VLC Information:")
                appendLine("  VLC Version: ${getVlcVersion()}")
                appendLine()
            } catch (e: Exception) {
                appendLine("VLC Information: Unable to retrieve")
                appendLine()
            }
            
            // Media information
            appendLine("Media Information:")
            try {
                val media = mediaPlayer.media()
                if (media != null) {
                    val mrl = media.info()?.mrl()
                    if (mrl != null) {
                        appendLine("  URL: $mrl")
                    }
                    
                    // Video track info
                    val videoTrack = media.info()?.videoTracks()?.firstOrNull()
                    if (videoTrack != null) {
                        appendLine("  Video Codec: ${videoTrack.codec()}")
                        appendLine("  Resolution: ${videoTrack.width()}x${videoTrack.height()}")
                        appendLine("  Frame Rate: ${videoTrack.frameRate()} fps")
                        appendLine("  Bitrate: ${videoTrack.bitRate()} bps")
                    } else {
                        appendLine("  Video Track: Not available")
                    }
                    
                    // Audio track info
                    val audioTrack = media.info()?.audioTracks()?.firstOrNull()
                    if (audioTrack != null) {
                        appendLine("  Audio Codec: ${audioTrack.codec()}")
                        appendLine("  Channels: ${audioTrack.channels()}")
                        appendLine("  Sample Rate: ${audioTrack.rate()} Hz")
                    }
                } else {
                    appendLine("  No media loaded")
                }
            } catch (e: Exception) {
                appendLine("  Error retrieving media info: ${e.message}")
            }
            appendLine()
            
            // Playback state
            appendLine("Playback State:")
            try {
                appendLine("  State: ${mediaPlayer.status().state()}")
                appendLine("  Is Playing: ${mediaPlayer.status().isPlaying}")
                appendLine("  Is Seekable: ${mediaPlayer.status().isSeekable}")
                appendLine("  Current Time: ${formatTime(mediaPlayer.status().time())}")
                appendLine("  Total Length: ${formatTime(mediaPlayer.status().length())}")
                appendLine("  Volume: ${mediaPlayer.audio().volume()}%")
            } catch (e: Exception) {
                appendLine("  Error retrieving playback state: ${e.message}")
            }
            appendLine()
            
            // Video output state
            appendLine("Video Output State:")
            try {
                val dimension = mediaPlayer.video().videoDimension()
                if (dimension != null) {
                    appendLine("  Video Dimensions: ${dimension.width}x${dimension.height}")
                    appendLine("  Has Video Output: true")
                } else {
                    appendLine("  Video Dimensions: N/A")
                    appendLine("  Has Video Output: false")
                }
                
                // Note: FPS is available from video track info, not directly from video() API
                appendLine("  Current FPS: See video track info above")
            } catch (e: Exception) {
                appendLine("  Error retrieving video output state: ${e.message}")
            }
            appendLine()
            
            // Black screen detection
            appendLine("Black Screen Detection:")
            val diagnosis = detectBlackScreen(mediaPlayer)
            appendLine("  Is Black Screen: ${diagnosis.isBlackScreen}")
            if (diagnosis.isBlackScreen) {
                appendLine()
                appendLine("  Possible Causes:")
                diagnosis.possibleCauses.forEach { cause ->
                    appendLine("    - $cause")
                }
                appendLine()
                appendLine("  Suggested Fixes:")
                diagnosis.suggestedFixes.forEach { fix ->
                    appendLine("    - $fix")
                }
            } else {
                appendLine("  Status: Video rendering appears normal")
            }
            appendLine()
            
            appendLine("==========================================")
        }
    }
    
    /**
     * 格式化时间（毫秒转为 HH:MM:SS）
     */
    private fun formatTime(timeMs: Long): String {
        if (timeMs < 0) return "N/A"
        
        val totalSeconds = timeMs / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        
        return if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }
    
    /**
     * 获取当前时间戳
     */
    private fun getCurrentTimestamp(): String {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        return LocalDateTime.now().format(formatter)
    }
    
    /**
     * 获取VLC版本信息
     */
    private fun getVlcVersion(): String {
        return try {
            // VLC version is typically available through system properties or native library
            // This is a placeholder - actual implementation may vary
            "VLC 3.x (version detection not implemented)"
        } catch (e: Exception) {
            "Unknown"
        }
    }
}

/**
 * 黑屏诊断结果数据类
 * 
 * @property isBlackScreen 是否检测到黑屏
 * @property possibleCauses 可能的原因列表
 * @property suggestedFixes 建议的解决方案列表
 */
data class BlackScreenDiagnosis(
    val isBlackScreen: Boolean,
    val possibleCauses: List<String>,
    val suggestedFixes: List<String>
)
