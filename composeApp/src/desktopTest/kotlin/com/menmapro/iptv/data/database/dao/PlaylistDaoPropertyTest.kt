package com.menmapro.iptv.data.database.dao

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.menmapro.iptv.data.database.IptvDatabase
import com.menmapro.iptv.data.model.Channel
import com.menmapro.iptv.data.model.Playlist
import com.menmapro.iptv.data.model.PlaylistType
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.*
import io.kotest.property.checkAll
import kotlinx.coroutines.runBlocking
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

/**
 * Property-based tests for PlaylistDao
 * 
 * Feature: playlist-enhancements, Property 13: Playlist data completeness
 * Validates: Requirements 4.1, 4.3
 */
class PlaylistDaoPropertyTest {
    
    private lateinit var driver: SqlDriver
    private lateinit var database: IptvDatabase
    private lateinit var playlistDao: PlaylistDao
    
    @BeforeTest
    fun setup() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        IptvDatabase.Schema.create(driver)
        database = IptvDatabase(driver)
        playlistDao = PlaylistDao(database)
    }
    
    @AfterTest
    fun teardown() {
        driver.close()
    }
    
    // Generators for property-based testing
    
    private fun arbPlaylistType(): Arb<PlaylistType> = Arb.enum<PlaylistType>()
    
    private var channelIdCounter = java.util.concurrent.atomic.AtomicLong(0)
    private var playlistIdCounter = java.util.concurrent.atomic.AtomicLong(0)
    
    private fun arbChannel(): Arb<Channel> = arbitrary {
        val uniqueId = "channel-${java.util.UUID.randomUUID()}"
        Channel(
            id = uniqueId,
            name = Arb.string(1..100).bind(),
            url = "http://example.com/${Arb.string(1..20).bind()}",
            logoUrl = Arb.string(1..100).orNull().bind(),
            group = Arb.string(1..50).orNull().bind(),
            categoryId = Arb.string(1..50).orNull().bind()
        )
    }
    
    private fun arbPlaylist(): Arb<Playlist> = arbitrary {
        val uniqueId = "playlist-${java.util.UUID.randomUUID()}"
        Playlist(
            id = uniqueId,
            name = Arb.string(1..100).bind(),
            url = Arb.string(1..200).orNull().bind(),
            type = arbPlaylistType().bind(),
            channels = Arb.list(arbChannel(), 0..10).bind(),
            categories = emptyList() // Categories tested separately
        )
    }
    
    /**
     * Property 13: Playlist data completeness
     * For any playlist stored in or retrieved from the database, 
     * the data should include all required fields: id, name, type, and timestamps.
     */
    @Test
    fun `property 13 - playlist data completeness after insert and retrieve`() {
        runBlocking {
            checkAll(100, arbPlaylist()) { originalPlaylist ->
                // Ensure all channels have unique IDs (fix for shrinking duplicates)
                val playlist = originalPlaylist.copy(
                    channels = originalPlaylist.channels.mapIndexed { index, channel ->
                        channel.copy(id = "${channel.id}-$index")
                    }
                )
                
                // Insert the playlist
                playlistDao.insertPlaylist(playlist)
                
                // Retrieve the playlist
                val retrieved = playlistDao.getPlaylistById(playlist.id)
                
                // Verify all required fields are present and correct
                retrieved shouldNotBe null
                retrieved!!.id shouldBe playlist.id
                retrieved.name shouldBe playlist.name
                retrieved.type shouldBe playlist.type
                retrieved.url shouldBe playlist.url
                
                // Verify channels are preserved
                retrieved.channels.size shouldBe playlist.channels.size
                
                // Clean up for next iteration
                playlistDao.deletePlaylist(playlist.id)
            }
        }
    }
    
    /**
     * Property 13 (variant): Playlist name update preserves other fields
     * For any playlist, when updating the name, all other fields should remain unchanged.
     */
    @Test
    fun `property 13 - playlist name update preserves other fields`() {
        runBlocking {
            checkAll(100, arbPlaylist(), Arb.string(1..100)) { originalPlaylist, newName ->
                // Ensure all channels have unique IDs (fix for shrinking duplicates)
                val playlist = originalPlaylist.copy(
                    channels = originalPlaylist.channels.mapIndexed { index, channel ->
                        channel.copy(id = "${channel.id}-$index")
                    }
                )
                
                // Insert the playlist
                playlistDao.insertPlaylist(playlist)
                
                // Update the name
                playlistDao.updatePlaylistName(playlist.id, newName)
                
                // Retrieve the playlist
                val retrieved = playlistDao.getPlaylistById(playlist.id)
                
                // Verify name is updated
                retrieved shouldNotBe null
                retrieved!!.name shouldBe newName
                
                // Verify other fields are unchanged
                retrieved.id shouldBe playlist.id
                retrieved.type shouldBe playlist.type
                retrieved.url shouldBe playlist.url
                retrieved.channels.size shouldBe playlist.channels.size
                
                // Clean up for next iteration
                playlistDao.deletePlaylist(playlist.id)
            }
        }
    }
    
    /**
     * Property 14: Xtream channel category association
     * For any Xtream channel stored in the database, 
     * the channel data should include both categoryId and category name fields.
     */
    @Test
    fun `property 14 - xtream channel category association`() {
        runBlocking {
            // Generator for Xtream channels with category information
            val arbXtreamChannel = arbitrary {
                val categoryId = Arb.string(1..50).bind()
                val uniqueId = "xtream-channel-${java.util.UUID.randomUUID()}"
                Channel(
                    id = uniqueId,
                    name = Arb.string(1..100).bind(),
                    url = "http://example.com/${Arb.string(1..20).bind()}",
                    logoUrl = Arb.string(1..100).orNull().bind(),
                    group = Arb.string(1..50).bind(), // Category name stored in group
                    categoryId = categoryId
                )
            }
            
            val arbXtreamPlaylist = arbitrary {
                val uniqueId = "xtream-playlist-${java.util.UUID.randomUUID()}"
                Playlist(
                    id = uniqueId,
                    name = Arb.string(1..100).bind(),
                    url = Arb.string(1..200).orNull().bind(),
                    type = PlaylistType.XTREAM,
                    channels = Arb.list(arbXtreamChannel, 1..10).bind(),
                    categories = emptyList()
                )
            }
            
            checkAll(100, arbXtreamPlaylist) { originalPlaylist ->
                // Ensure all channels have unique IDs (fix for shrinking duplicates)
                val playlist = originalPlaylist.copy(
                    channels = originalPlaylist.channels.mapIndexed { index, channel ->
                        channel.copy(id = "${channel.id}-$index")
                    }
                )
                
                // Insert the Xtream playlist
                playlistDao.insertPlaylist(playlist)
                
                // Retrieve the playlist
                val retrieved = playlistDao.getPlaylistById(playlist.id)
                
                // Verify all channels have categoryId and group (category name)
                retrieved shouldNotBe null
                retrieved!!.channels.forEach { channel ->
                    // Verify categoryId is present
                    channel.categoryId shouldNotBe null
                    
                    // Verify group (category name) is present
                    channel.group shouldNotBe null
                }
                
                // Verify the number of channels is preserved
                retrieved.channels.size shouldBe playlist.channels.size
                
                // Clean up for next iteration
                playlistDao.deletePlaylist(playlist.id)
            }
        }
    }
}
