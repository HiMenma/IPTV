package com.menmapro.iptv.di

import org.koin.dsl.module

/**
 * Android stub for desktop player module
 * 
 * Android uses its own video player implementation (ExoPlayer),
 * so this module is empty on Android.
 */
actual val desktopPlayerModule = module {
    // Empty module for Android
}
