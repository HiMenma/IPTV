// swift-tools-version: 5.9
// The swift-tools-version declares the minimum version of Swift required to build this package.

import PackageDescription

let package = Package(
    name: "IPTVPlayer",
    platforms: [
        .macOS(.v13)
    ],
    products: [
        .library(
            name: "IPTVPlayer",
            targets: ["IPTVPlayer"]),
    ],
    dependencies: [
        // Networking
        .package(url: "https://github.com/Alamofire/Alamofire.git", from: "5.8.0"),
        // Property-based testing
        .package(url: "https://github.com/typelift/SwiftCheck.git", from: "0.12.0"),
    ],
    targets: [
        .target(
            name: "IPTVPlayer",
            dependencies: [
                "Alamofire"
            ]),
        .testTarget(
            name: "IPTVPlayerTests",
            dependencies: [
                "IPTVPlayer",
                "SwiftCheck"
            ]),
    ]
)
