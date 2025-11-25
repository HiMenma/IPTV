package com.menmapro.iptv.data.repository

import com.menmapro.iptv.data.database.dao.PlaylistDao
import com.menmapro.iptv.data.model.Channel
import com.menmapro.iptv.data.model.Playlist
import com.menmapro.iptv.data.model.PlaylistType
import com.menmapro.iptv.data.model.XtreamAccount
import com.menmapro.iptv.data.network.XtreamClient
import com.menmapro.iptv.data.parser.M3uParser
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.flow.Flow

class PlaylistRepository(
    private val httpClient: HttpClient,
    private val m3uParser: M3uParser,
    private val xtreamClient: XtreamClient,
    private val playlistDao: PlaylistDao
) {
    
    fun getAllPlaylists(): Flow<List<Playlist>> {
        return playlistDao.getAllPlaylists()
    }

    suspend fun addM3uUrl(name: String, url: String) {
        try {
            val content = httpClient.get(url).bodyAsText()
            val channels = m3uParser.parse(content)
            val playlist = Playlist(
                id = url.hashCode().toString(),
                name = name,
                url = url,
                type = PlaylistType.M3U_URL,
                channels = channels
            )
            playlistDao.insertPlaylist(playlist)
        } catch (e: Exception) {
            println("Error adding M3U URL: $e")
            throw e
        }
    }

    suspend fun addXtreamAccount(account: XtreamAccount) {
        if (xtreamClient.authenticate(account)) {
            // 获取分类列表
            val categories = xtreamClient.getLiveCategories(account)
            
            // 获取频道列表
            val channels = xtreamClient.getLiveStreams(account)
            
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
            playlistDao.insertPlaylist(playlist)
        } else {
            throw Exception("Failed to authenticate Xtream account")
        }
    }
    
    suspend fun addM3uContent(name: String, content: String) {
        val channels = m3uParser.parse(content)
        val playlist = Playlist(
            id = name.hashCode().toString(),
            name = name,
            type = PlaylistType.M3U_FILE,
            channels = channels
        )
        playlistDao.insertPlaylist(playlist)
    }
    
    suspend fun deletePlaylist(id: String) {
        playlistDao.deletePlaylist(id)
    }
    
    suspend fun getPlaylistById(id: String): Playlist? {
        return playlistDao.getPlaylistById(id)
    }
}

