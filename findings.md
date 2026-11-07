# Findings & Decisions — DroidPad

## Requirements
<!-- Captured from user request -->
- **Target OS:** Linux-based distributions (Ubuntu, Fedora, Arch, etc.)
- **Target Device:** Android smartphone/tablet
- **Input modes:** Keyboard emulation + Xbox gamepad emulation
- **Transport layers:** Wi-Fi, Bluetooth, USB (all three must work)
- **UI requirement:** Interactive application UI on both Linux host and Android client
- **Connection direction:** Android → Linux (Android is the controller, Linux is the host)

---

## Research Findings

### Linux Input Emulation

#### `uinput` (kernel module)
- Located at `/dev/uinput`; requires `uinput` kernel module loaded (default on most distros)
- Allows userspace to create virtual input devices (keyboard, mouse, gamepad)
- Rust crates: [`evdev`](https://crates.io/crates/evdev) (read), [`uinput`](https://crates.io/crates/uinput)
- For Xbox gamepad: set `BUS_USB`, `ID_VENDOR=0x045e`, `ID_PRODUCT=0x028e` (Xbox 360 controller)
- Key Linux kernel HID IDs for Xbox 360: ABS axes (LX, LY, RX, RY, LT, RT), BTN_SOUTH/NORTH/EAST/WEST, BTN_TL/TR

#### USB HID Gadget (ConfigFS)
- Requires Linux with `g_hid` or `libcomposite` kernel module
- Device must expose a USB gadget interface (Raspberry Pi, some SBCs — not typical laptops)
- More complex; overkill for PC targets; better suited for embedded Linux hosts

#### `xpad` / `xone` kernel drivers
- `xpad`: ships with mainline kernel; supports Xbox 360 wired/wireless (via receiver)
- `xone`: out-of-tree; needed for Xbox One/Series controllers
- For emulation purposes, we mimic `xpad` evdev layout via `uinput`

---

### Android Input APIs

#### Keyboard / IME
- `InputMethodService` — full custom IME; can inject key events
- `AccessibilityService` — alternative if not using IME
- `KeyEvent` class covers all standard HID keycodes; mapping table needed for Linux scancodes

#### Gamepad / Touch
- `MotionEvent` for joystick axes and touch tracking
- Dual virtual joystick: left stick (move), right stick (look/aim)
- Trigger axes: `AXIS_LTRIGGER`, `AXIS_RTRIGGER` (0.0–1.0 float)
- Buttons: standard `KeyEvent.KEYCODE_BUTTON_*` constants

#### Vibration / Rumble
- `Vibrator` / `VibrationEffect` API (Android 8+) for haptic feedback when host sends rumble
- Rumble from host → Android: needs reverse channel in protocol

---

### Transport Layer Research

#### Wi-Fi
- Simple TCP socket; use `ServerSocket` on Android or Kotlin `ServerSocket` / `NioServerSocketChannel`
- **mDNS/NSD (Network Service Discovery):** `NsdManager` on Android to broadcast service; `avahi` on Linux to discover it
- TLS: JVM `SSLSocket` (Android) ↔ `rustls` or `native-tls` (Linux daemon)
- Typical LAN latency: 1–5 ms; suitable for < 20 ms target

#### Bluetooth
- **Android:** `BluetoothAdapter`, `BluetoothServerSocket` (RFCOMM with `listenUsingRfcommWithServiceRecord`)
- **Linux:** BlueZ `rfcomm` socket via `libbluetooth` or `btleplug` crate
- UUID for RFCOMM: define custom UUID (e.g., `com.droidpad.rfcomm`)
- Pairing: handled by OS; app triggers bond with `device.createBond()`
- Typical BT Classic latency: 10–40 ms; acceptable

#### USB
- **Option A — ADB port-forward (MVP):**
  - Run `adb forward tcp:PORT tcp:PORT`; Android listens, host connects via localhost
  - No special hardware; works on developer-unlocked devices
  - Latency: 1–3 ms (USB 2.0 bandwidth >> needed)
- **Option B — USB Accessory Mode (AOA):**
  - Android Open Accessory 2.0; Linux side uses `libusb`
  - Device appears as AOA accessory; no ADB needed
  - Better for end users; more complex setup
- **Option C — USB CDC ACM (Serial Gadget on Android — rooted):**
  - Requires root; too restrictive

**MVP recommendation:** ADB forward → future upgrade to AOA for non-dev users

---

### Protocol Design (Preliminary)

```
 ┌─────────────────────────────────────────┐
 │          DroidPad Wire Protocol          │
 │             (Binary, Little-Endian)      │
 └─────────────────────────────────────────┘

 Header (4 bytes):
   [0]   MAGIC  = 0xDP  (0x44 0x50)
   [2]   TYPE   = u8    (0x01=Keyboard, 0x02=Gamepad, 0x03=Ping, 0x04=Rumble, 0x05=Handshake)
   [3]   LENGTH = u8    (payload byte count, 0–255)

 Keyboard Payload (8 bytes):
   [0]   Modifier mask  (u8: Ctrl/Shift/Alt/Super bits)
   [1]   Reserved       (u8 = 0x00)
   [2-7] Keycodes[6]   (HID Usage IDs, up to 6 simultaneous keys)

 Gamepad Payload (12 bytes):
   [0-1] Buttons        (u16 bitmask: A,B,X,Y,LB,RB,Start,Select,L3,R3,Up,Down,Left,Right,Guide)
   [2-3] LeftX          (i16, -32768..32767)
   [4-5] LeftY          (i16)
   [6-7] RightX         (i16)
   [8-9] RightY         (i16)
   [10]  LeftTrigger    (u8, 0..255)
   [11]  RightTrigger   (u8, 0..255)

 Rumble Payload (2 bytes):
   [0]   Left motor     (u8, 0..255)
   [1]   Right motor    (u8, 0..255)

 Ping/Pong: LENGTH = 0 (echo reply)
```

---

### UI Framework Research

#### Linux Host UI
| Framework | Language | Pros | Cons |
|-----------|----------|------|------|
| GTK4 (libadwaita) | Rust/C | Native GNOME look; good a11y | Less native on KDE/XFCE |
| Qt6 (qml) | Rust/C++ | Cross-DE; professional look | Larger dep footprint |
| Tauri | Rust + HTML | Cross-platform; web tech UI | WebView overhead |
| **GTK4 / Libadwaita** | **Rust** | **Best GNOME/Wayland support** | — |

**Decision:** GTK4 + Libadwaita (Rust bindings via `gtk4-rs`) for Linux UI

#### Android UI
- **Jetpack Compose** (Kotlin): modern declarative; Material 3 design; best tooling
- Min SDK: **API 26** (Android 8.0) for BluetoothLE + modern APIs

---

### Project Structure (Proposed)

```
droidpad/
├── linux-daemon/              # Rust workspace
│   ├── Cargo.toml
│   ├── src/
│   │   ├── main.rs            # Entry point, arg parsing
│   │   ├── daemon.rs          # Core daemon loop
│   │   ├── uinput/
│   │   │   ├── keyboard.rs    # uinput keyboard device
│   │   │   └── gamepad.rs     # uinput Xbox360 gamepad device
│   │   ├── transport/
│   │   │   ├── mod.rs         # Transport trait
│   │   │   ├── wifi.rs        # TCP + TLS server
│   │   │   ├── bluetooth.rs   # BlueZ RFCOMM server
│   │   │   └── usb.rs         # ADB forward / AOA
│   │   ├── protocol/
│   │   │   ├── mod.rs
│   │   │   └── packet.rs      # Packet encode/decode
│   │   └── ui/
│   │       ├── tray.rs        # System tray (libappindicator)
│   │       └── settings.rs    # GTK4 settings window
│   └── packaging/
│       ├── droidpad.service   # systemd user service
│       ├── droidpad.desktop   # XDG autostart
│       └── build-deb.sh
│
├── android-app/               # Android Studio project
│   ├── app/
│   │   └── src/main/
│   │       ├── java/com/droidpad/
│   │       │   ├── MainActivity.kt
│   │       │   ├── service/
│   │       │   │   └── DroidPadService.kt   # Foreground service
│   │       │   ├── transport/
│   │       │   │   ├── TransportManager.kt
│   │       │   │   ├── WifiTransport.kt
│   │       │   │   ├── BluetoothTransport.kt
│   │       │   │   └── UsbTransport.kt
│   │       │   ├── hid/
│   │       │   │   ├── KeyboardEncoder.kt
│   │       │   │   └── GamepadEncoder.kt
│   │       │   └── ui/
│   │       │       ├── screens/
│   │       │       │   ├── HomeScreen.kt
│   │       │       │   ├── KeyboardScreen.kt
│   │       │       │   ├── GamepadScreen.kt
│   │       │       │   └── SettingsScreen.kt
│   │       │       └── components/
│   │       │           ├── VirtualJoystick.kt
│   │       │           ├── TriggerSlider.kt
│   │       │           └── DpadButton.kt
│   │       └── res/
│   └── build.gradle.kts
│
├── docs/
│   ├── PROTOCOL.md
│   ├── INSTALL.md
│   └── ARCHITECTURE.md
│
├── task_plan.md
├── findings.md
├── progress.md
└── README.md
```

---

## Technical Decisions
| Decision | Rationale |
|----------|-----------|
| Rust for Linux daemon | Async via Tokio; safe uinput/BlueZ bindings; small binary |
| GTK4 + libadwaita for Linux UI | Native Wayland/X11; GNOME-first; Rust bindings mature |
| Kotlin + Jetpack Compose for Android | Modern; Material 3; best IDE support |
| TCP + TLS for Wi-Fi | Reliable ordered; TLS prevents keylog sniffing |
| RFCOMM for Bluetooth | Universally supported; simpler than BLE GATT |
| ADB forward for USB MVP | No root/hardware constraints; upgradesable to AOA |
| Xbox 360 HID layout | xpad is in mainline kernel; most games support it |
| Binary protocol with fixed header | Low overhead; easy to decode in Rust/Kotlin |
| NSD/mDNS for host discovery | Zero-config; user doesn't need to enter IP address |
| Foreground Android service | Keeps transport alive when app is backgrounded |

---

## Issues Encountered
| Issue | Resolution |
|-------|------------|
| — | — |

---

## Resources
- Linux `uinput` docs: https://www.kernel.org/doc/html/latest/input/uinput.html
- Rust `evdev` crate: https://crates.io/crates/evdev
- Rust `gtk4` crate: https://crates.io/crates/gtk4
- BlueZ RFCOMM: https://www.bluez.org/
- Android `BluetoothSocket`: https://developer.android.com/reference/android/bluetooth/BluetoothSocket
- Android `NsdManager`: https://developer.android.com/reference/android/net/nsd/NsdManager
- Android Jetpack Compose: https://developer.android.com/jetpack/compose
- Xbox 360 controller USB IDs: VID=0x045e PID=0x028e
- ADB port forward docs: https://developer.android.com/tools/adb#forwardports

---

*Update this file after every 2 view/browser/search operations*
