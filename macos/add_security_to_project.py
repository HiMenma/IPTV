#!/usr/bin/env python3
"""
Script to add security files (KeychainManager and InputValidator) to the Xcode project
"""

import re
import uuid

def generate_uuid():
    """Generate a unique ID for Xcode project"""
    return uuid.uuid4().hex[:12].upper()

def add_files_to_project(project_path):
    """Add security files to the Xcode project"""
    
    with open(project_path, 'r') as f:
        content = f.read()
    
    files = [
        ('KeychainManager.swift', 'KM'),
        ('InputValidator.swift', 'IV')
    ]
    
    for filename, prefix in files:
        # Generate UUIDs for the new file
        file_ref_id = f"{prefix}{generate_uuid()}"
        build_file_id = f"{prefix}{generate_uuid()}"
        
        # Add to PBXBuildFile section (after PerformanceMonitor)
        build_file_pattern = r'(48CD276A1F3B49B18746E53A /\* PerformanceMonitor\.swift in Sources \*/ = \{isa = PBXBuildFile; fileRef = FE2A29DB84DF48D59807BF65 /\* PerformanceMonitor\.swift \*/; \};)'
        build_file_entry = r'\1\n\t\t' + build_file_id + f' /* {filename} in Sources */ = {{isa = PBXBuildFile; fileRef = {file_ref_id} /* {filename} */; }};'
        content = re.sub(build_file_pattern, build_file_entry, content)
        
        # Add to PBXFileReference section (after PerformanceMonitor)
        file_ref_pattern = r'(FE2A29DB84DF48D59807BF65 /\* PerformanceMonitor\.swift \*/ = \{isa = PBXFileReference; lastKnownFileType = sourcecode\.swift; path = PerformanceMonitor\.swift; sourceTree = "<group>"; \};)'
        file_ref_entry = r'\1\n\t\t' + file_ref_id + f' /* {filename} */ = {{isa = PBXFileReference; lastKnownFileType = sourcecode.swift; path = {filename}; sourceTree = "<group>"; }};'
        content = re.sub(file_ref_pattern, file_ref_entry, content)
        
        # Add to Services group (after PerformanceMonitor)
        services_pattern = r'(FE2A29DB84DF48D59807BF65 /\* PerformanceMonitor\.swift \*/,)'
        services_entry = r'\1\n\t\t\t\t' + file_ref_id + f' /* {filename} */,'
        content = re.sub(services_pattern, services_entry, content)
        
        # Add to Sources build phase (after PerformanceMonitor)
        sources_pattern = r'(48CD276A1F3B49B18746E53A /\* PerformanceMonitor\.swift in Sources \*/,)'
        sources_entry = r'\1\n\t\t\t\t' + build_file_id + f' /* {filename} in Sources */,'
        content = re.sub(sources_pattern, sources_entry, content)
        
        print(f"✅ Added {filename} to project")
        print(f"   File Reference ID: {file_ref_id}")
        print(f"   Build File ID: {build_file_id}")
    
    # Write back
    with open(project_path, 'w') as f:
        f.write(content)

if __name__ == '__main__':
    project_path = 'IPTVPlayer.xcodeproj/project.pbxproj'
    add_files_to_project(project_path)
