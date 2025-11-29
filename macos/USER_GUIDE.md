# IPTV Player - macOS User Guide

Welcome to IPTV Player for macOS! This guide will help you get started with the application and make the most of its features.

## Table of Contents

1. [Installation](#installation)
2. [Getting Started](#getting-started)
3. [Adding Playlists](#adding-playlists)
4. [Browsing Channels](#browsing-channels)
5. [Playing Videos](#playing-videos)
6. [Managing Favorites](#managing-favorites)
7. [Keyboard Shortcuts](#keyboard-shortcuts)
8. [Troubleshooting](#troubleshooting)
9. [Privacy & Security](#privacy--security)
10. [FAQ](#faq)

---

## Installation

### System Requirements

- **macOS**: 13.0 (Ventura) or later
- **Processor**: Apple Silicon (M1/M2/M3) or Intel
- **Memory**: 4 GB RAM minimum, 8 GB recommended
- **Storage**: 100 MB free space
- **Internet**: Broadband connection for streaming

### Installing the Application

1. **Download** the latest DMG file from the [Releases page](https://github.com/YOUR_REPO/releases)
2. **Open** the downloaded DMG file
3. **Drag** the IPTV Player icon to your Applications folder
4. **Launch** the app from Applications or Spotlight

### First Launch

On first launch, macOS may show a security warning because the app is not from the App Store:

1. Click **Cancel** on the warning dialog
2. Open **System Settings** → **Privacy & Security**
3. Scroll down and click **Open Anyway** next to the IPTV Player message
4. Click **Open** in the confirmation dialog

---

## Getting Started

### Main Interface

The IPTV Player interface consists of three main areas:

```
┌─────────────────────────────────────────────────────┐
│  Playlist Sidebar  │  Channel List  │  Player       │
│                    │                │               │
│  • My Playlists    │  📺 CNN        │  [Video]      │
│  • Favorites       │  📺 BBC        │               │
│  • Add Playlist    │  📺 ESPN       │  [Controls]   │
│                    │                │               │
└─────────────────────────────────────────────────────┘
```

1. **Playlist Sidebar** (Left): Manage your playlists and access favorites
2. **Channel List** (Center): Browse channels from the selected playlist
3. **Player** (Right): Watch videos with playback controls

### Quick Start

1. **Add a playlist** (see [Adding Playlists](#adding-playlists))
2. **Select a playlist** from the sidebar
3. **Click a channel** to start playing
4. **Enjoy!** 🎉

---

## Adding Playlists

IPTV Player supports three types of playlists:

### 1. M3U URL Playlist

Add a playlist from a web URL:

1. Click the **"+"** button in the playlist sidebar
2. Select **"Add M3U URL"**
3. Enter the playlist URL (e.g., `http://example.com/playlist.m3u`)
4. Enter a name for the playlist
5. Click **"Add"**

**Example URLs:**
- `http://example.com/channels.m3u`
- `https://iptv-provider.com/playlist.m3u8`

### 2. M3U File Playlist

Add a playlist from a local file:

1. Click the **"+"** button in the playlist sidebar
2. Select **"Add M3U File"**
3. Choose an M3U file from your computer
4. Enter a name for the playlist
5. Click **"Add"**

**Supported formats:**
- `.m3u` - Standard M3U format
- `.m3u8` - UTF-8 encoded M3U format

### 3. Xtream Codes Account

Add a playlist using Xtream Codes API:

1. Click the **"+"** button in the playlist sidebar
2. Select **"Add Xtream Account"**
3. Enter your credentials:
   - **Server URL**: Your provider's server (e.g., `http://server.com:8080`)
   - **Username**: Your account username
   - **Password**: Your account password
4. Enter a name for the playlist
5. Click **"Add"**

**Note:** Your credentials are securely stored in macOS Keychain.

### Managing Playlists

**Rename a playlist:**
1. Right-click on the playlist
2. Select **"Rename"**
3. Enter the new name
4. Press **Enter**

**Delete a playlist:**
1. Right-click on the playlist
2. Select **"Delete"**
3. Confirm the deletion

**Refresh a playlist:**
1. Right-click on the playlist
2. Select **"Refresh"**
3. Wait for the channels to reload

---

## Browsing Channels

### Channel List

Once you select a playlist, all channels appear in the center panel.

**Channel Information:**
- **Name**: Channel name
- **Category**: Channel category (News, Sports, Movies, etc.)
- **Logo**: Channel logo/icon (if available)

### Search and Filter

**Search for channels:**
1. Click the search field at the top of the channel list
2. Type your search query
3. Results update as you type

**Filter by category:**
1. Click the **"Category"** dropdown
2. Select a category
3. Only channels in that category will be shown

**Clear filters:**
- Click the **"×"** button in the search field
- Select **"All Categories"** from the category dropdown

### Sorting

Click the column headers to sort channels:
- **Name**: Alphabetical order
- **Category**: Group by category
- **Recently Added**: Newest first

---

## Playing Videos

### Starting Playback

**To play a channel:**
1. Click on a channel in the channel list
2. The video will start playing automatically
3. Playback controls appear at the bottom

### Playback Controls

**Play/Pause:**
- Click the **Play/Pause** button
- Press **Space** on your keyboard

**Volume:**
- Click and drag the volume slider
- Press **↑** / **↓** arrow keys
- Press **M** to mute/unmute

**Seek:**
- Click anywhere on the progress bar
- Press **←** / **→** arrow keys (10 seconds)
- Press **Shift + ←** / **→** (30 seconds)

**Fullscreen:**
- Click the **Fullscreen** button
- Press **F** or **Cmd+F**
- Press **Esc** to exit fullscreen

### Video Quality

The player automatically adjusts quality based on your connection:
- **Auto**: Adapts to network conditions (recommended)
- **HD**: High definition (requires fast connection)
- **SD**: Standard definition (for slower connections)

### Troubleshooting Playback

**Video won't play:**
- Check your internet connection
- Try a different channel
- Refresh the playlist
- See [Troubleshooting](#troubleshooting) section

**Video is buffering:**
- Wait a few seconds for buffering to complete
- Check your internet speed
- Try lowering the quality setting
- Close other bandwidth-intensive applications

**Audio but no video:**
- The stream may be audio-only
- Try a different channel
- Check if hardware acceleration is enabled

---

## Managing Favorites

### Adding Favorites

**To add a channel to favorites:**
1. Find the channel in the channel list
2. Click the **star icon** (☆) next to the channel name
3. The star becomes filled (★) and the channel is added to favorites

**Quick method:**
- Right-click on a channel
- Select **"Add to Favorites"**

### Viewing Favorites

**To see all your favorite channels:**
1. Click **"Favorites"** in the playlist sidebar
2. All favorite channels from all playlists appear
3. Click any channel to play

### Removing Favorites

**To remove a channel from favorites:**
1. Click the filled star icon (★) next to the channel
2. The star becomes empty (☆) and the channel is removed

**From favorites view:**
- Right-click on a channel
- Select **"Remove from Favorites"**

### Organizing Favorites

Favorites are automatically organized by:
- **Playlist**: Group by source playlist
- **Category**: Group by channel category
- **Recently Added**: Most recently favorited first

---

## Keyboard Shortcuts

### General

| Shortcut | Action |
|----------|--------|
| `Cmd + N` | Add new playlist |
| `Cmd + R` | Refresh current playlist |
| `Cmd + F` | Focus search field |
| `Cmd + ,` | Open preferences |
| `Cmd + Q` | Quit application |

### Playback

| Shortcut | Action |
|----------|--------|
| `Space` | Play/Pause |
| `F` or `Cmd + F` | Toggle fullscreen |
| `M` | Mute/Unmute |
| `↑` / `↓` | Volume up/down |
| `←` / `→` | Seek backward/forward (10s) |
| `Shift + ←` / `→` | Seek backward/forward (30s) |
| `Esc` | Exit fullscreen |

### Navigation

| Shortcut | Action |
|----------|--------|
| `Cmd + 1` | Show playlists |
| `Cmd + 2` | Show favorites |
| `↑` / `↓` | Navigate channel list |
| `Enter` | Play selected channel |
| `Cmd + [` | Previous channel |
| `Cmd + ]` | Next channel |

---

## Troubleshooting

### Common Issues

#### Application Won't Launch

**Problem:** App crashes on launch or shows error message

**Solutions:**
1. Check system requirements (macOS 13.0+)
2. Restart your Mac
3. Reinstall the application
4. Check Console.app for error logs

#### Playlist Won't Load

**Problem:** "Failed to load playlist" error

**Solutions:**
1. **Check URL**: Ensure the M3U URL is correct and accessible
2. **Check internet**: Verify your internet connection
3. **Try browser**: Open the URL in Safari to test
4. **Contact provider**: The playlist may be temporarily unavailable
5. **Check format**: Ensure the file is valid M3U format

#### Xtream Login Fails

**Problem:** "Authentication failed" error

**Solutions:**
1. **Verify credentials**: Double-check username and password
2. **Check server URL**: Ensure format is `http://server.com:port`
3. **Test in browser**: Try accessing `http://server.com:port/player_api.php?username=X&password=Y`
4. **Contact provider**: Your account may be expired or suspended
5. **Check firewall**: Ensure the server isn't blocked

#### Video Won't Play

**Problem:** Black screen or error when playing channel

**Solutions:**
1. **Try another channel**: The specific stream may be down
2. **Check internet speed**: Streaming requires stable connection
3. **Refresh playlist**: Right-click playlist → Refresh
4. **Clear cache**: Preferences → Advanced → Clear Cache
5. **Update app**: Check for updates in the Help menu

#### Poor Video Quality

**Problem:** Pixelated or low-quality video

**Solutions:**
1. **Check internet speed**: Run speed test (need 5+ Mbps)
2. **Close other apps**: Free up bandwidth
3. **Move closer to router**: Improve WiFi signal
4. **Use ethernet**: Wired connection is more stable
5. **Contact provider**: Stream quality depends on source

#### Audio/Video Out of Sync

**Problem:** Audio doesn't match video

**Solutions:**
1. **Restart playback**: Stop and play again
2. **Try different channel**: Issue may be with specific stream
3. **Check system load**: Close resource-intensive apps
4. **Restart app**: Quit and relaunch IPTV Player
5. **Report issue**: If persistent, report to support

### Performance Issues

#### High CPU Usage

**Solutions:**
1. Disable hardware acceleration (if causing issues)
2. Close other applications
3. Reduce video quality
4. Check Activity Monitor for other processes
5. Restart the application

#### High Memory Usage

**Solutions:**
1. Restart the application periodically
2. Reduce number of open playlists
3. Clear image cache (Preferences → Advanced)
4. Check for memory leaks (report if persistent)

### Getting Help

If you can't resolve an issue:

1. **Check logs**: Help → Show Logs
2. **Report bug**: Help → Report Issue
3. **Contact support**: Include logs and steps to reproduce
4. **Community forum**: Ask other users for help

---

## Privacy & Security

### Data Storage

**What we store locally:**
- Playlist information (names, URLs)
- Channel data (names, URLs, logos)
- Favorite channels
- Application preferences

**What we DON'T store:**
- Video content (streaming only)
- Browsing history
- Personal information

### Credentials Security

**Xtream Codes credentials** are stored securely:
- Encrypted in macOS Keychain
- Never transmitted except to your IPTV provider
- Can be deleted by removing the playlist

**To remove stored credentials:**
1. Delete the Xtream playlist
2. Or use Keychain Access app to manually remove

### Network Security

**HTTPS Support:**
- Playlists and streams over HTTPS are fully supported
- HTTP connections are allowed but less secure
- We recommend using HTTPS URLs when available

**Firewall:**
- The app requires internet access for streaming
- No incoming connections are needed
- Safe to use behind corporate firewalls

### Privacy Policy

- We don't collect any personal data
- No analytics or tracking
- No ads or third-party services
- Your playlists and favorites stay on your device

---

## FAQ

### General Questions

**Q: Is IPTV Player free?**  
A: Yes, the application is free and open source.

**Q: Do I need an IPTV subscription?**  
A: Yes, you need a playlist URL or Xtream account from an IPTV provider.

**Q: Can I use my own M3U files?**  
A: Yes, you can add local M3U files or URLs.

**Q: Does it work with all IPTV providers?**  
A: It works with any provider that offers M3U playlists or Xtream Codes API.

### Technical Questions

**Q: What video formats are supported?**  
A: HLS (m3u8), RTSP, HTTP streams, and most common formats via AVPlayer.

**Q: Does it support EPG (Electronic Program Guide)?**  
A: EPG support is planned for a future release.

**Q: Can I record streams?**  
A: Recording is not currently supported.

**Q: Does it support multiple audio tracks?**  
A: Yes, if the stream provides multiple audio tracks.

**Q: Can I use it on multiple Macs?**  
A: Yes, but playlists and favorites are stored per device.

### Troubleshooting Questions

**Q: Why do some channels not work?**  
A: Channels may be offline, geo-restricted, or require specific codecs.

**Q: Why is playback stuttering?**  
A: Check your internet speed and close bandwidth-intensive applications.

**Q: Can I use a VPN?**  
A: Yes, VPNs work fine and may be required for geo-restricted content.

**Q: Why can't I add a playlist?**  
A: Check the URL format and ensure the playlist is accessible.

### Feature Requests

**Q: Can you add feature X?**  
A: Please open an issue on GitHub with your feature request.

**Q: Will there be an iOS version?**  
A: An iOS version is not currently planned.

**Q: Can I contribute to development?**  
A: Yes! The project is open source. See the GitHub repository.

---

## Support

### Getting Help

- **Documentation**: Read this guide and other docs in the Help menu
- **GitHub Issues**: Report bugs and request features
- **Community**: Join discussions on GitHub
- **Email**: [Add support email]

### Reporting Bugs

When reporting bugs, please include:
1. macOS version
2. App version (Help → About)
3. Steps to reproduce
4. Error messages or logs
5. Screenshots if applicable

### Contributing

We welcome contributions! See the [Contributing Guide](../CONTRIBUTING.md) for details.

---

## Credits

**Developed by:** [Your Name/Team]  
**License:** [License Type]  
**Source Code:** [GitHub URL]

**Third-Party Libraries:**
- AVFoundation (Apple)
- SwiftUI (Apple)
- Core Data (Apple)

---

## Version History

### Version 1.0.0 (Current)
- Initial release
- M3U playlist support
- Xtream Codes API support
- Favorite channels
- Hardware-accelerated playback
- Native macOS UI

### Planned Features
- EPG support
- Recording capability
- Picture-in-picture mode
- Chromecast support
- Playlist synchronization

---

**Last Updated:** November 29, 2025  
**App Version:** 1.0.0  
**macOS Compatibility:** 13.0+

For the latest version of this guide, visit: [Documentation URL]
