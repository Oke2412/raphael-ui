@echo off
setlocal
echo ==========================================================
echo Khoi chay Raphael Great Sage Desktop (Tauri v2)
echo ==========================================================

set "PATH=%USERPROFILE%\.cargo\bin;%PATH%"
cd /d "d:\project\tensurafrontend\tauri-desktop"
cmd.exe /c "npx tauri dev"
pause
