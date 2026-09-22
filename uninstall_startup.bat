@echo off
title Tensura Raphael - Disable Windows Startup
cd /d "%~dp0"

echo ====================================================================
echo    GO BO TENSURA RAPHAEL KHOI KHOI DONG CUNG WINDOWS
echo ====================================================================

set "SHORTCUT_PATH=%APPDATA%\Microsoft\Windows\Start Menu\Programs\Startup\TensuraRaphael.lnk"

if exist "%SHORTCUT_PATH%" (
    del /f /q "%SHORTCUT_PATH%"
    echo [✓] Da go bo thanh cong khoi Startup!
    echo     Raphael se khong con tu dong mo khi bat may tinh nua.
) else (
    echo [-] Raphael chua duoc cai dat khoi dong cung Windows.
)

echo.
pause
