// Tensura (Raphael) - Native Windows Desktop Overlay
// True Transparency, Frameless, Always-on-top, System Tray & Taskbar App
// Architectural Minimalist & Stealth Background Daemon

const { app, BrowserWindow, screen, Tray, Menu, globalShortcut, ipcMain, Notification } = require('electron');
const path = require('path');
const fs = require('fs');
const http = require('http');
const { exec, execSync } = require('child_process');

let mainWindow = null;
let tray = null;
let staticServer = null;
let isQuitting = false;
let currentMode = 'hidden';

// Single-instance lock
const gotTheLock = app.requestSingleInstanceLock();
if (!gotTheLock) {
  app.quit();
} else {
  app.on('second-instance', () => {
    if (mainWindow) {
      if (mainWindow.isMinimized()) mainWindow.restore();
      applyWindowMode('overlay');
      if (mainWindow.webContents) {
        mainWindow.webContents.send('set-mode-from-main', 'overlay');
      }
    }
  });

  app.whenReady().then(() => {
    // 300ms delay to let Windows DWM compositor initialize transparency properly
    setTimeout(createDesktopApp, 300);
  });
}

function startStaticServer(outDir) {
  const mimeTypes = {
    '.html': 'text/html; charset=utf-8',
    '.js': 'text/javascript; charset=utf-8',
    '.css': 'text/css; charset=utf-8',
    '.json': 'application/json; charset=utf-8',
    '.png': 'image/png',
    '.jpg': 'image/jpeg',
    '.svg': 'image/svg+xml',
    '.woff2': 'font/woff2',
    '.woff': 'font/woff',
    '.ttf': 'font/ttf',
    '.ico': 'image/x-icon',
  };

  return new Promise((resolve) => {
    const server = http.createServer((req, res) => {
      let reqPath = req.url.split('?')[0];
      if (reqPath === '/' || reqPath === '') reqPath = '/index.html';
      const filePath = path.join(outDir, decodeURIComponent(reqPath));

      fs.readFile(filePath, (err, data) => {
        if (err) {
          const reqExt = path.extname(reqPath);
          if (!reqExt || reqExt === '.html') {
            fs.readFile(path.join(outDir, 'index.html'), (errIndex, indexData) => {
              if (errIndex) {
                res.writeHead(404, { 'Content-Type': 'text/plain' });
                res.end('Not Found');
              } else {
                res.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' });
                res.end(indexData);
              }
            });
            return;
          }
          res.writeHead(404, { 'Content-Type': 'text/plain' });
          res.end('Not Found');
          return;
        }
        const ext = path.extname(filePath).toLowerCase();
        const contentType = mimeTypes[ext] || 'application/octet-stream';
        res.writeHead(200, { 'Content-Type': contentType });
        res.end(data);
      });
    });

    server.listen(0, '127.0.0.1', () => {
      const port = server.address().port;
      resolve({ server, port });
    });
  });
}

function getStartupShortcutPath() {
  const startupFolder = path.join(
    process.env.APPDATA || '',
    'Microsoft',
    'Windows',
    'Start Menu',
    'Programs',
    'Startup'
  );
  return path.join(startupFolder, 'TensuraRaphael.lnk');
}

async function createDesktopApp() {
  const primaryDisplay = screen.getPrimaryDisplay();
  const { workArea, bounds } = primaryDisplay;

  // Mặc định khởi chạy ngầm hoàn toàn (hidden), trừ khi có cờ gọi hiển thị
  const startMode = process.argv.includes('--overlay')
    ? 'overlay'
    : process.argv.includes('--docked')
    ? 'docked'
    : process.argv.includes('--sidedeck')
    ? 'sidedeck'
    : 'hidden';
  currentMode = startMode;

  mainWindow = new BrowserWindow({
    title: 'Tensura Raphael - AI Assistant',
    icon: path.join(__dirname, '..', 'public', 'icon-light-32x32.png'),
    width: bounds.width,
    height: bounds.height,
    x: bounds.x,
    y: bounds.y,
    frame: false,
    transparent: true,
    alwaysOnTop: true,
    hasShadow: false,
    resizable: true,
    skipTaskbar: startMode === 'hidden',
    show: startMode !== 'hidden',
    webPreferences: {
      preload: path.join(__dirname, 'preload.cjs'),
      nodeIntegration: false,
      contextIsolation: true,
      webSecurity: false,
    },
  });

  mainWindow.setAlwaysOnTop(true, 'screen-saver', 1);
  mainWindow.setVisibleOnAllWorkspaces(true, { visibleOnFullScreen: true });

  mainWindow.webContents.on('console-message', (event, level, message) => {
    console.log(`[Renderer] ${message}`);
  });

  mainWindow.once('ready-to-show', () => {
    if (currentMode !== 'hidden') {
      applyWindowMode(currentMode);
      mainWindow.show();
      mainWindow.focus();
      mainWindow.moveTop();
      console.log(`[Electron] Raphael opened in mode: ${currentMode}`);
    } else {
      mainWindow.hide();
      mainWindow.setSkipTaskbar(true);
      console.log('[Electron] Raphael is running in STEALTH background mode. Listening silently.');

      // Thông báo Windows Toast khi app khởi động ngầm thành công
      if (Notification.isSupported()) {
        try {
          new Notification({
            title: 'Tensura Raphael đã sẵn sàng',
            body: 'Raphael đang túc trực ngầm. Nói "Raphael" hoặc bấm Ctrl+Alt+Shift+\\ để đánh thức.',
            icon: path.join(__dirname, '..', 'public', 'icon-light-32x32.png'),
          }).show();
        } catch (_) {}
      }
    }
  });

  mainWindow.webContents.on('did-finish-load', () => {
    if (currentMode !== 'hidden') {
      mainWindow.show();
      mainWindow.focus();
    }
  });

  mainWindow.webContents.on('did-fail-load', (e, code, desc) => {
    console.error('[Electron] Failed to load UI:', code, desc);
  });

  // Lắng nghe phím Escape bên trong cửa sổ: ẩn hoàn toàn về chế độ chạy ngầm
  mainWindow.webContents.on('before-input-event', (event, input) => {
    if (input.key === 'Escape' && input.type === 'keyDown') {
      event.preventDefault();
      applyWindowMode('hidden');
      mainWindow.webContents.send('set-mode-from-main', 'hidden');
    }
  });

  // Đọc phím tắt từ cấu hình settings.json (Mặc định: 2 tay Ctrl+Alt+Shift+\ trên bàn phím 68 keys)
  let hotkey = 'Ctrl+Alt+Shift+\\';
  try {
    const configPath = path.join(__dirname, '..', '..', 'config', 'settings.json');
    if (fs.existsSync(configPath)) {
      const cfg = JSON.parse(fs.readFileSync(configPath, 'utf8'));
      if (cfg.system && cfg.system.hotkey) {
        hotkey = cfg.system.hotkey;
      }
    }
  } catch (_) {}

  // Đăng ký phím tắt toàn cầu (Toggle giữa Hiện và Ẩn ngầm)
  try {
    // Chuẩn hóa tên phím cho Electron globalShortcut
    const electronHotkey = hotkey.replace(/ctrl/i, 'CommandOrControl').replace(/\\/g, '\\');
    globalShortcut.register(electronHotkey, () => {
      console.log(`[Electron] Hotkey ${hotkey} triggered`);
      if (!mainWindow || mainWindow.isDestroyed()) return;
      if (currentMode === 'hidden' || !mainWindow.isVisible()) {
        applyWindowMode('overlay');
        mainWindow.webContents.send('set-mode-from-main', 'overlay');
      } else {
        applyWindowMode('hidden');
        mainWindow.webContents.send('set-mode-from-main', 'hidden');
      }
    });
    console.log(`[Electron] Registered global hotkey: ${hotkey}`);
  } catch (err) {
    console.warn('[Electron] Could not register global shortcut:', err);
  }

  // Khởi động server nội bộ phục vụ bản build tĩnh
  const outDir = path.join(__dirname, '..', 'out');
  if (fs.existsSync(path.join(outDir, 'index.html'))) {
    const { server, port } = await startStaticServer(outDir);
    staticServer = server;
    console.log(`[Electron] Internal static server running at http://127.0.0.1:${port}`);
    mainWindow.loadURL(`http://127.0.0.1:${port}`);
  } else {
    mainWindow.loadURL('http://localhost:3000');
  }

  // Khởi tạo khay hệ thống (System Tray)
  createSystemTray();

  mainWindow.on('closed', () => {
    mainWindow = null;
    if (staticServer) {
      try { staticServer.close(); } catch (_) {}
    }
  });

  mainWindow.on('close', () => {
    isQuitting = true;
    if (staticServer) {
      try { staticServer.close(); } catch (_) {}
    }
    app.quit();
  });
}

function applyWindowMode(mode) {
  if (!mainWindow || mainWindow.isDestroyed()) return;
  currentMode = mode;
  const primaryDisplay = screen.getPrimaryDisplay();
  const { workArea, bounds } = primaryDisplay;

  if (mode === 'hidden') {
    mainWindow.hide();
    mainWindow.setSkipTaskbar(true);
    console.log('[Electron] Raphael switched to HIDDEN (Stealth mode)');
    return;
  }

  mainWindow.setSkipTaskbar(false);

  if (mode === 'docked') {
    const orbSize = 190;
    const marginX = 24;
    const marginY = 24;
    const x = Math.round(workArea.x + workArea.width - orbSize - marginX);
    const y = Math.round(workArea.y + workArea.height - orbSize - marginY);
    mainWindow.setResizable(true);
    mainWindow.setBounds({ x, y, width: orbSize, height: orbSize });
    mainWindow.setIgnoreMouseEvents(false);
    mainWindow.setAlwaysOnTop(true, 'screen-saver', 1);
    mainWindow.show();
    mainWindow.focus();
  } else if (mode === 'sidedeck') {
    const sideWidth = 440;
    const x = Math.round(workArea.x + workArea.width - sideWidth);
    const y = Math.round(workArea.y);
    const h = Math.round(workArea.height);
    mainWindow.setResizable(true);
    mainWindow.setBounds({ x, y, width: sideWidth, height: h });
    mainWindow.setIgnoreMouseEvents(false);
    mainWindow.setAlwaysOnTop(true, 'screen-saver', 1);
    mainWindow.show();
    mainWindow.focus();
  } else if (mode === 'overlay') {
    mainWindow.setResizable(true);
    mainWindow.setBounds({
      x: bounds.x,
      y: bounds.y,
      width: bounds.width,
      height: bounds.height,
    });
    mainWindow.setAlwaysOnTop(true, 'screen-saver', 1);
    mainWindow.show();
    mainWindow.focus();
    mainWindow.moveTop();
  }
}

// IPC Handlers
ipcMain.on('set-visual-mode', (event, mode) => {
  console.log(`[Electron] Switching window bounds to mode: ${mode}`);
  applyWindowMode(mode);
});

ipcMain.on('set-ignore-mouse-events', (event, ignore, options) => {
  if (mainWindow && !mainWindow.isDestroyed()) {
    mainWindow.setIgnoreMouseEvents(ignore, options);
  }
});

ipcMain.on('quit-app', () => {
  isQuitting = true;
  app.quit();
});

ipcMain.on('stop-assistant', () => {
  console.log('[Electron] Requesting full stop of Raphael...');
  const rootDir = path.join(__dirname, '..', '..');
  const stopBat = path.join(rootDir, 'stop_assistant.bat');
  if (fs.existsSync(stopBat)) {
    exec(`cmd /c "${stopBat}"`, () => {
      app.quit();
    });
  } else {
    app.quit();
  }
});

ipcMain.handle('get-startup-status', () => {
  const lnk = getStartupShortcutPath();
  return fs.existsSync(lnk);
});

ipcMain.handle('toggle-startup', (event, enable) => {
  const lnk = getStartupShortcutPath();
  const rootDir = path.join(__dirname, '..', '..');
  const vbsPath = path.join(rootDir, 'run_silent.vbs');

  try {
    if (enable) {
      const psCommand = `$ws = New-Object -ComObject WScript.Shell; $s = $ws.CreateShortcut('${lnk.replace(/'/g, "''")}'); $s.TargetPath = 'wscript.exe'; $s.Arguments = '\`"${vbsPath.replace(/'/g, "''")}\`"'; $s.WorkingDirectory = '\`"${rootDir.replace(/'/g, "''")}\`"'; $s.Description = 'Tensura Raphael AI Assistant'; $s.Save();`;
      execSync(`powershell -NoProfile -Command "${psCommand}"`);
      return fs.existsSync(lnk);
    } else {
      if (fs.existsSync(lnk)) {
        fs.unlinkSync(lnk);
      }
      return false;
    }
  } catch (err) {
    console.error('[Startup] Failed to toggle startup:', err);
    return fs.existsSync(lnk);
  }
});

function createSystemTray() {
  const iconPath = path.join(__dirname, '..', 'public', 'icon-light-32x32.png');
  try {
    tray = new Tray(iconPath);
    tray.setToolTip('Tensura Raphael (Nói "Raphael" hoặc Ctrl+Alt+Shift+\\)');

    const updateTrayMenu = () => {
      const contextMenu = Menu.buildFromTemplate([
        {
          label: '🔮 Đánh thức Raphael (Mở Overlay)',
          click: () => {
            if (mainWindow) {
              applyWindowMode('overlay');
              mainWindow.webContents.send('set-mode-from-main', 'overlay');
            }
          },
        },
        {
          label: '👁️ Ẩn hoàn toàn (Chạy ngầm)',
          click: () => {
            if (mainWindow) {
              applyWindowMode('hidden');
              mainWindow.webContents.send('set-mode-from-main', 'hidden');
            }
          },
        },
        { type: 'separator' },
        {
          label: '🔵 Chế độ góc (Mini Orb)',
          click: () => {
            if (mainWindow) {
              applyWindowMode('docked');
              mainWindow.webContents.send('set-mode-from-main', 'docked');
            }
          },
        },
        {
          label: '📱 Thanh bên (Sidedeck Panel)',
          click: () => {
            if (mainWindow) {
              applyWindowMode('sidedeck');
              mainWindow.webContents.send('set-mode-from-main', 'sidedeck');
            }
          },
        },
        { type: 'separator' },
        {
          label: '❌ Thoát hoàn toàn Raphael',
          click: () => {
            const rootDir = path.join(__dirname, '..', '..');
            const stopBat = path.join(rootDir, 'stop_assistant.bat');
            if (fs.existsSync(stopBat)) {
              exec(`cmd /c "${stopBat}"`, () => {
                app.quit();
              });
            } else {
              app.quit();
            }
          },
        },
      ]);
      tray.setContextMenu(contextMenu);
    };

    updateTrayMenu();

    tray.on('click', () => {
      if (mainWindow) {
        if (mainWindow.isVisible() && currentMode !== 'hidden') {
          applyWindowMode('hidden');
          mainWindow.webContents.send('set-mode-from-main', 'hidden');
        } else {
          applyWindowMode('overlay');
          mainWindow.webContents.send('set-mode-from-main', 'overlay');
        }
        updateTrayMenu();
      }
    });
  } catch (err) {
    console.warn('[Tray] Tray icon warning:', err.message);
  }
}

app.on('will-quit', () => {
  globalShortcut.unregisterAll();
  if (staticServer) {
    try { staticServer.close(); } catch (_) {}
  }
});

app.on('window-all-closed', () => {
  app.quit();
});
