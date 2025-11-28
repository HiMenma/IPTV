package com.menmapro.iptv.player.ffmpeg

import org.bytedeco.javacv.FFmpegFrameGrabber
import java.io.File

/**
 * 硬件加速管理器
 * 
 * 负责检测和配置平台特定的硬件加速方案，包括：
 * - macOS: VideoToolbox
 * - Linux: VAAPI, VDPAU
 * - Windows: DXVA2, D3D11VA
 * 
 * 提供自动检测、配置和回退机制，确保在硬件加速不可用时能够
 * 自动切换到软件解码。
 * 
 * Requirements: 4.1, 4.2, 4.3, 4.4, 4.5
 */
object HardwareAccelerationManager {
    
    /**
     * 当前操作系统
     */
    private val osName: String = System.getProperty("os.name").lowercase()
    
    /**
     * 是否为 macOS
     */
    private val isMacOS: Boolean = osName.contains("mac")
    
    /**
     * 是否为 Linux
     */
    private val isLinux: Boolean = osName.contains("linux")
    
    /**
     * 是否为 Windows
     */
    private val isWindows: Boolean = osName.contains("windows")
    
    /**
     * 硬件加速回退事件监听器
     */
    private var fallbackListener: ((HardwareAccelerationType, String) -> Unit)? = null
    
    /**
     * 记录失败的硬件加速类型
     * 用于避免重复尝试已知失败的硬件加速方案
     */
    private val failedAccelerationTypes = mutableSetOf<HardwareAccelerationType>()
    
    /**
     * 回退事件历史记录
     */
    private val fallbackHistory = mutableListOf<FallbackEvent>()
    
    /**
     * 检测可用的硬件加速方案
     * 
     * 根据当前平台自动检测最佳的硬件加速方案。
     * 检测顺序：
     * - macOS: VideoToolbox
     * - Linux: VAAPI -> VDPAU -> 软件解码
     * - Windows: D3D11VA -> DXVA2 -> 软件解码
     * 
     * 会跳过之前已知失败的硬件加速类型。
     * 
     * @return HardwareAcceleration 实例，包含检测结果
     * 
     * Requirements: 4.1 - 自动检测并启用硬件解码
     */
    fun detectHardwareAcceleration(): HardwareAcceleration {
        val detected = when {
            isMacOS -> detectMacOSAcceleration()
            isLinux -> detectLinuxAcceleration()
            isWindows -> detectWindowsAcceleration()
            else -> HardwareAcceleration(
                type = HardwareAccelerationType.NONE,
                isAvailable = false,
                deviceName = null
            )
        }
        
        // 如果检测到的类型之前失败过，返回 NONE
        if (detected.isAvailable && hasFailedBefore(detected.type)) {
            println("Skipping ${detected.type} as it has failed before")
            return HardwareAcceleration(
                type = HardwareAccelerationType.NONE,
                isAvailable = false,
                deviceName = null
            )
        }
        
        return detected
    }
    
    /**
     * 检测 macOS 硬件加速
     * 
     * macOS 使用 VideoToolbox 框架进行硬件加速。
     * VideoToolbox 在 macOS 10.8+ 上可用，支持 H.264 和 HEVC 解码。
     * 
     * @return HardwareAcceleration 实例
     * 
     * Requirements: 4.2 - macOS VideoToolbox 硬件加速
     */
    private fun detectMacOSAcceleration(): HardwareAcceleration {
        return try {
            // VideoToolbox 在现代 macOS 上默认可用
            // 检查系统版本
            val osVersion = System.getProperty("os.version")
            val majorVersion = osVersion.split(".").firstOrNull()?.toIntOrNull() ?: 0
            
            // macOS 10.8+ 支持 VideoToolbox
            val isAvailable = majorVersion >= 10
            
            HardwareAcceleration(
                type = HardwareAccelerationType.VIDEOTOOLBOX,
                isAvailable = isAvailable,
                deviceName = "VideoToolbox"
            )
        } catch (e: Exception) {
            println("Failed to detect VideoToolbox: ${e.message}")
            HardwareAcceleration(
                type = HardwareAccelerationType.NONE,
                isAvailable = false,
                deviceName = null
            )
        }
    }
    
    /**
     * 检测 Linux 硬件加速
     * 
     * Linux 支持多种硬件加速方案：
     * - VAAPI: Intel 和 AMD GPU
     * - VDPAU: NVIDIA GPU
     * 
     * @return HardwareAcceleration 实例
     * 
     * Requirements: 4.3 - Linux VAAPI/VDPAU 硬件加速
     */
    private fun detectLinuxAcceleration(): HardwareAcceleration {
        // 优先检测 VAAPI
        val vaapiDevice = detectVAAPI()
        if (vaapiDevice != null) {
            return HardwareAcceleration(
                type = HardwareAccelerationType.VAAPI,
                isAvailable = true,
                deviceName = vaapiDevice
            )
        }
        
        // 检测 VDPAU
        val vdpauAvailable = detectVDPAU()
        if (vdpauAvailable) {
            return HardwareAcceleration(
                type = HardwareAccelerationType.VDPAU,
                isAvailable = true,
                deviceName = "VDPAU"
            )
        }
        
        // 无硬件加速可用
        return HardwareAcceleration(
            type = HardwareAccelerationType.NONE,
            isAvailable = false,
            deviceName = null
        )
    }
    
    /**
     * 检测 VAAPI 设备
     * 
     * VAAPI 设备通常位于 /dev/dri/renderD128
     * 
     * @return 设备路径，如果不可用则返回 null
     */
    private fun detectVAAPI(): String? {
        val devices = listOf(
            "/dev/dri/renderD128",
            "/dev/dri/renderD129",
            "/dev/dri/card0"
        )
        
        return devices.firstOrNull { device ->
            try {
                File(device).exists() && File(device).canRead()
            } catch (e: Exception) {
                false
            }
        }
    }
    
    /**
     * 检测 VDPAU 支持
     * 
     * VDPAU 主要用于 NVIDIA GPU
     * 
     * @return true 如果 VDPAU 可用
     */
    private fun detectVDPAU(): Boolean {
        return try {
            // 检查 NVIDIA 驱动
            val nvidiaDevice = File("/proc/driver/nvidia/version")
            nvidiaDevice.exists()
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 检测 Windows 硬件加速
     * 
     * Windows 支持多种硬件加速方案：
     * - D3D11VA: DirectX 11 视频加速（Windows 7+）
     * - DXVA2: DirectX 视频加速 2（Windows Vista+）
     * 
     * @return HardwareAcceleration 实例
     * 
     * Requirements: 4.4 - Windows DXVA2/D3D11VA 硬件加速
     */
    private fun detectWindowsAcceleration(): HardwareAcceleration {
        return try {
            // 检查 Windows 版本
            val osVersion = System.getProperty("os.version")
            val versionParts = osVersion.split(".")
            val majorVersion = versionParts.firstOrNull()?.toIntOrNull() ?: 0
            val minorVersion = versionParts.getOrNull(1)?.toIntOrNull() ?: 0
            
            // Windows 7+ (6.1+) 支持 D3D11VA
            // Windows Vista+ (6.0+) 支持 DXVA2
            val type = when {
                majorVersion > 6 || (majorVersion == 6 && minorVersion >= 1) -> 
                    HardwareAccelerationType.D3D11VA
                majorVersion == 6 && minorVersion == 0 -> 
                    HardwareAccelerationType.DXVA2
                else -> 
                    HardwareAccelerationType.NONE
            }
            
            HardwareAcceleration(
                type = type,
                isAvailable = type != HardwareAccelerationType.NONE,
                deviceName = type.name
            )
        } catch (e: Exception) {
            println("Failed to detect Windows hardware acceleration: ${e.message}")
            HardwareAcceleration(
                type = HardwareAccelerationType.NONE,
                isAvailable = false,
                deviceName = null
            )
        }
    }

    /**
     * 配置 FFmpeg 使用硬件加速
     * 
     * 根据检测到的硬件加速类型配置 FFmpegFrameGrabber。
     * 如果配置失败，会自动回退到软件解码。
     * 
     * @param grabber FFmpegFrameGrabber 实例
     * @param hwAccel 硬件加速配置
     * @return true 如果配置成功，false 如果回退到软件解码
     * 
     * Requirements: 4.1, 4.2, 4.3, 4.4 - 配置平台特定的硬件加速
     */
    fun configureHardwareAcceleration(
        grabber: FFmpegFrameGrabber,
        hwAccel: HardwareAcceleration
    ): Boolean {
        if (!hwAccel.isAvailable || hwAccel.type == HardwareAccelerationType.NONE) {
            println("Hardware acceleration not available, using software decoding")
            return false
        }
        
        return try {
            val success = when (hwAccel.type) {
                HardwareAccelerationType.VIDEOTOOLBOX -> {
                    configureVideoToolbox(grabber)
                }
                HardwareAccelerationType.VAAPI -> {
                    configureVAAPI(grabber, hwAccel.deviceName)
                }
                HardwareAccelerationType.VDPAU -> {
                    configureVDPAU(grabber)
                }
                HardwareAccelerationType.DXVA2 -> {
                    configureDXVA2(grabber)
                }
                HardwareAccelerationType.D3D11VA -> {
                    configureD3D11VA(grabber)
                }
                HardwareAccelerationType.NONE -> false
            }
            
            if (!success) {
                handleFallback(hwAccel.type, "Configuration returned false")
            }
            
            success
        } catch (e: Exception) {
            println("Failed to configure hardware acceleration: ${e.message}")
            handleFallback(hwAccel.type, e.message ?: "Unknown error")
            false
        }
    }
    
    /**
     * 配置 VideoToolbox 硬件加速（macOS）
     * 
     * @param grabber FFmpegFrameGrabber 实例
     * @return true 如果配置成功
     */
    private fun configureVideoToolbox(grabber: FFmpegFrameGrabber): Boolean {
        return try {
            grabber.setOption("hwaccel", "videotoolbox")
            grabber.setOption("hwaccel_output_format", "videotoolbox_vld")
            println("VideoToolbox hardware acceleration enabled")
            true
        } catch (e: Exception) {
            println("Failed to enable VideoToolbox: ${e.message}")
            false
        }
    }
    
    /**
     * 配置 VAAPI 硬件加速（Linux）
     * 
     * @param grabber FFmpegFrameGrabber 实例
     * @param deviceName VAAPI 设备路径
     * @return true 如果配置成功
     */
    private fun configureVAAPI(grabber: FFmpegFrameGrabber, deviceName: String?): Boolean {
        return try {
            grabber.setOption("hwaccel", "vaapi")
            if (deviceName != null) {
                grabber.setOption("hwaccel_device", deviceName)
            }
            grabber.setOption("hwaccel_output_format", "vaapi")
            println("VAAPI hardware acceleration enabled (device: $deviceName)")
            true
        } catch (e: Exception) {
            println("Failed to enable VAAPI: ${e.message}")
            false
        }
    }
    
    /**
     * 配置 VDPAU 硬件加速（Linux/NVIDIA）
     * 
     * @param grabber FFmpegFrameGrabber 实例
     * @return true 如果配置成功
     */
    private fun configureVDPAU(grabber: FFmpegFrameGrabber): Boolean {
        return try {
            grabber.setOption("hwaccel", "vdpau")
            grabber.setOption("hwaccel_output_format", "vdpau")
            println("VDPAU hardware acceleration enabled")
            true
        } catch (e: Exception) {
            println("Failed to enable VDPAU: ${e.message}")
            false
        }
    }
    
    /**
     * 配置 DXVA2 硬件加速（Windows）
     * 
     * @param grabber FFmpegFrameGrabber 实例
     * @return true 如果配置成功
     */
    private fun configureDXVA2(grabber: FFmpegFrameGrabber): Boolean {
        return try {
            grabber.setOption("hwaccel", "dxva2")
            grabber.setOption("hwaccel_output_format", "dxva2_vld")
            println("DXVA2 hardware acceleration enabled")
            true
        } catch (e: Exception) {
            println("Failed to enable DXVA2: ${e.message}")
            false
        }
    }
    
    /**
     * 配置 D3D11VA 硬件加速（Windows）
     * 
     * @param grabber FFmpegFrameGrabber 实例
     * @return true 如果配置成功
     */
    private fun configureD3D11VA(grabber: FFmpegFrameGrabber): Boolean {
        return try {
            grabber.setOption("hwaccel", "d3d11va")
            grabber.setOption("hwaccel_output_format", "d3d11")
            println("D3D11VA hardware acceleration enabled")
            true
        } catch (e: Exception) {
            println("Failed to enable D3D11VA: ${e.message}")
            false
        }
    }
    
    /**
     * 获取平台特定的硬件加速方案
     * 
     * 这是一个便捷方法，直接返回当前平台推荐的硬件加速方案。
     * 
     * @return HardwareAcceleration 实例
     */
    fun getPlatformHardwareAcceleration(): HardwareAcceleration {
        return detectHardwareAcceleration()
    }
    
    /**
     * 处理硬件加速回退
     * 
     * 当硬件加速失败时，记录回退事件并通知监听器。
     * 
     * @param type 失败的硬件加速类型
     * @param reason 失败原因
     * 
     * Requirements: 4.5 - 硬件加速回退机制
     */
    private fun handleFallback(type: HardwareAccelerationType, reason: String) {
        val message = "Hardware acceleration ($type) failed: $reason. Falling back to software decoding."
        println(message)
        
        // 记录失败的硬件加速类型
        failedAccelerationTypes.add(type)
        
        // 记录回退事件
        val event = FallbackEvent(
            type = type,
            reason = reason,
            timestamp = System.currentTimeMillis()
        )
        fallbackHistory.add(event)
        
        // 通知监听器
        fallbackListener?.invoke(type, reason)
    }
    
    /**
     * 检查硬件加速类型是否已知失败
     * 
     * @param type 硬件加速类型
     * @return true 如果该类型之前失败过
     */
    fun hasFailedBefore(type: HardwareAccelerationType): Boolean {
        return failedAccelerationTypes.contains(type)
    }
    
    /**
     * 重置失败记录
     * 
     * 清除所有失败的硬件加速类型记录，允许重新尝试。
     * 这在系统配置更改或驱动更新后可能有用。
     */
    fun resetFailureTracking() {
        failedAccelerationTypes.clear()
        fallbackHistory.clear()
        println("Hardware acceleration failure tracking reset")
    }
    
    /**
     * 获取回退事件历史
     * 
     * @return 回退事件列表
     */
    fun getFallbackHistory(): List<FallbackEvent> {
        return fallbackHistory.toList()
    }
    
    /**
     * 获取回退统计信息
     * 
     * @return 格式化的统计信息字符串
     */
    fun getFallbackStatistics(): String {
        return buildString {
            appendLine("Hardware Acceleration Fallback Statistics:")
            appendLine("  Total fallback events: ${fallbackHistory.size}")
            appendLine("  Failed acceleration types: ${failedAccelerationTypes.joinToString(", ")}")
            
            if (fallbackHistory.isNotEmpty()) {
                appendLine("\nRecent fallback events:")
                fallbackHistory.takeLast(5).forEach { event ->
                    appendLine("  - ${event.type}: ${event.reason} (${event.getFormattedTimestamp()})")
                }
            }
        }
    }
    
    /**
     * 设置硬件加速回退事件监听器
     * 
     * @param listener 回退事件监听器，接收失败的硬件加速类型和原因
     */
    fun setFallbackListener(listener: (HardwareAccelerationType, String) -> Unit) {
        fallbackListener = listener
    }
    
    /**
     * 清除回退事件监听器
     */
    fun clearFallbackListener() {
        fallbackListener = null
    }
    
    /**
     * 配置硬件加速并自动回退
     * 
     * 这是一个高级方法，尝试配置硬件加速，如果失败则自动回退到软件解码。
     * 该方法会：
     * 1. 检测可用的硬件加速
     * 2. 尝试配置硬件加速
     * 3. 如果配置失败，自动切换到软件解码
     * 4. 记录所有回退事件
     * 
     * @param grabber FFmpegFrameGrabber 实例
     * @return 使用的硬件加速类型（NONE 表示软件解码）
     * 
     * Requirements: 4.5 - 硬件加速回退机制
     */
    fun configureWithFallback(grabber: FFmpegFrameGrabber): HardwareAccelerationType {
        // 1. 检测硬件加速
        val hwAccel = detectHardwareAcceleration()
        
        if (!hwAccel.isAvailable || hwAccel.type == HardwareAccelerationType.NONE) {
            println("No hardware acceleration available, using software decoding")
            return HardwareAccelerationType.NONE
        }
        
        // 2. 尝试配置硬件加速
        println("Attempting to configure hardware acceleration: ${hwAccel.type}")
        val success = configureHardwareAcceleration(grabber, hwAccel)
        
        if (success) {
            println("Hardware acceleration configured successfully: ${hwAccel.type}")
            return hwAccel.type
        }
        
        // 3. 配置失败，回退到软件解码
        println("Hardware acceleration failed, falling back to software decoding")
        fallbackToSoftwareDecoding(grabber, hwAccel.type, "Configuration failed")
        return HardwareAccelerationType.NONE
    }
    
    /**
     * 回退到软件解码
     * 
     * 清除所有硬件加速选项，确保使用软件解码。
     * 
     * @param grabber FFmpegFrameGrabber 实例
     * @param failedType 失败的硬件加速类型
     * @param reason 失败原因
     * 
     * Requirements: 4.5 - 自动切换到软件解码
     */
    private fun fallbackToSoftwareDecoding(
        grabber: FFmpegFrameGrabber,
        failedType: HardwareAccelerationType,
        reason: String
    ) {
        try {
            // 清除硬件加速选项
            grabber.setOption("hwaccel", "none")
            
            // 记录回退事件
            val message = "Falling back to software decoding from $failedType: $reason"
            println(message)
            
            // 通知监听器
            fallbackListener?.invoke(failedType, reason)
            
        } catch (e: Exception) {
            println("Error during fallback to software decoding: ${e.message}")
        }
    }
    
    /**
     * 检测硬件加速是否失败
     * 
     * 通过尝试解码测试帧来验证硬件加速是否正常工作。
     * 
     * @param grabber 已配置硬件加速的 FFmpegFrameGrabber
     * @return true 如果硬件加速工作正常，false 如果失败
     * 
     * Requirements: 4.5 - 检测硬件加速失败
     */
    fun verifyHardwareAcceleration(grabber: FFmpegFrameGrabber): Boolean {
        return try {
            // 尝试抓取第一帧来验证硬件加速
            val frame = grabber.grab()
            frame != null
        } catch (e: Exception) {
            println("Hardware acceleration verification failed: ${e.message}")
            false
        }
    }
    
    /**
     * 尝试硬件加速并在失败时回退
     * 
     * 这个方法在 grabber 已经启动后使用，用于验证硬件加速是否工作。
     * 如果检测到失败，会重新配置为软件解码。
     * 
     * @param grabber 已启动的 FFmpegFrameGrabber
     * @param hwType 当前使用的硬件加速类型
     * @return true 如果硬件加速工作正常，false 如果已回退到软件解码
     * 
     * Requirements: 4.5 - 检测硬件加速失败并回退
     */
    fun verifyAndFallback(
        grabber: FFmpegFrameGrabber,
        hwType: HardwareAccelerationType
    ): Boolean {
        if (hwType == HardwareAccelerationType.NONE) {
            // 已经在使用软件解码
            return false
        }
        
        val isWorking = verifyHardwareAcceleration(grabber)
        
        if (!isWorking) {
            println("Hardware acceleration verification failed for $hwType")
            handleFallback(hwType, "Verification failed during playback")
            // 注意：此时 grabber 已经启动，无法重新配置
            // 需要在下次播放时使用软件解码
        }
        
        return isWorking
    }
    
    /**
     * 获取当前平台信息
     * 
     * @return 平台信息字符串
     */
    fun getPlatformInfo(): String {
        return buildString {
            appendLine("Platform Information:")
            appendLine("  OS: $osName")
            appendLine("  OS Version: ${System.getProperty("os.version")}")
            appendLine("  Architecture: ${System.getProperty("os.arch")}")
            appendLine("  Java Version: ${System.getProperty("java.version")}")
        }
    }
}

/**
 * 硬件加速配置
 * 
 * @property type 硬件加速类型
 * @property isAvailable 是否可用
 * @property deviceName 设备名称（可选）
 */
data class HardwareAcceleration(
    val type: HardwareAccelerationType,
    val isAvailable: Boolean,
    val deviceName: String?
) {
    /**
     * 获取格式化的描述字符串
     */
    fun getDescription(): String {
        return if (isAvailable) {
            "Hardware Acceleration: $type${deviceName?.let { " ($it)" } ?: ""}"
        } else {
            "Hardware Acceleration: Not available (using software decoding)"
        }
    }
}

/**
 * 硬件加速类型枚举
 */
enum class HardwareAccelerationType {
    /**
     * VideoToolbox (macOS)
     */
    VIDEOTOOLBOX,
    
    /**
     * VAAPI (Linux - Intel/AMD)
     */
    VAAPI,
    
    /**
     * VDPAU (Linux - NVIDIA)
     */
    VDPAU,
    
    /**
     * DXVA2 (Windows Vista+)
     */
    DXVA2,
    
    /**
     * D3D11VA (Windows 7+)
     */
    D3D11VA,
    
    /**
     * 无硬件加速（软件解码）
     */
    NONE
}

/**
 * 硬件加速回退事件
 * 
 * 记录硬件加速失败和回退到软件解码的事件。
 * 
 * @property type 失败的硬件加速类型
 * @property reason 失败原因
 * @property timestamp 事件发生时间戳（毫秒）
 */
data class FallbackEvent(
    val type: HardwareAccelerationType,
    val reason: String,
    val timestamp: Long
) {
    /**
     * 获取格式化的时间戳字符串
     */
    fun getFormattedTimestamp(): String {
        val date = java.util.Date(timestamp)
        val format = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        return format.format(date)
    }
    
    /**
     * 获取事件描述
     */
    fun getDescription(): String {
        return "Hardware acceleration fallback: $type failed ($reason) at ${getFormattedTimestamp()}"
    }
}
