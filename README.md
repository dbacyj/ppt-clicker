<div align="center">

# PPT Clicker

Turn your phone into a presentation remote.

Bluetooth HID (driver-free) or WiFi companion — no hardware required.

[![Release](https://img.shields.io/github/v/release/dbacyj/ppt-clicker?color=blue)](https://github.com/dbacyj/ppt-clicker/releases)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![CI](https://github.com/dbacyj/ppt-clicker/actions/workflows/ci.yml/badge.svg)](https://github.com/dbacyj/ppt-clicker/actions/workflows/ci.yml)
[![Platform](https://img.shields.io/badge/platform-Android%209%2B-green.svg)](#download)

[Download](#download) · [How it works](#how-it-works) · [Build](#build) · [Contribute](#contributing)

</div>

---

## Why

Hardware presentation clickers cost money, run out of battery, and get forgotten at home. The one device you always have with you — your phone — is more than capable of doing the job, and more.

PPT Clicker is built around two ideas:

1. **Just works, with nothing installed.** Pair your phone over Bluetooth and it behaves like a real keyboard — no drivers, no companion app on the PC, no setup ceremony.
2. **Goes further when you want it to.** An optional desktop companion unlocks features a hardware clicker can never offer (teleprompter, slide-aware AI — on the roadmap).

It is free and open source under the MIT license.

## How it works

Two connection modes, auto-selected based on your device's capabilities.

| Mode | PC setup | Best for |
|------|----------|----------|
| **Bluetooth HID** | None — your phone appears as a standard Bluetooth keyboard | Quick, no-fuss presenting |
| **WiFi Companion** | Run a small companion program on the PC | Lower latency, future slide-aware features |

> **Note:** Bluetooth HID requires the `BluetoothHidDevice` API (Android 9+). Some Android ROMs strip this capability — the app detects this and gracefully falls back to WiFi mode. iOS cannot emulate a Bluetooth keyboard, so iPhone users use the companion mode (iOS client is on the roadmap).

## Features

**v0.1.0 (MVP)**
- Next / previous slide
- Start presentation (F5), exit (Esc)
- Black screen (B), screen toggle (.)
- Jump to first / last slide
- Automatic Bluetooth pairing
- WiFi companion with mDNS auto-discovery + pairing code
- Heartbeat keep-alive and reconnect
- Automatic Bluetooth → WiFi fallback

**Roadmap**
- Slide-aware teleprompter (speaker notes on phone)
- Timer with haptic alerts
- Digital spotlight / laser pointer
- Voice control ("next slide")
- AI presentation assistant
- iOS client

See the [open issues](https://github.com/dbacyj/ppt-clicker/issues) and [project board](https://github.com/dbacyj/ppt-clicker/projects) for what's being worked on.

## Download

**[Latest release →](https://github.com/dbacyj/ppt-clicker/releases/latest)** — Android APK

> The Windows and macOS companions will be distributed alongside releases once they ship.

## Build

### Android app

CI builds a signed APK on every `v*` tag — push a tag and a release appears automatically:

```bash
git tag v0.1.0 && git push origin v0.1.0
```

Local build (requires Android Studio + JDK 17):

```bash
cd android-app
./gradlew assembleDebug
```

### Windows companion

```bash
cd companion/windows
dotnet publish -c Release
```

### macOS companion

```bash
cd companion/macos
swift build -c release
```

> The macOS companion needs **Accessibility** permission on first run (System Settings → Privacy → Accessibility) for input simulation to work.

## Project structure

```
ppt-clicker/
├── .github/workflows/   # CI + release pipelines
├── protocol/            # WiFi protocol spec (shared by all clients/companions)
├── android-app/         # Android app (Kotlin, minSdk 28)
├── companion/
│   ├── windows/         # Windows companion (C#, .NET 8)
│   └── macos/           # macOS companion (Swift, Network.framework)
└── docs/                # GitHub Pages site
```

## Documentation

- [WiFi protocol specification](protocol/spec.md)
- [Key mapping](protocol/spec.md#6-key-mapping-logical-key-names)
- [Setting up release signing](.github/scripts/gen-keystore.md)
- [Website setup](docs/README.md)

## Contributing

Contributions are welcome — bug reports, features, translations, or code.

1. Read the [contributing guide](CONTRIBUTING.md)
2. Check [open issues](https://github.com/dbacyj/ppt-clicker/issues) for something to work on
3. Open a pull request

Please follow the [code of conduct](CODE_OF_CONDUCT.md) in all interactions.

## Origin

The initial version of this project was created with assistance from [ZCode](https://zcode.dev/) using the GLM-5.2 and GLM-5-Turbo models — an experiment in AI-assisted software development. It is now maintained as a community project.

## License

[MIT](LICENSE) — free to use, modify, and distribute.
