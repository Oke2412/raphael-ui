@echo off
title Tensura Raphael - Stop All Processes
echo ====================================================================
echo    DANG DUNG TOAN BO HE THONG TENSURA RAPHAEL (CHAY NGAM)
echo ====================================================================

:: 1. Dong tien trinh Electron
taskkill /F /IM electron.exe /T 2>nul

:: 2. Dong tien trinh Python chạy main.py
wmic process where "name='pythonw.exe' and commandline like '%%main.py%%'" call terminate 2>nul
wmic process where "name='python.exe' and commandline like '%%main.py%%'" call terminate 2>nul
taskkill /F /FI "WINDOWTITLE eq Tensura*" 2>nul

echo.
echo [✓] Da dung toan bo tien trinh Raphael thanh cong!
timeout /t 2 /nobreak >nul
