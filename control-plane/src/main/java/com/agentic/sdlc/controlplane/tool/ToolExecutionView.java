package com.agentic.sdlc.controlplane.tool;

import com.agentic.sdlc.controlplane.agent.AgentCapability;
import com.agentic.sdlc.controlplane.agent.AgentRole;

import java.time.Instant;
import java.util.UUID;

public record ToolExecutionView(
        UUID id,
        String nodeKey,
        AgentRole agentRole,
        ToolActionType tool,
        AgentCapability requiredCapability,
        ToolExecutionStatus status,
        String inputHash,
        String outputHash,
        Integer exitCode,
        String summary,
        Long latencyMs,
        Instant startedAt,
        Instant completedAt
) {
    public static ToolExecutionView from(ToolExecutionEntity entity) {
        return new ToolExecutionView(
                entity.getId(), entity.getNodeKey(), entity.getAgentRole(), entity.getToolType(),
                entity.getRequiredCapability(), entity.getStatus(), entity.getInputHash(), entity.getOutputHash(),
                entity.getExitCode(), entity.getSummary(), entity.getLatencyMs(), entity.getStartedAt(), entity.getCompletedAt());
    }
}
