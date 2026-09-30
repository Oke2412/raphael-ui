@echo off
setlocal
echo ==========================================================
echo Dang kiem tra quyen Administrator de cai dat Visual C++ Build Tools...
echo ==========================================================

:: Kiem tra quyen Admin
net session >nul 2>&1
if %errorLevel% neq 0 (
    echo [THONG BAO] Yeu cau quyen Administrator de chay trinh cai dat.
    echo Dang mo cua so UAC de xin cap quyen...
    powershell -Command "Start-Process '%~f0' -Verb RunAs"
    exit /b
)

echo Dang tien hanh cai dat Microsoft Visual C++ Build Tools (MSVC)...
echo Qua trinh nay se cai dat link.exe va cac thu vien can thiet cho Tauri/Rust.
echo Vui long cho trong giay lat...

if not exist "%TEMP%\vs_BuildTools.exe" (
    echo Dang tai vs_BuildTools.exe tu Microsoft...
    powershell -Command "Invoke-WebRequest -Uri 'https://aka.ms/vs/17/release/vs_BuildTools.exe' -OutFile '$env:TEMP\vs_BuildTools.exe'"
)

"%TEMP%\vs_BuildTools.exe" --passive --wait --norestart --nocache --add Microsoft.VisualStudio.Workload.VCTools --includeRecommended

echo ==========================================================
echo Cai dat hoan tat! Ban co the chay Tauri ngay bay gio.
echo ==========================================================
pause
