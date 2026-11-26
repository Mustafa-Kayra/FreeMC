@echo off
rem FreeMC derleme betigi (Windows)
setlocal
cd /d "%~dp0"

if exist build rmdir /s /q build
if exist dist rmdir /s /q dist
mkdir build\classes
mkdir dist

dir /s /b src\main\java\*.java > build\sources.txt
javac --release 17 -encoding UTF-8 -d build\classes @build\sources.txt
if errorlevel 1 exit /b 1

jar --create --file dist\freemc.jar --main-class dev.freemc.Main -C build\classes . -C src\main\resources .
if errorlevel 1 exit /b 1

echo Hazir: dist\freemc.jar
