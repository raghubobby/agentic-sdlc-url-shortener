#!/usr/bin/env bash
set -euo pipefail
export SPRING_PROFILES_ACTIVE=postgres
export URL_DB_URL="${URL_DB_URL:-jdbc:postgresql://localhost:5432/agentic?currentSchema=url_shortener}"
export URL_DB_USER="${URL_DB_USER:-agentic}"
export URL_DB_PASSWORD="${URL_DB_PASSWORD:-agentic}"
export SHORT_URL_CACHE_ENABLED="${SHORT_URL_CACHE_ENABLED:-false}"
exec mvn -pl url-shortener spring-boot:run
