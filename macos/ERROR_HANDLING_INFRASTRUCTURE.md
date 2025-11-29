# Error Handling Infrastructure

This document describes the error handling infrastructure implemented for the macOS IPTV Player application.

## Overview

The error handling infrastructure provides a comprehensive, centralized approach to managing errors across the application. It includes error categorization, logging, retry mechanisms, and user-friendly error presentation.

## Components

### 1. AppError.swift

Defines the main error types and categories used throughout the application.

**Error Categories:**
- `NetworkError`: Connection timeouts, no internet, server errors, client errors
- `ParsingError`: Invalid M3U format, JSON parsing errors, encoding issues
- `DatabaseError`: Connection failures, query failures, constraint violations, disk full
- `PlayerError`: Stream not found, unsupported formats, decoding errors, hardware acceleration failures

**Main Error Type:**
- `AppError`: Wraps all error categories with localized descriptions and recovery suggestions

### 2. Logger.swift

Provides logging infrastructure using Apple's `os.log` framework.

**Features:**
- Multiple log levels: debug, info, warning, error, critical
- Category-based logging (Network, Parsing, Database, Player, UI)
- Configurable minimum log level
- Shared logger instance (`AppLogger.shared`)

**Usage:**
```swift
AppLogger.network("Fetching data from server", level: .info)
AppLogger.error("Failed to parse response", category: "Parsing", error: error)
```

### 3. RetryMechanism.swift

Implements retry logic with exponential backoff for handling transient failures.

**Features:**
- Configurable retry attempts, delays, and backoff multipliers
- Smart retry logic that distinguishes between retryable and non-retryable errors
- Pre-configured profiles for network and database operations
- Automatic logging of retry attempts

**Usage:**
```swift
let result = try await RetryMechanism.withNetworkRetry {
    try await performNetworkRequest()
}
```

**Configurations:**
- `RetryConfiguration.default`: 3 attempts, 1s initial delay, 2x backoff
- `RetryConfiguration.network`: Optimized for network requests
- `RetryConfiguration.database`: Optimized for database operations (2 attempts, 0.5s delay)

### 4. ErrorPresenter.swift

Converts errors into user-friendly presentations with appropriate severity levels and recovery suggestions.

**Features:**
- Automatic error categorization
- Localized error messages (Chinese)
- Severity-based presentation (info, warning, error, critical)
- Recovery suggestions for common error scenarios

**Usage:**
```swift
let presentation = AppErrorPresenter.shared.present(error: error)
// Use presentation.title, presentation.message, presentation.severity
```

### 5. ErrorView.swift

SwiftUI views and modifiers for displaying errors to users.

**Components:**
- `ErrorView`: Full-screen error display with icon, message, and action buttons
- `ErrorAlert`: Alert modifier for displaying errors as system alerts
- `ErrorBanner`: Inline banner for non-critical errors

**Usage:**
```swift
// As an alert
.errorAlert(error: $viewModel.error, onRetry: { viewModel.retry() })

// As a banner
ErrorBanner(presentation: presentation, onDismiss: { })

// As a full view
ErrorView(presentation: presentation, onDismiss: { }, onRetry: { })
```

## Integration with Existing Code

The error handling infrastructure integrates with existing services:

### M3UParser
- Already defines `M3UParserError` enum
- Errors are automatically handled by `ErrorPresenter`

### XtreamClient
- Already defines `XtreamClientError` enum
- Already implements retry logic (can be migrated to `RetryMechanism`)
- Errors are automatically handled by `ErrorPresenter`

## Error Handling Flow

1. **Error Occurs**: Service throws a specific error (e.g., `NetworkError.connectionTimeout`)
2. **Logging**: Error is logged with appropriate level and category
3. **Retry (if applicable)**: `RetryMechanism` attempts to retry the operation
4. **Presentation**: `ErrorPresenter` converts error to user-friendly format
5. **Display**: `ErrorView` or `ErrorAlert` shows error to user
6. **Recovery**: User can retry or dismiss based on error type

## Best Practices

### When to Use Each Error Type

**NetworkError:**
- HTTP request failures
- Connection timeouts
- Server errors (5xx)
- Client errors (4xx)

**ParsingError:**
- M3U parsing failures
- JSON decoding errors
- Invalid data formats

**DatabaseError:**
- Core Data save/fetch failures
- Database connection issues
- Constraint violations

**PlayerError:**
- Video playback failures
- Stream not available
- Codec/format issues

### Logging Guidelines

- Use `debug` for detailed diagnostic information
- Use `info` for normal operational messages
- Use `warning` for recoverable issues
- Use `error` for failures that need attention
- Use `critical` for severe failures that may require user action

### Retry Guidelines

- Use retry for transient network failures
- Don't retry authentication failures
- Don't retry validation errors
- Use exponential backoff to avoid overwhelming servers
- Log retry attempts for debugging

## Testing

A comprehensive test suite is provided in `ErrorHandlingTests.swift` covering:
- Error type creation and properties
- Logger functionality
- Retry mechanism behavior
- Error presenter output
- Retry configuration logic

## Future Enhancements

Potential improvements:
1. Error analytics/reporting
2. User-configurable retry settings
3. Offline error queue
4. Error recovery workflows
5. Localization for multiple languages

## Requirements Validation

This implementation satisfies the following requirements from the design document:

- ✅ Define error types and categories (network, parsing, database, player)
- ✅ Implement error presentation logic
- ✅ Add logging infrastructure
- ✅ Implement retry mechanisms

All error handling requirements from the specification are met.
