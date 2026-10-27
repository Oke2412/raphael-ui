# Tensura Raphael - Workspace Rules & Development Standards

## 1. System Architecture & Components
- **Audio Pipeline & AI Backend** (Python): `main.py`, `server/ws_server.py`, `pipeline/manager.py`.
  - Runs local STT (Whisper), Wake Word ("Raphael" via openWakeWord), TTS (VieNeu/RVC), LLM (Ollama/remote), and Neo4j GraphRAG.
  - Exposes WebSocket server on `ws://127.0.0.1:8765`.
  - Logs to `logs/pipeline.log`.
- **Desktop Companion UI** (Tauri v2 + React): `tauri-desktop/`.
  - Replaces legacy Electron (`nang-cap-visual-great-sage/`).
  - Native binary: `tauri-desktop/src-tauri/target/debug/tensura-raphael.exe`.
  - Rust Backend: `tauri-desktop/src-tauri/src/lib.rs`.
  - Frontend: `tauri-desktop/src/components/sage-console.tsx`, `sage-core-canvas.tsx`.
  - Logs to `logs/desktop.log` and `logs/panic.log`.

---

## 2. Desktop Mode & Window Integrity Rules
- **Rule 1: Stealth Background Daemon by Default**:
  - The application MUST launch 100% invisibly in the system tray (`visible: false`, `skipTaskbar: true`).
  - NEVER pop up or flash the window on system boot or app launch unless explicitly passed `--overlay`, `--docked`, or `--sidedeck`.
- **Rule 2: Never use `set_fullscreen(true)` for Overlay**:
  - When switching to `"overlay"`, use borderless window bounds matching the monitor resolution:
    ```rust
    let _ = window.set_fullscreen(false);
    let _ = window.set_position(Position::Physical(*screen_pos));
    let _ = window.set_size(Size::Physical(*screen_size));
    let _ = window.set_always_on_top(true);
    ```
  - Using `set_fullscreen(true)` marks the window as a fullscreen exclusive app in Windows DWM, causing focus fights, window restoration bugs (revealing the 1280x800 box), and crash on click-through.
- **Rule 3: Fullscreen Interactive Overlay (No Click-Through)**:
  - `"overlay"` is a focused full-screen dashboard capturing mouse events natively without background hit-testing overhead or click-through conflicts.
  - Transparent borderless styling is maintained with decorations stripped and `resizable: false`.
- **Rule 4: Draggable Floating Widgets with Position Memory**:
  - Both Mini Orb (`docked`, 120px) and Sidedeck (`sidedeck`, 370x540) are freely draggable across the screen via pointer events.
  - Positions are remembered in `AppState` (`last_docked_x/y` and `last_sidedeck_x/y`) and restored when toggled.
  - Non-client DWM shadow is disabled (`DWMNCRP_DISABLED`) to avoid clipping box artifacts.

---

## 3. Testing & Verification Protocol
- After making changes to the desktop companion:
  1. Build frontend: `cmd.exe /c "npm run build"` inside `tauri-desktop`.
  2. Verify Rust compilation: `$env:PATH = "$env:USERPROFILE\.cargo\bin;$env:PATH"; cargo check` in `tauri-desktop/src-tauri`.
  3. Compile binary: `cargo build` in `tauri-desktop/src-tauri`.
  4. Test in debug console mode: Run `scripts/run_tauri_debug.bat` to see real-time logs and verify click-through and wake word.
  5. Check logs: Verify `logs/desktop.log` and `logs/pipeline.log` have 0 errors, and `logs/panic.log` is clean.

---

## 4. Multi-Agent & Backend Isolation Invariants
- **Rule 5: Zero-Touch Backend Isolation**:
  - The Backend codebase (remote server / Docker `sttap`) is STRICTLY UNTOUCHABLE.
  - The Frontend Agent must NEVER modify backend source code.
  - Architecture directive: **Thin Backend** (hardware sensors, raw cronjobs) + **Thick Frontend** (UI, voice pipeline, MCP tools, dynamic plugins).
- **Rule 6: Multi-Agent Collaboration Bridge**:
  - Two Antigravity agents operate concurrently: Frontend Agent (this workspace) and Backend Agent (remote server repo).
  - All inter-agent technical specifications, sensor schema requests, and cronjob registrations MUST be communicated asynchronously via `bridge/agent_chat.log`.
  - Format: `[YYYY-MM-DD HH:mm:ss][SENDER][TAG][ID] Message`.
- **Rule 7: Verification Before Completion**:
  - All project components are under active development.
  - NEVER mark tasks as completed in documentation, README, or responses without actual testing and verification.
- **Rule 8: Windows PowerShell CLI Safety**:
  - Never execute `.ps1` script wrappers directly on Windows PowerShell due to execution policy restrictions.
  - Always invoke Node and package managers via `cmd.exe /c "npx.cmd ..."` or `cmd.exe /c "npm.cmd ..."`.

