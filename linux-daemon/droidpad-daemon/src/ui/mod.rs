//! DroidPad Linux UI: Libadwaita Application & Status Window.

pub mod settings;
pub mod tray;

use anyhow::Result;
use libadwaita::prelude::*;
use libadwaita::Application;
use std::sync::Arc;
use tokio::sync::watch;

use crate::daemon::ConnectionState;

/// Initialize and run the GTK4 / Libadwaita GUI event loop on the main thread.
pub fn run_app(
    state_rx: watch::Receiver<ConnectionState>,
    _rt: Arc<tokio::runtime::Runtime>,
) -> Result<()> {
    let app = Application::builder()
        .application_id("com.droidpad.LinuxDaemon")
        .build();

    app.connect_activate(move |app| {
        tray::build_main_window(app, state_rx.clone());
    });

    let empty_args: Vec<String> = vec![];
    app.run_with_args(&empty_args);

    Ok(())
}
