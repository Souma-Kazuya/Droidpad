//! DroidPad Linux Daemon — entry point.
//!
//! Parses CLI arguments, initialises logging, spawns the connection daemon,
//! and optionally launches the GTK4/libadwaita UI (system tray + settings).

mod daemon;
mod protocol;
mod transport;
mod ui;
mod uinput;

use anyhow::Result;
use std::sync::Arc;
use tokio::sync::watch;
use tracing::info;

/// CLI arguments parsed from the command line.
struct Args {
    /// Disable the system-tray / GTK4 UI.
    no_tray: bool,
    /// TCP port for Wi-Fi (TLS) transport. Default: 27183.
    port: u16,
    /// Bluetooth advertised device name. Default: "DroidPad".
    bt_name: String,
    /// ADB forwarded USB port. Default: 27184.
    usb_port: u16,
}

impl Args {
    fn parse() -> Self {
        let mut args = Args {
            no_tray: false,
            port: 27183,
            bt_name: "DroidPad".to_string(),
            usb_port: 27184,
        };

        let raw: Vec<String> = std::env::args().collect();
        let mut i = 1;
        while i < raw.len() {
            match raw[i].as_str() {
                "--no-tray" => args.no_tray = true,
                "--port" => {
                    i += 1;
                    if let Some(v) = raw.get(i) {
                        args.port = v.parse().expect("--port must be a valid u16");
                    }
                }
                "--bt-name" => {
                    i += 1;
                    if let Some(v) = raw.get(i) {
                        args.bt_name = v.clone();
                    }
                }
                "--usb-port" => {
                    i += 1;
                    if let Some(v) = raw.get(i) {
                        args.usb_port = v.parse().expect("--usb-port must be a valid u16");
                    }
                }
                other => eprintln!("Unknown argument: {other}"),
            }
            i += 1;
        }

        args
    }
}

fn main() -> Result<()> {
    // Initialise tracing/logging before anything else.
    tracing_subscriber::fmt()
        .with_env_filter(
            tracing_subscriber::EnvFilter::try_from_default_env()
                .unwrap_or_else(|_| tracing_subscriber::EnvFilter::new("info")),
        )
        .init();

    let args = Args::parse();
    info!("DroidPad daemon starting (Wi-Fi port={}, BT name={:?})", args.port, args.bt_name);

    // Build a watch channel so the daemon can broadcast connection state to the UI.
    let (state_tx, state_rx) = watch::channel(daemon::ConnectionState::Disconnected);

    if args.no_tray {
        // Headless mode: run the Tokio runtime on the current thread.
        let rt = tokio::runtime::Builder::new_multi_thread()
            .enable_all()
            .build()?;
        rt.block_on(async move {
            daemon::run(args.port, args.usb_port, args.bt_name, state_tx).await
        })?;
    } else {
        // GUI mode: GTK4 must run on the main thread; Tokio runs in background threads.
        let rt = Arc::new(
            tokio::runtime::Builder::new_multi_thread()
                .enable_all()
                .build()?,
        );

        let rt_clone = Arc::clone(&rt);
        let bt_name = args.bt_name.clone();
        let port = args.port;
        let usb_port = args.usb_port;

        // Spawn the daemon on the Tokio runtime (background).
        rt.spawn(async move {
            if let Err(e) = daemon::run(port, usb_port, bt_name, state_tx).await {
                tracing::error!("Daemon exited with error: {e:#}");
            }
        });

        // Start the GTK4 application on the main thread.
        ui::run_app(state_rx, rt_clone)?;
    }

    Ok(())
}
