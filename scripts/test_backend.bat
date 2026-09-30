@echo off
title Tensura Raphael - Backend Connection Tester
chcp 65001 >nul
cd /d "%~dp0.."

set "PYTHON_EXE=%~dp0..\.venv\Scripts\python.exe"

if not exist "%PYTHON_EXE%" (
    echo [!] Khong tim thay Python trong .venv!
    pause
    exit /b 1
)

echo [*] Dang kiem tra ket noi toi Backend Docker / Local...
"%PYTHON_EXE%" -X utf8 "%~dp0test_backend_connection.py" %*

echo.
pause
