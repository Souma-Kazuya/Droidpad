# DroidPad Wire Protocol v1

Binary protocol, little-endian, over a reliable ordered stream (TCP/TLS, RFCOMM, or ADB TCP).

---

## Framing

Every message is a **header** (4 bytes) followed by a **payload** (0–255 bytes).

```
 0        1        2        3
 ┌────────┬────────┬────────┬────────┐
 │ 0x44   │ 0x50   │  TYPE  │ LENGTH │
 └────────┴────────┴────────┴────────┘
   magic[0] magic[1]  u8      u8
```

- **Magic:** Always `0x44 0x50` ("DP"). Drop connection on mismatch.
- **TYPE:** Packet type identifier (see below).
- **LENGTH:** Number of payload bytes that follow immediately after the header.

---

## Packet Types

| Value | Name | Direction | Payload size |
|-------|------|-----------|--------------|
| 0x01 | Keyboard | Android → Linux | 8 bytes |
| 0x02 | Gamepad | Android → Linux | 12 bytes |
| 0x03 | Ping | Both | 0 bytes |
| 0x04 | Rumble | Linux → Android | 2 bytes |
| 0x05 | Handshake | Both | variable (JSON) |

Ping receiver must reply with a Ping. If no ping reply within 5 s, close connection.

---

## Payload Formats

### 0x01 — Keyboard (8 bytes)

```
 Byte  Field       Type  Description
 ────  ──────────  ────  ──────────────────────────────────────────
  0    modifiers   u8    Modifier bitmask (see below)
  1    reserved    u8    Must be 0x00
  2    keycode[0]  u8    HID Usage ID (0x00 = empty slot)
  3    keycode[1]  u8
  4    keycode[2]  u8
  5    keycode[3]  u8
  6    keycode[4]  u8
  7    keycode[5]  u8    Up to 6 simultaneous keys (N-key rollover)
```

**Modifier bitmask:**
```
  bit 0  Left Ctrl
  bit 1  Left Shift
  bit 2  Left Alt
  bit 3  Left Meta (Super/Win)
  bit 4  Right Ctrl
  bit 5  Right Shift
  bit 6  Right Alt
  bit 7  Right Meta
```

**HID Usage IDs** (USB HID Usage Tables, page 0x07 — Keyboard):

| HID | Key    | HID | Key    | HID | Key    |
|-----|--------|-----|--------|-----|--------|
| 0x04 | A | 0x28 | Enter | 0x52 | ↑ |
| 0x05 | B | 0x29 | Escape | 0x51 | ↓ |
| … | … | 0x2A | Backspace | 0x50 | ← |
| 0x1D | Z | 0x2B | Tab | 0x4F | → |
| 0x1E | 1 | 0x2C | Space | 0x3A | F1 |
| … | … | … | … | … | … |
| 0x27 | 0 | | | 0x45 | F12 |

Full table: USB HID Usage Tables 1.3, Section 10, pp. 83–88.

---

### 0x02 — Gamepad (12 bytes)

```
 Bytes  Field          Type   Range          Description
 ─────  ─────────────  ─────  ─────────────  ─────────────────────
  0–1   buttons        u16 LE  bitmask        Button state
  2–3   left_x         i16 LE  -32768..32767  Left stick X axis
  4–5   left_y         i16 LE  -32768..32767  Left stick Y axis (up=negative)
  6–7   right_x        i16 LE  -32768..32767  Right stick X axis
  8–9   right_y        i16 LE  -32768..32767  Right stick Y axis
  10    left_trigger   u8      0..255         Left analog trigger
  11    right_trigger  u8      0..255         Right analog trigger
```

**Button bitmask:**
```
  bit  0   A (BTN_SOUTH)
  bit  1   B (BTN_EAST)
  bit  2   X (BTN_WEST)
  bit  3   Y (BTN_NORTH)
  bit  4   Left Bumper  (BTN_TL)
  bit  5   Right Bumper (BTN_TR)
  bit  6   Start        (BTN_START)
  bit  7   Select/Back  (BTN_SELECT)
  bit  8   Left Stick   (BTN_THUMBL / L3)
  bit  9   Right Stick  (BTN_THUMBR / R3)
  bit 10   D-pad Up
  bit 11   D-pad Down
  bit 12   D-pad Left
  bit 13   D-pad Right
  bit 14   Guide/Xbox   (BTN_MODE)
  bit 15   (reserved)
```

D-pad bits (10–13) are mapped by the daemon to `ABS_HAT0X` / `ABS_HAT0Y` on the uinput device.

---

### 0x03 — Ping (0 bytes)

No payload. The receiver must reply with a Ping packet within **5 seconds** or the connection is considered dead.

---

### 0x04 — Rumble (2 bytes)

Sent from Linux daemon → Android app to trigger haptic feedback.

```
 Byte  Field        Type  Range   Description
 ────  ───────────  ────  ──────  ───────────────────────────────
  0    left_motor   u8    0..255  Low-frequency (heavy) rumble
  1    right_motor  u8    0..255  High-frequency (light) rumble
```

Android maps `left_motor` → long vibration, `right_motor` → short buzz using `VibrationEffect`.

---

### 0x05 — Handshake (variable, JSON UTF-8)

Sent in both directions immediately after connection establishment.

```json
{
  "version": "1",
  "name": "Pixel 8 Pro"
}
```

- `version`: protocol version string; must be "1" for this spec.
- `name`: human-readable device name for display in the other side's UI.

If `version` doesn't match, the receiver closes the connection.

---

## Connection Lifecycle

```
Android                              Linux Daemon
   │                                      │
   │──── TCP/TLS connect ────────────────►│
   │◄─── Handshake (Linux name) ──────────│
   │──── Handshake (Android name) ───────►│
   │                                      │
   │  [connected - bidirectional stream]  │
   │                                      │
   │──── Keyboard / Gamepad packets ─────►│ (continuous)
   │◄─── Rumble packets ──────────────────│ (on gamepad use)
   │                                      │
   │──── Ping ───────────────────────────►│ (every 2 s)
   │◄─── Ping ────────────────────────────│ (echo reply)
   │                                      │
   │  [disconnect / timeout]              │
   │                                      │
```

---

## Security

- **Wi-Fi:** TLS 1.3. Self-signed certificate generated on first daemon start. Certificate fingerprint displayed in settings for manual verification. Android accepts after first-use trust (TOFU).
- **Bluetooth:** Relies on OS-level Bluetooth bonding/pairing.
- **USB:** ADB channel is trusted (requires physical access + USB debugging enabled). No additional encryption.
