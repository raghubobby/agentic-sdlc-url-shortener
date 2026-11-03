#!/usr/bin/env bash
set -euo pipefail

fail=0
check() {
  local name="$1"; shift
  if "$@" >/dev/null 2>&1; then
    printf 'PASS  %s\n' "$name"
  else
    printf 'FAIL  %s\n' "$name"
    fail=1
  fi
}

check "Java 21" bash -c 'java -version 2>&1 | grep -q "version \"21"'
check "Git" git --version
check "Maven" mvn -version

if git rev-parse --is-inside-work-tree >/dev/null 2>&1; then
  printf 'PASS  Git checkout\n'
else
  printf 'INFO  Not yet a Git checkout; run ./scripts/bootstrap-local-repo.sh before agent worktree execution.\n'
fi

if docker version >/dev/null 2>&1 && docker compose version >/dev/null 2>&1; then
  printf 'PASS  Docker / Compose (full demo + Testcontainers available)\n'
else
  printf 'INFO  Docker unavailable; local PostgreSQL + H2 product mode is still supported.\n'
fi

if [[ "$fail" -ne 0 ]]; then
  echo "Preflight failed. Fix required prerequisites above."
  exit 1
fi

echo "Preflight passed."
