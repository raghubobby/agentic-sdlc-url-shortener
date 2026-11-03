package com.agentic.sdlc.controlplane.workflow;

import com.agentic.sdlc.controlplane.governance.RiskLevel;
import com.agentic.sdlc.controlplane.recovery.CompensationStatus;
import com.agentic.sdlc.controlplane.recovery.FailureCategory;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record WorkflowSnapshot(
        UUID id,
        String scenario,
        String requirement,
        WorkflowStatus status,
        int planRevision,
        String baseCommit,
        long version,
        Instant startedAt,
        Instant completedAt,
        List<NodeSnapshot> nodes
) {
    public static WorkflowSnapshot from(WorkflowRunEntity run, List<WorkflowNodeExecutionEntity> nodes) {
        return new WorkflowSnapshot(
                run.getId(), run.getScenario(), run.getRequirement(), run.getStatus(), run.getPlanRevision(), run.getBaseCommit(), run.getLockVersion(),
                run.getStartedAt(), run.getCompletedAt(), nodes.stream().map(NodeSnapshot::from).toList());
    }

    public record NodeSnapshot(
            String key,
            NodeStatus status,
            RiskLevel riskLevel,
            boolean requiresApproval,
            int attempt,
            int generation,
            int validatedRevision,
            FailureCategory failureCategory,
            String lastError,
            CompensationStatus compensationStatus,
            Instant startedAt,
            Instant completedAt
    ) {
        static NodeSnapshot from(WorkflowNodeExecutionEntity node) {
            return new NodeSnapshot(
                    node.getNodeKey(), node.getStatus(), node.getRiskLevel(), node.isRequiresApproval(),
                    node.getAttempt(), node.getGeneration(), node.getValidatedRevision(),
                    node.getFailureCategory(), node.getLastError(), node.getCompensationStatus(),
                    node.getStartedAt(), node.getCompletedAt());
        }
    }
}
