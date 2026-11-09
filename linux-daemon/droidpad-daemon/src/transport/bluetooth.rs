//! Bluetooth transport via BlueZ RFCOMM.
//!
//! Exposes an RFCOMM SPP (Serial Port Profile) service advertised with
//! custom UUID `6e400001-b5a3-f393-e0a9-e50e24dcca9e`.
//!
//! If BlueZ is not running or no Bluetooth adapter is present, this transport
//! will log a warning and stand by without crashing the daemon, allowing Wi-Fi
//! and USB transports to continue serving input.

use anyhow::{Context, Result};
use async_trait::async_trait;
use futures_util::StreamExt;
use std::future;
use std::sync::Arc;
use tokio::sync::Mutex;
use tracing::{info, warn};

use super::{Connection, Transport};
use crate::protocol::packet::Packet;

/// Custom UUID used for DroidPad RFCOMM service.
pub const DROIDPAD_UUID: &str = "6e400001-b5a3-f393-e0a9-e50e24dcca9e";

// ---------------------------------------------------------------------------
// BluetoothConnection
// ---------------------------------------------------------------------------

pub struct BluetoothConnection {
    stream: bluer::rfcomm::Stream,
    peer: String,
}

#[async_trait]
impl Connection for BluetoothConnection {
    async fn read_packet(&mut self) -> Result<Packet> {
        Packet::read_async(&mut self.stream).await
    }

    async fn write_packet(&mut self, pkt: &Packet) -> Result<()> {
        pkt.write_async(&mut self.stream).await
    }

    fn peer_name(&self) -> String {
        format!("Bluetooth ({})", self.peer)
    }
}

// ---------------------------------------------------------------------------
// BluetoothTransport
// ---------------------------------------------------------------------------

pub struct BluetoothTransport {
    session: Option<bluer::Session>,
    profile_handle: Option<bluer::rfcomm::ProfileHandle>,
    incoming_rx: Arc<Mutex<tokio::sync::mpsc::Receiver<bluer::rfcomm::Stream>>>,
    name: String,
}

impl BluetoothTransport {
    pub async fn new(device_name: String) -> Result<Self> {
        let (_tx, rx) = tokio::sync::mpsc::channel(4);

        let session_res = bluer::Session::new().await;
        let mut session = None;
        let mut profile_handle = None;

        match session_res {
            Ok(sess) => {
                info!("Connected to BlueZ system D-Bus");
                match sess.default_adapter().await {
                    Ok(adapter) => {
                        let _ = adapter.set_powered(true).await;
                        let _ = adapter.set_discoverable(true).await;
                        info!("Bluetooth adapter {:?} initialized", adapter.name());

                        // Register RFCOMM profile with custom UUID
                        let profile = bluer::rfcomm::Profile {
                            uuid: bluer::Uuid::parse_str(DROIDPAD_UUID)
                                .unwrap_or_else(|_| bluer::Uuid::nil()),
                            name: Some(device_name.clone()),
                            channel: Some(1),
                            role: Some(bluer::rfcomm::Role::Server),
                            require_authentication: Some(false),
                            require_authorization: Some(false),
                            auto_connect: Some(true),
                            ..Default::default()
                        };

                        match sess.register_profile(profile).await {
                            Ok(handle) => {
                                info!("Registered DroidPad Bluetooth RFCOMM profile ({DROIDPAD_UUID})");
                                profile_handle = Some(handle);
                            }
                            Err(e) => {
                                warn!("Could not register BlueZ RFCOMM profile: {e}. Bluetooth transport disabled.");
                            }
                        }
                    }
                    Err(e) => {
                        warn!("No active Bluetooth adapter found ({e}). Bluetooth transport disabled.");
                    }
                }
                session = Some(sess);
            }
            Err(e) => {
                warn!("BlueZ D-Bus session unavailable ({e}). Bluetooth transport disabled.");
            }
        }

        Ok(Self {
            session,
            profile_handle,
            incoming_rx: Arc::new(Mutex::new(rx)),
            name: device_name,
        })
    }
}

#[async_trait]
impl Transport for BluetoothTransport {
    async fn accept(&mut self) -> Result<Box<dyn Connection>> {
        if let Some(ref mut handle) = self.profile_handle {
            // Wait for incoming RFCOMM connection request from registered profile
            let req = handle
                .next()
                .await
                .context("Bluetooth profile connection request stream closed")?;

            let stream = req
                .accept()
                .context("Accepting Bluetooth RFCOMM connection")?;

            let peer_addr = stream
                .peer_addr()
                .map(|a| a.to_string())
                .unwrap_or_else(|_| "Unknown BT".into());
            info!("Bluetooth RFCOMM connected from {peer_addr}");

            Ok(Box::new(BluetoothConnection {
                stream,
                peer: peer_addr,
            }))
        } else {
            // Keep pending indefinitely if Bluetooth is unavailable
            future::pending::<()>().await;
            anyhow::bail!("Bluetooth unavailable")
        }
    }

    fn name(&self) -> &'static str {
        "Bluetooth"
    }
}
