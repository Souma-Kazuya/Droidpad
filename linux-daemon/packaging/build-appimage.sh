#!/usr/bin/env bash
# DroidPad — Linux AppImage Builder
# Builds a portable, self-contained DroidPad-x86_64.AppImage

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
DAEMON_DIR="${REPO_ROOT}/linux-daemon"
BUILD_DIR="${DAEMON_DIR}/target/appimage-build"
APPDIR="${BUILD_DIR}/AppDir"
OUTPUT_DIR="${1:-${DAEMON_DIR}/target/release}"
OUTPUT_FILE="${OUTPUT_DIR}/DroidPad-x86_64.AppImage"

echo "=== Building DroidPad Release Binary ==="
cd "${DAEMON_DIR}"
cargo build --release

echo "=== Setting up AppDir Structure ==="
rm -rf "${APPDIR}"
mkdir -p "${APPDIR}/usr/bin"
mkdir -p "${APPDIR}/usr/share/applications"
mkdir -p "${APPDIR}/usr/share/icons/hicolor/scalable/apps"
mkdir -p "${APPDIR}/usr/share/icons/hicolor/256x256/apps"
mkdir -p "${APPDIR}/usr/share/glib-2.0/schemas"
mkdir -p "${OUTPUT_DIR}"

# Copy binary
cp "${DAEMON_DIR}/target/release/droidpad-daemon" "${APPDIR}/usr/bin/droidpad-daemon"
chmod +x "${APPDIR}/usr/bin/droidpad-daemon"

# Copy desktop file
cp "${SCRIPT_DIR}/droidpad.desktop" "${APPDIR}/usr/share/applications/droidpad.desktop"
cp "${SCRIPT_DIR}/droidpad.desktop" "${APPDIR}/droidpad.desktop"

# Copy icons
cp "${SCRIPT_DIR}/droidpad.svg" "${APPDIR}/usr/share/icons/hicolor/scalable/apps/droidpad.svg"
cp "${SCRIPT_DIR}/droidpad.svg" "${APPDIR}/droidpad.svg"

if [ -f "${SCRIPT_DIR}/droidpad.png" ]; then
    cp "${SCRIPT_DIR}/droidpad.png" "${APPDIR}/usr/share/icons/hicolor/256x256/apps/droidpad.png"
    cp "${SCRIPT_DIR}/droidpad.png" "${APPDIR}/droidpad.png"
    cp "${SCRIPT_DIR}/droidpad.png" "${APPDIR}/.DirIcon"
fi

# Compile GSettings schemas into AppDir
echo "=== Bundling GSettings Schemas ==="
if [ -d /usr/share/glib-2.0/schemas ]; then
    cp -r /usr/share/glib-2.0/schemas/* "${APPDIR}/usr/share/glib-2.0/schemas/" 2>/dev/null || true
    glib-compile-schemas "${APPDIR}/usr/share/glib-2.0/schemas" || true
fi

# Create standard AppRun launcher
cat << 'EOF' > "${APPDIR}/AppRun"
#!/bin/sh
HERE="$(dirname "$(readlink -f "${0}")")"
export PATH="${HERE}/usr/bin:${PATH}"
export LD_LIBRARY_PATH="${HERE}/usr/lib:${HERE}/usr/lib/x86_64-linux-gnu:${LD_LIBRARY_PATH}"
export XDG_DATA_DIRS="${HERE}/usr/share:${XDG_DATA_DIRS:-/usr/local/share:/usr/share}"
export GSETTINGS_SCHEMA_DIR="${HERE}/usr/share/glib-2.0/schemas:${GSETTINGS_SCHEMA_DIR}"
if [ -d "${HERE}/usr/lib/gtk-4.0" ]; then
    export GTK_PATH="${HERE}/usr/lib/gtk-4.0:${GTK_PATH}"
fi
exec "${HERE}/usr/bin/droidpad-daemon" "$@"
EOF
chmod +x "${APPDIR}/AppRun"

echo "=== Packaging AppImage ==="
# Check for linuxdeploy or appimagetool
TOOLS_DIR="${BUILD_DIR}/tools"
mkdir -p "${TOOLS_DIR}"

if command -v linuxdeploy >/dev/null 2>&1; then
    LINUXDEPLOY="linuxdeploy"
else
    LINUXDEPLOY="${TOOLS_DIR}/linuxdeploy-x86_64.AppImage"
    if [ ! -f "${LINUXDEPLOY}" ]; then
        echo "Downloading linuxdeploy..."
        curl -sSL -o "${LINUXDEPLOY}" "https://github.com/linuxdeploy/linuxdeploy/releases/download/continuous/linuxdeploy-x86_64.AppImage"
        chmod +x "${LINUXDEPLOY}"
    fi
fi

GTK_PLUGIN="${TOOLS_DIR}/linuxdeploy-plugin-gtk.sh"
if [ ! -f "${GTK_PLUGIN}" ]; then
    echo "Downloading linuxdeploy-plugin-gtk..."
    curl -sSL -o "${GTK_PLUGIN}" "https://raw.githubusercontent.com/linuxdeploy/linuxdeploy-plugin-gtk/master/linuxdeploy-plugin-gtk.sh" || true
    chmod +x "${GTK_PLUGIN}" || true
fi

# Configure linuxdeploy options
export OUTPUT="${OUTPUT_FILE}"
export LDAI_OUTPUT="${OUTPUT_FILE}"
export NO_STRIP=1
export ARCH="x86_64"

# Attempt AppImage generation with linuxdeploy
if [ -x "${LINUXDEPLOY}" ]; then
    echo "Running linuxdeploy to package AppImage..."
    export PATH="${TOOLS_DIR}:${PATH}"
    # If in container/CI without FUSE, extract linuxdeploy
    if [ "${APPIMAGE_EXTRACT_AND_RUN:-0}" = "1" ] || [ -n "${GITHUB_ACTIONS:-}" ]; then
        export APPIMAGE_EXTRACT_AND_RUN=1
    fi

    "${LINUXDEPLOY}" --appdir "${APPDIR}" --desktop-file "${APPDIR}/droidpad.desktop" --icon-file "${APPDIR}/droidpad.svg" --output appimage || {
        echo "linuxdeploy returned $?, attempting fallback with appimagetool..."
        APPIMAGETOOL="${TOOLS_DIR}/appimagetool-x86_64.AppImage"
        if [ ! -f "${APPIMAGETOOL}" ]; then
            curl -sSL -o "${APPIMAGETOOL}" "https://github.com/AppImage/AppImageKit/releases/download/continuous/appimagetool-x86_64.AppImage"
            chmod +x "${APPIMAGETOOL}"
        fi
        ARCH=x86_64 "${APPIMAGETOOL}" "${APPDIR}" "${OUTPUT_FILE}"
    }
fi

if [ -f "${OUTPUT_FILE}" ]; then
    chmod +x "${OUTPUT_FILE}"
    echo "SUCCESS: AppImage generated at: ${OUTPUT_FILE}"
else
    echo "AppDir prepared at: ${APPDIR}"
fi
