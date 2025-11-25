package com.menmapro.iptv.ui.components

import uk.co.caprica.vlcj.player.component.EmbeddedMediaPlayerComponent
import uk.co.caprica.vlcj.player.base.MediaPlayer

/**
 * VideoRenderingRecovery - 视频渲染错误恢复系统
 * 
 * 提供全面的错误处理和恢复机制，当视频渲染失败时尝试多种备用配置。
 * 记录所有尝试的配置和结果，提供详细的诊断信息。
 * 
 * Requirements: 1.4, 2.4, 3.4, 4.4
 */
object VideoRenderingRecovery {
    
    /**
     * 配置尝试记录
     */
    data class ConfigurationAttempt(
        val attemptNumber: Int,
        val configType: ConfigurationType,
        val options: Array<String>,
        val success: Boolean,
        val errorMessage: String? = null,
        val timestamp: Long = System.currentTimeMillis()
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            
            other as ConfigurationAttempt
            
            if (attemptNumber != other.attemptNumber) return false
            if (configType != other.configType) return false
            if (!options.contentEquals(other.options)) return false
            if (success != other.success) return false
            
            return true
        }
        
        override fun hashCode(): Int {
            var result = attemptNumber
            result = 31 * result + configType.hashCode()
            result = 31 * result + options.contentHashCode()
            result = 31 * result + success.hashCode()
            return result
        }
    }
    
    /**
     * 配置类型枚举
     */
    enum class ConfigurationType {
        PRIMARY_WITH_HW_ACCEL,      // 主要配置 + 硬件加速
        PRIMARY_WITHOUT_HW_ACCEL,   // 主要配置 + 软件解码
        FALLBACK_WITH_HW_ACCEL,     // 备用配置 + 硬件加速
        FALLBACK_WITHOUT_HW_ACCEL,  // 备用配置 + 软件解码
        MINIMAL_OPENGL              // 最小OpenGL配置
    }
    
    /**
     * 恢复结果
     */
    data class RecoveryResult(
        val success: Boolean,
        val mediaPlayerComponent: EmbeddedMediaPlayerComponent? = null,
        val configurationUsed: ConfigurationType? = null,
        val attempts: List<ConfigurationAttempt> = emptyList(),
        val finalErrorMessage: String? = null
    )
    
    /**
     * 尝试使用多种配置初始化媒体播放器
     * 
     * 按优先级尝试不同的配置组合，直到成功或所有选项都失败。
     * 记录每次尝试的详细信息。
     * 
     * @return 恢复结果，包含成功的播放器组件或详细的错误信息
     * 
     * Validates: Requirements 1.4, 2.4
     */
    fun attemptRecovery(): RecoveryResult {
        val attempts = mutableListOf<ConfigurationAttempt>()
        var attemptNumber = 1
        
        println("=== Starting Video Rendering Recovery ===")
        println("Will try multiple configurations to initialize video player")
        println()
        
        // Strategy 1: Primary configuration with hardware acceleration
        println("[$attemptNumber] Attempting: Primary configuration with hardware acceleration")
        val hwAccelSupport = HardwareAccelerationDetector.detectHardwareAcceleration()
        
        if (hwAccelSupport.isSupported) {
            val primaryOptions = VideoOutputConfiguration.getPlatformVideoOptions()
            val hwAccelOptions = HardwareAccelerationDetector.getHardwareAccelerationOptions(hwAccelSupport)
            val options1 = primaryOptions + hwAccelOptions
            
            val result1 = tryInitialization(
                options1,
                ConfigurationType.PRIMARY_WITH_HW_ACCEL,
                attemptNumber
            )
            attempts.add(result1)
            
            if (result1.success) {
                println("✓ Success! Using primary configuration with hardware acceleration")
                println("==========================================")
                return RecoveryResult(
                    success = true,
                    mediaPlayerComponent = createPlayerComponent(options1),
                    configurationUsed = ConfigurationType.PRIMARY_WITH_HW_ACCEL,
                    attempts = attempts
                )
            }
            attemptNumber++
        } else {
            println("  Skipped: Hardware acceleration not supported (${hwAccelSupport.reason})")
            attemptNumber++
        }
        
        // Strategy 2: Primary configuration without hardware acceleration
        println("[$attemptNumber] Attempting: Primary configuration without hardware acceleration")
        val primaryOptions = VideoOutputConfiguration.getPlatformVideoOptions()
        val result2 = tryInitialization(
            primaryOptions,
            ConfigurationType.PRIMARY_WITHOUT_HW_ACCEL,
            attemptNumber
        )
        attempts.add(result2)
        
        if (result2.success) {
            println("✓ Success! Using primary configuration with software decoding")
            println("==========================================")
            return RecoveryResult(
                success = true,
                mediaPlayerComponent = createPlayerComponent(primaryOptions),
                configurationUsed = ConfigurationType.PRIMARY_WITHOUT_HW_ACCEL,
                attempts = attempts
            )
        }
        attemptNumber++
        
        // Strategy 3: Fallback configuration with hardware acceleration
        if (hwAccelSupport.isSupported) {
            println("[$attemptNumber] Attempting: Fallback configuration with hardware acceleration")
            val fallbackOptions = VideoOutputConfiguration.getFallbackVideoOptions()
            val hwAccelOptions = HardwareAccelerationDetector.getHardwareAccelerationOptions(hwAccelSupport)
            val options3 = fallbackOptions + hwAccelOptions
            
            val result3 = tryInitialization(
                options3,
                ConfigurationType.FALLBACK_WITH_HW_ACCEL,
                attemptNumber
            )
            attempts.add(result3)
            
            if (result3.success) {
                println("✓ Success! Using fallback configuration with hardware acceleration")
                println("==========================================")
                return RecoveryResult(
                    success = true,
                    mediaPlayerComponent = createPlayerComponent(options3),
                    configurationUsed = ConfigurationType.FALLBACK_WITH_HW_ACCEL,
                    attempts = attempts
                )
            }
            attemptNumber++
        }
        
        // Strategy 4: Fallback configuration without hardware acceleration
        println("[$attemptNumber] Attempting: Fallback configuration without hardware acceleration")
        val fallbackOptions = VideoOutputConfiguration.getFallbackVideoOptions()
        val result4 = tryInitialization(
            fallbackOptions,
            ConfigurationType.FALLBACK_WITHOUT_HW_ACCEL,
            attemptNumber
        )
        attempts.add(result4)
        
        if (result4.success) {
            println("✓ Success! Using fallback configuration with software decoding")
            println("==========================================")
            return RecoveryResult(
                success = true,
                mediaPlayerComponent = createPlayerComponent(fallbackOptions),
                configurationUsed = ConfigurationType.FALLBACK_WITHOUT_HW_ACCEL,
                attempts = attempts
            )
        }
        attemptNumber++
        
        // Strategy 5: Minimal OpenGL configuration (last resort)
        println("[$attemptNumber] Attempting: Minimal OpenGL configuration (last resort)")
        val minimalOptions = arrayOf("--vout=opengl")
        val result5 = tryInitialization(
            minimalOptions,
            ConfigurationType.MINIMAL_OPENGL,
            attemptNumber
        )
        attempts.add(result5)
        
        if (result5.success) {
            println("✓ Success! Using minimal OpenGL configuration")
            println("⚠️  Warning: Using minimal configuration may have limited functionality")
            println("==========================================")
            return RecoveryResult(
                success = true,
                mediaPlayerComponent = createPlayerComponent(minimalOptions),
                configurationUsed = ConfigurationType.MINIMAL_OPENGL,
                attempts = attempts
            )
        }
        
        // All strategies failed
        println("✗ All recovery strategies failed")
        println("==========================================")
        
        val errorMessage = generateFailureReport(attempts)
        
        return RecoveryResult(
            success = false,
            mediaPlayerComponent = null,
            configurationUsed = null,
            attempts = attempts,
            finalErrorMessage = errorMessage
        )
    }
    
    /**
     * 尝试使用指定选项初始化播放器
     */
    private fun tryInitialization(
        options: Array<String>,
        configType: ConfigurationType,
        attemptNumber: Int
    ): ConfigurationAttempt {
        println("  Options: ${options.joinToString(", ")}")
        
        return try {
            // Try to create the component
            val component = EmbeddedMediaPlayerComponent(*options)
            
            // Verify the component is usable
            val mediaPlayer = component.mediaPlayer()
            if (mediaPlayer == null) {
                throw Exception("Media player instance is null")
            }
            
            // Component created successfully
            component.release() // Release test component
            println("  Result: ✓ Success")
            
            ConfigurationAttempt(
                attemptNumber = attemptNumber,
                configType = configType,
                options = options,
                success = true
            )
        } catch (e: Exception) {
            println("  Result: ✗ Failed - ${e.message}")
            
            ConfigurationAttempt(
                attemptNumber = attemptNumber,
                configType = configType,
                options = options,
                success = false,
                errorMessage = e.message
            )
        }
    }
    
    /**
     * 创建播放器组件（用于成功的配置）
     */
    private fun createPlayerComponent(options: Array<String>): EmbeddedMediaPlayerComponent {
        return EmbeddedMediaPlayerComponent(*options)
    }
    
    /**
     * 生成详细的失败报告
     * 
     * Validates: Requirements 3.4, 4.4
     */
    private fun generateFailureReport(attempts: List<ConfigurationAttempt>): String {
        return buildString {
            appendLine("无法初始化VLC视频播放器")
            appendLine()
            appendLine("尝试了 ${attempts.size} 种配置，全部失败:")
            appendLine()
            
            attempts.forEach { attempt ->
                appendLine("尝试 ${attempt.attemptNumber}: ${getConfigTypeDescription(attempt.configType)}")
                appendLine("  选项: ${attempt.options.joinToString(", ")}")
                appendLine("  结果: 失败")
                if (attempt.errorMessage != null) {
                    appendLine("  错误: ${attempt.errorMessage}")
                }
                appendLine()
            }
            
            appendLine("可能的原因:")
            appendLine("  1. VLC Media Player 未正确安装或版本不兼容")
            appendLine("  2. 系统图形驱动存在问题")
            appendLine("  3. 缺少必要的系统库或依赖")
            appendLine("  4. 操作系统权限限制")
            appendLine()
            appendLine("建议的解决方案:")
            appendLine("  1. 重新安装 VLC Media Player (推荐版本 3.0.x)")
            appendLine("  2. 更新系统图形驱动程序")
            appendLine("  3. 检查系统日志以获取更多错误信息")
            appendLine("  4. 尝试以管理员权限运行应用程序")
            appendLine("  5. 确认系统满足最低要求")
            appendLine()
            appendLine("系统信息:")
            appendLine(VideoOutputConfiguration.getPlatformInfo().prependIndent("  "))
        }
    }
    
    /**
     * 获取配置类型的描述
     */
    private fun getConfigTypeDescription(configType: ConfigurationType): String {
        return when (configType) {
            ConfigurationType.PRIMARY_WITH_HW_ACCEL -> "主要配置 + 硬件加速"
            ConfigurationType.PRIMARY_WITHOUT_HW_ACCEL -> "主要配置 + 软件解码"
            ConfigurationType.FALLBACK_WITH_HW_ACCEL -> "备用配置 + 硬件加速"
            ConfigurationType.FALLBACK_WITHOUT_HW_ACCEL -> "备用配置 + 软件解码"
            ConfigurationType.MINIMAL_OPENGL -> "最小OpenGL配置"
        }
    }
    
    /**
     * 生成配置尝试的摘要报告
     * 
     * 用于日志记录和用户反馈
     */
    fun generateAttemptsSummary(attempts: List<ConfigurationAttempt>): String {
        return buildString {
            appendLine("配置尝试摘要:")
            appendLine("总尝试次数: ${attempts.size}")
            appendLine("成功次数: ${attempts.count { it.success }}")
            appendLine("失败次数: ${attempts.count { !it.success }}")
            appendLine()
            appendLine("详细记录:")
            attempts.forEach { attempt ->
                val status = if (attempt.success) "✓" else "✗"
                appendLine("  $status 尝试 ${attempt.attemptNumber}: ${getConfigTypeDescription(attempt.configType)}")
            }
        }
    }
}
