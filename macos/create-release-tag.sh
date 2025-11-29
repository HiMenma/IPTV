#!/bin/bash

# IPTV Player macOS - Create Release Tag Script
# This script helps create a properly formatted git tag for releases

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_info() {
    echo -e "${BLUE}ℹ ${NC}$1"
}

print_success() {
    echo -e "${GREEN}✓ ${NC}$1"
}

print_warning() {
    echo -e "${YELLOW}⚠ ${NC}$1"
}

print_error() {
    echo -e "${RED}✗ ${NC}$1"
}

# Function to check if we're in the right directory
check_directory() {
    if [ ! -f "IPTVPlayer.xcodeproj/project.pbxproj" ]; then
        print_error "This script must be run from the macos/ directory"
        exit 1
    fi
}

# Function to check git status
check_git_status() {
    if [ -n "$(git status --porcelain)" ]; then
        print_warning "You have uncommitted changes:"
        git status --short
        echo ""
        read -p "Do you want to continue anyway? (y/N) " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            print_info "Aborting. Please commit your changes first."
            exit 1
        fi
    else
        print_success "Working directory is clean"
    fi
}

# Function to get version from VERSION file
get_version() {
    if [ -f "VERSION" ]; then
        VERSION=$(cat VERSION | tr -d '[:space:]')
        print_info "Version from VERSION file: $VERSION"
    else
        print_error "VERSION file not found"
        exit 1
    fi
}

# Function to validate version format
validate_version() {
    if [[ ! $VERSION =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
        print_error "Invalid version format: $VERSION"
        print_info "Version must be in format: MAJOR.MINOR.PATCH (e.g., 1.0.0)"
        exit 1
    fi
    print_success "Version format is valid"
}

# Function to check if tag already exists
check_tag_exists() {
    TAG="v$VERSION"
    if git rev-parse "$TAG" >/dev/null 2>&1; then
        print_error "Tag $TAG already exists"
        print_info "Existing tags:"
        git tag -l "v*"
        exit 1
    fi
    print_success "Tag $TAG does not exist yet"
}

# Function to verify release files exist
check_release_files() {
    local missing_files=()
    
    if [ ! -f "RELEASE_NOTES.md" ]; then
        missing_files+=("RELEASE_NOTES.md")
    fi
    
    if [ ! -f "CHANGELOG.md" ]; then
        missing_files+=("CHANGELOG.md")
    fi
    
    if [ ! -f "VERSION" ]; then
        missing_files+=("VERSION")
    fi
    
    if [ ${#missing_files[@]} -gt 0 ]; then
        print_error "Missing required files:"
        for file in "${missing_files[@]}"; do
            echo "  - $file"
        done
        exit 1
    fi
    
    print_success "All required release files exist"
}

# Function to verify version in files
verify_version_in_files() {
    local files_to_check=("README.md" "CHANGELOG.md" "RELEASE_NOTES.md")
    local missing_version=()
    
    for file in "${files_to_check[@]}"; do
        if [ -f "$file" ]; then
            if ! grep -q "$VERSION" "$file"; then
                missing_version+=("$file")
            fi
        fi
    done
    
    if [ ${#missing_version[@]} -gt 0 ]; then
        print_warning "Version $VERSION not found in:"
        for file in "${missing_version[@]}"; do
            echo "  - $file"
        done
        echo ""
        read -p "Continue anyway? (y/N) " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            print_info "Aborting. Please update version in all files."
            exit 1
        fi
    else
        print_success "Version found in all documentation files"
    fi
}

# Function to show release summary
show_summary() {
    echo ""
    echo "═══════════════════════════════════════════════════════════"
    echo "  Release Summary"
    echo "═══════════════════════════════════════════════════════════"
    echo ""
    echo "  Version:     $VERSION"
    echo "  Tag:         v$VERSION"
    echo "  Branch:      $(git branch --show-current)"
    echo "  Commit:      $(git rev-parse --short HEAD)"
    echo ""
    
    if [ -f "RELEASE_NOTES.md" ]; then
        echo "  Release Notes Preview:"
        echo "  ─────────────────────────────────────────────────────────"
        head -n 10 RELEASE_NOTES.md | sed 's/^/  /'
        echo "  ..."
        echo ""
    fi
    
    echo "═══════════════════════════════════════════════════════════"
    echo ""
}

# Function to create the tag
create_tag() {
    TAG="v$VERSION"
    
    # Create tag message from RELEASE_NOTES.md
    if [ -f "RELEASE_NOTES.md" ]; then
        TAG_MESSAGE="Release version $VERSION

$(head -n 50 RELEASE_NOTES.md)"
    else
        TAG_MESSAGE="Release version $VERSION"
    fi
    
    print_info "Creating annotated tag: $TAG"
    
    if git tag -a "$TAG" -m "$TAG_MESSAGE"; then
        print_success "Tag created successfully"
    else
        print_error "Failed to create tag"
        exit 1
    fi
}

# Function to show next steps
show_next_steps() {
    echo ""
    echo "═══════════════════════════════════════════════════════════"
    echo "  Next Steps"
    echo "═══════════════════════════════════════════════════════════"
    echo ""
    echo "  1. Review the tag:"
    echo "     git show v$VERSION"
    echo ""
    echo "  2. Push the tag to trigger release workflow:"
    echo "     git push origin v$VERSION"
    echo ""
    echo "  3. Monitor GitHub Actions:"
    echo "     https://github.com/[your-repo]/actions"
    echo ""
    echo "  4. Verify release artifacts:"
    echo "     - DMG file created"
    echo "     - Checksums generated"
    echo "     - GitHub Release created"
    echo ""
    echo "  5. Test the release:"
    echo "     - Download DMG"
    echo "     - Install on clean system"
    echo "     - Verify functionality"
    echo ""
    echo "  If you need to delete the tag:"
    echo "     git tag -d v$VERSION"
    echo ""
    echo "═══════════════════════════════════════════════════════════"
    echo ""
}

# Main script
main() {
    echo ""
    echo "═══════════════════════════════════════════════════════════"
    echo "  IPTV Player macOS - Release Tag Creator"
    echo "═══════════════════════════════════════════════════════════"
    echo ""
    
    # Run checks
    print_info "Running pre-flight checks..."
    check_directory
    check_git_status
    get_version
    validate_version
    check_tag_exists
    check_release_files
    verify_version_in_files
    
    # Show summary
    show_summary
    
    # Confirm
    read -p "Create release tag v$VERSION? (y/N) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        print_info "Aborting."
        exit 0
    fi
    
    # Create tag
    create_tag
    
    # Show next steps
    show_next_steps
    
    print_success "Release tag created successfully!"
}

# Run main function
main
