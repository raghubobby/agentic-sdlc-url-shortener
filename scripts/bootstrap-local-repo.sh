#!/usr/bin/env bash
set -euo pipefail

if git rev-parse --is-inside-work-tree >/dev/null 2>&1; then
  echo "Already inside a Git checkout: $(git rev-parse --show-toplevel)"
  exit 0
fi

git init -q -b main
git add .
git -c user.name="Agentic SDLC Local" -c user.email="local@example.invalid" commit -qm "baseline"
echo "Initialized local Git baseline at $(git rev-parse HEAD)"
