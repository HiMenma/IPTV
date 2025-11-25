package com.menmapro.iptv.data.database

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import app.cash.sqldelight.db.SqlDriver
import kotlinx.coroutines.flow.first

/**
 * Database migration manager
 * Handles schema version upgrades and data migrations
 */
object DatabaseMigration {
    private const val CURRENT_VERSION = 2
    private const val VERSION_KEY = "db_version"
    
    private fun logInfo(message: String) {
        println("[DatabaseMigration] INFO: $message")
    }
    
    private fun logError(message: String, error: Throwable? = null) {
        println("[DatabaseMigration] ERROR: $message")
        error?.let { 
            println("[DatabaseMigration] ERROR: ${it.message}")
            println("[DatabaseMigration] ERROR: Stack trace: ${it.stackTraceToString()}")
        }
    }
    
    /**
     * Perform database migration if needed
     * @param driver SQL driver for executing migration scripts
     * @param dataStore DataStore for storing version information
     */
    suspend fun migrate(driver: SqlDriver, dataStore: DataStore<Preferences>) {
        try {
            val versionKey = intPreferencesKey(VERSION_KEY)
            val currentVersion = dataStore.data.first()[versionKey] ?: 1
            
            logInfo("Current database version: $currentVersion, Target version: $CURRENT_VERSION")
            
            if (currentVersion < CURRENT_VERSION) {
                logInfo("Starting migration from version $currentVersion to $CURRENT_VERSION")
                performMigration(driver, currentVersion, CURRENT_VERSION)
                
                // Update version in DataStore
                dataStore.edit { preferences ->
                    preferences[versionKey] = CURRENT_VERSION
                }
                
                logInfo("✓ Migration completed successfully. Database is now at version $CURRENT_VERSION")
            } else {
                logInfo("Database is already at the latest version ($CURRENT_VERSION). No migration needed.")
            }
        } catch (e: Exception) {
            logError("Failed to perform database migration", e)
            throw e
        }
    }
    
    /**
     * Execute migration scripts based on version range
     */
    private fun performMigration(driver: SqlDriver, fromVersion: Int, toVersion: Int) {
        logInfo("Executing migration scripts from v$fromVersion to v$toVersion")
        
        // Execute migrations in sequence
        for (version in (fromVersion + 1)..toVersion) {
            when (version) {
                2 -> migrateV1ToV2(driver)
                // Future migrations can be added here
                // 3 -> migrateV2ToV3(driver)
                else -> logInfo("No migration script for version $version")
            }
        }
    }
    
    /**
     * Migration from version 1 to version 2
     * Adds categoryId column to Channel table
     */
    private fun migrateV1ToV2(driver: SqlDriver) {
        logInfo("Executing migration v1 → v2: Adding categoryId column to Channel table")
        
        try {
            // Check if column already exists
            val hasColumn = try {
                driver.executeQuery(
                    identifier = null,
                    sql = "SELECT categoryId FROM Channel LIMIT 0",
                    mapper = { cursor -> 
                        app.cash.sqldelight.db.QueryResult.Value(cursor)
                    },
                    parameters = 0,
                    binders = null
                )
                true
            } catch (e: Exception) {
                false
            }
            
            if (hasColumn) {
                logInfo("Column categoryId already exists in Channel table. Skipping migration.")
                return
            }
            
            // Add categoryId column
            driver.execute(
                identifier = null,
                sql = "ALTER TABLE Channel ADD COLUMN categoryId TEXT",
                parameters = 0,
                binders = null
            )
            
            logInfo("✓ Successfully added categoryId column to Channel table")
            
        } catch (e: Exception) {
            // If the column already exists, SQLite will throw an error
            // We can safely ignore this error
            if (e.message?.contains("duplicate column") == true) {
                logInfo("Column categoryId already exists (duplicate column error). Migration skipped.")
            } else {
                logError("Failed to execute migration v1 → v2", e)
                throw e
            }
        }
    }
    
    /**
     * Reset database to initial state
     * WARNING: This will delete all user data
     */
    suspend fun resetDatabase(driver: SqlDriver, dataStore: DataStore<Preferences>) {
        logInfo("Resetting database to initial state...")
        
        try {
            // Drop all tables
            val tables = listOf("Channel", "Playlist", "Category", "Favorite", "EpgProgram")
            tables.forEach { table ->
                try {
                    driver.execute(
                        identifier = null,
                        sql = "DROP TABLE IF EXISTS $table",
                        parameters = 0,
                        binders = null
                    )
                    logInfo("✓ Dropped table: $table")
                } catch (e: Exception) {
                    logError("Failed to drop table: $table", e)
                }
            }
            
            // Recreate schema
            IptvDatabase.Schema.create(driver)
            logInfo("✓ Database schema recreated")
            
            // Reset version to current
            val versionKey = intPreferencesKey(VERSION_KEY)
            dataStore.edit { preferences ->
                preferences[versionKey] = CURRENT_VERSION
            }
            
            logInfo("✓ Database reset completed successfully")
            
        } catch (e: Exception) {
            logError("Failed to reset database", e)
            throw e
        }
    }
}
