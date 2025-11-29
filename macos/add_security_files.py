#!/usr/bin/env python3
"""
Script to add security-related files to the Xcode project
"""

import sys
import uuid

def generate_uuid():
    """Generate a unique UUID for Xcode"""
    return ''.join(str(uuid.uuid4()).upper().split('-'))

def add_files_to_project(project_path):
    """Add security files to the Xcode project"""
    
    with open(project_path, 'r') as f:
        content = f.read()
    
    # Generate UUIDs for new files
    keychain_file_ref = generate_uuid()
    keychain_build_ref = generate_uuid()
    validator_file_ref = generate_uuid()
    validator_build_ref = generate_uuid()
    security_test_file_ref = generate_uuid()
    security_test_build_ref = generate_uuid()
    
    # Find the Services group
    services_group_start = content.find('/* Services */ = {')
    if services_group_start == -1:
        print("Error: Could not find Services group")
        return False
    
    # Find the children array in Services group
    services_children_start = content.find('children = (', services_group_start)
    services_children_end = content.find(');', services_children_start)
    
    # Add KeychainManager.swift and InputValidator.swift to Services group
    keychain_entry = f'\t\t\t\t{keychain_file_ref} /* KeychainManager.swift */,\n'
    validator_entry = f'\t\t\t\t{validator_file_ref} /* InputValidator.swift */,\n'
    
    insert_pos = services_children_end
    content = content[:insert_pos] + keychain_entry + validator_entry + content[insert_pos:]
    
    # Find the IPTVPlayerTests group
    tests_group_start = content.find('/* IPTVPlayerTests */ = {')
    if tests_group_start == -1:
        print("Error: Could not find IPTVPlayerTests group")
        return False
    
    # Find the children array in IPTVPlayerTests group
    tests_children_start = content.find('children = (', tests_group_start)
    tests_children_end = content.find(');', tests_children_start)
    
    # Add SecurityTests.swift to IPTVPlayerTests group
    security_test_entry = f'\t\t\t\t{security_test_file_ref} /* SecurityTests.swift */,\n'
    
    insert_pos = tests_children_end
    content = content[:insert_pos] + security_test_entry + content[insert_pos:]
    
    # Add PBXFileReference entries
    file_ref_section_start = content.find('/* Begin PBXFileReference section */')
    file_ref_section_end = content.find('/* End PBXFileReference section */')
    
    keychain_file_ref_entry = f'\t\t{keychain_file_ref} /* KeychainManager.swift */ = {{isa = PBXFileReference; lastKnownFileType = sourcecode.swift; path = KeychainManager.swift; sourceTree = "<group>"; }};\n'
    validator_file_ref_entry = f'\t\t{validator_file_ref} /* InputValidator.swift */ = {{isa = PBXFileReference; lastKnownFileType = sourcecode.swift; path = InputValidator.swift; sourceTree = "<group>"; }};\n'
    security_test_file_ref_entry = f'\t\t{security_test_file_ref} /* SecurityTests.swift */ = {{isa = PBXFileReference; lastKnownFileType = sourcecode.swift; path = SecurityTests.swift; sourceTree = "<group>"; }};\n'
    
    insert_pos = file_ref_section_end
    content = content[:insert_pos] + keychain_file_ref_entry + validator_file_ref_entry + security_test_file_ref_entry + content[insert_pos:]
    
    # Add PBXBuildFile entries
    build_file_section_start = content.find('/* Begin PBXBuildFile section */')
    build_file_section_end = content.find('/* End PBXBuildFile section */')
    
    keychain_build_entry = f'\t\t{keychain_build_ref} /* KeychainManager.swift in Sources */ = {{isa = PBXBuildFile; fileRef = {keychain_file_ref} /* KeychainManager.swift */; }};\n'
    validator_build_entry = f'\t\t{validator_build_ref} /* InputValidator.swift in Sources */ = {{isa = PBXBuildFile; fileRef = {validator_file_ref} /* InputValidator.swift */; }};\n'
    security_test_build_entry = f'\t\t{security_test_build_ref} /* SecurityTests.swift in Sources */ = {{isa = PBXBuildFile; fileRef = {security_test_file_ref} /* SecurityTests.swift */; }};\n'
    
    insert_pos = build_file_section_end
    content = content[:insert_pos] + keychain_build_entry + validator_build_entry + security_test_build_entry + content[insert_pos:]
    
    # Add to Sources build phase for main target
    main_sources_phase = content.find('/* Sources */ = {', content.find('/* IPTVPlayer */'))
    if main_sources_phase != -1:
        main_files_start = content.find('files = (', main_sources_phase)
        main_files_end = content.find(');', main_files_start)
        
        keychain_source_entry = f'\t\t\t\t{keychain_build_ref} /* KeychainManager.swift in Sources */,\n'
        validator_source_entry = f'\t\t\t\t{validator_build_ref} /* InputValidator.swift in Sources */,\n'
        
        insert_pos = main_files_end
        content = content[:insert_pos] + keychain_source_entry + validator_source_entry + content[insert_pos:]
    
    # Add to Sources build phase for test target
    test_sources_phase = content.find('/* Sources */ = {', content.find('/* IPTVPlayerTests */'))
    if test_sources_phase != -1:
        test_files_start = content.find('files = (', test_sources_phase)
        test_files_end = content.find(');', test_files_start)
        
        security_test_source_entry = f'\t\t\t\t{security_test_build_ref} /* SecurityTests.swift in Sources */,\n'
        
        insert_pos = test_files_end
        content = content[:insert_pos] + security_test_source_entry + content[insert_pos:]
    
    # Write back to file
    with open(project_path, 'w') as f:
        f.write(content)
    
    print("✅ Successfully added security files to Xcode project")
    print(f"   - KeychainManager.swift")
    print(f"   - InputValidator.swift")
    print(f"   - SecurityTests.swift")
    return True

if __name__ == '__main__':
    project_path = 'IPTVPlayer.xcodeproj/project.pbxproj'
    
    if add_files_to_project(project_path):
        sys.exit(0)
    else:
        sys.exit(1)
