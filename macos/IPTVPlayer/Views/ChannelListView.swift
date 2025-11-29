//
//  ChannelListView.swift
//  IPTVPlayer
//
//  View for displaying and managing channel list
//

import SwiftUI

/// View that displays a list of channels with search and filter capabilities
struct ChannelListView: View {
    @ObservedObject var viewModel: MainViewModel
    
    var body: some View {
        VStack(spacing: 0) {
            // Search bar
            searchBar
            
            // Category filter
            if !categories.isEmpty {
                categoryFilter
            }
            
            // Channel list or empty state
            if viewModel.isLoading {
                loadingState
            } else if viewModel.filteredChannels.isEmpty {
                emptyState
            } else {
                channelList
            }
        }
        .navigationTitle(viewModel.selectedPlaylist?.name ?? "Channels")
    }
    
    // MARK: - Search Bar
    
    private var searchBar: some View {
        HStack(spacing: 8) {
            Image(systemName: "magnifyingglass")
                .foregroundStyle(.secondary)
                .font(.body)
            
            TextField("Search channels...", text: $viewModel.searchQuery)
                .textFieldStyle(.plain)
                .font(.body)
            
            if !viewModel.searchQuery.isEmpty {
                Button(action: {
                    withAnimation(.easeInOut(duration: 0.2)) {
                        viewModel.searchQuery = ""
                    }
                }) {
                    Image(systemName: "xmark.circle.fill")
                        .foregroundStyle(.secondary)
                        .font(.body)
                }
                .buttonStyle(.plain)
                .transition(.scale.combined(with: .opacity))
            }
        }
        .padding(10)
        .background(
            RoundedRectangle(cornerRadius: 10)
                .fill(Color(nsColor: .controlBackgroundColor))
                .shadow(color: .black.opacity(0.05), radius: 2, x: 0, y: 1)
        )
        .padding(.horizontal)
        .padding(.top, 12)
        .padding(.bottom, 8)
    }
    
    // MARK: - Category Filter
    
    private var categories: [String] {
        let allCategories = viewModel.channels.compactMap { $0.group }
        return Array(Set(allCategories)).sorted()
    }
    
    private var categoryFilter: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                // "All" button
                CategoryFilterButton(
                    title: "All",
                    isSelected: viewModel.selectedCategory == nil,
                    action: {
                        viewModel.selectedCategory = nil
                    }
                )
                
                // Category buttons
                ForEach(categories, id: \.self) { category in
                    CategoryFilterButton(
                        title: category,
                        isSelected: viewModel.selectedCategory == category,
                        action: {
                            viewModel.selectedCategory = category
                        }
                    )
                }
            }
            .padding(.horizontal)
        }
        .padding(.bottom, 8)
    }
    
    // MARK: - Channel List
    
    private var channelList: some View {
        ScrollView {
            LazyVStack(spacing: 0) {
                ForEach(viewModel.filteredChannels) { channel in
                    ChannelRow(
                        channel: channel,
                        viewModel: viewModel
                    )
                    .contentShape(Rectangle())
                    .onTapGesture {
                        withAnimation(.easeInOut(duration: 0.2)) {
                            viewModel.selectChannel(channel)
                        }
                    }
                    .transition(.opacity.combined(with: .move(edge: .leading)))
                    
                    Divider()
                        .padding(.leading, 84)
                }
            }
            .animation(.easeInOut(duration: 0.3), value: viewModel.filteredChannels.count)
        }
    }
    
    // MARK: - Loading State
    
    private var loadingState: some View {
        VStack(spacing: 20) {
            ProgressView()
                .scaleEffect(1.5)
                .progressViewStyle(.circular)
            
            Text("Loading channels...")
                .font(.headline)
                .foregroundStyle(.secondary)
            
            Text("Please wait while we fetch your content")
                .font(.caption)
                .foregroundStyle(.tertiary)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .transition(.opacity)
    }
    
    // MARK: - Empty State
    
    private var emptyState: some View {
        VStack(spacing: 24) {
            if #available(macOS 14.0, *) {
                Image(systemName: viewModel.searchQuery.isEmpty ? "tv.slash" : "magnifyingglass")
                    .font(.system(size: 56))
                    .foregroundStyle(.tertiary)
                    .symbolEffect(.pulse, options: .repeating)
            } else {
                Image(systemName: viewModel.searchQuery.isEmpty ? "tv.slash" : "magnifyingglass")
                    .font(.system(size: 56))
                    .foregroundColor(.secondary)
            }
            
            VStack(spacing: 8) {
                Text(emptyStateMessage)
                    .font(.title3)
                    .fontWeight(.semibold)
                
                Text(emptyStateSubtitle)
                    .font(.body)
                    .foregroundStyle(.secondary)
                    .multilineTextAlignment(.center)
            }
            
            if !viewModel.searchQuery.isEmpty {
                Button(action: {
                    withAnimation(.easeInOut(duration: 0.2)) {
                        viewModel.searchQuery = ""
                    }
                }) {
                    Label("Clear Search", systemImage: "xmark.circle")
                }
                .buttonStyle(.borderedProminent)
                .controlSize(.large)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .padding()
        .transition(.opacity)
    }
    
    private var emptyStateMessage: String {
        if !viewModel.searchQuery.isEmpty {
            return "No Results Found"
        } else if viewModel.selectedCategory != nil {
            return "No Channels in Category"
        } else {
            return "No Channels Available"
        }
    }
    
    private var emptyStateSubtitle: String {
        if !viewModel.searchQuery.isEmpty {
            return "Try adjusting your search terms or clear the search to see all channels"
        } else if viewModel.selectedCategory != nil {
            return "This category doesn't contain any channels yet"
        } else {
            return "This playlist doesn't have any channels"
        }
    }
}

// MARK: - Category Filter Button

private struct CategoryFilterButton: View {
    let title: String
    let isSelected: Bool
    let action: () -> Void
    
    var body: some View {
        Button(action: action) {
            Text(title)
                .font(.subheadline)
                .padding(.horizontal, 12)
                .padding(.vertical, 6)
                .background(isSelected ? Color.accentColor : Color(nsColor: .controlBackgroundColor))
                .foregroundColor(isSelected ? .white : .primary)
                .cornerRadius(16)
        }
        .buttonStyle(.plain)
    }
}

// MARK: - Channel Row

private struct ChannelRow: View {
    let channel: Channel
    @ObservedObject var viewModel: MainViewModel
    @State private var isFavorite: Bool = false
    @State private var isHovered: Bool = false
    
    var body: some View {
        HStack(spacing: 12) {
            // Channel thumbnail
            channelThumbnail
            
            // Channel info
            VStack(alignment: .leading, spacing: 4) {
                Text(channel.name)
                    .font(.headline)
                    .lineLimit(1)
                    .foregroundStyle(isHovered ? .primary : .primary)
                
                if let category = channel.group {
                    HStack(spacing: 4) {
                        Image(systemName: "folder")
                            .font(.caption2)
                        Text(category)
                            .font(.caption)
                    }
                    .foregroundStyle(.secondary)
                    .lineLimit(1)
                }
            }
            
            Spacer()
            
            // Favorite button with animation
            favoriteButton
                .opacity(isHovered || isFavorite ? 1.0 : 0.0)
                .animation(.easeInOut(duration: 0.2), value: isHovered)
                .animation(.easeInOut(duration: 0.2), value: isFavorite)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 10)
        .background(
            RoundedRectangle(cornerRadius: 8)
                .fill(isHovered ? Color.accentColor.opacity(0.08) : Color.clear)
        )
        .padding(.horizontal, 8)
        .padding(.vertical, 2)
        .contentShape(Rectangle())
        .onHover { hovering in
            withAnimation(.easeInOut(duration: 0.15)) {
                isHovered = hovering
            }
        }
        .task {
            isFavorite = await viewModel.isFavorite(channel)
        }
    }
    
    private var channelThumbnail: some View {
        Group {
            if let logoUrl = channel.logoUrl, let url = URL(string: logoUrl) {
                CachedAsyncImage(url: url) { nsImage in
                    Image(nsImage: nsImage)
                        .resizable()
                        .aspectRatio(contentMode: .fit)
                        .frame(width: 60, height: 60)
                        .cornerRadius(8)
                } placeholder: {
                    ProgressView()
                        .frame(width: 60, height: 60)
                }
            } else {
                placeholderImage
            }
        }
    }
    
    private var placeholderImage: some View {
        ZStack {
            RoundedRectangle(cornerRadius: 8)
                .fill(Color(nsColor: .controlBackgroundColor))
                .frame(width: 60, height: 60)
            
            Image(systemName: "tv")
                .font(.title2)
                .foregroundColor(.secondary)
        }
    }
    
    private var favoriteButton: some View {
        Button {
            Task { @MainActor in
                await viewModel.toggleFavorite(channel)
                let newFavoriteState = await viewModel.isFavorite(channel)
                withAnimation(.spring(response: 0.3, dampingFraction: 0.6)) {
                    isFavorite = newFavoriteState
                }
            }
        } label: {
            Image(systemName: isFavorite ? "star.fill" : "star")
                .foregroundStyle(isFavorite ? .yellow : .secondary)
                .font(.title3)
        }
        .buttonStyle(.plain)
        .help(isFavorite ? "Remove from favorites (⌘⇧F)" : "Add to favorites (⌘⇧F)")
    }
}

// MARK: - Preview

#Preview {
    let context = PersistenceController.preview.container.viewContext
    let playlistRepository = CoreDataPlaylistRepository(context: context)
    let favoriteRepository = CoreDataFavoriteRepository(context: context)
    let m3uParser = M3UParserImpl()
    let xtreamClient = XtreamClientImpl()
    let keychainManager = KeychainManagerImpl()
    let inputValidator = InputValidatorImpl()
    
    return ChannelListView(viewModel: MainViewModel(
        playlistRepository: playlistRepository,
        favoriteRepository: favoriteRepository,
        m3uParser: m3uParser,
        xtreamClient: xtreamClient,
        keychainManager: keychainManager,
        inputValidator: inputValidator
    ))
}
