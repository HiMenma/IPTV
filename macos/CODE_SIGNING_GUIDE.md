# Code Signing Configuration Guide

## Overview

This guide provides comprehensive instructions for configuring code signing for the IPTV Player macOS application. Code signing is essential for:

- **Security**: Verifies the app's authenticity and integrity
- **Distribution**: Required for distribution outside the Mac App Store
- **Notarization**: Enables Apple's notarization process
- **User Trust**: Prevents Gatekeeper warnings on user systems

## Prerequisites

### 1. Apple Developer Account

You need an active Apple Developer account:
- **Individual Account**: $99/year - https://developer.apple.com/programs/
- **Organization Account**: $99/year - Requires D-U-N-S number

### 2. Development Environment

- macOS 13.0 or later
- Xcode 15.0 or later
- Command Line Tools installed: `xcode-select --install`

## Step 1: Set Up Apple Developer Account

### 1.1 Enroll in Apple Developer Program

1. Visit https://developer.apple.com/programs/enroll/
2. Sign in with your Apple ID
3. Complete the enrollment process
4. Pay the annual fee ($99 USD)
5. Wait for approval (usually 24-48 hours)

### 1.2 Add Your Apple ID to Xcode

1. Open Xcode
2. Go to **Xcode > Settings** (or **Preferences** in older versions)
3. Select the **Accounts** tab
4. Click the **+** button at the bottom left
5. Select **Apple ID** and click **Continue**
6. Enter your Apple ID credentials
7. Click **Sign In**

## Step 2: Create Signing Certificates

### 2.1 Create Certificates via Xcode (Recommended)

Xcode can automatically create and manage certificates:

1. Open the project in Xcode: `open macos/IPTVPlayer.xcodeproj`
2. Select the **IPTVPlayer** project in the navigator
3. Select the **IPTVPlayer** target
4. Go to the **Signing & Capabilities** tab
5. Check **Automatically manage signing**
6. Select your **Team** from the dropdown
7. Xcode will automatically create necessary certificates

### 2.2 Create Certificates Manually (Advanced)

If you prefer manual certificate management:

#### Developer ID Application Certificate

1. Go to https://developer.apple.com/account/resources/certificates/list
2. Click the **+** button to create a new certificate
3. Select **Developer ID Application**
4. Click **Continue**
5. Follow the instructions to create a Certificate Signing Request (CSR):
   - Open **Keychain Access** on your Mac
   - Go to **Keychain Access > Certificate Assistant > Request a Certificate from a Certificate Authority**
   - Enter your email address
   - Enter "IPTV Player" as Common Name
   - Select **Saved to disk**
   - Click **Continue** and save the CSR file
6. Upload the CSR file
7. Download the certificate
8. Double-click to install it in your Keychain

#### Mac App Distribution Certificate (for App Store)

If you plan to distribute via the Mac App Store:

1. Follow the same process as above
2. Select **Mac App Distribution** instead
3. Install the certificate in your Keychain

### 2.3 Verify Certificates

Check that certificates are installed:

```bash
security find-identity -v -p codesigning
```

You should see output like:
```
1) XXXXXXXXXX "Developer ID Application: Your Name (TEAM_ID)"
2) YYYYYYYYYY "Apple Development: your.email@example.com (TEAM_ID)"
```

## Step 3: Configure Provisioning Profiles

### 3.1 Automatic Provisioning (Recommended)

With automatic signing enabled in Xcode:
- Xcode automatically creates and manages provisioning profiles
- Profiles are downloaded and refreshed as needed
- No manual intervention required

### 3.2 Manual Provisioning (Advanced)

For manual provisioning:

1. Go to https://developer.apple.com/account/resources/profiles/list
2. Click the **+** button
3. Select **macOS App Development** or **Developer ID**
4. Select your App ID (or create one)
5. Select your certificate
6. Download and double-click to install

## Step 4: Enable Hardened Runtime

The hardened runtime protects your app's runtime environment.

### 4.1 Enable in Xcode

1. Select the **IPTVPlayer** target
2. Go to **Signing & Capabilities**
3. Click **+ Capability**
4. Add **Hardened Runtime**

### 4.2 Configure Hardened Runtime Exceptions

Our app requires these exceptions (already configured in entitlements):

- **Network Client**: For streaming IPTV content
- **User Selected Files**: For importing M3U files

### 4.3 Verify Hardened Runtime

After building, verify hardened runtime is enabled:

```bash
codesign -d --entitlements - macos/build/Release/IPTVPlayer.app
```

## Step 5: Configure Entitlements

Entitlements define the capabilities your app needs.

### 5.1 Current Entitlements

The app is configured with these entitlements (in `IPTVPlayer.entitlements`):

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <!-- Enable App Sandbox for security -->
    <key>com.apple.security.app-sandbox</key>
    <true/>
    
    <!-- Allow outgoing network connections for IPTV streaming -->
    <key>com.apple.security.network.client</key>
    <true/>
    
    <!-- Allow reading/writing user-selected files (M3U imports) -->
    <key>com.apple.security.files.user-selected.read-write</key>
    <true/>
    
    <!-- Enable hardened runtime -->
    <key>com.apple.security.cs.allow-jit</key>
    <false/>
    <key>com.apple.security.cs.allow-unsigned-executable-memory</key>
    <false/>
    <key>com.apple.security.cs.allow-dyld-environment-variables</key>
    <false/>
    <key>com.apple.security.cs.disable-library-validation</key>
    <false/>
</dict>
</plist>
```

### 5.2 Additional Entitlements (Optional)

If you need additional capabilities:

**Keychain Access** (for storing credentials):
```xml
<key>keychain-access-groups</key>
<array>
    <string>$(AppIdentifierPrefix)com.menmapro.IPTVPlayer</string>
</array>
```

**Camera/Microphone** (if adding video recording):
```xml
<key>com.apple.security.device.camera</key>
<true/>
<key>com.apple.security.device.audio-input</key>
<true/>
```

## Step 6: Update Xcode Project Settings

### 6.1 Configure Build Settings

Update the project build settings for code signing:

1. Open `IPTVPlayer.xcodeproj` in Xcode
2. Select the project in the navigator
3. Select the **IPTVPlayer** target
4. Go to **Build Settings**
5. Search for "code sign"
6. Configure these settings:

**For Development:**
- **Code Signing Style**: Automatic
- **Code Signing Identity**: Apple Development
- **Development Team**: [Your Team ID]

**For Release/Distribution:**
- **Code Signing Style**: Automatic (or Manual if using specific profiles)
- **Code Signing Identity**: Developer ID Application
- **Development Team**: [Your Team ID]

### 6.2 Configure Signing & Capabilities

1. Go to **Signing & Capabilities** tab
2. Ensure these are configured:
   - ✅ Automatically manage signing (recommended)
   - ✅ Team selected
   - ✅ Bundle Identifier: `com.menmapro.IPTVPlayer`
   - ✅ Hardened Runtime enabled
   - ✅ App Sandbox enabled

### 6.3 Update Info.plist

Ensure `Info.plist` has required keys:

```xml
<key>CFBundleIdentifier</key>
<string>com.menmapro.IPTVPlayer</string>
<key>CFBundleVersion</key>
<string>1</string>
<key>CFBundleShortVersionString</key>
<string>1.0</string>
<key>LSMinimumSystemVersion</key>
<string>13.0</string>
```

## Step 7: Build and Sign the Application

### 7.1 Build for Development

```bash
cd macos
xcodebuild -project IPTVPlayer.xcodeproj \
    -scheme IPTVPlayer \
    -configuration Debug \
    -derivedDataPath build \
    CODE_SIGN_IDENTITY="Apple Development" \
    CODE_SIGN_STYLE=Automatic \
    DEVELOPMENT_TEAM="YOUR_TEAM_ID"
```

### 7.2 Build for Distribution

```bash
cd macos
xcodebuild -project IPTVPlayer.xcodeproj \
    -scheme IPTVPlayer \
    -configuration Release \
    -derivedDataPath build \
    CODE_SIGN_IDENTITY="Developer ID Application" \
    CODE_SIGN_STYLE=Automatic \
    DEVELOPMENT_TEAM="YOUR_TEAM_ID"
```

### 7.3 Verify Code Signature

After building, verify the signature:

```bash
# Check signature
codesign -dv --verbose=4 macos/build/Build/Products/Release/IPTVPlayer.app

# Verify signature
codesign --verify --deep --strict --verbose=2 macos/build/Build/Products/Release/IPTVPlayer.app

# Check entitlements
codesign -d --entitlements - macos/build/Build/Products/Release/IPTVPlayer.app
```

Expected output:
```
Executable=/path/to/IPTVPlayer.app/Contents/MacOS/IPTVPlayer
Identifier=com.menmapro.IPTVPlayer
Format=app bundle with Mach-O universal (x86_64 arm64)
CodeDirectory v=20500 size=... flags=0x10000(runtime) hashes=...
Signature size=...
Authority=Developer ID Application: Your Name (TEAM_ID)
Authority=Developer ID Certification Authority
Authority=Apple Root CA
Timestamp=...
Info.plist entries=...
TeamIdentifier=TEAM_ID
Runtime Version=13.0.0
Sealed Resources version=2 rules=...
Internal requirements count=1 size=...
```

## Step 8: Notarization (Required for Distribution)

### 8.1 Create App-Specific Password

1. Go to https://appleid.apple.com/account/manage
2. Sign in with your Apple ID
3. Go to **Security** section
4. Under **App-Specific Passwords**, click **Generate Password**
5. Enter a label like "IPTV Player Notarization"
6. Save the generated password securely

### 8.2 Store Credentials in Keychain

```bash
xcrun notarytool store-credentials "IPTVPlayer-Notarization" \
    --apple-id "your.email@example.com" \
    --team-id "YOUR_TEAM_ID" \
    --password "app-specific-password"
```

### 8.3 Create DMG for Distribution

```bash
# Create a DMG
hdiutil create -volname "IPTV Player" \
    -srcfolder macos/build/Build/Products/Release/IPTVPlayer.app \
    -ov -format UDZO \
    IPTVPlayer.dmg
```

### 8.4 Submit for Notarization

```bash
# Submit DMG for notarization
xcrun notarytool submit IPTVPlayer.dmg \
    --keychain-profile "IPTVPlayer-Notarization" \
    --wait

# Check status
xcrun notarytool info SUBMISSION_ID \
    --keychain-profile "IPTVPlayer-Notarization"

# Get log if needed
xcrun notarytool log SUBMISSION_ID \
    --keychain-profile "IPTVPlayer-Notarization"
```

### 8.5 Staple Notarization Ticket

After successful notarization:

```bash
# Staple the ticket to the DMG
xcrun stapler staple IPTVPlayer.dmg

# Verify stapling
xcrun stapler validate IPTVPlayer.dmg
```

## Step 9: CI/CD Integration

### 9.1 Store Secrets in GitHub

Add these secrets to your GitHub repository:

- `APPLE_ID`: Your Apple ID email
- `APPLE_ID_PASSWORD`: App-specific password
- `APPLE_TEAM_ID`: Your team ID
- `MACOS_CERTIFICATE`: Base64-encoded certificate (.p12)
- `MACOS_CERTIFICATE_PASSWORD`: Certificate password
- `KEYCHAIN_PASSWORD`: Temporary keychain password

### 9.2 Update GitHub Actions Workflow

The workflow in `.github/workflows/macos-ci.yml` handles:
- Certificate installation
- Code signing
- Building
- Notarization
- DMG creation

### 9.3 Export Certificates for CI

```bash
# Export certificate from Keychain
security find-identity -v -p codesigning

# Export as .p12
security export -k ~/Library/Keychains/login.keychain-db \
    -t identities \
    -f pkcs12 \
    -o certificate.p12 \
    -P "password"

# Encode to base64
base64 -i certificate.p12 -o certificate.p12.base64
```

## Step 10: Testing and Validation

### 10.1 Test on Clean System

1. Copy the signed app to a USB drive
2. Test on a Mac that hasn't run the app before
3. Verify no Gatekeeper warnings appear
4. Test all functionality

### 10.2 Validate Gatekeeper

```bash
# Check Gatekeeper status
spctl -a -vvv -t install IPTVPlayer.app

# Expected output for properly signed app:
# IPTVPlayer.app: accepted
# source=Developer ID
```

### 10.3 Test Sandbox Restrictions

Verify the app works within sandbox:
- Network streaming works
- File import works
- Database access works
- No unexpected permission dialogs

## Troubleshooting

### Common Issues

**Issue: "No signing certificate found"**
- Solution: Ensure certificates are installed in Keychain
- Run: `security find-identity -v -p codesigning`

**Issue: "Provisioning profile doesn't match"**
- Solution: Clean derived data and rebuild
- Run: `rm -rf ~/Library/Developer/Xcode/DerivedData`

**Issue: "App is damaged and can't be opened"**
- Solution: App needs to be notarized
- Follow notarization steps above

**Issue: "Code signing failed"**
- Solution: Check team ID and bundle identifier match
- Verify entitlements are valid

**Issue: "Notarization failed"**
- Solution: Check notarization log for details
- Common causes: Missing hardened runtime, invalid entitlements

### Getting Help

- Apple Developer Forums: https://developer.apple.com/forums/
- Technical Support: https://developer.apple.com/support/
- Documentation: https://developer.apple.com/documentation/security/notarizing_macos_software_before_distribution

## Security Best Practices

1. **Never commit certificates or passwords to version control**
2. **Use app-specific passwords, not your main Apple ID password**
3. **Rotate app-specific passwords periodically**
4. **Keep certificates backed up securely**
5. **Use automatic signing for development**
6. **Use manual signing for release builds in CI/CD**
7. **Enable all recommended hardened runtime protections**
8. **Minimize entitlements to only what's needed**

## Next Steps

After completing code signing configuration:

1. ✅ Test the signed app locally
2. ✅ Submit for notarization
3. ✅ Create distribution DMG
4. ✅ Update CI/CD pipeline
5. ✅ Test on multiple macOS versions
6. ✅ Document the release process
7. ✅ Prepare for distribution

## References

- [Code Signing Guide](https://developer.apple.com/library/archive/documentation/Security/Conceptual/CodeSigningGuide/)
- [Notarizing macOS Software](https://developer.apple.com/documentation/security/notarizing_macos_software_before_distribution)
- [Hardened Runtime](https://developer.apple.com/documentation/security/hardened_runtime)
- [App Sandbox](https://developer.apple.com/documentation/security/app_sandbox)
- [Entitlements](https://developer.apple.com/documentation/bundleresources/entitlements)
