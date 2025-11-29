#!/bin/bash

# Notarization Script for IPTV Player macOS
# This script handles the notarization process for distribution

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

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

print_header() {
    echo ""
    echo -e "${BLUE}═══════════════════════════════════════════════════════${NC}"
    echo -e "${BLUE}  $1${NC}"
    echo -e "${BLUE}═══════════════════════════════════════════════════════${NC}"
    echo ""
}

# Check if running on macOS
if [[ "$OSTYPE" != "darwin"* ]]; then
    print_error "This script must be run on macOS"
    exit 1
fi

print_header "IPTV Player - Notarization"

# Configuration
APP_NAME="IPTVPlayer"
APP_PATH="build/Build/Products/Release/${APP_NAME}.app"
DMG_NAME="${APP_NAME}.dmg"
KEYCHAIN_PROFILE="IPTVPlayer-Notarization"

# Check if app exists
if [ ! -d "$APP_PATH" ]; then
    print_error "App not found at: $APP_PATH"
    print_info "Please build the release version first:"
    print_info "  xcodebuild -project IPTVPlayer.xcodeproj -scheme IPTVPlayer -configuration Release"
    exit 1
fi

print_success "Found app at: $APP_PATH"

# Step 1: Verify code signature
print_header "Step 1: Verifying Code Signature"

print_info "Checking code signature..."
if codesign --verify --deep --strict --verbose=2 "$APP_PATH" 2>&1; then
    print_success "Code signature is valid"
else
    print_error "Code signature verification failed"
    print_info "Please ensure the app is properly signed"
    exit 1
fi

# Display signature info
print_info "Signature details:"
codesign -dv --verbose=4 "$APP_PATH" 2>&1 | grep -E "(Identifier|Authority|TeamIdentifier)"

# Step 2: Check for notarization credentials
print_header "Step 2: Checking Notarization Credentials"

# Check if keychain profile exists
if security find-generic-password -s "AC_PASSWORD" -a "$KEYCHAIN_PROFILE" &> /dev/null; then
    print_success "Keychain profile '$KEYCHAIN_PROFILE' found"
else
    print_warning "Keychain profile not found"
    echo ""
    echo "To set up notarization credentials:"
    echo "  1. Create an app-specific password at https://appleid.apple.com"
    echo "  2. Run the following command:"
    echo ""
    echo "     xcrun notarytool store-credentials \"$KEYCHAIN_PROFILE\" \\"
    echo "         --apple-id \"your.email@example.com\" \\"
    echo "         --team-id \"YOUR_TEAM_ID\" \\"
    echo "         --password \"app-specific-password\""
    echo ""
    read -p "Have you set up the credentials? (y/n) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        print_error "Please set up notarization credentials first"
        exit 1
    fi
fi

# Step 3: Create DMG
print_header "Step 3: Creating DMG"

print_info "Creating disk image..."

# Remove old DMG if exists
rm -f "$DMG_NAME"

# Create temporary directory for DMG contents
TMP_DMG_DIR=$(mktemp -d)
cp -R "$APP_PATH" "$TMP_DMG_DIR/"

# Create Applications symlink
ln -s /Applications "$TMP_DMG_DIR/Applications"

# Create DMG
hdiutil create -volname "$APP_NAME" \
    -srcfolder "$TMP_DMG_DIR" \
    -ov -format UDZO \
    "$DMG_NAME"

# Clean up
rm -rf "$TMP_DMG_DIR"

if [ -f "$DMG_NAME" ]; then
    print_success "DMG created: $DMG_NAME"
    DMG_SIZE=$(du -h "$DMG_NAME" | cut -f1)
    print_info "Size: $DMG_SIZE"
else
    print_error "Failed to create DMG"
    exit 1
fi

# Step 4: Submit for notarization
print_header "Step 4: Submitting for Notarization"

print_info "Uploading to Apple for notarization..."
print_warning "This may take several minutes..."

# Submit and wait for result
SUBMIT_OUTPUT=$(xcrun notarytool submit "$DMG_NAME" \
    --keychain-profile "$KEYCHAIN_PROFILE" \
    --wait 2>&1)

echo "$SUBMIT_OUTPUT"

# Extract submission ID
SUBMISSION_ID=$(echo "$SUBMIT_OUTPUT" | grep "id:" | head -n 1 | awk '{print $2}')

if [ -z "$SUBMISSION_ID" ]; then
    print_error "Failed to get submission ID"
    exit 1
fi

print_info "Submission ID: $SUBMISSION_ID"

# Check if notarization succeeded
if echo "$SUBMIT_OUTPUT" | grep -q "status: Accepted"; then
    print_success "Notarization succeeded!"
else
    print_error "Notarization failed"
    print_info "Fetching detailed log..."
    xcrun notarytool log "$SUBMISSION_ID" \
        --keychain-profile "$KEYCHAIN_PROFILE"
    exit 1
fi

# Step 5: Staple the ticket
print_header "Step 5: Stapling Notarization Ticket"

print_info "Stapling ticket to DMG..."
if xcrun stapler staple "$DMG_NAME" 2>&1; then
    print_success "Ticket stapled successfully"
else
    print_error "Failed to staple ticket"
    exit 1
fi

# Verify stapling
print_info "Verifying stapled ticket..."
if xcrun stapler validate "$DMG_NAME" 2>&1; then
    print_success "Stapled ticket is valid"
else
    print_error "Stapled ticket validation failed"
    exit 1
fi

# Step 6: Verify Gatekeeper
print_header "Step 6: Verifying Gatekeeper"

print_info "Checking Gatekeeper assessment..."
if spctl -a -vvv -t install "$DMG_NAME" 2>&1 | grep -q "accepted"; then
    print_success "Gatekeeper accepts the DMG"
else
    print_warning "Gatekeeper assessment failed"
fi

# Step 7: Summary
print_header "Notarization Complete!"

print_success "Your app is ready for distribution!"
echo ""
echo "Distribution package: $DMG_NAME"
echo "Submission ID: $SUBMISSION_ID"
echo ""
echo "Next steps:"
echo "  1. Test the DMG on a clean macOS system"
echo "  2. Upload to your distribution channel"
echo "  3. Update release notes"
echo ""
echo "To verify on another Mac:"
echo "  spctl -a -vvv -t install $DMG_NAME"
echo ""

print_success "Done!"
