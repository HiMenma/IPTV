//
//  ContentView.swift
//  IPTVPlayer
//
//  Created on 2025-11-28.
//

import SwiftUI
import CoreData

struct ContentView: View {
    @Environment(\.managedObjectContext) private var viewContext
    @StateObject private var viewModel: MainViewModel
    @State private var showingPlayerView = false
    @State private var columnVisibility: NavigationSplitViewVisibility = .all
    
    init() {
        // Initialize dependencies
        let context = PersistenceController.shared.container.viewContext
        let playlistRepository = CoreDataPlaylistRepository(context: context)
        let favoriteRepository = CoreDataFavoriteRepository(context: context)
        let m3uParser = M3UParserImpl()
        let xtreamClient = XtreamClientImpl()
        let keychainManager = KeychainManagerImpl()
        let inputValidator = InputValidatorImpl()
        
        // Initialize view model
        _viewModel = StateObject(wrappedValue: MainViewModel(
            playlistRepository: playlistRepository,
            favoriteRepository: favoriteRepository,
            m3uParser: m3uParser,
            xtreamClient: xtreamClient,
            keychainManager: keychainManager,
            inputValidator: inputValidator
        ))
    }

    var body: some View {
        NavigationSplitView(columnVisibility: $columnVisibility) {
            PlaylistSidebarView(viewModel: viewModel)
                .navigationSplitViewColumnWidth(min: 200, ideal: 250, max: 350)
        } detail: {
            if let selectedPlaylist = viewModel.selectedPlaylist {
                ChannelListView(viewModel: viewModel)
                    .transition(.opacity.combined(with: .move(edge: .trailing)))
            } else {
                emptyDetailView
                    .transition(.opacity)
            }
        }
        .animation(.easeInOut(duration: 0.3), value: viewModel.selectedPlaylist?.id)
        .frame(minWidth: 900, minHeight: 600)
        .toolbar {
            ToolbarItem(placement: .navigation) {
                Button(action: toggleSidebar) {
                    Label("Toggle Sidebar", systemImage: "sidebar.left")
                }
                .help("Toggle Sidebar (⌘⇧S)")
                .keyboardShortcut("s", modifiers: [.command, .shift])
            }
        }
        // Enhanced error presentation with banner
        .overlay(alignment: .top) {
            if let errorMessage = viewModel.errorMessage {
                ErrorBanner(
                    presentation: ErrorPresentation(
                        title: "Error",
                        message: errorMessage,
                        severity: .error,
                        category: .general
                    ),
                    onDismiss: {
                        withAnimation(.easeInOut(duration: 0.3)) {
                            viewModel.clearError()
                        }
                    }
                )
                .padding()
                .transition(.move(edge: .top).combined(with: .opacity))
                .animation(.spring(response: 0.4, dampingFraction: 0.8), value: viewModel.errorMessage)
                .zIndex(100)
            }
        }
        // Keyboard shortcuts
        .onAppear {
            setupKeyboardShortcuts()
        }
        // Watch for channel selection to show player
        .onChange(of: viewModel.selectedChannel) { newChannel in
            if newChannel != nil {
                showingPlayerView = true
            }
        }
        .sheet(isPresented: $showingPlayerView) {
            if let channel = viewModel.selectedChannel {
                PlayerView(viewModel: PlayerViewModel(
                    playerService: AVPlayerService(),
                    channel: channel
                ))
            }
        }
    }
    
    // MARK: - Empty Detail View
    
    private var emptyDetailView: some View {
        VStack(spacing: 24) {
            if #available(macOS 14.0, *) {
                Image(systemName: "tv")
                    .font(.system(size: 64))
                    .foregroundStyle(.tertiary)
                    .symbolEffect(.pulse, options: .repeating)
            } else {
                Image(systemName: "tv")
                    .font(.system(size: 64))
                    .foregroundColor(.secondary)
            }
            
            VStack(spacing: 8) {
                Text("No Playlist Selected")
                    .font(.title2)
                    .fontWeight(.semibold)
                
                Text("Select a playlist from the sidebar to view channels")
                    .font(.body)
                    .foregroundStyle(.secondary)
                    .multilineTextAlignment(.center)
            }
            
            // Quick action buttons
            VStack(spacing: 12) {
                Text("Quick Actions")
                    .font(.caption)
                    .foregroundStyle(.tertiary)
                    .textCase(.uppercase)
                
                HStack(spacing: 16) {
                    QuickActionButton(
                        icon: "link",
                        title: "Add M3U URL",
                        subtitle: "⌘N"
                    ) {
                        // Trigger add M3U URL action
                    }
                    
                    QuickActionButton(
                        icon: "doc",
                        title: "Import File",
                        subtitle: "⌘O"
                    ) {
                        // Trigger import file action
                    }
                    
                    QuickActionButton(
                        icon: "server.rack",
                        title: "Xtream Codes",
                        subtitle: "⌘⇧X"
                    ) {
                        // Trigger Xtream codes action
                    }
                }
            }
            .padding(.top, 16)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color(nsColor: .textBackgroundColor))
    }
    
    // MARK: - Helper Methods
    
    private func toggleSidebar() {
        withAnimation(.easeInOut(duration: 0.3)) {
            columnVisibility = columnVisibility == .all ? .detailOnly : .all
        }
    }
    
    private func setupKeyboardShortcuts() {
        // Keyboard shortcuts are handled via SwiftUI modifiers
        // Additional global shortcuts can be set up here if needed
    }
}

// MARK: - Quick Action Button

struct QuickActionButton: View {
    let icon: String
    let title: String
    let subtitle: String
    let action: () -> Void
    
    @State private var isHovered = false
    
    var body: some View {
        Button(action: action) {
            VStack(spacing: 8) {
                Image(systemName: icon)
                    .font(.system(size: 28))
                    .foregroundStyle(isHovered ? .primary : .secondary)
                
                VStack(spacing: 2) {
                    Text(title)
                        .font(.caption)
                        .fontWeight(.medium)
                    
                    Text(subtitle)
                        .font(.caption2)
                        .foregroundStyle(.tertiary)
                }
            }
            .frame(width: 100, height: 100)
            .background(
                RoundedRectangle(cornerRadius: 12)
                    .fill(isHovered ? Color.accentColor.opacity(0.1) : Color(nsColor: .controlBackgroundColor))
            )
            .overlay(
                RoundedRectangle(cornerRadius: 12)
                    .stroke(isHovered ? Color.accentColor.opacity(0.3) : Color.clear, lineWidth: 2)
            )
            .scaleEffect(isHovered ? 1.05 : 1.0)
            .animation(.spring(response: 0.3, dampingFraction: 0.7), value: isHovered)
        }
        .buttonStyle(.plain)
        .onHover { hovering in
            isHovered = hovering
        }
    }
}

#Preview {
    ContentView()
        .environment(\.managedObjectContext, PersistenceController.preview.container.viewContext)
}
