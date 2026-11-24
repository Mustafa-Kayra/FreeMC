#!/usr/bin/env bash
# FreeMC başlatma betiği (Linux / macOS)
set -euo pipefail
cd "$(dirname "$0")"

if [ ! -f dist/freemc.jar ]; then
  ./build.sh
fi

exec java -jar dist/freemc.jar "$@"
