package com.menmapro.iptv.data.repository

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.menmapro.iptv.data.database.IptvDatabase
import com.menmapro.iptv.data.database.dao.PlaylistDao
import com.menmapro.iptv.data.model.Category
import com.menmapro.iptv.data.model.Channel
import com.menmapro.iptv.data.model.Playlist
import com.menmapro.iptv.data.model.PlaylistType
import com.menmapro.iptv.data.network.XtreamClient
import com.menmapro.iptv.data.parser.M3uParser
import io.ktor.client.HttpClient
import kotlinx.coroutines.runBlocking
import kotlin.test.*

class PlaylistRepositoryTest {
    
    private lateinit var driver: SqlDriver
    private lateinit var database: IptvDatabase
    private lateinit var playlistDao: PlaylistDao
    private lateinit var repository: PlaylistRepository
    
    @BeforeTest
    fun setup() {
        // Create an in-memory database for testing
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        IptvDatabase.Schema.create(driver)
        database = IptvDatabase(driver)
        playlistDao = PlaylistDao(database)
        
        // Create repository with mock dependencies
        // We only need PlaylistDao for these tests
        repository = PlaylistRepository(
            httpClient = HttpClient(),
            m3uParser = M3uParser(),
            xtreamClient = XtreamClient(HttpClient()),
            playlistDao = playlistDao
        )
    }
    
    @AfterTest
    fun teardown() {
        driver.close()
    }
    
    // Test renamePlaylist normal flow
    @Test
    fun testRenamePlaylist_normalFlow() = runBlocking {
        // Insert a test playlist
        val playlistId = "test-playlist-1"
        val originalName = "Original Name"
        val playlist = Playlist(
            id = playlistId,
            name = originalName,
            url = "http://test.com/playlist.m3u",
            type = PlaylistType.M3U_URL,
            channels = emptyList()
        )
        playlistDao.insertPlaylist(playlist)
        
        // Rename the playlist
        val newName = "New Name"
        repository.renamePlaylist(playlistId, newName)
        
        // Verify the name was updated
        val updatedPlaylist = repository.getPlaylistById(playlistId)
        assertNotNull(updatedPlaylist)
        assertEquals(newName, updatedPlaylist.name)
        assertEquals(playlistId, updatedPlaylist.id)
        assertEquals(PlaylistType.M3U_URL, updatedPlaylist.type)
    }
    
    // Test renamePlaylist with empty name
    @Test
    fun testRenamePlaylist_emptyName() = runBlocking {
        // Insert a test playlist
        val playlistId = "test-playlist-2"
        val originalName = "Original Name"
        val playlist = Playlist(
            id = playlistId,
            name = originalName,
            type = PlaylistType.M3U_URL,
            channels = emptyList()
        )
        playlistDao.insertPlaylist(playlist)
        
        // Attempt to rename with empty name
        val exception = assertFailsWith<IllegalArgumentException> {
            repository.renamePlaylist(playlistId, "")
        }
        assertEquals("Playlist name cannot be empty", exception.message)
        
        // Verify the name was not changed
        val unchangedPlaylist = repository.getPlaylistById(playlistId)
        assertNotNull(unchangedPlaylist)
        assertEquals(originalName, unchangedPlaylist.name)
    }
    
    // Test renamePlaylist with whitespace-only name
    @Test
    fun testRenamePlaylist_whitespaceOnlyName() = runBlocking {
        // Insert a test playlist
        val playlistId = "test-playlist-3"
        val originalName = "Original Name"
        val playlist = Playlist(
            id = playlistId,
            name = originalName,
            type = PlaylistType.M3U_URL,
            channels = emptyList()
        )
        playlistDao.insertPlaylist(playlist)
        
        // Attempt to rename with whitespace-only name
        val exception = assertFailsWith<IllegalArgumentException> {
            repository.renamePlaylist(playlistId, "   ")
        }
        assertEquals("Playlist name cannot be empty", exception.message)
        
        // Verify the name was not changed
        val unchangedPlaylist = repository.getPlaylistById(playlistId)
        assertNotNull(unchangedPlaylist)
        assertEquals(originalName, unchangedPlaylist.name)
    }
    
    // Test renamePlaylist with name exceeding length limit
    @Test
    fun testRenamePlaylist_nameTooLong() = runBlocking {
        // Insert a test playlist
        val playlistId = "test-playlist-4"
        val originalName = "Original Name"
        val playlist = Playlist(
            id = playlistId,
            name = originalName,
            type = PlaylistType.M3U_URL,
            channels = emptyList()
        )
        playlistDao.insertPlaylist(playlist)
        
        // Attempt to rename with name exceeding 100 characters
        val longName = "a".repeat(101)
        val exception = assertFailsWith<IllegalArgumentException> {
            repository.renamePlaylist(playlistId, longName)
        }
        assertEquals("Playlist name cannot exceed 100 characters", exception.message)
        
        // Verify the name was not changed
        val unchangedPlaylist = repository.getPlaylistById(playlistId)
        assertNotNull(unchangedPlaylist)
        assertEquals(originalName, unchangedPlaylist.name)
    }
    
    // Test renamePlaylist trims whitespace
    @Test
    fun testRenamePlaylist_trimsWhitespace() = runBlocking {
        // Insert a test playlist
        val playlistId = "test-playlist-5"
        val originalName = "Original Name"
        val playlist = Playlist(
            id = playlistId,
            name = originalName,
            type = PlaylistType.M3U_URL,
            channels = emptyList()
        )
        playlistDao.insertPlaylist(playlist)
        
        // Rename with leading/trailing whitespace
        val newNameWithWhitespace = "  New Name  "
        repository.renamePlaylist(playlistId, newNameWithWhitespace)
        
        // Verify the name was trimmed
        val updatedPlaylist = repository.getPlaylistById(playlistId)
        assertNotNull(updatedPlaylist)
        assertEquals("New Name", updatedPlaylist.name)
    }

    
    // Test getCategories for Xtream playlist
    @Test
    fun testGetCategories_xtreamPlaylist() = runBlocking {
        // Insert an Xtream playlist
        val playlistId = "test-playlist-6"
        val playlist = Playlist(
            id = playlistId,
            name = "Xtream Playlist",
            type = PlaylistType.XTREAM,
            channels = emptyList()
        )
        playlistDao.insertPlaylist(playlist)
        
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
            name = "Movies",
            parentId = null
        )
        
        // Get categories
        val categories = repository.getCategories(playlistId)
        
        assertEquals(2, categories.size)
        assertEquals("Movies", categories[0].name) // Sorted by name ASC
        assertEquals("Sports", categories[1].name)
    }
    
    // Test getCategories for M3U playlist returns empty list
    @Test
    fun testGetCategories_m3uPlaylist() = runBlocking {
        // Insert an M3U playlist
        val playlistId = "test-playlist-7"
        val playlist = Playlist(
            id = playlistId,
            name = "M3U Playlist",
            type = PlaylistType.M3U_URL,
            channels = emptyList()
        )
        playlistDao.insertPlaylist(playlist)
        
        // Get categories (should be empty for M3U)
        val categories = repository.getCategories(playlistId)
        
        assertTrue(categories.isEmpty())
    }
    
    // Test getCategories for non-existent playlist
    @Test
    fun testGetCategories_nonExistentPlaylist() = runBlocking {
        // Attempt to get categories for non-existent playlist
        val exception = assertFailsWith<Exception> {
            repository.getCategories("non-existent-id")
        }
        assertTrue(exception.message?.contains("Playlist not found") == true)
    }
    
    // Test getChannelsByCategory filtering logic
    @Test
    fun testGetChannelsByCategory_filtering() = runBlocking {
        // Insert a playlist
        val playlistId = "test-playlist-8"
        val playlist = Playlist(
            id = playlistId,
            name = "Test Playlist",
            type = PlaylistType.XTREAM,
            channels = emptyList()
        )
        playlistDao.insertPlaylist(playlist)
        
        // Insert channels with different categories
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
            name = "HBO",
            url = "http://test.com/hbo",
            logoUrl = null,
            groupName = null,
            tvgId = null,
            tvgName = null,
            epgChannelId = null,
            categoryId = "cat-movies"
        )
        
        // Get channels by sports category
        val sportsChannels = repository.getChannelsByCategory(playlistId, "cat-sports")
        
        assertEquals(2, sportsChannels.size)
        assertEquals("ESPN", sportsChannels[0].name)
        assertEquals("Fox Sports", sportsChannels[1].name)
        assertEquals("cat-sports", sportsChannels[0].categoryId)
        assertEquals("cat-sports", sportsChannels[1].categoryId)
        
        // Get channels by movies category
        val movieChannels = repository.getChannelsByCategory(playlistId, "cat-movies")
        
        assertEquals(1, movieChannels.size)
        assertEquals("HBO", movieChannels[0].name)
        assertEquals("cat-movies", movieChannels[0].categoryId)
    }
    
    // Test getChannelsByCategory with empty result
    @Test
    fun testGetChannelsByCategory_emptyResult() = runBlocking {
        // Insert a playlist
        val playlistId = "test-playlist-9"
        val playlist = Playlist(
            id = playlistId,
            name = "Test Playlist",
            type = PlaylistType.XTREAM,
            channels = emptyList()
        )
        playlistDao.insertPlaylist(playlist)
        
        // Get channels for non-existent category
        val channels = repository.getChannelsByCategory(playlistId, "non-existent-category")
        
        assertTrue(channels.isEmpty())
    }
    
    // Test getCategoryChannelCount accuracy
    @Test
    fun testGetCategoryChannelCount_accuracy() = runBlocking {
        // Insert a playlist
        val playlistId = "test-playlist-10"
        val playlist = Playlist(
            id = playlistId,
            name = "Test Playlist",
            type = PlaylistType.XTREAM,
            channels = emptyList()
        )
        playlistDao.insertPlaylist(playlist)
        
        // Insert channels with different categories
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
            name = "Sky Sports",
            url = "http://test.com/sky",
            logoUrl = null,
            groupName = null,
            tvgId = null,
            tvgName = null,
            epgChannelId = null,
            categoryId = "cat-sports"
        )
        database.iptvDatabaseQueries.insertChannel(
            id = "ch-4",
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
            id = "ch-5",
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
        // Insert a channel without category (should not be counted)
        database.iptvDatabaseQueries.insertChannel(
            id = "ch-6",
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
        
        // Get category channel counts
        val counts = repository.getCategoryChannelCount(playlistId)
        
        assertEquals(3, counts.size) // Only categories with channels
        assertEquals(3, counts["cat-sports"])
        assertEquals(1, counts["cat-movies"])
        assertEquals(1, counts["cat-news"])
        // Uncategorized channels (with null categoryId) should not be counted
    }
    
    // Test getCategoryChannelCount with empty playlist
    @Test
    fun testGetCategoryChannelCount_emptyPlaylist() = runBlocking {
        // Insert a playlist with no channels
        val playlistId = "test-playlist-11"
        val playlist = Playlist(
            id = playlistId,
            name = "Empty Playlist",
            type = PlaylistType.XTREAM,
            channels = emptyList()
        )
        playlistDao.insertPlaylist(playlist)
        
        // Get category channel counts
        val counts = repository.getCategoryChannelCount(playlistId)
        
        assertTrue(counts.isEmpty())
    }
}
