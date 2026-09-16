@echo off
title Tensura Raphael - Real-time Audio Pipeline

echo ====================================================================
echo    KHOI DONG CORE AUDIO PIPELINE - TENSURA (RAPHAEL)
echo    NVIDIA GeForce RTX 2050 (4GB VRAM)
echo ====================================================================

cd /d "%~dp0"

if not exist ".venv\Scripts\python.exe" (
    echo [!] Chua tim thay moi truong ao .venv. Dang khoi tao...
    uv venv .venv --python 3.10
)

echo [*] Dang kich hoat moi truong Python...
call .venv\Scripts\activate.bat

echo [*] Dang khoi dong Core Audio Pipeline va WebSocket Server (ws://127.0.0.1:8765)...
python main.py

pause
