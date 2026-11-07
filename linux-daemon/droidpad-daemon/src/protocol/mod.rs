//! Protocol packet definitions and async serialisation/deserialisation.
//!
//! Wire format (see project README):
//! ```text
//! Header (4 bytes):
//!   [0] = 0x44  [1] = 0x50  (magic "DP")
//!   [2] = TYPE u8
//!   [3] = LENGTH u8 (payload bytes that follow)
//! ```

pub mod packet;
