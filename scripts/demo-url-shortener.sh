#!/usr/bin/env bash
set -euo pipefail
BASE_URL="${BASE_URL:-http://localhost:8082}"

printf '\n== create / replay-safe ==\n'
curl -sS -i -X POST "$BASE_URL/api/v1/urls" \
  -H 'Content-Type: application/json' \
  -H 'Idempotency-Key: demo-001' \
  -d '{"url":"https://example.com/docs","customAlias":"staffdemo"}'

printf '\n== redirect ==\n'
curl -sS -i "$BASE_URL/r/staffdemo" -H 'User-Agent: curl/8.0'

printf '\n== analytics ==\n'
curl -sS "$BASE_URL/api/v1/urls/staffdemo/analytics"; echo

printf '\n== deactivate ==\n'
curl -sS -i -X DELETE "$BASE_URL/api/v1/urls/staffdemo"

printf '\n== redirect after deactivate (expected 410) ==\n'
curl -sS -i "$BASE_URL/r/staffdemo" || true
