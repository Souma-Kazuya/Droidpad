//! Mock transport and connection implementations for testing.

use std::collections::VecDeque;
use std::sync::Arc;
use anyhow::{bail, Result};
use async_trait::async_trait;
use tokio::sync::{mpsc, Mutex};

use crate::protocol::packet::Packet;
use super::{Connection, Transport};

// ---------------------------------------------------------------------------
// In-Memory MockConnection (Queue-based)
// ---------------------------------------------------------------------------

/// Simple in-memory mock connection with pre-queued incoming packets
/// and an inspectable list of outgoing packets.
pub struct MockConnection {
    pub peer: String,
    pub incoming: VecDeque<Packet>,
    pub outgoing: Vec<Packet>,
    pub closed: bool,
}

impl MockConnection {
    pub fn new(peer: impl Into<String>) -> Self {
        Self {
            peer: peer.into(),
            incoming: VecDeque::new(),
            outgoing: Vec::new(),
            closed: false,
        }
    }

    pub fn with_incoming(peer: impl Into<String>, packets: impl IntoIterator<Item = Packet>) -> Self {
        Self {
            peer: peer.into(),
            incoming: packets.into_iter().collect(),
            outgoing: Vec::new(),
            closed: false,
        }
    }

    #[allow(dead_code)]
    pub fn push_incoming(&mut self, pkt: Packet) {
        self.incoming.push_back(pkt);
    }
}

#[async_trait]
impl Connection for MockConnection {
    async fn read_packet(&mut self) -> Result<Packet> {
        if self.closed {
            bail!("MockConnection is closed");
        }
        match self.incoming.pop_front() {
            Some(pkt) => Ok(pkt),
            None => {
                self.closed = true;
                bail!("EOF: connection closed by client");
            }
        }
    }

    async fn write_packet(&mut self, pkt: &Packet) -> Result<()> {
        if self.closed {
            bail!("Cannot write to closed MockConnection");
        }
        self.outgoing.push(pkt.clone());
        Ok(())
    }

    fn peer_name(&self) -> String {
        self.peer.clone()
    }
}

// ---------------------------------------------------------------------------
// Channel-backed MockConnection (Interactive Async)
// ---------------------------------------------------------------------------

/// Channel-backed connection for concurrent testing of client-server loops.
pub struct ChannelMockConnection {
    pub peer: String,
    incoming_rx: mpsc::Receiver<Packet>,
    outgoing_tx: mpsc::Sender<Packet>,
}

/// Control handle for `ChannelMockConnection` held by the test runner.
pub struct ChannelMockHandle {
    pub incoming_tx: mpsc::Sender<Packet>,
    pub outgoing_rx: mpsc::Receiver<Packet>,
}

impl ChannelMockConnection {
    pub fn pair(peer: impl Into<String>) -> (Self, ChannelMockHandle) {
        let (in_tx, in_rx) = mpsc::channel(32);
        let (out_tx, out_rx) = mpsc::channel(32);

        let conn = Self {
            peer: peer.into(),
            incoming_rx: in_rx,
            outgoing_tx: out_tx,
        };

        let handle = ChannelMockHandle {
            incoming_tx: in_tx,
            outgoing_rx: out_rx,
        };

        (conn, handle)
    }
}

#[async_trait]
impl Connection for ChannelMockConnection {
    async fn read_packet(&mut self) -> Result<Packet> {
        match self.incoming_rx.recv().await {
            Some(pkt) => Ok(pkt),
            None => bail!("EOF: incoming channel closed"),
        }
    }

    async fn write_packet(&mut self, pkt: &Packet) -> Result<()> {
        self.outgoing_tx
            .send(pkt.clone())
            .await
            .map_err(|_| anyhow::anyhow!("outgoing channel receiver dropped"))
    }

    fn peer_name(&self) -> String {
        self.peer.clone()
    }
}

// ---------------------------------------------------------------------------
// MockTransport
// ---------------------------------------------------------------------------

/// Mock transport implementing [`Transport`] that yields queued connections.
pub struct MockTransport {
    pub name: &'static str,
    connections: Arc<Mutex<VecDeque<Box<dyn Connection>>>>,
}

impl MockTransport {
    pub fn new(name: &'static str) -> Self {
        Self {
            name,
            connections: Arc::new(Mutex::new(VecDeque::new())),
        }
    }

    pub async fn push_connection(&self, conn: impl Connection + 'static) {
        let mut guard = self.connections.lock().await;
        guard.push_back(Box::new(conn));
    }
}

#[async_trait]
impl Transport for MockTransport {
    async fn accept(&mut self) -> Result<Box<dyn Connection>> {
        let mut guard = self.connections.lock().await;
        if let Some(conn) = guard.pop_front() {
            return Ok(conn);
        }
        drop(guard);

        // Keep pending indefinitely if no connections queued, matching real transports
        std::future::pending::<()>().await;
        bail!("Transport closed")
    }

    fn name(&self) -> &'static str {
        self.name
    }
}
