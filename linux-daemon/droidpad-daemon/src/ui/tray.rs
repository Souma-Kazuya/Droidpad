//! DroidPad Linux UI: Main Status & Tray Window.

use gtk4::glib;
use gtk4::prelude::*;
use gtk4::{Align, Box as GtkBox, Button, Image, Label, Orientation};
use libadwaita::prelude::*;
use libadwaita::{Application, HeaderBar, Window};
use tokio::sync::watch;

use crate::daemon::ConnectionState;
use crate::ui::settings::show_settings_window;

/// Construct the main status window displaying current connection and quick settings.
pub fn build_main_window(app: &Application, state_rx: watch::Receiver<ConnectionState>) {
    let window = Window::builder()
        .application(app)
        .title("DroidPad")
        .default_width(380)
        .default_height(280)
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

    let content = GtkBox::new(Orientation::Vertical, 16);
    content.set_margin_top(24);
    content.set_margin_bottom(24);
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
            }
        }
        glib::ControlFlow::Continue
    });

    root.append(&content);
    window.set_content(Some(&root));
    window.present();
}
