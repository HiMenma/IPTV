package com.menmapro.iptv

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.navigator.Navigator
import com.menmapro.iptv.di.initKoin
import com.menmapro.iptv.ui.screens.PlaylistScreen
import com.menmapro.iptv.ui.theme.IPTVTheme
import org.koin.compose.KoinContext

@Composable
fun App() {
    // Initialize Koin (should be done once, but for simplicity here in App or Platform specific init)
    // Ideally initKoin() is called from platform specific entry points.
    // We will assume it's called there or we use a check.
    
    IPTVTheme {
        KoinContext {
            Navigator(PlaylistScreen())
        }
    }
}
