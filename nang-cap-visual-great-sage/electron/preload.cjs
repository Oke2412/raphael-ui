// Tensura (Raphael) - Preload Script
// Safely expose Electron APIs to renderer process

const { contextBridge, ipcRenderer } = require('electron');

contextBridge.exposeInMainWorld('electronAPI', {
  setVisualMode: (mode) => {
    ipcRenderer.send('set-visual-mode', mode);
  },
  onSetVisualMode: (callback) => {
    ipcRenderer.on('set-mode-from-main', (_event, mode) => callback(mode));
  },
  setIgnoreMouseEvents: (ignore, options) => {
    ipcRenderer.send('set-ignore-mouse-events', ignore, options);
  },
  moveWindowBy: (dx, dy) => {
    ipcRenderer.send('move-window-by', { dx, dy });
  },
  quitApp: () => {
    ipcRenderer.send('quit-app');
  },
  getStartupStatus: () => ipcRenderer.invoke('get-startup-status'),
  toggleStartup: (enable) => ipcRenderer.invoke('toggle-startup', enable),
  stopAssistant: () => {
    ipcRenderer.send('stop-assistant');
  },
  isElectron: true,
});
