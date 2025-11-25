# Database Migration Guide

## Overview

This document describes the database schema versioning and migration strategy for the IPTV application.

## Schema Versions

### Version 1 (Initial Schema)
- **Playlist** table: Stores playlist information
- **Channel** table: Stores channel information (without categoryId)
- **Favorite** table: Stores user favorites
- **EpgProgram** table: Stores EPG (Electronic Program Guide) data

### Version 2 (Current Schema)
- Added **Category** table: Stores channel categories for Xtream playlists
- Modified **Channel** table: Added `categoryId` field to associate channels with categories

## Migration Implementation

The migration is handled by the `DatabaseSchema` object in `DatabaseSchema.kt`.

### Key Features

1. **Automatic Migration**: When the app starts, the database driver automatically detects the current schema version and applies necessary migrations.

2. **Idempotent Migrations**: Migrations can be run multiple times safely. The migration logic checks if tables/columns already exist before attempting to create them.

3. **Data Preservation**: All existing data is preserved during migration. No data loss occurs.

4. **Platform Support**: The migration works on both Android and Desktop platforms.

## Migration Process

### From Version 1 to Version 2

The migration performs the following steps:

1. **Create Category Table**:
   ```sql
   CREATE TABLE IF NOT EXISTS Category (
       id TEXT PRIMARY KEY NOT NULL,
       playlistId TEXT NOT NULL,
       name TEXT NOT NULL,
       parentId TEXT,
       FOREIGN KEY (playlistId) REFERENCES Playlist(id) ON DELETE CASCADE
   )
   ```

2. **Add categoryId Column to Channel Table**:
   ```sql
   ALTER TABLE Channel ADD COLUMN categoryId TEXT
   ```

### Safety Checks

Before each migration step, the system checks:
- If the Category table already exists (using `sqlite_master` table)
- If the categoryId column already exists in Channel table (using `PRAGMA table_info`)

This ensures migrations are safe to run multiple times.

## Testing

The migration logic is thoroughly tested in `DatabaseMigrationTest.kt`:

- ✅ Migration from v1 to v2 creates Category table
- ✅ Migration is idempotent (can run multiple times)
- ✅ Fresh database creation uses latest schema
- ✅ Category table has correct structure
- ✅ Migration preserves existing data

## Platform-Specific Implementation

### Android
Uses `AndroidSqliteDriver` with the custom `DatabaseSchema` object:
```kotlin
AndroidSqliteDriver(DatabaseSchema, context, "iptv.db")
```

### Desktop
Uses `JdbcSqliteDriver` with the custom `DatabaseSchema` object:
```kotlin
val driver = JdbcSqliteDriver("jdbc:sqlite:${databasePath.absolutePath}")
DatabaseSchema.create(driver)
```

## Future Migrations

To add a new migration:

1. Increment the version number in `DatabaseSchema.version`
2. Add a new migration function (e.g., `migrateV2ToV3`)
3. Call the migration function in the `migrate()` method
4. Add tests for the new migration in `DatabaseMigrationTest.kt`

Example:
```kotlin
object DatabaseSchema : SqlSchema<QueryResult.Value<Unit>> {
    override val version: Long = 3  // Increment version
    
    override fun migrate(
        driver: SqlDriver,
        oldVersion: Long,
        newVersion: Long,
        vararg callbacks: AfterVersion
    ): QueryResult.Value<Unit> {
        if (oldVersion < 2 && newVersion >= 2) {
            migrateV1ToV2(driver)
        }
        if (oldVersion < 3 && newVersion >= 3) {
            migrateV2ToV3(driver)  // Add new migration
        }
        return QueryResult.Value(Unit)
    }
    
    private fun migrateV2ToV3(driver: SqlDriver) {
        // Add migration logic here
    }
}
```

## Troubleshooting

### Migration Fails

If a migration fails:
1. Check the logs for detailed error messages
2. Verify the database file is not corrupted
3. Ensure the app has write permissions to the database directory
4. On Desktop: Check `~/.iptv/iptv.db`
5. On Android: Check app's private storage

### Testing Migrations

To test migrations locally:
1. Run the app with an older schema version
2. Add some test data
3. Update to the new version
4. Verify data is preserved and new features work

### Manual Migration

If automatic migration fails, you can manually migrate by:
1. Backup the database file
2. Delete the database file
3. Restart the app (creates fresh database with latest schema)
4. Re-import playlists

## References

- SQLDelight Documentation: https://cashapp.github.io/sqldelight/
- SQLite ALTER TABLE: https://www.sqlite.org/lang_altertable.html
- Database Schema Design: See `design.md` in the spec folder
