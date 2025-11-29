# Manual Testing Guide - macOS IPTV Player

**Date:** November 29, 2025  
**Version:** 1.0  
**Tester:** _____________  
**Build:** _____________

## Overview

This document provides comprehensive manual testing procedures for the macOS IPTV Player application. Follow each test scenario carefully and document any issues found.

## Prerequisites

Before starting testing:
- [ ] Application builds successfully
- [ ] Test M3U playlist URLs are available
- [ ] Test Xtream Codes credentials are available (if applicable)
- [ ] macOS system meets minimum requirements
- [ ] Network connection is stable

## Test Environment

- **macOS Version:** _____________
- **Hardware:** _____________
- **Network:** _____________

---

## Test Scenario 1: Add M3U Playlist → Browse Channels → Play Video

### Objective
Verify the complete user flow for adding an M3U playlist, browsing channels, and playing video content.

### Test Steps

#### 1.1 Launch Application
- [ ] **Step:** Launch IPTVPlayer.app
- [ ] **Expected:** Application launches without errors
- [ ] **Expected:** Main window appears with empty state message
- [ ] **Expected:** Sidebar shows "No playlists" or empty list
- [ ] **Result:** ☐ Pass ☐ Fail
- **Notes:** _____________________________________________

#### 1.2 Add M3U Playlist via URL
- [ ] **Step:** Click "Add Playlist" button or use ⌘N shortcut
- [ ] **Expected:** Add playlist dialog appears with options
- [ ] **Step:** Select "M3U URL" option
- [ ] **Expected:** URL input dialog appears
- [ ] **Step:** Enter a valid M3U playlist URL (e.g., `http://example.com/playlist.m3u`)
- [ ] **Expected:** URL field accepts input
- [ ] **Step:** Click "Add" or press Enter
- [ ] **Expected:** Loading indicator appears
- [ ] **Expected:** Playlist is downloaded and parsed
- [ ] **Expected:** New playlist appears in sidebar with extracted name
- [ ] **Expected:** Loading indicator disappears
- [ ] **Result:** ☐ Pass ☐ Fail
- **Notes:** _____________________________________________

#### 1.3 Browse Channels
- [ ] **Step:** Click on the newly added playlist in sidebar
- [ ] **Expected:** Playlist is selected (highlighted)
- [ ] **Expected:** Channel list appears in detail view
- [ ] **Expected:** Channels display with name, category, and thumbnail (if available)
- [ ] **Expected:** Channel count is accurate
- [ ] **Step:** Scroll through channel list
- [ ] **Expected:** Smooth scrolling performance
- [ ] **Expected:** All channels are visible
- [ ] **Result:** ☐ Pass ☐ Fail
- **Notes:** _____________________________________________

#### 1.4 Search Channels
- [ ] **Step:** Click in search field or use ⌘F
- [ ] **Expected:** Search field is focused
- [ ] **Step:** Type a channel name (partial or full)
- [ ] **Expected:** Channel list filters in real-time (with ~300ms debounce)
- [ ] **Expected:** Only matching channels are displayed
- [ ] **Step:** Clear search field
- [ ] **Expected:** All channels reappear
- [ ] **Result:** ☐ Pass ☐ Fail
- **Notes:** _____________________________________________

#### 1.5 Filter by Category
- [ ] **Step:** Select a category from category filter (if available)
- [ ] **Expected:** Channel list filters to show only channels in that category
- [ ] **Step:** Clear category filter
- [ ] **Expected:** All channels reappear
- [ ] **Result:** ☐ Pass ☐ Fail
- **Notes:** _____________________________________________

#### 1.6 Play Video
- [ ] **Step:** Click on a channel to select it
- [ ] **Expected:** Channel is highlighted
- [ ] **Expected:** Player view opens in a sheet/window
- [ ] **Expected:** Video starts loading
- [ ] **Expected:** Loading indicator appears
- [ ] **Expected:** Video begins playback automatically
- [ ] **Expected:** Player controls are visible
- [ ] **Expected:** Channel information is displayed
- [ ] **Result:** ☐ Pass ☐ Fail
- **Notes:** _____________________________________________

#### 1.7 Player Controls
- [ ] **Step:** Click pause button
- [ ] **Expected:** Video pauses
- [ ] **Expected:** Button changes to play icon
- [ ] **Step:** Click play button
- [ ] **Expected:** Video resumes
- [ ] **Step:** Adjust volume slider
- [ ] **Expected:** Audio volume changes accordingly
- [ ] **Step:** Click fullscreen button
- [ ] **Expected:** Video enters fullscreen mode
- [ ] **Step:** Press Escape or click exit fullscreen
- [ ] **Expected:** Video exits fullscreen mode
- [ ] **Result:** ☐ Pass ☐ Fail
- **Notes:** _____________________________________________

#### 1.8 Close Player
- [ ] **Step:** Close player window/sheet
- [ ] **Expected:** Player closes
- [ ] **Expected:** Video stops playing
- [ ] **Expected:** Returns to channel list view
- [ ] **Result:** ☐ Pass ☐ Fail
- **Notes:** _____________________________________________

### Test Scenario 1 Summary
- **Overall Result:** ☐ Pass ☐ Fail
- **Issues Found:** _____________________________________________
- **Severity:** ☐ Critical ☐ Major ☐ Minor ☐ Cosmetic

---

## Test Scenario 2: Add Xtream Account → Browse Channels → Play Video

### Objective
Verify the complete user flow for adding an Xtream Codes account, browsing channels, and playing video content.

### Test Steps

#### 2.1 Add Xtream Codes Account
- [ ] **Step:** Click "Add Playlist" button or use ⌘⇧X shortcut
- [ ] **Expected:** Add playlist dialog appears
- [ ] **Step:** Select "Xtream Codes" option
- [ ] **Expected:** Xtream credentials dialog appears
- [ ] **Step:** Enter server URL (e.g., `https://example.com:8080`)
- [ ] **Step:** Enter username
- [ ] **Step:** Enter password
- [ ] **Expected:** All fields accept input
- [ ] **Step:** Click "Add" or press Enter
- [ ] **Expected:** Loading indicator appears
- [ ] **Expected:** Authentication request is sent
- [ ] **Expected:** Channels and categories are fetched
- [ ] **Expected:** New playlist appears in sidebar with "Xtream - [username]" name
- [ ] **Expected:** Loading indicator disappears
- [ ] **Result:** ☐ Pass ☐ Fail
- **Notes:** _____________________________________________

#### 2.2 Browse Xtream Channels
- [ ] **Step:** Click on the Xtream playlist in sidebar
- [ ] **Expected:** Playlist is selected
- [ ] **Expected:** Channel list appears with all live streams
- [ ] **Expected:** Channels display with name, category, and logo
- [ ] **Expected:** Categories are available for filtering
- [ ] **Result:** ☐ Pass ☐ Fail
- **Notes:** _____________________________________________

#### 2.3 Filter by Category
- [ ] **Step:** Select a category from the category list
- [ ] **Expected:** Channel list filters to show only channels in that category
- [ ] **Expected:** Channel count updates
- [ ] **Step:** Select "All Categories" or clear filter
- [ ] **Expected:** All channels reappear
- [ ] **Result:** ☐ Pass ☐ Fail
- **Notes:** _____________________________________________

#### 2.4 Play Xtream Channel
- [ ] **Step:** Click on a channel to play
- [ ] **Expected:** Player view opens
- [ ] **Expected:** Video stream loads and plays
- [ ] **Expected:** Player controls work correctly
- [ ] **Expected:** Channel information is displayed
- [ ] **Result:** ☐ Pass ☐ Fail
- **Notes:** _____________________________________________

#### 2.5 Verify Credentials Storage
- [ ] **Step:** Close and reopen the application
- [ ] **Expected:** Xtream playlist is still available
- [ ] **Expected:** Can browse channels without re-entering credentials
- [ ] **Expected:** Credentials are securely stored in Keychain
- [ ] **Result:** ☐ Pass ☐ Fail
- **Notes:** _____________________________________________

### Test Scenario 2 Summary
- **Overall Result:** ☐ Pass ☐ Fail
- **Issues Found:** _____________________________________________
- **Severity:** ☐ Critical ☐ Major ☐ Minor ☐ Cosmetic

---

## Test Scenario 3: Favorite Management Flow

### Objective
Verify the complete favorite management functionality.

### Test Steps

#### 3.1 Add Channel to Favorites
- [ ] **Step:** Select a playlist and browse channels
- [ ] **Step:** Click the favorite/star icon on a channel
- [ ] **Expected:** Icon changes to filled/active state
- [ ] **Expected:** Channel is added to favorites
- [ ] **Expected:** No error messages appear
- [ ] **Result:** ☐ Pass ☐ Fail
- **Notes:** _____________________________________________

#### 3.2 View Favorites
- [ ] **Step:** Click on "Favorites" section in sidebar
- [ ] **Expected:** Favorites section expands or navigates to favorites view
- [ ] **Expected:** All favorited channels are displayed
- [ ] **Expected:** Channels show correct information
- [ ] **Result:** ☐ Pass ☐ Fail
- **Notes:** _____________________________________________

#### 3.3 Play Favorite Channel
- [ ] **Step:** Click on a favorite channel
- [ ] **Expected:** Player opens and video plays
- [ ] **Expected:** Playback works correctly
- [ ] **Result:** ☐ Pass ☐ Fail
- **Notes:** _____________________________________________

#### 3.4 Remove from Favorites
- [ ] **Step:** Click the favorite icon on a favorited channel
- [ ] **Expected:** Icon changes to unfilled/inactive state
- [ ] **Expected:** Channel is removed from favorites
- [ ] **Step:** Navigate to Favorites section
- [ ] **Expected:** Removed channel no longer appears in favorites
- [ ] **Result:** ☐ Pass ☐ Fail
- **Notes:** _____________________________________________

#### 3.5 Favorites Persistence
- [ ] **Step:** Add multiple channels to favorites
- [ ] **Step:** Close and reopen the application
- [ ] **Expected:** All favorites are preserved
- [ ] **Expected:** Favorite status is correctly displayed
- [ ] **Result:** ☐ Pass ☐ Fail
- **Notes:** _____________________________________________

### Test Scenario 3 Summary
- **Overall Result:** ☐ Pass ☐ Fail
- **Issues Found:** _____________________________________________
- **Severity:** ☐ Critical ☐ Major ☐ Minor ☐ Cosmetic

---

## Test Scenario 4: Error Scenarios

### Objective
Verify proper error handling and user feedback for various error conditions.

### Test Steps

#### 4.1 Invalid M3U URL
- [ ] **Step:** Try to add M3U playlist with invalid URL (e.g., "not-a-url")
- [ ] **Expected:** Validation error appears
- [ ] **Expected:** Error message is clear and helpful
- [ ] **Expected:** Playlist is not added
- [ ] **Expected:** Application remains stable
- [ ] **Result:** ☐ Pass ☐ Fail
- **Notes:** _____________________________________________

#### 4.2 Unreachable M3U URL
- [ ] **Step:** Try to add M3U playlist with unreachable URL (e.g., "http://nonexistent.example.com/playlist.m3u")
- [ ] **Expected:** Network error appears after timeout
- [ ] **Expected:** Error message explains the issue
- [ ] **Expected:** Suggests checking network connection
- [ ] **Expected:** Application remains stable
- [ ] **Result:** ☐ Pass ☐ Fail
- **Notes:** _____________________________________________

#### 4.3 Malformed M3U Content
- [ ] **Step:** Add M3U playlist with malformed content
- [ ] **Expected:** Parser handles errors gracefully
- [ ] **Expected:** Valid channels are extracted
- [ ] **Expected:** Error message indicates parsing issues
- [ ] **Expected:** Application remains stable
- [ ] **Result:** ☐ Pass ☐ Fail
- **Notes:** _____________________________________________

#### 4.4 Invalid Xtream Credentials
- [ ] **Step:** Try to add Xtream account with incorrect credentials
- [ ] **Expected:** Authentication fails
- [ ] **Expected:** Clear error message appears
- [ ] **Expected:** Suggests checking credentials
- [ ] **Expected:** Account is not added
- [ ] **Expected:** Application remains stable
- [ ] **Result:** ☐ Pass ☐ Fail
- **Notes:** _____________________________________________

#### 4.5 Unreachable Xtream Server
- [ ] **Step:** Try to add Xtream account with unreachable server
- [ ] **Expected:** Network error appears
- [ ] **Expected:** Error message is informative
- [ ] **Expected:** Application remains stable
- [ ] **Result:** ☐ Pass ☐ Fail
- **Notes:** _____________________________________________

#### 4.6 Invalid Video Stream
- [ ] **Step:** Try to play a channel with invalid/dead stream URL
- [ ] **Expected:** Player shows error message
- [ ] **Expected:** Error indicates stream is unavailable
- [ ] **Expected:** Suggests trying another channel
- [ ] **Expected:** Application remains stable
- [ ] **Expected:** Can close player and continue using app
- [ ] **Result:** ☐ Pass ☐ Fail
- **Notes:** _____________________________________________

#### 4.7 Network Interruption During Playback
- [ ] **Step:** Start playing a channel
- [ ] **Step:** Disable network connection
- [ ] **Expected:** Player detects network loss
- [ ] **Expected:** Error message or buffering indicator appears
- [ ] **Step:** Re-enable network connection
- [ ] **Expected:** Player attempts to reconnect (if implemented)
- [ ] **Expected:** Playback resumes or clear error is shown
- [ ] **Result:** ☐ Pass ☐ Fail
- **Notes:** _____________________________________________

#### 4.8 Empty Playlist Name
- [ ] **Step:** Try to rename playlist to empty string
- [ ] **Expected:** Validation error appears
- [ ] **Expected:** Rename is rejected
- [ ] **Expected:** Original name is preserved
- [ ] **Result:** ☐ Pass ☐ Fail
- **Notes:** _____________________________________________

#### 4.9 Duplicate Playlist
- [ ] **Step:** Try to add the same M3U URL twice
- [ ] **Expected:** Either allows duplicate or shows appropriate message
- [ ] **Expected:** Application handles gracefully
- [ ] **Result:** ☐ Pass ☐ Fail
- **Notes:** _____________________________________________

#### 4.10 Database Errors
- [ ] **Step:** (If possible) Simulate database error
- [ ] **Expected:** Error is caught and handled
- [ ] **Expected:** User-friendly error message appears
- [ ] **Expected:** Application remains stable
- [ ] **Result:** ☐ Pass ☐ Fail
- **Notes:** _____________________________________________

### Test Scenario 4 Summary
- **Overall Result:** ☐ Pass ☐ Fail
- **Issues Found:** _____________________________________________
- **Severity:** ☐ Critical ☐ Major ☐ Minor ☐ Cosmetic

---

## Additional Test Cases

### Playlist Management

#### Delete Playlist
- [ ] **Step:** Right-click on playlist and select "Delete"
- [ ] **Expected:** Confirmation dialog appears
- [ ] **Step:** Confirm deletion
- [ ] **Expected:** Playlist is removed from sidebar
- [ ] **Expected:** Associated data is cleaned up
- [ ] **Expected:** If Xtream playlist, credentials are removed from Keychain
- [ ] **Result:** ☐ Pass ☐ Fail
- **Notes:** _____________________________________________

#### Rename Playlist
- [ ] **Step:** Right-click on playlist and select "Rename"
- [ ] **Expected:** Rename dialog appears with current name
- [ ] **Step:** Enter new name
- [ ] **Step:** Confirm
- [ ] **Expected:** Playlist name updates in sidebar
- [ ] **Expected:** Name persists after app restart
- [ ] **Result:** ☐ Pass ☐ Fail
- **Notes:** _____________________________________________

#### Duplicate Playlist
- [ ] **Step:** Right-click on playlist and select "Duplicate"
- [ ] **Expected:** New playlist is created with "(Copy)" suffix
- [ ] **Expected:** All channels are duplicated
- [ ] **Expected:** Both playlists are independent
- [ ] **Result:** ☐ Pass ☐ Fail
- **Notes:** _____________________________________________

### UI/UX Testing

#### Keyboard Shortcuts
- [ ] **⌘N:** Opens add M3U URL dialog
- [ ] **⌘O:** Opens file picker for M3U file
- [ ] **⌘⇧X:** Opens Xtream Codes dialog
- [ ] **⌘F:** Focuses search field
- [ ] **⌘⇧S:** Toggles sidebar
- [ ] **Result:** ☐ Pass ☐ Fail
- **Notes:** _____________________________________________

#### Window Resizing
- [ ] **Step:** Resize main window to minimum size
- [ ] **Expected:** UI adapts gracefully
- [ ] **Expected:** No content is cut off
- [ ] **Step:** Resize to maximum size
- [ ] **Expected:** UI scales appropriately
- [ ] **Result:** ☐ Pass ☐ Fail
- **Notes:** _____________________________________________

#### Sidebar Toggle
- [ ] **Step:** Click sidebar toggle button or use ⌘⇧S
- [ ] **Expected:** Sidebar hides with animation
- [ ] **Step:** Toggle again
- [ ] **Expected:** Sidebar reappears with animation
- [ ] **Result:** ☐ Pass ☐ Fail
- **Notes:** _____________________________________________

#### Dark Mode
- [ ] **Step:** Switch system to Dark Mode
- [ ] **Expected:** Application adapts to dark theme
- [ ] **Expected:** All UI elements are readable
- [ ] **Expected:** Colors are appropriate
- [ ] **Step:** Switch back to Light Mode
- [ ] **Expected:** Application adapts to light theme
- [ ] **Result:** ☐ Pass ☐ Fail
- **Notes:** _____________________________________________

### Performance Testing

#### Large Playlist
- [ ] **Step:** Add playlist with 500+ channels
- [ ] **Expected:** Parsing completes in reasonable time
- [ ] **Expected:** Channel list scrolls smoothly
- [ ] **Expected:** Search/filter remains responsive
- [ ] **Expected:** Memory usage is reasonable
- [ ] **Result:** ☐ Pass ☐ Fail
- **Notes:** _____________________________________________

#### Multiple Playlists
- [ ] **Step:** Add 5+ playlists
- [ ] **Expected:** All playlists load correctly
- [ ] **Expected:** Switching between playlists is smooth
- [ ] **Expected:** No performance degradation
- [ ] **Result:** ☐ Pass ☐ Fail
- **Notes:** _____________________________________________

#### Image Loading
- [ ] **Step:** Browse channels with many thumbnails
- [ ] **Expected:** Images load progressively
- [ ] **Expected:** Placeholder shown while loading
- [ ] **Expected:** Failed images show default icon
- [ ] **Expected:** Image caching works (faster on second view)
- [ ] **Result:** ☐ Pass ☐ Fail
- **Notes:** _____________________________________________

### Security Testing

#### HTTPS Enforcement
- [ ] **Step:** Try to add Xtream account with HTTP URL (not HTTPS)
- [ ] **Expected:** Validation error appears
- [ ] **Expected:** Requires HTTPS for security
- [ ] **Result:** ☐ Pass ☐ Fail
- **Notes:** _____________________________________________

#### Input Sanitization
- [ ] **Step:** Try to enter special characters in playlist name
- [ ] **Expected:** Input is sanitized
- [ ] **Expected:** No injection vulnerabilities
- [ ] **Result:** ☐ Pass ☐ Fail
- **Notes:** _____________________________________________

#### Keychain Integration
- [ ] **Step:** Add Xtream account
- [ ] **Step:** Open Keychain Access app
- [ ] **Expected:** Credentials are stored in Keychain
- [ ] **Expected:** Credentials are encrypted
- [ ] **Step:** Delete playlist
- [ ] **Expected:** Credentials are removed from Keychain
- [ ] **Result:** ☐ Pass ☐ Fail
- **Notes:** _____________________________________________

---

## Issues Log

### Issue #1
- **Severity:** ☐ Critical ☐ Major ☐ Minor ☐ Cosmetic
- **Test Scenario:** _____________________________________________
- **Description:** _____________________________________________
- **Steps to Reproduce:** _____________________________________________
- **Expected Behavior:** _____________________________________________
- **Actual Behavior:** _____________________________________________
- **Screenshots/Logs:** _____________________________________________

### Issue #2
- **Severity:** ☐ Critical ☐ Major ☐ Minor ☐ Cosmetic
- **Test Scenario:** _____________________________________________
- **Description:** _____________________________________________
- **Steps to Reproduce:** _____________________________________________
- **Expected Behavior:** _____________________________________________
- **Actual Behavior:** _____________________________________________
- **Screenshots/Logs:** _____________________________________________

### Issue #3
- **Severity:** ☐ Critical ☐ Major ☐ Minor ☐ Cosmetic
- **Test Scenario:** _____________________________________________
- **Description:** _____________________________________________
- **Steps to Reproduce:** _____________________________________________
- **Expected Behavior:** _____________________________________________
- **Actual Behavior:** _____________________________________________
- **Screenshots/Logs:** _____________________________________________

*(Add more issues as needed)*

---

## Test Summary

### Overall Statistics
- **Total Test Cases:** _____
- **Passed:** _____
- **Failed:** _____
- **Blocked:** _____
- **Pass Rate:** _____%

### Critical Issues
- **Count:** _____
- **List:** _____________________________________________

### Major Issues
- **Count:** _____
- **List:** _____________________________________________

### Minor Issues
- **Count:** _____
- **List:** _____________________________________________

### Recommendations
_____________________________________________
_____________________________________________
_____________________________________________

### Sign-off
- **Tester Name:** _____________________________________________
- **Date:** _____________________________________________
- **Signature:** _____________________________________________

---

## Notes for Testers

### Tips for Effective Testing
1. Test one scenario at a time
2. Document everything, even if it seems minor
3. Take screenshots of issues
4. Note the exact steps to reproduce issues
5. Test both happy paths and edge cases
6. Pay attention to performance and responsiveness
7. Test with different network conditions
8. Verify data persistence across app restarts

### Common Issues to Watch For
- Memory leaks during video playback
- UI freezing during network operations
- Incorrect error messages
- Data loss after app restart
- Broken keyboard shortcuts
- Poor performance with large playlists
- Inconsistent UI states
- Security vulnerabilities

### Test Data Sources
- **Free M3U Playlists:** Search for "free IPTV m3u" online
- **Test Xtream Servers:** Use demo/test credentials if available
- **Sample Channels:** Use well-known public streams for testing

---

## Appendix

### Build Information
- **Xcode Version:** _____________
- **Swift Version:** _____________
- **macOS SDK:** _____________
- **Build Configuration:** ☐ Debug ☐ Release

### Dependencies
- AVFoundation (Video Playback)
- Core Data (Data Persistence)
- SwiftUI (UI Framework)
- Combine (Reactive Programming)

### Known Limitations
- List any known limitations or features not yet implemented
- _____________________________________________
- _____________________________________________

