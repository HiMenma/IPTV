package com.menmapro.iptv.platform

/**
 * Platform-specific file manager for reading and writing files
 */
expect class FileManager() {
    /**
     * Pick an M3U file from the device storage
     * @return The content of the selected file, or null if cancelled
     */
    suspend fun pickM3uFile(): String?
    
    /**
     * Save M3U content to a file
     * @param fileName The name of the file to save
     * @param content The M3U content to save
     * @return true if successful, false otherwise
     */
    suspend fun saveM3uFile(fileName: String, content: String): Boolean
}
