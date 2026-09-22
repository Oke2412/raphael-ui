@echo off
cd /d "%~dp0"

if "%1"=="--stop" goto stop
if "%1"=="-s" goto stop
if "%1"=="--status" goto status
if "%1"=="--enroll" goto enroll

:: Chay hoan toan ngam qua run_silent.vbs va thoat cmd ngay lap tuc
start "" wscript.exe "%~dp0run_silent.vbs"
exit /b 0

:stop
call "%~dp0stop_assistant.bat"
exit /b 0

:status
call "%~dp0check_status.bat"
exit /b 0

:enroll
call .venv\Scripts\activate.bat
python scripts\enroll_voice.py
exit /b 0
