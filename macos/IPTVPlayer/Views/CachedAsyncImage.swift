//
//  CachedAsyncImage.swift
//  IPTVPlayer
//
//  Cached async image view for optimized image loading
//

import SwiftUI
import AppKit

/// A view that asynchronously loads and caches images
struct CachedAsyncImage<Content: View, Placeholder: View>: View {
    let url: URL?
    let content: (NSImage) -> Content
    let placeholder: () -> Placeholder
    
    @State private var image: NSImage?
    @State private var isLoading = false
    @State private var error: Error?
    
    init(
        url: URL?,
        @ViewBuilder content: @escaping (NSImage) -> Content,
        @ViewBuilder placeholder: @escaping () -> Placeholder
    ) {
        self.url = url
        self.content = content
        self.placeholder = placeholder
    }
    
    var body: some View {
        Group {
            if let image = image {
                content(image)
            } else if isLoading {
                placeholder()
            } else if error != nil {
                placeholder()
            } else {
                placeholder()
            }
        }
        .task(id: url) {
            await loadImage()
        }
    }
    
    private func loadImage() async {
        guard let url = url else { return }
        
        isLoading = true
        error = nil
        
        do {
            let loadedImage = try await ImageCache.shared.loadImage(from: url.absoluteString)
            await MainActor.run {
                self.image = loadedImage
                self.isLoading = false
            }
        } catch {
            await MainActor.run {
                self.error = error
                self.isLoading = false
            }
        }
    }
}

// MARK: - Convenience Initializers

extension CachedAsyncImage where Content == Image, Placeholder == ProgressView<EmptyView, EmptyView> {
    init(url: URL?) {
        self.init(
            url: url,
            content: { nsImage in
                Image(nsImage: nsImage)
                    .resizable()
            },
            placeholder: {
                ProgressView()
            }
        )
    }
}

// MARK: - Image Extension for NSImage

extension Image {
    init(nsImage: NSImage) {
        self.init(nsImage: nsImage)
    }
}
