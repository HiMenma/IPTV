#!/usr/bin/env python3
"""
Script to add FavoriteRepositoryTests.swift to the Xcode project
"""

import re

# Read the project file
with open('IPTVPlayer.xcodeproj/project.pbxproj', 'r') as f:
    content = f.read()

# New test file to add
filename = 'FavoriteRepositoryTests.swift'

# Generate unique IDs
file_id = "A1000030F"
build_id = "A100003FF"

# Create file reference and build file entries
file_ref = f"\t\t{file_id} /* {filename} */ = {{isa = PBXFileReference; lastKnownFileType = sourcecode.swift; path = {filename}; sourceTree = \"<group>\"; }};"
build_file = f"\t\t{build_id} /* {filename} in Sources */ = {{isa = PBXBuildFile; fileRef = {file_id} /* {filename} */; }};"

# Add to PBXBuildFile section
build_file_section = "/* Begin PBXBuildFile section */"
build_file_end = "/* End PBXBuildFile section */"
build_file_match = re.search(f"{re.escape(build_file_section)}(.*?){re.escape(build_file_end)}", content, re.DOTALL)
if build_file_match:
    existing_build_files = build_file_match.group(1)
    new_build_section = build_file_section + existing_build_files.rstrip() + "\n" + build_file + "\n" + build_file_end
    content = content.replace(build_file_match.group(0), new_build_section)

# Add to PBXFileReference section
file_ref_section = "/* Begin PBXFileReference section */"
file_ref_end = "/* End PBXFileReference section */"
file_ref_match = re.search(f"{re.escape(file_ref_section)}(.*?){re.escape(file_ref_end)}", content, re.DOTALL)
if file_ref_match:
    existing_file_refs = file_ref_match.group(1)
    new_file_section = file_ref_section + existing_file_refs.rstrip() + "\n" + file_ref + "\n" + file_ref_end
    content = content.replace(file_ref_match.group(0), new_file_section)

# Add to IPTVPlayerTests group - find the test group and add the file
tests_pattern = r"(A10000060 /\* IPTVPlayerTests \*/ = \{[^}]*children = \([^)]*)"
tests_child = f"\t\t\t\t{file_id} /* {filename} */,"
content = re.sub(tests_pattern, r"\1\n" + tests_child, content)

# Add to test Sources build phase - find the test target's sources phase
# First, find the test target's sources phase ID
test_sources_pattern = r"(A0FFFFED0 /\* Sources \*/ = \{[^}]*files = \([^)]*)"
test_sources_ref = f"\t\t\t\t{build_id} /* {filename} in Sources */,"
content = re.sub(test_sources_pattern, r"\1\n" + test_sources_ref, content)

# Write back
with open('IPTVPlayer.xcodeproj/project.pbxproj', 'w') as f:
    f.write(content)

print(f"{filename} added to Xcode project successfully!")
