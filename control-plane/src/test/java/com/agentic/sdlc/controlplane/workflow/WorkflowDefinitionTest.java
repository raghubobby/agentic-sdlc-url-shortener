package com.agentic.sdlc.controlplane.workflow;

import com.agentic.sdlc.controlplane.governance.NodeKind;
import com.agentic.sdlc.controlplane.recovery.RetryPolicy;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WorkflowDefinitionTest {

    @Test
    void exposesParallelImplementationNodesAfterArchitectureCompletes() {
        WorkflowDefinition definition = WorkflowDefinition.defaultEngineeringWorkflow();
        Map<String, NodeStatus> statuses = pendingStatuses(definition);
        statuses.put("requirements", NodeStatus.SUCCEEDED);
        statuses.put("planning", NodeStatus.SUCCEEDED);
        statuses.put("architecture", NodeStatus.SUCCEEDED);

        List<String> ready = definition.readyNodes(statuses).stream()
                .map(WorkflowNodeDefinition::key)
                .toList();

        assertThat(ready).containsExactly("implementation-api", "implementation-persistence");
    }

    @Test
    void fansOutValidationAndSecurityReviewAfterImplementationJoins() {
        WorkflowDefinition definition = WorkflowDefinition.defaultEngineeringWorkflow();
        Map<String, NodeStatus> statuses = pendingStatuses(definition);
        statuses.put("requirements", NodeStatus.SUCCEEDED);
        statuses.put("planning", NodeStatus.SUCCEEDED);
        statuses.put("architecture", NodeStatus.SUCCEEDED);
        statuses.put("implementation-api", NodeStatus.SUCCEEDED);
        statuses.put("implementation-persistence", NodeStatus.SUCCEEDED);

        // Both nodes are in the same execution wave and may run in parallel.
        // Their relative order is not part of the workflow semantics.
        assertThat(definition.readyNodes(statuses).stream().map(WorkflowNodeDefinition::key).toList())
                .containsExactlyInAnyOrder("validation-tests", "security-review");
    }

    @Test
    void computesOnlyDownstreamNodesForSelectiveReplanning() {
        WorkflowDefinition definition = WorkflowDefinition.defaultEngineeringWorkflow();

        // Descendants represent an impacted-node set for selective replanning;
        // ordering is intentionally not part of that contract.
        assertThat(definition.descendantsOf("implementation-api"))
                .containsExactlyInAnyOrder(
                        "validation-tests",
                        "security-review",
                        "release-approval",
                        "release-readiness");

        assertThat(definition.descendantsOf("planning"))
                .containsExactlyInAnyOrder(
                        "architecture",
                        "implementation-api",
                        "implementation-persistence",
                        "validation-tests",
                        "security-review",
                        "release-approval",
                        "release-readiness");
    }

    @Test
    void detectsCyclesAtDefinitionTime() {
        assertThatThrownBy(() -> new WorkflowDefinition(List.of(
                new WorkflowNodeDefinition("a", Set.of("b"), NodeKind.VALIDATION, Set.of(), RetryPolicy.noRetry(), false),
                new WorkflowNodeDefinition("b", Set.of("a"), NodeKind.VALIDATION, Set.of(), RetryPolicy.noRetry(), false)
        ))).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("cycle");
    }

    private Map<String, NodeStatus> pendingStatuses(WorkflowDefinition definition) {
        Map<String, NodeStatus> statuses = new HashMap<>();
        definition.nodes().forEach(node -> statuses.put(node.key(), NodeStatus.PENDING));
        return statuses;
    }
}
