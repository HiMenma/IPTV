package com.menmapro.iptv.data.repository

import com.menmapro.iptv.data.database.dao.FavoriteDao
import com.menmapro.iptv.data.model.Channel
import kotlinx.coroutines.flow.Flow

class FavoriteRepository(private val favoriteDao: FavoriteDao) {
    
    fun getAllFavorites(): Flow<List<Channel>> {
        return favoriteDao.getAllFavorites()
    }
    
    suspend fun isFavorite(channelId: String): Boolean {
        return favoriteDao.isFavorite(channelId)
    }
    
    suspend fun toggleFavorite(channelId: String) {
        if (isFavorite(channelId)) {
            favoriteDao.removeFavorite(channelId)
        } else {
            favoriteDao.addFavorite(channelId)
        }
    }
    
    suspend fun addFavorite(channelId: String) {
        favoriteDao.addFavorite(channelId)
    }
    
    suspend fun removeFavorite(channelId: String) {
        favoriteDao.removeFavorite(channelId)
    }
}
