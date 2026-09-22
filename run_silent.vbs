' Tensura Raphael - 100% Invisible Stealth Launcher
' TUYET DOI KHONG CO CUA SO CMD / POWERSHELL
Option Explicit

Dim WshShell, fso, scriptDir, pythonExe, mainPy, electronExe, electronMain

Set WshShell = CreateObject("WScript.Shell")
Set fso = CreateObject("Scripting.FileSystemObject")
scriptDir = fso.GetParentFolderName(WScript.ScriptFullName)
WshShell.CurrentDirectory = scriptDir

' 1. Chay Python Audio Pipeline (WindowStyle = 0: Hoan toan an, khong cua so)
pythonExe = scriptDir & "\.venv\Scripts\python.exe"
mainPy = scriptDir & "\main.py"
WshShell.Run """" & pythonExe & """ -X utf8 """ & mainPy & """", 0, False

' 2. Nghi 2.5 giay cho Pipeline & WebSocket khoi tao
WScript.Sleep 2500

' 3. Chay Electron Native Desktop (WindowStyle = 0: Hoan toan an)
electronExe = scriptDir & "\nang-cap-visual-great-sage\node_modules\electron\dist\electron.exe"
electronMain = scriptDir & "\nang-cap-visual-great-sage\electron\main.cjs"
WshShell.Run """" & electronExe & """ """ & electronMain & """", 0, False

Set WshShell = Nothing
Set fso = Nothing
