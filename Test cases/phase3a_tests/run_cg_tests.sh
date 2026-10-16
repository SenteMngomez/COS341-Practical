#!/bin/bash
# Phase 3a test runner.
#
# Location:  <project>/Test cases/phase3a_tests/run_cg_tests.sh
# Tests:     <project>/Test cases/phase3a_tests/cg/*.spl  (+ matching *.expected)
# Sources:   <project>/src/**/*.java   (Main.java is in the default package)
#
# Run from anywhere:   bash "Test cases/phase3a_tests/run_cg_tests.sh"
# Needs on PATH:       javac, java, g++   (Git Bash / WSL / Linux / macOS)

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "$HERE/../.." && pwd)"
SRC="$ROOT/src"
CG="$HERE/cg"

# Windows (Git Bash / MSYS) paths must be converted for java.exe / javac.exe / g++.exe.
winpath() {
    if command -v cygpath >/dev/null 2>&1; then cygpath -m "$1"; else printf '%s' "$1"; fi
}

for tool in javac java g++; do
    command -v "$tool" >/dev/null 2>&1 || { echo "ERROR: '$tool' not found on PATH."; exit 2; }
done

WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT
BUILD="$WORK/classes"
mkdir -p "$BUILD"

# ---------------------------------------------------------------- compile
echo "== Compiling $SRC"
( cd "$SRC" && find . -name '*.java' | sed 's/.*/"&"/' > "$WORK/sources.txt" )
( cd "$SRC" && javac -encoding UTF-8 -d "$(winpath "$BUILD")" @"$(winpath "$WORK/sources.txt")" ) \
    || { echo "COMPILATION FAILED"; exit 1; }

# ------------------------------------------------------------------ tests
pass=0
fail=0

shopt -s nullglob
for spl in "$CG"/*.spl; do
    name="$(basename "$spl" .spl)"
    dir="$WORK/run_$name"
    mkdir -p "$dir"

    # Main writes output.cpp / output_runnable.cpp / tree.xml into the working directory.
    ( cd "$dir" && java -cp "$(winpath "$BUILD")" Main "$(winpath "$spl")" --runnable > main.log 2>&1 )
    rc=$?
    if [ "$rc" -ne 0 ]; then
        echo "FAIL $name: compiler exit code $rc: $(grep -E 'Error' "$dir/main.log" | head -1)"
        fail=$((fail + 1)); continue
    fi

    if ! ( cd "$dir" && g++ -std=c++17 -Wall -Wextra -o a.exe output_runnable.cpp 2> gpp.log ); then
        echo "FAIL $name: g++ rejected the generated code"
        head -5 "$dir/gpp.log"
        fail=$((fail + 1)); continue
    fi

    got="$( cd "$dir" && ./a.exe | tr -d '\r' )"
    expected="$( tr -d '\r' < "${spl%.spl}.expected" )"

    if [ "$got" == "$expected" ]; then
        echo "PASS $name"
        pass=$((pass + 1))
    else
        echo "FAIL $name: got '$got', expected '$expected'"
        fail=$((fail + 1))
    fi
done

echo
echo "passed=$pass failed=$fail"
[ "$fail" -eq 0 ]