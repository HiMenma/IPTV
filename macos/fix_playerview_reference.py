#!/usr/bin/env python3
"""
Fix PlayerView.swift reference in Xcode project
"""

import re

# Read the project file
with open('IPTVPlayer.xcodeproj/project.pbxproj', 'r') as f:
    content = f.read()

# Find and remove the incorrect PlayerView.swift reference in Services
# Look for the pattern with Services/PlayerView.swift
content = re.sub(
    r'([A-F0-9]{24} /\* PlayerView\.swift \*/ = \{isa = PBXFileReference; lastKnownFileType = sourcecode\.swift; path = PlayerView\.swift; sourceTree = "<group>"; \};)',
    r'',
    content
)

# Remove from Services group children
content = re.sub(
    r'([A-F0-9]{24} /\* PlayerView\.swift \*/,\s*)',
    r'',
    content
)

# Remove from build phase if it appears twice
lines = content.split('\n')
playerview_count = 0
new_lines = []
in_sources_build_phase = False

for line in lines:
    if 'PBXSourcesBuildPhase' in line:
        in_sources_build_phase = True
    elif 'End PBXSourcesBuildPhase' in line:
        in_sources_build_phase = False
    
    if 'PlayerView.swift in Sources' in line:
        playerview_count += 1
        if playerview_count > 1 and in_sources_build_phase:
            # Skip duplicate
            continue
    
    new_lines.append(line)

content = '\n'.join(new_lines)

# Write back
with open('IPTVPlayer.xcodeproj/project.pbxproj', 'w') as f:
    f.write(content)

print("Fixed PlayerView.swift reference in Xcode project")
