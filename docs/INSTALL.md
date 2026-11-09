# Installation Guide

DroidPad provides pre-built packages on [**GitHub Releases**](../../releases) for easy, zero-friction installation:
- 📱 **Android**: `droidpad.apk` (tap to install)
- 🐧 **Linux**: `DroidPad-x86_64.AppImage` (portable executable, runs on any modern distro)

---

## 🐧 Linux Host

### Method 1: AppImage (Recommended — No build or package dependencies required)

The AppImage bundles the daemon, Libadwaita / GTK4 desktop interface, icons, and dependencies in a single file.

1. **Download the AppImage**:
   Download `DroidPad-x86_64.AppImage` from the [GitHub Releases](../../releases) page.

2. **Make it executable & run**:
   ```bash
   chmod +x DroidPad-x86_64.AppImage
   ./DroidPad-x86_64.AppImage
   ```
   *Or right-click the file in your desktop file manager → Properties → Allow executing file as program, then double-click.*

3. **Configure input permissions (one-time setup)**:
   Linux requires permission to write to `/dev/uinput` to create virtual controllers.
   Run the included permission helper script (also available on Releases):
   ```bash
   curl -sSL -O https://raw.githubusercontent.com/souma/droidpad/main/packaging/setup-uinput.sh
   chmod +x setup-uinput.sh
   sudo ./setup-uinput.sh
   ```
   *Log out and back in once for group permissions to take effect.*

### Method 2: Headless Daemon (CLI / Server / Raspberry Pi)

If running without a desktop or in a background session:
```bash
# Download the standalone binary from Releases
chmod +x droidpad-daemon-linux-x86_64
./droidpad-daemon-linux-x86_64 --no-tray
```

### Method 3: Build from Source

```bash
# Install Rust
curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs | sh

# Install GTK4 & BlueZ development libraries
# Ubuntu / Debian:
sudo apt install -y libgtk-4-dev libadwaita-1-dev libbluetooth-dev libdbus-1-dev pkg-config
# Fedora:
sudo dnf install -y gtk4-devel libadwaita-devel bluez-libs-devel dbus-devel pkg-config
# Arch Linux:
sudo pacman -S gtk4 libadwaita bluez-libs dbus

# Build release binary
cd linux-daemon
cargo build --release

# Run
./target/release/droidpad-daemon
```

### Optional: Systemd Auto-start on Login

To launch DroidPad automatically in the background on login:
```bash
mkdir -p ~/.config/systemd/user
cp linux-daemon/packaging/droidpad.service ~/.config/systemd/user/
systemctl --user daemon-reload
systemctl --user enable --now droidpad
```

---

## 📱 Android App

### Method 1: Install APK from GitHub Releases (Recommended)

1. Open your Android phone's web browser and go to the [**GitHub Releases**](../../releases) page.
2. Download `droidpad.apk`.
3. Open the downloaded file in your notification panel or Files app.
4. Tap **Install** (if prompted, allow "Install unknown apps" for your browser/file manager).
5. Open **DroidPad** from your launcher!

### Method 2: Install via ADB

```bash
adb install droidpad.apk
```

### Method 3: Build APK from Source

```bash
cd android-app
# Build release APK
./gradlew assembleRelease
# The installable APK will be in app/build/outputs/apk/release/
```

---

## 🔌 Connection Setup

Once both the Linux daemon and Android app are installed:

| Connection | Setup Steps |
|------------|-------------|
| **Wi-Fi** | Connect phone and Linux PC to the same Wi-Fi network. Open DroidPad on phone, tap **Wi-Fi**, and select your PC from auto-discovery. |
| **Bluetooth** | Pair phone with your Linux PC in Android Bluetooth Settings first. Open DroidPad, tap **Bluetooth**, and select your PC. |
| **USB** | Plug phone into PC via USB cable. Enable **USB Debugging** on phone. Run on PC: `adb forward tcp:27184 tcp:27184`. Open DroidPad, tap **USB**. |

---

## 🛡️ Firewall Configuration

If Wi-Fi discovery does not connect, permit incoming traffic on TCP port **27183**:

```bash
# UFW (Ubuntu / Debian / Pop!_OS)
sudo ufw allow 27183/tcp

# firewalld (Fedora / RHEL / CentOS)
sudo firewall-cmd --permanent --add-port=27183/tcp
sudo firewall-cmd --reload
```
