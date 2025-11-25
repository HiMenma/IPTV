package com.menmapro.iptv.ui.components

import uk.co.caprica.vlcj.factory.discovery.NativeDiscovery
import java.io.File

/**
 * Utility class to check VLC availability and provide installation instructions
 */
object VlcAvailabilityChecker {
    
    private var cachedAvailability: Boolean? = null
    
    /**
     * Check if VLC is available on the system
     */
    fun isVlcAvailable(): Boolean {
        if (cachedAvailability != null) {
            return cachedAvailability!!
        }
        
        return try {
            val discovery = NativeDiscovery()
            val found = discovery.discover()
            cachedAvailability = found
            
            if (found) {
                println("VLC libraries found successfully")
            } else {
                println("VLC libraries not found")
            }
            
            found
        } catch (e: Exception) {
            println("Error checking VLC availability: ${e.message}")
            e.printStackTrace()
            cachedAvailability = false
            false
        }
    }
    
    /**
     * Get installation instructions based on the operating system
     */
    fun getInstallationInstructions(): String {
        val os = System.getProperty("os.name").lowercase()
        
        return when {
            os.contains("mac") || os.contains("darwin") -> getMacOsInstructions()
            os.contains("win") -> getWindowsInstructions()
            os.contains("nux") || os.contains("nix") -> getLinuxInstructions()
            else -> getGenericInstructions()
        }
    }
    
    /**
     * Get a user-friendly error message when VLC is not available
     */
    fun getErrorMessage(): String {
        return """
            VLC Media Player 未安装或无法找到
            
            此应用需要 VLC Media Player 才能播放视频。
            请按照以下说明安装 VLC：
            
            ${getInstallationInstructions()}
            
            安装完成后，请重启应用程序。
        """.trimIndent()
    }
    
    private fun getMacOsInstructions(): String {
        return """
            macOS 安装说明：
            
            方法 1 - 使用 Homebrew（推荐）：
            1. 打开终端
            2. 运行命令：brew install --cask vlc
            
            方法 2 - 手动下载：
            1. 访问 https://www.videolan.org/vlc/
            2. 下载 macOS 版本
            3. 打开 .dmg 文件并将 VLC 拖到应用程序文件夹
            
            注意：安装后可能需要在"系统偏好设置 > 安全性与隐私"中允许 VLC 运行
        """.trimIndent()
    }
    
    private fun getWindowsInstructions(): String {
        return """
            Windows 安装说明：
            
            方法 1 - 使用安装程序（推荐）：
            1. 访问 https://www.videolan.org/vlc/
            2. 下载 Windows 版本（32位或64位，根据您的系统）
            3. 运行安装程序并按照提示完成安装
            4. 确保安装到默认位置（C:\Program Files\VideoLAN\VLC）
            
            方法 2 - 使用 Chocolatey：
            1. 打开命令提示符（管理员权限）
            2. 运行命令：choco install vlc
            
            注意：安装后请重启应用程序
        """.trimIndent()
    }
    
    private fun getLinuxInstructions(): String {
        return """
            Linux 安装说明：
            
            Ubuntu/Debian：
            sudo apt-get update
            sudo apt-get install vlc
            
            Fedora：
            sudo dnf install vlc
            
            Arch Linux：
            sudo pacman -S vlc
            
            openSUSE：
            sudo zypper install vlc
            
            注意：安装后可能需要重启应用程序
        """.trimIndent()
    }
    
    private fun getGenericInstructions(): String {
        return """
            请访问 VLC 官方网站下载并安装：
            https://www.videolan.org/vlc/
            
            选择适合您操作系统的版本并按照安装向导完成安装。
            安装完成后，请重启应用程序。
        """.trimIndent()
    }
    
    /**
     * Get detailed system information for troubleshooting
     */
    fun getSystemInfo(): String {
        val os = System.getProperty("os.name")
        val osVersion = System.getProperty("os.version")
        val osArch = System.getProperty("os.arch")
        val javaVersion = System.getProperty("java.version")
        
        return """
            系统信息：
            操作系统: $os
            系统版本: $osVersion
            系统架构: $osArch
            Java 版本: $javaVersion
        """.trimIndent()
    }
}
