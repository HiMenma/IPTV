#!/bin/bash

# Local DMG Creation and Testing Script
# This script replicates the CI/CD workflow for creating a DMG locally

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BUILD_DIR="${PROJECT_DIR}/build"
ARCHIVE_PATH="${BUILD_DIR}/IPTVPlayer.xcarchive"
EXPORT_PATH="${BUILD_DIR}/export"
PROJECT_FILE="${PROJECT_DIR}/IPTVPlayer.xcodeproj"
SCHEME="IPTVPlayer"
CONFIGURATION="Release"

# Get version from Info.plist
VERSION=$(defaults read "${PROJECT_DIR}/IPTVPlayer/Info.plist" CFBundleShortVersionString 2>/dev/null || echo "1.0.0")
DMG_NAME="IPTVPlayer-${VERSION}.dmg"
DMG_PATH="${BUILD_DIR}/${DMG_NAME}"

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}IPTV Player - Local DMG Creation${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""
echo -e "${GREEN}Version:${NC} ${VERSION}"
echo -e "${GREEN}Build Directory:${NC} ${BUILD_DIR}"
echo -e "${GREEN}DMG Output:${NC} ${DMG_PATH}"
echo ""

# Function to print step headers
print_step() {
    echo ""
    echo -e "${BLUE}==>${NC} ${1}"
}

# Function to print success messages
print_success() {
    echo -e "${GREEN}✓${NC} ${1}"
}

# Function to print error messages
print_error() {
    echo -e "${RED}✗${NC} ${1}"
}

# Function to print warning messages
print_warning() {
    echo -e "${YELLOW}⚠${NC} ${1}"
}

# Check prerequisites
print_step "Checking prerequisites..."

if ! command -v xcodebuild &> /dev/null; then
    print_error "xcodebuild not found. Please install Xcode."
    exit 1
fi
print_success "xcodebuild found"

# Check for create-dmg
if ! command -v create-dmg &> /dev/null; then
    print_warning "create-dmg not found. Will use hdiutil as fallback."
    print_warning "To install create-dmg: brew install create-dmg"
    USE_HDIUTIL=true
else
    print_success "create-dmg found"
    USE_HDIUTIL=false
fi

# Show Xcode version
XCODE_VERSION=$(xcodebuild -version | head -n 1)
print_success "Using ${XCODE_VERSION}"

# Clean previous build
print_step "Cleaning previous build..."
if [ -d "${BUILD_DIR}" ]; then
    rm -rf "${BUILD_DIR}"
    print_success "Removed previous build directory"
fi
mkdir -p "${BUILD_DIR}"
print_success "Created fresh build directory"

# Resolve dependencies
print_step "Resolving Swift Package dependencies..."
xcodebuild \
    -project "${PROJECT_FILE}" \
    -scheme "${SCHEME}" \
    -resolvePackageDependencies
print_success "Dependencies resolved"

# Build and archive
print_step "Building and archiving (${CONFIGURATION})..."
echo "This may take a few minutes..."
xcodebuild \
    -project "${PROJECT_FILE}" \
    -scheme "${SCHEME}" \
    -configuration "${CONFIGURATION}" \
    -destination 'platform=macOS' \
    -archivePath "${ARCHIVE_PATH}" \
    archive \
    CODE_SIGN_IDENTITY="" \
    CODE_SIGNING_REQUIRED=NO \
    CODE_SIGNING_ALLOWED=NO \
    | grep -E "^\*\*|error:|warning:|note:" || true

if [ ! -d "${ARCHIVE_PATH}" ]; then
    print_error "Archive creation failed"
    exit 1
fi
print_success "Archive created successfully"

# Export archive
print_step "Exporting application..."
mkdir -p "${EXPORT_PATH}"

# Create export options plist
EXPORT_OPTIONS="${BUILD_DIR}/ExportOptions.plist"
cat > "${EXPORT_OPTIONS}" << EOF
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>method</key>
    <string>mac-application</string>
    <key>signingStyle</key>
    <string>manual</string>
    <key>stripSwiftSymbols</key>
    <true/>
</dict>
</plist>
EOF

# Try to export, fallback to copying if it fails
if xcodebuild \
    -exportArchive \
    -archivePath "${ARCHIVE_PATH}" \
    -exportPath "${EXPORT_PATH}" \
    -exportOptionsPlist "${EXPORT_OPTIONS}" \
    CODE_SIGN_IDENTITY="" \
    CODE_SIGNING_REQUIRED=NO \
    CODE_SIGNING_ALLOWED=NO 2>/dev/null; then
    print_success "Archive exported successfully"
else
    print_warning "Export failed, copying app directly from archive"
    cp -R "${ARCHIVE_PATH}/Products/Applications/IPTVPlayer.app" "${EXPORT_PATH}/"
    print_success "App copied from archive"
fi

APP_PATH="${EXPORT_PATH}/IPTVPlayer.app"
if [ ! -d "${APP_PATH}" ]; then
    print_error "Application not found at ${APP_PATH}"
    exit 1
fi

# Verify app structure
print_step "Verifying application structure..."
if [ -f "${APP_PATH}/Contents/MacOS/IPTVPlayer" ]; then
    print_success "Executable found"
else
    print_error "Executable not found"
    exit 1
fi

if [ -f "${APP_PATH}/Contents/Info.plist" ]; then
    print_success "Info.plist found"
    APP_VERSION=$(defaults read "${APP_PATH}/Contents/Info.plist" CFBundleShortVersionString 2>/dev/null || echo "unknown")
    print_success "App version: ${APP_VERSION}"
else
    print_error "Info.plist not found"
    exit 1
fi

# Create DMG
print_step "Creating DMG..."

if [ "${USE_HDIUTIL}" = true ]; then
    # Use hdiutil (built-in macOS tool)
    print_warning "Using hdiutil to create DMG"
    hdiutil create \
        -volname "IPTV Player" \
        -srcfolder "${APP_PATH}" \
        -ov \
        -format UDZO \
        "${DMG_PATH}"
else
    # Use create-dmg (better looking DMG)
    create-dmg \
        --volname "IPTV Player" \
        --window-pos 200 120 \
        --window-size 800 400 \
        --icon-size 100 \
        --icon "IPTVPlayer.app" 200 190 \
        --hide-extension "IPTVPlayer.app" \
        --app-drop-link 600 185 \
        --no-internet-enable \
        "${DMG_PATH}" \
        "${APP_PATH}" 2>&1 | grep -v "^hdiutil:" || true
fi

if [ ! -f "${DMG_PATH}" ]; then
    print_error "DMG creation failed"
    exit 1
fi
print_success "DMG created successfully"

# Get DMG size
DMG_SIZE=$(du -h "${DMG_PATH}" | cut -f1)
print_success "DMG size: ${DMG_SIZE}"

# Calculate checksum
print_step "Calculating checksum..."
CHECKSUM=$(shasum -a 256 "${DMG_PATH}" | cut -d' ' -f1)
echo "${CHECKSUM}  ${DMG_NAME}" > "${BUILD_DIR}/checksums.txt"
print_success "SHA-256: ${CHECKSUM}"

# Verify DMG can be mounted
print_step "Verifying DMG can be mounted..."
MOUNT_POINT=$(mktemp -d)
if hdiutil attach "${DMG_PATH}" -mountpoint "${MOUNT_POINT}" -nobrowse -quiet; then
    print_success "DMG mounted successfully at ${MOUNT_POINT}"
    
    # Check if app exists in mounted DMG
    if [ -d "${MOUNT_POINT}/IPTVPlayer.app" ]; then
        print_success "Application found in DMG"
        
        # Verify app can be read
        if [ -f "${MOUNT_POINT}/IPTVPlayer.app/Contents/MacOS/IPTVPlayer" ]; then
            print_success "Application executable is accessible"
        else
            print_error "Application executable not accessible"
        fi
    else
        print_error "Application not found in DMG"
    fi
    
    # Unmount
    hdiutil detach "${MOUNT_POINT}" -quiet
    print_success "DMG unmounted successfully"
    rmdir "${MOUNT_POINT}"
else
    print_error "Failed to mount DMG"
    exit 1
fi

# Test installation simulation
print_step "Testing installation simulation..."
TEST_INSTALL_DIR=$(mktemp -d)
print_warning "Simulating installation to: ${TEST_INSTALL_DIR}"

# Mount DMG again
MOUNT_POINT=$(mktemp -d)
hdiutil attach "${DMG_PATH}" -mountpoint "${MOUNT_POINT}" -nobrowse -quiet

# Copy app to test location
cp -R "${MOUNT_POINT}/IPTVPlayer.app" "${TEST_INSTALL_DIR}/"
print_success "Application copied to test location"

# Unmount
hdiutil detach "${MOUNT_POINT}" -quiet
rmdir "${MOUNT_POINT}"

# Verify copied app
if [ -d "${TEST_INSTALL_DIR}/IPTVPlayer.app" ]; then
    print_success "Installation simulation successful"
    
    # Check if app is executable
    if [ -x "${TEST_INSTALL_DIR}/IPTVPlayer.app/Contents/MacOS/IPTVPlayer" ]; then
        print_success "Application is executable"
    else
        print_warning "Application may not be executable (expected for unsigned builds)"
    fi
else
    print_error "Installation simulation failed"
fi

# Cleanup test installation
rm -rf "${TEST_INSTALL_DIR}"
print_success "Test installation cleaned up"

# Summary
echo ""
echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Build Summary${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""
echo -e "${GREEN}✓ DMG created successfully${NC}"
echo -e "  Location: ${DMG_PATH}"
echo -e "  Size: ${DMG_SIZE}"
echo -e "  SHA-256: ${CHECKSUM}"
echo ""
echo -e "${GREEN}Next steps:${NC}"
echo -e "  1. Test the DMG by opening it: open ${DMG_PATH}"
echo -e "  2. Drag the app to Applications folder"
echo -e "  3. Try launching the app from Applications"
echo -e "  4. If you see security warnings, go to System Settings > Privacy & Security"
echo ""
echo -e "${YELLOW}Note:${NC} This is an unsigned build for testing only."
echo -e "      For distribution, you'll need to sign and notarize the app."
echo ""

