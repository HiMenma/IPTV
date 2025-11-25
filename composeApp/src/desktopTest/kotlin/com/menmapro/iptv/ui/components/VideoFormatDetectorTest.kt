package com.menmapro.iptv.ui.components

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

/**
 * Tests for VideoFormatDetector
 * 
 * Validates: Requirements 1.3, 4.3
 */
class VideoFormatDetectorTest {
    
    @Test
    fun `detectVideoFormat should detect H264 from mp4 extension`() {
        val url = "http://example.com/video.mp4"
        val format = VideoFormatDetector.detectVideoFormat(url)
        assertEquals(VideoFormat.H264, format)
    }
    
    @Test
    fun `detectVideoFormat should detect H265 from hevc extension`() {
        val url = "http://example.com/video.hevc"
        val format = VideoFormatDetector.detectVideoFormat(url)
        assertEquals(VideoFormat.H265, format)
    }
    
    @Test
    fun `detectVideoFormat should detect H265 from mkv with hevc indicator`() {
        val url = "http://example.com/video.hevc.mkv"
        val format = VideoFormatDetector.detectVideoFormat(url)
        assertEquals(VideoFormat.H265, format)
    }
    
    @Test
    fun `detectVideoFormat should detect H264 from mkv without hevc indicator`() {
        val url = "http://example.com/video.mkv"
        val format = VideoFormatDetector.detectVideoFormat(url)
        assertEquals(VideoFormat.H264, format)
    }
    
    @Test
    fun `detectVideoFormat should detect VP8 from webm extension`() {
        val url = "http://example.com/video.webm"
        val format = VideoFormatDetector.detectVideoFormat(url)
        assertEquals(VideoFormat.VP8, format)
    }
    
    @Test
    fun `detectVideoFormat should detect VP9 from webm with vp9 indicator`() {
        val url = "http://example.com/video.vp9.webm"
        val format = VideoFormatDetector.detectVideoFormat(url)
        assertEquals(VideoFormat.VP9, format)
    }
    
    @Test
    fun `detectVideoFormat should detect AV1 from av1 extension`() {
        val url = "http://example.com/video.av1"
        val format = VideoFormatDetector.detectVideoFormat(url)
        assertEquals(VideoFormat.AV1, format)
    }
    
    @Test
    fun `detectVideoFormat should detect MPEG2 from mpg extension`() {
        val url = "http://example.com/video.mpg"
        val format = VideoFormatDetector.detectVideoFormat(url)
        assertEquals(VideoFormat.MPEG2, format)
    }
    
    @Test
    fun `detectVideoFormat should detect H264 from HLS m3u8`() {
        val url = "http://example.com/stream.m3u8"
        val format = VideoFormatDetector.detectVideoFormat(url)
        assertEquals(VideoFormat.H264, format)
    }
    
    @Test
    fun `detectVideoFormat should detect H264 from RTSP stream`() {
        val url = "rtsp://example.com/stream"
        val format = VideoFormatDetector.detectVideoFormat(url)
        assertEquals(VideoFormat.H264, format)
    }
    
    @Test
    fun `detectVideoFormat should return UNKNOWN for unknown extension`() {
        val url = "http://example.com/video.xyz"
        val format = VideoFormatDetector.detectVideoFormat(url)
        assertEquals(VideoFormat.UNKNOWN, format)
    }
    
    @Test
    fun `isLiveStreamUrl should detect RTSP as live stream`() {
        val url = "rtsp://example.com/live"
        assertTrue(VideoFormatDetector.isLiveStreamUrl(url))
    }
    
    @Test
    fun `isLiveStreamUrl should detect RTMP as live stream`() {
        val url = "rtmp://example.com/live"
        assertTrue(VideoFormatDetector.isLiveStreamUrl(url))
    }
    
    @Test
    fun `isLiveStreamUrl should detect m3u8 as live stream`() {
        val url = "http://example.com/stream.m3u8"
        assertTrue(VideoFormatDetector.isLiveStreamUrl(url))
    }
    
    @Test
    fun `isLiveStreamUrl should detect live path pattern as live stream`() {
        val url = "http://example.com/live/stream"
        assertTrue(VideoFormatDetector.isLiveStreamUrl(url))
    }
    
    @Test
    fun `isLiveStreamUrl should not detect mp4 as live stream`() {
        val url = "http://example.com/video.mp4"
        assertFalse(VideoFormatDetector.isLiveStreamUrl(url))
    }
    
    @Test
    fun `detectStreamFormat should detect HLS format`() {
        val url = "http://example.com/stream.m3u8"
        val format = VideoFormatDetector.detectStreamFormat(url)
        assertEquals("HLS (HTTP Live Streaming)", format)
    }
    
    @Test
    fun `detectStreamFormat should detect RTSP format`() {
        val url = "rtsp://example.com/stream"
        val format = VideoFormatDetector.detectStreamFormat(url)
        assertEquals("RTSP (Real Time Streaming Protocol)", format)
    }
    
    @Test
    fun `detectStreamFormat should detect RTMP format`() {
        val url = "rtmp://example.com/stream"
        val format = VideoFormatDetector.detectStreamFormat(url)
        assertEquals("RTMP (Real-Time Messaging Protocol)", format)
    }
    
    @Test
    fun `detectStreamFormat should detect HTTP progressive download`() {
        val url = "http://example.com/video.mp4"
        val format = VideoFormatDetector.detectStreamFormat(url)
        assertEquals("HTTP Progressive Download", format)
    }
    
    @Test
    fun `getFormatSpecificOptions should return H264 options`() {
        val options = VideoFormatDetector.getFormatSpecificOptions(VideoFormat.H264)
        assertTrue(options.isNotEmpty())
        assertTrue(options.contains(":avcodec-skiploopfilter=0"))
        assertTrue(options.contains(":avcodec-skip-frame=0"))
    }
    
    @Test
    fun `getFormatSpecificOptions should return H265 options`() {
        val options = VideoFormatDetector.getFormatSpecificOptions(VideoFormat.H265)
        assertTrue(options.isNotEmpty())
        assertTrue(options.contains(":avcodec-threads=0"))
    }
    
    @Test
    fun `getFormatSpecificOptions should return VP8 options`() {
        val options = VideoFormatDetector.getFormatSpecificOptions(VideoFormat.VP8)
        assertTrue(options.isNotEmpty())
        assertTrue(options.contains(":avcodec-threads=0"))
    }
    
    @Test
    fun `getFormatSpecificOptions should return VP9 options`() {
        val options = VideoFormatDetector.getFormatSpecificOptions(VideoFormat.VP9)
        assertTrue(options.isNotEmpty())
        assertTrue(options.contains(":avcodec-threads=0"))
    }
    
    @Test
    fun `getFormatName should return correct names`() {
        assertEquals("H.264/AVC", VideoFormatDetector.getFormatName(VideoFormat.H264))
        assertEquals("H.265/HEVC", VideoFormatDetector.getFormatName(VideoFormat.H265))
        assertEquals("VP8", VideoFormatDetector.getFormatName(VideoFormat.VP8))
        assertEquals("VP9", VideoFormatDetector.getFormatName(VideoFormat.VP9))
        assertEquals("AV1", VideoFormatDetector.getFormatName(VideoFormat.AV1))
        assertEquals("Unknown", VideoFormatDetector.getFormatName(VideoFormat.UNKNOWN))
    }
    
    @Test
    fun `getFormatDescription should return non-empty descriptions`() {
        VideoFormat.values().forEach { format ->
            val description = VideoFormatDetector.getFormatDescription(format)
            assertTrue(description.isNotEmpty(), "Description for $format should not be empty")
        }
    }
    
    @Test
    fun `detectVideoFormat should handle case insensitivity`() {
        val url1 = "http://example.com/VIDEO.MP4"
        val url2 = "http://example.com/video.MP4"
        val url3 = "http://example.com/VIDEO.mp4"
        
        assertEquals(VideoFormat.H264, VideoFormatDetector.detectVideoFormat(url1))
        assertEquals(VideoFormat.H264, VideoFormatDetector.detectVideoFormat(url2))
        assertEquals(VideoFormat.H264, VideoFormatDetector.detectVideoFormat(url3))
    }
    
    @Test
    fun `detectVideoFormat should handle URLs with query parameters`() {
        val url = "http://example.com/video.mp4?token=abc123&quality=hd"
        val format = VideoFormatDetector.detectVideoFormat(url)
        assertEquals(VideoFormat.H264, format)
    }
    
    @Test
    fun `detectVideoFormat should detect WMV format`() {
        val url = "http://example.com/video.wmv"
        val format = VideoFormatDetector.detectVideoFormat(url)
        assertEquals(VideoFormat.WMV, format)
    }
    
    @Test
    fun `detectVideoFormat should detect FLV format`() {
        val url = "http://example.com/video.flv"
        val format = VideoFormatDetector.detectVideoFormat(url)
        assertEquals(VideoFormat.FLV, format)
    }
    
    @Test
    fun `detectVideoFormat should detect MPEG4 from AVI`() {
        val url = "http://example.com/video.avi"
        val format = VideoFormatDetector.detectVideoFormat(url)
        assertEquals(VideoFormat.MPEG4, format)
    }
}
