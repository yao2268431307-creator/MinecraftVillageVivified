#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OUT="$ROOT/build/classes"

rm -rf "$OUT"
mkdir -p "$OUT"

find "$ROOT/src/main/java" "$ROOT/src/test/java" -name "*.java" | sort > "$ROOT/build/sources.txt"
javac -encoding UTF-8 -d "$OUT" @"$ROOT/build/sources.txt"
java -cp "$OUT" com.livingvillages.core.CoreTestRunner

