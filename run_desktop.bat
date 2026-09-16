@echo off
title Tensura Raphael - Native Desktop Overlay
cd /d "%~dp0nang-cap-visual-great-sage"

echo ====================================================================
echo    KHOI DONG TENSURA (RAPHAEL) NATIVE DESKTOP OVERLAY
echo    Che do: Chay ngam Native Desktop (Khong can Web / Localhost)
echo    System Tray: Icon Raphael tai goc dong ho Windows
echo ====================================================================

if not exist "out\index.html" (
    echo [*] Dang tao ban build tinh lan dau...
    call npx.cmd pnpm build
)

echo [*] Dang khoi chay cua so trong suot Native tren man hinh...
call npx.cmd electron electron/main.cjs
