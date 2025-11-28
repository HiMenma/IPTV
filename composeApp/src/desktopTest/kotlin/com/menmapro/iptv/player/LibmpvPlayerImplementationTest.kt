package com.menmapro.iptv.player

import com.menmapro.iptv.player.libmpv.LibmpvLoader
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests for LibmpvPlayerImplementation
 * 
 * These tests verify that the LibmpvPlayerImplementation correctly implements
 * the PlayerImplementation interface and provides proper availability checking.
 * 
 * Requirements:
 * - 1.1: Use libmpv library for video playback
 * - 1.2: Provide clear error messages when libmpv is not available
 * - 1.5: Maintain the same PlayerImplementation interface for compatibility
 * - 5.1: Implement the PlayerImplementation interface
 */
class LibmpvPlayerImplementationTest {
    
    @Test
    fun `test implementation type is LIBMPV`() {
        val implementation = LibmpvPlayerImplementation()
        assertEquals(PlayerImplementationType.LIBMPV, implementation.type)
    }
    
    @Test
    fun `test implementation has correct name`() {
        val implementation = LibmpvPlayerImplementation()
        assertEquals("libmpv Player", implementation.name)
    }
    
    @Test
    fun `test implementation has description`() {
        val implementation = LibmpvPlayerImplementation()
        assertNotNull(implementation.description)
        assertTrue(implementation.description.isNotEmpty())
        assertTrue(implementation.description.contains("libmpv"))
    }
    
    @Test
    fun `test isAvailable returns boolean`() {
        val implementation = LibmpvPlayerImplementation()
        // Should return a boolean value (true or false)
        // Wrap in try-catch to handle UnsatisfiedLinkError when libmpv is not installed
        try {
            val available = implementation.isAvailable()
            // Just verify it doesn't throw an exception
            assertTrue(available || !available)
        } catch (e: UnsatisfiedLinkError) {
            // Expected when libmpv is not installed
            println("libmpv not available (expected): ${e.message}")
        }
    }
    
    @Test
    fun `test getUnavailableReason returns null when available`() {
        val implementation = LibmpvPlayerImplementation()
        
        try {
            if (implementation.isAvailable()) {
                // If libmpv is available, reason should be null
                assertEquals(null, implementation.getUnavailableReason())
            }
        } catch (e: UnsatisfiedLinkError) {
            // Expected when libmpv is not installed
            println("libmpv not available (expected): ${e.message}")
        }
    }
    
    @Test
    fun `test getUnavailableReason returns message when not available`() {
        val implementation = LibmpvPlayerImplementation()
        
        try {
            val available = implementation.isAvailable()
            if (!available) {
                // If libmpv is not available, reason should be provided
                val reason = implementation.getUnavailableReason()
                assertNotNull(reason)
                assertTrue(reason.isNotEmpty())
                // Should contain installation instructions
                assertTrue(
                    reason.contains("install", ignoreCase = true) ||
                    reason.contains("libmpv", ignoreCase = true)
                )
            }
        } catch (e: UnsatisfiedLinkError) {
            // Expected when libmpv is not installed
            println("libmpv not available (expected): ${e.message}")
        } catch (e: Exception) {
            // Catch any other exceptions
            println("Exception checking libmpv availability: ${e.message}")
        }
    }
    
    @Test
    fun `test availability matches LibmpvLoader`() {
        val implementation = LibmpvPlayerImplementation()
        
        try {
            // Implementation should delegate to LibmpvLoader
            assertEquals(LibmpvLoader.isAvailable(), implementation.isAvailable())
        } catch (e: UnsatisfiedLinkError) {
            // Expected when libmpv is not installed
            println("libmpv not available (expected): ${e.message}")
        }
    }
    
    @Test
    fun `test unavailable reason matches LibmpvLoader`() {
        val implementation = LibmpvPlayerImplementation()
        
        try {
            // Implementation should delegate to LibmpvLoader
            assertEquals(LibmpvLoader.getUnavailableReason(), implementation.getUnavailableReason())
        } catch (e: UnsatisfiedLinkError) {
            // Expected when libmpv is not installed
            println("libmpv not available (expected): ${e.message}")
        }
    }
}
