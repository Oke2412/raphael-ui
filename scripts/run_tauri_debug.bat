@echo off
title Tensura Raphael - Tauri Desktop Debug Console
chcp 65001 >nul
cd /d "%~dp0.."

echo ====================================================================
echo    KHOI DONG TENSURA RAPHAEL DESKTOP (CHE DO DEBUG CONSOLE)
echo    Logs se duoc in truc tiep tai day va ghi vao logs\desktop.log
echo    Dap ung phím tat: Ctrl+Alt+Shift+\ hoac Wake Word 'Raphael'
echo ====================================================================
echo.

set "EXE_PATH=%~dp0..\tauri-desktop\src-tauri\target\debug\tensura-raphael.exe"
if not exist "%EXE_PATH%" (
    set "EXE_PATH=%~dp0..\tauri-desktop\src-tauri\target\release\tensura-raphael.exe"
)

if not exist "%EXE_PATH%" (
    echo [!] Chua tim thay tensura-raphael.exe. Dang build...
    cd /d "%~dp0..\tauri-desktop\src-tauri"
    set "PATH=%USERPROFILE%\.cargo\bin;%PATH%"
    cargo build
    cd /d "%~dp0.."
)

set "ARGS=%*"
if "%ARGS%"=="" set "ARGS=--overlay"

set "RUST_LOG=info"
echo [*] Dang chay Tensura Raphael voi che do: %ARGS%
"%EXE_PATH%" %ARGS%

echo.
echo [*] Ung dung da dong (Exit Code: %errorlevel%).
pause
