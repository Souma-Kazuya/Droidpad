//! DroidPad Linux UI: Main Status & Tray Window.

use gtk4::glib;
use gtk4::prelude::*;
use gtk4::{Align, Box as GtkBox, Button, Image, Label, Orientation};
use libadwaita::prelude::*;
use libadwaita::{Application, HeaderBar, Window};
use tokio::sync::watch;

use crate::daemon::ConnectionState;
use crate::ui::settings::show_settings_window;

/// Retrieve primary non-loopback IPv4 addresses of this machine.
pub fn get_host_ips() -> Vec<String> {
    if let Ok(interfaces) = if_addrs::get_if_addrs() {
        let mut ips = Vec::new();
        for iface in interfaces {
            if !iface.is_loopback() {
                if let if_addrs::IfAddr::V4(ref addr) = iface.addr {
                    ips.push(addr.ip.to_string());
                }
            }
        }
        if !ips.is_empty() {
            return ips;
        }
    }
    vec!["127.0.0.1".to_string()]
}

/// Construct the main status window displaying current connection and quick settings.
pub fn build_main_window(app: &Application, state_rx: watch::Receiver<ConnectionState>) {
    let window = Window::builder()
        .application(app)
        .title("DroidPad")
        .default_width(400)
        .default_height(320)
        .build();

    let root = GtkBox::new(Orientation::Vertical, 0);

    let header = HeaderBar::new();
    let settings_btn = Button::from_icon_name("preferences-system-symbolic");
    let app_clone = app.clone();
    settings_btn.connect_clicked(move |_| {
        show_settings_window(&app_clone);
    });
    header.pack_end(&settings_btn);
    root.append(&header);

    let content = GtkBox::new(Orientation::Vertical, 14);
    content.set_margin_top(20);
    content.set_margin_bottom(20);
    content.set_margin_start(24);
    content.set_margin_end(24);
    content.set_valign(Align::Center);
    content.set_halign(Align::Center);

    let icon = Image::builder()
        .icon_name("droidpad")
        .pixel_size(64)
        .build();
    content.append(&icon);

    let status_title = Label::builder()
        .label("Ready to Connect")
        .css_classes(["title-2"])
        .build();
    content.append(&status_title);

    let status_subtitle = Label::builder()
        .label("Listening on Wi-Fi, Bluetooth & USB")
        .css_classes(["dim-label"])
        .build();
    content.append(&status_subtitle);

    // Host IP pill / card for easy Wi-Fi manual connection on Android
    let host_ips = get_host_ips();
    let ip_str = host_ips.join(" • ");
    let ip_box = GtkBox::new(Orientation::Vertical, 4);
    ip_box.set_margin_top(8);
    ip_box.set_halign(Align::Center);

    let ip_badge_label = Label::builder()
        .label(format!("Wi-Fi IP: {ip_str} (Port 27183)"))
        .selectable(true)
        .css_classes(["accent", "heading"])
        .build();
    ip_box.append(&ip_badge_label);

    let ip_hint_label = Label::builder()
        .label("Enter this IP in DroidPad on Android if Wi-Fi discovery does not find it")
        .css_classes(["caption", "dim-label"])
        .build();
    ip_box.append(&ip_hint_label);

    content.append(&ip_box);

    // Watch channel updater for connection state
    let status_title_clone = status_title.clone();
    let status_subtitle_clone = status_subtitle.clone();
    let mut rx = state_rx.clone();

    glib::timeout_add_local(std::time::Duration::from_millis(250), move || {
        if rx.has_changed().unwrap_or(false) {
            let state = rx.borrow_and_update().clone();
            match state {
                ConnectionState::Disconnected => {
                    status_title_clone.set_label("Ready to Connect");
                    status_subtitle_clone.set_label("Listening on Wi-Fi, Bluetooth & USB");
                }
                ConnectionState::Connected { peer, transport } => {
                    status_title_clone.set_label("Connected");
                    status_subtitle_clone.set_label(&format!("{peer} via {transport}"));
                }
                ConnectionState::MultipleConnected(devices) => {
                    if devices.is_empty() {
                        status_title_clone.set_label("Ready to Connect");
                        status_subtitle_clone.set_label("Listening on Wi-Fi, Bluetooth & USB");
                    } else if devices.len() == 1 {
                        let dev = &devices[0];
                        status_title_clone.set_label(&format!("Player {} Connected", dev.slot));
                        status_subtitle_clone.set_label(&format!(
                            "{} ({}) via {}",
                            dev.name, dev.peer, dev.transport
                        ));
                    } else {
                        status_title_clone
                            .set_label(&format!("{} Players Connected", devices.len()));
                        let details: Vec<String> = devices
                            .iter()
                            .map(|d| format!("P{}: {} [{}]", d.slot, d.name, d.transport))
                            .collect();
                        status_subtitle_clone.set_label(&details.join("\n"));
                    }
                }
            }
        }
        glib::ControlFlow::Continue
    });

    root.append(&content);
    window.set_content(Some(&root));
    window.present();
}
