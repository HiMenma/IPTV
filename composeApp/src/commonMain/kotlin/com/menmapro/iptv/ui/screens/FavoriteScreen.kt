package com.menmapro.iptv.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil3.compose.AsyncImage
import com.menmapro.iptv.data.model.Channel
import com.menmapro.iptv.data.repository.FavoriteRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch

class FavoriteScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = getScreenModel<FavoriteScreenModel>()
        val favorites by screenModel.favorites.collectAsState(emptyList())

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("收藏频道") },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                        }
                    }
                )
            }
        ) { padding ->
            if (favorites.isEmpty()) {
                Box(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("暂无收藏频道")
                }
            } else {
                LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {
                    items(favorites) { channel ->
                        FavoriteChannelRow(
                            channel = channel,
                            onChannelClick = { navigator.push(PlayerScreen(channel)) },
                            onRemoveFavorite = { screenModel.removeFavorite(channel.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FavoriteChannelRow(
    channel: Channel,
    onChannelClick: () -> Unit,
    onRemoveFavorite: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .clickable(onClick = onChannelClick)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (channel.logoUrl != null) {
                AsyncImage(
                    model = channel.logoUrl,
                    contentDescription = null,
                    modifier = Modifier.size(50.dp),
                    contentScale = ContentScale.Fit
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Text(text = channel.name, style = MaterialTheme.typography.body1)
                if (channel.group != null) {
                    Text(text = channel.group, style = MaterialTheme.typography.caption)
                }
            }
            
            IconButton(onClick = onRemoveFavorite) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "取消收藏",
                    tint = MaterialTheme.colors.error
                )
            }
        }
    }
}

class FavoriteScreenModel(private val favoriteRepository: FavoriteRepository) : ScreenModel {
    val favorites: StateFlow<List<Channel>> = favoriteRepository.getAllFavorites()
        .stateIn(
            scope = screenModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    fun removeFavorite(channelId: String) {
        screenModelScope.launch {
            favoriteRepository.removeFavorite(channelId)
        }
    }
}
