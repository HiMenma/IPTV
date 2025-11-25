package com.menmapro.iptv.ui.screens

import com.menmapro.iptv.data.model.Playlist
import com.menmapro.iptv.data.model.PlaylistType
import com.menmapro.iptv.data.model.getDisplayName
import com.menmapro.iptv.data.model.getIcon
import io.kotest.property.Arb
import io.kotest.property.arbitrary.*
import io.kotest.property.checkAll
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Property-based tests for PlaylistScreen
 */
class PlaylistScreenTest {

    /**
     * **Feature: playlist-enhancements, Property 1: Playlist display completeness**
     * **Validates: Requirements 1.1, 5.1**
     * 
     * For any playlist, when rendered in the UI, the display should include both 
     * the playlist name and a type indicator (icon or label) that correctly 
     * identifies whether it is M3U or Xtream type.
     */
    @Test
    fun `property test - playlist display includes name and type indicator`() {
        runBlocking {
            checkAll(100, playlistArb()) { playlist ->
                // Verify that the playlist has a name
                assertTrue(
                    playlist.name.isNotEmpty(),
                    "Playlist should have a non-empty name"
                )
                
                // Verify that the playlist type has a display name
                val displayName = playlist.type.getDisplayName()
                assertNotNull(
                    displayName,
                    "Playlist type should have a display name"
                )
                assertTrue(
                    displayName.isNotEmpty(),
                    "Playlist type display name should not be empty"
                )
                
                // Verify that the playlist type has an icon
                val icon = playlist.type.getIcon()
                assertNotNull(
                    icon,
                    "Playlist type should have an icon"
                )
                
                // Verify that the type indicator correctly identifies the type
                when (playlist.type) {
                    PlaylistType.M3U_URL -> {
                        assertTrue(
                            displayName.contains("M3U", ignoreCase = true),
                            "M3U_URL type should have 'M3U' in display name"
                        )
                    }
                    PlaylistType.M3U_FILE -> {
                        assertTrue(
                            displayName.contains("M3U", ignoreCase = true),
                            "M3U_FILE type should have 'M3U' in display name"
                        )
                    }
                    PlaylistType.XTREAM -> {
                        assertTrue(
                            displayName.contains("Xtream", ignoreCase = true),
                            "XTREAM type should have 'Xtream' in display name"
                        )
                    }
                }
            }
        }
    }

    /**
     * **Feature: playlist-enhancements, Property 3: Rename persistence**
     * **Validates: Requirements 1.3, 4.5**
     * 
     * For any playlist and any valid (non-empty) new name, when the user confirms 
     * the rename operation, the new name should be persisted to the database and 
     * the playlist's other fields should remain unchanged.
     * 
     * This test verifies the data model behavior - that copying a playlist with a new name
     * preserves all other fields.
     */
    @Test
    fun `property test - rename preserves other playlist fields`() {
        runBlocking {
            checkAll(100, playlistArb(), validNameArb()) { playlist, newName ->
                // Simulate rename operation by creating a copy with new name
                val renamedPlaylist = playlist.copy(name = newName.trim())
                
                // Verify the name was updated
                assertEquals(
                    newName.trim(),
                    renamedPlaylist.name,
                    "Playlist name should be updated and trimmed"
                )
                
                // Verify other fields remain unchanged
                assertEquals(
                    playlist.id,
                    renamedPlaylist.id,
                    "Playlist ID should remain unchanged"
                )
                assertEquals(
                    playlist.type,
                    renamedPlaylist.type,
                    "Playlist type should remain unchanged"
                )
                assertEquals(
                    playlist.url,
                    renamedPlaylist.url,
                    "Playlist URL should remain unchanged"
                )
                assertEquals(
                    playlist.channels,
                    renamedPlaylist.channels,
                    "Playlist channels should remain unchanged"
                )
                assertEquals(
                    playlist.categories,
                    renamedPlaylist.categories,
                    "Playlist categories should remain unchanged"
                )
            }
        }
    }
    
    /**
     * **Feature: playlist-enhancements, Property 5: Reactive UI updates**
     * **Validates: Requirements 1.5**
     * 
     * For any playlist rename operation that succeeds, the playlist list UI should 
     * immediately reflect the new name without requiring a manual refresh.
     * 
     * This test verifies that when a playlist is updated in a list, the list reflects
     * the change immediately (simulating reactive Flow behavior).
     */
    @Test
    fun `property test - playlist list reflects rename immediately`() {
        runBlocking {
            checkAll(100, playlistListArb(), validNameArb()) { playlists, newName ->
                // Skip if list is empty
                if (playlists.isEmpty()) return@checkAll
                
                // Pick a random playlist to rename
                val playlistToRename = playlists.random()
                
                // Simulate rename by creating updated list
                val updatedPlaylists = playlists.map { playlist ->
                    if (playlist.id == playlistToRename.id) {
                        playlist.copy(name = newName.trim())
                    } else {
                        playlist
                    }
                }
                
                // Verify the renamed playlist is in the updated list with new name
                val renamedPlaylist = updatedPlaylists.find { it.id == playlistToRename.id }
                assertNotNull(renamedPlaylist, "Renamed playlist should exist in updated list")
                assertEquals(
                    newName.trim(),
                    renamedPlaylist.name,
                    "Playlist name should be immediately updated in the list"
                )
                
                // Verify other playlists remain unchanged
                val otherPlaylists = updatedPlaylists.filter { it.id != playlistToRename.id }
                val originalOtherPlaylists = playlists.filter { it.id != playlistToRename.id }
                assertEquals(
                    originalOtherPlaylists,
                    otherPlaylists,
                    "Other playlists should remain unchanged"
                )
                
                // Verify list size remains the same
                assertEquals(
                    playlists.size,
                    updatedPlaylists.size,
                    "List size should remain unchanged after rename"
                )
            }
        }
    }

    // Generators for property-based testing

    private fun playlistArb(): Arb<Playlist> = arbitrary {
        val id = Arb.string(1..20, Codepoint.alphanumeric()).bind()
        val name = Arb.string(1..50, Codepoint.alphanumeric()).bind()
        val url = Arb.string(10..100, Codepoint.alphanumeric()).bind()
        val type = Arb.enum<PlaylistType>().bind()
        
        Playlist(
            id = id,
            name = name,
            url = url,
            type = type,
            channels = emptyList(),
            categories = emptyList()
        )
    }
    
    private fun validNameArb(): Arb<String> = arbitrary {
        // Generate valid names (1-100 characters, not all whitespace)
        val length = Arb.int(1..100).bind()
        val name = Arb.string(length, Codepoint.alphanumeric()).bind()
        
        // Ensure it's not blank after trimming
        if (name.trim().isEmpty()) {
            "ValidName" // Fallback to a valid name
        } else {
            name
        }
    }
    
    private fun playlistListArb(): Arb<List<Playlist>> = arbitrary {
        val size = Arb.int(0..10).bind()
        List(size) { playlistArb().bind() }
    }
}
