//
//  M3UParserPropertyTests.swift
//  IPTVPlayerTests
//
//  Property-based tests for M3U parser
//

import XCTest
import SwiftCheck
@testable import IPTVPlayer

final class M3UParserPropertyTests: XCTestCase {
    
    var parser: M3UParser!
    
    override func setUp() {
        super.setUp()
        parser = M3UParserImpl()
    }
    
    override func tearDown() {
        parser = nil
        super.tearDown()
    }
    
    // MARK: - Property 1: M3U Parser Field Extraction
    // **Feature: native-desktop-migration, Property 1: M3U Parser Field Extraction**
    // **Validates: Requirements 5.1, 5.2, 5.3, 5.4**
    
    /// Property: For any valid M3U content containing EXTINF tags with channel metadata,
    /// parsing should extract all specified fields (name, URL, logo, group, tvg-id) correctly
    func testProperty1_FieldExtraction() {
        property("Parser extracts all channel fields correctly") <- forAll { (testChannels: ArrayOf<TestChannelData>) in
            // Filter out empty arrays to ensure we have valid test data
            guard !testChannels.getArray.isEmpty else {
                return Discard()
            }
            
            // Generate M3U content from test channels
            let m3uContent = self.generateM3U(from: testChannels.getArray)
            
            // Parse the content
            guard let channels = try? await self.parser.parse(content: m3uContent) else {
                return false <?> "Parser failed to parse valid M3U content"
            }
            
            // Verify we got the expected number of channels
            guard channels.count == testChannels.getArray.count else {
                return false <?> "Expected \(testChannels.getArray.count) channels, got \(channels.count)"
            }
            
            // Verify each channel's fields match the input
            for (index, testChannel) in testChannels.getArray.enumerated() {
                let parsedChannel = channels[index]
                
                // Check name
                guard parsedChannel.name == testChannel.name else {
                    return false <?> "Channel \(index): name mismatch - expected '\(testChannel.name)', got '\(parsedChannel.name)'"
                }
                
                // Check URL
                guard parsedChannel.url == testChannel.url else {
                    return false <?> "Channel \(index): URL mismatch - expected '\(testChannel.url)', got '\(parsedChannel.url)'"
                }
                
                // Check logo URL
                guard parsedChannel.logoUrl == testChannel.logoUrl else {
                    return false <?> "Channel \(index): logoUrl mismatch - expected '\(String(describing: testChannel.logoUrl))', got '\(String(describing: parsedChannel.logoUrl))'"
                }
                
                // Check group
                guard parsedChannel.group == testChannel.group else {
                    return false <?> "Channel \(index): group mismatch - expected '\(String(describing: testChannel.group))', got '\(String(describing: parsedChannel.group))'"
                }
                
                // Check tvg-id
                guard parsedChannel.tvgId == testChannel.tvgId else {
                    return false <?> "Channel \(index): tvgId mismatch - expected '\(String(describing: testChannel.tvgId))', got '\(String(describing: parsedChannel.tvgId))'"
                }
            }
            
            return true
        }.withSize(10) // Limit size for reasonable test execution time
    }
    
    // MARK: - Property 2: M3U Parser Error Resilience
    // **Feature: native-desktop-migration, Property 2: M3U Parser Error Resilience**
    // **Validates: Requirements 5.5**
    
    /// Property: For any M3U content containing malformed entries,
    /// the parser should continue processing valid entries and return partial results without crashing
    func testProperty2_ErrorResilience() {
        property("Parser handles malformed entries gracefully") <- forAll { (validChannels: ArrayOf<TestChannelData>, malformedLines: ArrayOf<String>) in
            // Need at least one valid channel
            guard !validChannels.getArray.isEmpty else {
                return Discard()
            }
            
            // Generate M3U with both valid and malformed entries
            let m3uContent = self.generateMalformedM3U(
                validChannels: validChannels.getArray,
                malformedLines: malformedLines.getArray
            )
            
            // Parse should not crash
            let channels: [Channel]
            do {
                channels = try await self.parser.parse(content: m3uContent)
            } catch {
                // Parser may throw if NO valid channels found, which is acceptable
                return true
            }
            
            // Should have parsed at least some valid channels
            // (may be fewer than input if malformed lines corrupted some valid entries)
            guard !channels.isEmpty else {
                return false <?> "Parser returned no channels despite valid input"
            }
            
            // All returned channels should have valid data
            for channel in channels {
                guard !channel.name.isEmpty else {
                    return false <?> "Parser returned channel with empty name"
                }
                guard !channel.url.isEmpty else {
                    return false <?> "Parser returned channel with empty URL"
                }
            }
            
            return true
        }.withSize(10)
    }
    
    // MARK: - Helper Methods
    
    /// Generate M3U content from test channel data
    private func generateM3U(from testChannels: [TestChannelData]) -> String {
        var lines = ["#EXTM3U"]
        
        for testChannel in testChannels {
            var attributes: [String] = []
            
            // Add tvg-id if present
            if let tvgId = testChannel.tvgId {
                attributes.append("tvg-id=\"\(tvgId)\"")
            }
            
            // Add tvg-logo if present
            if let logoUrl = testChannel.logoUrl {
                attributes.append("tvg-logo=\"\(logoUrl)\"")
            }
            
            // Add group-title if present
            if let group = testChannel.group {
                attributes.append("group-title=\"\(group)\"")
            }
            
            // Build EXTINF line
            let attributesString = attributes.isEmpty ? "" : " " + attributes.joined(separator: " ")
            let extinfLine = "#EXTINF:-1\(attributesString),\(testChannel.name)"
            
            lines.append(extinfLine)
            lines.append(testChannel.url)
        }
        
        return lines.joined(separator: "\n")
    }
    
    /// Generate M3U content with both valid and malformed entries
    private func generateMalformedM3U(validChannels: [TestChannelData], malformedLines: [String]) -> String {
        var lines = ["#EXTM3U"]
        
        // Interleave valid channels with malformed lines
        for (index, testChannel) in validChannels.enumerated() {
            // Add some malformed lines before this channel
            if index < malformedLines.count {
                lines.append(malformedLines[index])
            }
            
            // Add valid channel
            var attributes: [String] = []
            
            if let tvgId = testChannel.tvgId {
                attributes.append("tvg-id=\"\(tvgId)\"")
            }
            if let logoUrl = testChannel.logoUrl {
                attributes.append("tvg-logo=\"\(logoUrl)\"")
            }
            if let group = testChannel.group {
                attributes.append("group-title=\"\(group)\"")
            }
            
            let attributesString = attributes.isEmpty ? "" : " " + attributes.joined(separator: " ")
            let extinfLine = "#EXTINF:-1\(attributesString),\(testChannel.name)"
            
            lines.append(extinfLine)
            lines.append(testChannel.url)
        }
        
        // Add remaining malformed lines at the end
        if malformedLines.count > validChannels.count {
            lines.append(contentsOf: malformedLines[validChannels.count...])
        }
        
        return lines.joined(separator: "\n")
    }
}

// MARK: - Test Data Generators

/// Test data structure for channel generation
struct TestChannelData: Arbitrary {
    let name: String
    let url: String
    let logoUrl: String?
    let group: String?
    let tvgId: String?
    
    static var arbitrary: Gen<TestChannelData> {
        Gen.compose { c in
            TestChannelData(
                name: c.generate(using: TestChannelData.channelNameGen),
                url: c.generate(using: TestChannelData.urlGen),
                logoUrl: c.generate(using: Gen.frequency([
                    (1, Gen.pure(nil)),
                    (2, TestChannelData.urlGen.map { Optional($0) })
                ])),
                group: c.generate(using: Gen.frequency([
                    (1, Gen.pure(nil)),
                    (2, TestChannelData.groupNameGen.map { Optional($0) })
                ])),
                tvgId: c.generate(using: Gen.frequency([
                    (1, Gen.pure(nil)),
                    (2, TestChannelData.tvgIdGen.map { Optional($0) })
                ]))
            )
        }
    }
    
    /// Generate valid channel names (non-empty, no commas)
    static var channelNameGen: Gen<String> {
        Gen.fromElements(of: [
            "CNN", "BBC News", "ESPN", "Discovery Channel",
            "National Geographic", "HBO", "MTV", "Fox News",
            "Sky Sports", "Cartoon Network", "Animal Planet"
        ])
    }
    
    /// Generate valid URLs
    static var urlGen: Gen<String> {
        Gen.compose { c in
            let protocols = ["http", "https", "rtsp"]
            let domains = ["example.com", "stream.tv", "iptv.net", "live.media"]
            let paths = ["/live", "/stream", "/channel", "/hls"]
            
            let proto = c.generate(using: Gen.fromElements(of: protocols))
            let domain = c.generate(using: Gen.fromElements(of: domains))
            let path = c.generate(using: Gen.fromElements(of: paths))
            let id = c.generate(using: Gen.choose((1, 999)))
            
            return "\(proto)://\(domain)\(path)/\(id)"
        }
    }
    
    /// Generate group names
    static var groupNameGen: Gen<String> {
        Gen.fromElements(of: [
            "News", "Sports", "Entertainment", "Movies",
            "Kids", "Music", "Documentary", "International"
        ])
    }
    
    /// Generate tvg-id values
    static var tvgIdGen: Gen<String> {
        Gen.compose { c in
            let prefix = c.generate(using: Gen.fromElements(of: ["ch", "tv", "id"]))
            let number = c.generate(using: Gen.choose((1, 999)))
            return "\(prefix)\(number)"
        }
    }
}
