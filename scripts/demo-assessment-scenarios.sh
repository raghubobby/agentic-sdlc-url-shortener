#!/usr/bin/env bash
set -euo pipefail
CONTROL_PLANE="${CONTROL_PLANE:-http://localhost:8081}"

json_field() {
  python3 -c 'import json,sys; d=json.load(sys.stdin); print(eval(sys.argv[1], {"d":d}))' "$1"
}

start_scenario() {
  local scenario="$1"
  curl -sS -X POST "$CONTROL_PLANE/api/scenarios/$scenario/workflows" \
    -H 'Content-Type: application/json' -d '{"autoRun":true}'
}

printf '\n== available scenarios ==\n'
curl -sS "$CONTROL_PLANE/api/scenarios"; echo

printf '\n== ambiguous safe-stop ==\n'
AMBIGUOUS_JSON="$(start_scenario ambiguous)"
echo "$AMBIGUOUS_JSON"
WORKFLOW_ID="$(printf '%s' "$AMBIGUOUS_JSON" | json_field 'd["id"]')"

printf '\n== clarification request ==\n'
CLARIFICATIONS="$(curl -sS "$CONTROL_PLANE/api/workflows/$WORKFLOW_ID/clarifications")"
echo "$CLARIFICATIONS"
CLARIFICATION_ID="$(printf '%s' "$CLARIFICATIONS" | json_field 'd[0]["id"]')"

printf '\n== resolve ambiguity and resume ==\n'
curl -sS -X POST "$CONTROL_PLANE/api/workflows/$WORKFLOW_ID/clarifications/$CLARIFICATION_ID/resolve" \
  -H 'Content-Type: application/json' \
  -d '{"answer":"Protect enterprise links against enumeration and unauthorized redirect access. Enterprise links require authenticated access and tenant isolation. Existing public links remain backward compatible.","resolvedBy":"demo-reviewer","autoResume":true}'; echo

printf '\nThe workflow will subsequently pause at risk-based implementation/release approval gates.\n'
printf 'Inspect: %s/api/workflows/%s\n' "$CONTROL_PLANE" "$WORKFLOW_ID"
printf 'Summary: %s/api/workflows/%s/engineering-summary\n' "$CONTROL_PLANE" "$WORKFLOW_ID"
