package com.menmapro.iptv.data.database.dao

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.menmapro.iptv.data.database.IptvDatabase
import com.menmapro.iptv.data.model.Channel
import com.menmapro.iptv.data.model.Playlist
import com.menmapro.iptv.data.model.PlaylistType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class PlaylistDao(private val database: IptvDatabase) {
    
    fun getAllPlaylists(): Flow<List<Playlist>> {
        return database.iptvDatabaseQueries
            .selectAllPlaylists()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { playlists ->
                playlists.map { dbPlaylist ->
                    val channels = getChannelsByPlaylistId(dbPlaylist.id)
                    Playlist(
                        id = dbPlaylist.id,
                        name = dbPlaylist.name,
                        url = dbPlaylist.url,
                        type = PlaylistType.valueOf(dbPlaylist.type),
                        channels = channels
                    )
                }
            }
    }
    
    suspend fun getPlaylistById(id: String): Playlist? = withContext(Dispatchers.Default) {
        val dbPlaylist = database.iptvDatabaseQueries.selectPlaylistById(id).executeAsOneOrNull()
            ?: return@withContext null
        
        val channels = getChannelsByPlaylistId(id)
        Playlist(
            id = dbPlaylist.id,
            name = dbPlaylist.name,
            url = dbPlaylist.url,
            type = PlaylistType.valueOf(dbPlaylist.type),
            channels = channels
        )
    }
    
    suspend fun insertPlaylist(playlist: Playlist) = withContext(Dispatchers.Default) {
        val currentTime = System.currentTimeMillis()
        
        database.transaction {
            // Insert playlist
            database.iptvDatabaseQueries.insertPlaylist(
                id = playlist.id,
                name = playlist.name,
                url = playlist.url,
                type = playlist.type.name,
                createdAt = currentTime,
                updatedAt = currentTime
            )
            
            // Delete old channels for this playlist
            database.iptvDatabaseQueries.deleteChannelsByPlaylistId(playlist.id)
            
            // Insert channels
            playlist.channels.forEach { channel ->
                database.iptvDatabaseQueries.insertChannel(
                    id = channel.id,
                    playlistId = playlist.id,
                    name = channel.name,
                    url = channel.url,
                    logoUrl = channel.logoUrl,
                    groupName = channel.group,
                    tvgId = null,
                    tvgName = null,
                    epgChannelId = null
                )
            }
        }
    }
    
    suspend fun deletePlaylist(id: String) = withContext(Dispatchers.Default) {
        database.iptvDatabaseQueries.deletePlaylist(id)
    }
    
    private suspend fun getChannelsByPlaylistId(playlistId: String): List<Channel> = 
        withContext(Dispatchers.Default) {
            database.iptvDatabaseQueries
                .selectChannelsByPlaylistId(playlistId)
                .executeAsList()
                .map { dbChannel ->
                    Channel(
                        id = dbChannel.id,
                        name = dbChannel.name,
                        url = dbChannel.url,
                        logoUrl = dbChannel.logoUrl,
                        group = dbChannel.groupName
                    )
                }
        }
}
