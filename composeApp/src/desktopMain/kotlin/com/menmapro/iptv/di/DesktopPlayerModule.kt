package com.menmapro.iptv.di

import com.menmapro.iptv.player.FFmpegPlayerImplementation
import com.menmapro.iptv.player.PlayerImplementation
import org.koin.dsl.module

/**
 * Desktop-specific Koin module for player configuration
 * 
 * This module provides FFmpeg player implementation for desktop platforms.
 * VLC support has been removed - FFmpeg is now the only player.
 */
actual val desktopPlayerModule = module {
    // Player Implementation - FFmpeg only
    single<PlayerImplementation> {
        println("=== Initializing FFmpeg Player ===")
        println("FFmpeg is the default and only player implementation")
        println("===================================")
        FFmpegPlayerImplementation()
    }
}
