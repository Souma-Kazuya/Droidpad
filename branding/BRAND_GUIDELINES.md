# DroidPad — Brand & Logo Guidelines

## 1. The Logo System
- **Core Concept (The Synapse Controller)**: An ergonomic, dual-handled controller silhouette with negative-space D-pad, ABXY button cluster, and a central status node. It represents the ultra-low-latency bridge linking Android touch surfaces to the Linux input subsystem.
- **The Ecosystem**:
  - **Android Mobile App**: The mark rendered on dark slate `#0F1318` with glowing `#4ADE80` Mint Green accents for adaptive launchers.
  - **Linux Daemon**: The mark in high-contrast graphite `#171E27` for desktop app launchers (`.desktop`), and crisp monochrome glyphs for the Linux system tray.

---

## 2. Colour Palette

| Name | Role | HEX | RGB | Use Case |
|---|---|---|---|---|
| **Accent Mint Green** | Primary Brand Accent | `#4ADE80` | `74, 222, 128` | Controller symbol, active connections, HUD pill |
| **Dark Slate Background** | Primary Dark Background | `#0F1318` | `15, 19, 24` | Main screen backdrop, launcher squircle |
| **Dark Control Card** | Surface / Tile | `#171E27` | `23, 30, 39` | Linux desktop tile, dialog surfaces |
| **Control Element** | Neutral Mid-tone | `#232A35` | `35, 42, 53` | Secondary buttons, borders, keycaps |
| **Text White** | High-contrast Light | `#FFFFFF` | `255, 255, 255` | Reversed mark, white glyphs, titles |

---

## 3. Minimum Sizes & Scaling Rules

| Format | Minimum Pixel Size | Recommended File |
|---|---|---|
| **System Tray / Favicon** | 16 × 16 px | `dist/droidpad-favicon.svg`, `dist/favicon-16.png` |
| **Panel / Menu Icon** | 24 × 24 px / 32 × 32 px | `dist/droidpad-mono-4ade80-32.png` |
| **Linux Desktop Launcher** | 64 × 64 px / 128 × 128 px | `droidpad-linux-icon.svg` / `dist/droidpad-app-icon-128.png` |
| **Android Launcher** | 192 × 192 px / 512 × 512 px | `droidpad-android-icon.svg` / `dist/icon-512.png` |

---

## 4. Production Master Files

- **Vector Masters**:
  - [`droidpad-symbol.svg`](file:///home/souma/droidpad/branding/logos/droidpad-symbol.svg) — Standard 256×256 master mark.
  - [`droidpad-symbol-small.svg`](file:///home/souma/droidpad/branding/logos/droidpad-symbol-small.svg) — Optical cut with widened apertures for 16–24px.
  - [`droidpad-android-icon.svg`](file:///home/souma/droidpad/branding/logos/droidpad-android-icon.svg) — Full Android adaptive launcher icon.
  - [`droidpad-linux-icon.svg`](file:///home/souma/droidpad/branding/logos/droidpad-linux-icon.svg) — Full Linux desktop application icon.
- **Delivery Bundle (`branding/logos/dist/`)**:
  - `droidpad-white.svg` / `droidpad-black.svg` / `droidpad-mono-4ade80.svg`
  - Web & Favicon set: `favicon.ico`, `apple-touch-icon.png`, `icon-192.png`, `icon-512.png`, `site.webmanifest`
- **Interactive Presentation Board**:
  - [`presentation.html`](file:///home/souma/droidpad/branding/logos/presentation.html) (Mockups on README, terminal, mobile app icon, and website)

---

## 5. Usage Rules (Do's and Don'ts)
- **Do** use the reversed white or mint green mark on dark backgrounds (`#0F1318` or `#171E27`).
- **Do** use `droidpad-symbol-small.svg` for 16px to 24px tray bars to preserve internal button apertures.
- **Don't** distort, rotate, stretch, or alter the controller curvature.
- **Don't** add unapproved gradients, dropshadows, or 3D extrusions to the monochrome mark.
