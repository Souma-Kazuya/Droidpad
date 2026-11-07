//! DroidPad Core Daemon Loop.
//!
//! Orchestrates the active transports (Wi-Fi, Bluetooth, USB), accepts connections,
//! decodes protocol packets, and dispatches them to the `uinput` virtual keyboard
//! and Xbox 360 gamepad devices.

use anyhow::{Context, Result};
use std::future;
use tokio::sync::watch;
use tracing::{info, warn};

use crate::protocol::packet::Packet;
use crate::transport::wifi::{build_self_signed_tls_config, WifiTransport};
use crate::transport::usb::UsbTransport;
use crate::transport::bluetooth::BluetoothTransport;
use crate::transport::{Connection, Transport};
use crate::uinput::keyboard::VirtualKeyboard;
use crate::uinput::gamepad::VirtualGamepad;

/// Connection state observed by UI components.
#[derive(Debug, Clone, PartialEq, Eq)]
pub enum ConnectionState {
    Disconnected,
    Connected {
        peer: String,
        transport: String,
    },
}

/// Run the primary DroidPad daemon loop.
pub async fn run(
    wifi_port: u16,
    usb_port: u16,
    bt_name: String,
    state_tx: watch::Sender<ConnectionState>,
) -> Result<()> {
    info!("Initializing uinput virtual devices...");
    let mut keyboard = match VirtualKeyboard::new() {
        Ok(k) => {
            info!("Virtual keyboard registered on /dev/uinput");
            Some(k)
        }
        Err(e) => {
            warn!("Failed to initialize virtual keyboard: {e}. Check /dev/uinput permissions.");
            None
        }
    };

    let mut gamepad = match VirtualGamepad::new() {
        Ok(g) => {
            info!("Virtual Xbox 360 gamepad registered on /dev/uinput");
            Some(g)
        }
        Err(e) => {
            warn!("Failed to initialize virtual gamepad: {e}. Check /dev/uinput permissions.");
            None
        }
    };

    info!("Initializing transports...");
    let tls_config = build_self_signed_tls_config()
        .context("generating TLS credentials for Wi-Fi transport")?;

    let mut wifi_transport = match WifiTransport::new(wifi_port, tls_config).await {
        Ok(w) => Some(w),
        Err(e) => {
            warn!("Could not start Wi-Fi transport: {e}");
            None
        }
    };

    let mut usb_transport = match UsbTransport::new(usb_port).await {
        Ok(u) => Some(u),
        Err(e) => {
            warn!("Could not start USB transport: {e}");
            None
        }
    };

    let mut bt_transport = match BluetoothTransport::new(bt_name).await {
        Ok(b) => Some(b),
        Err(e) => {
            warn!("Could not start Bluetooth transport: {e}");
            None
        }
    };

    info!("All ready. Listening for incoming Android connections...");

    loop {
        // Accept from any available transport
        let (mut conn, transport_name) = tokio::select! {
            res = accept_opt(wifi_transport.as_mut()) => {
                match res {
                    Ok(c) => (c, "Wi-Fi"),
                    Err(e) => { warn!("Wi-Fi accept error: {e}"); continue; }
                }
            }
            res = accept_opt(usb_transport.as_mut()) => {
                match res {
                    Ok(c) => (c, "USB"),
                    Err(e) => { warn!("USB accept error: {e}"); continue; }
                }
            }
            res = accept_opt(bt_transport.as_mut()) => {
                match res {
                    Ok(c) => (c, "Bluetooth"),
                    Err(e) => { warn!("Bluetooth accept error: {e}"); continue; }
                }
            }
        };

        let peer_name = conn.peer_name();
        info!("Client connected via {transport_name}: {peer_name}");

        let _ = state_tx.send(ConnectionState::Connected {
            peer: peer_name.clone(),
            transport: transport_name.to_string(),
        });

        // Exchange Handshake
        let host_name = std::fs::read_to_string("/etc/hostname")
            .unwrap_or_else(|_| "Linux Host".to_string())
            .trim()
            .to_string();

        let hs = Packet::handshake(host_name);

        if let Err(e) = conn.write_packet(&hs).await {
            warn!("Failed to send handshake: {e}");
            let _ = state_tx.send(ConnectionState::Disconnected);
            continue;
        }

        // Handle packets from this connection until disconnect
        while let Ok(pkt) = conn.read_packet().await {
            match pkt.payload {
                crate::protocol::packet::Payload::Keyboard(ref k) => {
                    if let Some(ref mut kb) = keyboard {
                        if let Err(e) = kb.apply(k) {
                            debug_warn!("Error applying keyboard event: {e}");
                        }
                    }
                }
                crate::protocol::packet::Payload::Gamepad(ref g) => {
                    if let Some(ref mut gp) = gamepad {
                        if let Err(e) = gp.apply(g) {
                            debug_warn!("Error applying gamepad event: {e}");
                        }
                    }
                }
                crate::protocol::packet::Payload::Ping => {
                    let _ = conn.write_packet(&Packet::ping()).await;
                }
                crate::protocol::packet::Payload::Handshake(hs) => {
                    info!("Received client handshake from {}: version {}", hs.name, hs.version);
                }
                crate::protocol::packet::Payload::Rumble(_) => {
                    // Host does not consume rumble
                }
            }
        }

        info!("Client disconnected ({peer_name})");
        if let Some(ref mut kb) = keyboard {
            let _ = kb.release_all();
        }
        if let Some(ref mut gp) = gamepad {
            let _ = gp.reset();
        }
        let _ = state_tx.send(ConnectionState::Disconnected);
    }
}

async fn accept_opt<T: Transport + ?Sized>(t: Option<&mut T>) -> Result<Box<dyn Connection>> {
    match t {
        Some(transport) => transport.accept().await,
        None => {
            // Keep pending forever if transport is unconfigured/unavailable
            future::pending::<()>().await;
            anyhow::bail!("Transport uninitialized")
        }
    }
}

macro_rules! debug_warn {
    ($($arg:tt)*) => {
        tracing::debug!($($arg)*)
    };
}
pub(crate) use debug_warn;
