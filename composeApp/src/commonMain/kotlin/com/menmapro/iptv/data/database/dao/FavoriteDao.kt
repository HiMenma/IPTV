package com.menmapro.iptv.data.database.dao

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.menmapro.iptv.data.database.IptvDatabase
import com.menmapro.iptv.data.model.Channel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class FavoriteDao(private val database: IptvDatabase) {
    
    fun getAllFavorites(): Flow<List<Channel>> {
        return database.iptvDatabaseQueries
            .selectAllFavorites()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { dbChannels ->
                dbChannels.map { dbChannel ->
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
    
    suspend fun isFavorite(channelId: String): Boolean = withContext(Dispatchers.Default) {
        database.iptvDatabaseQueries.isFavorite(channelId).executeAsOne()
    }
    
    suspend fun addFavorite(channelId: String) = withContext(Dispatchers.Default) {
        database.iptvDatabaseQueries.insertFavorite(
            channelId = channelId,
            addedAt = System.currentTimeMillis()
        )
    }
    
    suspend fun removeFavorite(channelId: String) = withContext(Dispatchers.Default) {
        database.iptvDatabaseQueries.deleteFavorite(channelId)
    }
}
