//
//  ErrorView.swift
//  IPTVPlayer
//
//  SwiftUI view for displaying errors to users
//

import SwiftUI

/// View for displaying error messages with appropriate styling
struct ErrorView: View {
    let presentation: ErrorPresentation
    let onDismiss: (() -> Void)?
    let onRetry: (() -> Void)?
    
    init(
        presentation: ErrorPresentation,
        onDismiss: (() -> Void)? = nil,
        onRetry: (() -> Void)? = nil
    ) {
        self.presentation = presentation
        self.onDismiss = onDismiss
        self.onRetry = onRetry
    }
    
    var body: some View {
        VStack(spacing: 16) {
            // Error icon
            Image(systemName: presentation.severity.iconName)
                .font(.system(size: 48))
                .foregroundColor(presentation.severity.color)
            
            // Title
            Text(presentation.title)
                .font(.headline)
                .multilineTextAlignment(.center)
            
            // Message
            Text(presentation.message)
                .font(.body)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
            
            // Recovery suggestion
            if let suggestion = presentation.recoverySuggestion {
                Text(suggestion)
                    .font(.caption)
                    .foregroundColor(.secondary)
                    .multilineTextAlignment(.center)
                    .padding(.top, 4)
            }
            
            // Action buttons
            HStack(spacing: 12) {
                if let onRetry = onRetry {
                    Button("重试") {
                        onRetry()
                    }
                    .buttonStyle(.borderedProminent)
                }
                
                if let onDismiss = onDismiss {
                    Button("关闭") {
                        onDismiss()
                    }
                    .buttonStyle(.bordered)
                }
            }
            .padding(.top, 8)
        }
        .padding(24)
        .frame(maxWidth: 400)
    }
}

/// Alert modifier for displaying errors
struct ErrorAlert: ViewModifier {
    @Binding var error: Error?
    let onRetry: (() -> Void)?
    
    func body(content: Content) -> some View {
        content
            .alert(isPresented: .constant(error != nil)) {
                if let error = error {
                    let presentation = AppErrorPresenter.shared.present(error: error)
                    
                    if let onRetry = onRetry {
                        return Alert(
                            title: Text(presentation.title),
                            message: Text(errorMessage(from: presentation)),
                            primaryButton: .default(Text("重试"), action: onRetry),
                            secondaryButton: .cancel(Text("关闭")) {
                                self.error = nil
                            }
                        )
                    } else {
                        return Alert(
                            title: Text(presentation.title),
                            message: Text(errorMessage(from: presentation)),
                            dismissButton: .default(Text("关闭")) {
                                self.error = nil
                            }
                        )
                    }
                } else {
                    return Alert(title: Text("错误"))
                }
            }
    }
    
    private func errorMessage(from presentation: ErrorPresentation) -> String {
        var message = presentation.message
        if let suggestion = presentation.recoverySuggestion {
            message += "\n\n\(suggestion)"
        }
        return message
    }
}

extension View {
    /// Display an error alert
    /// - Parameters:
    ///   - error: Binding to optional error
    ///   - onRetry: Optional retry action
    func errorAlert(error: Binding<Error?>, onRetry: (() -> Void)? = nil) -> some View {
        modifier(ErrorAlert(error: error, onRetry: onRetry))
    }
}

/// Inline error banner view
struct ErrorBanner: View {
    let presentation: ErrorPresentation
    let onDismiss: () -> Void
    
    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: presentation.severity.iconName)
                .foregroundColor(presentation.severity.color)
            
            VStack(alignment: .leading, spacing: 4) {
                Text(presentation.title)
                    .font(.headline)
                
                Text(presentation.message)
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
            
            Spacer()
            
            Button(action: onDismiss) {
                Image(systemName: "xmark.circle.fill")
                    .foregroundColor(.secondary)
            }
            .buttonStyle(.plain)
        }
        .padding()
        .background(
            RoundedRectangle(cornerRadius: 8)
                .fill(presentation.severity.color.opacity(0.1))
        )
        .overlay(
            RoundedRectangle(cornerRadius: 8)
                .stroke(presentation.severity.color.opacity(0.3), lineWidth: 1)
        )
    }
}

#Preview("Error View - Network Error") {
    ErrorView(
        presentation: ErrorPresentation(
            title: "网络错误",
            message: "无法连接到服务器",
            recoverySuggestion: "请检查网络连接并重试",
            severity: .error,
            category: .network
        ),
        onDismiss: {},
        onRetry: {}
    )
}

#Preview("Error Banner - Warning") {
    ErrorBanner(
        presentation: ErrorPresentation(
            title: "警告",
            message: "硬件加速失败，已切换到软件解码",
            severity: .warning,
            category: .player
        ),
        onDismiss: {}
    )
    .padding()
}

// MARK: - ErrorSeverity Extensions

extension ErrorSeverity {
    var iconName: String {
        switch self {
        case .info:
            return "info.circle.fill"
        case .warning:
            return "exclamationmark.triangle.fill"
        case .error:
            return "xmark.circle.fill"
        case .critical:
            return "exclamationmark.octagon.fill"
        }
    }
    
    var color: Color {
        switch self {
        case .info:
            return .blue
        case .warning:
            return .orange
        case .error:
            return .red
        case .critical:
            return .purple
        }
    }
}
