@echo off
title Tensura Raphael - Full Native Assistant Launcher
cd /d "%~dp0"

echo ====================================================================
echo       KHOI DONG TOAN BO TRO LY AO TENSURA RAPHAEL (CHAY NGAM)
echo ====================================================================
echo 1. Chay Core Audio Pipeline (Faster-Whisper CUDA, Silero VAD, Wake Word 'Raphael')
echo 2. Chay Native Desktop Mini Orb goc man hinh (190x190px - Khong chiem man hinh)
echo ====================================================================

:: Khoi dong Python Backend chay ngam
start "Tensura Pipeline Backend" /min cmd /c "call run_pipeline.bat"

:: Doi 2 giay cho WebSocket Server san sang
timeout /t 2 /nobreak >nul

:: Khoi dong Native Desktop Overlay o che do ngam (Docked Mini Orb)
cd /d "%~dp0nang-cap-visual-great-sage"
if not exist "out\index.html" (
    echo [*] Dang tao ban build tinh cho Desktop...
    call npx.cmd pnpm build
)

start "Raphael Desktop Overlay" cmd /c "call npx.cmd electron electron/main.cjs --docked"

echo ====================================================================
echo  RAPHAEL DA KHOI DONG THANH CONG O CHE DO CHAY NGAM!
echo  - Mini Orb dang o goc duoi ben phai man hinh (khong chan chuot cua ban).
echo  - Nhin vao Mini Orb: Den xanh 'Nghe: Raphael' va chop sang khi co giong noi.
echo  - Noi 'Raphael' hoac 'Raphael oi' de danh thuc tro ly bung ra toan man hinh.
echo  - Hoac nhan Alt+R / Ctrl+Space de thuc day bang phim tat.
echo ====================================================================
