package com.menmapro.iptv.player.ffmpeg

import java.util.concurrent.atomic.AtomicLong

/**
 * 音视频同步器
 * 
 * 管理音视频同步，确保视频帧与音频时钟保持同步。
 * 使用音频时钟作为主时钟，视频帧根据时间戳调整渲染时机。
 * 
 * Requirements:
 * - 6.1: 保持音视频时间戳同步
 * - 6.2: 检测到不同步时自动调整播放速度
 * - 6.3: 音频快于视频时丢弃视频帧
 * - 6.4: 视频快于音频时延迟视频渲染
 * - 6.5: 同步误差超过阈值时记录警告
 */
class AudioVideoSynchronizer(
    private val audioClock: AudioClock
) {
    /**
     * 同步阈值（毫秒）
     * 在此范围内的时间差被认为是同步的
     */
    private val syncThreshold = 40L // 40ms
    
    /**
     * 最大同步差异（毫秒）
     * 超过此值将触发丢帧或其他恢复机制
     */
    private val maxSyncDiff = 1000L // 1 second
    
    /**
     * 严重不同步阈值（毫秒）
     * 超过此值将记录警告
     */
    private val severeSyncThreshold = 200L // 200ms
    
    /**
     * 累计同步误差（毫秒）
     * 用于监控长期同步质量
     */
    private val cumulativeSyncError = AtomicLong(0)
    
    /**
     * 同步误差样本计数
     */
    private val syncErrorSampleCount = AtomicLong(0)
    
    /**
     * 丢帧计数
     */
    private val droppedFrameCount = AtomicLong(0)
    
    /**
     * 延迟帧计数
     */
    private val delayedFrameCount = AtomicLong(0)
    
    /**
     * 计算视频帧应该延迟的时间
     * 
     * 根据视频时间戳和音频时钟计算视频帧应该延迟多久才能渲染。
     * 
     * @param videoTimestamp 视频帧的时间戳（微秒）
     * @return 应该延迟的时间（毫秒），0 表示立即渲染
     * 
     * Requirements: 6.1, 6.4
     */
    fun calculateVideoDelay(videoTimestamp: Long): Long {
        if (!audioClock.isStarted()) {
            return 0L
        }
        
        val audioTime = audioClock.getTime()
        val diff = videoTimestamp - audioTime
        val diffMs = diff / 1000 // 转换为毫秒
        
        // 更新同步误差统计
        updateSyncStatistics(diffMs)
        
        return when {
            diffMs > syncThreshold -> {
                // 视频慢了（时间戳大于音频），需要等待
                delayedFrameCount.incrementAndGet()
                diffMs.coerceAtMost(maxSyncDiff)
            }
            diffMs < -syncThreshold -> {
                // 视频快了（时间戳小于音频），立即显示
                0L
            }
            else -> {
                // 在同步范围内
                0L
            }
        }
    }
    
    /**
     * 判断是否需要丢帧
     * 
     * 当视频严重落后于音频时，返回 true 表示应该丢弃该帧以追赶进度。
     * 
     * @param videoTimestamp 视频帧的时间戳（微秒）
     * @return true 如果应该丢弃该帧
     * 
     * Requirements: 6.2, 6.3
     */
    fun shouldDropFrame(videoTimestamp: Long): Boolean {
        if (!audioClock.isStarted()) {
            return false
        }
        
        val audioTime = audioClock.getTime()
        val diff = audioTime - videoTimestamp
        val diffMs = diff / 1000 // 转换为毫秒
        
        val shouldDrop = diffMs > maxSyncDiff
        
        if (shouldDrop) {
            droppedFrameCount.incrementAndGet()
            println("[AudioVideoSync] 丢帧: 视频落后 ${diffMs}ms (视频: ${videoTimestamp/1000}ms, 音频: ${audioTime/1000}ms)")
        }
        
        return shouldDrop
    }
    
    /**
     * 获取当前同步误差
     * 
     * 计算视频时间戳与音频时钟之间的差异。
     * 
     * @param videoTimestamp 视频帧的时间戳（微秒）
     * @return 同步误差（毫秒），正值表示视频慢，负值表示视频快
     */
    fun getSyncError(videoTimestamp: Long): Long {
        if (!audioClock.isStarted()) {
            return 0L
        }
        
        val audioTime = audioClock.getTime()
        val diff = videoTimestamp - audioTime
        return diff / 1000 // 转换为毫秒
    }
    
    /**
     * 检查同步误差是否超过阈值
     * 
     * @param videoTimestamp 视频帧的时间戳（微秒）
     * @return true 如果同步误差超过严重阈值
     * 
     * Requirements: 6.5
     */
    fun isSyncErrorSevere(videoTimestamp: Long): Boolean {
        val error = Math.abs(getSyncError(videoTimestamp))
        
        if (error > severeSyncThreshold) {
            println("[AudioVideoSync] 警告: 同步误差超过阈值 ${error}ms (阈值: ${severeSyncThreshold}ms)")
            return true
        }
        
        return false
    }
    
    /**
     * 更新同步统计信息
     * 
     * @param syncErrorMs 同步误差（毫秒）
     */
    private fun updateSyncStatistics(syncErrorMs: Long) {
        val absError = Math.abs(syncErrorMs)
        cumulativeSyncError.addAndGet(absError)
        syncErrorSampleCount.incrementAndGet()
    }
    
    /**
     * 获取平均同步误差
     * 
     * @return 平均同步误差（毫秒）
     */
    fun getAverageSyncError(): Double {
        val count = syncErrorSampleCount.get()
        return if (count > 0) {
            cumulativeSyncError.get().toDouble() / count
        } else {
            0.0
        }
    }
    
    /**
     * 获取丢帧计数
     * 
     * @return 丢帧总数
     */
    fun getDroppedFrameCount(): Long {
        return droppedFrameCount.get()
    }
    
    /**
     * 获取延迟帧计数
     * 
     * @return 延迟帧总数
     */
    fun getDelayedFrameCount(): Long {
        return delayedFrameCount.get()
    }
    
    /**
     * 获取同步阈值
     * 
     * @return 同步阈值（毫秒）
     */
    fun getSyncThreshold(): Long {
        return syncThreshold
    }
    
    /**
     * 获取最大同步差异
     * 
     * @return 最大同步差异（毫秒）
     */
    fun getMaxSyncDiff(): Long {
        return maxSyncDiff
    }
    
    /**
     * 重置同步统计
     * 
     * 清除所有统计数据，用于新的播放会话
     */
    fun resetStatistics() {
        cumulativeSyncError.set(0)
        syncErrorSampleCount.set(0)
        droppedFrameCount.set(0)
        delayedFrameCount.set(0)
    }
    
    /**
     * 生成同步诊断报告
     * 
     * @return 包含同步统计信息的字符串
     */
    fun generateDiagnosticReport(): String {
        return buildString {
            appendLine("=== 音视频同步诊断报告 ===")
            appendLine("同步阈值: ${syncThreshold}ms")
            appendLine("最大同步差异: ${maxSyncDiff}ms")
            appendLine("严重不同步阈值: ${severeSyncThreshold}ms")
            appendLine()
            appendLine("统计信息:")
            appendLine("  平均同步误差: ${"%.2f".format(getAverageSyncError())}ms")
            appendLine("  丢帧总数: ${getDroppedFrameCount()}")
            appendLine("  延迟帧总数: ${getDelayedFrameCount()}")
            appendLine("  同步误差样本数: ${syncErrorSampleCount.get()}")
            appendLine("============================")
        }
    }
}
