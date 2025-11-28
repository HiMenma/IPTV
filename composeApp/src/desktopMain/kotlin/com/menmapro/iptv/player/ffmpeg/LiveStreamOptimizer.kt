package com.menmapro.iptv.player.ffmpeg

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * 直播流优化器
 * 
 * 提供直播流的优化功能，包括：
 * - 低延迟缓冲策略
 * - 动态缓冲调整
 * - 延迟累积检测和处理
 * - 自动重连机制
 * 
 * Requirements:
 * - 5.1: 使用低延迟缓冲策略
 * - 5.2: 动态调整缓冲区大小
 * - 5.3: 延迟累积时自动跳帧
 * - 5.4: 连接中断时自动重连
 * - 5.5: 恢复后从最新位置继续播放
 */
class LiveStreamOptimizer(
    private val isLiveStream: Boolean
) {
    
    // ========== 缓冲策略配置 ==========
    
    /**
     * 最小缓冲区大小（帧数）
     */
    private val minBufferSize = 5
    
    /**
     * 最大缓冲区大小（帧数）
     */
    private val maxBufferSize = 30
    
    /**
     * 目标缓冲区大小（帧数）
     * 直播流使用较小的缓冲区以降低延迟
     */
    private val targetBufferSize = if (isLiveStream) 10 else 20
    
    /**
     * 当前推荐的缓冲区大小
     */
    private val currentBufferSize = AtomicInteger(targetBufferSize)
    
    // ========== 延迟累积检测 ==========
    
    /**
     * 延迟累积阈值（毫秒）
     * 超过此值将触发跳帧
     */
    private val latencyThreshold = if (isLiveStream) 3000L else 5000L // 3秒 for live, 5秒 for VOD
    
    /**
     * 当前累积延迟（毫秒）
     */
    private val currentLatency = AtomicLong(0)
    
    /**
     * 上次延迟检查时间
     */
    private var lastLatencyCheckTime = System.currentTimeMillis()
    
    // ========== 网络抖动检测 ==========
    
    /**
     * 网络抖动检测窗口大小
     */
    private val jitterWindowSize = 10
    
    /**
     * 缓冲区大小历史记录
     */
    private val bufferSizeHistory = ArrayDeque<Int>(jitterWindowSize)
    
    /**
     * 网络抖动阈值
     * 缓冲区大小变化超过此值认为存在网络抖动
     */
    private val jitterThreshold = 5
    
    // ========== 重连机制 ==========
    
    /**
     * 是否启用自动重连
     */
    private val autoReconnectEnabled = AtomicBoolean(isLiveStream)
    
    /**
     * 最大重连次数
     */
    private val maxReconnectAttempts = 5
    
    /**
     * 当前重连次数
     */
    private val reconnectAttempts = AtomicInteger(0)
    
    /**
     * 重连基础延迟（毫秒）
     */
    private val baseReconnectDelay = 1000L
    
    /**
     * 上次重连时间
     */
    private var lastReconnectTime = 0L
    
    // ========== 统计信息 ==========
    
    /**
     * 跳帧总数
     */
    private val totalFramesSkipped = AtomicLong(0)
    
    /**
     * 缓冲区调整次数
     */
    private val bufferAdjustmentCount = AtomicLong(0)
    
    /**
     * 重连成功次数
     */
    private val successfulReconnects = AtomicInteger(0)
    
    // ========== 公共方法 ==========
    
    /**
     * 获取推荐的缓冲区大小
     * 
     * 根据当前网络状况和延迟情况返回推荐的缓冲区大小。
     * 
     * @return 推荐的缓冲区大小（帧数）
     * 
     * Requirements: 5.1
     */
    fun getRecommendedBufferSize(): Int {
        return currentBufferSize.get()
    }
    
    /**
     * 更新缓冲区状态
     * 
     * 根据当前缓冲区大小动态调整缓冲策略。
     * 检测网络抖动并相应调整缓冲区大小。
     * 
     * @param currentSize 当前缓冲区中的帧数
     * 
     * Requirements: 5.2
     */
    fun updateBufferStatus(currentSize: Int) {
        // 添加到历史记录
        bufferSizeHistory.addLast(currentSize)
        if (bufferSizeHistory.size > jitterWindowSize) {
            bufferSizeHistory.removeFirst()
        }
        
        // 检测网络抖动
        if (bufferSizeHistory.size >= jitterWindowSize) {
            val hasJitter = detectNetworkJitter()
            
            if (hasJitter) {
                // 检测到网络抖动，增加缓冲区大小
                adjustBufferSize(increase = true)
            } else if (currentSize > targetBufferSize * 1.5) {
                // 缓冲区过大，减小缓冲区大小
                adjustBufferSize(increase = false)
            }
        }
    }
    
    /**
     * 检测网络抖动
     * 
     * 通过分析缓冲区大小的变化来检测网络抖动。
     * 
     * @return true 如果检测到网络抖动
     */
    private fun detectNetworkJitter(): Boolean {
        if (bufferSizeHistory.size < jitterWindowSize) {
            return false
        }
        
        // 计算缓冲区大小的标准差
        val mean = bufferSizeHistory.average()
        val variance = bufferSizeHistory.map { (it - mean) * (it - mean) }.average()
        val stdDev = Math.sqrt(variance)
        
        // 如果标准差超过阈值，认为存在网络抖动
        return stdDev > jitterThreshold
    }
    
    /**
     * 调整缓冲区大小
     * 
     * @param increase true 表示增加缓冲区，false 表示减小缓冲区
     */
    private fun adjustBufferSize(increase: Boolean) {
        val current = currentBufferSize.get()
        val newSize = if (increase) {
            (current + 5).coerceAtMost(maxBufferSize)
        } else {
            (current - 5).coerceAtLeast(minBufferSize)
        }
        
        if (newSize != current) {
            currentBufferSize.set(newSize)
            bufferAdjustmentCount.incrementAndGet()
            println("[LiveStreamOptimizer] 缓冲区大小调整: $current -> $newSize (${if (increase) "增加" else "减小"})")
        }
    }
    
    /**
     * 更新延迟信息
     * 
     * 更新当前累积延迟，用于判断是否需要跳帧。
     * 
     * @param latencyMs 当前延迟（毫秒）
     * 
     * Requirements: 5.3
     */
    fun updateLatency(latencyMs: Long) {
        currentLatency.set(latencyMs)
        lastLatencyCheckTime = System.currentTimeMillis()
    }
    
    /**
     * 检查是否需要跳帧
     * 
     * 当延迟累积超过阈值时，返回 true 表示应该跳帧以追赶进度。
     * 
     * @return true 如果应该跳帧
     * 
     * Requirements: 5.3
     */
    fun shouldSkipFrames(): Boolean {
        if (!isLiveStream) {
            return false
        }
        
        val latency = currentLatency.get()
        val shouldSkip = latency > latencyThreshold
        
        if (shouldSkip) {
            totalFramesSkipped.incrementAndGet()
            println("[LiveStreamOptimizer] 延迟累积超过阈值: ${latency}ms > ${latencyThreshold}ms，触发跳帧")
        }
        
        return shouldSkip
    }
    
    /**
     * 计算需要跳过的帧数
     * 
     * 根据当前延迟计算需要跳过多少帧才能追赶进度。
     * 假设帧率为 25fps。
     * 
     * @return 需要跳过的帧数
     */
    fun calculateFramesToSkip(): Int {
        val latency = currentLatency.get()
        if (latency <= latencyThreshold) {
            return 0
        }
        
        // 计算超出阈值的延迟
        val excessLatency = latency - latencyThreshold
        
        // 假设 25fps，计算需要跳过的帧数
        val framesToSkip = (excessLatency / 40).toInt() // 40ms per frame at 25fps
        
        return framesToSkip.coerceAtMost(10) // 最多跳过 10 帧
    }
    
    /**
     * 检查是否需要重连
     * 
     * 判断是否应该尝试重连。
     * 
     * @return true 如果应该尝试重连
     * 
     * Requirements: 5.4
     */
    fun shouldReconnect(): Boolean {
        if (!autoReconnectEnabled.get()) {
            return false
        }
        
        val attempts = reconnectAttempts.get()
        if (attempts >= maxReconnectAttempts) {
            println("[LiveStreamOptimizer] 已达到最大重连次数: $attempts")
            return false
        }
        
        // 检查是否需要等待（指数退避）
        val now = System.currentTimeMillis()
        val requiredDelay = calculateReconnectDelay(attempts)
        val timeSinceLastReconnect = now - lastReconnectTime
        
        if (timeSinceLastReconnect < requiredDelay) {
            return false
        }
        
        return true
    }
    
    /**
     * 计算重连延迟（指数退避）
     * 
     * @param attemptNumber 重连尝试次数
     * @return 应该等待的延迟（毫秒）
     */
    private fun calculateReconnectDelay(attemptNumber: Int): Long {
        // 指数退避: 1s, 2s, 4s, 8s, 16s
        return baseReconnectDelay * (1 shl attemptNumber.coerceAtMost(4))
    }
    
    /**
     * 记录重连尝试
     * 
     * 增加重连计数并记录时间。
     * 
     * Requirements: 5.4
     */
    fun recordReconnectAttempt() {
        val attempts = reconnectAttempts.incrementAndGet()
        lastReconnectTime = System.currentTimeMillis()
        val delay = calculateReconnectDelay(attempts - 1)
        println("[LiveStreamOptimizer] 重连尝试 #$attempts (延迟: ${delay}ms)")
    }
    
    /**
     * 记录重连成功
     * 
     * 重置重连计数。
     * 
     * Requirements: 5.5
     */
    fun recordReconnectSuccess() {
        val attempts = reconnectAttempts.get()
        reconnectAttempts.set(0)
        successfulReconnects.incrementAndGet()
        println("[LiveStreamOptimizer] 重连成功 (尝试次数: $attempts)")
    }
    
    /**
     * 重置重连状态
     * 
     * 清除重连计数，用于新的播放会话。
     */
    fun resetReconnectState() {
        reconnectAttempts.set(0)
        lastReconnectTime = 0L
    }
    
    /**
     * 启用/禁用自动重连
     * 
     * @param enabled true 表示启用自动重连
     */
    fun setAutoReconnectEnabled(enabled: Boolean) {
        autoReconnectEnabled.set(enabled)
        println("[LiveStreamOptimizer] 自动重连: ${if (enabled) "启用" else "禁用"}")
    }
    
    /**
     * 检查是否为直播流
     * 
     * @return true 如果是直播流
     */
    fun isLiveStream(): Boolean {
        return isLiveStream
    }
    
    /**
     * 获取延迟阈值
     * 
     * @return 延迟阈值（毫秒）
     */
    fun getLatencyThreshold(): Long {
        return latencyThreshold
    }
    
    /**
     * 获取当前延迟
     * 
     * @return 当前延迟（毫秒）
     */
    fun getCurrentLatency(): Long {
        return currentLatency.get()
    }
    
    /**
     * 获取跳帧总数
     * 
     * @return 跳帧总数
     */
    fun getTotalFramesSkipped(): Long {
        return totalFramesSkipped.get()
    }
    
    /**
     * 获取缓冲区调整次数
     * 
     * @return 缓冲区调整次数
     */
    fun getBufferAdjustmentCount(): Long {
        return bufferAdjustmentCount.get()
    }
    
    /**
     * 获取重连尝试次数
     * 
     * @return 当前重连尝试次数
     */
    fun getReconnectAttempts(): Int {
        return reconnectAttempts.get()
    }
    
    /**
     * 获取成功重连次数
     * 
     * @return 成功重连次数
     */
    fun getSuccessfulReconnects(): Int {
        return successfulReconnects.get()
    }
    
    /**
     * 重置统计信息
     */
    fun resetStatistics() {
        totalFramesSkipped.set(0)
        bufferAdjustmentCount.set(0)
        currentLatency.set(0)
        bufferSizeHistory.clear()
        currentBufferSize.set(targetBufferSize)
    }
    
    /**
     * 生成诊断报告
     * 
     * @return 包含优化统计信息的字符串
     */
    fun generateDiagnosticReport(): String {
        return buildString {
            appendLine("=== 直播流优化诊断报告 ===")
            appendLine("流类型: ${if (isLiveStream) "直播流" else "点播"}")
            appendLine("自动重连: ${if (autoReconnectEnabled.get()) "启用" else "禁用"}")
            appendLine()
            
            appendLine("缓冲策略:")
            appendLine("  目标缓冲区大小: $targetBufferSize 帧")
            appendLine("  当前缓冲区大小: ${currentBufferSize.get()} 帧")
            appendLine("  缓冲区范围: $minBufferSize - $maxBufferSize 帧")
            appendLine("  缓冲区调整次数: ${bufferAdjustmentCount.get()}")
            appendLine()
            
            appendLine("延迟管理:")
            appendLine("  延迟阈值: ${latencyThreshold}ms")
            appendLine("  当前延迟: ${currentLatency.get()}ms")
            appendLine("  跳帧总数: ${totalFramesSkipped.get()}")
            appendLine()
            
            appendLine("重连统计:")
            appendLine("  当前重连尝试: ${reconnectAttempts.get()}/${maxReconnectAttempts}")
            appendLine("  成功重连次数: ${successfulReconnects.get()}")
            appendLine()
            
            appendLine("============================")
        }
    }
}
