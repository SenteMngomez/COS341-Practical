#!/bin/bash
# Location : <project>/Test cases/additional/run_tests.sh
# Usage    : bash run_tests.sh [category-prefix] [jar-or-classes-dir]
#   bash run_tests.sh                      -> all tests, using <project>/group-30.jar
#   bash run_tests.sh 04_codegen           -> only that folder
#   bash run_tests.sh 03 ../../src-classes -> custom classpath (jar OR directory of .class files)
# Each test is run in a throw-away folder, so your tree.xml / output.cpp are never overwritten.
# Results are checked against EXPECTED_RESULTS.txt (same folder). Exit code = number of failures.

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
FILTER="${1:-}"
CP="${2:-$HERE/../../group-30.jar}"
MAN="$HERE/EXPECTED_RESULTS.txt"

# absolute classpath (we cd into a temp folder before running java)
[[ -e "$CP" ]] || { echo "Classpath not found: $CP"; exit 99; }
CP="$(cd "$(dirname "$CP")" && pwd)/$(basename "$CP")"

# Git Bash / MSYS: java and g++ want Windows-style paths
winpath() { if command -v cygpath >/dev/null 2>&1; then cygpath -w "$1"; else echo "$1"; fi; }
JCP="$(winpath "$CP")"

pass=0; fail=0
for f in "$HERE"/*/*.txt; do
  rel="${f#$HERE/}"
  [[ -n "$FILTER" && "$rel" != $FILTER* ]] && continue
  name="$(basename "$f")"
  line="$(grep -F "$name " "$MAN" | head -1)"
  [[ -z "$line" ]] && { echo "SKIP  $rel (not in EXPECTED_RESULTS.txt)"; continue; }

  wd="$(mktemp -d)"
  err="$( cd "$wd" && java -cp "$JCP" Main "$(winpath "$f")" 2>&1 >/dev/null </dev/null )"
  got_stage="$(echo "$err" | grep -o '\[[A-Za-z]* Error\]' | head -1 | sed 's/\[\([A-Za-z]*\) Error\]/\1/')"

  ok=0; detail=""
  if [[ "$line" == *" ACCEPT "* ]]; then
    exp_out="$(echo "$line" | sed -n "s/.*output='\(.*\)'.*/\1/p")"
    if [[ -n "$got_stage" ]]; then
      detail="expected ACCEPT but got $got_stage error: $(echo "$err" | grep -m1 Error | cut -c1-100)"
    elif ! ( cd "$wd" && g++ -std=c++11 -pedantic-errors -o prog output.cpp 2>"$wd/gpp.log" ); then
      detail="generated C++ did not compile: $(head -c 150 "$wd/gpp.log")"
    else
      got_out="$( cd "$wd" && ./prog )"
      if [[ "$got_out" == "$exp_out" ]]; then ok=1; else detail="output '$got_out' expected '$exp_out'"; fi
    fi
  else
    exp_stage="$(echo "$line" | awk '{print $3}')"
    exp_text="$(echo "$line" | sed -n "s/.*containing '\(.*\)'.*/\1/p")"
    if [[ "$got_stage" == "$exp_stage" ]] && echo "$err" | grep -qF -- "$exp_text"; then ok=1
    else detail="expected $exp_stage error containing '$exp_text', got: ${got_stage:-no error} $(echo "$err" | grep -m1 Error | cut -c1-100)"; fi
  fi

  if [[ $ok -eq 1 ]]; then pass=$((pass+1)); echo "PASS  $rel"; else fail=$((fail+1)); echo "FAIL  $rel -> $detail"; fi
  rm -rf "$wd"
done
echo "-----------------------------------------"
echo "passed: $pass   failed: $fail"
exit $fail