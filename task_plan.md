# Task Plan: DroidPad — Android as Linux Keyboard & Xbox Gamepad

## Goal
Build a cross-platform system (Linux daemon + Android app) that lets an Android device act as a keyboard and Xbox-compatible gamepad on a Linux host over Wi-Fi, Bluetooth, and USB — with an interactive UI on both sides.

## Current Phase
Delivery & Summary (All phases completed)

---

## Phases

### Phase 1: Requirements & Discovery
- [x] Capture user requirements
- [x] Identify transport layers (Wi-Fi, Bluetooth, USB)
- [x] Identify input emulation approach on Linux (uinput / USB gadget)
- [x] Research Android input APIs
- [x] Document findings in findings.md
- **Status:** complete

---

### Phase 2: Architecture & Design
- [x] Define protocol spec (packet format, handshake, heartbeat)
- [x] Design Linux daemon architecture (transport manager, uinput writer, config)
- [x] Design Android app architecture (UI, transport manager, HID encoder)
- [x] Choose UI frameworks (Linux: GTK4 / Libadwaita; Android: Jetpack Compose)
- [x] Define project folder structure for both sides
- [x] Document all decisions with rationale
- **Status:** complete

---

### Phase 3: Linux Daemon — Core
- [x] Scaffold Rust workspace for daemon
- [x] Implement `uinput` keyboard device creation with 6KRO
- [x] Implement `uinput` Xbox gamepad device creation (XInput-compatible, VID 0x045E, PID 0x028E)
- [x] Implement Wi-Fi transport server (TCP socket + TLS + mDNS advertisement)
- [x] Implement Bluetooth transport server (BlueZ RFCOMM with graceful fallback)
- [x] Implement USB transport (loopback TCP over ADB reverse/forward)
- [x] Wire transport events → uinput events in core daemon loop
- **Status:** complete

---

### Phase 4: Linux Daemon — UI / Status
- [x] Build Libadwaita main status window with live state listener
- [x] Display connected client identity and transport type (Wi-Fi / BT / USB)
- [x] Settings window (preferences for Wi-Fi port, BT UUID, ADB port, emulation device toggles)
- [x] Auto-start on login (systemd user service unit & XDG desktop entry)
- **Status:** complete

---

### Phase 5: Android App — Core
- [x] Scaffold Android Gradle project with Version Catalog (libs.versions.toml)
- [x] Implement binary wire protocol codec (Packet.kt) matching Linux daemon
- [x] Implement Wi-Fi transport client (SSLSocket TLS + mDNS NSD discovery)
- [x] Implement Bluetooth transport client (BluetoothSocket RFCOMM)
- [x] Implement USB transport client (ADB reverse tunnel)
- [x] Implement KeyboardEncoder (modifiers & HID usage tables)
- [x] Implement GamepadEncoder (Xbox 360 buttons, axes, triggers & deadzone scaling)
- **Status:** complete

---

### Phase 6: Android App — UI
- [x] Theme & Colors (Material 3 with Xbox-inspired green/dark styling)
- [x] Custom interactive components: VirtualJoystick, GamepadButton, Dpad, TriggerSlider
- [x] Home screen with connection status & quick transport selectors (Wi-Fi / BT / USB)
- [x] Keyboard screen (full interactive QWERTY layout + modifiers + F-keys + arrows)
- [x] Gamepad screen (dual sticks, ABXY diamond, D-pad, bumpers, triggers, center buttons)
- [x] Settings screen (deadzone calibration slider, connection setup guides)
- [x] Foreground Service (DroidPadService) keeping connection alive with ongoing notification & haptic rumble
- **Status:** complete

---

### Phase 7: Pairing & Security
- [x] TLS encryption over Wi-Fi with self-signed certificate generation on host
- [x] Bluetooth bonding integration with Android system settings
- [x] USB security isolated to local ADB bridge
- [x] Protocol Handshake verification with device name & version exchange
- **Status:** complete

---

### Phase 8: Packaging & Distribution
- [x] Systemd user service (`packaging/droidpad.service`)
- [x] Desktop entry (`packaging/droidpad.desktop`)
- [x] GitHub Actions CI workflow for Linux build, unit tests, and Android APK build
- [x] Comprehensive documentation: README.md, docs/PROTOCOL.md, docs/INSTALL.md
- **Status:** complete

---

### Phase 9: Testing & QA
- [x] Unit tests for packet encoding/decoding (Ping, Keyboard, Gamepad, Handshake)
- [x] Verified unit tests passing with `cargo test` (4/4 passed)
- [x] Clean compilation verified on Linux daemon
- **Status:** complete

---

## Decisions Made
| Decision | Rationale |
|----------|-----------|
| Rust for Linux daemon | Memory-safe, async (Tokio), kernel uinput & BlueZ support |
| Kotlin + Jetpack Compose for Android | Modern, declarative, reactive Material 3 UI |
| GTK4 + Libadwaita for Linux UI | Native Wayland/X11 support, matches GNOME HIG |
| TCP + TLS over Wi-Fi | Reliable ordered delivery; prevents packet injection |
| RFCOMM over Bluetooth | Universally supported across mobile devices and Linux BlueZ |
| uinput for keyboard & gamepad | Kernel-level emulation; recognized out-of-the-box by games/apps |
| USB via ADB reverse tunnel | No root or custom kernel modules needed on Android |
| Xbox 360 gamepad profile | `xpad` kernel module loaded by default on all major distros |

---

## Errors Encountered
| Error | Attempt | Resolution |
|-------|---------|------------|
| Missing `libadwaita::Switch` | 1 | Changed to `gtk4::Switch` |
| Missing `futures_util` dependency | 1 | Added `futures-util = "0.3"` to Cargo.toml |
| Packet enum vs struct mismatch in daemon | 1 | Updated `match pkt.payload` to access `Payload` variants |
| BlueZ RFCOMM `ProfileHandle.next()` trait bound | 1 | Imported `futures_util::StreamExt` |
| Subagents Claude quota exhausted | 1 | Resumed directly in primary workspace session |
