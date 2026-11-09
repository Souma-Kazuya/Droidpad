# 🎮 DroidPad

**Use your Android phone as a keyboard and Xbox gamepad on Linux — over Wi-Fi, Bluetooth, or USB.**

```
  Android App  ──── Wi-Fi / Bluetooth / USB ────►  Linux Daemon
  (Controller)                                       (Host)
      📱                                              🐧
  Keyboard UI    ──── HID packets ────►   /dev/uinput  ──►  evdev
  Gamepad UI     ──── HID packets ────►   /dev/uinput  ──►  xpad-compat
```

---

## Features

| Feature | Detail |
|---------|--------|
| **Keyboard** | Full QWERTY + modifiers + F-keys + arrows |
| **Gamepad** | Xbox 360-compatible (A/B/X/Y, sticks, triggers, D-pad, bumpers) |
| **Wi-Fi** | Auto-discovery via mDNS · encrypted (TLS) |
| **Bluetooth** | RFCOMM (BT Classic) · PIN pairing via OS |
| **USB** | Via ADB forward (no root required) |
| **Linux UI** | GTK4 system tray + settings window |
| **Android UI** | Jetpack Compose · Material 3 · dark/light theme |
| **Rumble** | Host → Android haptic feedback |

---

## Download & Installation

Pre-built binaries are available for every release on [**GitHub Releases**](../../releases):

| Platform | Download | Instructions |
|----------|----------|--------------|
| **📱 Android** | [**`droidpad.apk`**](../../releases/latest) | Download to phone → Tap to install |
| **🐧 Linux** | [**`DroidPad-x86_64.AppImage`**](../../releases/latest) | `chmod +x DroidPad-x86_64.AppImage && ./DroidPad-x86_64.AppImage` |
| **⚙️ Permissions** | [**`setup-uinput.sh`**](../../releases/latest) | `sudo ./setup-uinput.sh` (one-time `/dev/uinput` setup) |

See the [**Installation Guide**](docs/INSTALL.md) for full instructions, systemd auto-start, and build-from-source steps.

---

## Quick Start

### 1. Launch on Linux Host

Run the AppImage or daemon:
```bash
chmod +x DroidPad-x86_64.AppImage
./DroidPad-x86_64.AppImage
```

The daemon opens the status window and listens concurrently on:
- Wi-Fi: port **27183** (TCP+TLS, auto-discovered via mDNS)
- Bluetooth: RFCOMM UUID `6e400001-b5a3-f393-e0a9-e50e24dcca9e`
- USB: port **27184** on `127.0.0.1` (ADB tunnel)

### 2. Connect from Android Phone

Open **DroidPad** on your phone, choose your preferred connection:

| Method | Steps |
|--------|-------|
| **Wi-Fi** | Ensure phone and PC are on the same Wi-Fi · tap **Wi-Fi** · tap your PC name |
| **Bluetooth** | Pair phone with PC in Android Bluetooth Settings · tap **Bluetooth** · tap your PC |
| **USB** | Plug in USB cable · enable USB debugging · run `adb forward tcp:27184 tcp:27184` · tap **USB** |

---

## Architecture

```
┌────────────────────────────────────────────────────────────┐
│                     Android App                            │
│  ┌──────────┐  ┌──────────────┐  ┌──────────────────────┐ │
│  │ Keyboard │  │   Gamepad    │  │ Connection / Settings │ │
│  │  Screen  │  │   Screen     │  │       Screen          │ │
│  └────┬─────┘  └──────┬───────┘  └──────────────────────┘ │
│       │               │                                    │
│  ┌────▼───────────────▼──────────────────────────────────┐ │
│  │            HID Encoder (Keyboard / Gamepad)           │ │
│  └────────────────────────┬──────────────────────────────┘ │
│                            │  Packet (binary)               │
│  ┌─────────────────────────▼─────────────────────────────┐ │
│  │          Transport Manager (Wi-Fi / BT / USB)         │ │
│  │               DroidPad Foreground Service             │ │
│  └───────────────────────────────────────────────────────┘ │
└────────────────────────────────────────────────────────────┘
                           │  TCP+TLS / RFCOMM / ADB
┌──────────────────────────▼──────────────────────────────────┐
│                     Linux Daemon (Rust)                      │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │        Transport Server (Tokio async, all 3 at once)    │ │
│  └────────────────────────┬────────────────────────────────┘ │
│                            │  Packet decode                  │
│  ┌─────────────────────────▼──────────────┐                 │
│  │  Protocol Parser → Dispatcher          │                 │
│  └──┬─────────────────────────────────────┘                 │
│     │ keyboard events      │ gamepad events                 │
│  ┌──▼──────────────┐  ┌────▼──────────────────────────────┐ │
│  │ VirtualKeyboard │  │        VirtualGamepad              │ │
│  │  (uinput)       │  │  Xbox 360 layout (uinput)         │ │
│  └──────────────────┘  └────────────────────────────────────┘ │
│                                                              │
│  ┌──────────────────────────────────────────────────────────┐ │
│  │  GTK4 UI: System Tray (status) + Settings Window        │ │
│  └──────────────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────────────┘
                           │
                    /dev/uinput
                           │
                    Linux kernel evdev
                           │
              ┌────────────┴────────────┐
        Any game/app              Desktop input
```

---

## Wire Protocol

See [`docs/PROTOCOL.md`](docs/PROTOCOL.md) for the full binary protocol spec.

---

## Building from Source

### Linux Daemon

```bash
cd linux-daemon
cargo build --release
# Binary at: target/release/droidpad-daemon
```

Requirements: `libgtk-4-dev`, `libadwaita-1-dev`, `libbluetooth-dev`

### Android App

```bash
cd android-app
./gradlew assembleDebug
# APK at: app/build/outputs/apk/debug/app-debug.apk
```

---

## Latency Targets

| Transport | Target | Typical |
|-----------|--------|---------|
| Wi-Fi (LAN) | < 20 ms | 2–8 ms |
| Bluetooth | < 40 ms | 15–35 ms |
| USB (ADB) | < 10 ms | 1–4 ms |

---

## License

MIT © DroidPad Contributors
