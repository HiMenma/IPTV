#!/usr/bin/env python3
"""
Remove duplicate PlayerView.swift reference from Xcode project
"""

import re

# Read the project file
with open('IPTVPlayer.xcodeproj/project.pbxproj', 'r') as f:
    lines = f.readlines()

# Track which lines to keep
new_lines = []
playerview_in_sources_count = 0
in_sources_section = False
sources_section_depth = 0

for i, line in enumerate(lines):
    # Track if we're in the Sources build phase
    if 'PBXSourcesBuildPhase' in line and 'Begin' in line:
        in_sources_section = True
    elif 'PBXSourcesBuildPhase' in line and 'End' in line:
        in_sources_section = False
    
    # Count PlayerView.swift in Sources references
    if 'PlayerView.swift in Sources' in line:
        playerview_in_sources_count += 1
        # Keep only the first occurrence
        if playerview_in_sources_count > 1:
            print(f"Removing duplicate PlayerView.swift in Sources at line {i+1}")
            continue
    
    # Remove Services/PlayerView.swift file reference if it exists
    if 'Services/PlayerView.swift' in line or ('PlayerView.swift' in line and 'Services' in lines[max(0, i-5):i+5]):
        # Check if this is a file reference in Services folder
        if 'path = PlayerView.swift' in line:
            # Look back a few lines to see if we're in Services group
            context = ''.join(lines[max(0, i-10):i+10])
            if 'Services' in context and 'Views' not in context:
                print(f"Removing Services/PlayerView.swift reference at line {i+1}")
                continue
    
    new_lines.append(line)

# Write back
with open('IPTVPlayer.xcodeproj/project.pbxproj', 'w') as f:
    f.writelines(new_lines)

print(f"Removed {playerview_in_sources_count - 1} duplicate PlayerView.swift references")
print("Fixed Xcode project")
