package com.menmapro.iptv.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.menmapro.iptv.data.model.Channel
import com.menmapro.iptv.ui.components.*

data class PlayerScreen(val channel: Channel) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val playerState = remember { mutableStateOf(PlayerState()) }
        var playerControls by remember { mutableStateOf<PlayerControls?>(null) }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(channel.name) },
                    navigationIcon = {
                        IconButton(onClick = { 
                            playerControls?.release()
                            navigator.pop()
                        }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                        }
                    }
                )
            }
        ) { padding ->
            VideoPlayer(
                url = channel.url,
                modifier = Modifier.padding(padding).fillMaxSize(),
                playerState = playerState,
                onPlayerControls = { controls ->
                    playerControls = controls
                }
            )
        }
    }
}
