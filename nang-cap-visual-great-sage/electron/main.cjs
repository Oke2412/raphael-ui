// Tensura (Raphael) - Native Windows Desktop Overlay
// True Transparency, Frameless, Always-on-top, System Tray Background App

const { app, BrowserWindow, screen, Tray, Menu, ipcMain } = require('electron');
const path = require('path');
const fs = require('fs');

// Cho phép cửa sổ trong suốt và tăng tốc GPU
app.commandLine.appendSwitch('enable-transparent-visuals');
app.commandLine.appendSwitch('high-dpi-support', '1');

let mainWindow = null;
let tray = null;
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

function createDesktopApp() {
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

  // Nạp giao diện tĩnh đã xuất (Không cần mở localhost hay server web!)
  const outIndexPath = path.join(__dirname, '..', 'out', 'index.html');
  if (fs.existsSync(outIndexPath)) {
    mainWindow.loadFile(outIndexPath);
  } else {
    // Nếu chưa build export tĩnh thì kết nối localhost dev server
    mainWindow.loadURL('http://localhost:3000');
  }

  // Khởi tạo khay hệ thống (System Tray) ở góc đồng hồ Windows
  createSystemTray();

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
