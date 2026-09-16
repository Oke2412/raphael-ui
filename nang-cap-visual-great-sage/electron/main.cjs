// Tensura (Raphael) - Native Windows Desktop Overlay
// True Transparency, Frameless, Always-on-top, System Tray Background App

const { app, BrowserWindow, screen, Tray, Menu, ipcMain } = require('electron');
const path = require('path');
const fs = require('fs');
const http = require('http');

// Cho phép cửa sổ trong suốt và tăng tốc GPU
app.commandLine.appendSwitch('enable-transparent-visuals');
app.commandLine.appendSwitch('high-dpi-support', '1');

let mainWindow = null;
let tray = null;
let staticServer = null;
let isQuitting = false;

// Đảm bảo chỉ có 1 instance chạy ngầm duy nhất
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

  app.whenReady().then(createDesktopApp);
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
          // Fallback to index.html for client-side navigation
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
    skipTaskbar: true, // Chạy ngầm trong System Tray, không chiếm thanh taskbar
    webPreferences: {
      nodeIntegration: false,
      contextIsolation: true,
      webSecurity: false,
    },
  });

  // Nạp giao diện tĩnh đã xuất qua server nội bộ hoặc localhost
  const outDir = path.join(__dirname, '..', 'out');
  if (fs.existsSync(path.join(outDir, 'index.html'))) {
    const { server, port } = await startStaticServer(outDir);
    staticServer = server;
    mainWindow.loadURL(`http://127.0.0.1:${port}`);
  } else {
    // Nếu chưa build export tĩnh thì kết nối localhost dev server
    mainWindow.loadURL('http://localhost:3000');
  }

  // Khởi tạo khay hệ thống (System Tray) ở góc đồng hồ Windows
  createSystemTray();

  // Hỗ trợ tự động tắt sau N giây nếu được truyền tham số (ví dụ kiểm thử)
  const durationArg = process.argv.find((arg) => arg.startsWith('--duration='));
  const durationSeconds = durationArg
    ? parseInt(durationArg.split('=')[1], 10)
    : process.env.AUTO_CLOSE_SECONDS
    ? parseInt(process.env.AUTO_CLOSE_SECONDS, 10)
    : 0;

  if (durationSeconds > 0) {
    setTimeout(() => {
      isQuitting = true;
      if (staticServer) {
        try { staticServer.close(); } catch (_) {}
      }
      app.quit();
    }, durationSeconds * 1000);
  }

  // Đảm bảo không bị tắt khi đóng cửa sổ
  mainWindow.on('close', (e) => {
    if (!isQuitting) {
      e.preventDefault();
      mainWindow.hide();
    }
  });
}

function createSystemTray() {
  const iconPath = path.join(__dirname, '..', 'public', 'icon-light-32x32.png');
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
      label: '⚙️ Trạng thái: Chạy ngầm Native',
      enabled: false,
    },
    { type: 'separator' },
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
}

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') {
    // Giữ app chạy ngầm trong tray
  }
});
