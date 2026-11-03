package com.agentic.sdlc.controlplane.observability;

public record SystemReliabilitySnapshot(
        long totalWorkflows,
        long completedWorkflows,
        long failedWorkflows,
        long activeOrBlockedWorkflows,
        double workflowSuccessRate,
        long nodeAttemptCount,
        long retryCount,
        double retryFrequency,
        long compensationCount,
        double compensationFrequency,
        long replanCount,
        long policyDenialCount,
        long modelInvocationCount,
        long toolExecutionCount,
        long artifactCount
) {}
