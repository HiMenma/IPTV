package com.menmapro.iptv.data.database

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import okio.Path.Companion.toPath
import org.koin.mp.KoinPlatformTools

actual fun createDataStore(): DataStore<Preferences> {
    val context = KoinPlatformTools.defaultContext().get().get<android.content.Context>()
    val dataStoreFile = context.filesDir.resolve("iptv_preferences.preferences_pb")
    
    return PreferenceDataStoreFactory.createWithPath(
        produceFile = { dataStoreFile.absolutePath.toPath() }
    )
}
