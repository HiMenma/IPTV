//
//  PerformanceMonitor.swift
//  IPTVPlayer
//
//  Performance monitoring utility for profiling and optimization
//

import Foundation

/// Performance monitoring utility for tracking execution times and memory usage
class PerformanceMonitor {
    static let shared = PerformanceMonitor()
    
    private var timers: [String: Date] = [:]
    private let logger = AppLogger.shared
    
    private init() {}
    
    // MARK: - Timing Methods
    
    /// Start timing an operation
    func startTimer(_ identifier: String) {
        timers[identifier] = Date()
    }
    
    /// Stop timing an operation and log the duration
    func stopTimer(_ identifier: String) {
        guard let startTime = timers[identifier] else {
            logger.warning("No timer found for identifier: \(identifier)", category: "Performance")
            return
        }
        
        let duration = Date().timeIntervalSince(startTime)
        logger.debug("⏱️ \(identifier): \(String(format: "%.3f", duration))s", category: "Performance")
        
        timers.removeValue(forKey: identifier)
    }
    
    /// Measure the execution time of a synchronous operation
    func measure<T>(_ identifier: String, operation: () throws -> T) rethrows -> T {
        startTimer(identifier)
        defer { stopTimer(identifier) }
        return try operation()
    }
    
    /// Measure the execution time of an asynchronous operation
    func measureAsync<T>(_ identifier: String, operation: () async throws -> T) async rethrows -> T {
        startTimer(identifier)
        defer { stopTimer(identifier) }
        return try await operation()
    }
    
    // MARK: - Memory Monitoring
    
    /// Get current memory usage in bytes
    func getCurrentMemoryUsage() -> UInt64 {
        var info = mach_task_basic_info()
        var count = mach_msg_type_number_t(MemoryLayout<mach_task_basic_info>.size) / 4
        
        let kerr: kern_return_t = withUnsafeMutablePointer(to: &info) {
            $0.withMemoryRebound(to: integer_t.self, capacity: 1) {
                task_info(
                    mach_task_self_,
                    task_flavor_t(MACH_TASK_BASIC_INFO),
                    $0,
                    &count
                )
            }
        }
        
        guard kerr == KERN_SUCCESS else {
            return 0
        }
        
        return info.resident_size
    }
    
    /// Log current memory usage
    func logMemoryUsage(_ context: String = "") {
        let memoryUsage = getCurrentMemoryUsage()
        let memoryMB = Double(memoryUsage) / 1024.0 / 1024.0
        
        let message = context.isEmpty
            ? "Memory usage: \(String(format: "%.2f", memoryMB)) MB"
            : "\(context) - Memory usage: \(String(format: "%.2f", memoryMB)) MB"
        
        logger.debug("💾 \(message)", category: "Performance")
    }
    
    /// Monitor memory usage during an operation
    func monitorMemory<T>(_ identifier: String, operation: () throws -> T) rethrows -> T {
        let startMemory = getCurrentMemoryUsage()
        logger.debug("💾 \(identifier) - Start memory: \(formatBytes(startMemory))", category: "Performance")
        
        let result = try operation()
        
        let endMemory = getCurrentMemoryUsage()
        let delta = Int64(endMemory) - Int64(startMemory)
        let deltaSign = delta >= 0 ? "+" : ""
        
        logger.debug("💾 \(identifier) - End memory: \(formatBytes(endMemory)) (\(deltaSign)\(formatBytes(UInt64(abs(delta)))))", category: "Performance")
        
        return result
    }
    
    /// Monitor memory usage during an async operation
    func monitorMemoryAsync<T>(_ identifier: String, operation: () async throws -> T) async rethrows -> T {
        let startMemory = getCurrentMemoryUsage()
        logger.debug("💾 \(identifier) - Start memory: \(formatBytes(startMemory))", category: "Performance")
        
        let result = try await operation()
        
        let endMemory = getCurrentMemoryUsage()
        let delta = Int64(endMemory) - Int64(startMemory)
        let deltaSign = delta >= 0 ? "+" : ""
        
        logger.debug("💾 \(identifier) - End memory: \(formatBytes(endMemory)) (\(deltaSign)\(formatBytes(UInt64(abs(delta)))))", category: "Performance")
        
        return result
    }
    
    // MARK: - Helper Methods
    
    private func formatBytes(_ bytes: UInt64) -> String {
        let formatter = ByteCountFormatter()
        formatter.countStyle = .memory
        return formatter.string(fromByteCount: Int64(bytes))
    }
}

// MARK: - Convenience Extensions

extension PerformanceMonitor {
    /// Profile a database query
    func profileDatabaseQuery<T>(_ queryName: String, operation: () async throws -> T) async rethrows -> T {
        return try await measureAsync("DB Query: \(queryName)") {
            try await operation()
        }
    }
    
    /// Profile a network request
    func profileNetworkRequest<T>(_ requestName: String, operation: () async throws -> T) async rethrows -> T {
        return try await measureAsync("Network: \(requestName)") {
            try await operation()
        }
    }
    
    /// Profile UI rendering
    func profileUIRender<T>(_ viewName: String, operation: () -> T) -> T {
        return measure("UI Render: \(viewName)") {
            operation()
        }
    }
}
