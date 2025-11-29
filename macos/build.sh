#!/bin/bash

# Build script for IPTV Player macOS

set -e

echo "Building IPTV Player for macOS..."

# Check if xcodebuild is available
if ! command -v xcodebuild &> /dev/null; then
    echo "Error: xcodebuild not found. Please install Xcode."
    exit 1
fi

# Build the project
xcodebuild \
    -project IPTVPlayer.xcodeproj \
    -scheme IPTVPlayer \
    -configuration Debug \
    -destination 'platform=macOS' \
    clean build

echo "Build completed successfully!"
