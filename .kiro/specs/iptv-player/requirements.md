# Requirements Document

## Introduction

本文档定义了一个跨平台IPTV播放器应用的需求。该播放器使用Flutter框架开发，支持Android、iOS、Web、Windows、macOS和Linux平台。播放器集成Chewie开源播放器组件，支持多种内容源（Xtream Codes API、M3U/M3U8文件和网络链接），并提供配置管理、收藏、浏览历史、深色模式等功能。

## Glossary

- **IPTV Player**: 互联网协议电视播放器应用程序
- **Xtream Codes**: 一种IPTV服务提供商使用的API协议，用于管理和分发直播流
- **M3U**: 一种播放列表文件格式，包含媒体文件的URL列表
- **M3U8**: M3U文件的UTF-8编码版本，常用于HLS流媒体
- **Configuration**: 用户创建的播放源配置，可以是Xtream账号、M3U文件或网络链接
- **Channel**: 频道，指IPTV播放列表中的单个媒体流项目
- **Favorite**: 用户标记为收藏的频道
- **Browse History**: 用户浏览和播放频道的历史记录
- **Chewie**: 一个开源的Flutter视频播放器包，基于video_player插件提供丰富的播放控制界面
- **Dark Mode**: 深色模式，使用深色背景和浅色文本的UI主题，适合低光环境使用

## Requirements

### Requirement 1

**User Story:** 作为用户，我想要在多个平台上使用同一个播放器应用，以便在不同设备上获得一致的观看体验

#### Acceptance Criteria

1. WHEN the application is built for Android THEN the IPTV Player SHALL run on Android devices with full functionality
2. WHEN the application is built for iOS THEN the IPTV Player SHALL run on iOS devices with full functionality
3. WHEN the application is built for Web THEN the IPTV Player SHALL run in web browsers with full functionality
4. WHEN the application is built for Windows THEN the IPTV Player SHALL run on Windows systems with full functionality
5. WHEN the application is built for macOS THEN the IPTV Player SHALL run on macOS systems with full functionality
6. WHEN the application is built for Linux THEN the IPTV Player SHALL run on Linux systems with full functionality

### Requirement 2

**User Story:** 作为用户，我想要使用Xtream Codes账号登录，以便访问我订阅的IPTV服务

#### Acceptance Criteria

1. WHEN a user provides Xtream server URL, username, and password THEN the IPTV Player SHALL authenticate with the Xtream API and retrieve available channels
2. WHEN Xtream authentication succeeds THEN the IPTV Player SHALL store the configuration and display the channel list
3. WHEN Xtream authentication fails THEN the IPTV Player SHALL display an error message and prevent configuration creation
4. WHEN a user requests to refresh a Xtream configuration THEN the IPTV Player SHALL re-fetch the channel list from the Xtream API
5. WHEN a user exports a Xtream configuration THEN the IPTV Player SHALL convert the channel list to M3U format and save it as a local file

### Requirement 3

**User Story:** 作为用户，我想要导入M3U或M3U8文件，以便播放本地或网络上的播放列表

#### Acceptance Criteria

1. WHEN a user selects a local M3U file THEN the IPTV Player SHALL parse the file and display the channel list
2. WHEN a user selects a local M3U8 file THEN the IPTV Player SHALL parse the file and display the channel list
3. WHEN a user provides an M3U network URL THEN the IPTV Player SHALL fetch and parse the remote file and display the channel list
4. WHEN a user provides an M3U8 network URL THEN the IPTV Player SHALL fetch and parse the remote file and display the channel list
5. WHEN M3U parsing fails THEN the IPTV Player SHALL display an error message with details about the parsing failure
6. WHEN a user requests to refresh an M3U network configuration THEN the IPTV Player SHALL re-fetch and parse the remote file

### Requirement 4

**User Story:** 作为用户，我想要管理多个配置，以便在不同的IPTV源之间切换

#### Acceptance Criteria

1. WHEN a user creates a new configuration THEN the IPTV Player SHALL save it with a user-provided name
2. WHEN a user renames a configuration THEN the IPTV Player SHALL update the configuration name while preserving all other data
3. WHEN a user deletes a configuration THEN the IPTV Player SHALL remove it from storage and update the configuration list
4. WHEN configurations are stored THEN the IPTV Player SHALL persist them to local storage for future sessions
5. WHEN the application starts THEN the IPTV Player SHALL load all saved configurations and display them in the configuration list

### Requirement 5

**User Story:** 作为用户，我想要播放选中的频道，以便观看IPTV内容

#### Acceptance Criteria

1. WHEN a user selects a channel THEN the IPTV Player SHALL use Aliyun Player Widget to play the channel stream
2. WHEN playback starts THEN the IPTV Player SHALL display video content with playback controls
3. WHEN a stream URL is invalid or unavailable THEN the IPTV Player SHALL display an error message and handle the failure gracefully
4. WHEN a user stops playback THEN the IPTV Player SHALL release player resources and return to the channel list
5. WHEN playback is in progress THEN the IPTV Player SHALL provide controls for pause, resume, volume adjustment, and fullscreen

### Requirement 6

**User Story:** 作为用户，我想要收藏喜欢的频道，以便快速访问常看的内容

#### Acceptance Criteria

1. WHEN a user marks a channel as favorite THEN the IPTV Player SHALL add it to the favorites list
2. WHEN a user unmarks a favorite channel THEN the IPTV Player SHALL remove it from the favorites list
3. WHEN favorites are modified THEN the IPTV Player SHALL persist the changes to local storage
4. WHEN the application starts THEN the IPTV Player SHALL load saved favorites and display them
5. WHEN a user views favorites THEN the IPTV Player SHALL display all favorited channels in a dedicated section

### Requirement 7

**User Story:** 作为用户，我想要查看浏览历史，以便找回之前观看过的频道

#### Acceptance Criteria

1. WHEN a user plays a channel THEN the IPTV Player SHALL record it in the browse history with timestamp
2. WHEN browse history is recorded THEN the IPTV Player SHALL persist it to local storage
3. WHEN the application starts THEN the IPTV Player SHALL load saved browse history
4. WHEN a user views browse history THEN the IPTV Player SHALL display previously watched channels in reverse chronological order
5. WHEN a user clears browse history THEN the IPTV Player SHALL remove all history records from storage

### Requirement 8

**User Story:** 作为用户，我想要看到应用的品牌标识，以便识别应用

#### Acceptance Criteria

1. WHEN the application is built THEN the IPTV Player SHALL use the logo files from the icon folder as application icons
2. WHEN the application is installed on a device THEN the IPTV Player SHALL display the custom logo as the app icon
3. WHEN the application launches THEN the IPTV Player SHALL display the logo in the splash screen or initial loading state

### Requirement 9

**User Story:** 作为开发者，我想要使用免费开源的播放器组件，以便获得稳定可靠的播放能力而无需付费

#### Acceptance Criteria

1. WHEN the application is built THEN the IPTV Player SHALL integrate the Chewie package from https://pub.dev/packages/chewie
2. WHEN a stream needs to be played THEN the IPTV Player SHALL use Chewie APIs to initialize and control playback
3. WHEN playback errors occur THEN the IPTV Player SHALL handle Chewie error callbacks appropriately
4. WHEN the player is disposed THEN the IPTV Player SHALL properly release Chewie resources

### Requirement 10

**User Story:** 作为用户，我想要应用支持深色模式，以便在不同光线环境下获得舒适的观看体验

#### Acceptance Criteria

1. WHEN the system is set to dark mode THEN the IPTV Player SHALL display a dark theme with appropriate colors
2. WHEN the system is set to light mode THEN the IPTV Player SHALL display a light theme with appropriate colors
3. WHEN the theme changes THEN the IPTV Player SHALL update all UI components to match the new theme
4. WHEN displaying video content THEN the IPTV Player SHALL maintain consistent player controls styling across both themes
5. WHEN displaying text and icons THEN the IPTV Player SHALL ensure sufficient contrast for readability in both themes
