#!/usr/bin/env bash
set -euo pipefail

DB_NAME="${DB_NAME:-agentic}"
DB_USER="${DB_USER:-agentic}"
DB_PASSWORD="${DB_PASSWORD:-agentic}"

find_psql() {
  if command -v psql >/dev/null 2>&1; then command -v psql; return; fi
  for candidate in \
    /opt/homebrew/opt/postgresql@16/bin/psql \
    /opt/homebrew/bin/psql \
    /usr/local/opt/postgresql@16/bin/psql \
    /usr/local/bin/psql; do
    if [[ -x "$candidate" ]]; then echo "$candidate"; return; fi
  done
  return 1
}

PSQL="${PSQL:-$(find_psql || true)}"
if [[ -z "$PSQL" ]]; then
  echo "ERROR: psql was not found." >&2
  echo "Install PostgreSQL with: brew install postgresql@16" >&2
  echo "Then start it with: brew services start postgresql@16" >&2
  exit 1
fi

export PATH="$(dirname "$PSQL"):$PATH"

echo "Using psql: $PSQL"
echo "Creating local PostgreSQL role/database for the assessment (idempotent)..."
"$PSQL" postgres -v ON_ERROR_STOP=1 <<SQL
DO \$\$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = '${DB_USER}') THEN
    CREATE ROLE ${DB_USER} LOGIN PASSWORD '${DB_PASSWORD}';
  ELSE
    ALTER ROLE ${DB_USER} WITH LOGIN PASSWORD '${DB_PASSWORD}';
  END IF;
END
\$\$;
SELECT 'CREATE DATABASE ${DB_NAME} OWNER ${DB_USER}'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = '${DB_NAME}')\gexec
GRANT ALL PRIVILEGES ON DATABASE ${DB_NAME} TO ${DB_USER};
SQL

echo "Ready: jdbc:postgresql://localhost:5432/${DB_NAME} (user ${DB_USER})"
