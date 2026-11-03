#!/usr/bin/env bash
set -euo pipefail
CP="${CONTROL_PLANE_URL:-http://localhost:8081}"
ID="${1:?Usage: ./scripts/demo-evidence.sh <workflow-id>}"

echo "== Workflow =="; curl -fsS "$CP/api/workflows/$ID"; echo
echo "== Reliability =="; curl -fsS "$CP/api/observability/workflows/$ID"; echo
echo "== Final engineering summary =="; curl -fsS "$CP/api/workflows/$ID/engineering-summary"; echo
echo "== Model invocations =="; curl -fsS "$CP/api/workflows/$ID/model-invocations"; echo
echo "== Tool executions =="; curl -fsS "$CP/api/workflows/$ID/tool-executions"; echo
echo "== Artifacts =="; curl -fsS "$CP/api/workflows/$ID/artifacts"; echo
