package com.menmapro.iptv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.menmapro.iptv.di.initKoin
import org.koin.android.ext.koin.androidContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initKoin {
            androidContext(this@MainActivity)
        }
        
        // Initialize FileManager with activity
        try {
            val fileManager = org.koin.core.context.GlobalContext.get().get<com.menmapro.iptv.platform.FileManager>()
            fileManager.setActivity(this)
        } catch (e: Exception) {
            println("Error initializing FileManager: ${e.message}")
        }
        
        setContent {
            App()
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
