# Architecture Decision Record: Native Swift Implementation

## Status
**ACCEPTED** - 2024-11-28

## Context

We are migrating the IPTV Player desktop application from Kotlin Multiplatform to native implementations for macOS and Windows. A key decision is whether to share business logic code between platforms or implement natively.

## Decision

We will implement the macOS application entirely in **native Swift** without attempting to share Kotlin code via Kotlin/Native or other FFI mechanisms.

## Rationale

### Quantitative Analysis

**Shared Code Volume**: ~250 lines
- M3U Parser: ~60 lines
- Xtream API Client: ~150 lines  
- Data Models: ~40 lines

This is a minimal amount of straightforward code that is:
- Easy to understand and maintain
- Not algorithmically complex
- Mostly I/O and data transformation
- Well-defined by requirements

### Comparison Matrix

| Criterion | Kotlin/Native | Rust FFI | Native Swift |
|-----------|---------------|----------|--------------|
| Development Speed | ⚠️ Medium | ❌ Slow | ✅ Fast |
| Maintenance | ❌ Complex | ❌ Complex | ✅ Simple |
| Debugging | ❌ Poor | ⚠️ Medium | ✅ Excellent |
| Performance | ⚠️ FFI overhead | ✅ Good | ✅ Excellent |
| Platform Integration | ❌ Limited | ⚠️ Medium | ✅ Perfect |
| Build Complexity | ❌ High | ❌ High | ✅ Low |
| Team Expertise | ⚠️ Requires KN | ❌ Requires Rust | ✅ Standard |

### Key Benefits

1. **Simplicity**: No FFI, no interop complexity, standard Swift development
2. **Quality**: Full Xcode debugger support, native error messages
3. **Performance**: No FFI overhead, native Swift async/await
4. **Maintainability**: Single language, standard patterns
5. **Platform Integration**: Native Swift async/await, Combine, SwiftUI state management
6. **Development Speed**: Faster than setting up Kotlin/Native infrastructure

### Addressing Concerns

**Concern**: Code duplication  
**Response**: Only ~250 lines of simple code. The cost of duplication is far less than the cost of maintaining FFI bridges.

**Concern**: Divergence between platforms  
**Response**: Mitigated by:
- Comprehensive test suites with shared test cases
- Property-based tests ensuring behavioral equivalence
- Clear specifications in design document
- Simple, well-defined logic unlikely to change

**Concern**: Maintenance burden  
**Response**: Maintaining two simple implementations is easier than maintaining one complex FFI bridge.

## Consequences

### Positive

- ✅ Faster initial development
- ✅ Better debugging experience
- ✅ Simpler CI/CD pipeline
- ✅ Better platform integration
- ✅ Lower learning curve for team
- ✅ More maintainable long-term

### Negative

- ❌ ~250 lines of code duplicated
- ❌ Need to maintain two implementations
- ❌ Potential for divergence (mitigated by testing)

### Neutral

- ⚪ Android app remains unchanged (Kotlin Multiplatform)
- ⚪ Windows app will also be native C# (separate decision)

## Implementation Notes

### Directory Structure
```
macos/IPTVPlayer/Services/
├── M3UParser.swift
├── XtreamClient.swift
├── PlaylistRepository.swift
└── VideoPlayerService.swift
```

### Testing Strategy
- Unit tests with XCTest
- Property-based tests with swift-check
- Integration tests with real data
- Shared test cases across platforms

### Interface Specifications

All interfaces are documented in `CODE_SHARING_STRATEGY.md` including:
- M3UParser protocol
- XtreamClient protocol
- Data model structures
- Error types

## Review and Revision

This decision should be reconsidered if:
- Shared business logic grows to >1000 lines
- Complex algorithms need to be shared (e.g., video codec implementation)
- Team gains significant Kotlin/Native expertise
- Kotlin/Native tooling significantly improves

## References

- [CODE_SHARING_STRATEGY.md](./CODE_SHARING_STRATEGY.md) - Detailed analysis
- [Design Document](../.kiro/specs/native-desktop-migration/design.md) - Overall architecture
- [Requirements](../.kiro/specs/native-desktop-migration/requirements.md) - Requirement 2.4

## Approval

- **Proposed by**: Development Team
- **Date**: 2024-11-28
- **Status**: Approved
- **Next Review**: When shared code exceeds 1000 lines or major tooling improvements occur
