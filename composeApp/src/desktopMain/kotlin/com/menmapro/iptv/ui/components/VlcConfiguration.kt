package com.menmapro.iptv.ui.components

/**
 * VlcConfiguration - VLC配置数据类
 * 
 * 封装所有VLC播放器的配置参数，包括视频输出、硬件加速、缓存设置等。
 * 提供配置验证和转换为VLC命令行参数的功能。
 * 
 * Validates: Requirements 2.1, 2.2, 2.3
 * 
 * 使用示例:
 * ```
 * val config = VlcConfiguration.defaultForPlatform()
 * val vlcArgs = config.toVlcArgs()
 * val component = EmbeddedMediaPlayerComponent(null, null, null, null, vlcArgs)
 * ```
 */
data class VlcConfiguration(
    /**
     * 视频输出模块
     * 例如: "macosx", "opengl", "xcb_x11", "directdraw"
     */
    val videoOutput: String,
    
    /**
     * 是否启用硬件加速
     */
    val hardwareAcceleration: Boolean = true,
    
    /**
     * 网络缓存时间（毫秒）
     * 推荐值: 300-3000ms
     */
    val networkCaching: Int = 1000,
    
    /**
     * 直播流缓存时间（毫秒）
     * 推荐值: 300-1000ms
     */
    val liveCaching: Int = 300,
    
    /**
     * 是否显示视频标题
     */
    val showVideoTitle: Boolean = false,
    
    /**
     * 是否显示屏幕显示（OSD）
     */
    val showOSD: Boolean = false,
    
    /**
     * 额外的VLC选项
     */
    val additionalOptions: List<String> = emptyList()
) {
    
    /**
     * 验证配置的有效性
     * 
     * @return ValidationResult 包含验证结果和问题列表
     */
    fun validate(): ValidationResult {
        val issues = mutableListOf<String>()
        
        // 验证视频输出模块
        if (videoOutput.isBlank()) {
            issues.add("Video output module cannot be empty")
        }
        
        // 验证缓存值
        if (networkCaching < 0) {
            issues.add("Network caching must be non-negative (got: $networkCaching)")
        }
        
        if (liveCaching < 0) {
            issues.add("Live caching must be non-negative (got: $liveCaching)")
        }
        
        // 警告：过高的缓存值
        if (networkCaching > 10000) {
            issues.add("Warning: Network caching is very high ($networkCaching ms), may cause excessive delay")
        }
        
        if (liveCaching > 5000) {
            issues.add("Warning: Live caching is very high ($liveCaching ms), may cause excessive delay for live streams")
        }
        
        return ValidationResult(
            isValid = issues.none { it.startsWith("Error") || !it.startsWith("Warning") },
            issues = issues
        )
    }
    
    /**
     * 转换为VLC命令行参数数组
     * 
     * 将配置对象转换为VLC可以使用的命令行参数。
     * 这些参数在创建EmbeddedMediaPlayerComponent时使用。
     * 
     * @return VLC命令行参数数组
     */
    fun toVlcArgs(): Array<String> {
        val args = mutableListOf<String>()
        
        // 视频输出模块
        args.add("--vout=$videoOutput")
        
        // 硬件加速
        if (hardwareAcceleration) {
            args.add("--avcodec-hw=any")
        } else {
            args.add("--avcodec-hw=none")
        }
        
        // 视频标题显示
        if (!showVideoTitle) {
            args.add("--no-video-title-show")
        }
        
        // OSD显示
        if (!showOSD) {
            args.add("--no-osd")
        }
        
        // 添加额外选项
        args.addAll(additionalOptions)
        
        return args.toTypedArray()
    }
    
    /**
     * 转换为媒体选项数组
     * 
     * 将配置对象转换为VLC媒体选项。
     * 这些选项在调用mediaPlayer.media().play(url, options)时使用。
     * 
     * @return VLC媒体选项数组
     */
    fun toMediaOptions(): Array<String> {
        val options = mutableListOf<String>()
        
        // 网络缓存
        options.add(":network-caching=$networkCaching")
        
        // 直播缓存
        options.add(":live-caching=$liveCaching")
        
        // 硬件加速（媒体选项格式）
        if (hardwareAcceleration) {
            options.add(":avcodec-hw=any")
        }
        
        return options.toTypedArray()
    }
    
    /**
     * 获取配置的描述性字符串
     * 用于日志和诊断
     * 
     * @return 配置描述字符串
     */
    fun toDescriptiveString(): String {
        return buildString {
            appendLine("VLC Configuration:")
            appendLine("  Video Output: $videoOutput")
            appendLine("  Hardware Acceleration: ${if (hardwareAcceleration) "Enabled" else "Disabled"}")
            appendLine("  Network Caching: ${networkCaching}ms")
            appendLine("  Live Caching: ${liveCaching}ms")
            appendLine("  Show Video Title: $showVideoTitle")
            appendLine("  Show OSD: $showOSD")
            if (additionalOptions.isNotEmpty()) {
                appendLine("  Additional Options: ${additionalOptions.joinToString(", ")}")
            }
            
            val validation = validate()
            if (!validation.isValid || validation.issues.isNotEmpty()) {
                appendLine("  Validation Issues:")
                validation.issues.forEach { issue ->
                    appendLine("    - $issue")
                }
            }
        }
    }
    
    /**
     * 验证结果数据类
     */
    data class ValidationResult(
        val isValid: Boolean,
        val issues: List<String>
    )
    
    companion object {
        /**
         * 创建当前平台的默认配置
         * 
         * 根据操作系统自动选择最佳的视频输出模块和硬件加速设置。
         * 
         * Validates: Requirements 2.1, 2.2, 2.3
         * 
         * @return 平台特定的VlcConfiguration实例
         */
        fun defaultForPlatform(): VlcConfiguration {
            val os = VideoOutputConfiguration.detectOperatingSystem()
            val hwSupport = HardwareAccelerationDetector.detectHardwareAcceleration()
            
            val videoOutput = when (os) {
                VideoOutputConfiguration.OperatingSystem.MACOS -> "macosx"
                VideoOutputConfiguration.OperatingSystem.LINUX -> "xcb_x11"
                VideoOutputConfiguration.OperatingSystem.WINDOWS -> "directdraw"
                VideoOutputConfiguration.OperatingSystem.UNKNOWN -> "opengl"
            }
            
            return VlcConfiguration(
                videoOutput = videoOutput,
                hardwareAcceleration = hwSupport.isSupported,
                networkCaching = 1000,
                liveCaching = 300,
                showVideoTitle = false,
                showOSD = false
            )
        }
        
        /**
         * 创建备用配置
         * 
         * 使用OpenGL作为视频输出，这是最通用的跨平台选项。
         * 当平台特定的视频输出失败时使用。
         * 
         * @return 备用VlcConfiguration实例
         */
        fun fallbackConfiguration(): VlcConfiguration {
            return VlcConfiguration(
                videoOutput = "opengl",
                hardwareAcceleration = false,  // 禁用硬件加速以提高兼容性
                networkCaching = 1000,
                liveCaching = 300,
                showVideoTitle = false,
                showOSD = false
            )
        }
        
        /**
         * 创建针对直播流优化的配置
         * 
         * 使用低延迟缓存设置和直播流优化选项。
         * 
         * @return 直播流优化的VlcConfiguration实例
         */
        fun forLiveStream(): VlcConfiguration {
            return defaultForPlatform().copy(
                networkCaching = 1000,
                liveCaching = 300,
                additionalOptions = listOf(
                    "--clock-jitter=0",
                    "--clock-synchro=0",
                    "--no-audio-time-stretch"
                )
            )
        }
        
        /**
         * 创建针对点播内容优化的配置
         * 
         * 使用较高的缓存以获得更流畅的播放。
         * 
         * @return 点播内容优化的VlcConfiguration实例
         */
        fun forVOD(): VlcConfiguration {
            return defaultForPlatform().copy(
                networkCaching = 3000,
                liveCaching = 1000
            )
        }
        
        /**
         * 创建针对本地文件优化的配置
         * 
         * 使用最小缓存以快速启动。
         * 
         * @return 本地文件优化的VlcConfiguration实例
         */
        fun forLocalFile(): VlcConfiguration {
            return defaultForPlatform().copy(
                networkCaching = 300,
                liveCaching = 100
            )
        }
        
        /**
         * 从现有的VLC参数数组创建配置
         * 
         * 解析VLC命令行参数并创建相应的配置对象。
         * 这对于从现有配置迁移很有用。
         * 
         * @param vlcArgs VLC命令行参数数组
         * @return 解析后的VlcConfiguration实例
         */
        fun fromVlcArgs(vlcArgs: Array<String>): VlcConfiguration {
            var videoOutput = "opengl"
            var hardwareAcceleration = true
            var showVideoTitle = true
            var showOSD = true
            val additionalOptions = mutableListOf<String>()
            
            vlcArgs.forEach { arg ->
                when {
                    arg.startsWith("--vout=") -> {
                        videoOutput = arg.substringAfter("--vout=")
                    }
                    arg.startsWith("--avcodec-hw=") -> {
                        hardwareAcceleration = !arg.contains("none")
                    }
                    arg == "--no-video-title-show" -> {
                        showVideoTitle = false
                    }
                    arg == "--no-osd" -> {
                        showOSD = false
                    }
                    else -> {
                        additionalOptions.add(arg)
                    }
                }
            }
            
            return VlcConfiguration(
                videoOutput = videoOutput,
                hardwareAcceleration = hardwareAcceleration,
                networkCaching = 1000,  // 默认值
                liveCaching = 300,      // 默认值
                showVideoTitle = showVideoTitle,
                showOSD = showOSD,
                additionalOptions = additionalOptions
            )
        }
    }
}
