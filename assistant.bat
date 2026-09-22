@echo off
chcp 65001 >nul
cd /d "%~dp0"

if "%1"=="--stop" goto stop
if "%1"=="-s" goto stop
if "%1"=="--status" goto status
if "%1"=="--overlay" goto overlay
if "%1"=="--docked" goto docked
if "%1"=="--enroll" goto enroll

:: Mac dinh: Khoi dong ngam 100% (Khong cua so cmd)
echo ====================================================================
echo    🔮 KHOI DONG TENSURA RAPHAEL (CHAY NGAM 100%% STEALTH)
echo ====================================================================
echo [*] Dang khoi dong toan bo tien trinh ngam...
wscript.exe "%~dp0run_silent.vbs"
timeout /t 2 /nobreak >nul

echo.
echo [✓] RAPHAEL DA KHOI DONG THANH CONG O CHE DO CHAY NGAM!
echo  - Micro dang lang nghe ngam tu khoa: "Raphael" hoac "Raphael oi"
echo  - To hop phim 2 tay danh thuc / an ngam: Ctrl + Alt + Shift + \
echo  - System Tray: Icon nho o goc dong ho Windows
echo  - Khi goi, Raphael se lap tuc bung giao dien Overlay len man hinh.
echo  - De kiem tra trang thai: assistant.bat --status
echo  - De dung tro ly: assistant.bat --stop
echo ====================================================================
timeout /t 4 /nobreak >nul
exit /b 0

:stop
call "%~dp0stop_assistant.bat"
exit /b 0

:status
call "%~dp0check_status.bat"
exit /b 0

:enroll
echo [*] Ghi danh van giong chinh chu...
call .venv\Scripts\activate.bat
python scripts\enroll_voice.py
exit /b 0

:overlay
start "" "%~dp0nang-cap-visual-great-sage\node_modules\electron\dist\electron.exe" "%~dp0nang-cap-visual-great-sage\electron\main.cjs" --overlay
exit /b 0

:docked
start "" "%~dp0nang-cap-visual-great-sage\node_modules\electron\dist\electron.exe" "%~dp0nang-cap-visual-great-sage\electron\main.cjs" --docked
exit /b 0
