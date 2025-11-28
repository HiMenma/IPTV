package com.menmapro.iptv.player.libmpv

import com.sun.jna.Native
import com.sun.jna.Platform
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files

/**
 * Platform-specific loader for libmpv library
 * 
 * This object handles loading the libmpv shared library on different platforms
 * (macOS, Linux, Windows) with appropriate library names and search paths.
 * 
 * The loader first attempts to extract and load the bundled library from resources,
 * then falls back to system-installed libraries if bundled version is not available.
 * 
 * Requirements:
 * - 1.4: Provide JNA bindings to libmpv C API
 * - 1.2: Provide clear error messages when libmpv is not available
 */
object LibmpvLoader {
    
    private var cachedBindings: LibmpvBindings? = null
    private var loadError: String? = null
    private var extractedLibPath: String? = null
    
    /**
     * Load the libmpv library
     * 
     * Attempts to load the libmpv shared library using the following strategy:
     * 1. Try to extract and load bundled library from resources
     * 2. Try to load from custom search paths
     * 3. Try to load using system library search
     * 
     * @return LibmpvBindings instance, or null if loading failed
     */
    fun load(): LibmpvBindings? {
        // Return cached instance if already loaded
        cachedBindings?.let { return it }
        
        // Return null if previous load attempt failed
        loadError?.let { return null }
        
        try {
            // Strategy 1: Try to load bundled library from resources
            println("Attempting to load bundled libmpv from resources...")
            val bundledLib = extractBundledLibrary()
            if (bundledLib != null) {
                try {
                    val bindings = Native.load(bundledLib, LibmpvBindings::class.java) as LibmpvBindings
                    cachedBindings = bindings
                    println("✓ Successfully loaded bundled libmpv from: $bundledLib")
                    return bindings
                } catch (e: Exception) {
                    println("✗ Failed to load bundled library: ${e.message}")
                    // Continue to next strategy
                }
            } else {
                println("✗ No bundled library found for current platform")
            }
            
            // Strategy 2: Try to load from custom search paths
            println("Attempting to load libmpv from system paths...")
            val libraryName = getLibraryName()
            val searchPaths = getSearchPaths()
            
            for (path in searchPaths) {
                try {
                    val fullPath = File(path, getLibraryFileName()).absolutePath
                    if (File(fullPath).exists()) {
                        val bindings = Native.load(fullPath, LibmpvBindings::class.java) as LibmpvBindings
                        cachedBindings = bindings
                        println("✓ Successfully loaded libmpv from: $fullPath")
                        return bindings
                    }
                } catch (e: Exception) {
                    // Continue to next path
                }
            }
            
            // Strategy 3: Try to load using system library search
            try {
                val bindings = Native.load(libraryName, LibmpvBindings::class.java) as LibmpvBindings
                cachedBindings = bindings
                println("✓ Successfully loaded libmpv using system search")
                return bindings
            } catch (e: Exception) {
                loadError = buildErrorMessage(e)
                return null
            }
        } catch (e: Exception) {
            loadError = "Unexpected error loading libmpv: ${e.message}"
            return null
        }
    }
    
    /**
     * Extract bundled library from resources to a temporary location
     * 
     * @return Path to extracted library, or null if extraction failed
     */
    private fun extractBundledLibrary(): String? {
        // Return cached path if already extracted
        extractedLibPath?.let { 
            if (File(it).exists()) {
                return it
            }
        }
        
        try {
            val platformDir = getPlatformResourceDir()
            val libraryFileName = getLibraryFileName()
            val resourcePath = "/native/$platformDir/$libraryFileName"
            
            println("Looking for bundled library at: $resourcePath")
            
            // Try to load resource
            val resourceStream = LibmpvLoader::class.java.getResourceAsStream(resourcePath)
            if (resourceStream == null) {
                println("Bundled library not found in resources")
                return null
            }
            
            // Create temporary directory for extracted library
            val tempDir = Files.createTempDirectory("libmpv-native").toFile()
            tempDir.deleteOnExit()
            
            val extractedFile = File(tempDir, libraryFileName)
            extractedFile.deleteOnExit()
            
            // Extract library to temporary file
            println("Extracting bundled library to: ${extractedFile.absolutePath}")
            resourceStream.use { input ->
                FileOutputStream(extractedFile).use { output ->
                    input.copyTo(output)
                }
            }
            
            // Make executable on Unix-like systems
            if (!Platform.isWindows()) {
                extractedFile.setExecutable(true, false)
                extractedFile.setReadable(true, false)
            }
            
            extractedLibPath = extractedFile.absolutePath
            println("✓ Library extracted successfully")
            return extractedLibPath
            
        } catch (e: Exception) {
            println("Failed to extract bundled library: ${e.message}")
            e.printStackTrace()
            return null
        }
    }
    
    /**
     * Get the platform-specific resource directory name
     * 
     * @return Resource directory name for the current platform
     */
    private fun getPlatformResourceDir(): String {
        return when {
            Platform.isMac() -> {
                val arch = System.getProperty("os.arch")
                if (arch == "aarch64" || arch == "arm64") {
                    "macos-aarch64"
                } else {
                    "macos-x86_64"
                }
            }
            Platform.isLinux() -> "linux-x86_64"
            Platform.isWindows() -> "windows-x86_64"
            else -> "unknown"
        }
    }
    
    /**
     * Check if libmpv is available
     * 
     * @return true if libmpv can be loaded, false otherwise
     */
    fun isAvailable(): Boolean {
        return load() != null
    }
    
    /**
     * Get the reason why libmpv is not available
     * 
     * @return Error message, or null if libmpv is available
     */
    fun getUnavailableReason(): String? {
        if (isAvailable()) {
            return null
        }
        return loadError ?: "Unknown error loading libmpv"
    }
    
    /**
     * Get the platform-specific library name
     * 
     * @return Library name for the current platform
     */
    private fun getLibraryName(): String {
        return when {
            Platform.isMac() -> "mpv"
            Platform.isWindows() -> "mpv-2"
            Platform.isLinux() -> "mpv"
            else -> "mpv"
        }
    }
    
    /**
     * Get the platform-specific library file name
     * 
     * @return Library file name for the current platform
     */
    private fun getLibraryFileName(): String {
        return when {
            Platform.isMac() -> "libmpv.dylib"
            Platform.isWindows() -> "mpv-2.dll"
            Platform.isLinux() -> "libmpv.so"
            else -> "libmpv.so"
        }
    }
    
    /**
     * Get platform-specific search paths for libmpv
     * 
     * @return List of directories to search for libmpv
     */
    private fun getSearchPaths(): List<String> {
        return when {
            Platform.isMac() -> listOf(
                "/opt/homebrew/lib",           // Apple Silicon Homebrew
                "/usr/local/lib",               // Intel Homebrew
                "/opt/local/lib",               // MacPorts
                System.getProperty("user.home") + "/lib"
            )
            Platform.isLinux() -> listOf(
                "/usr/lib",
                "/usr/local/lib",
                "/usr/lib/x86_64-linux-gnu",
                "/usr/lib64",
                System.getProperty("user.home") + "/.local/lib"
            )
            Platform.isWindows() -> listOf(
                System.getenv("ProgramFiles") + "\\mpv",
                System.getenv("ProgramFiles(x86)") + "\\mpv",
                System.getProperty("user.home") + "\\mpv",
                "C:\\mpv"
            )
            else -> emptyList()
        }
    }
    
    /**
     * Build a detailed error message with installation instructions
     * 
     * @param exception The exception that occurred during loading
     * @return Detailed error message with installation instructions
     */
    private fun buildErrorMessage(exception: Exception): String {
        val baseMessage = "libmpv library not found: ${exception.message}"
        val instructions = getInstallationInstructions()
        return "$baseMessage\n\n$instructions"
    }
    
    /**
     * Get platform-specific installation instructions
     * 
     * @return Installation instructions for the current platform
     */
    fun getInstallationInstructions(): String {
        return when {
            Platform.isMac() -> """
                libmpv is not installed on your system.
                
                To install libmpv on macOS:
                
                Using Homebrew (recommended):
                  brew install mpv
                
                Using MacPorts:
                  sudo port install mpv
                
                After installation, restart the application.
                
                Searched paths:
                ${getSearchPaths().joinToString("\n  - ", "  - ")}
            """.trimIndent()
            
            Platform.isLinux() -> """
                libmpv is not installed on your system.
                
                To install libmpv on Linux:
                
                Ubuntu/Debian:
                  sudo apt-get install libmpv-dev
                
                Fedora:
                  sudo dnf install mpv-libs-devel
                
                Arch Linux:
                  sudo pacman -S mpv
                
                After installation, restart the application.
                
                Searched paths:
                ${getSearchPaths().joinToString("\n  - ", "  - ")}
            """.trimIndent()
            
            Platform.isWindows() -> """
                libmpv is not installed on your system.
                
                To install libmpv on Windows:
                
                1. Download MPV from: https://mpv.io/installation/
                2. Extract the archive to a folder (e.g., C:\mpv)
                3. Ensure the folder contains mpv-2.dll
                4. Restart the application
                
                Alternatively, you can place mpv-2.dll in one of these locations:
                ${getSearchPaths().joinToString("\n  - ", "  - ")}
            """.trimIndent()
            
            else -> """
                libmpv is not installed on your system.
                
                Please install MPV media player from: https://mpv.io/installation/
                
                After installation, restart the application.
            """.trimIndent()
        }
    }
    
    /**
     * Get the loaded libmpv bindings
     * 
     * @return LibmpvBindings instance, or null if not loaded
     */
    fun getBindings(): LibmpvBindings? {
        return cachedBindings
    }
    
    /**
     * Clear the cached bindings (for testing purposes)
     */
    internal fun clearCache() {
        cachedBindings = null
        loadError = null
        extractedLibPath = null
    }
    
    /**
     * Check if bundled library is available
     * 
     * @return true if bundled library exists in resources
     */
    fun isBundledLibraryAvailable(): Boolean {
        val platformDir = getPlatformResourceDir()
        val libraryFileName = getLibraryFileName()
        val resourcePath = "/native/$platformDir/$libraryFileName"
        
        return LibmpvLoader::class.java.getResource(resourcePath) != null
    }
}
