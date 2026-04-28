#!/usr/bin/env sh
set -eu

ROOT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
CLASSES_DIR="${TMPDIR:-/tmp}/codemetric-studio-classes"

CP="$CLASSES_DIR"
CP="$CP:$HOME/.m2/repository/info/picocli/picocli/4.7.6/picocli-4.7.6.jar"
CP="$CP:$HOME/.m2/repository/com/fasterxml/jackson/core/jackson-databind/2.18.0/jackson-databind-2.18.0.jar"
CP="$CP:$HOME/.m2/repository/com/fasterxml/jackson/core/jackson-core/2.18.0/jackson-core-2.18.0.jar"
CP="$CP:$HOME/.m2/repository/com/fasterxml/jackson/core/jackson-annotations/2.18.0/jackson-annotations-2.18.0.jar"
CP="$CP:$HOME/.m2/repository/com/github/javaparser/javaparser-core/3.26.3/javaparser-core-3.26.3.jar"

rm -rf "$CLASSES_DIR"
mkdir -p "$CLASSES_DIR"
cd "$ROOT_DIR"

javac --release 17 -cp "$CP" -d "$CLASSES_DIR" $(find src/main/java -name '*.java')
exec java -cp "$CP" com.codemetricstudio.cli.Main serve --port "${1:-9090}"
