#!/usr/bin/env python3
"""
Script to add CoreDataEntities.swift to the Xcode project
"""

import re
import uuid

def generate_uuid():
    """Generate a unique ID for Xcode project"""
    return uuid.uuid4().hex[:12].upper()

def add_file_to_project(project_path):
    """Add CoreDataEntities.swift to the Xcode project"""
    
    with open(project_path, 'r') as f:
        content = f.read()
    
    # Generate UUIDs for the new file
    file_ref_id = f"CD{generate_uuid()}"
    build_file_id = f"CD{generate_uuid()}"
    
    # Add to PBXBuildFile section
    build_file_entry = f"\t\t{build_file_id} /* CoreDataEntities.swift in Sources */ = {{isa = PBXBuildFile; fileRef = {file_ref_id} /* CoreDataEntities.swift */; }};\n"
    
    # Find the end of PBXBuildFile section
    build_file_pattern = r'(/\* End PBXBuildFile section \*/)'
    content = re.sub(build_file_pattern, build_file_entry + r'\1', content)
    
    # Add to PBXFileReference section
    file_ref_entry = f"\t\t{file_ref_id} /* CoreDataEntities.swift */ = {{isa = PBXFileReference; lastKnownFileType = sourcecode.swift; path = CoreDataEntities.swift; sourceTree = \"<group>\"; }};\n"
    
    # Find the end of PBXFileReference section
    file_ref_pattern = r'(/\* End PBXFileReference section \*/)'
    content = re.sub(file_ref_pattern, file_ref_entry + r'\1', content)
    
    # Add to Models group (find the Models group and add the file)
    # Look for the Models group which contains Persistence.swift
    models_pattern = r'(AA10000017 /\* Persistence\.swift \*/;)'
    models_entry = r'\1\n\t\t\t\t' + file_ref_id + ' /* CoreDataEntities.swift */;'
    content = re.sub(models_pattern, models_entry, content)
    
    # Add to Sources build phase
    # Find the section with other .swift files being compiled
    sources_pattern = r'(AA1000007B /\* Persistence\.swift in Sources \*/ = \{isa = PBXBuildFile; fileRef = AA10000017 /\* Persistence\.swift \*/; \};)'
    sources_entry = r'\1\n\t\t' + build_file_id + ' /* CoreDataEntities.swift in Sources */ = {isa = PBXBuildFile; fileRef = ' + file_ref_id + ' /* CoreDataEntities.swift */; };'
    content = re.sub(sources_pattern, sources_entry, content)
    
    # Write back
    with open(project_path, 'w') as f:
        f.write(content)
    
    print(f"✅ Added CoreDataEntities.swift to project")
    print(f"   File Reference ID: {file_ref_id}")
    print(f"   Build File ID: {build_file_id}")

if __name__ == '__main__':
    project_path = 'IPTVPlayer.xcodeproj/project.pbxproj'
    add_file_to_project(project_path)
