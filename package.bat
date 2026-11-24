@echo off
rem FreeMC kurulabilir paket (Windows): dist\package\FreeMC
setlocal
cd /d "%~dp0"

call build.bat
if errorlevel 1 exit /b 1

if exist dist\package rmdir /s /q dist\package
mkdir dist\package

jpackage --type app-image --name FreeMC --app-version 1.1.0 --input dist --main-jar freemc.jar --main-class dev.freemc.Main --dest dist\package
if errorlevel 1 exit /b 1

echo Hazir: dist\package\FreeMC\FreeMC.exe
