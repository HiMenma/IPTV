#!/usr/bin/env python3
"""
Script to add new Swift files to the Xcode project
"""

import re

# Read the project file
with open('IPTVPlayer.xcodeproj/project.pbxproj', 'r') as f:
    content = f.read()

# New files to add
new_files = [
    ('AppError.swift', 'Services'),
    ('Logger.swift', 'Services'),
    ('RetryMechanism.swift', 'Services'),
    ('ErrorPresenter.swift', 'Services'),
    ('ErrorView.swift', 'Views'),
    ('M3UParser.swift', 'Services'),
    ('XtreamClient.swift', 'Services'),
    ('Channel.swift', 'Models'),
    ('Category.swift', 'Models'),
    ('XtreamAccount.swift', 'Models'),
    ('Persistence.swift', 'Models'),
]

# Generate unique IDs for new files
base_id = 0xA1000000D
file_refs = []
build_files = []

for i, (filename, group) in enumerate(new_files):
    file_id = f"A{base_id + i:08X}"
    build_id = f"A{base_id + i + 100:08X}"
    
    file_refs.append(f"\t\t{file_id} /* {filename} */ = {{isa = PBXFileReference; lastKnownFileType = sourcecode.swift; path = {filename}; sourceTree = \"<group>\"; }};")
    build_files.append(f"\t\t{build_id} /* {filename} in Sources */ = {{isa = PBXBuildFile; fileRef = {file_id} /* {filename} */; }};")

# Add to PBXBuildFile section
build_file_section = "/* Begin PBXBuildFile section */"
build_file_end = "/* End PBXBuildFile section */"
build_file_match = re.search(f"{re.escape(build_file_section)}(.*?){re.escape(build_file_end)}", content, re.DOTALL)
if build_file_match:
    existing_build_files = build_file_match.group(1)
    new_build_section = build_file_section + existing_build_files.rstrip() + "\n" + "\n".join(build_files) + "\n" + build_file_end
    content = content.replace(build_file_match.group(0), new_build_section)

# Add to PBXFileReference section
file_ref_section = "/* Begin PBXFileReference section */"
file_ref_end = "/* End PBXFileReference section */"
file_ref_match = re.search(f"{re.escape(file_ref_section)}(.*?){re.escape(file_ref_end)}", content, re.DOTALL)
if file_ref_match:
    existing_file_refs = file_ref_match.group(1)
    new_file_section = file_ref_section + existing_file_refs.rstrip() + "\n" + "\n".join(file_refs) + "\n" + file_ref_end
    content = content.replace(file_ref_match.group(0), new_file_section)

# Add to Services group
services_group = "A10000040 /* Services */ = {\n\t\t\tisa = PBXGroup;\n\t\t\tchildren = (\n\t\t\t);"
services_files = [f for f in new_files if f[1] == 'Services']
services_children = "\n".join([f"\t\t\t\tA{base_id + i:08X} /* {filename} */," for i, (filename, group) in enumerate(new_files) if group == 'Services'])
new_services_group = f"A10000040 /* Services */ = {{\n\t\t\tisa = PBXGroup;\n\t\t\tchildren = (\n{services_children}\n\t\t\t);"
content = content.replace(services_group, new_services_group)

# Add to Views group  
views_group_pattern = r"(A10000020 /\* Views \*/ = \{[^}]+children = \(\s+A10000002 /\* ContentView\.swift \*/,)"
views_files = [f for f in new_files if f[1] == 'Views']
views_children = "\n".join([f"\t\t\t\tA{base_id + i:08X} /* {filename} */," for i, (filename, group) in enumerate(new_files) if group == 'Views'])
content = re.sub(views_group_pattern, r"\1\n" + views_children, content)

# Add to Models group
models_group_pattern = r"(A10000050 /\* Models \*/ = \{[^}]+children = \(\s+A10000009 /\* IPTVPlayer\.xcdatamodeld \*/,)"
models_files = [f for f in new_files if f[1] == 'Models']
models_children = "\n".join([f"\t\t\t\tA{base_id + i:08X} /* {filename} */," for i, (filename, group) in enumerate(new_files) if group == 'Models'])
content = re.sub(models_group_pattern, r"\1\n" + models_children, content)

# Add to Sources build phase
sources_phase_pattern = r"(A0FFFFEC0 /\* Sources \*/ = \{[^}]+files = \([^)]+)"
sources_refs = "\n".join([f"\t\t\t\tA{base_id + i + 100:08X} /* {filename} in Sources */," for i, (filename, _) in enumerate(new_files)])
content = re.sub(sources_phase_pattern, r"\1\n" + sources_refs, content)

# Write back
with open('IPTVPlayer.xcodeproj/project.pbxproj', 'w') as f:
    f.write(content)

print("Files added to Xcode project successfully!")
