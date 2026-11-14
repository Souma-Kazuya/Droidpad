//! DroidPad Core Daemon Loop.
//!
//! Orchestrates the active transports (Wi-Fi, Bluetooth, USB), accepts connections,
//! decodes protocol packets, and dispatches them to the `uinput` virtual keyboard
//! and Xbox 360 gamepad devices.

use anyhow::{Context, Result};
use std::future;
use std::sync::Arc;
use tokio::sync::{watch, Mutex};
use tracing::{info, warn};

use crate::protocol::packet::Packet;
use crate::slot::{ConnectedDevice, SlotManager, MAX_SLOTS};
use crate::transport::bluetooth::BluetoothTransport;
use crate::transport::usb::UsbTransport;
use crate::transport::wifi::{build_self_signed_tls_config, WifiTransport};
use crate::transport::{Connection, Transport};
use crate::uinput::gamepad::VirtualGamepad;
use crate::uinput::keyboard::VirtualKeyboard;

/// Connection state observed by UI components.
#[derive(Debug, Clone, PartialEq, Eq)]
pub enum ConnectionState {
    Disconnected,
    Connected { peer: String, transport: String },
    MultipleConnected(Vec<ConnectedDevice>),
}

/// Run the primary DroidPad daemon loop.
pub async fn run(
    wifi_port: u16,
    usb_port: u16,
    bt_name: String,
    state_tx: watch::Sender<ConnectionState>,
) -> Result<()> {
    info!("Initializing uinput virtual devices...");
    let keyboard = match VirtualKeyboard::new() {
        Ok(k) => {
            info!("Virtual keyboard registered on /dev/uinput");
            Some(Arc::new(Mutex::new(k)))
        }
        Err(e) => {
            warn!("Failed to initialize virtual keyboard: {e}. Check /dev/uinput permissions.");
            None
        }
    };

    let slot_manager = Arc::new(SlotManager::new());

    info!("Initializing transports...");
    let tls_config =
        build_self_signed_tls_config().context("generating TLS credentials for Wi-Fi transport")?;

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

    let host_name = std::fs::read_to_string("/etc/hostname")
        .unwrap_or_else(|_| "Linux Host".to_string())
        .trim()
        .to_string();

    loop {
        // Accept from any available transport
        let (conn, transport_name) = tokio::select! {
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

        let initial_name = format!("Device ({})", peer_name);
        let slot_info = slot_manager.allocate_slot(
            peer_name.clone(),
            initial_name.clone(),
            transport_name.to_string(),
        );

        let (client_id, player_slot) = match slot_info {
            Some(info) => info,
            None => {
                warn!("Max player capacity reached ({MAX_SLOTS} players). Rejecting connection from {peer_name}");
                continue;
            }
        };

        info!("Assigned Player Slot {player_slot} to client {peer_name}");

        // Update UI state with connected devices
        let _ = state_tx.send(ConnectionState::MultipleConnected(
            slot_manager.get_connected_devices(),
        ));

        // Create dedicated virtual gamepad for this player slot
        let player_gamepad = match VirtualGamepad::new_with_slot(player_slot) {
            Ok(gp) => {
                info!("Virtual gamepad registered for Player {player_slot}");
                Some(gp)
            }
            Err(e) => {
                warn!("Failed to initialize virtual gamepad for Player {player_slot}: {e}");
                None
            }
        };

        let host_name_clone = host_name.clone();
        let keyboard_clone = keyboard.clone();
        let slot_manager_clone = Arc::clone(&slot_manager);
        let state_tx_clone = state_tx.clone();

        tokio::spawn(async move {
            let mut conn = conn;
            if let Err(e) = handle_multi_client_session(
                &mut *conn,
                keyboard_clone,
                player_gamepad,
                &host_name_clone,
                client_id,
                player_slot,
                &slot_manager_clone,
                &state_tx_clone,
            )
            .await
            {
                warn!("Session error with client {client_id} (Player {player_slot}): {e}");
            }

            slot_manager_clone.release_slot(client_id);
            info!("Client disconnected (Player {player_slot}, {peer_name})");

            let remaining = slot_manager_clone.get_connected_devices();
            if remaining.is_empty() {
                let _ = state_tx_clone.send(ConnectionState::Disconnected);
            } else {
                let _ = state_tx_clone.send(ConnectionState::MultipleConnected(remaining));
            }
        });
    }
}

/// Process an active multi-client connection: exchange handshakes, dispatch input packets,
/// reply to keep-alive pings, and clean up virtual device state on disconnect.
#[allow(clippy::too_many_arguments)]
pub async fn handle_multi_client_session<C: Connection + ?Sized>(
    conn: &mut C,
    keyboard: Option<Arc<Mutex<VirtualKeyboard>>>,
    mut gamepad: Option<VirtualGamepad>,
    host_name: &str,
    client_id: usize,
    player_slot: u8,
    slot_manager: &SlotManager,
    state_tx: &watch::Sender<ConnectionState>,
) -> Result<()> {
    // 1. Send Host Handshake with assigned slot
    let host_handshake_name = format!("{host_name} (Player {player_slot})");
    let hs = Packet::handshake(host_handshake_name);
    conn.write_packet(&hs)
        .await
        .context("sending server handshake")?;

    // 2. Dispatch loop
    while let Ok(pkt) = conn.read_packet().await {
        match pkt.payload {
            crate::protocol::packet::Payload::Keyboard(ref k) => {
                if let Some(ref kb_lock) = keyboard {
                    let mut kb = kb_lock.lock().await;
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
                info!(
                    "Received client handshake from {} (Player {player_slot}): version {}",
                    hs.name, hs.version
                );
                slot_manager.update_client_name(client_id, hs.name);
                let _ = state_tx.send(ConnectionState::MultipleConnected(
                    slot_manager.get_connected_devices(),
                ));
            }
            crate::protocol::packet::Payload::Rumble(_) => {
                // Host does not consume rumble
            }
            crate::protocol::packet::Payload::GameContext(_) => {
                // Host broadcast / echo not consumed on host
            }
        }
    }

    // 3. Reset virtual device on disconnect
    if let Some(ref mut gp) = gamepad {
        let _ = gp.reset();
    }

    Ok(())
}

/// Process an active client connection: exchange handshakes, dispatch input packets,
/// reply to keep-alive pings, and clean up virtual device state on disconnect.
pub async fn handle_client_session<C: Connection + ?Sized>(
    conn: &mut C,
    mut keyboard: Option<&mut VirtualKeyboard>,
    mut gamepad: Option<&mut VirtualGamepad>,
    host_name: &str,
) -> Result<()> {
    // 1. Send Host Handshake
    let hs = Packet::handshake(host_name.to_string());
    conn.write_packet(&hs)
        .await
        .context("sending server handshake")?;

    // 2. Dispatch loop
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
                info!(
                    "Received client handshake from {}: version {}",
                    hs.name, hs.version
                );
            }
            crate::protocol::packet::Payload::Rumble(_) => {
                // Host does not consume rumble
            }
            crate::protocol::packet::Payload::GameContext(_) => {
                // Host broadcast / echo not consumed on host
            }
        }
    }

    // 3. Reset virtual devices on disconnect
    if let Some(ref mut kb) = keyboard {
        let _ = kb.release_all();
    }
    if let Some(ref mut gp) = gamepad {
        let _ = gp.reset();
    }

    Ok(())
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

#[cfg(test)]
mod tests {
    use super::*;
    use crate::protocol::packet::{GamepadPayload, KeyboardPayload, Payload};
    use crate::transport::mock::{ChannelMockConnection, MockConnection, MockTransport};

    #[tokio::test]
    async fn test_mock_connection_sends_server_handshake() {
        let mut conn = MockConnection::new("192.168.1.50:45000");

        let res = handle_client_session(&mut conn, None, None, "ArchLinux-Host").await;
        assert!(res.is_ok());

        // First packet sent must be server handshake
        assert_eq!(conn.outgoing.len(), 1);
        if let Payload::Handshake(ref hs) = conn.outgoing[0].payload {
            assert_eq!(hs.version, "1");
            assert_eq!(hs.name, "ArchLinux-Host");
        } else {
            panic!(
                "Expected Handshake payload, got {:?}",
                conn.outgoing[0].payload
            );
        }
    }

    #[tokio::test]
    async fn test_mock_connection_replies_to_ping() {
        let mut conn = MockConnection::with_incoming("192.168.1.50:45000", vec![Packet::ping()]);

        let res = handle_client_session(&mut conn, None, None, "TestHost").await;
        assert!(res.is_ok());

        // Expect 2 outgoing packets: [0] = Handshake, [1] = Ping response
        assert_eq!(conn.outgoing.len(), 2);
        assert!(matches!(conn.outgoing[0].payload, Payload::Handshake(_)));
        assert_eq!(
            conn.outgoing[1].header.pkt_type,
            crate::protocol::packet::PacketType::Ping
        );
    }

    #[tokio::test]
    async fn test_mock_connection_stream_dispatch() {
        let kbd_packet = Packet {
            header: crate::protocol::packet::Header {
                pkt_type: crate::protocol::packet::PacketType::Keyboard,
                length: 8,
            },
            payload: Payload::Keyboard(KeyboardPayload {
                modifiers: 0x02,                                // LShift
                keycodes: [0x04, 0x00, 0x00, 0x00, 0x00, 0x00], // 'A'
            }),
        };

        let pad_packet = Packet {
            header: crate::protocol::packet::Header {
                pkt_type: crate::protocol::packet::PacketType::Gamepad,
                length: 12,
            },
            payload: Payload::Gamepad(GamepadPayload {
                buttons: 0x0001, // BTN_A
                left_x: 10000,
                left_y: -10000,
                right_x: 0,
                right_y: 0,
                left_trigger: 128,
                right_trigger: 0,
            }),
        };

        let client_hs = Packet::handshake("Google Pixel 7".into());
        let rumble = Packet::rumble(50, 50);

        let mut conn = MockConnection::with_incoming(
            "Bluetooth (AA:BB:CC:DD:EE:FF)",
            vec![client_hs, kbd_packet, Packet::ping(), pad_packet, rumble],
        );

        let res = handle_client_session(&mut conn, None, None, "DesktopHost").await;
        assert!(res.is_ok());

        // Outgoing: Handshake + Ping response
        assert_eq!(conn.outgoing.len(), 2);
    }

    #[tokio::test]
    async fn test_mock_transport_queue_and_accept() {
        let mut transport = MockTransport::new("Virtual Wi-Fi");
        assert_eq!(transport.name(), "Virtual Wi-Fi");

        let conn = MockConnection::new("127.0.0.1:9999");
        transport.push_connection(conn).await;

        let accepted = transport
            .accept()
            .await
            .expect("Failed to accept mock connection");
        assert_eq!(accepted.peer_name(), "127.0.0.1:9999");
    }

    #[tokio::test]
    async fn test_channel_mock_interactive_session() {
        let (mut conn, mut handle) = ChannelMockConnection::pair("USB/ADB (127.0.0.1:27184)");

        // Spawn session handler in background
        let session_task = tokio::spawn(async move {
            handle_client_session(&mut conn, None, None, "InteractiveHost").await
        });

        // 1. Receive server handshake
        let server_hs = handle
            .outgoing_rx
            .recv()
            .await
            .expect("Did not receive server handshake");
        if let Payload::Handshake(ref hs) = server_hs.payload {
            assert_eq!(hs.name, "InteractiveHost");
        } else {
            panic!("Expected handshake");
        }

        // 2. Send Ping and receive Pong
        handle.incoming_tx.send(Packet::ping()).await.unwrap();
        let pong = handle
            .outgoing_rx
            .recv()
            .await
            .expect("Did not receive ping reply");
        assert_eq!(
            pong.header.pkt_type,
            crate::protocol::packet::PacketType::Ping
        );

        // 3. Close incoming channel to simulate disconnect
        drop(handle.incoming_tx);

        let result = session_task.await.unwrap();
        assert!(result.is_ok());
    }

    #[tokio::test]
    async fn test_multi_transport_selection() {
        let mut wifi = MockTransport::new("Wi-Fi");
        let mut usb = MockTransport::new("USB");

        // Queue connection on USB transport only
        usb.push_connection(MockConnection::new("USB-Client-1"))
            .await;

        // Select should pick USB immediately without hanging on empty Wi-Fi
        let (conn, selected_name) = tokio::select! {
            res = accept_opt(Some(&mut wifi)) => (res.unwrap(), "Wi-Fi"),
            res = accept_opt(Some(&mut usb)) => (res.unwrap(), "USB"),
        };

        assert_eq!(selected_name, "USB");
        assert_eq!(conn.peer_name(), "USB-Client-1");
    }

    #[tokio::test]
    async fn test_multi_client_concurrent_sessions() {
        let (mut conn1, mut handle1) = ChannelMockConnection::pair("Device-1");
        let (mut conn2, mut handle2) = ChannelMockConnection::pair("Device-2");

        let slot_mgr = Arc::new(SlotManager::new());
        let (state_tx, _state_rx) = watch::channel(ConnectionState::Disconnected);

        let (id1, slot1) = slot_mgr
            .allocate_slot("192.168.1.10".into(), "Phone 1".into(), "Wi-Fi".into())
            .unwrap();
        let (id2, slot2) = slot_mgr
            .allocate_slot("192.168.1.11".into(), "Phone 2".into(), "Wi-Fi".into())
            .unwrap();

        assert_eq!(slot1, 1);
        assert_eq!(slot2, 2);

        let sm1 = Arc::clone(&slot_mgr);
        let st1 = state_tx.clone();
        let t1 = tokio::spawn(async move {
            handle_multi_client_session(&mut conn1, None, None, "Host", id1, slot1, &sm1, &st1)
                .await
        });

        let sm2 = Arc::clone(&slot_mgr);
        let st2 = state_tx.clone();
        let t2 = tokio::spawn(async move {
            handle_multi_client_session(&mut conn2, None, None, "Host", id2, slot2, &sm2, &st2)
                .await
        });

        // Verify Player 1 handshake
        let hs1 = handle1.outgoing_rx.recv().await.unwrap();
        if let Payload::Handshake(ref h) = hs1.payload {
            assert!(h.name.contains("Player 1"));
        } else {
            panic!("Expected handshake");
        }

        // Verify Player 2 handshake
        let hs2 = handle2.outgoing_rx.recv().await.unwrap();
        if let Payload::Handshake(ref h) = hs2.payload {
            assert!(h.name.contains("Player 2"));
        } else {
            panic!("Expected handshake");
        }

        // Send client handshakes with device names
        handle1
            .incoming_tx
            .send(Packet::handshake("Alice Pixel".into()))
            .await
            .unwrap();
        handle2
            .incoming_tx
            .send(Packet::handshake("Bob Galaxy".into()))
            .await
            .unwrap();

        // Give a moment for handshakes to process
        tokio::time::sleep(std::time::Duration::from_millis(50)).await;

        let devices = slot_mgr.get_connected_devices();
        assert_eq!(devices.len(), 2);
        assert_eq!(devices[0].name, "Alice Pixel");
        assert_eq!(devices[1].name, "Bob Galaxy");

        // Disconnect handle1
        drop(handle1.incoming_tx);
        let _ = t1.await;
        slot_mgr.release_slot(id1);

        assert_eq!(slot_mgr.connected_count(), 1);
        assert_eq!(slot_mgr.get_connected_devices()[0].slot, 2);

        // Disconnect handle2
        drop(handle2.incoming_tx);
        let _ = t2.await;
        slot_mgr.release_slot(id2);

        assert_eq!(slot_mgr.connected_count(), 0);
    }
}
