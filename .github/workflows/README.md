# GitHub Actions Workflows

This directory contains CI/CD workflows for the IPTV Player project.

## Workflows

### 1. macOS CI/CD (`macos-ci.yml`)

Automated build, test, and release pipeline for the native macOS application.

#### Triggers
- **Push**: Runs on pushes to `main` or `develop` branches when macOS files change
- **Pull Request**: Runs on PRs to `main` or `develop` branches when macOS files change
- **Manual**: Can be triggered manually via workflow_dispatch
- **Tags**: Creates releases when tags starting with `v` are pushed

#### Jobs

##### Build and Test
- Runs on every push and PR
- Uses macOS-latest runner with Xcode 15.2
- Steps:
  1. Checkout code
  2. Select Xcode version
  3. Cache Swift Package Manager dependencies
  4. Resolve package dependencies
  5. Build in Debug configuration
  6. Run unit tests with code coverage
  7. Generate and upload coverage report to Codecov
  8. Archive build logs on failure

##### Build Release
- Runs only on pushes to `main` or tags
- Requires successful build-and-test job
- Steps:
  1. Build in Release configuration
  2. Create archive
  3. Export application
  4. Create DMG installer
  5. Calculate checksums
  6. Upload artifacts (DMG and checksums)

##### Create Release
- Runs only on version tags (e.g., `v1.0.0`)
- Requires successful build-release job
- Steps:
  1. Download build artifacts
  2. Create GitHub Release with DMG and checksums
  3. Generate release notes automatically

#### Artifacts

**Build Artifacts** (retained for 90 days):
- `IPTVPlayer-macOS-{version}.dmg` - macOS installer
- `checksums.txt` - SHA-256 checksums for verification

**Build Logs** (retained for 7 days, only on failure):
- Diagnostic reports from failed builds

#### Code Signing

Currently configured for unsigned builds (development). For production releases:

1. Add signing certificates to GitHub Secrets:
   - `MACOS_CERTIFICATE` - Base64-encoded .p12 certificate
   - `MACOS_CERTIFICATE_PASSWORD` - Certificate password
   - `MACOS_KEYCHAIN_PASSWORD` - Temporary keychain password

2. Update the workflow to import certificates:
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

3. Remove `CODE_SIGN_IDENTITY=""` and related flags from xcodebuild commands

#### Notarization

For App Store or public distribution, add notarization step:

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

Required secrets:
- `APPLE_ID` - Apple ID email
- `APPLE_APP_PASSWORD` - App-specific password
- `APPLE_TEAM_ID` - Developer Team ID

### 2. Build Release (`build-release.yml`)

Legacy workflow for Kotlin Multiplatform builds (Android and desktop).

This workflow continues to support the existing Android application and Kotlin-based desktop builds.

## Usage

### Running Tests Locally

```bash
cd macos
xcodebuild test \
  -project IPTVPlayer.xcodeproj \
  -scheme IPTVPlayer \
  -destination 'platform=macOS'
```

### Building Locally

```bash
cd macos
./build.sh
```

### Creating a Release

1. Update version in `macos/IPTVPlayer/Info.plist`
2. Commit changes
3. Create and push a tag:
   ```bash
   git tag v1.0.0
   git push origin v1.0.0
   ```
4. GitHub Actions will automatically build and create a release

### Manual Workflow Trigger

1. Go to Actions tab in GitHub
2. Select "macOS CI/CD" workflow
3. Click "Run workflow"
4. Select branch and click "Run workflow"

## Monitoring

- **Build Status**: Check the Actions tab for workflow runs
- **Code Coverage**: View coverage reports in Codecov (if configured)
- **Artifacts**: Download from workflow run summary page
- **Releases**: View in the Releases section

## Troubleshooting

### Build Failures

1. Check build logs in the workflow run
2. Download diagnostic reports artifact
3. Run build locally to reproduce

### Test Failures

1. Review test output in workflow logs
2. Run tests locally: `xcodebuild test -scheme IPTVPlayer -destination 'platform=macOS'`
3. Check for environment-specific issues

### DMG Creation Failures

1. Verify app was built successfully
2. Check if create-dmg is available
3. Review export options plist configuration

### Cache Issues

If builds are slow or failing due to cache:
1. Go to Actions → Caches
2. Delete relevant caches
3. Re-run workflow

## Requirements Satisfied

This CI/CD setup satisfies:
- ✅ **Requirement 9.6**: Automated builds and releases via GitHub Actions
- ✅ Automated testing in CI pipeline
- ✅ DMG artifact generation
- ✅ Release creation on version tags
- ✅ Code coverage reporting
- ✅ Build artifact retention

## Future Enhancements

- [ ] Add code signing and notarization
- [ ] Add UI tests to CI pipeline
- [ ] Add performance benchmarks
- [ ] Add security scanning (e.g., SAST)
- [ ] Add dependency vulnerability scanning
- [ ] Add automated changelog generation
- [ ] Add Slack/Discord notifications for build status
