// Tensura (Raphael) - Native Windows Desktop Overlay
// True Transparency, Frameless, Always-on-top, System Tray & Taskbar App

const { app, BrowserWindow, screen, Tray, Menu, globalShortcut } = require('electron');
const path = require('path');
const fs = require('fs');
const http = require('http');

let mainWindow = null;
let tray = null;
let staticServer = null;
let isQuitting = false;

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
    width: width,
    height: height,
    x: 0,
    y: 0,
    frame: false,
    transparent: true,
    alwaysOnTop: true,
    hasShadow: false,
    resizable: false,
    skipTaskbar: false, // User can see icon in taskbar, click, switch, or close
    show: true, // Show immediately
    webPreferences: {
      nodeIntegration: false,
      contextIsolation: true,
      webSecurity: false,
    },
  });

  // Keep on top of normal windows
  mainWindow.setAlwaysOnTop(true);

  mainWindow.webContents.on('console-message', (event, level, message) => {
    console.log(`[Renderer] ${message}`);
  });

  mainWindow.once('ready-to-show', () => {
    mainWindow.show();
    mainWindow.focus();
    console.log('[Electron] Raphael Desktop Overlay is now OPEN on your screen.');
  });

  mainWindow.webContents.on('did-finish-load', () => {
    mainWindow.show();
    mainWindow.focus();
  });

  mainWindow.webContents.on('did-fail-load', (e, code, desc) => {
    console.error('[Electron] Failed to load UI:', code, desc);
  });

  // Register ESC key to easily exit
  globalShortcut.register('Escape', () => {
    console.log('[Electron] User pressed Escape. Closing overlay...');
    isQuitting = true;
    app.quit();
  });

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

function createSystemTray() {
  const iconPath = path.join(__dirname, '..', 'public', 'icon-light-32x32.png');
  try {
    tray = new Tray(iconPath);
    tray.setToolTip('Tensura Great Sage (Raphael) - AI Assistant');

    const contextMenu = Menu.buildFromTemplate([
      {
        label: '🔮 Hiện Raphael (Overlay)',
        click: () => {
          if (mainWindow) {
            mainWindow.show();
            mainWindow.focus();
          }
        },
      },
      {
        label: '👁️ Ẩn cửa sổ',
        click: () => {
          if (mainWindow) mainWindow.hide();
        },
      },
      { type: 'separator' },
      {
        label: '❌ Thoát ứng dụng (Esc)',
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
