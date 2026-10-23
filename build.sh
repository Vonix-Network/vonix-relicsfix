#!/usr/bin/env bash
# Rebuild vonix_relicsfix from source without Gradle.
# Requires: JDK 21, a NeoForge 21.1.x server install (for libraries/), and the target Relics jar.
set -euo pipefail
JAVAC=${JAVAC:-javac}
JAR=${JAR:-jar}
LIB=${LIB:-libraries}          # path to a NeoForge server install's libraries/
OUT=${OUT:-build/classes}
MIXIN=$(ls "$LIB"/net/fabricmc/sponge-mixin/*/*.jar | head -1)
LOADER=$(ls "$LIB"/net/neoforged/fancymodloader/loader/*/*.jar | head -1)
SLF=$(ls "$LIB"/org/slf4j/slf4j-api/*/*.jar | head -1)
BUS=$(ls "$LIB"/net/neoforged/bus/*/bus-*.jar | head -1)
# NOTE: on Windows javac separates classpath entries with ';' not ':'
SEP=$(case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) echo ';' ;; *) echo ':' ;; esac)
CP="$MIXIN${SEP}$LOADER${SEP}$SLF${SEP}$BUS"
rm -rf "$OUT"; mkdir -p "$OUT"
find src -name '*.java' > build/sources.txt
"$JAVAC" -proc:none -nowarn -d "$OUT" -cp "$CP" @build/sources.txt
cp -r resources/* "$OUT"/
(cd "$OUT" && "$JAR" --create --file ../vonix_relicsfix.jar .)
sha256sum build/vonix_relicsfix.jar || shasum -a 256 build/vonix_relicsfix.jar
