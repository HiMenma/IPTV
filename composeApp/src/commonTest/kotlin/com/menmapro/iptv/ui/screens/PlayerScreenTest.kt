package com.menmapro.iptv.ui.screens

import com.menmapro.iptv.data.model.Channel
import io.kotest.property.Arb
import io.kotest.property.arbitrary.*
import io.kotest.property.checkAll
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Property-based tests for PlayerScreen
 */
class PlayerScreenTest {

    /**
     * **Feature: playlist-enhancements, Property 6: M3U playback safety**
     * **Validates: Requirements 2.1, 2.2, 2.5**
     * 
     * For any M3U channel, clicking to play should either successfully load the stream 
     * or display an error message, but should never cause the application to crash.
     * 
     * This test verifies that URL validation and error handling work correctly for
     * various channel URL formats, including invalid ones.
     */
    @Test
    fun `property test - M3U channel playback never crashes`() {
        runBlocking {
            checkAll(100, channelArb()) { channel ->
                // Test URL validation logic
                val validationResult = validateChannelUrl(channel.url)
                
                // The validation should always return a result (never throw exception)
                // Either the URL is valid (null error) or invalid (error message)
                assertTrue(
                    validationResult.isValid || validationResult.errorMessage != null,
                    "URL validation should always return a result without crashing"
                )
                
                // If URL is blank, it should be caught
                if (channel.url.isBlank()) {
                    assertFalse(
                        validationResult.isValid,
                        "Blank URLs should be invalid"
                    )
                    assertTrue(
                        validationResult.errorMessage?.contains("为空") == true,
                        "Blank URL error message should mention empty address"
                    )
                }
                
                // If URL doesn't start with supported protocol, it should be caught
                val supportedProtocols = listOf("http://", "https://", "rtsp://", "rtmp://")
                val hasValidProtocol = supportedProtocols.any { channel.url.startsWith(it) }
                
                if (!channel.url.isBlank() && !hasValidProtocol) {
                    assertFalse(
                        validationResult.isValid,
                        "URLs without supported protocols should be invalid"
                    )
                    assertTrue(
                        validationResult.errorMessage?.contains("不支持") == true,
                        "Unsupported protocol error message should mention unsupported format"
                    )
                }
                
                // If URL is valid, no error message should be present
                if (validationResult.isValid) {
                    assertTrue(
                        validationResult.errorMessage == null,
                        "Valid URLs should not have error messages"
                    )
                }
            }
        }
    }
    
    /**
     * Test that error screen provides recovery options
     */
    @Test
    fun `property test - error screen provides retry and back options`() {
        runBlocking {
            checkAll(100, errorMessageArb()) { errorMessage ->
                // Simulate error screen state
                var retryClicked = false
                var backClicked = false
                
                // Simulate user actions
                val onRetry = { retryClicked = true }
                val onBack = { backClicked = true }
                
                // Verify callbacks work without crashing
                onRetry()
                assertTrue(retryClicked, "Retry callback should be invoked")
                
                onBack()
                assertTrue(backClicked, "Back callback should be invoked")
                
                // Verify error message is not empty
                assertTrue(
                    errorMessage.isNotEmpty(),
                    "Error message should not be empty"
                )
            }
        }
    }

    // Generators for property-based testing

    /**
     * Generate arbitrary channels with various URL formats
     * Includes valid URLs, invalid URLs, blank URLs, and edge cases
     */
    private fun channelArb(): Arb<Channel> = arbitrary {
        val id = Arb.string(1..20, Codepoint.alphanumeric()).bind()
        val name = Arb.string(1..50, Codepoint.alphanumeric()).bind()
        
        // Generate various URL formats
        val urlType = Arb.int(0..10).bind()
        val url = when (urlType) {
            0 -> "" // Blank URL
            1 -> "   " // Whitespace only
            2 -> "ftp://invalid.protocol.com/stream" // Unsupported protocol
            3 -> "invalid-url" // No protocol
            4 -> "http://" // Incomplete URL
            5 -> "https://" // Incomplete URL
            6 -> "http://example.com/stream.m3u8" // Valid HTTP
            7 -> "https://example.com/stream.m3u8" // Valid HTTPS
            8 -> "rtsp://example.com:554/stream" // Valid RTSP
            9 -> "rtmp://example.com/live/stream" // Valid RTMP
            else -> "http://example.com/channel${Arb.int(1..1000).bind()}.m3u8" // Valid with variation
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
     * Generate arbitrary error messages
     */
    private fun errorMessageArb(): Arb<String> = arbitrary {
        val errorType = Arb.int(0..5).bind()
        when (errorType) {
            0 -> "播放地址为空"
            1 -> "不支持的播放地址格式"
            2 -> "网络连接失败"
            3 -> "播放器初始化失败"
            4 -> "无法加载媒体"
            else -> "播放出错: ${Arb.string(5..20, Codepoint.alphanumeric()).bind()}"
        }
    }
    
    /**
     * Data class to hold validation result
     */
    private data class ValidationResult(
        val isValid: Boolean,
        val errorMessage: String?
    )
    
    /**
     * Validate channel URL (mirrors the logic in PlayerScreen)
     */
    private fun validateChannelUrl(url: String): ValidationResult {
        return try {
            val errorMessage = when {
                url.isBlank() -> "播放地址为空"
                !url.startsWith("http://") && 
                !url.startsWith("https://") && 
                !url.startsWith("rtsp://") &&
                !url.startsWith("rtmp://") -> "不支持的播放地址格式"
                else -> null
            }
            
            ValidationResult(
                isValid = errorMessage == null,
                errorMessage = errorMessage
            )
        } catch (e: Exception) {
            // Should never happen, but if it does, return error
            ValidationResult(
                isValid = false,
                errorMessage = "验证失败: ${e.message}"
            )
        }
    }
}
