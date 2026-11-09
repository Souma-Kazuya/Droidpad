//! GTK4 / Libadwaita Preferences & Configuration window.

use gtk4::{Align, Label, Switch};
use libadwaita::prelude::*;
use libadwaita::{ActionRow, Application, PreferencesGroup, PreferencesPage, PreferencesWindow};

/// Create and present the DroidPad Settings & Status window.
pub fn show_settings_window(app: &Application) {
    let window = PreferencesWindow::builder()
        .application(app)
        .title("DroidPad Settings")
        .default_width(540)
        .default_height(480)
        .build();

    let page = PreferencesPage::new();
    page.set_title("General");
    page.set_icon_name(Some("preferences-system-symbolic"));

    // ── Transports Group ───────────────────────────────────────────────────────
    let transport_group = PreferencesGroup::new();
    transport_group.set_title("Transports & Connections");
    transport_group.set_description(Some("Configure local listeners on this machine"));

    let wifi_row = ActionRow::builder()
        .title("Wi-Fi (TLS + mDNS)")
        .subtitle("Port 27183 • Service _droidpad._tcp.local")
        .build();
    let wifi_switch = Switch::builder().active(true).valign(Align::Center).build();
    wifi_row.add_suffix(&wifi_switch);
    transport_group.add(&wifi_row);

    let usb_row = ActionRow::builder()
        .title("USB (ADB Tunnel)")
        .subtitle("Port 27184 on 127.0.0.1")
        .build();
    let usb_switch = Switch::builder().active(true).valign(Align::Center).build();
    usb_row.add_suffix(&usb_switch);
    transport_group.add(&usb_row);

    let bt_row = ActionRow::builder()
        .title("Bluetooth (RFCOMM)")
        .subtitle("UUID: 6e400001-b5a3-f393-e0a9-e50e24dcca9e")
        .build();
    let bt_switch = Switch::builder().active(true).valign(Align::Center).build();
    bt_row.add_suffix(&bt_switch);
    transport_group.add(&bt_row);

    page.add(&transport_group);

    // ── Emulation Group ────────────────────────────────────────────────────────
    let emu_group = PreferencesGroup::new();
    emu_group.set_title("Emulation Devices");
    emu_group.set_description(Some("Kernel virtual device drivers via /dev/uinput"));

    let kb_row = ActionRow::builder()
        .title("Virtual Keyboard")
        .subtitle("Linux evdev keyboard layout with 6KRO")
        .build();
    let kb_status = Label::builder()
        .label("Active")
        .css_classes(["success"])
        .valign(Align::Center)
        .build();
    kb_row.add_suffix(&kb_status);
    emu_group.add(&kb_row);

    let gp_row = ActionRow::builder()
        .title("Xbox 360 Gamepad")
        .subtitle("Vendor 0x045E • Product 0x028E (xpad driver compatible)")
        .build();
    let gp_status = Label::builder()
        .label("Active")
        .css_classes(["success"])
        .valign(Align::Center)
        .build();
    gp_row.add_suffix(&gp_status);
    emu_group.add(&gp_row);

    page.add(&emu_group);

    // ── About Group ────────────────────────────────────────────────────────────
    let about_group = PreferencesGroup::new();
    about_group.set_title("About");

    let ver_row = ActionRow::builder()
        .title("DroidPad Version")
        .subtitle("1.0.0 (Linux x86_64)")
        .build();
    about_group.add(&ver_row);

    let info_row = ActionRow::builder()
        .title("GitHub Repository")
        .subtitle("https://github.com/droidpad/droidpad")
        .build();
    about_group.add(&info_row);

    page.add(&about_group);

    window.add(&page);
    window.present();
}
