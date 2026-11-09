#!/usr/bin/env bash
# DroidPad — /dev/uinput Permissions Setup Helper
# Configures udev rules so non-root users can create virtual keyboards and gamepads.

set -euo pipefail

echo "=========================================================="
echo "  DroidPad — Linux Input Permissions Setup"
echo "=========================================================="
echo

if [ "$(id -u)" -ne 0 ]; then
    echo "This script needs superuser privileges to install the udev rule."
    echo "Re-running with sudo..."
    exec sudo "$0" "$@"
fi

TARGET_USER="${SUDO_USER:-$USER}"

echo "1. Installing udev rule to /etc/udev/rules.d/99-uinput.rules..."
cat << 'EOF' > /etc/udev/rules.d/99-uinput.rules
# Allow users in 'input' group to access /dev/uinput for DroidPad
KERNEL=="uinput", GROUP="input", MODE="0660", OPTIONS+="static_node=uinput"
EOF

echo "2. Loading uinput kernel module..."
modprobe uinput || true

# Ensure uinput loads on boot
if [ -d /etc/modules-load.d ]; then
    echo "uinput" > /etc/modules-load.d/uinput.conf
fi

echo "3. Reloading udev rules..."
udevadm control --reload-rules || true
udevadm trigger /dev/uinput 2>/dev/null || true

echo "4. Adding user '$TARGET_USER' to 'input' group..."
groupadd -f input
usermod -aG input "$TARGET_USER"

echo
echo "=========================================================="
echo "  Configuration Complete! "
echo "=========================================================="
echo "NOTE: If you just added '$TARGET_USER' to the 'input' group,"
echo "you may need to log out and log back in (or run 'newgrp input')"
echo "for group membership to take effect in your current session."
echo
echo "You can now run DroidPad without sudo!"
echo "=========================================================="
