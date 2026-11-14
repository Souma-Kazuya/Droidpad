//! DroidPad Linux UI: Splash Screen Window.

use gtk4::glib;
use gtk4::prelude::*;
use gtk4::{Align, Box as GtkBox, Image, Label, Orientation, ProgressBar};
use libadwaita::prelude::*;
use libadwaita::{Application, Window};
use std::cell::Cell;
use std::rc::Rc;
use tokio::sync::watch;

use crate::daemon::ConnectionState;
use crate::ui::tray::build_main_window;

/// Show an elegant splash screen on Linux launch, then transition into the main window.
pub fn show_splash_window(app: &Application, state_rx: watch::Receiver<ConnectionState>) {
    let splash = Window::builder()
        .application(app)
        .title("DroidPad")
        .default_width(340)
        .default_height(240)
        .resizable(false)
        .decorated(false)
        .build();

    let root = GtkBox::new(Orientation::Vertical, 14);
    root.set_margin_top(28);
    root.set_margin_bottom(28);
    root.set_margin_start(32);
    root.set_margin_end(32);
    root.set_valign(Align::Center);
    root.set_halign(Align::Center);

    let icon = Image::builder()
        .icon_name("droidpad")
        .pixel_size(72)
        .build();
    root.append(&icon);

    let title = Label::builder()
        .label("DroidPad")
        .css_classes(["title-1"])
        .build();
    root.append(&title);

    let subtitle = Label::builder()
        .label("Initializing Linux Daemon & Virtual Devices...")
        .css_classes(["dim-label"])
        .build();
    root.append(&subtitle);

    let progress = ProgressBar::builder().fraction(0.0).build();
    progress.set_width_request(200);
    root.append(&progress);

    splash.set_content(Some(&root));
    splash.present();

    let app_clone = app.clone();
    let splash_clone = splash.clone();
    let state_rx_clone = state_rx.clone();

    let progress_val = Rc::new(Cell::new(0.0f64));

    glib::timeout_add_local(std::time::Duration::from_millis(50), move || {
        let current = progress_val.get() + 0.04;
        progress_val.set(current);
        progress.set_fraction(current.min(1.0));

        if current >= 1.0 {
            // Close splash and present main window
            splash_clone.close();
            build_main_window(&app_clone, state_rx_clone.clone());
            glib::ControlFlow::Break
        } else {
            glib::ControlFlow::Continue
        }
    });
}
