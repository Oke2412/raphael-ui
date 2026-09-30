@echo off
title Tensura Raphael - System Status Check
chcp 65001 >nul
cd /d "%~dp0"

echo ====================================================================
echo         KIEM TRA TRANG THAI TENSURA RAPHAEL (CHAY NGAM)
echo ====================================================================

echo [*] 1. Kiem tra WebSocket Server (Port 8765):
netstat -ano | findstr 8765 >nul
if %errorlevel% equ 0 (
    echo     [✓] Cổng 8765 DANG LANG NGHE (WebSocket Server San Sang)
) else (
    echo     [X] Cổng 8765 CHUA MO (Backend co the chua chay)
)

echo.
echo [*] 2. Kiem tra tien trinh he thong:
tasklist /FI "IMAGENAME eq tensura-raphael.exe" 2>nul | findstr /I "tensura-raphael.exe" >nul
if %errorlevel% equ 0 (
    echo     [✓] Giao dien Tauri v2 Desktop: DANG CHAY NGAM (Stealth Daemon)
) else (
    tasklist /FI "IMAGENAME eq electron.exe" 2>nul | findstr /I "electron.exe" >nul
    if %errorlevel% equ 0 (
        echo     [✓] Giao dien Electron: DANG CHAY NGAM (Stealth Daemon)
    ) else (
        echo     [X] Giao dien Desktop (Tauri/Electron): CHUA CHAY
    )
)

tasklist /FI "IMAGENAME eq pythonw.exe" 2>nul | findstr /I "pythonw.exe" >nul
if %errorlevel% equ 0 (
    echo     [✓] Backend Python (pythonw.exe): DANG CHAY NGAM (0%% Console)
) else (
    tasklist /FI "IMAGENAME eq python.exe" 2>nul | findstr /I "python.exe" >nul
    if %errorlevel% equ 0 (
        echo     [✓] Backend Python (python.exe): DANG CHAY
    ) else (
        echo     [X] Backend Python: CHUA CHAY
    )
)

echo.
echo [*] 3. Kiem tra khoi dong cung Windows:
if exist "%APPDATA%\Microsoft\Windows\Start Menu\Programs\Startup\TensuraRaphael.lnk" (
    echo     [✓] Windows Startup: DA BAT (Tu dong chay khi bat laptop)
) else (
    echo     [-] Windows Startup: CHUA BAT (Dung install_startup.bat de bat)
)

echo.
echo [*] 4. Kiem tra Van giong Chinh chu (Voice Biometrics):
if exist "models\voiceprint.npy" (
    echo     [✓] Voiceprint: DA GHI DANH (Chi mo khi nhan dien dung giong cua ban)
) else (
    echo     [-] Voiceprint: CHUA GHI DANH (Chay 'python scripts/enroll_voice.py' de ghi danh)
)

echo.
echo [*] 5. Nhat ky hoat dong moi nhat (logs\pipeline.log):
echo --------------------------------------------------------------------
if exist "logs\pipeline.log" (
    powershell -NoProfile -Command "Get-Content -Path 'logs\pipeline.log' -Tail 10 -Encoding UTF8"
) else (
    echo     (Chua co file log)
)
echo --------------------------------------------------------------------
echo.
pause
