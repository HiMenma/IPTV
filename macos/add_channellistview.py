#!/usr/bin/env python3
"""
Script to add ChannelListView.swift to the Xcode project
"""

import re
import uuid

def generate_uuid():
    """Generate a unique ID for Xcode project"""
    return uuid.uuid4().hex[:12].upper()

def add_file_to_project(project_path):
    """Add ChannelListView.swift to the Xcode project"""
    
    with open(project_path, 'r') as f:
        content = f.read()
    
    # Generate UUIDs for the new file
    file_ref_id = f"CL{generate_uuid()}"
    build_file_id = f"CL{generate_uuid()}"
    
    # Add to PBXBuildFile section (after PlaylistSidebarView)
    build_file_pattern = r'(A1000060D /\* PlaylistSidebarView\.swift in Sources \*/ = \{isa = PBXBuildFile; fileRef = A1000050D /\* PlaylistSidebarView\.swift \*/; \};)'
    build_file_entry = r'\1\n\t\t' + build_file_id + ' /* ChannelListView.swift in Sources */ = {isa = PBXBuildFile; fileRef = ' + file_ref_id + ' /* ChannelListView.swift */; };'
    content = re.sub(build_file_pattern, build_file_entry, content)
    
    # Add to PBXFileReference section (after PlaylistSidebarView)
    file_ref_pattern = r'(A1000050D /\* PlaylistSidebarView\.swift \*/ = \{isa = PBXFileReference; lastKnownFileType = sourcecode\.swift; path = PlaylistSidebarView\.swift; sourceTree = "<group>"; \};)'
    file_ref_entry = r'\1\n\t\t' + file_ref_id + ' /* ChannelListView.swift */ = {isa = PBXFileReference; lastKnownFileType = sourcecode.swift; path = ChannelListView.swift; sourceTree = "<group>"; };'
    content = re.sub(file_ref_pattern, file_ref_entry, content)
    
    # Add to Views group (after PlaylistSidebarView)
    views_pattern = r'(A1000050D /\* PlaylistSidebarView\.swift \*/,)'
    views_entry = r'\1\n\t\t\t\t' + file_ref_id + ' /* ChannelListView.swift */,'
    content = re.sub(views_pattern, views_entry, content)
    
    # Add to Sources build phase (after PlaylistSidebarView)
    sources_pattern = r'(A1000060D /\* PlaylistSidebarView\.swift in Sources \*/,)'
    sources_entry = r'\1\n\t\t\t\t' + build_file_id + ' /* ChannelListView.swift in Sources */,'
    content = re.sub(sources_pattern, sources_entry, content)
    
    # Write back
    with open(project_path, 'w') as f:
        f.write(content)
    
    print(f"✅ Added ChannelListView.swift to project")
    print(f"   File Reference ID: {file_ref_id}")
    print(f"   Build File ID: {build_file_id}")

if __name__ == '__main__':
    project_path = 'IPTVPlayer.xcodeproj/project.pbxproj'
    add_file_to_project(project_path)
