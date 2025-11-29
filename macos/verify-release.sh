#!/bin/bash

# IPTV Player macOS - Release Verification Script
# This script verifies that a release is ready for distribution

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Counters
PASSED=0
FAILED=0
WARNINGS=0

# Function to print colored output
print_info() {
    echo -e "${BLUE}ℹ ${NC}$1"
}

print_success() {
    echo -e "${GREEN}✓ ${NC}$1"
    ((PASSED++))
}

print_warning() {
    echo -e "${YELLOW}⚠ ${NC}$1"
    ((WARNINGS++))
}

print_error() {
    echo -e "${RED}✗ ${NC}$1"
    ((FAILED++))
}

# Function to check if we're in the right directory
check_directory() {
    if [ ! -f "IPTVPlayer.xcodeproj/project.pbxproj" ]; then
        echo "This script must be run from the macos/ directory"
        exit 1
    fi
}

# Function to get version
get_version() {
    if [ -f "VERSION" ]; then
        VERSION=$(cat VERSION | tr -d '[:space:]')
    else
        VERSION="unknown"
    fi
}

# Header
print_header() {
    echo ""
    echo "═══════════════════════════════════════════════════════════"
    echo "  IPTV Player macOS - Release Verification"
    echo "  Version: $VERSION"
    echo "═══════════════════════════════════════════════════════════"
    echo ""
}

# Check documentation files
check_documentation() {
    echo "Checking Documentation..."
    echo "─────────────────────────────────────────────────────────"
    
    local required_files=(
        "README.md"
        "RELEASE_NOTES.md"
        "CHANGELOG.md"
        "VERSION"
        "USER_GUIDE.md"
    )
    
    for file in "${required_files[@]}"; do
        if [ -f "$file" ]; then
            print_success "$file exists"
        else
            print_error "$file is missing"
        fi
    done
    
    # Check if version is mentioned in docs
    if [ -f "README.md" ] && grep -q "$VERSION" "README.md"; then
        print_success "Version $VERSION found in README.md"
    else
        print_warning "Version $VERSION not found in README.md"
    fi
    
    if [ -f "CHANGELOG.md" ] && grep -q "$VERSION" "CHANGELOG.md"; then
        print_success "Version $VERSION found in CHANGELOG.md"
    else
        print_error "Version $VERSION not found in CHANGELOG.md"
    fi
    
    if [ -f "RELEASE_NOTES.md" ] && grep -q "$VERSION" "RELEASE_NOTES.md"; then
        print_success "Version $VERSION found in RELEASE_NOTES.md"
    else
        print_error "Version $VERSION not found in RELEASE_NOTES.md"
    fi
    
    echo ""
}

# Check project configuration
check_project_config() {
    echo "Checking Project Configuration..."
    echo "─────────────────────────────────────────────────────────"
    
    # Check if project file exists
    if [ -f "IPTVPlayer.xcodeproj/project.pbxproj" ]; then
        print_success "Xcode project file exists"
    else
        print_error "Xcode project file not found"
    fi
    
    # Check Info.plist
    if [ -f "IPTVPlayer/Info.plist" ]; then
        print_success "Info.plist exists"
    else
        print_error "Info.plist not found"
    fi
    
    # Check entitlements
    if [ -f "IPTVPlayer/IPTVPlayer.entitlements" ]; then
        print_success "Entitlements file exists"
    else
        print_warning "Entitlements file not found"
    fi
    
    echo ""
}

# Check source files
check_source_files() {
    echo "Checking Source Files..."
    echo "─────────────────────────────────────────────────────────"
    
    local required_dirs=(
        "IPTVPlayer/App"
        "IPTVPlayer/Views"
        "IPTVPlayer/ViewModels"
        "IPTVPlayer/Services"
        "IPTVPlayer/Models"
    )
    
    for dir in "${required_dirs[@]}"; do
        if [ -d "$dir" ]; then
            local file_count=$(find "$dir" -name "*.swift" | wc -l)
            print_success "$dir exists ($file_count Swift files)"
        else
            print_error "$dir not found"
        fi
    done
    
    echo ""
}

# Check test files
check_tests() {
    echo "Checking Tests..."
    echo "─────────────────────────────────────────────────────────"
    
    if [ -d "IPTVPlayerTests" ]; then
        local test_count=$(find IPTVPlayerTests -name "*Test*.swift" | wc -l)
        print_success "Test directory exists ($test_count test files)"
        
        # List test files
        if [ $test_count -gt 0 ]; then
            find IPTVPlayerTests -name "*Test*.swift" -exec basename {} \; | while read test_file; do
                print_info "  - $test_file"
            done
        fi
    else
        print_warning "Test directory not found"
    fi
    
    echo ""
}

# Check CI/CD configuration
check_cicd() {
    echo "Checking CI/CD Configuration..."
    echo "─────────────────────────────────────────────────────────"
    
    if [ -f "../.github/workflows/macos-ci.yml" ]; then
        print_success "GitHub Actions workflow exists"
    else
        print_warning "GitHub Actions workflow not found"
    fi
    
    if [ -f "ci-test.sh" ]; then
        print_success "CI test script exists"
        if [ -x "ci-test.sh" ]; then
            print_success "CI test script is executable"
        else
            print_warning "CI test script is not executable"
        fi
    else
        print_warning "CI test script not found"
    fi
    
    echo ""
}

# Check build artifacts
check_build_artifacts() {
    echo "Checking Build Artifacts..."
    echo "─────────────────────────────────────────────────────────"
    
    if [ -d "build" ]; then
        print_info "Build directory exists"
        
        # Check for DMG
        local dmg_count=$(find build -name "*.dmg" 2>/dev/null | wc -l)
        if [ $dmg_count -gt 0 ]; then
            print_success "Found $dmg_count DMG file(s)"
            find build -name "*.dmg" -exec basename {} \; | while read dmg_file; do
                print_info "  - $dmg_file"
            done
        else
            print_warning "No DMG files found (run build first)"
        fi
        
        # Check for checksums
        if [ -f "build/checksums.txt" ]; then
            print_success "Checksums file exists"
        else
            print_warning "Checksums file not found"
        fi
    else
        print_warning "Build directory not found (run build first)"
    fi
    
    echo ""
}

# Check git status
check_git() {
    echo "Checking Git Status..."
    echo "─────────────────────────────────────────────────────────"
    
    # Check if in git repo
    if git rev-parse --git-dir > /dev/null 2>&1; then
        print_success "In git repository"
        
        # Check current branch
        local branch=$(git branch --show-current)
        print_info "Current branch: $branch"
        
        # Check for uncommitted changes
        if [ -n "$(git status --porcelain)" ]; then
            print_warning "Uncommitted changes detected"
            git status --short | head -n 5
        else
            print_success "No uncommitted changes"
        fi
        
        # Check for tag
        if git rev-parse "v$VERSION" >/dev/null 2>&1; then
            print_success "Tag v$VERSION exists"
        else
            print_warning "Tag v$VERSION does not exist yet"
        fi
        
        # Check remote
        if git remote -v | grep -q origin; then
            print_success "Remote 'origin' configured"
        else
            print_warning "Remote 'origin' not configured"
        fi
    else
        print_error "Not in a git repository"
    fi
    
    echo ""
}

# Check code signing
check_code_signing() {
    echo "Checking Code Signing..."
    echo "─────────────────────────────────────────────────────────"
    
    # Check for signing scripts
    if [ -f "setup-code-signing.sh" ]; then
        print_success "Code signing setup script exists"
    else
        print_warning "Code signing setup script not found"
    fi
    
    if [ -f "notarize.sh" ]; then
        print_success "Notarization script exists"
    else
        print_warning "Notarization script not found"
    fi
    
    # Check for built app
    if [ -d "build/export/IPTVPlayer.app" ]; then
        print_info "Checking app signature..."
        if codesign --verify --deep --strict build/export/IPTVPlayer.app 2>/dev/null; then
            print_success "App is properly signed"
            codesign -dv build/export/IPTVPlayer.app 2>&1 | grep "Authority" | head -n 1 | sed 's/^/  /'
        else
            print_warning "App is not signed or signature is invalid"
        fi
    else
        print_warning "Built app not found (run build first)"
    fi
    
    echo ""
}

# Check security
check_security() {
    echo "Checking Security Implementation..."
    echo "─────────────────────────────────────────────────────────"
    
    # Check for security-related files
    if [ -f "IPTVPlayer/Services/KeychainManager.swift" ]; then
        print_success "KeychainManager exists"
    else
        print_warning "KeychainManager not found"
    fi
    
    if [ -f "IPTVPlayer/Services/InputValidator.swift" ]; then
        print_success "InputValidator exists"
    else
        print_warning "InputValidator not found"
    fi
    
    # Check for hardcoded secrets (basic check)
    if grep -r "password.*=.*\"" IPTVPlayer/ --include="*.swift" | grep -v "// " | grep -v "placeholder" > /dev/null; then
        print_warning "Possible hardcoded passwords found (review manually)"
    else
        print_success "No obvious hardcoded passwords found"
    fi
    
    echo ""
}

# Summary
print_summary() {
    echo "═══════════════════════════════════════════════════════════"
    echo "  Verification Summary"
    echo "═══════════════════════════════════════════════════════════"
    echo ""
    echo "  Passed:   $PASSED"
    echo "  Warnings: $WARNINGS"
    echo "  Failed:   $FAILED"
    echo ""
    
    if [ $FAILED -eq 0 ]; then
        if [ $WARNINGS -eq 0 ]; then
            print_success "All checks passed! Release is ready."
        else
            print_warning "All critical checks passed, but there are warnings."
            echo "  Review warnings before proceeding with release."
        fi
    else
        print_error "Some checks failed. Fix issues before releasing."
    fi
    
    echo ""
    echo "═══════════════════════════════════════════════════════════"
    echo ""
}

# Main script
main() {
    check_directory
    get_version
    print_header
    
    check_documentation
    check_project_config
    check_source_files
    check_tests
    check_cicd
    check_build_artifacts
    check_git
    check_code_signing
    check_security
    
    print_summary
    
    # Exit with error if any checks failed
    if [ $FAILED -gt 0 ]; then
        exit 1
    fi
}

# Run main function
main
