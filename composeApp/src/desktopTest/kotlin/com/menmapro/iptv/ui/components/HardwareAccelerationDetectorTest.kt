package com.menmapro.iptv.ui.components

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain

/**
 * Tests for HardwareAccelerationDetector
 * 
 * Validates: Requirements 2.3
 */
class HardwareAccelerationDetectorTest : StringSpec({
    
    "detectHardwareAcceleration should return valid support information" {
        val support = HardwareAccelerationDetector.detectHardwareAcceleration()
        
        // Should always return a valid result
        support shouldNotBe null
        support.reason shouldNotBe ""
    }
    
    "getHardwareAccelerationOptions should return valid VLC options" {
        val support = HardwareAccelerationDetector.detectHardwareAcceleration()
        val options = HardwareAccelerationDetector.getHardwareAccelerationOptions(support)
        
        // Should return at least one option
        options.size shouldBe 1
        
        // Option should start with --avcodec-hw=
        options[0] shouldContain "--avcodec-hw="
    }
    
    "getHardwareAccelerationInfo should return descriptive information" {
        val support = HardwareAccelerationDetector.detectHardwareAcceleration()
        val info = HardwareAccelerationDetector.getHardwareAccelerationInfo(support)
        
        // Should contain key information
        info shouldContain "Hardware Acceleration Status:"
        info shouldContain "Supported:"
        info shouldContain "Type:"
        info shouldContain "Reason:"
    }
    
    "isHardwareAccelerationEnabled should detect enabled hardware acceleration" {
        val enabledOptions = arrayOf("--avcodec-hw=any", "--vout=opengl")
        val isEnabled = HardwareAccelerationDetector.isHardwareAccelerationEnabled(enabledOptions)
        
        isEnabled shouldBe true
    }
    
    "isHardwareAccelerationEnabled should detect disabled hardware acceleration" {
        val disabledOptions = arrayOf("--avcodec-hw=none", "--vout=opengl")
        val isEnabled = HardwareAccelerationDetector.isHardwareAccelerationEnabled(disabledOptions)
        
        isEnabled shouldBe false
    }
    
    "isHardwareAccelerationEnabled should return false when no hw option present" {
        val noHwOptions = arrayOf("--vout=opengl", "--no-osd")
        val isEnabled = HardwareAccelerationDetector.isHardwareAccelerationEnabled(noHwOptions)
        
        isEnabled shouldBe false
    }
    
    "hardware acceleration should be supported on macOS" {
        val os = VideoOutputConfiguration.detectOperatingSystem()
        
        if (os == VideoOutputConfiguration.OperatingSystem.MACOS) {
            val support = HardwareAccelerationDetector.detectHardwareAcceleration()
            
            support.isSupported shouldBe true
            support.accelerationType shouldBe HardwareAccelerationDetector.AccelerationType.VIDEOTOOLBOX
        }
    }
    
    "hardware acceleration should be supported on Linux" {
        val os = VideoOutputConfiguration.detectOperatingSystem()
        
        if (os == VideoOutputConfiguration.OperatingSystem.LINUX) {
            val support = HardwareAccelerationDetector.detectHardwareAcceleration()
            
            support.isSupported shouldBe true
            support.accelerationType shouldBe HardwareAccelerationDetector.AccelerationType.AUTO
        }
    }
    
    "hardware acceleration should be supported on Windows" {
        val os = VideoOutputConfiguration.detectOperatingSystem()
        
        if (os == VideoOutputConfiguration.OperatingSystem.WINDOWS) {
            val support = HardwareAccelerationDetector.detectHardwareAcceleration()
            
            support.isSupported shouldBe true
            support.accelerationType shouldBe HardwareAccelerationDetector.AccelerationType.AUTO
        }
    }
})
