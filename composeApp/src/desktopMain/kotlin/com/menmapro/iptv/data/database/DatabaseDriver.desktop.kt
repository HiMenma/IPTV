package com.menmapro.iptv.data.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.io.File

actual fun createDatabaseDriver(): SqlDriver {
    val databasePath = File(System.getProperty("user.home"), ".iptv/iptv.db")
    databasePath.parentFile?.mkdirs()
    
    val driver = JdbcSqliteDriver("jdbc:sqlite:${databasePath.absolutePath}")
    IptvDatabase.Schema.create(driver)
    return driver
}
