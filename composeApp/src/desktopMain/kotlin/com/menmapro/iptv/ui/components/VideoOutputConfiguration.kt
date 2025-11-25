package com.menmapro.iptv.ui.components

/**
 * VideoOutputConfiguration - 视频输出配置工具类
 * 
 * 负责检测操作系统并提供平台特定的VLC视频输出选项。
 * 这确保视频能够在不同平台的嵌入式组件中正确渲染。
 */
object VideoOutputConfiguration {
    
    /**
     * 操作系统枚举
     */
    enum class OperatingSystem {
        MACOS,
        LINUX,
        WINDOWS,
        UNKNOWN
    }
    
    /**
     * 检测当前操作系统
     * 
     * @return 当前运行的操作系统
     */
    fun detectOperatingSystem(): OperatingSystem {
        val osName = System.getProperty("os.name").lowercase()
        
        return when {
            osName.contains("mac") || osName.contains("darwin") -> OperatingSystem.MACOS
            osName.contains("linux") -> OperatingSystem.LINUX
            osName.contains("win") -> OperatingSystem.WINDOWS
            else -> OperatingSystem.UNKNOWN
        }
    }
    
    /**
     * 获取当前平台的最佳视频输出选项
     * 
     * 根据操作系统返回最适合的VLC视频输出模块配置。
     * 这些选项针对嵌入式播放器进行了优化。
     * 
     * @return VLC命令行参数数组
     */
    fun getPlatformVideoOptions(): Array<String> {
        val os = detectOperatingSystem()
        
        return when (os) {
            OperatingSystem.MACOS -> arrayOf(
                // Use caopengllayer for embedded playback on macOS
                // This is more reliable than macosx vout for embedded scenarios
                "--vout=caopengllayer",
                "--no-video-title-show",
                "--no-osd",
                // Disable screen saver
                "--no-disable-screensaver"
            )
            
            OperatingSystem.LINUX -> arrayOf(
                "--vout=xcb_x11",
                "--no-video-title-show",
                "--no-osd"
            )
            
            OperatingSystem.WINDOWS -> arrayOf(
                "--vout=directdraw",
                "--no-video-title-show",
                "--no-osd"
            )
            
            OperatingSystem.UNKNOWN -> {
                println("⚠️ Unknown operating system, using OpenGL fallback")
                getFallbackVideoOptions()
            }
        }
    }
    
    /**
     * 获取备用视频输出选项
     * 
     * 当主要视频输出模块失败时使用。
     * OpenGL是跨平台的通用选项，兼容性最好。
     * 
     * @return VLC命令行参数数组
     */
    fun getFallbackVideoOptions(): Array<String> {
        val os = detectOperatingSystem()
        
        return when (os) {
            OperatingSystem.MACOS -> arrayOf(
                // Fallback to OpenGL for macOS
                "--vout=opengl",
                "--no-video-title-show",
                "--no-osd"
            )
            else -> arrayOf(
                "--vout=opengl",
                "--no-video-title-show",
                "--no-osd"
            )
        }
    }
    
    /**
     * 获取当前平台信息的描述性字符串
     * 用于日志和诊断
     * 
     * @return 平台信息字符串
     */
    fun getPlatformInfo(): String {
        val os = detectOperatingSystem()
        val osName = System.getProperty("os.name")
        val osVersion = System.getProperty("os.version")
        val osArch = System.getProperty("os.arch")
        
        return """
            Operating System: $os
            OS Name: $osName
            OS Version: $osVersion
            OS Architecture: $osArch
        """.trimIndent()
    }
}
