package com.agentic.sdlc.controlplane.governance;

import com.agentic.sdlc.controlplane.recovery.RetryPolicy;
import com.agentic.sdlc.controlplane.workflow.WorkflowNodeDefinition;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PolicyEngineTest {
    private final PolicyEngine engine = new PolicyEngine(new RiskClassifier());

    @Test
    void dataModelImplementationRequiresHumanApproval() {
        WorkflowNodeDefinition node = new WorkflowNodeDefinition(
                "db-change", Set.of(), NodeKind.IMPLEMENTATION,
                Set.of(ChangeImpact.DATA_MODEL), RetryPolicy.noRetry(), true);

        PolicyOutcome outcome = engine.evaluate(node);

        assertThat(outcome.risk().level()).isEqualTo(RiskLevel.HIGH);
        assertThat(outcome.decision()).isEqualTo(PolicyDecision.REQUIRE_HUMAN_APPROVAL);
        assertThat(outcome.requiredApprovals()).isEqualTo(1);
    }

    @Test
    void criticalReleaseRequiresTwoIndependentApprovals() {
        WorkflowNodeDefinition node = new WorkflowNodeDefinition(
                "release", Set.of(), NodeKind.RELEASE,
                Set.of(ChangeImpact.RELEASE_ACTION), RetryPolicy.noRetry(), false);

        PolicyOutcome outcome = engine.evaluate(node);

        assertThat(outcome.risk().level()).isEqualTo(RiskLevel.CRITICAL);
        assertThat(outcome.requiredApprovals()).isEqualTo(2);
    }

    @Test
    void directProductionMutationIsDeniedEvenWithHumanApprovalAvailable() {
        WorkflowNodeDefinition node = new WorkflowNodeDefinition(
                "unsafe", Set.of(), NodeKind.IMPLEMENTATION,
                Set.of(ChangeImpact.DIRECT_PRODUCTION_MUTATION), RetryPolicy.noRetry(), true);

        PolicyOutcome outcome = engine.evaluate(node);

        assertThat(outcome.decision()).isEqualTo(PolicyDecision.DENY);
        assertThat(outcome.reason()).contains("not permitted");
    }
}
