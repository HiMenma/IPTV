package com.menmapro.iptv.di

import com.menmapro.iptv.data.database.dao.CategoryDao
import com.menmapro.iptv.data.database.dao.PlaylistDao
import com.menmapro.iptv.data.repository.PlaylistRepository
import com.menmapro.iptv.ui.screens.PlaylistScreenModel
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.koin.core.context.GlobalContext
import org.koin.core.context.stopKoin
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Test to verify Koin dependency injection configuration
 * Validates: Requirements 3.1, 3.2
 */
class KoinConfigurationTest {
    
    @Before
    fun setup() {
        // Clean up any existing Koin instance
        if (GlobalContext.getOrNull() != null) {
            stopKoin()
        }
    }
    
    @After
    fun tearDown() {
        stopKoin()
    }
    
    @Test
    fun `verify CategoryDao is registered in Koin`() {
        // Initialize Koin
        initKoin()
        
        // Verify CategoryDao can be resolved
        val categoryDao = GlobalContext.get().get<CategoryDao>()
        assertNotNull(categoryDao, "CategoryDao should be registered in Koin")
    }
    
    @Test
    fun `verify PlaylistRepository can access PlaylistDao`() {
        // Initialize Koin
        initKoin()
        
        // Verify PlaylistRepository can be resolved
        val playlistRepository = GlobalContext.get().get<PlaylistRepository>()
        assertNotNull(playlistRepository, "PlaylistRepository should be registered in Koin")
        
        // Verify PlaylistDao is also available (used by PlaylistRepository)
        val playlistDao = GlobalContext.get().get<PlaylistDao>()
        assertNotNull(playlistDao, "PlaylistDao should be registered in Koin")
    }
    
    @Test
    fun `verify all new components have proper dependency injection`() {
        // Initialize Koin
        initKoin()
        
        // Verify CategoryDao
        val categoryDao = GlobalContext.get().get<CategoryDao>()
        assertNotNull(categoryDao, "CategoryDao should be available")
        
        // Verify PlaylistRepository
        val playlistRepository = GlobalContext.get().get<PlaylistRepository>()
        assertNotNull(playlistRepository, "PlaylistRepository should be available")
        
        // Verify PlaylistScreenModel (factory)
        val playlistScreenModel = GlobalContext.get().get<PlaylistScreenModel>()
        assertNotNull(playlistScreenModel, "PlaylistScreenModel should be available")
    }
    
    @Test
    fun `verify Koin initialization is idempotent`() {
        // First initialization
        initKoin()
        assertTrue(isKoinInitialized(), "Koin should be initialized")
        
        // Second initialization should not fail
        initKoin()
        assertTrue(isKoinInitialized(), "Koin should still be initialized")
    }
}
