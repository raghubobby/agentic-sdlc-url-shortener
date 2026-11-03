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

# Zero-infrastructure product mode uses H2 and no Redis by default.
exec "$MAVEN_BIN" -pl url-shortener spring-boot:run
