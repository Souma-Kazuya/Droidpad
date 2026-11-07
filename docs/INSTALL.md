# Installation Guide

## Linux Host

### Requirements

| Requirement | Notes |
|-------------|-------|
| Linux kernel ≥ 5.4 | uinput module must be loadable |
| `uinput` module | Usually pre-loaded; run `modprobe uinput` if not |
| BlueZ ≥ 5.50 | For Bluetooth transport |
| GTK4 + libadwaita | For the UI (optional — `--no-tray` to skip) |

### Pre-built packages

```bash
# Ubuntu / Debian
sudo dpkg -i droidpad_1.0.0_amd64.deb
sudo systemctl --user enable --now droidpad

# Fedora / RHEL
sudo rpm -i droidpad-1.0.0.x86_64.rpm
systemctl --user enable --now droidpad

# Arch Linux (AUR)
yay -S droidpad

# Flatpak
flatpak install flathub com.droidpad.DroidPad
```

### Build from source

```bash
# Install Rust
curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs | sh

# Install GTK4 dev libs (Ubuntu/Debian)
sudo apt install libgtk-4-dev libadwaita-1-dev libbluetooth-dev pkg-config

# Install GTK4 dev libs (Fedora)
sudo dnf install gtk4-devel libadwaita-devel bluez-libs-devel pkg-config

# Build
cd linux-daemon
cargo build --release

# Install binary
sudo cp target/release/droidpad-daemon /usr/local/bin/droidpad

# Install systemd service
cp packaging/droidpad.service ~/.config/systemd/user/
systemctl --user daemon-reload
systemctl --user enable --now droidpad
```

### uinput permissions

By default `/dev/uinput` requires root. Add yourself to the `input` group or create a udev rule:

```bash
# Option 1: udev rule (recommended)
echo 'KERNEL=="uinput", GROUP="input", MODE="0660"' | \
  sudo tee /etc/udev/rules.d/99-uinput.rules
sudo udevadm control --reload && sudo udevadm trigger
sudo usermod -aG input $USER
# Log out and back in

# Option 2: Run daemon with sudo (not recommended for production)
sudo droidpad-daemon
```

---

## Android App

### Requirements
- Android 8.0 (API 26) or higher
- Bluetooth Classic enabled (for BT transport)
- USB debugging enabled (for USB transport)
- Same Wi-Fi network as host (for Wi-Fi transport)

### Install APK

```bash
# Via ADB
adb install droidpad-1.0.0.apk

# Or sideload manually:
# Transfer APK to phone → open Files app → tap APK → install
# (Enable "Install from unknown sources" in Settings → Apps → Special access)
```

### USB Transport Setup

1. Connect Android to Linux PC via USB cable
2. Enable USB debugging on Android (Settings → Developer Options → USB Debugging)
3. Trust the PC fingerprint on Android when prompted
4. Run on the Linux PC:
   ```bash
   adb forward tcp:27184 tcp:27184
   ```
5. In DroidPad app, tap **USB** to connect

---

## Firewall Configuration

For Wi-Fi transport, allow incoming TCP on port 27183:

```bash
# UFW (Ubuntu)
sudo ufw allow 27183/tcp

# firewalld (Fedora)
sudo firewall-cmd --permanent --add-port=27183/tcp
sudo firewall-cmd --reload

# iptables
sudo iptables -A INPUT -p tcp --dport 27183 -j ACCEPT
```

---

## Troubleshooting

| Problem | Fix |
|---------|-----|
| `Permission denied: /dev/uinput` | Add udev rule (see above) |
| Gamepad not recognized in game | Check `evdev` listing: `evtest /dev/input/eventN` |
| Bluetooth not connecting | Pair device in OS Settings first |
| Wi-Fi device not appearing | Check firewall; ensure same subnet |
| USB transport refused | Run `adb forward` command; check USB debugging |
| High latency on Wi-Fi | Disable Wi-Fi power saving: `iwconfig wlan0 power off` |
