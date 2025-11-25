package com.menmapro.iptv.data.database

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okio.Path.Companion.toPath
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DatabaseMigrationTest {
    
    private lateinit var driver: JdbcSqliteDriver
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var testDbFile: File
    private lateinit var testDataStoreFile: File
    
    @Before
    fun setup() {
        // Create temporary test database
        testDbFile = File.createTempFile("test_iptv", ".db")
        testDbFile.deleteOnExit()
        
        driver = JdbcSqliteDriver("jdbc:sqlite:${testDbFile.absolutePath}")
        
        // Create temporary DataStore
        testDataStoreFile = File.createTempFile("test_preferences", ".preferences_pb")
        testDataStoreFile.deleteOnExit()
        
        dataStore = PreferenceDataStoreFactory.createWithPath(
            produceFile = { testDataStoreFile.absolutePath.toPath() }
        )
    }
    
    @After
    fun tearDown() {
        driver.close()
        testDbFile.delete()
        testDataStoreFile.delete()
    }
    
    @Test
    fun `test migration from v1 to v2 adds categoryId column`() = runBlocking {
        // Create v1 schema (without categoryId)
        driver.execute(null, """
            CREATE TABLE IF NOT EXISTS Channel (
                id TEXT PRIMARY KEY NOT NULL,
                playlistId TEXT NOT NULL,
                name TEXT NOT NULL,
                url TEXT NOT NULL,
                logoUrl TEXT,
                groupName TEXT,
                tvgId TEXT,
                tvgName TEXT,
                epgChannelId TEXT
            )
        """.trimIndent(), 0)
        
        // Set database version to 1
        dataStore.edit { preferences ->
            preferences[intPreferencesKey("db_version")] = 1
        }
        
        // Verify version is 1
        val versionBefore = dataStore.data.first()[intPreferencesKey("db_version")]
        assertEquals(1, versionBefore, "Database version should be 1 before migration")
        
        // Run migration
        DatabaseMigration.migrate(driver, dataStore)
        
        // Verify version is now 2
        val versionAfter = dataStore.data.first()[intPreferencesKey("db_version")]
        assertEquals(2, versionAfter, "Database version should be 2 after migration")
        
        // Verify categoryId column exists by trying to query it
        val result = driver.executeQuery(
            identifier = null,
            sql = "SELECT categoryId FROM Channel LIMIT 0",
            mapper = { cursor -> 
                app.cash.sqldelight.db.QueryResult.Value(true)
            },
            parameters = 0,
            binders = null
        )
        
        assertTrue(result.value, "categoryId column should exist after migration")
    }
    
    @Test
    fun `test migration is idempotent`() = runBlocking {
        // Create v1 schema
        driver.execute(null, """
            CREATE TABLE IF NOT EXISTS Channel (
                id TEXT PRIMARY KEY NOT NULL,
                playlistId TEXT NOT NULL,
                name TEXT NOT NULL,
                url TEXT NOT NULL,
                logoUrl TEXT,
                groupName TEXT,
                tvgId TEXT,
                tvgName TEXT,
                epgChannelId TEXT
            )
        """.trimIndent(), 0)
        
        // Set version to 1
        dataStore.edit { preferences ->
            preferences[intPreferencesKey("db_version")] = 1
        }
        
        // Run migration twice
        DatabaseMigration.migrate(driver, dataStore)
        DatabaseMigration.migrate(driver, dataStore)
        
        // Verify version is still 2
        val version = dataStore.data.first()[intPreferencesKey("db_version")]
        assertEquals(2, version, "Database version should be 2 after multiple migrations")
        
        // Verify column still exists
        val result = driver.executeQuery(
            identifier = null,
            sql = "SELECT categoryId FROM Channel LIMIT 0",
            mapper = { cursor -> 
                app.cash.sqldelight.db.QueryResult.Value(true)
            },
            parameters = 0,
            binders = null
        )
        
        assertTrue(result.value, "categoryId column should still exist")
    }
    
    @Test
    fun `test migration skips if already at latest version`() = runBlocking {
        // Create v2 schema (with categoryId)
        driver.execute(null, """
            CREATE TABLE IF NOT EXISTS Channel (
                id TEXT PRIMARY KEY NOT NULL,
                playlistId TEXT NOT NULL,
                name TEXT NOT NULL,
                url TEXT NOT NULL,
                logoUrl TEXT,
                groupName TEXT,
                tvgId TEXT,
                tvgName TEXT,
                epgChannelId TEXT,
                categoryId TEXT
            )
        """.trimIndent(), 0)
        
        // Set version to 2
        dataStore.edit { preferences ->
            preferences[intPreferencesKey("db_version")] = 2
        }
        
        // Run migration
        DatabaseMigration.migrate(driver, dataStore)
        
        // Verify version is still 2
        val version = dataStore.data.first()[intPreferencesKey("db_version")]
        assertEquals(2, version, "Database version should remain 2")
    }
    
    @Test
    fun `test migration handles column already exists`() = runBlocking {
        // Create v2 schema (with categoryId already present)
        driver.execute(null, """
            CREATE TABLE IF NOT EXISTS Channel (
                id TEXT PRIMARY KEY NOT NULL,
                playlistId TEXT NOT NULL,
                name TEXT NOT NULL,
                url TEXT NOT NULL,
                logoUrl TEXT,
                groupName TEXT,
                tvgId TEXT,
                tvgName TEXT,
                epgChannelId TEXT,
                categoryId TEXT
            )
        """.trimIndent(), 0)
        
        // Set version to 1 (simulate inconsistent state)
        dataStore.edit { preferences ->
            preferences[intPreferencesKey("db_version")] = 1
        }
        
        // Run migration - should not fail even though column exists
        DatabaseMigration.migrate(driver, dataStore)
        
        // Verify version is updated to 2
        val version = dataStore.data.first()[intPreferencesKey("db_version")]
        assertEquals(2, version, "Database version should be updated to 2")
    }
}
