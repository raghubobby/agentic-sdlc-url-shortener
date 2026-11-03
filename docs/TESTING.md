# Testing, limitations, and trade-offs

The test strategy is designed around the same trust boundary as the runtime: AI can propose work, but deterministic tests and trusted infrastructure provide acceptance evidence.

## Test layers

| Layer | Purpose |
|---|---|
| Unit tests | DAG validation, risk/policy behavior, retry classification, hashing, clarification, tool scope, capability enforcement, and domain logic |
| Spring/product tests | URL creation, redirect lifecycle, validation, idempotency/collision behavior, and application wiring |
| Integration tests | PostgreSQL and Redis behavior through Testcontainers under Maven Failsafe |
| CI verification | `mvn clean verify`, Compose validation, and Docker image builds |
| Runtime evidence | Scenario state, approvals, audit history, model/tool execution records, artifacts, and reliability snapshots |

## Commands

Fast local test path:

```bash
mvn test
```

Full verification with Docker available:

```bash
mvn clean verify
docker compose -f docker-compose.demo.yml config --quiet
```

GitHub Actions runs the full verification path and builds both application images.

## Trust-boundary decision

The model-influenced execution environment is not given Docker-daemon authority. In other words, `/var/run/docker.sock` is not part of the agent tool surface.

That is intentional. Bounded engineering tools can compile code, run appropriate tests, inspect repositories, generate diffs, and run static checks. Containerized integration tests execute in the trusted CI/evaluator boundary where infrastructure authority is explicit.

## What to verify during evaluation

For the control plane:

- dependency ordering and fan-out/fan-in behavior;
- high/critical approval gates;
- duplicate approval/idempotency behavior;
- clarification stop before planning;
- retry classification and bounded retry counts;
- capability and repository-scope enforcement;
- artifact hashing/lineage and selective replanning;
- audit/model/tool evidence persistence.

For the URL shortener:

- creation and retrieval;
- redirect lifecycle and `410 Gone` behavior;
- target URL validation;
- idempotency behavior;
- short-code collision handling;
- PostgreSQL-backed integration path;
- Redis degradation/fallback behavior where applicable.

## Prototype limitations

- The orchestrator is a single application process. PostgreSQL makes state durable, but there is no distributed scheduler leader election.
- Policy is code-based rather than managed through an external policy language or policy service.
- Git worktree execution is local. A production system would normally use ephemeral sandbox workers with stronger resource isolation.
- Compensation demonstrates retry/rollback orchestration semantics; the exact rollback mechanism for a real production action would be domain-specific.
- The deterministic model provider proves repeatable orchestration and structured-output handling, not semantic model quality.
- The prototype does not attempt production-grade multi-tenant authentication/authorization for the control-plane UI/API.

## Engineering trade-offs

### PostgreSQL instead of file/in-memory workflow state

The workflow needs transactions, restart durability, concurrency-safe approvals, and an auditable history. PostgreSQL is a small infrastructure cost for a much clearer correctness boundary.

### Deterministic provider by default

A live model would make the demo dependent on credentials, network access, rate limits, and changing model behavior. Deterministic mode makes the evaluator path repeatable while retaining a provider-neutral model interface and optional live adapter.

### Custom DAG engine for this prototype

The assessment specifically benefits from making orchestration behavior visible in code. This would not automatically be the right choice for a production platform. At larger scale, adopting a mature workflow engine would likely reduce operational risk.

### Database-authoritative redirects with optional Redis

Correctness should not depend on cache availability. Redis can improve latency, but an unavailable or stale cache must not decide whether a link is valid.

### No autonomous deployment

The system produces release-readiness evidence and enforces a critical approval gate, but it intentionally has no production deployment capability. This is a governance boundary, not an unfinished deployment feature.
