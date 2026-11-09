//! Virtual keyboard via Linux `uinput`.
//!
//! Translates HID Usage IDs (USB HID Usage Tables, page 0x07 "Keyboard/Keypad")
//! into evdev `Key` values and injects events through `/dev/uinput`.

use anyhow::{Context, Result};
use evdev::{
    uinput::{VirtualDevice, VirtualDeviceBuilder},
    AttributeSet, InputEvent, Key,
};
use tracing::debug;

use crate::protocol::packet::KeyboardPayload;

// ---------------------------------------------------------------------------
// HID Usage → evdev Key lookup table
// ---------------------------------------------------------------------------

/// Map a HID keyboard usage ID (page 0x07) to an evdev `Key`.
///
/// Returns `None` for unrecognised / reserved codes.
pub fn hid_to_evdev(hid: u8) -> Option<Key> {
    // Reference: HID Usage Tables 1.3, section 10 "Keyboard/Keypad Page (0x07)"
    Some(match hid {
        // --- Letters A-Z (HID 0x04..0x1D) ---
        0x04 => Key::KEY_A,
        0x05 => Key::KEY_B,
        0x06 => Key::KEY_C,
        0x07 => Key::KEY_D,
        0x08 => Key::KEY_E,
        0x09 => Key::KEY_F,
        0x0A => Key::KEY_G,
        0x0B => Key::KEY_H,
        0x0C => Key::KEY_I,
        0x0D => Key::KEY_J,
        0x0E => Key::KEY_K,
        0x0F => Key::KEY_L,
        0x10 => Key::KEY_M,
        0x11 => Key::KEY_N,
        0x12 => Key::KEY_O,
        0x13 => Key::KEY_P,
        0x14 => Key::KEY_Q,
        0x15 => Key::KEY_R,
        0x16 => Key::KEY_S,
        0x17 => Key::KEY_T,
        0x18 => Key::KEY_U,
        0x19 => Key::KEY_V,
        0x1A => Key::KEY_W,
        0x1B => Key::KEY_X,
        0x1C => Key::KEY_Y,
        0x1D => Key::KEY_Z,

        // --- Digits 1-9, 0 (HID 0x1E..0x27) ---
        0x1E => Key::KEY_1,
        0x1F => Key::KEY_2,
        0x20 => Key::KEY_3,
        0x21 => Key::KEY_4,
        0x22 => Key::KEY_5,
        0x23 => Key::KEY_6,
        0x24 => Key::KEY_7,
        0x25 => Key::KEY_8,
        0x26 => Key::KEY_9,
        0x27 => Key::KEY_0,

        // --- Special keys ---
        0x28 => Key::KEY_ENTER,
        0x29 => Key::KEY_ESC,
        0x2A => Key::KEY_BACKSPACE,
        0x2B => Key::KEY_TAB,
        0x2C => Key::KEY_SPACE,
        0x2D => Key::KEY_MINUS,
        0x2E => Key::KEY_EQUAL,
        0x2F => Key::KEY_LEFTBRACE,
        0x30 => Key::KEY_RIGHTBRACE,
        0x31 => Key::KEY_BACKSLASH,
        0x32 => Key::KEY_BACKSLASH, // Non-US #/~
        0x33 => Key::KEY_SEMICOLON,
        0x34 => Key::KEY_APOSTROPHE,
        0x35 => Key::KEY_GRAVE,
        0x36 => Key::KEY_COMMA,
        0x37 => Key::KEY_DOT,
        0x38 => Key::KEY_SLASH,
        0x39 => Key::KEY_CAPSLOCK,

        // --- F1-F12 (HID 0x3A..0x45) ---
        0x3A => Key::KEY_F1,
        0x3B => Key::KEY_F2,
        0x3C => Key::KEY_F3,
        0x3D => Key::KEY_F4,
        0x3E => Key::KEY_F5,
        0x3F => Key::KEY_F6,
        0x40 => Key::KEY_F7,
        0x41 => Key::KEY_F8,
        0x42 => Key::KEY_F9,
        0x43 => Key::KEY_F10,
        0x44 => Key::KEY_F11,
        0x45 => Key::KEY_F12,

        // --- Navigation / editing cluster ---
        0x46 => Key::KEY_SYSRQ,
        0x47 => Key::KEY_SCROLLLOCK,
        0x48 => Key::KEY_PAUSE,
        0x49 => Key::KEY_INSERT,
        0x4A => Key::KEY_HOME,
        0x4B => Key::KEY_PAGEUP,
        0x4C => Key::KEY_DELETE,
        0x4D => Key::KEY_END,
        0x4E => Key::KEY_PAGEDOWN,
        0x4F => Key::KEY_RIGHT,
        0x50 => Key::KEY_LEFT,
        0x51 => Key::KEY_DOWN,
        0x52 => Key::KEY_UP,

        // --- Numpad ---
        0x53 => Key::KEY_NUMLOCK,
        0x54 => Key::KEY_KPSLASH,
        0x55 => Key::KEY_KPASTERISK,
        0x56 => Key::KEY_KPMINUS,
        0x57 => Key::KEY_KPPLUS,
        0x58 => Key::KEY_KPENTER,
        0x59 => Key::KEY_KP1,
        0x5A => Key::KEY_KP2,
        0x5B => Key::KEY_KP3,
        0x5C => Key::KEY_KP4,
        0x5D => Key::KEY_KP5,
        0x5E => Key::KEY_KP6,
        0x5F => Key::KEY_KP7,
        0x60 => Key::KEY_KP8,
        0x61 => Key::KEY_KP9,
        0x62 => Key::KEY_KP0,
        0x63 => Key::KEY_KPDOT,

        // --- F13-F24 ---
        0x68 => Key::KEY_F13,
        0x69 => Key::KEY_F14,
        0x6A => Key::KEY_F15,
        0x6B => Key::KEY_F16,
        0x6C => Key::KEY_F17,
        0x6D => Key::KEY_F18,
        0x6E => Key::KEY_F19,
        0x6F => Key::KEY_F20,
        0x70 => Key::KEY_F21,
        0x71 => Key::KEY_F22,
        0x72 => Key::KEY_F23,
        0x73 => Key::KEY_F24,

        // --- Modifiers (HID 0xE0..0xE7) ---
        0xE0 => Key::KEY_LEFTCTRL,
        0xE1 => Key::KEY_LEFTSHIFT,
        0xE2 => Key::KEY_LEFTALT,
        0xE3 => Key::KEY_LEFTMETA,
        0xE4 => Key::KEY_RIGHTCTRL,
        0xE5 => Key::KEY_RIGHTSHIFT,
        0xE6 => Key::KEY_RIGHTALT,
        0xE7 => Key::KEY_RIGHTMETA,

        // --- Multimedia / consumer (commonly mapped) ---
        0x7F => Key::KEY_MUTE,
        0x80 => Key::KEY_VOLUMEUP,
        0x81 => Key::KEY_VOLUMEDOWN,

        _ => return None,
    })
}

// ---------------------------------------------------------------------------
// Modifier mask → list of modifier Key values
// ---------------------------------------------------------------------------

/// Modifier bitmask positions → evdev keys.
const MODIFIER_KEYS: [(u8, Key); 8] = [
    (0x01, Key::KEY_LEFTCTRL),
    (0x02, Key::KEY_LEFTSHIFT),
    (0x04, Key::KEY_LEFTALT),
    (0x08, Key::KEY_LEFTMETA),
    (0x10, Key::KEY_RIGHTCTRL),
    (0x20, Key::KEY_RIGHTSHIFT),
    (0x40, Key::KEY_RIGHTALT),
    (0x80, Key::KEY_RIGHTMETA),
];

// ---------------------------------------------------------------------------
// VirtualKeyboard
// ---------------------------------------------------------------------------

/// A virtual keyboard device backed by Linux uinput.
pub struct VirtualKeyboard {
    device: VirtualDevice,
    /// Currently pressed evdev keys (tracked to synthesise release events).
    pressed: Vec<Key>,
}

impl VirtualKeyboard {
    /// Create the uinput virtual keyboard device.
    ///
    /// Requires `/dev/uinput` to be accessible (typically needs `input` group
    /// membership or `CAP_SYS_ADMIN`, or a udev rule).
    pub fn new() -> Result<Self> {
        // Build a full set of keys we want to report.
        let mut keys = AttributeSet::<Key>::new();

        // Letters
        for hid in 0x04u8..=0x1D {
            if let Some(k) = hid_to_evdev(hid) {
                keys.insert(k);
            }
        }
        // Digits + special + F-keys + nav + numpad + modifiers
        for hid in 0x1Eu8..=0xE7 {
            if let Some(k) = hid_to_evdev(hid) {
                keys.insert(k);
            }
        }

        let device = VirtualDeviceBuilder::new()
            .context("creating VirtualDeviceBuilder")?
            .name("DroidPad Virtual Keyboard")
            .with_keys(&keys)
            .context("setting key capabilities")?
            .build()
            .context("building VirtualDevice")?;

        Ok(Self {
            device,
            pressed: Vec::new(),
        })
    }

    /// Apply a keyboard report: release old keys, press new keys.
    pub fn apply(&mut self, payload: &KeyboardPayload) -> Result<()> {
        // Collect target keys from modifier mask + keycode array.
        let mut target_keys: Vec<Key> = Vec::new();

        for &(bit, key) in &MODIFIER_KEYS {
            if payload.modifiers & bit != 0 {
                target_keys.push(key);
            }
        }
        for &hid in &payload.keycodes {
            if hid == 0 {
                continue;
            }
            if let Some(k) = hid_to_evdev(hid) {
                target_keys.push(k);
            } else {
                debug!("Unrecognised HID keycode: 0x{hid:02X}");
            }
        }

        // Release keys that were pressed but are no longer in the new report.
        let mut events: Vec<InputEvent> = Vec::new();
        let mut new_pressed: Vec<Key> = Vec::new();

        for &k in &self.pressed {
            if !target_keys.contains(&k) {
                events.push(InputEvent::new(evdev::EventType::KEY, k.code(), 0));
            } else {
                new_pressed.push(k);
            }
        }

        // Press keys that are in the new report but were not previously pressed.
        for &k in &target_keys {
            if !self.pressed.contains(&k) {
                events.push(InputEvent::new(evdev::EventType::KEY, k.code(), 1));
                new_pressed.push(k);
            }
        }

        if !events.is_empty() {
            // Synchronise.
            events.push(InputEvent::new(evdev::EventType::SYNCHRONIZATION, 0, 0));
            self.device
                .emit(&events)
                .context("emitting keyboard events")?;
        }

        self.pressed = new_pressed;
        Ok(())
    }

    /// Release all currently-pressed keys (call on disconnect).
    pub fn release_all(&mut self) -> Result<()> {
        if self.pressed.is_empty() {
            return Ok(());
        }
        let mut events: Vec<InputEvent> = self
            .pressed
            .drain(..)
            .map(|k| InputEvent::new(evdev::EventType::KEY, k.code(), 0))
            .collect();
        events.push(InputEvent::new(evdev::EventType::SYNCHRONIZATION, 0, 0));
        self.device
            .emit(&events)
            .context("releasing all keyboard keys")?;
        Ok(())
    }
}
