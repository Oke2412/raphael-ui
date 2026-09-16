@echo off
title Tensura Raphael - Full Native Assistant Launcher
cd /d "%~dp0"

echo ====================================================================
echo       KHOI DONG TOAN BO TRO LY AO TENSURA RAPHAEL (NATIVE)
echo ====================================================================
echo 1. Chay Core Audio Pipeline (Faster-Whisper, VieNeu, RVC, OpenWakeWord)
echo 2. Chay Native Desktop Overlay trong suot (System Tray)
echo ====================================================================

:: Khoi dong Python Backend chay ngam
start "Tensura Pipeline Backend" /min cmd /c "call run_pipeline.bat"

:: Doi 2 giay cho WebSocket Server san sang
timeout /t 2 /nobreak >nul

:: Khoi dong Native Desktop Overlay
cd /d "%~dp0nang-cap-visual-great-sage"
if not exist "out\index.html" (
    echo [*] Dang tao ban build tinh cho Desktop...
    call npx.cmd pnpm build
)

start "Raphael Desktop Overlay" cmd /c "call npx.cmd electron electron/main.cjs"

echo ====================================================================
echo  RAPHAEL DA SAN SANG!
echo  - Noi 'Raphael' de goi tro ly
echo  - Hoac nhan Ctrl+Space de thuc day
echo  - App chay ngam trong khay he thong (System Tray o goc phai taskbar)
echo ====================================================================
