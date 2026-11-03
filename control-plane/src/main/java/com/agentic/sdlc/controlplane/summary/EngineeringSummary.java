package com.agentic.sdlc.controlplane.summary;

import com.agentic.sdlc.controlplane.workflow.WorkflowStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record EngineeringSummary(
        UUID workflowId,
        String scenario,
        String requirement,
        WorkflowStatus status,
        int planRevision,
        String baseCommit,
        Instant generatedAt,
        ExecutionMetrics metrics,
        List<String> decisionsAndGovernance,
        List<String> validationEvidence,
        List<String> risksAndTradeoffs,
        List<String> assumptionsAndLimitations,
        String markdown
) {
    public record ExecutionMetrics(
            int totalNodes,
            long succeededNodes,
            long failedNodes,
            int artifacts,
            int modelInvocations,
            int toolExecutions,
            long retries,
            long compensations,
            int replans,
            int clarifications,
            long approvalRequests,
            long policyDenials,
            long failureInjections
    ) {}
}
