#!/usr/bin/env python3
"""
Script to add VideoPlayerControlsTest.swift to the Xcode project
"""

import re
import sys

# Read the project file
try:
    with open('IPTVPlayer.xcodeproj/project.pbxproj', 'r') as f:
        content = f.read()
except FileNotFoundError:
    print("Error: Could not find project.pbxproj file")
    print("Make sure you run this script from the macos directory")
    sys.exit(1)

# New test file to add
filename = 'VideoPlayerControlsTest.swift'
group = 'IPTVPlayerTests'

# Generate unique IDs
file_id = "A1000FFFD"
build_id = "A1000FFFC"

# Create file reference
file_ref = f"\t\t{file_id} /* {filename} */ = {{isa = PBXFileReference; lastKnownFileType = sourcecode.swift; path = {filename}; sourceTree = \"<group>\"; }};"

# Create build file
build_file = f"\t\t{build_id} /* {filename} in Sources */ = {{isa = PBXBuildFile; fileRef = {file_id} /* {filename} */; }};"

# Add to PBXBuildFile section
build_file_section = "/* Begin PBXBuildFile section */"
build_file_end = "/* End PBXBuildFile section */"
build_file_match = re.search(f"{re.escape(build_file_section)}(.*?){re.escape(build_file_end)}", content, re.DOTALL)
if build_file_match:
    existing_build_files = build_file_match.group(1)
    new_build_section = build_file_section + existing_build_files.rstrip() + "\n" + build_file + "\n" + build_file_end
    content = content.replace(build_file_match.group(0), new_build_section)
    print(f"✓ Added {filename} to PBXBuildFile section")
else:
    print("✗ Could not find PBXBuildFile section")

# Add to PBXFileReference section
file_ref_section = "/* Begin PBXFileReference section */"
file_ref_end = "/* End PBXFileReference section */"
file_ref_match = re.search(f"{re.escape(file_ref_section)}(.*?){re.escape(file_ref_end)}", content, re.DOTALL)
if file_ref_match:
    existing_file_refs = file_ref_match.group(1)
    new_file_section = file_ref_section + existing_file_refs.rstrip() + "\n" + file_ref + "\n" + file_ref_end
    content = content.replace(file_ref_match.group(0), new_file_section)
    print(f"✓ Added {filename} to PBXFileReference section")
else:
    print("✗ Could not find PBXFileReference section")

# Write back
try:
    with open('IPTVPlayer.xcodeproj/project.pbxproj', 'w') as f:
        f.write(content)
    print(f"\n✓ Successfully added {filename} to Xcode project!")
except Exception as e:
    print(f"\n✗ Error writing project file: {e}")
    sys.exit(1)

