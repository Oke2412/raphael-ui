export type VisualMode = 'overlay' | 'docked' | 'sidedeck' | 'hidden';

export interface IElectronAPI {
  setVisualMode: (mode: VisualMode) => void;
  onSetVisualMode: (callback: (mode: VisualMode) => void) => void;
  setIgnoreMouseEvents: (ignore: boolean, options?: { forward: boolean }) => void;
  moveWindowBy?: (dx: number, dy: number) => void;
  quitApp: () => void;
  getStartupStatus: () => Promise<boolean>;
  toggleStartup: (enable: boolean) => Promise<boolean>;
  stopAssistant: () => void;
  isElectron?: boolean;
}

declare global {
  interface Window {
    electronAPI?: IElectronAPI;
  }
}
