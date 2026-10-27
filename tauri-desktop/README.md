# Tensura Raphael — Desktop Companion (Tauri v2)

Windows desktop shell for the Raphael companion. React 19 + Vite frontend, Rust/Tauri v2 backend.
It replaces the legacy Electron app in `../nang-cap-visual-great-sage/`, targeting **<30 MB RAM** in stealth mode.

## Prerequisites (Windows)

- Node.js 20+ and npm
- Rust stable (MSVC toolchain) — `rustup default stable-x86_64-pc-windows-msvc`
- Visual Studio Build Tools with "Desktop development with C++"
- WebView2 runtime (preinstalled on Windows 10/11)

## Commands

```powershell
npm install          # install frontend dependencies
npm run dev          # Vite only, UI in a normal browser
npm run tauri dev    # full desktop app with Rust backend
npm run build        # TypeScript check + production frontend build
npm run tauri build  # Windows installer / executable
```

Build output: `src-tauri/target/release/` (`tensura-raphael.exe`, MSI/NSIS bundles).
`npm run lint` runs Oxlint.

## Runtime contract

- The UI connects to the Python WebSocket server at `ws://127.0.0.1:8765` (see `src/lib/sage-protocol.ts`).
- Start the backend from the repo root before the desktop app: `start_assistant.bat` (or `main.py`).
- Global hotkey: `Ctrl + Alt + Shift + \`.
- Window modes: `hidden` (default, invisible daemon), `overlay`, `docked`, `sidedeck`.
  Mode switching, DWM transparency, click-through, and window positioning are handled in `src-tauri/src/lib.rs`.

## Layout

```text
src/                  React UI (sage-console, canvas, control panel, dialogs)
src/lib/              WebSocket hook + protocol types
src-tauri/src/lib.rs  Tauri commands, window modes, tray, shortcuts, logging
src-tauri/icons/      App icons
```

## Notes

- `src-tauri/gen/` and `src-tauri/target/` are generated and git-ignored.
- The app is Windows-only: `lib.rs` uses Win32/DWM APIs directly.
- Logs are written to the repo-level `logs/` directory (`desktop.log`, `panic.log`).
