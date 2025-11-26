package com.menmapro.iptv.platform

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

actual class FileManager {
    private var currentActivity: Activity? = null
    
    fun setActivity(activity: Activity) {
        currentActivity = activity
    }
    
    actual suspend fun pickM3uFile(): String? = suspendCancellableCoroutine { continuation ->
        val activity = currentActivity as? ComponentActivity
        if (activity == null) {
            continuation.resume(null)
            return@suspendCancellableCoroutine
        }
        
        val launcher = activity.registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            if (uri != null) {
                try {
                    val content = activity.contentResolver.openInputStream(uri)?.use { stream ->
                        stream.bufferedReader().readText()
                    }
                    continuation.resume(content)
                } catch (e: Exception) {
                    println("Error reading M3U file: ${e.message}")
                    continuation.resume(null)
                }
            } else {
                continuation.resume(null)
            }
        }
        
        launcher.launch("*/*")
    }
    
    actual suspend fun saveM3uFile(fileName: String, content: String): Boolean = 
        suspendCancellableCoroutine { continuation ->
            val activity = currentActivity as? ComponentActivity
            if (activity == null) {
                continuation.resume(false)
                return@suspendCancellableCoroutine
            }
            
            val launcher = activity.registerForActivityResult(
                ActivityResultContracts.CreateDocument("application/x-mpegurl")
            ) { uri: Uri? ->
                if (uri != null) {
                    try {
                        activity.contentResolver.openOutputStream(uri)?.use { stream ->
                            stream.write(content.toByteArray())
                        }
                        continuation.resume(true)
                    } catch (e: Exception) {
                        println("Error saving M3U file: ${e.message}")
                        continuation.resume(false)
                    }
                } else {
                    continuation.resume(false)
                }
            }
            
            val safeFileName = if (fileName.endsWith(".m3u", ignoreCase = true)) {
                fileName
            } else {
                "$fileName.m3u"
            }
            
            launcher.launch(safeFileName)
        }
}
