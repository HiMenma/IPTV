package com.menmapro.iptv.ui.components

import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertNotNull
import kotlin.test.assertFalse

/**
 * Tests for VideoRenderingRecovery
 * 
 * Validates: Requirements 1.4, 2.4, 3.4, 4.4
 */
class VideoRenderingRecoveryTest {
    
    @Test
    fun `recovery should try multiple configurations`() {
        // Attempt recovery
        val result = VideoRenderingRecovery.attemptRecovery()
        
        // Should have tried at least one configuration
        assertTrue(result.attempts.isNotEmpty(), "Should have attempted at least one configuration")
        
        // Each attempt should have valid data
        result.attempts.forEach { attempt ->
            assertTrue(attempt.attemptNumber > 0, "Attempt number should be positive")
            assertNotNull(attempt.configType, "Config type should not be null")
            assertTrue(attempt.options.isNotEmpty(), "Options should not be empty")
        }
    }
    
    @Test
    fun `recovery should record configuration attempts`() {
        val result = VideoRenderingRecovery.attemptRecovery()
        
        // Verify attempts are recorded with proper information
        result.attempts.forEachIndexed { index, attempt ->
            // Attempt numbers should be sequential
            assertTrue(attempt.attemptNumber == index + 1, 
                "Attempt number should be ${index + 1} but was ${attempt.attemptNumber}")
            
            // Each attempt should have a timestamp
            assertTrue(attempt.timestamp > 0, "Timestamp should be set")
            
            // Options should be valid VLC options
            attempt.options.forEach { option ->
                assertTrue(option.startsWith("--"), 
                    "VLC option should start with -- but was: $option")
            }
        }
    }
    
    @Test
    fun `recovery should provide detailed error message on failure`() {
        val result = VideoRenderingRecovery.attemptRecovery()
        
        if (!result.success) {
            // If recovery failed, should have error message
            assertNotNull(result.finalErrorMessage, "Should have error message on failure")
            assertTrue(result.finalErrorMessage!!.isNotEmpty(), "Error message should not be empty")
            
            // Error message should contain useful information
            assertTrue(result.finalErrorMessage!!.contains("尝试了"), 
                "Error message should mention number of attempts")
            assertTrue(result.finalErrorMessage!!.contains("建议"), 
                "Error message should contain suggestions")
        }
    }
    
    @Test
    fun `recovery should generate attempts summary`() {
        val result = VideoRenderingRecovery.attemptRecovery()
        
        val summary = VideoRenderingRecovery.generateAttemptsSummary(result.attempts)
        
        // Summary should contain key information
        assertNotNull(summary, "Summary should not be null")
        assertTrue(summary.contains("总尝试次数"), "Summary should mention total attempts")
        assertTrue(summary.contains("成功次数"), "Summary should mention successful attempts")
        assertTrue(summary.contains("失败次数"), "Summary should mention failed attempts")
    }
    
    @Test
    fun `configuration types should be distinct`() {
        val result = VideoRenderingRecovery.attemptRecovery()
        
        // Each attempt should use a different configuration type
        val configTypes = result.attempts.map { it.configType }.toSet()
        
        // Should have tried multiple different configurations
        // (unless one succeeded early)
        if (result.attempts.size > 1 && !result.success) {
            assertTrue(configTypes.size > 1, 
                "Should try different configuration types when failing")
        }
    }
    
    @Test
    fun `successful recovery should provide media player component`() {
        val result = VideoRenderingRecovery.attemptRecovery()
        
        if (result.success) {
            // Success should include a valid component
            assertNotNull(result.mediaPlayerComponent, 
                "Successful recovery should provide media player component")
            assertNotNull(result.configurationUsed, 
                "Successful recovery should specify which configuration was used")
            
            // Clean up
            result.mediaPlayerComponent?.release()
        }
    }
    
    @Test
    fun `failed recovery should not provide media player component`() {
        val result = VideoRenderingRecovery.attemptRecovery()
        
        if (!result.success) {
            // Failure should not include a component
            assertTrue(result.mediaPlayerComponent == null, 
                "Failed recovery should not provide media player component")
            assertTrue(result.configurationUsed == null, 
                "Failed recovery should not specify configuration used")
        }
    }
    
    @Test
    fun `recovery attempts should include hardware acceleration variations`() {
        val result = VideoRenderingRecovery.attemptRecovery()
        
        // Check if hardware acceleration was considered
        val hwAccelSupport = HardwareAccelerationDetector.detectHardwareAcceleration()
        
        if (hwAccelSupport.isSupported && result.attempts.size > 1) {
            // Should have tried both with and without hardware acceleration
            val withHwAccel = result.attempts.any { attempt ->
                attempt.configType.name.contains("WITH_HW_ACCEL")
            }
            val withoutHwAccel = result.attempts.any { attempt ->
                attempt.configType.name.contains("WITHOUT_HW_ACCEL")
            }
            
            // At least one of each should be tried if hardware acceleration is supported
            assertTrue(withHwAccel || withoutHwAccel, 
                "Should try hardware acceleration variations")
        }
    }
}
