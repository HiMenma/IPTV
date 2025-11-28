# Native Libraries

This directory contains platform-specific native libraries bundled with the application.

## Structure

```
native/
├── macos-aarch64/     # macOS Apple Silicon (M1/M2/M3)
│   └── libmpv.2.dylib
├── macos-x86_64/      # macOS Intel
│   └── libmpv.2.dylib
├── linux-x86_64/      # Linux 64-bit
│   └── libmpv.so.2
└── windows-x86_64/    # Windows 64-bit
    └── libmpv-2.dll
```

## Purpose

These libraries are automatically extracted and loaded by the application at runtime,
eliminating the need for users to manually install dependencies.

## How to Update

Use the provided scripts to copy the latest library files:

```bash
# macOS / Linux
./scripts/copy-libmpv.sh

# Windows
.\scripts\copy-libmpv.ps1
```

## Git

These library files **should be committed** to version control as they are part of
the application distribution. They ensure the application works out-of-the-box
without requiring users to install system dependencies.

## Size

- Each library: ~3-10 MB
- Total (all platforms): ~15-40 MB

## License

All libraries in this directory are licensed under their respective licenses:
- **libmpv**: LGPL 2.1+ (https://github.com/mpv-player/mpv)

See [LIBMPV_INTEGRATION.md](../../../../../LIBMPV_INTEGRATION.md) for license compliance details.
