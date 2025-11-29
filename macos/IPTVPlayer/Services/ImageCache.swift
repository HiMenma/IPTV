//
//  ImageCache.swift
//  IPTVPlayer
//
//  Image caching service for optimized image loading
//

import Foundation
import AppKit

/// Image cache service that provides memory and disk caching for channel logos
actor ImageCache {
    static let shared = ImageCache()
    
    private var memoryCache: NSCache<NSString, NSImage>
    private let fileManager = FileManager.default
    private let cacheDirectory: URL
    private let maxMemoryCacheSize: Int = 50 * 1024 * 1024 // 50 MB
    private let maxDiskCacheSize: Int = 200 * 1024 * 1024 // 200 MB
    
    private init() {
        memoryCache = NSCache<NSString, NSImage>()
        memoryCache.totalCostLimit = maxMemoryCacheSize
        
        // Setup disk cache directory
        let cachesDirectory = fileManager.urls(for: .cachesDirectory, in: .userDomainMask).first!
        cacheDirectory = cachesDirectory.appendingPathComponent("ImageCache", isDirectory: true)
        
        // Create cache directory if it doesn't exist
        try? fileManager.createDirectory(at: cacheDirectory, withIntermediateDirectories: true)
        
        // Note: macOS doesn't have memory warnings like iOS
        // We rely on manual cache management and size limits
    }
    
    // MARK: - Public Methods
    
    /// Load image from cache or download if not cached
    func loadImage(from urlString: String) async throws -> NSImage {
        let cacheKey = cacheKey(for: urlString)
        
        // Check memory cache first
        if let cachedImage = memoryCache.object(forKey: cacheKey as NSString) {
            return cachedImage
        }
        
        // Check disk cache
        if let diskImage = try? await loadFromDisk(cacheKey: cacheKey) {
            // Store in memory cache for faster access
            memoryCache.setObject(diskImage, forKey: cacheKey as NSString)
            return diskImage
        }
        
        // Download image
        guard let url = URL(string: urlString) else {
            throw ImageCacheError.invalidURL
        }
        
        let (data, _) = try await URLSession.shared.data(from: url)
        guard let image = NSImage(data: data) else {
            throw ImageCacheError.invalidImageData
        }
        
        // Cache the image
        await cacheImage(image, for: cacheKey, data: data)
        
        return image
    }
    
    /// Clear all cached images
    func clearCache() async {
        clearMemoryCache()
        await clearDiskCache()
    }
    
    /// Clear only memory cache
    func clearMemoryCache() {
        memoryCache.removeAllObjects()
    }
    
    /// Get current cache size
    func getCacheSize() async -> Int64 {
        var totalSize: Int64 = 0
        
        guard let enumerator = fileManager.enumerator(at: cacheDirectory, includingPropertiesForKeys: [.fileSizeKey]) else {
            return 0
        }
        
        for case let fileURL as URL in enumerator {
            if let resourceValues = try? fileURL.resourceValues(forKeys: [.fileSizeKey]),
               let fileSize = resourceValues.fileSize {
                totalSize += Int64(fileSize)
            }
        }
        
        return totalSize
    }
    
    // MARK: - Private Methods
    
    private func cacheKey(for urlString: String) -> String {
        return urlString.data(using: .utf8)?.base64EncodedString() ?? urlString
    }
    
    private func cacheImage(_ image: NSImage, for key: String, data: Data) async {
        // Store in memory cache
        memoryCache.setObject(image, forKey: key as NSString, cost: data.count)
        
        // Store in disk cache
        let fileURL = cacheDirectory.appendingPathComponent(key)
        try? data.write(to: fileURL)
        
        // Check disk cache size and clean if necessary
        await cleanDiskCacheIfNeeded()
    }
    
    private func loadFromDisk(cacheKey: String) async throws -> NSImage {
        let fileURL = cacheDirectory.appendingPathComponent(cacheKey)
        let data = try Data(contentsOf: fileURL)
        
        guard let image = NSImage(data: data) else {
            throw ImageCacheError.invalidImageData
        }
        
        return image
    }
    
    private func clearDiskCache() async {
        try? fileManager.removeItem(at: cacheDirectory)
        try? fileManager.createDirectory(at: cacheDirectory, withIntermediateDirectories: true)
    }
    
    private func cleanDiskCacheIfNeeded() async {
        let currentSize = await getCacheSize()
        
        guard currentSize > maxDiskCacheSize else { return }
        
        // Get all cached files sorted by modification date
        guard let enumerator = fileManager.enumerator(
            at: cacheDirectory,
            includingPropertiesForKeys: [.contentModificationDateKey, .fileSizeKey]
        ) else { return }
        
        var files: [(url: URL, date: Date, size: Int)] = []
        
        for case let fileURL as URL in enumerator {
            if let resourceValues = try? fileURL.resourceValues(forKeys: [.contentModificationDateKey, .fileSizeKey]),
               let modificationDate = resourceValues.contentModificationDate,
               let fileSize = resourceValues.fileSize {
                files.append((fileURL, modificationDate, fileSize))
            }
        }
        
        // Sort by modification date (oldest first)
        files.sort { $0.date < $1.date }
        
        // Delete oldest files until we're under the limit
        var deletedSize = 0
        let targetSize = maxDiskCacheSize * 3 / 4 // Delete until we're at 75% capacity
        
        for file in files {
            guard currentSize - Int64(deletedSize) > targetSize else { break }
            
            try? fileManager.removeItem(at: file.url)
            deletedSize += file.size
        }
    }
}

// MARK: - Error Types

enum ImageCacheError: LocalizedError {
    case invalidURL
    case invalidImageData
    
    var errorDescription: String? {
        switch self {
        case .invalidURL:
            return "Invalid image URL"
        case .invalidImageData:
            return "Invalid image data"
        }
    }
}
