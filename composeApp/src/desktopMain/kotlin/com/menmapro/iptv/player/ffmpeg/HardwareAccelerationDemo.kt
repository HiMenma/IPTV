package com.menmapro.iptv.player.ffmpeg

/**
 * 硬件加速回退机制演示
 * 
 * 这个文件演示了如何使用 HardwareAccelerationManager 的回退机制。
 * 
 * Requirements: 4.5 - 硬件加速回退机制
 */
object HardwareAccelerationDemo {
    
    /**
     * 演示基本的硬件加速检测和配置
     */
    fun demonstrateBasicDetection() {
        println("=== Hardware Acceleration Detection Demo ===\n")
        
        // 1. 获取平台信息
        println(HardwareAccelerationManager.getPlatformInfo())
        println()
        
        // 2. 检测硬件加速
        val hwAccel = HardwareAccelerationManager.detectHardwareAcceleration()
        println("Detected: ${hwAccel.getDescription()}")
        println()
    }
    
    /**
     * 演示回退监听器的使用
     */
    fun demonstrateFallbackListener() {
        println("=== Fallback Listener Demo ===\n")
        
        // 设置回退监听器
        HardwareAccelerationManager.setFallbackListener { type, reason ->
            println("⚠️  Fallback Event Detected!")
            println("   Type: $type")
            println("   Reason: $reason")
            println("   Action: Switching to software decoding")
            println()
        }
        
        println("Fallback listener has been set up.")
        println("It will be triggered when hardware acceleration fails.")
        println()
    }
    
    /**
     * 演示回退统计信息
     */
    fun demonstrateFallbackStatistics() {
        println("=== Fallback Statistics Demo ===\n")
        
        // 获取统计信息
        val statistics = HardwareAccelerationManager.getFallbackStatistics()
        println(statistics)
        println()
        
        // 获取历史记录
        val history = HardwareAccelerationManager.getFallbackHistory()
        if (history.isEmpty()) {
            println("No fallback events recorded yet.")
        } else {
            println("Fallback History:")
            history.forEach { event ->
                println("  - ${event.getDescription()}")
            }
        }
        println()
    }
    
    /**
     * 演示完整的使用流程
     */
    fun demonstrateCompleteWorkflow() {
        println("=== Complete Workflow Demo ===\n")
        
        // 1. 设置监听器
        println("Step 1: Setting up fallback listener...")
        HardwareAccelerationManager.setFallbackListener { type, reason ->
            println("   [Fallback] $type failed: $reason")
        }
        
        // 2. 检测硬件加速
        println("\nStep 2: Detecting hardware acceleration...")
        val hwAccel = HardwareAccelerationManager.detectHardwareAcceleration()
        println("   ${hwAccel.getDescription()}")
        
        // 3. 检查是否之前失败过
        println("\nStep 3: Checking failure history...")
        if (HardwareAccelerationManager.hasFailedBefore(hwAccel.type)) {
            println("   ⚠️  This hardware acceleration type has failed before")
            println("   Will use software decoding instead")
        } else {
            println("   ✓ No previous failures for ${hwAccel.type}")
        }
        
        // 4. 显示统计信息
        println("\nStep 4: Current statistics:")
        println(HardwareAccelerationManager.getFallbackStatistics())
        
        // 5. 清理
        println("\nStep 5: Cleanup...")
        HardwareAccelerationManager.clearFallbackListener()
        println("   Fallback listener cleared")
        println()
    }
    
    /**
     * 演示如何在实际播放器中使用
     */
    fun demonstratePlayerIntegration() {
        println("=== Player Integration Example ===\n")
        
        println("""
            // In your player initialization code:
            
            // 1. Set up fallback listener to handle failures
            HardwareAccelerationManager.setFallbackListener { type, reason ->
                logger.warn("Hardware acceleration failed: ${'$'}type - ${'$'}reason")
                // Update UI to show software decoding is being used
                updateDecodingStatus("Software Decoding")
            }
            
            // 2. Configure hardware acceleration with automatic fallback
            val grabber = FFmpegFrameGrabber(url)
            val hwType = HardwareAccelerationManager.configureWithFallback(grabber)
            
            // 3. Start the grabber
            try {
                grabber.start()
                
                // 4. Verify hardware acceleration is working
                val isHwWorking = HardwareAccelerationManager.verifyAndFallback(grabber, hwType)
                if (isHwWorking) {
                    logger.info("Hardware acceleration is working: ${'$'}hwType")
                } else {
                    logger.info("Using software decoding")
                }
            } catch (e: Exception) {
                logger.error("Failed to start grabber", e)
            }
            
            // 5. On next playback, failed types will be automatically skipped
            // The manager remembers which hardware acceleration types failed
        """.trimIndent())
        println()
    }
    
    /**
     * 主演示函数
     */
    @JvmStatic
    fun main(args: Array<String>) {
        println("\n" + "=".repeat(60))
        println("Hardware Acceleration Fallback Mechanism Demo")
        println("=".repeat(60) + "\n")
        
        demonstrateBasicDetection()
        demonstrateFallbackListener()
        demonstrateFallbackStatistics()
        demonstrateCompleteWorkflow()
        demonstratePlayerIntegration()
        
        println("=".repeat(60))
        println("Demo Complete")
        println("=".repeat(60) + "\n")
    }
}
