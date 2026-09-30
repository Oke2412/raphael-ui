use std::sync::Mutex;
use tauri::{
    menu::{Menu, MenuItem, PredefinedMenuItem},
    tray::{MouseButton, MouseButtonState, TrayIconBuilder, TrayIconEvent},
    AppHandle, Emitter, Manager, PhysicalPosition, PhysicalSize, Position, Size, State,
};
use tauri_plugin_global_shortcut::{Code, GlobalShortcutExt, Modifiers, Shortcut, ShortcutState};

#[derive(serde::Deserialize, serde::Serialize, Clone, Copy, Debug)]
pub struct PhysicalRect {
    pub x: i32,
    pub y: i32,
    pub width: i32,
    pub height: i32,
}

#[derive(Default)]
pub struct AppState {
    pub current_mode: Mutex<String>,
    pub last_docked_x: Mutex<Option<i32>>,
    pub last_docked_y: Mutex<Option<i32>>,
    pub last_sidedeck_x: Mutex<Option<i32>>,
    pub last_sidedeck_y: Mutex<Option<i32>>,
    pub interactive_rects: Mutex<Vec<PhysicalRect>>,
    pub overlay_origin: Mutex<PhysicalPosition<i32>>,
}

#[link(name = "user32")]
extern "system" {
    fn GetWindowLongW(hwnd: *mut std::ffi::c_void, n_index: i32) -> i32;
    fn SetWindowLongW(hwnd: *mut std::ffi::c_void, n_index: i32, new_long: i32) -> i32;
    fn SetWindowPos(
        hwnd: *mut std::ffi::c_void,
        hwnd_insert_after: *mut std::ffi::c_void,
        x: i32,
        y: i32,
        cx: i32,
        cy: i32,
        u_flags: u32,
    ) -> i32;
}

#[link(name = "dwmapi")]
extern "system" {
    fn DwmSetWindowAttribute(
        hwnd: *mut std::ffi::c_void,
        dw_attribute: u32,
        pv_attribute: *const std::ffi::c_void,
        cb_attribute: u32,
    ) -> i32;
}

const GWL_STYLE: i32 = -16;
const GWL_EXSTYLE: i32 = -20;
const WS_EX_TRANSPARENT: i32 = 0x00000020;
const WS_CAPTION: i32 = 0x00C00000;
const WS_THICKFRAME: i32 = 0x00040000;
const WS_SYSMENU: i32 = 0x00080000;
const WS_MINIMIZEBOX: i32 = 0x00020000;
const WS_MAXIMIZEBOX: i32 = 0x00010000;

const SWP_NOSIZE: u32 = 0x0001;
const SWP_NOMOVE: u32 = 0x0002;
const SWP_NOZORDER: u32 = 0x0004;
const SWP_NOACTIVATE: u32 = 0x0010;
const SWP_FRAMECHANGED: u32 = 0x0020;

fn strip_window_decorations(hwnd: *mut std::ffi::c_void) {
    if hwnd.is_null() {
        return;
    }
    unsafe {
        let cur = GetWindowLongW(hwnd, GWL_STYLE);
        let unwanted = WS_CAPTION | WS_THICKFRAME | WS_SYSMENU | WS_MINIMIZEBOX | WS_MAXIMIZEBOX;
        let cleaned = cur & !unwanted;
        if cleaned != cur {
            SetWindowLongW(hwnd, GWL_STYLE, cleaned);
            SetWindowPos(
                hwnd,
                std::ptr::null_mut(),
                0, 0, 0, 0,
                SWP_NOSIZE | SWP_NOMOVE | SWP_NOZORDER | SWP_NOACTIVATE | SWP_FRAMECHANGED,
            );
        }
    }
}

fn set_window_click_through(hwnd: *mut std::ffi::c_void, transparent: bool) {
    if hwnd.is_null() {
        return;
    }
    unsafe {
        let cur = GetWindowLongW(hwnd, GWL_EXSTYLE);
        let next = if transparent {
            cur | WS_EX_TRANSPARENT
        } else {
            cur & !WS_EX_TRANSPARENT
        };
        if next != cur {
            SetWindowLongW(hwnd, GWL_EXSTYLE, next);
        }
    }
}

fn disable_dwm_shadow(hwnd: *mut std::ffi::c_void) {
    if hwnd.is_null() {
        return;
    }
    unsafe {
        let policy: u32 = 1; // DWMNCRP_DISABLED
        let _ = DwmSetWindowAttribute(
            hwnd,
            2, // DWMWA_NCRENDERING_POLICY
            &policy as *const u32 as *const std::ffi::c_void,
            std::mem::size_of::<u32>() as u32,
        );
    }
}

fn apply_mode(app: &AppHandle, state: &AppState, mode: &str) -> Result<(), String> {
    let window = app.get_webview_window("main").ok_or("No main window")?;
    {
        let mut cur = state.current_mode.lock().unwrap();
        *cur = mode.to_string();
    }
    log::info!("[Window] Applying visual mode: {}", mode);

    // Gửi sự kiện cho giao diện React đồng bộ trạng thái
    let _ = app.emit("set-mode-from-backend", mode);
    let _ = window.set_shadow(false);

    let hwnd_ptr = match window.hwnd() {
        Ok(h) => h.0 as *mut std::ffi::c_void,
        Err(_) => std::ptr::null_mut(),
    };

    disable_dwm_shadow(hwnd_ptr);
    strip_window_decorations(hwnd_ptr);

    if mode == "hidden" {
        let _ = window.hide();
        let _ = window.set_skip_taskbar(true);
        return Ok(());
    }

    let _ = window.set_skip_taskbar(false);

    let monitor = window
        .current_monitor()
        .map_err(|e| e.to_string())?
        .or_else(|| window.primary_monitor().ok().flatten())
        .ok_or("No monitor found")?;

    let screen_pos = monitor.position();
    let screen_size = monitor.size();
    let scale_factor = monitor.scale_factor();

    match mode {
        "docked" => {
            let _ = window.set_fullscreen(false);
            let orb_box_logical = 120.0f64;
            let orb_box = (orb_box_logical * scale_factor).round() as u32;
            let margin = (20.0f64 * scale_factor).round() as i32;
            let default_x = screen_pos.x + screen_size.width as i32 - orb_box as i32 - margin;
            let default_y = screen_pos.y + screen_size.height as i32 - orb_box as i32 - margin;

            let x = state.last_docked_x.lock().unwrap().unwrap_or(default_x);
            let y = state.last_docked_y.lock().unwrap().unwrap_or(default_y);

            let _ = window.set_size(Size::Physical(PhysicalSize {
                width: orb_box,
                height: orb_box,
            }));
            let _ = window.set_position(Position::Physical(PhysicalPosition { x, y }));
            strip_window_decorations(hwnd_ptr);
            set_window_click_through(hwnd_ptr, false);
            let _ = window.set_always_on_top(true);
            let _ = window.show();
            let _ = window.set_focus();
        }
        "sidedeck" => {
            let _ = window.set_fullscreen(false);
            let width = (370.0f64 * scale_factor).round() as u32;
            let height = (540.0f64 * scale_factor).round() as u32;
            let margin_x = (16.0f64 * scale_factor).round() as i32;
            let margin_y = (16.0f64 * scale_factor).round() as i32;
            let default_x = screen_pos.x + screen_size.width as i32 - width as i32 - margin_x;
            let default_y = screen_pos.y + screen_size.height as i32 - height as i32 - margin_y;

            let x = state.last_sidedeck_x.lock().unwrap().unwrap_or(default_x);
            let y = state.last_sidedeck_y.lock().unwrap().unwrap_or(default_y);

            let _ = window.set_size(Size::Physical(PhysicalSize { width, height }));
            let _ = window.set_position(Position::Physical(PhysicalPosition { x, y }));
            strip_window_decorations(hwnd_ptr);
            set_window_click_through(hwnd_ptr, false);
            let _ = window.set_always_on_top(true);
            let _ = window.show();
            let _ = window.set_focus();
        }
        "overlay" => {
            {
                let mut origin = state.overlay_origin.lock().unwrap();
                *origin = *screen_pos;
            }
            let _ = window.set_fullscreen(false);
            let _ = window.set_position(Position::Physical(*screen_pos));
            let _ = window.set_size(Size::Physical(*screen_size));
            strip_window_decorations(hwnd_ptr);
            set_window_click_through(hwnd_ptr, false);
            let _ = window.set_always_on_top(true);
            let _ = window.show();
            let _ = window.set_focus();
        }
        _ => {}
    }

    Ok(())
}

#[tauri::command]
fn set_visual_mode(app: AppHandle, state: State<'_, AppState>, mode: String) -> Result<(), String> {
    apply_mode(&app, &state, &mode)
}

#[tauri::command]
fn move_window_by(app: AppHandle, state: State<'_, AppState>, dx: i32, dy: i32) -> Result<(), String> {
    let window = app.get_webview_window("main").ok_or("No main window")?;
    let cur_pos = window.outer_position().map_err(|e| e.to_string())?;
    let new_x = cur_pos.x + dx;
    let new_y = cur_pos.y + dy;

    let _ = window.set_position(Position::Physical(PhysicalPosition {
        x: new_x,
        y: new_y,
    }));

    let cur = state.current_mode.lock().unwrap();
    if *cur == "docked" {
        *state.last_docked_x.lock().unwrap() = Some(new_x);
        *state.last_docked_y.lock().unwrap() = Some(new_y);
    } else if *cur == "sidedeck" {
        *state.last_sidedeck_x.lock().unwrap() = Some(new_x);
        *state.last_sidedeck_y.lock().unwrap() = Some(new_y);
    }
    Ok(())
}

#[tauri::command]
fn set_ignore_mouse_events(app: AppHandle, ignore: bool) -> Result<(), String> {
    if let Some(window) = app.get_webview_window("main") {
        if let Ok(hwnd) = window.hwnd() {
            set_window_click_through(hwnd.0 as *mut std::ffi::c_void, ignore);
        }
    }
    Ok(())
}

#[tauri::command]
fn update_interactive_rects(state: State<'_, AppState>, rects: Vec<PhysicalRect>) {
    let mut stored = state.interactive_rects.lock().unwrap();
    *stored = rects;
}

#[tauri::command]
fn quit_app(app: AppHandle) {
    app.exit(0);
}

#[tauri::command]
fn stop_assistant(app: AppHandle) {
    let _ = std::process::Command::new("cmd")
        .args(["/c", "..\\stop_assistant.bat"])
        .spawn();
    app.exit(0);
}

fn get_logs_dir() -> std::path::PathBuf {
    if std::path::Path::new("logs").is_dir() {
        return std::path::PathBuf::from("logs");
    }
    if let Ok(exe) = std::env::current_exe() {
        let mut cur = exe.as_path();
        while let Some(parent) = cur.parent() {
            let candidate = parent.join("logs");
            if candidate.is_dir() {
                return candidate;
            }
            cur = parent;
        }
    }
    std::path::PathBuf::from("logs")
}

#[cfg_attr(mobile, tauri::mobile_entry_point)]
pub fn run() {
    let logs_dir = get_logs_dir();
    let _ = std::fs::create_dir_all(&logs_dir);

    // 1. Hook bắt lỗi Crash/Panic tự động ghi vào logs\panic.log
    let panic_log_path = logs_dir.join("panic.log");
    std::panic::set_hook(Box::new(move |info| {
        eprintln!("[FATAL PANIC] {:?}", info);
        if let Ok(mut f) = std::fs::OpenOptions::new()
            .create(true)
            .append(true)
            .open(&panic_log_path)
        {
            use std::io::Write;
            let _ = writeln!(f, "[PANIC] {:?}", info);
        }
    }));

    let app_state = AppState::default();

    // 2. Cấu hình logger ghi ra cả console lẫn file logs\desktop.log
    let log_plugin = tauri_plugin_log::Builder::new()
        .targets([
            tauri_plugin_log::Target::new(tauri_plugin_log::TargetKind::Stdout),
            tauri_plugin_log::Target::new(tauri_plugin_log::TargetKind::Folder {
                path: logs_dir,
                file_name: Some("desktop".into()),
            }),
        ])
        .level(log::LevelFilter::Info)
        .build();

    tauri::Builder::default()
        .manage(app_state)
        .plugin(log_plugin)
        .plugin(tauri_plugin_global_shortcut::Builder::new().build())
        .plugin(tauri_plugin_shell::init())
        .setup(|app| {
            let handle = app.handle().clone();

            // Khởi tạo khay hệ thống (System Tray)
            let item_overlay = MenuItem::with_id(app, "tray_overlay", "🔮 Đánh thức Raphael (Mở Overlay)", true, None::<&str>)?;
            let item_hidden = MenuItem::with_id(app, "tray_hidden", "👁️ Ẩn hoàn toàn (Chạy ngầm)", true, None::<&str>)?;
            let item_docked = MenuItem::with_id(app, "tray_docked", "🔵 Chế độ góc (Mini Orb)", true, None::<&str>)?;
            let item_sidedeck = MenuItem::with_id(app, "tray_sidedeck", "📱 Thanh bên (Sidedeck Panel)", true, None::<&str>)?;
            let item_quit = MenuItem::with_id(app, "tray_quit", "❌ Thoát hoàn toàn", true, None::<&str>)?;

            let tray_menu = Menu::with_items(
                app,
                &[
                    &item_overlay,
                    &item_hidden,
                    &PredefinedMenuItem::separator(app)?,
                    &item_docked,
                    &item_sidedeck,
                    &PredefinedMenuItem::separator(app)?,
                    &item_quit,
                ],
            )?;

            let _tray = TrayIconBuilder::new()
                .icon(app.default_window_icon().unwrap().clone())
                .tooltip("Tensura Raphael (Nói \"Raphael\" hoặc Ctrl+Alt+Shift+\\)")
                .menu(&tray_menu)
                .on_menu_event(move |app, event| {
                    let state = app.state::<AppState>();
                    match event.id.as_ref() {
                        "tray_overlay" => {
                            let _ = apply_mode(app, &state, "overlay");
                        }
                        "tray_hidden" => {
                            let _ = apply_mode(app, &state, "hidden");
                        }
                        "tray_docked" => {
                            let _ = apply_mode(app, &state, "docked");
                        }
                        "tray_sidedeck" => {
                            let _ = apply_mode(app, &state, "sidedeck");
                        }
                        "tray_quit" => {
                            app.exit(0);
                        }
                        _ => {}
                    }
                })
                .on_tray_icon_event(|tray, event| {
                    if let TrayIconEvent::Click {
                        button: MouseButton::Left,
                        button_state: MouseButtonState::Up,
                        ..
                    } = event
                    {
                        let app = tray.app_handle();
                        let state = app.state::<AppState>();
                        let is_hidden = {
                            let cur = state.current_mode.lock().unwrap();
                            *cur == "hidden"
                        };
                        if is_hidden {
                            let _ = apply_mode(app, &state, "overlay");
                        } else {
                            let _ = apply_mode(app, &state, "hidden");
                        }
                    }
                })
                .build(app)?;

            // Đăng ký Phím tắt Toàn Cục: Ctrl+Alt+Shift+\ (Type-safe)
            let shortcut = Shortcut::new(Some(Modifiers::CONTROL | Modifiers::ALT | Modifiers::SHIFT), Code::Backslash);
            let h_shortcut = handle.clone();
            let _ = app.global_shortcut().on_shortcut(shortcut, move |_app, _shortcut, event| {
                if event.state == ShortcutState::Pressed {
                    let state = h_shortcut.state::<AppState>();
                    let is_hidden = {
                        let cur = state.current_mode.lock().unwrap();
                        *cur == "hidden"
                    };
                    if is_hidden {
                        let _ = apply_mode(&h_shortcut, &state, "overlay");
                    } else {
                        let _ = apply_mode(&h_shortcut, &state, "hidden");
                    }
                }
            });

            // Đọc cờ khởi động: mặc định chạy ngầm (hidden) 100% trong khay hệ thống, trừ khi có cờ gọi hiển thị
            let args: Vec<String> = std::env::args().collect();
            let start_mode = if args.iter().any(|a| a == "--overlay") {
                "overlay"
            } else if args.iter().any(|a| a == "--docked") {
                "docked"
            } else if args.iter().any(|a| a == "--sidedeck") {
                "sidedeck"
            } else {
                "hidden"
            };

            let state = app.state::<AppState>();
            let _ = apply_mode(app.handle(), &state, start_mode);

            // Tắt shadow mặc định
            let _ = app.get_webview_window("main").map(|w| {
                let _ = w.set_shadow(false);
            });

            Ok(())
        })
        .invoke_handler(tauri::generate_handler![
            set_visual_mode,
            move_window_by,
            set_ignore_mouse_events,
            update_interactive_rects,
            quit_app,
            stop_assistant
        ])
        .run(tauri::generate_context!())
        .expect("error while building tauri application");
}
