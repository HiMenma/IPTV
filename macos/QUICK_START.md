# Quick Start Guide - macOS Native Implementation

This guide helps you quickly understand the code sharing decision and start implementing the macOS application.

## TL;DR

**Decision**: Native Swift implementation (no Kotlin/Native)  
**Reason**: Only ~250 lines of simple code to duplicate, native is faster and better  
**Next Steps**: Implement M3U parser, Xtream client, and repositories in Swift

## The Decision

We evaluated three options for code sharing:

| Option | Pros | Cons | Verdict |
|--------|------|------|---------|
| Kotlin/Native | Reuse existing code | Complex FFI, poor debugging, high maintenance | ❌ Rejected |
| Rust FFI | High performance | Complete rewrite, learning curve | ❌ Rejected |
| Native Swift | Simple, fast, native | ~250 lines duplicated | ✅ **CHOSEN** |

## What This Means

### You Will Implement in Swift:

1. **M3UParser** (~60 lines)
   - Parse M3U playlist format
   - Extract channel metadata
   - Handle malformed entries

2. **XtreamClient** (~150 lines)
   - HTTP requests to Xtream API
   - Authentication
   - Fetch categories and streams

3. **Data Models** (~40 lines)
   - Channel, Playlist, Category, XtreamAccount
   - Simple structs with Codable

### You Will NOT:

- ❌ Set up Kotlin/Native toolchain
- ❌ Deal with FFI bridges
- ❌ Convert between Kotlin and Swift types
- ❌ Debug crashes across language boundaries
- ❌ Maintain complex build configurations

## Implementation Checklist

### Phase 1: Core Services (Week 1)

- [ ] Create `Services/M3UParser.swift`
  - Implement `parse(content:)` method
  - Use regex for EXTINF parsing
  - Write unit tests

- [ ] Create `Services/XtreamClient.swift`
  - Implement authentication
  - Implement stream fetching
  - Add retry logic
  - Write unit tests

- [ ] Create `Models/` directory
  - Define Channel, Playlist, Category structs
  - Implement Codable conformance
  - Add validation

### Phase 2: Persistence (Week 1)

- [ ] Create `Services/PlaylistRepository.swift`
  - Implement Core Data operations
  - Add CRUD methods
  - Write integration tests

- [ ] Create `Services/FavoriteRepository.swift`
  - Implement favorite management
  - Write integration tests

### Phase 3: Testing (Week 1)

- [ ] Write property-based tests
  - M3U parser properties
  - Repository round-trip properties
  - Use swift-check

- [ ] Write integration tests
  - End-to-end workflows
  - Real Core Data stack

## Code Examples

### M3U Parser (Swift)

```swift
class M3UParser {
    func parse(content: String) async throws -> [Channel] {
        var channels: [Channel] = []
        let lines = content.components(separatedBy: .newlines)
        
        var currentName: String?
        var currentLogo: String?
        var currentGroup: String?
        
        for line in lines {
            if line.hasPrefix("#EXTINF:") {
                // Extract metadata
                currentName = extractName(from: line)
                currentLogo = extractAttribute(from: line, attribute: "tvg-logo")
                currentGroup = extractAttribute(from: line, attribute: "group-title")
            } else if !line.isEmpty && !line.hasPrefix("#") {
                // This is the URL
                if let name = currentName {
                    channels.append(Channel(
                        id: UUID().uuidString,
                        name: name,
                        url: line,
                        logoUrl: currentLogo,
                        group: currentGroup
                    ))
                }
                // Reset
                currentName = nil
                currentLogo = nil
                currentGroup = nil
            }
        }
        
        return channels
    }
}
```

### Xtream Client (Swift)

```swift
class XtreamClient {
    private let session: URLSession
    
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
}
```

## Testing Examples

### Unit Test

```swift
class M3UParserTests: XCTestCase {
    func testParseValidM3U() async throws {
        let parser = M3UParser()
        let content = """
        #EXTM3U
        #EXTINF:-1 tvg-logo="logo.png" group-title="News",CNN
        http://stream.cnn.com/live
        """
        
        let channels = try await parser.parse(content: content)
        
        XCTAssertEqual(channels.count, 1)
        XCTAssertEqual(channels[0].name, "CNN")
        XCTAssertEqual(channels[0].logoUrl, "logo.png")
    }
}
```

### Property Test

```swift
import SwiftCheck

class M3UParserPropertyTests: XCTestCase {
    func testParserPreservesChannelCount() {
        property("Valid M3U preserves channel count") <- forAll { (channels: [TestChannel]) in
            let m3u = generateM3U(from: channels)
            let parsed = try! await parser.parse(content: m3u)
            return parsed.count == channels.count
        }
    }
}
```

## Key Resources

### Must Read (in order):
1. [INTERFACE_SPECIFICATIONS.md](./INTERFACE_SPECIFICATIONS.md) - Complete API specs
2. [CODE_SHARING_STRATEGY.md](./CODE_SHARING_STRATEGY.md) - Detailed rationale
3. [Design Document](../.kiro/specs/native-desktop-migration/design.md) - Overall architecture

### Reference:
- [Requirements](../.kiro/specs/native-desktop-migration/requirements.md) - What we're building
- [Tasks](../.kiro/specs/native-desktop-migration/tasks.md) - Implementation plan
- [PROJECT_SETUP.md](./PROJECT_SETUP.md) - Xcode setup

## Common Questions

### Q: Why not reuse the Kotlin code?
**A**: Only ~250 lines of simple code. Native Swift is faster to implement and maintain than setting up Kotlin/Native FFI.

### Q: What about code duplication?
**A**: Mitigated by comprehensive tests, clear specs, and simple logic. The cost of duplication is less than the cost of FFI complexity.

### Q: How do we ensure consistency with Android?
**A**: Shared test cases, property-based tests, and clear specifications ensure both implementations behave identically.

### Q: What if the shared code grows?
**A**: We'll reconsider if it exceeds 1000 lines or becomes algorithmically complex. For now, it's simple I/O and parsing.

### Q: Can we change this decision later?
**A**: Yes! This is documented as an Architecture Decision Record (ADR) with clear criteria for when to reconsider.

## Getting Help

1. **Read the specs**: [INTERFACE_SPECIFICATIONS.md](./INTERFACE_SPECIFICATIONS.md) has all the details
2. **Check the design**: [Design Document](../.kiro/specs/native-desktop-migration/design.md) explains the architecture
3. **Review examples**: Look at the code examples in this guide
4. **Ask questions**: Document unclear requirements for future developers

## Next Steps

1. ✅ Read this guide
2. ⏭️ Read [INTERFACE_SPECIFICATIONS.md](./INTERFACE_SPECIFICATIONS.md)
3. ⏭️ Start implementing M3U parser (Task 4)
4. ⏭️ Implement Xtream client (Task 5)
5. ⏭️ Write comprehensive tests

---

**Remember**: Native Swift is the right choice for this project. Keep it simple, test thoroughly, and enjoy the excellent Swift development experience!

**Last Updated**: 2024-11-28
