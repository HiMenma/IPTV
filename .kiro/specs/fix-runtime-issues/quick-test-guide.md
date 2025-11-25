# Quick Test Guide

## Fast Track Testing for All Fixes

This guide provides the fastest way to verify all fixes are working correctly.

---

## Prerequisites

```bash
# Ensure you have the latest build
./gradlew clean build
```

---

## 5-Minute Desktop Test

### 1. Test VLC Availability (30 seconds)

```bash
# Run the desktop app
./gradlew :composeApp:run
```

**Expected**: 
- If VLC installed: App runs normally
- If VLC not installed: Error message with installation instructions

---

### 2. Test Channel Switching (2 minutes)

1. Add a test M3U playlist (use any public IPTV test URL)
2. Play a channel
3. Quickly switch between 5 different channels
4. Close the player

**Expected**: 
- No crashes
- Smooth transitions
- Console shows: "✓ VLC player released successfully - no memory leaks"

**Console Check**:
```
✓ Playback stopped
✓ Event listener removed successfully
✓ Verified: All event listeners removed
✓ Media player component released
```

---

### 3. Test Network Retry (2 minutes)

1. Disconnect your network
2. Try to add an M3U URL playlist
3. Reconnect network after 2 seconds
4. Observe retry behavior

**Expected**:
- Console shows: "Attempt 1 of 3", "Attempt 2 of 3", etc.
- Success after network reconnects
- Or clear error message after 3 attempts

**Console Check**:
```
[PlaylistRepository] INFO: Attempt 1 of 3
[PlaylistRepository] ERROR: Attempt 1 failed, retrying in 1000ms
[PlaylistRepository] INFO: Attempt 2 of 3
```

---

### 4. Test Player State Verification (30 seconds)

1. Play a channel
2. Close the player
3. Try to click play/pause buttons

**Expected**:
- No crashes
- Console shows: "✗ 无法执行 play: 播放器已释放"

---

## 5-Minute Android Test

### 1. Build and Install

```bash
./gradlew :composeApp:installDebug
```

---

### 2. Test Database Persistence (2 minutes)

1. Launch app
2. Add a playlist
3. Close app completely (swipe away from recent apps)
4. Reopen app

**Expected**:
- Playlist still there
- No crashes
- Logcat shows: "[PlaylistRepository] INFO: Successfully added..."

---

### 3. Test Favorites (1 minute)

1. Add a channel to favorites
2. Close and reopen app
3. Check favorites

**Expected**:
- Favorites persist
- No database errors

---

### 4. Check Logcat (1 minute)

```bash
adb logcat | grep -E "PlaylistRepository|Database"
```

**Expected**:
- No "Context not provided" errors
- Successful database operations logged

---

## Critical Console Output to Look For

### ✅ Success Indicators

```
✓ VLC player released successfully - no memory leaks
✓ Event listener removed successfully
✓ Verified: All event listeners removed
✓ Play command executed successfully
[PlaylistRepository] INFO: Successfully added M3U URL playlist
[Database] Database driver initialized successfully
```

### ❌ Failure Indicators (Should NOT See)

```
SIGSEGV
NullPointerException
Context not provided
Memory leak detected
Double-release error
```

---

## Quick Verification Checklist

Run through this in 10 minutes:

- [ ] Desktop app launches without VLC → Shows error message ✅
- [ ] Desktop app launches with VLC → Works normally ✅
- [ ] Switch 5 channels rapidly → No crashes ✅
- [ ] Close player → Clean resource release ✅
- [ ] Network retry → Retries 3 times ✅
- [ ] Android app → Database works ✅
- [ ] Android persistence → Data survives restart ✅
- [ ] Console logs → All success indicators present ✅

---

## If Something Fails

### Desktop Crashes on Channel Switch
**Check**: Console for SIGSEGV or release errors  
**Expected Fix**: Should see "✓ VLC player released successfully"

### Android Database Errors
**Check**: Logcat for "Context not provided"  
**Expected Fix**: Should see "[Database] Database driver initialized successfully"

### Network Requests Fail Immediately
**Check**: Console for retry attempts  
**Expected Fix**: Should see "Attempt 1 of 3", "Attempt 2 of 3"

### VLC Error Not Showing
**Check**: VLC is actually not installed  
**Expected Fix**: Should see error UI with installation instructions

---

## Build Commands Reference

```bash
# Clean build
./gradlew clean build

# Run desktop
./gradlew :composeApp:run

# Install Android debug
./gradlew :composeApp:installDebug

# View Android logs
adb logcat | grep -E "PlaylistRepository|Database|VideoPlayer"

# Check for crashes
adb logcat | grep -E "FATAL|SIGSEGV|Exception"
```

---

## Test URLs (Public IPTV Test Streams)

Use these for testing if you don't have your own:

```
# M3U Test URL (example - replace with actual test URL)
https://iptv-org.github.io/iptv/index.m3u

# Or create a simple test M3U file:
#EXTM3U
#EXTINF:-1,Test Channel 1
http://example.com/stream1.m3u8
#EXTINF:-1,Test Channel 2
http://example.com/stream2.m3u8
```

---

## Success Criteria

### All Tests Pass If:

1. ✅ No application crashes
2. ✅ All console logs show success indicators (✓)
3. ✅ No error indicators in logs (SIGSEGV, NullPointer, etc.)
4. ✅ Data persists across restarts
5. ✅ Network retries work as expected
6. ✅ VLC error message shows when VLC not installed
7. ✅ Memory usage remains stable

### Ready for Production If:

- All 7 success criteria met
- Manual testing completed
- No critical issues found

---

**Quick Test Duration**: ~10 minutes  
**Comprehensive Test Duration**: ~30 minutes  
**Full Manual Test Suite**: ~1-2 hours

Choose the appropriate level based on your needs!
