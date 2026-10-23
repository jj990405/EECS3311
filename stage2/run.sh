#!/usr/bin/env bash
set -euo pipefail

root="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
mode="${1:-gui}"
data_directory="${2:-$root/data}"
build_directory="$root/build/classes"

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
mapfile -d '' sources < <(find "$root/src/main/java" -name '*.java' -print0)
"$javac_command" --release 17 -encoding UTF-8 -d "$build_directory" "${sources[@]}"

if [[ "$mode" == test ]]; then
  mapfile -d '' tests < <(find "$root/src/test/java" -name '*.java' -print0)
  "$javac_command" --release 17 -encoding UTF-8 -cp "$build_directory" -d "$build_directory" "${tests[@]}"
  "$java_command" -cp "$build_directory" eecs3311.studyagent.AgentControllerTest
elif [[ "$mode" == cli ]]; then
  "$java_command" -cp "$build_directory" eecs3311.studyagent.Main --cli --data "$data_directory"
else
  "$java_command" -cp "$build_directory" eecs3311.studyagent.Main --data "$data_directory"
fi
