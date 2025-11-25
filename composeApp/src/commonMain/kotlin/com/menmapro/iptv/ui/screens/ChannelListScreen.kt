package com.menmapro.iptv.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil3.compose.AsyncImage
import com.menmapro.iptv.data.model.Category
import com.menmapro.iptv.data.model.Channel
import com.menmapro.iptv.data.model.Playlist
import com.menmapro.iptv.data.model.PlaylistType
import com.menmapro.iptv.data.repository.FavoriteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import kotlinx.coroutines.flow.SharingStarted

data class ChannelListScreen(
    val playlist: Playlist,
    val categoryId: String? = null
) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val favoriteRepository: FavoriteRepository = koinInject()
        val playlistRepository: com.menmapro.iptv.data.repository.PlaylistRepository = koinInject()
        val screenModel = remember { ChannelListScreenModel(playlist, categoryId, favoriteRepository, playlistRepository) }
        
        val filteredChannels by screenModel.filteredChannels.collectAsState()
        val searchQuery by screenModel.searchQuery.collectAsState()
        val selectedCategory by screenModel.selectedCategory.collectAsState()
        val categoryName by screenModel.categoryName.collectAsState()

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { 
                        Text(
                            if (categoryId != null && categoryName != null) {
                                "$categoryName - ${playlist.name}"
                            } else {
                                playlist.name
                            }
                        ) 
                    },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                        }
                    }
                )
            }
        ) { padding ->
            Column(modifier = Modifier.padding(padding).fillMaxSize()) {
                // 搜索框
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { screenModel.updateSearchQuery(it) },
                    placeholder = { Text("搜索频道...") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "搜索")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    singleLine = true
                )
                
                // 分类标签（仅 Xtream）
                if (playlist.type == PlaylistType.XTREAM && playlist.categories.isNotEmpty()) {
                    ScrollableTabRow(
                        selectedTabIndex = playlist.categories.indexOfFirst { it.id == selectedCategory?.id } + 1,
                        edgePadding = 8.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // "全部"标签
                        Tab(
                            selected = selectedCategory == null,
                            onClick = { screenModel.selectCategory(null) },
                            text = { Text("全部") }
                        )
                        
                        // 分类标签
                        playlist.categories.forEach { category ->
                            Tab(
                                selected = selectedCategory?.id == category.id,
                                onClick = { screenModel.selectCategory(category) },
                                text = { Text(category.name) }
                            )
                        }
                    }
                }
                
                // 频道列表
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(filteredChannels, key = { it.id }) { channel ->
                        val isFavorite by screenModel.isFavorite(channel.id).collectAsState(false)
                        
                        ChannelRow(
                            channel = channel,
                            isFavorite = isFavorite,
                            onChannelClick = { navigator.push(PlayerScreen(channel)) },
                            onFavoriteClick = { screenModel.toggleFavorite(channel.id) }
                        )
                    }
                    
                    // 空状态提示
                    if (filteredChannels.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (searchQuery.isNotBlank()) "未找到匹配的频道" else "此分类暂无频道",
                                    style = MaterialTheme.typography.body2,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChannelRow(
    channel: Channel,
    isFavorite: Boolean,
    onChannelClick: () -> Unit,
    onFavoriteClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
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
        
        IconButton(onClick = onFavoriteClick) {
            Icon(
                imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = if (isFavorite) "取消收藏" else "收藏",
                tint = if (isFavorite) Color.Red else Color.Gray
            )
        }
    }
}

class ChannelListScreenModel(
    private val playlist: Playlist,
    private val categoryId: String?,
    private val favoriteRepository: FavoriteRepository,
    private val playlistRepository: com.menmapro.iptv.data.repository.PlaylistRepository
) : ScreenModel {
    private val favoriteStates = mutableMapOf<String, MutableStateFlow<Boolean>>()
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery
    
    private val _selectedCategory = MutableStateFlow<Category?>(null)
    val selectedCategory: StateFlow<Category?> = _selectedCategory
    
    private val _categoryName = MutableStateFlow<String?>(null)
    val categoryName: StateFlow<String?> = _categoryName
    
    private val _channelsFromCategory = MutableStateFlow<List<Channel>>(emptyList())
    
    init {
        // If categoryId is provided, load channels for that category
        if (categoryId != null) {
            screenModelScope.launch {
                try {
                    val channels = playlistRepository.getChannelsByCategory(playlist.id, categoryId)
                    _channelsFromCategory.value = channels
                    
                    // Find category name from playlist categories
                    val category = playlist.categories.find { it.id == categoryId }
                    _categoryName.value = category?.name
                } catch (e: Exception) {
                    println("Error loading channels for category: ${e.message}")
                    _channelsFromCategory.value = emptyList()
                }
            }
        } else {
            _channelsFromCategory.value = playlist.channels
        }
    }
    
    val filteredChannels: StateFlow<List<Channel>> = combine(
        _searchQuery,
        _selectedCategory,
        _channelsFromCategory
    ) { query, category, channelsFromCategory ->
        var result = if (categoryId != null) {
            // When viewing a specific category, use channels from that category
            channelsFromCategory
        } else {
            // When viewing all channels, use playlist channels
            playlist.channels
        }
        
        // 分类过滤 (only when no categoryId is set, for the tab filtering)
        if (categoryId == null && category != null) {
            result = result.filter { it.group == category.name }
        }
        
        // 搜索过滤
        if (query.isNotBlank()) {
            result = result.filter { channel ->
                channel.name.contains(query, ignoreCase = true)
            }
        }
        
        result
    }.stateIn(
        scope = screenModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = if (categoryId != null) emptyList() else playlist.channels
    )
    
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }
    
    fun selectCategory(category: Category?) {
        _selectedCategory.value = category
    }
    
    fun isFavorite(channelId: String): StateFlow<Boolean> {
        return favoriteStates.getOrPut(channelId) {
            MutableStateFlow(false).also { flow ->
                screenModelScope.launch {
                    flow.value = favoriteRepository.isFavorite(channelId)
                }
            }
        }
    }
    
    fun toggleFavorite(channelId: String) {
        screenModelScope.launch {
            favoriteRepository.toggleFavorite(channelId)
            favoriteStates[channelId]?.value = favoriteRepository.isFavorite(channelId)
        }
    }
}
