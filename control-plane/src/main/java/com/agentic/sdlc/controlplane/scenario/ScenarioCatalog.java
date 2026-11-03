package com.agentic.sdlc.controlplane.scenario;

import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class ScenarioCatalog {
    private final List<ScenarioBlueprint> scenarios = List.of(
            new ScenarioBlueprint(
                    ScenarioType.GREENFIELD,
                    "Greenfield: single-use links",
                    "Add single-use short links. A caller can request a link that becomes permanently unavailable after its first successful redirect. Existing multi-use links must preserve current behavior.",
                    "Demonstrate requirement decomposition, API/data-model design, safe parallel implementation, validation, security review, and release governance for a new capability.",
                    List.of(
                            "Existing link behavior remains backward compatible",
                            "Single-use consumption is race-safe under concurrent redirects",
                            "A consumed single-use link returns 410 Gone",
                            "Cache behavior cannot allow a second redirect after consumption",
                            "Integration and security evidence are attached to release readiness"),
                    List.of(
                            "Greenfield decomposition",
                            "Concurrency reasoning",
                            "Backward compatibility",
                            "Fan-out/fan-in orchestration")),
            new ScenarioBlueprint(
                    ScenarioType.BROWNFIELD,
                    "Brownfield: redirect correctness under cache degradation",
                    "Production telemetry shows intermittent redirect correctness issues when cache state is stale or Redis is degraded. Diagnose the existing redirect path and produce the smallest safe change that preserves redirect availability without serving expired or deactivated links.",
                    "Force repository reasoning before implementation, constrain the change surface, require regression evidence, and preserve the product's database-as-source-of-truth reliability contract.",
                    List.of(
                            "Current redirect/cache/data-source behavior is documented before modification",
                            "The change is targeted rather than a rewrite",
                            "Redis degradation still falls back safely to PostgreSQL",
                            "Expired/deactivated links cannot be resurrected by stale cache state",
                            "Regression evidence covers the original failure mode"),
                    List.of(
                            "Brownfield codebase reasoning",
                            "Smallest-safe-change judgment",
                            "Reliability and cache consistency",
                            "Regression-driven validation")),
            new ScenarioBlueprint(
                    ScenarioType.AMBIGUOUS,
                    "Ambiguous: enterprise link security",
                    "Make shortened links more secure for enterprise customers.",
                    "Demonstrate that controlled autonomy includes knowing when not to code. Material ambiguity must trigger a human clarification gate before planning or implementation proceeds.",
                    List.of(
                            "Material ambiguity is surfaced explicitly",
                            "No planning or implementation occurs before human clarification",
                            "The clarification becomes a versioned requirement artifact",
                            "Only downstream work is invalidated/replanned after clarification",
                            "The final summary preserves the human decision and evidence lineage"),
                    List.of(
                            "Requirement judgment",
                            "Safe-stop behavior",
                            "Human authority",
                            "Decision lineage and replanning"))
    );

    public List<ScenarioBlueprint> all() { return scenarios; }

    public ScenarioBlueprint get(ScenarioType type) {
        return scenarios.stream().filter(s -> s.type() == type).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown scenario: " + type));
    }

    public ScenarioBlueprint get(String value) { return get(ScenarioType.from(value)); }
}
