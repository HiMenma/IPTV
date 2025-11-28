package com.menmapro.iptv.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import com.menmapro.iptv.player.PlayerImplementationType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okio.Path.Companion.toPath
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Test user preferences compatibility during libmpv migration
 * 
 * This test verifies:
 * - Existing user preferences still work after migration
 * - Player selection preferences can be saved and loaded
 * - Volume preferences can be saved and loaded
 * - No data loss occurs during migration
 * 
 * **Validates: Requirements 8.5**
 */
class UserPreferencesCompatibilityTest {
    
    private lateinit var testDataStore: DataStore<Preferences>
    private lateinit var testDataStoreFile: File
    
    // Preference keys that would be used in the application
    private val PLAYER_TYPE_KEY = stringPreferencesKey("player_type")
    private val VOLUME_KEY = floatPreferencesKey("volume")
    private val LAST_PLAYED_URL_KEY = stringPreferencesKey("last_played_url")
    private val AUTO_FALLBACK_KEY = booleanPreferencesKey("auto_fallback")
    
    @Before
    fun setup() {
        // Create a temporary DataStore for testing
        val tempDir = File(System.getProperty("java.io.tmpdir"), "iptv_test_${System.currentTimeMillis()}")
        tempDir.mkdirs()
        testDataStoreFile = File(tempDir, "test_preferences.preferences_pb")
        
        testDataStore = PreferenceDataStoreFactory.createWithPath(
            produceFile = { testDataStoreFile.absolutePath.toPath() }
        )
    }
    
    @After
    fun cleanup() {
        // Clean up test files
        testDataStoreFile.parentFile?.deleteRecursively()
    }
    
    /**
     * Test that player selection preferences can be saved and loaded correctly
     * 
     * Verifies that the system can:
     * - Save player type preference
     * - Load player type preference
     * - Handle all player types (VLC, FFMPEG, LIBMPV)
     */
    @Test
    fun `test player selection preferences work correctly`() = runBlocking {
        // Test saving and loading LIBMPV player type
        testDataStore.edit { preferences ->
            preferences[PLAYER_TYPE_KEY] = PlayerImplementationType.LIBMPV.name
        }
        
        val loadedPreferences = testDataStore.data.first()
        val playerType = loadedPreferences[PLAYER_TYPE_KEY]
        
        assertNotNull(playerType, "Player type should be saved")
        assertEquals(PlayerImplementationType.LIBMPV.name, playerType, "Player type should be LIBMPV")
        
        // Verify we can convert back to enum
        val playerEnum = PlayerImplementationType.valueOf(playerType)
        assertEquals(PlayerImplementationType.LIBMPV, playerEnum, "Should convert to LIBMPV enum")
    }
    
    /**
     * Test that volume preferences can be saved and loaded correctly
     * 
     * Verifies that the system can:
     * - Save volume level (0.0 to 1.0)
     * - Load volume level
     * - Maintain precision
     */
    @Test
    fun `test volume preferences work correctly`() = runBlocking {
        val testVolume = 0.75f
        
        // Save volume preference
        testDataStore.edit { preferences ->
            preferences[VOLUME_KEY] = testVolume
        }
        
        // Load volume preference
        val loadedPreferences = testDataStore.data.first()
        val volume = loadedPreferences[VOLUME_KEY]
        
        assertNotNull(volume, "Volume should be saved")
        assertEquals(testVolume, volume, "Volume should match saved value")
    }
    
    /**
     * Test that multiple preferences can coexist without data loss
     * 
     * Verifies that the system can:
     * - Save multiple preferences simultaneously
     * - Load all preferences correctly
     * - No data loss when updating one preference
     */
    @Test
    fun `test multiple preferences coexist without data loss`() = runBlocking {
        // Save multiple preferences
        testDataStore.edit { preferences ->
            preferences[PLAYER_TYPE_KEY] = PlayerImplementationType.LIBMPV.name
            preferences[VOLUME_KEY] = 0.8f
            preferences[LAST_PLAYED_URL_KEY] = "http://example.com/stream.m3u8"
            preferences[AUTO_FALLBACK_KEY] = true
        }
        
        // Load all preferences
        val loadedPreferences = testDataStore.data.first()
        
        assertEquals(PlayerImplementationType.LIBMPV.name, loadedPreferences[PLAYER_TYPE_KEY])
        assertEquals(0.8f, loadedPreferences[VOLUME_KEY])
        assertEquals("http://example.com/stream.m3u8", loadedPreferences[LAST_PLAYED_URL_KEY])
        assertEquals(true, loadedPreferences[AUTO_FALLBACK_KEY])
        
        // Update one preference and verify others remain intact
        testDataStore.edit { preferences ->
            preferences[VOLUME_KEY] = 0.5f
        }
        
        val updatedPreferences = testDataStore.data.first()
        assertEquals(PlayerImplementationType.LIBMPV.name, updatedPreferences[PLAYER_TYPE_KEY], "Player type should remain")
        assertEquals(0.5f, updatedPreferences[VOLUME_KEY], "Volume should be updated")
        assertEquals("http://example.com/stream.m3u8", updatedPreferences[LAST_PLAYED_URL_KEY], "URL should remain")
        assertEquals(true, updatedPreferences[AUTO_FALLBACK_KEY], "Auto fallback should remain")
    }
    
    /**
     * Test migration scenario: existing preferences remain valid
     * 
     * Simulates a migration where old preferences (e.g., FFMPEG player)
     * are still valid and can be read after migration to LIBMPV
     */
    @Test
    fun `test existing FFMPEG preferences remain valid after migration`() = runBlocking {
        // Simulate existing preferences before migration
        testDataStore.edit { preferences ->
            preferences[PLAYER_TYPE_KEY] = PlayerImplementationType.FFMPEG.name
            preferences[VOLUME_KEY] = 0.9f
        }
        
        // Verify old preferences can still be read
        val loadedPreferences = testDataStore.data.first()
        val playerType = loadedPreferences[PLAYER_TYPE_KEY]
        val volume = loadedPreferences[VOLUME_KEY]
        
        assertNotNull(playerType, "Old player type should be readable")
        assertNotNull(volume, "Old volume should be readable")
        
        // Verify we can still parse the old player type
        val playerEnum = PlayerImplementationType.valueOf(playerType)
        assertEquals(PlayerImplementationType.FFMPEG, playerEnum, "Should still support FFMPEG enum")
        assertEquals(0.9f, volume, "Volume should be preserved")
    }
    
    /**
     * Test that all player types can be stored and retrieved
     * 
     * Verifies backward compatibility with all player implementation types
     */
    @Test
    fun `test all player types can be stored and retrieved`() = runBlocking {
        val playerTypes = listOf(
            PlayerImplementationType.VLC,
            PlayerImplementationType.FFMPEG,
            PlayerImplementationType.LIBMPV
        )
        
        for (playerType in playerTypes) {
            // Save player type
            testDataStore.edit { preferences ->
                preferences[PLAYER_TYPE_KEY] = playerType.name
            }
            
            // Load and verify
            val loadedPreferences = testDataStore.data.first()
            val loadedType = loadedPreferences[PLAYER_TYPE_KEY]
            
            assertNotNull(loadedType, "Player type $playerType should be saved")
            assertEquals(playerType.name, loadedType, "Player type should match")
            
            // Verify enum conversion works
            val playerEnum = PlayerImplementationType.valueOf(loadedType)
            assertEquals(playerType, playerEnum, "Should convert to correct enum")
        }
    }
    
    /**
     * Test volume boundary values
     * 
     * Verifies that volume preferences handle edge cases correctly
     */
    @Test
    fun `test volume boundary values are handled correctly`() = runBlocking {
        val volumeValues = listOf(0.0f, 0.5f, 1.0f)
        
        for (volume in volumeValues) {
            testDataStore.edit { preferences ->
                preferences[VOLUME_KEY] = volume
            }
            
            val loadedPreferences = testDataStore.data.first()
            val loadedVolume = loadedPreferences[VOLUME_KEY]
            
            assertNotNull(loadedVolume, "Volume $volume should be saved")
            assertEquals(volume, loadedVolume, "Volume should match exactly")
        }
    }
    
    /**
     * Test that preferences are written to disk
     * 
     * Verifies that preferences are actually persisted to the file system
     * This ensures no data loss during application restarts
     */
    @Test
    fun `test preferences are written to disk`() = runBlocking {
        // Save preferences
        testDataStore.edit { preferences ->
            preferences[PLAYER_TYPE_KEY] = PlayerImplementationType.LIBMPV.name
            preferences[VOLUME_KEY] = 0.65f
        }
        
        // Read back to ensure they're persisted
        val loadedPreferences = testDataStore.data.first()
        
        assertEquals(PlayerImplementationType.LIBMPV.name, loadedPreferences[PLAYER_TYPE_KEY], 
            "Player type should persist")
        assertEquals(0.65f, loadedPreferences[VOLUME_KEY], 
            "Volume should persist")
        
        // Verify the file has content (indicating data was written)
        assertTrue(testDataStoreFile.exists(), "DataStore file should exist")
        assertTrue(testDataStoreFile.length() > 0, "DataStore file should contain data")
    }
    
    /**
     * Test that default values work when preferences don't exist
     * 
     * Verifies graceful handling of missing preferences
     */
    @Test
    fun `test default values work when preferences don't exist`() = runBlocking {
        // Don't save any preferences
        val loadedPreferences = testDataStore.data.first()
        
        // Verify we can provide defaults for missing values
        val playerType = loadedPreferences[PLAYER_TYPE_KEY] ?: PlayerImplementationType.LIBMPV.name
        val volume = loadedPreferences[VOLUME_KEY] ?: 1.0f
        
        assertEquals(PlayerImplementationType.LIBMPV.name, playerType, "Should use default player type")
        assertEquals(1.0f, volume, "Should use default volume")
    }
    
    /**
     * Test that DataStore file is created in correct location
     * 
     * Verifies that the preference file structure is correct
     */
    @Test
    fun `test DataStore file is created correctly`() = runBlocking {
        // Save a preference to trigger file creation
        testDataStore.edit { preferences ->
            preferences[PLAYER_TYPE_KEY] = PlayerImplementationType.LIBMPV.name
        }
        
        // Verify file exists
        assertTrue(testDataStoreFile.exists(), "DataStore file should be created")
        assertTrue(testDataStoreFile.isFile, "DataStore should be a file")
        assertTrue(testDataStoreFile.length() > 0, "DataStore file should not be empty")
    }
}
