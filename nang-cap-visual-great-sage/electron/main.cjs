// Tensura (Raphael) - Native Windows Desktop Overlay
// True Transparency, Frameless, Always-on-top, System Tray & Taskbar App

const { app, BrowserWindow, screen, Tray, Menu, globalShortcut, ipcMain } = require('electron');
const path = require('path');
const fs = require('fs');
const http = require('http');

let mainWindow = null;
let tray = null;
let staticServer = null;
let isQuitting = false;
let currentMode = 'overlay';

// Single-instance lock
const gotTheLock = app.requestSingleInstanceLock();
if (!gotTheLock) {
  app.quit();
} else {
  app.on('second-instance', () => {
    if (mainWindow) {
      if (mainWindow.isMinimized()) mainWindow.restore();
      mainWindow.show();
      mainWindow.focus();
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

async function createDesktopApp() {
  const primaryDisplay = screen.getPrimaryDisplay();
  const { width, height } = primaryDisplay.bounds;

  mainWindow = new BrowserWindow({
    title: 'Tensura Raphael - AI Assistant',
    icon: path.join(__dirname, '..', 'public', 'icon-light-32x32.png'),
    width: width,
    height: height,
    x: 0,
    y: 0,
    frame: false,
    transparent: true,
    alwaysOnTop: true,
    hasShadow: false,
    resizable: true,
    skipTaskbar: false, // Show in taskbar so user can click to bring to front
    show: true,
    webPreferences: {
      preload: path.join(__dirname, 'preload.cjs'),
      nodeIntegration: false,
      contextIsolation: true,
      webSecurity: false,
    },
  });

  // Force window to stay above all other applications
  mainWindow.setAlwaysOnTop(true, 'screen-saver', 1);
  mainWindow.setVisibleOnAllWorkspaces(true, { visibleOnFullScreen: true });
  mainWindow.moveTop();

  mainWindow.webContents.on('console-message', (event, level, message) => {
    console.log(`[Renderer] ${message}`);
  });

  mainWindow.once('ready-to-show', () => {
    mainWindow.show();
    mainWindow.focus();
    mainWindow.moveTop();
    console.log('[Electron] Raphael Desktop Overlay is now OPEN on your screen.');
  });

  mainWindow.webContents.on('did-finish-load', () => {
    mainWindow.show();
    mainWindow.focus();
    setTimeout(async () => {
      try {
        const image = await mainWindow.capturePage();
        fs.writeFileSync(path.join(__dirname, 'screenshot.png'), image.toPNG());
        console.log('[Electron] Screenshot saved to screenshot.png');
      } catch (err) {
        console.error('[Electron] Capture failed:', err);
      }
    }, 2000);
  });

  mainWindow.webContents.on('did-fail-load', (e, code, desc) => {
    console.error('[Electron] Failed to load UI:', code, desc);
  });

  // Lắng nghe phím Escape bên trong cửa sổ: thu nhỏ về docked mode thay vì thoát app
  mainWindow.webContents.on('before-input-event', (event, input) => {
    if (input.key === 'Escape' && input.type === 'keyDown') {
      if (currentMode !== 'docked') {
        event.preventDefault();
        applyWindowMode('docked');
        mainWindow.webContents.send('set-mode-from-main', 'docked');
      }
    }
  });

  // Phím tắt toàn cầu Alt+R: toggle giữa Overlay toàn màn hình và Docked Mini Orb
  try {
    globalShortcut.register('Alt+R', () => {
      console.log('[Electron] Hotkey Alt+R triggered');
      if (!mainWindow || mainWindow.isDestroyed()) return;
      if (currentMode === 'docked') {
        applyWindowMode('overlay');
        mainWindow.webContents.send('set-mode-from-main', 'overlay');
      } else {
        applyWindowMode('docked');
        mainWindow.webContents.send('set-mode-from-main', 'docked');
      }
    });
  } catch (err) {
    console.warn('[Electron] Could not register Alt+R shortcut:', err);
  }

  // Nạp giao diện tĩnh đã xuất qua server nội bộ hoặc localhost
  const outDir = path.join(__dirname, '..', 'out');
  if (fs.existsSync(path.join(outDir, 'index.html'))) {
    const { server, port } = await startStaticServer(outDir);
    staticServer = server;
    console.log(`[Electron] Internal static server running at http://127.0.0.1:${port}`);
    mainWindow.loadURL(`http://127.0.0.1:${port}`);
  } else {
    mainWindow.loadURL('http://localhost:3000');
  }

  // Khởi tạo khay hệ thống (System Tray) ở góc đồng hồ Windows
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

  if (mode === 'docked') {
    // Docked Mini Orb: window is only 180x180 px at bottom right corner!
    // Leaves 98% of the screen completely free for desktop apps/VS Code!
    const orbSize = 180;
    const marginX = 24;
    const marginY = 24;
    const x = Math.round(workArea.x + workArea.width - orbSize - marginX);
    const y = Math.round(workArea.y + workArea.height - orbSize - marginY);
    mainWindow.setResizable(true);
    mainWindow.setBounds({ x, y, width: orbSize, height: orbSize });
    mainWindow.setIgnoreMouseEvents(false);
    mainWindow.setAlwaysOnTop(true, 'screen-saver', 1);
  } else if (mode === 'sidedeck') {
    // Sidedeck panel: right-docked (440px wide, 100% height)
    // Leaves entire left side completely free for VS Code / IDE!
    const sideWidth = 440;
    const x = Math.round(workArea.x + workArea.width - sideWidth);
    const y = Math.round(workArea.y);
    const h = Math.round(workArea.height);
    mainWindow.setResizable(true);
    mainWindow.setBounds({ x, y, width: sideWidth, height: h });
    mainWindow.setIgnoreMouseEvents(false);
    mainWindow.setAlwaysOnTop(true, 'screen-saver', 1);
  } else if (mode === 'overlay') {
    // Fullscreen transparent overlay
    mainWindow.setResizable(true);
    mainWindow.setBounds({
      x: bounds.x,
      y: bounds.y,
      width: bounds.width,
      height: bounds.height,
    });
    mainWindow.setAlwaysOnTop(true, 'screen-saver', 1);
  }
}

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

function createSystemTray() {
  const iconPath = path.join(__dirname, '..', 'public', 'icon-light-32x32.png');
  try {
    tray = new Tray(iconPath);
    tray.setToolTip('Tensura Great Sage (Raphael) - AI Assistant (Alt+R)');

    const contextMenu = Menu.buildFromTemplate([
      {
        label: '🔮 Toàn màn hình (Overlay)',
        click: () => {
          if (mainWindow) {
            mainWindow.show();
            mainWindow.focus();
            applyWindowMode('overlay');
            mainWindow.webContents.send('set-mode-from-main', 'overlay');
          }
        },
      },
      {
        label: '🔵 Thu nhỏ góc (Mini Orb)',
        click: () => {
          if (mainWindow) {
            mainWindow.show();
            applyWindowMode('docked');
            mainWindow.webContents.send('set-mode-from-main', 'docked');
          }
        },
      },
      {
        label: '📱 Thanh bên (Sidedeck Panel)',
        click: () => {
          if (mainWindow) {
            mainWindow.show();
            applyWindowMode('sidedeck');
            mainWindow.webContents.send('set-mode-from-main', 'sidedeck');
          }
        },
      },
      { type: 'separator' },
      {
        label: '👁️ Ẩn cửa sổ',
        click: () => {
          if (mainWindow) mainWindow.hide();
        },
      },
      {
        label: '❌ Thoát ứng dụng',
        click: () => {
          isQuitting = true;
          app.quit();
        },
      },
    ]);

    tray.setContextMenu(contextMenu);

    tray.on('click', () => {
      if (mainWindow) {
        if (mainWindow.isVisible()) {
          mainWindow.focus();
        } else {
          mainWindow.show();
        }
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
