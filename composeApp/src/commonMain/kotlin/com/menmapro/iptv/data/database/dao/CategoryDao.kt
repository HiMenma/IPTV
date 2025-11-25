package com.menmapro.iptv.data.database.dao

import com.menmapro.iptv.data.database.IptvDatabase
import com.menmapro.iptv.data.model.Category
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CategoryDao(private val database: IptvDatabase) {
    
    private fun logInfo(message: String) {
        println("[CategoryDao] INFO: $message")
    }
    
    private fun logError(message: String, error: Throwable? = null) {
        println("[CategoryDao] ERROR: $message")
        error?.let { 
            println("[CategoryDao] ERROR: ${it.message}")
            println("[CategoryDao] ERROR: Stack trace: ${it.stackTraceToString()}")
        }
    }
    
    suspend fun selectCategoriesByPlaylistId(playlistId: String): List<Category> = 
        withContext(Dispatchers.Default) {
            logInfo("Fetching categories for playlist: id='$playlistId'")
            try {
                val categories = database.iptvDatabaseQueries
                    .selectCategoriesByPlaylistId(playlistId)
                    .executeAsList()
                    .map { dbCategory ->
                        Category(
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
    
    suspend fun insertCategory(category: Category, playlistId: String) = 
        withContext(Dispatchers.Default) {
            logInfo("Inserting category: id='${category.id}', name='${category.name}', playlistId='$playlistId'")
            try {
                database.iptvDatabaseQueries.insertCategory(
                    id = category.id,
                    playlistId = playlistId,
                    name = category.name,
                    parentId = category.parentId
                )
                logInfo("Successfully inserted category: id='${category.id}', name='${category.name}'")
            } catch (e: Exception) {
                logError("Failed to insert category: id='${category.id}', name='${category.name}'", e)
                throw e
            }
        }
    
    suspend fun countChannelsByCategory(playlistId: String): Map<String, Int> = 
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
