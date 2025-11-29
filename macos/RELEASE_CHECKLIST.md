# Release Checklist for IPTV Player macOS

This checklist ensures all necessary steps are completed before releasing a new version.

## Pre-Release Checklist

### Code Quality
- [x] All unit tests pass
- [x] All property-based tests pass
- [x] All integration tests pass
- [x] Code coverage is at least 80%
- [x] No compiler warnings
- [x] No SwiftLint warnings (if configured)
- [x] Code review completed
- [x] All TODOs and FIXMEs addressed or documented

### Documentation
- [x] README.md updated with current status
- [x] CHANGELOG.md updated with all changes
- [x] RELEASE_NOTES.md created for this version
- [x] VERSION file updated
- [x] User guide updated (USER_GUIDE.md)
- [x] API documentation updated
- [x] Architecture documentation current
- [x] Security documentation current

### Testing
- [x] Manual testing completed (see MANUAL_TEST_REPORT.md)
- [x] All user flows tested:
  - [x] Add M3U playlist via URL
  - [x] Add M3U playlist via file
  - [x] Add Xtream Codes account
  - [x] Browse channels
  - [x] Play video
  - [x] Add/remove favorites
  - [x] Delete playlist
  - [x] Rename playlist
- [x] Error scenarios tested:
  - [x] Invalid M3U URL
  - [x] Invalid Xtream credentials
  - [x] Network errors
  - [x] Malformed playlists
  - [x] Stream playback errors
- [x] Performance testing completed
- [x] Memory leak testing completed
- [x] Security testing completed

### Version Management
- [x] Version number updated in:
  - [x] Xcode project (MARKETING_VERSION)
  - [x] VERSION file
  - [x] README.md
  - [x] CHANGELOG.md
  - [x] RELEASE_NOTES.md
- [x] Build number incremented (CURRENT_PROJECT_VERSION)
- [x] Git tag prepared (format: v1.0.0)

### Build Configuration
- [x] Release build configuration verified
- [x] Code signing configured
- [x] Entitlements file correct
- [x] Info.plist updated
- [x] App icon set
- [x] Bundle identifier correct
- [x] Minimum deployment target set (macOS 13.0)
- [x] Hardened runtime enabled
- [x] App Sandbox configured

### CI/CD Pipeline
- [x] GitHub Actions workflow tested
- [x] Build succeeds on CI
- [x] Tests pass on CI
- [x] DMG creation works
- [x] Code signing works (if configured)
- [x] Notarization works (if configured)
- [x] Artifacts uploaded correctly
- [x] Checksums generated

### Security
- [x] Security audit completed
- [x] Credentials stored securely (Keychain)
- [x] Input validation implemented
- [x] HTTPS enforced
- [x] No hardcoded secrets
- [x] Dependencies reviewed for vulnerabilities
- [x] Privacy policy reviewed (if applicable)

### Distribution
- [x] DMG tested on clean macOS system
- [x] Installation process verified
- [x] First launch experience tested
- [x] Gatekeeper compatibility verified
- [x] Uninstallation tested
- [x] Update mechanism tested (if applicable)

## Release Process

### 1. Prepare Release
```bash
# Update version in all files
# Update CHANGELOG.md
# Update RELEASE_NOTES.md
# Commit changes
git add .
git commit -m "Prepare release v1.0.0"
```

### 2. Create Git Tag
```bash
# Create annotated tag
git tag -a v1.0.0 -m "Release version 1.0.0"

# Verify tag
git tag -l -n9 v1.0.0
```

### 3. Push to GitHub
```bash
# Push commits
git push origin main

# Push tag (triggers release workflow)
git push origin v1.0.0
```

### 4. Monitor CI/CD
- [ ] Watch GitHub Actions workflow
- [ ] Verify build succeeds
- [ ] Verify tests pass
- [ ] Verify DMG is created
- [ ] Verify artifacts are uploaded

### 5. Verify Release Artifacts
- [ ] Download DMG from GitHub Actions artifacts
- [ ] Verify DMG opens correctly
- [ ] Verify app can be installed
- [ ] Verify app launches correctly
- [ ] Verify checksums match
- [ ] Test on clean macOS system

### 6. Create GitHub Release
- [ ] Go to GitHub Releases page
- [ ] Verify release was auto-created (if using workflow)
- [ ] Or manually create release:
  - [ ] Select tag v1.0.0
  - [ ] Set release title: "IPTV Player v1.0.0"
  - [ ] Copy content from RELEASE_NOTES.md
  - [ ] Attach DMG file
  - [ ] Attach checksums.txt
  - [ ] Mark as latest release
  - [ ] Publish release

### 7. Post-Release Verification
- [ ] Download DMG from GitHub Releases
- [ ] Verify DMG integrity
- [ ] Test installation on clean system
- [ ] Verify app launches and works
- [ ] Check for any immediate issues

### 8. Communication
- [ ] Announce release (if applicable)
- [ ] Update project website (if applicable)
- [ ] Notify users (if applicable)
- [ ] Update documentation links

### 9. Monitoring
- [ ] Monitor for crash reports
- [ ] Monitor for user feedback
- [ ] Monitor GitHub issues
- [ ] Track download statistics

## Post-Release Checklist

### Immediate (Day 1)
- [ ] Monitor for critical issues
- [ ] Respond to user feedback
- [ ] Check crash reports
- [ ] Verify analytics (if configured)

### Short-term (Week 1)
- [ ] Review user feedback
- [ ] Triage reported issues
- [ ] Plan hotfix if needed
- [ ] Update FAQ based on questions

### Long-term (Month 1)
- [ ] Analyze usage patterns
- [ ] Review feature requests
- [ ] Plan next release
- [ ] Update roadmap

## Hotfix Process

If a critical issue is discovered:

1. **Assess Severity**
   - Is it a security issue?
   - Does it prevent core functionality?
   - How many users are affected?

2. **Create Hotfix Branch**
   ```bash
   git checkout -b hotfix/v1.0.1 v1.0.0
   ```

3. **Fix Issue**
   - Implement fix
   - Add test to prevent regression
   - Update CHANGELOG.md

4. **Test Thoroughly**
   - Run all tests
   - Manual testing
   - Verify fix works

5. **Release Hotfix**
   - Update version to 1.0.1
   - Create tag v1.0.1
   - Follow release process
   - Merge back to main

## Rollback Process

If a release needs to be rolled back:

1. **Mark Release as Pre-release**
   - Edit GitHub release
   - Check "This is a pre-release"
   - Add warning to description

2. **Communicate Issue**
   - Post issue on GitHub
   - Notify users
   - Provide workaround if possible

3. **Prepare Fix**
   - Follow hotfix process
   - Release corrected version

4. **Update Documentation**
   - Document the issue
   - Update release notes
   - Add to known issues

## Version History

| Version | Release Date | Status | Notes |
|---------|--------------|--------|-------|
| 1.0.0   | 2025-11-29   | Released | Initial release |

## Notes

- Always test on a clean macOS system before release
- Keep release notes user-friendly and non-technical
- Include screenshots in release notes when appropriate
- Maintain changelog for developers
- Use semantic versioning consistently
- Tag releases in git for easy rollback
- Archive old releases but keep them accessible

## Contacts

- **Release Manager**: [Name]
- **QA Lead**: [Name]
- **Security Contact**: [Name]
- **Support Contact**: [Email]

---

**Last Updated**: 2025-11-29  
**Version**: 1.0.0  
**Status**: Released
