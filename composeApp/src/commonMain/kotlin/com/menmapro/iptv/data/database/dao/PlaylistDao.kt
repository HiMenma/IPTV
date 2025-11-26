package com.menmapro.iptv.data.database.dao

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.menmapro.iptv.data.database.IptvDatabase
import com.menmapro.iptv.data.model.Channel
import com.menmapro.iptv.data.model.Playlist
import com.menmapro.iptv.data.model.PlaylistType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class PlaylistDao(private val database: IptvDatabase) {
    
    private fun logInfo(message: String) {
        println("[PlaylistDao] INFO: $message")
    }
    
    private fun logError(message: String, error: Throwable? = null) {
        println("[PlaylistDao] ERROR: $message")
        error?.let { 
            println("[PlaylistDao] ERROR: ${it.message}")
            println("[PlaylistDao] ERROR: Stack trace: ${it.stackTraceToString()}")
        }
    }
    
    fun getAllPlaylists(): Flow<List<Playlist>> {
        logInfo("Starting to fetch all playlists from database")
        return database.iptvDatabaseQueries
            .selectAllPlaylists()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { playlists ->
                logInfo("Fetched ${playlists.size} playlists from database")
                playlists.map { dbPlaylist ->
                    try {
                        val channels = getChannelsByPlaylistId(dbPlaylist.id)
                        val xtreamAccount = if (dbPlaylist.xtreamServerUrl != null && 
                            dbPlaylist.xtreamUsername != null && 
                            dbPlaylist.xtreamPassword != null) {
                            com.menmapro.iptv.data.model.XtreamAccount(
                                serverUrl = dbPlaylist.xtreamServerUrl,
                                username = dbPlaylist.xtreamUsername,
                                password = dbPlaylist.xtreamPassword
                            )
                        } else null
                        
                        Playlist(
                            id = dbPlaylist.id,
                            name = dbPlaylist.name,
                            url = dbPlaylist.url,
                            type = PlaylistType.valueOf(dbPlaylist.type),
                            channels = channels,
                            xtreamAccount = xtreamAccount
                        )
                    } catch (e: Exception) {
                        logError("Failed to map playlist: id='${dbPlaylist.id}', name='${dbPlaylist.name}'", e)
                        throw e
                    }
                }
            }
            .catch { e ->
                logError("Failed to fetch all playlists from database", e)
                throw e
            }
    }
    
    suspend fun getPlaylistById(id: String): Playlist? = withContext(Dispatchers.Default) {
        logInfo("Fetching playlist by id: '$id'")
        try {
            val dbPlaylist = database.iptvDatabaseQueries.selectPlaylistById(id).executeAsOneOrNull()
                ?: run {
                    logInfo("Playlist not found in database: id='$id'")
                    return@withContext null
                }
            
            val channels = getChannelsByPlaylistId(id)
            val xtreamAccount = if (dbPlaylist.xtreamServerUrl != null && 
                dbPlaylist.xtreamUsername != null && 
                dbPlaylist.xtreamPassword != null) {
                com.menmapro.iptv.data.model.XtreamAccount(
                    serverUrl = dbPlaylist.xtreamServerUrl,
                    username = dbPlaylist.xtreamUsername,
                    password = dbPlaylist.xtreamPassword
                )
            } else null
            
            logInfo("Successfully fetched playlist: id='$id', name='${dbPlaylist.name}', channels=${channels.size}")
            Playlist(
                id = dbPlaylist.id,
                name = dbPlaylist.name,
                url = dbPlaylist.url,
                type = PlaylistType.valueOf(dbPlaylist.type),
                channels = channels,
                xtreamAccount = xtreamAccount
            )
        } catch (e: Exception) {
            logError("Failed to fetch playlist by id: '$id'", e)
            throw e
        }
    }
    
    suspend fun insertPlaylist(playlist: Playlist) = withContext(Dispatchers.Default) {
        logInfo("Inserting playlist into database: id='${playlist.id}', name='${playlist.name}', channels=${playlist.channels.size}, categories=${playlist.categories.size}")
        try {
            val currentTime = System.currentTimeMillis()
            
            database.transaction {
                // Insert playlist
                logInfo("Inserting playlist record: id='${playlist.id}'")
                database.iptvDatabaseQueries.insertPlaylist(
                    id = playlist.id,
                    name = playlist.name,
                    url = playlist.url,
                    type = playlist.type.name,
                    createdAt = currentTime,
                    updatedAt = currentTime,
                    xtreamServerUrl = playlist.xtreamAccount?.serverUrl,
                    xtreamUsername = playlist.xtreamAccount?.username,
                    xtreamPassword = playlist.xtreamAccount?.password
                )
                
                // Delete old channels for this playlist
                logInfo("Deleting old channels for playlist: id='${playlist.id}'")
                database.iptvDatabaseQueries.deleteChannelsByPlaylistId(playlist.id)
                
                // Insert categories if present
                if (playlist.categories.isNotEmpty()) {
                    logInfo("Inserting ${playlist.categories.size} categories for playlist: id='${playlist.id}'")
                    playlist.categories.forEach { category ->
                        try {
                            database.iptvDatabaseQueries.insertCategory(
                                id = category.id,
                                playlistId = playlist.id,
                                name = category.name,
                                parentId = category.parentId
                            )
                        } catch (e: Exception) {
                            logError("Failed to insert category: id='${category.id}', name='${category.name}'", e)
                            throw e
                        }
                    }
                }
                
                // Insert channels
                logInfo("Inserting ${playlist.channels.size} channels for playlist: id='${playlist.id}'")
                playlist.channels.forEachIndexed { index, channel ->
                    try {
                        database.iptvDatabaseQueries.insertChannel(
                            id = channel.id,
                            playlistId = playlist.id,
                            name = channel.name,
                            url = channel.url,
                            logoUrl = channel.logoUrl,
                            groupName = channel.group,
                            tvgId = null,
                            tvgName = null,
                            epgChannelId = null,
                            categoryId = channel.categoryId
                        )
                    } catch (e: Exception) {
                        logError("Failed to insert channel ${index + 1}/${playlist.channels.size}: id='${channel.id}', name='${channel.name}'", e)
                        throw e
                    }
                }
            }
            logInfo("Successfully inserted playlist: id='${playlist.id}', name='${playlist.name}', channels=${playlist.channels.size}, categories=${playlist.categories.size}")
        } catch (e: Exception) {
            logError("Failed to insert playlist: id='${playlist.id}', name='${playlist.name}'", e)
            throw e
        }
    }
    
    suspend fun deletePlaylist(id: String) = withContext(Dispatchers.Default) {
        logInfo("Deleting playlist from database: id='$id'")
        try {
            database.iptvDatabaseQueries.deletePlaylist(id)
            logInfo("Successfully deleted playlist: id='$id'")
        } catch (e: Exception) {
            logError("Failed to delete playlist: id='$id'", e)
            throw e
        }
    }
    
    private suspend fun getChannelsByPlaylistId(playlistId: String): List<Channel> = 
        withContext(Dispatchers.Default) {
            try {
                val channels = database.iptvDatabaseQueries
                    .selectChannelsByPlaylistId(playlistId)
                    .executeAsList()
                    .map { dbChannel ->
                        Channel(
                            id = dbChannel.id,
                            name = dbChannel.name,
                            url = dbChannel.url,
                            logoUrl = dbChannel.logoUrl,
                            group = dbChannel.groupName,
                            categoryId = dbChannel.categoryId
                        )
                    }
                logInfo("Fetched ${channels.size} channels for playlist: id='$playlistId'")
                channels
            } catch (e: Exception) {
                logError("Failed to fetch channels for playlist: id='$playlistId'", e)
                throw e
            }
        }
    
    suspend fun updatePlaylistName(playlistId: String, newName: String) = withContext(Dispatchers.Default) {
        logInfo("Updating playlist name: id='$playlistId', newName='$newName'")
        try {
            val currentTime = System.currentTimeMillis()
            database.iptvDatabaseQueries.updatePlaylistName(
                name = newName,
                updatedAt = currentTime,
                id = playlistId
            )
            logInfo("Successfully updated playlist name: id='$playlistId', newName='$newName'")
        } catch (e: Exception) {
            logError("Failed to update playlist name: id='$playlistId', newName='$newName'", e)
            throw e
        }
    }
    
    suspend fun getCategoriesByPlaylistId(playlistId: String): List<com.menmapro.iptv.data.model.Category> = 
        withContext(Dispatchers.Default) {
            logInfo("Fetching categories for playlist: id='$playlistId'")
            try {
                val categories = database.iptvDatabaseQueries
                    .selectCategoriesByPlaylistId(playlistId)
                    .executeAsList()
                    .map { dbCategory ->
                        com.menmapro.iptv.data.model.Category(
                            id = dbCategory.id,
                            name = dbCategory.name,
                            parentId = dbCategory.parentId
                        )
                    }
                logInfo("Fetched ${categories.size} categories for playlist: id='$playlistId'")
                categories
            } catch (e: Exception) {
                logError("Failed to fetch categories for playlist: id='$playlistId'", e)
                throw e
            }
        }
    
    suspend fun getChannelsByCategoryId(playlistId: String, categoryId: String): List<Channel> = 
        withContext(Dispatchers.Default) {
            logInfo("Fetching channels by category: playlistId='$playlistId', categoryId='$categoryId'")
            try {
                val channels = database.iptvDatabaseQueries
                    .selectChannelsByCategoryId(playlistId, categoryId)
                    .executeAsList()
                    .map { dbChannel ->
                        Channel(
                            id = dbChannel.id,
                            name = dbChannel.name,
                            url = dbChannel.url,
                            logoUrl = dbChannel.logoUrl,
                            group = dbChannel.groupName,
                            categoryId = dbChannel.categoryId
                        )
                    }
                logInfo("Fetched ${channels.size} channels for category: playlistId='$playlistId', categoryId='$categoryId'")
                channels
            } catch (e: Exception) {
                logError("Failed to fetch channels by category: playlistId='$playlistId', categoryId='$categoryId'", e)
                throw e
            }
        }
    
    suspend fun getCategoryChannelCounts(playlistId: String): Map<String, Int> = 
        withContext(Dispatchers.Default) {
            logInfo("Fetching channel counts by category for playlist: id='$playlistId'")
            try {
                val counts = database.iptvDatabaseQueries
                    .countChannelsByCategory(playlistId)
                    .executeAsList()
                    .associate { it.categoryId to it.channelCount.toInt() }
                logInfo("Fetched channel counts for ${counts.size} categories in playlist: id='$playlistId'")
                counts
            } catch (e: Exception) {
                logError("Failed to fetch category channel counts for playlist: id='$playlistId'", e)
                throw e
            }
        }
}
