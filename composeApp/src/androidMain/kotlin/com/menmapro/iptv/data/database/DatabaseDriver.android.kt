package com.menmapro.iptv.data.database

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver

fun createDatabaseDriver(context: Context): SqlDriver {
    return AndroidSqliteDriver(IptvDatabase.Schema, context, "iptv.db")
}

// For Koin
actual fun createDatabaseDriver(): SqlDriver {
    throw IllegalStateException("Android requires Context for database creation. Use createDatabaseDriver(context) instead.")
}
