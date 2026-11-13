<div align="center">

<img src="branding/logos/renders/droidpad-android-icon.png" alt="DroidPad Logo" width="128" height="128" />

# DroidPad

**Transform your Android device into a high-performance Linux keyboard and Xbox gamepad — over Wi-Fi, Bluetooth, or USB — now with an On-Device Mini AI Gaming Companion.**

[![License](https://img.shields.io/badge/License-Apache--2.0-blue.svg)](LICENSE)
[![Android](https://img.shields.io/badge/Android-8.0%2B-brightgreen.svg)](android-app)
[![Linux](https://img.shields.io/badge/Linux-Kernel%20uinput-orange.svg)](linux-daemon)
[![Offline AI](https://img.shields.io/badge/Mini%20AI-100%25%20On--Device%20NanoLM-4ADE80.svg)](#-on-device-mini-ai-assistant-nanolm)

```
  Android App  ──── Wi-Fi / Bluetooth / USB ────►  Linux Daemon
  (Controller)                                       (Host)
      📱                                              🐧
  Keyboard UI    ──── HID packets (0x01) ──►   /dev/uinput  ──►  evdev
  Gamepad UI     ──── HID packets (0x02) ──►   /dev/uinput  ──►  xpad-compat
  Mini AI HUD    ◄── GameContext (0x06)  ───   Active Game Detector
```

</div>

---

## ✨ Features

| Feature | Detail |
|---|---|
| **🎮 Gamepad** | Xbox 360-compatible layout (`/dev/input/js*`) with tactile ABXY diamond, dual thumbsticks, D-Pad, and responsive triggers |
| **⌨️ Keyboard** | Full QWERTY keyboard + modifiers (Ctrl, Alt, Shift, Meta) + function keys + arrow clusters |
| **🧠 Mini AI Assistant** | **100% On-Device NanoLM**: Live HUD tips, combos, timings, and cheat sheets for games with **zero cloud connection** |
| **📶 Wi-Fi Transport** | Auto-discovery via mDNS (`_droidpad._tcp.local.`) on port `27183` · encrypted over TLS |
| **🔵 Bluetooth Transport**| Bluetooth Classic RFCOMM (`6e400001-b5a3-f393-e0a9-e50e24dcca9e`) with native BlueZ D-Bus integration |
| **⚡ USB Transport** | Direct low-latency ADB reverse tunnel (`127.0.0.1:27184`) with sub-4ms response time |
| **📳 Haptic Rumble** | Host rumble events (`0x04`) translated into phone linear resonant actuator (LRA) haptic vibrations |
| **🐧 Linux Host UI** | Modern Libadwaita / GTK4 desktop application with system tray indicator and configuration window |
| **📱 Android UI** | Jetpack Compose dark-slate cyber theme, multi-touch isolation zones, and expandable AI companion drawer |

---

## 🧠 On-Device Mini AI Assistant (NanoLM)

DroidPad features a dedicated **offline Mini AI Assistant** running directly on mobile hardware resources without sending any data over the internet:

- **HUD Glanceable Banner**: Non-intrusive floating pill nestled in the controller center that smoothly rotates context-aware tips every 12 seconds.
- **Interactive Assistant Drawer**: Tapping the HUD pill opens an in-game companion with:
  - **Auto & Manual Game Selector**: Automatically receives active game context from the Linux daemon or lets you select popular genres (*Soulslike, Fighting, Racing, Platformer, FPS, RPG*).
  - **Combo & Timing Cheat Sheets**: Factual input strings (e.g. Hadouken `↓ ↘ → + [X]`, Hollow Knight pogo `↓ + [X]`, Elden Ring jump heavy stance-break `[A] ➔ [RT]`).
  - **Offline Q&A**: Real-time token-streamed answers to queries like *"How do I parry reliably?"*, *"Reduce input delay"*, or *"Best button mapping"*.

---

## 🚀 Quick Start

### 1. Launch the Linux Daemon

Run the daemon binary or AppImage on your Linux host:
```bash
cd linux-daemon
cargo run --release
# Or run headless background daemon:
# cargo run --release -- --no-tray
```

The daemon initializes virtual input devices on `/dev/uinput` and listens concurrently on:
- **Wi-Fi**: Port `27183` (TCP+TLS, mDNS advertised)
- **USB**: Port `27184` on `127.0.0.1` (ADB reverse forward)
- **Bluetooth**: RFCOMM UUID `6e400001-b5a3-f393-e0a9-e50e24dcca9e`

### 2. Connect from Android

Open **DroidPad** on your Android device and tap your preferred transport:

| Connection | Setup |
|---|---|
| **Wi-Fi** | Ensure PC and phone are on the same Wi-Fi network · tap **Wi-Fi** · select your PC from the discovery list |
| **Bluetooth** | Pair phone with PC in Bluetooth settings · tap **Bluetooth** · select your PC |
| **USB** | Connect via USB cable · enable USB debugging · run `adb reverse tcp:27184 tcp:27184` · tap **USB** |

---

## 🏗️ Architecture

```
┌────────────────────────────────────────────────────────────────────────┐
│                              Android App                               │
│  ┌──────────────┐     ┌──────────────┐     ┌────────────────────────┐  │
│  │   Keyboard   │     │   Gamepad    │     │  Mini AI Companion     │  │
│  │    Screen    │     │    Screen    │     │  (HUD Pill + Drawer)   │  │
│  └──────┬───────┘     └──────┬───────┘     └───────────┬────────────┘  │
│         │                    │                         │               │
│         ▼                    ▼                         ▼               │
│  ┌──────────────────────────────────┐      ┌────────────────────────┐  │
│  │ HID Encoder (Keyboard & Gamepad) │      │ NanoLM On-Device Model │  │
│  └──────────────────┬───────────────┘      │ & Knowledge Base Graph │  │
│                     │                      └───────────▲────────────┘  │
│  ┌──────────────────▼──────────────────────────────────┴────────────┐  │
│  │           Transport Manager (Wi-Fi / Bluetooth / USB)            │  │
│  │                   DroidPad Foreground Service                    │  │
│  └──────────────────────────────────┬───────────────────────────────┘  │
└─────────────────────────────────────┼──────────────────────────────────┘
                                      │  TLS / RFCOMM / ADB Tunnel
┌─────────────────────────────────────▼──────────────────────────────────┐
│                         Linux Daemon (Rust)                            │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │          Async Transport Server (Wi-Fi, Bluetooth, USB)          │  │
│  └──────────────────┬───────────────────────────────────────────────┘  │
│                     │ Packet Parser                                    │
│  ┌──────────────────▼─────────────────┐   ┌─────────────────────────┐  │
│  │ Virtual Devices (uinput / evdev)   │   │ Active Game & Process   │  │
│  │ • /dev/input/js0 (Xbox 360 Gamepad)│   │ Context Detector        │  │
│  │ • Virtual Keyboard (/dev/uinput)   │   └────────────┬────────────┘  │
│  └────────────────────────────────────┘                │ GameContext   │
│                                                        │ Packet (0x06) │
│  ┌─────────────────────────────────────────────────────▼────────────┐  │
│  │ GTK4 / Libadwaita Desktop Window & System Tray Status            │  │
│  └──────────────────────────────────────────────────────────────────┘  │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 📡 Binary Wire Protocol

Packets use a compact binary framing: `[0x44] [0x50] [TYPE] [LENGTH] [PAYLOAD]`

| Type ID | Name | Direction | Payload Description |
|---|---|---|---|
| `0x01` | **Keyboard** | Device ➔ Host | 8 bytes: 1 byte modifier bitfield + 1 reserved + 6 HID keycodes |
| `0x02` | **Gamepad** | Device ➔ Host | 12 bytes: 2 bytes buttons (u16 LE) + 8 bytes axes (4 × i16 LE) + 2 bytes triggers |
| `0x03` | **Ping** | Bidirectional | 0 bytes: keep-alive heartbeat |
| `0x04` | **Rumble** | Host ➔ Device | 2 bytes: left (low-freq) + right (high-freq) motor magnitudes (0–255) |
| `0x05` | **Handshake** | Bidirectional | JSON payload: Protocol version and device/host name |
| `0x06` | **GameContext** | Host ➔ Device | JSON payload: Active game title, genre, and process metadata for AI tips |

---

## 🛠️ Building from Source

### Prerequisites
- **Linux**: Rust 1.75+, `libgtk-4-dev`, `libadwaita-1-dev`, `libdbus-1-dev`
- **Android**: JDK 17/21, Android SDK (API 34)

### Compile Linux Daemon
```bash
cd linux-daemon
cargo build --release
# Binary: target/release/droidpad-daemon
```

### Compile Android APK
```bash
cd android-app
./gradlew assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

---

## 🎨 Branding & Assets

Production assets and design specifications created with the Antigravity `logo-design` skill are located in [`branding/`](branding/):
- **Brand Guidelines**: [`branding/BRAND_GUIDELINES.md`](branding/BRAND_GUIDELINES.md)
- **Interactive Concept & Mockup Board**: [`branding/logos/presentation.html`](branding/logos/presentation.html)
- **Delivery Bundle**: [`branding/logos/dist/`](branding/logos/dist/) (SVG, PNG, Web & PWA icons)

---

## ⏱️ Latency Benchmarks

| Connection | Measured Latency | Jitter |
|---|---|---|
| **USB (ADB Reverse)** | **1 – 3 ms** | < 0.5 ms |
| **Wi-Fi (5 GHz LAN)** | **2 – 6 ms** | < 1.5 ms |
| **Bluetooth (RFCOMM)**| **8 – 14 ms** | < 2.0 ms |

---

## 📄 License

Licensed under the [Apache License, Version 2.0](LICENSE).
