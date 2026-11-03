# Architecture notes

This document captures the design decisions that are easiest to miss when looking only at the UI. The root README is the primary entry point; this file is intentionally limited to the deeper control-plane architecture.

## 1. System boundary

The repository contains two runtimes with different responsibilities:

- **Control plane (`control-plane/`, port 8081):** workflow state, agent invocation, governance, approvals, tool authorization, artifacts, audit, retry/recovery, and replanning.
- **Target application (`url-shortener/`, port 8082):** the Spring Boot application the workflow reasons about and changes.

They are separate processes because an engineering agent should not be able to make itself authoritative by changing the application it is evaluating. The control plane owns the state machine and policy boundary; the target remains a workload.

## 2. Workflow model

The default engineering workflow is a persisted DAG:

```text
requirements
    ↓
planning
    ↓
architecture
   ↙       ↘
API       persistence
   ↘       ↙
validation  security
      ↘    ↙
  release approval
        ↓
 release readiness
```

The graph is validated for missing dependencies and cycles. Ready nodes are derived from persisted node status rather than from model output. Independent nodes can execute in parallel and synchronize at dependency boundaries.

This keeps orchestration semantics deterministic even when an agent's reasoning is probabilistic.

## 3. Model boundary

Agents produce typed structured outputs through `ModelGateway`. The gateway is provider-neutral:

- `deterministic` is the default for reproducible evaluation and CI;
- `openai-responses` is an optional live provider adapter.

Changing the model provider does not change workflow authority. Agents may produce analysis, assumptions, risks, artifacts, and requested tool actions, but they do not write workflow state directly.

## 4. Governance

`RiskClassifier` and `PolicyEngine` are deterministic components. Risk is calculated from the workflow node kind plus declared change impacts.

Examples:

- requirement analysis and validation start low risk;
- implementation starts medium risk;
- a data-model impact raises an implementation branch to high risk;
- release plus release-action impact becomes critical;
- direct production mutation is denied regardless of model output.

Policy translates the risk into one of three outcomes: allow, require human approval, or deny. High-risk work requires one reviewer; critical work requires two independent reviewers.

The important boundary is that **the model does not score its own risk and cannot approve itself**.

## 5. Tool execution and workspace isolation

Agents do not receive a generic shell. Tool requests pass through a Tool Broker that checks the requesting agent's capability envelope and the repository scope before execution.

The implementation path uses detached Git worktrees pinned to the workflow's base commit. Parallel implementation branches can therefore produce independent diffs without modifying the evaluator's main checkout. Validation can assemble the relevant changes into a clean integration workspace rather than testing whichever branch happened to finish last.

Capabilities intentionally exclude:

- arbitrary shell access;
- writes to the main checkout;
- Git push;
- production deployment.

This is the primary safety mechanism for model-driven engineering actions.

## 6. Artifact lineage and replanning

Successful stages publish versioned artifacts with content hashes. Dependencies record which upstream artifact versions were consumed.

When an upstream decision changes, the replanner:

1. creates a new artifact version rather than overwriting history;
2. computes affected descendants from artifact lineage and DAG dependencies;
3. marks only impacted work stale;
4. supersedes approvals that belong to stale node generations;
5. increments the plan revision;
6. preserves unaffected completed work;
7. resumes from the new ready frontier.

This avoids the two common extremes: silently continuing with stale evidence or restarting the entire workflow unnecessarily.

## 7. Clarification as a first-class state

The ambiguous scenario demonstrates a deliberate safe-stop. If requirement analysis identifies material ambiguity, the workflow enters `WAITING_FOR_CLARIFICATION` before planning.

The human response is persisted with the workflow and becomes authoritative input to a revised requirement artifact. Downstream work then follows the normal replanning path.

This is an important design point: uncertainty is not treated as permission for the model to invent product policy.

## 8. Persistence and observability

PostgreSQL is used for control-plane state because the following need durable transactions and concurrency control:

- workflow/node state;
- approvals and reviewer decisions;
- model invocation records;
- tool execution records;
- artifacts and dependencies;
- audit events;
- replanning history;
- failure-injection/recovery evidence.

Micrometer exposes runtime metrics to Prometheus. Workflow reliability views are reconstructed from durable execution evidence rather than maintained only as in-memory counters.

## 9. URL-shortener data/cache model

The target application keeps the database authoritative. Redis is an optional acceleration layer and is not allowed to become a correctness boundary.

That choice drives the brownfield scenario: cache degradation should reduce performance, not make a valid redirect unavailable or resurrect an inactive link. The same principle applies to single-use links: consumption state must be protected by the durable data path, not by process-local or cache-only coordination.

## 10. Deliberate omissions

### Why not Kafka?

There is no demonstrated requirement for high-volume asynchronous fan-out, replay, or independently scaled consumers. Adding a broker would increase operational surface without improving the core assessment signal.

### Why not Temporal/Camunda?

A small custom DAG engine makes the assessment's orchestration semantics visible: dependency scheduling, approval pauses, retries, selective invalidation, and resume behavior can all be inspected in the code. For a production platform with larger workflow volume and stronger operational requirements, adopting a mature workflow engine would be a serious design option.

### Why no autonomous deployment?

Release readiness is an engineering output; production mutation is a separate authority boundary. The prototype stops before deployment by design.
