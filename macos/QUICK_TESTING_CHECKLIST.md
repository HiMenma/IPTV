# Quick Testing Checklist - macOS IPTV Player

**Date:** ___________  
**Tester:** ___________  
**Build:** ___________

## Pre-Flight Check
- [ ] App builds successfully
- [ ] App launches without crash
- [ ] Main window appears

## Core Flows (5-10 minutes)

### Flow 1: M3U Playlist
- [ ] Add M3U URL → Success
- [ ] Channels appear in list
- [ ] Select channel → Player opens
- [ ] Video plays
- [ ] Controls work (play/pause/volume)

### Flow 2: Xtream Codes
- [ ] Add Xtream account → Success
- [ ] Channels load
- [ ] Categories available
- [ ] Select channel → Video plays

### Flow 3: Favorites
- [ ] Add channel to favorites → Success
- [ ] View favorites list
- [ ] Play favorite channel
- [ ] Remove from favorites → Success

## Error Handling (2-3 minutes)
- [ ] Invalid URL → Error shown
- [ ] Bad credentials → Error shown
- [ ] Dead stream → Error shown
- [ ] App remains stable

## Quick UI Check (1-2 minutes)
- [ ] Search works
- [ ] Sidebar toggle works
- [ ] Dark mode looks good
- [ ] Window resizes properly

## Data Persistence (1 minute)
- [ ] Close and reopen app
- [ ] Playlists still there
- [ ] Favorites preserved

## Overall Assessment
- [ ] **PASS** - Ready for release
- [ ] **FAIL** - Issues found (see notes)

**Critical Issues:** ___________________________________________

**Notes:** ___________________________________________

