package com.menmapro.iptv.player.ffmpeg

/**
 * 流协议类型枚举
 */
enum class StreamProtocol {
    HTTP,
    HTTPS,
    RTSP,
    RTMP,
    FILE,
    HLS
}

/**
 * 缓冲配置数据类
 * 
 * @property probeSize FFmpeg 探测大小（字节）
 * @property maxAnalyzeDuration FFmpeg 最大分析时长（微秒）
 * @property bufferSize 缓冲区大小（字节）
 */
data class BufferConfiguration(
    val probeSize: Int,
    val maxAnalyzeDuration: Long,
    val bufferSize: Int
)

/**
 * 流类型检测器
 * 
 * 负责检测媒体流的类型、协议，并生成推荐的缓冲配置。
 * 支持 HTTP/HTTPS/RTSP/HLS/FILE 等协议的识别。
 */
object StreamTypeDetector {
    
    // 直播流关键字
    private val liveStreamKeywords = listOf("live", "stream", "rtmp", "rtsp")
    
    // HLS 文件扩展名
    private val hlsExtensions = listOf(".m3u8", ".m3u")
    
    /**
     * 检测是否为直播流
     * 
     * 通过 URL 特征判断是否为直播流：
     * - 包含 "live" 关键字
     * - HLS 格式 (.m3u8)
     * - RTSP 协议
     * - RTMP 协议
     * 
     * @param url 媒体 URL
     * @return true 如果是直播流，否则 false
     */
    fun isLiveStream(url: String): Boolean {
        val lowerUrl = url.lowercase()
        
        // 检查是否包含直播关键字
        if (liveStreamKeywords.any { lowerUrl.contains(it) }) {
            return true
        }
        
        // 检查是否为 HLS 格式
        if (hlsExtensions.any { lowerUrl.endsWith(it) }) {
            return true
        }
        
        // 检查是否为 RTSP 或 RTMP 协议
        if (lowerUrl.startsWith("rtsp://") || lowerUrl.startsWith("rtmp://")) {
            return true
        }
        
        return false
    }
    
    /**
     * 检测流协议
     * 
     * 根据 URL 前缀识别协议类型。
     * 
     * @param url 媒体 URL
     * @return 识别的协议类型
     */
    fun detectProtocol(url: String): StreamProtocol {
        val lowerUrl = url.lowercase()
        
        return when {
            lowerUrl.startsWith("https://") -> {
                // 检查是否为 HLS
                if (hlsExtensions.any { lowerUrl.contains(it) }) {
                    StreamProtocol.HLS
                } else {
                    StreamProtocol.HTTPS
                }
            }
            lowerUrl.startsWith("http://") -> {
                // 检查是否为 HLS
                if (hlsExtensions.any { lowerUrl.contains(it) }) {
                    StreamProtocol.HLS
                } else {
                    StreamProtocol.HTTP
                }
            }
            lowerUrl.startsWith("rtsp://") -> StreamProtocol.RTSP
            lowerUrl.startsWith("rtmp://") -> StreamProtocol.RTMP
            lowerUrl.startsWith("file://") || lowerUrl.startsWith("/") -> StreamProtocol.FILE
            else -> {
                // 默认尝试作为本地文件
                StreamProtocol.FILE
            }
        }
    }
    
    /**
     * 获取推荐的缓冲配置
     * 
     * 根据流类型和协议生成优化的缓冲配置：
     * - 直播流：小缓冲区，快速探测，低延迟
     * - VOD：大缓冲区，完整探测，高质量
     * - 本地文件：最大缓冲区，完整分析
     * 
     * @param url 媒体 URL
     * @return 推荐的缓冲配置
     */
    fun getBufferConfiguration(url: String): BufferConfiguration {
        val protocol = detectProtocol(url)
        val isLive = isLiveStream(url)
        
        return when {
            // 直播流：低延迟配置
            isLive -> BufferConfiguration(
                probeSize = 128 * 1024,           // 128KB - 增加探测大小以获取完整信息
                maxAnalyzeDuration = 1_000_000L,  // 1秒 - 给予足够时间解析音频流
                bufferSize = 128 * 1024           // 128KB - 适中缓冲区
            )
            
            // RTSP 流：实时流配置
            protocol == StreamProtocol.RTSP -> BufferConfiguration(
                probeSize = 64 * 1024,           // 64KB
                maxAnalyzeDuration = 1_000_000L,  // 1秒
                bufferSize = 128 * 1024           // 128KB
            )
            
            // HLS 流：自适应流配置
            protocol == StreamProtocol.HLS -> BufferConfiguration(
                probeSize = 128 * 1024,          // 128KB
                maxAnalyzeDuration = 2_000_000L,  // 2秒
                bufferSize = 256 * 1024           // 256KB
            )
            
            // 本地文件：完整分析配置
            protocol == StreamProtocol.FILE -> BufferConfiguration(
                probeSize = 10 * 1024 * 1024,    // 10MB - 完整探测
                maxAnalyzeDuration = 10_000_000L, // 10秒 - 完整分析
                bufferSize = 1024 * 1024          // 1MB - 大缓冲区
            )
            
            // HTTP/HTTPS VOD：标准配置
            else -> BufferConfiguration(
                probeSize = 5 * 1024 * 1024,     // 5MB
                maxAnalyzeDuration = 5_000_000L,  // 5秒
                bufferSize = 512 * 1024           // 512KB
            )
        }
    }
    
    /**
     * 获取流类型的描述信息
     * 
     * @param url 媒体 URL
     * @return 流类型的可读描述
     */
    fun getStreamTypeDescription(url: String): String {
        val protocol = detectProtocol(url)
        val isLive = isLiveStream(url)
        
        return buildString {
            append("Protocol: ${protocol.name}")
            if (isLive) {
                append(" (Live Stream)")
            } else {
                append(" (VOD)")
            }
        }
    }
}
