@echo off
setlocal
cd /d "%~dp0"
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0start-local.ps1"
set "START_EXIT_CODE=%ERRORLEVEL%"
echo.
pause
exit /b %START_EXIT_CODE%
