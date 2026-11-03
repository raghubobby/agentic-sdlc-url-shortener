#!/usr/bin/env bash
set -euo pipefail

CP="${CONTROL_PLANE_URL:-http://localhost:8081}"

echo "== Create greenfield workflow without starting it =="
CREATE=$(curl -fsS -X POST "$CP/api/scenarios/greenfield/workflows" \
  -H 'Content-Type: application/json' \
  -d '{"autoRun":false}')
echo "$CREATE"
WORKFLOW_ID=$(printf '%s' "$CREATE" | python3 -c 'import json,sys; print(json.load(sys.stdin)["id"])')
echo "workflowId=$WORKFLOW_ID"

echo
echo "== Arm one controlled transient failure on requirements =="
curl -fsS -X POST "$CP/api/demo/workflows/$WORKFLOW_ID/failures" \
  -H 'Content-Type: application/json' \
  -d '{
    "nodeKey":"requirements",
    "category":"TRANSIENT_INFRASTRUCTURE",
    "failures":1,
    "sideEffectsPossible":false,
    "message":"Demo: transient model/tool dependency unavailable",
    "actor":"assessment-demo"
  }'
echo

echo
echo "== Resume. Retry policy should recover requirements automatically =="
curl -fsS -X POST "$CP/api/workflows/$WORKFLOW_ID/resume"
echo

echo
echo "== Durable reliability snapshot =="
curl -fsS "$CP/api/observability/workflows/$WORKFLOW_ID"
echo

echo
echo "== Retry / failure-injection audit evidence =="
curl -fsS "$CP/api/workflows/$WORKFLOW_ID/audit" | python3 -c '
import json,sys
for e in json.load(sys.stdin):
    if e["eventType"] in {"FAILURE_INJECTION_ARMED","FAILURE_INJECTED","NODE_RETRY_SCHEDULED","NODE_SUCCEEDED"}:
        print("{}: {} node={} {}".format(e["eventSequence"], e["eventType"], e.get("nodeKey"), e["payload"]))
'

echo
echo "Prometheus: $CP/actuator/prometheus"
echo "Grafana (full demo compose): http://localhost:3000  admin/admin"
