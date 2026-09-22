@echo off
cd /d "%~dp0.."
start "" wscript.exe "%~dp0..\run_silent.vbs"
exit /b 0
