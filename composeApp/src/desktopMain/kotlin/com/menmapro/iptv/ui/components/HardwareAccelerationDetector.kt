package com.menmapro.iptv.ui.components

/**
 * HardwareAccelerationDetector - 硬件加速检测器
 * 
 * 负责检测系统是否支持硬件加速视频解码，并提供相应的VLC配置选项。
 * 硬件加速可以显著提高视频解码性能并降低CPU使用率。
 * 
 * Validates: Requirements 2.3
 */
object HardwareAccelerationDetector {
    
    /**
     * 硬件加速支持状态
     */
    data class HardwareAccelerationSupport(
        val isSupported: Boolean,
        val accelerationType: AccelerationType,
        val reason: String
    )
    
    /**
     * 硬件加速类型
     */
    enum class AccelerationType {
        NONE,           // 不支持或禁用
        AUTO,           // 自动检测（推荐）
        VIDEOTOOLBOX,   // macOS VideoToolbox
        VAAPI,          // Linux VA-API
        DXVA2,          // Windows DirectX Video Acceleration
        D3D11VA,        // Windows Direct3D 11 Video Acceleration
        CUDA,           // NVIDIA CUDA
        QSV             // Intel Quick Sync Video
    }
    
    /**
     * 检测系统是否支持硬件加速
     * 
     * 基于操作系统和可用的硬件加速API进行检测。
     * 
     * @return HardwareAccelerationSupport 包含支持状态和详细信息
     */
    fun detectHardwareAcceleration(): HardwareAccelerationSupport {
        val os = VideoOutputConfiguration.detectOperatingSystem()
        
        return when (os) {
            VideoOutputConfiguration.OperatingSystem.MACOS -> {
                // macOS通常支持VideoToolbox硬件加速
                HardwareAccelerationSupport(
                    isSupported = true,
                    accelerationType = AccelerationType.VIDEOTOOLBOX,
                    reason = "macOS supports VideoToolbox hardware acceleration"
                )
            }
            
            VideoOutputConfiguration.OperatingSystem.LINUX -> {
                // Linux支持多种硬件加速方式，使用AUTO让VLC自动选择
                HardwareAccelerationSupport(
                    isSupported = true,
                    accelerationType = AccelerationType.AUTO,
                    reason = "Linux supports VA-API and other hardware acceleration methods"
                )
            }
            
            VideoOutputConfiguration.OperatingSystem.WINDOWS -> {
                // Windows支持DXVA2和D3D11VA，使用AUTO让VLC自动选择
                HardwareAccelerationSupport(
                    isSupported = true,
                    accelerationType = AccelerationType.AUTO,
                    reason = "Windows supports DXVA2 and D3D11VA hardware acceleration"
                )
            }
            
            VideoOutputConfiguration.OperatingSystem.UNKNOWN -> {
                // 未知系统，尝试使用AUTO
                HardwareAccelerationSupport(
                    isSupported = true,
                    accelerationType = AccelerationType.AUTO,
                    reason = "Unknown OS, attempting auto-detection"
                )
            }
        }
    }
    
    /**
     * 获取硬件加速的VLC选项
     * 
     * 根据检测结果返回适当的VLC命令行参数。
     * 如果硬件加速不可用，VLC会自动回退到软件解码。
     * 
     * @param support 硬件加速支持信息
     * @return VLC命令行参数数组
     */
    fun getHardwareAccelerationOptions(support: HardwareAccelerationSupport): Array<String> {
        if (!support.isSupported) {
            return arrayOf("--avcodec-hw=none")
        }
        
        return when (support.accelerationType) {
            AccelerationType.NONE -> arrayOf("--avcodec-hw=none")
            AccelerationType.AUTO -> arrayOf("--avcodec-hw=any")
            AccelerationType.VIDEOTOOLBOX -> arrayOf("--avcodec-hw=videotoolbox")
            AccelerationType.VAAPI -> arrayOf("--avcodec-hw=vaapi")
            AccelerationType.DXVA2 -> arrayOf("--avcodec-hw=dxva2")
            AccelerationType.D3D11VA -> arrayOf("--avcodec-hw=d3d11va")
            AccelerationType.CUDA -> arrayOf("--avcodec-hw=cuda")
            AccelerationType.QSV -> arrayOf("--avcodec-hw=qsv")
        }
    }
    
    /**
     * 获取硬件加速状态的描述性字符串
     * 用于日志和诊断
     * 
     * @param support 硬件加速支持信息
     * @return 状态描述字符串
     */
    fun getHardwareAccelerationInfo(support: HardwareAccelerationSupport): String {
        return buildString {
            appendLine("Hardware Acceleration Status:")
            appendLine("  Supported: ${if (support.isSupported) "Yes" else "No"}")
            appendLine("  Type: ${support.accelerationType}")
            appendLine("  Reason: ${support.reason}")
            if (support.isSupported) {
                appendLine("  VLC Options: ${getHardwareAccelerationOptions(support).joinToString(", ")}")
            }
        }
    }
    
    /**
     * 检查硬件加速是否实际启用
     * 
     * 通过检查VLC选项来确认硬件加速是否被启用。
     * 
     * @param vlcOptions VLC命令行参数数组
     * @return true如果硬件加速已启用，false否则
     */
    fun isHardwareAccelerationEnabled(vlcOptions: Array<String>): Boolean {
        return vlcOptions.any { option ->
            option.startsWith("--avcodec-hw=") && !option.contains("none")
        }
    }
}
