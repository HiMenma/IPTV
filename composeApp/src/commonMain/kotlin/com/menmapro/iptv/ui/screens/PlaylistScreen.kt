package com.menmapro.iptv.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.menmapro.iptv.data.model.Playlist
import com.menmapro.iptv.data.model.XtreamAccount
import com.menmapro.iptv.data.repository.PlaylistRepository
import kotlinx.coroutines.launch

class PlaylistScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = getScreenModel<PlaylistScreenModel>()
        val playlists by screenModel.playlists.collectAsState(emptyList())
        var showAddDialog by remember { mutableStateOf(false) }

        Scaffold(
            topBar = { 
                TopAppBar(
                    title = { Text("播放列表") },
                    actions = {
                        IconButton(onClick = { navigator.push(FavoriteScreen()) }) {
                            Icon(Icons.Default.FavoriteBorder, contentDescription = "收藏")
                        }
                        // TODO: Add settings when ready
                        // IconButton(onClick = { navigator.push(SettingsScreen()) }) {
                        //     Icon(Icons.Default.Settings, contentDescription = "设置")
                        // }
                    }
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Playlist")
                }
            }
        ) { padding ->
            LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {
                items(playlists) { playlist ->
                    PlaylistRow(playlist) {
                        navigator.push(ChannelListScreen(playlist))
                    }
                }
            }
            
            if (showAddDialog) {
                AddPlaylistDialog(
                    onDismiss = { showAddDialog = false },
                    onAddM3u = { url -> 
                        screenModel.addM3uUrl("New Playlist", url)
                        showAddDialog = false
                    },
                    onAddXtream = { url, user, pass ->
                        screenModel.addXtream(url, user, pass)
                        showAddDialog = false
                    }
                )
            }
        }
    }
}

@Composable
fun PlaylistRow(playlist: Playlist, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(8.dp).clickable(onClick = onClick)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = playlist.name, style = MaterialTheme.typography.h6)
            Text(text = "${playlist.channels.size} Channels", style = MaterialTheme.typography.body2)
            Text(text = playlist.type.name, style = MaterialTheme.typography.caption)
        }
    }
}

@Composable
fun AddPlaylistDialog(
    onDismiss: () -> Unit,
    onAddM3u: (String) -> Unit,
    onAddXtream: (String, String, String) -> Unit
) {
    var tabIndex by remember { mutableStateOf(0) }
    var m3uUrl by remember { mutableStateOf("") }
    var xtreamUrl by remember { mutableStateOf("") }
    var xtreamUser by remember { mutableStateOf("") }
    var xtreamPass by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Playlist") },
        text = {
            Column {
                TabRow(selectedTabIndex = tabIndex) {
                    Tab(selected = tabIndex == 0, onClick = { tabIndex = 0 }, text = { Text("M3U URL") })
                    Tab(selected = tabIndex == 1, onClick = { tabIndex = 1 }, text = { Text("Xtream") })
                }
                Spacer(modifier = Modifier.height(16.dp))
                if (tabIndex == 0) {
                    OutlinedTextField(
                        value = m3uUrl,
                        onValueChange = { m3uUrl = it },
                        label = { Text("M3U URL") },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    OutlinedTextField(
                        value = xtreamUrl,
                        onValueChange = { xtreamUrl = it },
                        label = { Text("Server URL") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = xtreamUser,
                        onValueChange = { xtreamUser = it },
                        label = { Text("Username") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = xtreamPass,
                        onValueChange = { xtreamPass = it },
                        label = { Text("Password") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (tabIndex == 0) {
                    onAddM3u(m3uUrl)
                } else {
                    onAddXtream(xtreamUrl, xtreamUser, xtreamPass)
                }
            }) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

class PlaylistScreenModel(private val repository: PlaylistRepository) : ScreenModel {
    val playlists = repository.getAllPlaylists()

    fun addM3uUrl(name: String, url: String) {
        screenModelScope.launch {
            try {
                repository.addM3uUrl(name, url)
            } catch (e: Exception) {
                // TODO: Show error to user
                println("Error adding M3U: ${e.message}")
            }
        }
    }

    fun addXtream(url: String, user: String, pass: String) {
        screenModelScope.launch {
            try {
                repository.addXtreamAccount(XtreamAccount(url, user, pass))
            } catch (e: Exception) {
                // TODO: Show error to user
                println("Error adding Xtream: ${e.message}")
            }
        }
    }
}
