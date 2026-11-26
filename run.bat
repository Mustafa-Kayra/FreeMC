@echo off
chcp 65001 >nul
rem FreeMC baslatma betigi (Windows)
setlocal
cd /d "%~dp0"

if not exist dist\freemc.jar (
  call build.bat
  if errorlevel 1 exit /b 1
)

where javaw >nul 2>&1
if errorlevel 1 goto nojava

java -version 2>&1 | findstr /c:"version \"1.8" /c:"version \"1.7" /c:"version \"11" /c:"version \"9" >nul 2>&1
if not errorlevel 1 (
  echo [HATA] Kurulu Java eski. FreeMC en az Java 17 ister.
  echo Java 21 kur: https://adoptium.net/temurin/releases/?version=21
  echo Ya da dogrudan acmak icin: jre klasoru iceren FreeMC-Windows zip'i kullan.
  pause
  exit /b 1
)

start "" javaw -jar "dist\freemc.jar" %*
exit /b 0

:nojava
echo [HATA] Java bulunamadi. FreeMC en az Java 17 ister.
echo Java 21 kur: https://adoptium.net/temurin/releases/?version=21
pause
exit /b 1
