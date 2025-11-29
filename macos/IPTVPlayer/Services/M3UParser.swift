//
//  M3UParser.swift
//  IPTVPlayer
//
//  M3U playlist parser implementation
//

import Foundation

/// Protocol for M3U playlist parsing
protocol M3UParser {
    /// Parse M3U content and extract channels
    /// - Parameter content: The M3U playlist content as a string
    /// - Returns: Array of parsed channels
    /// - Throws: M3UParserError if parsing fails completely
    func parse(content: String) async throws -> [Channel]
}

/// Errors that can occur during M3U parsing
enum M3UParserError: LocalizedError {
    case emptyContent
    case invalidFormat
    case noValidChannels
    
    var errorDescription: String? {
        switch self {
        case .emptyContent:
            return "M3U content is empty"
        case .invalidFormat:
            return "M3U format is invalid - missing #EXTM3U header"
        case .noValidChannels:
            return "No valid channels found in M3U content"
        }
    }
}

/// Implementation of M3U parser
class M3UParserImpl: M3UParser {
    
    func parse(content: String) async throws -> [Channel] {
        // Trim whitespace
        let trimmedContent = content.trimmingCharacters(in: .whitespacesAndNewlines)
        
        // Check for empty content
        guard !trimmedContent.isEmpty else {
            throw M3UParserError.emptyContent
        }
        
        // Split into lines
        let lines = trimmedContent.components(separatedBy: .newlines)
        
        // Check for M3U header
        guard lines.first?.hasPrefix("#EXTM3U") == true else {
            throw M3UParserError.invalidFormat
        }
        
        var channels: [Channel] = []
        var currentExtinf: String?
        
        for line in lines {
            let trimmedLine = line.trimmingCharacters(in: .whitespaces)
            
            // Skip empty lines and M3U header
            if trimmedLine.isEmpty || trimmedLine == "#EXTM3U" {
                continue
            }
            
            // Check if this is an EXTINF line
            if trimmedLine.hasPrefix("#EXTINF:") {
                currentExtinf = trimmedLine
            } else if !trimmedLine.hasPrefix("#") {
                // This is a URL line
                if let extinf = currentExtinf {
                    // Try to parse the channel
                    if let channel = parseChannel(extinf: extinf, url: trimmedLine) {
                        channels.append(channel)
                    }
                    // Reset for next channel
                    currentExtinf = nil
                }
            }
            // Ignore other comment lines
        }
        
        // If no valid channels were found, throw an error
        if channels.isEmpty {
            throw M3UParserError.noValidChannels
        }
        
        return channels
    }
    
    /// Parse a single channel from EXTINF line and URL
    private func parseChannel(extinf: String, url: String) -> Channel? {
        // Validate URL is not empty
        guard !url.trimmingCharacters(in: .whitespaces).isEmpty else {
            return nil
        }
        
        // Extract channel name (after the last comma)
        let name = extractChannelName(from: extinf)
        
        // Extract metadata attributes
        let logoUrl = extractAttribute(name: "tvg-logo", from: extinf)
        let group = extractAttribute(name: "group-title", from: extinf)
        let tvgId = extractAttribute(name: "tvg-id", from: extinf)
        
        return Channel(
            name: name,
            url: url.trimmingCharacters(in: .whitespaces),
            logoUrl: logoUrl,
            group: group,
            tvgId: tvgId
        )
    }
    
    /// Extract channel name from EXTINF line
    /// Format: #EXTINF:-1 [attributes],Channel Name
    private func extractChannelName(from extinf: String) -> String {
        // Find the last comma which separates attributes from name
        if let lastCommaIndex = extinf.lastIndex(of: ",") {
            let nameStartIndex = extinf.index(after: lastCommaIndex)
            let name = String(extinf[nameStartIndex...])
            return name.trimmingCharacters(in: .whitespaces)
        }
        
        // Fallback: return "Unknown Channel" if no comma found
        return "Unknown Channel"
    }
    
    /// Extract attribute value from EXTINF line
    /// Supports both quoted and unquoted values
    private func extractAttribute(name: String, from extinf: String) -> String? {
        // Pattern: attribute-name="value" or attribute-name=value
        let pattern = "\(name)=\"([^\"]*)\"|" + "\(name)=([^\\s,]+)"
        
        guard let regex = try? NSRegularExpression(pattern: pattern, options: []) else {
            return nil
        }
        
        let nsString = extinf as NSString
        let range = NSRange(location: 0, length: nsString.length)
        
        guard let match = regex.firstMatch(in: extinf, options: [], range: range) else {
            return nil
        }
        
        // Try quoted value first (capture group 1)
        if match.range(at: 1).location != NSNotFound {
            let valueRange = match.range(at: 1)
            let value = nsString.substring(with: valueRange)
            return value.isEmpty ? nil : value
        }
        
        // Try unquoted value (capture group 2)
        if match.range(at: 2).location != NSNotFound {
            let valueRange = match.range(at: 2)
            let value = nsString.substring(with: valueRange)
            return value.isEmpty ? nil : value
        }
        
        return nil
    }
}
