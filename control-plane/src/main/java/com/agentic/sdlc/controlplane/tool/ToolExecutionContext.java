package com.agentic.sdlc.controlplane.tool;

import com.agentic.sdlc.controlplane.agent.AgentRole;
import com.agentic.sdlc.controlplane.agent.CapabilityEnvelope;

import java.nio.file.Path;
import java.util.UUID;

public record ToolExecutionContext(
        UUID workflowId,
        String nodeKey,
        int attempt,
        AgentRole agentRole,
        CapabilityEnvelope capabilityEnvelope,
        Path workspace
) {}
