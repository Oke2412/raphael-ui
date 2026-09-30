@echo off
title Tensura Raphael - Voice Output Tester (VieNeu + RVC)
chcp 65001 >nul
cd /d "%~dp0.."

set "PYTHON_EXE=%~dp0..\.venv\Scripts\python.exe"

if not exist "%PYTHON_EXE%" (
    echo [!] Khong tim thay Python trong .venv!
    pause
    exit /b 1
)

echo [*] Dang kiem thu am thanh VieNeu-TTS va RVC...
"%PYTHON_EXE%" -X utf8 "%~dp0test_voice_output.py" %*

echo.
pause
