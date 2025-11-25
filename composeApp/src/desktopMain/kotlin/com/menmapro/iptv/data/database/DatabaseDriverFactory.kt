package com.menmapro.iptv.data.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.io.File

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        return try {
            // Get user home directory
            val userHome = System.getProperty("user.home")
                ?: throw IllegalStateException("Unable to determine user home directory")
            
            // Create database directory path
            val databaseDir = File(userHome, ".iptv")
            val databasePath = File(databaseDir, "iptv.db")
            
            // Ensure database directory exists
            if (!databaseDir.exists()) {
                val dirCreated = databaseDir.mkdirs()
                if (!dirCreated) {
                    throw IllegalStateException("Failed to create database directory: ${databaseDir.absolutePath}")
                }
                println("[DatabaseFactory] Created database directory: ${databaseDir.absolutePath}")
            }
            
            // Verify directory is writable
            if (!databaseDir.canWrite()) {
                throw IllegalStateException("Database directory is not writable: ${databaseDir.absolutePath}")
            }
            
            println("[DatabaseFactory] Initializing database at: ${databasePath.absolutePath}")
            
            // Create JDBC SQLite driver
            val driver = try {
                JdbcSqliteDriver("jdbc:sqlite:${databasePath.absolutePath}")
            } catch (e: Exception) {
                throw IllegalStateException("Failed to create database driver: ${e.message}", e)
            }
            
            // Create database schema
            try {
                IptvDatabase.Schema.create(driver)
                println("[DatabaseFactory] Database schema created successfully")
            } catch (e: Exception) {
                // Clean up driver on schema creation failure
                try {
                    driver.close()
                } catch (closeException: Exception) {
                    println("[DatabaseFactory] Error closing driver after schema failure: ${closeException.message}")
                }
                throw IllegalStateException("Failed to create database schema: ${e.message}", e)
            }
            
            // Verify schema was created by checking if tables exist
            try {
                driver.executeQuery(
                    identifier = null,
                    sql = "SELECT name FROM sqlite_master WHERE type='table' AND name='Playlist'",
                    mapper = { cursor ->
                        val hasTable = cursor.next().value
                        if (hasTable) {
                            println("[DatabaseFactory] Database schema verification successful")
                        } else {
                            println("[DatabaseFactory] Warning: Playlist table not found in schema")
                        }
                        app.cash.sqldelight.db.QueryResult.Value(hasTable)
                    },
                    parameters = 0,
                    binders = null
                ).value
            } catch (e: Exception) {
                println("[DatabaseFactory] Warning: Could not verify database schema: ${e.message}")
            }
            
            println("[DatabaseFactory] Database driver initialized successfully")
            driver
            
        } catch (e: Exception) {
            println("[DatabaseFactory] ERROR: Failed to initialize database driver: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }
}
