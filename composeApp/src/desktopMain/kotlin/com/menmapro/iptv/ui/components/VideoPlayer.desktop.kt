package com.menmapro.iptv.ui.components

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.graphics.Color
import uk.co.caprica.vlcj.factory.discovery.NativeDiscovery
import uk.co.caprica.vlcj.player.component.EmbeddedMediaPlayerComponent
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
actual fun VideoPlayer(
    url: String,
    modifier: Modifier,
    playerState: MutableState<PlayerState>,
    onPlayerControls: (PlayerControls) -> Unit
) {
    val mediaPlayerComponent = remember {
        NativeDiscovery().discover()
        EmbeddedMediaPlayerComponent()
    }
    
    val controls = remember {
        object : PlayerControls {
            override fun play() {
                mediaPlayerComponent.mediaPlayer().controls().play()
            }
            
            override fun pause() {
                mediaPlayerComponent.mediaPlayer().controls().pause()
            }
            
            override fun seekTo(positionMs: Long) {
                mediaPlayerComponent.mediaPlayer().controls().setTime(positionMs)
            }
            
            override fun setVolume(volume: Float) {
                val volumeInt = (volume * 100).toInt()
                mediaPlayerComponent.mediaPlayer().audio().setVolume(volumeInt)
            }
            
            override fun toggleFullscreen() {
                mediaPlayerComponent.mediaPlayer().fullScreen().toggle()
            }
            
            override fun release() {
                mediaPlayerComponent.release()
            }
        }
    }
    
    DisposableEffect(Unit) {
        // Setup event listener
        mediaPlayerComponent.mediaPlayer().events().addMediaPlayerEventListener(
            object : MediaPlayerEventAdapter() {
                override fun playing(mediaPlayer: MediaPlayer) {
                    playerState.value = playerState.value.copy(
                        playbackState = PlaybackState.PLAYING
                    )
                }
                
                override fun paused(mediaPlayer: MediaPlayer) {
                    playerState.value = playerState.value.copy(
                        playbackState = PlaybackState.PAUSED
                    )
                }
                
                override fun buffering(mediaPlayer: MediaPlayer, newCache: Float) {
                    if (newCache < 100f) {
                        playerState.value = playerState.value.copy(
                            playbackState = PlaybackState.BUFFERING
                        )
                    }
                }
                
                override fun finished(mediaPlayer: MediaPlayer) {
                    playerState.value = playerState.value.copy(
                        playbackState = PlaybackState.ENDED
                    )
                }
                
                override fun error(mediaPlayer: MediaPlayer) {
                    playerState.value = playerState.value.copy(
                        playbackState = PlaybackState.ERROR,
                        errorMessage = "播放错误"
                    )
                }
                
                override fun lengthChanged(mediaPlayer: MediaPlayer, newLength: Long) {
                    playerState.value = playerState.value.copy(
                        duration = newLength
                    )
                }
            }
        )
        
        onPlayerControls(controls)
        
        onDispose {
            mediaPlayerComponent.release()
        }
    }
    
    // Update position periodically
    LaunchedEffect(Unit) {
        while (isActive) {
            if (playerState.value.playbackState == PlaybackState.PLAYING) {
                val currentTime = mediaPlayerComponent.mediaPlayer().status().time()
                val length = mediaPlayerComponent.mediaPlayer().status().length()
                val volume = mediaPlayerComponent.mediaPlayer().audio().volume() / 100f
                
                playerState.value = playerState.value.copy(
                    position = currentTime,
                    duration = if (length > 0) length else playerState.value.duration,
                    volume = volume
                )
            }
            delay(500)
        }
    }
    
    LaunchedEffect(url) {
        delay(500)
        try {
            mediaPlayerComponent.mediaPlayer().media().play(url)
        } catch (e: Exception) {
            playerState.value = playerState.value.copy(
                playbackState = PlaybackState.ERROR,
                errorMessage = "无法加载媒体: ${e.message}"
            )
        }
    }

    SwingPanel(
        background = Color.Black,
        modifier = modifier,
        factory = { mediaPlayerComponent }
    )
}
