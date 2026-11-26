#!/usr/bin/env bash
# Windows kurulum exe'si uretir (Linux uzerinde capraz derleme).
# Gereksinimler: mingw-w64, nsis, unzip, curl. Cikti: dist/FreeMC-Setup-<surum>.exe
set -euo pipefail
cd "$(dirname "$0")/../.."

VERSION_BASE_URL="https://api.adoptium.net/v3/binary/latest/21/ga/windows/x64/jre/hotspot/normal/eclipse"
STAGE="packaging/win/payload"

./build.sh

rm -rf "$STAGE"
mkdir -p "$STAGE" build

x86_64-w64-mingw32-windres packaging/win/FreeMC.rc -O coff -o build/FreeMC.res
x86_64-w64-mingw32-gcc -O2 -municode -mwindows -o "$STAGE/FreeMC.exe" packaging/win/FreeMC.c build/FreeMC.res
cp dist/freemc.jar "$STAGE/"
cp packaging/win/OKUBENI.txt "$STAGE/"

if [ ! -x "$STAGE/jre/bin/javaw.exe" ]; then
  echo "Windows JRE indiriliyor..."
  curl -sSL -o build/jre-win.zip "$VERSION_BASE_URL"
  rm -rf build/jre-unpack && mkdir -p build/jre-unpack
  unzip -q build/jre-win.zip -d build/jre-unpack
  mv build/jre-unpack/*/ "$STAGE/jre"
  rm -rf build/jre-unpack
fi

makensis packaging/win/FreeMC.nsi

rm -rf "$STAGE"
echo "Hazir: dist/FreeMC-Setup-1.2.0.exe"
