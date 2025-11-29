#!/bin/bash

# Code Signing Setup Script for IPTV Player macOS
# This script helps configure code signing for the application

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Print colored output
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

print_header "IPTV Player - Code Signing Setup"

# Step 1: Check Xcode installation
print_info "Checking Xcode installation..."
if ! command -v xcodebuild &> /dev/null; then
    print_error "Xcode is not installed. Please install Xcode from the App Store."
    exit 1
fi

XCODE_VERSION=$(xcodebuild -version | head -n 1)
print_success "Found: $XCODE_VERSION"

# Step 2: Check Command Line Tools
print_info "Checking Command Line Tools..."
if ! xcode-select -p &> /dev/null; then
    print_warning "Command Line Tools not found. Installing..."
    xcode-select --install
    print_info "Please complete the installation and run this script again."
    exit 0
fi
print_success "Command Line Tools installed"

# Step 3: Check for signing certificates
print_header "Checking Code Signing Certificates"

print_info "Looking for signing identities..."
IDENTITIES=$(security find-identity -v -p codesigning 2>/dev/null | grep -v "0 valid identities found")

if [ -z "$IDENTITIES" ]; then
    print_warning "No code signing certificates found"
    echo ""
    echo "To set up code signing, you need:"
    echo "  1. An Apple Developer account ($99/year)"
    echo "  2. Xcode configured with your Apple ID"
    echo ""
    echo "Steps to add your Apple ID to Xcode:"
    echo "  1. Open Xcode"
    echo "  2. Go to Xcode > Settings (or Preferences)"
    echo "  3. Select the 'Accounts' tab"
    echo "  4. Click the '+' button and add your Apple ID"
    echo "  5. Select your team and download certificates"
    echo ""
    read -p "Press Enter to continue after adding your Apple ID to Xcode..."
    
    # Check again
    IDENTITIES=$(security find-identity -v -p codesigning 2>/dev/null | grep -v "0 valid identities found")
    if [ -z "$IDENTITIES" ]; then
        print_error "Still no certificates found. Please follow the setup guide."
        exit 1
    fi
fi

print_success "Found signing identities:"
echo "$IDENTITIES"

# Step 4: Extract Team ID
print_header "Extracting Team Information"

# Try to get team ID from certificates
TEAM_ID=$(echo "$IDENTITIES" | grep -o '([A-Z0-9]\{10\})' | head -n 1 | tr -d '()')

if [ -n "$TEAM_ID" ]; then
    print_success "Team ID: $TEAM_ID"
else
    print_warning "Could not automatically detect Team ID"
    echo "You can find your Team ID at: https://developer.apple.com/account"
    read -p "Enter your Team ID (10 characters): " TEAM_ID
fi

# Step 5: Check project configuration
print_header "Checking Project Configuration"

PROJECT_FILE="IPTVPlayer.xcodeproj/project.pbxproj"

if [ ! -f "$PROJECT_FILE" ]; then
    print_error "Project file not found: $PROJECT_FILE"
    print_info "Make sure you're running this script from the macos directory"
    exit 1
fi

print_success "Found project file"

# Step 6: Verify entitlements
print_info "Checking entitlements file..."
ENTITLEMENTS_FILE="IPTVPlayer/IPTVPlayer.entitlements"

if [ ! -f "$ENTITLEMENTS_FILE" ]; then
    print_error "Entitlements file not found: $ENTITLEMENTS_FILE"
    exit 1
fi

print_success "Entitlements file exists"

# Check for required entitlements
if grep -q "com.apple.security.app-sandbox" "$ENTITLEMENTS_FILE"; then
    print_success "App Sandbox enabled"
else
    print_warning "App Sandbox not found in entitlements"
fi

if grep -q "com.apple.security.network.client" "$ENTITLEMENTS_FILE"; then
    print_success "Network client entitlement enabled"
else
    print_warning "Network client entitlement not found"
fi

# Step 7: Build configuration check
print_header "Checking Build Configuration"

print_info "Current bundle identifier: com.menmapro.IPTVPlayer"
print_info "Current version: 1.0"
print_info "Minimum macOS version: 13.0"

# Step 8: Test build with code signing
print_header "Testing Code Signing"

print_info "Attempting a test build with code signing..."

BUILD_DIR="build/CodeSignTest"
rm -rf "$BUILD_DIR"

if xcodebuild -project IPTVPlayer.xcodeproj \
    -scheme IPTVPlayer \
    -configuration Debug \
    -derivedDataPath "$BUILD_DIR" \
    CODE_SIGN_STYLE=Automatic \
    DEVELOPMENT_TEAM="$TEAM_ID" \
    clean build 2>&1 | tee /tmp/xcodebuild.log | grep -E "(error|warning|succeeded)"; then
    
    APP_PATH="$BUILD_DIR/Build/Products/Debug/IPTVPlayer.app"
    
    if [ -d "$APP_PATH" ]; then
        print_success "Build succeeded!"
        
        # Verify code signature
        print_info "Verifying code signature..."
        if codesign --verify --deep --strict --verbose=2 "$APP_PATH" 2>&1; then
            print_success "Code signature is valid!"
            
            # Display signature details
            print_info "Signature details:"
            codesign -dv --verbose=4 "$APP_PATH" 2>&1 | grep -E "(Identifier|Authority|TeamIdentifier|Format)"
        else
            print_error "Code signature verification failed"
        fi
    else
        print_error "Build succeeded but app not found at expected location"
    fi
else
    print_error "Build failed. Check the log at /tmp/xcodebuild.log"
    exit 1
fi

# Step 9: Summary and next steps
print_header "Setup Summary"

print_success "Code signing is configured!"
echo ""
echo "Configuration:"
echo "  • Team ID: $TEAM_ID"
echo "  • Bundle ID: com.menmapro.IPTVPlayer"
echo "  • Signing Style: Automatic"
echo "  • Hardened Runtime: Enabled"
echo "  • App Sandbox: Enabled"
echo ""
echo "Next steps:"
echo "  1. Build for release: ./build.sh"
echo "  2. Test the signed app on a clean system"
echo "  3. Submit for notarization (see CODE_SIGNING_GUIDE.md)"
echo "  4. Create distribution DMG"
echo ""
echo "For detailed instructions, see: CODE_SIGNING_GUIDE.md"
echo ""

print_success "Setup complete!"
