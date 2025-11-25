# Manual Test Checklist for Task 11

This checklist provides step-by-step instructions for manually testing all the fixes implemented in tasks 1-10.

## Prerequisites

- Desktop environment with or without VLC installed
- Android device or emulator (optional)
- Test M3U playlist URL (e.g., a public IPTV test stream)

---

## Test 1: Desktop频道切换不崩溃

### Setup
1. Build and run the Desktop application:
   ```bash
   ./gradlew :composeApp:run
   ```

### Test Steps
1. **Add a playlist** with multiple channels
2. **Play a channel** and verify video starts
3. **Quickly switch to another channel** (within 1-2 seconds)
4. **Repeat step 3** multiple times (at least 5 times)
5. **Close the player** while video is playing

### Expected Results
- ✅ No SIGSEGV crashes
- ✅ Smooth channel transitions
- ✅ Console shows: "✓ VLC player released successfully - no memory leaks"
- ✅ No error dialogs appear

### Console Output to Look For
```
Starting safe release of VLC player...
✓ Playback stopped
✓ Event listener removed successfully
✓ Verified: All event listeners removed
✓ Media player component released
✓ VLC player released successfully - no memory leaks
```

---

## Test 2: VLC未安装时的错误提示

### Setup (Option A - VLC Not Installed)
1. Temporarily rename or move VLC installation directory
2. Build and run the Desktop application

### Setup (Option B - Simulate Missing VLC)
1. Modify `VlcAvailabilityChecker.kt` to return `false` temporarily
2. Build and run the Desktop application

### Test Steps
1. **Launch the application**
2. **Navigate to a channel** and try to play

### Expected Results
- ✅ Error message displayed: "⚠️ VLC Media Player 未安装"
- ✅ Installation instructions shown for your OS
- ✅ System information displayed (OS, version, architecture, Java version)
- ✅ Note about restarting after installation

### UI Elements to Verify
- Error title in red color
- Clear description of the problem
- Platform-specific installation instructions:
  - **macOS**: Homebrew and manual download instructions
  - **Windows**: Installer and Chocolatey instructions
  - **Linux**: apt/dnf/pacman commands
- System information section
- Restart reminder in orange/yellow color

---

## Test 3: Android数据库正常工作

### Setup
1. Build and install the Android app:
   ```bash
   ./gradlew :composeApp:installDebug
   ```
2. Launch the app on Android device/emulator

### Test Steps
1. **Add a playlist** (M3U URL or Xtream account)
2. **Close the app** completely
3. **Reopen the app**
4. **Verify playlist is still there**
5. **Add to favorites** and verify persistence
6. **Close and reopen** again

### Expected Results
- ✅ No crashes on app startup
- ✅ Playlists persist across app restarts
- ✅ Favorites persist across app restarts
- ✅ No "Context not provided" errors in logcat

### Logcat Output to Look For
```
[PlaylistRepository] INFO: Saving playlist to database: id='...'
[PlaylistRepository] INFO: Successfully added M3U URL playlist: name='...', channels=...
```

---

## Test 4: 网络请求重试机制

### Setup
1. Prepare a test scenario with poor network or use a slow/unreliable URL
2. Build and run the application

### Test Steps

#### Test 4A: Temporary Network Failure
1. **Disconnect network** temporarily
2. **Try to add M3U URL playlist**
3. **Reconnect network** after 2-3 seconds
4. **Observe retry behavior**

#### Test 4B: Slow Server Response
1. **Use a slow-responding M3U URL** (if available)
2. **Add the playlist**
3. **Observe retry attempts**

#### Test 4C: Invalid URL
1. **Use an invalid or non-existent URL**
2. **Try to add playlist**
3. **Verify error message after retries**

### Expected Results
- ✅ Console shows retry attempts: "Attempt 1 of 3", "Attempt 2 of 3", etc.
- ✅ Delays between retries: 1s, 2s, 4s (exponential backoff)
- ✅ Success on retry if network recovers
- ✅ Clear error message after all retries fail
- ✅ Specific error messages for different failure types:
  - Timeout: "Request timeout: The server took too long to respond..."
  - Connection: "Connection timeout: Unable to connect to the server..."
  - Socket: "Socket timeout: The connection was interrupted..."

### Console Output to Look For
```
[PlaylistRepository] INFO: Attempt 1 of 3
[PlaylistRepository] ERROR: Attempt 1 failed, retrying in 1000ms
[PlaylistRepository] INFO: Attempt 2 of 3
[PlaylistRepository] ERROR: Attempt 2 failed, retrying in 2000ms
[PlaylistRepository] INFO: Attempt 3 of 3
[PlaylistRepository] ERROR: All 3 attempts failed
```

---

## Test 5: 内存泄漏验证

### Setup
1. Build and run Desktop application
2. Open system monitor or Activity Monitor to watch memory usage

### Test Steps
1. **Play a channel** for 30 seconds
2. **Switch to another channel**
3. **Repeat steps 1-2** at least 10 times
4. **Monitor memory usage** throughout

### Expected Results
- ✅ Memory usage remains stable (no continuous growth)
- ✅ Console shows listener removal: "✓ Event listener removed successfully"
- ✅ Console shows verification: "✓ Verified: All event listeners removed"
- ✅ No "Warning: Listener registration flag still true" messages

### Console Output to Look For (Each Channel Switch)
```
DisposableEffect onDispose called - cleaning up resources
Starting safe release of VLC player...
✓ Playback stopped
✓ Event listener removed successfully
✓ Verified: All event listeners removed
✓ Media player component released
✓ VLC player released successfully - no memory leaks
```

### Memory Baseline
- Initial memory: ~XXX MB
- After 10 channel switches: Should be within +50MB of initial
- No continuous upward trend

---

## Test 6: 播放器状态验证

### Setup
1. Build and run Desktop application

### Test Steps

#### Test 6A: Operations After Release
1. **Play a channel**
2. **Close the player** (trigger release)
3. **Try to use player controls** (play/pause/seek)

#### Test 6B: Operations During Release
1. **Play a channel**
2. **Quickly switch channels** and try to pause during transition

#### Test 6C: Operations Before Initialization
1. **Start the app**
2. **Try to use player controls** before playing any channel

### Expected Results
- ✅ No crashes when operating on released player
- ✅ Clear error messages in console:
  - "✗ 无法执行 play: 播放器已释放"
  - "✗ 无法执行 pause: 播放器正在释放中"
  - "✗ 无法执行 seekTo: 播放器未初始化"
- ✅ Player state shows ERROR with appropriate message
- ✅ Successful operations show: "✓ Player state verified for operation: play"

### Console Output to Look For

**Invalid State**:
```
✗ 无法执行 play: 播放器已释放
```

**Valid State**:
```
✓ Player state verified for operation: play
✓ Play command executed successfully
```

---

## Test 7: 错误处理和日志记录

### Setup
1. Build and run the application with console visible

### Test Steps
1. **Add invalid M3U URL** (404 error)
2. **Add invalid Xtream credentials**
3. **Try to play invalid stream URL**
4. **Trigger network timeout** (disconnect during download)

### Expected Results
- ✅ Detailed error logs for each failure
- ✅ Stack traces in console for debugging
- ✅ User-friendly error messages in UI
- ✅ No silent failures

### Console Output to Look For
```
[PlaylistRepository] ERROR: Failed to add M3U URL playlist: name='...', url='...'
[PlaylistRepository] ERROR: Connection timeout: Unable to connect to the server...
[PlaylistRepository] ERROR: Stack trace: ...
```

---

## Test 8: Xtream API重试机制

### Setup
1. Have Xtream account credentials ready (or use test credentials)

### Test Steps
1. **Add Xtream account** with correct credentials
2. **Observe authentication retry** (if network is slow)
3. **Observe category fetching retry**
4. **Observe channel fetching retry**

### Expected Results
- ✅ Each API call has retry logic
- ✅ Console shows retry attempts for each operation
- ✅ Success after retry if network recovers
- ✅ All operations complete successfully

### Console Output to Look For
```
[PlaylistRepository] INFO: Authenticating Xtream account
[PlaylistRepository] INFO: Attempt 1 of 3
[PlaylistRepository] INFO: Successfully authenticated Xtream account
[PlaylistRepository] INFO: Fetching live categories from Xtream server
[PlaylistRepository] INFO: Attempt 1 of 3
[PlaylistRepository] INFO: Successfully fetched X categories
[PlaylistRepository] INFO: Fetching live streams from Xtream server
[PlaylistRepository] INFO: Attempt 1 of 3
[PlaylistRepository] INFO: Successfully fetched X channels
```

---

## Test 9: URL切换逻辑

### Setup
1. Build and run Desktop application

### Test Steps
1. **Play channel A**
2. **Wait for playback to start** (2-3 seconds)
3. **Switch to channel B** immediately
4. **Verify smooth transition**
5. **Check console for proper sequence**

### Expected Results
- ✅ Current playback stops before loading new URL
- ✅ 200ms delay between stop and load
- ✅ Buffering state shown during load
- ✅ No errors during transition
- ✅ Console shows proper sequence

### Console Output to Look For
```
Loading new URL: http://...
Current playback stopped
Media loaded successfully: http://...
```

---

## Test 10: 跨平台数据库驱动

### Setup
1. Test on both Desktop and Android platforms

### Test Steps

#### Desktop
1. **Run Desktop app**
2. **Add playlists**
3. **Check database file** at `~/.iptv/iptv.db`
4. **Verify data persists**

#### Android
1. **Run Android app**
2. **Add playlists**
3. **Check logcat** for database operations
4. **Verify data persists**

### Expected Results
- ✅ Desktop: Database created in user home directory
- ✅ Android: Database created with app context
- ✅ Both: No initialization errors
- ✅ Both: Data persists across restarts

---

## Summary Checklist

Mark each test as you complete it:

- [ ] Test 1: Desktop频道切换不崩溃
- [ ] Test 2: VLC未安装时的错误提示
- [ ] Test 3: Android数据库正常工作
- [ ] Test 4: 网络请求重试机制
- [ ] Test 5: 内存泄漏验证
- [ ] Test 6: 播放器状态验证
- [ ] Test 7: 错误处理和日志记录
- [ ] Test 8: Xtream API重试机制
- [ ] Test 9: URL切换逻辑
- [ ] Test 10: 跨平台数据库驱动

---

## Reporting Issues

If any test fails, document:
1. **Test number and name**
2. **Steps to reproduce**
3. **Expected behavior**
4. **Actual behavior**
5. **Console output**
6. **Screenshots** (if applicable)

---

## Notes

- All tests should be performed on a clean build
- Console output is crucial for verification
- Memory monitoring requires system tools (Activity Monitor, Task Manager, etc.)
- Network tests may require network manipulation tools or test URLs
- Some tests may require multiple runs to observe behavior under different conditions

**Test Date**: _____________  
**Tester**: _____________  
**Platform**: _____________  
**Results**: _____________
