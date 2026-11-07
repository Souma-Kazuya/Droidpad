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

## Quick Start

### Linux Host

```bash
# Install (Debian/Ubuntu)
sudo dpkg -i droidpad_*.deb

# Or run directly
cargo install --path linux-daemon/droidpad-daemon
droidpad-daemon
```

The daemon starts and shows a system-tray icon. By default it listens on:
- Wi-Fi: port **27183** (TCP+TLS, mDNS advertised as `_droidpad._tcp.local.`)
- Bluetooth: RFCOMM UUID `6e400001-b5a3-f393-e0a9-e50e24dcca9e`
- USB: port **27184** on `127.0.0.1` (ADB tunnel)

### Android App

Install the APK, open **DroidPad**, tap a connection method:

| Method | Steps |
|--------|-------|
| **Wi-Fi** | Ensure phone and PC are on same network · tap Wi-Fi · select host from list |
| **Bluetooth** | Pair phone with PC in Android Settings first · tap Bluetooth · select PC |
| **USB** | Plug phone in · enable USB debugging · run `adb forward tcp:27184 tcp:27184` · tap USB |

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
