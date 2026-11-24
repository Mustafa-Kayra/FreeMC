@echo off
rem FreeMC kurulabilir paket (Windows): dist\package\FreeMC
setlocal
cd /d "%~dp0"

call build.bat
if errorlevel 1 exit /b 1

if exist dist\package rmdir /s /q dist\package
if exist dist\jpackage-input rmdir /s /q dist\jpackage-input
mkdir dist\jpackage-input
copy dist\freemc.jar dist\jpackage-input\

jpackage --type app-image --name FreeMC --app-version 1.1.0 --input dist\jpackage-input --main-jar freemc.jar --main-class dev.freemc.Main --dest dist\package
if errorlevel 1 exit /b 1

echo Hazir: dist\package\FreeMC\FreeMC.exe
