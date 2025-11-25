package com.menmapro.iptv.di

import com.menmapro.iptv.data.database.IptvDatabase
import com.menmapro.iptv.data.database.createDatabaseDriver
import com.menmapro.iptv.data.database.dao.EpgDao
import com.menmapro.iptv.data.database.dao.FavoriteDao
import com.menmapro.iptv.data.database.dao.PlaylistDao
import com.menmapro.iptv.data.network.XtreamClient
import com.menmapro.iptv.data.parser.M3uParser
import com.menmapro.iptv.data.repository.FavoriteRepository
import com.menmapro.iptv.data.repository.PlaylistRepository
import com.menmapro.iptv.ui.screens.FavoriteScreenModel
import com.menmapro.iptv.ui.screens.PlaylistScreenModel
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.core.KoinApplication
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.dsl.module

val appModule = module {
    // HTTP Client
    single {
        HttpClient {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    prettyPrint = true
                    isLenient = true
                })
            }
            install(HttpTimeout) {
                requestTimeoutMillis = 30000 // 30 seconds
                connectTimeoutMillis = 15000 // 15 seconds
                socketTimeoutMillis = 30000 // 30 seconds
            }
        }
    }

    // Database
    single { 
        val driver = createDatabaseDriver()
        IptvDatabase(driver)
    }

    // DAOs
    single { PlaylistDao(get()) }
    single { FavoriteDao(get()) }
    single { EpgDao(get()) }

    // Parsers and Clients
    single { M3uParser() }
    single { XtreamClient(get()) }

    // Repositories
    single { PlaylistRepository(httpClient = get(), m3uParser = get(), xtreamClient = get(), playlistDao = get()) }
    single { FavoriteRepository(get()) }

    // ViewModels/ScreenModels
    factory { PlaylistScreenModel(get()) }
    factory { FavoriteScreenModel(get()) }
}

fun initKoin(appDeclaration: KoinApplication.() -> Unit = {}) {
    // Check if Koin is already initialized to prevent duplicate initialization
    if (GlobalContext.getOrNull() != null) {
        println("Koin is already initialized, skipping re-initialization")
        return
    }
    
    startKoin {
        appDeclaration()
        modules(appModule)
    }
}

/**
 * Check if Koin has been initialized
 */
fun isKoinInitialized(): Boolean {
    return GlobalContext.getOrNull() != null
}
