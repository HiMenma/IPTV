# Code Signing Quick Start Guide

## TL;DR - Get Started in 5 Minutes

### For Development (No Apple Developer Account Needed)

```bash
# Just build and run - no signing required
cd macos
xcodebuild -project IPTVPlayer.xcodeproj \
    -scheme IPTVPlayer \
    -configuration Debug \
    build
```

The app will run locally without code signing.

### For Distribution (Apple Developer Account Required)

```bash
# 1. Set up code signing (one-time)
cd macos
./setup-code-signing.sh

# 2. Build release version
./build.sh

# 3. Notarize for distribution
./notarize.sh
```

Done! You'll have a notarized DMG ready for distribution.

## What You Need

### Development
- ✅ macOS 13.0+
- ✅ Xcode 15.0+
- ❌ No Apple Developer account needed

### Distribution
- ✅ macOS 13.0+
- ✅ Xcode 15.0+
- ✅ Apple Developer account ($99/year)
- ✅ App-specific password

## Common Tasks

### First Time Setup

```bash
cd macos
./setup-code-signing.sh
```

This script will:
- Check your Xcode installation
- Find your signing certificates
- Verify project configuration
- Test build with signing

### Build Signed App

```bash
cd macos
xcodebuild -project IPTVPlayer.xcodeproj \
    -scheme IPTVPlayer \
    -configuration Release \
    clean build
```

### Create Distribution DMG

```bash
cd macos
./notarize.sh
```

This will:
- Verify code signature
- Create DMG
- Submit for notarization
- Staple ticket
- Verify Gatekeeper

### Verify Signature

```bash
# Check if app is signed
codesign -dv macos/build/Build/Products/Release/IPTVPlayer.app

# Verify signature is valid
codesign --verify --deep --strict macos/build/Build/Products/Release/IPTVPlayer.app

# Check entitlements
codesign -d --entitlements - macos/build/Build/Products/Release/IPTVPlayer.app
```

## Troubleshooting

### "No signing certificate found"

**Solution**: Add your Apple ID to Xcode
1. Open Xcode
2. Xcode > Settings > Accounts
3. Click + and add your Apple ID
4. Select your team

### "App is damaged and can't be opened"

**Solution**: App needs notarization
```bash
cd macos
./notarize.sh
```

### "Provisioning profile doesn't match"

**Solution**: Clean and rebuild
```bash
rm -rf ~/Library/Developer/Xcode/DerivedData
cd macos
xcodebuild clean build
```

## CI/CD Setup

### Add Secrets to GitHub

1. Go to repository Settings > Secrets
2. Add these secrets:
   - `MACOS_CERTIFICATE`: Base64-encoded .p12 file
   - `MACOS_CERTIFICATE_PASSWORD`: Certificate password
   - `KEYCHAIN_PASSWORD`: Any secure password
   - `APPLE_ID`: Your Apple ID email
   - `APPLE_ID_PASSWORD`: App-specific password
   - `APPLE_TEAM_ID`: Your team ID

### Export Certificate

```bash
# Find your certificate
security find-identity -v -p codesigning

# Export as .p12
security export -k ~/Library/Keychains/login.keychain-db \
    -t identities \
    -f pkcs12 \
    -o certificate.p12 \
    -P "password"

# Encode to base64
base64 -i certificate.p12 -o certificate.p12.base64

# Copy the base64 content to GitHub Secrets
cat certificate.p12.base64
```

### Trigger Release

```bash
# Tag and push
git tag v1.0.0
git push origin v1.0.0

# GitHub Actions will automatically:
# - Build signed app
# - Notarize
# - Create release
# - Upload DMG
```

## File Reference

### Documentation
- `CODE_SIGNING_GUIDE.md` - Complete guide (read this for details)
- `CODE_SIGNING_CHECKLIST.md` - Implementation checklist
- `CODE_SIGNING_IMPLEMENTATION_SUMMARY.md` - What was implemented
- `CODE_SIGNING_QUICK_START.md` - This file

### Scripts
- `setup-code-signing.sh` - Interactive setup
- `notarize.sh` - Notarization automation
- `build.sh` - Build automation

### Configuration
- `IPTVPlayer/IPTVPlayer.entitlements` - App entitlements
- `IPTVPlayer.xcodeproj/project.pbxproj` - Xcode project
- `.github/workflows/macos-ci.yml` - CI/CD workflow

## Need More Help?

- **Detailed Guide**: See `CODE_SIGNING_GUIDE.md`
- **Checklist**: See `CODE_SIGNING_CHECKLIST.md`
- **Apple Docs**: https://developer.apple.com/documentation/security
- **Support**: https://developer.apple.com/support/

## Quick Commands Reference

```bash
# Setup
./setup-code-signing.sh

# Build
xcodebuild -project IPTVPlayer.xcodeproj -scheme IPTVPlayer -configuration Release build

# Verify
codesign --verify --deep --strict IPTVPlayer.app

# Notarize
./notarize.sh

# Check Gatekeeper
spctl -a -vvv -t install IPTVPlayer.dmg

# Clean
rm -rf ~/Library/Developer/Xcode/DerivedData
```

---

**Pro Tip**: For development, you don't need code signing at all. Just build and run. Code signing is only required when distributing to other users.
