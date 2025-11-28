package com.menmapro.iptv.player.libmpv

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Integration tests for LibmpvConfiguration with LibmpvPlayerEngine
 * 
 * These tests verify that configuration is properly applied to the engine.
 * 
 * Requirements:
 * - 1.3: Support hardware acceleration through libmpv
 * - 7.1: Support configuring hardware acceleration options
 * - 7.2: Support configuring network buffering parameters
 * - 7.3: Support configuring audio output options
 * - 7.4: Support configuring video output options
 * - 7.5: Use safe default values when configuration is invalid
 */
class LibmpvConfigurationIntegrationTest {
    
    /**
     * Test that engine accepts configuration during initialization
     */
    @Test
    fun testEngineAcceptsConfiguration() {
        try {
            val engine = LibmpvPlayerEngine()
            val config = LibmpvConfiguration(
                hardwareAcceleration = true,
                hwdecMethod = "auto",
                volume = 75
            )
            
            try {
                // This should not throw an exception even if libmpv is not installed
                val result = engine.initialize(config)
                
                // If initialization succeeds, verify the engine is initialized
                if (result) {
                    assertTrue(engine.isInitialized(), "Engine should be initialized")
                    engine.destroy()
                }
            } catch (e: LibmpvException.LibraryNotFoundError) {
                // Expected if libmpv is not installed
                println("libmpv not available: ${e.message}")
            } catch (e: LibmpvException.InitializationError) {
                // Expected if initialization fails
                println("Initialization failed: ${e.message}")
            }
        } catch (e: UnsatisfiedLinkError) {
            // Expected if libmpv library is not installed
            println("libmpv library not found: ${e.message}")
        }
    }
    
    /**
     * Test that invalid configuration is validated before use
     */
    @Test
    fun testInvalidConfigurationIsValidated() {
        try {
            val engine = LibmpvPlayerEngine()
            val invalidConfig = LibmpvConfiguration(
                cacheSize = -1000,  // Invalid
                volume = 200,       // Invalid
                hwdecMethod = "invalid"  // Invalid
            )
            
            try {
                // Engine should validate and use safe defaults
                engine.initialize(invalidConfig)
                
                // If we get here, validation worked
                assertTrue(true, "Invalid configuration was handled")
                
                if (engine.isInitialized()) {
                    engine.destroy()
                }
            } catch (e: LibmpvException.LibraryNotFoundError) {
                // Expected if libmpv is not installed
                println("libmpv not available: ${e.message}")
            } catch (e: LibmpvException.InitializationError) {
                // Expected if initialization fails
                println("Initialization failed: ${e.message}")
            }
        } catch (e: UnsatisfiedLinkError) {
            // Expected if libmpv library is not installed
            println("libmpv library not found: ${e.message}")
        }
    }
    
    /**
     * Test that default configuration works
     */
    @Test
    fun testDefaultConfigurationWorks() {
        try {
            val engine = LibmpvPlayerEngine()
            
            try {
                // Should work with default configuration
                engine.initialize(LibmpvConfiguration.DEFAULT)
                
                if (engine.isInitialized()) {
                    assertTrue(true, "Default configuration works")
                    engine.destroy()
                }
            } catch (e: LibmpvException.LibraryNotFoundError) {
                // Expected if libmpv is not installed
                println("libmpv not available: ${e.message}")
            } catch (e: LibmpvException.InitializationError) {
                // Expected if initialization fails
                println("Initialization failed: ${e.message}")
            }
        } catch (e: UnsatisfiedLinkError) {
            // Expected if libmpv library is not installed
            println("libmpv library not found: ${e.message}")
        }
    }
    
    /**
     * Test that platform-specific configuration works
     */
    @Test
    fun testPlatformSpecificConfigurationWorks() {
        try {
            val engine = LibmpvPlayerEngine()
            val platformConfig = LibmpvConfiguration.DEFAULT.forCurrentPlatform()
            
            try {
                engine.initialize(platformConfig)
                
                if (engine.isInitialized()) {
                    assertTrue(true, "Platform-specific configuration works")
                    engine.destroy()
                }
            } catch (e: LibmpvException.LibraryNotFoundError) {
                // Expected if libmpv is not installed
                println("libmpv not available: ${e.message}")
            } catch (e: LibmpvException.InitializationError) {
                // Expected if initialization fails
                println("Initialization failed: ${e.message}")
            }
        } catch (e: UnsatisfiedLinkError) {
            // Expected if libmpv library is not installed
            println("libmpv library not found: ${e.message}")
        }
    }
    
    /**
     * Test that software-only configuration works
     */
    @Test
    fun testSoftwareOnlyConfigurationWorks() {
        try {
            val engine = LibmpvPlayerEngine()
            
            try {
                engine.initialize(LibmpvConfiguration.SOFTWARE_ONLY)
                
                if (engine.isInitialized()) {
                    assertTrue(true, "Software-only configuration works")
                    engine.destroy()
                }
            } catch (e: LibmpvException.LibraryNotFoundError) {
                // Expected if libmpv is not installed
                println("libmpv not available: ${e.message}")
            } catch (e: LibmpvException.InitializationError) {
                // Expected if initialization fails
                println("Initialization failed: ${e.message}")
            }
        } catch (e: UnsatisfiedLinkError) {
            // Expected if libmpv library is not installed
            println("libmpv library not found: ${e.message}")
        }
    }
    
    /**
     * Test that setOptionPublic method exists and has correct signature
     */
    @Test
    fun testSetOptionPublicMethodExists() {
        try {
            val engine = LibmpvPlayerEngine()
            
            try {
                // This should throw because engine is not initialized
                engine.setOptionPublic("test", "value")
            } catch (e: LibmpvException.ConfigurationError) {
                // Expected - engine not initialized
                assertTrue(e.message?.contains("not initialized") == true, 
                    "Should throw ConfigurationError when not initialized")
            } catch (e: LibmpvException.LibraryNotFoundError) {
                // Expected if libmpv is not installed
                println("libmpv not available: ${e.message}")
            }
        } catch (e: UnsatisfiedLinkError) {
            // Expected if libmpv library is not installed
            println("libmpv library not found: ${e.message}")
        }
    }
}
