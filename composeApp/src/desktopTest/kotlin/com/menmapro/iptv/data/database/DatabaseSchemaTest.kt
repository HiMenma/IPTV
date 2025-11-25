package com.menmapro.iptv.data.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.menmapro.iptv.data.database.IptvDatabase
import kotlin.test.*

class DatabaseSchemaTest {
    
    private lateinit var driver: SqlDriver
    private lateinit var database: IptvDatabase
    
    @BeforeTest
    fun setup() {
        // Create an in-memory database for testing
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        IptvDatabase.Schema.create(driver)
        database = IptvDatabase(driver)
    }
    
    @AfterTest
    fun teardown() {
        driver.close()
    }
    
    @Test
    fun testCategoryTableCreationAndQuery() {
        // Insert a test playlist
        val playlistId = "test-playlist-1"
        database.iptvDatabaseQueries.insertPlaylist(
            id = playlistId,
            name = "Test Playlist",
            url = "http://test.com/playlist.m3u",
            type = "M3U_URL",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        
        // Insert categories
        database.iptvDatabaseQueries.insertCategory(
            id = "cat-1",
            playlistId = playlistId,
            name = "Sports",
            parentId = null
        )
        
        database.iptvDatabaseQueries.insertCategory(
            id = "cat-2",
            playlistId = playlistId,
            name = "News",
            parentId = null
        )
        
        // Query categories
        val categories = database.iptvDatabaseQueries
            .selectCategoriesByPlaylistId(playlistId)
            .executeAsList()
        
        assertEquals(2, categories.size)
        assertEquals("News", categories[0].name) // Sorted by name ASC
        assertEquals("Sports", categories[1].name)
    }
    
    @Test
    fun testPlaylistRenameQuery() {
        // Insert a test playlist
        val playlistId = "test-playlist-2"
        val originalName = "Original Name"
        val createdAt = System.currentTimeMillis()
        
        database.iptvDatabaseQueries.insertPlaylist(
            id = playlistId,
            name = originalName,
            url = null,
            type = "XTREAM",
            createdAt = createdAt,
            updatedAt = createdAt
        )
        
        // Verify original name
        val originalPlaylist = database.iptvDatabaseQueries
            .selectPlaylistById(playlistId)
            .executeAsOne()
        assertEquals(originalName, originalPlaylist.name)
        
        // Update the name
        val newName = "Updated Name"
        val updatedAt = System.currentTimeMillis()
        database.iptvDatabaseQueries.updatePlaylistName(
            name = newName,
            updatedAt = updatedAt,
            id = playlistId
        )
        
        // Verify updated name
        val updatedPlaylist = database.iptvDatabaseQueries
            .selectPlaylistById(playlistId)
            .executeAsOne()
        assertEquals(newName, updatedPlaylist.name)
        assertEquals(updatedAt, updatedPlaylist.updatedAt)
        assertEquals(createdAt, updatedPlaylist.createdAt) // createdAt should not change
    }
    
    @Test
    fun testCategoryChannelCountQuery() {
        // Insert a test playlist
        val playlistId = "test-playlist-3"
        database.iptvDatabaseQueries.insertPlaylist(
            id = playlistId,
            name = "Test Playlist",
            url = null,
            type = "XTREAM",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        
        // Insert categories
        database.iptvDatabaseQueries.insertCategory(
            id = "cat-sports",
            playlistId = playlistId,
            name = "Sports",
            parentId = null
        )
        
        database.iptvDatabaseQueries.insertCategory(
            id = "cat-news",
            playlistId = playlistId,
            name = "News",
            parentId = null
        )
        
        // Insert channels with categories
        database.iptvDatabaseQueries.insertChannel(
            id = "ch-1",
            playlistId = playlistId,
            name = "ESPN",
            url = "http://test.com/espn",
            logoUrl = null,
            groupName = null,
            tvgId = null,
            tvgName = null,
            epgChannelId = null,
            categoryId = "cat-sports"
        )
        
        database.iptvDatabaseQueries.insertChannel(
            id = "ch-2",
            playlistId = playlistId,
            name = "Fox Sports",
            url = "http://test.com/fox",
            logoUrl = null,
            groupName = null,
            tvgId = null,
            tvgName = null,
            epgChannelId = null,
            categoryId = "cat-sports"
        )
        
        database.iptvDatabaseQueries.insertChannel(
            id = "ch-3",
            playlistId = playlistId,
            name = "CNN",
            url = "http://test.com/cnn",
            logoUrl = null,
            groupName = null,
            tvgId = null,
            tvgName = null,
            epgChannelId = null,
            categoryId = "cat-news"
        )
        
        // Insert a channel without category
        database.iptvDatabaseQueries.insertChannel(
            id = "ch-4",
            playlistId = playlistId,
            name = "Uncategorized",
            url = "http://test.com/uncat",
            logoUrl = null,
            groupName = null,
            tvgId = null,
            tvgName = null,
            epgChannelId = null,
            categoryId = null
        )
        
        // Query channel counts by category
        val counts = database.iptvDatabaseQueries
            .countChannelsByCategory(playlistId)
            .executeAsList()
        
        assertEquals(2, counts.size) // Only categories with channels
        
        val sportsCount = counts.find { it.categoryId == "cat-sports" }
        assertNotNull(sportsCount)
        assertEquals(2, sportsCount.channelCount)
        
        val newsCount = counts.find { it.categoryId == "cat-news" }
        assertNotNull(newsCount)
        assertEquals(1, newsCount.channelCount)
    }
    
    @Test
    fun testSelectChannelsByCategoryId() {
        // Insert a test playlist
        val playlistId = "test-playlist-4"
        database.iptvDatabaseQueries.insertPlaylist(
            id = playlistId,
            name = "Test Playlist",
            url = null,
            type = "XTREAM",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        
        // Insert category
        database.iptvDatabaseQueries.insertCategory(
            id = "cat-movies",
            playlistId = playlistId,
            name = "Movies",
            parentId = null
        )
        
        // Insert channels
        database.iptvDatabaseQueries.insertChannel(
            id = "ch-movie-1",
            playlistId = playlistId,
            name = "HBO",
            url = "http://test.com/hbo",
            logoUrl = null,
            groupName = null,
            tvgId = null,
            tvgName = null,
            epgChannelId = null,
            categoryId = "cat-movies"
        )
        
        database.iptvDatabaseQueries.insertChannel(
            id = "ch-movie-2",
            playlistId = playlistId,
            name = "Showtime",
            url = "http://test.com/showtime",
            logoUrl = null,
            groupName = null,
            tvgId = null,
            tvgName = null,
            epgChannelId = null,
            categoryId = "cat-movies"
        )
        
        database.iptvDatabaseQueries.insertChannel(
            id = "ch-sports-1",
            playlistId = playlistId,
            name = "ESPN",
            url = "http://test.com/espn",
            logoUrl = null,
            groupName = null,
            tvgId = null,
            tvgName = null,
            epgChannelId = null,
            categoryId = "cat-sports"
        )
        
        // Query channels by category
        val movieChannels = database.iptvDatabaseQueries
            .selectChannelsByCategoryId(playlistId, "cat-movies")
            .executeAsList()
        
        assertEquals(2, movieChannels.size)
        assertEquals("HBO", movieChannels[0].name)
        assertEquals("Showtime", movieChannels[1].name)
    }
    
    @Test
    fun testChannelCategoryIdField() {
        // Insert a test playlist
        val playlistId = "test-playlist-5"
        database.iptvDatabaseQueries.insertPlaylist(
            id = playlistId,
            name = "Test Playlist",
            url = null,
            type = "XTREAM",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        
        // Insert channel with categoryId
        val channelId = "ch-with-cat"
        val categoryId = "cat-test"
        database.iptvDatabaseQueries.insertChannel(
            id = channelId,
            playlistId = playlistId,
            name = "Test Channel",
            url = "http://test.com/channel",
            logoUrl = null,
            groupName = null,
            tvgId = null,
            tvgName = null,
            epgChannelId = null,
            categoryId = categoryId
        )
        
        // Verify categoryId is stored
        val channel = database.iptvDatabaseQueries
            .selectChannelById(channelId)
            .executeAsOne()
        
        assertEquals(categoryId, channel.categoryId)
        
        // Insert channel without categoryId
        val channelId2 = "ch-without-cat"
        database.iptvDatabaseQueries.insertChannel(
            id = channelId2,
            playlistId = playlistId,
            name = "Test Channel 2",
            url = "http://test.com/channel2",
            logoUrl = null,
            groupName = null,
            tvgId = null,
            tvgName = null,
            epgChannelId = null,
            categoryId = null
        )
        
        // Verify categoryId is null
        val channel2 = database.iptvDatabaseQueries
            .selectChannelById(channelId2)
            .executeAsOne()
        
        assertNull(channel2.categoryId)
    }
}
