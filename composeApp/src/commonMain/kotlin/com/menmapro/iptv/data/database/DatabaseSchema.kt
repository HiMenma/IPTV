package com.menmapro.iptv.data.database

import app.cash.sqldelight.db.AfterVersion
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema

/**
 * Custom database schema wrapper that handles versioning and migrations.
 * 
 * Version History:
 * - Version 1: Initial schema with Playlist, Channel, Favorite, and EpgProgram tables
 * - Version 2: Added Category table and categoryId field to Channel table
 */
object DatabaseSchema : SqlSchema<QueryResult.Value<Unit>> {
    override val version: Long = 2
    
    override fun create(driver: SqlDriver): QueryResult.Value<Unit> {
        // Create all tables with the latest schema
        IptvDatabase.Schema.create(driver)
        return QueryResult.Value(Unit)
    }
    
    override fun migrate(
        driver: SqlDriver,
        oldVersion: Long,
        newVersion: Long,
        vararg callbacks: AfterVersion
    ): QueryResult.Value<Unit> {
        println("[DatabaseSchema] Migrating database from version $oldVersion to $newVersion")
        
        // Migration from version 1 to version 2
        if (oldVersion < 2 && newVersion >= 2) {
            migrateV1ToV2(driver)
        }
        
        // Execute callbacks for each version
        callbacks.filter { it.afterVersion in (oldVersion + 1)..newVersion }
            .sortedBy { it.afterVersion }
            .forEach { callback ->
                println("[DatabaseSchema] Executing callback for version ${callback.afterVersion}")
                callback.block(driver)
            }
        
        println("[DatabaseSchema] Migration completed successfully")
        return QueryResult.Value(Unit)
    }
    
    /**
     * Migrate from version 1 to version 2.
     * Adds:
     * - Category table for organizing channels
     * - categoryId field to Channel table
     */
    private fun migrateV1ToV2(driver: SqlDriver) {
        println("[DatabaseSchema] Applying migration from v1 to v2")
        
        try {
            // Check if Category table already exists
            val categoryTableExists = checkTableExists(driver, "Category")
            
            if (!categoryTableExists) {
                println("[DatabaseSchema] Creating Category table")
                driver.execute(
                    identifier = null,
                    sql = """
                        CREATE TABLE IF NOT EXISTS Category (
                            id TEXT PRIMARY KEY NOT NULL,
                            playlistId TEXT NOT NULL,
                            name TEXT NOT NULL,
                            parentId TEXT,
                            FOREIGN KEY (playlistId) REFERENCES Playlist(id) ON DELETE CASCADE
                        )
                    """.trimIndent(),
                    parameters = 0,
                    binders = null
                )
                println("[DatabaseSchema] Category table created successfully")
            } else {
                println("[DatabaseSchema] Category table already exists, skipping creation")
            }
            
            // Check if categoryId column exists in Channel table
            val categoryIdExists = checkColumnExists(driver, "Channel", "categoryId")
            
            if (!categoryIdExists) {
                println("[DatabaseSchema] Adding categoryId column to Channel table")
                driver.execute(
                    identifier = null,
                    sql = "ALTER TABLE Channel ADD COLUMN categoryId TEXT",
                    parameters = 0,
                    binders = null
                )
                println("[DatabaseSchema] categoryId column added successfully")
            } else {
                println("[DatabaseSchema] categoryId column already exists, skipping addition")
            }
            
            println("[DatabaseSchema] Migration v1 to v2 completed successfully")
            
        } catch (e: Exception) {
            println("[DatabaseSchema] ERROR during migration v1 to v2: ${e.message}")
            e.printStackTrace()
            throw IllegalStateException("Failed to migrate database from v1 to v2: ${e.message}", e)
        }
    }
    
    /**
     * Check if a table exists in the database.
     */
    private fun checkTableExists(driver: SqlDriver, tableName: String): Boolean {
        return try {
            driver.executeQuery(
                identifier = null,
                sql = "SELECT name FROM sqlite_master WHERE type='table' AND name=?",
                mapper = { cursor ->
                    QueryResult.Value(cursor.next().value)
                },
                parameters = 1,
                binders = {
                    bindString(0, tableName)
                }
            ).value
        } catch (e: Exception) {
            println("[DatabaseSchema] Error checking if table $tableName exists: ${e.message}")
            false
        }
    }
    
    /**
     * Check if a column exists in a table.
     */
    private fun checkColumnExists(driver: SqlDriver, tableName: String, columnName: String): Boolean {
        return try {
            driver.executeQuery(
                identifier = null,
                sql = "PRAGMA table_info($tableName)",
                mapper = { cursor ->
                    var exists = false
                    while (cursor.next().value) {
                        val name = cursor.getString(1) // Column name is at index 1
                        if (name == columnName) {
                            exists = true
                            break
                        }
                    }
                    QueryResult.Value(exists)
                },
                parameters = 0,
                binders = null
            ).value
        } catch (e: Exception) {
            println("[DatabaseSchema] Error checking if column $columnName exists in table $tableName: ${e.message}")
            false
        }
    }
}
