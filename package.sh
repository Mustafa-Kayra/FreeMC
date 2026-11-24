#!/usr/bin/env bash
# FreeMC kurulabilir paket (Linux / macOS): dist/package/FreeMC
set -euo pipefail
cd "$(dirname "$0")"

./build.sh
rm -rf dist/package
mkdir -p dist/package

jpackage \
  --type app-image \
  --name FreeMC \
  --app-version 1.1.0 \
  --input dist \
  --main-jar freemc.jar \
  --main-class dev.freemc.Main \
  --dest dist/package

echo "Hazır: dist/package/FreeMC (çalıştır: dist/package/FreeMC/bin/FreeMC)"
