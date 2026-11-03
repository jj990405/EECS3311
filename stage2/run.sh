#!/usr/bin/env bash
set -euo pipefail

root="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
mode="${1:-gui}"
data_directory="${2:-$root/data}"
build_directory="$root/build/classes"
pdfbox_jar="$root/lib/pdfbox-app-3.0.8.jar"
classpath="$build_directory:$pdfbox_jar"

if [[ "$mode" != gui && "$mode" != cli && "$mode" != test ]]; then
  echo "Usage: ./run.sh [gui|cli|test] [data-directory]" >&2
  exit 2
fi

if [[ -n "${JAVA_HOME:-}" ]]; then
  javac_command="$JAVA_HOME/bin/javac"
  java_command="$JAVA_HOME/bin/java"
else
  javac_command=javac
  java_command=java
fi

mkdir -p "$build_directory"
if [[ ! -f "$pdfbox_jar" ]]; then
  echo "Missing $pdfbox_jar" >&2
  exit 1
fi
mapfile -d '' sources < <(find "$root/src/main/java" -name '*.java' -print0)
"$javac_command" --release 17 -encoding UTF-8 -cp "$pdfbox_jar" -d "$build_directory" "${sources[@]}"

if [[ "$mode" == test ]]; then
  tests=("$root/src/test/java/eecs3311/studyagent/AgentControllerTest.java")
  "$javac_command" --release 17 -encoding UTF-8 -cp "$classpath" -d "$build_directory" "${tests[@]}"
  "$java_command" -Dproject2.root="$root" -cp "$classpath" eecs3311.studyagent.AgentControllerTest
elif [[ "$mode" == cli ]]; then
  "$java_command" -Dproject2.root="$root" -cp "$classpath" eecs3311.studyagent.Main --cli --data "$data_directory"
else
  "$java_command" -Dproject2.root="$root" -cp "$classpath" eecs3311.studyagent.Main --data "$data_directory"
fi
