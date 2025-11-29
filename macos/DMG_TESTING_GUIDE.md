# DMG Creation and Testing Guide

This guide provides instructions for testing the DMG creation process locally before deploying to CI/CD.

## Overview

The DMG (Disk Image) is the standard distribution format for macOS applications. This guide covers:
1. Creating a DMG locally
2. Verifying the DMG structure
3. Testing the installation process
4. Troubleshooting common issues

## Prerequisites

### Required
- macOS 13.0 or later
- Xcode 15.0 or later
- Command Line Tools installed

### Optional (Recommended)
- `create-dmg` tool for better-looking DMGs
  ```bash
  brew install create-dmg
  ```

## Quick Start

### 1. Create DMG Locally

Run the automated script:

```bash
cd macos
./create-dmg-local.sh
```

This script will:
- ✓ Check prerequisites
- ✓ Clean previous builds
- ✓ Resolve Swift Package dependencies
- ✓ Build the app in Release configuration
- ✓ Create an archive
- ✓ Export the application
- ✓ Create a DMG file
- ✓ Verify the DMG can be mounted
- ✓ Simulate installation
- ✓ Calculate checksums

### 2. Expected Output

The script will create:
- `build/IPTVPlayer.xcarchive` - The Xcode archive
- `build/export/IPTVPlayer.app` - The exported application
- `build/IPTVPlayer-{version}.dmg` - The disk image
- `build/checksums.txt` - SHA-256 checksums

### 3. Test the DMG

After the script completes, test the DMG manually:

```bash
# Open the DMG
open build/IPTVPlayer-*.dmg
```

## Manual Testing Checklist

### Phase 1: DMG Structure Verification

- [ ] **Open DMG**: Double-click the DMG file
- [ ] **Verify Window**: DMG window opens with:
  - [ ] IPTVPlayer.app icon on the left
  - [ ] Applications folder shortcut on the right (if using create-dmg)
  - [ ] Clean, professional appearance
- [ ] **Check App Icon**: Application icon displays correctly
- [ ] **Verify Size**: DMG size is reasonable (typically 10-50 MB)

### Phase 2: Installation Testing

#### Test 1: Fresh Installation

1. **Mount the DMG**
   ```bash
   open build/IPTVPlayer-*.dmg
   ```

2. **Drag to Applications**
   - Drag IPTVPlayer.app to Applications folder
   - Wait for copy to complete
   - Verify no errors during copy

3. **Eject DMG**
   - Right-click DMG in Finder sidebar
   - Click "Eject"
   - Verify DMG unmounts cleanly

4. **Launch Application**
   - Open Applications folder
   - Find IPTVPlayer.app
   - Double-click to launch

5. **Handle Security Warnings** (for unsigned builds)
   - If you see "cannot be opened because it is from an unidentified developer":
     - Go to System Settings > Privacy & Security
     - Click "Open Anyway" next to the security warning
     - Confirm you want to open the app
   - Or use command line:
     ```bash
     xattr -cr /Applications/IPTVPlayer.app
     ```

6. **Verify Launch**
   - [ ] App launches without crashes
   - [ ] Main window appears
   - [ ] UI elements load correctly
   - [ ] No console errors (check Console.app)

#### Test 2: Upgrade Installation

1. **Keep existing installation** (if you have one)
2. **Mount new DMG**
3. **Drag to Applications** (overwrite existing)
4. **Verify replacement**
   - [ ] Old version is replaced
   - [ ] User data is preserved (if applicable)
   - [ ] App launches with new version

#### Test 3: Installation to Different Location

1. **Mount DMG**
2. **Drag to Desktop** (or other location)
3. **Launch from that location**
4. **Verify functionality**
   - [ ] App works from non-Applications location
   - [ ] All features function correctly

### Phase 3: DMG Integrity Verification

#### Verify Checksum

```bash
# Calculate checksum of your DMG
shasum -a 256 build/IPTVPlayer-*.dmg

# Compare with checksums.txt
cat build/checksums.txt
```

- [ ] Checksums match
- [ ] No corruption detected

#### Verify DMG Properties

```bash
# Get DMG info
hdiutil imageinfo build/IPTVPlayer-*.dmg
```

Check:
- [ ] Format: UDZO (compressed)
- [ ] Checksum present
- [ ] No errors reported

#### Verify App Bundle

```bash
# Check app structure
ls -la build/export/IPTVPlayer.app/Contents/

# Verify Info.plist
plutil -lint build/export/IPTVPlayer.app/Contents/Info.plist

# Check executable
file build/export/IPTVPlayer.app/Contents/MacOS/IPTVPlayer
```

Expected:
- [ ] Info.plist is valid
- [ ] Executable is Mach-O 64-bit
- [ ] All required frameworks present
- [ ] Resources folder contains assets

### Phase 4: Functional Testing

After installation, test core functionality:

#### Basic Functionality
- [ ] App launches successfully
- [ ] Main window displays correctly
- [ ] Menu bar items work
- [ ] Preferences can be opened
- [ ] App can be quit normally

#### Feature Testing
- [ ] Can add M3U playlist
- [ ] Can add Xtream account
- [ ] Playlist sidebar displays
- [ ] Channel list loads
- [ ] Video player works
- [ ] Favorites can be added/removed

#### System Integration
- [ ] App appears in Launchpad
- [ ] App appears in Spotlight search
- [ ] Dock icon displays correctly
- [ ] Window management works (minimize, maximize, close)
- [ ] App respects system dark/light mode

### Phase 5: Clean System Testing

For thorough testing, test on a clean macOS system:

#### Using a Virtual Machine

1. **Create macOS VM** (using Parallels, VMware, or UTM)
2. **Install fresh macOS**
3. **Copy DMG to VM**
4. **Test installation** (follow Phase 2 steps)
5. **Verify no dependency issues**

#### Using a Test Mac

1. **Create new user account** on test Mac
2. **Log in as new user**
3. **Test installation** (follow Phase 2 steps)
4. **Verify clean installation experience**

### Phase 6: Uninstallation Testing

- [ ] **Drag app to Trash**
- [ ] **Empty Trash**
- [ ] **Verify cleanup**:
  ```bash
  # Check for leftover files
  ls ~/Library/Application\ Support/IPTVPlayer/
  ls ~/Library/Preferences/com.menmapro.iptv.*
  ls ~/Library/Caches/com.menmapro.iptv.*
  ```
- [ ] **Reinstall and verify** it works as fresh install

## Troubleshooting

### Issue: DMG Creation Fails

**Symptoms**: Script exits with error during DMG creation

**Solutions**:
1. Check disk space: `df -h`
2. Verify build succeeded: Check `build/export/IPTVPlayer.app` exists
3. Try with hdiutil fallback:
   ```bash
   hdiutil create -volname "IPTV Player" \
     -srcfolder build/export/IPTVPlayer.app \
     -ov -format UDZO \
     build/IPTVPlayer-test.dmg
   ```

### Issue: DMG Won't Mount

**Symptoms**: Error when trying to open DMG

**Solutions**:
1. Verify DMG integrity:
   ```bash
   hdiutil verify build/IPTVPlayer-*.dmg
   ```
2. Check for corruption:
   ```bash
   shasum -a 256 build/IPTVPlayer-*.dmg
   ```
3. Recreate DMG from scratch

### Issue: App Won't Launch

**Symptoms**: App crashes or shows error on launch

**Solutions**:
1. Check Console.app for crash logs
2. Verify app signature (even if unsigned):
   ```bash
   codesign -dv build/export/IPTVPlayer.app
   ```
3. Check for missing frameworks:
   ```bash
   otool -L build/export/IPTVPlayer.app/Contents/MacOS/IPTVPlayer
   ```
4. Remove quarantine attribute:
   ```bash
   xattr -cr /Applications/IPTVPlayer.app
   ```

### Issue: Security Warning Persists

**Symptoms**: macOS blocks app even after "Open Anyway"

**Solutions**:
1. Remove all extended attributes:
   ```bash
   sudo xattr -cr /Applications/IPTVPlayer.app
   ```
2. Check Gatekeeper status:
   ```bash
   spctl --status
   ```
3. For testing only, temporarily disable Gatekeeper:
   ```bash
   sudo spctl --master-disable
   # Remember to re-enable after testing:
   sudo spctl --master-enable
   ```

### Issue: App Missing Dependencies

**Symptoms**: App launches but features don't work

**Solutions**:
1. Verify Swift runtime is embedded:
   ```bash
   ls build/export/IPTVPlayer.app/Contents/Frameworks/
   ```
2. Check for missing resources:
   ```bash
   ls build/export/IPTVPlayer.app/Contents/Resources/
   ```
3. Rebuild with clean build folder:
   ```bash
   rm -rf build
   ./create-dmg-local.sh
   ```

## Advanced Testing

### Test DMG on Different macOS Versions

If possible, test on:
- [ ] macOS 13 (Ventura) - Minimum supported
- [ ] macOS 14 (Sonoma)
- [ ] macOS 15 (Sequoia) - Latest

### Test on Different Mac Hardware

- [ ] Intel Mac
- [ ] Apple Silicon (M1/M2/M3)

### Performance Testing

```bash
# Measure app launch time
time open -a IPTVPlayer

# Check memory usage
top -pid $(pgrep IPTVPlayer) -l 1

# Monitor CPU usage
top -pid $(pgrep IPTVPlayer) -stats pid,cpu,mem -l 5
```

## CI/CD Integration Verification

After local testing succeeds, verify CI/CD workflow:

1. **Push to branch** that triggers CI/CD
2. **Monitor GitHub Actions** workflow
3. **Download artifact** from workflow
4. **Compare local vs CI DMG**:
   ```bash
   # Compare sizes
   ls -lh build/IPTVPlayer-*.dmg
   ls -lh ~/Downloads/IPTVPlayer-*.dmg
   
   # Compare checksums
   shasum -a 256 build/IPTVPlayer-*.dmg
   shasum -a 256 ~/Downloads/IPTVPlayer-*.dmg
   ```
5. **Test CI-built DMG** using same checklist

## Signing and Notarization (Future)

For production releases, you'll need to:

1. **Code Sign** the application
   - Requires Apple Developer account
   - Developer ID Application certificate
   - See `CODE_SIGNING_GUIDE.md`

2. **Notarize** the DMG
   - Submit to Apple for notarization
   - Staple notarization ticket
   - See `notarize.sh` script

3. **Verify** signed and notarized DMG:
   ```bash
   # Check signature
   codesign -dv --verbose=4 /Applications/IPTVPlayer.app
   
   # Check notarization
   spctl -a -vvv -t install IPTVPlayer-*.dmg
   
   # Verify stapled ticket
   xcrun stapler validate IPTVPlayer-*.dmg
   ```

## Checklist Summary

Use this quick checklist for each DMG test:

- [ ] DMG creation script runs without errors
- [ ] DMG file is created in `build/` directory
- [ ] DMG can be opened/mounted
- [ ] App can be dragged to Applications
- [ ] App launches successfully
- [ ] Core features work correctly
- [ ] No console errors or crashes
- [ ] DMG can be unmounted cleanly
- [ ] Checksums match expected values
- [ ] Installation works on clean system (if tested)

## Success Criteria

The DMG is ready for distribution when:

✅ All items in the checklist are complete
✅ No errors during creation or installation
✅ App functions correctly after installation
✅ Works on minimum supported macOS version
✅ No security warnings (for signed builds)
✅ CI/CD workflow produces identical results

## Next Steps

After successful local testing:

1. **Update tasks.md** - Mark task 28 as complete
2. **Document any issues** found during testing
3. **Proceed to task 29** - Final checkpoint
4. **Prepare for release** - Task 30

## Resources

- [Apple DMG Documentation](https://developer.apple.com/library/archive/documentation/CoreFoundation/Conceptual/CFBundles/BundleTypes/BundleTypes.html)
- [create-dmg GitHub](https://github.com/create-dmg/create-dmg)
- [hdiutil man page](https://ss64.com/osx/hdiutil.html)
- [Code Signing Guide](./CODE_SIGNING_GUIDE.md)
- [CI/CD Guide](./CI_CD_GUIDE.md)

