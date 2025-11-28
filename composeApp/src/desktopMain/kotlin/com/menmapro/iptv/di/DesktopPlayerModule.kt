package com.menmapro.iptv.di

import com.menmapro.iptv.player.LibmpvPlayerImplementation
import com.menmapro.iptv.player.PlayerImplementation
import org.koin.dsl.module

/**
 * Desktop-specific Koin module for player configuration
 * 
 * This module provides libmpv player implementation for desktop platforms.
 * libmpv is now the default player, offering better performance and simpler integration.
 * 
 * Requirements:
 * - 1.1: Use libmpv library for video playback
 */
actual val desktopPlayerModule = module {
    // Player Implementation - libmpv as default
    single<PlayerImplementation> {
        println("=== Initializing libmpv Player ===")
        println("libmpv is the default player implementation")
        println("Provides excellent format support and hardware acceleration")
        println("======================================")
        LibmpvPlayerImplementation()
    }
}
