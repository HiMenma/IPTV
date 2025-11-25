package com.menmapro.iptv.data.database

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import okio.Path.Companion.toPath
import java.io.File

actual fun createDataStore(): DataStore<Preferences> {
    val userHome = System.getProperty("user.home")
        ?: throw IllegalStateException("Unable to determine user home directory")
    
    val dataStoreDir = File(userHome, ".iptv")
    if (!dataStoreDir.exists()) {
        dataStoreDir.mkdirs()
    }
    
    val dataStoreFile = File(dataStoreDir, "iptv_preferences.preferences_pb")
    
    return PreferenceDataStoreFactory.createWithPath(
        produceFile = { dataStoreFile.absolutePath.toPath() }
    )
}
