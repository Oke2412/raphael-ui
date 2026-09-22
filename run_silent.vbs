' Tensura Raphael - 100% Invisible Background Launcher (Khong cua so cmd)
Set WshShell = CreateObject("WScript.Shell")
Set fso = CreateObject("Scripting.FileSystemObject")
scriptDir = fso.GetParentFolderName(WScript.ScriptFullName)
WshShell.CurrentDirectory = scriptDir

' Chay start_silent.bat voi cua so che giau hoan toan (0 = Hide window, False = Don't block)
WshShell.Run "cmd /c """ & scriptDir & "\scripts\start_silent.bat""", 0, False
