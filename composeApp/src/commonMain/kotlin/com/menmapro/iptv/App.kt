package com.menmapro.iptv

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.navigator.Navigator
import com.menmapro.iptv.ui.screens.PlaylistScreen
import com.menmapro.iptv.ui.theme.IPTVTheme
import org.koin.compose.KoinContext

@Composable
fun App() {
    IPTVTheme {
        KoinContext {
            Navigator(PlaylistScreen())
        }
    }
}
