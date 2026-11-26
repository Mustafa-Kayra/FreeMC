#!/usr/bin/env bash
# FreeMC derleme betiği (Linux / macOS)
set -euo pipefail
cd "$(dirname "$0")"

rm -rf build dist
mkdir -p build/classes dist

find src/main/java -name '*.java' > build/sources.txt
javac --release 17 -encoding UTF-8 -d build/classes @build/sources.txt
jar --create --file dist/freemc.jar --main-class dev.freemc.Main -C build/classes . -C src/main/resources .

echo "Hazır: dist/freemc.jar"
