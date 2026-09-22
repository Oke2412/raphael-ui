@echo off
title Tensura Raphael - Enable Windows Startup
cd /d "%~dp0"

echo ====================================================================
echo    CAI DAT TU DONG KHOI DONG TENSURA RAPHAEL CUNG WINDOWS
echo ====================================================================

set "STARTUP_FOLDER=%APPDATA%\Microsoft\Windows\Start Menu\Programs\Startup"
set "SHORTCUT_PATH=%STARTUP_FOLDER%\TensuraRaphael.lnk"
set "TARGET_VBS=%~dp0run_silent.vbs"
set "WORKING_DIR=%~dp0"
set "ICON_PATH=%~dp0nang-cap-visual-great-sage\public\icon-light-32x32.png"

powershell -NoProfile -Command "$ws = New-Object -ComObject WScript.Shell; $s = $ws.CreateShortcut('%SHORTCUT_PATH%'); $s.TargetPath = 'wscript.exe'; $s.Arguments = '\"%TARGET_VBS%\"'; $s.WorkingDirectory = '\"%WORKING_DIR%\"'; $s.Description = 'Tensura Raphael AI Assistant'; $s.Save()"

if exist "%SHORTCUT_PATH%" (
    echo [✓] DA CAI DAT THANH CONG!
    echo     Moi khi ban mo laptop, Raphael se tu dong chay ngam 100%%.
    echo     Micro se lang nghe giong noi va bung ra khi ban goi 'Raphael'!
) else (
    echo [!] Khong the tao shortcut. Vui long thu lai bang quyen Administrator.
)

echo.
pause
