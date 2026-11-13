//! Packet type definitions and async read/write helpers.
//!
//! All multi-byte integers in payloads are **little-endian** as per the wire spec.

use anyhow::{bail, Context, Result};
use serde::{Deserialize, Serialize};
use tokio::io::{AsyncRead, AsyncReadExt, AsyncWrite, AsyncWriteExt};

// ---------------------------------------------------------------------------
// Magic bytes
// ---------------------------------------------------------------------------

pub const MAGIC_0: u8 = 0x44; // 'D'
pub const MAGIC_1: u8 = 0x50; // 'P'

// ---------------------------------------------------------------------------
// Packet type discriminant
// ---------------------------------------------------------------------------

/// Discriminant for the packet TYPE byte (header[2]).
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum PacketType {
    /// Keyboard HID report.
    Keyboard = 0x01,
    /// Gamepad (Xbox 360 layout) report.
    Gamepad = 0x02,
    /// Keep-alive ping — no payload.
    Ping = 0x03,
    /// Rumble feedback sent from host → device.
    Rumble = 0x04,
    /// Initial handshake JSON.
    Handshake = 0x05,
    /// Active game context JSON (host → device) for on-device AI assistance.
    GameContext = 0x06,
}

impl TryFrom<u8> for PacketType {
    type Error = anyhow::Error;

    fn try_from(v: u8) -> Result<Self> {
        match v {
            0x01 => Ok(Self::Keyboard),
            0x02 => Ok(Self::Gamepad),
            0x03 => Ok(Self::Ping),
            0x04 => Ok(Self::Rumble),
            0x05 => Ok(Self::Handshake),
            0x06 => Ok(Self::GameContext),
            other => bail!("Unknown packet type byte: 0x{other:02X}"),
        }
    }
}

impl From<PacketType> for u8 {
    fn from(pt: PacketType) -> u8 {
        pt as u8
    }
}

// ---------------------------------------------------------------------------
// Header
// ---------------------------------------------------------------------------

/// Four-byte packet header.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct Header {
    /// Packet type discriminant.
    pub pkt_type: PacketType,
    /// Number of payload bytes that follow the header.
    pub length: u8,
}

impl Header {
    /// Expected payload length for each packet type (None = variable).
    pub fn expected_payload_len(pkt_type: PacketType) -> Option<u8> {
        match pkt_type {
            PacketType::Keyboard => Some(8),
            PacketType::Gamepad => Some(12),
            PacketType::Ping => Some(0),
            PacketType::Rumble => Some(2),
            PacketType::Handshake => None, // variable
            PacketType::GameContext => None, // variable
        }
    }
}

// ---------------------------------------------------------------------------
// Payload structs
// ---------------------------------------------------------------------------

/// Keyboard HID report (8 bytes).
///
/// `modifiers` bit layout:
/// - bit 0: LCtrl  bit 1: LShift  bit 2: LAlt   bit 3: LMeta
/// - bit 4: RCtrl  bit 5: RShift  bit 6: RAlt   bit 7: RMeta
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct KeyboardPayload {
    pub modifiers: u8,
    /// Up to 6 simultaneous HID Usage IDs (0x00 = empty slot).
    pub keycodes: [u8; 6],
}

/// Gamepad report (12 bytes), Xbox 360 layout.
///
/// Button bit layout (`buttons` u16 LE):
/// - bit 0: A       bit 1: B       bit 2: X       bit 3: Y
/// - bit 4: LB      bit 5: RB      bit 6: Start   bit 7: Select
/// - bit 8: L3      bit 9: R3      bit 10: D-Up   bit 11: D-Down
/// - bit 12: D-Left  bit 13: D-Right  bit 14: Guide
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct GamepadPayload {
    /// Button bitfield (see above).
    pub buttons: u16,
    pub left_x: i16,
    pub left_y: i16,
    pub right_x: i16,
    pub right_y: i16,
    /// Left trigger 0–255.
    pub left_trigger: u8,
    /// Right trigger 0–255.
    pub right_trigger: u8,
}

/// Rumble feedback (2 bytes).
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct RumblePayload {
    /// Left (low-frequency) motor, 0–255.
    pub left: u8,
    /// Right (high-frequency) motor, 0–255.
    pub right: u8,
}

/// Initial handshake JSON payload.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub struct HandshakePayload {
    pub version: String,
    pub name: String,
}

/// Active game context JSON payload (host → device) for AI assistant.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub struct GameContextPayload {
    pub game_title: String,
    pub genre: String,
    pub active_process: String,
}

// ---------------------------------------------------------------------------
// Payload enum + top-level Packet
// ---------------------------------------------------------------------------

/// Discriminated union of all payload types.
#[derive(Debug, Clone, PartialEq, Eq)]
pub enum Payload {
    Keyboard(KeyboardPayload),
    Gamepad(GamepadPayload),
    Ping,
    Rumble(RumblePayload),
    Handshake(HandshakePayload),
    GameContext(GameContextPayload),
}

/// A complete DroidPad wire packet.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct Packet {
    pub header: Header,
    pub payload: Payload,
}

// ---------------------------------------------------------------------------
// Async read
// ---------------------------------------------------------------------------

impl Packet {
    /// Read one packet from an async byte stream.
    ///
    /// Validates magic bytes, parses the header, reads the declared payload
    /// length, then deserialises the payload.
    pub async fn read_async<R>(reader: &mut R) -> Result<Packet>
    where
        R: AsyncRead + Unpin,
    {
        // --- Read 4-byte header ---
        let mut hdr = [0u8; 4];
        reader
            .read_exact(&mut hdr)
            .await
            .context("reading packet header")?;

        // Validate magic.
        if hdr[0] != MAGIC_0 || hdr[1] != MAGIC_1 {
            bail!(
                "Bad magic bytes: 0x{:02X} 0x{:02X} (expected 0x44 0x50)",
                hdr[0],
                hdr[1]
            );
        }

        let pkt_type = PacketType::try_from(hdr[2]).context("parsing packet type")?;
        let length = hdr[3];

        // Validate declared length against expected length where applicable.
        if let Some(expected) = Header::expected_payload_len(pkt_type) {
            if length != expected {
                bail!(
                    "Packet type {:?} expects {} payload bytes, got {}",
                    pkt_type,
                    expected,
                    length
                );
            }
        }

        // --- Read payload bytes ---
        let mut buf = vec![0u8; length as usize];
        if length > 0 {
            reader
                .read_exact(&mut buf)
                .await
                .context("reading packet payload")?;
        }

        // --- Parse payload ---
        let payload = Self::parse_payload(pkt_type, &buf)?;

        Ok(Packet {
            header: Header { pkt_type, length },
            payload,
        })
    }

    fn parse_payload(pkt_type: PacketType, buf: &[u8]) -> Result<Payload> {
        match pkt_type {
            PacketType::Ping => Ok(Payload::Ping),

            PacketType::Keyboard => {
                if buf.len() < 8 {
                    bail!("Keyboard payload too short: {} bytes", buf.len());
                }
                let modifiers = buf[0];
                // buf[1] is reserved
                let mut keycodes = [0u8; 6];
                keycodes.copy_from_slice(&buf[2..8]);
                Ok(Payload::Keyboard(KeyboardPayload {
                    modifiers,
                    keycodes,
                }))
            }

            PacketType::Gamepad => {
                if buf.len() < 12 {
                    bail!("Gamepad payload too short: {} bytes", buf.len());
                }
                let buttons = u16::from_le_bytes([buf[0], buf[1]]);
                let left_x = i16::from_le_bytes([buf[2], buf[3]]);
                let left_y = i16::from_le_bytes([buf[4], buf[5]]);
                let right_x = i16::from_le_bytes([buf[6], buf[7]]);
                let right_y = i16::from_le_bytes([buf[8], buf[9]]);
                let left_trigger = buf[10];
                let right_trigger = buf[11];
                Ok(Payload::Gamepad(GamepadPayload {
                    buttons,
                    left_x,
                    left_y,
                    right_x,
                    right_y,
                    left_trigger,
                    right_trigger,
                }))
            }

            PacketType::Rumble => {
                if buf.len() < 2 {
                    bail!("Rumble payload too short: {} bytes", buf.len());
                }
                Ok(Payload::Rumble(RumblePayload {
                    left: buf[0],
                    right: buf[1],
                }))
            }

            PacketType::Handshake => {
                let json = std::str::from_utf8(buf).context("Handshake payload is not UTF-8")?;
                let hs: HandshakePayload =
                    serde_json::from_str(json).context("Handshake JSON parse error")?;
                Ok(Payload::Handshake(hs))
            }

            PacketType::GameContext => {
                let json = std::str::from_utf8(buf).context("GameContext payload is not UTF-8")?;
                let gc: GameContextPayload =
                    serde_json::from_str(json).context("GameContext JSON parse error")?;
                Ok(Payload::GameContext(gc))
            }
        }
    }

    // ---------------------------------------------------------------------------
    // Async write
    // ---------------------------------------------------------------------------

    /// Serialise and write this packet to an async byte sink.
    pub async fn write_async<W>(&self, writer: &mut W) -> Result<()>
    where
        W: AsyncWrite + Unpin,
    {
        let payload_bytes = self.serialise_payload()?;
        let length = payload_bytes.len() as u8;

        // Write header.
        let hdr = [MAGIC_0, MAGIC_1, u8::from(self.header.pkt_type), length];
        writer.write_all(&hdr).await.context("writing header")?;

        // Write payload.
        if !payload_bytes.is_empty() {
            writer
                .write_all(&payload_bytes)
                .await
                .context("writing payload")?;
        }

        writer.flush().await.context("flushing writer")?;
        Ok(())
    }

    fn serialise_payload(&self) -> Result<Vec<u8>> {
        match &self.payload {
            Payload::Ping => Ok(vec![]),

            Payload::Keyboard(k) => {
                let mut buf = vec![0u8; 8];
                buf[0] = k.modifiers;
                buf[1] = 0x00; // reserved
                buf[2..8].copy_from_slice(&k.keycodes);
                Ok(buf)
            }

            Payload::Gamepad(g) => {
                let mut buf = vec![0u8; 12];
                buf[0..2].copy_from_slice(&g.buttons.to_le_bytes());
                buf[2..4].copy_from_slice(&g.left_x.to_le_bytes());
                buf[4..6].copy_from_slice(&g.left_y.to_le_bytes());
                buf[6..8].copy_from_slice(&g.right_x.to_le_bytes());
                buf[8..10].copy_from_slice(&g.right_y.to_le_bytes());
                buf[10] = g.left_trigger;
                buf[11] = g.right_trigger;
                Ok(buf)
            }

            Payload::Rumble(r) => Ok(vec![r.left, r.right]),

            Payload::Handshake(hs) => {
                let json = serde_json::to_string(hs).context("serialising handshake JSON")?;
                Ok(json.into_bytes())
            }

            Payload::GameContext(gc) => {
                let json = serde_json::to_string(gc).context("serialising game context JSON")?;
                Ok(json.into_bytes())
            }
        }
    }

    // ---------------------------------------------------------------------------
    // Convenience constructors
    // ---------------------------------------------------------------------------

    /// Create a GameContext packet (host → device) for on-device AI tips.
    pub fn game_context(
        game_title: impl Into<String>,
        genre: impl Into<String>,
        active_process: impl Into<String>,
    ) -> Self {
        let gc = GameContextPayload {
            game_title: game_title.into(),
            genre: genre.into(),
            active_process: active_process.into(),
        };
        let length = serde_json::to_string(&gc).map(|s| s.len() as u8).unwrap_or(0);
        Packet {
            header: Header {
                pkt_type: PacketType::GameContext,
                length,
            },
            payload: Payload::GameContext(gc),
        }
    }

    /// Create a Rumble packet (host → device).
    pub fn rumble(left: u8, right: u8) -> Self {
        Packet {
            header: Header {
                pkt_type: PacketType::Rumble,
                length: 2,
            },
            payload: Payload::Rumble(RumblePayload { left, right }),
        }
    }

    /// Create a Ping packet.
    pub fn ping() -> Self {
        Packet {
            header: Header {
                pkt_type: PacketType::Ping,
                length: 0,
            },
            payload: Payload::Ping,
        }
    }

    /// Create a Handshake packet.
    pub fn handshake(name: String) -> Self {
        Packet {
            header: Header {
                pkt_type: PacketType::Handshake,
                length: 0,
            },
            payload: Payload::Handshake(HandshakePayload {
                version: "1".into(),
                name,
            }),
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use std::io::Cursor;

    #[tokio::test]
    async fn test_packet_ping_roundtrip() {
        let pkt = Packet::ping();
        let mut buf = Vec::new();
        pkt.write_async(&mut buf).await.unwrap();

        let mut cursor = Cursor::new(buf);
        let parsed = Packet::read_async(&mut cursor).await.unwrap();
        assert_eq!(parsed.header.pkt_type, PacketType::Ping);
    }

    #[tokio::test]
    async fn test_packet_keyboard_roundtrip() {
        let pkt = Packet {
            header: Header {
                pkt_type: PacketType::Keyboard,
                length: 8,
            },
            payload: Payload::Keyboard(KeyboardPayload {
                modifiers: 0x05,                                // LCtrl + LAlt
                keycodes: [0x04, 0x05, 0x06, 0x00, 0x00, 0x00], // A, B, C
            }),
        };
        let mut buf = Vec::new();
        pkt.write_async(&mut buf).await.unwrap();

        let mut cursor = Cursor::new(buf);
        let parsed = Packet::read_async(&mut cursor).await.unwrap();
        assert_eq!(parsed.header.pkt_type, PacketType::Keyboard);
        if let Payload::Keyboard(k) = parsed.payload {
            assert_eq!(k.modifiers, 0x05);
            assert_eq!(k.keycodes, [0x04, 0x05, 0x06, 0x00, 0x00, 0x00]);
        } else {
            panic!("Wrong payload variant");
        }
    }

    #[tokio::test]
    async fn test_packet_gamepad_roundtrip() {
        let pkt = Packet {
            header: Header {
                pkt_type: PacketType::Gamepad,
                length: 12,
            },
            payload: Payload::Gamepad(GamepadPayload {
                buttons: 0x0001 | 0x0400, // A + DPAD_UP
                left_x: -12345,
                left_y: 20000,
                right_x: 0,
                right_y: -30000,
                left_trigger: 250,
                right_trigger: 128,
            }),
        };
        let mut buf = Vec::new();
        pkt.write_async(&mut buf).await.unwrap();

        let mut cursor = Cursor::new(buf);
        let parsed = Packet::read_async(&mut cursor).await.unwrap();
        assert_eq!(parsed.header.pkt_type, PacketType::Gamepad);
        if let Payload::Gamepad(g) = parsed.payload {
            assert_eq!(g.buttons, 0x0001 | 0x0400);
            assert_eq!(g.left_x, -12345);
            assert_eq!(g.left_y, 20000);
            assert_eq!(g.left_trigger, 250);
            assert_eq!(g.right_trigger, 128);
        } else {
            panic!("Wrong payload variant");
        }
    }

    #[tokio::test]
    async fn test_packet_handshake_roundtrip() {
        let pkt = Packet::handshake("Pixel 8".into());
        let mut buf = Vec::new();
        pkt.write_async(&mut buf).await.unwrap();

        let mut cursor = Cursor::new(buf);
        let parsed = Packet::read_async(&mut cursor).await.unwrap();
        assert_eq!(parsed.header.pkt_type, PacketType::Handshake);
        if let Payload::Handshake(hs) = parsed.payload {
            assert_eq!(hs.version, "1");
            assert_eq!(hs.name, "Pixel 8");
        } else {
            panic!("Wrong payload variant");
        }
    }

    // -----------------------------------------------------------------------
    // PacketType conversion tests
    // -----------------------------------------------------------------------

    #[test]
    fn test_packet_type_try_from_valid() {
        assert_eq!(PacketType::try_from(0x01).unwrap(), PacketType::Keyboard);
        assert_eq!(PacketType::try_from(0x02).unwrap(), PacketType::Gamepad);
        assert_eq!(PacketType::try_from(0x03).unwrap(), PacketType::Ping);
        assert_eq!(PacketType::try_from(0x04).unwrap(), PacketType::Rumble);
        assert_eq!(PacketType::try_from(0x05).unwrap(), PacketType::Handshake);
        assert_eq!(PacketType::try_from(0x06).unwrap(), PacketType::GameContext);
    }

    #[test]
    fn test_packet_type_try_from_invalid() {
        assert!(PacketType::try_from(0x00).is_err());
        assert!(PacketType::try_from(0x07).is_err());
        assert!(PacketType::try_from(0xFF).is_err());
    }

    #[test]
    fn test_packet_type_into_u8() {
        assert_eq!(u8::from(PacketType::Keyboard), 0x01);
        assert_eq!(u8::from(PacketType::Gamepad), 0x02);
        assert_eq!(u8::from(PacketType::Ping), 0x03);
        assert_eq!(u8::from(PacketType::Rumble), 0x04);
        assert_eq!(u8::from(PacketType::Handshake), 0x05);
    }

    // -----------------------------------------------------------------------
    // Header expected_payload_len tests
    // -----------------------------------------------------------------------

    #[test]
    fn test_expected_payload_len() {
        assert_eq!(Header::expected_payload_len(PacketType::Keyboard), Some(8));
        assert_eq!(Header::expected_payload_len(PacketType::Gamepad), Some(12));
        assert_eq!(Header::expected_payload_len(PacketType::Ping), Some(0));
        assert_eq!(Header::expected_payload_len(PacketType::Rumble), Some(2));
        assert_eq!(Header::expected_payload_len(PacketType::Handshake), None);
    }

    // -----------------------------------------------------------------------
    // Rumble roundtrip
    // -----------------------------------------------------------------------

    #[tokio::test]
    async fn test_packet_rumble_roundtrip() {
        let pkt = Packet::rumble(128, 64);
        let mut buf = Vec::new();
        pkt.write_async(&mut buf).await.unwrap();

        let mut cursor = Cursor::new(buf);
        let parsed = Packet::read_async(&mut cursor).await.unwrap();
        assert_eq!(parsed.header.pkt_type, PacketType::Rumble);
        if let Payload::Rumble(r) = parsed.payload {
            assert_eq!(r.left, 128);
            assert_eq!(r.right, 64);
        } else {
            panic!("Wrong payload variant");
        }
    }

    // -----------------------------------------------------------------------
    // Wire format validation
    // -----------------------------------------------------------------------

    #[tokio::test]
    async fn test_ping_wire_format() {
        let pkt = Packet::ping();
        let mut buf = Vec::new();
        pkt.write_async(&mut buf).await.unwrap();
        // Header: [0x44, 0x50, 0x03, 0x00] — no payload
        assert_eq!(buf, vec![0x44, 0x50, 0x03, 0x00]);
    }

    #[tokio::test]
    async fn test_rumble_wire_format() {
        let pkt = Packet::rumble(0xFF, 0x00);
        let mut buf = Vec::new();
        pkt.write_async(&mut buf).await.unwrap();
        // Header + 2-byte payload
        assert_eq!(buf, vec![0x44, 0x50, 0x04, 0x02, 0xFF, 0x00]);
    }

    // -----------------------------------------------------------------------
    // Error path tests
    // -----------------------------------------------------------------------

    #[tokio::test]
    async fn test_bad_magic_bytes() {
        let data = vec![0x00, 0x00, 0x03, 0x00]; // wrong magic
        let mut cursor = Cursor::new(data);
        let result = Packet::read_async(&mut cursor).await;
        assert!(result.is_err());
        let err = result.unwrap_err().to_string();
        assert!(err.contains("Bad magic bytes"), "Unexpected error: {err}");
    }

    #[tokio::test]
    async fn test_unknown_packet_type() {
        let data = vec![0x44, 0x50, 0xFF, 0x00]; // unknown type 0xFF
        let mut cursor = Cursor::new(data);
        let result = Packet::read_async(&mut cursor).await;
        assert!(result.is_err());
        let err = format!("{:#}", result.unwrap_err());
        assert!(
            err.contains("Unknown packet type"),
            "Unexpected error: {err}"
        );
    }

    #[tokio::test]
    async fn test_wrong_payload_length_for_keyboard() {
        // Keyboard expects 8 bytes, declare 4
        let data = vec![0x44, 0x50, 0x01, 0x04];
        let mut cursor = Cursor::new(data);
        let result = Packet::read_async(&mut cursor).await;
        assert!(result.is_err());
        let err = format!("{:#}", result.unwrap_err());
        assert!(
            err.contains("expects 8 payload bytes"),
            "Unexpected error: {err}"
        );
    }

    #[tokio::test]
    async fn test_wrong_payload_length_for_gamepad() {
        // Gamepad expects 12 bytes, declare 6
        let data = vec![0x44, 0x50, 0x02, 0x06];
        let mut cursor = Cursor::new(data);
        let result = Packet::read_async(&mut cursor).await;
        assert!(result.is_err());
    }

    #[tokio::test]
    async fn test_wrong_payload_length_for_rumble() {
        // Rumble expects 2 bytes, declare 0
        let data = vec![0x44, 0x50, 0x04, 0x00];
        let mut cursor = Cursor::new(data);
        let result = Packet::read_async(&mut cursor).await;
        assert!(result.is_err());
    }

    #[tokio::test]
    async fn test_truncated_header() {
        // Only 2 bytes — incomplete header
        let data = vec![0x44, 0x50];
        let mut cursor = Cursor::new(data);
        let result = Packet::read_async(&mut cursor).await;
        assert!(result.is_err());
    }

    #[tokio::test]
    async fn test_truncated_payload() {
        // Valid keyboard header (expects 8 bytes) but only 4 payload bytes
        let data = vec![0x44, 0x50, 0x01, 0x08, 0x00, 0x00, 0x04, 0x05];
        let mut cursor = Cursor::new(data);
        let result = Packet::read_async(&mut cursor).await;
        assert!(result.is_err());
    }

    #[tokio::test]
    async fn test_empty_stream() {
        let data: Vec<u8> = vec![];
        let mut cursor = Cursor::new(data);
        let result = Packet::read_async(&mut cursor).await;
        assert!(result.is_err());
    }

    #[tokio::test]
    async fn test_invalid_handshake_json() {
        // Valid header for handshake, but payload is not valid JSON
        let garbage = b"not json at all";
        let mut data = vec![0x44, 0x50, 0x05, garbage.len() as u8];
        data.extend_from_slice(garbage);
        let mut cursor = Cursor::new(data);
        let result = Packet::read_async(&mut cursor).await;
        assert!(result.is_err());
        let err = format!("{:#}", result.unwrap_err());
        assert!(err.contains("JSON"), "Unexpected error: {err}");
    }

    #[tokio::test]
    async fn test_handshake_non_utf8() {
        // Valid header for handshake, but payload is invalid UTF-8
        let bad_utf8: Vec<u8> = vec![0xFF, 0xFE, 0xFD];
        let mut data = vec![0x44, 0x50, 0x05, bad_utf8.len() as u8];
        data.extend_from_slice(&bad_utf8);
        let mut cursor = Cursor::new(data);
        let result = Packet::read_async(&mut cursor).await;
        assert!(result.is_err());
        let err = format!("{:#}", result.unwrap_err());
        assert!(err.contains("UTF-8"), "Unexpected error: {err}");
    }

    // -----------------------------------------------------------------------
    // Gamepad extreme values
    // -----------------------------------------------------------------------

    #[tokio::test]
    async fn test_gamepad_extreme_values() {
        let pkt = Packet {
            header: Header {
                pkt_type: PacketType::Gamepad,
                length: 12,
            },
            payload: Payload::Gamepad(GamepadPayload {
                buttons: 0x7FFF, // all 15 buttons pressed
                left_x: i16::MIN,
                left_y: i16::MAX,
                right_x: i16::MIN,
                right_y: i16::MAX,
                left_trigger: 255,
                right_trigger: 0,
            }),
        };
        let mut buf = Vec::new();
        pkt.write_async(&mut buf).await.unwrap();

        let mut cursor = Cursor::new(buf);
        let parsed = Packet::read_async(&mut cursor).await.unwrap();
        if let Payload::Gamepad(g) = parsed.payload {
            assert_eq!(g.buttons, 0x7FFF);
            assert_eq!(g.left_x, i16::MIN);
            assert_eq!(g.left_y, i16::MAX);
            assert_eq!(g.right_x, i16::MIN);
            assert_eq!(g.right_y, i16::MAX);
            assert_eq!(g.left_trigger, 255);
            assert_eq!(g.right_trigger, 0);
        } else {
            panic!("Wrong payload variant");
        }
    }

    // -----------------------------------------------------------------------
    // Keyboard all modifiers + full keyroll
    // -----------------------------------------------------------------------

    #[tokio::test]
    async fn test_keyboard_all_modifiers() {
        let pkt = Packet {
            header: Header {
                pkt_type: PacketType::Keyboard,
                length: 8,
            },
            payload: Payload::Keyboard(KeyboardPayload {
                modifiers: 0xFF,                                // all 8 modifier bits set
                keycodes: [0x04, 0x05, 0x06, 0x07, 0x08, 0x09], // A-F
            }),
        };
        let mut buf = Vec::new();
        pkt.write_async(&mut buf).await.unwrap();

        let mut cursor = Cursor::new(buf);
        let parsed = Packet::read_async(&mut cursor).await.unwrap();
        if let Payload::Keyboard(k) = parsed.payload {
            assert_eq!(k.modifiers, 0xFF);
            assert_eq!(k.keycodes, [0x04, 0x05, 0x06, 0x07, 0x08, 0x09]);
        } else {
            panic!("Wrong payload variant");
        }
    }

    // -----------------------------------------------------------------------
    // Multi-packet sequential read
    // -----------------------------------------------------------------------

    #[tokio::test]
    async fn test_sequential_packet_read() {
        let mut buf = Vec::new();
        Packet::ping().write_async(&mut buf).await.unwrap();
        Packet::rumble(10, 20).write_async(&mut buf).await.unwrap();
        Packet::handshake("Test".into())
            .write_async(&mut buf)
            .await
            .unwrap();

        let mut cursor = Cursor::new(buf);
        let p1 = Packet::read_async(&mut cursor).await.unwrap();
        assert_eq!(p1.header.pkt_type, PacketType::Ping);

        let p2 = Packet::read_async(&mut cursor).await.unwrap();
        assert_eq!(p2.header.pkt_type, PacketType::Rumble);

        let p3 = Packet::read_async(&mut cursor).await.unwrap();
        assert_eq!(p3.header.pkt_type, PacketType::Handshake);

        // No more data — next read should fail
        let p4 = Packet::read_async(&mut cursor).await;
        assert!(p4.is_err());
    }

    // -----------------------------------------------------------------------
    // Handshake with unicode device name
    // -----------------------------------------------------------------------

    #[tokio::test]
    async fn test_handshake_unicode_name() {
        let pkt = Packet::handshake("小米14 Ultra".into());
        let mut buf = Vec::new();
        pkt.write_async(&mut buf).await.unwrap();

        let mut cursor = Cursor::new(buf);
        let parsed = Packet::read_async(&mut cursor).await.unwrap();
        if let Payload::Handshake(hs) = parsed.payload {
            assert_eq!(hs.name, "小米14 Ultra");
        } else {
            panic!("Wrong payload variant");
        }
    }

    #[tokio::test]
    async fn test_packet_game_context_roundtrip() {
        let pkt = Packet::game_context("Elden Ring", "soulslike", "eldenring.exe");
        let mut buf = Vec::new();
        pkt.write_async(&mut buf).await.unwrap();

        let mut cursor = Cursor::new(buf);
        let parsed = Packet::read_async(&mut cursor).await.unwrap();
        assert_eq!(parsed.header.pkt_type, PacketType::GameContext);
        if let Payload::GameContext(gc) = parsed.payload {
            assert_eq!(gc.game_title, "Elden Ring");
            assert_eq!(gc.genre, "soulslike");
            assert_eq!(gc.active_process, "eldenring.exe");
        } else {
            panic!("Wrong payload variant");
        }
    }
}
