package com.menmapro.iptv.ui.components

import uk.co.caprica.vlcj.player.component.EmbeddedMediaPlayerComponent

/**
 * VideoPlaybackPreCheck - 视频播放前预检查工具类
 * 
 * 在开始视频播放之前执行全面的预检查，确保所有必要条件都已满足。
 * 这有助于提前发现问题并提供详细的错误信息，避免播放失败。
 * 
 * Requirements: 1.1, 2.1, 3.1
 */
object VideoPlaybackPreCheck {
    
    /**
     * 执行完整的播放前预检查
     * 
     * 检查项目包括：
     * 1. URL有效性
     * 2. VLC可用性
     * 3. 视频表面就绪状态
     * 4. 视频输出配置有效性
     * 
     * @param url 要播放的媒体URL
     * @param mediaPlayerComponent 媒体播放器组件（可为null）
     * @return PreCheckResult 包含检查结果和详细信息
     */
    fun performPreCheck(
        url: String,
        mediaPlayerComponent: EmbeddedMediaPlayerComponent?
    ): PreCheckResult {
        val issues = mutableListOf<PreCheckIssue>()
        
        // 1. 检查URL有效性
        // Validates: Requirements 1.1
        val urlCheck = checkUrlValidity(url)
        if (!urlCheck.passed) {
            issues.add(urlCheck)
        }
        
        // 2. 检查VLC是否可用
        // Validates: Requirements 2.1
        val vlcCheck = checkVlcAvailability()
        if (!vlcCheck.passed) {
            issues.add(vlcCheck)
        }
        
        // 3. 检查视频表面是否就绪（仅当组件已初始化时）
        // Validates: Requirements 3.1
        if (mediaPlayerComponent != null) {
            val surfaceCheck = checkVideoSurfaceReady(mediaPlayerComponent)
            if (!surfaceCheck.passed) {
                issues.add(surfaceCheck)
            }
        } else {
            issues.add(
                PreCheckIssue(
                    checkName = "媒体播放器组件",
                    passed = false,
                    severity = IssueSeverity.CRITICAL,
                    message = "媒体播放器组件未初始化",
                    details = "EmbeddedMediaPlayerComponent 为 null，无法继续播放",
                    suggestions = listOf(
                        "确保VLC已正确安装",
                        "检查VLC初始化日志",
                        "尝试重启应用程序"
                    )
                )
            )
        }
        
        // 4. 检查视频输出配置是否有效
        // Validates: Requirements 2.1
        val outputConfigCheck = checkVideoOutputConfiguration()
        if (!outputConfigCheck.passed) {
            issues.add(outputConfigCheck)
        }
        
        // 确定整体结果
        val hasCriticalIssues = issues.any { it.severity == IssueSeverity.CRITICAL }
        val hasWarnings = issues.any { it.severity == IssueSeverity.WARNING }
        
        val overallStatus = when {
            hasCriticalIssues -> PreCheckStatus.FAILED
            hasWarnings -> PreCheckStatus.WARNING
            else -> PreCheckStatus.PASSED
        }
        
        return PreCheckResult(
            status = overallStatus,
            issues = issues,
            canProceed = !hasCriticalIssues
        )
    }
    
    /**
     * 检查URL有效性
     * 
     * 验证URL格式、协议和基本结构
     * 
     * @param url 要检查的URL
     * @return PreCheckIssue 检查结果
     */
    private fun checkUrlValidity(url: String): PreCheckIssue {
        // 检查URL是否为空或空白
        if (url.isBlank()) {
            return PreCheckIssue(
                checkName = "URL有效性",
                passed = false,
                severity = IssueSeverity.CRITICAL,
                message = "URL为空或空白",
                details = "提供的媒体URL为空字符串或仅包含空白字符",
                suggestions = listOf(
                    "确保提供了有效的媒体URL",
                    "检查URL来源是否正确",
                    "验证播放列表数据是否完整"
                )
            )
        }
        
        // 检查URL长度
        if (url.length < 10) {
            return PreCheckIssue(
                checkName = "URL有效性",
                passed = false,
                severity = IssueSeverity.CRITICAL,
                message = "URL过短，可能无效",
                details = "URL长度: ${url.length} 字符，这通常表示URL不完整",
                suggestions = listOf(
                    "检查URL是否完整",
                    "确认URL没有被截断"
                )
            )
        }
        
        // 检查URL协议
        val supportedProtocols = listOf("http://", "https://", "rtsp://", "rtmp://", "file://", "mms://", "mmsh://")
        val hasValidProtocol = supportedProtocols.any { url.lowercase().startsWith(it) }
        
        if (!hasValidProtocol) {
            return PreCheckIssue(
                checkName = "URL有效性",
                passed = false,
                severity = IssueSeverity.WARNING,
                message = "URL协议可能不受支持",
                details = "URL: $url\n未检测到标准协议 (http, https, rtsp, rtmp, file, mms, mmsh)",
                suggestions = listOf(
                    "确认URL协议是否正确",
                    "检查VLC是否支持该协议",
                    "尝试使用标准协议的URL"
                )
            )
        }
        
        // 检查URL中是否包含空格（通常表示格式错误）
        if (url.contains(" ")) {
            return PreCheckIssue(
                checkName = "URL有效性",
                passed = false,
                severity = IssueSeverity.WARNING,
                message = "URL包含空格字符",
                details = "URL中包含空格，这可能导致播放失败",
                suggestions = listOf(
                    "检查URL是否需要URL编码",
                    "移除URL中的空格",
                    "使用正确格式的URL"
                )
            )
        }
        
        // URL检查通过
        return PreCheckIssue(
            checkName = "URL有效性",
            passed = true,
            severity = IssueSeverity.INFO,
            message = "URL格式有效",
            details = "URL: $url",
            suggestions = emptyList()
        )
    }
    
    /**
     * 检查VLC可用性
     * 
     * 验证VLC Media Player是否已安装并可用
     * 
     * @return PreCheckIssue 检查结果
     */
    private fun checkVlcAvailability(): PreCheckIssue {
        val isAvailable = VlcAvailabilityChecker.isVlcAvailable()
        
        return if (isAvailable) {
            PreCheckIssue(
                checkName = "VLC可用性",
                passed = true,
                severity = IssueSeverity.INFO,
                message = "VLC Media Player 已安装并可用",
                details = "VLC库已成功加载",
                suggestions = emptyList()
            )
        } else {
            PreCheckIssue(
                checkName = "VLC可用性",
                passed = false,
                severity = IssueSeverity.CRITICAL,
                message = "VLC Media Player 未安装或无法找到",
                details = VlcAvailabilityChecker.getErrorMessage(),
                suggestions = listOf(
                    "按照安装说明安装VLC",
                    "确认VLC安装路径正确",
                    "重启应用程序后重试"
                )
            )
        }
    }
    
    /**
     * 检查视频表面是否就绪
     * 
     * 验证视频渲染表面已正确初始化、可见且具有有效尺寸
     * 
     * @param component 媒体播放器组件
     * @return PreCheckIssue 检查结果
     */
    private fun checkVideoSurfaceReady(component: EmbeddedMediaPlayerComponent): PreCheckIssue {
        val validationResult = VideoSurfaceValidator.validateVideoSurface(component)
        
        return if (validationResult.isValid) {
            val dimensions = VideoSurfaceValidator.getVideoSurfaceDimensions(component)
            PreCheckIssue(
                checkName = "视频表面就绪",
                passed = true,
                severity = IssueSeverity.INFO,
                message = "视频表面已正确初始化",
                details = "尺寸: ${dimensions?.width}x${dimensions?.height}, 可见性: ${VideoSurfaceValidator.isVideoSurfaceVisible(component)}",
                suggestions = emptyList()
            )
        } else {
            PreCheckIssue(
                checkName = "视频表面就绪",
                passed = false,
                severity = IssueSeverity.CRITICAL,
                message = "视频表面未正确初始化",
                details = buildString {
                    appendLine("发现的问题:")
                    validationResult.issues.forEach { issue ->
                        appendLine("  • $issue")
                    }
                },
                suggestions = validationResult.suggestions
            )
        }
    }
    
    /**
     * 检查视频输出配置是否有效
     * 
     * 验证平台特定的视频输出配置是否可用
     * 
     * @return PreCheckIssue 检查结果
     */
    private fun checkVideoOutputConfiguration(): PreCheckIssue {
        return try {
            val os = VideoOutputConfiguration.detectOperatingSystem()
            val primaryOptions = VideoOutputConfiguration.getPlatformVideoOptions()
            val fallbackOptions = VideoOutputConfiguration.getFallbackVideoOptions()
            
            if (os == VideoOutputConfiguration.OperatingSystem.UNKNOWN) {
                PreCheckIssue(
                    checkName = "视频输出配置",
                    passed = true,
                    severity = IssueSeverity.WARNING,
                    message = "操作系统未识别，将使用通用配置",
                    details = "检测到的操作系统: ${System.getProperty("os.name")}\n将使用OpenGL作为备用输出",
                    suggestions = listOf(
                        "确认应用在支持的操作系统上运行",
                        "如果遇到渲染问题，请报告操作系统信息"
                    )
                )
            } else {
                PreCheckIssue(
                    checkName = "视频输出配置",
                    passed = true,
                    severity = IssueSeverity.INFO,
                    message = "视频输出配置有效",
                    details = buildString {
                        appendLine("操作系统: $os")
                        appendLine("主要输出选项: ${primaryOptions.joinToString(", ")}")
                        appendLine("备用输出选项: ${fallbackOptions.joinToString(", ")}")
                    },
                    suggestions = emptyList()
                )
            }
        } catch (e: Exception) {
            PreCheckIssue(
                checkName = "视频输出配置",
                passed = false,
                severity = IssueSeverity.WARNING,
                message = "无法验证视频输出配置",
                details = "错误: ${e.message}",
                suggestions = listOf(
                    "检查系统配置",
                    "确认应用权限正常",
                    "查看详细日志"
                )
            )
        }
    }
    
    /**
     * 生成预检查报告
     * 
     * 创建一个格式化的报告，包含所有检查结果和建议
     * 
     * @param result 预检查结果
     * @return 格式化的报告字符串
     */
    fun generatePreCheckReport(result: PreCheckResult): String {
        return buildString {
            appendLine("=== 视频播放预检查报告 ===")
            appendLine()
            appendLine("整体状态: ${result.status}")
            appendLine("可以继续播放: ${if (result.canProceed) "是" else "否"}")
            appendLine()
            
            if (result.issues.isEmpty()) {
                appendLine("✓ 所有检查通过，可以开始播放")
            } else {
                appendLine("检查结果详情:")
                appendLine()
                
                // 按严重程度分组
                val critical = result.issues.filter { it.severity == IssueSeverity.CRITICAL }
                val warnings = result.issues.filter { it.severity == IssueSeverity.WARNING }
                val info = result.issues.filter { it.severity == IssueSeverity.INFO }
                
                if (critical.isNotEmpty()) {
                    appendLine("❌ 严重问题 (${critical.size}):")
                    critical.forEach { issue ->
                        appendLine()
                        appendLine("  ${issue.checkName}:")
                        appendLine("    消息: ${issue.message}")
                        appendLine("    详情: ${issue.details.prependIndent("      ")}")
                        if (issue.suggestions.isNotEmpty()) {
                            appendLine("    建议:")
                            issue.suggestions.forEach { suggestion ->
                                appendLine("      • $suggestion")
                            }
                        }
                    }
                    appendLine()
                }
                
                if (warnings.isNotEmpty()) {
                    appendLine("⚠️  警告 (${warnings.size}):")
                    warnings.forEach { issue ->
                        appendLine()
                        appendLine("  ${issue.checkName}:")
                        appendLine("    消息: ${issue.message}")
                        appendLine("    详情: ${issue.details.prependIndent("      ")}")
                        if (issue.suggestions.isNotEmpty()) {
                            appendLine("    建议:")
                            issue.suggestions.forEach { suggestion ->
                                appendLine("      • $suggestion")
                            }
                        }
                    }
                    appendLine()
                }
                
                if (info.isNotEmpty()) {
                    appendLine("ℹ️  信息 (${info.size}):")
                    info.forEach { issue ->
                        appendLine("  ✓ ${issue.checkName}: ${issue.message}")
                    }
                    appendLine()
                }
            }
            
            appendLine("============================")
        }
    }
}

/**
 * 预检查结果
 * 
 * @property status 整体检查状态
 * @property issues 发现的问题列表
 * @property canProceed 是否可以继续播放
 */
data class PreCheckResult(
    val status: PreCheckStatus,
    val issues: List<PreCheckIssue>,
    val canProceed: Boolean
)

/**
 * 预检查状态枚举
 */
enum class PreCheckStatus {
    PASSED,   // 所有检查通过
    WARNING,  // 有警告但可以继续
    FAILED    // 有严重问题，不能继续
}

/**
 * 预检查问题
 * 
 * @property checkName 检查项名称
 * @property passed 是否通过
 * @property severity 严重程度
 * @property message 简短消息
 * @property details 详细信息
 * @property suggestions 建议的解决方案
 */
data class PreCheckIssue(
    val checkName: String,
    val passed: Boolean,
    val severity: IssueSeverity,
    val message: String,
    val details: String,
    val suggestions: List<String>
)

/**
 * 问题严重程度枚举
 */
enum class IssueSeverity {
    INFO,      // 信息性，不影响播放
    WARNING,   // 警告，可能影响播放质量
    CRITICAL   // 严重，阻止播放
}
