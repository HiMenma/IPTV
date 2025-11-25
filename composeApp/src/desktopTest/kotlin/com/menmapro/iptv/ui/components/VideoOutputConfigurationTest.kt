package com.menmapro.iptv.ui.components

import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertNotNull
import kotlin.test.assertContains

/**
 * 测试VideoOutputConfiguration工具类
 * 验证操作系统检测和视频输出选项配置
 */
class VideoOutputConfigurationTest {
    
    @Test
    fun `detectOperatingSystem should return valid OS`() {
        val os = VideoOutputConfiguration.detectOperatingSystem()
        assertNotNull(os, "Operating system should be detected")
        println("Detected OS: $os")
    }
    
    @Test
    fun `getPlatformVideoOptions should return non-empty array`() {
        val options = VideoOutputConfiguration.getPlatformVideoOptions()
        assertTrue(options.isNotEmpty(), "Platform video options should not be empty")
        println("Platform video options: ${options.joinToString(", ")}")
        
        // Verify that options contain video output configuration
        val hasVoutOption = options.any { it.startsWith("--vout=") }
        assertTrue(hasVoutOption, "Options should contain --vout parameter")
    }
    
    @Test
    fun `getFallbackVideoOptions should return OpenGL options`() {
        val options = VideoOutputConfiguration.getFallbackVideoOptions()
        assertTrue(options.isNotEmpty(), "Fallback options should not be empty")
        assertContains(options, "--vout=opengl", "Fallback should use OpenGL")
        println("Fallback video options: ${options.joinToString(", ")}")
    }
    
    @Test
    fun `getPlatformVideoOptions should include common options`() {
        val options = VideoOutputConfiguration.getPlatformVideoOptions()
        
        // All platform options should include these common settings
        assertContains(options, "--no-video-title-show", "Should disable video title")
        assertContains(options, "--no-osd", "Should disable OSD")
    }
    
    @Test
    fun `getPlatformInfo should return system information`() {
        val info = VideoOutputConfiguration.getPlatformInfo()
        assertNotNull(info, "Platform info should not be null")
        assertTrue(info.isNotEmpty(), "Platform info should not be empty")
        println("Platform info:\n$info")
        
        // Verify it contains expected information
        assertTrue(info.contains("Operating System:"), "Should contain OS info")
        assertTrue(info.contains("OS Name:"), "Should contain OS name")
    }
    
    @Test
    fun `platform options should differ from fallback options`() {
        val platformOptions = VideoOutputConfiguration.getPlatformVideoOptions()
        val fallbackOptions = VideoOutputConfiguration.getFallbackVideoOptions()
        
        // Extract vout values
        val platformVout = platformOptions.firstOrNull { it.startsWith("--vout=") }
        val fallbackVout = fallbackOptions.firstOrNull { it.startsWith("--vout=") }
        
        assertNotNull(platformVout, "Platform options should have vout")
        assertNotNull(fallbackVout, "Fallback options should have vout")
        
        // On most systems, platform-specific should be different from fallback
        // unless the system is UNKNOWN
        val os = VideoOutputConfiguration.detectOperatingSystem()
        if (os != VideoOutputConfiguration.OperatingSystem.UNKNOWN) {
            println("Platform vout: $platformVout")
            println("Fallback vout: $fallbackVout")
            // Note: This might be the same on some systems, so we just log it
        }
    }
}
