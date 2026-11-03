package com.agentic.sdlc.controlplane.llm;

import com.agentic.sdlc.controlplane.agent.AgentRole;

import java.util.Map;
import java.util.UUID;

public record ModelRequest(
        UUID requestId,
        UUID workflowId,
        String nodeKey,
        AgentRole agentRole,
        ModelPrompt prompt,
        String schemaName,
        Map<String, Object> responseSchema
) {
    public ModelRequest {
        responseSchema = Map.copyOf(responseSchema);
    }
}
