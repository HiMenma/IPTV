# macOS CI/CD Guide

This guide explains the Continuous Integration and Continuous Deployment (CI/CD) setup for the macOS native application.

## Overview

The macOS application uses GitHub Actions for automated building, testing, and releasing. The workflow is defined in `.github/workflows/macos-ci.yml`.

## Workflow Structure

### 1. Build and Test Job

**Triggers:**
- Push to `main` or `develop` branches (when macOS files change)
- Pull requests to `main` or `develop` branches (when macOS files change)
- Manual trigger via GitHub UI

**What it does:**
1. Sets up macOS runner with Xcode 15.2
2. Caches Swift Package Manager dependencies
3. Resolves package dependencies
4. Builds the app in Debug configuration
5. Runs all unit tests
6. Generates code coverage report
7. Uploads coverage to Codecov (optional)
8. Archives build logs on failure

**Duration:** ~5-10 minutes

### 2. Build Release Job

**Triggers:**
- Push to `main` branch
- Push of version tags (e.g., `v1.0.0`)

**Requirements:**
- Build and Test job must pass first

**What it does:**
1. Builds the app in Release configuration
2. Creates an Xcode archive
3. Exports the application
4. Creates a DMG installer
5. Calculates SHA-256 checksums
6. Uploads DMG and checksums as artifacts

**Duration:** ~10-15 minutes

### 3. Create Release Job

**Triggers:**
- Push of version tags starting with `v` (e.g., `v1.0.0`)

**Requirements:**
- Build Release job must complete successfully

**What it does:**
1. Downloads build artifacts
2. Creates a GitHub Release
3. Attaches DMG and checksums
4. Generates release notes automatically

**Duration:** ~1-2 minutes

## Local Testing

Before pushing changes, you can test the CI/CD pipeline locally:

```bash
cd macos
./ci-test.sh
```

This script simulates the GitHub Actions workflow and will:
- Build Debug and Release configurations
- Run tests
- Create a DMG
- Calculate checksums

## Creating a Release

### Step 1: Update Version

Edit `macos/IPTVPlayer/Info.plist` and update the version:

```xml
<key>CFBundleShortVersionString</key>
<string>1.0.0</string>
<key>CFBundleVersion</key>
<string>1</string>
```

### Step 2: Commit and Tag

```bash
git add macos/IPTVPlayer/Info.plist
git commit -m "Bump version to 1.0.0"
git tag v1.0.0
git push origin main
git push origin v1.0.0
```

### Step 3: Monitor Build

1. Go to the Actions tab in GitHub
2. Watch the "macOS CI/CD" workflow
3. Wait for all jobs to complete

### Step 4: Verify Release

1. Go to the Releases section
2. Find your new release
3. Download and test the DMG

## Artifacts

### Build Artifacts

**Location:** Workflow run summary page → Artifacts section

**Files:**
- `IPTVPlayer-macOS-{version}.dmg` - The installer
- `checksums.txt` - SHA-256 checksums

**Retention:** 90 days

### Build Logs

**Location:** Workflow run → Job → Step logs

**On Failure:** Diagnostic reports are uploaded as artifacts (7 day retention)

## Code Coverage

Code coverage is generated during the test phase and can be uploaded to Codecov.

### Setup Codecov (Optional)

1. Sign up at [codecov.io](https://codecov.io)
2. Add your repository
3. Get the upload token
4. Add it to GitHub Secrets as `CODECOV_TOKEN`

The workflow will automatically upload coverage reports.

### View Coverage Locally

After running tests:

```bash
xcrun llvm-cov show \
  -instr-profile=$(find ~/Library/Developer/Xcode/DerivedData -name "*.profdata" | head -n 1) \
  $(find ~/Library/Developer/Xcode/DerivedData -name "IPTVPlayer" -type f | head -n 1) \
  -format=html \
  -output-dir=coverage
open coverage/index.html
```

## Code Signing (Future)

Currently, builds are unsigned for development. For production:

### Required Secrets

Add these to GitHub repository secrets:

1. `MACOS_CERTIFICATE` - Base64-encoded .p12 certificate
2. `MACOS_CERTIFICATE_PASSWORD` - Certificate password
3. `MACOS_KEYCHAIN_PASSWORD` - Temporary keychain password

### Generate Certificate

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
base64 -i certificate.p12 -o certificate.base64
```

### Update Workflow

Remove these lines from xcodebuild commands:
```yaml
CODE_SIGN_IDENTITY=""
CODE_SIGNING_REQUIRED=NO
CODE_SIGNING_ALLOWED=NO
```

Add certificate import step before building:
```yaml
- name: Import Code Signing Certificate
  run: |
    echo "${{ secrets.MACOS_CERTIFICATE }}" | base64 --decode > certificate.p12
    security create-keychain -p "${{ secrets.MACOS_KEYCHAIN_PASSWORD }}" build.keychain
    security default-keychain -s build.keychain
    security unlock-keychain -p "${{ secrets.MACOS_KEYCHAIN_PASSWORD }}" build.keychain
    security import certificate.p12 -k build.keychain -P "${{ secrets.MACOS_CERTIFICATE_PASSWORD }}" -T /usr/bin/codesign
    security set-key-partition-list -S apple-tool:,apple:,codesign: -s -k "${{ secrets.MACOS_KEYCHAIN_PASSWORD }}" build.keychain
```

## Notarization (Future)

For public distribution, Apple requires notarization.

### Required Secrets

1. `APPLE_ID` - Your Apple ID email
2. `APPLE_APP_PASSWORD` - App-specific password
3. `APPLE_TEAM_ID` - Your Developer Team ID

### Generate App-Specific Password

1. Go to [appleid.apple.com](https://appleid.apple.com)
2. Sign in
3. Security → App-Specific Passwords
4. Generate new password

### Add Notarization Step

After creating DMG:

```yaml
- name: Notarize DMG
  run: |
    xcrun notarytool submit \
      ./build/IPTVPlayer-${VERSION}.dmg \
      --apple-id "${{ secrets.APPLE_ID }}" \
      --password "${{ secrets.APPLE_APP_PASSWORD }}" \
      --team-id "${{ secrets.APPLE_TEAM_ID }}" \
      --wait
    
    xcrun stapler staple ./build/IPTVPlayer-${VERSION}.dmg
```

## Troubleshooting

### Build Fails on Dependency Resolution

**Problem:** Swift Package Manager can't resolve dependencies

**Solution:**
1. Check Package.swift for correct versions
2. Clear SPM cache locally: `rm -rf ~/Library/Caches/org.swift.swiftpm`
3. Clear GitHub Actions cache
4. Verify package URLs are accessible

### Tests Fail in CI but Pass Locally

**Problem:** Environment differences

**Solution:**
1. Check Xcode version matches (15.2)
2. Verify macOS version compatibility
3. Check for hardcoded paths
4. Review test logs for specific failures

### DMG Creation Fails

**Problem:** create-dmg not available or fails

**Solution:**
- Workflow has fallback to hdiutil
- Check if app was exported successfully
- Verify app bundle structure

### Slow Builds

**Problem:** Builds take too long

**Solution:**
1. Check if cache is working
2. Review cache key in workflow
3. Consider splitting jobs
4. Optimize build settings

### Artifacts Not Uploaded

**Problem:** Can't find build artifacts

**Solution:**
1. Check if build completed successfully
2. Verify artifact paths in workflow
3. Check retention period (90 days)
4. Ensure sufficient storage quota

## Performance Optimization

### Cache Strategy

The workflow caches:
- Swift Package Manager dependencies
- Xcode DerivedData

**Cache Key:** Based on Package.swift and project.pbxproj hashes

**Invalidation:** Automatic when dependencies change

### Build Optimization

**Debug Builds:**
- No optimization (-Onone)
- Full debug info
- Fast compilation

**Release Builds:**
- Whole module optimization (-O)
- Stripped symbols
- Smaller binary size

## Monitoring

### Build Status Badge

Add to README.md:

```markdown
[![macOS CI/CD](https://github.com/YOUR_USERNAME/YOUR_REPO/actions/workflows/macos-ci.yml/badge.svg)](https://github.com/YOUR_USERNAME/YOUR_REPO/actions/workflows/macos-ci.yml)
```

### Notifications

Configure in repository settings:
- Settings → Notifications
- Enable email notifications for workflow failures

### Slack Integration (Optional)

Add to workflow:

```yaml
- name: Notify Slack
  if: failure()
  uses: slackapi/slack-github-action@v1
  with:
    webhook-url: ${{ secrets.SLACK_WEBHOOK_URL }}
    payload: |
      {
        "text": "macOS build failed: ${{ github.event.head_commit.message }}"
      }
```

## Best Practices

1. **Always test locally first** using `ci-test.sh`
2. **Use semantic versioning** for tags (v1.0.0, v1.1.0, etc.)
3. **Write meaningful commit messages** for release notes
4. **Keep dependencies updated** regularly
5. **Monitor build times** and optimize if needed
6. **Review test coverage** and add tests for new features
7. **Test DMG installation** before releasing
8. **Keep secrets secure** and rotate regularly

## Requirements Satisfied

This CI/CD setup satisfies:

✅ **Requirement 9.6**: Support for automated builds and releases via GitHub Actions
- Automated building on push and PR
- Automated testing with coverage
- Automated DMG generation
- Automated release creation

## Additional Resources

- [GitHub Actions Documentation](https://docs.github.com/en/actions)
- [Xcode Build Settings Reference](https://developer.apple.com/documentation/xcode/build-settings-reference)
- [Code Signing Guide](https://developer.apple.com/support/code-signing/)
- [Notarization Guide](https://developer.apple.com/documentation/security/notarizing_macos_software_before_distribution)
- [DMG Creation](https://github.com/create-dmg/create-dmg)

## Support

For issues with CI/CD:
1. Check workflow logs
2. Review this guide
3. Test locally with ci-test.sh
4. Open an issue with logs attached
