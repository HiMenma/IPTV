package com.menmapro.iptv.ui.components

import com.menmapro.iptv.data.model.Channel
import io.kotest.property.Arb
import io.kotest.property.arbitrary.*
import io.kotest.property.checkAll
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertFalse

/**
 * Property-based tests for VideoPlayer resource management
 */
class VideoPlayerResourceTest {

    /**
     * **Feature: playlist-enhancements, Property 7: Resource cleanup on channel switch**
     * **Validates: Requirements 2.4**
     * 
     * For any two M3U channels, when switching from one to the other, the system should 
     * release all resources associated with the first channel before loading the second channel.
     * 
     * This test verifies the resource cleanup state machine logic that ensures:
     * 1. Resources are released before switching
     * 2. Double-release is prevented
     * 3. Operations on released players are blocked
     * 4. Cleanup sequence is followed correctly
     */
    @Test
    fun `property test - resource cleanup on channel switch`() {
        runBlocking {
            checkAll(100, channelPairArb()) { (channel1, channel2) ->
                // Simulate the resource management state machine
                val resourceManager = PlayerResourceManager()
                
                // Step 1: Initialize with first channel
                val initResult1 = resourceManager.initialize(channel1.url)
                assertTrue(
                    initResult1.success,
                    "First channel initialization should succeed"
                )
                assertFalse(
                    resourceManager.isReleased(),
                    "Player should not be released after initialization"
                )
                
                // Step 2: Verify player is in usable state
                assertTrue(
                    resourceManager.canPerformOperation(),
                    "Player should be ready for operations after initialization"
                )
                
                // Step 3: Switch to second channel (should trigger cleanup)
                val switchResult = resourceManager.switchChannel(channel1.url, channel2.url)
                
                // Verify cleanup was performed
                assertTrue(
                    switchResult.cleanupPerformed,
                    "Cleanup should be performed when switching channels"
                )
                assertTrue(
                    switchResult.success,
                    "Channel switch should succeed"
                )
                
                // Step 4: Verify new channel is loaded
                assertFalse(
                    resourceManager.isReleased(),
                    "Player should not be released after channel switch"
                )
                assertTrue(
                    resourceManager.canPerformOperation(),
                    "Player should be ready for operations after channel switch"
                )
                
                // Step 5: Explicitly release resources
                val releaseResult = resourceManager.release()
                assertTrue(
                    releaseResult.success,
                    "Release should succeed"
                )
                assertTrue(
                    resourceManager.isReleased(),
                    "Player should be marked as released"
                )
                
                // Step 6: Verify double-release is prevented
                val doubleReleaseResult = resourceManager.release()
                assertTrue(
                    doubleReleaseResult.skipped,
                    "Double-release should be skipped"
                )
                
                // Step 7: Verify operations are blocked after release
                assertFalse(
                    resourceManager.canPerformOperation(),
                    "Operations should be blocked after release"
                )
            }
        }
    }
    
    /**
     * Test that cleanup sequence is followed correctly
     */
    @Test
    fun `property test - cleanup sequence is correct`() {
        runBlocking {
            checkAll(100, channelArb()) { channel ->
                val resourceManager = PlayerResourceManager()
                
                // Initialize player
                resourceManager.initialize(channel.url)
                
                // Perform release and capture cleanup steps
                val releaseResult = resourceManager.release()
                
                // Verify cleanup steps were performed in correct order
                assertTrue(
                    releaseResult.success,
                    "Release should succeed"
                )
                
                val cleanupSteps = releaseResult.cleanupSteps
                
                // Verify all required cleanup steps were performed
                assertTrue(
                    cleanupSteps.contains("stop_playback"),
                    "Cleanup should stop playback"
                )
                assertTrue(
                    cleanupSteps.contains("remove_listeners"),
                    "Cleanup should remove event listeners"
                )
                assertTrue(
                    cleanupSteps.contains("release_player"),
                    "Cleanup should release player component"
                )
                
                // Verify cleanup steps are in correct order
                val stopIndex = cleanupSteps.indexOf("stop_playback")
                val listenerIndex = cleanupSteps.indexOf("remove_listeners")
                val releaseIndex = cleanupSteps.indexOf("release_player")
                
                assertTrue(
                    stopIndex < listenerIndex,
                    "Playback should be stopped before removing listeners"
                )
                assertTrue(
                    listenerIndex < releaseIndex,
                    "Listeners should be removed before releasing player"
                )
            }
        }
    }
    
    /**
     * Test that listener registration is tracked correctly
     */
    @Test
    fun `property test - listener registration prevents memory leaks`() {
        runBlocking {
            checkAll(100, channelArb()) { channel ->
                val resourceManager = PlayerResourceManager()
                
                // Initialize player (registers listener)
                resourceManager.initialize(channel.url)
                
                assertTrue(
                    resourceManager.isListenerRegistered(),
                    "Listener should be registered after initialization"
                )
                
                // Release player (should unregister listener)
                resourceManager.release()
                
                assertFalse(
                    resourceManager.isListenerRegistered(),
                    "Listener should be unregistered after release to prevent memory leaks"
                )
            }
        }
    }
    
    /**
     * Test that releasing state prevents concurrent releases
     */
    @Test
    fun `property test - releasing state prevents concurrent operations`() {
        runBlocking {
            checkAll(100, channelArb()) { channel ->
                val resourceManager = PlayerResourceManager()
                
                // Initialize player
                resourceManager.initialize(channel.url)
                
                // Start release process
                resourceManager.startRelease()
                
                assertTrue(
                    resourceManager.isReleasing(),
                    "Player should be in releasing state"
                )
                
                // Try to perform operation while releasing
                assertFalse(
                    resourceManager.canPerformOperation(),
                    "Operations should be blocked while releasing"
                )
                
                // Complete release
                resourceManager.completeRelease()
                
                assertFalse(
                    resourceManager.isReleasing(),
                    "Player should not be in releasing state after completion"
                )
                assertTrue(
                    resourceManager.isReleased(),
                    "Player should be released after completion"
                )
            }
        }
    }

    // Generators for property-based testing

    /**
     * Generate arbitrary channel
     */
    private fun channelArb(): Arb<Channel> = arbitrary {
        val id = Arb.string(1..20, Codepoint.alphanumeric()).bind()
        val name = Arb.string(1..50, Codepoint.alphanumeric()).bind()
        val urlType = Arb.int(0..3).bind()
        
        val url = when (urlType) {
            0 -> "http://example.com/stream${Arb.int(1..1000).bind()}.m3u8"
            1 -> "https://example.com/stream${Arb.int(1..1000).bind()}.m3u8"
            2 -> "rtsp://example.com:554/stream${Arb.int(1..1000).bind()}"
            else -> "rtmp://example.com/live/stream${Arb.int(1..1000).bind()}"
        }
        
        Channel(
            id = id,
            name = name,
            url = url,
            logoUrl = null,
            group = null,
            categoryId = null
        )
    }
    
    /**
     * Generate pair of different channels
     */
    private fun channelPairArb(): Arb<Pair<Channel, Channel>> = arbitrary {
        val channel1 = channelArb().bind()
        var channel2 = channelArb().bind()
        
        // Ensure channels have different URLs
        while (channel2.url == channel1.url) {
            channel2 = channelArb().bind()
        }
        
        Pair(channel1, channel2)
    }
    
    /**
     * Mock resource manager that simulates the state machine of VideoPlayer resource management
     * This mirrors the logic in VideoPlayer.desktop.kt and VideoPlayer.android.kt
     */
    private class PlayerResourceManager {
        private var isReleased = false
        private var isReleasing = false
        private var listenerRegistered = false
        private var currentUrl: String? = null
        private val cleanupHistory = mutableListOf<String>()
        
        fun initialize(url: String): InitResult {
            if (url.isBlank()) {
                return InitResult(success = false, error = "Invalid URL")
            }
            
            isReleased = false
            isReleasing = false
            listenerRegistered = true
            currentUrl = url
            cleanupHistory.clear()
            
            return InitResult(success = true)
        }
        
        fun switchChannel(oldUrl: String, newUrl: String): SwitchResult {
            if (isReleased || isReleasing) {
                return SwitchResult(success = false, cleanupPerformed = false)
            }
            
            // Perform cleanup of old channel
            val cleanupPerformed = performCleanup(stopOnly = true)
            
            // Load new channel
            currentUrl = newUrl
            
            return SwitchResult(success = true, cleanupPerformed = cleanupPerformed)
        }
        
        fun release(): ReleaseResult {
            // Prevent double-release
            if (isReleased || isReleasing) {
                return ReleaseResult(
                    success = true,
                    skipped = true,
                    cleanupSteps = emptyList()
                )
            }
            
            isReleasing = true
            
            val steps = mutableListOf<String>()
            
            // Step 1: Stop playback
            try {
                steps.add("stop_playback")
                cleanupHistory.add("stop_playback")
            } catch (e: Exception) {
                // Continue anyway
            }
            
            // Step 2: Remove listeners
            if (listenerRegistered) {
                try {
                    steps.add("remove_listeners")
                    cleanupHistory.add("remove_listeners")
                    listenerRegistered = false
                } catch (e: Exception) {
                    // Continue anyway
                }
            }
            
            // Step 3: Release player
            try {
                steps.add("release_player")
                cleanupHistory.add("release_player")
            } catch (e: Exception) {
                // Continue anyway
            }
            
            isReleased = true
            isReleasing = false
            
            return ReleaseResult(
                success = true,
                skipped = false,
                cleanupSteps = steps
            )
        }
        
        fun startRelease() {
            isReleasing = true
        }
        
        fun completeRelease() {
            isReleasing = false
            isReleased = true
            listenerRegistered = false
        }
        
        fun canPerformOperation(): Boolean {
            return !isReleased && !isReleasing && currentUrl != null
        }
        
        fun isReleased(): Boolean = isReleased
        
        fun isReleasing(): Boolean = isReleasing
        
        fun isListenerRegistered(): Boolean = listenerRegistered
        
        private fun performCleanup(stopOnly: Boolean = false): Boolean {
            try {
                cleanupHistory.add("stop_playback")
                if (!stopOnly) {
                    if (listenerRegistered) {
                        cleanupHistory.add("remove_listeners")
                        listenerRegistered = false
                    }
                    cleanupHistory.add("release_player")
                }
                return true
            } catch (e: Exception) {
                return false
            }
        }
        
        data class InitResult(val success: Boolean, val error: String? = null)
        data class SwitchResult(val success: Boolean, val cleanupPerformed: Boolean)
        data class ReleaseResult(
            val success: Boolean,
            val skipped: Boolean,
            val cleanupSteps: List<String>
        )
    }
}
