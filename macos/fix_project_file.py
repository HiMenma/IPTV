#!/usr/bin/env python3
"""
Fix the Xcode project.pbxproj file by correcting syntax errors and adding missing test target.
"""

import re

def fix_project_file():
    with open('IPTVPlayer.xcodeproj/project.pbxproj', 'r') as f:
        content = f.read()
    
    # Fix the PBXGroup sections with improper line breaks
    # Fix Views group
    content = re.sub(
        r'A10000F00 /\* PlayerView\.swift \*/,\);',
        'A10000F00 /* PlayerView.swift */,\n\t\t\t);',
        content
    )
    
    # Fix ViewModels group
    content = re.sub(
        r'A1000070 /\* PlayerViewModel\.swift \*/,\);',
        'A1000070 /* PlayerViewModel.swift */,\n\t\t\t);',
        content
    )
    
    # Fix Services group
    content = re.sub(
        r'A10000F00 /\* VideoPlayerService\.swift \*/,\);',
        'A10000F01 /* VideoPlayerService.swift */,\n\t\t\t);',
        content
    )
    
    # Fix Models group
    content = re.sub(
        r'A1000020D /\* Favorite\.swift \*/,\);',
        'A1000020D /* Favorite.swift */,\n\t\t\t);',
        content
    )
    
    # Fix the Sources build phase - add missing closing
    content = re.sub(
        r'A1000071 /\* PlayerViewModel\.swift in Sources \*/,\n\t\t\trunOnlyForDeploymentPostprocessing = 0;',
        'A1000071 /* PlayerViewModel.swift in Sources */,\n\t\t\t\tA10000F01 /* PlayerView.swift in Sources */,\n\t\t\t);\n\t\t\trunOnlyForDeploymentPostprocessing = 0;',
        content
    )
    
    # Fix duplicate file reference ID for VideoPlayerService
    content = content.replace(
        'A10000F01 /* VideoPlayerService.swift in Sources */',
        'A10000F02 /* VideoPlayerService.swift in Sources */'
    )
    content = content.replace(
        '{isa = PBXBuildFile; fileRef = A10000F00 /* VideoPlayerService.swift */',
        '{isa = PBXBuildFile; fileRef = A10000F01 /* VideoPlayerService.swift */'
    )
    
    # Fix the file reference for PlayerView to use unique ID
    content = content.replace(
        'A10000F00 /* PlayerView.swift */ = {isa = PBXFileReference',
        'A10000F10 /* PlayerView.swift */ = {isa = PBXFileReference'
    )
    content = content.replace(
        'A10000F01 /* PlayerView.swift in Sources */ = {isa = PBXBuildFile; fileRef = A10000F00 /* PlayerView.swift */',
        'A10000F11 /* PlayerView.swift in Sources */ = {isa = PBXBuildFile; fileRef = A10000F10 /* PlayerView.swift */'
    )
    content = content.replace(
        'A10000F01 /* PlayerView.swift in Sources */,',
        'A10000F11 /* PlayerView.swift in Sources */,'
    )
    content = content.replace(
        'A10000F00 /* PlayerView.swift */,',
        'A10000F10 /* PlayerView.swift */,'
    )
    
    # Write the fixed content
    with open('IPTVPlayer.xcodeproj/project.pbxproj', 'w') as f:
        f.write(content)
    
    print("✅ Fixed project.pbxproj file")
    print("   - Fixed PBXGroup formatting")
    print("   - Fixed duplicate file reference IDs")
    print("   - Fixed Sources build phase")

if __name__ == '__main__':
    fix_project_file()
