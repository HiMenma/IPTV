# Code Sharing Strategy for macOS Native Application

## Executive Summary

After evaluating the options for code sharing between the existing Kotlin Multiplatform codebase and the new native macOS application, **we recommend implementing the macOS application entirely in native Swift** without attempting to share Kotlin code via Kotlin/Native.

**Decision: Native Swift Implementation (Option 3)**

## Evaluation Criteria

The following criteria were used to evaluate each option:

1. **Development Velocity**: Time to implement and iterate
2. **Maintenance Burden**: Long-term cost of maintaining the solution
3. **Performance**: Runtime performance and resource usage
4. **Platform Integration**: How well it integrates with macOS ecosystem
5. **Debugging Experience**: Ease of troubleshooting issues
6. **Team Expertise**: Required skills and learning curve
7. **Build Complexity**: CI/CD and build system complexity

## Option 1: Kotlin/Native Framework

### Description
Compile existing Kotlin shared code (M3U parser, Xtream API client, data models) to a native macOS framework using Kotlin/Native, then consume it from Swift.

### Current Shared Code Analysis

The existing Kotlin codebase has the following shared components:

**M3uParser.kt** (~60 lines)
- Parses M3U playlist format
- Extracts channel metadata (name, URL, logo, group, tvg-id)
- Uses regex for parsing EXTINF tags
- Dependencies: Kotlin coroutines

**XtreamClient.kt** (~150 lines)
- Xtream Codes API client
- Authentication, live categories, live streams
- Dependencies: Ktor HTTP client, kotlinx.serialization

**Models.kt** (~40 lines)
- Data classes: Channel, Playlist, Category, XtreamAccount
- Simple data structures with no complex logic

**Total Shared Code: ~250 lines**

### Pros
- ✅ Reuses existing tested Kotlin code
- ✅ Maintains consistency with Android implementation
- ✅ Reduces initial implementation time for business logic

### Cons
- ❌ **Kotlin/Native is still maturing**: Known issues with memory management, debugging, and stability
- ❌ **Complex build setup**: Requires maintaining Kotlin/Native toolchain alongside Xcode
- ❌ **Poor debugging experience**: Crashes in native code are hard to debug, limited Swift debugger support
- ❌ **FFI overhead**: Performance penalty for Swift ↔ Kotlin interop
- ❌ **Type mapping complexity**: Converting between Swift and Kotlin types (especially async/await vs coroutines)
- ❌ **Limited platform integration**: Cannot easily use Swift-specific features (async/await, Combine, SwiftUI state management)
- ❌ **Dependency management**: Need to bundle Kotlin runtime and dependencies
- ❌ **Team expertise**: Requires maintaining Kotlin/Native knowledge
- ❌ **CI/CD complexity**: Need to build Kotlin framework before building Swift app

### Implementation Complexity: **HIGH**

Example of the complexity:

```swift
// Swift side - awkward interop
import KotlinShared

class PlaylistViewModel {
    private let parser = KotlinM3UParser()
    
    func parsePlaylist(content: String) async throws -> [Channel] {
        // Need to bridge between Swift async/await and Kotlin coroutines
        return try await withCheckedThrowingContinuation { continuation in
            parser.parse(content: content) { result, error in
                if let error = error {
                    continuation.resume(throwing: error)
                } else if let channels = result {
                    // Need to convert Kotlin objects to Swift objects
                    let swiftChannels = channels.map { kotlinChannel in
                        Channel(
                            id: kotlinChannel.id,
                            name: kotlinChannel.name,
                            url: kotlinChannel.url,
                            // ... more mapping
                        )
                    }
                    continuation.resume(returning: swiftChannels)
                }
            }
        }
    }
}
```

## Option 2: Rust Shared Library

### Description
Rewrite shared business logic in Rust and expose it to Swift via FFI.

### Pros
- ✅ Excellent performance
- ✅ Strong cross-platform support
- ✅ Memory safety guarantees
- ✅ Good FFI support

### Cons
- ❌ **Complete rewrite required**: All ~250 lines need to be rewritten in Rust
- ❌ **Learning curve**: Team needs to learn Rust
- ❌ **Development time**: Significantly longer than other options
- ❌ **FFI complexity**: Still need to handle Swift ↔ Rust interop
- ❌ **Overkill for this use case**: The shared code is simple and doesn't require Rust's benefits

### Implementation Complexity: **VERY HIGH**

## Option 3: Native Swift Implementation (RECOMMENDED)

### Description
Implement all business logic natively in Swift, without attempting to share code with the Kotlin codebase.

### Pros
- ✅ **Simple and straightforward**: No FFI, no interop complexity
- ✅ **Excellent debugging**: Full Xcode debugger support
- ✅ **Best platform integration**: Native Swift async/await, Combine, SwiftUI
- ✅ **Fast development**: No build system complexity
- ✅ **Easy maintenance**: Single language, standard Swift patterns
- ✅ **Better performance**: No FFI overhead
- ✅ **Team friendly**: Most iOS/macOS developers know Swift
- ✅ **Clean CI/CD**: Standard Xcode build process

### Cons
- ❌ Code duplication (~250 lines)
- ❌ Need to maintain two implementations
- ❌ Potential for divergence between platforms

### Implementation Complexity: **LOW**

### Code Duplication Analysis

The amount of code to duplicate is minimal:

1. **M3U Parser**: ~60 lines → Simple regex parsing, easy to implement in Swift
2. **Xtream Client**: ~150 lines → Standard HTTP requests, trivial with URLSession
3. **Data Models**: ~40 lines → Simple structs in Swift

**Total duplication: ~250 lines of straightforward code**

This is a small amount of code that is:
- Easy to understand and maintain
- Not algorithmically complex
- Mostly I/O and data transformation
- Well-defined by the requirements

### Native Swift Implementation Example

```swift
// Swift M3U Parser - Clean and idiomatic
class M3UParser {
    func parse(content: String) async throws -> [Channel] {
        var channels: [Channel] = []
        let lines = content.components(separatedBy: .newlines)
        
        var currentName: String?
        var currentLogo: String?
        var currentGroup: String?
        var currentId: String?
        
        for line in lines {
            let trimmed = line.trimmingCharacters(in: .whitespaces)
            
            if trimmed.hasPrefix("#EXTINF:") {
                let info = trimmed.replacingOccurrences(of: "#EXTINF:", with: "")
                
                // Extract name (after last comma)
                if let commaIndex = info.lastIndex(of: ",") {
                    currentName = String(info[info.index(after: commaIndex)...]).trimmingCharacters(in: .whitespaces)
                }
                
                // Extract attributes using regex
                currentLogo = extractAttribute(from: info, attribute: "tvg-logo")
                currentGroup = extractAttribute(from: info, attribute: "group-title")
                currentId = extractAttribute(from: info, attribute: "tvg-id")
                
            } else if !trimmed.isEmpty && !trimmed.hasPrefix("#") {
                // This is the URL
                if let name = currentName {
                    channels.append(Channel(
                        id: currentId ?? trimmed,
                        name: name,
                        url: trimmed,
                        logoUrl: currentLogo,
                        group: currentGroup
                    ))
                    
                    // Reset for next channel
                    currentName = nil
                    currentLogo = nil
                    currentGroup = nil
                    currentId = nil
                }
            }
        }
        
        return channels
    }
    
    private func extractAttribute(from text: String, attribute: String) -> String? {
        let pattern = "\(attribute)=\"([^\"]*)\""
        guard let regex = try? NSRegularExpression(pattern: pattern),
              let match = regex.firstMatch(in: text, range: NSRange(text.startIndex..., in: text)),
              let range = Range(match.range(at: 1), in: text) else {
            return nil
        }
        return String(text[range])
    }
}

// Swift Xtream Client - Using native URLSession
class XtreamClient {
    private let session: URLSession
    
    init(session: URLSession = .shared) {
        self.session = session
    }
    
    func authenticate(account: XtreamAccount) async throws -> Bool {
        var components = URLComponents(string: "\(account.serverUrl)/player_api.php")!
        components.queryItems = [
            URLQueryItem(name: "username", value: account.username),
            URLQueryItem(name: "password", value: account.password)
        ]
        
        let (data, _) = try await session.data(from: components.url!)
        let response = try JSONDecoder().decode(XtreamAuthResponse.self, from: data)
        return response.userInfo.auth == 1
    }
    
    func getLiveStreams(account: XtreamAccount) async throws -> [Channel] {
        var components = URLComponents(string: "\(account.serverUrl)/player_api.php")!
        components.queryItems = [
            URLQueryItem(name: "username", value: account.username),
            URLQueryItem(name: "password", value: account.password),
            URLQueryItem(name: "action", value: "get_live_streams")
        ]
        
        let (data, _) = try await session.data(from: components.url!)
        let streams = try JSONDecoder().decode([XtreamStream].self, from: data)
        
        return streams.map { stream in
            Channel(
                id: String(stream.streamId),
                name: stream.name,
                url: "\(account.serverUrl)/live/\(account.username)/\(account.password)/\(stream.streamId).ts",
                logoUrl: stream.streamIcon,
                group: stream.categoryId
            )
        }
    }
}
```

## Recommendation Rationale

### Why Native Swift is the Best Choice

1. **Minimal Code Duplication**: Only ~250 lines of simple, straightforward code
2. **Development Speed**: Faster to implement than setting up Kotlin/Native
3. **Maintenance**: Easier to maintain one clean Swift codebase than complex interop
4. **Quality**: Better debugging, testing, and development experience
5. **Performance**: No FFI overhead, native Swift performance
6. **Future-Proof**: Not dependent on Kotlin/Native maturity
7. **Team Efficiency**: Standard Swift development workflow

### Risk Mitigation for Code Duplication

To address the concern about maintaining two implementations:

1. **Comprehensive Test Suite**: Property-based tests ensure both implementations behave identically
2. **Shared Test Cases**: Use the same test data and expected outputs across platforms
3. **Clear Specifications**: The design document provides precise specifications for behavior
4. **Simple Logic**: The business logic is straightforward and unlikely to change frequently

### When to Reconsider

This decision should be reconsidered if:

- The shared business logic grows to >1000 lines
- Complex algorithms need to be shared (e.g., video codec implementation)
- The team gains significant Kotlin/Native expertise
- Kotlin/Native tooling significantly improves

## Implementation Plan

### Phase 1: Core Business Logic (Week 1)

1. **Create Swift Package for Shared Logic**
   ```
   macos/IPTVPlayer/Services/
   ├── M3UParser.swift
   ├── XtreamClient.swift
   └── Models/
       ├── Channel.swift
       ├── Playlist.swift
       ├── Category.swift
       └── XtreamAccount.swift
   ```

2. **Implement M3U Parser**
   - Port parsing logic from Kotlin to Swift
   - Use native Swift regex and string manipulation
   - Add comprehensive unit tests

3. **Implement Xtream Client**
   - Use URLSession for HTTP requests
   - Use Codable for JSON parsing
   - Implement retry logic with exponential backoff
   - Add unit tests with mock responses

4. **Define Data Models**
   - Create Swift structs for all data models
   - Implement Codable for serialization
   - Add validation logic

### Phase 2: Testing (Week 1)

1. **Unit Tests**
   - Test M3U parser with various formats
   - Test Xtream client with mock responses
   - Test error handling

2. **Property-Based Tests**
   - Use swift-check for property testing
   - Verify parser correctness properties
   - Verify API client error handling

3. **Integration Tests**
   - Test with real M3U files
   - Test with Xtream test servers (if available)

### Phase 3: Documentation (Week 1)

1. **API Documentation**
   - Document all public interfaces
   - Add usage examples
   - Document error cases

2. **Architecture Documentation**
   - Update design document
   - Document Swift-specific patterns
   - Add sequence diagrams

## Interface Specifications

### M3UParser Protocol

```swift
protocol M3UParser {
    /// Parses M3U playlist content and returns a list of channels
    /// - Parameter content: The M3U playlist content as a string
    /// - Returns: Array of Channel objects
    /// - Throws: M3UParseError if parsing fails
    func parse(content: String) async throws -> [Channel]
}

enum M3UParseError: Error {
    case invalidFormat(message: String)
    case emptyContent
}
```

### XtreamClient Protocol

```swift
protocol XtreamClient {
    /// Authenticates with Xtream Codes server
    /// - Parameter account: Xtream account credentials
    /// - Returns: true if authentication successful
    /// - Throws: XtreamError if request fails
    func authenticate(account: XtreamAccount) async throws -> Bool
    
    /// Fetches live categories from Xtream server
    /// - Parameter account: Xtream account credentials
    /// - Returns: Array of Category objects
    /// - Throws: XtreamError if request fails
    func getLiveCategories(account: XtreamAccount) async throws -> [Category]
    
    /// Fetches live streams from Xtream server
    /// - Parameter account: Xtream account credentials
    /// - Returns: Array of Channel objects
    /// - Throws: XtreamError if request fails
    func getLiveStreams(account: XtreamAccount) async throws -> [Channel]
}

enum XtreamError: Error {
    case networkError(underlying: Error)
    case authenticationFailed
    case invalidResponse
    case serverError(statusCode: Int)
}
```

### Data Models

```swift
struct Channel: Codable, Identifiable, Equatable {
    let id: String
    let name: String
    let url: String
    let logoUrl: String?
    let group: String?
    let categoryId: String?
    let headers: [String: String]
    
    init(id: String, name: String, url: String, logoUrl: String? = nil, 
         group: String? = nil, categoryId: String? = nil, 
         headers: [String: String] = [:]) {
        self.id = id
        self.name = name
        self.url = url
        self.logoUrl = logoUrl
        self.group = group
        self.categoryId = categoryId
        self.headers = headers
    }
}

struct Playlist: Codable, Identifiable, Equatable {
    let id: String
    var name: String
    let url: String?
    let type: PlaylistType
    var channels: [Channel]
    var categories: [Category]
    let xtreamAccount: XtreamAccount?
    let createdAt: Date
    var updatedAt: Date
}

enum PlaylistType: String, Codable {
    case m3uUrl = "M3U_URL"
    case m3uFile = "M3U_FILE"
    case xtream = "XTREAM"
}

struct Category: Codable, Identifiable, Equatable {
    let id: String
    let name: String
    let parentId: String?
}

struct XtreamAccount: Codable, Equatable {
    let serverUrl: String
    let username: String
    let password: String
}
```

## Build Configuration

No special build configuration needed - standard Xcode project with Swift Package Manager for dependencies.

### Dependencies

```swift
// Package.swift
dependencies: [
    // No external dependencies needed for core business logic
    // URLSession and Foundation are built-in
]
```

## Testing Strategy

### Unit Tests (XCTest)

```swift
class M3UParserTests: XCTestCase {
    var parser: M3UParser!
    
    override func setUp() {
        super.setUp()
        parser = M3UParserImpl()
    }
    
    func testParseValidM3U() async throws {
        let content = """
        #EXTM3U
        #EXTINF:-1 tvg-id="cnn" tvg-logo="http://logo.png" group-title="News",CNN
        http://stream.cnn.com/live
        """
        
        let channels = try await parser.parse(content: content)
        
        XCTAssertEqual(channels.count, 1)
        XCTAssertEqual(channels[0].name, "CNN")
        XCTAssertEqual(channels[0].url, "http://stream.cnn.com/live")
    }
}
```

### Property-Based Tests (swift-check)

```swift
import SwiftCheck

class M3UParserPropertyTests: XCTestCase {
    func testParserPreservesChannelCount() {
        property("Parser preserves channel count for valid M3U") <- forAll { (channels: [TestChannel]) in
            let m3u = self.generateValidM3U(from: channels)
            let parsed = try! await self.parser.parse(content: m3u)
            return parsed.count == channels.count
        }
    }
}
```

## Conclusion

**Decision: Implement macOS application entirely in native Swift**

This approach provides:
- ✅ Fastest development velocity
- ✅ Lowest maintenance burden
- ✅ Best debugging and development experience
- ✅ Excellent platform integration
- ✅ Simple build and CI/CD
- ✅ Minimal code duplication (~250 lines)

The benefits of native Swift far outweigh the cost of duplicating a small amount of straightforward business logic.

## Next Steps

1. ✅ Document this decision
2. ⏭️ Proceed to Task 4: Implement M3U parser in Swift
3. ⏭️ Proceed to Task 5: Implement Xtream API client in Swift
4. ⏭️ Set up comprehensive test suite

---

**Document Version**: 1.0  
**Date**: 2024-11-28  
**Author**: Development Team  
**Status**: Approved
