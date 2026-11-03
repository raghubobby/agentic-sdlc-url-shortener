package com.agentic.sdlc.controlplane.observability;

import com.agentic.sdlc.controlplane.workflow.WorkflowStatus;

import java.util.List;
import java.util.UUID;

public record WorkflowReliabilitySnapshot(
        UUID workflowId,
        String scenario,
        WorkflowStatus status,
        int planRevision,
        long endToEndLatencyMs,
        int totalNodes,
        int succeededNodes,
        int failedNodes,
        double nodeSuccessRate,
        int retryCount,
        int recoveredNodeCount,
        Long meanTimeToRecoveryMs,
        int compensationCount,
        int replanCount,
        int policyDenialCount,
        int approvalRequestCount,
        Long averageApprovalWaitMs,
        int clarificationCount,
        Long averageClarificationWaitMs,
        int modelInvocationCount,
        int toolExecutionCount,
        int artifactCount,
        List<String> activeRisks
) {}
