//
//  PlayerView.swift
//  IPTVPlayer
//
//  Video player view with controls and overlays
//

import SwiftUI
import AVKit

/// Main player view that displays video content with controls
struct PlayerView: View {
    @ObservedObject var viewModel: PlayerViewModel
    @Environment(\.dismiss) private var dismiss
    
    var body: some View {
        ZStack {
            // Video player layer
            if let player = viewModel.getAVPlayer() {
                VideoPlayerLayerView(player: player)
                    .ignoresSafeArea()
                    .onTapGesture {
                        viewModel.toggleControls()
                    }
            } else {
                Color.black
                    .ignoresSafeArea()
            }
            
            // Buffering indicator
            if viewModel.isBuffering {
                bufferingOverlay
            }
            
            // Error overlay
            if let errorMessage = viewModel.errorMessage {
                errorOverlay(message: errorMessage)
            }
            
            // Player controls
            if viewModel.showControls {
                playerControls
                    .transition(.opacity)
            }
        }
        .background(Color.black)
        .onAppear {
            viewModel.showControlsBriefly()
        }
    }
    
    // MARK: - Buffering Overlay
    
    private var bufferingOverlay: some View {
        VStack(spacing: 16) {
            ProgressView()
                .scaleEffect(1.5)
                .progressViewStyle(CircularProgressViewStyle(tint: .white))
            
            Text("Buffering...")
                .font(.headline)
                .foregroundColor(.white)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color.black.opacity(0.5))
    }
    
    // MARK: - Error Overlay
    
    private func errorOverlay(message: String) -> some View {
        VStack(spacing: 16) {
            Image(systemName: "exclamationmark.triangle")
                .font(.system(size: 48))
                .foregroundColor(.red)
            
            Text("Playback Error")
                .font(.title2)
                .fontWeight(.bold)
                .foregroundColor(.white)
            
            Text(message)
                .font(.body)
                .foregroundColor(.white)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 32)
            
            HStack(spacing: 16) {
                Button("Retry") {
                    if let channel = viewModel.currentChannel {
                        viewModel.play(channel: channel)
                    }
                }
                .buttonStyle(PlayerButtonStyle())
                
                Button("Close") {
                    viewModel.clearError()
                    dismiss()
                }
                .buttonStyle(PlayerButtonStyle())
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color.black.opacity(0.8))
    }
    
    // MARK: - Player Controls
    
    private var playerControls: some View {
        VStack {
            // Top bar with channel info and close button
            topBar
            
            Spacer()
            
            // Bottom controls
            bottomControls
        }
        .padding()
        .background(
            LinearGradient(
                gradient: Gradient(colors: [
                    Color.black.opacity(0.7),
                    Color.clear,
                    Color.black.opacity(0.7)
                ]),
                startPoint: .top,
                endPoint: .bottom
            )
        )
    }
    
    // MARK: - Top Bar
    
    private var topBar: some View {
        HStack {
            // Channel information
            if let channel = viewModel.currentChannel {
                VStack(alignment: .leading, spacing: 4) {
                    Text(channel.name)
                        .font(.title2)
                        .fontWeight(.semibold)
                        .foregroundColor(.white)
                    
                    if let group = channel.group {
                        Text(group)
                            .font(.subheadline)
                            .foregroundColor(.white.opacity(0.8))
                    }
                }
            }
            
            Spacer()
            
            // Close button
            Button(action: {
                viewModel.stop()
                dismiss()
            }) {
                Image(systemName: "xmark")
                    .font(.title2)
                    .foregroundColor(.white)
                    .frame(width: 44, height: 44)
                    .background(Color.white.opacity(0.2))
                    .clipShape(Circle())
            }
            .buttonStyle(.plain)
            .help("Close player")
        }
    }
    
    // MARK: - Bottom Controls
    
    private var bottomControls: some View {
        VStack(spacing: 12) {
            // Seek bar
            seekBar
            
            // Control buttons
            HStack(spacing: 24) {
                // Play/Pause button
                Button(action: {
                    viewModel.togglePlayPause()
                }) {
                    Image(systemName: viewModel.isPlaying ? "pause.fill" : "play.fill")
                        .font(.title)
                        .foregroundColor(.white)
                        .frame(width: 44, height: 44)
                }
                .buttonStyle(.plain)
                .help(viewModel.isPlaying ? "Pause" : "Play")
                
                // Seek backward button
                Button(action: {
                    viewModel.seekBackward()
                }) {
                    Image(systemName: "gobackward.10")
                        .font(.title2)
                        .foregroundColor(.white)
                        .frame(width: 44, height: 44)
                }
                .buttonStyle(.plain)
                .help("Seek backward 10 seconds")
                
                // Seek forward button
                Button(action: {
                    viewModel.seekForward()
                }) {
                    Image(systemName: "goforward.10")
                        .font(.title2)
                        .foregroundColor(.white)
                        .frame(width: 44, height: 44)
                }
                .buttonStyle(.plain)
                .help("Seek forward 10 seconds")
                
                Spacer()
                
                // Volume control
                volumeControl
                
                // Fullscreen button
                Button(action: {
                    viewModel.toggleFullscreen()
                }) {
                    Image(systemName: viewModel.isFullscreen ? "arrow.down.right.and.arrow.up.left" : "arrow.up.left.and.arrow.down.right")
                        .font(.title2)
                        .foregroundColor(.white)
                        .frame(width: 44, height: 44)
                }
                .buttonStyle(.plain)
                .help(viewModel.isFullscreen ? "Exit fullscreen" : "Enter fullscreen")
            }
        }
    }
    
    // MARK: - Seek Bar
    
    private var seekBar: some View {
        VStack(spacing: 4) {
            // Progress bar
            GeometryReader { geometry in
                ZStack(alignment: .leading) {
                    // Background track
                    Rectangle()
                        .fill(Color.white.opacity(0.3))
                        .frame(height: 4)
                    
                    // Progress track
                    Rectangle()
                        .fill(Color.accentColor)
                        .frame(width: geometry.size.width * viewModel.progress, height: 4)
                }
                .cornerRadius(2)
                .gesture(
                    DragGesture(minimumDistance: 0)
                        .onChanged { value in
                            let progress = value.location.x / geometry.size.width
                            let clampedProgress = max(0, min(1, progress))
                            let newTime = clampedProgress * viewModel.duration
                            viewModel.seek(to: newTime)
                        }
                )
            }
            .frame(height: 4)
            
            // Time labels
            HStack {
                Text(viewModel.currentTimeFormatted)
                    .font(.caption)
                    .foregroundColor(.white)
                    .monospacedDigit()
                
                Spacer()
                
                Text(viewModel.durationFormatted)
                    .font(.caption)
                    .foregroundColor(.white)
                    .monospacedDigit()
            }
        }
    }
    
    // MARK: - Volume Control
    
    private var volumeControl: some View {
        HStack(spacing: 8) {
            Button(action: {
                viewModel.toggleMute()
            }) {
                Image(systemName: volumeIcon)
                    .font(.title2)
                    .foregroundColor(.white)
                    .frame(width: 44, height: 44)
            }
            .buttonStyle(.plain)
            .help(viewModel.volume > 0 ? "Mute" : "Unmute")
            
            Slider(value: $viewModel.volume, in: 0...1)
                .frame(width: 100)
                .accentColor(.white)
        }
    }
    
    private var volumeIcon: String {
        if viewModel.volume == 0 {
            return "speaker.slash.fill"
        } else if viewModel.volume < 0.33 {
            return "speaker.wave.1.fill"
        } else if viewModel.volume < 0.66 {
            return "speaker.wave.2.fill"
        } else {
            return "speaker.wave.3.fill"
        }
    }
}

// MARK: - Video Player Layer View

/// UIViewRepresentable wrapper for AVPlayerLayer
struct VideoPlayerLayerView: NSViewRepresentable {
    let player: AVPlayer
    
    func makeNSView(context: Context) -> PlayerNSView {
        let view = PlayerNSView()
        view.player = player
        return view
    }
    
    func updateNSView(_ nsView: PlayerNSView, context: Context) {
        nsView.player = player
    }
    
    /// Custom NSView that hosts AVPlayerLayer
    class PlayerNSView: NSView {
        var player: AVPlayer? {
            didSet {
                playerLayer.player = player
            }
        }
        
        private var playerLayer: AVPlayerLayer {
            return layer as! AVPlayerLayer
        }
        
        override init(frame: NSRect) {
            super.init(frame: frame)
            wantsLayer = true
        }
        
        required init?(coder: NSCoder) {
            super.init(coder: coder)
            wantsLayer = true
        }
        
        override func makeBackingLayer() -> CALayer {
            let layer = AVPlayerLayer()
            layer.videoGravity = .resizeAspect
            return layer
        }
        
        override func layout() {
            super.layout()
            playerLayer.frame = bounds
        }
    }
}

// MARK: - Player Button Style

struct PlayerButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .padding(.horizontal, 24)
            .padding(.vertical, 12)
            .background(Color.accentColor)
            .foregroundColor(.white)
            .cornerRadius(8)
            .opacity(configuration.isPressed ? 0.8 : 1.0)
    }
}

// MARK: - Preview

#Preview {
    let playerService = AVPlayerService()
    let viewModel = PlayerViewModel(playerService: playerService)
    
    // Set up sample channel
    viewModel.currentChannel = Channel(
        name: "Sample Channel",
        url: "http://example.com/stream",
        group: "Entertainment"
    )
    
    return PlayerView(viewModel: viewModel)
}
