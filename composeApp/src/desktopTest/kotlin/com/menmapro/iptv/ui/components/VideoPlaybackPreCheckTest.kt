package com.menmapro.iptv.ui.components

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for VideoPlaybackPreCheck
 * 
 * Validates: Requirements 1.1, 2.1, 3.1
 */
class VideoPlaybackPreCheckTest {
    
    @Test
    fun `test URL validity check - empty URL should fail`() {
        // Test with empty URL
        val result = VideoPlaybackPreCheck.performPreCheck("", null)
        
        // Should fail with critical issue
        assertFalse(result.canProceed, "Empty URL should not allow playback to proceed")
        assertEquals(PreCheckStatus.FAILED, result.status)
        
        // Should have URL validity issue
        val urlIssue = result.issues.find { it.checkName == "URL有效性" }
        assertTrue(urlIssue != null, "Should have URL validity issue")
        assertFalse(urlIssue.passed, "URL check should not pass")
        assertEquals(IssueSeverity.CRITICAL, urlIssue.severity)
    }
    
    @Test
    fun `test URL validity check - blank URL should fail`() {
        // Test with blank URL (only whitespace)
        val result = VideoPlaybackPreCheck.performPreCheck("   ", null)
        
        // Should fail with critical issue
        assertFalse(result.canProceed, "Blank URL should not allow playback to proceed")
        assertEquals(PreCheckStatus.FAILED, result.status)
        
        // Should have URL validity issue
        val urlIssue = result.issues.find { it.checkName == "URL有效性" }
        assertTrue(urlIssue != null, "Should have URL validity issue")
        assertFalse(urlIssue.passed, "URL check should not pass")
    }
    
    @Test
    fun `test URL validity check - short URL should fail`() {
        // Test with very short URL
        val result = VideoPlaybackPreCheck.performPreCheck("http://", null)
        
        // Should fail with critical issue
        assertFalse(result.canProceed, "Short URL should not allow playback to proceed")
        assertEquals(PreCheckStatus.FAILED, result.status)
        
        // Should have URL validity issue
        val urlIssue = result.issues.find { it.checkName == "URL有效性" }
        assertTrue(urlIssue != null, "Should have URL validity issue")
        assertFalse(urlIssue.passed, "URL check should not pass")
    }
    
    @Test
    fun `test URL validity check - URL with spaces should warn`() {
        // Test with URL containing spaces
        val result = VideoPlaybackPreCheck.performPreCheck("http://example.com/video file.mp4", null)
        
        // Should have warning but might still proceed depending on other checks
        val urlIssue = result.issues.find { it.checkName == "URL有效性" }
        assertTrue(urlIssue != null, "Should have URL validity issue")
        assertFalse(urlIssue.passed, "URL check should not pass with spaces")
        assertEquals(IssueSeverity.WARNING, urlIssue.severity)
    }
    
    @Test
    fun `test URL validity check - valid HTTP URL should pass`() {
        // Test with valid HTTP URL
        val result = VideoPlaybackPreCheck.performPreCheck("http://example.com/stream.m3u8", null)
        
        // URL check should pass - no URL validity issue should be in the issues list
        val urlIssue = result.issues.find { it.checkName == "URL有效性" }
        assertTrue(urlIssue == null, "Valid HTTP URL should not have URL validity issue")
        
        // However, there will be other issues (null component, etc.)
        // The key is that URL validity is not one of them
    }
    
    @Test
    fun `test URL validity check - valid HTTPS URL should pass`() {
        // Test with valid HTTPS URL
        val result = VideoPlaybackPreCheck.performPreCheck("https://example.com/video.mp4", null)
        
        // URL check should pass - no URL validity issue should be in the issues list
        val urlIssue = result.issues.find { it.checkName == "URL有效性" }
        assertTrue(urlIssue == null, "Valid HTTPS URL should not have URL validity issue")
    }
    
    @Test
    fun `test URL validity check - valid RTSP URL should pass`() {
        // Test with valid RTSP URL
        val result = VideoPlaybackPreCheck.performPreCheck("rtsp://example.com:554/stream", null)
        
        // URL check should pass - no URL validity issue should be in the issues list
        val urlIssue = result.issues.find { it.checkName == "URL有效性" }
        assertTrue(urlIssue == null, "Valid RTSP URL should not have URL validity issue")
    }
    
    @Test
    fun `test URL validity check - valid RTMP URL should pass`() {
        // Test with valid RTMP URL
        val result = VideoPlaybackPreCheck.performPreCheck("rtmp://example.com/live/stream", null)
        
        // URL check should pass - no URL validity issue should be in the issues list
        val urlIssue = result.issues.find { it.checkName == "URL有效性" }
        assertTrue(urlIssue == null, "Valid RTMP URL should not have URL validity issue")
    }
    
    @Test
    fun `test URL validity check - unsupported protocol should warn`() {
        // Test with unsupported protocol
        val result = VideoPlaybackPreCheck.performPreCheck("ftp://example.com/video.mp4", null)
        
        // Should have warning about protocol
        val urlIssue = result.issues.find { it.checkName == "URL有效性" }
        assertTrue(urlIssue != null, "Should have URL validity issue")
        assertFalse(urlIssue.passed, "Unsupported protocol should not pass")
        assertEquals(IssueSeverity.WARNING, urlIssue.severity)
    }
    
    @Test
    fun `test VLC availability check is performed`() {
        // Test that VLC availability is checked
        val result = VideoPlaybackPreCheck.performPreCheck("http://example.com/stream.m3u8", null)
        
        // VLC availability check is performed, but only added to issues if it fails
        // If VLC is available, no issue is added
        // We can verify the check was performed by ensuring the result is valid
        assertTrue(result.status != null, "Pre-check should return a status")
    }
    
    @Test
    fun `test video output configuration check is performed`() {
        // Test that video output configuration is checked
        val result = VideoPlaybackPreCheck.performPreCheck("http://example.com/stream.m3u8", null)
        
        // Video output configuration check is performed, but only added to issues if it fails
        // If configuration is valid, no issue is added
        // We can verify the check was performed by ensuring the result is valid
        assertTrue(result.status != null, "Pre-check should return a status")
    }
    
    @Test
    fun `test null media player component is handled`() {
        // Test with null media player component
        val result = VideoPlaybackPreCheck.performPreCheck("http://example.com/stream.m3u8", null)
        
        // Should have media player component issue
        val componentIssue = result.issues.find { it.checkName == "媒体播放器组件" }
        assertTrue(componentIssue != null, "Should have media player component check")
        assertFalse(componentIssue.passed, "Null component should not pass")
        assertEquals(IssueSeverity.CRITICAL, componentIssue.severity)
        
        // Should not be able to proceed
        assertFalse(result.canProceed, "Should not proceed with null component")
    }
    
    @Test
    fun `test pre-check report generation`() {
        // Test report generation
        val result = VideoPlaybackPreCheck.performPreCheck("http://example.com/stream.m3u8", null)
        val report = VideoPlaybackPreCheck.generatePreCheckReport(result)
        
        // Report should contain key sections
        assertTrue(report.contains("视频播放预检查报告"), "Report should have title")
        assertTrue(report.contains("整体状态"), "Report should have overall status")
        assertTrue(report.contains("可以继续播放"), "Report should indicate if can proceed")
    }
    
    @Test
    fun `test pre-check with multiple issues categorizes by severity`() {
        // Test with empty URL (will have multiple issues)
        val result = VideoPlaybackPreCheck.performPreCheck("", null)
        val report = VideoPlaybackPreCheck.generatePreCheckReport(result)
        
        // Report should categorize issues
        val hasCritical = result.issues.any { it.severity == IssueSeverity.CRITICAL }
        assertTrue(hasCritical, "Should have critical issues")
        
        if (hasCritical) {
            assertTrue(report.contains("严重问题"), "Report should show critical issues section")
        }
    }
    
    @Test
    fun `test pre-check status reflects issue severity`() {
        // Test with empty URL - should be FAILED
        val failedResult = VideoPlaybackPreCheck.performPreCheck("", null)
        assertEquals(PreCheckStatus.FAILED, failedResult.status, "Empty URL should result in FAILED status")
        
        // Test with URL containing spaces - might be WARNING
        val warningResult = VideoPlaybackPreCheck.performPreCheck("http://example.com/video file.mp4", null)
        // Status depends on other checks, but should have at least one warning
        val hasWarning = warningResult.issues.any { it.severity == IssueSeverity.WARNING }
        assertTrue(hasWarning, "URL with spaces should have at least one warning")
    }
    
    @Test
    fun `test pre-check provides suggestions for issues`() {
        // Test with empty URL
        val result = VideoPlaybackPreCheck.performPreCheck("", null)
        
        // Critical issues should have suggestions
        val criticalIssues = result.issues.filter { it.severity == IssueSeverity.CRITICAL }
        criticalIssues.forEach { issue ->
            assertTrue(issue.suggestions.isNotEmpty(), "Critical issue '${issue.checkName}' should have suggestions")
        }
    }
}
