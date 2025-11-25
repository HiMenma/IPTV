package com.menmapro.iptv.ui.components

import uk.co.caprica.vlcj.player.component.EmbeddedMediaPlayerComponent
import java.awt.Dimension

/**
 * Utility class for validating video surface initialization and configuration
 * 
 * This validator ensures that the video rendering surface is properly initialized,
 * visible, and has valid dimensions before attempting to play video content.
 * 
 * Requirements: 3.1, 3.2
 */
object VideoSurfaceValidator {
    
    /**
     * Validate video surface is correctly initialized
     * 
     * Performs comprehensive validation of the video surface including:
     * - Component initialization check
     * - Video surface component existence
     * - Visibility verification
     * - Dimension validation
     * 
     * @param component The EmbeddedMediaPlayerComponent to validate
     * @return ValidationResult containing validation status, issues, and suggestions
     */
    fun validateVideoSurface(component: EmbeddedMediaPlayerComponent): ValidationResult {
        val issues = mutableListOf<String>()
        val suggestions = mutableListOf<String>()
        
        try {
            // Check if component is initialized
            if (!isComponentInitialized(component)) {
                issues.add("EmbeddedMediaPlayerComponent 未正确初始化")
                suggestions.add("确保在创建组件时提供了正确的VLC选项")
                return ValidationResult(
                    isValid = false,
                    issues = issues,
                    suggestions = suggestions
                )
            }
            
            // Get video surface component
            val videoSurface = try {
                component.videoSurfaceComponent()
            } catch (e: Exception) {
                issues.add("无法获取视频表面组件: ${e.message}")
                suggestions.add("检查VLC库是否正确加载")
                return ValidationResult(
                    isValid = false,
                    issues = issues,
                    suggestions = suggestions
                )
            }
            
            if (videoSurface == null) {
                issues.add("视频表面组件为空")
                suggestions.add("重新初始化媒体播放器组件")
                return ValidationResult(
                    isValid = false,
                    issues = issues,
                    suggestions = suggestions
                )
            }
            
            // Check visibility
            if (!isVideoSurfaceVisible(component)) {
                issues.add("视频表面不可见")
                suggestions.add("设置视频表面可见性: videoSurfaceComponent().isVisible = true")
            }
            
            // Check dimensions
            val dimensions = getVideoSurfaceDimensions(component)
            if (dimensions == null) {
                issues.add("无法获取视频表面尺寸")
                suggestions.add("确保视频表面已添加到布局中")
            } else if (dimensions.width <= 0 || dimensions.height <= 0) {
                issues.add("视频表面尺寸无效: ${dimensions.width}x${dimensions.height}")
                suggestions.add("设置有效的视频表面尺寸: videoSurfaceComponent().setSize(width, height)")
            }
            
            // Check if video surface is displayable
            if (!videoSurface.isDisplayable) {
                issues.add("视频表面不可显示 (未添加到显示层次结构)")
                suggestions.add("确保视频表面已正确添加到Swing容器中")
            }
            
            // Check if video surface is enabled
            if (!videoSurface.isEnabled) {
                issues.add("视频表面未启用")
                suggestions.add("启用视频表面: videoSurfaceComponent().isEnabled = true")
            }
            
            // Validation passed if no issues found
            val isValid = issues.isEmpty()
            
            if (isValid) {
                suggestions.add("视频表面验证通过，可以开始播放")
            }
            
            return ValidationResult(
                isValid = isValid,
                issues = issues,
                suggestions = suggestions
            )
            
        } catch (e: Exception) {
            issues.add("验证过程中发生异常: ${e.message}")
            suggestions.add("检查VLC库是否正确安装和加载")
            return ValidationResult(
                isValid = false,
                issues = issues,
                suggestions = suggestions
            )
        }
    }
    
    /**
     * Check if video surface is visible
     * 
     * @param component The EmbeddedMediaPlayerComponent to check
     * @return true if video surface is visible, false otherwise
     */
    fun isVideoSurfaceVisible(component: EmbeddedMediaPlayerComponent): Boolean {
        return try {
            val videoSurface = component.videoSurfaceComponent()
            videoSurface?.isVisible == true
        } catch (e: Exception) {
            println("Error checking video surface visibility: ${e.message}")
            false
        }
    }
    
    /**
     * Get video surface dimensions
     * 
     * @param component The EmbeddedMediaPlayerComponent to query
     * @return Dimension object with width and height, or null if unavailable
     */
    fun getVideoSurfaceDimensions(component: EmbeddedMediaPlayerComponent): Dimension? {
        return try {
            val videoSurface = component.videoSurfaceComponent()
            videoSurface?.size
        } catch (e: Exception) {
            println("Error getting video surface dimensions: ${e.message}")
            null
        }
    }
    
    /**
     * Check if the EmbeddedMediaPlayerComponent is properly initialized
     * 
     * @param component The component to check
     * @return true if initialized, false otherwise
     */
    private fun isComponentInitialized(component: EmbeddedMediaPlayerComponent): Boolean {
        return try {
            // Try to access the media player to verify initialization
            val mediaPlayer = component.mediaPlayer()
            mediaPlayer != null
        } catch (e: Exception) {
            println("Component initialization check failed: ${e.message}")
            false
        }
    }
}

/**
 * Result of video surface validation
 * 
 * @property isValid Whether the video surface passed all validation checks
 * @property issues List of issues found during validation
 * @property suggestions List of suggestions to fix the issues
 */
data class ValidationResult(
    val isValid: Boolean,
    val issues: List<String>,
    val suggestions: List<String>
)
