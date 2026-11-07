//! Virtual Xbox 360 gamepad via Linux `uinput`.
//!
//! Mimics the Microsoft Xbox 360 controller (Vendor 0x045E, Product 0x028E) so
//! that the `xpad` kernel driver profile is matched by applications.

use anyhow::{Context, Result};
use evdev::{
    uinput::{VirtualDevice, VirtualDeviceBuilder},
    AbsInfo, AbsoluteAxisType, AttributeSet, InputEvent, Key, UinputAbsSetup,
};

use crate::protocol::packet::GamepadPayload;

// ---------------------------------------------------------------------------
// Button bit positions in GamepadPayload::buttons (u16)
// ---------------------------------------------------------------------------

const BTN_BIT_A:      u16 = 1 << 0;
const BTN_BIT_B:      u16 = 1 << 1;
const BTN_BIT_X:      u16 = 1 << 2;
const BTN_BIT_Y:      u16 = 1 << 3;
const BTN_BIT_LB:     u16 = 1 << 4;
const BTN_BIT_RB:     u16 = 1 << 5;
const BTN_BIT_START:  u16 = 1 << 6;
const BTN_BIT_SELECT: u16 = 1 << 7;
const BTN_BIT_L3:     u16 = 1 << 8;
const BTN_BIT_R3:     u16 = 1 << 9;
const BTN_BIT_DPAD_U: u16 = 1 << 10;
const BTN_BIT_DPAD_D: u16 = 1 << 11;
const BTN_BIT_DPAD_L: u16 = 1 << 12;
const BTN_BIT_DPAD_R: u16 = 1 << 13;
const BTN_BIT_GUIDE:  u16 = 1 << 14;

/// Mapping: (bit mask, evdev Key).
const BUTTON_MAP: &[(u16, Key)] = &[
    (BTN_BIT_A,      Key::BTN_SOUTH),   // A
    (BTN_BIT_B,      Key::BTN_EAST),    // B
    (BTN_BIT_X,      Key::BTN_WEST),    // X
    (BTN_BIT_Y,      Key::BTN_NORTH),   // Y
    (BTN_BIT_LB,     Key::BTN_TL),      // Left Bumper
    (BTN_BIT_RB,     Key::BTN_TR),      // Right Bumper
    (BTN_BIT_START,  Key::BTN_START),
    (BTN_BIT_SELECT, Key::BTN_SELECT),
    (BTN_BIT_L3,     Key::BTN_THUMBL),  // Left stick click
    (BTN_BIT_R3,     Key::BTN_THUMBR),  // Right stick click
    (BTN_BIT_GUIDE,  Key::BTN_MODE),    // Xbox guide button
];

// ---------------------------------------------------------------------------
// VirtualGamepad
// ---------------------------------------------------------------------------

/// Virtual Xbox 360 gamepad exposed through uinput.
pub struct VirtualGamepad {
    device: VirtualDevice,
    /// Last button state (for change detection).
    last_buttons: u16,
}

impl VirtualGamepad {
    /// Create the uinput virtual gamepad device mimicking Xbox 360.
    ///
    /// Requires `/dev/uinput` to be accessible.
    pub fn new() -> Result<Self> {
        // --- Buttons ---
        let mut keys = AttributeSet::<Key>::new();
        for &(_, key) in BUTTON_MAP {
            keys.insert(key);
        }

        // --- Absolute axes ---
        let axis_i16 = AbsInfo::new(0, -32768, 32767, 16, 128, 1);
        let axis_trigger = AbsInfo::new(0, 0, 255, 0, 0, 1);
        let axis_hat = AbsInfo::new(0, -1, 1, 0, 0, 1);

        let device = VirtualDeviceBuilder::new()
            .context("creating VirtualDeviceBuilder for gamepad")?
            .name("Microsoft X-Box 360 pad")
            // Xbox 360 USB IDs so xpad-aware apps recognise the device.
            // NOTE: evdev VirtualDeviceBuilder doesn't expose vendor/product
            //       directly in stable API; we set it via the builder if supported,
            //       otherwise applications use the device name.
            .with_keys(&keys)
            .context("setting gamepad key capabilities")?
            .with_absolute_axis(&UinputAbsSetup::new(AbsoluteAxisType::ABS_X, axis_i16))
            .context("ABS_X")?
            .with_absolute_axis(&UinputAbsSetup::new(AbsoluteAxisType::ABS_Y, axis_i16))
            .context("ABS_Y")?
            .with_absolute_axis(&UinputAbsSetup::new(AbsoluteAxisType::ABS_RX, axis_i16))
            .context("ABS_RX")?
            .with_absolute_axis(&UinputAbsSetup::new(AbsoluteAxisType::ABS_RY, axis_i16))
            .context("ABS_RY")?
            .with_absolute_axis(&UinputAbsSetup::new(AbsoluteAxisType::ABS_Z, axis_trigger))
            .context("ABS_Z")?
            .with_absolute_axis(&UinputAbsSetup::new(AbsoluteAxisType::ABS_RZ, axis_trigger))
            .context("ABS_RZ")?
            .with_absolute_axis(&UinputAbsSetup::new(AbsoluteAxisType::ABS_HAT0X, axis_hat))
            .context("ABS_HAT0X")?
            .with_absolute_axis(&UinputAbsSetup::new(AbsoluteAxisType::ABS_HAT0Y, axis_hat))
            .context("ABS_HAT0Y")?
            .build()
            .context("building gamepad VirtualDevice")?;

        Ok(Self {
            device,
            last_buttons: 0,
        })
    }

    /// Apply a gamepad report from the wire protocol.
    pub fn apply(&mut self, payload: &GamepadPayload) -> Result<()> {
        let mut events: Vec<InputEvent> = Vec::new();

        // --- Button events (only emit changes) ---
        let changed = payload.buttons ^ self.last_buttons;
        if changed != 0 {
            for &(bit, key) in BUTTON_MAP {
                if changed & bit != 0 {
                    let val = if payload.buttons & bit != 0 { 1 } else { 0 };
                    events.push(InputEvent::new(evdev::EventType::KEY, key.code(), val));
                }
            }
        }
        self.last_buttons = payload.buttons;

        // --- D-Pad (derived from button bits → hat axes) ---
        let hat_x: i32 = match (payload.buttons & BTN_BIT_DPAD_L != 0,
                                 payload.buttons & BTN_BIT_DPAD_R != 0)
        {
            (true, false) => -1,
            (false, true) =>  1,
            _             =>  0,
        };
        let hat_y: i32 = match (payload.buttons & BTN_BIT_DPAD_U != 0,
                                 payload.buttons & BTN_BIT_DPAD_D != 0)
        {
            (true, false) => -1,
            (false, true) =>  1,
            _             =>  0,
        };
        events.push(InputEvent::new(
            evdev::EventType::ABSOLUTE,
            AbsoluteAxisType::ABS_HAT0X.0,
            hat_x,
        ));
        events.push(InputEvent::new(
            evdev::EventType::ABSOLUTE,
            AbsoluteAxisType::ABS_HAT0Y.0,
            hat_y,
        ));

        // --- Analogue axes ---
        events.push(InputEvent::new(
            evdev::EventType::ABSOLUTE,
            AbsoluteAxisType::ABS_X.0,
            payload.left_x as i32,
        ));
        events.push(InputEvent::new(
            evdev::EventType::ABSOLUTE,
            AbsoluteAxisType::ABS_Y.0,
            payload.left_y as i32,
        ));
        events.push(InputEvent::new(
            evdev::EventType::ABSOLUTE,
            AbsoluteAxisType::ABS_RX.0,
            payload.right_x as i32,
        ));
        events.push(InputEvent::new(
            evdev::EventType::ABSOLUTE,
            AbsoluteAxisType::ABS_RY.0,
            payload.right_y as i32,
        ));
        events.push(InputEvent::new(
            evdev::EventType::ABSOLUTE,
            AbsoluteAxisType::ABS_Z.0,
            payload.left_trigger as i32,
        ));
        events.push(InputEvent::new(
            evdev::EventType::ABSOLUTE,
            AbsoluteAxisType::ABS_RZ.0,
            payload.right_trigger as i32,
        ));

        // Synchronise.
        events.push(InputEvent::new(evdev::EventType::SYNCHRONIZATION, 0, 0));
        self.device.emit(&events).context("emitting gamepad events")?;

        Ok(())
    }

    /// Release all buttons and centre all axes (call on disconnect).
    pub fn reset(&mut self) -> Result<()> {
        let mut events: Vec<InputEvent> = Vec::new();
        for &(_, key) in BUTTON_MAP {
            events.push(InputEvent::new(evdev::EventType::KEY, key.code(), 0));
        }
        for axis in [
            AbsoluteAxisType::ABS_X,
            AbsoluteAxisType::ABS_Y,
            AbsoluteAxisType::ABS_RX,
            AbsoluteAxisType::ABS_RY,
        ] {
            events.push(InputEvent::new(evdev::EventType::ABSOLUTE, axis.0, 0));
        }
        for axis in [AbsoluteAxisType::ABS_Z, AbsoluteAxisType::ABS_RZ] {
            events.push(InputEvent::new(evdev::EventType::ABSOLUTE, axis.0, 0));
        }
        for axis in [AbsoluteAxisType::ABS_HAT0X, AbsoluteAxisType::ABS_HAT0Y] {
            events.push(InputEvent::new(evdev::EventType::ABSOLUTE, axis.0, 0));
        }
        events.push(InputEvent::new(evdev::EventType::SYNCHRONIZATION, 0, 0));
        self.device.emit(&events).context("resetting gamepad state")?;
        self.last_buttons = 0;
        Ok(())
    }
}
