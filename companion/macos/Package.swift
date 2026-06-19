// SPDX-License-Identifier: MIT
// Package.swift — macOS 伴侣程序（Swift 命令行可执行文件）
// 依赖：swift-nio + swift-nio-websocket（WebSocket server）

// swift-tools-version:5.9
import PackageDescription

let package = Package(
    name: "PPTClicker",
    platforms: [.macOS(.v12)],
    targets: [
        .executableTarget(
            name: "PPTClicker",
            path: "Sources/PPTClicker"
        ),
    ]
)
