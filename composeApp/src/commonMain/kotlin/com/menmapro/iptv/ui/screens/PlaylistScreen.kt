package com.menmapro.iptv.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Refresh
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
import com.menmapro.iptv.data.model.getDisplayName
import com.menmapro.iptv.data.model.getIcon
import com.menmapro.iptv.data.repository.PlaylistRepository
import com.menmapro.iptv.ui.components.EmptyView
import com.menmapro.iptv.ui.components.LoadingDialog
import com.menmapro.iptv.ui.components.RenamePlaylistDialog
import kotlinx.coroutines.launch

class PlaylistScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = getScreenModel<PlaylistScreenModel>()
        val playlists by screenModel.playlists.collectAsState(emptyList())
        var showAddDialog by remember { mutableStateOf(false) }
        var playlistToRename by remember { mutableStateOf<Playlist?>(null) }
        var isLoading by remember { mutableStateOf(false) }
        var loadingMessage by remember { mutableStateOf("") }
        var errorMessage by remember { mutableStateOf<String?>(null) }

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
            if (playlists.isEmpty()) {
                EmptyView(
                    message = "暂无播放列表\n点击右下角按钮添加",
                    modifier = Modifier.padding(padding).fillMaxSize()
                )
            } else {
                LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {
                    items(playlists) { playlist ->
                        PlaylistRow(
                            playlist = playlist,
                            onClick = {
                                // Navigate based on playlist type
                                when (playlist.type) {
                                    com.menmapro.iptv.data.model.PlaylistType.XTREAM -> {
                                        // For Xtream playlists, show category list first
                                        navigator.push(CategoryListScreen(playlist))
                                    }
                                    com.menmapro.iptv.data.model.PlaylistType.M3U_URL,
                                    com.menmapro.iptv.data.model.PlaylistType.M3U_FILE -> {
                                        // For M3U playlists, go directly to channel list
                                        navigator.push(ChannelListScreen(playlist))
                                    }
                                }
                            },
                            onEdit = {
                                playlistToRename = playlist
                            },
                            onRefresh = {
                                isLoading = true
                                loadingMessage = "正在刷新播放列表..."
                                screenModel.refreshPlaylist(
                                    playlistId = playlist.id,
                                    onSuccess = {
                                        isLoading = false
                                    },
                                    onError = { error ->
                                        isLoading = false
                                        errorMessage = error
                                    }
                                )
                            },
                            onExport = {
                                isLoading = true
                                loadingMessage = "正在导出M3U文件..."
                                screenModel.exportPlaylist(
                                    playlistId = playlist.id,
                                    playlistName = playlist.name,
                                    onSuccess = {
                                        isLoading = false
                                    },
                                    onError = { error ->
                                        isLoading = false
                                        errorMessage = error
                                    }
                                )
                            }
                        )
                    }
                }
            }
            
            if (showAddDialog) {
                AddPlaylistDialog(
                    onDismiss = { showAddDialog = false },
                    onAddM3u = { name, url -> 
                        isLoading = true
                        loadingMessage = "正在加载播放列表..."
                        showAddDialog = false
                        screenModel.addM3uUrl(
                            name = name.ifBlank { "M3U播放列表" },
                            url = url,
                            onSuccess = {
                                isLoading = false
                            },
                            onError = { error ->
                                isLoading = false
                                errorMessage = error
                            }
                        )
                    },
                    onAddXtream = { name, url, user, pass ->
                        isLoading = true
                        loadingMessage = "正在连接Xtream服务器..."
                        showAddDialog = false
                        screenModel.addXtream(
                            name = name.ifBlank { url },
                            url = url,
                            user = user,
                            pass = pass,
                            onSuccess = {
                                isLoading = false
                            },
                            onError = { error ->
                                isLoading = false
                                errorMessage = error
                            }
                        )
                    },
                    onAddM3uFile = { name ->
                        isLoading = true
                        loadingMessage = "正在读取M3U文件..."
                        showAddDialog = false
                        screenModel.pickAndAddM3uFile(
                            name = name,
                            onSuccess = {
                                isLoading = false
                            },
                            onError = { error ->
                                isLoading = false
                                errorMessage = error
                            }
                        )
                    }
                )
            }
            
            if (isLoading) {
                LoadingDialog(message = loadingMessage)
            }
            
            errorMessage?.let { error ->
                AlertDialog(
                    onDismissRequest = { errorMessage = null },
                    title = { Text("错误") },
                    text = { Text(error) },
                    confirmButton = {
                        Button(onClick = { errorMessage = null }) {
                            Text("确定")
                        }
                    }
                )
            }
            
            playlistToRename?.let { playlist ->
                RenamePlaylistDialog(
                    playlist = playlist,
                    onDismiss = { playlistToRename = null },
                    onConfirm = { newName ->
                        screenModel.renamePlaylist(playlist.id, newName)
                        playlistToRename = null
                    }
                )
            }
        }
    }
}

@Composable
fun PlaylistRow(
    playlist: Playlist,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onRefresh: () -> Unit,
    onExport: () -> Unit = {}
) {
    var showMenu by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Type icon
            Icon(
                imageVector = playlist.type.getIcon(),
                contentDescription = playlist.type.getDisplayName(),
                modifier = Modifier.size(40.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Playlist info
            Column(modifier = Modifier.weight(1f)) {
                Text(text = playlist.name, style = MaterialTheme.typography.h6)
                Text(
                    text = "${playlist.channels.size} Channels",
                    style = MaterialTheme.typography.body2
                )
                Text(
                    text = playlist.type.getDisplayName(),
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                )
            }
            
            // More options button
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "更多选项"
                    )
                }
                
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    // Refresh option (only for M3U_URL and XTREAM)
                    if (playlist.type == com.menmapro.iptv.data.model.PlaylistType.M3U_URL ||
                        playlist.type == com.menmapro.iptv.data.model.PlaylistType.XTREAM) {
                        DropdownMenuItem(onClick = {
                            showMenu = false
                            onRefresh()
                        }) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("刷新")
                        }
                    }
                    
                    // Export option
                    DropdownMenuItem(onClick = {
                        showMenu = false
                        onExport()
                    }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("导出为M3U")
                    }
                    
                    // Rename option
                    DropdownMenuItem(onClick = {
                        showMenu = false
                        onEdit()
                    }) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("重命名")
                    }
                }
            }
        }
    }
}

@Composable
fun AddPlaylistDialog(
    onDismiss: () -> Unit,
    onAddM3u: (String, String) -> Unit,
    onAddXtream: (String, String, String, String) -> Unit,
    onAddM3uFile: (String) -> Unit = {}
) {
    var tabIndex by remember { mutableStateOf(0) }
    var playlistName by remember { mutableStateOf("") }
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
                    Tab(selected = tabIndex == 1, onClick = { tabIndex = 1 }, text = { Text("本地文件") })
                    Tab(selected = tabIndex == 2, onClick = { tabIndex = 2 }, text = { Text("Xtream") })
                }
                Spacer(modifier = Modifier.height(16.dp))
                
                // 名称输入字段（通用）
                OutlinedTextField(
                    value = playlistName,
                    onValueChange = { playlistName = it },
                    label = { Text("播放列表名称（可选）") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                when (tabIndex) {
                    0 -> {
                        OutlinedTextField(
                            value = m3uUrl,
                            onValueChange = { m3uUrl = it },
                            label = { Text("M3U URL") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    1 -> {
                        Button(
                            onClick = { onAddM3uFile(playlistName) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("选择M3U文件")
                        }
                        Text(
                            text = "点击按钮选择本地M3U文件",
                            style = MaterialTheme.typography.caption,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    2 -> {
                        OutlinedTextField(
                            value = xtreamUrl,
                            onValueChange = { xtreamUrl = it },
                            label = { Text("服务器地址") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = xtreamUser,
                            onValueChange = { xtreamUser = it },
                            label = { Text("用户名") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = xtreamPass,
                            onValueChange = { xtreamPass = it },
                            label = { Text("密码") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (tabIndex != 1) {
                Button(onClick = {
                    when (tabIndex) {
                        0 -> onAddM3u(playlistName, m3uUrl)
                        2 -> onAddXtream(playlistName, xtreamUrl, xtreamUser, xtreamPass)
                    }
                }) {
                    Text("添加")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

class PlaylistScreenModel(
    private val repository: PlaylistRepository,
    private val fileManager: com.menmapro.iptv.platform.FileManager
) : ScreenModel {
    val playlists = repository.getAllPlaylists()

    fun addM3uUrl(
        name: String,
        url: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        screenModelScope.launch {
            try {
                repository.addM3uUrl(name, url)
                onSuccess()
            } catch (e: Exception) {
                println("Error adding M3U: ${e.message}")
                onError(e.message ?: "添加M3U播放列表失败")
            }
        }
    }
    
    fun addM3uContent(
        name: String,
        content: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        screenModelScope.launch {
            try {
                repository.addM3uContent(name, content)
                onSuccess()
            } catch (e: Exception) {
                println("Error adding M3U content: ${e.message}")
                onError(e.message ?: "添加M3U文件失败")
            }
        }
    }

    fun addXtream(
        name: String,
        url: String,
        user: String,
        pass: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        screenModelScope.launch {
            try {
                repository.addXtreamAccount(name, XtreamAccount(url, user, pass))
                onSuccess()
            } catch (e: Exception) {
                println("Error adding Xtream: ${e.message}")
                onError(e.message ?: "添加Xtream账户失败")
            }
        }
    }
    
    fun renamePlaylist(playlistId: String, newName: String) {
        screenModelScope.launch {
            try {
                repository.renamePlaylist(playlistId, newName)
            } catch (e: Exception) {
                println("Error renaming playlist: ${e.message}")
            }
        }
    }
    
    fun refreshPlaylist(
        playlistId: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        screenModelScope.launch {
            try {
                repository.refreshPlaylist(playlistId)
                onSuccess()
            } catch (e: Exception) {
                println("Error refreshing playlist: ${e.message}")
                onError(e.message ?: "刷新播放列表失败")
            }
        }
    }
    
    fun exportPlaylist(
        playlistId: String,
        playlistName: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        screenModelScope.launch {
            try {
                val m3uContent = repository.exportPlaylistToM3u(playlistId)
                val success = fileManager.saveM3uFile(playlistName, m3uContent)
                if (success) {
                    onSuccess()
                } else {
                    onError("用户取消了保存操作")
                }
            } catch (e: Exception) {
                println("Error exporting playlist: ${e.message}")
                onError(e.message ?: "导出M3U文件失败")
            }
        }
    }
    
    fun pickAndAddM3uFile(
        name: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        screenModelScope.launch {
            try {
                val content = fileManager.pickM3uFile()
                if (content != null) {
                    val playlistName = name.ifBlank { "本地M3U播放列表" }
                    addM3uContent(playlistName, content, onSuccess, onError)
                } else {
                    onError("未选择文件或文件读取失败")
                }
            } catch (e: Exception) {
                println("Error picking M3U file: ${e.message}")
                onError(e.message ?: "选择文件失败")
            }
        }
    }
}
