//! USB transport via ADB forwarding or local loopback bridge.
//!
//! When connecting over USB, the connection travels through an ADB tunnel:
//! Either:
//!   `adb reverse tcp:27184 tcp:27184` (allowing Android to connect to localhost:27184)
//! Or a local port-forwarding setup.
//!
//! Because this traffic is confined to localhost / the secure ADB tunnel,
//! plain TCP is used for minimal latency (~1-3ms).

use anyhow::{Context, Result};
use async_trait::async_trait;
use std::net::SocketAddr;
use tokio::net::{TcpListener, TcpStream};
use tracing::info;

use super::{Connection, Transport};
use crate::protocol::packet::Packet;

// ---------------------------------------------------------------------------
// UsbConnection
// ---------------------------------------------------------------------------

pub struct UsbConnection {
    stream: TcpStream,
    peer: SocketAddr,
}

#[async_trait]
impl Connection for UsbConnection {
    async fn read_packet(&mut self) -> Result<Packet> {
        Packet::read_async(&mut self.stream).await
    }

    async fn write_packet(&mut self, pkt: &Packet) -> Result<()> {
        pkt.write_async(&mut self.stream).await
    }

    fn peer_name(&self) -> String {
        format!("USB/ADB ({})", self.peer)
    }
}

// ---------------------------------------------------------------------------
// UsbTransport
// ---------------------------------------------------------------------------

pub struct UsbTransport {
    listener: TcpListener,
    port: u16,
}

impl UsbTransport {
    pub async fn new(port: u16) -> Result<Self> {
        let addr = format!("127.0.0.1:{port}");
        let listener = TcpListener::bind(&addr)
            .await
            .with_context(|| format!("binding USB/ADB TCP listener on {addr}"))?;
        info!("USB transport listening on {addr}");

        Ok(Self { listener, port })
    }
}

#[async_trait]
impl Transport for UsbTransport {
    async fn accept(&mut self) -> Result<Box<dyn Connection>> {
        let (stream, peer) = self.listener.accept().await.context("USB TCP accept")?;
        info!("USB: accepted connection from {peer}");
        Ok(Box::new(UsbConnection { stream, peer }))
    }

    fn name(&self) -> &'static str {
        "USB"
    }
}
