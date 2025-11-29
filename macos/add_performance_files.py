#!/usr/bin/env python3
"""
Script to add performance optimization files to Xcode project
"""

import sys
import uuid

def generate_uuid():
    """Generate a unique 24-character hex string for Xcode"""
    return uuid.uuid4().hex[:24].upper()

def add_files_to_project():
    project_path = 'macos/IPTVPlayer.xcodeproj/project.pbxproj'
    
    # Read the project file
    with open(project_path, 'r') as f:
        content = f.read()
    
    # Generate UUIDs for new files
    image_cache_ref = generate_uuid()
    image_cache_build = generate_uuid()
    cached_async_image_ref = generate_uuid()
    cached_async_image_build = generate_uuid()
    perf_monitor_ref = generate_uuid()
    perf_monitor_build = generate_uuid()
    
    # File references to add
    file_refs = f'''
		{image_cache_ref} /* ImageCache.swift */ = {{isa = PBXFileReference; lastKnownFileType = sourcecode.swift; path = ImageCache.swift; sourceTree = "<group>"; }};
		{cached_async_image_ref} /* CachedAsyncImage.swift */ = {{isa = PBXFileReference; lastKnownFileType = sourcecode.swift; path = CachedAsyncImage.swift; sourceTree = "<group>"; }};
		{perf_monitor_ref} /* PerformanceMonitor.swift */ = {{isa = PBXFileReference; lastKnownFileType = sourcecode.swift; path = PerformanceMonitor.swift; sourceTree = "<group>"; }};
'''
    
    # Build files to add
    build_files = f'''
		{image_cache_build} /* ImageCache.swift in Sources */ = {{isa = PBXBuildFile; fileRef = {image_cache_ref} /* ImageCache.swift */; }};
		{cached_async_image_build} /* CachedAsyncImage.swift in Sources */ = {{isa = PBXBuildFile; fileRef = {cached_async_image_ref} /* CachedAsyncImage.swift */; }};
		{perf_monitor_build} /* PerformanceMonitor.swift in Sources */ = {{isa = PBXBuildFile; fileRef = {perf_monitor_ref} /* PerformanceMonitor.swift */; }};
'''
    
    # Find the Services group and add ImageCache and PerformanceMonitor
    services_marker = '/* Services */ = {'
    services_idx = content.find(services_marker)
    if services_idx != -1:
        # Find the children array in Services group
        children_start = content.find('children = (', services_idx)
        if children_start != -1:
            insert_pos = content.find(');', children_start)
            if insert_pos != -1:
                services_additions = f'''				{image_cache_ref} /* ImageCache.swift */,
				{perf_monitor_ref} /* PerformanceMonitor.swift */,
'''
                content = content[:insert_pos] + services_additions + content[insert_pos:]
    
    # Find the Views group and add CachedAsyncImage
    views_marker = '/* Views */ = {'
    views_idx = content.find(views_marker)
    if views_idx != -1:
        # Find the children array in Views group
        children_start = content.find('children = (', views_idx)
        if children_start != -1:
            insert_pos = content.find(');', children_start)
            if insert_pos != -1:
                views_additions = f'''				{cached_async_image_ref} /* CachedAsyncImage.swift */,
'''
                content = content[:insert_pos] + views_additions + content[insert_pos:]
    
    # Add file references section
    file_ref_section_end = content.find('/* End PBXFileReference section */')
    if file_ref_section_end != -1:
        content = content[:file_ref_section_end] + file_refs + content[file_ref_section_end:]
    
    # Add build files section
    build_file_section_end = content.find('/* End PBXBuildFile section */')
    if build_file_section_end != -1:
        content = content[:build_file_section_end] + build_files + content[build_file_section_end:]
    
    # Add to Sources build phase
    sources_phase_marker = 'isa = PBXSourcesBuildPhase;'
    sources_idx = content.find(sources_phase_marker)
    if sources_idx != -1:
        files_start = content.find('files = (', sources_idx)
        if files_start != -1:
            insert_pos = content.find(');', files_start)
            if insert_pos != -1:
                sources_additions = f'''				{image_cache_build} /* ImageCache.swift in Sources */,
				{cached_async_image_build} /* CachedAsyncImage.swift in Sources */,
				{perf_monitor_build} /* PerformanceMonitor.swift in Sources */,
'''
                content = content[:insert_pos] + sources_additions + content[insert_pos:]
    
    # Write back
    with open(project_path, 'w') as f:
        f.write(content)
    
    print("✅ Successfully added performance optimization files to Xcode project")
    print(f"   - ImageCache.swift (Services)")
    print(f"   - PerformanceMonitor.swift (Services)")
    print(f"   - CachedAsyncImage.swift (Views)")

if __name__ == '__main__':
    try:
        add_files_to_project()
    except Exception as e:
        print(f"❌ Error: {e}", file=sys.stderr)
        sys.exit(1)
