package com.menmapro.iptv.ui.components

/**
 * VideoFormatDetector - 视频格式检测器
 * 
 * 检测视频URL的格式（通过扩展名或内容类型），并提供格式特定的解码选项。
 * 
 * Validates: Requirements 1.3, 4.3
 */
object VideoFormatDetector {
    
    /**
     * 从URL检测视频格式
     * 
     * 通过分析URL的文件扩展名、协议和路径模式来确定视频格式。
     * 
     * @param url 要分析的媒体URL
     * @return 检测到的视频格式
     */
    fun detectVideoFormat(url: String): VideoFormat {
        val lowerUrl = url.lowercase()
        
        // H.264 detection
        if (lowerUrl.contains(".mp4") || 
            lowerUrl.contains(".m4v") ||
            lowerUrl.contains(".mov") ||
            lowerUrl.contains(".3gp") ||
            lowerUrl.contains(".f4v")) {
            return VideoFormat.H264
        }
        
        // H.265/HEVC detection
        if (lowerUrl.contains(".hevc") ||
            lowerUrl.contains(".h265") ||
            lowerUrl.contains(".265")) {
            return VideoFormat.H265
        }
        
        // MKV can contain various codecs, but often H.265
        if (lowerUrl.contains(".mkv")) {
            // Check for HEVC indicators in URL
            if (lowerUrl.contains("hevc") || lowerUrl.contains("h265") || lowerUrl.contains("x265")) {
                return VideoFormat.H265
            }
            // Default to H.264 for MKV
            return VideoFormat.H264
        }
        
        // VP8 detection
        if (lowerUrl.contains(".webm") && !lowerUrl.contains("vp9")) {
            return VideoFormat.VP8
        }
        
        // VP9 detection
        if (lowerUrl.contains(".webm") && lowerUrl.contains("vp9")) {
            return VideoFormat.VP9
        }
        if (lowerUrl.contains(".vp9")) {
            return VideoFormat.VP9
        }
        
        // AV1 detection
        if (lowerUrl.contains(".av1") ||
            lowerUrl.contains("av01") ||
            (lowerUrl.contains(".webm") && lowerUrl.contains("av1"))) {
            return VideoFormat.AV1
        }
        
        // MPEG-2 detection
        if (lowerUrl.contains(".mpg") ||
            lowerUrl.contains(".mpeg") ||
            lowerUrl.contains(".vob") ||
            lowerUrl.contains(".m2v")) {
            return VideoFormat.MPEG2
        }
        
        // Streaming formats - typically use H.264
        if (lowerUrl.contains(".m3u8") ||  // HLS
            lowerUrl.contains(".ts") ||     // MPEG-TS
            lowerUrl.startsWith("rtsp://") ||
            lowerUrl.startsWith("rtmp://")) {
            return VideoFormat.H264
        }
        
        // MPEG-DASH
        if (lowerUrl.contains(".mpd")) {
            // DASH can use various codecs, default to H.264
            return VideoFormat.H264
        }
        
        // AVI can contain various codecs
        if (lowerUrl.contains(".avi")) {
            return VideoFormat.MPEG4
        }
        
        // WMV/ASF
        if (lowerUrl.contains(".wmv") ||
            lowerUrl.contains(".asf")) {
            return VideoFormat.WMV
        }
        
        // FLV
        if (lowerUrl.contains(".flv")) {
            return VideoFormat.FLV
        }
        
        return VideoFormat.UNKNOWN
    }
    
    /**
     * 检测流媒体格式
     * 
     * @param url 要分析的媒体URL
     * @return 检测到的流媒体格式描述
     */
    fun detectStreamFormat(url: String): String {
        val lowerUrl = url.lowercase()
        
        return when {
            lowerUrl.contains(".m3u8") -> "HLS (HTTP Live Streaming)"
            lowerUrl.startsWith("rtsp://") -> "RTSP (Real Time Streaming Protocol)"
            lowerUrl.startsWith("rtmp://") || lowerUrl.startsWith("rtmps://") -> "RTMP (Real-Time Messaging Protocol)"
            lowerUrl.startsWith("rtp://") -> "RTP (Real-time Transport Protocol)"
            lowerUrl.startsWith("udp://") -> "UDP (User Datagram Protocol)"
            lowerUrl.contains(".mpd") -> "MPEG-DASH"
            lowerUrl.startsWith("mms://") || lowerUrl.startsWith("mmsh://") -> "MMS (Microsoft Media Server)"
            lowerUrl.startsWith("http://") || lowerUrl.startsWith("https://") -> "HTTP Progressive Download"
            lowerUrl.startsWith("file://") || (!lowerUrl.contains("://")) -> "Local File"
            else -> "Unknown"
        }
    }
    
    /**
     * 检测URL是否为直播流
     * 
     * 支持检测各种直播流格式：
     * - HLS (HTTP Live Streaming): .m3u8 文件
     * - RTSP (Real Time Streaming Protocol): rtsp:// URLs
     * - RTMP (Real-Time Messaging Protocol): rtmp://, rtmps:// URLs
     * - 其他流媒体协议: rtp://, udp://, mms://
     * 
     * @param url 要检查的媒体URL
     * @return 如果URL看起来是直播流则返回true，否则返回false
     */
    fun isLiveStreamUrl(url: String): Boolean {
        val lowerUrl = url.lowercase()
        
        // Check for live streaming protocols
        if (lowerUrl.startsWith("rtsp://") ||
            lowerUrl.startsWith("rtmp://") || 
            lowerUrl.startsWith("rtmps://") ||
            lowerUrl.startsWith("rtmpe://") || 
            lowerUrl.startsWith("rtmpt://") ||
            lowerUrl.startsWith("rtp://") ||
            lowerUrl.startsWith("udp://") ||
            lowerUrl.startsWith("mms://") ||
            lowerUrl.startsWith("mmsh://")) {
            return true
        }
        
        // Check for live streaming formats
        if (lowerUrl.contains(".m3u8") || lowerUrl.contains(".mpd")) {
            return true
        }
        
        // Check for common live stream URL patterns
        if (lowerUrl.contains("/live/") ||
            lowerUrl.contains("/stream/") ||
            lowerUrl.contains("/livestream/") ||
            lowerUrl.contains("live.") ||
            lowerUrl.contains("stream.")) {
            return true
        }
        
        return false
    }
    
    /**
     * 获取格式特定的解码选项
     * 
     * 根据视频格式返回优化的VLC解码选项。
     * 
     * Validates: Requirements 4.3
     * 
     * @param format 视频格式
     * @return VLC解码选项数组
     */
    fun getFormatSpecificOptions(format: VideoFormat): Array<String> {
        return when (format) {
            VideoFormat.H264 -> {
                // H.264 optimization options
                arrayOf(
                    ":avcodec-skiploopfilter=0",  // Don't skip loop filter (better quality)
                    ":avcodec-skip-frame=0",       // Don't skip frames
                    ":avcodec-skip-idct=0",        // Don't skip IDCT
                    ":h264-fps=0"                  // Auto-detect frame rate
                )
            }
            
            VideoFormat.H265 -> {
                // H.265/HEVC optimization options
                arrayOf(
                    ":avcodec-skiploopfilter=0",
                    ":avcodec-skip-frame=0",
                    ":avcodec-skip-idct=0",
                    ":avcodec-threads=0"           // Auto-detect thread count for better performance
                )
            }
            
            VideoFormat.VP8 -> {
                // VP8 optimization options
                arrayOf(
                    ":avcodec-threads=0",          // Auto-detect thread count
                    ":avcodec-skip-frame=0"
                )
            }
            
            VideoFormat.VP9 -> {
                // VP9 optimization options
                arrayOf(
                    ":avcodec-threads=0",          // VP9 benefits from multi-threading
                    ":avcodec-skip-frame=0",
                    ":avcodec-skip-loop-filter=0"
                )
            }
            
            VideoFormat.AV1 -> {
                // AV1 optimization options
                arrayOf(
                    ":avcodec-threads=0",          // AV1 is computationally intensive
                    ":avcodec-skip-frame=0"
                )
            }
            
            VideoFormat.MPEG2 -> {
                // MPEG-2 optimization options
                arrayOf(
                    ":avcodec-hurry-up=0",         // Don't hurry decoding
                    ":avcodec-skip-frame=0"
                )
            }
            
            VideoFormat.MPEG4 -> {
                // MPEG-4 optimization options
                arrayOf(
                    ":avcodec-skiploopfilter=0",
                    ":avcodec-skip-frame=0"
                )
            }
            
            VideoFormat.WMV -> {
                // WMV optimization options
                arrayOf(
                    ":avcodec-skip-frame=0"
                )
            }
            
            VideoFormat.FLV -> {
                // FLV optimization options
                arrayOf(
                    ":avcodec-skip-frame=0"
                )
            }
            
            VideoFormat.UNKNOWN -> {
                // Default options for unknown formats
                arrayOf(
                    ":avcodec-skip-frame=0"
                )
            }
        }
    }
    
    /**
     * 获取格式的友好名称
     * 
     * @param format 视频格式
     * @return 格式的可读名称
     */
    fun getFormatName(format: VideoFormat): String {
        return when (format) {
            VideoFormat.H264 -> "H.264/AVC"
            VideoFormat.H265 -> "H.265/HEVC"
            VideoFormat.VP8 -> "VP8"
            VideoFormat.VP9 -> "VP9"
            VideoFormat.AV1 -> "AV1"
            VideoFormat.MPEG2 -> "MPEG-2"
            VideoFormat.MPEG4 -> "MPEG-4"
            VideoFormat.WMV -> "Windows Media Video"
            VideoFormat.FLV -> "Flash Video"
            VideoFormat.UNKNOWN -> "Unknown"
        }
    }
    
    /**
     * 获取格式的详细描述
     * 
     * @param format 视频格式
     * @return 格式的详细描述
     */
    fun getFormatDescription(format: VideoFormat): String {
        return when (format) {
            VideoFormat.H264 -> "H.264/AVC - 广泛使用的视频编码标准，兼容性好"
            VideoFormat.H265 -> "H.265/HEVC - 高效视频编码，比H.264压缩率更高"
            VideoFormat.VP8 -> "VP8 - Google开发的开源视频编码格式"
            VideoFormat.VP9 -> "VP9 - VP8的继任者，压缩效率更高"
            VideoFormat.AV1 -> "AV1 - 新一代开源视频编码格式，压缩效率极高"
            VideoFormat.MPEG2 -> "MPEG-2 - 传统视频编码标准，用于DVD和数字电视"
            VideoFormat.MPEG4 -> "MPEG-4 - 多媒体编码标准"
            VideoFormat.WMV -> "Windows Media Video - 微软的视频编码格式"
            VideoFormat.FLV -> "Flash Video - Adobe Flash使用的视频格式"
            VideoFormat.UNKNOWN -> "未知格式 - 将使用默认解码选项"
        }
    }
}

/**
 * 视频格式枚举
 * 
 * 支持的视频编码格式列表
 */
enum class VideoFormat {
    /** H.264/AVC - 最常用的视频编码格式 */
    H264,
    
    /** H.265/HEVC - 高效视频编码 */
    H265,
    
    /** VP8 - Google的开源视频编码 */
    VP8,
    
    /** VP9 - VP8的继任者 */
    VP9,
    
    /** AV1 - 新一代开源视频编码 */
    AV1,
    
    /** MPEG-2 - 传统视频编码 */
    MPEG2,
    
    /** MPEG-4 - 多媒体编码标准 */
    MPEG4,
    
    /** Windows Media Video */
    WMV,
    
    /** Flash Video */
    FLV,
    
    /** 未知格式 */
    UNKNOWN
}
