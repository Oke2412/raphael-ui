export interface IElectronAPI {
  setVisualMode: (mode: 'overlay' | 'docked' | 'sidedeck') => void;
  onSetVisualMode: (callback: (mode: 'overlay' | 'docked' | 'sidedeck') => void) => void;
  setIgnoreMouseEvents: (ignore: boolean, options?: { forward: boolean }) => void;
  quitApp: () => void;
  isElectron?: boolean;
}

declare global {
  interface Window {
    electronAPI?: IElectronAPI;
  }
}
