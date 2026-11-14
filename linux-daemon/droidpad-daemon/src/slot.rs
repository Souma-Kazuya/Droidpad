//! Player Slot & Active Device Manager.
//!
//! Manages player controller slot assignment (Player 1..4) and tracks active
//! connected devices across transports (Wi-Fi, USB, Bluetooth).

use std::collections::HashMap;
use std::sync::{Arc, Mutex};

pub const MAX_SLOTS: u8 = 4;

/// Information about a connected client device.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct ConnectedDevice {
    pub id: usize,
    pub slot: u8,
    pub peer: String,
    pub name: String,
    pub transport: String,
}

/// Slot Manager keeping track of allocated controller slots.
#[derive(Debug, Default)]
pub struct SlotManager {
    inner: Arc<Mutex<SlotState>>,
}

#[derive(Debug)]
struct SlotState {
    next_id: usize,
    /// Maps client ID -> ConnectedDevice
    clients: HashMap<usize, ConnectedDevice>,
    /// Which slots (1..=MAX_SLOTS) are currently taken by which client ID
    slot_assignments: HashMap<u8, usize>,
}

impl Default for SlotState {
    fn default() -> Self {
        Self {
            next_id: 1,
            clients: HashMap::new(),
            slot_assignments: HashMap::new(),
        }
    }
}

impl SlotManager {
    pub fn new() -> Self {
        Self {
            inner: Arc::new(Mutex::new(SlotState::default())),
        }
    }

    /// Allocate the lowest available slot for a new client.
    /// Returns `(client_id, slot_number)` if a slot is available.
    pub fn allocate_slot(
        &self,
        peer: String,
        name: String,
        transport: String,
    ) -> Option<(usize, u8)> {
        let mut state = self.inner.lock().unwrap();

        // Find lowest free slot between 1 and MAX_SLOTS
        let mut assigned_slot = None;
        for s in 1..=MAX_SLOTS {
            if !state.slot_assignments.contains_key(&s) {
                assigned_slot = Some(s);
                break;
            }
        }

        let slot = assigned_slot?;
        let id = state.next_id;
        state.next_id += 1;

        state.slot_assignments.insert(slot, id);
        state.clients.insert(
            id,
            ConnectedDevice {
                id,
                slot,
                peer,
                name,
                transport,
            },
        );

        Some((id, slot))
    }

    /// Update client device name (e.g. after handshake packet).
    pub fn update_client_name(&self, id: usize, name: String) {
        let mut state = self.inner.lock().unwrap();
        if let Some(client) = state.clients.get_mut(&id) {
            client.name = name;
        }
    }

    /// Release slot when a client disconnects.
    pub fn release_slot(&self, id: usize) -> Option<u8> {
        let mut state = self.inner.lock().unwrap();
        if let Some(client) = state.clients.remove(&id) {
            state.slot_assignments.remove(&client.slot);
            Some(client.slot)
        } else {
            None
        }
    }

    /// Get a snapshot of all currently connected devices.
    pub fn get_connected_devices(&self) -> Vec<ConnectedDevice> {
        let state = self.inner.lock().unwrap();
        let mut list: Vec<ConnectedDevice> = state.clients.values().cloned().collect();
        list.sort_by_key(|d| d.slot);
        list
    }

    /// Count of currently connected devices.
    pub fn connected_count(&self) -> usize {
        let state = self.inner.lock().unwrap();
        state.clients.len()
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_slot_allocation_and_release() {
        let mgr = SlotManager::new();

        let c1 = mgr.allocate_slot("192.168.1.10:1234".into(), "Phone 1".into(), "Wi-Fi".into());
        assert_eq!(c1, Some((1, 1)));

        let c2 = mgr.allocate_slot("192.168.1.11:1234".into(), "Phone 2".into(), "Wi-Fi".into());
        assert_eq!(c2, Some((2, 2)));

        let devices = mgr.get_connected_devices();
        assert_eq!(devices.len(), 2);
        assert_eq!(devices[0].slot, 1);
        assert_eq!(devices[1].slot, 2);

        // Release slot 1
        assert_eq!(mgr.release_slot(1), Some(1));
        assert_eq!(mgr.connected_count(), 1);

        // New client should take slot 1 (lowest available)
        let c3 = mgr.allocate_slot("192.168.1.12:1234".into(), "Phone 3".into(), "USB".into());
        assert_eq!(c3, Some((3, 1)));
    }

    #[test]
    fn test_slot_limit() {
        let mgr = SlotManager::new();
        for i in 1..=MAX_SLOTS {
            assert!(mgr
                .allocate_slot(format!("peer_{i}"), format!("dev_{i}"), "Wi-Fi".into())
                .is_some());
        }
        // 5th client should fail to allocate
        assert!(mgr
            .allocate_slot("peer_5".into(), "dev_5".into(), "Wi-Fi".into())
            .is_none());
    }
}
