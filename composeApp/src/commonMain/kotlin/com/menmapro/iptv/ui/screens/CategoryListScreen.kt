package com.menmapro.iptv.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.List
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.menmapro.iptv.data.model.Category
import com.menmapro.iptv.data.model.Playlist
import com.menmapro.iptv.data.repository.PlaylistRepository
import com.menmapro.iptv.ui.components.ErrorView
import com.menmapro.iptv.ui.components.EmptyView
import com.menmapro.iptv.ui.components.LoadingView
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

data class CategoryListScreen(val playlist: Playlist) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val repository: PlaylistRepository = koinInject()
        val screenModel = remember { CategoryListScreenModel(playlist, repository) }
        
        val categories by screenModel.categories.collectAsState()
        val channelCounts by screenModel.channelCounts.collectAsState()
        val isLoading by screenModel.isLoading.collectAsState()
        val error by screenModel.error.collectAsState()
        
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(playlist.name) },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                        }
                    }
                )
            }
        ) { padding ->
            when {
                isLoading -> {
                    LoadingView(
                        modifier = Modifier.fillMaxSize().padding(padding)
                    )
                }
                error != null -> {
                    ErrorView(
                        message = error!!,
                        onRetry = { screenModel.loadCategories() },
                        modifier = Modifier.fillMaxSize().padding(padding)
                    )
                }
                categories.isEmpty() -> {
                    EmptyView(
                        message = "暂无分类",
                        modifier = Modifier.fillMaxSize().padding(padding)
                    )
                }
                else -> {
                    LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {
                        items(categories) { category ->
                            CategoryRow(
                                category = category,
                                channelCount = channelCounts[category.id] ?: 0,
                                onClick = {
                                    navigator.push(
                                        ChannelListScreen(
                                            playlist = playlist,
                                            categoryId = category.id
                                        )
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryRow(
    category: Category,
    channelCount: Int,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
        elevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.List,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colors.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.h6
                )
                Text(
                    text = "$channelCount 个频道",
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                )
            }
            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colors.onSurface.copy(alpha = 0.4f)
            )
        }
    }
}

class CategoryListScreenModel(
    private val playlist: Playlist,
    private val repository: PlaylistRepository
) : ScreenModel {
    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories
    
    private val _channelCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val channelCounts: StateFlow<Map<String, Int>> = _channelCounts
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error
    
    init {
        loadCategories()
    }
    
    fun loadCategories() {
        screenModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                _categories.value = repository.getCategories(playlist.id)
                _channelCounts.value = repository.getCategoryChannelCount(playlist.id)
            } catch (e: Exception) {
                _error.value = "加载分类失败: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
