package com.menmapro.iptv.player.ffmpeg

import org.bytedeco.javacv.FFmpegFrameGrabber
import org.bytedeco.javacv.Java2DFrameConverter
import org.bytedeco.ffmpeg.global.avcodec

/**
 * 测试类，用于验证 JavaCV 和 FFmpeg 依赖是否正确配置
 */
object FFmpegTest {
    /**
     * 验证 FFmpeg 库是否可用
     */
    fun isFFmpegAvailable(): Boolean {
        return try {
            // 尝试访问 FFmpeg 常量
            val codecId = avcodec.AV_CODEC_ID_H264
            codecId > 0
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 获取 FFmpeg 版本信息
     */
    fun getFFmpegVersion(): String {
        return try {
            "FFmpeg ${org.bytedeco.ffmpeg.global.avutil.av_version_info()}"
        } catch (e: Exception) {
            "Unknown"
        }
    }
}
