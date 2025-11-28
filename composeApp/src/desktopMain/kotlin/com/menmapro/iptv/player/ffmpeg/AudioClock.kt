package com.menmapro.iptv.player.ffmpeg

import java.util.concurrent.atomic.AtomicLong

/**
 * 音频时钟，用于音视频同步
 * 
 * 提供线程安全的时间戳跟踪和更新功能，作为音视频同步的主时钟。
 * 音频时钟是同步的基准，视频帧会根据音频时钟调整渲染时机。
 * 
 * Requirements: 6.1 - 音视频同步
 */
class AudioClock {
    /**
     * 当前音频时间戳（微秒）
     * 使用 AtomicLong 确保线程安全
     */
    private val timestamp: AtomicLong = AtomicLong(0)
    
    /**
     * 时钟启动时间（系统时间，毫秒）
     */
    private var startTime: Long = 0
    
    /**
     * 时钟是否已启动
     */
    @Volatile
    private var isStarted: Boolean = false
    
    /**
     * 启动时钟
     * 记录启动时间，用于计算相对时间
     */
    fun start() {
        startTime = System.currentTimeMillis()
        isStarted = true
    }
    
    /**
     * 更新音频时间戳
     * 
     * @param audioTimestamp 音频帧的时间戳（微秒）
     */
    fun update(audioTimestamp: Long) {
        timestamp.set(audioTimestamp)
    }
    
    /**
     * 获取当前时间
     * 线程安全的时间查询接口
     * 
     * @return 当前音频时间戳（微秒）
     */
    fun getTime(): Long {
        return timestamp.get()
    }
    
    /**
     * 获取当前时间（毫秒）
     * 
     * @return 当前音频时间戳（毫秒）
     */
    fun getTimeMs(): Long {
        return timestamp.get() / 1000
    }
    
    /**
     * 重置时钟
     * 将时间戳归零并重置启动状态
     */
    fun reset() {
        timestamp.set(0)
        startTime = 0
        isStarted = false
    }
    
    /**
     * 检查时钟是否已启动
     * 
     * @return true 如果时钟已启动
     */
    fun isStarted(): Boolean {
        return isStarted
    }
    
    /**
     * 获取自启动以来的经过时间（毫秒）
     * 
     * @return 经过的时间（毫秒）
     */
    fun getElapsedTime(): Long {
        return if (isStarted) {
            System.currentTimeMillis() - startTime
        } else {
            0
        }
    }
}
