package com.menmapro.iptv.data.repository

import com.menmapro.iptv.data.database.dao.PlaylistDao
import com.menmapro.iptv.data.model.Channel
import com.menmapro.iptv.data.model.Playlist
import com.menmapro.iptv.data.model.PlaylistType
import com.menmapro.iptv.data.model.XtreamAccount
import com.menmapro.iptv.data.network.XtreamClient
import com.menmapro.iptv.data.parser.M3uParser
import io.ktor.client.HttpClient
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow

class PlaylistRepository(
    private val httpClient: HttpClient,
    private val m3uParser: M3uParser,
    private val xtreamClient: XtreamClient,
    private val playlistDao: PlaylistDao
) {
    
    private fun logInfo(message: String) {
        println("[PlaylistRepository] INFO: $message")
    }
    
    private fun logError(message: String, error: Throwable? = null) {
        println("[PlaylistRepository] ERROR: $message")
        error?.let { 
            println("[PlaylistRepository] ERROR: ${it.message}")
            println("[PlaylistRepository] ERROR: Stack trace: ${it.stackTraceToString()}")
        }
    }
    
    /**
     * Retry a suspend function with exponential backoff
     * @param maxRetries Maximum number of retry attempts (default: 3)
     * @param initialDelay Initial delay in milliseconds (default: 1000ms)
     * @param maxDelay Maximum delay in milliseconds (default: 10000ms)
     * @param factor Multiplier for exponential backoff (default: 2.0)
     * @param block The suspend function to retry
     * @return Result of the block function
     */
    private suspend fun <T> retryWithBackoff(
        maxRetries: Int = 3,
        initialDelay: Long = 1000,
        maxDelay: Long = 10000,
        factor: Double = 2.0,
        block: suspend () -> T
    ): T {
        var currentDelay = initialDelay
        var lastException: Exception? = null
        
        repeat(maxRetries) { attempt ->
            try {
                logInfo("Attempt ${attempt + 1} of $maxRetries")
                return block()
            } catch (e: Exception) {
                lastException = e
                
                // Don't retry on the last attempt
                if (attempt < maxRetries - 1) {
                    logError("Attempt ${attempt + 1} failed, retrying in ${currentDelay}ms", e)
                    delay(currentDelay)
                    currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
                } else {
                    logError("All $maxRetries attempts failed", e)
                }
            }
        }
        
        // If we get here, all retries failed
        throw lastException ?: Exception("All retry attempts failed")
    }
    
    fun getAllPlaylists(): Flow<List<Playlist>> {
        logInfo("Fetching all playlists from database")
        return playlistDao.getAllPlaylists()
    }

    suspend fun addM3uUrl(name: String, url: String) {
        logInfo("Starting to add M3U URL playlist: name='$name', url='$url'")
        try {
            // Use retry mechanism for downloading M3U content
            val content = retryWithBackoff(
                maxRetries = 3,
                initialDelay = 1000,
                maxDelay = 10000,
                factor = 2.0
            ) {
                logInfo("Downloading M3U content from URL: $url")
                val response = httpClient.get(url).bodyAsText()
                logInfo("Successfully downloaded M3U content, size: ${response.length} bytes")
                response
            }
            
            logInfo("Parsing M3U content")
            val channels = m3uParser.parse(content)
            logInfo("Successfully parsed ${channels.size} channels from M3U content")
            
            val playlist = Playlist(
                id = url.hashCode().toString(),
                name = name,
                url = url,
                type = PlaylistType.M3U_URL,
                channels = channels
            )
            
            logInfo("Saving playlist to database: id='${playlist.id}'")
            playlistDao.insertPlaylist(playlist)
            logInfo("Successfully added M3U URL playlist: name='$name', channels=${channels.size}")
        } catch (e: HttpRequestTimeoutException) {
            logError("Request timeout while downloading M3U from URL: $url", e)
            throw Exception("Request timeout: The server took too long to respond after multiple retries. Please try again later.", e)
        } catch (e: ConnectTimeoutException) {
            logError("Connection timeout while connecting to URL: $url", e)
            throw Exception("Connection timeout: Unable to connect to the server after multiple retries. Please check the URL and your internet connection.", e)
        } catch (e: SocketTimeoutException) {
            logError("Socket timeout while downloading M3U from URL: $url", e)
            throw Exception("Socket timeout: The connection was interrupted after multiple retries. Please try again later.", e)
        } catch (e: Exception) {
            logError("Failed to add M3U URL playlist: name='$name', url='$url'", e)
            throw Exception("Failed to add M3U playlist: ${e.message ?: "Unknown error"}", e)
        }
    }

    suspend fun addXtreamAccount(account: XtreamAccount) {
        logInfo("Starting to add Xtream account: server='${account.serverUrl}', username='${account.username}'")
        try {
            // Use retry mechanism for authentication
            val isAuthenticated = retryWithBackoff(
                maxRetries = 3,
                initialDelay = 1000,
                maxDelay = 10000,
                factor = 2.0
            ) {
                logInfo("Authenticating Xtream account")
                val result = xtreamClient.authenticate(account)
                if (!result) {
                    throw Exception("Authentication failed")
                }
                result
            }
            
            if (isAuthenticated) {
                logInfo("Successfully authenticated Xtream account")
                
                // Use retry mechanism for fetching categories
                val categories = retryWithBackoff(
                    maxRetries = 3,
                    initialDelay = 1000,
                    maxDelay = 10000,
                    factor = 2.0
                ) {
                    logInfo("Fetching live categories from Xtream server")
                    val result = xtreamClient.getLiveCategories(account)
                    logInfo("Successfully fetched ${result.size} categories")
                    result
                }
                
                // Use retry mechanism for fetching channels
                val channels = retryWithBackoff(
                    maxRetries = 3,
                    initialDelay = 1000,
                    maxDelay = 10000,
                    factor = 2.0
                ) {
                    logInfo("Fetching live streams from Xtream server")
                    val result = xtreamClient.getLiveStreams(account)
                    logInfo("Successfully fetched ${result.size} channels")
                    result
                }
                
                // 将 category_id 映射为 category_name
                val categoryMap = categories.associateBy { it.id }
                val channelsWithCategoryNames = channels.map { channel ->
                    val categoryName = categoryMap[channel.group]?.name
                    channel.copy(group = categoryName ?: channel.group)
                }
                
                val playlist = Playlist(
                    id = account.hashCode().toString(),
                    name = account.serverUrl,
                    type = PlaylistType.XTREAM,
                    channels = channelsWithCategoryNames,
                    categories = categories
                )
                
                logInfo("Saving Xtream playlist to database: id='${playlist.id}'")
                playlistDao.insertPlaylist(playlist)
                logInfo("Successfully added Xtream account: server='${account.serverUrl}', channels=${channels.size}")
            } else {
                logError("Failed to authenticate Xtream account: server='${account.serverUrl}', username='${account.username}'")
                throw Exception("Failed to authenticate Xtream account. Please check your credentials.")
            }
        } catch (e: HttpRequestTimeoutException) {
            logError("Request timeout while connecting to Xtream server: ${account.serverUrl}", e)
            throw Exception("Request timeout: The Xtream server took too long to respond after multiple retries. Please try again later.", e)
        } catch (e: ConnectTimeoutException) {
            logError("Connection timeout while connecting to Xtream server: ${account.serverUrl}", e)
            throw Exception("Connection timeout: Unable to connect to the Xtream server after multiple retries. Please check the server URL and your internet connection.", e)
        } catch (e: SocketTimeoutException) {
            logError("Socket timeout while communicating with Xtream server: ${account.serverUrl}", e)
            throw Exception("Socket timeout: The connection was interrupted after multiple retries. Please try again later.", e)
        } catch (e: Exception) {
            logError("Failed to add Xtream account: server='${account.serverUrl}'", e)
            throw Exception("Failed to add Xtream account: ${e.message ?: "Unknown error"}", e)
        }
    }
    
    suspend fun addM3uContent(name: String, content: String) {
        logInfo("Starting to add M3U content playlist: name='$name', content size=${content.length} bytes")
        try {
            logInfo("Parsing M3U content")
            val channels = m3uParser.parse(content)
            logInfo("Successfully parsed ${channels.size} channels from M3U content")
            
            val playlist = Playlist(
                id = name.hashCode().toString(),
                name = name,
                type = PlaylistType.M3U_FILE,
                channels = channels
            )
            
            logInfo("Saving playlist to database: id='${playlist.id}'")
            playlistDao.insertPlaylist(playlist)
            logInfo("Successfully added M3U content playlist: name='$name', channels=${channels.size}")
        } catch (e: Exception) {
            logError("Failed to add M3U content playlist: name='$name'", e)
            throw Exception("Failed to add M3U content: ${e.message ?: "Unknown error"}", e)
        }
    }
    
    suspend fun deletePlaylist(id: String) {
        logInfo("Deleting playlist: id='$id'")
        try {
            playlistDao.deletePlaylist(id)
            logInfo("Successfully deleted playlist: id='$id'")
        } catch (e: Exception) {
            logError("Failed to delete playlist: id='$id'", e)
            throw Exception("Failed to delete playlist: ${e.message ?: "Unknown error"}", e)
        }
    }
    
    suspend fun getPlaylistById(id: String): Playlist? {
        logInfo("Fetching playlist by id: '$id'")
        try {
            val playlist = playlistDao.getPlaylistById(id)
            if (playlist != null) {
                logInfo("Successfully fetched playlist: id='$id', name='${playlist.name}', channels=${playlist.channels.size}")
            } else {
                logInfo("Playlist not found: id='$id'")
            }
            return playlist
        } catch (e: Exception) {
            logError("Failed to fetch playlist by id: '$id'", e)
            throw Exception("Failed to fetch playlist: ${e.message ?: "Unknown error"}", e)
        }
    }
    
    suspend fun renamePlaylist(playlistId: String, newName: String) {
        logInfo("Renaming playlist: id='$playlistId', newName='$newName'")
        
        // Validate input
        if (newName.isBlank()) {
            logError("Attempted to rename playlist with empty name: id='$playlistId'")
            throw IllegalArgumentException("Playlist name cannot be empty")
        }
        
        if (newName.length > 100) {
            logError("Attempted to rename playlist with name too long: id='$playlistId', length=${newName.length}")
            throw IllegalArgumentException("Playlist name cannot exceed 100 characters")
        }
        
        try {
            playlistDao.updatePlaylistName(playlistId, newName.trim())
            logInfo("Successfully renamed playlist: id='$playlistId', newName='$newName'")
        } catch (e: Exception) {
            logError("Failed to rename playlist: id='$playlistId', newName='$newName'", e)
            throw Exception("Failed to rename playlist: ${e.message ?: "Unknown error"}", e)
        }
    }
    
    suspend fun getCategories(playlistId: String): List<com.menmapro.iptv.data.model.Category> {
        logInfo("Fetching categories for playlist: id='$playlistId'")
        try {
            val playlist = getPlaylistById(playlistId)
            if (playlist == null) {
                logError("Playlist not found when fetching categories: id='$playlistId'")
                throw Exception("Playlist not found")
            }
            
            return when (playlist.type) {
                PlaylistType.XTREAM -> {
                    val categories = playlistDao.getCategoriesByPlaylistId(playlistId)
                    logInfo("Successfully fetched ${categories.size} categories for Xtream playlist: id='$playlistId'")
                    categories
                }
                else -> {
                    logInfo("Playlist is not Xtream type, returning empty categories: id='$playlistId', type=${playlist.type}")
                    emptyList()
                }
            }
        } catch (e: Exception) {
            logError("Failed to fetch categories for playlist: id='$playlistId'", e)
            throw Exception("Failed to fetch categories: ${e.message ?: "Unknown error"}", e)
        }
    }
    
    suspend fun getChannelsByCategory(playlistId: String, categoryId: String): List<Channel> {
        logInfo("Fetching channels by category: playlistId='$playlistId', categoryId='$categoryId'")
        try {
            val channels = playlistDao.getChannelsByCategoryId(playlistId, categoryId)
            logInfo("Successfully fetched ${channels.size} channels for category: playlistId='$playlistId', categoryId='$categoryId'")
            return channels
        } catch (e: Exception) {
            logError("Failed to fetch channels by category: playlistId='$playlistId', categoryId='$categoryId'", e)
            throw Exception("Failed to fetch channels by category: ${e.message ?: "Unknown error"}", e)
        }
    }
    
    suspend fun getCategoryChannelCount(playlistId: String): Map<String, Int> {
        logInfo("Fetching channel counts by category for playlist: id='$playlistId'")
        try {
            val counts = playlistDao.getCategoryChannelCounts(playlistId)
            logInfo("Successfully fetched channel counts for ${counts.size} categories in playlist: id='$playlistId'")
            return counts
        } catch (e: Exception) {
            logError("Failed to fetch category channel counts for playlist: id='$playlistId'", e)
            throw Exception("Failed to fetch category channel counts: ${e.message ?: "Unknown error"}", e)
        }
    }
}

