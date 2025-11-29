#!/usr/bin/env python3
"""
Script to add MainViewModel.swift to the Xcode project
"""

import sys
import os

# Add the parent directory to the path to import the pbxproj module
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

try:
    from mod_pbxproj import XcodeProject
except ImportError:
    print("Error: mod_pbxproj module not found")
    print("Please ensure mod_pbxproj.py is in the same directory")
    sys.exit(1)

def main():
    project_path = "IPTVPlayer.xcodeproj/project.pbxproj"
    
    if not os.path.exists(project_path):
        print(f"Error: Project file not found at {project_path}")
        sys.exit(1)
    
    # Load the project
    project = XcodeProject.load(project_path)
    
    # Add MainViewModel.swift to ViewModels group
    viewmodel_file = "IPTVPlayer/ViewModels/MainViewModel.swift"
    
    if os.path.exists(viewmodel_file):
        print(f"Adding {viewmodel_file} to project...")
        
        # Add to ViewModels group
        project.add_file(
            viewmodel_file,
            parent_group="IPTVPlayer/ViewModels",
            target_name="IPTVPlayer"
        )
        
        print(f"✓ Added {viewmodel_file}")
    else:
        print(f"Warning: {viewmodel_file} not found")
    
    # Save the project
    project.save()
    print("\n✓ Project file updated successfully")

if __name__ == "__main__":
    main()
