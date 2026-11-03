#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

find_maven() {
  if command -v mvn >/dev/null 2>&1; then command -v mvn; return; fi
  for candidate in /opt/homebrew/bin/mvn /usr/local/bin/mvn; do
    if [[ -x "$candidate" ]]; then echo "$candidate"; return; fi
  done
  return 1
}

MAVEN_BIN="${MAVEN_BIN:-$(find_maven || true)}"
if [[ -z "$MAVEN_BIN" ]]; then
  echo "ERROR: Maven was not found. Install it with: brew install maven" >&2
  exit 1
fi
export PATH="$(dirname "$MAVEN_BIN"):$PATH"

if ! git rev-parse --is-inside-work-tree >/dev/null 2>&1; then
  echo "No Git baseline found. Bootstrapping the extracted assessment repo..."
  ./scripts/bootstrap-local-repo.sh
fi

export DB_URL="${DB_URL:-jdbc:postgresql://localhost:5432/agentic}"
export DB_USERNAME="${DB_USERNAME:-agentic}"
export DB_PASSWORD="${DB_PASSWORD:-agentic}"
export MODEL_PROVIDER="${MODEL_PROVIDER:-deterministic}"
export AGENTIC_FAILURE_INJECTION_ENABLED="${AGENTIC_FAILURE_INJECTION_ENABLED:-true}"
export REPOSITORY_ROOT="${REPOSITORY_ROOT:-$ROOT}"
export URL_SHORTENER_BASE_URL="${URL_SHORTENER_BASE_URL:-http://localhost:8082}"
export URL_SHORTENER_PUBLIC_URL="${URL_SHORTENER_PUBLIC_URL:-http://localhost:8082}"

exec "$MAVEN_BIN" -pl control-plane spring-boot:run
