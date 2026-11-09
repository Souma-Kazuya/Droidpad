//! Transport abstraction layer.
//!
//! Each transport (Wi-Fi/TLS, Bluetooth RFCOMM, USB/ADB) implements the
//! [`Transport`] trait which yields [`Connection`] objects.  The daemon
//! polls all active transports concurrently via `tokio::select!`.

pub mod bluetooth;
pub mod mock;
pub mod usb;
pub mod wifi;

use anyhow::Result;
use async_trait::async_trait;

use crate::protocol::packet::Packet;

// ---------------------------------------------------------------------------
// Transport trait
// ---------------------------------------------------------------------------

/// A listening transport that can accept incoming device connections.
#[async_trait]
pub trait Transport: Send + Sync {
    /// Block until a new client connects, then return a [`Connection`].
    async fn accept(&mut self) -> Result<Box<dyn Connection>>;

    /// Human-readable transport name ("Wi-Fi", "Bluetooth", "USB").
    fn name(&self) -> &'static str;
}

// ---------------------------------------------------------------------------
// Connection trait
// ---------------------------------------------------------------------------

/// An active connection to a DroidPad Android client.
#[async_trait]
pub trait Connection: Send + Sync {
    /// Read the next packet from the stream.
    async fn read_packet(&mut self) -> Result<Packet>;

    /// Write a packet to the stream.
    async fn write_packet(&mut self, pkt: &Packet) -> Result<()>;

    /// Human-readable peer identifier (IP address, BT address, etc.).
    fn peer_name(&self) -> String;
}
