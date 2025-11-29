# Quick Release Guide - IPTV Player macOS

This is a quick reference for releasing a new version. For detailed information, see RELEASE_CHECKLIST.md.

## Prerequisites

- [ ] All tests passing
- [ ] Documentation updated
- [ ] Version numbers updated
- [ ] Changes committed to git

## Release in 5 Steps

### 1. Verify Everything is Ready

```bash
cd macos
./verify-release.sh
```

Fix any issues reported by the script.

### 2. Create Release Tag

```bash
./create-release-tag.sh
```

This will:
- Validate version format
- Check git status
- Create annotated tag
- Show next steps

### 3. Push Tag to GitHub

```bash
git push origin v1.0.0
```

This triggers the CI/CD pipeline automatically.

### 4. Monitor GitHub Actions

1. Go to: https://github.com/[your-repo]/actions
2. Watch the workflow run
3. Wait for all jobs to complete (usually 10-15 minutes)

### 5. Verify and Publish

1. Download DMG from Actions artifacts
2. Test on clean macOS system
3. Go to GitHub Releases
4. Review auto-created release
5. Publish release

## Done! 🎉

Your release is now live.

## Quick Troubleshooting

### Build Fails
- Check GitHub Actions logs
- Verify Xcode project builds locally
- Check for missing files

### Tests Fail
- Run tests locally: `xcodebuild test ...`
- Fix failing tests
- Commit and push
- Delete and recreate tag

### DMG Not Created
- Check build logs in Actions
- Verify export step succeeded
- Check create-dmg installation

### Tag Already Exists
```bash
# Delete local tag
git tag -d v1.0.0

# Delete remote tag (if pushed)
git push origin :refs/tags/v1.0.0

# Recreate tag
./create-release-tag.sh
```

## Hotfix Release

For urgent fixes:

```bash
# Create hotfix branch from tag
git checkout -b hotfix/v1.0.1 v1.0.0

# Make fix
# Update version to 1.0.1
# Commit changes

# Create new tag
./create-release-tag.sh

# Push
git push origin v1.0.1
git push origin hotfix/v1.0.1

# Merge back to main
git checkout main
git merge hotfix/v1.0.1
git push origin main
```

## Version Numbering

- **Major** (1.x.x): Breaking changes
- **Minor** (x.1.x): New features
- **Patch** (x.x.1): Bug fixes

## Files to Update for New Version

1. `VERSION` - Version number
2. `CHANGELOG.md` - Add new version section
3. `RELEASE_NOTES.md` - Create/update release notes
4. `README.md` - Update version and status

## Common Commands

```bash
# Check current version
cat VERSION

# List all tags
git tag -l

# Show tag details
git show v1.0.0

# Delete local tag
git tag -d v1.0.0

# Delete remote tag
git push origin :refs/tags/v1.0.0

# Build locally
xcodebuild -project IPTVPlayer.xcodeproj \
  -scheme IPTVPlayer \
  -configuration Release \
  build

# Run tests locally
xcodebuild test \
  -project IPTVPlayer.xcodeproj \
  -scheme IPTVPlayer \
  -destination 'platform=macOS'
```

## Support

- **Documentation**: See RELEASE_CHECKLIST.md
- **Issues**: GitHub Issues
- **CI/CD**: .github/workflows/macos-ci.yml

---

**Remember**: Always test the DMG on a clean system before publishing!
