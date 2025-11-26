package com.menmapro.iptv.platform

import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

actual class FileManager {
    
    actual suspend fun pickM3uFile(): String? {
        return try {
            val fileChooser = JFileChooser()
            fileChooser.dialogTitle = "选择M3U文件"
            fileChooser.fileFilter = FileNameExtensionFilter("M3U Files (*.m3u, *.m3u8)", "m3u", "m3u8")
            
            val result = fileChooser.showOpenDialog(null)
            if (result == JFileChooser.APPROVE_OPTION) {
                val file = fileChooser.selectedFile
                file.readText()
            } else {
                null
            }
        } catch (e: Exception) {
            println("Error picking M3U file: ${e.message}")
            null
        }
    }
    
    actual suspend fun saveM3uFile(fileName: String, content: String): Boolean {
        return try {
            val fileChooser = JFileChooser()
            fileChooser.dialogTitle = "保存M3U文件"
            
            val safeFileName = if (fileName.endsWith(".m3u", ignoreCase = true)) {
                fileName
            } else {
                "$fileName.m3u"
            }
            
            fileChooser.selectedFile = File(safeFileName)
            fileChooser.fileFilter = FileNameExtensionFilter("M3U Files (*.m3u)", "m3u")
            
            val result = fileChooser.showSaveDialog(null)
            if (result == JFileChooser.APPROVE_OPTION) {
                val file = fileChooser.selectedFile
                file.writeText(content)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            println("Error saving M3U file: ${e.message}")
            false
        }
    }
}
