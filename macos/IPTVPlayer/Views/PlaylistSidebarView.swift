//
//  PlaylistSidebarView.swift
//  IPTVPlayer
//
//  Sidebar view for displaying and managing playlists
//

import SwiftUI

struct PlaylistSidebarView: View {
    @ObservedObject var viewModel: MainViewModel
    
    // State for showing dialogs
    @State private var showingAddM3UURLDialog = false
    @State private var showingAddM3UFileDialog = false
    @State private var showingAddXtreamDialog = false
    @State private var showingRenameDialog = false
    @State private var playlistToRename: Playlist?
    @State private var newPlaylistName = ""
    
    // State for M3U URL input
    @State private var m3uURL = ""
    
    // State for Xtream credentials
    @State private var xtreamServerURL = ""
    @State private var xtreamUsername = ""
    @State private var xtreamPassword = ""
    
    // Drag and drop state
    @State private var draggedPlaylist: Playlist?
    @State private var dropTargetIndex: Int?
    
    var body: some View {
        VStack(spacing: 0) {
            // Header with Add button
            HStack {
                Text("Playlists")
                    .font(.headline)
                
                Spacer()
                
                Menu {
                    Button("M3U URL") {
                        showingAddM3UURLDialog = true
                    }
                    
                    Button("M3U File") {
                        showingAddM3UFileDialog = true
                    }
                    
                    Button("Xtream Codes") {
                        showingAddXtreamDialog = true
                    }
                } label: {
                    Image(systemName: "plus")
                }
                .menuStyle(.borderlessButton)
                .frame(width: 20, height: 20)
            }
            .padding()
            
            Divider()
            
            // Playlists list
            if viewModel.playlists.isEmpty {
                // Empty state
                VStack(spacing: 16) {
                    Image(systemName: "list.bullet.rectangle")
                        .font(.system(size: 48))
                        .foregroundColor(.secondary)
                    
                    Text("No Playlists")
                        .font(.headline)
                        .foregroundColor(.secondary)
                    
                    Text("Add a playlist to get started")
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                        .multilineTextAlignment(.center)
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .padding()
            } else {
                List(selection: $viewModel.selectedPlaylist) {
                    // Favorites section
                    Section("Favorites") {
                        Button(action: {
                            Task {
                                await viewModel.loadFavorites()
                            }
                        }) {
                            Label("Favorites", systemImage: "star.fill")
                                .foregroundColor(.yellow)
                        }
                        .buttonStyle(.plain)
                    }
                    
                    // Playlists section with drag and drop
                    Section("My Playlists") {
                        ForEach(viewModel.playlists) { playlist in
                            PlaylistRow(playlist: playlist)
                                .tag(playlist)
                                .contextMenu {
                                    Button("Rename") {
                                        playlistToRename = playlist
                                        newPlaylistName = playlist.name
                                        showingRenameDialog = true
                                    }
                                    
                                    Button("Duplicate") {
                                        Task {
                                            await viewModel.duplicatePlaylist(playlist)
                                        }
                                    }
                                    
                                    Divider()
                                    
                                    Button("Delete", role: .destructive) {
                                        Task {
                                            await viewModel.deletePlaylist(playlist)
                                        }
                                    }
                                }
                                .onTapGesture {
                                    withAnimation(.easeInOut(duration: 0.2)) {
                                        Task {
                                            await viewModel.selectPlaylist(playlist)
                                        }
                                    }
                                }
                                // Drag support
                                .onDrag {
                                    self.draggedPlaylist = playlist
                                    return NSItemProvider(object: playlist.id as NSString)
                                }
                                // Drop support
                                .onDrop(of: [.text], delegate: PlaylistDropDelegate(
                                    playlist: playlist,
                                    playlists: $viewModel.playlists,
                                    draggedPlaylist: $draggedPlaylist,
                                    dropTargetIndex: $dropTargetIndex
                                ))
                        }
                    }
                }
                .listStyle(.sidebar)
            }
        }
        .navigationTitle("IPTV Player")
        // M3U URL Dialog
        .sheet(isPresented: $showingAddM3UURLDialog) {
            AddM3UURLDialog(
                url: $m3uURL,
                isPresented: $showingAddM3UURLDialog,
                onAdd: {
                    Task {
                        await viewModel.addM3UPlaylist(url: m3uURL)
                        m3uURL = ""
                    }
                }
            )
        }
        // M3U File Dialog
        .fileImporter(
            isPresented: $showingAddM3UFileDialog,
            allowedContentTypes: [.plainText, .data],
            allowsMultipleSelection: false
        ) { result in
            switch result {
            case .success(let urls):
                if let url = urls.first {
                    Task {
                        await viewModel.addM3UPlaylistFromFile(fileURL: url)
                    }
                }
            case .failure(let error):
                viewModel.errorMessage = "Failed to select file: \(error.localizedDescription)"
            }
        }
        // Xtream Codes Dialog
        .sheet(isPresented: $showingAddXtreamDialog) {
            AddXtreamDialog(
                serverURL: $xtreamServerURL,
                username: $xtreamUsername,
                password: $xtreamPassword,
                isPresented: $showingAddXtreamDialog,
                onAdd: {
                    Task {
                        await viewModel.addXtreamAccount(
                            serverUrl: xtreamServerURL,
                            username: xtreamUsername,
                            password: xtreamPassword
                        )
                        xtreamServerURL = ""
                        xtreamUsername = ""
                        xtreamPassword = ""
                    }
                }
            )
        }
        // Rename Dialog
        .sheet(isPresented: $showingRenameDialog) {
            if let playlist = playlistToRename {
                RenamePlaylistDialog(
                    playlistName: $newPlaylistName,
                    isPresented: $showingRenameDialog,
                    onRename: {
                        Task {
                            await viewModel.renamePlaylist(playlist, newName: newPlaylistName)
                            playlistToRename = nil
                        }
                    }
                )
            }
        }
        .task {
            await viewModel.loadPlaylists()
        }
        // Keyboard shortcuts
        .onAppear {
            NSEvent.addLocalMonitorForEvents(matching: .keyDown) { event in
                return handleKeyPress(event: event)
            }
        }
    }
    
    // MARK: - Keyboard Shortcuts
    
    private func handleKeyPress(event: NSEvent) -> NSEvent? {
        // Command+N: Add M3U URL
        if event.modifierFlags.contains(.command) && event.charactersIgnoringModifiers == "n" {
            showingAddM3UURLDialog = true
            return nil
        }
        
        // Command+O: Import M3U File
        if event.modifierFlags.contains(.command) && event.charactersIgnoringModifiers == "o" {
            showingAddM3UFileDialog = true
            return nil
        }
        
        // Command+Shift+X: Add Xtream Codes
        if event.modifierFlags.contains([.command, .shift]) && event.charactersIgnoringModifiers == "x" {
            showingAddXtreamDialog = true
            return nil
        }
        
        // Delete key: Delete selected playlist
        if event.keyCode == 51 { // Delete key
            if let selected = viewModel.selectedPlaylist {
                Task {
                    await viewModel.deletePlaylist(selected)
                }
                return nil
            }
        }
        
        return event
    }
}

// MARK: - Playlist Row

struct PlaylistRow: View {
    let playlist: Playlist
    
    var body: some View {
        HStack {
            Image(systemName: playlistIcon)
                .foregroundColor(.accentColor)
            
            VStack(alignment: .leading, spacing: 2) {
                Text(playlist.name)
                    .font(.body)
                
                Text("\(playlist.channels.count) channels")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
        }
        .padding(.vertical, 4)
    }
    
    private var playlistIcon: String {
        switch playlist.type {
        case .m3uUrl:
            return "link"
        case .m3uFile:
            return "doc"
        case .xtream:
            return "server.rack"
        }
    }
}

// MARK: - Add M3U URL Dialog

struct AddM3UURLDialog: View {
    @Binding var url: String
    @Binding var isPresented: Bool
    let onAdd: () -> Void
    
    @FocusState private var isTextFieldFocused: Bool
    
    var body: some View {
        VStack(spacing: 24) {
            // Header
            VStack(spacing: 8) {
                Image(systemName: "link.circle.fill")
                    .font(.system(size: 48))
                    .foregroundStyle(.blue)
                    .symbolEffect(.pulse)
                
                Text("Add M3U Playlist")
                    .font(.title2)
                    .fontWeight(.semibold)
                
                Text("Enter the URL of your M3U playlist")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
            
            // URL Input
            VStack(alignment: .leading, spacing: 8) {
                Text("Playlist URL")
                    .font(.caption)
                    .foregroundStyle(.secondary)
                    .textCase(.uppercase)
                
                TextField("https://example.com/playlist.m3u", text: $url)
                    .textFieldStyle(.roundedBorder)
                    .focused($isTextFieldFocused)
                
                if !url.isEmpty && !isValidURL(url) {
                    Label("Please enter a valid HTTP or HTTPS URL", systemImage: "exclamationmark.triangle")
                        .font(.caption)
                        .foregroundStyle(.red)
                }
            }
            
            // Buttons
            HStack(spacing: 12) {
                Button("Cancel") {
                    isPresented = false
                }
                .keyboardShortcut(.cancelAction)
                .controlSize(.large)
                
                Spacer()
                
                Button("Add Playlist") {
                    onAdd()
                    isPresented = false
                }
                .keyboardShortcut(.defaultAction)
                .buttonStyle(.borderedProminent)
                .controlSize(.large)
                .disabled(url.isEmpty || !isValidURL(url))
            }
        }
        .padding(24)
        .frame(width: 480)
        .onAppear {
            isTextFieldFocused = true
        }
    }
    
    private func isValidURL(_ string: String) -> Bool {
        guard let url = URL(string: string) else { return false }
        return url.scheme == "http" || url.scheme == "https"
    }
}

// MARK: - Add Xtream Dialog

struct AddXtreamDialog: View {
    @Binding var serverURL: String
    @Binding var username: String
    @Binding var password: String
    @Binding var isPresented: Bool
    let onAdd: () -> Void
    
    @FocusState private var focusedField: Field?
    
    enum Field {
        case serverURL, username, password
    }
    
    var body: some View {
        VStack(spacing: 24) {
            // Header
            VStack(spacing: 8) {
                Image(systemName: "server.rack")
                    .font(.system(size: 48))
                    .foregroundStyle(.purple)
                    .symbolEffect(.pulse)
                
                Text("Add Xtream Codes Account")
                    .font(.title2)
                    .fontWeight(.semibold)
                
                Text("Connect to your Xtream Codes IPTV service")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
            
            // Form
            VStack(alignment: .leading, spacing: 16) {
                // Server URL
                VStack(alignment: .leading, spacing: 6) {
                    Text("Server URL")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                        .textCase(.uppercase)
                    
                    TextField("http://example.com:8080", text: $serverURL)
                        .textFieldStyle(.roundedBorder)
                        .focused($focusedField, equals: .serverURL)
                        .onSubmit {
                            focusedField = .username
                        }
                }
                
                // Username
                VStack(alignment: .leading, spacing: 6) {
                    Text("Username")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                        .textCase(.uppercase)
                    
                    TextField("Username", text: $username)
                        .textFieldStyle(.roundedBorder)
                        .focused($focusedField, equals: .username)
                        .onSubmit {
                            focusedField = .password
                        }
                }
                
                // Password
                VStack(alignment: .leading, spacing: 6) {
                    Text("Password")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                        .textCase(.uppercase)
                    
                    SecureField("Password", text: $password)
                        .textFieldStyle(.roundedBorder)
                        .focused($focusedField, equals: .password)
                        .onSubmit {
                            if isValidInput {
                                onAdd()
                                isPresented = false
                            }
                        }
                }
                
                if !serverURL.isEmpty && !isValidServerURL {
                    Label("Please enter a valid HTTP or HTTPS URL", systemImage: "exclamationmark.triangle")
                        .font(.caption)
                        .foregroundStyle(.red)
                }
            }
            
            // Buttons
            HStack(spacing: 12) {
                Button("Cancel") {
                    isPresented = false
                }
                .keyboardShortcut(.cancelAction)
                .controlSize(.large)
                
                Spacer()
                
                Button("Connect") {
                    onAdd()
                    isPresented = false
                }
                .keyboardShortcut(.defaultAction)
                .buttonStyle(.borderedProminent)
                .controlSize(.large)
                .disabled(!isValidInput)
            }
        }
        .padding(24)
        .frame(width: 480)
        .onAppear {
            focusedField = .serverURL
        }
    }
    
    private var isValidServerURL: Bool {
        guard let url = URL(string: serverURL) else { return false }
        return url.scheme == "http" || url.scheme == "https"
    }
    
    private var isValidInput: Bool {
        !serverURL.isEmpty && !username.isEmpty && !password.isEmpty && isValidServerURL
    }
}

// MARK: - Rename Playlist Dialog

struct RenamePlaylistDialog: View {
    @Binding var playlistName: String
    @Binding var isPresented: Bool
    let onRename: () -> Void
    
    @FocusState private var isTextFieldFocused: Bool
    
    var body: some View {
        VStack(spacing: 24) {
            // Header
            VStack(spacing: 8) {
                Image(systemName: "pencil.circle.fill")
                    .font(.system(size: 48))
                    .foregroundStyle(.orange)
                    .symbolEffect(.pulse)
                
                Text("Rename Playlist")
                    .font(.title2)
                    .fontWeight(.semibold)
                
                Text("Enter a new name for your playlist")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
            
            // Name Input
            VStack(alignment: .leading, spacing: 8) {
                Text("Playlist Name")
                    .font(.caption)
                    .foregroundStyle(.secondary)
                    .textCase(.uppercase)
                
                TextField("My Playlist", text: $playlistName)
                    .textFieldStyle(.roundedBorder)
                    .focused($isTextFieldFocused)
                    .onSubmit {
                        if !playlistName.isEmpty {
                            onRename()
                            isPresented = false
                        }
                    }
                
                if playlistName.isEmpty {
                    Label("Playlist name cannot be empty", systemImage: "exclamationmark.triangle")
                        .font(.caption)
                        .foregroundStyle(.red)
                }
            }
            
            // Buttons
            HStack(spacing: 12) {
                Button("Cancel") {
                    isPresented = false
                }
                .keyboardShortcut(.cancelAction)
                .controlSize(.large)
                
                Spacer()
                
                Button("Rename") {
                    onRename()
                    isPresented = false
                }
                .keyboardShortcut(.defaultAction)
                .buttonStyle(.borderedProminent)
                .controlSize(.large)
                .disabled(playlistName.isEmpty)
            }
        }
        .padding(24)
        .frame(width: 420)
        .onAppear {
            isTextFieldFocused = true
        }
    }
}

// MARK: - Preview

#Preview {
    NavigationSplitView {
        PlaylistSidebarView(viewModel: MainViewModel(
            playlistRepository: MockPlaylistRepository(),
            favoriteRepository: MockFavoriteRepository(),
            m3uParser: MockM3UParser(),
            xtreamClient: MockXtreamClient(),
            keychainManager: KeychainManagerImpl(),
            inputValidator: InputValidatorImpl()
        ))
    } detail: {
        Text("Select a playlist")
    }
}

// MARK: - Mock Implementations for Preview

class MockPlaylistRepository: PlaylistRepository {
    func getAllPlaylists() async throws -> [Playlist] {
        return [
            Playlist(name: "Sample Playlist 1", type: .m3uUrl, channels: [
                Channel(name: "Channel 1", url: "http://example.com/1"),
                Channel(name: "Channel 2", url: "http://example.com/2")
            ]),
            Playlist(name: "Sample Playlist 2", type: .xtream, channels: [
                Channel(name: "Channel 3", url: "http://example.com/3")
            ])
        ]
    }
    
    func getPlaylist(id: String) async throws -> Playlist? {
        return nil
    }
    
    func savePlaylist(_ playlist: Playlist) async throws {}
    
    func deletePlaylist(id: String) async throws {}
    
    func updatePlaylist(_ playlist: Playlist) async throws {}
}

class MockFavoriteRepository: FavoriteRepository {
    func addFavorite(channelId: String, playlistId: String) async throws {}
    
    func removeFavorite(channelId: String, playlistId: String) async throws {}
    
    func getAllFavorites() async throws -> [Favorite] {
        return []
    }
    
    func isFavorite(channelId: String, playlistId: String) async throws -> Bool {
        return false
    }
}

class MockM3UParser: M3UParser {
    func parse(content: String) async throws -> [Channel] {
        return []
    }
}

class MockXtreamClient: XtreamClient {
    func authenticate(account: XtreamAccount) async throws -> Bool {
        return true
    }
    
    func getLiveCategories(account: XtreamAccount) async throws -> [Category] {
        return []
    }
    
    func getLiveStreams(account: XtreamAccount, categoryId: String?) async throws -> [Channel] {
        return []
    }
    
    func getVODCategories(account: XtreamAccount) async throws -> [Category] {
        return []
    }
    
    func getVODStreams(account: XtreamAccount, categoryId: String?) async throws -> [Channel] {
        return []
    }
    
    func getEPGData(account: XtreamAccount, streamId: String) async throws -> [String: Any] {
        return [:]
    }
}

// MARK: - Playlist Drop Delegate

struct PlaylistDropDelegate: DropDelegate {
    let playlist: Playlist
    @Binding var playlists: [Playlist]
    @Binding var draggedPlaylist: Playlist?
    @Binding var dropTargetIndex: Int?
    
    func dropEntered(info: DropInfo) {
        guard let draggedPlaylist = draggedPlaylist,
              draggedPlaylist.id != playlist.id,
              let fromIndex = playlists.firstIndex(where: { $0.id == draggedPlaylist.id }),
              let toIndex = playlists.firstIndex(where: { $0.id == playlist.id }) else {
            return
        }
        
        dropTargetIndex = toIndex
        
        withAnimation(.spring(response: 0.3, dampingFraction: 0.7)) {
            playlists.move(fromOffsets: IndexSet(integer: fromIndex), toOffset: toIndex > fromIndex ? toIndex + 1 : toIndex)
        }
    }
    
    func dropExited(info: DropInfo) {
        dropTargetIndex = nil
    }
    
    func performDrop(info: DropInfo) -> Bool {
        draggedPlaylist = nil
        dropTargetIndex = nil
        return true
    }
    
    func dropUpdated(info: DropInfo) -> DropProposal? {
        return DropProposal(operation: .move)
    }
}
