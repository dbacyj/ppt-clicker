# PPT Clicker

> 把手机变成翻页笔。一个 App 两种用法：**蓝牙 HID 免安装** 或 **WiFi 伴侣增强**。

## 这是什么

一个让你用手机控制 PPT 翻页的项目，目标是替代需要额外购买的硬件翻页笔。

**两种连接模式（双轨）：**

| 模式 | 电脑端是否需要装软件 | 适用场景 |
|------|---------------------|----------|
| **蓝牙 HID** | ❌ 完全免安装 | 手机模拟成蓝牙键盘，电脑原生识别，即配即用 |
| **WiFi 伴侣** | ✅ 需运行伴侣程序 | 通过局域网下发指令，为后续提词器/计时/AI 功能预留通道 |

> ⚠️ 蓝牙 HID 模式依赖 Android 9+ 的 `BluetoothHidDevice` API。**部分国产 ROM（MIUI/EMUI/ColorOS）阉割了该能力**，App 会自动探测并降级到 WiFi 模式。
> iOS 受苹果限制无法模拟蓝牙键鼠，iPhone 用户需走 WiFi 伴侣模式（iOS 端暂未实现，见路线图）。

## 功能（MVP v0.1.0）

- ✅ 上/下翻页（PageUp/PageDown）
- ✅ 开始演示（F5）、退出全屏（Esc）
- ✅ 黑屏（B）、白/黑屏切换（.）
- ✅ 回首页（Home）、末页（End）
- ✅ 蓝牙 HID 自动配对连接
- ✅ WiFi 伴侣：mDNS 自动发现 + 配对码
- ✅ 心跳保活、断线重连
- ✅ 蓝牙不可用时自动降级 WiFi

后续（P1/P2）：提词器、备注同步、当前页、计时震动、聚光灯、语音翻页、AI 演讲辅助。

## 仓库结构

```
ppt-clicker/
├── .github/
│   ├── workflows/      # GitHub Actions（CI + 自动编译发布）
│   └── scripts/        # 签名密钥配置指引
├── protocol/           # WiFi 通信协议规范（安卓+两端伴侣共用）
├── android-app/        # 安卓 App（Kotlin，minSdk 28）
├── companion/
│   ├── windows/        # Windows 伴侣（C# .NET 8）
│   └── macos/          # macOS 伴侣（Swift，Network.framework）
├── landing/            # GitHub Pages 落地页
├── LICENSE             # MIT 许可证
└── README.md
```

## 构建与运行

### 安卓 App（推荐：GitHub Actions 云端编译，无需本地环境）

**一键发布**：推送 `v*` 格式的 tag，GitHub Actions 自动编译签名 APK 并发布到 Release。

```bash
# 标记版本并推送（首次需配置签名密钥，见 .github/scripts/gen-keystore.md）
git tag v0.1.0
git push origin v0.1.0
# 等待 ~3 分钟，在仓库的 Releases 页面下载 APK
```

CI 也会在每次 push/PR 到 main 时自动编译 debug APK 做健康检查（见 `.github/workflows/ci.yml`）。

**本地调试**（可选，需 Android Studio + JDK 17）：

```bash
cd android-app
./gradlew assembleDebug
# 产物：app/build/outputs/apk/debug/app-debug.apk
```

### Windows 伴侣

需要 .NET 8 SDK。

```bash
cd companion/windows
dotnet publish -c Release
# 产物：bin/Release/net8.0-windows/win-x64/publish/PPTClicker.exe（自包含，无需装运行时）
```

运行：双击 `PPTClicker.exe`，控制台会显示配对码与监听端口。

### macOS 伴侣

需要 Xcode Command Line Tools（Swift 5.9+）。

```bash
cd companion/macos
swift build -c release
# 产物：.build/release/PPTClicker
.build/release/PPTClicker
```

> ⚠️ macOS 首次运行需在「系统设置 → 隐私与安全性 → 辅助功能」中授权本程序（CGEvent 按键需要）。

## 使用流程

**蓝牙模式（免安装）：**
1. 手机装好 App，打开蓝牙，进入「蓝牙 · 免安装」模式
2. 电脑蓝牙设置里搜索设备，找到 "PPT Clicker" 并配对
3. 手机状态显示「已连接电脑」后即可翻页

**WiFi 模式（伴侣程序）：**
1. 电脑运行伴侣程序，记下显示的配对码
2. 手机与电脑连同一 WiFi
3. App 切到「WiFi · 伴侣程序」模式，自动发现电脑，输入配对码
4. 连接成功后即可翻页

## 发布流程

本项目通过 **GitHub 一站式发布**，零账号、零成本。

### 发布新版本（3 步）

1. **配置签名密钥**（仅首次）：按 [.github/scripts/gen-keystore.md](.github/scripts/gen-keystore.md) 生成 keystore 并添加到 GitHub Secrets
2. **打 tag 并推送**：
   ```bash
   git tag v0.1.0
   git push origin v0.1.0
   ```
3. **等待 ~3 分钟**：GitHub Actions 自动编译签名 APK 并创建 Release，在仓库 **Releases** 页面即可下载

### 官网落地页

开启 GitHub Pages（Settings → Pages → Source: `main` 分支），`landing/index.html` 会自动部署为 `https://<你的用户名>.github.io/ppt-clicker/`。落地页会从 GitHub Release API 自动拉取最新下载链接。

### 后续上架应用商店（可选）

- Google Play：$25 一次性（个人账号需 ≥20 人测试 14 天后才能公开发布）
- Apple App Store：$99/年（需开发 iOS 端 + 电脑伴侣模式）

## 技术文档

- [WiFi 通信协议规范](protocol/spec.md)
- [键位映射表](protocol/spec.md#6-键值表逻辑键名)

## 路线图

- [x] MVP：双轨翻页（蓝牙 HID + WiFi 伴侣）
- [ ] P1：提词器、当前页、计时震动、聚光灯（依赖伴侣程序解析 PPT 状态）
- [ ] P2：语音翻页、AI 演讲辅助
- [ ] iOS 端（需 Apple 开发者账号 + 电脑伴侣模式）
- [ ] 应用商店上架

## 许可证

[MIT](LICENSE) —— 自由使用、修改、分发。

## 赞助

本项目完全免费开源。如果你觉得有用，欢迎支持开发者继续维护：

- 💚 [GitHub Sponsors](https://github.com/sponsors)（国际用户，GitHub 首年免手续费）
- 🩷 [爱发电](https://afdian.net)（国内用户，支持微信/支付宝）

## 贡献

欢迎提交 Issue 和 Pull Request！详见 [贡献指南](CONTRIBUTING.md)。
