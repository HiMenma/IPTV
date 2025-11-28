package com.menmapro.iptv.player.ffmpeg

import org.bytedeco.javacv.FFmpegFrameGrabber

/**
 * 媒体信息模型
 * 
 * 包含从 FFmpeg 提取的媒体元数据，用于播放器初始化和诊断。
 * 
 * Requirements: 7.1 - 播放信息日志记录
 */
data class MediaInfo(
    /**
     * 媒体总时长（微秒）
     */
    val duration: Long,
    
    /**
     * 视频编解码器名称（如 "h264", "hevc"）
     */
    val videoCodec: String,
    
    /**
     * 音频编解码器名称（如 "aac", "mp3"）
     */
    val audioCodec: String,
    
    /**
     * 视频宽度（像素）
     */
    val videoWidth: Int,
    
    /**
     * 视频高度（像素）
     */
    val videoHeight: Int,
    
    /**
     * 视频帧率（帧/秒）
     */
    val videoFrameRate: Double,
    
    /**
     * 音频比特率（比特/秒）
     */
    val audioBitrate: Int,
    
    /**
     * 视频比特率（比特/秒）
     */
    val videoBitrate: Int,
    
    /**
     * 音频声道数
     */
    val audioChannels: Int,
    
    /**
     * 音频采样率（赫兹）
     */
    val audioSampleRate: Int,
    
    /**
     * 媒体格式名称（如 "mp4", "hls"）
     */
    val format: String = "unknown",
    
    /**
     * 是否包含视频流
     */
    val hasVideo: Boolean = true,
    
    /**
     * 是否包含音频流
     */
    val hasAudio: Boolean = true
) {
    companion object {
        /**
         * 从 FFmpegFrameGrabber 提取媒体信息
         * 
         * @param grabber 已初始化的 FFmpegFrameGrabber 实例
         * @return MediaInfo 实例，如果提取失败则返回默认值
         */
        fun fromGrabber(grabber: FFmpegFrameGrabber): MediaInfo {
            return try {
                MediaInfo(
                    duration = grabber.lengthInTime,
                    videoCodec = grabber.videoCodecName ?: "unknown",
                    audioCodec = grabber.audioCodecName ?: "unknown",
                    videoWidth = grabber.imageWidth,
                    videoHeight = grabber.imageHeight,
                    videoFrameRate = grabber.frameRate,
                    audioBitrate = grabber.audioBitrate,
                    videoBitrate = grabber.videoBitrate,
                    audioChannels = grabber.audioChannels,
                    audioSampleRate = grabber.sampleRate,
                    format = grabber.format ?: "unknown",
                    hasVideo = grabber.imageWidth > 0 && grabber.imageHeight > 0,
                    hasAudio = grabber.audioChannels > 0
                )
            } catch (e: Exception) {
                // 返回默认值，避免因提取失败而中断播放
                MediaInfo(
                    duration = 0,
                    videoCodec = "unknown",
                    audioCodec = "unknown",
                    videoWidth = 0,
                    videoHeight = 0,
                    videoFrameRate = 0.0,
                    audioBitrate = 0,
                    videoBitrate = 0,
                    audioChannels = 0,
                    audioSampleRate = 0,
                    format = "unknown",
                    hasVideo = false,
                    hasAudio = false
                )
            }
        }
        
        /**
         * 创建一个空的 MediaInfo 实例
         * 用于初始化或错误情况
         */
        fun empty(): MediaInfo {
            return MediaInfo(
                duration = 0,
                videoCodec = "unknown",
                audioCodec = "unknown",
                videoWidth = 0,
                videoHeight = 0,
                videoFrameRate = 0.0,
                audioBitrate = 0,
                videoBitrate = 0,
                audioChannels = 0,
                audioSampleRate = 0
            )
        }
    }
    
    /**
     * 获取格式化的时长字符串
     * 
     * @return 格式化的时长（如 "01:23:45"）
     */
    fun getFormattedDuration(): String {
        val seconds = duration / 1_000_000
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        return String.format("%02d:%02d:%02d", hours, minutes, secs)
    }
    
    /**
     * 获取格式化的分辨率字符串
     * 
     * @return 格式化的分辨率（如 "1920x1080"）
     */
    fun getFormattedResolution(): String {
        return "${videoWidth}x${videoHeight}"
    }
    
    /**
     * 获取格式化的比特率字符串
     * 
     * @return 格式化的比特率（如 "5.2 Mbps"）
     */
    fun getFormattedBitrate(): String {
        val totalBitrate = videoBitrate + audioBitrate
        return when {
            totalBitrate >= 1_000_000 -> String.format("%.1f Mbps", totalBitrate / 1_000_000.0)
            totalBitrate >= 1_000 -> String.format("%.1f Kbps", totalBitrate / 1_000.0)
            else -> "$totalBitrate bps"
        }
    }
    
    /**
     * 生成详细的媒体信息字符串
     * 用于日志记录和诊断
     * 
     * @return 格式化的媒体信息
     */
    fun toDetailedString(): String {
        return buildString {
            appendLine("Media Information:")
            appendLine("  Format: $format")
            appendLine("  Duration: ${getFormattedDuration()}")
            if (hasVideo) {
                appendLine("  Video:")
                appendLine("    Codec: $videoCodec")
                appendLine("    Resolution: ${getFormattedResolution()}")
                appendLine("    Frame Rate: ${String.format("%.2f", videoFrameRate)} fps")
                appendLine("    Bitrate: ${String.format("%.1f", videoBitrate / 1_000_000.0)} Mbps")
            }
            if (hasAudio) {
                appendLine("  Audio:")
                appendLine("    Codec: $audioCodec")
                appendLine("    Channels: $audioChannels")
                appendLine("    Sample Rate: $audioSampleRate Hz")
                appendLine("    Bitrate: ${String.format("%.1f", audioBitrate / 1_000.0)} Kbps")
            }
        }
    }
}
