package com.menmapro.iptv.data.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.mp.KoinPlatformTools

actual fun createDatabaseDriver(): SqlDriver {
    // Get Android context from Koin
    val context = KoinPlatformTools.defaultContext().get().get<android.content.Context>()
    return AndroidSqliteDriver(DatabaseSchema, context, "iptv.db")
}
