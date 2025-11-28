# Design Document

## Overview

本设计文档描述了将 IPTV 播放器桌面平台从 Kotlin Multiplatform 迁移到原生开发的技术方案。我们将为 macOS 和 Windows 平台分别使用 Swift/SwiftUI 和 C#/WPF 重新实现桌面应用，同时保持核心业务逻辑的可复用性。

### 设计目标

1. **原生体验**: 为每个平台提供符合平台设计规范的用户界面和交互体验
2. **高性能**: 利用平台原生的视频播放能力，实现流畅的视频播放体验
3. **代码复用**: 在可能的情况下复用业务逻辑代码，减少重复开发
4. **可维护性**: 建立清晰的架构边界，便于长期维护和功能扩展
5. **独立性**: 保持 Android 应用的独立性，不影响现有开发流程

## Architecture

### 整体架构

```
┌─────────────────────────────────────────────────────────────┐
│                     Application Layer                        │
├──────────────────────┬──────────────────────────────────────┤
│   macOS App          │         Windows App                   │
│   (Swift/SwiftUI)    │         (C#/WPF)                     │
└──────────────────────┴──────────────────────────────────────┘
           │                            │
           └────────────┬───────────────┘
                        │
┌───────────────────────▼───────────────────────────────────┐
│              Shared Business Logic Layer                   │
│  ┌─────────────┬──────────────┬────────────────────────┐  │
│  │ M3U Parser  │ Xtream API   │ Data Models           │  │
│  │             │ Client       │                        │  │
│  └─────────────┴──────────────┴────────────────────────┘  │
└────────────────────────────────────────────────────────────┘
           │                            │
┌──────────▼────────────┐    ┌─────────▼──────────────┐
│  Platform Services    │    │  Platform Services     │
│  (macOS)              │    │  (Windows)             │
│  - AVPlayer           │    │  - MediaElement        │
│  - Core Data          │    │  - SQLite/EF Core      │
│  - URLSession         │    │  - HttpClient          │
└───────────────────────┘    └────────────────────────┘
```

### 架构层次

#### 1. Application Layer (应用层)
- **macOS App**: 使用 Swift 和 SwiftUI 实现的原生 macOS 应用
- **Windows App**: 使用 C# 和 WPF 实现的原生 Windows 应用
- 负责 UI 渲染、用户交互、导航和状态管理

#### 2. Shared Business Logic Layer (共享业务逻辑层)
- M3U 播放列表解析
- Xtream Codes API 客户端
- 数据模型定义
- 业务规则和验证逻辑

#### 3. Platform Services Layer (平台服务层)
- 视频播放引擎
- 数据持久化
- 网络请求
- 文件系统访问

### 代码共享策略

我们将采用以下策略来实现代码复用：

**选项 1: 保留 Kotlin 共享库**
- 将现有的 Kotlin 共享代码编译为原生库
- macOS: 通过 Kotlin/Native 编译为 Framework
- Windows: 通过 Kotlin/Native 编译为 DLL
- 优点: 最大化代码复用，减少重写工作
- 缺点: 需要维护 Kotlin/Native 工具链

**选项 2: 使用 Rust 重写共享逻辑**
- 将核心业务逻辑用 Rust 重写
- 通过 FFI 为 Swift 和 C# 提供接口
- 优点: 高性能，良好的跨平台支持
- 缺点: 需要学习 Rust，重写工作量大

**选项 3: 各平台独立实现**
- 在 Swift 和 C# 中分别实现业务逻辑
- 优点: 完全原生，无额外依赖
- 缺点: 代码重复，维护成本高

**推荐方案**: 选项 1（保留 Kotlin 共享库），因为可以最大化利用现有代码，减少迁移工作量。

## Components and Interfaces

### macOS Application Components

#### 1. UI Layer (SwiftUI)

**MainView**
```swift
struct MainView: View {
    @StateObject private var viewModel: MainViewModel
    
    var body: some View {
        NavigationSplitView {
            PlaylistSidebarView(viewModel: viewModel)
        } detail: {
            ChannelListView(viewModel: viewModel)
        }
    }
}
```

**PlaylistSidebarView**
- 显示播放列表列表
- 支持添加、删除、重命名播放列表
- 显示收藏夹

**ChannelListView**
- 显示选中播放列表的频道列表
- 支持搜索和过滤
- 显示频道缩略图和分组

**PlayerView**
- 视频播放界面
- 播放控制（播放/暂停、音量、全屏）
- 显示频道信息

#### 2. ViewModel Layer

**MainViewModel**
```swift
@MainActor
class MainViewModel: ObservableObject {
    @Published var playlists: [Playlist] = []
    @Published var selectedPlaylist: Playlist?
    @Published var channels: [Channel] = []
    @Published var selectedChannel: Channel?
    @Published var isLoading: Bool = false
    @Published var errorMessage: String?
    
    private let playlistRepository: PlaylistRepository
    private let m3uParser: M3UParser
    private let xtreamClient: XtreamClient
    
    func addM3UPlaylist(url: String) async
    func addXtreamAccount(serverUrl: String, username: String, password: String) async
    func loadPlaylists() async
    func selectPlaylist(_ playlist: Playlist) async
    func deletePlaylist(_ playlist: Playlist) async
}
```

**PlayerViewModel**
```swift
@MainActor
class PlayerViewModel: ObservableObject {
    @Published var isPlaying: Bool = false
    @Published var volume: Double = 1.0
    @Published var isFullscreen: Bool = false
    @Published var currentTime: TimeInterval = 0
    @Published var duration: TimeInterval = 0
    
    private let player: AVPlayer
    
    func play(channel: Channel)
    func pause()
    func resume()
    func stop()
    func seek(to time: TimeInterval)
    func setVolume(_ volume: Double)
    func toggleFullscreen()
}
```

#### 3. Service Layer

**PlaylistRepository**
```swift
protocol PlaylistRepository {
    func getAllPlaylists() async throws -> [Playlist]
    func getPlaylist(id: String) async throws -> Playlist?
    func savePlaylist(_ playlist: Playlist) async throws
    func deletePlaylist(id: String) async throws
    func updatePlaylist(_ playlist: Playlist) async throws
}

class CoreDataPlaylistRepository: PlaylistRepository {
    private let context: NSManagedObjectContext
    // Implementation using Core Data
}
```

**M3UParser**
```swift
protocol M3UParser {
    func parse(content: String) async throws -> [Channel]
}

class M3UParserImpl: M3UParser {
    func parse(content: String) async throws -> [Channel] {
        // Call Kotlin shared library or implement in Swift
    }
}
```

**XtreamClient**
```swift
protocol XtreamClient {
    func authenticate(account: XtreamAccount) async throws -> Bool
    func getLiveCategories(account: XtreamAccount) async throws -> [Category]
    func getLiveStreams(account: XtreamAccount) async throws -> [Channel]
}

class XtreamClientImpl: XtreamClient {
    private let httpClient: URLSession
    // Implementation using URLSession
}
```

**VideoPlayerService**
```swift
protocol VideoPlayerService {
    func play(url: URL)
    func pause()
    func resume()
    func stop()
    func seek(to time: TimeInterval)
    var currentTime: TimeInterval { get }
    var duration: TimeInterval { get }
}

class AVPlayerService: VideoPlayerService {
    private let player: AVPlayer
    // Implementation using AVPlayer
}
```

### Windows Application Components

#### 1. UI Layer (WPF/XAML)

**MainWindow.xaml**
```xml
<Window x:Class="IPTVPlayer.MainWindow">
    <Grid>
        <Grid.ColumnDefinitions>
            <ColumnDefinition Width="250"/>
            <ColumnDefinition Width="*"/>
        </Grid.ColumnDefinitions>
        
        <PlaylistSidebar Grid.Column="0"/>
        <ChannelList Grid.Column="1"/>
    </Grid>
</Window>
```

**PlaylistSidebar**
- 显示播放列表列表
- 支持添加、删除、重命名播放列表
- 显示收藏夹

**ChannelList**
- 显示选中播放列表的频道列表
- 支持搜索和过滤
- 显示频道缩略图和分组

**PlayerWindow**
- 视频播放界面
- 播放控制（播放/暂停、音量、全屏）
- 显示频道信息

#### 2. ViewModel Layer (MVVM)

**MainViewModel.cs**
```csharp
public class MainViewModel : INotifyPropertyChanged
{
    private readonly IPlaylistRepository _playlistRepository;
    private readonly IM3UParser _m3uParser;
    private readonly IXtreamClient _xtreamClient;
    
    public ObservableCollection<Playlist> Playlists { get; set; }
    public Playlist SelectedPlaylist { get; set; }
    public ObservableCollection<Channel> Channels { get; set; }
    public Channel SelectedChannel { get; set; }
    public bool IsLoading { get; set; }
    public string ErrorMessage { get; set; }
    
    public ICommand AddM3UPlaylistCommand { get; }
    public ICommand AddXtreamAccountCommand { get; }
    public ICommand DeletePlaylistCommand { get; }
    public ICommand SelectPlaylistCommand { get; }
    
    public async Task AddM3UPlaylist(string url)
    public async Task AddXtreamAccount(string serverUrl, string username, string password)
    public async Task LoadPlaylists()
    public async Task SelectPlaylist(Playlist playlist)
    public async Task DeletePlaylist(Playlist playlist)
}
```

**PlayerViewModel.cs**
```csharp
public class PlayerViewModel : INotifyPropertyChanged
{
    private readonly IVideoPlayerService _playerService;
    
    public bool IsPlaying { get; set; }
    public double Volume { get; set; }
    public bool IsFullscreen { get; set; }
    public TimeSpan CurrentTime { get; set; }
    public TimeSpan Duration { get; set; }
    
    public ICommand PlayCommand { get; }
    public ICommand PauseCommand { get; }
    public ICommand StopCommand { get; }
    public ICommand SeekCommand { get; }
    public ICommand ToggleFullscreenCommand { get; }
    
    public void Play(Channel channel)
    public void Pause()
    public void Resume()
    public void Stop()
    public void Seek(TimeSpan time)
    public void SetVolume(double volume)
    public void ToggleFullscreen()
}
```

#### 3. Service Layer

**IPlaylistRepository.cs**
```csharp
public interface IPlaylistRepository
{
    Task<List<Playlist>> GetAllPlaylists();
    Task<Playlist> GetPlaylist(string id);
    Task SavePlaylist(Playlist playlist);
    Task DeletePlaylist(string id);
    Task UpdatePlaylist(Playlist playlist);
}

public class SqlitePlaylistRepository : IPlaylistRepository
{
    private readonly string _connectionString;
    // Implementation using SQLite or Entity Framework Core
}
```

**IM3UParser.cs**
```csharp
public interface IM3UParser
{
    Task<List<Channel>> Parse(string content);
}

public class M3UParser : IM3UParser
{
    public async Task<List<Channel>> Parse(string content)
    {
        // Call Kotlin shared library or implement in C#
    }
}
```

**IXtreamClient.cs**
```csharp
public interface IXtreamClient
{
    Task<bool> Authenticate(XtreamAccount account);
    Task<List<Category>> GetLiveCategories(XtreamAccount account);
    Task<List<Channel>> GetLiveStreams(XtreamAccount account);
}

public class XtreamClient : IXtreamClient
{
    private readonly HttpClient _httpClient;
    // Implementation using HttpClient
}
```

**IVideoPlayerService.cs**
```csharp
public interface IVideoPlayerService
{
    void Play(Uri url);
    void Pause();
    void Resume();
    void Stop();
    void Seek(TimeSpan time);
    TimeSpan CurrentTime { get; }
    TimeSpan Duration { get; }
    event EventHandler<PlayerStateChangedEventArgs> StateChanged;
}

public class MediaElementPlayerService : IVideoPlayerService
{
    // Implementation using WPF MediaElement
}

public class LibVLCPlayerService : IVideoPlayerService
{
    // Alternative implementation using LibVLCSharp
}
```

### Shared Business Logic (Kotlin)

如果选择保留 Kotlin 共享库，需要定义清晰的接口：

**KotlinBridge (macOS)**
```swift
// Generated from Kotlin/Native
class KotlinM3UParser {
    func parse(content: String) -> [KotlinChannel]
}

class KotlinXtreamClient {
    func authenticate(account: KotlinXtreamAccount) async -> Bool
    func getLiveStreams(account: KotlinXtreamAccount) async -> [KotlinChannel]
}
```

**KotlinBridge (Windows)**
```csharp
// P/Invoke to Kotlin/Native DLL
[DllImport("iptv_shared.dll")]
public static extern IntPtr ParseM3U(string content);

[DllImport("iptv_shared.dll")]
public static extern bool AuthenticateXtream(string serverUrl, string username, string password);
```

## Data Models

### Core Data Models

所有平台共享相同的数据模型结构：

**Channel**
```
{
    id: String
    name: String
    url: String
    logoUrl: String?
    group: String?
    categoryId: String?
    headers: Map<String, String>
}
```

**Playlist**
```
{
    id: String
    name: String
    url: String?
    type: PlaylistType (M3U_URL, M3U_FILE, XTREAM)
    channels: List<Channel>
    categories: List<Category>
    xtreamAccount: XtreamAccount?
    createdAt: DateTime
    updatedAt: DateTime
}
```

**Category**
```
{
    id: String
    name: String
    parentId: String?
}
```

**XtreamAccount**
```
{
    serverUrl: String
    username: String
    password: String
}
```

**Favorite**
```
{
    id: String
    channelId: String
    playlistId: String
    createdAt: DateTime
}
```

### Database Schema

#### macOS (Core Data)

**PlaylistEntity**
- id: String (Primary Key)
- name: String
- url: String?
- type: String
- createdAt: Date
- updatedAt: Date
- channels: Relationship (One-to-Many)
- xtreamAccount: Relationship (One-to-One)

**ChannelEntity**
- id: String (Primary Key)
- name: String
- url: String
- logoUrl: String?
- group: String?
- categoryId: String?
- playlist: Relationship (Many-to-One)

**FavoriteEntity**
- id: String (Primary Key)
- channelId: String
- playlistId: String
- createdAt: Date

#### Windows (SQLite/Entity Framework Core)

**Playlists Table**
```sql
CREATE TABLE Playlists (
    Id TEXT PRIMARY KEY,
    Name TEXT NOT NULL,
    Url TEXT,
    Type TEXT NOT NULL,
    CreatedAt DATETIME NOT NULL,
    UpdatedAt DATETIME NOT NULL
);
```

**Channels Table**
```sql
CREATE TABLE Channels (
    Id TEXT PRIMARY KEY,
    Name TEXT NOT NULL,
    Url TEXT NOT NULL,
    LogoUrl TEXT,
    GroupName TEXT,
    CategoryId TEXT,
    PlaylistId TEXT NOT NULL,
    FOREIGN KEY (PlaylistId) REFERENCES Playlists(Id) ON DELETE CASCADE
);
```

**XtreamAccounts Table**
```sql
CREATE TABLE XtreamAccounts (
    Id TEXT PRIMARY KEY,
    ServerUrl TEXT NOT NULL,
    Username TEXT NOT NULL,
    Password TEXT NOT NULL,
    PlaylistId TEXT NOT NULL,
    FOREIGN KEY (PlaylistId) REFERENCES Playlists(Id) ON DELETE CASCADE
);
```

**Favorites Table**
```sql
CREATE TABLE Favorites (
    Id TEXT PRIMARY KEY,
    ChannelId TEXT NOT NULL,
    PlaylistId TEXT NOT NULL,
    CreatedAt DATETIME NOT NULL,
    FOREIGN KEY (ChannelId) REFERENCES Channels(Id) ON DELETE CASCADE,
    FOREIGN KEY (PlaylistId) REFERENCES Playlists(Id) ON DELETE CASCADE
);
```

## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system-essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

Based on the prework analysis, the following correctness properties have been identified:

### Property 1: M3U Parser Field Extraction
*For any* valid M3U content containing EXTINF tags with channel metadata, parsing should extract all specified fields (name, URL, logo, group) correctly
**Validates: Requirements 5.1, 5.2, 5.3, 5.4**

### Property 2: M3U Parser Error Resilience
*For any* M3U content containing malformed entries, the parser should continue processing valid entries and return partial results without crashing
**Validates: Requirements 5.5**

### Property 3: Channel List Display Completeness
*For any* list of channels, the UI should display all required information (name, category, thumbnail) for each channel
**Validates: Requirements 3.4, 4.4**

### Property 4: Player Control State Consistency
*For any* player control operation (play, pause, volume change), the player state should update consistently to reflect the operation
**Validates: Requirements 3.6, 4.6, 7.7**

### Property 5: Favorite Persistence
*For any* channel marked as favorite, the favorite record should persist in the database and be retrievable after application restart
**Validates: Requirements 3.7, 4.7, 8.2**

### Property 6: Playlist Persistence Round Trip
*For any* playlist added to the system, saving and then loading should return an equivalent playlist with all data intact
**Validates: Requirements 8.1, 8.3**

### Property 7: Playlist Deletion Completeness
*For any* playlist deleted from the system, all associated data (channels, favorites) should be removed from the database
**Validates: Requirements 8.4**

### Property 8: Database Transaction Consistency
*For any* database operation that fails, the system should maintain data consistency by rolling back partial changes
**Validates: Requirements 8.5**

### Property 9: Xtream API Error Handling
*For any* Xtream API request that fails, the system should return a clear error message and support retry without crashing
**Validates: Requirements 6.6**

## Error Handling

### Error Categories

#### 1. Network Errors
- **Connection Timeout**: 当网络请求超时时，显示友好的错误消息并提供重试选项
- **No Internet Connection**: 检测网络连接状态，提示用户检查网络设置
- **Server Error (5xx)**: 显示服务器错误消息，建议稍后重试
- **Client Error (4xx)**: 显示具体的错误原因（如认证失败、资源不存在）

#### 2. Parsing Errors
- **Invalid M3U Format**: 提示用户 M3U 文件格式不正确，显示具体的错误位置
- **Invalid JSON Response**: 记录错误日志，提示用户服务器响应格式异常
- **Encoding Issues**: 尝试多种编码方式解析，失败时提示用户

#### 3. Database Errors
- **Connection Failed**: 提示数据库连接失败，建议重启应用
- **Query Failed**: 记录错误日志，显示通用错误消息
- **Constraint Violation**: 提示用户操作违反数据完整性约束
- **Disk Full**: 提示用户磁盘空间不足

#### 4. Player Errors
- **Stream Not Found**: 提示频道流不可用，建议尝试其他频道
- **Unsupported Format**: 提示视频格式不支持，建议更新播放器
- **Decoding Error**: 显示解码错误，建议检查网络或尝试其他频道
- **Hardware Acceleration Failed**: 回退到软件解码，记录警告日志

### Error Handling Strategy

#### macOS Implementation

```swift
enum AppError: LocalizedError {
    case networkError(underlying: Error)
    case parsingError(message: String)
    case databaseError(underlying: Error)
    case playerError(message: String)
    
    var errorDescription: String? {
        switch self {
        case .networkError(let error):
            return "网络错误: \(error.localizedDescription)"
        case .parsingError(let message):
            return "解析错误: \(message)"
        case .databaseError(let error):
            return "数据库错误: \(error.localizedDescription)"
        case .playerError(let message):
            return "播放器错误: \(message)"
        }
    }
    
    var recoverySuggestion: String? {
        switch self {
        case .networkError:
            return "请检查网络连接并重试"
        case .parsingError:
            return "请检查文件格式是否正确"
        case .databaseError:
            return "请尝试重启应用"
        case .playerError:
            return "请尝试播放其他频道"
        }
    }
}

// Usage in ViewModel
func addM3UPlaylist(url: String) async {
    do {
        let content = try await downloadM3U(url: url)
        let channels = try await m3uParser.parse(content: content)
        let playlist = Playlist(name: "New Playlist", channels: channels)
        try await playlistRepository.savePlaylist(playlist)
    } catch let error as AppError {
        errorMessage = error.localizedDescription
    } catch {
        errorMessage = "未知错误: \(error.localizedDescription)"
    }
}
```

#### Windows Implementation

```csharp
public class AppException : Exception
{
    public ErrorCategory Category { get; }
    public string RecoverySuggestion { get; }
    
    public AppException(ErrorCategory category, string message, string recoverySuggestion, Exception innerException = null)
        : base(message, innerException)
    {
        Category = category;
        RecoverySuggestion = recoverySuggestion;
    }
}

public enum ErrorCategory
{
    Network,
    Parsing,
    Database,
    Player
}

// Usage in ViewModel
public async Task AddM3UPlaylist(string url)
{
    try
    {
        var content = await DownloadM3U(url);
        var channels = await _m3uParser.Parse(content);
        var playlist = new Playlist { Name = "New Playlist", Channels = channels };
        await _playlistRepository.SavePlaylist(playlist);
    }
    catch (AppException ex)
    {
        ErrorMessage = $"{ex.Message}\n{ex.RecoverySuggestion}";
        // Log error
    }
    catch (Exception ex)
    {
        ErrorMessage = $"未知错误: {ex.Message}";
        // Log error
    }
}
```

### Retry Mechanism

对于网络请求和临时性错误，实现指数退避重试机制：

```swift
// macOS
func retryWithExponentialBackoff<T>(
    maxAttempts: Int = 3,
    initialDelay: TimeInterval = 1.0,
    operation: @escaping () async throws -> T
) async throws -> T {
    var attempt = 0
    var delay = initialDelay
    
    while attempt < maxAttempts {
        do {
            return try await operation()
        } catch {
            attempt += 1
            if attempt >= maxAttempts {
                throw error
            }
            try await Task.sleep(nanoseconds: UInt64(delay * 1_000_000_000))
            delay *= 2
        }
    }
    
    fatalError("Should not reach here")
}
```

```csharp
// Windows
public async Task<T> RetryWithExponentialBackoff<T>(
    Func<Task<T>> operation,
    int maxAttempts = 3,
    TimeSpan? initialDelay = null)
{
    var delay = initialDelay ?? TimeSpan.FromSeconds(1);
    var attempt = 0;
    
    while (attempt < maxAttempts)
    {
        try
        {
            return await operation();
        }
        catch (Exception ex)
        {
            attempt++;
            if (attempt >= maxAttempts)
            {
                throw;
            }
            await Task.Delay(delay);
            delay = TimeSpan.FromSeconds(delay.TotalSeconds * 2);
        }
    }
    
    throw new InvalidOperationException("Should not reach here");
}
```

## Testing Strategy

### Unit Testing

#### macOS (XCTest)

**测试框架**: XCTest
**测试目标**: 
- ViewModel 逻辑
- Service 层业务逻辑
- 数据模型转换
- M3U 解析器
- Xtream API 客户端

**示例测试**:
```swift
import XCTest
@testable import IPTVPlayer

class M3UParserTests: XCTestCase {
    var parser: M3UParser!
    
    override func setUp() {
        super.setUp()
        parser = M3UParserImpl()
    }
    
    func testParseValidM3U() async throws {
        let content = """
        #EXTM3U
        #EXTINF:-1 tvg-id="cnn" tvg-logo="http://logo.png" group-title="News",CNN
        http://stream.cnn.com/live
        """
        
        let channels = try await parser.parse(content: content)
        
        XCTAssertEqual(channels.count, 1)
        XCTAssertEqual(channels[0].name, "CNN")
        XCTAssertEqual(channels[0].url, "http://stream.cnn.com/live")
        XCTAssertEqual(channels[0].logoUrl, "http://logo.png")
        XCTAssertEqual(channels[0].group, "News")
    }
    
    func testParseMalformedM3U() async throws {
        let content = """
        #EXTM3U
        #EXTINF:-1,Valid Channel
        http://valid.stream
        Invalid Line Without URL
        #EXTINF:-1,Another Valid
        http://another.stream
        """
        
        let channels = try await parser.parse(content: content)
        
        // Should parse valid entries and skip invalid ones
        XCTAssertEqual(channels.count, 2)
    }
}
```

#### Windows (xUnit)

**测试框架**: xUnit
**测试目标**:
- ViewModel 逻辑
- Service 层业务逻辑
- 数据模型转换
- M3U 解析器
- Xtream API 客户端

**示例测试**:
```csharp
using Xunit;
using IPTVPlayer.Services;

public class M3UParserTests
{
    private readonly IM3UParser _parser;
    
    public M3UParserTests()
    {
        _parser = new M3UParser();
    }
    
    [Fact]
    public async Task ParseValidM3U_ReturnsChannels()
    {
        var content = @"
#EXTM3U
#EXTINF:-1 tvg-id=""cnn"" tvg-logo=""http://logo.png"" group-title=""News"",CNN
http://stream.cnn.com/live
";
        
        var channels = await _parser.Parse(content);
        
        Assert.Single(channels);
        Assert.Equal("CNN", channels[0].Name);
        Assert.Equal("http://stream.cnn.com/live", channels[0].Url);
        Assert.Equal("http://logo.png", channels[0].LogoUrl);
        Assert.Equal("News", channels[0].Group);
    }
    
    [Fact]
    public async Task ParseMalformedM3U_ReturnsValidChannels()
    {
        var content = @"
#EXTM3U
#EXTINF:-1,Valid Channel
http://valid.stream
Invalid Line Without URL
#EXTINF:-1,Another Valid
http://another.stream
";
        
        var channels = await _parser.Parse(content);
        
        // Should parse valid entries and skip invalid ones
        Assert.Equal(2, channels.Count);
    }
}
```

### Property-Based Testing

由于这是一个迁移项目，主要关注功能实现而非算法正确性，property-based testing 将作为可选的增强测试手段。

**macOS (swift-check)**:
```swift
import SwiftCheck
@testable import IPTVPlayer

class M3UParserPropertyTests: XCTestCase {
    func testParserPreservesChannelCount() {
        property("Parser should preserve channel count") <- forAll { (channels: [TestChannel]) in
            let m3uContent = generateM3U(from: channels)
            let parsed = try! await parser.parse(content: m3uContent)
            return parsed.count == channels.count
        }
    }
}
```

**Windows (FsCheck)**:
```csharp
using FsCheck;
using FsCheck.Xunit;

public class M3UParserPropertyTests
{
    [Property]
    public Property ParserPreservesChannelCount()
    {
        return Prop.ForAll<TestChannel[]>(channels =>
        {
            var m3uContent = GenerateM3U(channels);
            var parsed = _parser.Parse(m3uContent).Result;
            return parsed.Count == channels.Length;
        });
    }
}
```

### Integration Testing

**测试目标**:
- 数据库操作的完整流程
- 网络请求和响应处理
- 播放器初始化和控制
- UI 导航流程

**macOS**:
```swift
class PlaylistRepositoryIntegrationTests: XCTestCase {
    var repository: PlaylistRepository!
    var context: NSManagedObjectContext!
    
    override func setUp() {
        super.setUp()
        // Setup in-memory Core Data stack
        context = createInMemoryContext()
        repository = CoreDataPlaylistRepository(context: context)
    }
    
    func testSaveAndLoadPlaylist() async throws {
        let playlist = Playlist(
            id: UUID().uuidString,
            name: "Test Playlist",
            type: .m3uUrl,
            channels: []
        )
        
        try await repository.savePlaylist(playlist)
        let loaded = try await repository.getPlaylist(id: playlist.id)
        
        XCTAssertEqual(loaded?.name, playlist.name)
    }
}
```

**Windows**:
```csharp
public class PlaylistRepositoryIntegrationTests : IDisposable
{
    private readonly IPlaylistRepository _repository;
    private readonly string _testDbPath;
    
    public PlaylistRepositoryIntegrationTests()
    {
        _testDbPath = Path.GetTempFileName();
        _repository = new SqlitePlaylistRepository(_testDbPath);
    }
    
    [Fact]
    public async Task SaveAndLoadPlaylist_ReturnsCorrectData()
    {
        var playlist = new Playlist
        {
            Id = Guid.NewGuid().ToString(),
            Name = "Test Playlist",
            Type = PlaylistType.M3U_URL,
            Channels = new List<Channel>()
        };
        
        await _repository.SavePlaylist(playlist);
        var loaded = await _repository.GetPlaylist(playlist.Id);
        
        Assert.Equal(playlist.Name, loaded.Name);
    }
    
    public void Dispose()
    {
        File.Delete(_testDbPath);
    }
}
```

### UI Testing

**macOS (XCUITest)**:
```swift
class IPTVPlayerUITests: XCTestCase {
    var app: XCUIApplication!
    
    override func setUp() {
        super.setUp()
        app = XCUIApplication()
        app.launch()
    }
    
    func testAddPlaylistFlow() {
        app.buttons["Add Playlist"].tap()
        app.textFields["URL"].tap()
        app.textFields["URL"].typeText("http://example.com/playlist.m3u")
        app.buttons["Add"].tap()
        
        XCTAssertTrue(app.staticTexts["New Playlist"].exists)
    }
}
```

**Windows (WPF UI Automation)**:
```csharp
[TestClass]
public class IPTVPlayerUITests
{
    private Application _app;
    private Window _mainWindow;
    
    [TestInitialize]
    public void Setup()
    {
        _app = Application.Launch("IPTVPlayer.exe");
        _mainWindow = _app.GetWindow("IPTV Player");
    }
    
    [TestMethod]
    public void TestAddPlaylistFlow()
    {
        var addButton = _mainWindow.Get<Button>("AddPlaylistButton");
        addButton.Click();
        
        var urlTextBox = _mainWindow.Get<TextBox>("UrlTextBox");
        urlTextBox.Text = "http://example.com/playlist.m3u";
        
        var confirmButton = _mainWindow.Get<Button>("AddButton");
        confirmButton.Click();
        
        Assert.IsTrue(_mainWindow.Get<Label>("PlaylistLabel").Text.Contains("New Playlist"));
    }
    
    [TestCleanup]
    public void Cleanup()
    {
        _app.Close();
    }
}
```

### Test Coverage Goals

- **Unit Tests**: 目标覆盖率 80%+
- **Integration Tests**: 覆盖所有关键业务流程
- **UI Tests**: 覆盖主要用户场景
- **Property Tests**: 可选，用于增强核心算法测试

## Implementation Phases

### Phase 1: Project Setup and Infrastructure (Week 1-2)

**macOS**:
1. 创建 Xcode 项目
2. 配置 Swift Package Manager 依赖
3. 设置 Core Data 模型
4. 配置项目结构（MVVM 架构）
5. 设置 CI/CD 管道（GitHub Actions）

**Windows**:
1. 创建 Visual Studio 项目
2. 配置 NuGet 依赖
3. 设置 Entity Framework Core 或 SQLite
4. 配置项目结构（MVVM 架构）
5. 设置 CI/CD 管道（GitHub Actions）

**Shared**:
1. 决定代码共享策略（Kotlin/Native vs 独立实现）
2. 如果使用 Kotlin/Native，配置编译目标
3. 定义跨平台接口规范

### Phase 2: Core Business Logic (Week 3-4)

**macOS & Windows**:
1. 实现 M3U 解析器
2. 实现 Xtream API 客户端
3. 实现数据模型和转换逻辑
4. 编写单元测试
5. 实现错误处理机制

### Phase 3: Data Persistence (Week 5-6)

**macOS**:
1. 实现 Core Data 实体和关系
2. 实现 PlaylistRepository
3. 实现数据迁移逻辑
4. 编写集成测试

**Windows**:
1. 实现数据库模型（EF Core 或 SQLite）
2. 实现 PlaylistRepository
3. 实现数据迁移逻辑
4. 编写集成测试

### Phase 4: Video Player Integration (Week 7-8)

**macOS**:
1. 集成 AVPlayer
2. 实现播放控制逻辑
3. 实现 HLS/RTSP/HTTP 协议支持
4. 实现硬件加速配置
5. 编写播放器测试

**Windows**:
1. 选择并集成播放器（MediaElement 或 LibVLC）
2. 实现播放控制逻辑
3. 实现 HLS/RTSP/HTTP 协议支持
4. 实现硬件加速配置
5. 编写播放器测试

### Phase 5: UI Implementation (Week 9-11)

**macOS**:
1. 实现主窗口和导航
2. 实现播放列表侧边栏
3. 实现频道列表视图
4. 实现播放器视图
5. 实现设置和对话框
6. 编写 UI 测试

**Windows**:
1. 实现主窗口和导航
2. 实现播放列表侧边栏
3. 实现频道列表视图
4. 实现播放器窗口
5. 实现设置和对话框
6. 编写 UI 测试

### Phase 6: Polish and Testing (Week 12-13)

**Both Platforms**:
1. 性能优化
2. UI/UX 改进
3. 完善错误处理
4. 完整的端到端测试
5. 用户验收测试
6. 文档编写

### Phase 7: Deployment (Week 14)

**macOS**:
1. 配置代码签名
2. 创建 DMG 安装包
3. 配置自动更新
4. 发布到 GitHub Releases

**Windows**:
1. 配置代码签名
2. 创建 MSI 安装包
3. 配置自动更新
4. 发布到 GitHub Releases

## Migration Strategy

### Gradual Migration Approach

1. **保持 Android 应用不变**: Android 应用继续使用 Kotlin Multiplatform
2. **并行开发**: 在新的目录中开发原生桌面应用，不影响现有代码
3. **共享资源**: 复用现有的测试数据、文档和设计资源
4. **逐步切换**: 完成开发和测试后，更新发布流程

### Project Structure

```
IPTV/
├── android/                    # Android 应用（保持不变）
│   └── composeApp/
├── macos/                      # 新的 macOS 应用
│   ├── IPTVPlayer.xcodeproj
│   ├── IPTVPlayer/
│   │   ├── App/
│   │   ├── Views/
│   │   ├── ViewModels/
│   │   ├── Services/
│   │   └── Models/
│   └── IPTVPlayerTests/
├── windows/                    # 新的 Windows 应用
│   ├── IPTVPlayer.sln
│   ├── IPTVPlayer/
│   │   ├── App.xaml
│   │   ├── Views/
│   │   ├── ViewModels/
│   │   ├── Services/
│   │   └── Models/
│   └── IPTVPlayer.Tests/
├── shared/                     # 共享代码（可选）
│   └── kotlin-native/          # 如果使用 Kotlin/Native
└── docs/                       # 文档
```

### Backward Compatibility

- 数据库格式保持兼容，允许用户在不同版本间迁移
- 配置文件格式保持一致
- 播放列表格式保持标准化

## Performance Considerations

### macOS

1. **内存管理**: 使用 ARC，避免循环引用
2. **并发**: 使用 async/await 和 actor 模型
3. **UI 性能**: 使用 LazyVStack 和虚拟化列表
4. **视频播放**: 启用硬件加速，优化缓冲策略

### Windows

1. **内存管理**: 正确释放非托管资源
2. **并发**: 使用 async/await 和 Task
3. **UI 性能**: 使用虚拟化控件（VirtualizingStackPanel）
4. **视频播放**: 启用硬件加速，优化缓冲策略

### General Optimizations

1. **图片加载**: 实现图片缓存和懒加载
2. **数据库查询**: 使用索引和批量操作
3. **网络请求**: 实现请求缓存和去重
4. **启动时间**: 延迟加载非关键组件

## Security Considerations

1. **凭证存储**: 使用系统 Keychain（macOS）或 Credential Manager（Windows）存储敏感信息
2. **HTTPS**: 强制使用 HTTPS 进行网络请求
3. **输入验证**: 验证所有用户输入，防止注入攻击
4. **权限管理**: 请求最小必要权限
5. **代码签名**: 对发布的应用进行代码签名

## Deployment and Distribution

### macOS

**构建流程**:
```bash
xcodebuild -project IPTVPlayer.xcodeproj \
           -scheme IPTVPlayer \
           -configuration Release \
           -archivePath build/IPTVPlayer.xcarchive \
           archive

xcodebuild -exportArchive \
           -archivePath build/IPTVPlayer.xcarchive \
           -exportPath build/Release \
           -exportOptionsPlist ExportOptions.plist
```

**分发方式**:
- DMG 安装包
- GitHub Releases
- 可选: Mac App Store

### Windows

**构建流程**:
```bash
dotnet publish -c Release -r win-x64 --self-contained

# 使用 WiX 创建 MSI 安装包
candle IPTVPlayer.wxs
light -out IPTVPlayer.msi IPTVPlayer.wixobj
```

**分发方式**:
- MSI 安装包
- GitHub Releases
- 可选: Microsoft Store

### CI/CD Pipeline

**GitHub Actions 工作流**:
```yaml
name: Build and Release

on:
  push:
    tags:
      - 'v*'

jobs:
  build-macos:
    runs-on: macos-latest
    steps:
      - uses: actions/checkout@v3
      - name: Build macOS App
        run: |
          cd macos
          xcodebuild -project IPTVPlayer.xcodeproj \
                     -scheme IPTVPlayer \
                     -configuration Release \
                     archive
      - name: Create DMG
        run: |
          # DMG creation script
      - name: Upload Release Asset
        uses: actions/upload-release-asset@v1
        
  build-windows:
    runs-on: windows-latest
    steps:
      - uses: actions/checkout@v3
      - name: Setup .NET
        uses: actions/setup-dotnet@v3
      - name: Build Windows App
        run: |
          cd windows
          dotnet publish -c Release
      - name: Create MSI
        run: |
          # MSI creation script
      - name: Upload Release Asset
        uses: actions/upload-release-asset@v1
```
