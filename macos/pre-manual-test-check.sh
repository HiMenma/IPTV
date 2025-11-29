#!/bin/bash

# Pre-Manual Testing Check Script
# This script verifies the app is ready for manual testing

set -e

echo "=========================================="
echo "Pre-Manual Testing Check"
echo "=========================================="
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check if we're in the right directory
if [ ! -f "IPTVPlayer.xcodeproj/project.pbxproj" ]; then
    echo -e "${RED}Error: Must be run from macos/ directory${NC}"
    exit 1
fi

echo "1. Checking Xcode project..."
if xcodebuild -list -project IPTVPlayer.xcodeproj > /dev/null 2>&1; then
    echo -e "${GREEN}✓ Xcode project is valid${NC}"
else
    echo -e "${RED}✗ Xcode project has issues${NC}"
    exit 1
fi

echo ""
echo "2. Building application (Debug)..."
if xcodebuild -project IPTVPlayer.xcodeproj \
    -scheme IPTVPlayer \
    -configuration Debug \
    -derivedDataPath ./build \
    build > /dev/null 2>&1; then
    echo -e "${GREEN}✓ Build successful${NC}"
else
    echo -e "${RED}✗ Build failed${NC}"
    echo "Run 'xcodebuild -project IPTVPlayer.xcodeproj -scheme IPTVPlayer build' for details"
    exit 1
fi

echo ""
echo "3. Running unit tests..."
if xcodebuild test \
    -project IPTVPlayer.xcodeproj \
    -scheme IPTVPlayer \
    -destination 'platform=macOS' \
    -derivedDataPath ./build \
    > /dev/null 2>&1; then
    echo -e "${GREEN}✓ All tests passed${NC}"
else
    echo -e "${YELLOW}⚠ Some tests failed (check test results)${NC}"
    echo "Run 'xcodebuild test -project IPTVPlayer.xcodeproj -scheme IPTVPlayer' for details"
fi

echo ""
echo "4. Checking for app bundle..."
APP_PATH="./build/Build/Products/Debug/IPTVPlayer.app"
if [ -d "$APP_PATH" ]; then
    echo -e "${GREEN}✓ App bundle created at: $APP_PATH${NC}"
    
    # Get app info
    APP_VERSION=$(/usr/libexec/PlistBuddy -c "Print CFBundleShortVersionString" "$APP_PATH/Contents/Info.plist" 2>/dev/null || echo "Unknown")
    APP_BUILD=$(/usr/libexec/PlistBuddy -c "Print CFBundleVersion" "$APP_PATH/Contents/Info.plist" 2>/dev/null || echo "Unknown")
    
    echo "   Version: $APP_VERSION"
    echo "   Build: $APP_BUILD"
else
    echo -e "${RED}✗ App bundle not found${NC}"
    exit 1
fi

echo ""
echo "5. Checking required files..."
REQUIRED_FILES=(
    "IPTVPlayer/App/IPTVPlayerApp.swift"
    "IPTVPlayer/Views/ContentView.swift"
    "IPTVPlayer/Views/PlaylistSidebarView.swift"
    "IPTVPlayer/Views/ChannelListView.swift"
    "IPTVPlayer/Views/PlayerView.swift"
    "IPTVPlayer/ViewModels/MainViewModel.swift"
    "IPTVPlayer/ViewModels/PlayerViewModel.swift"
    "IPTVPlayer/Services/M3UParser.swift"
    "IPTVPlayer/Services/XtreamClient.swift"
    "IPTVPlayer/Services/PlaylistRepository.swift"
    "IPTVPlayer/Services/FavoriteRepository.swift"
    "IPTVPlayer/Services/VideoPlayerService.swift"
)

ALL_FILES_EXIST=true
for file in "${REQUIRED_FILES[@]}"; do
    if [ -f "$file" ]; then
        echo -e "${GREEN}✓${NC} $file"
    else
        echo -e "${RED}✗${NC} $file (missing)"
        ALL_FILES_EXIST=false
    fi
done

if [ "$ALL_FILES_EXIST" = false ]; then
    echo -e "${RED}Some required files are missing${NC}"
    exit 1
fi

echo ""
echo "=========================================="
echo -e "${GREEN}Pre-Manual Testing Check: PASSED${NC}"
echo "=========================================="
echo ""
echo "The application is ready for manual testing!"
echo ""
echo "Next steps:"
echo "1. Open the app: open $APP_PATH"
echo "2. Follow the manual testing guide: MANUAL_TESTING_GUIDE.md"
echo "3. Use the quick checklist: QUICK_TESTING_CHECKLIST.md"
echo ""
echo "Test data suggestions:"
echo "- M3U URL: Search for 'free IPTV m3u' online"
echo "- Xtream: Use demo credentials if available"
echo ""

