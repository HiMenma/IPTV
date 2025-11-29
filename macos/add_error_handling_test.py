#!/usr/bin/env python3
"""
Script to add VideoPlayerErrorHandlingTest.swift to the Xcode project
"""

import re
import sys

# Read the project file
with open('IPTVPlayer.xcodeproj/project.pbxproj', 'r') as f:
    content = f.read()

# New test file to add
filename = 'VideoPlayerErrorHandlingTest.swift'
file_id = 'A1000ETEST'
build_id = 'A1000ETESTB'

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
    print(f"Added {filename} to PBXBuildFile section")
else:
    print("Could not find PBXBuildFile section")
    sys.exit(1)

# Add to PBXFileReference section
file_ref_section = "/* Begin PBXFileReference section */"
file_ref_end = "/* End PBXFileReference section */"
file_ref_match = re.search(f"{re.escape(file_ref_section)}(.*?){re.escape(file_ref_end)}", content, re.DOTALL)
if file_ref_match:
    existing_file_refs = file_ref_match.group(1)
    new_file_section = file_ref_section + existing_file_refs.rstrip() + "\n" + file_ref + "\n" + file_ref_end
    content = content.replace(file_ref_match.group(0), new_file_section)
    print(f"Added {filename} to PBXFileReference section")
else:
    print("Could not find PBXFileReference section")
    sys.exit(1)

# Find IPTVPlayerTests group and add file
# Look for the IPTVPlayerTests group
tests_group_pattern = r"(A10000070 /\* IPTVPlayerTests \*/ = \{[^}]+children = \([^)]+)"
if re.search(tests_group_pattern, content):
    content = re.sub(tests_group_pattern, r"\1\n\t\t\t\t" + file_id + f" /* {filename} */,", content)
    print(f"Added {filename} to IPTVPlayerTests group")
else:
    print("Could not find IPTVPlayerTests group")
    sys.exit(1)

# Add to test target's Sources build phase
# Find the test target's PBXSourcesBuildPhase
test_sources_pattern = r"(A0FFFFED0 /\* Sources \*/ = \{[^}]+files = \([^)]+)"
if re.search(test_sources_pattern, content):
    content = re.sub(test_sources_pattern, r"\1\n\t\t\t\t" + build_id + f" /* {filename} in Sources */,", content)
    print(f"Added {filename} to test Sources build phase")
else:
    print("Could not find test Sources build phase")
    sys.exit(1)

# Write back
with open('IPTVPlayer.xcodeproj/project.pbxproj', 'w') as f:
    f.write(content)

print(f"\n✅ {filename} added to Xcode project successfully!")
