package com.menmapro.iptv.platform

import android.app.Activity
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.CompletableDeferred

actual class FileManager {
    private var currentActivity: ComponentActivity? = null
    private var pickFileLauncher: ActivityResultLauncher<String>? = null
    private var createFileLauncher: ActivityResultLauncher<String>? = null
    private var pickFileDeferred: CompletableDeferred<Uri?>? = null
    private var createFileDeferred: CompletableDeferred<Uri?>? = null
    
    fun setActivity(activity: Activity) {
        if (activity is ComponentActivity) {
            currentActivity = activity
            
            // Register launchers
            pickFileLauncher = activity.registerForActivityResult(
                ActivityResultContracts.GetContent()
            ) { uri ->
                pickFileDeferred?.complete(uri)
                pickFileDeferred = null
            }
            
            createFileLauncher = activity.registerForActivityResult(
                ActivityResultContracts.CreateDocument("application/x-mpegurl")
            ) { uri ->
                createFileDeferred?.complete(uri)
                createFileDeferred = null
            }
        }
    }
    
    actual suspend fun pickM3uFile(): String? {
        val activity = currentActivity ?: run {
            println("Error: Activity not set in FileManager")
            return null
        }
        
        val launcher = pickFileLauncher ?: run {
            println("Error: Pick file launcher not initialized")
            return null
        }
        
        return try {
            pickFileDeferred = CompletableDeferred()
            launcher.launch("*/*")
            
            val uri = pickFileDeferred?.await()
            if (uri != null) {
                activity.contentResolver.openInputStream(uri)?.use { stream ->
                    stream.bufferedReader().readText()
                }
            } else {
                println("No file selected")
                null
            }
        } catch (e: Exception) {
            println("Error reading M3U file: ${e.message}")
            e.printStackTrace()
            null
        }
    }
    
    actual suspend fun saveM3uFile(fileName: String, content: String): Boolean {
        val activity = currentActivity ?: run {
            println("Error: Activity not set in FileManager")
            return false
        }
        
        val launcher = createFileLauncher ?: run {
            println("Error: Create file launcher not initialized")
            return false
        }
        
        return try {
            val safeFileName = if (fileName.endsWith(".m3u", ignoreCase = true)) {
                fileName
            } else {
                "$fileName.m3u"
            }
            
            createFileDeferred = CompletableDeferred()
            launcher.launch(safeFileName)
            
            val uri = createFileDeferred?.await()
            if (uri != null) {
                activity.contentResolver.openOutputStream(uri)?.use { stream ->
                    stream.write(content.toByteArray())
                }
                println("M3U file saved successfully")
                true
            } else {
                println("No save location selected")
                false
            }
        } catch (e: Exception) {
            println("Error saving M3U file: ${e.message}")
            e.printStackTrace()
            false
        }
    }
}
