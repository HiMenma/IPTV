package com.menmapro.iptv.player.ffmpeg

import org.bytedeco.javacv.FFmpegFrameGrabber

/**
 * FFmpegFrameGrabber 配置器
 * 
 * 负责根据流类型、硬件加速能力等因素配置 FFmpegFrameGrabber。
 * 提供针对不同场景（直播流、VOD、本地文件）的优化配置。
 * 
 * Requirements: 3.1, 3.2, 3.3, 3.4 - 多种媒体格式和协议支持
 * Requirements: 4.1 - 硬件加速支持
 * Requirements: 5.1 - 直播流优化
 */
object FFmpegGrabberConfigurator {
    
    /**
     * 配置 FFmpegFrameGrabber
     * 
     * 这是主要的配置方法，会：
     * 1. 检测流类型和协议
     * 2. 应用相应的缓冲配置
     * 3. 配置硬件加速
     * 4. 设置直播流优化选项
     * 
     * @param grabber FFmpegFrameGrabber 实例
     * @param url 媒体 URL
     * @param enableHardwareAcceleration 是否启用硬件加速（默认 true）
     * @return 使用的硬件加速类型
     * 
     * Requirements: 3.1, 3.2, 3.3, 3.4, 4.1, 5.1
     */
    fun configure(
        grabber: FFmpegFrameGrabber,
        url: String,
        enableHardwareAcceleration: Boolean = true
    ): HardwareAccelerationType {
        println("[FFmpegGrabberConfigurator] Configuring grabber for URL: $url")
        
        // 1. 检测流类型
        val isLive = StreamTypeDetector.isLiveStream(url)
        val protocol = StreamTypeDetector.detectProtocol(url)
        val streamType = StreamTypeDetector.getStreamTypeDescription(url)
        
        println("[FFmpegGrabberConfigurator] Stream type: $streamType")
        
        // 2. 应用基础配置
        applyBasicConfiguration(grabber)
        
        // 3. 应用缓冲配置
        val bufferConfig = StreamTypeDetector.getBufferConfiguration(url)
        applyBufferConfiguration(grabber, bufferConfig)
        
        // 4. 应用流类型特定配置
        if (isLive) {
            applyLiveStreamOptimizations(grabber, protocol)
        } else {
            applyVODOptimizations(grabber, protocol)
        }
        
        // 5. 配置硬件加速
        var hwType = HardwareAccelerationType.NONE
        if (enableHardwareAcceleration) {
            hwType = HardwareAccelerationManager.configureWithFallback(grabber)
            println("[FFmpegGrabberConfigurator] Hardware acceleration: $hwType")
        } else {
            println("[FFmpegGrabberConfigurator] Hardware acceleration disabled")
        }
        
        // 6. 应用协议特定配置
        applyProtocolConfiguration(grabber, protocol)
        
        println("[FFmpegGrabberConfigurator] Configuration complete")
        return hwType
    }
    
    /**
     * 应用基础配置
     * 
     * 设置所有流类型通用的基础选项。
     * 
     * @param grabber FFmpegFrameGrabber 实例
     */
    private fun applyBasicConfiguration(grabber: FFmpegFrameGrabber) {
        try {
            // 不设置 format，让 FFmpeg 自动检测
            // grabber.format = null 或不设置都可以
            
            // 启用多线程解码
            grabber.setOption("threads", "auto")
            
            // 设置超时（10秒）
            grabber.setOption("timeout", "10000000") // 微秒
            
            // 启用快速查找
            grabber.setOption("fflags", "+fastseek")
            
            // 设置音频参数（如果未指定，使用默认值）
            // 这有助于处理音频信息不明确的流
            grabber.audioChannels = 2      // 立体声
            grabber.sampleRate = 48000     // 48kHz 采样率
            
            // 设置图像模式为 COLOR（RGB），避免 GRAY 模式
            grabber.imageMode = org.bytedeco.javacv.FrameGrabber.ImageMode.COLOR
            
            println("[FFmpegGrabberConfigurator] Basic configuration applied")
        } catch (e: Exception) {
            println("[FFmpegGrabberConfigurator] Error applying basic configuration: ${e.message}")
        }
    }
    
    /**
     * 应用缓冲配置
     * 
     * 根据流类型设置探测大小、分析时长和缓冲区大小。
     * 
     * @param grabber FFmpegFrameGrabber 实例
     * @param config 缓冲配置
     * 
     * Requirements: 5.1 - 直播流低延迟缓冲策略
     */
    private fun applyBufferConfiguration(
        grabber: FFmpegFrameGrabber,
        config: BufferConfiguration
    ) {
        try {
            // 设置探测大小
            grabber.setOption("probesize", config.probeSize.toString())
            
            // 设置分析时长
            grabber.setOption("analyzeduration", config.maxAnalyzeDuration.toString())
            
            // 设置缓冲区大小
            grabber.setOption("buffer_size", config.bufferSize.toString())
            
            println("[FFmpegGrabberConfigurator] Buffer configuration applied: " +
                    "probesize=${config.probeSize}, " +
                    "analyzeduration=${config.maxAnalyzeDuration}, " +
                    "buffer_size=${config.bufferSize}")
        } catch (e: Exception) {
            println("[FFmpegGrabberConfigurator] Error applying buffer configuration: ${e.message}")
        }
    }
    
    /**
     * 应用直播流优化
     * 
     * 为直播流设置低延迟选项：
     * - 禁用缓冲
     * - 启用低延迟标志
     * - 使用外部时钟同步
     * - 减少初始延迟
     * 
     * @param grabber FFmpegFrameGrabber 实例
     * @param protocol 流协议
     * 
     * Requirements: 5.1 - 直播流低延迟缓冲策略
     */
    private fun applyLiveStreamOptimizations(
        grabber: FFmpegFrameGrabber,
        protocol: StreamProtocol
    ) {
        try {
            println("[FFmpegGrabberConfigurator] Applying live stream optimizations")
            
            // 禁用缓冲以减少延迟
            grabber.setOption("fflags", "nobuffer")
            
            // 启用低延迟标志
            grabber.setOption("flags", "low_delay")
            
            // 使用外部时钟同步
            grabber.setOption("sync", "ext")
            
            // 减少初始延迟
            grabber.setOption("max_delay", "0")
            
            // 禁用预读
            grabber.setOption("avioflags", "direct")
            
            // 协议特定优化
            when (protocol) {
                StreamProtocol.RTSP -> {
                    // RTSP 特定优化
                    grabber.setOption("rtsp_transport", "tcp") // 使用 TCP 传输
                    grabber.setOption("rtsp_flags", "prefer_tcp")
                    println("[FFmpegGrabberConfigurator] RTSP optimizations applied")
                }
                StreamProtocol.HLS -> {
                    // HLS 特定优化
                    grabber.setOption("live_start_index", "-1") // 从最新片段开始
                    println("[FFmpegGrabberConfigurator] HLS live optimizations applied")
                }
                else -> {
                    // 其他协议的通用优化
                }
            }
            
        } catch (e: Exception) {
            println("[FFmpegGrabberConfigurator] Error applying live stream optimizations: ${e.message}")
        }
    }
    
    /**
     * 应用 VOD 优化
     * 
     * 为点播内容设置优化选项：
     * - 启用完整探测和分析
     * - 启用精确查找
     * - 优化缓冲策略
     * 
     * @param grabber FFmpegFrameGrabber 实例
     * @param protocol 流协议
     */
    private fun applyVODOptimizations(
        grabber: FFmpegFrameGrabber,
        protocol: StreamProtocol
    ) {
        try {
            println("[FFmpegGrabberConfigurator] Applying VOD optimizations")
            
            // 启用精确查找
            grabber.setOption("fflags", "+genpts+igndts")
            
            // 启用快速查找
            grabber.setOption("flags", "fast")
            
            // 协议特定优化
            when (protocol) {
                StreamProtocol.FILE -> {
                    // 本地文件优化
                    grabber.setOption("fflags", "+fastseek+genpts")
                    println("[FFmpegGrabberConfigurator] Local file optimizations applied")
                }
                StreamProtocol.HTTP, StreamProtocol.HTTPS -> {
                    // HTTP 流优化
                    grabber.setOption("reconnect", "1")
                    grabber.setOption("reconnect_streamed", "1")
                    grabber.setOption("reconnect_delay_max", "5")
                    println("[FFmpegGrabberConfigurator] HTTP VOD optimizations applied")
                }
                StreamProtocol.HLS -> {
                    // HLS VOD 优化
                    grabber.setOption("allowed_extensions", "ALL")
                    println("[FFmpegGrabberConfigurator] HLS VOD optimizations applied")
                }
                else -> {
                    // 其他协议的通用优化
                }
            }
            
        } catch (e: Exception) {
            println("[FFmpegGrabberConfigurator] Error applying VOD optimizations: ${e.message}")
        }
    }
    
    /**
     * 应用协议特定配置
     * 
     * 为不同协议设置特定的选项。
     * 
     * @param grabber FFmpegFrameGrabber 实例
     * @param protocol 流协议
     * 
     * Requirements: 3.1, 3.2, 3.3, 3.4 - 支持多种协议
     */
    private fun applyProtocolConfiguration(
        grabber: FFmpegFrameGrabber,
        protocol: StreamProtocol
    ) {
        try {
            when (protocol) {
                StreamProtocol.HTTP, StreamProtocol.HTTPS -> {
                    configureHTTP(grabber)
                }
                StreamProtocol.RTSP -> {
                    configureRTSP(grabber)
                }
                StreamProtocol.RTMP -> {
                    configureRTMP(grabber)
                }
                StreamProtocol.HLS -> {
                    configureHLS(grabber)
                }
                StreamProtocol.FILE -> {
                    configureFile(grabber)
                }
            }
        } catch (e: Exception) {
            println("[FFmpegGrabberConfigurator] Error applying protocol configuration: ${e.message}")
        }
    }
    
    /**
     * 配置 HTTP/HTTPS 协议
     * 
     * @param grabber FFmpegFrameGrabber 实例
     * 
     * Requirements: 3.1 - HTTP/HTTPS 流支持
     */
    private fun configureHTTP(grabber: FFmpegFrameGrabber) {
        try {
            // 设置 User-Agent
            grabber.setOption("user_agent", "FFmpeg IPTV Player")
            
            // 启用自动重连
            grabber.setOption("reconnect", "1")
            grabber.setOption("reconnect_streamed", "1")
            grabber.setOption("reconnect_delay_max", "5")
            
            // 设置 HTTP 超时
            grabber.setOption("http_persistent", "1")
            
            println("[FFmpegGrabberConfigurator] HTTP/HTTPS configuration applied")
        } catch (e: Exception) {
            println("[FFmpegGrabberConfigurator] Error configuring HTTP: ${e.message}")
        }
    }
    
    /**
     * 配置 RTSP 协议
     * 
     * @param grabber FFmpegFrameGrabber 实例
     * 
     * Requirements: 3.2 - RTSP 流支持
     */
    private fun configureRTSP(grabber: FFmpegFrameGrabber) {
        try {
            // 使用 TCP 传输（更可靠）
            grabber.setOption("rtsp_transport", "tcp")
            
            // 设置 RTSP 标志
            grabber.setOption("rtsp_flags", "prefer_tcp")
            
            // 设置超时
            grabber.setOption("stimeout", "5000000") // 5秒，微秒
            
            println("[FFmpegGrabberConfigurator] RTSP configuration applied")
        } catch (e: Exception) {
            println("[FFmpegGrabberConfigurator] Error configuring RTSP: ${e.message}")
        }
    }
    
    /**
     * 配置 RTMP 协议
     * 
     * @param grabber FFmpegFrameGrabber 实例
     */
    private fun configureRTMP(grabber: FFmpegFrameGrabber) {
        try {
            // RTMP 特定配置
            grabber.setOption("rtmp_live", "live")
            
            println("[FFmpegGrabberConfigurator] RTMP configuration applied")
        } catch (e: Exception) {
            println("[FFmpegGrabberConfigurator] Error configuring RTMP: ${e.message}")
        }
    }
    
    /**
     * 配置 HLS 协议
     * 
     * @param grabber FFmpegFrameGrabber 实例
     * 
     * Requirements: 3.3 - HLS 流支持
     */
    private fun configureHLS(grabber: FFmpegFrameGrabber) {
        try {
            // 允许所有扩展名
            grabber.setOption("allowed_extensions", "ALL")
            
            // 设置 HTTP 选项
            grabber.setOption("http_persistent", "1")
            grabber.setOption("http_multiple", "1")
            
            println("[FFmpegGrabberConfigurator] HLS configuration applied")
        } catch (e: Exception) {
            println("[FFmpegGrabberConfigurator] Error configuring HLS: ${e.message}")
        }
    }
    
    /**
     * 配置本地文件
     * 
     * @param grabber FFmpegFrameGrabber 实例
     * 
     * Requirements: 3.4 - 本地文件支持
     */
    private fun configureFile(grabber: FFmpegFrameGrabber) {
        try {
            // 启用快速查找
            grabber.setOption("fflags", "+fastseek+genpts")
            
            // 启用精确查找
            grabber.setOption("accurate_seek", "1")
            
            println("[FFmpegGrabberConfigurator] Local file configuration applied")
        } catch (e: Exception) {
            println("[FFmpegGrabberConfigurator] Error configuring file: ${e.message}")
        }
    }
    
    /**
     * 创建并配置 FFmpegFrameGrabber
     * 
     * 这是一个便捷方法，创建新的 grabber 并应用配置。
     * 
     * @param url 媒体 URL
     * @param enableHardwareAcceleration 是否启用硬件加速
     * @return 配置好的 FFmpegFrameGrabber 实例和使用的硬件加速类型
     */
    fun createAndConfigure(
        url: String,
        enableHardwareAcceleration: Boolean = true
    ): Pair<FFmpegFrameGrabber, HardwareAccelerationType> {
        val grabber = FFmpegFrameGrabber(url)
        val hwType = configure(grabber, url, enableHardwareAcceleration)
        return Pair(grabber, hwType)
    }
    
    /**
     * 验证配置
     * 
     * 检查 grabber 配置是否正确，尝试启动并抓取第一帧。
     * 
     * @param grabber 已配置的 FFmpegFrameGrabber
     * @return true 如果配置有效，false 如果配置失败
     */
    fun validateConfiguration(grabber: FFmpegFrameGrabber): Boolean {
        return try {
            println("[FFmpegGrabberConfigurator] Validating configuration...")
            
            // 尝试启动
            grabber.start()
            
            // 尝试抓取第一帧
            val frame = grabber.grab()
            val isValid = frame != null
            
            if (isValid) {
                println("[FFmpegGrabberConfigurator] Configuration validated successfully")
            } else {
                println("[FFmpegGrabberConfigurator] Configuration validation failed: no frame grabbed")
            }
            
            isValid
        } catch (e: Exception) {
            println("[FFmpegGrabberConfigurator] Configuration validation failed: ${e.message}")
            false
        }
    }
    
    /**
     * 获取配置摘要
     * 
     * 生成当前配置的可读摘要。
     * 
     * @param url 媒体 URL
     * @param hwType 硬件加速类型
     * @return 格式化的配置摘要
     */
    fun getConfigurationSummary(url: String, hwType: HardwareAccelerationType): String {
        val isLive = StreamTypeDetector.isLiveStream(url)
        val protocol = StreamTypeDetector.detectProtocol(url)
        val bufferConfig = StreamTypeDetector.getBufferConfiguration(url)
        
        return buildString {
            appendLine("FFmpeg Grabber Configuration Summary:")
            appendLine("  URL: $url")
            appendLine("  Protocol: ${protocol.name}")
            appendLine("  Stream Type: ${if (isLive) "Live" else "VOD"}")
            appendLine("  Hardware Acceleration: ${hwType.name}")
            appendLine("  Buffer Configuration:")
            appendLine("    Probe Size: ${bufferConfig.probeSize} bytes")
            appendLine("    Analyze Duration: ${bufferConfig.maxAnalyzeDuration} μs")
            appendLine("    Buffer Size: ${bufferConfig.bufferSize} bytes")
        }
    }
    
    /**
     * 应用自定义选项
     * 
     * 允许应用额外的自定义 FFmpeg 选项。
     * 
     * @param grabber FFmpegFrameGrabber 实例
     * @param options 选项映射（键值对）
     */
    fun applyCustomOptions(grabber: FFmpegFrameGrabber, options: Map<String, String>) {
        try {
            println("[FFmpegGrabberConfigurator] Applying ${options.size} custom options")
            
            options.forEach { (key, value) ->
                try {
                    grabber.setOption(key, value)
                    println("[FFmpegGrabberConfigurator] Applied option: $key=$value")
                } catch (e: Exception) {
                    println("[FFmpegGrabberConfigurator] Failed to apply option $key=$value: ${e.message}")
                }
            }
        } catch (e: Exception) {
            println("[FFmpegGrabberConfigurator] Error applying custom options: ${e.message}")
        }
    }
}
