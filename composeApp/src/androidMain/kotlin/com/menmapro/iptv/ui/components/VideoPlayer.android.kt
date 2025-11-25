package com.menmapro.iptv.ui.components

import android.view.ViewGroup
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

@Composable
actual fun VideoPlayer(
    url: String,
    modifier: Modifier,
    playerState: MutableState<PlayerState>,
    onPlayerControls: (PlayerControls) -> Unit
) {
    val context = LocalContext.current
    
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(url))
            prepare()
            playWhenReady = true
        }
    }
    
    val controls = remember {
        object : PlayerControls {
            override fun play() {
                exoPlayer.play()
            }
            
            override fun pause() {
                exoPlayer.pause()
            }
            
            override fun seekTo(positionMs: Long) {
                exoPlayer.seekTo(positionMs)
            }
            
            override fun setVolume(volume: Float) {
                exoPlayer.volume = volume
            }
            
            override fun toggleFullscreen() {
                // TODO: Implement fullscreen
            }
            
            override fun release() {
                exoPlayer.release()
            }
        }
    }
    
    DisposableEffect(Unit) {
        onPlayerControls(controls)
        
        onDispose {
            exoPlayer.release()
        }
    }
    
    // Update player state periodically
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(500)
            if (exoPlayer.isPlaying) {
                playerState.value = playerState.value.copy(
                    playbackState = PlaybackState.PLAYING,
                    position = exoPlayer.currentPosition,
                    duration = exoPlayer.duration.coerceAtLeast(0),
                    volume = exoPlayer.volume
                )
            } else {
                playerState.value = playerState.value.copy(
                    playbackState = PlaybackState.PAUSED,
                    position = exoPlayer.currentPosition,
                    duration = exoPlayer.duration.coerceAtLeast(0)
                )
            }
        }
    }
    
    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = exoPlayer
                useController = true
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        },
        modifier = modifier
    )
}