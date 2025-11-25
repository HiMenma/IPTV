package com.menmapro.iptv.data.database

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences

/**
 * Platform-specific DataStore factory
 */
expect fun createDataStore(): DataStore<Preferences>
