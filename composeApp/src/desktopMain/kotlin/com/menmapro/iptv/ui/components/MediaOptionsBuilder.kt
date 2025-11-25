package com.menmapro.iptv.ui.components

/**
 * MediaOptionsBuilder - 媒体选项构建器
 * 
 * 用于构建VLC媒体播放选项。这些选项在调用mediaPlayer.media().play(url, options)时使用，
 * 可以优化网络缓存、直播流延迟、硬件加速等参数。
 * 
 * 使用示例:
 * ```
 * val options = MediaOptionsBuilder()
 *     .withNetworkCaching(1000)
 *     .withLiveCaching(300)
 *     .withHardwareAcceleration(true)
 *     .build()
 * mediaPlayer.media().play(url, *options)
 * ```
 */
class MediaOptionsBuilder {
    private val options = mutableListOf<String>()
    
    /**
     * 设置网络缓存时间
     * 
     * 网络缓存用于缓冲网络流，以应对网络波动。
     * 较高的值提供更好的缓冲，但会增加延迟。
     * 
     * @param ms 缓存时间（毫秒）
     *           - 本地文件: 300ms
     *           - 局域网: 1000ms
     *           - 互联网: 3000ms
     * @return MediaOptionsBuilder实例，支持链式调用
     */
    fun withNetworkCaching(ms: Int): MediaOptionsBuilder {
        options.add(":network-caching=$ms")
        return this
    }
    
    /**
     * 设置直播流缓存时间
     * 
     * 直播流缓存专门用于实时流媒体，应该设置较低的值以减少延迟。
     * 
     * @param ms 缓存时间（毫秒）
     *           - 推荐值: 300-1000ms
     * @return MediaOptionsBuilder实例，支持链式调用
     */
    fun withLiveCaching(ms: Int): MediaOptionsBuilder {
        options.add(":live-caching=$ms")
        return this
    }
    
    /**
     * 启用或禁用硬件加速
     * 
     * 硬件加速使用GPU进行视频解码，可以显著提高性能并降低CPU使用率。
     * 如果硬件加速不可用或导致问题，VLC会自动回退到软件解码。
     * 
     * @param enabled true启用硬件加速，false禁用
     * @return MediaOptionsBuilder实例，支持链式调用
     */
    fun withHardwareAcceleration(enabled: Boolean): MediaOptionsBuilder {
        if (enabled) {
            options.add(":avcodec-hw=any")
        } else {
            options.add(":avcodec-hw=none")
        }
        return this
    }
    
    /**
     * 设置视频输出模块
     * 
     * 指定VLC使用的视频输出模块。这通常在初始化时设置，
     * 但也可以作为媒体选项传递。
     * 
     * @param vout 视频输出模块名称（如 "macosx", "opengl", "xcb_x11"等）
     * @return MediaOptionsBuilder实例，支持链式调用
     */
    fun withVideoOutput(vout: String): MediaOptionsBuilder {
        options.add(":vout=$vout")
        return this
    }
    
    /**
     * 添加自定义选项
     * 
     * 允许添加任何VLC媒体选项。选项应该以":"开头。
     * 
     * @param option VLC媒体选项字符串
     * @return MediaOptionsBuilder实例，支持链式调用
     */
    fun withCustomOption(option: String): MediaOptionsBuilder {
        options.add(option)
        return this
    }
    
    /**
     * 构建并返回VLC选项数组
     * 
     * 将所有配置的选项转换为VLC可以使用的字符串数组。
     * 
     * @return VLC媒体选项数组
     */
    fun build(): Array<String> {
        return options.toTypedArray()
    }
    
    /**
     * 清空所有选项
     * 
     * 重置构建器，移除所有已添加的选项。
     * 
     * @return MediaOptionsBuilder实例，支持链式调用
     */
    fun clear(): MediaOptionsBuilder {
        options.clear()
        return this
    }
    
    /**
     * 获取当前选项数量
     * 
     * @return 已添加的选项数量
     */
    fun size(): Int = options.size
    
    companion object {
        /**
         * 创建针对直播流优化的选项构建器
         * 
         * 预配置低延迟和适当的缓存设置。
         * 应用以下优化:
         * - 低延迟缓存设置 (300ms live caching, 1000ms network caching)
         * - 禁用时钟抖动和同步以减少延迟
         * - 禁用音频时间拉伸以保持同步
         * 
         * Validates: Requirements 4.2
         * 
         * @return 配置好的MediaOptionsBuilder实例
         */
        fun forLiveStream(): MediaOptionsBuilder {
            return MediaOptionsBuilder()
                .withNetworkCaching(1000)  // 网络缓存1秒
                .withLiveCaching(300)      // 直播缓存300ms，减少延迟
                .withCustomOption(":clock-jitter=0")        // 禁用时钟抖动
                .withCustomOption(":clock-synchro=0")       // 禁用时钟同步
                .withCustomOption(":no-audio-time-stretch") // 禁用音频时间拉伸
        }
        
        /**
         * 创建针对点播内容优化的选项构建器
         * 
         * 预配置较高的缓存以获得更流畅的播放。
         * 
         * @return 配置好的MediaOptionsBuilder实例
         */
        fun forVOD(): MediaOptionsBuilder {
            return MediaOptionsBuilder()
                .withNetworkCaching(3000)
        }
        
        /**
         * 创建针对本地文件优化的选项构建器
         * 
         * 预配置最小缓存以快速启动。
         * 
         * @return 配置好的MediaOptionsBuilder实例
         */
        fun forLocalFile(): MediaOptionsBuilder {
            return MediaOptionsBuilder()
                .withNetworkCaching(300)
        }
    }
}
