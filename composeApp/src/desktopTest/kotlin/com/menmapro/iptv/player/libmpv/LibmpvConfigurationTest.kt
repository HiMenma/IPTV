package com.menmapro.iptv.player.libmpv

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for LibmpvConfiguration
 * 
 * Requirements:
 * - 1.3: Support hardware acceleration through libmpv
 * - 7.1: Support configuring hardware acceleration options
 * - 7.2: Support configuring network buffering parameters
 * - 7.3: Support configuring audio output options
 * - 7.4: Support configuring video output options
 * - 7.5: Use safe default values when configuration is invalid
 */
class LibmpvConfigurationTest {
    
    /**
     * Test default configuration values
     */
    @Test
    fun testDefaultConfiguration() {
        val config = LibmpvConfiguration.DEFAULT
        
        assertTrue(config.hardwareAcceleration, "Hardware acceleration should be enabled by default")
        assertEquals("auto", config.hwdecMethod, "Hardware decoding method should be auto")
        assertEquals("gpu", config.videoOutput, "Video output should be gpu")
        assertEquals("auto", config.audioOutput, "Audio output should be auto")
        assertEquals(150000, config.cacheSize, "Cache size should be 150000 KB")
        assertEquals(10, config.cacheSecs, "Cache seconds should be 10")
        assertEquals(5, config.demuxerReadahead, "Demuxer readahead should be 5")
        assertEquals(30, config.networkTimeout, "Network timeout should be 30")
        assertEquals(100, config.volume, "Volume should be 100")
        assertEquals("info", config.logLevel, "Log level should be info")
    }
    
    /**
     * Test live streaming configuration
     */
    @Test
    fun testLiveStreamingConfiguration() {
        val config = LibmpvConfiguration.LIVE_STREAMING
        
        assertEquals(50000, config.cacheSize, "Live streaming should have smaller cache")
        assertEquals(5, config.cacheSecs, "Live streaming should have shorter cache duration")
        assertEquals(2, config.demuxerReadahead, "Live streaming should have shorter readahead")
        assertEquals(15, config.networkTimeout, "Live streaming should have shorter timeout")
    }
    
    /**
     * Test VOD configuration
     */
    @Test
    fun testVODConfiguration() {
        val config = LibmpvConfiguration.VOD
        
        assertEquals(300000, config.cacheSize, "VOD should have larger cache")
        assertEquals(20, config.cacheSecs, "VOD should have longer cache duration")
        assertEquals(10, config.demuxerReadahead, "VOD should have longer readahead")
        assertEquals(60, config.networkTimeout, "VOD should have longer timeout")
    }
    
    /**
     * Test software-only configuration
     */
    @Test
    fun testSoftwareOnlyConfiguration() {
        val config = LibmpvConfiguration.SOFTWARE_ONLY
        
        assertFalse(config.hardwareAcceleration, "Hardware acceleration should be disabled")
        assertEquals("no", config.hwdecMethod, "Hardware decoding should be disabled")
    }
    
    /**
     * Test platform-specific configurations
     */
    @Test
    fun testPlatformSpecificConfigurations() {
        val macosConfig = LibmpvConfiguration.MACOS
        assertEquals("videotoolbox", macosConfig.hwdecMethod, "macOS should use VideoToolbox")
        assertEquals("coreaudio", macosConfig.audioOutput, "macOS should use CoreAudio")
        
        val linuxVaapiConfig = LibmpvConfiguration.LINUX_VAAPI
        assertEquals("vaapi", linuxVaapiConfig.hwdecMethod, "Linux should support VAAPI")
        assertEquals("pulse", linuxVaapiConfig.audioOutput, "Linux should use PulseAudio")
        
        val linuxVdpauConfig = LibmpvConfiguration.LINUX_VDPAU
        assertEquals("vdpau", linuxVdpauConfig.hwdecMethod, "Linux should support VDPAU")
        
        val windowsConfig = LibmpvConfiguration.WINDOWS
        assertEquals("d3d11va", windowsConfig.hwdecMethod, "Windows should use D3D11VA")
        assertEquals("wasapi", windowsConfig.audioOutput, "Windows should use WASAPI")
    }
    
    /**
     * Test configuration validation - cache size
     */
    @Test
    fun testValidationCacheSize() {
        // Test too small cache size
        val tooSmall = LibmpvConfiguration(cacheSize = 100).validate()
        assertEquals(1000, tooSmall.cacheSize, "Cache size should be clamped to minimum 1000")
        
        // Test too large cache size
        val tooLarge = LibmpvConfiguration(cacheSize = 2000000).validate()
        assertEquals(1000000, tooLarge.cacheSize, "Cache size should be clamped to maximum 1000000")
        
        // Test valid cache size
        val valid = LibmpvConfiguration(cacheSize = 50000).validate()
        assertEquals(50000, valid.cacheSize, "Valid cache size should not be changed")
    }
    
    /**
     * Test configuration validation - cache seconds
     */
    @Test
    fun testValidationCacheSeconds() {
        val tooSmall = LibmpvConfiguration(cacheSecs = 0).validate()
        assertEquals(1, tooSmall.cacheSecs, "Cache seconds should be clamped to minimum 1")
        
        val tooLarge = LibmpvConfiguration(cacheSecs = 500).validate()
        assertEquals(300, tooLarge.cacheSecs, "Cache seconds should be clamped to maximum 300")
    }
    
    /**
     * Test configuration validation - demuxer readahead
     */
    @Test
    fun testValidationDemuxerReadahead() {
        val tooSmall = LibmpvConfiguration(demuxerReadahead = 0).validate()
        assertEquals(1, tooSmall.demuxerReadahead, "Demuxer readahead should be clamped to minimum 1")
        
        val tooLarge = LibmpvConfiguration(demuxerReadahead = 100).validate()
        assertEquals(60, tooLarge.demuxerReadahead, "Demuxer readahead should be clamped to maximum 60")
    }
    
    /**
     * Test configuration validation - network timeout
     */
    @Test
    fun testValidationNetworkTimeout() {
        val tooSmall = LibmpvConfiguration(networkTimeout = 1).validate()
        assertEquals(5, tooSmall.networkTimeout, "Network timeout should be clamped to minimum 5")
        
        val tooLarge = LibmpvConfiguration(networkTimeout = 500).validate()
        assertEquals(300, tooLarge.networkTimeout, "Network timeout should be clamped to maximum 300")
    }
    
    /**
     * Test configuration validation - volume
     */
    @Test
    fun testValidationVolume() {
        val tooSmall = LibmpvConfiguration(volume = -10).validate()
        assertEquals(0, tooSmall.volume, "Volume should be clamped to minimum 0")
        
        val tooLarge = LibmpvConfiguration(volume = 150).validate()
        assertEquals(100, tooLarge.volume, "Volume should be clamped to maximum 100")
    }
    
    /**
     * Test configuration validation - hwdec method
     */
    @Test
    fun testValidationHwdecMethod() {
        // Test invalid hwdec method with hardware acceleration enabled
        val invalid = LibmpvConfiguration(
            hardwareAcceleration = true,
            hwdecMethod = "invalid"
        ).validate()
        assertEquals("auto", invalid.hwdecMethod, "Invalid hwdec method should default to auto")
        
        // Test valid hwdec methods
        val validMethods = listOf("auto", "videotoolbox", "vaapi", "vdpau", "d3d11va", "no")
        validMethods.forEach { method ->
            val config = LibmpvConfiguration(hwdecMethod = method).validate()
            assertEquals(method, config.hwdecMethod, "Valid hwdec method $method should not be changed")
        }
        
        // Test that hwdec is set to "no" when hardware acceleration is disabled
        val disabled = LibmpvConfiguration(
            hardwareAcceleration = false,
            hwdecMethod = "auto"
        ).validate()
        assertEquals("no", disabled.hwdecMethod, "hwdec should be 'no' when hardware acceleration is disabled")
    }
    
    /**
     * Test configuration validation - log level
     */
    @Test
    fun testValidationLogLevel() {
        // Test invalid log level
        val invalid = LibmpvConfiguration(logLevel = "invalid").validate()
        assertEquals("info", invalid.logLevel, "Invalid log level should default to info")
        
        // Test valid log levels
        val validLevels = listOf("no", "fatal", "error", "warn", "info", "v", "debug", "trace")
        validLevels.forEach { level ->
            val config = LibmpvConfiguration(logLevel = level).validate()
            assertEquals(level, config.logLevel, "Valid log level $level should not be changed")
        }
    }
    
    /**
     * Test forCurrentPlatform method
     */
    @Test
    fun testForCurrentPlatform() {
        val config = LibmpvConfiguration.DEFAULT.forCurrentPlatform()
        
        val osName = System.getProperty("os.name").lowercase()
        
        when {
            osName.contains("mac") -> {
                assertEquals("videotoolbox", config.hwdecMethod, "macOS should use VideoToolbox")
                assertEquals("coreaudio", config.audioOutput, "macOS should use CoreAudio")
            }
            osName.contains("windows") -> {
                assertEquals("d3d11va", config.hwdecMethod, "Windows should use D3D11VA")
                assertEquals("wasapi", config.audioOutput, "Windows should use WASAPI")
            }
            osName.contains("linux") -> {
                assertEquals("vaapi", config.hwdecMethod, "Linux should use VAAPI")
                assertEquals("pulse", config.audioOutput, "Linux should use PulseAudio")
            }
        }
    }
    
    /**
     * Test that configuration is immutable (data class copy)
     */
    @Test
    fun testConfigurationImmutability() {
        val original = LibmpvConfiguration.DEFAULT
        val modified = original.copy(volume = 50)
        
        assertEquals(100, original.volume, "Original configuration should not be modified")
        assertEquals(50, modified.volume, "Modified configuration should have new value")
    }
}
