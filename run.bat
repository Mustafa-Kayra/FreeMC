@echo off
rem FreeMC baslatma betigi (Windows)
setlocal
cd /d "%~dp0"

if not exist dist\freemc.jar call build.bat
if errorlevel 1 exit /b 1

start "" javaw -jar "dist\freemc.jar" %*
