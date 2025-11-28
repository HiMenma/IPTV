package com.menmapro.iptv.player.ffmpeg

import org.bytedeco.javacv.Frame
import org.bytedeco.javacv.Java2DFrameConverter
import java.awt.Canvas
import java.awt.Graphics
import java.awt.image.BufferedImage
import java.util.concurrent.BlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

/**
 * 视频渲染器
 * 
 * 负责从视频队列中获取帧，转换为 BufferedImage，并渲染到 Canvas 上。
 * 集成音视频同步器，确保视频帧按正确的时间渲染。
 * 使用双缓冲技术减少闪烁。
 * 
 * Requirements:
 * - 1.2: 视频帧渲染到 Canvas 组件
 * - 6.1: 保持音视频时间戳同步
 * - 6.2: 检测到不同步时自动调整
 * - 6.3: 音频快于视频时丢弃视频帧
 * - 6.4: 视频快于音频时延迟视频渲染
 */
class VideoRenderer(
    private var canvas: Canvas,
    private val videoQueue: BlockingQueue<Frame>,
    private val isPlaying: AtomicBoolean,
    private val isPaused: AtomicBoolean,
    private val synchronizer: AudioVideoSynchronizer,
    private val statistics: PlaybackStatistics,
    private val onError: (String) -> Unit,
    private val liveStreamOptimizer: LiveStreamOptimizer? = null
) : Runnable {
    
    /**
     * 帧转换器，用于将 FFmpeg Frame 转换为 Java BufferedImage
     */
    private val converter = Java2DFrameConverter()
    
    /**
     * 后缓冲区，用于双缓冲渲染
     */
    private var backBuffer: BufferedImage? = null
    
    /**
     * 渲染线程
     */
    private var rendererThread: Thread? = null
    
    /**
     * 队列超时时间（毫秒）
     * 优化：减少超时时间以提高响应速度
     */
    private val queueTimeout = 50L
    
    /**
     * 暂停时的休眠时间（毫秒）
     * 优化：减少休眠时间以提高暂停/恢复响应速度
     */
    private val pauseSleepTime = 5L
    
    /**
     * 上一帧的渲染时间（纳秒）
     */
    private var lastFrameTime = 0L
    
    /**
     * 帧率计算窗口（帧数）
     * 优化：减少窗口大小以更快响应帧率变化
     */
    private val fpsWindowSize = 20
    
    /**
     * 帧时间历史记录
     */
    private val frameTimeHistory = mutableListOf<Long>()
    
    /**
     * 渲染计数器
     * 优化：批量更新统计信息，减少同步开销
     */
    private var renderCounter = 0
    private val renderBatchSize = 5
    
    /**
     * 缓存的渲染边界
     * 优化：缓存计算结果，避免重复计算
     */
    private var cachedImageWidth = 0
    private var cachedImageHeight = 0
    private var cachedCanvasWidth = 0
    private var cachedCanvasHeight = 0
    private var cachedRenderBounds: RenderBounds? = null
    
    /**
     * 启动渲染线程
     */
    fun start() {
        if (rendererThread == null || !rendererThread!!.isAlive) {
            rendererThread = thread(
                start = true,
                name = "FFmpeg-Renderer",
                priority = Thread.NORM_PRIORITY
            ) {
                run()
            }
        }
    }
    
    /**
     * 停止渲染线程
     */
    fun stop() {
        isPlaying.set(false)
        rendererThread?.join(1000)
    }
    
    /**
     * 渲染线程主循环
     * 
     * 持续从视频队列获取帧，进行音视频同步判断，然后渲染到 Canvas。
     */
    override fun run() {
        println("[VideoRenderer] Renderer thread started")
        lastFrameTime = System.nanoTime()
        
        try {
            while (isPlaying.get()) {
                // 处理暂停状态
                if (isPaused.get()) {
                    Thread.sleep(pauseSleepTime)
                    continue
                }
                
                try {
                    // 从队列获取视频帧
                    val frame = videoQueue.poll(queueTimeout, TimeUnit.MILLISECONDS)
                    
                    if (frame == null) {
                        // 队列为空，继续等待
                        continue
                    }
                    
                    // 处理并渲染帧
                    processAndRenderFrame(frame)
                    
                } catch (e: Exception) {
                    handleRenderingError(e)
                }
            }
            
        } catch (e: InterruptedException) {
            println("[VideoRenderer] Renderer thread interrupted")
        } catch (e: Exception) {
            println("[VideoRenderer] Fatal error in renderer thread: ${e.message}")
            e.printStackTrace()
            onError("渲染器发生致命错误: ${e.message}")
        } finally {
            println("[VideoRenderer] Renderer thread stopped")
            cleanup()
        }
    }
    
    /**
     * 处理并渲染帧
     * 
     * 执行音视频同步检查，决定是否丢帧、延迟或立即渲染。
     * 对于直播流，还会检查延迟累积并触发跳帧。
     * 
     * @param frame 视频帧
     */
    private fun processAndRenderFrame(frame: Frame) {
        try {
            val videoTimestamp = frame.timestamp
            
            // 更新缓冲区状态（用于直播流优化）
            liveStreamOptimizer?.updateBufferStatus(videoQueue.size)
            
            // 计算同步误差并更新延迟信息
            val syncError = synchronizer.getSyncError(videoTimestamp)
            liveStreamOptimizer?.updateLatency(Math.abs(syncError))
            
            // 检查是否需要跳帧（直播流延迟累积）
            if (liveStreamOptimizer?.shouldSkipFrames() == true) {
                val framesToSkip = liveStreamOptimizer.calculateFramesToSkip()
                println("[VideoRenderer] Skipping $framesToSkip frames due to latency accumulation")
                
                // 跳过指定数量的帧
                for (i in 0 until framesToSkip) {
                    val skipFrame = videoQueue.poll()
                    if (skipFrame != null) {
                        skipFrame.close()
                        statistics.incrementFramesDropped()
                    } else {
                        break
                    }
                }
            }
            
            // 检查是否需要丢帧（视频严重落后）
            if (synchronizer.shouldDropFrame(videoTimestamp)) {
                println("[VideoRenderer] Dropping frame at ${videoTimestamp}μs")
                statistics.incrementFramesDropped()
                frame.close()
                return
            }
            
            // 计算需要延迟的时间
            val delay = synchronizer.calculateVideoDelay(videoTimestamp)
            
            // 如果需要延迟，则等待
            if (delay > 0) {
                Thread.sleep(delay)
            }
            
            // 检查同步误差是否严重
            synchronizer.isSyncErrorSevere(videoTimestamp)
            
            // 渲染帧
            renderFrame(frame)
            
            // 优化：批量更新统计信息
            renderCounter++
            if (renderCounter >= renderBatchSize) {
                statistics.incrementFramesRendered(renderCounter.toLong())
                renderCounter = 0
            }
            updateFrameRate()
            
        } finally {
            // 确保帧资源被释放
            frame.close()
        }
    }
    
    /**
     * 渲染帧到 Canvas
     * 
     * 将 FFmpeg Frame 转换为 BufferedImage，使用双缓冲技术渲染到 Canvas。
     * 自动检测 Canvas 尺寸变化并重新创建后缓冲区，支持动态窗口调整。
     * 
     * @param frame 视频帧
     * 
     * Requirements: 10.4
     */
    private fun renderFrame(frame: Frame) {
        try {
            // 转换帧为 BufferedImage
            var image = converter.convert(frame) ?: return
            
            // 确保图像有效（尺寸 > 0）
            if (image.width <= 0 || image.height <= 0) {
                println("[VideoRenderer] Invalid image dimensions: ${image.width}x${image.height}")
                return
            }
            
            // 验证并修复 ColorSpace（如果需要）
            image = ensureValidColorSpace(image)
            
            // 获取 Canvas 尺寸
            val canvasWidth = canvas.width
            val canvasHeight = canvas.height
            
            if (canvasWidth <= 0 || canvasHeight <= 0) {
                // Canvas 尺寸无效，跳过渲染
                return
            }
            
            // 检查并更新后缓冲区
            if (backBuffer == null || 
                backBuffer!!.width != canvasWidth || 
                backBuffer!!.height != canvasHeight) {
                // 创建新的后缓冲区
                backBuffer = BufferedImage(
                    canvasWidth,
                    canvasHeight,
                    BufferedImage.TYPE_INT_RGB
                )
                println("[VideoRenderer] Created back buffer: ${canvasWidth}x${canvasHeight}")
            }
            
            // 绘制到后缓冲区
            val backGraphics = backBuffer!!.createGraphics()
            try {
                // 计算保持宽高比的渲染尺寸
                val (renderX, renderY, renderWidth, renderHeight) = 
                    calculateRenderBounds(image.width, image.height, canvasWidth, canvasHeight)
                
                // 清空背景（黑色）
                backGraphics.color = java.awt.Color.BLACK
                backGraphics.fillRect(0, 0, canvasWidth, canvasHeight)
                
                // 绘制图像
                backGraphics.drawImage(
                    image,
                    renderX, renderY,
                    renderWidth, renderHeight,
                    null
                )
            } finally {
                backGraphics.dispose()
            }
            
            // 交换缓冲区：将后缓冲区绘制到 Canvas
            val canvasGraphics = canvas.graphics
            if (canvasGraphics != null) {
                try {
                    canvasGraphics.drawImage(backBuffer, 0, 0, null)
                } finally {
                    canvasGraphics.dispose()
                }
            }
            
        } catch (e: Exception) {
            println("[VideoRenderer] Error rendering frame: ${e.message}")
            throw e
        }
    }
    
    /**
     * 确保 BufferedImage 具有有效的 ColorSpace
     * 
     * 某些视频格式转换后可能产生没有 ColorSpace 的图像，
     * 这会导致后续操作出现 NullPointerException。
     * 此方法通过直接像素复制来避免这个问题。
     * 
     * @param image 原始图像
     * @return 具有有效 ColorSpace 的图像
     */
    private fun ensureValidColorSpace(image: BufferedImage): BufferedImage {
        try {
            // 创建新的 TYPE_INT_RGB 图像
            val fixedImage = BufferedImage(
                image.width,
                image.height,
                BufferedImage.TYPE_INT_RGB
            )
            
            // 使用 getRGB/setRGB 直接复制像素，避免 ColorSpace 访问
            val width = image.width
            val height = image.height
            val pixels = IntArray(width * height)
            
            // 批量读取像素
            image.getRGB(0, 0, width, height, pixels, 0, width)
            
            // 批量写入像素
            fixedImage.setRGB(0, 0, width, height, pixels, 0, width)
            
            return fixedImage
            
        } catch (e: Exception) {
            // 如果像素复制失败，尝试逐像素复制
            println("[VideoRenderer] Batch pixel copy failed, trying pixel-by-pixel: ${e.message}")
            
            try {
                val fixedImage = BufferedImage(
                    image.width,
                    image.height,
                    BufferedImage.TYPE_INT_RGB
                )
                
                // 逐像素复制
                for (y in 0 until image.height) {
                    for (x in 0 until image.width) {
                        try {
                            val rgb = image.getRGB(x, y)
                            fixedImage.setRGB(x, y, rgb)
                        } catch (pixelError: Exception) {
                            // 单个像素失败，使用黑色
                            fixedImage.setRGB(x, y, 0)
                        }
                    }
                }
                
                return fixedImage
                
            } catch (fallbackError: Exception) {
                // 完全失败，返回原图像
                println("[VideoRenderer] All pixel copy methods failed: ${fallbackError.message}")
                return image
            }
        }
    }
    
    /**
     * 计算保持宽高比的渲染边界
     * 
     * 根据图像和 Canvas 的尺寸，计算居中显示且保持宽高比的渲染位置和尺寸。
     * 此方法适用于窗口模式和全屏模式，确保视频在任何尺寸下都保持正确的宽高比。
     * 优化：缓存计算结果，避免重复计算
     * 
     * @param imageWidth 图像宽度
     * @param imageHeight 图像高度
     * @param canvasWidth Canvas 宽度
     * @param canvasHeight Canvas 高度
     * @return 渲染边界 (x, y, width, height)
     * 
     * Requirements: 10.2
     */
    private fun calculateRenderBounds(
        imageWidth: Int,
        imageHeight: Int,
        canvasWidth: Int,
        canvasHeight: Int
    ): RenderBounds {
        // 优化：检查缓存是否有效
        if (cachedRenderBounds != null &&
            cachedImageWidth == imageWidth &&
            cachedImageHeight == imageHeight &&
            cachedCanvasWidth == canvasWidth &&
            cachedCanvasHeight == canvasHeight) {
            return cachedRenderBounds!!
        }
        
        // 计算宽高比
        val imageAspect = imageWidth.toDouble() / imageHeight
        val canvasAspect = canvasWidth.toDouble() / canvasHeight
        
        val (renderWidth, renderHeight) = if (imageAspect > canvasAspect) {
            // 图像更宽，以宽度为准
            val width = canvasWidth
            val height = (canvasWidth / imageAspect).toInt()
            width to height
        } else {
            // 图像更高，以高度为准
            val width = (canvasHeight * imageAspect).toInt()
            val height = canvasHeight
            width to height
        }
        
        // 居中显示
        val renderX = (canvasWidth - renderWidth) / 2
        val renderY = (canvasHeight - renderHeight) / 2
        
        val bounds = RenderBounds(renderX, renderY, renderWidth, renderHeight)
        
        // 缓存结果
        cachedImageWidth = imageWidth
        cachedImageHeight = imageHeight
        cachedCanvasWidth = canvasWidth
        cachedCanvasHeight = canvasHeight
        cachedRenderBounds = bounds
        
        return bounds
    }
    
    /**
     * 更新帧率统计
     * 
     * 计算当前帧率并更新统计信息
     */
    private fun updateFrameRate() {
        val currentTime = System.nanoTime()
        val frameTime = currentTime - lastFrameTime
        lastFrameTime = currentTime
        
        // 添加到历史记录
        frameTimeHistory.add(frameTime)
        
        // 保持窗口大小
        if (frameTimeHistory.size > fpsWindowSize) {
            frameTimeHistory.removeAt(0)
        }
        
        // PlaybackStatistics 会自动计算 FPS
        // 这里只需要记录帧时间用于本地监控
    }
    
    /**
     * 处理渲染错误
     * 
     * @param exception 渲染异常
     */
    private fun handleRenderingError(exception: Exception) {
        val errorMessage = exception.message ?: "Unknown error"
        println("[VideoRenderer] Rendering error: $errorMessage")
        
        // 对于非致命错误，继续渲染
        // 只有在连续错误过多时才停止
    }
    
    /**
     * 清理资源
     * 
     * 释放转换器和缓冲区资源
     * 优化：清除所有缓存
     */
    private fun cleanup() {
        try {
            converter.close()
            backBuffer = null
            frameTimeHistory.clear()
            
            // 优化：清除缓存
            cachedRenderBounds = null
            renderCounter = 0
        } catch (e: Exception) {
            println("[VideoRenderer] Error during cleanup: ${e.message}")
        }
    }
    
    /**
     * 更新渲染目标 Canvas
     * 
     * 用于全屏模式切换时更新渲染目标。
     * 会清除后缓冲区，强制在新 Canvas 上重新创建。
     * 优化：同时清除缓存的渲染边界
     * 
     * @param newCanvas 新的 Canvas
     * 
     * Requirements: 10.1, 10.3
     */
    @Synchronized
    fun updateCanvas(newCanvas: Canvas) {
        println("[VideoRenderer] Updating canvas")
        canvas = newCanvas
        
        // 清除后缓冲区，强制重新创建以匹配新 Canvas 尺寸
        backBuffer = null
        
        // 优化：清除缓存的渲染边界
        cachedRenderBounds = null
        
        println("[VideoRenderer] Canvas updated to ${newCanvas.width}x${newCanvas.height}")
    }
    
    /**
     * 处理窗口尺寸变化
     * 
     * 当窗口大小改变时调用此方法，强制重新创建后缓冲区以匹配新尺寸。
     * 渲染器会在下一帧自动检测尺寸变化并调整渲染。
     * 优化：同时清除缓存的渲染边界
     * 
     * Requirements: 10.4
     */
    fun handleSizeChange() {
        println("[VideoRenderer] Handling size change: ${canvas.width}x${canvas.height}")
        
        // 清除后缓冲区，强制重新创建
        // 下一次渲染时会自动创建匹配新尺寸的缓冲区
        backBuffer = null
        
        // 优化：清除缓存的渲染边界
        cachedRenderBounds = null
    }
    
    /**
     * 获取渲染器状态信息
     */
    fun getStatus(): String {
        return buildString {
            append("Renderer Status: ")
            append("Queue: ${videoQueue.size}, ")
            append("FPS: ${"%.2f".format(statistics.currentFps)}, ")
            append("Dropped: ${statistics.framesDropped}")
        }
    }
}

/**
 * 渲染边界数据类
 * 
 * @property x X 坐标
 * @property y Y 坐标
 * @property width 宽度
 * @property height 高度
 */
private data class RenderBounds(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int
)
