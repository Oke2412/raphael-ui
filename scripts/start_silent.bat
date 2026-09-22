@echo off
setlocal
cd /d "%~dp0.."

if not exist "logs" mkdir "logs"

:: 1. Khoi dong Python Backend ngam bang pythonw.exe (Khong cua so cmd)
set PYTHONIOENCODING=utf-8
start "" "%~dp0..\.venv\Scripts\pythonw.exe" main.py > logs\pipeline.log 2>&1

:: 2. Cho 2 giay de WebSocket server san sang tren port 8765
timeout /t 2 /nobreak >nul

:: 3. Khoi dong Native Desktop Electron ngam (Hidden Mode mac dinh)
cd /d "%~dp0..\nang-cap-visual-great-sage"
start "" "node_modules\electron\dist\electron.exe" "electron\main.cjs"
