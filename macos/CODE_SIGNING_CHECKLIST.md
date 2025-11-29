# Code Signing Configuration Checklist

This checklist helps ensure all code signing requirements are properly configured for the IPTV Player macOS application.

## Prerequisites

- [ ] macOS 13.0 or later installed
- [ ] Xcode 15.0 or later installed
- [ ] Command Line Tools installed (`xcode-select --install`)
- [ ] Active Apple Developer account ($99/year)

## Apple Developer Account Setup

- [ ] Enrolled in Apple Developer Program
- [ ] Account approved and active
- [ ] Apple ID added to Xcode (Xcode > Settings > Accounts)
- [ ] Team selected in Xcode
- [ ] Certificates downloaded

## Certificates

### Development Certificates
- [ ] Apple Development certificate created
- [ ] Certificate installed in Keychain
- [ ] Certificate verified: `security find-identity -v -p codesigning`

### Distribution Certificates
- [ ] Developer ID Application certificate created
- [ ] Certificate installed in Keychain
- [ ] Certificate verified and valid

### Certificate Backup
- [ ] Certificates exported as .p12 files
- [ ] .p12 files stored securely (NOT in version control)
- [ ] Certificate passwords documented securely

## Provisioning Profiles

- [ ] Development provisioning profile created (if using manual signing)
- [ ] Distribution provisioning profile created (if using manual signing)
- [ ] Profiles downloaded and installed
- [ ] Profiles match bundle identifier: `com.menmapro.IPTVPlayer`

## Xcode Project Configuration

### Signing & Capabilities Tab
- [ ] Automatically manage signing enabled (recommended)
- [ ] Team selected from dropdown
- [ ] Bundle Identifier set: `com.menmapro.IPTVPlayer`
- [ ] Signing certificate selected
- [ ] Provisioning profile selected (if manual signing)

### Capabilities
- [ ] Hardened Runtime enabled
- [ ] App Sandbox enabled
- [ ] Network Client entitlement added
- [ ] User Selected Files entitlement added

### Build Settings
- [ ] Code Signing Style: Automatic (or Manual)
- [ ] Code Signing Identity (Debug): Apple Development
- [ ] Code Signing Identity (Release): Developer ID Application
- [ ] Development Team: [Your Team ID]
- [ ] Code Sign Entitlements: IPTVPlayer/IPTVPlayer.entitlements

## Entitlements File

File: `IPTVPlayer/IPTVPlayer.entitlements`

Required entitlements:
- [ ] `com.apple.security.app-sandbox` = true
- [ ] `com.apple.security.network.client` = true
- [ ] `com.apple.security.files.user-selected.read-write` = true
- [ ] `com.apple.security.cs.allow-jit` = false
- [ ] `com.apple.security.cs.allow-unsigned-executable-memory` = false
- [ ] `com.apple.security.cs.allow-dyld-environment-variables` = false
- [ ] `com.apple.security.cs.disable-library-validation` = false

## Info.plist Configuration

File: `IPTVPlayer/Info.plist`

Required keys:
- [ ] `CFBundleIdentifier`: com.menmapro.IPTVPlayer
- [ ] `CFBundleVersion`: 1 (or current build number)
- [ ] `CFBundleShortVersionString`: 1.0 (or current version)
- [ ] `LSMinimumSystemVersion`: 13.0
- [ ] `CFBundleDisplayName`: IPTV Player
- [ ] `LSApplicationCategoryType`: public.app-category.entertainment

## Build and Verification

### Development Build
- [ ] Clean build succeeds: `xcodebuild clean build`
- [ ] App launches without errors
- [ ] Code signature verified: `codesign --verify --deep --strict IPTVPlayer.app`
- [ ] Entitlements correct: `codesign -d --entitlements - IPTVPlayer.app`

### Release Build
- [ ] Release build succeeds
- [ ] Code signature verified
- [ ] Hardened runtime enabled: `codesign -d --verbose IPTVPlayer.app | grep runtime`
- [ ] All frameworks signed
- [ ] No unsigned code warnings

### Signature Verification Commands
```bash
# Verify signature
codesign --verify --deep --strict --verbose=2 IPTVPlayer.app

# Check signature details
codesign -dv --verbose=4 IPTVPlayer.app

# Verify entitlements
codesign -d --entitlements - IPTVPlayer.app

# Check hardened runtime
codesign -d --verbose IPTVPlayer.app | grep runtime
```

## Notarization Setup

### App-Specific Password
- [ ] App-specific password created at appleid.apple.com
- [ ] Password stored securely (NOT in version control)
- [ ] Password saved in Keychain for notarytool

### Notarization Credentials
- [ ] Credentials stored in Keychain:
  ```bash
  xcrun notarytool store-credentials "IPTVPlayer-Notarization" \
      --apple-id "your.email@example.com" \
      --team-id "YOUR_TEAM_ID" \
      --password "app-specific-password"
  ```
- [ ] Credentials verified: `xcrun notarytool history --keychain-profile "IPTVPlayer-Notarization"`

### Notarization Process
- [ ] DMG created successfully
- [ ] DMG submitted for notarization
- [ ] Notarization succeeded (status: Accepted)
- [ ] Ticket stapled to DMG: `xcrun stapler staple IPTVPlayer.dmg`
- [ ] Stapling verified: `xcrun stapler validate IPTVPlayer.dmg`
- [ ] Gatekeeper accepts DMG: `spctl -a -vvv -t install IPTVPlayer.dmg`

## CI/CD Configuration

### GitHub Secrets
- [ ] `APPLE_ID`: Apple ID email
- [ ] `APPLE_ID_PASSWORD`: App-specific password
- [ ] `APPLE_TEAM_ID`: Team ID
- [ ] `MACOS_CERTIFICATE`: Base64-encoded certificate
- [ ] `MACOS_CERTIFICATE_PASSWORD`: Certificate password
- [ ] `KEYCHAIN_PASSWORD`: Temporary keychain password

### GitHub Actions Workflow
- [ ] Workflow file exists: `.github/workflows/macos-ci.yml`
- [ ] Certificate installation step configured
- [ ] Code signing step configured
- [ ] Build step configured
- [ ] Notarization step configured
- [ ] DMG creation step configured
- [ ] Artifact upload configured

### CI/CD Testing
- [ ] Workflow runs successfully
- [ ] Artifacts generated correctly
- [ ] DMG is properly signed and notarized
- [ ] Release created automatically on tag

## Testing

### Local Testing
- [ ] App runs on development machine
- [ ] All features work correctly
- [ ] No permission errors
- [ ] Network streaming works
- [ ] File import works
- [ ] Database operations work

### Clean System Testing
- [ ] App copied to USB drive
- [ ] Tested on Mac without Xcode
- [ ] No Gatekeeper warnings
- [ ] App opens without issues
- [ ] All features work correctly
- [ ] No sandbox violations

### Gatekeeper Testing
```bash
# Test Gatekeeper assessment
spctl -a -vvv -t install IPTVPlayer.app

# Expected output:
# IPTVPlayer.app: accepted
# source=Developer ID
```

### Sandbox Testing
- [ ] Network access works (IPTV streaming)
- [ ] File access works (M3U import)
- [ ] Database access works (Core Data)
- [ ] No unexpected permission dialogs
- [ ] No sandbox violations in Console.app

## Security Hardening

### Hardened Runtime
- [ ] JIT compilation disabled
- [ ] Unsigned executable memory disabled
- [ ] DYLD environment variables disabled
- [ ] Library validation enabled

### App Sandbox
- [ ] Sandbox enabled
- [ ] Minimal entitlements configured
- [ ] No unnecessary permissions
- [ ] File access limited to user-selected files

### Best Practices
- [ ] No hardcoded credentials in code
- [ ] Sensitive data stored in Keychain
- [ ] HTTPS enforced for network requests
- [ ] Input validation implemented
- [ ] Error messages don't leak sensitive info

## Documentation

- [ ] CODE_SIGNING_GUIDE.md reviewed
- [ ] Setup script tested: `./setup-code-signing.sh`
- [ ] Notarization script tested: `./notarize.sh`
- [ ] Build script updated with signing
- [ ] Release process documented
- [ ] Troubleshooting guide available

## Distribution

### Pre-Distribution
- [ ] Version number updated
- [ ] Release notes prepared
- [ ] Screenshots updated
- [ ] User guide updated
- [ ] Known issues documented

### Distribution Package
- [ ] DMG created and tested
- [ ] DMG properly signed
- [ ] DMG notarized and stapled
- [ ] DMG tested on clean system
- [ ] DMG size optimized

### Release
- [ ] Git tag created
- [ ] GitHub release created
- [ ] DMG uploaded to release
- [ ] Release notes published
- [ ] Download link tested

## Post-Release

- [ ] Monitor for installation issues
- [ ] Check for Gatekeeper problems
- [ ] Verify download statistics
- [ ] Collect user feedback
- [ ] Plan next release

## Troubleshooting Reference

### Common Issues

**"No signing certificate found"**
- Check: `security find-identity -v -p codesigning`
- Solution: Install certificates in Keychain

**"Provisioning profile doesn't match"**
- Clean derived data: `rm -rf ~/Library/Developer/Xcode/DerivedData`
- Rebuild project

**"App is damaged and can't be opened"**
- App needs notarization
- Run: `./notarize.sh`

**"Code signing failed"**
- Verify team ID matches
- Check bundle identifier
- Verify entitlements are valid

**"Notarization failed"**
- Check notarization log
- Verify hardened runtime enabled
- Check for invalid entitlements

### Getting Help

- Apple Developer Forums: https://developer.apple.com/forums/
- Technical Support: https://developer.apple.com/support/
- Documentation: https://developer.apple.com/documentation/

## Sign-Off

### Development Team
- [ ] Code signing configured by: _________________ Date: _______
- [ ] Configuration verified by: _________________ Date: _______
- [ ] Notarization tested by: _________________ Date: _______

### Release Manager
- [ ] Release build approved by: _________________ Date: _______
- [ ] Distribution package verified by: _________________ Date: _______
- [ ] Release published by: _________________ Date: _______

## Notes

Use this section to document any project-specific notes, issues encountered, or deviations from the standard process:

---

**Last Updated**: November 29, 2025
**Version**: 1.0
**Status**: Ready for Implementation
