# PPT Clicker

> Turn your phone into a presentation remote. **Bluetooth HID (driver-free)** or **WiFi companion**.

> 把手机变成翻页笔。**蓝牙 HID 免安装** 或 **WiFi 伴侣增强**，完全免费开源。

[![Release](https://img.shields.io/github/v/release/dbacyj/ppt-clicker)](https://github.com/dbacyj/ppt-clicker/releases)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![CI](https://github.com/dbacyj/ppt-clicker/actions/workflows/ci.yml/badge.svg)](https://github.com/dbacyj/ppt-clicker/actions)

## 这是什么 / What is it

A project that lets you control presentation slides with your phone, aiming to replace dedicated hardware clickers. Available in two connection modes (dual-track):

让你用手机控制 PPT 翻页，目标是替代需要额外购买的硬件翻页笔。两种连接模式（双轨）：

| 模式 / Mode | 电脑需装软件? / PC software needed? | 适用 / Use case |
|------|---------------------|----------|
| **蓝牙 HID / Bluetooth HID** | ❌ 完全免安装 / None | 手机模拟成蓝牙键盘，电脑原生识别，即配即用 / Phone emulates a BT keyboard, works out of the box |
| **WiFi 伴侣 / WiFi Companion** | ✅ 需运行伴侣程序 / Companion app | 通过局域网下发指令，为后续提词器/计时/AI 功能预留通道 / LAN-based, unlocks future features |

> ⚠️ Bluetooth HID relies on the `BluetoothHidDevice` API (Android 9+). **Some Chinese ROMs (MIUI/EMUI/ColorOS) strip this capability** — the app auto-detects and falls back to WiFi mode.
>
> iOS cannot emulate a Bluetooth keyboard due to Apple restrictions; iPhone users must use the WiFi companion mode (iOS client not yet implemented, see roadmap).

## 功能 / Features (v0.1.0)

- ✅ 上/下翻页 / Next & previous slide (PageUp/PageDown)
- ✅ 开始演示 (F5)、退出全屏 (Esc) / Start presentation, exit fullscreen
- ✅ 黑屏 (B)、白/黑屏切换 (.) / Black screen, screen toggle
- ✅ 回首页 (Home)、末页 (End) / Jump to first / last slide
- ✅ 蓝牙 HID 自动配对 / Auto Bluetooth pairing
- ✅ WiFi 伴侣：mDNS 自动发现 + 配对码 / mDNS discovery + pairing code
- ✅ 心跳保活、断线重连 / Heartbeat, auto-reconnect
- ✅ 蓝牙不可用时自动降级 WiFi / Auto fallback to WiFi

后续 / Roadmap (P1/P2)：提词器、备注同步、当前页、计时震动、聚光灯、语音翻页、AI 演讲辅助。

## 下载 / Download

⬇️ **[Latest Release](https://github.com/dbacyj/ppt-clicker/releases/latest)** — Android APK

官网 / Website: https://dbacyj.github.io/ppt-clicker/

## 使用流程 / Usage

**蓝牙模式 / Bluetooth (driver-free):**
1. 手机装好 App，打开蓝牙，进入「蓝牙 · 免安装」模式 / Open the app, enable Bluetooth, select Bluetooth mode
2. 电脑蓝牙设置里搜索设备，找到 "PPT Clicker" 并配对 / Pair with "PPT Clicker" from your PC's Bluetooth settings
3. 手机状态显示「已连接电脑」后即可翻页 / Start clicking once it shows "Connected"

**WiFi 模式 / WiFi (companion):**
1. 电脑运行伴侣程序，记下显示的配对码 / Run the companion on your PC, note the pairing code
2. 手机与电脑连同一 WiFi / Connect both devices to the same WiFi
3. App 切到「WiFi · 伴侣程序」模式，自动发现电脑，输入配对码 / Select WiFi mode, auto-discover, enter code

## 仓库结构 / Repository

```
ppt-clicker/
├── .github/workflows/  # GitHub Actions (CI + release build)
├── protocol/           # WiFi protocol spec (shared by all clients/companions)
├── android-app/        # Android app (Kotlin, minSdk 28)
├── companion/
│   ├── windows/        # Windows companion (C# .NET 8)
│   └── macos/          # macOS companion (Swift, Network.framework)
└── docs/               # GitHub Pages landing page
```

## 构建 / Build

**Android (GitHub Actions cloud build, no local setup needed):**
Push a `v*` tag — CI compiles a signed APK and publishes it to Releases automatically.
```bash
git tag v0.1.0 && git push origin v0.1.0
```

**Local Android build** (Android Studio + JDK 17):
```bash
cd android-app && ./gradlew assembleDebug
```

**Windows companion** (.NET 8 SDK):
```bash
cd companion/windows && dotnet publish -c Release
```

**macOS companion** (Xcode Command Line Tools, Swift 5.9+):
```bash
cd companion/macos && swift build -c release
```

> ⚠️ macOS: first run requires granting **Accessibility** permission (System Settings → Privacy → Accessibility) for CGEvent to work.

## 技术文档 / Docs

- [WiFi 通信协议规范 / Protocol spec](protocol/spec.md)
- [键位映射表 / Key mapping](protocol/spec.md#6-键值表逻辑键名)

## 路线图 / Roadmap

- [x] MVP：双轨翻页（蓝牙 HID + WiFi 伴侣）/ Dual-track clicking
- [ ] P1：提词器、当前页、计时震动、聚光灯 / Teleprompter, current slide, timer, spotlight
- [ ] P2：语音翻页、AI 演讲辅助 / Voice control, AI assistance
- [ ] iOS 端 / iOS client
- [ ] 应用商店上架 / App store release

## 关于本项目 / About

项目初始版本由 [ZCode](https://zcode.dev/) 使用 GLM-5.2 / GLM-5-Turbo 免费额度辅助生成，属于 AI + 人类协作的开源实践。后续版本将在此基础上持续迭代维护。

> The initial version was AI-assisted via ZCode (GLM models), an AI + human collaboration experiment. Active maintenance continues on top of it.

> 💡 **遇到问题？/ Got an issue?** [提交 Issue](https://github.com/dbacyj/ppt-clicker/issues) 反馈，或 [Fork 后提交 PR](https://github.com/dbacyj/ppt-clicker/fork) 直接修改——开源项目欢迎任何形式的参与。

## 许可证 / License

[MIT](LICENSE) — 自由使用、修改、分发 / Free to use, modify, and distribute.

## 赞助 / Sponsor

本项目完全免费开源。如果你觉得有用，欢迎支持开发者继续维护：
This project is free and open source. If you find it useful, consider supporting development:

- 💚 [GitHub Sponsors](https://github.com/sponsors/dbacyj)
- 🩷 [爱发电 / Afdian](https://afdian.net/a/dbacyj)

## 贡献 / Contributing

欢迎提交 Issue 和 Pull Request！详见 [贡献指南](CONTRIBUTING.md)。
